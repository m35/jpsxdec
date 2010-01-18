package jpsxdec.plugins.psx.video.mdec.idct;

/*
 * J2ME_MPEG: MPEG-1 decoder for J2ME
 *
 * Copyright (c) 2009 Sequence Point Software S.L.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */

/*
 * The discrete cosine transform (DCT) converts an 8 by 8 block of pel
 * values to an 8 by 8 matrix of horizontal and vertical spatial frequency 
 * coefficients. An 8 by 8 block of pel values can be reconstructed by 
 * performing the inverse discrete cosine transform (IDCT) on the spatial 
 * frequency coefficients. In general, most of the energy is concentrated 
 * in the low frequency coefficients, which are located in the upper left 
 * corner of the transformed matrix.
 * 
 * References: 
 *   C. Loeffler, A. Ligtenberg and G. Moschytz, "Practical Fast 1-D DCT
 *   Algorithms with 11 Multiplications", Proc. Int'l. Conf. on Acoustics,
 *   Speech, and Signal Processing 1989 (ICASSP '89), pp. 988-991.
 *   
 *   A. N. Netravali, B.G. Haskell, 
 *   "Digital Pictures - Representation and Compression", 
 *   2nd edit., New York, London: Plenum Press, 1995
 * <hr>
 * Originally from Sequence Point Software's J2ME_MPEG Idct.java
 * http://code.seqpoint.com/j2me_mpeg/
 */
public class J2meMpegIDCT implements IDCT_int {
    // Scale used for fixed point math
    public static final int FIXED_POINT_SCALE = 11;

    // DCT dimension is 64 (8x8)
    public static final int DCT_DIM = 64;

    // The basic DCT block is 8x8 samples
    public static final int DCT_SIZE = 8;
    public static final int HALF_DCT_SIZE = 4;

    private final long c1, c2, c3, c4, c5, c6, c7, c8, c9;

    public J2meMpegIDCT() {
    /*
     * We perform these calculations manually so the
     * code is CLDC 1.0 compliant.
     * 
     * 
     * final long factor = 1 << FIXED_POINT_SCALE;
     * final double alpha = Math.PI / 16.0;
     * 
     * c1 = (long)(factor * Math.cos(4.0 * alpha));
     * c2 = (long)(factor * Math.sin(4.0 * alpha));
     * c3 = (long)(factor * Math.cos(1.0 * alpha));
     * c4 = (long)(factor * Math.sin(1.0 * alpha));
     * c5 = (long)(factor * Math.cos(2.0 * alpha));
     * c6 = (long)(factor * Math.sin(2.0 * alpha));
     * c7 = (long)(factor * Math.cos(3.0 * alpha));
     * c8 = (long)(factor * Math.sin(3.0 * alpha));
     * c9 = (long)(factor * Math.sqrt(2.0));
     * 
     */

    	c1 = 1448;
    	c2 = 1448;
    	c3 = 2008;
    	c4 = 399;
    	c5 = 1892;
    	c6 = 783;
    	c7 = 1702;
    	c8 = 1137;
    	c9 = 2896;
    }

    public void norm(int[] quantizationMatrix) {
        
    }

    public void IDCT(int[] dct_coeff,
                     int iOutputOffset, int[] output)
    {
        for (int row = 0; row < DCT_SIZE; ++row)
            idctRow(dct_coeff, row);

        for (int col = 0; col < DCT_SIZE; ++col)
            idctCol(dct_coeff, col, iOutputOffset, output);
    }

    public void IDCT_1NonZero(int[] dct_coeff, int iNonZeroPos,
                              int iOutputOffset, int[] output)
    {
        for (int row = 0; row <= (iNonZeroPos >>> 3); ++row)
            idctRow(dct_coeff, row);

        for (int col = 0; col < DCT_SIZE; ++col)
            idctCol(dct_coeff, col, iOutputOffset, output);
    }

    private void idctRow(int[] dct_coeff, int row)
    {
        final long s1_0 = dct_coeff[row * DCT_SIZE + 4] << 3;
        final long s1_1 = dct_coeff[row * DCT_SIZE + 0] << 3;
        final long s1_2 = dct_coeff[row * DCT_SIZE + 1] << 3;
        final long s1_3 = dct_coeff[row * DCT_SIZE + 7] << 3;
        final long s1_4 = dct_coeff[row * DCT_SIZE + 2] << 3;
        final long s1_5 = dct_coeff[row * DCT_SIZE + 6] << 3;
        final long s1_6 = dct_coeff[row * DCT_SIZE + 3] << 3;
        final long s1_7 = dct_coeff[row * DCT_SIZE + 5] << 3;

        final long s2_0 = (((c1 * s1_0) >> FIXED_POINT_SCALE) + ((c2 * s1_1) >> FIXED_POINT_SCALE)) << 1;
        final long s2_1 = (((c5 * s1_4) >> FIXED_POINT_SCALE) + ((c6 * s1_5) >> FIXED_POINT_SCALE)) << 1;
        final long s2_2 = (((c1 * s1_1) >> FIXED_POINT_SCALE) - ((c2 * s1_0) >> FIXED_POINT_SCALE)) << 1;
        final long s2_3 = (((c5 * s1_5) >> FIXED_POINT_SCALE) - ((c6 * s1_4) >> FIXED_POINT_SCALE)) << 1;
        final long s2_4 = (((c3 * s1_2) >> FIXED_POINT_SCALE) + ((c4 * s1_3) >> FIXED_POINT_SCALE)) << 1;
        final long s2_5 = (((c7 * s1_6) >> FIXED_POINT_SCALE) + ((c8 * s1_7) >> FIXED_POINT_SCALE)) << 1;
        final long s2_6 = (((c3 * s1_3) >> FIXED_POINT_SCALE) - ((c4 * s1_2) >> FIXED_POINT_SCALE)) << 1;
        final long s2_7 = (((c7 * s1_7) >> FIXED_POINT_SCALE) - ((c8 * s1_6) >> FIXED_POINT_SCALE)) << 1;

        final long s3_0 = (s2_4 + s2_5) >> 1;
        final long s3_1 = (s2_6 - s2_7) >> 1;
        final long s3_2 = s2_0;
        final long s3_3 = (s2_4 - s2_5) >> 1;
        final long s3_4 = s2_1;
        final long s3_5 = s2_2;
        final long s3_6 = s2_3;
        final long s3_7 = (s2_6 + s2_7) >> 1;

        final long s4_0 = s3_0;
        final long s4_1 = s3_1;
        final long s4_2 = s3_2;
        final long s4_3 = s3_4;
        final long s4_4 = s3_5;
        final long s4_5 = (s3_3 * c9) >> FIXED_POINT_SCALE;
        final long s4_6 = s3_6;
        final long s4_7 = (s3_7 * c9) >> FIXED_POINT_SCALE;

        final long s5_0 = (s4_2 + s4_3) >> 1;
        final long s5_1 = s4_0;
        final long s5_2 = (s4_4 + s4_5) >> 1;
        final long s5_3 = (s4_6 + s4_7) >> 1;
        final long s5_4 = (s4_2 - s4_3) >> 1;
        final long s5_5 = s4_1;
        final long s5_6 = (s4_4 - s4_5) >> 1;
        final long s5_7 = (s4_6 - s4_7) >> 1;

        final long d0 = (s5_0 + s5_1) >> 1;
        final long d1 = (s5_2 - s5_3) >> 1;
        final long d2 = (s5_2 + s5_3) >> 1;
        final long d3 = (s5_4 - s5_5) >> 1;
        final long d4 = (s5_4 + s5_5) >> 1;
        final long d5 = (s5_6 + s5_7) >> 1;
        final long d6 = (s5_6 - s5_7) >> 1;
        final long d7 = (s5_0 - s5_1) >> 1;

        dct_coeff[row * DCT_SIZE + 0] = d0 < 0? (short)((d0 - HALF_DCT_SIZE) >> 3) : (short)((d0 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 1] = d1 < 0? (short)((d1 - HALF_DCT_SIZE) >> 3) : (short)((d1 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 2] = d2 < 0? (short)((d2 - HALF_DCT_SIZE) >> 3) : (short)((d2 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 3] = d3 < 0? (short)((d3 - HALF_DCT_SIZE) >> 3) : (short)((d3 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 4] = d4 < 0? (short)((d4 - HALF_DCT_SIZE) >> 3) : (short)((d4 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 5] = d5 < 0? (short)((d5 - HALF_DCT_SIZE) >> 3) : (short)((d5 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 6] = d6 < 0? (short)((d6 - HALF_DCT_SIZE) >> 3) : (short)((d6 + HALF_DCT_SIZE) >> 3);
        dct_coeff[row * DCT_SIZE + 7] = d7 < 0? (short)((d7 - HALF_DCT_SIZE) >> 3) : (short)((d7 + HALF_DCT_SIZE) >> 3);
    }

    private void idctCol(int[] dct_coeff, int col,
                         int iOutputOffset, int[] output)
    {
    	final long s1_0 = dct_coeff[4 * DCT_SIZE + col] << 3;
    	final long s1_1 = dct_coeff[0 * DCT_SIZE + col] << 3;
    	final long s1_2 = dct_coeff[1 * DCT_SIZE + col] << 3;
    	final long s1_3 = dct_coeff[7 * DCT_SIZE + col] << 3;
    	final long s1_4 = dct_coeff[2 * DCT_SIZE + col] << 3;
    	final long s1_5 = dct_coeff[6 * DCT_SIZE + col] << 3;
    	final long s1_6 = dct_coeff[3 * DCT_SIZE + col] << 3;
    	final long s1_7 = dct_coeff[5 * DCT_SIZE + col] << 3;

    	final long s2_0 = (((c1 * s1_0) >> FIXED_POINT_SCALE) + ((c2 * s1_1) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_1 = (((c5 * s1_4) >> FIXED_POINT_SCALE) + ((c6 * s1_5) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_2 = (((c1 * s1_1) >> FIXED_POINT_SCALE) - ((c2 * s1_0) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_3 = (((c5 * s1_5) >> FIXED_POINT_SCALE) - ((c6 * s1_4) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_4 = (((c3 * s1_2) >> FIXED_POINT_SCALE) + ((c4 * s1_3) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_5 = (((c7 * s1_6) >> FIXED_POINT_SCALE) + ((c8 * s1_7) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_6 = (((c3 * s1_3) >> FIXED_POINT_SCALE) - ((c4 * s1_2) >> FIXED_POINT_SCALE)) << 1;
    	final long s2_7 = (((c7 * s1_7) >> FIXED_POINT_SCALE) - ((c8 * s1_6) >> FIXED_POINT_SCALE)) << 1;

    	final long s3_0 = (s2_4 + s2_5) >> 1;
    	final long s3_1 = (s2_6 - s2_7) >> 1;
    	final long s3_2 = s2_0;
    	final long s3_3 = (s2_4 - s2_5) >> 1;
    	final long s3_4 = s2_1;
    	final long s3_5 = s2_2;
    	final long s3_6 = s2_3;
    	final long s3_7 = (s2_6 + s2_7) >> 1;

    	final long s4_0 = s3_0;
    	final long s4_1 = s3_1;
    	final long s4_2 = s3_2;
    	final long s4_3 = s3_4;
    	final long s4_4 = s3_5;
    	final long s4_5 = (s3_3 * c9) >> FIXED_POINT_SCALE;
    	final long s4_6 = s3_6;
    	final long s4_7 = (s3_7 * c9) >> FIXED_POINT_SCALE;

    	final long s5_0 = (s4_2 + s4_3) >> 1;
    	final long s5_1 = s4_0;
    	final long s5_2 = (s4_4 + s4_5) >> 1;
    	final long s5_3 = (s4_6 + s4_7) >> 1;
    	final long s5_4 = (s4_2 - s4_3) >> 1;
    	final long s5_5 = s4_1;
    	final long s5_6 = (s4_4 - s4_5) >> 1;
    	final long s5_7 = (s4_6 - s4_7) >> 1;

    	final long d0 = (s5_0 + s5_1) >> 1;
    	final long d1 = (s5_2 - s5_3) >> 1;
    	final long d2 = (s5_2 + s5_3) >> 1;
    	final long d3 = (s5_4 - s5_5) >> 1;
    	final long d4 = (s5_4 + s5_5) >> 1;
    	final long d5 = (s5_6 + s5_7) >> 1;
    	final long d6 = (s5_6 - s5_7) >> 1;
    	final long d7 = (s5_0 - s5_1) >> 1;

        output[iOutputOffset + 0 * DCT_SIZE + col] = d0 < 0? (short)((d0 - HALF_DCT_SIZE) >> 3) : (short)((d0 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 1 * DCT_SIZE + col] = d1 < 0? (short)((d1 - HALF_DCT_SIZE) >> 3) : (short)((d1 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 2 * DCT_SIZE + col] = d2 < 0? (short)((d2 - HALF_DCT_SIZE) >> 3) : (short)((d2 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 3 * DCT_SIZE + col] = d3 < 0? (short)((d3 - HALF_DCT_SIZE) >> 3) : (short)((d3 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 4 * DCT_SIZE + col] = d4 < 0? (short)((d4 - HALF_DCT_SIZE) >> 3) : (short)((d4 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 5 * DCT_SIZE + col] = d5 < 0? (short)((d5 - HALF_DCT_SIZE) >> 3) : (short)((d5 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 6 * DCT_SIZE + col] = d6 < 0? (short)((d6 - HALF_DCT_SIZE) >> 3) : (short)((d6 + HALF_DCT_SIZE) >> 3);
        output[iOutputOffset + 7 * DCT_SIZE + col] = d7 < 0? (short)((d7 - HALF_DCT_SIZE) >> 3) : (short)((d7 + HALF_DCT_SIZE) >> 3);
    }
}
