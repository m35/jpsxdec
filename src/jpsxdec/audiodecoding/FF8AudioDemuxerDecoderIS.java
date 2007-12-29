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
 * StrAudioDemuxerDecoderIS.java
 */

package jpsxdec.audiodecoding;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import jpsxdec.*;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.util.AdvancedIOIterator;
import jpsxdec.util.IO.Short2DArrayInputStream;
import jpsxdec.audiodecoding.StrADPCMDecoder.ADPCMDecodingContext;

public class FF8AudioDemuxerDecoderIS extends InputStream {

    public static int DebugVerbose = 2;

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private AdvancedIOIterator<PSXSector> m_oPsxSectorIterator;
    private Short2DArrayInputStream m_oCurrentDecodedBuffer;
    private double m_dblScale;
    private ADPCMDecodingContext m_oLeftContext;
    private ADPCMDecodingContext m_oRightContext;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public FF8AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter) 
            throws IOException
    {
        this(oPsxIter, 1.0f);
    }
    public FF8AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel) 
            throws IOException
    {
        this(oPsxIter, iChannel, 1.0f);
    }
    public FF8AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     double dblScale) 
            throws IOException
    {
        this(oPsxIter, -1, dblScale);
    }

    public FF8AudioDemuxerDecoderIS(AdvancedIOIterator<PSXSector> oPsxIter, 
                                     int iChannel,
                                     double dblScale)
            throws IOException
    {
        m_dblScale = dblScale;
        m_oPsxSectorIterator = oPsxIter;
        
        // we already know that FF8 audio is in stereo, so create the
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
                44100,     // FF8 audio is always this frequency
                16,        // we're turning it into a 16 bit signed pcm
                2,         // stereo
                true,      // signed
                false);    // little-endian (not big-endian)            
    }

    public long getLength() {
        // since we're decoding one sector at a time, there's
        // no way to determine the length ahead of time
        //System.out.println(AudioSystem.NOT_SPECIFIED);
        return AudioSystem.NOT_SPECIFIED;
    }

    public boolean HasAudio() {
        return m_oCurrentDecodedBuffer != null;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    int m_iBytesRead = 0;
    public int read() throws IOException {
        
        if (m_oCurrentDecodedBuffer == null) return -1;
        
        int iByte = m_oCurrentDecodedBuffer.read();
        while (iByte < 0)
        {
            if (DebugVerbose > 5)
                System.out.println("Bytes read from sector: " + m_iBytesRead);
            
            short[][] asiDecoded = FindAndDecodeNextFrame(m_oPsxSectorIterator);

            if (asiDecoded == null) {
                m_oCurrentDecodedBuffer = null;
                return -1;
            } else {
                m_oCurrentDecodedBuffer = new Short2DArrayInputStream(asiDecoded);
            }
            
            m_iBytesRead = 0;
            iByte = m_oCurrentDecodedBuffer.read(); // try again
        }
        m_iBytesRead++;
        return iByte;
    }

   
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    
    private short[][] FindAndDecodeNextFrame(AdvancedIOIterator<PSXSector> oIter) throws IOException {

            PSXSectorFF8AudioChunk oAudioSect;
            oAudioSect = FindNextMatchingSector(oIter, 1);
            if (oAudioSect == null) return null;
            short[][] asi = new short[2][];
            asi[0] = StrADPCMDecoder.DecodeMoreFF8(new DataInputStream(oAudioSect),
                                                    m_oLeftContext);
            oAudioSect = FindNextMatchingSector(oIter, 2);
            if (oAudioSect == null) return null;
            asi[1] = StrADPCMDecoder.DecodeMoreFF8(new DataInputStream(oAudioSect),
                                                    m_oRightContext);
            return asi;
    }

    /** Find the next left or right channel audio sector.
     * @param iLeftRight  Search for either the left or right channel */
    private PSXSectorFF8AudioChunk FindNextMatchingSector(AdvancedIOIterator<PSXSector> oPsxSectorIter, int iLeftRight) throws IOException {

        while (oPsxSectorIter.hasNext()) {

            PSXSector oSector = oPsxSectorIter.next();

            if (!(oSector instanceof PSXSectorFF8AudioChunk)) continue;


            PSXSectorFF8AudioChunk oAudioSect = (PSXSectorFF8AudioChunk)oSector;

            if (oAudioSect.getLeftRightChannel() == iLeftRight) 
                return oAudioSect;
        }

        return null;
    }


}
