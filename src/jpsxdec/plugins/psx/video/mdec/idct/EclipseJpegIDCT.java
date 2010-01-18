/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jpsxdec.plugins.psx.video.mdec.idct;

/** Originally from a slightly older version of
 *  org.eclipse.swt.internal.image.JPEGFileFormat. */
public class EclipseJpegIDCT implements IDCT_int {
	/* JPEGConstants */
	private static final int DCTSIZE = 8;
	/* JPEGFixedPointConstants */
	private static final int FIX_0_899976223 = 7373;
	private static final int FIX_1_961570560 = 16069;
	private static final int FIX_2_053119869 = 16819;
	private static final int FIX_0_298631336 = 2446;
	private static final int FIX_1_847759065 = 15137;
	private static final int FIX_1_175875602 = 9633;
	private static final int FIX_3_072711026 = 25172;
	private static final int FIX_0_765366865 = 6270;
	private static final int FIX_2_562915447 = 20995;
	private static final int FIX_0_541196100 = 4433;
	private static final int FIX_0_390180644 = 3196;
	private static final int FIX_1_501321110 = 12299;

    public void norm(int[] quantizationMatrix) {
    }

    public void IDCT_1NonZero(int[] dataUnit, int iNonZeroPos, int iOutputOffset, int[] output) {
        IDCT(dataUnit, iOutputOffset, output);
    }

    public void IDCT(int[] dataUnit, int iOutputOffset, int[] output) {
        for (int row = 0; row < 8; row++) {
            int rIndex = row * DCTSIZE;

            final int r0v = dataUnit[rIndex + 0];
            final int r1v = dataUnit[rIndex + 1];
            final int r2v = dataUnit[rIndex + 2];
            final int r3v = dataUnit[rIndex + 3];
            final int r4v = dataUnit[rIndex + 4];
            final int r5v = dataUnit[rIndex + 5];
            final int r6v = dataUnit[rIndex + 6];
            final int r7v = dataUnit[rIndex + 7];

            /**
             * Due to quantization, we will usually find that many of the input
             * coefficients are zero, especially the AC terms.  We can exploit this
             * by short-circuiting the IDCT calculation for any row in which all
             * the AC terms are zero.  In that case each output is equal to the
             * DC coefficient (with scale factor as needed).
             * With typical images and quantization tables, half or more of the
             * row DCT calculations can be simplified this way.
             */
            if ((r1v|r2v|r3v|r4v|r5v|r6v|r7v) == 0) {
                int dcVal = r0v * 4;
                dataUnit[rIndex] = dcVal;
                dataUnit[rIndex+1] = dcVal;
                dataUnit[rIndex+2] = dcVal;
                dataUnit[rIndex+3] = dcVal;
                dataUnit[rIndex+4] = dcVal;
                dataUnit[rIndex+5] = dcVal;
                dataUnit[rIndex+6] = dcVal;
                dataUnit[rIndex+7] = dcVal;
            } else {
                /**
                 * Even part: reverse the even part of the forward DCT.
                 * The rotator is sqrt(2)*c(-6).
                 */
                int z2 = r2v;
                int z3 = r6v;
                int z1 = (z2 + z3) * FIX_0_541196100;
                int tmp2 = z1 + (z3 * (0 - FIX_1_847759065));
                int tmp3 = z1 + (z2 * FIX_0_765366865);
                int tmp0 = (r0v + r4v) * 8192;
                int tmp1 = (r0v - r4v) * 8192;
                int tmp10 = tmp0 + tmp3;
                int tmp13 = tmp0 - tmp3;
                int tmp11 = tmp1 + tmp2;
                int tmp12 = tmp1 - tmp2;
                /**
                 * Odd part per figure 8; the matrix is unitary and hence its
                 * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
                 */
                tmp0 = r7v;
                tmp1 = r5v;
                tmp2 = r3v;
                tmp3 = r1v;
                z1 = tmp0 + tmp3;
                z2 = tmp1 + tmp2;
                z3 = tmp0 + tmp2;
                int z4 = tmp1 + tmp3;
                int z5 = (z3 + z4)* FIX_1_175875602; /* sqrt(2) * c3 */

				tmp0 *= FIX_0_298631336;		/* sqrt(2) * (-c1+c3+c5-c7) */
				tmp1 *= FIX_2_053119869;		/* sqrt(2) * ( c1+c3-c5+c7) */
				tmp2 *= FIX_3_072711026;		/* sqrt(2) * ( c1+c3+c5-c7) */
				tmp3 *= FIX_1_501321110;		/* sqrt(2) * ( c1+c3-c5-c7) */
				z1 *= 0 - FIX_0_899976223;	/* sqrt(2) * (c7-c3) */
				z2 *= 0 - FIX_2_562915447;	/* sqrt(2) * (-c1-c3) */
				z3 *= 0 - FIX_1_961570560;	/* sqrt(2) * (-c3-c5) */
				z4 *= 0 - FIX_0_390180644;	/* sqrt(2) * (c5-c3) */
	
				z3 += z5;
				z4 += z5;
				tmp0 += z1 + z3;
				tmp1 += z2 + z4;
				tmp2 += z2 + z3;
				tmp3 += z1 + z4;

			dataUnit[rIndex] = (tmp10 + tmp3 + 1024) >> 11;
			dataUnit[rIndex + 7] = (tmp10 - tmp3 + 1024) >> 11;
			dataUnit[rIndex + 1] = (tmp11 + tmp2 + 1024) >> 11;
			dataUnit[rIndex + 6] = (tmp11 - tmp2 + 1024) >> 11;
			dataUnit[rIndex + 2] = (tmp12 + tmp1 + 1024) >> 11;
			dataUnit[rIndex + 5] = (tmp12 - tmp1 + 1024) >> 11;
			dataUnit[rIndex + 3] = (tmp13 + tmp0 + 1024) >> 11;
			dataUnit[rIndex + 4] = (tmp13 - tmp0 + 1024) >> 11;
             }
        }
        /**
         * Pass 2: process columns.
         * Note that we must descale the results by a factor of 8 == 2**3,
         * and also undo the PASS1_BITS scaling.
         */
        for (int col = 0; col < 8; col++) {
            final int c0 = col;
            final int c1 = col + 8;
            final int c2 = col + 16;
            final int c3 = col + 24;
            final int c4 = col + 32;
            final int c5 = col + 40;
            final int c6 = col + 48;
            final int c7 = col + 56;

            final int c0v = dataUnit[c0];
            final int c1v = dataUnit[c1];
            final int c2v = dataUnit[c2];
            final int c3v = dataUnit[c3];
            final int c4v = dataUnit[c4];
            final int c5v = dataUnit[c5];
            final int c6v = dataUnit[c6];
            final int c7v = dataUnit[c7];

            if ((c1v|c2v|c3v|c4v|c5v|c6v|c7v) == 0) {
                int dcVal = (c0v + 16) / 32;
                output[iOutputOffset+ c0] = dcVal;
                output[iOutputOffset+ c1] = dcVal;
                output[iOutputOffset+ c2] = dcVal;
                output[iOutputOffset+ c3] = dcVal;
                output[iOutputOffset+ c4] = dcVal;
                output[iOutputOffset+ c5] = dcVal;
                output[iOutputOffset+ c6] = dcVal;
                output[iOutputOffset+ c7] = dcVal;
            } else {
                /**
                 * Even part: reverse the even part of the forward DCT.
                 * The rotator is sqrt(2)*c(-6).
                 */
                int z2 = c2v;
                int z3 = c6v;
                int z1 = (z2 + z3) * FIX_0_541196100;
                int tmp2 = z1 + (z3 * (0 - FIX_1_847759065));
                int tmp3 = z1 + (z2 * FIX_0_765366865);
                int tmp0 = (c0v + c4v) * 8192;
                int tmp1 = (c0v - c4v) * 8192;
                int tmp10 = tmp0 + tmp3;
                int tmp13 = tmp0 - tmp3;
                int tmp11 = tmp1 + tmp2;
                int tmp12 = tmp1 - tmp2;
                /**
                 * Odd part per figure 8; the matrix is unitary and hence its
                 * transpose is its inverse. i0..i3 are y7,y5,y3,y1 respectively.
                 */
                tmp0 = c7v;
                tmp1 = c5v;
                tmp2 = c3v;
                tmp3 = c1v;
                z1 = tmp0 + tmp3;
                z2 = tmp1 + tmp2;
                z3 = tmp0 + tmp2;
                int z4 = tmp1 + tmp3;
                int z5 = (z3 + z4) * FIX_1_175875602;	/* sqrt(2) * c3 */

                tmp0 = tmp0 * FIX_0_298631336;		/* sqrt(2) * (-c1+c3+c5-c7) */
                tmp1 = tmp1 * FIX_2_053119869;		/* sqrt(2) * ( c1+c3-c5+c7) */
                tmp2 = tmp2 * FIX_3_072711026;		/* sqrt(2) * ( c1+c3+c5-c7) */
                tmp3 = tmp3 * FIX_1_501321110;		/* sqrt(2) * ( c1+c3-c5-c7) */
                z1 = z1 * (0 - FIX_0_899976223);	/* sqrt(2) * (c7-c3) */
                z2 = z2 * (0 - FIX_2_562915447);	/* sqrt(2) * (-c1-c3) */
                z3 = z3 * (0 - FIX_1_961570560);	/* sqrt(2) * (-c3-c5) */
                z4 = z4 * (0 - FIX_0_390180644);	/* sqrt(2) * (c5-c3) */

                z3 = z3 + z5;
                z4 = z4 + z5;

                tmp0 = tmp0 + z1 + z3;
                tmp1 = tmp1 + z2 + z4;
                tmp2 = tmp2 + z2 + z3;
                tmp3 = tmp3 + z1 + z4;

                /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */
                output[iOutputOffset+ c0] = (tmp10 + tmp3 + 131072) / 262144;
                output[iOutputOffset+ c7] = (tmp10 - tmp3 + 131072) / 262144;
                output[iOutputOffset+ c1] = (tmp11 + tmp2 + 131072) / 262144;
                output[iOutputOffset+ c6] = (tmp11 - tmp2 + 131072) / 262144;
                output[iOutputOffset+ c2] = (tmp12 + tmp1 + 131072) / 262144;
                output[iOutputOffset+ c5] = (tmp12 - tmp1 + 131072) / 262144;
                output[iOutputOffset+ c3] = (tmp13 + tmp0 + 131072) / 262144;
                output[iOutputOffset+ c4] = (tmp13 - tmp0 + 131072) / 262144;
            }
        }
    }
}
