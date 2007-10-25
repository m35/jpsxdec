/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


/*
 * StrFrameMDEC.java
 *
 */

package jpsxdec;

import java.awt.image.BufferedImage;
import java.io.*;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.LittleEndianIO;
import jpsxdec.util.Matrix8x8;
import jpsxdec.util.Yuv4mpeg2;

/** Simple (and slow) emulation of the Playstation MDEC ("Motion Decoder") chip.
  * While it doesn't process 9000 macroblocks per second, it shouldn't be very
  * difficult to read. */
public final class StrFrameMDEC {
    
    /** How much debugging do you want to see? */
    public static int DebugVerbose = 2;
    
    // :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    
    /** This is the default intra quantization matrix used to scale
     *  the post-DCT matrix. This matrix originates from MPEG-1 spec. 
     *  I assmue these values were determined through lots of testing for 
     *  an optimal matrix. */
    static final Matrix8x8 MPEG1_DEFAULT_INTRA_QUANTIZATION_MATRIX = 
            new Matrix8x8(new double[] {
                 8, 16, 19, 22, 26, 27, 29, 34, 
                16, 16, 22, 24, 27, 29, 34, 37, 
                19, 22, 26, 27, 29, 34, 34, 38, 
                22, 22, 26, 27, 29, 34, 37, 40, 
                22, 26, 27, 29, 32, 35, 40, 48, 
                26, 27, 29, 32, 35, 40, 48, 58, 
                26, 27, 29, 34, 38, 46, 56, 69, 
                27, 29, 35, 38, 46, 56, 69, 83 
    });
    
    /** This is the order that the zig-zag vector is ordered */
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
    
    /** The 16 bit value for the MDEC indicating the end of a block.
     *  (the variable-length-code "10" will be translated to this) */
    public final static int MDEC_END_OF_BLOCK = 0xFE00;
    
    /** Simple class to manage 16 bit values to be passed into the MDEC.
     *  The MDEC accepts little-endian 16 bit integers, where the top 6 bits 
     *  hold one value, and the bottom 10 bits hold another. In most cases
     *  these are AC run-length codes, with the 6 bit value holding how many
     *  0-value AC coefficients preceeds the 10 bit non-zero value AC 
     *  coefficient. The only exception is when the 6 bit value holds the
     *  quantization scale, and the 10 bit value holds the DC coefficient. */
    public static class Mdec16Bits {
        
        String OriginalVariableLengthCodeBits;
        double OriginalFilePos = -1;
        int Top6Bits;
        int Bottom10Bits;

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
        public long ToMdecWord() {
            return (((long)Top6Bits & 63) << 10) | ((long)Bottom10Bits & 0x3FF); 
        }
    }
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Main input function. Reads MDEC codes from the stream until an
     *  entire image of width x height has been decoded. Returns the
     *  decoded image as a yuv4mpeg2 class. */
    public static Yuv4mpeg2 DecodeFrame(InputStream oStream, 
                                        long lngWidth, long lngHeight)
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
        
        Yuv4mpeg2 oImg = new Yuv4mpeg2((int)lngActualWidth, 
                                       (int)lngActualHeight);
            
        try {
            
            int iMacroBlockCount = 0;
            
            // The macro blocks are ordered in columns
            for (int iX = 0; iX < lngActualWidth; iX += 16) {
                for (int iY = 0; iY < lngActualHeight; iY += 16) {
                    Yuv4mpeg2 oMacroBlockYuv = DecodeMacroBlock(oStream);
                    iMacroBlockCount++;
                    
                    oImg.putYuvImage(iX, iY, oMacroBlockYuv);
                }
            }
        } catch (IOException ex) {
            if (DebugVerbose > 2)
                ex.printStackTrace();
            else
                System.err.println(ex.getMessage());
        }
        
        return oImg;
    }
    
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Decodes an entire macro block from oStream and returns it as a 
     *  16x16 YUV image. */
    private static Yuv4mpeg2 DecodeMacroBlock(InputStream oStream) 
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
        Yuv4mpeg2 oMacroBlock = new Yuv4mpeg2(16, 16);
        
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
        lngMdecWord = LittleEndianIO.ReadUInt16LE(oStream);
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
        lngMdecWord = LittleEndianIO.ReadUInt16LE(oStream); // read 16 bits
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
            lngMdecWord = LittleEndianIO.ReadUInt16LE(oStream);
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
        // Step 3) de-quanitize the matrix with the mpeg-1 matrix, and the
        //         block's quantization scale.
        ////////////////////////////////////////////////////////////////////////
        oDCTMatrix = Dequanitize(oQuantiziedMatrix,
                MPEG1_DEFAULT_INTRA_QUANTIZATION_MATRIX,
                (double)iQuantizationScale); 
        
        // Now we add the DC coefficint to the matrix, also de-quanitize it.
        oDCTMatrix.setPoint(0, 0, iDC_Coefficient 
                     * MPEG1_DEFAULT_INTRA_QUANTIZATION_MATRIX.getPoint(0, 0));
        
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
    private static Matrix8x8 Dequanitize(Matrix8x8 oMatrix, 
                                         Matrix8x8 QuantizationTable, 
                                         double dblScale) 
    {
        assert(QuantizationTable.getWidth() == oMatrix.getWidth() && 
               QuantizationTable.getHeight() == oMatrix.getHeight());

        Matrix8x8 oQuantizedMatrix = new Matrix8x8();

        for (int y=0; y < oMatrix.getHeight(); y++) {
            for (int x=0; x < oMatrix.getWidth(); x++) {
                oQuantizedMatrix.setPoint(x, y, 
                    // I don't know why this needs to be divided by two, 
                    // but it provides the most correct output.
                    oMatrix.getPoint(x, y) 
                    * QuantizationTable.getPoint(x, y)
                    * dblScale / 2);
            }
        }
        return oQuantizedMatrix;
    }
    
    private static Matrix8x8 InverseDiscreteCosineTransform(Matrix8x8 oMatrix) {
        //return IDCT.InverseDiscreteCosineTransform_Simple(oMatrix);
        /* Using the optimized version is about 13 times faster!!!!
         * The results are almost indistinguishable. I don't think there
         * is any intentional loss of precision with the optimized version.
         * It's just how the floating point numbers happen to round */
        return IDCT.InverseDiscreteCosineTransform_Optimized(oMatrix);
    }
    
    /**
     * <h3><b>A Java implementation of the Inverse Discreet Cosine Transform</b></h3><br><br>
     * <hr>
     * The discreet cosine transform converts spatial information to "frequency" or
     * spectral information, with the X and Y axes representing frequencies of the
     * signal in different dimensions. This allows for "lossy" compression of image
     * data by determining which information can be thrown away without compromising
     * the image.<br><br>
     * The DCT is used in many compression and transmission codecs, such as JPEG, MPEG
     * and others. The pixels when transformed are arraged from the most signifigant pixel
     * to the least signifigant pixel. The DCT functions themselves are lossless.
     * Pixel loss occurs when the least signifigant pixels are quantitized to 0.
     * <br><br>
     * The best way to get ahold of me is through my
     * <a href="http://eagle.uccb.ns.ca/steve/home.html">homepage</a>. There's
     * lots of goodies there too.
     * @version 1.0.1 August 22nd 1996
     * @author <a href="http://eagle.uccb.ns.ca/steve/home.html">Stephen Manley</a> - smanley@eagle.uccb.ns.ca
         */
    private static class IDCT {
        
        /** This is the infamous Inverse Discrete Cosine Transform.
         * If I understand correctly, it's inverse 2D DCT-II, specifically.
         * It's as simple as I could make it, and as such, it's about as
         * slow as can be. 
         * @author Me
         */
        private static Matrix8x8 InverseDiscreteCosineTransform_Simple(Matrix8x8 oDCTMat) {

            int iWidth = oDCTMat.getWidth();
            int iHeight = oDCTMat.getHeight();

            Matrix8x8 oPixelMat = new Matrix8x8(/*iWidth, iHeight*/);

            int Pixelx, Pixely, DCTx, DCTy;

            for (Pixelx = 0; Pixelx < iWidth; Pixelx++) {
                for (Pixely = 0; Pixely < iHeight; Pixely++) {

                    double dblTotal = 0;

                    for (DCTx = 0; DCTx < iWidth; DCTx++) {
                        for (DCTy = 0; DCTy < iHeight; DCTy++) {

                            double dblSubTotal = oDCTMat.getPoint(DCTx, DCTy);

                            if (DCTx == 0)
                                dblSubTotal *= Math.sqrt(1.0f / iWidth);
                            else
                                dblSubTotal *= Math.sqrt(2.0f / iWidth);

                            if (DCTy == 0)
                                dblSubTotal *= Math.sqrt(1.0f / iHeight);
                            else
                                dblSubTotal *= Math.sqrt(2.0f / iHeight);

                            dblSubTotal 
                                *= Math.cos( DCTx * Math.PI * (2 * Pixelx + 1) 
                                                / (2.0f * iWidth)            )
                                *  Math.cos( DCTy * Math.PI * (2 * Pixely + 1) 
                                                / (2.0f * iHeight)           );

                            dblTotal += dblSubTotal;
                        }
                    }

                    oPixelMat.setPoint(Pixelx, Pixely, dblTotal / 4);
                }
            }

            return oPixelMat;
        }



        /**
         * This method is preformed using the reverse of the operations preformed in
         * the DCT. This restores a N * N input block to the corresponding output
         * block and then stored in the input block of pixels.
         *
         * @param input N * N Matrix
         * @return output The pixel array output
         */
        private static Matrix8x8 InverseDiscreteCosineTransform_Optimized(Matrix8x8 input)
        {
            assert(input.getWidth() == N && input.getHeight() == N);
            Matrix8x8 output = new Matrix8x8(/*N, N*/);
            Matrix8x8 temp = new Matrix8x8(/*N, N*/);
            double temp1;
            int i;
            int j;
            int k;

            for (i=0; i<N; i++)
            {
                for (j=0; j<N; j++)
                {
                    temp.setPoint(i, j, 0.0);

                    for (k=0; k<N; k++)
                    {
                        temp.setAddPoint(i, j, input.getPoint(i, k) * c[k][j]);
                    }
                }
            }

            for (i=0; i<N; i++)
            {
                for (j=0; j<N; j++)
                {
                    temp1 = 0.0;

                    for (k=0; k<N; k++)
                    {
                        temp1 += cT[i][k] * temp.getPoint(k, j);
                    }

                     output.setPoint(i, j, temp1 / 4); 
                }
            }

            return output;
        }

        /** DCT Block Size */
        private final static int N  = 8;

        /** Cosine matrix. N * N. */
        private final static double c[][]        = init_c_Matrix();

        /** Transformed cosine matrix, N*N. */
        private final static double cT[][]       = transpose_c_Matrix(c);

        /**
         * This method initializes the Cosine Transform Matrix.
         * This is used by the inverse DCT. 
         */
        private static double[][] init_c_Matrix()
        {
            int i;
            int j;
            double _c[][] = new double[N][N];

            for (j = 0; j < N; j++)
            {
                double nn = (double)(N);
                _c[0][j]  = 1.0 / Math.sqrt(nn);
            }

            for (i = 1; i < N; i++)
            {
                for (j = 0; j < N; j++)
                {
                    double nn = (double)(N);
                    double jj = (double)j;
                    double ii = (double)i;
                    _c[i][j]  = Math.sqrt(2.0/nn) * Math.cos(((2.0 * jj + 1.0) * ii * Math.PI) 
                                                    / (2.0 * nn));
                }
            }

            return _c;
        }

        /**
         * This method initializes the Transposed CT.
         * This is used by the inverse DCT. 
         */
        private static double[][] transpose_c_Matrix(double _c[][])
        {
            int i;
            int j;
            double _cT[][] = new double[N][N];

            for (j = 0; j < N; j++)
            {
                _cT[j][0] = _c[0][j];
            }

            for (i = 1; i < N; i++)
            {
                for (j = 0; j < N; j++)
                {
                    _cT[j][i] = _c[i][j];
                }
            }

            return _cT;
        }
    }
}
