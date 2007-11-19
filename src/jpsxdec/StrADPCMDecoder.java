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
 * StrADPCMDecoder.java
 *
 */

package jpsxdec;

import java.io.*;
import javax.sound.sampled.*;

public final class StrADPCMDecoder {
    
    /** Holds the decoded data, plus maintains the Previous 2 PCM samples,
     *  necessary for ADPCM decoding. Only one context needed if decoding
     *  mono audio. If stereo, need one context for the left, and one for
     *  the right channel.
     *  Writing works like ByteArrayOutputStream, but with shorts. */
    private static class ADPCMDecodingContext {
        int m_iPos;
        short[] m_asiArray;
        
        public double PreviousPCMSample1 = 0;
        public double PreviousPCMSample2 = 0;
    
        public void setArray(long lngSampleCount) {
            assert(lngSampleCount > 0 && lngSampleCount < 0xFFFF);
            m_iPos = 0;
            m_asiArray = new short[(int)lngSampleCount];
        }
        
        public short[] getArray() {
            short[] asi = m_asiArray;
            m_asiArray = null;
            return asi;
        }
        
        public void write(short si) {
            assert(m_iPos < m_asiArray.length);
            m_asiArray[m_iPos] = si;
            m_iPos++;
        }
    }
    
    //;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    
    
    private int m_iBitsPerSample;
    private int m_iMonoStereo;
    private float m_fltScale;
    private ADPCMDecodingContext[] m_oLeftRightContexts;
    
    
    /** @param iMonoStereo - 1 for mono, 2 for stereo */
    public StrADPCMDecoder(int iBitsPerSample, int iMonoStereo, float fltScale ) 
    {
        this.m_iBitsPerSample = iBitsPerSample;
        this.m_iMonoStereo = iMonoStereo;
        this.m_fltScale = fltScale; 
        // Create a context for mono, or for left channel & right channel
        m_oLeftRightContexts = new ADPCMDecodingContext[iMonoStereo];
        for (int i = 0; i < m_iMonoStereo; i++) {
            m_oLeftRightContexts[i] = new ADPCMDecodingContext();
        }
    }
    
    
    public int getBitsPerSample() {
        return m_iBitsPerSample;
    }

    /** Returns 1 if the audio is mono, or 2 if the audio is stereo */
    public int getMonoStereo() {
        return m_iMonoStereo;
    }

    public float getScale() {
        return m_fltScale;
    }
    
    
    /** Audio data on the PSX is encoded using
     *  Adaptive Differential Pulse Code Modulation (ADPCM).
     * 
     * A full sector will decode to 4032/channels samples (or 8064 bytes).
     * Returns 4032 samples*channels shorts (or 8064 bytes).
     *
     * Returns an array of short[channels][samples].
     */
    public short[][] DecodeMore(DataInputStream oStream, 
                                long lngSampleCount) // number of samples expected
    {
        int aiSoundUnits[][];
        int aiSoundParams[];
        if (m_iBitsPerSample == 8) {
            aiSoundUnits = new int[4][28]; // 4 sound units when 8 bits/sample
            aiSoundParams = new int[4]; // a sound parameter for each sound unit
        } else {
            aiSoundUnits = new int[8][28]; // 8 sound units when 4 bits/sample
            aiSoundParams = new int[8]; // a sound parameter for each sound unit
        }
                
        for (int i = 0; i < m_oLeftRightContexts.length; i++) {
            m_oLeftRightContexts[i].setArray(lngSampleCount);
        }
        
        int iByte = 0; // hold a byte read from the stream
        
        try {
            
            /* There are 18 sound groups, 
             * each having 16 bytes of sound parameters,
             * and 112 bytes of ADPCM data ( 18*(16+112) = 2304 ) */
            for (int iSoundGroup = 0; iSoundGroup < 18; iSoundGroup++) {
                
                // Process the 16 byte sound parameters at the 
                // start of each sound group
                if (m_iBitsPerSample == 8) {
                    // the 4 sound parameters are ordered like this:
                    // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
                    
                    // so just read the first 4 parameters
                    for (int i = 0; i < 4; i++) { 
                        iByte = oStream.read();
                        if (iByte < 0) 
                         throw new EOFException("Unexpected end of audio data");
                        aiSoundParams[i] = iByte;
                    }
                    // and then skip the rest
                    if (oStream.skip(12) != 12)
                        throw new EOFException("Unexpected end of audio data");
                } else {
                    // the 8 sound parameters are ordered like this:
                    // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7
                    
                    // so skip the first 4 bytes
                    if (oStream.skip(4) != 4)
                        throw new EOFException("Unexpected end of audio data");
                    // read the 8 sound parameters
                    for (int i = 0; i < 8; i++) { 
                        iByte = oStream.read();
                        if (iByte < 0) 
                         throw new EOFException("Unexpected end of audio data");
                        aiSoundParams[i] = iByte;
                    }
                    // and skip the last 4 bytes
                    if (oStream.skip(4) != 4)
                        throw new EOFException("Unexpected end of audio data");
                }

                // read 112 bytes of inerleaved sound units
                for (int iSoundSample = 0; iSoundSample < 28; iSoundSample++) 
                {
                    for (int iSoundUnit = 0; 
                         iSoundUnit < aiSoundUnits.length; 
                         iSoundUnit++) 
                    {
                        iByte = oStream.read();
                        if (iByte < 0) 
                         throw new EOFException("Unexpected end of audio data");
                        // process either 8 bits or 2*4 bits 
                        // depending on iBitsPerSample
                        if (m_iBitsPerSample == 8) {
                            // sound unit bytes are interleaved like this
                            // 0,1,2,3, 0,1,2,3, 0,1,2,3 ...
                            aiSoundUnits[iSoundUnit][iSoundSample] = iByte;
                        } else {
                            // sound unit bytes are interleaved like this
                            // 1/0,3/2,5/4,7/6, 1/0,3/2,5/4,7/6, ...
                            aiSoundUnits[iSoundUnit][iSoundSample] = 
                                    iByte & 0xF;
                            iSoundUnit++;
                            aiSoundUnits[iSoundUnit][iSoundSample] = 
                                    (iByte >>> 4) & 0xF;
                        }
                    }
                }
                
                // now process the sound units in order
                for (int iSoundUnit = 0; 
                     iSoundUnit < aiSoundUnits.length; 
                     iSoundUnit++) 
                {
                    
                    DecodeSoundUnit(
                            aiSoundUnits[iSoundUnit], 
                            aiSoundParams[iSoundUnit],
                            m_oLeftRightContexts[iSoundUnit % m_iMonoStereo],
                            m_iBitsPerSample, m_fltScale);
                    
                }
            }
        
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // map the array of ShortArrayWriters to array of short[]
        short[][] asiRet = new short[m_iMonoStereo][];
        for (int i = 0; i < m_iMonoStereo; i++) {
            asiRet[i] = m_oLeftRightContexts[i].getArray();
        }
        
        return asiRet;
        
    }
    
    
    private static void DecodeSoundUnit(int[] aiADPCMSoundUnit, 
                                        int iSoundParam, 
                                        ADPCMDecodingContext oContext,
                                        int iBitsPerSample, 
                                        float fltScale) 
    {
        assert(aiADPCMSoundUnit.length == 28);
        
        int    iADPCM_sample; // unsigned byte
        double dblPCM_sample;
        long   lngPCM_sample;
        // there are 28 ADPCM samples in each sound unit
        for (int iSoundSample = 0; iSoundSample < 28; iSoundSample++) {

            iADPCM_sample = aiADPCMSoundUnit[iSoundSample];

            dblPCM_sample = ADPCMtoPCM(iBitsPerSample,                                            
                                       iSoundParam, 
                                       iADPCM_sample, 
                                       oContext.PreviousPCMSample1, 
                                       oContext.PreviousPCMSample2);

            oContext.PreviousPCMSample2 = oContext.PreviousPCMSample1;
            oContext.PreviousPCMSample1 = dblPCM_sample;

            // Scale it before 'clamping'
            // Tests revreal rounding provides the best quality
            lngPCM_sample = jpsxdec.util.Math.round(dblPCM_sample * fltScale);

            // "clamp"
            if(lngPCM_sample > 32767)
                oContext.write((short)32767);
            else if(lngPCM_sample < -32768)
                oContext.write((short)-32768);
            else
                oContext.write((short)lngPCM_sample);

        }
        
    }
    
    
    
    /** Standard K0 multiplier (don't ask me, it's just how it is) */
    final static double K0[] = new double[] {
        0.0,
        0.9375,
        1.796875,
        1.53125
    };
    /** Standard K1 multiplier (don't ask me, it's just how it is) */
    final static double K1[] = new double[] {
        0.0,
        0.0,
        -0.8125,
        -0.859375
    };
    
    private static double ADPCMtoPCM(int iBitsPerSample, 
                                    int iSoundParameter, 
                                    int iSample, 
                                    double dblPreviousPCMSample1, 
                                    double dblPreviousPCMSample2) 
    {
        long lngResult;
        
        byte bRange =  (byte)(iSoundParameter & 0xF);
        byte bFilter = (byte)((iSoundParameter >>> 4) & 0x3);

        if (iBitsPerSample == 8) {
            // shift the byte into the top of a short
            lngResult = (short)(iSample << 8);
        } else {
            // shift the nibble into the top of a short
            lngResult = (short)(iSample << 12);
        }
        
        // shift sound data according to the range, keeping the sign
        lngResult = (lngResult >> bRange); 
        
        // adjust according to the filter
        double dblresult = 
            (lngResult + K0[bFilter] * dblPreviousPCMSample1 
                       + K1[bFilter] * dblPreviousPCMSample2);
        
        return dblresult;
    }

}
