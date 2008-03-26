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
public class PSXSectorFrameChunk extends PSXSector 
        implements IVideoChunkSector 
{

    // .. Static stuff .....................................................

    public static final long VIDEO_CHUNK_MAGIC = 0x80010160;
    public static final int FRAME_CHUNK_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    protected long m_lngMagic;                //  0    [4 bytes - signed]
    protected long m_lngChunkNumber;          //  4    [2 bytes]
    protected long m_lngChunksInThisFrame;    //  6    [2 bytes]
    protected long m_lngFrameNumber;          //  8    [4 bytes]
    protected long m_lngChunkDuration;        //  12   [4 bytes] (frame duration?)
    protected long m_lngWidth;                //  16   [2 bytes]
    protected long m_lngHeight;               //  18   [2 bytes]
    protected long m_lngRunLengthCodeCount;   //  20   [2 bytes]
    protected long m_lngHeader3800;           //  22   [2 bytes] always 0x3800
    protected long m_lngQuantizationScale;    //  24   [2 bytes]
    protected long m_lngVersion;              //  26   [2 bytes]
    protected long m_lngFourZeros;            //  28   [4 bytes]
    //   32 TOTAL
    // .. Constructor .....................................................

    public PSXSectorFrameChunk(CDXASector oCDSect)
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
        if (m_lngFrameNumber == -1) // check for overflow
            throw new NotThisTypeException();

        // We aren't verifying this value because we also want to accept
        // FF7 frame chunk sectors.
        m_lngChunkDuration = IO.ReadUInt32LE(oBAIS);
        //if (m_lngChunkDuration == -1) // check for overflow
        //    throw new NotThisTypeException();

        m_lngWidth = IO.ReadUInt16LE(oBAIS);
        m_lngHeight = IO.ReadUInt16LE(oBAIS);
        m_lngRunLengthCodeCount = IO.ReadUInt16LE(oBAIS);

        // We aren't verifying this value because we also want to accept
        // FF7 frame chunk sectors.
        m_lngHeader3800 = IO.ReadUInt16LE(oBAIS);
        //if (m_lngHeader3800 != 0x3800)
        //    throw new NotThisTypeException();

        m_lngQuantizationScale = IO.ReadUInt16LE(oBAIS);
        m_lngVersion = IO.ReadUInt16LE(oBAIS);
        m_lngFourZeros = IO.ReadUInt32LE(oBAIS);
    }

    // .. Public functions .................................................

    public String toString() {
        return "Video " + super.toString() +
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
        return m_lngWidth;
    }

    public long getHeight() {
        return m_lngHeight;
    }

    /** Want to skip the frame header */
    protected int getDemuxedDataStart(int iDataSize) {
        return FRAME_CHUNK_HEADER_SIZE;
    }
    protected int getDemuxedDataLength(int iDataSize) {
        return 2048 - FRAME_CHUNK_HEADER_SIZE;
    }

}

