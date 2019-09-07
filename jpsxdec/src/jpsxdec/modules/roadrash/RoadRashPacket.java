/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.modules.roadrash;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SoundUnitDecoder;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import static jpsxdec.modules.roadrash.BitStreamUncompressorRoadRash.ROAD_RASH_BIT_CODE_ORDER;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.ZeroRunLengthAc;
import jpsxdec.psxvideo.bitstreams.ZeroRunLengthAcLookup;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;

/**
 * Support for "Road Rash 3D" videos -- but actually support for a video format
 * used in at least 2 games made by Electronic Arts.
 * It takes a unique approach to bitstreams, every movie has its own unique VLC table.
 * There's a lot of big-endian data in here.
 */
public abstract class RoadRashPacket {
    
    public static final int FRAMES_PER_SECOND = 15;
    public static final int SECTORS150_PER_FRAME = 10;
    public static final int SAMPLE_FRAMES_PER_SECOND = 22050;
    public static final int SAMPLE_FRAMES_PER_SECTOR = SAMPLE_FRAMES_PER_SECOND / 150;
    static {
        if (SAMPLE_FRAMES_PER_SECOND % 150 != 0) // assert
            throw new RuntimeException("Road Rash sample rate doesn't cleanly divide by sector rate");
    }

    /** 456 for Road Rash. Just a sanity check.
     * Theoretical smallest AU packet could be 32 bytes (15*2 round up).
     * Theoretical smallest MDEC packet could be dims+str header+at least 1
     * macro block, whatever that adds up to. */
    public static final int MIN_PACKET_SIZE = 32;
    /** All packets: 14200 for Road Rash, 15464 for SCUS-94276.
     * Just a size sanity check of 50 sectors. */
    public static final int MAX_PACKET_SIZE = 50 * 2048;
    /** 10 minutes. */
    private static final int MAX_PRESENTATION_SAMPLE_FRAME = SAMPLE_FRAMES_PER_SECOND * 60 * 10;
    /** 10 minutes. */
    private static final int MAX_FRAME_NUMBER = FRAMES_PER_SECOND * 60* 10;
    private static final int MIN_FRAME_DIMENSIONS = 16;
    private static final int MAX_FRAME_DIMENSIONS = 640;

    public static final long MAGIC_VLC0 = 0x564c4330;
    private static final long MAGIC_MDEC = 0x4d444543;
    private static final long MAGIC_au00 = 0x61753030;
    private static final long MAGIC_au01 = 0x61753031;

    public static final AudioFormat ROAD_RASH_AUDIO_FORMAT = new AudioFormat(SAMPLE_FRAMES_PER_SECOND, 16, 2, true, false);


    private static int _iMinPacketSize = Integer.MAX_VALUE;
    private static int _iMaxPacketSize = 0;

    public static class Header {
        public static final int SIZEOF = 8;

        public static Header read(@Nonnull InputStream is)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            return read(is, true);
        }

        private static Header read(@Nonnull InputStream is, boolean blnThrowEx)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            long lngPacketType = IO.readUInt32BE(is);

            // check for all zeroes, will mean the end
            if (lngPacketType == 0) {
                int iHeaderPacketSize = IO.readSInt32BE(is);
                if (iHeaderPacketSize != 0) {
                    if (blnThrowEx)
                        throw new BinaryDataNotRecognized("0 packet type with non zero header %08x", iHeaderPacketSize);
                    else
                        return null;
                }

                return new Header(0, 0);
            }

            if (lngPacketType != MAGIC_VLC0 &&
                lngPacketType != MAGIC_au00 &&
                lngPacketType != MAGIC_au01 &&
                lngPacketType != MAGIC_MDEC)
            {
                if (blnThrowEx)
                    throw new BinaryDataNotRecognized("Unknown packet type %08x", lngPacketType);
                else
                    return null;
            }

            int iHeaderPacketSize = IO.readSInt32BE(is);
            if (iHeaderPacketSize % 4 != 0) {
                if (blnThrowEx)
                    throw new BinaryDataNotRecognized("Invalid packet size " + iHeaderPacketSize);
                else
                    return null;
            }
            if (iHeaderPacketSize < MIN_PACKET_SIZE || iHeaderPacketSize > MAX_PACKET_SIZE) {
                if (blnThrowEx)
                    throw new BinaryDataNotRecognized("Invalid packet size " + iHeaderPacketSize);
                else
                    return null;
            }

            if (iHeaderPacketSize < _iMinPacketSize)
                _iMinPacketSize = iHeaderPacketSize;
            if (iHeaderPacketSize > _iMaxPacketSize)
                _iMaxPacketSize = iHeaderPacketSize;

            return new Header(lngPacketType, iHeaderPacketSize);
        }

        private final long _lngPacketType;    // @0 4 bytes (BE)
        private final int _iHeaderPacketSize; // @4 4 bytes BE

        private Header(long lngPacketType, int iHeaderPacketSize) {
            _lngPacketType = lngPacketType;
            _iHeaderPacketSize = iHeaderPacketSize;
        }

        public long getPacketType() {
            return _lngPacketType;
        }

        public int getPayloadSize() {
            return _iHeaderPacketSize - 8;
        }

        public boolean isEndPacket() {
            return _iHeaderPacketSize == 0;
        }

        public @Nonnull RoadRashPacket readPacket(@Nonnull InputStream is)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            if (_lngPacketType == MAGIC_au00 || _lngPacketType == MAGIC_au01) {
                return new AU(this, is);
            }
            if (_lngPacketType == MAGIC_MDEC) {
                return new MDEC(this, is);
            }
            if (_lngPacketType == MAGIC_VLC0) {
                return VLC0.read(this, is, true);
            }
            
            throw new BinaryDataNotRecognized("Trying to read not a packet " + this);
        }

        @Override
        public String toString() {
            return String.format("Header %08x size %d", _lngPacketType, _iHeaderPacketSize);
        }
    }

    // #########################################################################
    // #########################################################################

    @Nonnull
    private final Header _header;

    private RoadRashPacket(@Nonnull Header header) {
        _header = header;
    }

    public long getPacketType() {
        return _header._lngPacketType;
    }

    public int getPacketSizeInHeader() {
        return _header._iHeaderPacketSize;
    }

    // #########################################################################
    // #########################################################################

    public static @CheckForNull VLC0 readVlc0(@Nonnull InputStream is) throws EOFException, IOException {

        try {
            Header header = Header.read(is, false);
            if (header == null)
                return null;
            
            VLC0 vlc = VLC0.read(header, is, false);
            return vlc;
        } catch (BinaryDataNotRecognized ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    /**
     * "VLC" is known to mean "variable-length code/codes/coding".
     * Huffman coding is the kind of VLC logic used in PlayStation games.
     */
    public static class VLC0 extends RoadRashPacket {

        public static final int SIZEOF = Header.SIZEOF + ROAD_RASH_BIT_CODE_ORDER.length * 2;

        private static VLC0 read(@Nonnull Header header, @Nonnull InputStream is, boolean blnThrowEx)
                throws BinaryDataNotRecognized, EOFException, IOException
        {

            if (header._iHeaderPacketSize != (ROAD_RASH_BIT_CODE_ORDER.length * 2) + 8) {
                if (blnThrowEx)
                    throw new BinaryDataNotRecognized();
                else
                    return null;
            }

            byte[] abPacketPayload = IO.readByteArray(is, header._iHeaderPacketSize - 8);

            ZeroRunLengthAcLookup.Builder bldr = new ZeroRunLengthAcLookup.Builder();
            MdecCode[] aoVlcHeaderMdecCodesForReference = new MdecCode[ROAD_RASH_BIT_CODE_ORDER.length];

            for (int i = 0; i < ROAD_RASH_BIT_CODE_ORDER.length; i++) {
                int iMdec = IO.readUInt16LE(abPacketPayload, i * 2);
                MdecCode mdecCode = new MdecCode(iMdec);
                if (!mdecCode.isValid()) {
                    if (blnThrowEx)
                        throw new BinaryDataNotRecognized();
                    else
                        return null;
                }
                aoVlcHeaderMdecCodesForReference[i] = mdecCode;

                ZeroRunLengthAc bitCode = new ZeroRunLengthAc(ROAD_RASH_BIT_CODE_ORDER[i],
                        mdecCode.getTop6Bits(), mdecCode.getBottom10Bits(),
                        mdecCode.toMdecWord() == BitStreamUncompressorRoadRash.BITSTREAM_ESCAPE_CODE,
                        mdecCode.isEOD());

                bldr.add(bitCode);
            }

            return new VLC0(header, bldr.build(), aoVlcHeaderMdecCodesForReference);
        }

        @Nonnull
        private final ZeroRunLengthAcLookup _vlcLookup;
        @Nonnull
        private final MdecCode[] _aoVlcHeaderMdecCodesForReference;

        private VLC0(@Nonnull Header header, @Nonnull ZeroRunLengthAcLookup vlcLookup,
                     @Nonnull MdecCode[] aoVlcHeaderMdecCodesForReference)
        {
            super(header);
            _aoVlcHeaderMdecCodesForReference = aoVlcHeaderMdecCodesForReference;
            _vlcLookup = vlcLookup;
        }

        public @Nonnull BitStreamUncompressor makeFrameBitStreamUncompressor(@Nonnull RoadRashPacket.MDEC mdecPacket) {
            return new BitStreamUncompressorRoadRash(mdecPacket.getBitstream(), _vlcLookup, mdecPacket.getQuantizationScale());
        }

        /** For debugging. */
        public void printCodes() {
            for (int i = 0; i < _aoVlcHeaderMdecCodesForReference.length; i++) {
                MdecCode mdec = _aoVlcHeaderMdecCodesForReference[i];
                System.out.format("%04X %-8s %s", mdec.toMdecWord(), mdec, ROAD_RASH_BIT_CODE_ORDER[i]);
                if (mdec.toMdecWord() == BitStreamUncompressorRoadRash.BITSTREAM_ESCAPE_CODE)
                    System.out.println(" (escape code)");
                else
                    System.out.println();
            }
        }

        @Override
        public String toString() {
            return "VLC0";
        }
    }

    // #########################################################################
    // #########################################################################

    private static int _iMaxPresentationSampleFrame = 0;
    private static int _iMinAuPacketSize = Integer.MAX_VALUE;
    private static int _iMaxAuPacketSize = 0;

    public static class AU extends RoadRashPacket {

        private final int _iPresentationSampleFrame; // @8  4 bytes BE
        // 2048                                      // @12 2 bytes BE
        // 512                                       // @16 2 bytes BE

        @Nonnull
        private final byte[] _abCompressedSpu;

        public AU(@Nonnull Header header, @Nonnull InputStream is) 
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            super(header);

            if (header._iHeaderPacketSize < _iMinAuPacketSize)
                _iMinAuPacketSize = header._iHeaderPacketSize;
            if (header._iHeaderPacketSize > _iMaxAuPacketSize)
                _iMaxAuPacketSize = header._iHeaderPacketSize;

            _iPresentationSampleFrame = IO.readSInt32BE(is);
            if (_iPresentationSampleFrame < 0 || _iPresentationSampleFrame > MAX_PRESENTATION_SAMPLE_FRAME)
                throw new BinaryDataNotRecognized("Unexpected presentation sample frame " + _iPresentationSampleFrame);

            if (_iPresentationSampleFrame > _iMaxPresentationSampleFrame)
                _iMaxPresentationSampleFrame = _iPresentationSampleFrame;

            // I don't know what these values are, but they're always the same
            int i2048 = IO.readSInt16BE(is);
            if (i2048 != 2048)
                throw new BinaryDataNotRecognized("%d != 2048", i2048);
            int i512 = IO.readSInt16BE(is);
            if (i512 != 512)
                throw new BinaryDataNotRecognized("%d != 512", i512);

            _abCompressedSpu = IO.readByteArray(is, header._iHeaderPacketSize - 8 - 8);
        }

        public boolean isLastAudioPacket() {
            return getPacketType() == MAGIC_au01;
        }

        public int calcSampleFramesGenerated() {
            return getSpuSoundUnitPairCount() * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }

        public int getSpuSoundUnitPairCount() {
            int iSoundUnits = _abCompressedSpu.length / 15;
            return iSoundUnits / 2;
        }

        public @Nonnull DecodedAudioPacket decode(@Nonnull SpuAdpcmDecoder.Stereo decoder) {
            List<SpuAdpcmSoundUnit> units = unpackSpu(_abCompressedSpu);

            // each audio packet is split in half: half for left channel, half for right
            // I believe there can be a few 0 bytes padding the end to a multiple of 4
            int iHalf = units.size() / 2;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int iUnit = 0; iUnit < iHalf; iUnit++) {
                try {
                    decoder.decode(units.get(iUnit), units.get(iHalf + iUnit), out);
                } catch (IOException ex) {
                    throw new RuntimeException("Should not happen", ex);
                }
            }

            if (out.size() != calcSampleFramesGenerated() * 4)
                throw new RuntimeException();

            Fraction presentationSector = new Fraction(_iPresentationSampleFrame, SAMPLE_FRAMES_PER_SECTOR);

            return new DecodedAudioPacket(0, ROAD_RASH_AUDIO_FORMAT, presentationSector, out.toByteArray());
        }

        @Override
        public String toString() {
            return String.format("au0%c start sample frame:%d sample frames:%d",
                                 isLastAudioPacket() ? '1' : '0',
                                 _iPresentationSampleFrame, calcSampleFramesGenerated());
        }

    }

    // .........................................................................

    /** I guess to make the audio 1/16 smaller they chose to remove the
     * 2nd byte in each SPU sound unit, which is usually 0, but I found
     * it can also be filled with other values, but haven't found where
     * that logic lies. */
    private static @Nonnull List<SpuAdpcmSoundUnit> unpackSpu(@Nonnull byte[] abPayload) {
        List<SpuAdpcmSoundUnit> units = new ArrayList<SpuAdpcmSoundUnit>();

        int iBytesUsed;
        for (iBytesUsed = 0; iBytesUsed+14 < abPayload.length; iBytesUsed+=15) {
            byte[] abSpuSoundUnit = new byte[16];
            abSpuSoundUnit[0] = abPayload[iBytesUsed];
            // this byte can sometimes be values other than 0
            // I couldn't figure out how the game decides to make
            // those changes. Where would the data come from?
            // Maybe the leftover 2 bytes in some audio packets?
            // Maybe the timestamp?
            // More debugging is nedded
            abSpuSoundUnit[1] = 0;
            System.arraycopy(abPayload, iBytesUsed+1, abSpuSoundUnit, 2, 14);
            units.add(new SpuAdpcmSoundUnit(abSpuSoundUnit));
        }

        return units;
    }


    // #########################################################################
    // #########################################################################

    private static int _iMinMdecPacketSize = Integer.MAX_VALUE;
    private static int _iMaxMdecPacketSize = 0;

    private static int _iMinWidth = Integer.MAX_VALUE;
    private static int _iMaxWidth = 0;
    private static int _iMinHeight = Integer.MAX_VALUE;
    private static int _iMaxHeight = 0;
    private static int _iMaxFrameNumber = 0;

    public static class MDEC extends RoadRashPacket {

        private final int _iWidth;       // @8  2 bytes BE
        private final int _iHeight;      // @10 2 bytes BE
        private final int _iFrameNumber; // @12 4 bytes
        @Nonnull
        private final BitStreamUncompressor_STRv2.StrV2Header _strHeader;  // @16 8 bytes

        @Nonnull
        private final byte[] _abBitstream;

        public MDEC(@Nonnull Header header, @Nonnull InputStream is)
                throws EOFException, IOException, BinaryDataNotRecognized
        {
            super(header);

            if (header._iHeaderPacketSize < _iMinMdecPacketSize)
                _iMinMdecPacketSize = header._iHeaderPacketSize;
            if (header._iHeaderPacketSize > _iMaxMdecPacketSize)
                _iMaxMdecPacketSize = header._iHeaderPacketSize;

            _iWidth = IO.readSInt16BE(is);
            _iHeight = IO.readSInt16BE(is);

            if (_iWidth < MIN_FRAME_DIMENSIONS || _iWidth > MAX_FRAME_DIMENSIONS ||
                _iHeight < MIN_FRAME_DIMENSIONS || _iHeight > MAX_FRAME_DIMENSIONS)
            {
                throw new BinaryDataNotRecognized("Unexpected dimensions %dx%d", _iWidth, _iHeight);
            }

            if (_iWidth < _iMinWidth)
                _iMinWidth = _iWidth;
            if (_iWidth > _iMaxWidth)
                _iMaxWidth = _iWidth;
            if (_iHeight < _iMinHeight)
                _iMinHeight = _iHeight;
            if (_iHeight > _iMaxHeight)
                _iMaxHeight = _iHeight;

            _iFrameNumber = IO.readSInt32BE(is);
            if ((_iFrameNumber < 0 || _iFrameNumber > MAX_FRAME_NUMBER) && _iFrameNumber != 0x7fffad1c)
                throw new BinaryDataNotRecognized("Unexpected frame number " + _iFrameNumber);

            if (_iFrameNumber > _iMaxFrameNumber)
                _iMaxFrameNumber = _iFrameNumber;

            byte[] abStrHeader = IO.readByteArray(is, BitStreamUncompressor_STRv2.StrV2Header.SIZEOF);

            _strHeader = new BitStreamUncompressor_STRv2.StrV2Header(abStrHeader, abStrHeader.length);

            if (!_strHeader.isValid())
                throw new BinaryDataNotRecognized("Invalid STRv2 header");

            _abBitstream = IO.readByteArray(is, header._iHeaderPacketSize - 8 - 8 - abStrHeader.length);
        }

        public int getWidth() {
            return _iWidth;
        }

        public int getHeight() {
            return _iHeight;
        }

        public int getFrameNumber() {
            return _iFrameNumber;
        }

        public int getQuantizationScale() {
            return _strHeader.getQuantizationScale();
        }

        public byte[] getBitstream() {
            return _abBitstream;
        }

        @Override
        public String toString() {
            return String.format("MDEC %dx%d frame:%d qscale:%d",
                    _iWidth, _iHeight, _iFrameNumber, _strHeader.getQuantizationScale());
        }
    }

}
