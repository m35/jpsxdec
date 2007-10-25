/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


/*
 * StrFrameDemuxerIS.java
 *
 */

package jpsxdec;

import java.io.InputStream;
import java.io.IOException;
import java.util.AbstractList;
import jpsxdec.util.IGetFilePointer;

/** Demuxes a series of frame chunk sectors into a solid stream */
public class StrFrameDemuxerIS extends InputStream 
        implements IGetFilePointer
{
    
    public static int DebugVerbose = 2;
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Stores the matching Video Frame Chunks */
    private IVideoChunkSector m_aoFrameChunks[];

    // All submitted Video Frame Chunks should match these values
    private long m_iFrame = -1;
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    /** Keep track of the current chunk being read */
    private int m_iChunkIndex = -1;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public StrFrameDemuxerIS(AbstractList<PSXSector> oSectorList) {
        for (PSXSector oSector : oSectorList) {
            AddSector(oSector);
        }
    }

    /** Check that the submitted sector matches our requirements. 
     * Keep if it does. */
    public boolean AddSector(PSXSector oSector) {
        
        // Skip any non video chunk sectors
        if (!(oSector instanceof IVideoChunkSector)) return false;
            
        IVideoChunkSector oFrameChunk = (IVideoChunkSector)oSector;

        // base our matching on the first sector received
        if (m_aoFrameChunks == null)
        {
            m_iFrame = oFrameChunk.getFrameNumber();
            m_lngWidth = oFrameChunk.getWidth();
            m_lngHeight = oFrameChunk.getHeight();
            m_aoFrameChunks = 
                    new IVideoChunkSector[(int)oFrameChunk.getChunksInFrame()];
        } else {
            if (m_iFrame    != oFrameChunk.getFrameNumber() ||
                m_lngWidth  != oFrameChunk.getWidth() ||
                m_lngHeight != oFrameChunk.getHeight() ) 
                return false;
        }

        int iChunkNum = (int)oFrameChunk.getChunkNumber();
        if (m_aoFrameChunks[iChunkNum] != null) {
            // WARNING! We already have this chunk!! What's going on?!
        }
        m_aoFrameChunks[iChunkNum] = oFrameChunk;

        return true;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Get the current position in the current sector being read.
     *  implements IGetFilePointer. */
    public long getFilePointer() {
        if (m_iChunkIndex < 0) return -1;
        return m_aoFrameChunks[m_iChunkIndex].getFilePointer();
    }

    public long getWidth() {
        return m_lngWidth;
    }

    public long getHeight() {
        return m_lngHeight;
    }
    
    /** has chunks.... But seriously, we need to check if any frame
      * chunks were actually accepted */
    public boolean hasChunks() {
        return m_aoFrameChunks != null;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** extends InputStream */
    public int read() throws IOException {
        // if we never got any video frames
        if (m_aoFrameChunks == null) return -1;
        
        // If we have not yet begun to read
        if (m_iChunkIndex < 0) {
            m_iChunkIndex = 0;
            // skip missing chunks (there has to be at least one chunk in here)
            /* Technically, for standard STR, if we're missing the first chunk, 
             * then the decoding will eventually fail. */
            while (m_aoFrameChunks[m_iChunkIndex] == null)
                m_iChunkIndex++;
        }
        
        int iByte = m_aoFrameChunks[m_iChunkIndex].read();
        while (iByte < 0) { // at the end of the sector?
            
            // first make sure we are not at the end of the frame
            // or missing the next sector
            if (m_iChunkIndex+1 < m_aoFrameChunks.length && 
                    m_aoFrameChunks[m_iChunkIndex+1] != null) {
                
                m_iChunkIndex++; // move to next sector
                
                // skip the header
                iByte = m_aoFrameChunks[m_iChunkIndex].read(); // try again
            } else {
                // end of sectors or missing sector
                return -1; // end of stream
            }
            
        }
        return iByte;
    }

}
