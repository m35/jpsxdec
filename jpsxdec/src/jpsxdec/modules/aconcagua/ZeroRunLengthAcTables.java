/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/** Pretty standard variable length code bit reading mapped to MDEC codes.
 * These are for the Aconcagua opening FMV. The ending FMV seems to be different. */
public class ZeroRunLengthAcTables {

    public static class AcCode {
        @Nonnull
        public final String _sBits;
        @Nonnull
        public final MdecCode _code;

        protected AcCode(@Nonnull String sBits, @Nonnull MdecCode code) {
            _sBits = sBits;
            _code = code;
        }

        public @Nonnull MdecCode getMdecCodeCopy() {
            return _code.copy();
        }

        public void setMdec(@Nonnull MdecCode code) {
            code.setFrom(_code);
        }

        @Override
        public String toString() {
            return make();
        }

        public @Nonnull String make() {
            return String.format("new AcCode(%16s, new MdecCode(%2d, %4d)),",
                                 "\""+_sBits+"\"", _code.getTop6Bits(), _code.getBottom10Bits());
        }

    }

    public static @Nonnull AcCode lookupTable1(int iBottom14Bits) throws MdecException.ReadCorruption {
        return lookup(TABLE1_LOOKUP, iBottom14Bits);
    }

    public static @Nonnull AcCode lookupTable2(int iBottom14Bits) throws MdecException.ReadCorruption {
        return lookup(TABLE2_LOOKUP, iBottom14Bits);
    }

    public static @Nonnull AcCode lookupTable3(int iBottom14Bits) throws MdecException.ReadCorruption {
        return lookup(TABLE3_LOOKUP, iBottom14Bits);
    }

    private static @Nonnull AcCode lookup(@Nonnull AcCode[] aoTable, int iBottom14Bits)
            throws MdecException.ReadCorruption
    {
        AcCode code = aoTable[iBottom14Bits & BOTTOM_14_BITS];
        if (code == null)
            throw new MdecException.ReadCorruption(Misc.bitsToString(iBottom14Bits, BIT_LENGTH));
        return code;
    }

    private static final int BIT_LENGTH = 14;
    private static final int ENTRY_COUNT = 16384;
    private static final int BOTTOM_14_BITS = 0x3FFF;

    static final AcCode[] TABLE1_LOOKUP = new AcCode[ENTRY_COUNT];
    static final AcCode[] TABLE2_LOOKUP = new AcCode[ENTRY_COUNT];
    static final AcCode[] TABLE3_LOOKUP = new AcCode[ENTRY_COUNT];

    private static void buidLookup(@Nonnull AcCode[] table, @Nonnull AcCode[] lookup) {
        for (AcCode ac : table) {
            int iBits = Integer.parseInt(ac._sBits, 2);
            int iMax = BOTTOM_14_BITS >>> ac._sBits.length();

            for (int i = 0; i <= iMax; i++) {
                int j = (i << ac._sBits.length()) | iBits;
                if (j >= lookup.length)
                    System.out.println("");
                if (lookup[j] != null)
                    throw new RuntimeException("Wrong logic");
                lookup[j] = ac;
            }
        }
    }

    private static final AcCode[] TABLE1 = {
        new AcCode(          "0001", new MdecCode( 0,    2)),
        new AcCode(          "0010", new MdecCode( 0,    3)),
        new AcCode(          "0011", new MdecCode( 0,    1)),
        new AcCode(          "1100", new MdecCode( 0,   -3)),
        new AcCode(          "1101", new MdecCode( 0,   -1)),
        new AcCode(          "1110", new MdecCode( 0,   -2)),
        new AcCode(         "00101", new MdecCode( 0,   -5)),
        new AcCode(         "00111", new MdecCode( 0,   -4)),
        new AcCode(         "01000", new MdecCode( 0,   -7)),
        new AcCode(         "01010", new MdecCode( 0,    6)),
        new AcCode(         "01111", new MdecCode( 0,    4)),
        new AcCode(         "10110", new MdecCode( 0,   -6)),
        new AcCode(         "11000", new MdecCode( 0,    7)),
        new AcCode(         "11011", new MdecCode( 0,    5)),
        new AcCode(        "000110", new MdecCode( 0,  -10)),
        new AcCode(        "010100", new MdecCode( 0,  -11)),
        new AcCode(        "100110", new MdecCode( 0,   10)),
        new AcCode(        "101001", new MdecCode( 0,   -9)),
        new AcCode(        "110000", new MdecCode( 0,   12)),
        new AcCode(        "110100", new MdecCode( 0,   11)),
        new AcCode(        "110101", new MdecCode( 0,    9)),
        new AcCode(        "110111", new MdecCode( 0,   -8)),
        new AcCode(        "111111", new MdecCode( 0,    8)),
        new AcCode(       "0001011", new MdecCode( 0,  -13)),
        new AcCode(       "0011010", new MdecCode( 0,   15)),
        new AcCode(       "0011111", new MdecCode( 0,  -12)),
        new AcCode(       "0100000", new MdecCode( 0,   16)),
        new AcCode(       "0111001", new MdecCode( 0,  -14)),
        new AcCode(       "1000100", new MdecCode( 0,  -15)),
        new AcCode(       "1001001", new MdecCode( 0,   14)),
        new AcCode(       "1001011", new MdecCode( 0,   13)),
        new AcCode(       "1011010", new MdecCode( 1,    1)),
        new AcCode(       "1100100", new MdecCode( 1,   -1)),
        new AcCode(      "00000100", new MdecCode( 0,   21)),
        new AcCode(      "00010000", new MdecCode( 0,   22)),
        new AcCode(      "00010111", new MdecCode( 1,   -2)),
        new AcCode(      "00011001", new MdecCode( 0,   20)),
        new AcCode(      "00101011", new MdecCode( 0,  -18)),
        new AcCode(      "01000000", new MdecCode( 1,    4)),
        new AcCode(      "01010111", new MdecCode( 0,   17)),
        new AcCode(      "01100000", new MdecCode( 0,  -21)),
        new AcCode(      "01101011", new MdecCode( 1,    2)),
        new AcCode(      "01111001", new MdecCode( 1,    3)),
        new AcCode(      "10000000", new MdecCode( 0,  -16)),
        new AcCode(      "10011001", new MdecCode( 0,   19)),
        new AcCode(      "11010000", new MdecCode( 1,   -4)),
        new AcCode(      "11010101", new MdecCode( 0,   18)),
        new AcCode(      "11011001", new MdecCode( 0,  -19)),
        new AcCode(      "11011111", new MdecCode( 0,  -17)),
        new AcCode(      "11111001", new MdecCode( 1,   -3)),
        new AcCode(      "11111010", new MdecCode( 0,  -20)),
        new AcCode(     "001010101", new MdecCode( 0,   24)),
        new AcCode(     "001011111", new MdecCode( 0,  -22)),
        new AcCode(     "001111010", new MdecCode( 0,  -25)),
        new AcCode(     "010000100", new MdecCode( 1,   -6)),
        new AcCode(     "010001001", new MdecCode( 0,   25)),
        new AcCode(     "010010111", new MdecCode( 0,  -23)),
        new AcCode(     "010111010", new MdecCode( 0,  -26)),
        new AcCode(     "100001001", new MdecCode( 0,  -24)),
        new AcCode(     "100010101", new MdecCode( 1,   -5)),
        new AcCode(     "100100100", new MdecCode( 0,  -27)),
        new AcCode(     "101010000", new MdecCode( 0,  -28)),
        new AcCode(     "101011111", new MdecCode( 0,   23)),
        new AcCode(     "110000100", new MdecCode( 0,   26)),
        new AcCode(     "110101011", new MdecCode( 1,    5)),
        new AcCode(     "110111010", new MdecCode( 1,    6)),
        new AcCode(     "111100000", new MdecCode( 0,   27)),
        new AcCode(    "0000001001", new MdecCode( 2,   -1)),
        new AcCode(    "0000010101", new MdecCode( 0,   31)),
        new AcCode(    "0001011001", new MdecCode( 2,    1)),
        new AcCode(    "0010010000", new MdecCode( 1,    9)),
        new AcCode(    "0010010101", new MdecCode( 0,  -31)),
        new AcCode(    "0010101011", new MdecCode( 1,    7)),
        new AcCode(    "0011000000", new MdecCode( 1,   -9)),
        new AcCode(    "0101011001", new MdecCode( 0,  -32)),
        new AcCode(    "0110010111", new MdecCode( 0,   28)),
        new AcCode(    "0110100100", new MdecCode( 0,  -35)),
        new AcCode(    "0111000000", new MdecCode( 0,  -36)),
        new AcCode(    "0111010111", new MdecCode( 0,  -29)),
        new AcCode(    "1000010101", new MdecCode( 0,   30)),
        new AcCode(    "1000100100", new MdecCode( 0,   33)),
        new AcCode(    "1000111010", new MdecCode( 1,   -8)),
        new AcCode(    "1010010000", new MdecCode( 0,   35)),
        new AcCode(    "1010010101", new MdecCode( 0,  -30)),
        new AcCode(    "1010100100", new MdecCode( 0,  -34)),
        new AcCode(    "1010101011", new MdecCode( 1,   -7)),
        new AcCode(    "1100111010", new MdecCode( 0,   32)),
        new AcCode(    "1101111010", new MdecCode( 0,  -33)),
        new AcCode(    "1110001001", new MdecCode( 1,    8)),
        new AcCode(    "1110010111", new MdecCode( 0,   29)),
        new AcCode(    "1111000000", new MdecCode( 0,  -37)),
        new AcCode(   "00000111010", new MdecCode( 0,  -42)),
        new AcCode(   "00010100100", new MdecCode( 0,  -43)),
        new AcCode(   "00011010111", new MdecCode( 0,   34)),
        new AcCode(   "00011100000", new MdecCode( 0,   40)),
        new AcCode(   "00011101011", new MdecCode( 0,   39)),
        new AcCode(   "00100111010", new MdecCode( 1,  -12)),
        new AcCode(   "00101010101", new MdecCode( 0,  -45)),
        new AcCode(   "00101111010", new MdecCode( 1,  -11)),
        new AcCode(   "00110001001", new MdecCode( 0,   44)),
        new AcCode(   "00110010000", new MdecCode( 0,   45)),
        new AcCode(   "00110010101", new MdecCode( 0,  -44)),
        new AcCode(   "01000001001", new MdecCode( 0,  -41)),
        new AcCode(   "01101010101", new MdecCode( 2,   -2)),
        new AcCode(   "01110010000", new MdecCode( 0,   42)),
        new AcCode(   "01110010101", new MdecCode( 0,  -39)),
        new AcCode(   "01111010111", new MdecCode( 0,   38)),
        new AcCode(   "01111101011", new MdecCode( 1,   10)),
        new AcCode(   "10001010000", new MdecCode( 1,   12)),
        new AcCode(   "10010100100", new MdecCode( 0,  -48)),
        new AcCode(   "10011100000", new MdecCode( 1,   11)),
        new AcCode(   "10011101011", new MdecCode( 1,  -10)),
        new AcCode(   "10110010000", new MdecCode( 0,  -46)),
        new AcCode(   "10111101011", new MdecCode( 0,   37)),
        new AcCode(   "11001011001", new MdecCode( 0,   41)),
        new AcCode(   "11011010111", new MdecCode( 0,   36)),
        new AcCode(   "11011101011", new MdecCode( 0,  -38)),
        new AcCode(   "11110010101", new MdecCode( 0,  -40)),
        new AcCode(   "11110100100", new MdecCode( 2,    3)),
        new AcCode(   "11111101011", new MdecCode( 2,    2)),
        new AcCode(  "000000100100", new MdecCode( 0,   48)),
        new AcCode(  "000111101011", new MdecCode( 2,   -3)),
        new AcCode(  "001001010000", new MdecCode( 0,   56)),
        new AcCode(  "001011000000", new MdecCode( 1,   15)),
        new AcCode(  "001011010111", new MdecCode( 0,  -47)),
        new AcCode(  "001011100000", new MdecCode( 0,  -57)),
        new AcCode(  "001011101011", new MdecCode( 1,   13)),
        new AcCode(  "010000111010", new MdecCode( 1,  -14)),
        new AcCode(  "010011010111", new MdecCode( 0,   46)),
        new AcCode(  "010101111010", new MdecCode( 0,   43)),
        new AcCode(  "010110001001", new MdecCode( 1,   14)),
        new AcCode(  "010110010101", new MdecCode( 0,  -53)),
        new AcCode(  "011000001001", new MdecCode( 2,    4)),
        new AcCode(  "011001010000", new MdecCode( 0,   52)),
        new AcCode(  "011011100000", new MdecCode( 0,   50)),
        new AcCode(  "011101011001", new MdecCode( 3,    1)),
        new AcCode(  "100000100100", new MdecCode( 1,  -15)),
        new AcCode(  "100001010000", new MdecCode( 3,   -1)),
        new AcCode(  "100111101011", new MdecCode( 1,  -13)),
        new AcCode(  "101001010000", new MdecCode( 1,   16)),
        new AcCode(  "101001011001", new MdecCode( 2,   -4)),
        new AcCode(  "101011000000", new MdecCode( 0,   49)),
        new AcCode(  "101011010111", new MdecCode( 0,   47)),
        new AcCode(  "101011100000", new MdecCode( 0,   51)),
        new AcCode(  "110000100100", new MdecCode( 0,  -56)),
        new AcCode(  "110101111010", new MdecCode( 0,  -49)),
        new AcCode(  "110110001001", new MdecCode( 0,  -52)),
        new AcCode(  "111011000000", new MdecCode( 1,  -16)),
        new AcCode(  "111101011001", new MdecCode( 0,  -50)),
        new AcCode(  "111111010111", new MdecCode( 0,  -51)),
        new AcCode( "0001101011001", new MdecCode( 0,   57)),
        new AcCode( "0010000100100", new MdecCode( 0,   60)),
        new AcCode( "0011011000000", new MdecCode( 1,  -21)),
        new AcCode( "0011101010101", new MdecCode( 0,   53)),
        new AcCode( "0011110010000", new MdecCode( 1,   20)),
        new AcCode( "0011111010111", new MdecCode( 0,  -60)),
        new AcCode( "0101101011001", new MdecCode( 1,   17)),
        new AcCode( "0110000111010", new MdecCode( 0,  -59)),
        new AcCode( "0110011010111", new MdecCode( 2,    5)),
        new AcCode( "0110100111010", new MdecCode( 3,   -2)),
        new AcCode( "0111000001001", new MdecCode( 0,   61)),
        new AcCode( "0111011100000", new MdecCode( 1,  -18)),
        new AcCode( "0111101010101", new MdecCode( 0,   55)),
        new AcCode( "0111110010000", new MdecCode( 1,   19)),
        new AcCode( "1000001010000", new MdecCode( 3,    2)),
        new AcCode( "1001110100100", new MdecCode( 0,   59)),
        new AcCode( "1010100111010", new MdecCode( 0,   54)),
        new AcCode( "1010101010101", new MdecCode( 0,  -54)),
        new AcCode( "1011011000000", new MdecCode( 0,   67)),
        new AcCode( "1011110010000", new MdecCode( 0,   82)),
        new AcCode( "1101011101011", new MdecCode( 0,  -66)),
        new AcCode( "1101101011001", new MdecCode( 0,  -72)),
        new AcCode( "1110000111010", new MdecCode( 2,    6)),
        new AcCode( "1110100111010", new MdecCode( 2,   -5)),
        new AcCode( "1111000001001", new MdecCode( 1,  -17)),
        new AcCode( "1111001010000", new MdecCode( 0,  -70)),
        new AcCode( "1111101010101", new MdecCode( 0,  -55)),
        new AcCode( "1111110010000", new MdecCode( 1,   18)),
        new AcCode("00000001010000", new MdecCode( 0,   69)),
        new AcCode("00001001011001", new MdecCode( 1,   21)),
        new AcCode("00001110100100", new MdecCode( 1,   27)),
        new AcCode("00010100111010", new MdecCode( 3,    3)),
        new AcCode("00010101010101", new MdecCode( 0,   66)),
        new AcCode("00101011101011", new MdecCode( 0,   80)),
        new AcCode("00101110100100", new MdecCode( 0,   63)),
        new AcCode("00110101010101", new MdecCode( 0,   58)),
        new AcCode("00110110010101", new MdecCode( 1,  -20)),
        new AcCode("00111001010000", new MdecCode( 1,  -26)),
        new AcCode("01001001011001", new MdecCode( 0,  109)),
        new AcCode("01001101011001", new MdecCode( 0, -170)),
        new AcCode("01010000100100", new MdecCode( 0,  -68)),
        new AcCode("01011101010101", new MdecCode( 1,   26)),
        new AcCode("01011111010111", new MdecCode( 0,  -58)),
        new AcCode("01101110100100", new MdecCode( 0,   68)),
        new AcCode("01110011010111", new MdecCode( 0,  -69)),
        new AcCode("01110101010101", new MdecCode( 2,    7)),
        new AcCode("01110110010101", new MdecCode( 0,  -61)),
        new AcCode("01111011100000", new MdecCode( 0,   64)),
        new AcCode("10000001010000", new MdecCode( 0,  -85)),
        new AcCode("10001001011001", new MdecCode( 0,  -63)),
        new AcCode("10001110100100", new MdecCode( 1,   22)),
        new AcCode("10010100111010", new MdecCode( 0,  -71)),
        new AcCode("10010101010101", new MdecCode( 0,  -67)),
        new AcCode("10101011101011", new MdecCode( 0,   65)),
        new AcCode("10101110100100", new MdecCode( 2,   -7)),
        new AcCode("10110101010101", new MdecCode( 1,  -19)),
        new AcCode("10110110010101", new MdecCode( 2,   -6)),
        new AcCode("10111001010000", new MdecCode( 0,   72)),
        new AcCode("11001001011001", new MdecCode( 0, -122)),
        new AcCode("11001101011001", new MdecCode( 3,   -3)),
        new AcCode("11010000100100", new MdecCode( 0,  -86)),
        new AcCode("11011101010101", new MdecCode( 0,   62)),
        new AcCode("11011111010111", new MdecCode( 0,  -62)),
        new AcCode("11101110100100", new MdecCode( 0,  -64)),
        new AcCode("11110011010111", new MdecCode( 0,  -94)),
        new AcCode("11110101010101", new MdecCode( 0, -109)),
        new AcCode("11110110010101", new MdecCode( 4,   -1)),
        new AcCode("11111011100000", new MdecCode( 0,  -75)),
    };

    private static final AcCode[] TABLE2 = {
        new AcCode(           "001", new MdecCode( 0,    1)),
        new AcCode(           "110", new MdecCode( 0,   -1)),
        new AcCode(          "0100", new MdecCode( 0,    4)),
        new AcCode(          "0101", new MdecCode( 0,    3)),
        new AcCode(          "0111", new MdecCode( 0,   -2)),
        new AcCode(          "1000", new MdecCode( 0,   -4)),
        new AcCode(          "1010", new MdecCode( 0,   -3)),
        new AcCode(          "1111", new MdecCode( 0,    2)),
        new AcCode(         "01101", new MdecCode( 0,   -5)),
        new AcCode(         "10000", new MdecCode( 0,   -6)),
        new AcCode(         "11100", new MdecCode( 0,    6)),
        new AcCode(         "11101", new MdecCode( 0,    5)),
        new AcCode(        "000011", new MdecCode( 0,   -7)),
        new AcCode(        "001011", new MdecCode( 1,   -1)),
        new AcCode(        "100010", new MdecCode( 0,   -8)),
        new AcCode(        "100011", new MdecCode( 1,    1)),
        new AcCode(        "101100", new MdecCode( 0,    8)),
        new AcCode(        "110011", new MdecCode( 0,    7)),
        new AcCode(       "0010010", new MdecCode( 0,  -10)),
        new AcCode(       "0101011", new MdecCode( 0,   -9)),
        new AcCode(       "0110010", new MdecCode( 1,    3)),
        new AcCode(       "0111011", new MdecCode( 1,   -2)),
        new AcCode(       "1001100", new MdecCode( 0,   10)),
        new AcCode(       "1010010", new MdecCode( 1,   -3)),
        new AcCode(       "1011011", new MdecCode( 0,    9)),
        new AcCode(       "1111011", new MdecCode( 1,    2)),
        new AcCode(      "00001100", new MdecCode( 0,  -14)),
        new AcCode(      "00010011", new MdecCode( 0,  -12)),
        new AcCode(      "00100000", new MdecCode( 2,   -1)),
        new AcCode(      "01000000", new MdecCode( 0,   14)),
        new AcCode(      "01010011", new MdecCode( 1,   -4)),
        new AcCode(      "01101011", new MdecCode( 1,    4)),
        new AcCode(      "10001100", new MdecCode( 0,   15)),
        new AcCode(      "10011011", new MdecCode( 0,   11)),
        new AcCode(      "10100000", new MdecCode( 2,    1)),
        new AcCode(      "11000010", new MdecCode( 0,   13)),
        new AcCode(      "11010011", new MdecCode( 0,  -11)),
        new AcCode(      "11100000", new MdecCode( 0,  -13)),
        new AcCode(      "11101011", new MdecCode( 0,   12)),
        new AcCode(     "000000010", new MdecCode( 0,   18)),
        new AcCode(     "000011011", new MdecCode( 1,    5)),
        new AcCode(     "001000010", new MdecCode( 2,   -2)),
        new AcCode(     "001100000", new MdecCode( 0,   16)),
        new AcCode(     "001110010", new MdecCode( 2,    2)),
        new AcCode(     "010000000", new MdecCode( 1,   -7)),
        new AcCode(     "010000010", new MdecCode( 0,  -17)),
        new AcCode(     "010010011", new MdecCode( 0,  -15)),
        new AcCode(     "100000000", new MdecCode( 1,   -5)),
        new AcCode(     "100000010", new MdecCode( 0,  -16)),
        new AcCode(     "101000010", new MdecCode( 0,   17)),
        new AcCode(     "101100000", new MdecCode( 0,   20)),
        new AcCode(     "101110010", new MdecCode( 1,    6)),
        new AcCode(     "111110010", new MdecCode( 1,   -6)),
        new AcCode(    "0011110010", new MdecCode( 0,  -20)),
        new AcCode(    "0100011011", new MdecCode( 0,  -18)),
        new AcCode(    "0110000010", new MdecCode( 0,   23)),
        new AcCode(    "0110010011", new MdecCode( 1,    7)),
        new AcCode(    "1011110010", new MdecCode( 2,   -3)),
        new AcCode(    "1100011011", new MdecCode( 0,   19)),
        new AcCode(    "1110010011", new MdecCode( 2,    3)),
        new AcCode( "0111110000000", new MdecCode( 0,  -19)),
        new AcCode( "1111110000000", new MdecCode( 1,    8)),
        new AcCode("00000011000000", new MdecCode( 0,  -34)),
        new AcCode("00000110000000", new MdecCode( 0,   42)),
        new AcCode("00000111000000", new MdecCode( 0,  -39)),
        new AcCode("00001011000000", new MdecCode( 2,   -7)),
        new AcCode("00001110000000", new MdecCode( 0,  -23)),
        new AcCode("00001110000010", new MdecCode( 3,    2)),
        new AcCode("00001111000000", new MdecCode( 0,   37)),
        new AcCode("00010011000000", new MdecCode( 1,  -12)),
        new AcCode("00010110000000", new MdecCode( 0,  -38)),
        new AcCode("00010111000000", new MdecCode( 0,  -24)),
        new AcCode("00011011000000", new MdecCode( 0,  -41)),
        new AcCode("00011110000000", new MdecCode( 0,  -76)),
        new AcCode("00011110000010", new MdecCode( 0,   22)),
        new AcCode("00011111000000", new MdecCode( 0,  -29)),
        new AcCode("00100011000000", new MdecCode( 1,   12)),
        new AcCode("00100110000000", new MdecCode( 0,  -68)),
        new AcCode("00100111000000", new MdecCode( 3,   -4)),
        new AcCode("00101011000000", new MdecCode( 0,  -92)),
        new AcCode("00101110000000", new MdecCode( 3,    1)),
        new AcCode("00101110000010", new MdecCode( 0,   27)),
        new AcCode("00101111000000", new MdecCode( 0,  -35)),
        new AcCode("00110011000000", new MdecCode( 0,  -40)),
        new AcCode("00110110000000", new MdecCode( 1,  -20)),
        new AcCode("00110111000000", new MdecCode( 0,  -26)),
        new AcCode("00111011000000", new MdecCode( 0,   59)),
        new AcCode("00111110000010", new MdecCode( 1,    9)),
        new AcCode("00111111000000", new MdecCode( 0,   40)),
        new AcCode("01000011000000", new MdecCode( 0,   55)),
        new AcCode("01000110000000", new MdecCode( 2,  -13)),
        new AcCode("01000111000000", new MdecCode( 0,  -32)),
        new AcCode("01001011000000", new MdecCode( 1,   18)),
        new AcCode("01001110000000", new MdecCode( 0,   21)),
        new AcCode("01001110000010", new MdecCode( 2,    4)),
        new AcCode("01001111000000", new MdecCode( 0,   30)),
        new AcCode("01010011000000", new MdecCode( 1,  -15)),
        new AcCode("01010110000000", new MdecCode( 0,   43)),
        new AcCode("01010111000000", new MdecCode( 0,   32)),
        new AcCode("01011011000000", new MdecCode( 3,    4)),
        new AcCode("01011110000000", new MdecCode( 0,   56)),
        new AcCode("01011110000010", new MdecCode( 0,  -25)),
        new AcCode("01011111000000", new MdecCode( 3,   -3)),
        new AcCode("01100011000000", new MdecCode( 2,    7)),
        new AcCode("01100110000000", new MdecCode( 0,   53)),
        new AcCode("01100111000000", new MdecCode( 0,  -33)),
        new AcCode("01101011000000", new MdecCode( 1,   15)),
        new AcCode("01101110000000", new MdecCode( 1,   -8)),
        new AcCode("01101110000010", new MdecCode( 0,   29)),
        new AcCode("01101111000000", new MdecCode( 4,   -1)),
        new AcCode("01110011000000", new MdecCode( 2,   -6)),
        new AcCode("01110110000000", new MdecCode( 1,  -13)),
        new AcCode("01110111000000", new MdecCode( 0,   39)),
        new AcCode("01111011000000", new MdecCode( 0,  -43)),
        new AcCode("01111110000010", new MdecCode( 0,  -27)),
        new AcCode("01111111000000", new MdecCode( 2,   -5)),
        new AcCode("10000011000000", new MdecCode( 1,  -11)),
        new AcCode("10000110000000", new MdecCode( 0,   58)),
        new AcCode("10000111000000", new MdecCode( 0,   31)),
        new AcCode("10001011000000", new MdecCode( 3,    5)),
        new AcCode("10001110000000", new MdecCode( 0,  -21)),
        new AcCode("10001110000010", new MdecCode( 3,   -2)),
        new AcCode("10001111000000", new MdecCode( 0,   44)),
        new AcCode("10010011000000", new MdecCode( 0,  -37)),
        new AcCode("10010110000000", new MdecCode( 2,    9)),
        new AcCode("10010111000000", new MdecCode( 0,   41)),
        new AcCode("10011011000000", new MdecCode( 0,  -49)),
        new AcCode("10011110000000", new MdecCode( 0, -114)),
        new AcCode("10011110000010", new MdecCode( 2,   -4)),
        new AcCode("10011111000000", new MdecCode( 0,   36)),
        new AcCode("10100011000000", new MdecCode( 4,    3)),
        new AcCode("10100110000000", new MdecCode( 0,  -71)),
        new AcCode("10100111000000", new MdecCode( 3,    3)),
        new AcCode("10101011000000", new MdecCode( 0,   50)),
        new AcCode("10101110000000", new MdecCode( 0,  -22)),
        new AcCode("10101110000010", new MdecCode( 0,  -28)),
        new AcCode("10101111000000", new MdecCode( 1,  -14)),
        new AcCode("10110011000000", new MdecCode( 0,  -44)),
        new AcCode("10110110000000", new MdecCode( 2,   -8)),
        new AcCode("10110111000000", new MdecCode( 0,  -30)),
        new AcCode("10111011000000", new MdecCode( 1,  -18)),
        new AcCode("10111110000010", new MdecCode( 0,   24)),
        new AcCode("10111111000000", new MdecCode( 1,   11)),
        new AcCode("11000011000000", new MdecCode( 0,   57)),
        new AcCode("11000110000000", new MdecCode( 0,   47)),
        new AcCode("11000111000000", new MdecCode( 1,   10)),
        new AcCode("11001011000000", new MdecCode( 4,    2)),
        new AcCode("11001110000000", new MdecCode( 3,   -1)),
        new AcCode("11001110000010", new MdecCode( 0,   26)),
        new AcCode("11001111000000", new MdecCode( 2,    6)),
        new AcCode("11010011000000", new MdecCode( 0,   34)),
        new AcCode("11010110000000", new MdecCode( 4,   -2)),
        new AcCode("11010111000000", new MdecCode( 1,   -9)),
        new AcCode("11011011000000", new MdecCode( 1,   14)),
        new AcCode("11011110000000", new MdecCode( 0,  -57)),
        new AcCode("11011110000010", new MdecCode( 0,   25)),
        new AcCode("11011111000000", new MdecCode( 4,    1)),
        new AcCode("11100011000000", new MdecCode( 0,  -42)),
        new AcCode("11100110000000", new MdecCode( 0,  -45)),
        new AcCode("11100111000000", new MdecCode( 0,  -36)),
        new AcCode("11101011000000", new MdecCode( 2,    8)),
        new AcCode("11101110000000", new MdecCode( 0,   33)),
        new AcCode("11101110000010", new MdecCode( 0,  -31)),
        new AcCode("11101111000000", new MdecCode( 0,   35)),
        new AcCode("11110011000000", new MdecCode( 0,   38)),
        new AcCode("11110110000000", new MdecCode( 1,   13)),
        new AcCode("11110111000000", new MdecCode( 2,    5)),
        new AcCode("11111011000000", new MdecCode( 0,  -56)),
        new AcCode("11111110000010", new MdecCode( 0,   28)),
        new AcCode("11111111000000", new MdecCode( 1,  -10)),
    };

    private static final AcCode[] TABLE3 = {
        new AcCode(            "10", new MdecCode( 0,    1)),
        new AcCode(           "101", new MdecCode( 0,   -1)),
        new AcCode(          "0001", new MdecCode( 1,   -1)),
        new AcCode(          "0011", new MdecCode( 0,   -2)),
        new AcCode(          "1011", new MdecCode( 0,    2)),
        new AcCode(          "1111", new MdecCode( 1,    1)),
        new AcCode(         "00100", new MdecCode( 0,    3)),
        new AcCode(         "01100", new MdecCode( 2,    1)),
        new AcCode(         "10100", new MdecCode( 0,   -3)),
        new AcCode(         "11100", new MdecCode( 2,   -1)),
        new AcCode(        "001000", new MdecCode( 1,    2)),
        new AcCode(        "010000", new MdecCode( 4,   -1)),
        new AcCode(        "011001", new MdecCode( 3,    1)),
        new AcCode(        "100000", new MdecCode( 0,   -4)),
        new AcCode(        "100111", new MdecCode( 3,   -1)),
        new AcCode(        "101000", new MdecCode( 0,    4)),
        new AcCode(        "111000", new MdecCode( 1,   -2)),
        new AcCode(       "0011000", new MdecCode( 0,   -5)),
        new AcCode(       "0101001", new MdecCode( 4,    1)),
        new AcCode(       "0110000", new MdecCode( 6,    1)),
        new AcCode(       "1000000", new MdecCode( 6,   -1)),
        new AcCode(       "1001001", new MdecCode( 5,   -1)),
        new AcCode(       "1010111", new MdecCode( 0,    5)),
        new AcCode(       "1111001", new MdecCode( 5,    1)),
        new AcCode(      "00000111", new MdecCode( 1,   -3)),
        new AcCode(      "00001001", new MdecCode( 7,   -1)),
        new AcCode(      "01000111", new MdecCode( 0,    6)),
        new AcCode(      "01011000", new MdecCode( 8,   -1)),
        new AcCode(      "01110111", new MdecCode( 0,   -6)),
        new AcCode(      "10000111", new MdecCode( 1,    3)),
        new AcCode(      "10110111", new MdecCode( 2,   -2)),
        new AcCode(      "10111001", new MdecCode( 7,    1)),
        new AcCode(      "11110111", new MdecCode( 2,    2)),
        new AcCode(     "001101001", new MdecCode( 9,    1)),
        new AcCode(     "010010111", new MdecCode( 3,    2)),
        new AcCode(     "011011000", new MdecCode( 3,   -2)),
        new AcCode(     "011110000", new MdecCode( 1,   -4)),
        new AcCode(     "100000000", new MdecCode( 8,    1)),
        new AcCode(     "100110111", new MdecCode(10,    1)),
        new AcCode(     "100111001", new MdecCode( 0,   -7)),
        new AcCode(     "101101001", new MdecCode( 9,   -1)),
        new AcCode(     "110000000", new MdecCode(11,    1)),
        new AcCode(     "110001001", new MdecCode( 0,    7)),
        new AcCode(     "111000111", new MdecCode(10,   -1)),
        new AcCode(     "111011000", new MdecCode( 1,    4)),
        new AcCode(     "111110000", new MdecCode( 0,    8)),
        new AcCode(    "0000010111", new MdecCode( 0,   -9)),
        new AcCode(    "0000110111", new MdecCode( 2,   -3)),
        new AcCode(    "0000111001", new MdecCode(12,   -1)),
        new AcCode(    "0011000111", new MdecCode(12,    1)),
        new AcCode(    "0011101001", new MdecCode(11,   -1)),
        new AcCode(    "0110010111", new MdecCode( 4,    2)),
        new AcCode(    "1000010111", new MdecCode(13,    1)),
        new AcCode(    "1000110111", new MdecCode( 0,    9)),
        new AcCode(    "1010000000", new MdecCode( 1,    5)),
        new AcCode(    "1011000111", new MdecCode( 2,    3)),
        new AcCode(    "1011101001", new MdecCode( 0,   -8)),
        new AcCode(    "1100010111", new MdecCode(13,   -1)),
        new AcCode(    "1110010111", new MdecCode( 4,   -2)),
        new AcCode(   "00010000000", new MdecCode( 0,  -11)),
        new AcCode(   "00010001001", new MdecCode( 5,   -2)),
        new AcCode(   "00100010111", new MdecCode(14,   -1)),
        new AcCode(   "00111101001", new MdecCode( 1,   -5)),
        new AcCode(   "01000111001", new MdecCode( 5,    2)),
        new AcCode(   "01010001001", new MdecCode( 0,  -10)),
        new AcCode(   "10010000000", new MdecCode(14,    1)),
        new AcCode(   "10010001001", new MdecCode( 0,   10)),
        new AcCode(   "10100010111", new MdecCode( 0,   11)),
        new AcCode(   "11101110000", new MdecCode( 2,   -4)),
        new AcCode(  "001111101001", new MdecCode( 3,   -3)),
        new AcCode(  "010111101001", new MdecCode( 3,    3)),
        new AcCode(  "011000111001", new MdecCode( 7,    2)),
        new AcCode(  "011010001001", new MdecCode( 2,    4)),
        new AcCode(  "011111101001", new MdecCode( 6,    2)),
        new AcCode(  "101111101001", new MdecCode( 1,   -6)),
        new AcCode(  "110111101001", new MdecCode( 1,    6)),
        new AcCode(  "111000111001", new MdecCode( 0,  -12)),
        new AcCode(  "111010001001", new MdecCode( 0,   12)),
        new AcCode(  "111111101001", new MdecCode( 6,   -2)),
        new AcCode("00000001110000", new MdecCode( 2,    5)),
        new AcCode("00000101110000", new MdecCode(19,   -1)),
        new AcCode("00001001110000", new MdecCode(11,   -2)),
        new AcCode("00001101110000", new MdecCode( 0,  -13)),
        new AcCode("00010001110000", new MdecCode( 0,  -14)),
        new AcCode("00010101110000", new MdecCode(21,   -1)),
        new AcCode("00011001110000", new MdecCode( 1,    8)),
        new AcCode("00100001110000", new MdecCode(17,   -1)),
        new AcCode("00100101110000", new MdecCode( 1,    9)),
        new AcCode("00101001110000", new MdecCode( 3,   -5)),
        new AcCode("00101101110000", new MdecCode(15,    1)),
        new AcCode("00110001110000", new MdecCode( 3,    4)),
        new AcCode("00110101110000", new MdecCode( 6,    3)),
        new AcCode("00111001110000", new MdecCode(17,    1)),
        new AcCode("01000001110000", new MdecCode(16,   -1)),
        new AcCode("01000101110000", new MdecCode( 4,    4)),
        new AcCode("01001001110000", new MdecCode( 9,   -2)),
        new AcCode("01001101110000", new MdecCode( 7,   -2)),
        new AcCode("01010001110000", new MdecCode( 4,   -3)),
        new AcCode("01010101110000", new MdecCode( 2,    6)),
        new AcCode("01011001110000", new MdecCode(16,    1)),
        new AcCode("01100001110000", new MdecCode( 5,   -3)),
        new AcCode("01100101110000", new MdecCode( 4,   -4)),
        new AcCode("01101001110000", new MdecCode(10,    2)),
        new AcCode("01101101110000", new MdecCode(15,   -1)),
        new AcCode("01110001110000", new MdecCode( 0,   14)),
        new AcCode("01110101110000", new MdecCode( 0,  -17)),
        new AcCode("01111001110000", new MdecCode( 0,   16)),
        new AcCode("10000001110000", new MdecCode(11,    2)),
        new AcCode("10000101110000", new MdecCode(20,   -1)),
        new AcCode("10001001110000", new MdecCode( 0,   17)),
        new AcCode("10001101110000", new MdecCode( 1,    7)),
        new AcCode("10010001110000", new MdecCode( 8,   -2)),
        new AcCode("10010101110000", new MdecCode(20,    1)),
        new AcCode("10011001110000", new MdecCode( 1,   -8)),
        new AcCode("10100001110000", new MdecCode( 5,    3)),
        new AcCode("10100101110000", new MdecCode( 7,    3)),
        new AcCode("10101001110000", new MdecCode(10,   -2)),
        new AcCode("10101101110000", new MdecCode( 8,    2)),
        new AcCode("10110001110000", new MdecCode( 3,   -4)),
        new AcCode("10110101110000", new MdecCode( 0,   18)),
        new AcCode("10111001110000", new MdecCode(19,    1)),
        new AcCode("11000001110000", new MdecCode( 0,  -15)),
        new AcCode("11000101110000", new MdecCode( 6,   -3)),
        new AcCode("11001001110000", new MdecCode(18,    1)),
        new AcCode("11001101110000", new MdecCode( 0,   13)),
        new AcCode("11010001110000", new MdecCode( 4,    3)),
        new AcCode("11010101110000", new MdecCode(18,   -1)),
        new AcCode("11011001110000", new MdecCode( 0,  -16)),
        new AcCode("11100001110000", new MdecCode( 0,   15)),
        new AcCode("11100101110000", new MdecCode( 0,  -18)),
        new AcCode("11101001110000", new MdecCode( 2,   -6)),
        new AcCode("11101101110000", new MdecCode( 1,   -7)),
        new AcCode("11110001110000", new MdecCode( 2,   -5)),
        new AcCode("11110101110000", new MdecCode(12,   -2)),
        new AcCode("11111001110000", new MdecCode( 9,    2)),
    };

    static {
        buidLookup(TABLE1, TABLE1_LOOKUP);
        buidLookup(TABLE2, TABLE2_LOOKUP);
        buidLookup(TABLE3, TABLE3_LOOKUP);
    }
}
