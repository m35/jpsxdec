/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import jpsxdec.cdreaders.CdFileSectorReader;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.audio.SquareADPCMDecoder;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Base class for Final Fantasy 8 movie (audio/video) sectors. */
public abstract class SectorFF8 extends IdentifiedSector {

    public static final int SHARED_HEADER_SIZE = 8;

    protected final char _achHead[] = new char[4]; // [4 bytes] "SM_\1"
    protected int        _iSectorNumber;           // [1 byte]
    protected int        _iSectorsInAVFrame;       // [1 byte]
    protected int        _iFrameNumber;            // [2 bytes]
    
    public SectorFF8(CdSector oCDSect) throws NotThisTypeException {
        super(oCDSect);
    }

    protected InputStream ReadHeader(CdSector oCDSect)
            throws IOException, NotThisTypeException 
    {
        // both audio and video sectors are flagged as data
        if (oCDSect.hasRawSectorHeader() && !oCDSect.getSubMode().getData())
            throw new NotThisTypeException();
        
        InputStream oIS = oCDSect.getCdUserDataStream();
        
        char c;
        c = (char)oIS.read();
        if ((_achHead[0] = c) != 'S') throw new NotThisTypeException();
        c = (char)oIS.read();
        if ((_achHead[1] = c) != 'M') throw new NotThisTypeException();
        c = (char)oIS.read();
        _achHead[2] = c; // 'J' for video, 'N' left audio and 'R' for right audio
        c = (char)oIS.read();
        if ((_achHead[3] = c) != '\1') throw new NotThisTypeException();
        // There appear to be 10 sectors for every frame
        // First two sectors are audio (left, right channels)
        // Then the remaining 8 sectors is the video frame, always
        // 320x224.
        _iSectorNumber = oIS.read(); // 0 to _iSectorsInAVFrame
        _iSectorsInAVFrame = oIS.read(); // either 9 or 1
        //if (_iSectorsInAVFrame != 9) throw new NotThisTypeException();
        _iFrameNumber = IO.readSInt16LE(oIS); // starts @ 0
        if (_iFrameNumber < 0) throw new NotThisTypeException();
        
        return oIS;
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
    public static class SectorFF8Video
            extends SectorFF8
            implements IVideoSector 
    {


        public SectorFF8Video(CdSector oCDSect)
                throws NotThisTypeException 
        {
            super(oCDSect);
            try {
                super.ReadHeader(oCDSect);
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
            if (super._achHead[2] != 'J') throw new NotThisTypeException();
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
            return super.getCDSector().getCdUserDataSize() -
                   SectorFF8.SHARED_HEADER_SIZE;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                    SHARED_HEADER_SIZE, getIdentifiedUserDataSize());
        }

        public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
            super.getCDSector().getCdUserDataCopy(SHARED_HEADER_SIZE, abOut,
                    iOutPos, getIdentifiedUserDataSize());
        }

        public String toString() {
            return String.format("VideoFF8 %s 320x224", super.toString());
        }

        public int getSectorType() {
            return SECTOR_VIDEO;
        }
        
        public String getTypeName() {
            return "FF8Video";
        }

        public boolean matchesPrevious(IVideoSector prevSect) {
            if (!(prevSect instanceof SectorFF8Video))
                return false;

            SectorFF8Video prevFF8Vid = (SectorFF8Video) prevSect;

            long lngNextChunk = prevFF8Vid.getFF8ChunkNumber() + 1;
            long lngNextFrame = prevFF8Vid.getFrameNumber();
            if (lngNextChunk >= prevFF8Vid.getFF8ChunksInFrame()) {
                lngNextChunk = 2;
                lngNextFrame++;
            }

            if (lngNextChunk != getFF8ChunkNumber() ||
                lngNextFrame != getFrameNumber())
                return false;

            if (getFrameNumber() == prevFF8Vid.getFrameNumber() &&
                getFF8ChunksInFrame() != prevFF8Vid.getFF8ChunksInFrame())
                return false;

            return true;
        }

        public int replaceFrameData(CdFileSectorReader cd,
                                    byte[] abDemuxData, int iDemuxOfs,
                                    int iLuminQscale, int iChromQscale,
                                    int iMdecCodeCount)
                throws IOException
        {
            byte[] abSectUserData = getCDSector().getCdUserDataCopy();

            int iBytesToCopy = getIdentifiedUserDataSize();
            if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
                iBytesToCopy = abDemuxData.length - iDemuxOfs;

            // bytes to copy might be 0, which is ok because we
            // still need to write the updated headers
            System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, SectorFF8.SHARED_HEADER_SIZE, iBytesToCopy);

            cd.writeSector(getSectorNumber(), abSectUserData);

            return iBytesToCopy;
        }

        public boolean splitAudio() {
            // don't want to split audio because that would cut the audio
            // at the beginning of the movie
            return false;
        }
    }

    /** Final Fantasy 8 audio sector. */
    public static class SectorFF8Audio
            extends SectorFF8
            implements ISquareAudioSector
    {
        
        /*
        protected final char _achHead[] = new char[4]; // [4 bytes] "SM_\1"
        protected int        _iSectorNumber;           // [1 byte]
        protected int        _iSectorsInAVFrame;       // [1 byte]
        protected long       _iFrameNumber;          // [2 bytes]
        */
        
        public final static int AUDIO_ADDITIONAL_HEADER_SIZE = 360;

        // 232 bytes; unknown
        protected final char _achMORIYA[] = new char[6];
        // 10 bytes; unknown
        protected SquareAKAOstruct _oAKAOstruct;
        // 76 bytes; unknown
        
        public SectorFF8Audio(CdSector oCDSect)
                throws NotThisTypeException 
        {
            super(oCDSect);
            
            try {
                InputStream is = super.ReadHeader(oCDSect);
                
                if (super._achHead[2] != 'N' && super._achHead[2] != 'R')
                    throw new NotThisTypeException();

                IO.skip(is, 232);
                
                _achMORIYA[0] = (char)IO.readUInt8(is);
                _achMORIYA[1] = (char)IO.readUInt8(is);
                _achMORIYA[2] = (char)IO.readUInt8(is);
                _achMORIYA[3] = (char)IO.readUInt8(is);
                _achMORIYA[4] = (char)IO.readUInt8(is);
                _achMORIYA[5] = (char)IO.readUInt8(is);
                
                IO.skip(is, 10);
                
                _oAKAOstruct = new SquareAKAOstruct(is);

                // TODO: check Sound Parameters values that the index is valid
                
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
            
        }

        public int getSamplesPerSecond() {
            return 44100;
        }

        public int getIdentifiedUserDataSize() {
            return (int)_oAKAOstruct.BytesOfData;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                    SectorFF8.SHARED_HEADER_SIZE + AUDIO_ADDITIONAL_HEADER_SIZE,
                    getIdentifiedUserDataSize());
        }

        public String toString() {
            return String.format("AudioFF8 %s MORIYA:%s %s", 
                   super.toString(),
                   new String(_achMORIYA),
                   _oAKAOstruct.toString());
        }

        public int getAudioDataSize() {
            return (int)_oAKAOstruct.BytesOfData; // always 1680 for FF8
        }

        public int getAudioChunkNumber() {
            return super._iSectorNumber;
        }

        public int getAudioChunksInFrame() {
            return 2;
        }

        public int getSectorType() {
            return SECTOR_AUDIO;
        }
        
        public String getTypeName() {
            return "FF8Audio";
        }

        public boolean isStereo() {
            return true;
        }

        public int getAudioChannel() {
            return (int)getAudioChunkNumber();
        }

        public long getLeftSampleCount() {
            if (getAudioChunkNumber() == 0)
                return SquareADPCMDecoder.calculateSamplesGenerated(getAudioDataSize());
            else
                return 0;
        }

        public long getRightSampleCount() {
            if (getAudioChunkNumber() == 1)
                return SquareADPCMDecoder.calculateSamplesGenerated(getAudioDataSize());
            else
                return 0;
        }

         public boolean matchesPrevious(ISquareAudioSector oPrevSect) {
            if (!(oPrevSect instanceof SectorFF8Audio))
                return false;

            if (oPrevSect.getAudioChunkNumber() == 0) {
                if (getAudioChunkNumber() != 1 ||
                    oPrevSect.getFrameNumber() != getFrameNumber() ||
                    oPrevSect.getSectorNumber() + 1 != getSectorNumber())
                    return false;
            } else if (oPrevSect.getAudioChunkNumber() == 1) {
                if (getAudioChunkNumber() != 0 ||
                    oPrevSect.getFrameNumber() + 1 != getFrameNumber() ||
                    // the audio only movie
                    (oPrevSect.getSectorNumber() + 1 != getSectorNumber() &&
                     oPrevSect.getSectorNumber() + 9 != getSectorNumber()))
                    return false;
            }

            return true;
        }

    }
    
}