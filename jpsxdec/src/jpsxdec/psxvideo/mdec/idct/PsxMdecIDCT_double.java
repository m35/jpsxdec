/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.psxvideo.mdec.idct;

/** Based on the actual values that the MDEC chip uses, but using
 * floating point math for better precision. */
public class PsxMdecIDCT_double implements IDCT_double {

    private static final double F = 65536.0;

    private static final double[] PSX_DEFAULT_COSINE_MATRIX = {
        23170/F,  23170/F,  23170/F,  23170/F,  23170/F,  23170/F,  23170/F,  23170/F,
        32138/F,  27245/F,  18204/F,   6392/F,  -6393/F, -18205/F, -27246/F, -32139/F,
        30273/F,  12539/F, -12540/F, -30274/F, -30274/F, -12540/F,  12539/F,  30273/F,
        27245/F,  -6393/F, -32139/F, -18205/F,  18204/F,  32138/F,   6392/F, -27246/F,
        23170/F, -23171/F, -23171/F,  23170/F,  23170/F, -23171/F, -23171/F,  23170/F,
        18204/F, -32139/F,   6392/F,  27245/F, -27246/F,  -6393/F,  32138/F, -18205/F,
        12539/F, -30274/F,  30273/F, -12540/F, -12540/F,  30273/F, -30274/F,  12539/F,
         6392/F, -18205/F,  27245/F, -32139/F,  32138/F, -27246/F,  18204/F,  -6393/F,
    };

    private final double[] _aTemp = new double[64];

    @Override
    public void IDCT(double[] idctMatrix, int iOutputOffset, double[] output) {
        double tempSum;
        int x;
        int y;
        int i;

        for (x=0; x<8; x++) {
            for (y=0; y<8; y++) {
                tempSum = 0;

                for (i=0; i<8; i++) {
                    tempSum += idctMatrix[x + i*8] * PSX_DEFAULT_COSINE_MATRIX[y + i*8];
                }

                _aTemp[x + y*8] = tempSum;
            }
        }

        for (x=0; x<8; x++) {
            for (y=0; y<8; y++) {
                tempSum = 0;

                for (i=0; i<8; i++) {
                    tempSum += PSX_DEFAULT_COSINE_MATRIX[x + i*8] * _aTemp[i + y*8];
                }

                output[iOutputOffset + x + y*8] = tempSum;
            }
        }
    }


    public void DCT(double[] idctMatrix, int iOutputOffset, double[] output) {
        double tempSum;
        int x;
        int y;
        int i;

        for (x=0; x<8; x++) {
            for (y=0; y<8; y++) {
                tempSum = 0;

                for (i=0; i<8; i++) {
                    tempSum += idctMatrix[x + i*8] * PSX_DEFAULT_COSINE_MATRIX[i + y*8];
                }

                _aTemp[x + y*8] = tempSum;
            }
        }

        for (x=0; x<8; x++) {
            for (y=0; y<8; y++) {
                tempSum = 0;

                for (i=0; i<8; i++) {
                    tempSum += PSX_DEFAULT_COSINE_MATRIX[i + x*8] * _aTemp[i + y*8];
                }

                output[iOutputOffset + x + y*8] = tempSum;
            }
        }
    }

    @Override
    public void IDCT_1NonZero(double[] idctMatrix, int iNonZeroPos, int iOutputOffset, double[] output) {
        IDCT(idctMatrix, iOutputOffset, output);
    }

}
