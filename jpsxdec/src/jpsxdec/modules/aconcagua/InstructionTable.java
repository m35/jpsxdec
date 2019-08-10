/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

package jpsxdec.modules.aconcagua;

import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/** The "instruction" table, as I call it, indicates how many codes to read
 * from each of the three zero-run length AC tables for the current block.
 * This is for the Aconcagua opening FMV. The ending FMV seems to be different.
 * What even.. */
public class InstructionTable {

    public static class InstructionCode {
        @Nonnull
        private final String _sBits;
        private final int _iTable1Count;
        private final int _iTable2Count;
        private final int _iTable3Count;

        protected InstructionCode(@Nonnull String sBits, int iTable1Count, int iTable2Count, int iTable3Count) {
            _sBits = sBits;
            _iTable1Count = iTable1Count;
            _iTable2Count = iTable2Count;
            _iTable3Count = iTable3Count;
        }

        public int getTable1Count() {
            return _iTable1Count;
        }

        public int getTable2Count() {
            return _iTable2Count;
        }

        public int getTable3Count() {
            return _iTable3Count;
        }

        public int getBitCodeLen() {
            return _sBits.length();
        }

        public String getBits() {
            return _sBits;
        }

        @Override
        public String toString() {
            return make();
        }

        public @Nonnull String make() {
            return String.format("new InstructionCode(%12s, %2d, %2d, %2d),",
                                 "\""+_sBits+"\"", _iTable1Count, _iTable2Count, _iTable3Count);
        }
    }

    private static final int BOTTOM_10_BITS = 0x03ff;
    public static @Nonnull InstructionCode lookup(int iBottom10Bits) throws MdecException.ReadCorruption {
        iBottom10Bits &= BOTTOM_10_BITS;
        int iOnesCount;
        int b = iBottom10Bits;
        for (iOnesCount = 0; iOnesCount < 9; iOnesCount++) {
            if ((b & 1) == 0)
                break;
            b >>>= 1;
        }

        if (iOnesCount <= 3) {
            return INSTRUCTIONS[iOnesCount];
        } else if (iOnesCount < 7) {
            InstructionCode code = PARTIAL_LOOKUP[(iBottom10Bits >>> 4)];
            if (code == null)
                throw new MdecException.ReadCorruption("Aconcagua lookup table bits " + Misc.bitsToString(iBottom10Bits, 10));
            return code;
        } else if (iOnesCount <= 9) {
            return INSTRUCTIONS[4 + ((iBottom10Bits >>> 7) & 3)];
        }
        throw new MdecException.ReadCorruption("Aconcagua lookup table bits " + Misc.bitsToString(iBottom10Bits, 10));
    }

    private static void buildLookup() {
        for (int i = 8; i < INSTRUCTIONS.length; i++) {
            InstructionCode code = INSTRUCTIONS[i];
            int iLookupPos = Integer.parseInt(code._sBits, 2);
            PARTIAL_LOOKUP[iLookupPos >>> 4] = code;
        }
    }

    private static final int _111110b = 0x3E;
    private static final InstructionCode[] PARTIAL_LOOKUP = new InstructionCode[_111110b + 1];

    private static final InstructionCode[] INSTRUCTIONS = {
        new InstructionCode(         "0",  0,  0,  0),
        new InstructionCode(        "01",  0,  0,  1),
        new InstructionCode(       "011",  0,  0,  2),
        new InstructionCode(      "0111",  0,  0,  3),
        new InstructionCode( "001111111",  0,  0,  4),
        new InstructionCode( "011111111",  3,  1, 12),
        new InstructionCode( "101111111",  3,  1, 14),
        new InstructionCode( "111111111",  3,  1, 13),
        new InstructionCode("0000001111",  5,  2, 19),
        new InstructionCode("0000011111",  7, 43,  8),
        new InstructionCode("0000101111", 10,  7, 25),
        new InstructionCode("0000111111",  2,  0, 10),
        new InstructionCode("0001001111",  1,  0,  8),
        new InstructionCode("0001011111",  6, 27, 17),
        new InstructionCode("0001101111",  8,  4, 22),
        new InstructionCode("0010001111",  6,  3, 21),
        new InstructionCode("0010011111",  1, 56,  5),
        new InstructionCode("0010101111",  8, 16, 22),
        new InstructionCode("0010111111",  1,  1,  9),
        new InstructionCode("0011001111",  0,  1,  5),
        new InstructionCode("0011011111",  9, 24, 19),
        new InstructionCode("0011101111",  9,  5, 24),
        new InstructionCode("0100001111",  0,  0,  5),
        new InstructionCode("0100011111",  7, 44,  8),
        new InstructionCode("0100101111",  9,  6, 25),
        new InstructionCode("0100111111",  3,  0, 12),
        new InstructionCode("0101001111",  1,  0,  9),
        new InstructionCode("0101011111",  6, 25, 17),
        new InstructionCode("0101101111",  7,  3, 22),
        new InstructionCode("0110001111",  6,  2, 20),
        new InstructionCode("0110011111",  8, 33, 14),
        new InstructionCode("0110101111", 10,  9, 25),
        new InstructionCode("0110111111",  2,  1, 10),
        new InstructionCode("0111001111",  5,  1, 17),
        new InstructionCode("0111011111",  8, 28, 17),
        new InstructionCode("0111101111",  8,  5, 23),
        new InstructionCode("1000001111",  6,  1, 20),
        new InstructionCode("1000011111", 10, 34, 12),
        new InstructionCode("1000101111",  8, 11, 24),
        new InstructionCode("1000111111",  4,  1, 14),
        new InstructionCode("1001001111",  1,  0,  7),
        new InstructionCode("1001011111", 11, 24, 16),
        new InstructionCode("1001101111",  8,  4, 23),
        new InstructionCode("1010001111",  7,  3, 21),
        new InstructionCode("1010011111",  0, 58,  5),
        new InstructionCode("1010101111",  8, 18, 21),
        new InstructionCode("1010111111",  4,  1, 16),
        new InstructionCode("1011001111",  5,  1, 18),
        new InstructionCode("1011011111",  2, 53,  5),
        new InstructionCode("1011101111",  9,  6, 24),
        new InstructionCode("1100001111",  5,  2, 18),
        new InstructionCode("1100011111",  1, 54,  6),
        new InstructionCode("1100101111",  9,  7, 25),
        new InstructionCode("1100111111",  2,  1, 11),
        new InstructionCode("1101001111",  4,  1, 17),
        new InstructionCode("1101011111", 10, 20, 19),
        new InstructionCode("1101101111",  7,  4, 22),
        new InstructionCode("1110001111",  6,  3, 20),
        new InstructionCode("1110011111", 15, 29, 13),
        new InstructionCode("1110101111",  9, 12, 24),
        new InstructionCode("1110111111",  4,  1, 15),
        new InstructionCode("1111001111",  1,  0,  6),
        new InstructionCode("1111011111",  8, 33, 13),
        new InstructionCode("1111101111",  8,  6, 23),
    };

    static {
        buildLookup();
    }
}
