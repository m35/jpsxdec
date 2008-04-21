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
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorFF9.PSXSectorFF9Video;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IWidthHeight;
import jpsxdec.util.SequenceFPIS;

/** Demuxes a series of FF9 frame chunk sectors into a solid stream. 
 *  Since FF9 chunks are stored in reverse order, we have no choice
 *  but to just read all of them before we can demux them (unless we
 *  want to read from the disc backward, but that's kinda weird). 
 *  So it just uses the StrFramePushDemuxer for most of the work. */
public class FF9FramePullDemuxerIS implements IWidthHeight 
{

    private StrFramePushDemuxer m_oPushDemuxer;
    
    /** The iterator to walk through searching for the the sectors we need */
    AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    
    public FF9FramePullDemuxerIS(PSXSectorRangeIterator oPsxIter, long lngFrame) 
            throws IOException
    {
        m_oPushDemuxer = new StrFramePushDemuxer(lngFrame);
        m_oPsxSectorIterator = oPsxIter;
        FindFrameSectors();
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
            if (m_oPushDemuxer.getFrameNumber() < 0) {
                m_oPushDemuxer.addChunk(oFF9FrameChunk);
            } else {
                // we know what frame number we're looking for
                if (m_oPushDemuxer.getFrameNumber()  == oFF9FrameChunk.getFrameNumber()) 
                {
                    // we found another match
                    m_oPushDemuxer.addChunk(oFF9FrameChunk);
                } else if (oFF9FrameChunk.getFrameNumber() > m_oPushDemuxer.getFrameNumber()) {
                    // we passed the frame already, so we're done
                    /* Since all FF9 movies have the same format,
                     * we can safely assume this. */
                    return; // return so we don't skip this sector
                }
            }
            // we're done with this sector, go to next
            m_oPsxSectorIterator.skipNext();
        }
    }
    
    public SequenceFPIS getStream() {
        return m_oPushDemuxer.getStream();
    }

    
    /** [implements IWidthHeight] */
    public long getWidth() {
        return m_oPushDemuxer.getWidth();
    }

    /** [implements IWidthHeight] */
    public long getHeight() {
        return m_oPushDemuxer.getHeight();
    }

}
