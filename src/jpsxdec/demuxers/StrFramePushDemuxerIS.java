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
 * ArrayDemuxerIS.java
 */

package jpsxdec.demuxers;

import java.io.InputStream;
import java.io.IOException;
import java.util.LinkedList;
import jpsxdec.*;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.sectortypes.PSXSector.IVideoChunkSector;
import jpsxdec.util.IWidthHeight;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  Sectors need to be added ('pushed') in their proper order. */
public class StrFramePushDemuxerIS extends InputStream 
        implements IGetFilePointer, IWidthHeight
{
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private LinkedList<IVideoChunkSector> m_oChunks = 
            new LinkedList<IVideoChunkSector>();
    private IVideoChunkSector m_oCurChunk;
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    private long m_lngFrame = -1;
    private long m_lngAddedChunkNum = 0;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public void addChunk(IVideoChunkSector oChk) {
        if (m_lngWidth < 0)
            m_lngWidth = oChk.getWidth();
        else if (m_lngWidth != oChk.getWidth())
            throw new IllegalArgumentException("Not all chunks have the same width");

        if (m_lngHeight < 0) 
            m_lngHeight = oChk.getHeight();
        else if (m_lngHeight != oChk.getHeight())
            throw new IllegalArgumentException("Not all chunks have the same height");

        if (m_lngFrame < 0)
            m_lngFrame = oChk.getFrameNumber();
        else if (m_lngFrame != oChk.getFrameNumber())
            throw new IllegalArgumentException("Not all chunks have the same frame");

        if (oChk.getChunkNumber() == m_lngAddedChunkNum) {
            m_oChunks.offer(oChk);
            m_lngAddedChunkNum++;
            if (m_oCurChunk == null)
                m_oCurChunk = oChk;
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Get the current position in the current sector being read. */
    /* IGetFilePointer */
    public long getFilePointer() {
        if (m_oCurChunk != null)
            return m_oCurChunk.getFilePointer();
        else
            return -1;
    }
    
    /* IWidthHeight */
    public long getWidth() {
        return m_lngWidth;
    }

    /* IWidthHeight */
    public long getHeight() {
        return m_lngHeight;
    }
    
    public long getFrameNumber() {
        return m_lngFrame;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Override /* InputStream */
    public int read() throws IOException {
        if (m_oCurChunk == null) return -1;
        
        int iByte = m_oCurChunk.read();
        while (iByte < 0) { // at the end of the chunk?

            if (m_oChunks.size() >= 1)
                m_oCurChunk = m_oChunks.poll();
            else
                return -1;
            
            iByte = m_oCurChunk.read();
        }
        return iByte;
    }

}
