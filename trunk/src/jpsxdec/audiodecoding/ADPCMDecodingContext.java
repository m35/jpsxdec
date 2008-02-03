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
 * ADPCMDecodingContext.java
 */

package jpsxdec.audiodecoding;

import java.nio.BufferOverflowException;

/** Holds the decoded data, plus maintains the Previous 2 PCM samples,
 *  necessary for ADPCM decoding. Only one context needed if decoding
 *  mono audio. If stereo, need one context for the left, and one for
 *  the right channel.
 *  Writing works like ByteArrayOutputStream, but with shorts. */
public class ADPCMDecodingContext {
    
    /* ---------------------------------------------------------------------- */
    /* -- Fields ------------------------------------------------------------ */
    /* ---------------------------------------------------------------------- */
    
    double m_dblScale;
    
    /** Position in the buffer being written to. */
    int m_iPos;
    /** Buffer to hold the decoded PCM data */
    short[] m_asiDecodedPCM;
    
    private double m_dblPreviousPCMSample1 = 0;
    private double m_dblPreviousPCMSample2 = 0;
    
    /* ---------------------------------------------------------------------- */
    /* -- Construtor -------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Create new ADPCMDecodingContext.
     * @param dblScale Scale the decoded audio before clamping. */
    public ADPCMDecodingContext(double dblScale) {
        m_dblScale = dblScale;
    }

    /* ---------------------------------------------------------------------- */
    /* -- Functions --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Pre-allocate the buffer to hold the decoded PCM audio.
     * @param lngSampleCount  Number of samples to pre-allocate. */
    public void setArray(long lngSampleCount) {
        assert(lngSampleCount > 0 && lngSampleCount < 0xFFFF);
        m_iPos = 0;
        m_asiDecodedPCM = new short[(int) lngSampleCount];
    }
    
    /** Returns the decoded audio, then clears the internal buffer. 
     * @return decoded PCM audio.*/
    public short[] getArray() {
        short[] asi = m_asiDecodedPCM; // get the reference to the audio
        m_asiDecodedPCM = null; // delete this reference to it
        return asi; // and return it
    }
    
    /** @param dblSample raw PCM sample, before rounding or clamping */
    public void writeSample(double dblSample) {
        if (m_iPos >= m_asiDecodedPCM.length)
            throw new BufferOverflowException();
        // Save the previous sample
        m_dblPreviousPCMSample2 = m_dblPreviousPCMSample1;
        m_dblPreviousPCMSample1 = dblSample;
        // scale, round, and clamp
        long lngSample = jpsxdec.util.Math.round(dblSample * m_dblScale);
        m_asiDecodedPCM[m_iPos] = ClampPCM(lngSample);
        m_iPos++;
    }
    
    private short ClampPCM(long lngPCMSample) {
        if (lngPCMSample > 0x7FFF)
            return (short) 32767;
        else            if (lngPCMSample < -0x8000)
                return (short) 32768;
            else
                return (short) lngPCMSample;
    }        

    public double getPreviousPCMSample1() {
        return m_dblPreviousPCMSample1;
    }

    public double getPreviousPCMSample2() {
        return m_dblPreviousPCMSample2;
    }
}