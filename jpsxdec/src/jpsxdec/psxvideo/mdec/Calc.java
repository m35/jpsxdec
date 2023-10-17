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

package jpsxdec.psxvideo.mdec;

/** Functions to calculate blocks and macroblocks. */
public class Calc {

    /** Number of blocks necessary to store pixel size. */
    public static int blocks(int iPixelWidth, int iPixelHeight) {
        return macroblocks(iPixelWidth, iPixelHeight) * MdecBlock.count();
    }
    /** Number of macroblocks necessary to store pixel size. */
    public static int macroblocks(int iPixelWidth, int iPixelHeight) {
        return macroblockDim(iPixelWidth) * macroblockDim(iPixelHeight);
    }

    /** Rounds a pixel dimension (width or height) up to the nearest macroblock. */
    public static int fullDimension(int iPixelDimension) {
        return (iPixelDimension + 15) & ~15;
    }

    /** Converts a pixel dimension (width or height) into macroblock dimension. */
    public static int macroblockDim(int iPixelDimension) {
        return (iPixelDimension + 15) / 16;
    }

    /** A strange value needed for video bitstreams and video sector headers.
     * It's the number of MDEC codes, divided by two, then rounded up to the
     * next closest multiple of 32 (if not already a multiple of 32).
     * Another way to look at it is the number of MDEC codes rounded up to a
     * multiple of 64, then divided by two. */
    public static short calculateHalfCeiling32(int iMdecCodeCount) {
        return (short) ((((iMdecCodeCount + 1) / 2) + 31) & ~31);
    }
}
