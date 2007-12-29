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
 */

package jpsxdec.audiodecoding;

import java.io.*;
import java.nio.BufferOverflowException;
import javax.sound.sampled.*;

public final class StrADPCMDecoder {
    
    /** Holds the decoded data, plus maintains the Previous 2 PCM samples,
     *  necessary for ADPCM decoding. Only one context needed if decoding
     *  mono audio. If stereo, need one context for the left, and one for
     *  the right channel.
     *  Writing works like ByteArrayOutputStream, but with shorts. */
    public static class ADPCMDecodingContext {
        double m_dblScale;
        
        int m_iPos;
        short[] m_asiArray;
        
        private double PreviousPCMSample1 = 0;
        private double PreviousPCMSample2 = 0;
        
        public ADPCMDecodingContext(double dblScale) {
            m_dblScale = dblScale;
        }
    
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
        
        /** @param dblSample raw PCM sample, before rounding or clamping */
        public void writeSample(double dblSample) {
            if (m_iPos >= m_asiArray.length) 
                throw new BufferOverflowException();
            // Save the previous sample
            PreviousPCMSample2 = PreviousPCMSample1;
            PreviousPCMSample1 = dblSample;
            // scale, round, and clamp
            long lngSample = jpsxdec.util.Math.round(dblSample * m_dblScale);
            m_asiArray[m_iPos] = ClampPCM(lngSample);
            m_iPos++;
        }
        
        private short ClampPCM(long lngPCMSample) {
            if (lngPCMSample > 0x7FFF)
                return (short)0x7FFF;
            else if (lngPCMSample < -0x8000)
                return (short)0x8000;
            else
                return (short)lngPCMSample;
        }        

        public double getPreviousPCMSample1() {
            return PreviousPCMSample1;
        }

        public double getPreviousPCMSample2() {
            return PreviousPCMSample2;
        }
    }
    
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    /** Audio data on the PSX is encoded using
     *  Adaptive Differential Pulse Code Modulation (ADPCM).
     * 
     * A full sector will decode to 4032/channels samples (or 8064 bytes).
     * Returns 4032 samples/channels shorts (or 8064 bytes).
     *
     * @return an array of short[channels][samples].
     */
    public static short[][] DecodeMore(DataInputStream oStream, 
                                       int iBitsPerSample, 
                                       int iMonoStereo,
                                       ADPCMDecodingContext oLeftOrMonoContext,
                                       ADPCMDecodingContext oRightContext)
    {
        assert(iBitsPerSample == 4 || iBitsPerSample == 8); 
        assert(iMonoStereo == 1 || iMonoStereo == 2);
        
        int aiSoundUnits[][];
        int aiSoundParams[];
        if (iBitsPerSample == 8) {
            aiSoundUnits = new int[4][28]; // 4 sound units when 8 bits/sample
            aiSoundParams = new int[4]; // a sound parameter for each sound unit
        } else {
            aiSoundUnits = new int[8][28]; // 8 sound units when 4 bits/sample
            aiSoundParams = new int[8]; // a sound parameter for each sound unit
        }
        
        oLeftOrMonoContext.setArray(4032 / iMonoStereo);
        if (iMonoStereo == 2)
            oRightContext.setArray(4032 / iMonoStereo);
        
        int iByte = 0; // hold a byte read from the stream
        
        try {
            
            /* There are 18 sound groups, 
             * each having 16 bytes of sound parameters,
             * and 112 bytes of ADPCM data ( 18*(16+112) = 2304 ) */
            for (int iSoundGroup = 0; iSoundGroup < 18; iSoundGroup++) {
                
                // Process the 16 byte sound parameters at the 
                // start of each sound group
                if (iBitsPerSample == 8) {
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
                        if (iBitsPerSample == 8) {
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
                    
                    if (iSoundUnit % iMonoStereo == 0)
                        DecodeSoundUnit(
                                aiSoundUnits[iSoundUnit], 
                                aiSoundParams[iSoundUnit],
                                oLeftOrMonoContext,
                                iBitsPerSample);
                    else
                        DecodeSoundUnit(
                                aiSoundUnits[iSoundUnit], 
                                aiSoundParams[iSoundUnit],
                                oRightContext,
                                iBitsPerSample);
                }
            }
        
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // put the decoded array of shorts into an array
        short[][] asiRet = new short[iMonoStereo][];
        
        asiRet[0] = oLeftOrMonoContext.getArray();
        if (iMonoStereo == 2)
            asiRet[1] = asiRet[1] = oRightContext.getArray();
        
        return asiRet;
    }
    
    // ........................................................................
    
    private static void DecodeSoundUnit(int[] aiADPCMSoundUnit, 
                                        int iSoundParam, 
                                        ADPCMDecodingContext oContext,
                                        int iBitsPerSample) 
    {
        assert(aiADPCMSoundUnit.length == 28);
        
        int    iADPCM_sample; // unsigned byte
        double dblPCM_sample;
        // there are 28 ADPCM samples in each sound unit
        for (int iSoundSample = 0; iSoundSample < 28; iSoundSample++) {

            iADPCM_sample = aiADPCMSoundUnit[iSoundSample];

            dblPCM_sample = ADPCMtoPCM(iBitsPerSample,                                            
                                       iSoundParam, 
                                       iADPCM_sample, 
                                       oContext.getPreviousPCMSample1(), 
                                       oContext.getPreviousPCMSample2());

            oContext.writeSample(dblPCM_sample);

        }
        
    }
    
    // ........................................................................
    
    /** Standard K0 multiplier (don't ask me, it's just how it is) */
    final private static double K0[] = new double[] {
        0.0,
        0.9375,
        1.796875,
        1.53125
    };
    /** Standard K1 multiplier (don't ask me, it's just how it is) */
    final private static double K1[] = new double[] {
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
        } else /* (iBitsPerSample == 4) */ {
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
    
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    
    public static short[] DecodeMoreFF8(InputStream oStream,
                                        ADPCMDecodingContext oContext)
            throws IOException
    {
        oContext.setArray(2940);
        
        for (int iSoundGroup = 0; iSoundGroup < 105; iSoundGroup++) {
            int iSoundParameter1 = oStream.read();
            if (iSoundParameter1 < 0) 
                throw new EOFException("Unexpected end of audio data");
            
            if (oStream.skip(1) != 1)
                throw new EOFException("Unexpected end of audio data");
            
            for (int iSoundUnit = 0; iSoundUnit < 14; iSoundUnit++) {
                int i = oStream.read();
                if (i < 0) throw new EOFException("Unexpected end of audio data");
                int iADPCMSample1 = i & 0x0F;
                int iADPCMSample2 = (i >>> 4) & 0x0F;
                
                double dblPCMSample;
                dblPCMSample = FF8_ADPCMtoPCM(iSoundParameter1, iADPCMSample1,
                        oContext.getPreviousPCMSample1(), 
                        oContext.getPreviousPCMSample2());
    
                oContext.writeSample(dblPCMSample);
                
                dblPCMSample = FF8_ADPCMtoPCM(iSoundParameter1, iADPCMSample2,
                        oContext.getPreviousPCMSample1(), 
                        oContext.getPreviousPCMSample2());
    
                oContext.writeSample(dblPCMSample);
            }
        }
        
        return oContext.getArray();
    }
    
    // ........................................................................
    
    /** FF8 K0 multiplier (don't ask me, it's just how it is) */
    final private static double FF8_K0[] = new double[] {
        0.0,
        0.9375,
        1.796875,
        1.53125,
        1.90625
    };
    /** FF8 K1 multiplier (don't ask me, it's just how it is) */
    final private static double FF8_K1[] = new double[] {
        0.0,
        0.0,
        -0.8125,
        -0.859375,
        -0.9375
    };
    
    private static double FF8_ADPCMtoPCM(
                                     int iSoundParameter, 
                                     int iSample, 
                                     double dblPreviousPCMSample1, 
                                     double dblPreviousPCMSample2) 
    {
        byte bRange =  (byte)(iSoundParameter & 0xF);
        byte bFilterIndex = (byte)((iSoundParameter >>> 4) & 0xF);

        
        // shift the nibble into the top of a short
        long lngResult = (short)(iSample << 12);
        
        // shift sound data according to the range, keeping the sign
        lngResult = (lngResult >> bRange); 
        
        // adjust according to the filter
        double dblresult = 
            (lngResult + FF8_K0[bFilterIndex] * dblPreviousPCMSample1 
                       + FF8_K1[bFilterIndex] * dblPreviousPCMSample2);
        
        return dblresult;
    }
    
    

}
