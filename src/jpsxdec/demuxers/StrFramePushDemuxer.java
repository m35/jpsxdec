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
 * StrFramePushDemuxer.java
 */

package jpsxdec.demuxers;

import java.util.AbstractList;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.util.IWidthHeight;
import jpsxdec.util.SequenceFPIS;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  Sectors need to be added ('pushed') in their proper order. */
public class StrFramePushDemuxer implements IWidthHeight
{
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private IVideoChunkSector[] m_aoChunks;
            
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    private long m_lngFrame;
    
    private long m_lngDemuxFrameSize = 0;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public StrFramePushDemuxer() {
        m_lngFrame = -1;
    }
    
    /** @param lngFrame  -1 for the frame of the first chunk received. */
    public StrFramePushDemuxer(long lngFrame) {
        m_lngFrame = lngFrame;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** [IWidthHeight] */
    public long getWidth() {
        return m_lngWidth;
    }

    /** [IWidthHeight] */
    public long getHeight() {
        return m_lngHeight;
    }
    
    /** Returns the frame number being demuxer, or -1 if still unknown. */
    public long getFrameNumber() {
        return m_lngFrame;
    }
    
    public long getDemuxFrameSize() {
        return m_lngDemuxFrameSize;
    }
    
    public boolean isFull() {
        for (IVideoChunkSector chk : m_aoChunks) {
            if (chk == null) return false;
        }
        return true;
    }
    
    public int getChunksInFrame() {
        return m_aoChunks.length;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public void addChunk(IVideoChunkSector oChk) {
        if (m_lngFrame < 0)
            m_lngFrame = oChk.getFrameNumber();
        else if (m_lngFrame != oChk.getFrameNumber())
            throw new IllegalArgumentException("Not all chunks have the same frame");

        if (m_lngWidth < 0)
            m_lngWidth = oChk.getWidth();
        else if (m_lngWidth != oChk.getWidth())
            throw new IllegalArgumentException("Not all chunks have the same width");

        if (m_lngHeight < 0) 
            m_lngHeight = oChk.getHeight();
        else if (m_lngHeight != oChk.getHeight())
            throw new IllegalArgumentException("Not all chunks have the same height");

        // if this is the first chunk added
        if (m_aoChunks == null)
            m_aoChunks = new IVideoChunkSector[(int)oChk.getChunksInFrame()];
        else if (oChk.getChunkNumber() >= m_aoChunks.length) {
            IVideoChunkSector[] oldData = m_aoChunks;
	    m_aoChunks = new IVideoChunkSector[(int)oChk.getChunkNumber()+1];
	    System.arraycopy(oldData, 0, m_aoChunks, 0, oldData.length);
        }
        
        // now add the chunk where it belongs in the list
        int iChkNum = (int)oChk.getChunkNumber();
        // make sure we don't alrady have the chunk
        if (m_aoChunks[iChkNum] != null)
            throw new IllegalArgumentException("Chunk number " + iChkNum + " already received.");
        
        m_aoChunks[iChkNum] = oChk;
        // record add the sector's data size to the total
        m_lngDemuxFrameSize += oChk.getPsxUserDataSize();
    }
    
    public void addChunks(AbstractList<IVideoChunkSector> oChks) {
        for (IVideoChunkSector oChk : oChks) {
            addChunk(oChk);
        }
    }
    
    public SequenceFPIS getStream() {
        // TODO: Should add better error handling if chunks are missing
        return new SequenceFPIS(new SequenceFPIS.ArrayEnum(m_aoChunks));
    }

}
