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

import jpsxdec.util.Maths;

/** An attempt to create an IDCT that produces the same output as the
 * PlayStation 1 MDEC chip.
 *<p>
 * The MAME source code was used as a reference, and this IDCT seems to produce
 * identical values (although differences in other parts of jPSXdec code
 * ultimately result in slightly different color output).
 *<p>
 * Note that neither this code nor the MAME code produce what the
 * MDEC chip actually generates! */
public class PsxMdecIDCT_int implements IDCT_int {

    private static final int[] PSX_DEFAULT_COSINE_MATRIX = {
        23170,  23170,  23170,  23170,  23170,  23170,  23170,  23170,
        32138,  27245,  18204,   6392,  -6393, -18205, -27246, -32139,
        30273,  12539, -12540, -30274, -30274, -12540,  12539,  30273,
        27245,  -6393, -32139, -18205,  18204,  32138,   6392, -27246,
        23170, -23171, -23171,  23170,  23170, -23171, -23171,  23170,
        18204, -32139,   6392,  27245, -27246,  -6393,  32138, -18205,
        12539, -30274,  30273, -12540, -12540,  30273, -30274,  12539,
         6392, -18205,  27245, -32139,  32138, -27246,  18204,  -6393,
    };

    private final long[] _aTemp = new long[64];

    @Override
    public void IDCT(int[] idctMatrix, int iOutputOffset, int[] output) {
        long tempSum;
        int x;
        int y;
        int i;

        for (x=0; x<8; x++) {
            for (y=0; y<8; y++) {
                tempSum = 0;

                for (i=0; i<8; i++) {
                    tempSum += (long)idctMatrix[x + i*8] * PSX_DEFAULT_COSINE_MATRIX[y + i*8];
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

                output[iOutputOffset + x + y*8] = (int)Maths.shrRound(tempSum, 32);
            }
        }
    }

    @Override
    public void IDCT_1NonZero(int[] idctMatrix, int iNonZeroPos, int iOutputOffset, int[] output) {
        IDCT(idctMatrix, iOutputOffset, output);
    }

}
