/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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
import jpsxdec.audio.SpuAdpcmDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.ByteArrayFPIS;


/** Base class for Final Fantasy 8 movie (audio/video) sectors. */
public abstract class SectorFF8 extends IdentifiedSector {

    public static final int SHARED_HEADER_SIZE = 8;

    protected final char _achHead[] = new char[4]; // [4 bytes] "SM_\1"
    protected int        _iSectorNumber;           // [1 byte]
    protected int        _iSectorsInAVFrame;       // [1 byte]
    protected int        _iFrameNumber;            // [2 bytes]
    
    public SectorFF8(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;
        
        // both audio and video sectors are flagged as data
        if (cdSector.hasSubHeader() && !cdSector.getSubMode().getData())
            return;
        
        char c;
        c = (char)cdSector.readUserDataByte(0);
        if ((_achHead[0] = c) != 'S') return;
        c = (char)cdSector.readUserDataByte(1);
        if ((_achHead[1] = c) != 'M') return;
        c = (char)cdSector.readUserDataByte(2);
        _achHead[2] = c; // 'J' for video, 'N' left audio and 'R' for right audio
        c = (char)cdSector.readUserDataByte(3);
        if ((_achHead[3] = c) != '\1') return;
        // There appear to be 10 sectors for every frame
        // First two sectors are audio (left, right channels)
        // Then the remaining 8 sectors is the video frame, always
        // 320x224.
        _iSectorNumber = cdSector.readUserDataByte(4); // 0 to _iSectorsInAVFrame
        _iSectorsInAVFrame = cdSector.readUserDataByte(5); // either 9 or 1
        //if (_iSectorsInAVFrame != 9) throw new NotThisTypeException();
        _iFrameNumber = cdSector.readSInt16LE(6); // starts @ 0
        if (_iFrameNumber < 0) return;

        setProbability(100);
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }

    public int getFF8ChunkNumber() {
        return _iSectorNumber;
    }

    public int getFF8ChunksInFrame() {
        return _iSectorsInAVFrame+1;
    }

    @Override
    public String toString() {
        return super.toString() +
                String.format(" frame:%d chunk:%d/%d",
                    _iFrameNumber,
                    _iSectorNumber,
                    _iSectorsInAVFrame);
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /** Final Fantasy 8 video chunk sector. */
    public static class SectorFF8Video extends SectorFF8
            implements IVideoSectorWithFrameNumber
    {

        public SectorFF8Video(@Nonnull CdSector cdSector) {
            super(cdSector);
            if (isSuperInvalidElseReset()) return;

            if (super._achHead[2] != 'J') return;
            setProbability(100);
        }

        /** [implements IVideoSector] */
        public int getWidth() {
            return 320;
        }

        /** [implements IVideoSector] */
        public int getHeight() {
            return 224;
        }

        /** [implements IVideoSector] */
        public int getChunkNumber() {
            return super._iSectorNumber - 2;
        }
        /** [implements IVideoSector] */
        public int getChunksInFrame() {
            return super._iSectorsInAVFrame - 1; // i.e. return 8
        }

        public int getIdentifiedUserDataSize() {
            return super.getCdSector().getCdUserDataSize() -
                   SectorFF8.SHARED_HEADER_SIZE;
        }

        public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                    SHARED_HEADER_SIZE, getIdentifiedUserDataSize());
        }

        public void copyIdentifiedUserData(@Nonnull byte[] abOut, int iOutPos) {
            super.getCdSector().getCdUserDataCopy(SHARED_HEADER_SIZE, abOut,
                    iOutPos, getIdentifiedUserDataSize());
        }

        public String toString() {
            return String.format("VideoFF8 %s 320x224", super.toString());
        }

        public @Nonnull String getTypeName() {
            return "FF8Video";
        }

        public int checkAndPrepBitstreamForReplace(@Nonnull byte[] abDemuxData, int iUsedSize,
                                                   int iMdecCodeCount, @Nonnull byte[] abSectUserData)
        {
            // none of the FF8 video sector headers need to be modified
            // so just return the size of the header so the caller
            // can replace the frame data
            return SectorFF8.SHARED_HEADER_SIZE;
        }

    }

    /** Final Fantasy 8 audio sector. */
    public static class SectorFF8Audio
            extends SectorFF8
            implements ISquareAudioSector
    {

        private static final int FF8_AUDIO_SECTOR_BYTE_DATA_SIZE = 1680;
        
        /* // shared header
        protected final char _achHead[] = new char[4]; // [4 bytes] "SM_\1"
        protected int        _iSectorNumber;           // [1 byte]
        protected int        _iSectorsInAVFrame;       // [1 byte]
        protected long       _iFrameNumber;            // [2 bytes]
        */
        
        public final static int AUDIO_ADDITIONAL_HEADER_SIZE = 360;

        // 232 bytes; unknown
        private String _sSHUN_MORIYA;
        // 10 bytes; unknown
        private SquareAKAOstruct _AKAOstruct;
        // 76 bytes; unknown
        
        public SectorFF8Audio(@Nonnull CdSector cdSector) {
            super(cdSector);
            if (isSuperInvalidElseReset()) return;
            
            if (super._achHead[2] != 'N' && super._achHead[2] != 'R')
                return;

            StringBuilder sb = new StringBuilder(16*2);
            boolean blnOnly0xff = true;
            for (int i = 0; i < 16; i++) {
                byte b = cdSector.readUserDataByte(240 + i);
                if (b == -1)
                    sb.append("ff");
                else {
                    sb.append((char)b);
                    blnOnly0xff = false;
                }
            }

            _sSHUN_MORIYA = sb.toString();
            if (!(
                  blnOnly0xff || // demo disc has all 0xff for MORIYA field
                  _sSHUN_MORIYA.startsWith("MORIYA") || 
                  _sSHUN_MORIYA.startsWith("SHUN.MORIYA")
                ))
            {
                return;
            }

            _AKAOstruct = new SquareAKAOstruct(cdSector, 256);

            if (!(_AKAOstruct.AKAO == SquareAKAOstruct.AKAO_ID ||
                  _AKAOstruct.AKAO == 0xffffffffL)) // demo disc has all 0xff for AKAO struct
                return;

            if (!(_AKAOstruct.BytesOfData == FF8_AUDIO_SECTOR_BYTE_DATA_SIZE ||
                  _AKAOstruct.BytesOfData == 0xffffffffL)) // demo disc has all 0xff for AKAO struct
                return;

            // TODO: check Sound Parameters values that the parameter index is valid

            setProbability(100);
        }

        public int getSamplesPerSecond() {
            return 44100;
        }

        public int getIdentifiedUserDataSize() {
            return FF8_AUDIO_SECTOR_BYTE_DATA_SIZE;
        }

        public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                    SectorFF8.SHARED_HEADER_SIZE + AUDIO_ADDITIONAL_HEADER_SIZE,
                    getIdentifiedUserDataSize());
        }

        public String toString() {
            return String.format("AudioFF8 %s MORIYA:%s %s", 
                   super.toString(), _sSHUN_MORIYA, _AKAOstruct);
        }

        public int getAudioDataSize() {
            return FF8_AUDIO_SECTOR_BYTE_DATA_SIZE;
        }

        public int getAudioChunkNumber() {
            return super._iSectorNumber;
        }

        public int getAudioChunksInFrame() {
            return 2;
        }

        public @Nonnull String getTypeName() {
            return "FF8Audio";
        }

        public boolean isStereo() {
            return true;
        }

        public int getAudioChannel() {
            return getAudioChunkNumber();
        }

        public long getLeftSampleCount() {
            if (getAudioChunkNumber() == 0)
                return SpuAdpcmDecoder.calculatePcmSampleFramesGenerated(getAudioDataSize());
            else
                return 0;
        }

        public long getRightSampleCount() {
            if (getAudioChunkNumber() == 1)
                return SpuAdpcmDecoder.calculatePcmSampleFramesGenerated(getAudioDataSize());
            else
                return 0;
        }

         public boolean matchesPrevious(@Nonnull ISquareAudioSector prevSect) {
            if (!(prevSect instanceof SectorFF8Audio))
                return false;

            if (prevSect.getAudioChunkNumber() == 0) {
                if (getAudioChunkNumber() != 1 ||
                    prevSect.getFrameNumber() != getFrameNumber() ||
                    prevSect.getSectorNumber() + 1 != getSectorNumber())
                    return false;
            } else if (prevSect.getAudioChunkNumber() == 1) {
                if (getAudioChunkNumber() != 0 ||
                    prevSect.getFrameNumber() + 1 != getFrameNumber() ||
                    // the audio only movie
                    (prevSect.getSectorNumber() + 1 != getSectorNumber() &&
                     prevSect.getSectorNumber() + 9 != getSectorNumber()))
                    return false;
            }

            return true;
        }

    }
    
}