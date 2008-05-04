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
 * PSXSectorFrameChunk.java
 */

package jpsxdec.sectortypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for standard v2 and v3 video frame chunk sectors.
 *  Also covers FF7 (v1) frame chunk sectors, and Lain frame chunk sectors. */
public class PSXSectorAliceFrameChunk extends PSXSector 
        implements IVideoChunkSector 
{

    // .. Static stuff .....................................................

    public static final long VIDEO_CHUNK_MAGIC = 0x00000160;
    public static final int FRAME_CHUNK_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    protected long m_lngMagic;                //  0    [4 bytes - signed]
    protected long m_lngChunkNumber;          //  4    [2 bytes]
    protected long m_lngChunksInThisFrame;    //  6    [2 bytes]
    protected long m_lngFrameNumber;          //  8    [4 bytes]
    protected long m_lngChunkDuration;        //  12   [4 bytes] (frame duration?)
    // 16 bytes -- zeros?
    //   32 TOTAL
    // .. Constructor .....................................................

    public PSXSectorAliceFrameChunk(CDXASector oCDSect)
    throws NotThisTypeException {
        super(oCDSect);
        if (  oCDSect.hasSectorHeader() && 
            ( (oCDSect.getSubMode().video != 1) &&
              (oCDSect.getSubMode().data  != 1) ) ) 
        {
            throw new NotThisTypeException();
        }
        try {
            ReadHeader(oCDSect.getSectorDataStream());
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    protected void ReadHeader(ByteArrayInputStream oBAIS)
            throws NotThisTypeException, IOException 
    {
        m_lngMagic = IO.ReadUInt32LE(oBAIS);
        if (m_lngMagic != VIDEO_CHUNK_MAGIC)
            throw new NotThisTypeException();

        m_lngChunkNumber = IO.ReadUInt16LE(oBAIS);
        m_lngChunksInThisFrame = IO.ReadUInt16LE(oBAIS);
        // Note that reading these 32 bit values could overflow to -1
        // if that should happen, then this couldn't be a frame chunk
        m_lngFrameNumber = IO.ReadUInt32LE(oBAIS);
        if (m_lngFrameNumber == 0xFFFF)
            throw new NotThisTypeException(); // a null frame between movies
        
        m_lngFrameNumber &= 0x3FFF; // the high bit signifys the end of a video
        
        m_lngChunkDuration = IO.ReadUInt32LE(oBAIS);
        
        for (int i = 0; i < 16; i++)
            if (oBAIS.read() != 0)
                throw new NotThisTypeException();

    }

    // .. Public functions .................................................

    public String toString() {
        return "Alice " + super.toString() +
            String.format(
            " frame:%d chunk:%d/%d" + 
            "{dur=%d}",
            m_lngFrameNumber,
            m_lngChunkNumber,
            m_lngChunksInThisFrame,
            m_lngChunkDuration
            );
    }

    public long getChunkNumber() {
        return m_lngChunkNumber;
    }

    public long getChunksInFrame() {
        return m_lngChunksInThisFrame;
    }

    public long getFrameNumber() {
        return m_lngFrameNumber;
    }

    public long getWidth() {
        return 320;
    }

    public long getHeight() {
        return 224;
    }

    /** Want to skip the frame header */
    protected int getDemuxedDataStart(int iDataSize) {
        return FRAME_CHUNK_HEADER_SIZE;
    }
    protected int getDemuxedDataLength(int iDataSize) {
        return 2048 - FRAME_CHUNK_HEADER_SIZE;
    }

}

