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
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/** The "instruction" table, as I call it, indicates how many codes to read
 * from each of the three zero-run length AC tables for the current block.
 * What even... */
public class InstructionTable {

    private final int _iMaxBitLen;
    @Nonnull
    private final InstructionCode[] _table;

    private final int _iBitMask;
    @Nonnull
    private final InstructionCode[] _lookupTable;

    public InstructionTable(int iMaxBitLen, @Nonnull InstructionCode[] table) {
        _iMaxBitLen = iMaxBitLen;
        _table = table;

        int iLookupTableSize = 1 << _iMaxBitLen;
        _iBitMask = iLookupTableSize - 1;
        _lookupTable = new InstructionCode[iLookupTableSize];
        TableLookup.buildLookup(_iBitMask, table, _lookupTable);
    }

    @Nonnull InstructionCode[] getTable() {
        return _table;
    }

    public @Nonnull InstructionCode lookupInstruction(int iNext32Bits) throws MdecException.ReadCorruption {
        InstructionCode code = _lookupTable[iNext32Bits & _iBitMask];
        if (code == null)
            throw new MdecException.ReadCorruption("Aconcagua lookup table bits " + Misc.bitsToString(iNext32Bits, _iMaxBitLen));
        return code;
    }

    public static class InstructionCode implements TableLookup.IBits {
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

        public int getTotalCount() {
            return _iTable1Count + _iTable2Count + _iTable3Count;
        }

        public int getBitCodeLen() {
            return _sBits.length();
        }

        @Override
        public @Nonnull String getBits() {
            return _sBits;
        }

        @Override
        public String toString() {
            return String.format("'%s' (%2d, %2d, %2d)",
                                 _sBits, _iTable1Count, _iTable2Count, _iTable3Count);
        }

    }

}
