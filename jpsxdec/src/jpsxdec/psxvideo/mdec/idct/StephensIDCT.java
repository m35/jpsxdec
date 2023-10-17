/*
 * This code originally came from code written by
 * Stephen Manley. He generously offers his code free, without restriction.
 * http://www.nyx.net/~smanley/
 */

package jpsxdec.psxvideo.mdec.idct;

import java.util.logging.Level;
import java.util.logging.Logger;

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
public class StephensIDCT implements IDCT_double {

    private static final Logger LOG = Logger.getLogger(StephensIDCT.class.getName());

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

        if (LOG.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < N; x++) {
                sb.setLength(0);
                for (int y = 0; y < N; y++) {
                    sb.append(c[x][y]);
                    sb.append(' ');
                }
                LOG.finest(sb.toString());
            }
        }

    }

    final double[] temp = new double[64];

    /**
     * This method is preformed using the reverse of the operations preformed in
     * the DCT. This restores a N * N input block to the corresponding output
     * block and then stored in the input block of pixels.
     *
     * @param input N * N Matrix
     * @param out The pixel array output
     */
    @Override
    public void IDCT(double[] input, int iOutputOffset, double[] out) {
        assert(input.length == N*N);
        double temp1;
        int i;
        int j;
        int k;

        for (i=0; i<N; i++) {
            for (j=0; j<N; j++) {
                temp[i + j*N] = 0;

                for (k=0; k<N; k++) {
                    temp[i + j*N] += input[i + k*N] * c[k][j];
                }
            }
        }

        for (i=0; i<N; i++) {
            for (j=0; j<N; j++) {
                temp1 = 0.0;

                for (k=0; k<N; k++) {
                    temp1 += cT[i][k] * temp[k + j*N];
                }

                out[iOutputOffset + i + j*N] = temp1;
            }
        }

    }

    @Override
    public void IDCT_1NonZero(double[] adblIdctMatrix, int iNonZeroPos, int iOutputOffset, double[] adblOutput) {
        IDCT(adblIdctMatrix, iOutputOffset, adblOutput);
    }



    /**
     * This method preforms a matrix multiplication of the input pixel data matrix
     * by the transposed cosine matrix and store the result in a temporary
     * N * N matrix. This N * N matrix is then multiplied by the cosine matrix
     * and the result is stored in the output matrix.
     *
     * @param matrix The Input Pixel Matrix, overwritten with The DCT Result Matrix
     */
    public void forwardDCT(double matrix[])
    {
        double temp1;
        int i;
        int j;
        int k;

        for (i = 0; i < N; i++)
        {
            for (j = 0; j < N; j++)
            {
                temp[i+j*N] = 0.0;
                for (k = 0; k < N; k++)
                {
                    temp[i+j*N] += matrix[i + k*N] * cT[k][j];
                }
            }
        }

        for (i = 0; i < N; i++)
        {
            for (j = 0; j < N; j++)
            {
                temp1 = 0.0;

                for (k = 0; k < N; k++)
                {
                    temp1 += (c[i][k] * temp[k+j*N]);
                }

                matrix[i + j*N] = temp1;
            }
        }

    }

    public static void main(String[] args) {

        StephensIDCT idct = new StephensIDCT();
        for (int y = 0; y < N; y++) {
            System.out.print("[ ");
            for (int x = 0; x < N; x++) {
                System.out.format("%1.3f ", idct.c[y][x]);
            }
            System.out.println(" ]");
        }

        System.out.println();
        System.out.println();

        double[] matrix_for_dct = new double[] {
         -3.823, -3.823, -3.823, -3.823, -3.865, -3.823, -3.740, -3.698 ,
         -4.742, -4.742, -4.742, -4.742, -4.742, -4.742, -4.576, -4.742 ,
         -6.580, -6.580, -6.580, -6.580, -6.580, -6.454, -6.204, -6.454 ,
         -8.752, -8.752, -8.752, -8.752, -8.752, -8.751, -8.751, -8.917 ,
         -11.424, -11.424, -11.341, -11.174, -11.216, -11.424, -11.133, -11.424,
         -13.596, -13.430, -13.679, -13.596, -13.388, -13.596, -13.513, -13.637,
         -15.266, -15.183, -15.308, -15.309, -15.266, -15.392, -15.517, -15.267,
         -16.310, -16.268, -16.019, -16.352, -16.228, -16.103, -16.145, -16.228,
        };

        double[] matrix_for_idct = new double[] {
         -112.000, 60.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
         -54.000, 18.000, 8.250, 0.000, 0.000, 0.000, 0.000, 0.000,
         -14.250, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
         0.000, 0.000, 9.750, 0.000, 0.000, 0.000, 0.000, 0.000,
         0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
         0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
         0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
         0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.000,
        };

        double[] matrix = matrix_for_idct;

        //idct.forwardDCT(matrix);
        idct.IDCT(matrix, 0, matrix);

        for (int y = 0; y < N; y++) {
            System.out.print("[ ");
            for (int x = 0; x < N; x++) {
                System.out.format("%1.3f ", matrix[x + y * N]);
            }
            System.out.println(" ]");
        }
    }
}

