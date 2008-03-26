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
 * PSXSectorChronoXAudio.java
 */

package jpsxdec.sectortypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Audio sectors used in Chrono Cross movies. Nearly identical to FF9
 *  audio sectors. */
public class PSXSectorChronoXAudio extends PSXSector 
        implements ISquareAudioSector
{
    // .. Static stuff .....................................................

    public static final int AUDIO_CHUNK_MAGIC1 = 0x00000160;
    public static final int AUDIO_CHUNK_MAGIC2 = 0x00010160;
    public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

    // .. Instance .........................................................

    private long m_lngMagic;                  //  0    [4 bytes]
    private long m_lngAudioChunkNumber;       //  4    [2 bytes]
    private long m_lngAudioChunksInFrame;     //  6    [2 bytes]
    private long m_lngFrameNumber;            //  8    [2 bytes]
    // 118 bytes unknown
    protected SquareAKAOstruct m_oAKAOstruct; //       [36 bytes]
    // 44 bytes; unknown
    //   208 TOTAL

    public PSXSectorChronoXAudio(CDXASector oCDSect) throws NotThisTypeException {
        super(oCDSect);

        // since all Chrono Cross movie sectors are in Form 1, we can still
        // decode the movie even if there is no header.
        if (oCDSect.hasSectorHeader() &&
            ( oCDSect.getSubMode().data != 1 ||
              oCDSect.getSubMode().form != 1 )) 
            throw new NotThisTypeException();

        try {
            ByteArrayInputStream oIS = oCDSect.getSectorDataStream();

            m_lngMagic = IO.ReadUInt32LE(oIS);
            
            // make sure the magic nubmer is correct
            if (m_lngMagic != AUDIO_CHUNK_MAGIC1 && 
                m_lngMagic != AUDIO_CHUNK_MAGIC2)
                throw new NotThisTypeException();

            m_lngAudioChunkNumber = IO.ReadUInt16LE(oIS);
            m_lngAudioChunksInFrame = IO.ReadUInt16LE(oIS);
            m_lngFrameNumber = IO.ReadUInt16LE(oIS);
            
            oIS.skip(118);

            m_oAKAOstruct = new SquareAKAOstruct(oIS);
            
            if (m_oAKAOstruct.AKAO != SquareAKAOstruct.AKAO_ID)
                throw new NotThisTypeException();
            
            //oIS.skip(44);

        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    public long getAudioChunkNumber() {
        return m_lngAudioChunkNumber;
    }

    public long getAudioChunksInFrame() {
        return m_lngAudioChunksInFrame;
    }

    public long getFrameNumber() {
        return m_lngFrameNumber;
    }
    // .. Public functions .................................................

    public String toString() {
        return "ChronoXAud " + super.toString() +
            String.format(
            " frame:%d chunk:%d/%d %s",
            m_lngFrameNumber,
            m_lngAudioChunkNumber,
            m_lngAudioChunksInFrame,
            m_oAKAOstruct.toString()
            );
    }

    public int getAudioDataSize() {
        return (int)m_oAKAOstruct.BytesOfData;
    }

    public boolean hasAudio() {
        return !(getAudioDataSize() == 0);
    }

    public int getSamplesPerSecond() {
        return 44100;
    }



    /** Want to skip the frame header */
    protected int getDemuxedDataStart(int iDataSize) {
        return FRAME_AUDIO_CHUNK_HEADER_SIZE;
    }
    protected int getDemuxedDataLength(int iDataSize) {
        return (int)m_oAKAOstruct.BytesOfData; // 1680
    }


}
