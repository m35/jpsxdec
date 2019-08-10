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

/** Pretty standard variable length code bit reading mapped to DC codes.
 * This is for the Aconcagua opening FMV. The ending FMV seems to be different. */
public class DcTable {


    public static class DcCode {
        @Nonnull
        private final String _sBits;
        private final int _iRelativeDcCoefficient;

        protected DcCode(@Nonnull String sBits, int iRelativeDcCoefficient) {
            _sBits = sBits;
            _iRelativeDcCoefficient = iRelativeDcCoefficient;
        }

        public int getBitCodeLen() {
            return _sBits.length();
        }

        public int getRelativeDcCoefficient() {
            return _iRelativeDcCoefficient;
        }

        @Override
        public String toString() {
            return make();
        }

        public @Nonnull String make() {
            return String.format("new DcCode(%13s, %5d),",
                                 "\""+_sBits+"\"", _iRelativeDcCoefficient);
        }
    }

    public static @Nonnull DcCode lookup(int iBottom11Bits) throws MdecException.ReadCorruption {
        DcCode code = LOOKUP[iBottom11Bits & BOTTOM_11_BITS];
        if (code == null)
            throw new MdecException.ReadCorruption(Misc.bitsToString(iBottom11Bits, BIT_LENGTH));
        return code;
    }

    private static final int BIT_LENGTH = 11;
    private static final int ENTRY_COUNT = 2048;
    private static final int BOTTOM_11_BITS = 0x7FF;
    private static void buildLookup() {
        for (DcCode dc : DC_CODES) {
            int iBits = Integer.parseInt(dc._sBits, 2);
            int iMax = BOTTOM_11_BITS >>> dc._sBits.length();

            for (int i = 0; i <= iMax; i++) {
                int j = (i << dc._sBits.length()) | iBits;
                if (LOOKUP[j] != null)
                    throw new RuntimeException("Wrong logic");
                LOOKUP[j] = dc;
            }
        }
    }

    static final DcCode[] LOOKUP = new DcCode[ENTRY_COUNT];

    private static final DcCode[] DC_CODES = {
        new DcCode(        "100", -1024),
        new DcCode(       "0011",     1),
        new DcCode(       "1101",  1023),
        new DcCode(      "01110",    -2),
        new DcCode(      "10111",    -3),
        new DcCode(      "11110", -1022),
        new DcCode(      "11111", -1021),
        new DcCode(     "001001",  1020),
        new DcCode(     "100001",     4),
        new DcCode(     "110001",   516),
        new DcCode(     "111011",     5),
        new DcCode(    "0000010",    -5),
        new DcCode(    "0000110",    -6),
        new DcCode(    "0001010", -1018),
        new DcCode(    "0100110", -1017),
        new DcCode(    "1000101", -1016),
        new DcCode(    "1011001",    -7),
        new DcCode(    "1011011", -1015),
        new DcCode(    "1110101",    -8),
        new DcCode(   "00001111",  1004),
        new DcCode(   "00100010",  1015),
        new DcCode(   "00101010",    11),
        new DcCode(   "00101111",  1001),
        new DcCode(   "00111010",    12),
        new DcCode(   "01001010",  1011),
        new DcCode(   "01010001",    15),
        new DcCode(   "01011010",  1012),
        new DcCode(   "01100010",    10),
        new DcCode(   "01110010",  1014),
        new DcCode(   "01111010",  1010),
        new DcCode(   "10000001",    13),
        new DcCode(   "10000101",  1006),
        new DcCode(   "10001011",  1003),
        new DcCode(   "10001111",    20),
        new DcCode(   "10101011",  1005),
        new DcCode(   "10101111",    18),
        new DcCode(   "10110110",  1008),
        new DcCode(   "11000111",    19),
        new DcCode(   "11001011",    17),
        new DcCode(   "11010001",  1009),
        new DcCode(   "11010101",    14),
        new DcCode(   "11101010",  1013),
        new DcCode(   "11101011",  1007),
        new DcCode(   "11111001",    16),
        new DcCode(  "000000001",   -34),
        new DcCode(  "000000101",  -983),
        new DcCode(  "000010001",   -36),
        new DcCode(  "000010010",   -28),
        new DcCode(  "000010101",   -55),
        new DcCode(  "000010110",   -30),
        new DcCode(  "000100101",  -984),
        new DcCode(  "000100111",  -963),
        new DcCode(  "000101011",   -51),
        new DcCode(  "000110110",  -992),
        new DcCode(  "001001111",  -960),
        new DcCode(  "001010010",  -997),
        new DcCode(  "001010110",  -985),
        new DcCode(  "001100101",  -976),
        new DcCode(  "001100111",  -965),
        new DcCode(  "001101001",  -990),
        new DcCode(  "001111001",   -41),
        new DcCode(  "010010101",  -972),
        new DcCode(  "010011011",   -57),
        new DcCode(  "010100010", -1003),
        new DcCode(  "010100111",  -974),
        new DcCode(  "010101001",   -38),
        new DcCode(  "010101010", -1001),
        new DcCode(  "010110010",   -24),
        new DcCode(  "010111001",  -977),
        new DcCode(  "011000110",   -33),
        new DcCode(  "011001111",   -52),
        new DcCode(  "011100010",  -999),
        new DcCode(  "011100101",   -44),
        new DcCode(  "011100111",   -59),
        new DcCode(  "011101001",  -979),
        new DcCode(  "011110010", -1000),
        new DcCode(  "011110110",  -991),
        new DcCode(  "011111010",   -37),
        new DcCode(  "100000001",  -989),
        new DcCode(  "100000111",   -53),
        new DcCode(  "100001011",   -54),
        new DcCode(  "100010001",   -35),
        new DcCode(  "100010010",   -25),
        new DcCode(  "100010101",   -46),
        new DcCode(  "100010110",   -39),
        new DcCode(  "100101011",   -45),
        new DcCode(  "100110010",   -26),
        new DcCode(  "100110110",   -31),
        new DcCode(  "100111001",  -981),
        new DcCode(  "101000001",  -988),
        new DcCode(  "101000111",  -967),
        new DcCode(  "101001011",   -47),
        new DcCode(  "101001111",   -50),
        new DcCode(  "101010101",  -986),
        new DcCode(  "101100101",   -40),
        new DcCode(  "101100111",  -962),
        new DcCode(  "101101010",  -998),
        new DcCode(  "101101011",  -975),
        new DcCode(  "101101111",  -970),
        new DcCode(  "101111001",  -980),
        new DcCode(  "110000111",  -969),
        new DcCode(  "110010001",  -982),
        new DcCode(  "110010101",  -987),
        new DcCode(  "110010110",  -995),
        new DcCode(  "110011001",   -48),
        new DcCode(  "110011010",   -29),
        new DcCode(  "110011011",   -43),
        new DcCode(  "110100101",  -978),
        new DcCode(  "110100111",  -968),
        new DcCode(  "110101010", -1002),
        new DcCode(  "110110101",   -42),
        new DcCode(  "111000010",   -22),
        new DcCode(  "111000110",  -993),
        new DcCode(  "111011010",  -994),
        new DcCode(  "111100111",   -49),
        new DcCode(  "111110010",   -27),
        new DcCode(  "111110110",   -32),
        new DcCode(  "111111010",  -996),
        new DcCode( "0000000111",   111),
        new DcCode( "0000001011",   909),
        new DcCode( "0000011001",   901),
        new DcCode( "0000011010",   946),
        new DcCode( "0000011011",   120),
        new DcCode( "0000101001",   936),
        new DcCode( "0000110010",    69),
        new DcCode( "0000110101",   903),
        new DcCode( "0000111001",    90),
        new DcCode( "0001000001",   941),
        new DcCode( "0001000110",    67),
        new DcCode( "0001000111",   121),
        new DcCode( "0001001011",   898),
        new DcCode( "0001010101",   920),
        new DcCode( "0001100110",   940),
        new DcCode( "0001101010",    58),
        new DcCode( "0001101011",   912),
        new DcCode( "0001101111",   126),
        new DcCode( "0001110110",   926),
        new DcCode( "0010000111",   914),
        new DcCode( "0010010001",   103),
        new DcCode( "0010010010",    77),
        new DcCode( "0010010110",   948),
        new DcCode( "0010011001",   107),
        new DcCode( "0010011010",   957),
        new DcCode( "0010100101",   100),
        new DcCode( "0010110101",   109),
        new DcCode( "0010111010",   951),
        new DcCode( "0011000001",    89),
        new DcCode( "0011000010",   962),
        new DcCode( "0011001010",   958),
        new DcCode( "0011010010",   963),
        new DcCode( "0011010110",   942),
        new DcCode( "0011011010",    97),
        new DcCode( "0011100110",    76),
        new DcCode( "0011101111",   907),
        new DcCode( "0100000101",   110),
        new DcCode( "0100011001",    98),
        new DcCode( "0100011010",    82),
        new DcCode( "0100011011",   128),
        new DcCode( "0100100101",   932),
        new DcCode( "0100100111",   119),
        new DcCode( "0100101001",   928),
        new DcCode( "0100110101",   911),
        new DcCode( "0101000010",    53),
        new DcCode( "0101000110",    74),
        new DcCode( "0101010010",    68),
        new DcCode( "0101010110",   947),
        new DcCode( "0101100110",    86),
        new DcCode( "0101101001",   931),
        new DcCode( "0101110110",   952),
        new DcCode( "0110010010",   961),
        new DcCode( "0110100010",   960),
        new DcCode( "0110101001",   927),
        new DcCode( "0110110010",   944),
        new DcCode( "0110111001",   915),
        new DcCode( "0110111010",   956),
        new DcCode( "0111000001",    91),
        new DcCode( "0111001010",    65),
        new DcCode( "0111001111",   108),
        new DcCode( "0111010010",   968),
        new DcCode( "0111010110",   938),
        new DcCode( "0111100010",   954),
        new DcCode( "0111100101",    96),
        new DcCode( "0111100110",   935),
        new DcCode( "0111101001",   923),
        new DcCode( "0111101111",   886),
        new DcCode( "1000000111",   906),
        new DcCode( "1000001011",   890),
        new DcCode( "1000011001",   102),
        new DcCode( "1000011010",   950),
        new DcCode( "1000011011",   114),
        new DcCode( "1000101001",   917),
        new DcCode( "1000110010",    66),
        new DcCode( "1000110101",    99),
        new DcCode( "1000111001",   908),
        new DcCode( "1001000001",   930),
        new DcCode( "1001000010",   964),
        new DcCode( "1001000110",    93),
        new DcCode( "1001000111",   904),
        new DcCode( "1001001011",   106),
        new DcCode( "1001010101",   916),
        new DcCode( "1001100110",    81),
        new DcCode( "1001101010",    87),
        new DcCode( "1001101011",   895),
        new DcCode( "1001101111",   118),
        new DcCode( "1001110110",   922),
        new DcCode( "1010000111",   132),
        new DcCode( "1010010001",   929),
        new DcCode( "1010010010",    70),
        new DcCode( "1010010110",   924),
        new DcCode( "1010011001",    92),
        new DcCode( "1010011010",    84),
        new DcCode( "1010100101",   918),
        new DcCode( "1010110101",   105),
        new DcCode( "1010111010",    85),
        new DcCode( "1011000001",   905),
        new DcCode( "1011000010",   966),
        new DcCode( "1011001010",   955),
        new DcCode( "1011010010",    80),
        new DcCode( "1011010110",    79),
        new DcCode( "1011011010",    75),
        new DcCode( "1011100110",    71),
        new DcCode( "1011101111",   896),
        new DcCode( "1100000101",   101),
        new DcCode( "1100011001",    72),
        new DcCode( "1100011010",   943),
        new DcCode( "1100011011",   104),
        new DcCode( "1100100101",   115),
        new DcCode( "1100100111",   910),
        new DcCode( "1100101001",   925),
        new DcCode( "1100110101",   899),
        new DcCode( "1101000110",   933),
        new DcCode( "1101010010",    60),
        new DcCode( "1101010110",   937),
        new DcCode( "1101100110",    94),
        new DcCode( "1101101001",   913),
        new DcCode( "1101110110",    83),
        new DcCode( "1110010010",   949),
        new DcCode( "1110100010",   959),
        new DcCode( "1110101001",   921),
        new DcCode( "1110110010",    63),
        new DcCode( "1110111001",   939),
        new DcCode( "1110111010",   934),
        new DcCode( "1111000001",    88),
        new DcCode( "1111001010",   945),
        new DcCode( "1111001111",   893),
        new DcCode( "1111010010",    78),
        new DcCode( "1111010110",   953),
        new DcCode( "1111100010",    51),
        new DcCode( "1111100101",   919),
        new DcCode( "1111100110",    73),
        new DcCode( "1111101001",    95),
        new DcCode( "1111101111",   116),
        new DcCode("00001000010",  -156),
        new DcCode("01101000010",  -879),
        new DcCode("10001000010",  -912),
        new DcCode("11101000010",  -124),
    };

    static {
        buildLookup();
    }
}
