/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.psxvideo.mdec.tojpeg;

/** JPEG component. */
class Component {
    /** Index of this component. */
    public final int ComponentIndex;
    /** Index of the quantization table this component uses. */
    public final int QuantizationTableIndex;
    /** JPEG sampling. */
    public final int HSampling, VSampling;
    /** Index of the huffman tables this component uses. */
    public final int DcHuffTableIndex, AcHuffTableIndex;
    /** The DCT coefficients read from a MDEC stream.
     * The 8x8 blocks are stored in the order they are read from the stream. */
    public final int[] DctCoffZZ;
    /** Offset in {@link #DctCoffZZ} while reading MDEC. */
    public int WriteIndex;
    /** Track DC values while writing the JPEG. */
    public int PreviousDC;

    public Component(int iComponentIndex, int iQuantizationTableIndex,
                     int iHSampling, int iVSampling,
                     int iDcHuffTableIndex, int iAcHuffTableIndex,
                     int iMcuWidth, int iMcuHeight)
    {
        if (iHSampling * iVSampling > 4)
                throw new IllegalArgumentException();

        ComponentIndex = iComponentIndex;
        QuantizationTableIndex = iQuantizationTableIndex;
        HSampling = iHSampling;
        VSampling = iVSampling;
        DcHuffTableIndex = iDcHuffTableIndex;
        AcHuffTableIndex = iAcHuffTableIndex;
        DctCoffZZ = new int[iMcuWidth * iHSampling * iMcuHeight * iVSampling * 64];
    }

}
