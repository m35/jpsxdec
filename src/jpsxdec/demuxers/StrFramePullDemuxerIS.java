/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * StrFramePullDemuxerIS.java
 */

package jpsxdec.demuxers;

import java.io.InputStream;
import java.io.IOException;
import jpsxdec.*;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IWidthHeight;

/** Demuxes a series of frame chunk sectors into a solid stream.
 * Pulls frame chunks from an iterator (i.e. reads them from a disc) only as 
 * they are needed, so you may not need to read all the sectors of a frame
 * before you can decode it. 
 * 
 * But now that I think about it, this really doesn't
 * save any reading, because the next frame still has to read all the leftover
 * sectors before it reaches the first sector of its frame. */
public class StrFramePullDemuxerIS extends InputStream 
        implements IGetFilePointer, IWidthHeight
{
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** The iterator to walk through searching for the the sectors we need */
    AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    
    /** Holds the current chunk being read.
     *  Will be null when reading is done. */
    ByteArrayFPIS m_oCurrentChunk;

    // All Video Frame Chunks should match these values
    private long m_lngFrame = -1;
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    private long m_lngChunksInFrame;
    
    /** Before moving to the next frame chunk, save the last known
     *  file pointer in case there are no more frame chunks */
    private long m_lngLastFilePointer = -1;
    
    /** Size of frame user-data available for this frame. 
     *  This is calculated as the chunks are being read, and won't
     *  contain the full and final value until the last chunk is being read.*/
    private long m_lngSize = 0;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public StrFramePullDemuxerIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException 
    {
        this(oPsxIter, -1);
    }
    
    public StrFramePullDemuxerIS(AdvancedIOIterator<PSXSector> oPsxIter, long lngFrame) 
            throws IOException 
    {
        m_lngFrame = lngFrame;
        m_oPsxSectorIterator = oPsxIter;
        m_oCurrentChunk = FindNextMatchingChunk().getUserDataStream();
        m_lngSize += m_oCurrentChunk.size();
    }
    
    /** Finds the next matching sector. Does NOT devour any more sectors
     *  than it needs.
     * @return Next Video Chunk sector, or null if no more matches. */
    public IVideoChunkSector FindNextMatchingChunk() throws IOException {
        
        while (m_oPsxSectorIterator.hasNext()) {
        
            PSXSector oSector = m_oPsxSectorIterator.peekNext();
            
            // Skip any non video chunk sectors
            if (!(oSector instanceof IVideoChunkSector)) {
                m_oPsxSectorIterator.skipNext();
                continue;
            }

            IVideoChunkSector oFrameChunk = (IVideoChunkSector)oSector;
            
            if (m_lngFrame < 0) {
                // no specific frame desired, so
                // base our matching on the first sector received
                m_lngFrame = oFrameChunk.getFrameNumber();
                m_lngWidth = oFrameChunk.getWidth();
                m_lngHeight = oFrameChunk.getHeight();
                m_lngChunksInFrame = oFrameChunk.getChunksInFrame();
                
                m_oPsxSectorIterator.skipNext(); // move iterator to next sector
                return oFrameChunk;
            } else if (m_lngWidth < 0) {
                // we have a desired frame, but we haven't
                // found a match yet
                if (m_lngFrame == oFrameChunk.getFrameNumber()) {
                    // we found our first match
                    m_lngWidth = oFrameChunk.getWidth();
                    m_lngHeight = oFrameChunk.getHeight();
                    
                    m_oPsxSectorIterator.skipNext(); // move iterator to next sector
                    return oFrameChunk;
                } else if (oFrameChunk.getFrameNumber() > m_lngFrame) {
                    // we passed the frame already, so we're done
                    /* Note: This is faster, but if strange movies have
                       intermingled frame parts, then this would fail to find 
                       furthur frame chunks.*/
                    return null;
                } else {
                    // not at our frame yet, move along
                    m_oPsxSectorIterator.skipNext(); // move iterator to next sector
                }
                
            } else {
                // we have a frame and already found a match,
                // so find the next match
                if (m_lngFrame  == oFrameChunk.getFrameNumber() &&
                    m_lngWidth  == oFrameChunk.getWidth()       &&
                    m_lngHeight == oFrameChunk.getHeight() ) 
                {
                    // we found another match
                    m_oPsxSectorIterator.skipNext(); // so move iterator to next sector
                    return oFrameChunk;
                } else if (oFrameChunk.getFrameNumber() > m_lngFrame) {
                    // we passed the frame already, so we're done
                    /* Note: This is faster, but if strange movies have
                       intermingled frame parts, then this would fail to find 
                       furthur frame chunks.*/
                    return null; 
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

    /** [implements IWidthHeight] */
    public long getWidth() {
        return m_lngWidth;
    }

    /** [implements IWidthHeight] */
    public long getHeight() {
        return m_lngHeight;
    }
    
    public long getFrameNumber() {
        return m_lngFrame;
    }

    public long getChunksInFrame() {
        return m_lngChunksInFrame;
    }

    public long getFrameUserDataSize() {
        return m_lngSize;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** [extends InputStream] */
    public int read() throws IOException {
        
        if (m_oCurrentChunk == null) {
            // we're at the end of the chunks
            // or we never got any to begin with
            return -1;
        }
        
        int iByte = m_oCurrentChunk.read();
        while (iByte < 0) { // at the end of the chunk?

            m_lngLastFilePointer = m_oCurrentChunk.getFilePointer();
            
            // try to get the next chunk
            m_oCurrentChunk = FindNextMatchingChunk().getUserDataStream();
            if (m_oCurrentChunk == null) {
                // end of matching chunks
                return -1; // end of stream
            } else {
                m_lngSize += m_oCurrentChunk.size();
                iByte = m_oCurrentChunk.read(); // try again
            }
            
        }
        return iByte;
    }

}
