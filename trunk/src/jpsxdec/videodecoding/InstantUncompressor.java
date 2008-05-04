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
 * InstantUncompressor.java
 */

package jpsxdec.uncompressors;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.mdec.IDCT;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.MDEC.Mdec16Bits;
import jpsxdec.mdec.PsxYuvInt;
import jpsxdec.mdec.PsxYuvInt;
import jpsxdec.uncompressors.StrFrameUncompressor.ACVariableLengthCode;
import jpsxdec.uncompressors.StrFrameUncompressor.DCVariableLengthCode;
import jpsxdec.util.IO;


public class InstantUncompressor {
    public static PsxYuvInt Massive(StrFramePushDemuxer oDemux) throws IOException {
        // 1. Read the entire demuxed frame into an array
        byte[] abDemux = ReadDemuxIntoArray(oDemux.getStream(), oDemux.getDemuxFrameSize());
        
        // 2. Identify the type
        int iFrameType = IdentifyFrame(abDemux, oDemux.getFrameNumber());
        
        // 3. Wrap it with a bit reader
        ArrayBitReader m_oBitReader = new ArrayBitReader(abDemux);
        
        // 4. Read frame header
        
        // Frame info
        long lngNumberOfRunLenthCodes;
        long lngHeader3800;
        long lngQuantizationScaleChrom;
        long lngQuantizationScaleLumin;
        long lngVersion;

        switch (iFrameType) {
            case StrFrameUncompressor.FRAME_FF7:
                // FF7 videos have 40 bytes of camera data at the start of the frame
                m_oBitReader.SkipBits(40*8);
            case StrFrameUncompressor.FRAME_FF7_WITHOUT_CAMERA:
            case StrFrameUncompressor.FRAME_VER2: case StrFrameUncompressor.FRAME_VER3:
                lngNumberOfRunLenthCodes = m_oBitReader.ReadUnsignedBits(16);
                lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                lngQuantizationScaleChrom = 
                lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(16);
                lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                break;
                
            case StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE:
            case StrFrameUncompressor.FRAME_LAIN:
                // because it's set to little-endian right now, 
                // these 16 bits are reversed
                lngQuantizationScaleChrom = (int)m_oBitReader.ReadUnsignedBits(8);
                lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(8);
                
                lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                lngNumberOfRunLenthCodes = (int)m_oBitReader.ReadUnsignedBits(16);
                lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                // Lain also uses an actual byte stream, so we want big-endian reads
                m_oBitReader.setLittleEndian(false);
                break;
                
            case StrFrameUncompressor.FRAME_LOGO_IKI:
                throw new CriticalUncompressException(
                        "This appears to be the infamous logo.iki. " +
                        "Sorry, but I have no idea how to decode this thing.");
            default:
                throw new CriticalUncompressException("Unknown frame type.");
        }
        
        Ver3PreviousVals oVer3Vals = null;
        if (iFrameType == StrFrameUncompressor.FRAME_VER3)
            oVer3Vals = new Ver3PreviousVals();
        
        long lngActualWidth =  ((oDemux.getWidth()  + 15) & ~15);
        long lngActualHeight = ((oDemux.getHeight() + 15) & ~15);
        
        PsxYuvInt oImg = new PsxYuvInt((int)lngActualWidth, 
                                       (int)lngActualHeight);
        
         MDEC.Mdec16Bits oMdecCode = new Mdec16Bits();
        
        for (int iX = 0; iX < lngActualWidth; iX += 16) {
            for (int iY = 0; iY < lngActualHeight; iY += 16) {

                // 5. Read the bits and directly convert them to pre-idct matricies
                // 6. Run the idct on the matricies
                PsxYuvInt oMacroBlockYuv = DecodeMacroBlock(iFrameType, 
                                             (int)lngQuantizationScaleChrom, 
                                             (int)lngQuantizationScaleLumin, 
                                             m_oBitReader, oVer3Vals, oMdecCode);
                
                // 6. Combine into yuv
                oImg.putYuvImage(iX, iY, oMacroBlockYuv);
            }
        }
        
        // 7. And return it
        return oImg;
    }
    
    
    static int IdentifyFrame(byte[] abHeaderBytes, long lngFrameNum) throws CriticalUncompressException {
        
        // if 0x3800 found at the normal position
        if (abHeaderBytes[3] == 0x38 && abHeaderBytes[2] == 0x00)
        {
            // check the version at the normal position
            int iVersion = ((abHeaderBytes[7] & 0xFF) << 8) | (abHeaderBytes[6] & 0xFF);
            switch (iVersion) {
                case 0: return StrFrameUncompressor.FRAME_LAIN;
                // if for some reason it's an ff7 frame without the camera data
                case 1: return StrFrameUncompressor.FRAME_FF7_WITHOUT_CAMERA; 
                case 2: return StrFrameUncompressor.FRAME_VER2;
                case 3: return StrFrameUncompressor.FRAME_VER3;
                case 256:
                    if ((((abHeaderBytes[4] & 0xFF) << 8) | (abHeaderBytes[5] & 0xFF)) == 320)
                        return StrFrameUncompressor.FRAME_LOGO_IKI;
                    else
                        throw new CriticalUncompressException("Unknown frame version " + iVersion);
                default:
                    throw new CriticalUncompressException("Unknown frame version " + iVersion);
            }
        } 
        // if 0x3800 is found 40 bytes from where it should be, and the
        // relative frame version is 1
        else if (abHeaderBytes[40+3] == 0x38 && abHeaderBytes[40+2] == 0x00 &&
                 abHeaderBytes[40+6] == 1) 
        {
            // it's an ff7 header
            return StrFrameUncompressor.FRAME_FF7;
        } 
        else // what else could it be?
        {
            // if the supposed 'version' is 0
            if (abHeaderBytes[7] == 0) {
                // and the supposed 0x3800 bytes...
                int iFrameNum = (((abHeaderBytes[3] & 0xFF) << 8) | (abHeaderBytes[2] & 0xFF));
                // ...happend to equal the frame number (if available)
                if (lngFrameNum >= 0) {
                    if (lngFrameNum == iFrameNum) {
                        // definitely lain final movie
                        return StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE;
                    }
                // .. or are at least less-than the number
                //    of frames in the final Lain movie
                } else if (iFrameNum >= 1 && iFrameNum <= 4765){
                    // probably lain final movie
                    return StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE;
                } else {
                    throw new CriticalUncompressException("0x3800 not found in start of frame");
                }
            } else {
                throw new CriticalUncompressException("0x3800 not found in start of frame");
            }
        }
        throw new CriticalUncompressException("Unknown frame type");
    }
    
    
    private static byte[] ReadDemuxIntoArray(InputStream oDemuxIS, long lngSize) throws IOException {
        return IO.readByteArray(oDemuxIS, (int)lngSize);
    }
    
    
    private static IDCT idct = new IDCT();
    
    
    private static final int BLOCK_CR = 0;
    private static final int BLOCK_CB = 1;
    private static final int BLOCK_Y1 = 2;
    private static final int BLOCK_Y2 = 3;
    private static final int BLOCK_Y3 = 4;
    private static final int BLOCK_Y4 = 5;
    
    private static class Ver3PreviousVals {
        public int m_iPreviousCr_DC = 0;
        public int m_iPreviousCb_DC = 0;
        public int m_iPreviousY_DC = 0;
    }
    
    private static PsxYuvInt DecodeMacroBlock(int iFrameType, 
                                         int lngFrameQuantScaleChrom, 
                                         int lngFrameQuantScaleLumin,
                                         ArrayBitReader oReader, 
                                         Ver3PreviousVals oVer3Vals,
                                         MDEC.Mdec16Bits oMdecCode ) 
             throws UncompressionException, EOFException 
    {
        
        int[][] aiDecodedBlocks = new int[6][];
        
        if (iFrameType != StrFrameUncompressor.FRAME_VER3) 
        {
            
            // For version 2, all Cr, Cb, Y1, Y2, Y3, Y4 
            // DC Coefficients are encoded the same
            for (int iBlock = 0; iBlock <= BLOCK_Y4; iBlock++) 
            {
                int iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance(oReader);
                aiDecodedBlocks[iBlock] = Decode_AC_Coefficients(
                        (iBlock <= 1) ? lngFrameQuantScaleChrom : lngFrameQuantScaleLumin,
                        iDCCoefficient, oReader, iFrameType, oMdecCode);
                
            }
            
        } 
        else
        {
            // For version 3, DC coefficients are encoded differently for
            // DC Chrominance and DC Luminance. 
            // In addition, the value is relative to the previous value.
            // (this is the same way mpeg-1 does it)
            
            // Cr
            oVer3Vals.m_iPreviousCr_DC += DecodeV3_DC_Chrominance(oReader);
            
            aiDecodedBlocks[BLOCK_CR] = Decode_AC_Coefficients(
                    lngFrameQuantScaleChrom,
                    oVer3Vals.m_iPreviousCr_DC, oReader, iFrameType, oMdecCode);

            // Cb
            oVer3Vals.m_iPreviousCb_DC += DecodeV3_DC_Chrominance(oReader);
            
            aiDecodedBlocks[BLOCK_CB] = Decode_AC_Coefficients(
                    lngFrameQuantScaleChrom,
                    oVer3Vals.m_iPreviousCb_DC, oReader, iFrameType, oMdecCode);
            
            
            // Y1, Y2, Y3, Y4
            for (int iBlock = BLOCK_Y1; iBlock<=BLOCK_Y4; iBlock++) {
                oVer3Vals.m_iPreviousY_DC += DecodeV3_DC_Luminance(oReader);

                aiDecodedBlocks[iBlock] = Decode_AC_Coefficients(
                        lngFrameQuantScaleChrom,
                        oVer3Vals.m_iPreviousY_DC, oReader, iFrameType, oMdecCode);
            }
            
        }
        
        
        PsxYuvInt oMacroBlockYuv = new PsxYuvInt(16, 16);

        oMacroBlockYuv.setY(0, 0, 8, 8, aiDecodedBlocks[BLOCK_Y1]);
        oMacroBlockYuv.setY(8, 0, 8, 8, aiDecodedBlocks[BLOCK_Y2]);
        oMacroBlockYuv.setY(0, 8, 8, 8, aiDecodedBlocks[BLOCK_Y3]);
        oMacroBlockYuv.setY(8, 8, 8, 8, aiDecodedBlocks[BLOCK_Y4]);

        oMacroBlockYuv.setCbCr(0, 0, 8, 8, aiDecodedBlocks[BLOCK_CB], 
                                           aiDecodedBlocks[BLOCK_CR]);
        
        return oMacroBlockYuv;
    }
    
    private static int DecodeV2_DC_ChrominanceOrLuminance(ArrayBitReader oReader) throws EOFException 
    {
        int oDCChrominanceOrLuminance;
        
        // Now read the DC coefficient value
        oDCChrominanceOrLuminance = (int)oReader.ReadSignedBits(10);
        
        return oDCChrominanceOrLuminance;
    }
    
    
    private static final int b10000000 = 0x80;
    private static int DecodeV3_DC_Chrominance(ArrayBitReader m_oBitReader) 
        throws UncompressionException, EOFException 
    {
        int iDCChrominance;
        
        // Peek enough bits
        int iBits = (int)m_oBitReader.PeekUnsignedBits(
                StrFrameUncompressor.DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode tDcVlc;
        
        if ((iBits & b10000000) == 0) { // "0x"
            tDcVlc = StrFrameUncompressor.DC_Chrominance_VariableLengthCodes[7 + ((iBits >>> 6) & 1)];
        } else {                        // "1x"
            int iMask = 0x40;
            int iTblIndex = 6;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex--;
            }
            
            if (iMask == 0) {
                throw new UncompressionException("Error decoding macro block:" +
                                                 " Unknown DC variable length code " + iBits);
            }
            
            tDcVlc = StrFrameUncompressor.DC_Chrominance_VariableLengthCodes[iTblIndex];
            
        }
        
        m_oBitReader.SkipBits(tDcVlc.VariableLengthCode.length());

        if (tDcVlc.DC_Length == 0) {

            iDCChrominance = 0;

        } else {

            // Read the DC differential
            int iDC_Differential = 
                   (int)m_oBitReader.ReadUnsignedBits(tDcVlc.DC_Length);
            // Lookup its value
            iDCChrominance = 
                    tDcVlc.DC_DifferentialLookup[iDC_Differential];

            // !!! ???We must multiply it by 4 for no reason??? !!!
            iDCChrominance *= 4; 
        }

        // return the new DC Coefficient
        return iDCChrominance;
    }
    
    private static final int b1000000 = 0x40;
    private static final int b0100000 = 0x20;
    private static int DecodeV3_DC_Luminance(ArrayBitReader m_oBitReader) 
        throws UncompressionException, EOFException 
    {
        int iDCLuminance;
        
        // Peek enough bits
        int iBits = (int)m_oBitReader.PeekUnsignedBits(
                StrFrameUncompressor.DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode tDcVlc;
        
        if ((iBits & b1000000) == 0) {        // "0x"
            tDcVlc = StrFrameUncompressor.DC_Luminance_VariableLengthCodes[7 + ((iBits >>> 5) & 1)];
        } else if ((iBits & b0100000) == 0) { // "10"
            tDcVlc = StrFrameUncompressor.DC_Luminance_VariableLengthCodes[5 + ((iBits >>> 4) & 1)];
        } else {                              // "11x"
            int iMask = 0x10;
            int iTblIndex = 4;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex--;
            }
            
            if (iMask == 0) {
                throw new UncompressionException("Error decoding macro block:" +
                                                 " Unknown DC variable length code " + iBits);
            }
            
            tDcVlc = StrFrameUncompressor.DC_Luminance_VariableLengthCodes[iTblIndex];
            
        }
        
        m_oBitReader.SkipBits(tDcVlc.VariableLengthCode.length());

        if (tDcVlc.DC_Length == 0) {

            iDCLuminance = 0;

        } else {

            // Read the DC differential
            int iDC_Differential = 
                   (int)m_oBitReader.ReadUnsignedBits(tDcVlc.DC_Length);
            // Lookup its value
            iDCLuminance = 
                    tDcVlc.DC_DifferentialLookup[iDC_Differential];

            // !!! ???We must multiply it by 4 for no reason??? !!!
            iDCLuminance *= 4; 
        }

        // return the new DC Coefficient
        return iDCLuminance;
    }
    
    
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
    
    static {
        idct.norm(PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX);
    }
    
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
    
    private static int[] Decode_AC_Coefficients(
            int iQuantizationScale, 
            int iDC_Coefficient,
            ArrayBitReader m_oBitReader,
            int m_iFrameType,
            MDEC.Mdec16Bits oMdecCode ) 
            throws UncompressionException, EOFException 
    {
        
        int[] iPreIDCTMatrix = new int[64];
        
        int iVectorPos = 1; // current position in the vector
        int iNonZeroValueCount = 0; // count of non-zero coefficients
        int iNonZeroValuePos = -1; // last non-zero coefficient
        
        if (iDC_Coefficient != 0) {
            iPreIDCTMatrix[0] = iDC_Coefficient * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[0];
            iNonZeroValueCount = 1;
            iNonZeroValuePos = 0;
        }
        
        while (Decode_AC_VariableLengthCode(m_oBitReader, oMdecCode, m_iFrameType)) {

            iVectorPos += oMdecCode.Top6Bits;

            if (iVectorPos > 63) {
                throw new RuntimeException("[MDEC] Run length out of bounds: " + 
                                       (iVectorPos + 1));
            }

            // Reverse Zig-Zag and Dequantize all at the same time
            int iRevZigZagPos = REVERSE_ZIG_ZAG_SCAN_MATRIX[iVectorPos];
            iPreIDCTMatrix[iRevZigZagPos] = (
                        oMdecCode.Bottom10Bits
                      * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[iRevZigZagPos]
                      * iQuantizationScale) >> 3 ;
            iVectorPos ++;
            iNonZeroValueCount ++;
            iNonZeroValuePos = iRevZigZagPos;

        }
            
        ////////////////////////////////////////////////////////////////////////
        // Step 4) Finally, perform the inverse discrete cosine transform
        ////////////////////////////////////////////////////////////////////////
        
        if (iNonZeroValueCount != 0) { // if there were no non-zero coefficients
            if (iNonZeroValueCount == 1) // if there is only one
                idct.invers_dct_special(iPreIDCTMatrix, iNonZeroValuePos); // calculation is quicker
            else
                idct.invers_dct(iPreIDCTMatrix); // otherwise just use the normal idct
        }
        
        return iPreIDCTMatrix;
    }

    
    private static final int b1000000000000000_ = 0x10000;
    private static final int b0100000000000000_ = 0x08000;
    private static final int b0010000000000000_ = 0x04000;
    private static final int b0001100000000000_ = 0x03000;
    private static final int b0001000000000000_ = 0x02000;
    private static final int b0000100000000000_ = 0x01000;
    private static final int b0000010000000000_ = 0x00800;
    private static final int b0000001000000000_ = 0x00400;
    private static final int b0000000100000000_ = 0x00200;
    private static final int b0000000010000000_ = 0x00100;
    private static final int b0000000001000000_ = 0x00080;
    private static final int b0000000000100000_ = 0x00040;
    private static final int b0000000000010000_ = 0x00020;
    private static boolean Decode_AC_VariableLengthCode(ArrayBitReader m_oBitReader, MDEC.Mdec16Bits oRlc, int m_iFrameType) throws UncompressionException, EOFException 
    {
        ACVariableLengthCode aoVarLenCodes[];
        ACVariableLengthCode vlc = null;
        long lngBits = m_oBitReader.PeekUnsignedBits( 17
                 /* AC_LONGEST_VARIABLE_LENGTH_CODE + 1 */);
        
        // Use the correct AC variable length code list
        if (m_iFrameType != StrFrameUncompressor.FRAME_LAIN && 
            m_iFrameType != StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE) 
        {
            aoVarLenCodes = StrFrameUncompressor.AC_VARIABLE_LENGTH_CODES_MPEG1;
        } else {
            aoVarLenCodes = StrFrameUncompressor.AC_VARIABLE_LENGTH_CODES_LAIN;
        }
        
        // Walk through the bit tree
        if (    (lngBits & b1000000000000000_) != 0) {        // "1"
            if ((lngBits & b0100000000000000_) != 0) {        // "11"
                vlc = aoVarLenCodes[0];
            } else {                                          // "10"
                m_oBitReader.SkipBits(2);
                return false;
            }
        } else if ((lngBits & b0100000000000000_) != 0) {     // "01"
            if    ((lngBits & b0010000000000000_) != 0) {     // "011"
                vlc = aoVarLenCodes[1];
            } else {                                          // "010x"
                vlc = aoVarLenCodes[2 + (int)((lngBits >>> 13) & 1)];
            }
        } else if ((lngBits & b0010000000000000_) != 0) {      // "001"
            if    ((lngBits & b0001100000000000_) != 0)  {     // "001xx"
                vlc = aoVarLenCodes[3 + (int)((lngBits >>> 12) & 3)];
            } else {                                           // "00100xxx"
                vlc = aoVarLenCodes[15 + (int)((lngBits >>> 9) & 7)];
            }
        } else if ((lngBits & b0001000000000000_) != 0) {      // "0001xx"
            vlc = aoVarLenCodes[7 + (int)((lngBits >>> 11) & 3)];
        } else if ((lngBits & b0000100000000000_) != 0) {      // "00001xx"
            vlc = aoVarLenCodes[11 + (int)((lngBits >>> 10) & 3)];
        } else if ((lngBits & b0000010000000000_) != 0) {      // "000001"
            // escape code
            Decode_AC_EscapeCode(lngBits, m_iFrameType, oRlc, m_oBitReader);
            return true;
        } else if ((lngBits & b0000001000000000_) != 0) {      // "0000001xxx"
            vlc = aoVarLenCodes[23 + (int)((lngBits >>> 7) & 7)];
        } else if ((lngBits & b0000000100000000_) != 0) {      // "00000001xxxx"
            vlc = aoVarLenCodes[31 + (int)((lngBits >>> 5) & 15)];
        } else if ((lngBits & b0000000010000000_) != 0) {      // "000000001xxxx"
            vlc = aoVarLenCodes[47 + (int)((lngBits >>> 4) & 15)];
        } else if ((lngBits & b0000000001000000_) != 0) {      // "0000000001xxxx"
            vlc = aoVarLenCodes[63 + (int)((lngBits >>> 3) & 15)];
        } else if ((lngBits & b0000000000100000_) != 0) {      // "00000000001xxxx"
            vlc = aoVarLenCodes[79 + (int)((lngBits >>> 2) & 15)];
        } else if ((lngBits & b0000000000010000_) != 0) {      // "000000000001xxxx"
            vlc = aoVarLenCodes[95 + (int)((lngBits >>> 1) & 15)];
        } else {
            
            throw new UncompressionException(
                    "Error decoding macro block: " +
                    "Unmatched AC variable length code: " +
                     BufferedBitReader.PadZeroLeft(Long.toBinaryString(lngBits), 17));
        }
        
        // Yay we found it!
        /*
        String s = BufferedBitReader.PadZeroLeft(Long.toBinaryString(lngBits), 17);
        if (!s.startsWith(vlc.VariableLengthCode)) {
            System.out.println(oRlc.VariableLengthCodeBits);
            System.out.println(vlc.VariableLengthCode);
        }
        */  
        // Save the resulting code, and run of zeros
        oRlc.Top6Bits = vlc.RunOfZeros;
        // Take either the positive or negitive AC coefficient,
        // depending on the sign bit
        if ((lngBits & (1 << (16 - vlc.VariableLengthCode.length()))) != 0) {
            // negative
            oRlc.Bottom10Bits = -vlc.AbsoluteLevel;
        } else {
            // positive
            oRlc.Bottom10Bits = vlc.AbsoluteLevel;
        }

        // Skip that many bits
        m_oBitReader.SkipBits(vlc.VariableLengthCode.length() + 1);

        return true;
            
    }
    
    
    private static void Decode_AC_EscapeCode(long lngBits, int m_iFrameType, MDEC.Mdec16Bits tRlc,ArrayBitReader m_oBitReader ) 
            throws UncompressionException, EOFException 
    {
        
        tRlc.Top6Bits = (int)((lngBits >>> 5) /*17 - 12*/) & 63;

        m_oBitReader.SkipBits(12 /*Escape code (6) + top 6 bits*/);
        
        if (m_iFrameType != StrFrameUncompressor.FRAME_LAIN && m_iFrameType != StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE)
        {
            // Normal playstation encoding stores the escape code in 16 bits:
            // 6 for run of zeros (already read), 10 for AC Coefficient
            
            // Read the 10 bits of AC Coefficient
            tRlc.Bottom10Bits = (int)m_oBitReader.ReadSignedBits(10);

            // Did we end up with an AC coefficient of zero?
            if (tRlc.Bottom10Bits == 0) {
                // Normally this is concidered an error
                // but FF7 has these pointless codes. So we'll only allow it
                // if this is FF7
                if (m_iFrameType != StrFrameUncompressor.FRAME_FF7 && m_iFrameType != StrFrameUncompressor.FRAME_FF7_WITHOUT_CAMERA) 
                    // If not FF7, throw an error
                    throw new UncompressionException(
                            "Error decoding macro block: " +
                            "AC Escape code: Run length is zero");
            }
            
        } else { // Lain
            
            /* Lain playstation uses mpeg1 specification escape code
            Fixed Length Code       Level 
            forbidden               -256  
            1000 0000 0000 0001     -255  
            1000 0000 0000 0010     -254  
            ...                          
            1000 0000 0111 1111     -129  
            1000 0000 1000 0000     -128  
            1000 0001               -127  
            1000 0010               -126  
            ...                           
            1111 1110               -2    
            1111 1111               -1    
            forbidden                0    
            0000 0001                1    
            0000 0010                2    
            ...   
            0111 1110               126   
            0111 1111               127   
            0000 0000 1000 0000     128   
            0000 0000 1000 0001     129   
            ...   
            0000 0000 1111 1110     254   
            0000 0000 1111 1111     255   
             */
            // Peek at the first 8 bits
            lngBits = m_oBitReader.ReadSignedBits(8);
            tRlc.VariableLengthCodeBits += lngBits;
            if (lngBits == 0x00) {
                // If it's the special 00000000
                // Positive
                tRlc.Bottom10Bits = (int)m_oBitReader.ReadUnsignedBits(8);
                
            } else if ( (lngBits & 0xFF) == 0x80 ) {
                // If it's the special 10000000
                // Negitive
                tRlc.Bottom10Bits = -256 + (int)m_oBitReader.ReadUnsignedBits(8);
            } else {
                // Otherwise we already have the value
                tRlc.Bottom10Bits = (int)lngBits;
            }
            
        }
        
    }
    
    
}
