/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.psxvideo.bitstreams;

import javax.annotation.Nonnull;

/**
 * Specifically designed to decode the 111 MPEG1 AC coefficient
 * variable-length (Huffman) bit codes, also used by the PlayStation.
 *
 * The codes defined in the MPEG1 spec lists the 111 codes with a 'sign' bit at
 * the end. Here we split the code with the sign bit into two codes. The order
 * follows the list found in the MPG1 specification with the positive sign bit
 * (0) first, followed by the negative sign bit (1). Consumers of this table are
 * dependent on the order of these codes.
 */
public enum BitStreamCode {

    _110______________,  _111______________,  // 11s
    _0110_____________,  _0111_____________,  // 011s
    _01000____________,  _01001____________,  // 0100s
    _01010____________,  _01011____________,  // 0101s
    _001010___________,  _001011___________,  // 00101s
    _001100___________,  _001101___________,  // 00110s
    _001110___________,  _001111___________,  // 00111s
    _0001000__________,  _0001001__________,  // 000100s
    _0001010__________,  _0001011__________,  // 000101s
    _0001100__________,  _0001101__________,  // 000110s
    _0001110__________,  _0001111__________,  // 000111s
    _00001000_________,  _00001001_________,  // 0000100s
    _00001010_________,  _00001011_________,  // 0000101s
    _00001100_________,  _00001101_________,  // 0000110s
    _00001110_________,  _00001111_________,  // 0000111s
    _001000000________,  _001000001________,  // 00100000s
    _001000010________,  _001000011________,  // 00100001s
    _001000100________,  _001000101________,  // 00100010s
    _001000110________,  _001000111________,  // 00100011s
    _001001000________,  _001001001________,  // 00100100s
    _001001010________,  _001001011________,  // 00100101s
    _001001100________,  _001001101________,  // 00100110s
    _001001110________,  _001001111________,  // 00100111s
    _00000010000______,  _00000010001______,  // 0000001000s
    _00000010010______,  _00000010011______,  // 0000001001s
    _00000010100______,  _00000010101______,  // 0000001010s
    _00000010110______,  _00000010111______,  // 0000001011s
    _00000011000______,  _00000011001______,  // 0000001100s
    _00000011010______,  _00000011011______,  // 0000001101s
    _00000011100______,  _00000011101______,  // 0000001110s
    _00000011110______,  _00000011111______,  // 0000001111s
    _0000000100000____,  _0000000100001____,  // 000000010000s
    _0000000100010____,  _0000000100011____,  // 000000010001s
    _0000000100100____,  _0000000100101____,  // 000000010010s
    _0000000100110____,  _0000000100111____,  // 000000010011s
    _0000000101000____,  _0000000101001____,  // 000000010100s
    _0000000101010____,  _0000000101011____,  // 000000010101s
    _0000000101100____,  _0000000101101____,  // 000000010110s
    _0000000101110____,  _0000000101111____,  // 000000010111s
    _0000000110000____,  _0000000110001____,  // 000000011000s
    _0000000110010____,  _0000000110011____,  // 000000011001s
    _0000000110100____,  _0000000110101____,  // 000000011010s
    _0000000110110____,  _0000000110111____,  // 000000011011s
    _0000000111000____,  _0000000111001____,  // 000000011100s
    _0000000111010____,  _0000000111011____,  // 000000011101s
    _0000000111100____,  _0000000111101____,  // 000000011110s
    _0000000111110____,  _0000000111111____,  // 000000011111s
    _00000000100000___,  _00000000100001___,  // 0000000010000s
    _00000000100010___,  _00000000100011___,  // 0000000010001s
    _00000000100100___,  _00000000100101___,  // 0000000010010s
    _00000000100110___,  _00000000100111___,  // 0000000010011s
    _00000000101000___,  _00000000101001___,  // 0000000010100s
    _00000000101010___,  _00000000101011___,  // 0000000010101s
    _00000000101100___,  _00000000101101___,  // 0000000010110s
    _00000000101110___,  _00000000101111___,  // 0000000010111s
    _00000000110000___,  _00000000110001___,  // 0000000011000s
    _00000000110010___,  _00000000110011___,  // 0000000011001s
    _00000000110100___,  _00000000110101___,  // 0000000011010s
    _00000000110110___,  _00000000110111___,  // 0000000011011s
    _00000000111000___,  _00000000111001___,  // 0000000011100s
    _00000000111010___,  _00000000111011___,  // 0000000011101s
    _00000000111100___,  _00000000111101___,  // 0000000011110s
    _00000000111110___,  _00000000111111___,  // 0000000011111s
    _000000000100000__,  _000000000100001__,  // 00000000010000s
    _000000000100010__,  _000000000100011__,  // 00000000010001s
    _000000000100100__,  _000000000100101__,  // 00000000010010s
    _000000000100110__,  _000000000100111__,  // 00000000010011s
    _000000000101000__,  _000000000101001__,  // 00000000010100s
    _000000000101010__,  _000000000101011__,  // 00000000010101s
    _000000000101100__,  _000000000101101__,  // 00000000010110s
    _000000000101110__,  _000000000101111__,  // 00000000010111s
    _000000000110000__,  _000000000110001__,  // 00000000011000s
    _000000000110010__,  _000000000110011__,  // 00000000011001s
    _000000000110100__,  _000000000110101__,  // 00000000011010s
    _000000000110110__,  _000000000110111__,  // 00000000011011s
    _000000000111000__,  _000000000111001__,  // 00000000011100s
    _000000000111010__,  _000000000111011__,  // 00000000011101s
    _000000000111100__,  _000000000111101__,  // 00000000011110s
    _000000000111110__,  _000000000111111__,  // 00000000011111s
    _0000000000100000_,  _0000000000100001_,  // 000000000010000s
    _0000000000100010_,  _0000000000100011_,  // 000000000010001s
    _0000000000100100_,  _0000000000100101_,  // 000000000010010s
    _0000000000100110_,  _0000000000100111_,  // 000000000010011s
    _0000000000101000_,  _0000000000101001_,  // 000000000010100s
    _0000000000101010_,  _0000000000101011_,  // 000000000010101s
    _0000000000101100_,  _0000000000101101_,  // 000000000010110s
    _0000000000101110_,  _0000000000101111_,  // 000000000010111s
    _0000000000110000_,  _0000000000110001_,  // 000000000011000s
    _0000000000110010_,  _0000000000110011_,  // 000000000011001s
    _0000000000110100_,  _0000000000110101_,  // 000000000011010s
    _0000000000110110_,  _0000000000110111_,  // 000000000011011s
    _0000000000111000_,  _0000000000111001_,  // 000000000011100s
    _0000000000111010_,  _0000000000111011_,  // 000000000011101s
    _0000000000111100_,  _0000000000111101_,  // 000000000011110s
    _0000000000111110_,  _0000000000111111_,  // 000000000011111s
    _00000000000100000,  _00000000000100001,  // 0000000000010000s
    _00000000000100010,  _00000000000100011,  // 0000000000010001s
    _00000000000100100,  _00000000000100101,  // 0000000000010010s
    _00000000000100110,  _00000000000100111,  // 0000000000010011s
    _00000000000101000,  _00000000000101001,  // 0000000000010100s
    _00000000000101010,  _00000000000101011,  // 0000000000010101s
    _00000000000101100,  _00000000000101101,  // 0000000000010110s
    _00000000000101110,  _00000000000101111,  // 0000000000010111s
    _00000000000110000,  _00000000000110001,  // 0000000000011000s
    _00000000000110010,  _00000000000110011,  // 0000000000011001s
    _00000000000110100,  _00000000000110101,  // 0000000000011010s
    _00000000000110110,  _00000000000110111,  // 0000000000011011s
    _00000000000111000,  _00000000000111001,  // 0000000000011100s
    _00000000000111010,  _00000000000111011,  // 0000000000011101s
    _00000000000111100,  _00000000000111101,  // 0000000000011110s
    _00000000000111110,  _00000000000111111,  // 0000000000011111s

    /** Usually "end of block" (EOB). */
    _10_______________,
    /** Usually escape code. */
    _000001___________;

    // #########################################################################

    private final String _sBitString;

    /** Bit length of this code (could use <code>_sBitString.length()</code>, but
     * this appears to be faster). */
    private final int _iBitLength;

    private BitStreamCode() {
        _sBitString = this.name().replaceAll("_", "");
        _iBitLength = _sBitString.length();
    }

    /** Returns the bits as a string. */
    public @Nonnull String getString() {
        return _sBitString;
    }

    public int getLength() {
        return _iBitLength;
    }

    // #########################################################################

    /** Longest AC variable-length (Huffman) bit code, in bits. */
    public static final int LONGEST_BITSTREAM_CODE_17BITS = 17;

    /** Pre-load all the values into an array to avoid creating a
     * new array for every call. */
    private static final BitStreamCode[] VALUES = values();

    static {
        assert VALUES.length == 111 * 2 + 2;
    }

    public static @Nonnull BitStreamCode get(int i) {
        return VALUES[i];
    }

    public static int getNormalCount() {
        return VALUES.length - 2;
    }

    public static int getTotalCount() {
        return VALUES.length;
    }

}
