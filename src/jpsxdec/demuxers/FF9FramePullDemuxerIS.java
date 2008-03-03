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
 * FF9FramePullDemuxerIS.java
 */

package jpsxdec.demuxers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.PSXSectorFF9Video;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IWidthHeight;

// TODO:

/** Demuxes a series of FF9 frame chunk sectors into a solid stream. */
public class FF9FramePullDemuxerIS 
        extends InputStream 
        implements IGetFilePointer, IWidthHeight 
{

    /** The iterator to walk through searching for the the sectors we need */
    AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    
    private long m_lngFrame = -1;
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    private long m_lngLastFilePointer = -1;
    
    private Stack<PSXSectorFF9Video> m_oFrameSectors
            = new Stack<PSXSectorFF9Video>();
    
    public FF9FramePullDemuxerIS(PSXSectorRangeIterator oPsxIter, long lngFrame) 
            throws IOException
    {
        m_lngFrame = lngFrame;
        m_oPsxSectorIterator = oPsxIter;
        FindFrameSectors();
        if (m_oFrameSectors.size() != 8)
            throw new IOException(
                    "FF9 Frame " + m_lngFrame + " is missing sectors. " +
                    "Unable to decode.");
    }

    /** Find the 8 frame sectors for the desired frame. 
     *  Does NOT devour any more than the necessary sectors from the
     *  iterator. */
    private void FindFrameSectors() throws IOException {
        
        while (m_oPsxSectorIterator.hasNext()) {
        
            PSXSector oSector = m_oPsxSectorIterator.peekNext();
            
            // Skip any non FF9 video chunk sectors
            if (!(oSector instanceof PSXSectorFF9Video)) {
                m_oPsxSectorIterator.skipNext();
                continue;
            }

            PSXSectorFF9Video oFF9FrameChunk = (PSXSectorFF9Video)oSector;

            // if no specific frame desired...
            if (m_lngFrame < 0) {
                // base our matching on the first sector received
                m_lngFrame = oFF9FrameChunk.getFrameNumber();
                m_lngWidth = oFF9FrameChunk.getWidth();
                m_lngHeight = oFF9FrameChunk.getHeight();
                // put the sector in like a stack because
                // they come in reverse order
                m_oFrameSectors.push(oFF9FrameChunk);
            } else {
                // we know what frame number we're looking for
                if (m_lngFrame  == oFF9FrameChunk.getFrameNumber()) 
                {
                    // we found another match
                    m_lngWidth = oFF9FrameChunk.getWidth();
                    m_lngHeight = oFF9FrameChunk.getHeight();
                    // put the sector in like a stack because
                    // they come in reverse order
                    m_oFrameSectors.push(oFF9FrameChunk);
                } else if (oFF9FrameChunk.getFrameNumber() > m_lngFrame) {
                    // we passed the frame already, so we're done
                    /* Since all FF9 movies (hopefully) have the same format,
                     * we can safely assume this. */
                    return; // return before skipping this sector (we still need it)
                }
            }
            // we're done with this sector, go to next
            m_oPsxSectorIterator.skipNext();
        }
    }

    
    /** [implements IGetFilePointer] */
    public long getFilePointer() {
        if (m_oFrameSectors.size() == 0)
            return m_lngLastFilePointer;
        else
            return m_oFrameSectors.peek().getFilePointer();
    }
    
    /** [InputStream] */ @Override 
    public int read() throws IOException {
        if (m_oFrameSectors.size() == 0) {
            // we're at the end of the chunks
            // or we never got any to begin with
            return -1;
        }
        
        int iByte = m_oFrameSectors.peek().read();
        while (iByte < 0) { // at the end of the chunk?

            m_lngLastFilePointer = m_oFrameSectors.peek().getFilePointer();
            
            // remove the head of the list
            m_oFrameSectors.pop();
            
            // try to get the next chunk
            if (m_oFrameSectors.size() == 0) {
                // end of matching chunks
                return -1; // end of stream
            } else {
                iByte = m_oFrameSectors.peek().read(); // try again
            }
            
        }
        return iByte;
    }

    /** [implements IWidthHeight] */
    public long getWidth() {
        return m_lngWidth;
    }

    /** [implements IWidthHeight] */
    public long getHeight() {
        return m_lngHeight;
    }

}
