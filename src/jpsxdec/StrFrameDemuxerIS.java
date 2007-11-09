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
 * StrFrameDemuxerIS.java
 *
 */

package jpsxdec;

import java.io.InputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Iterator;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.PSXSector.*;

/** Demuxes a series of frame chunk sectors into a solid stream */
public class StrFrameDemuxerIS extends InputStream 
        implements IGetFilePointer
{
    
    public static int DebugVerbose = 2;
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Stores the matching Video Frame Chunks */
    //private IVideoChunkSector m_aoFrameChunks[];
    /** The iterator to walk through searching for the the sectors we need */
    Iterator<PSXSector> m_oPsxSectorIterator;
    IVideoChunkSector m_oCurrentChunk;

    // All submitted Video Frame Chunks should match these values
    private long m_lngFrame = -1;
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    private long m_lngLastFilePointer = -1;
    
    /** Keep track of the current chunk being read */
    //private int m_iChunkIndex = -1;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public StrFrameDemuxerIS(Iterator<PSXSector> oPsxIter) {
        this(oPsxIter, -1);
    }
    
    public StrFrameDemuxerIS(Iterator<PSXSector> oPsxIter, long lngFrame) {
        m_lngFrame = lngFrame;
        m_oPsxSectorIterator = oPsxIter;
        m_oCurrentChunk = FindNextMatchingChunk();
    }
    
    /** Find the next sector that matches our requirements. */
    public IVideoChunkSector FindNextMatchingChunk() {
        
        while (m_oPsxSectorIterator.hasNext()) {
        
            PSXSector oSector = m_oPsxSectorIterator.next();
            
            // Skip any non video chunk sectors
            if (!(oSector instanceof IVideoChunkSector)) continue;

            IVideoChunkSector oFrameChunk = (IVideoChunkSector)oSector;

            // base our matching on the first sector received
            if (m_lngFrame < 0)
            {
                m_lngFrame = oFrameChunk.getFrameNumber();
                m_lngWidth = oFrameChunk.getWidth();
                m_lngHeight = oFrameChunk.getHeight();
                return oFrameChunk;
            } else if (m_lngWidth < 0) {
                
                if (m_lngFrame == oFrameChunk.getFrameNumber()) 
                {
                    m_lngWidth = oFrameChunk.getWidth();
                    m_lngHeight = oFrameChunk.getHeight();
                    
                    return oFrameChunk;
                }
            } else {
                if (m_lngFrame  == oFrameChunk.getFrameNumber() &&
                    m_lngWidth  == oFrameChunk.getWidth()       &&
                    m_lngHeight == oFrameChunk.getHeight() ) 
                {
                    return oFrameChunk;
                }
            }

        }
        
        return null;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Get the current position in the current sector being read.
     *  [implements IGetFilePointer] */
    public long getFilePointer() {
        if (m_oCurrentChunk == null)
            return m_lngLastFilePointer;
        else
            return m_oCurrentChunk.getFilePointer();
    }

    public long getWidth() {
        return m_lngWidth;
    }

    public long getHeight() {
        return m_lngHeight;
    }
    
    /** has chunks.... But seriously, we need to share
     *  if any chunks are ready to be read. */
    public boolean hasChunks() {
        return m_oCurrentChunk != null;
    }
    
    public long getFrameNumber() {
        return m_lngFrame;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** extends InputStream */
    public int read() throws IOException {
        
        if (m_oCurrentChunk == null) {
            // we're at the end of the chunks
            // or we never got any to begin with
            return -1;
        }
        
        int iByte = m_oCurrentChunk.read();
        while (iByte < 0) { // at the end of the chunk?

            m_lngLastFilePointer = m_oCurrentChunk.getFilePointer();
            
            // try to find the next chunk
            m_oCurrentChunk = FindNextMatchingChunk();
            if (m_oCurrentChunk == null) {
                // end of matching chunks
                return -1; // end of stream
            } else {
                iByte = m_oCurrentChunk.read(); // try again
            }
            
        }
        return iByte;
    }

}
