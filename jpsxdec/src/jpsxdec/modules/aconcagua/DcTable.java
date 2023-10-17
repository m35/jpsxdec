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

import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.BitStreamDebugging;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/** Pretty standard variable length code bit reading mapped to DC codes. */
public class DcTable {

    @Nonnull
    private final EscapeCode _escapeCode;
    private final int _iMaxBitLen;
    @Nonnull
    private final DcCode[] _table;

    private final int _iBitMask;
    @Nonnull
    protected final DcCode[] _lookupTable;

    public DcTable(@Nonnull String sEscapeCode, int iMaxBitLen, @Nonnull DcCode[] table) {
        _escapeCode = new EscapeCode(sEscapeCode);
        _iMaxBitLen = iMaxBitLen;
        _table = table;

        int iLookupTableSize = 1 << _iMaxBitLen;
        _iBitMask = iLookupTableSize - 1;
        _lookupTable = new DcCode[iLookupTableSize];
        TableLookup.buildLookup(_iBitMask, table, _lookupTable);
    }

    public static class DcCode implements TableLookup.IBits {
        @Nonnull
        private final String _sBits;
        private final int _iRelativeDcCoefficient;

        protected DcCode(@Nonnull String sBits, int iRelativeDcCoefficient) {
            _sBits = sBits;
            _iRelativeDcCoefficient = iRelativeDcCoefficient;
        }

        @Override
        public @Nonnull String getBits() {
            return _sBits;
        }

        public int getBitCodeLen() {
            return _sBits.length();
        }

        public int getRelativeDcCoefficient() {
            return _iRelativeDcCoefficient;
        }

        @Override
        public String toString() {
            return String.format("'%s' relative DC %d", _sBits, _iRelativeDcCoefficient);
        }
    }

    private static final int BOTTOM_10_BITS = 0x03ff;

    /** Need to return these 2 values. This is the best way I could think to do it. */
    public static class DcRead {
        public int iDc;
        public int iBitLen;
    }

    public void readDc(@Nonnull DcRead dcRead, int iNext32Bits, int iPreviousDc) throws MdecException.ReadCorruption {
        if (_escapeCode.matches(iNext32Bits)) {
            int iSkipEscapeCode = iNext32Bits >> _escapeCode.getBitLen();
            dcRead.iDc = iSkipEscapeCode & BOTTOM_10_BITS; // DC code is always 10 bits long
            dcRead.iBitLen = 10 + _escapeCode.getBitLen();
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("DC escape code %s + DC %s", _escapeCode, Misc.bitsToString(dcRead.iDc, 10)));
        } else {
            int iLookupBits = iNext32Bits & _iBitMask;
            DcCode code = _lookupTable[iLookupBits];
            if (code == null)
                throw new MdecException.ReadCorruption(Misc.bitsToString(iNext32Bits, _iBitMask));
            dcRead.iDc = code.getRelativeDcCoefficient() + iPreviousDc;
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(String.format("DC code %s + prev DC %d = %d", code, iPreviousDc, dcRead.iDc));
            dcRead.iBitLen = code.getBitCodeLen();
        }
    }

    // ...................................................................................
    // Encoding

    public @Nonnull String[] encodeDc(int iDC, int iPreviousDc) {
        int iDiff = iDC - iPreviousDc;

        assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("DC "+iDC+" - prev DC "+iPreviousDc + " = " + iDiff);
        for (DcCode dcCode : _table) {
            if (dcCode.getRelativeDcCoefficient() == iDiff) {
                // found match in the table
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println(dcCode.toString());
                return new String[] {dcCode.getBits()};
            }
        }

        // no table match, use escape code
        String sEscape = _escapeCode.getBits();
        String sDc10bits = Misc.bitsToString(iDC, 10);
        assert !BitStreamDebugging.DEBUG || BitStreamDebugging.println("DCEscape " + _escapeCode + " " + sDc10bits);
        return new String[] {sEscape, sDc10bits};
    }


}
