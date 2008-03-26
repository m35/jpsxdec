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

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;

/** Decodes a requested amount of audio data, and puts it into the supplied
 *  ADPCMDecodingContext. */
public final class StrADPCMDecoder {
    
    /** The number of samples in a normal ADPCM audio sector = 4032. */
    private static final int SAMPLES_IN_NORMAL_SECTOR = 4032;
    
    /** Decodes a sector's worth of ADPCM data.
     * 
     * Audio data on the PSX is encoded using
     * Adaptive Differential Pulse Code Modulation (ADPCM).
     * 
     * A full sector will decode to 4032/channels samples (or 8064 bytes).
     * Returns 4032 samples/channels shorts (or 8064 bytes).
     *
     * @param iBitsPerSample  ADPCM bits per sample: either 4 or 8
     * @param iMonoStereo  1 for mono, 2 for stereo
     * @param oLeftOrMonoContext  Decoding context for left/mono channel.
     * @param oRightContext decoding context for right channel, or null if mono.
     * @return an array of short[channels][samples].
     */
    public static short[][] DecodeMore(InputStream oStream, 
                                       int iBitsPerSample, 
                                       int iMonoStereo,
                                       ADPCMDecodingContext oLeftOrMonoContext,
                                       ADPCMDecodingContext oRightContext)
    {
        if (iBitsPerSample != 4 && iBitsPerSample != 8) 
            throw new IllegalArgumentException("bits/sample must be either 4 or 8");
        if (iMonoStereo != 1 && iMonoStereo != 2) 
            throw new IllegalArgumentException("audio must be mono or stereo");
        
        int aiSoundUnits[][];
        int aiSoundParams[]; // sound parameters
        if (iBitsPerSample == 8) {
            aiSoundUnits  = new int[4][28]; // 4 sound units when 8 bits/sample
            aiSoundParams = new int[4]; // a sound parameter for each sound unit
        } else {
            aiSoundUnits  = new int[8][28]; // 8 sound units when 4 bits/sample
            aiSoundParams = new int[8]; // a sound parameter for each sound unit
        }
        
        oLeftOrMonoContext.setArray(SAMPLES_IN_NORMAL_SECTOR / iMonoStereo);
        if (iMonoStereo == 2)
            oRightContext.setArray(SAMPLES_IN_NORMAL_SECTOR / iMonoStereo);
        
        try {
            
            /* There are 18 sound groups, 
             * each having 16 bytes of sound parameters,
             * and 112 bytes of ADPCM data ( 18*(16+112) = 2304 ) */
            for (int iSoundGroup = 0; iSoundGroup < 18; iSoundGroup++) {
                
                // Process the 16 byte sound parameters at the 
                // start of each sound group
                ReadSoundParameters(oStream, iBitsPerSample, aiSoundParams);

                // read, and de-interleave 112 bytes of inerleaved sound units
                ReadInterleavedSoundUnits(oStream, iBitsPerSample, aiSoundUnits);
                
                // by our sound parameters and 
                // de-interleaved sound units combined, 
                // we form PCM data!
                for (int iSoundUnit = 0; 
                     iSoundUnit < aiSoundUnits.length; 
                     iSoundUnit++) 
                {
                    // toggle back and forth between left and right channel
                    // (but only if stereo)
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
        
        // put the decoded array(s) of shorts into an array of mono or stereo
        short[][] asiRet = new short[iMonoStereo][];
        asiRet[0] = oLeftOrMonoContext.getArray();
        if (iMonoStereo == 2)
            asiRet[1] = oRightContext.getArray();
        
        return asiRet;
    }
    
    // ........................................................................
    
    /** Read the 4 or 8 sound parameteres (depending on bits-per-sample, 
     * one for each sound unit) found in the 16 bytes at the
     *  start of each sound group. */
    private static void ReadSoundParameters(InputStream oStream,
                                            int iBitsPerSample,
                                            int[] aiSoundParams)
            throws IOException
    {
        int iByte;
        
        // Process the 16 byte sound parameters at the 
        // start of each sound group
        if (iBitsPerSample == 8) {
            // the 4 sound parameters (one for each sound unit)
            // are repeated four times and are ordered like this:
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
        } else { // iBitsPerSample == 4
            // the 8 sound parameters (one for each sound unit)
            // are repeated twice, and are ordered like this:
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
        
    }
    
    // ........................................................................
    
    /** 
     * Sound units are interleaved like this:
     * <pre>
     * 8 bits-per-sample
     *      byte 1: sound unit 0
     *      byte 2: sound unit 1
     *      byte 3: sound unit 2
     *      byte 4: sound unit 3
     *      byte 5: sound unit 0
     *      ...
     * 4 bits-per-sample
     *      byte 1: sound unit 1 | sound unit 0
     *      byte 2: sound unit 3 | sound unit 2
     *      byte 3: sound unit 5 | sound unit 4
     *      byte 4: sound unit 7 | sound unit 6
     *      byte 5: sound unit 1 | sound unit 0
     *      ...
     * </pre>
     */
    private static void ReadInterleavedSoundUnits(InputStream oStream,
                                                  int iBitsPerSample,
                                                  int[][] aiSoundUnits) 
            throws IOException
    {
        int iByte;
        
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
                    // two sound units per byte
                    aiSoundUnits[iSoundUnit][iSoundSample] = 
                            iByte & 0xF;
                    iSoundUnit++;
                    aiSoundUnits[iSoundUnit][iSoundSample] = 
                            (iByte >>> 4) & 0xF;
                }
            }
        }
    }
    
    // ........................................................................
    
    /** Taking de-interleaved sound unit data, the sound parameter for that
     *  unit, and the decoding context (for the previous 2 samples read),
     *  we can decode an entire sound unit. 
     * (Bits-per-sample will be passed on to ADPCMtoPCM()) */
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
    
}
