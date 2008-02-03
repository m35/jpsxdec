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
 * FF8and9AudioDemuxerDecoderIS
 */

package jpsxdec.audiodecoding;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IO.Short2DArrayInputStream;

/** Conceniently, Final Fantasy 8 and Final Fantasy 9 store the audio in the
 *  same format. */
public class FF8and9AudioDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Iterate through the sectors of the disc. */
    private AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;

    /** Buffer to hold the current decoded PCM data that will be sent out
     *  through the read() functon. This will be null when there is no
     *  more data to be read. */
    private Short2DArrayInputStream m_oCurrentDecodedBuffer;
    
    /** Decoding context for the left channel. */
    private ADPCMDecodingContext m_oLeftContext;
    /** Decoding context for the right channel. */
    private ADPCMDecodingContext m_oRightContext;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public FF8and9AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException
    {
        this(oPsxIter, 1.0f);
    }
    public FF8and9AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     double dblScale)
            throws IOException
    {
        m_oPsxSectorIterator = oPsxIter;
        
        // FF8/FF9 audio is always in stereo, so create the
        // ADPCM contexts for left and right channels.
        m_oLeftContext = new ADPCMDecodingContext(dblScale);
        m_oRightContext = new ADPCMDecodingContext(dblScale);
        
        short[][] asiDecoded = FindAndDecodeNextFrame(m_oPsxSectorIterator);

        if (asiDecoded == null)
            m_oCurrentDecodedBuffer = null;
        else
            m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public AudioFormat getFormat() {
        return new AudioFormat(
                44100,     // FF8/FF9 audio is always this frequency
                16,        // we're turning it into a 16 bit signed pcm
                2,         // stereo
                true,      // signed
                false);    // little-endian (not big-endian)            
    }

    public long getLength() {
        // I'd have to pre-calculate the length during indexing,
        // but I don't really want to.
        return AudioSystem.NOT_SPECIFIED;
    }

    /** Returns true if there is audio remaining to be read. */
    public boolean hasAudio() {
        return m_oCurrentDecodedBuffer != null;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int read() throws IOException {
        
        if (m_oCurrentDecodedBuffer == null) return -1;
        
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            short[][] asiDecoded = FindAndDecodeNextFrame(m_oPsxSectorIterator);

            if (asiDecoded == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            } else {
                m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
            }
            
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        return iByte;
    }

   
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    
    private short[][] FindAndDecodeNextFrame(AdvancedIOIterator<PSXSector> oIter) throws IOException {

        IFF8or9AudioSector oAudioSect;
        oAudioSect = FindNextMatchingSector(oIter, 1);
        if (oAudioSect == null) return null;
        short[][] asi = new short[2][];
        asi[0] = StrADPCMDecoder.DecodeMoreFF8(new DataInputStream((PSXSector)oAudioSect),
                                                m_oLeftContext);
        oAudioSect = FindNextMatchingSector(oIter, 2);
        if (oAudioSect == null) return null;
        asi[1] = StrADPCMDecoder.DecodeMoreFF8(new DataInputStream((PSXSector)oAudioSect),
                                                m_oRightContext);
        return asi;
    }

    /** Find the next left or right channel audio sector.
     * @param iLeftRight  Search for either the next left or right channel */
    private IFF8or9AudioSector FindNextMatchingSector(AdvancedIOIterator<PSXSector> oPsxSectorIter, int iLeftRight) throws IOException {

        while (oPsxSectorIter.hasNext()) {

            PSXSector oSector = oPsxSectorIter.next();

            if (!(oSector instanceof IFF8or9AudioSector)) continue;


            IFF8or9AudioSector oAudioSect = (IFF8or9AudioSector)oSector;

            if (oAudioSect.isLeftOrRightChannel() == iLeftRight) 
                return oAudioSect;
        }

        return null;
    }


}
