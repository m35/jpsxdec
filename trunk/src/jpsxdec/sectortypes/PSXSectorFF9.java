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
 * PSXSectorFF9.java
 */

package jpsxdec.sectortypes;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Base class for FF9 movie (audio/video) sectors. */
public abstract class PSXSectorFF9 extends PSXSector {
        
    protected long m_lngMagic;                //  0    [4 bytes]
    protected long m_lngChunkNumber;          //  4    [2 bytes]
    protected long m_lngChunksInThisFrame;    //  6    [2 bytes]
    protected long m_lngFrameNumber;          //  8    [4 bytes]

    protected PSXSectorFF9(CDXASector oCDSect) throws NotThisTypeException {
        super(oCDSect);
    }

    protected InputStream ReadHeader(CDXASector oCDSect, int iForm) 
            throws IOException, NotThisTypeException 
    {

        InputStream oIS = oCDSect.getSectorDataStream();
        
        if (!oCDSect.hasSectorHeader()) throw new NotThisTypeException();
        if (oCDSect.getSubMode().data != 1) throw new NotThisTypeException();
        if (oCDSect.getSubMode().form != iForm) throw new NotThisTypeException();

        m_lngMagic = IO.ReadUInt32LE(oIS);
        m_lngChunkNumber = IO.ReadUInt16LE(oIS);
        m_lngChunksInThisFrame = IO.ReadUInt16LE(oIS);
        // Note that reading these 32 bit values could overflow to -1
        // if that should happen, then this couldn't be a frame chunk
        m_lngFrameNumber = IO.ReadUInt32LE(oIS);
        if (m_lngFrameNumber == -1) // check for overflow
            throw new NotThisTypeException();
        
        return oIS;
    }

    public long getActualChunkNumber() {
        return m_lngChunkNumber;
    }

    public long getActualChunksInFrame() {
        return m_lngChunksInThisFrame;
    }

    public long getFrameNumber() {
        return m_lngFrameNumber;
    }

    /**************************************************************************/
    /**************************************************************************/

    public static class PSXSectorFF9Video 
            extends PSXSectorFF9 
            implements IVideoChunkSector 
    {

        // .. Static stuff .....................................................

        public static final int VIDEO_CHUNK_MAGIC = 0x00040160;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;

        // .. Instance .........................................................

        /*
        protected long m_lngMagic;                //  0    [4 bytes]
        protected long m_lngChunkNumber;          //  4    [2 bytes]
        protected long m_lngChunksInThisFrame;    //  6    [2 bytes]
        protected long m_lngFrameNumber;          //  8    [4 bytes]
        */
        protected long m_lngChunkDuration;        //  12   [4 bytes] (frame duration?)
        protected long m_lngWidth;                //  16   [2 bytes]
        protected long m_lngHeight;               //  18   [2 bytes]
        protected long m_lngRunLengthCodeCount;   //  20   [2 bytes]
        protected long m_lngHeader3800;           //  22   [2 bytes] always 0x3800
        protected long m_lngQuantizationScale;    //  24   [2 bytes]
        protected long m_lngVersion;              //  26   [2 bytes]
        protected long m_lngFourZeros;            //  28   [4 bytes]
        //   32 TOTAL

        public PSXSectorFF9Video(CDXASector oCDSect) throws NotThisTypeException {
            super(oCDSect);

            try {
                InputStream oIS = super.ReadHeader(oCDSect, 2);

                // first make sure the magic nubmer is correct
                if (m_lngMagic != VIDEO_CHUNK_MAGIC) {
                    throw new NotThisTypeException();
                }

                // then continue reading the header

                m_lngChunkDuration = IO.ReadUInt32LE(oIS);
                if (m_lngChunkDuration == -1) // check for overflow
                {
                    throw new NotThisTypeException();
                }

                m_lngWidth = IO.ReadUInt16LE(oIS);
                m_lngHeight = IO.ReadUInt16LE(oIS);
                m_lngRunLengthCodeCount = IO.ReadUInt16LE(oIS);

                m_lngHeader3800 = IO.ReadUInt16LE(oIS);
                if (m_lngHeader3800 != 0x3800) {
                    throw new NotThisTypeException();
                }

                m_lngQuantizationScale = IO.ReadUInt16LE(oIS);
                m_lngVersion = IO.ReadUInt16LE(oIS);
                if (m_lngVersion != 2) {
                    throw new NotThisTypeException();
                }
                m_lngFourZeros = IO.ReadUInt32LE(oIS);
                // the 4 zeros aren't always zero
                //if (m_lngFourZeros != 0)
                //    throw new NotThisTypeException();

            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }

        // .. Public functions .................................................

        public String toString() {
            return "FF9Vid " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %dx%d ver:%d " + 
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
                m_lngFrameNumber,
                m_lngChunkNumber,
                m_lngChunksInThisFrame,
                m_lngWidth,
                m_lngHeight,
                m_lngVersion,
                m_lngChunkDuration,
                m_lngRunLengthCodeCount,
                m_lngHeader3800,
                m_lngQuantizationScale,
                m_lngFourZeros
                );
        }

        /** Returns the chunk nubmer in order of how they should be demuxed
         * (i.e. 8, 7, 6, ..., 1, 0). [implements IVideoSector] */
        public long getChunkNumber() {
            return 9 - super.m_lngChunkNumber;
        }
        /** [implements IVideoSector] */
        public long getChunksInFrame() {
            return super.m_lngChunksInThisFrame - 2; // i.e. return 8
        }

        public long getWidth() {
            return m_lngWidth;
        }

        public long getHeight() {
            return m_lngHeight;
        }

        protected int getDemuxedDataStart(int iDataSize) {
            // skipping the frame header
            return FRAME_CHUNK_HEADER_SIZE;
        }
        protected int getDemuxedDataLength(int iDataSize) {
            return iDataSize - FRAME_CHUNK_HEADER_SIZE;
        }

    }

    /**************************************************************************/
    /**************************************************************************/
    
    public static class PSXSectorFF9Audio 
            extends PSXSectorFF9 
            implements ISquareAudioSector
    {

        // .. Static stuff .....................................................

        public static final int FF9_AUDIO_CHUNK_MAGIC = 0x00080160;
        public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

        // .. Instance .........................................................

        /*
        private long m_lngMagic;                //  0    [4 bytes]
        private long m_lngChunkNumber;          //  4    [2 bytes]
        private long m_lngChunksInThisFrame;    //  6    [2 bytes]
        private long m_lngFrameNumber;          //  8    [4 bytes]
        */
        
        // 116 bytes unknown
        protected SquareAKAOstruct m_oAKAOstruct;     // [36 bytes]
        // 44 bytes; unknown
        
        //   208 TOTAL

        public PSXSectorFF9Audio(CDXASector oCDSect) throws NotThisTypeException 
        {
            super(oCDSect);
            
            try {
                InputStream oIS = super.ReadHeader(oCDSect, 1);

                // first make sure the magic nubmer is correct
                if (m_lngMagic != FF9_AUDIO_CHUNK_MAGIC) {
                    throw new NotThisTypeException();
                }

                oIS.skip(116);

                m_oAKAOstruct = new SquareAKAOstruct(oIS);
                
                //oIS.skip(44);

            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }

        // .. Public functions .................................................

        public String toString() {
            return "FF9Aud " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %s",
                m_lngFrameNumber,
                m_lngChunkNumber,
                m_lngChunksInThisFrame,
                m_oAKAOstruct.toString()
                );
        }

        public int getAudioDataSize() {
            if (m_oAKAOstruct.AKAO == SquareAKAOstruct.AKAO_ID)
                return (int)m_oAKAOstruct.BytesOfData;
            else
                return 0;
        }

        public boolean hasAudio() {
            return !(getAudioDataSize() == 0);
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
                throw new RuntimeException("What kind of strange samples/second is this sector?");
        }



        /** Want to skip the frame header */
        protected int getDemuxedDataStart(int iDataSize) {
            return FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }
        protected int getDemuxedDataLength(int iDataSize) {
            return (int)m_oAKAOstruct.BytesOfData;
        }

        public long getAudioChunkNumber() {
            return super.m_lngChunkNumber;
        }

        public long getAudioChunksInFrame() {
            return 2;
        }
    }
}
