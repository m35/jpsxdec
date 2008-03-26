package jpsxdec.mdec;

/* 
 * Only a handful of differences from the original IDCT.java file:
 * - Line 1: Package change
 * - Lines 14-80: Changed header comment to be javadoc compatable
 * - Line 82: implements IDCTinterface
 * - Lines 89-96: Changed #define lines to Java final variables
 * - Line 127: Change norm() function to accept an array of doubles
 * - Lines 147-176 : Added IDCT() function
 *
 */

/*
 ****************************************************************************************
 *                                                                                      *
 *                                   "IDCT.java"                                        */
/**<pre>                                                                                *
 * This file contains the class "IDCT" which provides methods to perform the inverse    *
 * discrete cosine transform. Actually a IDFT (inverse discrete fourier transform) is   *
 * implemented. To gain the DFT values from the DCT values the quantization matrices    *
 * are multiplied with constants.  This occurs only once. The modified quantization     *
 * values cause a DCT-->DFT transformation during quantization step.                    *
 *                                                                                      *
 * The implementation follows the proposals of Arai, Agiu and Nakajima as well as of    *
 * Tseng and Miller completed by the extensions made by Feig. The reason is: Their      *
 * algorithm is a 2-D-approach which consumes only 462 additions, 54 multiplications    *
 * 6 shifts to the left per block. I thought this were be faster than the ordinary      *
 * horizontal/vertial- algorithms, which consume more operations. But whether this is   *
 * really true must be proved (or disproved) by a C implementation.                     *
 *                                                                                      *
 * To understand the algorithm refer to:                                                *
 * http://rnvs.informatik.tu-chemnitz.de/~ja/MPEG/HTML/IDCT.html                        *
 * The comments assume you are familar with this page.                                  *
 *                                                                                      *
 * To avoid double arithmetics the constants are multiplied with 2^CONST_BITS and       *
 * the DCT (DFT) coefficients are multiplied with 2^VAL_BITS. This is not a scientific  *
 * gained result. I tried some values and as I had the impression the error is quite    *
 * small I took the appropriate values. But feel free to experiment for your own.       *
 * BUT: If you change VAL_BITS don't forget to translate "MPEG_scan.java"! This	value   *
 * is used there.                                                                       *
 *                                                                                      *
 * The constants are in source text. I have replaced the "#define"s by a Java variable  *
 * declarations.                                                                        *
 *                                                                                      *
 *--------------------------------------------------------------------------------------*
 *                                                                                      *
 * Excuse the already computed constants! I sould write "matr2[8*6 + 1]" instead of     *
 * "matr2[49]" but I had the impression my Java compiler doesn't compute the constant   *
 * expression.(?). Use the equations:                                                   *
 *                                      row = index / 8                                 *
 *                                   column = index % 8                                 *
 * to determine row and column!                                                         *
 *                                                                                      *
 *--------------------------------------------------------------------------------------*
 *                                                                                      *
 *              Joerg Anders, TU Chemnitz, Fakultaet fuer Informatik, GERMANY           *
 *              ja@informatik.tu-chemnitz.de                                            *
 *                                                                                      *
 *--------------------------------------------------------------------------------------*
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under the    *
 * terms of the GNU General Public License as published by the Free Software            *
 * Foundation; either version 2 of the License, or (at your option) any later version.  *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with this    *
 * program; (See "LICENSE.GPL"). If not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                            *
 *                                                                                      *
 *--------------------------------------------------------------------------------------*
 *                                                                                      *
 * If the program runs as Java applet it isn't "interactive" in the sense of the GNU    *
 * General Public License. So paragraph 2c doesn't apply.                               *
 *                                                                                      *
 ****************************************************************************************
 </pre>*/

public class IDCT implements IDCTinterface {

    private final int CONST_BITS = 11;
    private final int VAL_BITS   = 11;
    private final int ALLBITS    = CONST_BITS + VAL_BITS;
    private final int TWO	 = CONST_BITS + 1; // The constant "2"
    
    private final int CONST_BITS_POW2 = (int)Math.pow(2, CONST_BITS);
    private final int C6   = (int)Math.round((2*Math.sin(Math.PI/8)) * CONST_BITS_POW2);
    private final int C4C6 = (int)Math.round((2*Math.sqrt(2)*Math.sin(Math.PI/8)) * CONST_BITS_POW2);
    private final int C4   = (int)Math.round((Math.sqrt(2))  * CONST_BITS_POW2);
    private final int Q    = (int)Math.round((2*(Math.cos(Math.PI/8)-Math.sin(Math.PI/8))) * CONST_BITS_POW2);
    private final int C4Q  = (int)Math.round((2*Math.sqrt(2)*(Math.cos(Math.PI/8)-Math.sin(Math.PI/8))) * CONST_BITS_POW2);
    private final int R    = (int)Math.round((2*(Math.cos(Math.PI/8)+Math.sin(Math.PI/8))) * CONST_BITS_POW2);
    private final int C4R  = (int)Math.round((2*Math.sqrt(2)*(Math.cos(Math.PI/8)+Math.sin(Math.PI/8))) * CONST_BITS_POW2);

    private int matr1[] = new int[64], matr2[] = new int[64]; // temporary storage

    // If only one AC coefficient is available the table of pre-computed
    // values is consulted:

    private static int IDFT_table[][] = new int[64][64];


    // initialize the "IDFT_table":

    public IDCT() {
        for (int i=0; i<64; i++) {
            for (int j = 0; j < 64; j++) {
                IDFT_table[i][j] = 0;
            }
            IDFT_table[i][i] = 2048;
            invers_dct(IDFT_table[i]);
            for (int k = 0; k < 8; k++) {
                for (int l = 0; l < 8; l++) {
                    IDFT_table[i][k * 8 + l] = (int) (IDFT_table[i][k * 8 + l] *  Math.cos(Math.PI * k / 16.0) * Math.cos(Math.PI * l / 16.0));
                }
            }
        }
    }

    /* The method "norm" performs the modification of the quatization matrices.		*/
    /* It is called in class "MPEG_scan" and changes the quantization values such	*/
    /* that a DCT-->DFT tranformation occurs during quantization step.			*/

    public void norm(double m1[]) {
        int i, j;
        double d;

        for (j = 0; j < 8; j++) {
            for (i = 0; i < 8; i++) {
                d = (double) m1[j*8+i];
                if (i == 0 && j == 0) {
                    d /= 8.0;
                } else if (i == 0 || j == 0) {
                    d /= 8.0 / Math.sqrt(2.0);
                } else {
                    d /= 4.0;
                }
                m1[j*8+i] = (int) (d * (1 << VAL_BITS) * Math.cos(Math.PI * i / 16.0) * Math.cos(Math.PI * j / 16.0) + 0.5);
            }
        }
    }
    
    
    /** Wraps invers_dct_special() and invers_dct() with Matrix8x8. 
     * [implements IIDCT] */
    public Matrix8x8 IDCT(Matrix8x8 oMat) {
        double[] adblMat = oMat.getPoints().clone();
        int[] aiMat = new int[64];

        int iNonZeroValueCount = 0;
        int iNonZeroValuePos = -1;
        // convert to integers
        for (int i = 0; i < 64; i++) {
            aiMat[i] = (int)adblMat[i];
            if (aiMat[i] != 0) {
                iNonZeroValueCount++;
                iNonZeroValuePos = i;
            }
        }

        if (iNonZeroValueCount == 0)
            return new Matrix8x8(adblMat);
        else if (iNonZeroValueCount == 1)
            invers_dct_special(aiMat, iNonZeroValuePos);
        else
            invers_dct(aiMat);

        // convert back to doubles
        for (int i = 0; i < 64; i++)
            adblMat[i] = aiMat[i];

        return new Matrix8x8(adblMat);
    }

    

    /* The method "invers_dct_special" is called if only one DCT value appears. */

    public void invers_dct_special(int coeff[], int pos) {
        int val, co;
        int ndataptr[], ptr;

        if (pos == 0) { // DC value
            val = (coeff[0] >> VAL_BITS);
            for (int i = 0; i < 64; coeff[i++] = val); // all values are equal
            return;

        }

        // AC value:

        // perform the IDFT using the lookup table
        co = coeff[pos];
        ndataptr = IDFT_table[pos++];
        ptr = 0;

        for (ptr = 0; ptr < 64; ptr++) {
            coeff[ptr] = (ndataptr[ptr] * co) >> (VAL_BITS-2);
        }
    }

    /* The method "invers_dct" is an implementation of a full IDCT (actually IDFT). The	*/
    /* algorithm is described on WEB page:						*/
    /*  http://rnvs.informatik.tu-chemnitz.de/~ja/MPEG/HTML/IDCT.html			*/

    public void invers_dct(int coeff[]) {
        int tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
        int plus8, plus16, plus24, plus32, plus40, plus48, plus56;
        int co1, co2, co3, co5, co6, co7, co35, co17;
        int n0, n1, n2, n3;
        int m1, m2, m3, m4, m5, m6, m7, tmp;
        int l0 = 0, l1 = 0, l2 = 0, l3 = 0;
        int g0, g1, g2, g3;
        int i, j, p;

        // compute B1 (horizontal / vertical algoritm):
        // (the vertical part is in tensor product)

        for (p = j = 0; j < 64; j+=8) {
            matr1[p++] = coeff[j+0];
            matr1[p++] = coeff[j+4];
            matr1[p++] = (co2 = coeff[j+2])-(co6 = coeff[j+6]);
            matr1[p++] = co2+co6;
            matr1[p++] =-(co3=coeff[j+3])+(co5=coeff[j+5]);
            matr1[p++] = (co17=(co1=coeff[j+1]+(co7=coeff[j+7])))-(co35=co3+co5);
            matr1[p++] = co1-co7;
            matr1[p++] = co17+co35;
        }

        // compute the vertical part and the (tensor product) M:

        for (p = i = 0; i < 8; i++) {
            switch(i) {
                case 0:
                case 1:
                case 3:
                case 7:
                    tmp4 = (co3=matr1[24+i])-(co5=matr1[40+i]);
                    tmp6 = (co1=matr1[ 8+i])-(co7=matr1[56+i]);
                    tmp = C6 * (tmp6-tmp4);
                    matr2[p++] =  matr1[i  ] << CONST_BITS;
                    matr2[p++] =  matr1[32+i] << CONST_BITS;
                    matr2[p++] =  ((co2=matr1[16+i])-(co6=matr1[48+i]))*C4;
                    matr2[p++] =  (co2+co6) << CONST_BITS;
                    matr2[p++] =  Q*tmp4-tmp;
                    matr2[p++] =  ((co17=co1 + co7)-(co35=co3+co5))*C4;
                    matr2[p++] =  R*tmp6-tmp;
                    matr2[p++] =  (co17+co35) << CONST_BITS;
                    break;
                case 2:
                case 5:
                    tmp4 = (co3=matr1[24+i])-(co5=matr1[40+i]);
                    tmp6 = (co1=matr1[ 8+i])-(co7=matr1[56+i]);
                    tmp = C4C6 * (tmp6-tmp4);
                    matr2[p++] = C4*matr1[i  ];
                    matr2[p++] = C4*matr1[i+32];
                    matr2[p++] = ((co2=matr1[16+i])-(co6=matr1[48+i])) << TWO;
                    matr2[p++] = C4*(co2+co6);
                    matr2[p++] = C4Q*tmp4-tmp;
                    matr2[p++] = ((co17 =co1+co7)-(co35=co3+co5)) << TWO;
                    matr2[p++] = C4R*tmp6-tmp;
                    matr2[p++] = C4* (co17+co35);
                    break;
                case 4:
                    matr2[p++] = matr1[   i];
                    matr2[p++] = matr1[32+i];
                    matr2[p++] = (co2=matr1[16+i])-(co6=matr1[48+i]);
                    matr2[p] = co2+co6;
                    l0 = l2 = -(co3=matr1[24+i])+(co5=matr1[40+i]);
                    p += 2;
                    matr2[p] = (co17 =(co1=matr1[ 8+i]) + (co7=matr1[56+i]))-(co35=co3+co5);
                    l3 = -( l1 = co1-co7);
                    p += 2;
                    matr2[p++] = co17+co35;
                    break;
                case 6:
                    matr2[p++] = matr1[   i];
                    matr2[p++] = matr1[32+i];
                    matr2[p++] = (co2=matr1[16+i])-(co6=matr1[48+i]);
                    matr2[p] = co2+co6;
                    l1 += (tmp4 = -(co3=matr1[24+i])+(co5=matr1[40+i]));
                    l3 += tmp4;
                    p += 2;
                    matr2[p] = (co17 =(co1=matr1[ 8+i]) + (co7=matr1[56+i]))-(co35=co3+co5);
                    l2 += (tmp6 = co1-co7);
                    l0 -= tmp6;
                    p += 2;
                    matr2[p++] = co17+co35;
                    break;
            }
        }
//	l0 = matr2[36]-matr2[54];
//	l1 = matr2[52]+matr2[38];
//	l2 = matr2[36]+matr2[54];
//	l3 = matr2[52]-matr2[38];


        g0 = C4*(l0+l1);
        g1 = C4*(l0-l1);
        g2 = l2 << TWO;
        g3 = l3 << TWO;

        matr2[36] = g0+g2;
        matr2[38] = g1+g3;
        matr2[52] = g1-g3;
        matr2[54] = g2-g0;


        tmp = C6*(matr2[32]+matr2[48]);
        matr2[32] = -Q*matr2[32]-tmp;
        matr2[48] =  R*matr2[48]-tmp;

        tmp = C6*(matr2[33] + matr2[49]);
        matr2[33] = -Q*matr2[33]-tmp;
        matr2[49] =  R*matr2[49]-tmp;

        tmp = C4C6 * (matr2[34] + matr2[50]);
        matr2[34] = -C4Q*matr2[34]-tmp;
        matr2[50] =  C4R*matr2[50]-tmp;

        tmp = C6*(matr2[35] + matr2[51]);
        matr2[35] = -Q*matr2[35]-tmp;
        matr2[51] =  R*matr2[51]-tmp;

        tmp = C4C6 * (matr2[37] + matr2[53]);
        matr2[37] = -C4Q*matr2[37]-tmp;
        matr2[53] =  C4R*matr2[53]-tmp;

        tmp = C6*(matr2[39] + matr2[55]);
        matr2[39] = -Q*matr2[39]-tmp;
        matr2[55] =  R*matr2[55]-tmp;

        // compute A1 x A2 x A3 (horizontal/vertical algoritm):

        for (p=i = 0; i < 8; i++,p+=8) {
            matr1[p] = (tmp4 = (n3 = matr2[p]+matr2[p+1]) + matr2[p+3]) + matr2[p+7];
            matr1[p+3] = (tmp6 = n3-matr2[p+3])- (tmp7=matr2[p+4]-
                    (tmp1 = (tmp2 = matr2[p+6]-matr2[p+7])- matr2[p+5]));
            matr1[p+4] = tmp6+tmp7;
            matr1[p+1] = (tmp3 = (n1 = matr2[p]-matr2[p+1]) + (n2 = matr2[p+2]-matr2[p+3]))+tmp2;
            matr1[p+2] = (tmp5 = n1-n2) -tmp1;
            matr1[p+5] = tmp5+tmp1;
            matr1[p+6] = tmp3-tmp2;
            matr1[p+7] = tmp4-matr2[p+7];
        }

        plus8 = 8; plus16 = 16; plus24 = 24; plus32 = 32; plus40 = 40; plus48 = 48; plus56 = 56;
        for (p = i = 0; p < 64; p+=8) {
            coeff[p] = ((tmp4 = (n3 = matr1[i]+matr1[plus8]) +matr1[plus24]) + matr1[plus56]) >> ALLBITS;
            coeff[p+3] = ((tmp6 = n3-matr1[plus24])- (tmp7=matr1[plus32++]-
                    (tmp1 = (tmp2 = matr1[plus48++]-matr1[plus56])- matr1[plus40++]))) >> ALLBITS;
            coeff[p+4] = (tmp6+tmp7) >> ALLBITS;
            coeff[p+1] = ((tmp3 = (n1 = matr1[i++]-matr1[plus8++])+ (n2 = matr1[plus16++]-matr1[plus24++]))+tmp2) >> ALLBITS;
            coeff[p+2] = ((tmp5 = n1-n2) -tmp1) >> ALLBITS;
            coeff[p+5] = (tmp5+tmp1) >> ALLBITS;
            coeff[p+6] = (tmp3-tmp2) >> ALLBITS;
            coeff[p+7] = (tmp4-matr1[plus56++]) >> ALLBITS;
        }
    }
}