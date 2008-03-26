/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * PSXSectorFF8.java
 */

package jpsxdec.sectortypes;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Base class for FF8 movie (audio/video) sectors. */
public abstract class PSXSectorFF8 extends PSXSector {

    public static final int SHARED_HEADER_SIZE = 8;

    protected final char m_achHead[] = new char[4]; // [4 bytes] "SM_\1"
    protected int        m_iSectorNumber;           // [1 byte]
    protected int        m_iSectorsInAVFrame;       // [1 byte]
    protected long       m_lngFrameNumber;          // [2 bytes]
    
    public PSXSectorFF8(CDXASector oCDSect) {
        super(oCDSect);
    }

    protected InputStream ReadHeader(CDXASector oCDSect)
            throws IOException, NotThisTypeException 
    {
        // both audio and video sectors are flagged as data
        if (oCDSect.hasSectorHeader() && oCDSect.getSubMode().data != 1)
            throw new NotThisTypeException();
        
        InputStream oIS = oCDSect.getSectorDataStream();
        
        char c;
        c = (char)oIS.read();
        if ((m_achHead[0] = c) != 'S') throw new NotThisTypeException();
        c = (char)oIS.read();
        if ((m_achHead[1] = c) != 'M') throw new NotThisTypeException();
        c = (char)oIS.read();
        m_achHead[2] = c; // 'J' for video, 'N' left audio and 'R' for right audio
        c = (char)oIS.read();
        if ((m_achHead[3] = c) != '\1') throw new NotThisTypeException();
        // There appear to be 10 sectors for every frame
        // First two sectors are audio (left, right channels)
        // Then the remaining 8 sectors is the video frame, always
        // 320x224.
        m_iSectorNumber = oIS.read(); // 0 to m_iSectorsInAVFrame
        m_iSectorsInAVFrame = oIS.read(); // either 9 or 1
        //if (m_iSectorsInAVFrame != 9) throw new NotThisTypeException();
        m_lngFrameNumber = IO.ReadUInt16LE(oIS); // starts @ 0
        
        return oIS;
    }

    public long getFrameNumber() {
        return m_lngFrameNumber;
    }

    public long getFF8ChunkNumber() {
        return m_iSectorNumber;
    }

    public long getFF8ChunksInFrame() {
        return m_iSectorsInAVFrame-1;
    }

    @Override
    public String toString() {
        return super.toString() +
                String.format(" frame:%d chunk:%d/%d",
                    m_lngFrameNumber, 
                    m_iSectorNumber,
                    m_iSectorsInAVFrame);
    }
    
    

    ////////////////////////////////////////////////////////////////////////////

    /** FF8 video sector. */
    public static class PSXSectorFF8Video
            extends PSXSectorFF8
            implements IVideoChunkSector 
    {


        public PSXSectorFF8Video(CDXASector oCDSect)
                throws NotThisTypeException 
        {
            super(oCDSect);
            try {
                super.ReadHeader(oCDSect);
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
            if (super.m_achHead[2] != 'J') throw new NotThisTypeException();
        }

        /** [implements IVideoSector] */
        public long getWidth() {
            return 320;
        }

        /** [implements IVideoSector] */
        public long getHeight() {
            return 224;
        }

        /** [implements IVideoSector] */
        public long getChunkNumber() {
            return super.m_iSectorNumber - 2;
        }
        /** [implements IVideoSector] */
        public long getChunksInFrame() {
            return super.m_iSectorsInAVFrame - 1; // i.e. return 8
        }


        /** Want to skip the header bytes.
         * [extends PSXSector] */
        protected int getDemuxedDataStart(int iDataSize) {
            return PSXSectorFF8.SHARED_HEADER_SIZE;
        }
        /** [extends PSXSector] */
        protected int getDemuxedDataLength(int iDataSize) {
            return iDataSize - PSXSectorFF8.SHARED_HEADER_SIZE;
        }

        public String toString() {
            return String.format("VideoFF8 %s 320x224", 
                   super.toString());
        }

    }

    /** FF8 audio sector. */
    public static class PSXSectorFF8Audio 
            extends PSXSectorFF8
            implements ISquareAudioSector
    {
        
        /*
        protected final char m_achHead[] = new char[4]; // [4 bytes] "SM_\1"
        protected int        m_iSectorNumber;           // [1 byte]
        protected int        m_iSectorsInAVFrame;       // [1 byte]
        protected long       m_lngFrameNumber;          // [2 bytes]
        */
        
        public final static int AUDIO_ADDITIONAL_HEADER_SIZE = 360;

        // 232 bytes; unknown
        protected final char m_achMORIYA[] = new char[6];
        // 10 bytes; unknown
        protected SquareAKAOstruct m_oAKAOstruct;
        // 76 bytes; unknown
        
        public PSXSectorFF8Audio(CDXASector oCDSect)
                throws NotThisTypeException 
        {
            super(oCDSect);
            
            try {
                InputStream oIS = super.ReadHeader(oCDSect);
                
                if (super.m_achHead[2] != 'N' && super.m_achHead[2] != 'R')
                    throw new NotThisTypeException();

                oIS.skip(232);
                
                m_achMORIYA[0] = (char)IO.ReadUInt8(oIS);
                m_achMORIYA[1] = (char)IO.ReadUInt8(oIS);
                m_achMORIYA[2] = (char)IO.ReadUInt8(oIS);
                m_achMORIYA[3] = (char)IO.ReadUInt8(oIS);
                m_achMORIYA[4] = (char)IO.ReadUInt8(oIS);
                m_achMORIYA[5] = (char)IO.ReadUInt8(oIS);
                
                oIS.skip(10);
                
                m_oAKAOstruct = new SquareAKAOstruct(oIS);
                
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
            
        }

        public int getSamplesPerSecond() {
            return 44100;
        }

        /** Want to skip the header bytes. 
         *  [extends PSXSector] */
        protected int getDemuxedDataStart(int iDataSize) {
            // audio data starts at 360 past the header
            return PSXSectorFF8.SHARED_HEADER_SIZE + AUDIO_ADDITIONAL_HEADER_SIZE;
        }
        /** [extends PSXSector] */
        protected int getDemuxedDataLength(int iDataSize) {
            return iDataSize - (PSXSectorFF8.SHARED_HEADER_SIZE + AUDIO_ADDITIONAL_HEADER_SIZE);
        }

        public String toString() {
            return String.format("AudioFF8 %s MORIYA:%s %s", 
                   super.toString(),
                   new String(m_achMORIYA),
                   m_oAKAOstruct.toString());
        }

        public int getAudioDataSize() {
            return (int)m_oAKAOstruct.BytesOfData; // 1680
        }

        public long getAudioChunkNumber() {
            return super.m_iSectorNumber;
        }

        public long getAudioChunksInFrame() {
            return 2;
        }

    }
    
}