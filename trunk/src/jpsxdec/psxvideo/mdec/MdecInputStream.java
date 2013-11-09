/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

/** Read MDEC codes one at a time from a stream. */
public abstract class MdecInputStream {

    /** Reads the next MDEC code from the stream into the provided
     * {@link MdecCode} object.
     * 
     *  @return  true if the EOD code is read. */
    public abstract boolean readMdecCode(MdecCode code)
            throws MdecException;

    /** 16-bit MDEC code indicating the end of a block. 
     * The equivalent MDEC value is (63, -512). */
    public final static int MDEC_END_OF_DATA = 0xFE00;
    /** Top 6 bits of {@link MDEC_END_OF_DATA}. */
    public final static int MDEC_END_OF_DATA_TOP6 = (MDEC_END_OF_DATA >> 10) & 63;
    /** Bottom 10 bits of {@link MDEC_END_OF_DATA}. */
    public final static int MDEC_END_OF_DATA_BOTTOM10 = (short)(MDEC_END_OF_DATA | 0xFC00);

    /** Standard quantization matrix for MDEC frames. */
    private static final int[] PSX_DEFAULT_QUANTIZATION_MATRIX = {
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

    /** Matrix of indexes indicating the order that matrix values are
     *  zig-zagged into a vector. */
    public static final int[] ZIG_ZAG_LOOKUP_MATRIX = {
         0,  1,  5,  6, 14, 15, 27, 28,
         2,  4,  7, 13, 16, 26, 29, 42,
         3,  8, 12, 17, 25, 30, 41, 43,
         9, 11, 18, 24, 31, 40, 44, 53,
        10, 19, 23, 32, 39, 45, 52, 54,
        20, 22, 33, 38, 46, 51, 55, 60,
        21, 34, 37, 47, 50, 56, 59, 61,
        35, 36, 48, 49, 57, 58, 62, 63,
    };

    /** List of vector indexes indicating the order that vector values
     * are reverse-zig-zagged into a matrix. */
    public static final int[] REVERSE_ZIG_ZAG_LOOKUP_LIST = {
         0,  1,  8, 16,  9,  2,  3, 10, 17, 24, 32, 25, 18, 11,  4,  5,
        12, 19, 26, 33, 40, 48, 41, 34, 27, 20, 13,  6,  7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63
    };

    /** Represents a 16-bit code readable by the PSX MDEC chip.
     *  If the MDEC code is the first of a block, the top 6 bits indicate
     *  the block's quantization scale, and the bottom 10 bits indicate
     *  the "direct current" (DC) coefficient.
     *  If the MDEC code is not the first of a block, and it is
     *  not a {@link #MDEC_END_OF_DATA} code (0xFE00), then the top 6 bits indicate
     *  the number of zeros preceeding an "alternating current" (AC) coefficient,
     *  with the bottom 10 bits indicating a (usually) non-zero AC coefficient.  */
    public static class MdecCode implements Cloneable {

        /** Most significant 6 bits of the 16-bit MDEC code.
         * Holds either a block's quantization scale or the
         * count of zero AC coefficients leading up to a non-zero
         * AC coefficient. */
        private int _iTop6Bits;

        /** Least significant 10 bits of the 16-bit MDEC code.
         * Holds either the DC coefficient of a block or
         * a non-zero AC coefficient. */
        private int _iBottom10Bits;

        /** Generic constructor */
        public MdecCode() {}

        public MdecCode(int iTop6Bits, int iBottom10Bits) {
            if (!validTop(iTop6Bits))
                throw new IllegalArgumentException("Invalid top 6 bits " + iTop6Bits);
            if (!validBottom(iBottom10Bits))
                throw new IllegalArgumentException("Invalid bottom 10 bits " + iBottom10Bits);
            _iTop6Bits = iTop6Bits;
            _iBottom10Bits = iBottom10Bits;
        }

        /** Extract the top 6 bit and bottom 10 bit values from 16 bits */
        public MdecCode(int iMdecWord) {
            set(iMdecWord);
        }

        public void set(MdecCode other) {
            _iTop6Bits = other._iTop6Bits;
            _iBottom10Bits = other._iBottom10Bits;
        }

        public int getBottom10Bits() {
            return _iBottom10Bits;
        }

        public void setBottom10Bits(int iBottom10Bits) {
            assert validBottom(iBottom10Bits);
            _iBottom10Bits = iBottom10Bits;
        }

        public int getTop6Bits() {
            return _iTop6Bits;
        }

        public void setTop6Bits(int iTop6Bits) {
            assert validTop(iTop6Bits);
            _iTop6Bits = iTop6Bits;
        }

        public void setBits(int iTop6, int iBottom10) {
            assert validTop(_iTop6Bits) && validBottom(_iBottom10Bits);
            _iTop6Bits = iTop6;
            _iBottom10Bits = iBottom10;
        }

        public void set(int iMdecWord) {
            _iTop6Bits = ((iMdecWord >> 10) & 63);
            _iBottom10Bits = (iMdecWord & 0x3FF);
            if ((_iBottom10Bits & 0x200) == 0x200) { // is it negitive?
                _iBottom10Bits -= 0x400;
            }
        }

        /** Combines the top 6 bits and bottom 10 bits into an unsigned 16 bit value. */
        public int toMdecWord() {
            if (!validTop(_iTop6Bits))
                throw new IllegalStateException("MDEC code has invalid top 6 bits " + _iTop6Bits);
            if (!validBottom(_iBottom10Bits))
                throw new IllegalStateException("MDEC code has invalid bottom 10 bits " + _iBottom10Bits);
            return ((_iTop6Bits & 63) << 10) | (_iBottom10Bits & 0x3FF);
        }

        /** Set the MDEC code to the special "End of Data" (EOD) value,
         * indicating the end of a block.
         * @see MdecInputStream#MDEC_END_OF_DATA */
        public void setToEndOfData() {
            _iTop6Bits = MDEC_END_OF_DATA_TOP6;
            _iBottom10Bits = MDEC_END_OF_DATA_BOTTOM10;
        }

        /** Returns if this MDEC code is set to the special "End of Data" (EOD)
         * value.
         * @see MdecInputStream#MDEC_END_OF_DATA */
        public boolean isEOD() {
            return (_iTop6Bits == MDEC_END_OF_DATA_TOP6 &&
                    _iBottom10Bits == MDEC_END_OF_DATA_BOTTOM10);
        }

        /** Returns if this MDEC code has valid values.
         * As an optimization, many parameter checks are disabled, so
         * this MDEC code could hold values that are be invalid. */
        public boolean isValid() {
            return validTop(_iTop6Bits) && validBottom(_iBottom10Bits);
        }

        /** Checks if the top 6 bits of an MDEC code are valid. */
        private static boolean validTop(int iTop6Bits) {
            return iTop6Bits >= 0 && iTop6Bits <= 63;
        }
        /** Checks if the bottom 10 bits of an MDEC code are valid. */
        private static boolean validBottom(int iBottom10Bits) {
            return iBottom10Bits >= -512 && iBottom10Bits <= 511;
        }

        @Override
        public MdecCode clone() {
            return new MdecCode(_iTop6Bits, _iBottom10Bits);
        }

        public String toString() {
            if (isEOD())
                return "EOD";
            else
                return "(" + _iTop6Bits + ", " + _iBottom10Bits + ")";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + this._iTop6Bits;
            hash = 97 * hash + this._iBottom10Bits;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MdecCode other = (MdecCode) obj;
            return _iTop6Bits == other._iTop6Bits &&
                   _iBottom10Bits == other._iBottom10Bits;
        }

    }
}
