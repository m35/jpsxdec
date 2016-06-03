/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

package jpsxdec.sectors;

import javax.annotation.Nonnull;
import jpsxdec.audio.SquareAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;


/** Base class for Final Fantasy 9 movie (audio/video) sectors. */
public abstract class SectorFF9 extends IdentifiedSector {
        
    protected long _lngMagic;              //  0    [4 bytes]
    protected int  _iChunkNumber;          //  4    [2 bytes]
    protected int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected int  _iFrameNumber;          //  8    [4 bytes]

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
        _iFrameNumber = cdSector.readSInt32LE(8);
        if (_iFrameNumber < 0) return;

        setProbability(100);
    }

    public long getActualChunkNumber() {
        return _iChunkNumber;
    }

    public long getActualChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }

    /**************************************************************************/
    /**************************************************************************/

    /** Final Fantasy 9 video chunk sector. */
    public static class SectorFF9Video
            extends SectorFF9
            implements IVideoSectorWithFrameNumber
    {

        // .. Static stuff .....................................................

        public static final int VIDEO_CHUNK_MAGIC = 0x00040160;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;

        // .. Instance .........................................................

        /* In the parent
        protected long _lngMagic;              //  0    [4 bytes]
        protected long _iChunkNumber;          //  4    [2 bytes]
        protected long _iChunksInThisFrame;    //  6    [2 bytes]
        protected long _iFrameNumber;          //  8    [4 bytes]
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

        public SectorFF9Video(@Nonnull CdSector cdSector) {
            // Read the shared header, requesting video magic number
            super(cdSector, VIDEO_CHUNK_MAGIC);
            if (isSuperInvalidElseReset()) return;

            // Without a sector header, we can't read FF9 video frames
            if (!cdSector.hasSubHeader()) return;
            if (cdSector.subModeMask(SubMode.MASK_DATA | SubMode.MASK_FORM) !=
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

        public String toString() {
            return "FF9Vid " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %dx%d ver:%d " + 
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
                _iFrameNumber,
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

        /** Returns the chunk number in order of how they should be demuxed
         * (i.e. 8, 7, 6, ..., 1, 0). [implements IVideoSector] */
        public int getChunkNumber() {
            return 9 - super._iChunkNumber;
        }
        /** [implements IVideoSector] */
        public int getChunksInFrame() {
            return super._iChunksInThisFrame - 2; // i.e. return 8
        }

        /** [implements IVideoSector] */
        public int getWidth() {
            return _iWidth;
        }

        /** [implements IVideoSector] */
        public int getHeight() {
            return _iHeight;
        }

        public int getIdentifiedUserDataSize() {
            return super.getCdSector().getCdUserDataSize() -
                    FRAME_CHUNK_HEADER_SIZE;
        }

        public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                    FRAME_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
        }

        public void copyIdentifiedUserData(@Nonnull byte[] abOut, int iOutPos) {
            super.getCdSector().getCdUserDataCopy(FRAME_CHUNK_HEADER_SIZE, abOut,
                    iOutPos, getIdentifiedUserDataSize());
        }
        
        public @Nonnull String getTypeName() {
            return "FF9Video";
        }

        public int checkAndPrepBitstreamForReplace(@Nonnull byte[] abDemuxData, int iUsedSize,
                                                   int iMdecCodeCount, @Nonnull byte[] abSectUserData)
                throws IncompatibleException
        {
            final int iQscale;
            try {
                iQscale = BitStreamUncompressor_STRv2.getQscale(abDemuxData);
            } catch (MdecException.Uncompress ex) {
                throw new IncompatibleException(I.REPLACE_FRAME_TYPE_NOT_V2(), ex);
            }

            int iDemuxSizeForHeader = (iUsedSize + 3) & ~3;

            IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader / 4);
            IO.writeInt16LE(abSectUserData, 20,
                    BitStreamUncompressor_STRv2.calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(abSectUserData, 24, (short)(iQscale));

            return 32;
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
        protected long _iFrameNumber;          //  8    [4 bytes]
        */
        // 116 bytes unknown
        private SquareAKAOstruct _akaoStruct;     // [80 bytes]
        //   208 TOTAL

        public SectorFF9Audio(@Nonnull CdSector cdSector) {
            super(cdSector, FF9_AUDIO_CHUNK_MAGIC);
            if (isSuperInvalidElseReset()) return;
            
            _akaoStruct = new SquareAKAOstruct(cdSector, 128);

            setProbability(100);
        }

        // .. Public functions .................................................

        public String toString() {
            return "FF9Aud " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %s",
                _iFrameNumber,
                _iChunkNumber,
                _iChunksInThisFrame,
                _akaoStruct
                );
        }

        public int getAudioDataSize() {
            // using the AKAO tag is more reliable than other methods to
            // determine if there is any audio data
            if (_akaoStruct.AKAO == SquareAKAOstruct.AKAO_ID)
                return (int)_akaoStruct.BytesOfData;
            else
                return 0;
        }

        public int getSamplesPerSecond() {
            int iAudData = getAudioDataSize();
            if (iAudData == 1824 || iAudData == 1840)
                return 48000;
            else if (iAudData == 1680)
                return 44100;
            else if (iAudData == 0)
                return 0;
            else
                throw new RuntimeException(
                    "What kind of strange samples/second is this sector?");
        }

        public int getIdentifiedUserDataSize() {
            return super.getCdSector().getCdUserDataSize() -
                    FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }

        public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                    FRAME_AUDIO_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
        }

        public int getAudioChunkNumber() {
            return super._iChunkNumber;
        }

        public int getAudioChannel() {
            return (int)getAudioChunkNumber();
        }

        public int getAudioChunksInFrame() {
            return 2; // there are always only 2
        }
        
        public @Nonnull String getTypeName() {
            return "FF9Audio";
        }

        public boolean isStereo() {
            return true;
        }

        public long getLeftSampleCount() {
            // if it's the 0th chunk, then it holds the left audio
            if (getAudioChunkNumber() == 0) 
                return SquareAdpcmDecoder.calculateSamplesGenerated(getAudioDataSize());
            else
                return 0;
        }

        public long getRightSampleCount() {
            // if it's the 1st chunk, then it holds the right audio
            if (getAudioChunkNumber() == 1) 
                return SquareAdpcmDecoder.calculateSamplesGenerated(getAudioDataSize());
            else
                return 0;
        }

        public boolean matchesPrevious(@Nonnull ISquareAudioSector prevSect) {
            if (!(prevSect instanceof SectorFF9Audio))
                return false;

            SectorFF9Audio prevFF9Aud = (SectorFF9Audio) prevSect;

            if (getSamplesPerSecond() != prevFF9Aud.getSamplesPerSecond())
                return false;

            if (prevSect.getAudioChunkNumber() == 0) {
                if (getAudioChunkNumber() != 1 ||
                    prevSect.getFrameNumber() != getFrameNumber() ||
                    prevSect.getSectorNumber() + 1 != getSectorNumber())
                    return false;
            } else if (prevSect.getAudioChunkNumber() == 1) {
                if (getAudioChunkNumber() != 0 ||
                    prevSect.getFrameNumber() + 1 != getFrameNumber() ||
                    prevSect.getSectorNumber() + 9 != getSectorNumber())
                    return false;
            }

            return true;
        }

     }
}
