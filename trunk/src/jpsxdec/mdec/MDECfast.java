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
 * MDECfast.java
 */

package jpsxdec.mdec;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.IO;

public class MDECfast extends MDEC {

    
    private IDCT idct = new IDCT();
    
    private static final int[] PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX = 
    {
                 2, 16, 19, 22, 26, 27, 29, 34, 
                16, 16, 22, 24, 27, 29, 34, 37, 
                19, 22, 26, 27, 29, 34, 34, 38, 
                22, 22, 26, 27, 29, 34, 37, 40, 
                22, 26, 27, 29, 32, 35, 40, 48, 
                26, 27, 29, 32, 35, 40, 48, 58, 
                26, 27, 29, 34, 38, 46, 56, 69, 
                27, 29, 35, 38, 46, 56, 69, 83 
    };
    
    /** The order that the zig-zag vector is ordered. */
    private static final int[] REVERSE_ZIG_ZAG_SCAN_MATRIX =
    {
		 0,  1,  8, 16,  9,  2,  3, 10,
		17, 24, 32, 25, 18, 11,  4,  5,
		12, 19, 26, 33, 40, 48, 41, 34,
		27, 20, 13,  6,  7, 14, 21, 28,
		35, 42, 49, 56, 57, 50, 43, 36,
		29, 22, 15, 23, 30, 37, 44, 51,
		58, 59, 52, 45, 38, 31, 39, 46,
		53, 60, 61, 54, 47, 55, 62, 63
    };

    public MDECfast() {
        idct.norm(PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX);
    }
    
    @Override
    protected Matrix8x8 DecodeBlock(InputStream oStream) throws IOException {
        // the un-zig-zag, Dequantized matrix
        int[] iPreIDCTMatrix = new int[64];
        
        int iQuantizationScale; // block's Quantization scale
        int iDC_Coefficient;    // The DC coefficient of the block
        
        // Read the quantization scale and DC Coefficient
        // The data is stored: 6 bits for the quantization scale
        //                    10 bits for the DC coefficient
        long lngMdecWord = IO.ReadUInt16LE(oStream); // little-endian 16 bits read from the stream
        Mdec16Bits oRunLenCode = new Mdec16Bits(lngMdecWord);
        iQuantizationScale = oRunLenCode.Top6Bits;
        iDC_Coefficient = oRunLenCode.Bottom10Bits;
        
        ////////////////////////////////////////////////////////////////////////
        // Step 1,2,3) * Decode the run-length codes into a vector
        //             * Convert the zig-zag vector to a matrix
        //             * de-quanitize the matrix with the PSX matrix, and the
        //               block's quantization scale.
        ////////////////////////////////////////////////////////////////////////
        
        int iVectorPos = 1; // current position in the vector
        int iNonZeroValueCount = 0; // count of non-zero coefficients
        int iNonZeroValuePos = -1; // last non-zero coefficient
        
        // We can already dequanitize the DC coefficient
        iPreIDCTMatrix[0] = (int)(iDC_Coefficient * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[0]);
        if (iPreIDCTMatrix[0] != 0) {
            iNonZeroValueCount = 1;
            iNonZeroValuePos = 0;
        }
        
        lngMdecWord = IO.ReadUInt16LE(oStream); // read 16 bits
        
        while (lngMdecWord != MDEC_END_OF_BLOCK) {
        
            // The data is stored: 6 bits for the run length of zeros
            //                    10 bits for the value
            oRunLenCode = new Mdec16Bits(lngMdecWord);
            iVectorPos += oRunLenCode.Top6Bits; // skip the run length zeros
            
            // The decoded run length codes should never exceed 64
            if (iVectorPos > 63) {
                throw new MdecException("[MDEC] Run length out of bounds: " + 
                                       (iVectorPos + 1));
            }
            
            // Reverse Zig-Zag and Dequantize all at the same time
            int iRevZigZagPos = REVERSE_ZIG_ZAG_SCAN_MATRIX[iVectorPos];
            iPreIDCTMatrix[iRevZigZagPos] = (int)(
                    2 * oRunLenCode.Bottom10Bits
                      * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[iRevZigZagPos]
                      * iQuantizationScale / 16.0 );
            iVectorPos += 1;
            iNonZeroValueCount += 1;
            iNonZeroValuePos = iRevZigZagPos;
            
            // read the next 16 bits
            lngMdecWord = IO.ReadUInt16LE(oStream);
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Step 4) Finally, perform the inverse discrete cosine transform
        ////////////////////////////////////////////////////////////////////////
        double[] adbl = new double[64];
        
        if (iNonZeroValueCount == 0) { // if there were no non-zero coefficients
            // we can just return a zero double matrix
            adbl[0] = iPreIDCTMatrix[0]; // but need to copy the DC coefficient
        } else { 
            if (iNonZeroValueCount == 1) // if there is only one
                idct.invers_dct_special(iPreIDCTMatrix, iNonZeroValuePos); // calculation is quicker
            else
                idct.invers_dct(iPreIDCTMatrix); // otherwise just use the normal idct
        
            // we need to convert back to doubles for Yuv stuff
            for (int i = 0; i < 64; i++)
                adbl[i] = iPreIDCTMatrix[i];
        }
        
        return new Matrix8x8(adbl);
    }

}
