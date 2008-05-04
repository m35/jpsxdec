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
 * StrFrameDecoderFast.java
 */

package jpsxdec.videodecoding;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.mdec.IDCT;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.IProgressListener;
import jpsxdec.media.IProgressListener.IProgressErrorListener;
import jpsxdec.videodecoding.StrFrameUncompressor.ACVariableLengthCode;
import jpsxdec.videodecoding.StrFrameUncompressor.DCVariableLengthCode;

/** A fairly optimized, full Java implementation of the PSX video decoding
 *  process. WARNING: The public methods are NOT thread safe. Create a separate
 *  instance of this class for each thread, or wrap the calls with syncronize. */
public class StrFrameDecoderFast {

    private static final IDCT idct = new IDCT();
    
    private StrFrameHeader _FrameHeader;
    private ArrayBitReader _BitReader;
    
    private final int[] _BlockCr = new int[64];
    private final int[] _BlockCb = new int[64];
    private final int[] _BlockY1 = new int[64];
    private final int[] _BlockY2 = new int[64];
    private final int[] _BlockY3 = new int[64];
    private final int[] _BlockY4 = new int[64];
    
    private int _PreviousCr_DC;
    private int _PreviousCb_DC;
    private int _PreviousY_DC;
    
    private int _ZeroRun;
    private int _ACCoefficient;
    
    private byte[] _InDemux;
    
    private int[] _OutRgb;
    
    private ACVariableLengthCode _VarLenCodes[];
    
    public StrFrameDecoderFast() {
    }
    
    /** Performs all decoding steps in a fairly optimized way:
     * Uncompressing, converting to pre-IDCT blocks, IDCT, and Yuv->RGB. 
     * This method is not thread-safe. */
    public final BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, IProgressListener oListener)
            throws IOException, CriticalUncompressException 
    {
        SetupForDecoding(oDemux);
        
        int iActualWidth =  (int)((oDemux.getWidth()  + 15) & ~15);
        int iActualHeight = (int)((oDemux.getHeight() + 15) & ~15);
        if (_OutRgb == null || _OutRgb.length < iActualWidth * iActualHeight)
            _OutRgb = new int[iActualWidth * iActualHeight];
        
        //int _iMacBlk = 0;
        try {
            for (int iX = 0; iX < iActualWidth; iX += 16) {
                for (int iY = 0; iY < iActualHeight; iY += 16) {

                    //System.err.println(String.format("Decoding macro block %d (%d, %d)", _iMacBlk++, iX, iY));

                    DecodeMacroBlock();

                    YCbCr2RGB( 0, _BlockY1, _OutRgb, iX + iY * iActualWidth, iActualWidth);
                    YCbCr2RGB( 4, _BlockY2, _OutRgb, iX + iY * iActualWidth + 8, iActualWidth);
                    YCbCr2RGB(32, _BlockY3, _OutRgb, iX + (iY + 8) * iActualWidth, iActualWidth);
                    YCbCr2RGB(36, _BlockY4, _OutRgb, iX + (iY + 8) * iActualWidth + 8, iActualWidth);

                }
            }
        } catch (UncompressionException ex) {
            if (oListener instanceof IProgressErrorListener)
                ((IProgressErrorListener)oListener).ProgressUpdate(ex);
        } catch (EOFException ex) {
            if (oListener instanceof IProgressErrorListener)
                ((IProgressErrorListener)oListener).ProgressUpdate(ex);
        }
        
        // 7. And return it
        BufferedImage bi = new BufferedImage(iActualWidth, iActualHeight, BufferedImage.TYPE_INT_RGB);
        WritableRaster wr = bi.getRaster();
        wr.setDataElements(0, 0, iActualWidth, iActualHeight, _OutRgb);
        
        return bi;
    }
    
    
    /** Performs all but the last decoding step in a fairly optimized way:
     * Uncompressing, converting to pre-IDCT blocks, IDCT, and combine into Yuv. */
    public final PsxYuv UncompressDecode(StrFramePushDemuxer oDemux, IProgressListener oListener) 
            throws IOException, CriticalUncompressException 
    {
        SetupForDecoding(oDemux);
        
        int iActualWidth =  (int)((oDemux.getWidth()  + 15) & ~15);
        int iActualHeight = (int)((oDemux.getHeight() + 15) & ~15);
        
        PsxYuv yuv = new PsxYuv(iActualWidth, iActualHeight);
        
        try {
            for (int iX = 0; iX < iActualWidth; iX += 16) {
                for (int iY = 0; iY < iActualHeight; iY += 16) {

                    //System.err.println("Decoding macro block " + _iMacBlk++);

                    DecodeMacroBlock();

                    // TODO: combine decoded data into Yuv image

                }
            }
        } catch (UncompressionException ex) {
            if (oListener instanceof IProgressErrorListener)
                ((IProgressErrorListener)oListener).ProgressUpdate(ex);
        } catch (EOFException ex) {
            if (oListener instanceof IProgressErrorListener)
                ((IProgressErrorListener)oListener).ProgressUpdate(ex);
        }
        
        return yuv;
    }
    
    /** Shared method to setup instance variables for the decoding process. */
    private final void SetupForDecoding(StrFramePushDemuxer oDemux) 
            throws IOException, CriticalUncompressException 
    {
        // Allocate the demux buffer if needed
        int iFrameSize = (int)oDemux.getDemuxFrameSize();
        if (_InDemux == null || _InDemux.length < iFrameSize)
            _InDemux = new byte[iFrameSize];
        
        try {
            // Read the entire demuxed frame into an array
            InputStream oIS = oDemux.getStream();
            int pos = oIS.read(_InDemux);
            if (pos < 0) throw new EOFException();
            while (pos < iFrameSize) {
                int i = oIS.read(_InDemux, pos, iFrameSize - pos);
                if (i < 0) {
                    iFrameSize = pos;
                    break;
                }
                pos += i;
            }
        } catch (EOFException ex) {
            // This basically means there is a gap between frame chunks somewhere
            // (most likely FF9 missing last frame sector, which means we don't have
            // the first frame chunk).
            // TODO: Assuming we got at least the first frame sector, try to continue
            // however, we'd need to pass the end of stream position to the ArrayBitReader to check 
            throw new CriticalUncompressException(
                    "Unexpected end of stream while reading frame "
                    + oDemux.getFrameNumber() + " data.");
        }
        
        // Identify the frame type and read frame header
        // TODO: If not enough data could be read for the frame header, need to check somehow
        _FrameHeader = new StrFrameHeader(_InDemux, oDemux.getFrameNumber());
        
        // Use the correct AC variable length code list
        if (_FrameHeader.FrameType != StrFrameHeader.FRAME_LAIN && 
            _FrameHeader.FrameType != StrFrameHeader.FRAME_LAIN_FINAL_MOVIE) 
        {
            _VarLenCodes = StrFrameUncompressor.AC_VARIABLE_LENGTH_CODES_MPEG1;
        } else {
            _VarLenCodes = StrFrameUncompressor.AC_VARIABLE_LENGTH_CODES_LAIN;
        }
        
        // Wrap it with a bit reader
        if (_BitReader == null)
            _BitReader = new ArrayBitReader(_InDemux, _FrameHeader.LittleEndian, _FrameHeader.DataStart, iFrameSize);
        else
            _BitReader.Reset(_InDemux, _FrameHeader.LittleEndian, _FrameHeader.DataStart, iFrameSize);
        
        //System.err.println(String.format("--- %dx%d ---", iActualWidth, iActualHeight));

        // Clear previous DC for v3 frames (will only be used if necessary)
        _PreviousCr_DC = _PreviousCb_DC = _PreviousY_DC = 0;
    }
    
    /** Reads and decodes all 6 blocks of a macro-block and stores them 
     *  in the instance block variables. Works for both ver 2 and ver 3 frames. */
    private final void DecodeMacroBlock() 
             throws UncompressionException, EOFException 
    {
        
        if (_FrameHeader.FrameType != StrFrameHeader.FRAME_VER3) 
        {
            
            // For version 2, all Cr, Cb, Y1, Y2, Y3, Y4 
            // DC Coefficients are encoded the same
            int iDCCoefficient;
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Cr DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleChrom,
                    iDCCoefficient, _BlockCr);
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Cb DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleChrom,
                    iDCCoefficient, _BlockCb);
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Y1 DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleLumin,
                    iDCCoefficient, _BlockY1);
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Y2 DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleLumin,
                    iDCCoefficient, _BlockY2);
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Y3 DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleLumin,
                    iDCCoefficient, _BlockY3);
            iDCCoefficient = DecodeV2_DC_ChrominanceOrLuminance();
            //System.err.println("Read Y4 DC " + iDCCoefficient);
            Decode_AC_RestOfBlock(
                    _FrameHeader.QuantizationScaleLumin,
                    iDCCoefficient, _BlockY4);
            
        } 
        else
        {
            // For version 3, DC coefficients are encoded differently for
            // DC Chrominance and DC Luminance. 
            // In addition, the value is relative to the previous value.
            // (this is the same way mpeg-1 does it)
            
            // Cr
            _PreviousCr_DC += DecodeV3_DC_Chrominance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleChrom,
                    _PreviousCr_DC, _BlockCr);

            // Cb
            _PreviousCb_DC += DecodeV3_DC_Chrominance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleChrom,
                    _PreviousCb_DC, _BlockCb);
            
            
            // Y1, Y2, Y3, Y4
            _PreviousY_DC += DecodeV3_DC_Luminance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleLumin,
                    _PreviousY_DC, _BlockY1);
            _PreviousY_DC += DecodeV3_DC_Luminance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleLumin,
                    _PreviousY_DC, _BlockY2);
            _PreviousY_DC += DecodeV3_DC_Luminance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleLumin,
                    _PreviousY_DC, _BlockY3);
            _PreviousY_DC += DecodeV3_DC_Luminance();
            Decode_AC_RestOfBlock(_FrameHeader.QuantizationScaleLumin,
                    _PreviousY_DC, _BlockY4);
            
        }
        
    }
    
    /** Reads and decodes a ver 2 DC Coefficient, for both Chrominance
     *  and Luminance blocks (since they're stored the same). */
    private final int DecodeV2_DC_ChrominanceOrLuminance() throws EOFException 
    {
        int oDCChrominanceOrLuminance;
        
        // Now read the DC coefficient value
        oDCChrominanceOrLuminance = (int)_BitReader.ReadSignedBits(10);
        
        return oDCChrominanceOrLuminance;
    }
    
    
    private static final int b10000000 = 0x80;
    private static final int b01000000 = 0x40;
    private final int DecodeV3_DC_Chrominance() 
        throws UncompressionException, EOFException 
    {
        int iDCChrominance;
        
        // Peek enough bits
        int iBits = (int)_BitReader.PeekUnsignedBits(
                StrFrameUncompressor.DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode tDcVlc;
        
        if ((iBits & b10000000) == 0) { // "0x"
            tDcVlc = StrFrameUncompressor.DC_Chrominance_VariableLengthCodes[7 + ((iBits >>> 6) & 1)];
        } else {                        // "1x"
            int iMask = b01000000;
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
        
        _BitReader.SkipBits(tDcVlc.VariableLengthCode.length());

        if (tDcVlc.DC_Length == 0) {

            iDCChrominance = 0;

        } else {

            // Read the DC differential
            int iDC_Differential = 
                   (int)_BitReader.ReadUnsignedBits(tDcVlc.DC_Length);
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
    private final int DecodeV3_DC_Luminance() 
        throws UncompressionException, EOFException 
    {
        // Peek enough bits
        int iBits = (int)_BitReader.PeekUnsignedBits(
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
        
        _BitReader.SkipBits(tDcVlc.VariableLengthCode.length());

        int iDCLuminance = 0;
        
        if (tDcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = 
                   (int)_BitReader.ReadUnsignedBits(tDcVlc.DC_Length);
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
    static { idct.norm(PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX); }
    
    
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
    
    private final void Decode_AC_RestOfBlock(
            final long iQuantizationScale, 
            final int iDC_Coefficient,
            final int[] aiPreIDCTMatrix) 
            throws UncompressionException, EOFException 
    {
        
        int iVectorPos = 1; // current position in the vector
        int iNonZeroValueCount = 0; // count of non-zero coefficients
        int iNonZeroValuePos = -1; // last non-zero coefficient
        int iRevZigZagPos;
        
        for (int i = 0; i < aiPreIDCTMatrix.length; i++) {
            aiPreIDCTMatrix[i] = 0;
        }

        
        if (iDC_Coefficient != 0) {
            aiPreIDCTMatrix[0] = 
                    iDC_Coefficient * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[0];
            iNonZeroValueCount = 1;
            iNonZeroValuePos = 0;
        }
        
        // Keep reading AC variable length codes until there is an end-of-block
        while (Decode_AC_VariableLengthCode()) {
            // _ZeroRun and _ACCoefficient are set with the values read
            
            iVectorPos += _ZeroRun;

            try {
                // Reverse Zig-Zag and Dequantize all at the same time
                iRevZigZagPos = REVERSE_ZIG_ZAG_SCAN_MATRIX[iVectorPos];
                aiPreIDCTMatrix[iRevZigZagPos] = (int)(
                            _ACCoefficient
                          * PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX[iRevZigZagPos]
                          * iQuantizationScale) >> 3 ;
                iVectorPos ++;
                iNonZeroValueCount ++;
                iNonZeroValuePos = iRevZigZagPos;
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new UncompressionException(
                        "[MDEC] Run length out of bounds: " + iVectorPos);
            }

        }
            
        // Finally, perform the inverse discrete cosine transform (IDCT)
        
        if (iNonZeroValueCount != 0) { // only if there were non-zero coefficients
            if (iNonZeroValueCount == 1) // if there is only one non-zero coefficient
                // calculation is quicker
                idct.invers_dct_special(aiPreIDCTMatrix, iNonZeroValuePos); 
            else
                // otherwise use the full idct
                idct.invers_dct(aiPreIDCTMatrix); 
        }
        // if all coefficients are 0, then IDCT won't change the matrix at all,
        // so we're already done.
        
    }

    
    private static final int b1000000000000000_ = 0x8000 << 1;
    private static final int b0100000000000000_ = 0x4000 << 1;
    private static final int b0010000000000000_ = 0x2000 << 1;
    private static final int b0001100000000000_ = 0x1800 << 1;
    private static final int b0001000000000000_ = 0x1000 << 1;
    private static final int b0000100000000000_ = 0x0800 << 1;
    private static final int b0000010000000000_ = 0x0400 << 1;
    private static final int b0000001000000000_ = 0x0200 << 1;
    private static final int b0000000100000000_ = 0x0100 << 1;
    private static final int b0000000010000000_ = 0x0080 << 1;
    private static final int b0000000001000000_ = 0x0040 << 1;
    private static final int b0000000000100000_ = 0x0020 << 1;
    private static final int b0000000000010000_ = 0x0010 << 1;
    private final  boolean Decode_AC_VariableLengthCode() 
            throws UncompressionException, EOFException 
    {
        final ACVariableLengthCode vlc;
        
        // peek enough bits for the longest variable length code (16 bits)
        // plus 1 for the sign bit.
        long lngBits = _BitReader.PeekUnsignedBits( 17 );
        
        // Walk through the bits, one-by-one
        // Fun fact: The Lain Playstation game uses this same decoding approach
        if (    (lngBits & b1000000000000000_) != 0) {        // "1"
            if ((lngBits & b0100000000000000_) != 0) {        // "11"
                vlc = _VarLenCodes[0];
            } else {                                          // "10"
                // End of block
                _BitReader.SkipBits(2);
                return false;
            }
        } else if ((lngBits & b0100000000000000_) != 0) {     // "01"
            if    ((lngBits & b0010000000000000_) != 0) {     // "011"
                vlc = _VarLenCodes[1];
            } else {                                          // "010x"
                vlc = _VarLenCodes[2 + (int)((lngBits >>> 13) & 1)];
            }
        } else if ((lngBits & b0010000000000000_) != 0) {      // "001"
            if    ((lngBits & b0001100000000000_) != 0)  {     // "001xx"
                vlc = _VarLenCodes[3 + (int)((lngBits >>> 12) & 3)];
            } else {                                           // "00100xxx"
                vlc = _VarLenCodes[15 + (int)((lngBits >>> 9) & 7)];
            }
        } else if ((lngBits & b0001000000000000_) != 0) {      // "0001xx"
            vlc = _VarLenCodes[7 + (int)((lngBits >>> 11) & 3)];
        } else if ((lngBits & b0000100000000000_) != 0) {      // "00001xx"
            vlc = _VarLenCodes[11 + (int)((lngBits >>> 10) & 3)];
        } else if ((lngBits & b0000010000000000_) != 0) {      // "000001"
            // escape code
            Decode_AC_EscapeCode(lngBits);
            return true;
        } else if ((lngBits & b0000001000000000_) != 0) {      // "0000001xxx"
            vlc = _VarLenCodes[23 + (int)((lngBits >>> 7) & 7)];
        } else if ((lngBits & b0000000100000000_) != 0) {      // "00000001xxxx"
            vlc = _VarLenCodes[31 + (int)((lngBits >>> 5) & 15)];
        } else if ((lngBits & b0000000010000000_) != 0) {      // "000000001xxxx"
            vlc = _VarLenCodes[47 + (int)((lngBits >>> 4) & 15)];
        } else if ((lngBits & b0000000001000000_) != 0) {      // "0000000001xxxx"
            vlc = _VarLenCodes[63 + (int)((lngBits >>> 3) & 15)];
        } else if ((lngBits & b0000000000100000_) != 0) {      // "00000000001xxxx"
            vlc = _VarLenCodes[79 + (int)((lngBits >>> 2) & 15)];
        } else if ((lngBits & b0000000000010000_) != 0) {      // "000000000001xxxx"
            vlc = _VarLenCodes[95 + (int)((lngBits >>> 1) & 15)];
        } else {
            
            throw new UncompressionException(
                    "Error decoding macro block: " +
                    "Unmatched AC variable length code: " +
                     BufferedBitReader.PadZeroLeft(Long.toBinaryString(lngBits), 17));
        }
        
        // Save the resulting code, and run of zeros
        _ZeroRun = vlc.RunOfZeros;
        // Take either the positive or negitive AC coefficient,
        // depending on the sign bit
        if ((lngBits & (1 << (16 - vlc.VariableLengthCode.length()))) != 0) {
            // negative
            _ACCoefficient = -vlc.AbsoluteLevel;
        } else {
            // positive
            _ACCoefficient = vlc.AbsoluteLevel;
        }

        // Skip that many bits
        _BitReader.SkipBits(vlc.VariableLengthCode.length() + 1);

        return true;
            
    }
    
    /**  */
    private final void Decode_AC_EscapeCode(long lngBits) 
            throws UncompressionException, EOFException 
    {
        // Read the run of zeros
        // 17 bits: eeeeeezzzzzz_____ : e = escape code, z = run of zeros
        _ZeroRun = (int)((lngBits >>> 5 /*17 - 12*/) & 63);

        // Skip the escape code (6 bits) and the run of zeros (6 bits)
        _BitReader.SkipBits( 12 );
        
        if (_FrameHeader.FrameType != StrFrameHeader.FRAME_LAIN && 
            _FrameHeader.FrameType != StrFrameHeader.FRAME_LAIN_FINAL_MOVIE)
        {
            // Normal playstation encoding stores the escape code in 16 bits:
            // 6 for run of zeros (already read), 10 for AC Coefficient
            
            // Read the 10 bits of AC Coefficient
            _ACCoefficient = (int)_BitReader.ReadSignedBits(10);

            // Did we end up with an AC coefficient of zero?
            if (_ACCoefficient == 0) {
                // Normally this is concidered an error
                // but FF7 has these pointless codes. So we'll only allow it
                // if this is FF7
                if (_FrameHeader.FrameType != StrFrameHeader.FRAME_FF7 && 
                    _FrameHeader.FrameType != StrFrameHeader.FRAME_FF7_WITHOUT_CAMERA) 
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
            lngBits = _BitReader.ReadSignedBits(8);
            if (lngBits == 0x00) {
                // If it's the special 00000000
                // Positive
                _ACCoefficient = (int)_BitReader.ReadUnsignedBits(8);
                
            } else if ( (lngBits & 0xFF) == 0x80 ) {
                // If it's the special 10000000
                // Negitive
                _ACCoefficient = -256 + (int)_BitReader.ReadUnsignedBits(8);
            } else {
                // Otherwise we already have the value
                _ACCoefficient = (int)lngBits;
            }
            
        }
        
    }
    
    
    private final static int CR_FAC = 0x166EA;             /* 1.402   * 2^16 */
    private final static int CB_FAC = 0x1C5A2;             /* 1.772   * 2^16 */
    private final static int CR_DIFF_FAC = 0xB6DC;         /* 0.7143 * 2^16 */
    private final static int CB_DIFF_FAC = 0x57FD;         /* 0.3437 * 2^16 */
    
    /** This function, and related constants, are originally from 
     *  Joerg Anders's MPG-1 player. Copyright Joerg Anders, licensed under
     *  GNU GENERAL PUBLIC LICENSE Version 2.
     *  http://vsr.informatik.tu-chemnitz.de/~jan/MPEG/MPEG_Play.html
     * <p>
     *  Modified to work with jpsxdec and the unique Playstation quirks.
     */
    private final void YCbCr2RGB(
            int iChromOfs,
            final int PixelsY[],
            final int PixelsRGB[],
            int RgbOfs,
            final int RgbWidth) 
    {
        int i, j;
        int Cb, Cr, CbGreen, CrGreen, Luminance;
        int LuminOfs = 0;
        
        int red, blue, green;
        
        for (i = 0; i < 4; i++, iChromOfs += 4, LuminOfs += 8, RgbOfs += (RgbWidth << 1) - 8) {
            for (j = 0; j < 4; j++, iChromOfs++) {

                Cb = _BlockCb[iChromOfs];
                Cr = _BlockCr[iChromOfs];
                CbGreen = Cb * CB_DIFF_FAC;
                CrGreen = Cr * CR_DIFF_FAC;
                Cb *= CB_FAC;
                Cr *= CR_FAC;

                Luminance = (PixelsY[LuminOfs] + 128) << 16;
                red = (Luminance + Cr);
                blue = (Luminance + Cb) >> 16;
                green = (Luminance - CrGreen - CbGreen) >> 8;
                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP
                PixelsRGB[RgbOfs] = ( red | green | blue );

                Luminance = (PixelsY[LuminOfs + 8] + 128) << 16;
                red = (Luminance + Cr);
                blue = (Luminance + Cb) >> 16;
                green = (Luminance - CrGreen - CbGreen) >> 8;
                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP
                PixelsRGB[RgbOfs + RgbWidth] = ( red | green | blue );

                LuminOfs++;
                RgbOfs++;
                
                //.............................................................
                
                Luminance = (PixelsY[LuminOfs] + 128) << 16;
                red = (Luminance + Cr);
                blue = (Luminance + Cb) >> 16;
                green = (Luminance - CrGreen - CbGreen) >> 8;
                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP
                PixelsRGB[RgbOfs] = ( red | green | blue );

                Luminance = (PixelsY[LuminOfs + 8] + 128) << 16;
                red = (Luminance + Cr);
                blue = (Luminance + Cb) >> 16;
                green = (Luminance - CrGreen - CbGreen) >> 8;
                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP
                PixelsRGB[RgbOfs + RgbWidth] = ( red | green | blue );

                LuminOfs++;
                RgbOfs++;
            }
        }

    }
    
    
    private final static int Clamp(int red, int blue, int green) {
        red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
        green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
        blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP

        return ( red | green | blue );
    }
    
    
}
