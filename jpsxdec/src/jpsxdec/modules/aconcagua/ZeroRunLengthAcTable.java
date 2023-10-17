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

package jpsxdec.modules.aconcagua;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.BitStreamDebugging;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/*
 * Many thanks to Martin Korth (no$psx developer) for sharing his knowledge of
 * the Aconcagua video format!
 */
public class ZeroRunLengthAcTable {

    @Nonnull
    private final EscapeCode _escapeCode1;
    @Nonnull
    private final EscapeCode _escapeCode2;
    private final int _iMaxBitLen;
    @Nonnull
    private final AcCode[] _table;

    private final int _iBitMask;
    @Nonnull
    protected final AcCode[] _lookupTable;

    public ZeroRunLengthAcTable(@Nonnull String sEscapeCode1,
                                @Nonnull String sEscapeCode2,
                                int iMaxBitLen, @Nonnull AcCode[] table)
    {
        _escapeCode1 = new EscapeCode(sEscapeCode1);
        _escapeCode2 = new EscapeCode(sEscapeCode2);
        _iMaxBitLen = iMaxBitLen;
        _table = table;

        int iLookupTableSize = 1 << _iMaxBitLen;
        _iBitMask = iLookupTableSize - 1;
        _lookupTable = new AcCode[iLookupTableSize];
        TableLookup.buildLookup(_iBitMask, table, _lookupTable);
    }

    private static final int BOTTOM_10_BITS = 0x03ff;

    // Each AC table is read a little differently. This class will hold all the AC
    // codes for any table, but allows them to be read as if they were for each table type.
    // How the heck did they come up with this stuff?

    public int readAsTable1(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        if (_escapeCode1.matches(iNext32Bits)) {
            // (NOTE this code path is never actually hit in either of the videos)
            // 0 zero-run length, 7 bit signed AC
            int iSkipEscape = iNext32Bits >> _escapeCode1.getBitLen();
            int iAcSigned = signExtend(iSkipEscape, 7);
            code.set(iAcSigned & BOTTOM_10_BITS);
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("AC escape1 %s + (table 1) 7 bits %s", _escapeCode1, Misc.bitsToString(iSkipEscape, 7)));
            return _escapeCode1.getBitLen() + 7;
        } else {
            return readEscapeCode2OrTableLookup(code, iNext32Bits);
        }
    }

    public int readAsTable2(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        if (_escapeCode1.matches(iNext32Bits)) {
            // 3 bit zero-run length, 5 bit signed AC
            int iSkipEscape = iNext32Bits >> _escapeCode1.getBitLen();
            int iZeroRunLength = (iSkipEscape >> 5) & 7;
            int iAc = iSkipEscape & 0x1f;
            int iSignExtendAc = signExtend(iAc, 5);
            code.set((iZeroRunLength << 10) | (iSignExtendAc & BOTTOM_10_BITS));
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("AC escape1 %s + (table 2) 3,5 bits %s", _escapeCode1, Misc.bitsToString(iSkipEscape, 8)));
            return _escapeCode1.getBitLen() + 8;
        } else {
            return readEscapeCode2OrTableLookup(code, iNext32Bits);
        }
    }

    public int readAsTable3(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        if (_escapeCode1.matches(iNext32Bits)) {
            // 4 bit zero-run length, 4 bit signed AC
            int iSkipEscape = iNext32Bits >> _escapeCode1.getBitLen();
            int iZeroRunLength = (iSkipEscape >> 4) & 0xf;
            int iAc = iSkipEscape & 0xf;
            int iSignExtendAc = signExtend(iAc, 4);
            code.set((iZeroRunLength << 10) | (iSignExtendAc & BOTTOM_10_BITS));
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("AC escape1 %s + (table 3) 4,4 bits %s", _escapeCode1, Misc.bitsToString(iSkipEscape, 8)));
            return _escapeCode1.getBitLen() + 8;
        } else {
            return readEscapeCode2OrTableLookup(code, iNext32Bits);
        }
    }

    /** The only difference between the tables is how escape code 1 is read.
     * Escape code 2 and table lookup behavior is all the same. */
    private int readEscapeCode2OrTableLookup(@Nonnull MdecCode code, int iNext32Bits)
            throws MdecException.ReadCorruption
    {
        if (_escapeCode2.matches(iNext32Bits)) {
            // Full 16 bit MDEC code
            int iMdecCode = iNext32Bits >> _escapeCode2.getBitLen();

            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("AC escape2 %s + 16 mdec bits %s", _escapeCode2, Misc.bitsToString(iMdecCode, 16)));

            code.set(iMdecCode & 0xffff);
            return _escapeCode2.getBitLen() + 16;
        }

        AcCode ac = _lookupTable[iNext32Bits & _iBitMask];
        if (ac == null)
            throw new MdecException.ReadCorruption(Misc.bitsToString(iNext32Bits, _iMaxBitLen));

        assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("AC %s", ac));
        ac.applyToMdec(code);
        return ac.getBitLength();
    }

    private static int signExtend(int iValue, int iNumberOfBits) {
        return (iValue << (32 - iNumberOfBits)) >> (32 - iNumberOfBits);
    }

    public static class AcCode implements TableLookup.IBits {
        @Nonnull
        private final String _sBits;
        @Nonnull
        protected final MdecCode _code;

        protected AcCode(@Nonnull String sBits, @Nonnull MdecCode code) {
            _sBits = sBits;
            _code = code;
        }

        public @Nonnull MdecCode getMdecCodeCopy() {
            return _code.copy();
        }

        public void applyToMdec(@Nonnull MdecCode code) {
            code.setFrom(_code);
        }

        @Override
        public @Nonnull String getBits() {
            return _sBits;
        }

        public int getBitLength() {
            return _sBits.length();
        }

        @Override
        public String toString() {
            return String.format("'%s' %s", _sBits, _code);
        }
    }

    // ...................................................................................
    // Encoding

    public @Nonnull String[] encodeAsTable(@Nonnull MdecCode code, int iTable) {
        for (AcCode acCode : _table) {
            if (acCode._code.equals(code)) {
                if (BitStreamCompressor_Aconcagua.DEBUG) System.out.println(code + " " + acCode);
                return new String[] {acCode.getBits()};
            }
        }

        // need escape code
        String[] sEncodedBits;

        if (iTable == 1) {

            sEncodedBits = encodeEscape1(_escapeCode1.getBits(), code, 0, 7);

        } else if (iTable == 2) {

            sEncodedBits = encodeEscape1(_escapeCode1.getBits(), code, 3, 5);

        } else if (iTable == 3) {

            sEncodedBits = encodeEscape1(_escapeCode1.getBits(), code, 4, 4);

        } else {
            throw new IllegalArgumentException();
        }


        if (sEncodedBits == null) {
            String sEscape = _escapeCode2.getBits();
            int i = code.toMdecShort();
            String sMdecBits = Misc.bitsToString(i, 16);
            if (BitStreamCompressor_Aconcagua.DEBUG) System.out.println("Escape2 " + _escapeCode2 + " " + sMdecBits);
            sEncodedBits = new String[] {sEscape, sMdecBits};
        }

        return sEncodedBits;
    }

    private @CheckForNull String[] encodeEscape1(@Nonnull String sEscapeCodeBits,
                                                 @Nonnull MdecCode code,
                                                 int iZeroRleBits,
                                                 int iAcSignedBits)
    {
        int iRleBitMask = (1 << iZeroRleBits) - 1;
        if ((code.getTop6Bits() & ~iRleBitMask) != 0) {
            return null;
        }

        int iAc = code.getBottom10Bits();
        iAc = signExtend(iAc, 10);

        int iUsedBits;
        if (iAc < 0) {
            iUsedBits = 32 - Integer.numberOfLeadingZeros(~iAc) + 1;
        } else {
            iUsedBits = 32 - Integer.numberOfLeadingZeros(iAc) + 1;
        }

        if (iUsedBits > iAcSignedBits) {
            return null;
        }

        String sRle = Misc.bitsToString(code.getTop6Bits(), iZeroRleBits);
        String sAc = Misc.bitsToString(code.getBottom10Bits(), iAcSignedBits);
        if (BitStreamCompressor_Aconcagua.DEBUG) System.out.println(code + " -> AC escape1 " + sEscapeCodeBits + " rle " + sRle + " ac " + sAc);
        return new String[] {sEscapeCodeBits, sRle + sAc};
    }

}
