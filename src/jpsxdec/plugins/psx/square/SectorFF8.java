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

package jpsxdec.plugins.psx.square;

import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.psx.str.IVideoSector;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
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
    
    public SectorFF8(CDSector oCDSect) {
        super(oCDSect);
    }

    protected InputStream ReadHeader(CDSector oCDSect)
            throws IOException, NotThisTypeException 
    {
        // both audio and video sectors are flagged as data
        if (oCDSect.hasSectorHeader() && 
            oCDSect.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.DATA)
            throw new NotThisTypeException();
        
        InputStream oIS = oCDSect.getCDUserDataStream();
        
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
    
    public JPSXPlugin getSourcePlugin() {
        return JPSXPluginSquare.getPlugin();
    }

    ////////////////////////////////////////////////////////////////////////////

    /** Final Fantasy 8 video chunk sector. */
    public static class PSXSectorFF8Video
            extends SectorFF8
            implements IVideoSector 
    {


        public PSXSectorFF8Video(CDSector oCDSect)
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

        public int getPSXUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                   SectorFF8.SHARED_HEADER_SIZE;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                    SHARED_HEADER_SIZE, getPSXUserDataSize());
        }

        public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
            super.getCDSector().getCdUserDataCopy(SHARED_HEADER_SIZE, abOut,
                    iOutPos, getPSXUserDataSize());
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
            if (!(prevSect instanceof PSXSectorFF8Video))
                return false;

            PSXSectorFF8Video prevFF8Vid = (PSXSectorFF8Video) prevSect;

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

        public DiscItem createMedia(int iStartSector, int iStartFrame,
                                    int iFrame1LastSector,
                                    int iSectors, int iPerFrame)
        {
            return new DiscItemSTRVideo(iStartSector, getSectorNumber(),
                                        iStartFrame, getFrameNumber(),
                                        getWidth(), getHeight(),
                                        iSectors, iPerFrame,
                                        iFrame1LastSector);
        }
        public DiscItem createMedia(int iStartSector, int iStartFrame, int iFrame1LastSector)
        {
            return createMedia(iStartSector, iStartFrame, iFrame1LastSector, 10, 1);
        }
    }

    /** Final Fantasy 8 audio sector. */
    public static class PSXSectorFF8Audio 
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
        
        public PSXSectorFF8Audio(CDSector oCDSect)
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
                
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
            
        }

        public int getSamplesPerSecond() {
            return 44100;
        }

        public int getPSXUserDataSize() {
            return (int)_oAKAOstruct.BytesOfData;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                    SectorFF8.SHARED_HEADER_SIZE + AUDIO_ADDITIONAL_HEADER_SIZE,
                    getPSXUserDataSize());
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
                return getAudioDataSize() / 2; // TODO: I know this is wrong
            else
                return 0;
        }

        public long getRightSampleCount() {
            if (getAudioChunkNumber() == 1)
                return getAudioDataSize() / 2; // TODO: I know this is wrong
            else
                return 0;
        }

         public boolean matchesPrevious(ISquareAudioSector oPrevSect) {
            if (!(oPrevSect instanceof PSXSectorFF8Audio))
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

        public DiscItem createMedia(int iStartSector, long lngLeftSampleCount, long lngRightSampleCount, int iPeriod) {
                return new DiscItemSquareAudioStream(
                    iStartSector, getSectorNumber(),
                    lngLeftSampleCount, lngRightSampleCount,
                    getSamplesPerSecond(), iPeriod - 1);
        }
    }
    
}