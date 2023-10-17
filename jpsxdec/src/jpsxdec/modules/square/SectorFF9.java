/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.square;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;


/** Base class for Final Fantasy 9 movie (audio/video) sectors.
 * Chunk 0: left audio
 * Chunk 1: right audio
 * Chunk 2 to 9: video chunks
 * The video chunks are in reverse order. */
public abstract class SectorFF9 extends IdentifiedSector {

    private static final Logger LOG = Logger.getLogger(SectorFF9.class.getName());

    protected long _lngMagic;              //  0    [4 bytes]
    protected int  _iChunkNumber;          //  4    [2 bytes]
    protected int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected int  _iHeaderFrameNumber;    //  8    [4 bytes]

    /** Reads the header common to both audio and video sectors.
     *  @param iMagicNumber The magic number to check for since it is different
     *                      between audio and videos sectors.
     */
    public SectorFF9(@Nonnull CdSector cdSector, int iMagicNumber) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        _lngMagic = cdSector.readUInt32LE(0);
        // make sure the magic nubmer is correct
        if (_lngMagic != iMagicNumber) return;

        _iChunkNumber = cdSector.readSInt16LE(4);
        if (_iChunkNumber < 0) return;
        _iChunksInThisFrame = cdSector.readSInt16LE(6);
        if (_iChunksInThisFrame != 10) return;
        _iHeaderFrameNumber = cdSector.readSInt32LE(8);
        if (_iHeaderFrameNumber < 0) return;

        setProbability(100);
    }

    public int getActualChunkNumber() {
        return _iChunkNumber;
    }

    public int getActualChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }

    /**************************************************************************/
    /**************************************************************************/

    /** Final Fantasy 9 video chunk sector. */
    public static class SectorFF9Video
            extends SectorFF9
            implements ISelfDemuxingVideoSector,
                       SectorBasedFrameReplace.IReplaceableVideoSector
    {

        // .. Static stuff .....................................................

        public static final int VIDEO_CHUNK_MAGIC = 0x00040160;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;

        // .. Instance .........................................................

        /* In the parent
        protected long _lngMagic;              //  0    [4 bytes]
        protected long _iChunkNumber;          //  4    [2 bytes]
        protected long _iChunksInThisFrame;    //  6    [2 bytes]
        protected long _iHeaderFrameNumber;    //  8    [4 bytes]
        */
        private long _lngUsedDemuxedSize;    //  12   [4 bytes]
        private int  _iWidth;                //  16   [2 bytes]
        private int  _iHeight;               //  18   [2 bytes]
        private int  _iRunLengthCodeCount;   //  20   [2 bytes]
        private int  _iMagic3800;            //  22   [2 bytes] always 0x3800
        private int  _iQuantizationScale;    //  24   [2 bytes]
        private int  _iVersion;              //  26   [2 bytes]
        private long _lngFourBytes;          //  28   [4 bytes] usually zero
        //   32 TOTAL

        @Override
        public int getVideoSectorHeaderSize() { return 32; }

        public SectorFF9Video(@Nonnull CdSector cdSector) {
            // Read the shared header, requesting video magic number
            super(cdSector, VIDEO_CHUNK_MAGIC);
            if (isSuperInvalidElseReset()) return;

            // Without a sector header, we can't read FF9 video frames
            CdSectorXaSubHeader sh = cdSector.getSubHeader();
            if (sh == null) return;
            if (sh.getSubMode().mask(SubMode.MASK_DATA | SubMode.MASK_FORM) !=
                                    (SubMode.MASK_DATA | SubMode.MASK_FORM))
                return;

            _lngUsedDemuxedSize = cdSector.readUInt32LE(12);
            if (_lngUsedDemuxedSize == -1) // check for overflow
                return;

            _iWidth = cdSector.readSInt16LE(16);
            if (_iWidth < 1) return;
            _iHeight = cdSector.readSInt16LE(18);
            if (_iHeight < 1) return;
            _iRunLengthCodeCount = cdSector.readUInt16LE(20);

            _iMagic3800 = cdSector.readUInt16LE(22);
            if (_iMagic3800 != 0x3800) return;

            _iQuantizationScale = cdSector.readSInt16LE(24);
            if (_iQuantizationScale < 0) return;
            _iVersion = cdSector.readUInt16LE(26);
            if (_iVersion != 2) return;
            _lngFourBytes = cdSector.readUInt32LE(28); // usually zero

            setProbability(100);
        }

        // .. Public functions .................................................

        @Override
        public String toString() {
            return String.format(
                "%s %s frame:%d chunk:%d/%d %dx%d ver:%d " +
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
                getTypeName(),
                super.toString(),
                _iHeaderFrameNumber,
                _iChunkNumber,
                _iChunksInThisFrame,
                _iWidth,
                _iHeight,
                _iVersion,
                _lngUsedDemuxedSize,
                _iRunLengthCodeCount,
                _iMagic3800,
                _iQuantizationScale,
                _lngFourBytes
                );
        }

        public int getInvertedVideoChunkNumber() {
            return super._iChunkNumber - 2;
        }
        public int getVideoChunksInFrame() {
            return super._iChunksInThisFrame - 2; // i.e. return 8
        }

        public int getWidth() {
            return _iWidth;
        }

        public int getHeight() {
            return _iHeight;
        }

        @Override
        public @Nonnull FF9Demuxer createDemuxer(@Nonnull ILocalizedLogger log) {
            return new FF9Demuxer(this, log);
        }

        @Override
        public int getDemuxPieceSize() {
            return getCdSector().getCdUserDataSize() - getVideoSectorHeaderSize();
        }

        @Override
        public byte getDemuxPieceByte(int i) {
            return getCdSector().readUserDataByte(getVideoSectorHeaderSize() + i);
        }

        @Override
        public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
            getCdSector().getCdUserDataCopy(getVideoSectorHeaderSize(), abOut,
                    iOutPos, getDemuxPieceSize());
        }

        @Override
        public @Nonnull String getTypeName() {
            return "FF9Video";
        }

        @Override
        public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                             @Nonnull BitStreamAnalysis newFrame,
                                             @Nonnull byte[] abCurrentVidSectorHeader)
                throws LocalizedIncompatibleException
        {
            if (!newFrame.isBitStreamClass(BitStreamUncompressor_STRv2.class))
                throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_V2());

            int iQscale = newFrame.getFrameQuantizationScale();

            IO.writeInt32LE(abCurrentVidSectorHeader, 12, newFrame.calculateUsedBytesRoundUp4() / 4);
            IO.writeInt16LE(abCurrentVidSectorHeader, 20, newFrame.calculateMdecHalfCeiling32());
            IO.writeInt16LE(abCurrentVidSectorHeader, 24, (short)iQscale);
        }

    }

    /**************************************************************************/
    /**************************************************************************/

    /** Final Fantasy 9 audio sector. */
    public static class SectorFF9Audio
            extends SectorFF9
            implements ISquareAudioSector
    {

        // .. Static stuff .....................................................

        public static final int FF9_AUDIO_CHUNK_MAGIC = 0x00080160;
        public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

        // .. Instance .........................................................

        /* In the parent
        protected long _lngMagic;              //  0    [4 bytes]
        protected long _iChunkNumber;          //  4    [2 bytes]
        protected long _iChunksInThisFrame;    //  6    [2 bytes]
        protected long _iHeaderFrameNumber;    //  8    [4 bytes]
        */
        // 116 bytes unknown
        private SquareAKAOstruct _akaoStruct;     // [80 bytes]
        //   208 TOTAL

        private int _iAudioDataSize;
        private int _iSampleFramesPerSecond;

        public SectorFF9Audio(@Nonnull CdSector cdSector) {
            super(cdSector, FF9_AUDIO_CHUNK_MAGIC);
            if (isSuperInvalidElseReset()) return;

            if (_iChunkNumber >= 2)
                return;

            _akaoStruct = new SquareAKAOstruct(cdSector, 128);

            if (_akaoStruct.AKAO != SquareAKAOstruct.AKAO_ID ||
                _akaoStruct.BytesOfData == 0)
            {
                // this means there's no audio data in this sector
                // so technically not an audio sector, but we'll just
                // make it a kinda null audio sector
                _iAudioDataSize = 0;
                _iSampleFramesPerSecond = 0;
                setProbability(100);
                return;
            }

            if (_akaoStruct.BytesOfData > CdSector.SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1 - FRAME_AUDIO_CHUNK_HEADER_SIZE)
                return;
            _iAudioDataSize = (int) _akaoStruct.BytesOfData;

            switch (_iAudioDataSize) {
                case 1824: case 1840:
                    _iSampleFramesPerSecond = 48000;
                    break;
                case 1680:
                    _iSampleFramesPerSecond = 44100;
                    break;
                default:
                    LOG.log(Level.WARNING, "Unexpected audio data size {0} for possible FF9 sector at {1}",
                                           new Object[]{_iAudioDataSize, cdSector});
                    return;
            }

            setProbability(100);
        }

        @Override
        public @Nonnull String getTypeName() {
            return "FF9Audio";
        }

        @Override
        public int getAudioDataStartOffset() {
            return FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }

        @Override
        public int getAudioDataSize() {
            return _iAudioDataSize;
        }

        @Override
        public int getSoundUnitCount() {
            return getAudioDataSize() / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT;
        }

        @Override
        public int getSampleFramesPerSecond() {
            return _iSampleFramesPerSecond;
        }

        @Override
        public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(getCdSector().getCdUserDataStream(),
                    getAudioDataStartOffset(), getAudioDataSize());
        }

        @Override
        public boolean isLeftChannel() {
            switch (_iChunkNumber) {
                case 0: return true;
                case 1: return false;
                default: throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return String.format("%s %s frame:%d chunk:%d/%d %s",
                getTypeName(),
                super.toString(),
                _iHeaderFrameNumber,
                _iChunkNumber,
                _iChunksInThisFrame,
                _akaoStruct);
        }

    }
}
