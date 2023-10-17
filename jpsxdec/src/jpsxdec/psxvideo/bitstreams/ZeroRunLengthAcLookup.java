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

package jpsxdec.psxvideo.bitstreams;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecCode;
import static jpsxdec.psxvideo.bitstreams.BitStreamCode.*;

/** Creates ultra fast bit-stream lookup tables. */
public class ZeroRunLengthAcLookup implements Iterable<ZeroRunLengthAc> {

    public static class Builder {

        private final ZeroRunLengthAc[] _aoList = new ZeroRunLengthAc[BitStreamCode.getTotalCount()];

        private final TreeSet<MdecCode> _duplicateCodeChecker = new TreeSet<MdecCode>();

        /** To set the zero run-length and AC coefficient for a single bit stream code. */
        public @Nonnull Builder set(@Nonnull BitStreamCode bitStreamCode, int iZeroRunLength, int iAcCoefficient) {
            return add(new ZeroRunLengthAc(bitStreamCode, iZeroRunLength,  iAcCoefficient));
        }

        /** To set the zero run-length and AC coefficient for a single bit stream code. */
        public @Nonnull Builder add(@Nonnull ZeroRunLengthAc zrlac) {
            MdecCode mdecCode = zrlac.getMdecCodeCopy();
            if (mdecCode != null) {
                if (!_duplicateCodeChecker.add(mdecCode))
                    throw new RuntimeException("Already got MDEC code " + mdecCode);
            }
            int iIndex = zrlac.getBitStreamCode().ordinal();
            if (_aoList[iIndex] != null)
                throw new RuntimeException("Trying to replace " + _aoList[iIndex] + " with " + zrlac + " at " + iIndex);
            _aoList[iIndex] = zrlac;
            return this;
        }

        // To set the zero run-length and AC coefficient for the 2 codes covered with a sign bit.
        //
        public Builder _11s      (int zr, int aac) { return set(_110______________, _111______________, zr, aac); }
        // ------------------------------------------------------------------------------
        public Builder _011s     (int zr, int aac) { return set(_0110_____________, _0111_____________, zr, aac); }
        public Builder _0100s    (int zr, int aac) { return set(_01000____________, _01001____________, zr, aac); }
        public Builder _0101s    (int zr, int aac) { return set(_01010____________, _01011____________, zr, aac); }
        public Builder _00101s   (int zr, int aac) { return set(_001010___________, _001011___________, zr, aac); }
        public Builder _00110s   (int zr, int aac) { return set(_001100___________, _001101___________, zr, aac); }
        public Builder _00111s   (int zr, int aac) { return set(_001110___________, _001111___________, zr, aac); }
        public Builder _000100s  (int zr, int aac) { return set(_0001000__________, _0001001__________, zr, aac); }
        public Builder _000101s  (int zr, int aac) { return set(_0001010__________, _0001011__________, zr, aac); }
        public Builder _000110s  (int zr, int aac) { return set(_0001100__________, _0001101__________, zr, aac); }
        public Builder _000111s  (int zr, int aac) { return set(_0001110__________, _0001111__________, zr, aac); }
        public Builder _0000100s (int zr, int aac) { return set(_00001000_________, _00001001_________, zr, aac); }
        public Builder _0000101s (int zr, int aac) { return set(_00001010_________, _00001011_________, zr, aac); }
        public Builder _0000110s (int zr, int aac) { return set(_00001100_________, _00001101_________, zr, aac); }
        public Builder _0000111s (int zr, int aac) { return set(_00001110_________, _00001111_________, zr, aac); }
        public Builder _00100000s(int zr, int aac) { return set(_001000000________, _001000001________, zr, aac); }
        public Builder _00100001s(int zr, int aac) { return set(_001000010________, _001000011________, zr, aac); }
        public Builder _00100010s(int zr, int aac) { return set(_001000100________, _001000101________, zr, aac); }
        public Builder _00100011s(int zr, int aac) { return set(_001000110________, _001000111________, zr, aac); }
        public Builder _00100100s(int zr, int aac) { return set(_001001000________, _001001001________, zr, aac); }
        public Builder _00100101s(int zr, int aac) { return set(_001001010________, _001001011________, zr, aac); }
        public Builder _00100110s(int zr, int aac) { return set(_001001100________, _001001101________, zr, aac); }
        public Builder _00100111s(int zr, int aac) { return set(_001001110________, _001001111________, zr, aac); }
        // ------------------------------------------------------------------------------
        public Builder _0000001000s(int zr, int aac) { return set(_00000010000______, _00000010001______, zr, aac); }
        public Builder _0000001001s(int zr, int aac) { return set(_00000010010______, _00000010011______, zr, aac); }
        public Builder _0000001010s(int zr, int aac) { return set(_00000010100______, _00000010101______, zr, aac); }
        public Builder _0000001011s(int zr, int aac) { return set(_00000010110______, _00000010111______, zr, aac); }
        public Builder _0000001100s(int zr, int aac) { return set(_00000011000______, _00000011001______, zr, aac); }
        public Builder _0000001101s(int zr, int aac) { return set(_00000011010______, _00000011011______, zr, aac); }
        public Builder _0000001110s(int zr, int aac) { return set(_00000011100______, _00000011101______, zr, aac); }
        public Builder _0000001111s(int zr, int aac) { return set(_00000011110______, _00000011111______, zr, aac); }
        public Builder _000000010000s(int zr, int aac) { return set(_0000000100000____, _0000000100001____, zr, aac); }
        public Builder _000000010001s(int zr, int aac) { return set(_0000000100010____, _0000000100011____, zr, aac); }
        public Builder _000000010010s(int zr, int aac) { return set(_0000000100100____, _0000000100101____, zr, aac); }
        public Builder _000000010011s(int zr, int aac) { return set(_0000000100110____, _0000000100111____, zr, aac); }
        public Builder _000000010100s(int zr, int aac) { return set(_0000000101000____, _0000000101001____, zr, aac); }
        public Builder _000000010101s(int zr, int aac) { return set(_0000000101010____, _0000000101011____, zr, aac); }
        public Builder _000000010110s(int zr, int aac) { return set(_0000000101100____, _0000000101101____, zr, aac); }
        public Builder _000000010111s(int zr, int aac) { return set(_0000000101110____, _0000000101111____, zr, aac); }
        public Builder _000000011000s(int zr, int aac) { return set(_0000000110000____, _0000000110001____, zr, aac); }
        public Builder _000000011001s(int zr, int aac) { return set(_0000000110010____, _0000000110011____, zr, aac); }
        public Builder _000000011010s(int zr, int aac) { return set(_0000000110100____, _0000000110101____, zr, aac); }
        public Builder _000000011011s(int zr, int aac) { return set(_0000000110110____, _0000000110111____, zr, aac); }
        public Builder _000000011100s(int zr, int aac) { return set(_0000000111000____, _0000000111001____, zr, aac); }
        public Builder _000000011101s(int zr, int aac) { return set(_0000000111010____, _0000000111011____, zr, aac); }
        public Builder _000000011110s(int zr, int aac) { return set(_0000000111100____, _0000000111101____, zr, aac); }
        public Builder _000000011111s(int zr, int aac) { return set(_0000000111110____, _0000000111111____, zr, aac); }
        public Builder _0000000010000s(int zr, int aac) { return set(_00000000100000___, _00000000100001___, zr, aac); }
        public Builder _0000000010001s(int zr, int aac) { return set(_00000000100010___, _00000000100011___, zr, aac); }
        public Builder _0000000010010s(int zr, int aac) { return set(_00000000100100___, _00000000100101___, zr, aac); }
        public Builder _0000000010011s(int zr, int aac) { return set(_00000000100110___, _00000000100111___, zr, aac); }
        public Builder _0000000010100s(int zr, int aac) { return set(_00000000101000___, _00000000101001___, zr, aac); }
        public Builder _0000000010101s(int zr, int aac) { return set(_00000000101010___, _00000000101011___, zr, aac); }
        public Builder _0000000010110s(int zr, int aac) { return set(_00000000101100___, _00000000101101___, zr, aac); }
        public Builder _0000000010111s(int zr, int aac) { return set(_00000000101110___, _00000000101111___, zr, aac); }
        public Builder _0000000011000s(int zr, int aac) { return set(_00000000110000___, _00000000110001___, zr, aac); }
        public Builder _0000000011001s(int zr, int aac) { return set(_00000000110010___, _00000000110011___, zr, aac); }
        public Builder _0000000011010s(int zr, int aac) { return set(_00000000110100___, _00000000110101___, zr, aac); }
        public Builder _0000000011011s(int zr, int aac) { return set(_00000000110110___, _00000000110111___, zr, aac); }
        public Builder _0000000011100s(int zr, int aac) { return set(_00000000111000___, _00000000111001___, zr, aac); }
        public Builder _0000000011101s(int zr, int aac) { return set(_00000000111010___, _00000000111011___, zr, aac); }
        public Builder _0000000011110s(int zr, int aac) { return set(_00000000111100___, _00000000111101___, zr, aac); }
        public Builder _0000000011111s(int zr, int aac) { return set(_00000000111110___, _00000000111111___, zr, aac); }
        // ------------------------------------------------------------------------------
        public Builder _00000000010000s(int zr, int aac) { return set(_000000000100000__, _000000000100001__, zr, aac); }
        public Builder _00000000010001s(int zr, int aac) { return set(_000000000100010__, _000000000100011__, zr, aac); }
        public Builder _00000000010010s(int zr, int aac) { return set(_000000000100100__, _000000000100101__, zr, aac); }
        public Builder _00000000010011s(int zr, int aac) { return set(_000000000100110__, _000000000100111__, zr, aac); }
        public Builder _00000000010100s(int zr, int aac) { return set(_000000000101000__, _000000000101001__, zr, aac); }
        public Builder _00000000010101s(int zr, int aac) { return set(_000000000101010__, _000000000101011__, zr, aac); }
        public Builder _00000000010110s(int zr, int aac) { return set(_000000000101100__, _000000000101101__, zr, aac); }
        public Builder _00000000010111s(int zr, int aac) { return set(_000000000101110__, _000000000101111__, zr, aac); }
        public Builder _00000000011000s(int zr, int aac) { return set(_000000000110000__, _000000000110001__, zr, aac); }
        public Builder _00000000011001s(int zr, int aac) { return set(_000000000110010__, _000000000110011__, zr, aac); }
        public Builder _00000000011010s(int zr, int aac) { return set(_000000000110100__, _000000000110101__, zr, aac); }
        public Builder _00000000011011s(int zr, int aac) { return set(_000000000110110__, _000000000110111__, zr, aac); }
        public Builder _00000000011100s(int zr, int aac) { return set(_000000000111000__, _000000000111001__, zr, aac); }
        public Builder _00000000011101s(int zr, int aac) { return set(_000000000111010__, _000000000111011__, zr, aac); }
        public Builder _00000000011110s(int zr, int aac) { return set(_000000000111100__, _000000000111101__, zr, aac); }
        public Builder _00000000011111s(int zr, int aac) { return set(_000000000111110__, _000000000111111__, zr, aac); }
        public Builder _000000000010000s(int zr, int aac) { return set(_0000000000100000_, _0000000000100001_, zr, aac); }
        public Builder _000000000010001s(int zr, int aac) { return set(_0000000000100010_, _0000000000100011_, zr, aac); }
        public Builder _000000000010010s(int zr, int aac) { return set(_0000000000100100_, _0000000000100101_, zr, aac); }
        public Builder _000000000010011s(int zr, int aac) { return set(_0000000000100110_, _0000000000100111_, zr, aac); }
        public Builder _000000000010100s(int zr, int aac) { return set(_0000000000101000_, _0000000000101001_, zr, aac); }
        public Builder _000000000010101s(int zr, int aac) { return set(_0000000000101010_, _0000000000101011_, zr, aac); }
        public Builder _000000000010110s(int zr, int aac) { return set(_0000000000101100_, _0000000000101101_, zr, aac); }
        public Builder _000000000010111s(int zr, int aac) { return set(_0000000000101110_, _0000000000101111_, zr, aac); }
        public Builder _000000000011000s(int zr, int aac) { return set(_0000000000110000_, _0000000000110001_, zr, aac); }
        public Builder _000000000011001s(int zr, int aac) { return set(_0000000000110010_, _0000000000110011_, zr, aac); }
        public Builder _000000000011010s(int zr, int aac) { return set(_0000000000110100_, _0000000000110101_, zr, aac); }
        public Builder _000000000011011s(int zr, int aac) { return set(_0000000000110110_, _0000000000110111_, zr, aac); }
        public Builder _000000000011100s(int zr, int aac) { return set(_0000000000111000_, _0000000000111001_, zr, aac); }
        public Builder _000000000011101s(int zr, int aac) { return set(_0000000000111010_, _0000000000111011_, zr, aac); }
        public Builder _000000000011110s(int zr, int aac) { return set(_0000000000111100_, _0000000000111101_, zr, aac); }
        public Builder _000000000011111s(int zr, int aac) { return set(_0000000000111110_, _0000000000111111_, zr, aac); }
        public Builder _0000000000010000s(int zr, int aac) { return set(_00000000000100000, _00000000000100001, zr, aac); }
        public Builder _0000000000010001s(int zr, int aac) { return set(_00000000000100010, _00000000000100011, zr, aac); }
        public Builder _0000000000010010s(int zr, int aac) { return set(_00000000000100100, _00000000000100101, zr, aac); }
        public Builder _0000000000010011s(int zr, int aac) { return set(_00000000000100110, _00000000000100111, zr, aac); }
        public Builder _0000000000010100s(int zr, int aac) { return set(_00000000000101000, _00000000000101001, zr, aac); }
        public Builder _0000000000010101s(int zr, int aac) { return set(_00000000000101010, _00000000000101011, zr, aac); }
        public Builder _0000000000010110s(int zr, int aac) { return set(_00000000000101100, _00000000000101101, zr, aac); }
        public Builder _0000000000010111s(int zr, int aac) { return set(_00000000000101110, _00000000000101111, zr, aac); }
        public Builder _0000000000011000s(int zr, int aac) { return set(_00000000000110000, _00000000000110001, zr, aac); }
        public Builder _0000000000011001s(int zr, int aac) { return set(_00000000000110010, _00000000000110011, zr, aac); }
        public Builder _0000000000011010s(int zr, int aac) { return set(_00000000000110100, _00000000000110101, zr, aac); }
        public Builder _0000000000011011s(int zr, int aac) { return set(_00000000000110110, _00000000000110111, zr, aac); }
        public Builder _0000000000011100s(int zr, int aac) { return set(_00000000000111000, _00000000000111001, zr, aac); }
        public Builder _0000000000011101s(int zr, int aac) { return set(_00000000000111010, _00000000000111011, zr, aac); }
        public Builder _0000000000011110s(int zr, int aac) { return set(_00000000000111100, _00000000000111101, zr, aac); }
        public Builder _0000000000011111s(int zr, int aac) { return set(_00000000000111110, _00000000000111111, zr, aac); }

        private @Nonnull Builder set(@Nonnull BitStreamCode positiveBitStreamCodeEndsWith0,
                                     @Nonnull BitStreamCode negativeBitStreamCodeEndsWith1,
                                     int iZeroRunLength, int iAbsoluteAcCoefficient)
        {
            set(positiveBitStreamCodeEndsWith0, iZeroRunLength,  iAbsoluteAcCoefficient);
            set(negativeBitStreamCodeEndsWith1, iZeroRunLength,  -iAbsoluteAcCoefficient);
            return this;
        }

        public @Nonnull ZeroRunLengthAcLookup build() {
            return new ZeroRunLengthAcLookup(_aoList);
        }

    }

    @Nonnull
    private final ZeroRunLengthAc[] _aoList;

    /** Table to look up '10' and '11s' codes using all but the first (1) bit. */
    private final ZeroRunLengthAc[] _aoTable_1xx = new ZeroRunLengthAc[4];
    /** Table to look up codes '011s' to '00100111s' using all but the first (0) bit.
     * Given a bit code in that range, strip the leading zero bit, then pad
     * any extra trailing bits to make an 8 bit value. Use that value as the
     * index in this table to get the corresponding code. */
    private final ZeroRunLengthAc[] _aoTable_0xxxxxxx = new ZeroRunLengthAc[256];
    /** Table to look up codes '0000001000s' to '0000000011111s' using all but
     *  the first 6 zero bits.
     * Given a bit code in that range, strip the leading 6 zero bits, then pad
     * any extra trailing bits to make an 8 bit value. Use that value as the
     * index in this table to get the corresponding code. */
    private final ZeroRunLengthAc[] _aoTable_000000xxxxxxxx = new ZeroRunLengthAc[256];
    /** Table to look up codes '00000000010000s' to '0000000000011111s' using
     *  all but the first 9 zero bits.
     * Given a bit code in that range, strip the leading 9 zero bits, then pad
     * any extra trailing bits to make an 8 bit value. Use that value as the
     * index in this table to get the corresponding code. */
    private final ZeroRunLengthAc[] _aoTable_000000000xxxxxxxx = new ZeroRunLengthAc[256];

    private ZeroRunLengthAcLookup(@Nonnull ZeroRunLengthAc[] aoList) {
        _aoList = aoList;
        for (int i = 0; i < aoList.length; i++) {
            ZeroRunLengthAc zrlac = aoList[i];
            BitStreamCode bitStreamCode = BitStreamCode.get(i);
            if (zrlac == null)
                throw new IllegalStateException("Table incomplete: missing " + bitStreamCode);
            setBits(bitStreamCode, zrlac);
        }
    }

    /** Identifies the lookup table in which to place the bit code. */
    private void setBits(@Nonnull BitStreamCode bsc, @Nonnull ZeroRunLengthAc zrlac) {
        final int iBitsRemain;
        final ZeroRunLengthAc[] aoTable;
        final int iTableStart;

        // This needs some explanation
        if        (bsc.getString().startsWith("000000000")) {
            aoTable =                 _aoTable_000000000xxxxxxxx;
            iBitsRemain = 8 - (bsc.getLength() - 9);
            iTableStart = Integer.parseInt(bsc.getString(), 2) << iBitsRemain;
        } else if (bsc.getString().startsWith("000000"   )) {
            aoTable =                 _aoTable_000000xxxxxxxx;
            iBitsRemain = 8 - (bsc.getLength() - 6);
            iTableStart = Integer.parseInt(bsc.getString(), 2) << iBitsRemain;
        } else if (bsc.getString().startsWith("0"        )) {
            aoTable =                 _aoTable_0xxxxxxx;
            iBitsRemain = 8 - (bsc.getLength() - 1);
            iTableStart = Integer.parseInt(bsc.getString(), 2) << iBitsRemain;
        } else { //                startsWith("1")
            aoTable =                 _aoTable_1xx;
            iBitsRemain = 2 - (bsc.getLength() - 1);
            iTableStart = Integer.parseInt(bsc.getString().substring(1), 2) << iBitsRemain;
        }

        final int iTableEntriesToAssociate = (1 << iBitsRemain);
        for (int i = 0; i < iTableEntriesToAssociate; i++) {
            if (aoTable[iTableStart + i] != null)
                throw new RuntimeException("Trying to replace " + aoTable[iTableStart + i] +
                                           " with " + zrlac);
            aoTable[iTableStart + i] = zrlac;
        }
    }

    // #########################################################################

    @Override
    public @Nonnull Iterator<ZeroRunLengthAc> iterator() {
        return new Iterator<ZeroRunLengthAc>() {
            private int _i = 0;
            @Override
            public boolean hasNext() {
                return _i < _aoList.length;
            }

            @Override
            public @Nonnull ZeroRunLengthAc next() throws NoSuchElementException {
                if (!hasNext())
                    throw new NoSuchElementException();
                return _aoList[_i++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("List is immutable");
            }
        };
    }

    /** Useful bitmasks. */
    private static final int
        b11000000000000000 = 0x18000,
        b10000000000000000 = 0x10000,
        b01000000000000000 = 0x08000,
        b00100000000000000 = 0x04000,
        b01111100000000000 = 0x0F800,
        b00000011100000000 = 0x00700,
        b00000000011100000 = 0x000E0;

    /** Converts bits to the equivalent {@link BitstreamToMdecLookup}.
     * 17 bits need to be supplied to decode (the longest bit code).
     * Bits should start at the least-significant bit.
     * Bits beyond the 17 least-significant bits are ignored.
     * If a full 17 bits are unavailable, fill the remaining with zeros
     * to ensure failure if bit code is invalid.
     *
     * @param i17bits  Integer containing 17 bits to decode.
     */
    public @CheckForNull ZeroRunLengthAc lookup(final int i17bits) {
        if        ((i17bits & b10000000000000000) != 0) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("Table 0 offset " + ((i17bits >> 14) & 3));
            return    _aoTable_1xx[(i17bits >> 14) & 3];
        } else if ((i17bits & b01111100000000000) != 0) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("Table 1 offset " + ((i17bits >> 8) & 0xff));
            return    _aoTable_0xxxxxxx[(i17bits >> 8) & 0xff];
        } else if ((i17bits & b00000011100000000) != 0) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("Table 2 offset " + ((i17bits >> 3) & 0xff));
            return    _aoTable_000000xxxxxxxx[(i17bits >> 3) & 0xff];
        } else if ((i17bits & b00000000011100000) != 0) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("Table 3 offset " + (i17bits & 0xff));
            return    _aoTable_000000000xxxxxxxx[i17bits & 0xff];
        } else {
            return null;
        }
    }

    /** Slow iterative search for a matching variable length code.
     * @return null if no match */
    public @CheckForNull ZeroRunLengthAc lookup(@Nonnull MdecCode mdecCode) {
        if (!mdecCode.isValid())
            throw new IllegalArgumentException("Invalid MDEC code " + mdecCode);

        for (ZeroRunLengthAc vlc : _aoList) {
            if (vlc.equalsMdec(mdecCode))
                return vlc;
        }

        return null;
    }

    // ..................................................

    /** Alternative lookup method that is slower that {@link #lookup(int)}. */
    public @CheckForNull ZeroRunLengthAc lookup_slow1(final int i17bits) {
        if        ((i17bits & b11000000000000000) == b10000000000000000) {
            return _aoList[_10_______________.ordinal()];
        } else if ((i17bits & b11000000000000000) == b11000000000000000) {
            // special handling for first AC code
            if ((i17bits & b00100000000000000) == 0) // check sign bit
                return  _aoList[_110______________.ordinal()];
            else
                return     _aoList[_111______________.ordinal()];
        } else {
            // escape code is already set in the lookup table
            final ZeroRunLengthAc[] array;
            final int c;
            if        ((i17bits & b01111100000000000) != 0) {
                array =      _aoTable_0xxxxxxx;
                c = (i17bits >> 8) & 0xff;
            } else if ((i17bits & b00000011100000000) != 0) {
                array =      _aoTable_000000xxxxxxxx;
                c = (i17bits >> 3) & 0xff;
            } else if ((i17bits & b00000000011100000) != 0) {
                array =      _aoTable_000000000xxxxxxxx;
                c = i17bits & 0xff;
            } else {
                return null;
            }
            return array[c];
        }
    }

    /** Alternative lookup method that is slower that {@link #lookup(int)}. */
    public @CheckForNull ZeroRunLengthAc lookup_slow2(final int i17bits) {
        if ((i17bits &        b11000000000000000) == b10000000000000000) {
            return _aoList[_10_______________.ordinal()];
        } else if ((i17bits & b11000000000000000) == b11000000000000000) {
            // special handling for first AC code
            if ((i17bits &    b00100000000000000) == 0) // check sign bit
                return        _aoList[_110______________.ordinal()];
            else
                return        _aoList[_111______________.ordinal()];
        } else {
            // escape code is already part of the lookup table
            if        ((i17bits & b01111100000000000) != 0) {
                return       _aoTable_0xxxxxxx[(i17bits >> 8) & 0xff];
            } else if ((i17bits & b00000011100000000) != 0) {
                return       _aoTable_000000xxxxxxxx[(i17bits >> 3) & 0xff];
            } else if ((i17bits & b00000000011100000) != 0) {
                return       _aoTable_000000000xxxxxxxx[i17bits & 0xff];
            } else {
                return null;
            }
        }
    }

    /** Alternative lookup method that is slower that {@link #lookup(int)}. */
    public @CheckForNull ZeroRunLengthAc lookup_slow3(final int i17bits) {
        if ((i17bits & b10000000000000000) != 0) {       // 1st bit is 1?
            if ((i17bits & b01000000000000000) == 0) {   // initial bits == '10'?
                return _aoList[_10_______________.ordinal()];
            } else {                                     // initial bits == '11'
                // special handling for first AC code
                // check sign bit
                if ((i17bits & b00100000000000000) == 0) // initial bits == '110'?
                    return     _aoList[_110______________.ordinal()];
                else                                     // initial bits == '111'
                    return     _aoList[_111______________.ordinal()];
            }
        } else {                                         // 1st bit is 0
            // escape code is already set in the lookup table
            final ZeroRunLengthAc[] array;
            final int c;
            if        ((i17bits & b01111100000000000) != 0) {
                array =      _aoTable_0xxxxxxx;
                c = (i17bits >> 8) & 0xff;
            } else if ((i17bits & b00000011100000000) != 0) {
                array =      _aoTable_000000xxxxxxxx;
                c = (i17bits >> 3) & 0xff;
            } else if ((i17bits & b00000000011100000) != 0) {
                array =      _aoTable_000000000xxxxxxxx;
                c = i17bits & 0xff;
            } else {
                return null;
            }
            return array[c];
        }
    }

    /** Bit masks for
     * {@link #lookup_old(int)} */
    private static final int
            b1000000000000000_ = 0x8000 << 1,
            b0100000000000000_ = 0x4000 << 1,
            b0010000000000000_ = 0x2000 << 1,
            b0001100000000000_ = 0x1800 << 1,
            b0001000000000000_ = 0x1000 << 1,
            b0000100000000000_ = 0x0800 << 1,
            b0000010000000000_ = 0x0400 << 1,
            b0000001000000000_ = 0x0200 << 1,
            b0000000100000000_ = 0x0100 << 1,
            b0000000010000000_ = 0x0080 << 1,
            b0000000001000000_ = 0x0040 << 1,
            b0000000000100000_ = 0x0020 << 1,
            b0000000000010000_ = 0x0010 << 1;

    /** This lookup approach was used in 0.96.0 and earlier. */
    public @CheckForNull ZeroRunLengthAc lookup_old(final int i17bits) {
        final BitStreamCode vlc;

        // Walk through the bits, one-by-one
        // Fun fact: The Lain Playstation game uses this same decoding approach
        if (    (i17bits & b1000000000000000_) != 0) {        // "1"
            if ((i17bits & b0100000000000000_) != 0) {        // "11"
                vlc = BitStreamCode.get(0);
            } else {                                          // "10"
                // End of block
                return _aoList[_10_______________.ordinal()];
            }
        } else if ((i17bits & b0100000000000000_) != 0) {     // "01"
            if    ((i17bits & b0010000000000000_) != 0) {     // "011"
                vlc = BitStreamCode.get(1);
            } else {                                          // "010x"
                vlc = BitStreamCode.get(2 + (int)((i17bits >>> 13) & 1));
            }
        } else if ((i17bits & b0010000000000000_) != 0) {      // "001"
            if    ((i17bits & b0001100000000000_) != 0)  {     // "001xx"
                vlc = BitStreamCode.get(3 + (int)((i17bits >>> 12) & 3));
            } else {                                           // "00100xxx"
                vlc = BitStreamCode.get(15 + (int)((i17bits >>> 9) & 7));
            }
        } else if ((i17bits & b0001000000000000_) != 0) {      // "0001xx"
            vlc = BitStreamCode.get(7 + (int)((i17bits >>> 11) & 3));
        } else if ((i17bits & b0000100000000000_) != 0) {      // "00001xx"
            vlc = BitStreamCode.get(11 + (int)((i17bits >>> 10) & 3));
        } else if ((i17bits & b0000010000000000_) != 0) {      // "000001"
            // escape code
            return _aoList[_000001___________.ordinal()];
        } else if ((i17bits & b0000001000000000_) != 0) {      // "0000001xxx"
            vlc = BitStreamCode.get(23 + (int)((i17bits >>> 7) & 7));
        } else if ((i17bits & b0000000100000000_) != 0) {      // "00000001xxxx"
            vlc = BitStreamCode.get(31 + (int)((i17bits >>> 5) & 15));
        } else if ((i17bits & b0000000010000000_) != 0) {      // "000000001xxxx"
            vlc = BitStreamCode.get(47 + (int)((i17bits >>> 4) & 15));
        } else if ((i17bits & b0000000001000000_) != 0) {      // "0000000001xxxx"
            vlc = BitStreamCode.get(63 + (int)((i17bits >>> 3) & 15));
        } else if ((i17bits & b0000000000100000_) != 0) {      // "00000000001xxxx"
            vlc = BitStreamCode.get(79 + (int)((i17bits >>> 2) & 15));
        } else if ((i17bits & b0000000000010000_) != 0) {      // "000000000001xxxx"
            vlc = BitStreamCode.get(95 + (int)((i17bits >>> 1) & 15));
        } else {
            return null;
        }

        // Take either the positive (0) or negative (1) AC coefficient,
        // depending on the sign bit
        if ((i17bits & (1 << (16 - vlc.getLength()))) == 0) {
            // positive
            return _aoList[vlc.ordinal()];
        } else {
            // negative
            return _aoList[vlc.ordinal() + 1];
        }
    }

    public void printAllLookupTables(@Nonnull PrintStream ps) {
        for (int i = 0; i < _aoTable_1xx.length; i++) {
            ps.println("Table_1xx["+i+"] = "+_aoTable_1xx[i]);
        }
        for (int i = 0; i < _aoTable_0xxxxxxx.length; i++) {
            ps.println("Table_0xxxxxxx["+i+"] = "+_aoTable_0xxxxxxx[i]);
        }
        for (int i = 0; i < _aoTable_000000xxxxxxxx.length; i++) {
            ps.println("Table_000000xxxxxxxx["+i+"] = "+_aoTable_000000xxxxxxxx[i]);
        }
        for (int i = 0; i < _aoTable_000000000xxxxxxxx.length; i++) {
            ps.println("Table_000000000xxxxxxxx["+i+"] = "+_aoTable_000000000xxxxxxxx[i]);
        }
    }
}
