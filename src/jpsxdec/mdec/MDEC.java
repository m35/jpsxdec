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
 * MDEC.java
 */

package jpsxdec.mdec;

import java.io.*;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IO;

/** Simple (and slow) emulation of the Playstation MDEC ("Motion Decoder") chip.
  * While it doesn't process 9000 macroblocks per second, it shouldn't be very
  * difficult to read. */
public final class MDEC {
    
    /** How much debugging do you want to see? */
    public static int DebugVerbose = 2;
    /** This is the inverse discrete cosine transform that will be used
     *  during decoding. */
    public static IDCTinterface IDCT = new StephensIDCT();
    
    // :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    
    /** The default intra quantization matrix used to scale
     *  the post-DCT matrix. This matrix is identical to the MPEG-1
     *  quantization matrix, except the first value is 2 instead of 8. 
     *  This needs to be public so the IDCT.java class can modifty it. */
    public static final Matrix8x8 PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX = 
            new Matrix8x8(new double[] {
        /* 8 */  2, 16, 19, 22, 26, 27, 29, 34, 
                16, 16, 22, 24, 27, 29, 34, 37, 
                19, 22, 26, 27, 29, 34, 34, 38, 
                22, 22, 26, 27, 29, 34, 37, 40, 
                22, 26, 27, 29, 32, 35, 40, 48, 
                26, 27, 29, 32, 35, 40, 48, 58, 
                26, 27, 29, 34, 38, 46, 56, 69, 
                27, 29, 35, 38, 46, 56, 69, 83 
    });
    
    /** The order that the zig-zag vector is ordered. */
    static final Matrix8x8 REVERSE_ZIG_ZAG_SCAN_MATRIX =
            new Matrix8x8(new double[] {
                 0,  1,  5,  6, 14, 15, 27, 28,
                 2,  4,  7, 13, 16, 26, 29, 42,
                 3,  8, 12, 17, 25, 30, 41, 43,
                 9, 11, 18, 24, 31, 40, 44, 53,
                10, 19, 23, 32, 39, 45, 52, 54,
                20, 22, 33, 38, 46, 51, 55, 60,
                21, 34, 37, 47, 50, 56, 59, 61,
                35, 36, 48, 49, 57, 58, 62, 63
    });
    
    // :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    
    /** The 16 bit value for the MDEC indicating the end of a block
     *  (the variable-length-code "10" will be translated to this). */
    public final static int MDEC_END_OF_BLOCK = 0xFE00;
    
    /** Simple class to manage 16 bit values to be passed into the MDEC.
     *  The MDEC accepts little-endian 16 bit integers, where the top 6 bits 
     *  hold one value, and the bottom 10 bits hold another. In most cases
     *  these are AC run-length codes, with the 6 bit value holding how many
     *  0-value AC coefficients preceeds the 10 bit non-zero value AC 
     *  coefficient. The only exception is when the 6 bit value holds the
     *  quantization scale, and the 10 bit value holds the DC coefficient. */
    public static class Mdec16Bits {
        
        public String VariableLengthCodeBits;
        public double OriginalFilePos = -1;
        public int Top6Bits;
        public int Bottom10Bits;

        /** Generic constructor */
        public Mdec16Bits() {
            Top6Bits = -1;
            Bottom10Bits = 0;
        }
        
        /** Extract the top 6 bit and bottom 10 bit values from 16 bits */
        public Mdec16Bits(long lngMdecWord) {
            Top6Bits = (int)((lngMdecWord >> 10) & 63);
            Bottom10Bits = (int)(lngMdecWord & 0x3FF);
            if ((Bottom10Bits & 0x200) == 0x200) { // is it negitive?
                Bottom10Bits -= 0x400;
            }
        }
        
        /** Combines the top 6 bits and bottom 10 bits into a 16 bit value */
        public long toMdecWord() {
            return (((long)Top6Bits & 63) << 10) | ((long)Bottom10Bits & 0x3FF); 
        }
        
        public String toString() {
            return "(" + Top6Bits + ", " + Bottom10Bits + ")";
        }
        
        public Object clone() {
            Mdec16Bits oNew = new Mdec16Bits();
            oNew.Top6Bits = Top6Bits;
            oNew.Bottom10Bits = Bottom10Bits;
            oNew.OriginalFilePos = OriginalFilePos;
            oNew.VariableLengthCodeBits = VariableLengthCodeBits;
            return oNew;
        }
    }
    
    /** An IOException that also returns as much of the 
     *  decoded frame as is available. */
    public static class DecodingException extends IOException {
        
        final private PsxYuv yuv;
        final private IOException ex;

        public DecodingException(IOException ex, PsxYuv yuv) {
            super();
            this.ex = ex;
            this.yuv = yuv;
        }

        public PsxYuv getYuv() {
            return yuv;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            if (ex == null)
                return super.fillInStackTrace();
            else
                return ex.fillInStackTrace();
        }

        @Override
        public Throwable getCause() {
            return ex.getCause();
        }

        @Override
        public String getLocalizedMessage() {
            return ex.getLocalizedMessage();
        }

        @Override
        public String getMessage() {
            return ex.getMessage();
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return ex.getStackTrace();
        }

        @Override
        public synchronized Throwable initCause(Throwable cause) {
            return ex.initCause(cause);
        }

        @Override
        public void printStackTrace() {
            ex.printStackTrace();
        }

        @Override
        public void printStackTrace(PrintStream s) {
            ex.printStackTrace(s);
        }

        @Override
        public void printStackTrace(PrintWriter s) {
            ex.printStackTrace(s);
        }

        @Override
        public void setStackTrace(StackTraceElement[] stackTrace) {
            ex.setStackTrace(stackTrace);
        }

        @Override
        public String toString() {
            return ex.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return ex.equals(obj);
        }

        @Override
        public int hashCode() {
            return ex.hashCode();
        }
        
    }
    
    /* ---------------------------------------------------------------------- */
    /* Static class --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private MDEC() {}
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Main input function. Reads MDEC codes from the stream until an
     *  entire image of width x height has been decoded. Returns the
     *  decoded image as a yuv4mpeg2 class. */
    public static PsxYuv DecodeFrame(InputStream oStream, 
                                     long lngWidth, long lngHeight)
        throws DecodingException
    {                                  
        
        // Calculate actual width/height in macroblocks 
        // (since you can't have a partial macroblock)
        long lngActualWidth, lngActualHeight;

        if ((lngWidth % 16) > 0)
            lngActualWidth = (lngWidth / 16 + 1) * 16;
        else
            lngActualWidth = lngWidth;

        if ((lngHeight % 16) > 0)
            lngActualHeight = (lngHeight / 16 + 1) * 16;
        else
            lngActualHeight = lngHeight;
        
        PsxYuv oImg = new PsxYuv((int)lngActualWidth, 
                                 (int)lngActualHeight);
            
        int iMacroBlockCount = 0;

        try {
            // The macro blocks are ordered in columns
            for (int iX = 0; iX < lngActualWidth; iX += 16) {
                for (int iY = 0; iY < lngActualHeight; iY += 16) {
                    PsxYuv oMacroBlockYuv = DecodeMacroBlock(oStream);
                    iMacroBlockCount++;

                    oImg.putYuvImage(iX, iY, oMacroBlockYuv);
                }
            }
        } catch (IOException ex) {
            throw new DecodingException(ex, oImg);
        }
        
        return oImg;
    }
    
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Decodes an entire macro block from oStream and returns it as a 
     *  16x16 YUV image. */
    private static PsxYuv DecodeMacroBlock(InputStream oStream) 
        throws IOException 
    {
        
        // decode blocks (Cr,Cb,Y1,Y2,Y3,Y4)
        Matrix8x8 oCrMatrix;
        Matrix8x8 oCbMatrix; 
        Matrix8x8 oY1Matrix;
        Matrix8x8 oY2Matrix;
        Matrix8x8 oY3Matrix;
        Matrix8x8 oY4Matrix;

        // -- Decode the Cr block
        if (DebugVerbose >= 4) System.err.println("Cr block");
        oCrMatrix = DecodeBlock(oStream);

        // -- Decode the Cb block
        if (DebugVerbose >= 4) System.err.println("Cb block");
        oCbMatrix = DecodeBlock(oStream);

        // -- Decode the Y1 block
        if (DebugVerbose >= 4) System.err.println("Y1 block");
        oY1Matrix = DecodeBlock(oStream);

        // -- Decode the Y2 block
        if (DebugVerbose >= 4) System.err.println("Y2 block");
        oY2Matrix = DecodeBlock(oStream);

        // -- Decode the Y3 block
        if (DebugVerbose >= 4) System.err.println("Y3 block");
        oY3Matrix = DecodeBlock(oStream);

        // -- Decode the Y4 block
        if (DebugVerbose >= 4) System.err.println("Y4 block");
        oY4Matrix = DecodeBlock(oStream);

        // The returned color space is 
        // Y : -128 to +127
        // Cb: -128 to +127
        // Cr: -128 to +127
        
        // Combine all these into a 16x16 YCbCr (YUV) macro block
        PsxYuv oMacroBlock = new PsxYuv(16, 16);
        
        oMacroBlock.setY(0, 0, 8, 8, oY1Matrix.getPoints());
        oMacroBlock.setY(8, 0, 8, 8, oY2Matrix.getPoints());
        oMacroBlock.setY(0, 8, 8, 8, oY3Matrix.getPoints());
        oMacroBlock.setY(8, 8, 8, 8, oY4Matrix.getPoints());
        
        oMacroBlock.setCbCr(0, 0, 8, 8, oCbMatrix.getPoints(), 
                                        oCrMatrix.getPoints());
        
        return oMacroBlock;
        
    }
    
    /** Decodes a single block from oStream. */
    private static Matrix8x8 DecodeBlock(InputStream oStream) throws IOException 
    {
        
        long lngMdecWord; // little-endian 16 bits read from the stream
        Mdec16Bits oRunLenCode;
        int iZigZagVector[] = new int[64];
        
        int iQuantizationScale;
        int iDC_Coefficient;

        Matrix8x8 oQuantiziedMatrix; // Matrix to be de-Quantizied
        Matrix8x8 oDCTMatrix;  // Matrix to have IDCT performed on it
        Matrix8x8 oYCbCrMatrix;// Final matrix containing Y, Cb, or Cr component
        
        // Read the quantization scale and DC Coefficient
        // The data is stored: 6 bits for the quantization scale
        //                    10 bits for the DC coefficient
        lngMdecWord = IO.ReadUInt16LE(oStream);
        oRunLenCode = new Mdec16Bits(lngMdecWord);
        iQuantizationScale = oRunLenCode.Top6Bits;
        iDC_Coefficient = oRunLenCode.Bottom10Bits;
        // we'll add the coefficient to the matrix in a little bit
        
        if (DebugVerbose >= 8)
            System.err.println("Quantization scale: " + iQuantizationScale + 
                               "  DC Coeff: " + iDC_Coefficient);

        ////////////////////////////////////////////////////////////////////////
        // Step 1) Decode the run-length codes into a vector
        ////////////////////////////////////////////////////////////////////////
        int iVectorPos = 1; // current position in the vector
        lngMdecWord = IO.ReadUInt16LE(oStream); // read 16 bits
        while (lngMdecWord != MDEC_END_OF_BLOCK) {
        
            // The data is stored: 6 bits for the run length of zeros
            //                    10 bits for the value
            oRunLenCode = new Mdec16Bits(lngMdecWord);
            iVectorPos += oRunLenCode.Top6Bits; // skip the run length zeros
            
            if (DebugVerbose >= 8) {
                if (oStream instanceof IGetFilePointer)
                    System.err.print(
                            ((IGetFilePointer)oStream).getFilePointer() + " :"
                    );
                
                System.err.println("(" + oRunLenCode.Top6Bits + ", " + 
                                        oRunLenCode.Bottom10Bits + ")");
            }
            
            // The decoded run length codes should never exceed 64
            if (iVectorPos > 63) {
                throw new IOException("[MDEC] Run length out of bounds: " + 
                                       (iVectorPos + 1));
            }
            
            // Add the value
            iZigZagVector[iVectorPos] = oRunLenCode.Bottom10Bits;
            iVectorPos += 1;
            
            // read the next 16 bits
            lngMdecWord = IO.ReadUInt16LE(oStream);
        }
        if (DebugVerbose >= 8)
            System.err.println("END OF BLOCK");
        
        ////////////////////////////////////////////////////////////////////////
        // Step 2) Convert the zig-zag vector to a matrix
        ////////////////////////////////////////////////////////////////////////
        oQuantiziedMatrix = UnZigZagVector(iZigZagVector);
        if (DebugVerbose >= 7) {
            System.err.println("Run length codes converted to a matrix");
            System.err.println(oQuantiziedMatrix.toString());
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Step 3) de-quanitize the matrix with the PSX matrix, and the
        //         block's quantization scale.
        ////////////////////////////////////////////////////////////////////////
        oDCTMatrix = Dequantize(iDC_Coefficient, oQuantiziedMatrix,
                PSX_DEFAULT_INTRA_QUANTIZATION_MATRIX,
                (double)iQuantizationScale); 
        
        if (DebugVerbose >= 7) {
            System.err.println("After dequantization");
            System.err.println(oDCTMatrix.toString());
        }
        
        ////////////////////////////////////////////////////////////////////////
        // Step 4) Finally, perform the inverse discrete cosine transform
        ////////////////////////////////////////////////////////////////////////
        oYCbCrMatrix = InverseDiscreteCosineTransform(oDCTMatrix);
        if (DebugVerbose >= 6) { 
            System.err.println("After IDCT");
            System.err.println(oYCbCrMatrix.toString()); 
        }
        
        return oYCbCrMatrix;
        
    }
    
    
    
    private static Matrix8x8 UnZigZagVector(int aiVector[]) {
        Matrix8x8 tUnZigZagedList = new Matrix8x8();
        
        for (int x = 0; x < tUnZigZagedList.getWidth(); x++) {
            for (int y = 0; y < tUnZigZagedList.getHeight(); y++) {
                tUnZigZagedList.setPoint(x, y, 
                    aiVector[(int)REVERSE_ZIG_ZAG_SCAN_MATRIX.getPoint(x, y)]);
            }
        }
        
        return tUnZigZagedList;
    }
    
    /** Dequanitizes the matrix using the table and scale. */
    private static Matrix8x8 Dequantize(int iDC_Coefficient,
                                         Matrix8x8 oMatrix, 
                                         Matrix8x8 QuantizationTable, 
                                         double dblScale) 
    {
        assert(QuantizationTable.getWidth() == oMatrix.getWidth() && 
               QuantizationTable.getHeight() == oMatrix.getHeight());

        Matrix8x8 oQuantizedMatrix = new Matrix8x8();

        for (int y=0; y < oMatrix.getHeight(); y++) {
            for (int x=0; x < oMatrix.getWidth(); x++) {
                oQuantizedMatrix.setPoint(x, y, 
                    2 * oMatrix.getPoint(x, y) 
                      * QuantizationTable.getPoint(x, y)
                      * dblScale 
                    / 16);
            }
        }
        // Now we add the DC coefficint to the matrix, also de-quanitize it.
        oQuantizedMatrix.setPoint(0, 0, 
                     iDC_Coefficient * QuantizationTable.getPoint(0, 0));
        
        return oQuantizedMatrix;
    }
    
    /** Performs the  Inverse Discrete Cosine Transform on a matrix.
     *  This uses the IDCT static variable object to perform the
     *  operation. */
    private static Matrix8x8 InverseDiscreteCosineTransform(Matrix8x8 oMatrix) {
        return IDCT.IDCT(oMatrix);
    }

}
