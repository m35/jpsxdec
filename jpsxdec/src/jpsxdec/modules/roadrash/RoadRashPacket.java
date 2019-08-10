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
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;

/**
 * Support for "Road Rash 3D" videos.
 * Road Rash takes a unique approach to bitstreams, every movie has its own unique VLC table.
 * There's a lot of big-endian data in here.
 */
public abstract class RoadRashPacket {

    public static final int MIN_PACKET_SIZE = 456;
    public static final int MAX_PACKET_SIZE = 14200;

    public static final long MAGIC_VLC0 = 0x564c4330;
    public static final long MAGIC_MDEC = 0x4d444543;
    public static final long MAGIC_au00 = 0x61753030;
    public static final long MAGIC_au01 = 0x61753031;

    public static final int SAMPLE_FRAMES_PER_SECOND = 22050;
    public static final int SAMPLE_FRAMES_PER_SECTOR = SAMPLE_FRAMES_PER_SECOND / 150;
    static {
        if (SAMPLE_FRAMES_PER_SECOND % 150 != 0)
            throw new RuntimeException("Road Rash sample rate doesn't cleanly divide by sector rate");
    }

    public static final AudioFormat ROAD_RASH_AUDIO_FORMAT = new AudioFormat(SAMPLE_FRAMES_PER_SECOND, 16, 2, true, false);

    // =========================================================================

    public static @CheckForNull RoadRashPacket readPacket(@Nonnull InputStream is)
            throws EOFException, IOException
    {

        long lngMagic = IO.readUInt32BE(is);
        if (lngMagic != MAGIC_VLC0 &&
            lngMagic != MAGIC_au00 &&
            lngMagic != MAGIC_au01 &&
            lngMagic != MAGIC_MDEC)
        {
            return null;
        }

        int iPacketSize = IO.readSInt32BE(is);
        if (iPacketSize % 4 != 0)
            throw new RuntimeException();
        if (iPacketSize < MIN_PACKET_SIZE || iPacketSize > MAX_PACKET_SIZE)
            throw new RuntimeException();
        if (lngMagic == MAGIC_au00 || lngMagic == MAGIC_au01) {
            return AU.readPacket(lngMagic, iPacketSize, is);
        }
        if (lngMagic == MAGIC_MDEC) {
            return MDEC.readPacket(lngMagic, iPacketSize, is);
        }
        if (lngMagic == MAGIC_VLC0) {
            return VLC0.readPacket(lngMagic, iPacketSize, is);
        }

        throw new RuntimeException("Should have been handled before this");
    }

    // =========================================================================

    public static final int HEADER_SIZEOF = 8;

    private final long _lngPacketType;    // @0 4 bytes (BE)
    private final int _iHeaderPacketSize; // @4 4 bytes BE

    /** Only payload data that is actual payload,
     * not including any additional payload sub-headers */
    @Nonnull
    private final byte[] _abPayloadAfterHeaders;

    public RoadRashPacket(long lngPacketType, int iHeaderPacketSize,
                          @Nonnull byte[] abPayloadAfterHeaders)
    {
        _lngPacketType = lngPacketType;
        _iHeaderPacketSize = iHeaderPacketSize;
        _abPayloadAfterHeaders = abPayloadAfterHeaders;
    }

    public long getPacketType() {
        return _lngPacketType;
    }

    public int getHeaderPacketSize() {
        return _iHeaderPacketSize;
    }

    protected @Nonnull byte[] getPayload() {
        return _abPayloadAfterHeaders;
    }

    // =========================================================================

    /**
     * "VLC" is known to mean "variable-length code/codes/coding".
     * Huffman coding is the kind of VLC logic used in PlayStation games.
     */
    public static class VLC0 extends RoadRashPacket {

        public static @CheckForNull VLC0 readPacket(long lngMagic, int iPacketSize, @Nonnull InputStream is)
                throws EOFException, IOException
        {
            if (iPacketSize != (ROAD_RASH_BIT_CODE_ORDER.length * 2) + 8)
                throw new RuntimeException();
            byte[] abPacketPayload = IO.readByteArray(is, iPacketSize - 8);

            ZeroRunLengthAcLookup.Builder bldr = new ZeroRunLengthAcLookup.Builder();
            MdecCode[] aoVlcHeaderMdecCodesForReference = new MdecCode[ROAD_RASH_BIT_CODE_ORDER.length];

            for (int i = 0; i < ROAD_RASH_BIT_CODE_ORDER.length; i++) {
                int iMdec = IO.readUInt16LE(abPacketPayload, i * 2);
                MdecCode mdecCode = new MdecCode(iMdec);
                if (!mdecCode.isValid())
                    throw new RuntimeException();
                aoVlcHeaderMdecCodesForReference[i] = mdecCode;

                ZeroRunLengthAc bitCode = new ZeroRunLengthAc(ROAD_RASH_BIT_CODE_ORDER[i],
                        mdecCode.getTop6Bits(), mdecCode.getBottom10Bits(),
                        mdecCode.toMdecWord() == BitStreamUncompressorRoadRash.BITSTREAM_ESCAPE_CODE,
                        mdecCode.isEOD());

                bldr.add(bitCode);
            }

            return new VLC0(lngMagic, iPacketSize, abPacketPayload, bldr.build(), aoVlcHeaderMdecCodesForReference);
        }

        @Nonnull
        private final ZeroRunLengthAcLookup _vlcLookup;
        @Nonnull
        private final MdecCode[] _aoVlcHeaderMdecCodesForReference;

        public VLC0(long lngPacketType, int iHeaderPacketSize,
                                  @Nonnull byte[] abPayloadAfterHeaders,
                                  @Nonnull ZeroRunLengthAcLookup vlcLookup,
                                  @Nonnull MdecCode[] aoVlcHeaderMdecCodesForReference)
        {
            super(lngPacketType, iHeaderPacketSize, abPayloadAfterHeaders);
            _vlcLookup = vlcLookup;
            _aoVlcHeaderMdecCodesForReference = aoVlcHeaderMdecCodesForReference;
        }

        public @Nonnull BitStreamUncompressor makeFrameBitStreamUncompressor(@Nonnull MDEC mdecPacket) {
            return new BitStreamUncompressorRoadRash(mdecPacket.getPayload(), _vlcLookup, mdecPacket.getQuantizationScale());
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


    // =========================================================================

    public static class AU extends RoadRashPacket {
        
        public static @CheckForNull AU readPacket(
                long lngMagic, int iPacketSize, @Nonnull InputStream is)
                throws EOFException, IOException
        {
            if (iPacketSize < 1576 || iPacketSize > 6348)
                return null;

            int iPresentationSampleFrame = IO.readSInt32BE(is);
            if (iPresentationSampleFrame < 0 || iPresentationSampleFrame > 1312556)
                throw new RuntimeException();

            // I don't know what these values are, but they're always the same
            int i2048 = IO.readSInt16BE(is);
            if (i2048 != 2048)
                throw new RuntimeException();
            int i512 = IO.readSInt16BE(is);
            if (i512 != 512)
                throw new RuntimeException();

            byte[] abPacketPayload = IO.readByteArray(is, iPacketSize - 8 - 8);
            return new AU(lngMagic, iPacketSize, abPacketPayload, iPresentationSampleFrame);
        }
        
        private final int _iPresentationSampleFrame; // @8  4 bytes BE
        // 2048                                      // @12 2 bytes BE
        // 512                                       // @16 2 bytes BE

        public AU(long lngPacketType, int iHeaderPacketSize,
                  @Nonnull byte[] abPayloadAfterHeaders, int iPresentationSampleFrame)
        {
            super(lngPacketType, iHeaderPacketSize, abPayloadAfterHeaders);
            _iPresentationSampleFrame = iPresentationSampleFrame;
        }

        public boolean isLastAudioPacket() {
            return getPacketType() == MAGIC_au01;
        }

        public int calcSampleFramesGenerated() {
            return getSpuSoundUnitPairCount() * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }

        public int getSpuSoundUnitPairCount() {
            int iSoundUnits = getPayload().length / 15;
            return iSoundUnits / 2;
        }

        public @Nonnull DecodedAudioPacket decode(@Nonnull SpuAdpcmDecoder.Stereo decoder) {
            List<SpuAdpcmSoundUnit> units = unpackSpu(getPayload());

            // each audio packet is split in half: half for left channel, half for right
            int iHalf = units.size() / 2;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int iUnit = 0; iUnit < iHalf; iUnit++) {
                try {
                    decoder.decode(units.get(iUnit), units.get(iHalf + iUnit), out);
                } catch (IOException ex) {
                    throw new RuntimeException("Should nothappen", ex);
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

    // =========================================================================

    public static class MDEC extends RoadRashPacket {

        public static @CheckForNull MDEC readPacket(
                long lngMagic, int iPacketSize, @Nonnull InputStream is)
                throws EOFException, IOException
        {
            if (iPacketSize < 688 || iPacketSize > 14200)
                throw new RuntimeException();

            int iWidth = IO.readSInt16BE(is);
            int iHeight = IO.readSInt16BE(is);

            if ((iWidth != 144 || iHeight != 112) &&
                (iWidth != 208 || iHeight != 144) &&
                (iWidth != 320 || iHeight != 144) &&
                (iWidth != 320 || iHeight != 224))
            {
                throw new RuntimeException();
            }

            int iFrameNumber = IO.readSInt32BE(is);
            if ((iFrameNumber < 0 || iFrameNumber > 893) && iFrameNumber != 0x7fffad1c)
                throw new RuntimeException();

            byte[] abStrHeader = IO.readByteArray(is, BitStreamUncompressor_STRv2.StrV2Header.SIZEOF);

            BitStreamUncompressor_STRv2.StrV2Header strHeader = 
                    new BitStreamUncompressor_STRv2.StrV2Header(abStrHeader, abStrHeader.length);

            if (!strHeader.isValid())
                throw new RuntimeException();
            if (strHeader.getQuantizationScale() < 1 || strHeader.getQuantizationScale() > 17)
                throw new RuntimeException();

            byte[] abPacketPayload = IO.readByteArray(is, iPacketSize - 8 - 8 - abStrHeader.length);
            return new MDEC(lngMagic, iPacketSize, abPacketPayload, iWidth, iHeight, iFrameNumber, strHeader);
        }

        private final int _iWidth;       // @8  2 bytes BE
        private final int _iHeight;      // @10 2 bytes BE
        private final int _iFrameNumber; // @12 4 bytes
        @Nonnull
        private final BitStreamUncompressor_STRv2.StrV2Header _strHeader;  // @16 8 bytes

        public MDEC(long lngPacketType, int iHeaderPacketSize,
                    @Nonnull byte[] abPayloadAfterHeaders,
                    int iWidth, int iHeight, int iFrameNumber,
                    @Nonnull BitStreamUncompressor_STRv2.StrV2Header strHeader)
        {
            super(lngPacketType, iHeaderPacketSize, abPayloadAfterHeaders);
            _iWidth = iWidth;
            _iHeight = iHeight;
            _iFrameNumber = iFrameNumber;
            _strHeader = strHeader;
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

        @Override
        public String toString() {
            return String.format("MDEC %dx%d frame:%d qscale:%d",
                    _iWidth, _iHeight, _iFrameNumber, _strHeader.getQuantizationScale());
        }
    }

}
