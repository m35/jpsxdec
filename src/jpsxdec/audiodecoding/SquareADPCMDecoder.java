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
 * SquareADPCMDecoder.java
 */

package jpsxdec.audiodecoding;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;

/** Decodes Square's unique ADPCM audio data. */
public final class SquareADPCMDecoder {
    
    /** Decodes a requested amount of audio data from the input stream, 
     * and writes it into the supplied ADPCMDecodingContext. */
    public static short[] DecodeMore(InputStream oStream,
                                     ADPCMDecodingContext oContext,
                                     int iNumBytes)
            throws IOException
    {
        if ((iNumBytes & 0xF) > 0) 
            throw new IllegalArgumentException("iNumBytes must be divisible by 16");
        int iNumSoundGrp = iNumBytes / 16;
        int iNumSamples = iNumSoundGrp * 28;
        oContext.setArray(iNumSamples);
        
        for (int iSoundGroup = 0; iSoundGroup < iNumSoundGrp; iSoundGroup++) {
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
                dblPCMSample = ADPCMtoPCM(iSoundParameter1, iADPCMSample1,
                        oContext.getPreviousPCMSample1(), 
                        oContext.getPreviousPCMSample2());
    
                oContext.writeSample(dblPCMSample);
                
                dblPCMSample = ADPCMtoPCM(iSoundParameter1, iADPCMSample2,
                        oContext.getPreviousPCMSample1(), 
                        oContext.getPreviousPCMSample2());
    
                oContext.writeSample(dblPCMSample);
            }
        }
        
        return oContext.getArray();
    }
    
    
    // ........................................................................
    
    /** Square's K0 multiplier (don't ask me, it's just how it is) */
    final private static double K0[] = new double[] {
        0.0,
        0.9375,
        1.796875,
        1.53125,
        1.90625
    };
    /** Square's K1 multiplier (don't ask me, it's just how it is) */
    final private static double K1[] = new double[] {
        0.0,
        0.0,
        -0.8125,
        -0.859375,
        -0.9375
    };
    
    /** 
     * 
     * @param iSoundParameter  the range in the bottom 4 bits, and filter index in the top 4 bits.
     * @param iSample  a 4 bit sample.
     * @param dblPreviousPCMSample1  previous PCM sample.
     * @param dblPreviousPCMSample2  previous previous PCM sample.
     * @return  decoded PCM sample.
     */
    private static double ADPCMtoPCM(int iSoundParameter, 
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
            (lngResult + K0[bFilterIndex] * dblPreviousPCMSample1 
                       + K1[bFilterIndex] * dblPreviousPCMSample2);
        
        return dblresult;
    }

}
