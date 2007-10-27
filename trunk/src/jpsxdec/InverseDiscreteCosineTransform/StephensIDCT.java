/*
 * StephensIDCT.java
 *
 */

package jpsxdec.InverseDiscreteCosineTransform;

import jpsxdec.util.Matrix8x8;

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
 * <a href="http://www.nyx.net/~smanley/">homepage</a>. There's
 * lots of goodies there too.
 * @version 1.0.1 August 22nd 1996
 * @author <a href="http://www.nyx.net/~smanley/">Stephen Manley</a> - smanley@nyx.net
 */
public class StephensIDCT implements IIDCT {

    /** DCT Block Size */
    private final static int N  = 8;

    /** Cosine matrix. N * N. */
    private double c[][] = new double[N][N];

    /** Transformed cosine matrix, N*N. */
    private double cT[][] = new double[N][N];

    /**
     * Constructs a new DCT object. Initializes the cosine transform matrix
     * these are used when computing the DCT and it's inverse.
     *
     */
    public StephensIDCT()
    {
        initMatrix();
    }

    /**
     * This method init the Cosine Transform Matrix and the Transposed CT.
     * These are used by the inverse DCT. 
     */
    private void initMatrix()
    {
        int i;
        int j;

        for (j = 0; j < N; j++)
        {
            double nn = (double)(N);
            c[0][j]  = 1.0 / Math.sqrt(nn);
            cT[j][0] = c[0][j];
        }

        for (i = 1; i < 8; i++)
        {
            for (j = 0; j < 8; j++)
            {
                double jj = (double)j;
                double ii = (double)i;
                c[i][j]  = Math.sqrt(2.0/8.0) * Math.cos(((2.0 * jj + 1.0) * ii * Math.PI) / (2.0 * 8.0));
                cT[j][i] = c[i][j];
            }
        }
    }
   
    /**
     * This method is preformed using the reverse of the operations preformed in
     * the DCT. This restores a N * N input block to the corresponding output
     * block and then stored in the input block of pixels.
     *
     * @param input N * N Matrix
     * @return output The pixel array output
     */
    public Matrix8x8 IDCT(Matrix8x8 input) {
        assert(input.getWidth() == N && input.getHeight() == N);
        Matrix8x8 output = new Matrix8x8(/*N, N*/);
        Matrix8x8 temp = new Matrix8x8(/*N, N*/);
        double temp1;
        int i;
        int j;
        int k;

        for (i=0; i<N; i++) {
            for (j=0; j<N; j++) {
                temp.setPoint(i, j, 0.0);

                for (k=0; k<N; k++) {
                    temp.setAddPoint(i, j, input.getPoint(i, k) * c[k][j]);
                }
            }
        }

        for (i=0; i<N; i++) {
            for (j=0; j<N; j++) {
                temp1 = 0.0;

                for (k=0; k<N; k++) {
                    temp1 += cT[i][k] * temp.getPoint(k, j);
                }

                output.setPoint(i, j, temp1 / 4);
            }
        }

        return output;
    }

}

