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

/** This is the simplest implementation of the Inverse Discrete Cosine
 *  Transform. If I understand correctly, it's inverse 2D DCT-II, specifically.
 *<p>
 * It's as simple as I could make it, and as such, it's about as
 * slow as can be.
 *<p>
 * Note: Using the {@link StephensIDCT} version is about 13 times faster!
 * The results are almost indistinguishable. His is also likely more accurate
 * as there are fewer operations performed.
 *<p>
 * This class is not actually used, but is provided as a reference of how
 * the IDCT works. */
public class NaiveIDCT implements IDCT_double {

    private final int[] _Temp_ = new int[64];

    public NaiveIDCT() {}


    @Override
    public void IDCT(double[] aiDCTMat, int iOutputOffset, double[] adblOutput) {

        int Pixelx, Pixely, DCTx, DCTy;

        for (Pixelx = 0; Pixelx < 16; Pixelx++) {
            for (Pixely = 0; Pixely < 16; Pixely++) {

                double dblTotal = 0;

                for (DCTx = 0; DCTx < 16; DCTx++) {
                    for (DCTy = 0; DCTy < 16; DCTy++) {

                        double dblSubTotal = aiDCTMat[DCTx + DCTy * 16];

                        if (DCTx == 0)
                            dblSubTotal *= Math.sqrt(1.0f / 16);
                        else
                            dblSubTotal *= Math.sqrt(2.0f / 16);

                        if (DCTy == 0)
                            dblSubTotal *= Math.sqrt(1.0f / 16);
                        else
                            dblSubTotal *= Math.sqrt(2.0f / 16);

                        dblSubTotal
                                *= Math.cos( DCTx * Math.PI * (2 * Pixelx + 1)
                                             / (2.0f * 16) )
                                *  Math.cos( DCTy * Math.PI * (2 * Pixely + 1)
                                             / (2.0f * 16) );

                        dblTotal += dblSubTotal;
                    }
                }

                _Temp_[Pixelx + Pixely*16] = (int)dblTotal;
            }
        }

        for (int i=63; i>0; i--)
            aiDCTMat[iOutputOffset + i] = _Temp_[i];

    }

    @Override
    public void IDCT_1NonZero(double[] adblIdctMatrix, int iNonZeroPos, int iOutputOffset, double[] adblOutput) {
        IDCT(adblOutput, iOutputOffset, adblOutput);
    }

}
