/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.video.mdec;

import java.io.EOFException;
import jpsxdec.plugins.psx.video.decode.UncompressionException;

/** Read MDEC codes one at a time from a stream. */
public abstract class MdecInputStream {

    /** Reads an MDEC code into the provided MdecCode object.
     *  @return  true if the EOD code is read. */
    public abstract boolean readMdecCode(MdecCode code)
            throws UncompressionException, EOFException;

    /** 16-bit MDEC code indicating the end of a block. */
    public final static int MDEC_END_OF_DATA = 0xFE00;
    /** Top 6 bits of MDEC_END_OF_DATA. */
    public final static int MDEC_END_OF_DATA_TOP6 = (MDEC_END_OF_DATA >> 10) & 63;
    /** Bottom 10 bits of MDEC_END_OF_DATA. */
    public final static int MDEC_END_OF_DATA_BOTTOM10 = (short)(MDEC_END_OF_DATA | 0xFC00);

    /** Standard qunatization matrix for MDEC frames. */
    private static final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
    {
         2, 16, 19, 22, 26, 27, 29, 34,
        16, 16, 22, 24, 27, 29, 34, 37,
        19, 22, 26, 27, 29, 34, 34, 38,
        22, 22, 26, 27, 29, 34, 37, 40,
        22, 26, 27, 29, 32, 35, 40, 48,
        26, 27, 29, 32, 35, 40, 48, 58,
        26, 27, 29, 34, 38, 46, 56, 69,
        27, 29, 35, 38, 46, 56, 69, 83
    };
    /** Retrieve a copy of the PSX default quantization matrix. */
    public static int[] getDefaultPsxQuantMatrixCopy() {
        return PSX_DEFAULT_QUANTIZATION_MATRIX.clone();
    }

    /** The order of the zig-zag vector. */
    public static final int[] REVERSE_ZIG_ZAG_SCAN_MATRIX =
    {
         0,  1,  8, 16,  9,  2,  3, 10,
        17, 24, 32, 25, 18, 11,  4,  5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13,  6,  7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };

    /** Represents a 16-bit code readable by the PSX MDEC chip.
     *  If the MDEC code is the first of a block, the top 6 bits indicate
     *  the block's quantization scale, and the bottom 10 bits indicate
     *  the "direct current" (DC) coefficient.
     *  If the MDEC code is not the first of a block, and it is
     *  not an END_OF_DATA code (0xFE00), then the top 6 bits indicate
     *  the number of zeros preceeding an AC code, with the bottom 10 bits
     *  indicating the "alternating current" (AC) code.  */
    public static class MdecCode {

        public int Top6Bits;
        public int Bottom10Bits;

        /** Generic constructor */
        public MdecCode() {}

        public MdecCode(int iTop6Bits, int iBottom10Bits) {
            Top6Bits = iTop6Bits;
            Bottom10Bits = iBottom10Bits;
        }

        /** Extract the top 6 bit and bottom 10 bit values from 16 bits */
        public MdecCode(int iMdecWord) {
            set(iMdecWord);
        }

        public void set(int iMdecWord) {
            Top6Bits = ((iMdecWord >> 10) & 63);
            Bottom10Bits = (iMdecWord & 0x3FF);
            if ((Bottom10Bits & 0x200) == 0x200) { // is it negitive?
                Bottom10Bits -= 0x400;
            }
        }

        /** Combines the top 6 bits and bottom 10 bits into a 16 bit value */
        public int toMdecWord() {
            return ((Top6Bits & 63) << 10) | (Bottom10Bits & 0x3FF);
        }

        public String toString() {
            if (isEOD())
                return "EOB";
            else
                return "(" + Top6Bits + ", " + Bottom10Bits + ")";
        }

        @Override
        public MdecCode clone() {
            return new MdecCode(Top6Bits, Bottom10Bits);
        }

        public boolean isEOD() {
            return (Top6Bits == MDEC_END_OF_DATA_TOP6 &&
                    Bottom10Bits == MDEC_END_OF_DATA_BOTTOM10);
        }
    }
}
