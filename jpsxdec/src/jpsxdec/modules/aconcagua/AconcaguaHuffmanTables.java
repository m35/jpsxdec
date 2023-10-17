/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

/** Collection of all the tables used in either the opening or ending video. */
public class AconcaguaHuffmanTables {

    @Nonnull
    private final InstructionTable _instructionTable;
    @Nonnull
    final DcTable _dcTable;
    @Nonnull
    final ZeroRunLengthAcTable _acTable1;
    @Nonnull
    final ZeroRunLengthAcTable _acTable2;
    @Nonnull
    final ZeroRunLengthAcTable _acTable3;

    public AconcaguaHuffmanTables(@Nonnull InstructionTable instructionTable,
                                  @Nonnull DcTable dcTable,
                                  @Nonnull ZeroRunLengthAcTable acTable1,
                                  @Nonnull ZeroRunLengthAcTable acTable2,
                                  @Nonnull ZeroRunLengthAcTable acTable3)
    {
        _instructionTable = instructionTable;
        _dcTable = dcTable;
        _acTable1 = acTable1;
        _acTable2 = acTable2;
        _acTable3 = acTable3;
    }

    public void readDc(@Nonnull DcTable.DcRead dcRead, int iNext32Bits, int iPreviousDc) throws MdecException.ReadCorruption {
        _dcTable.readDc(dcRead, iNext32Bits, iPreviousDc);
    }

    public @Nonnull InstructionTable.InstructionCode lookupInstruction(int iNext32Bits) throws MdecException.ReadCorruption {
        return _instructionTable.lookupInstruction(iNext32Bits);
    }

    public int readAcTable1(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        return _acTable1.readAsTable1(code, iNext32Bits);
    }

    public int readAcTable2(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        return _acTable2.readAsTable2(code, iNext32Bits);
    }

    public int readAcTable3(@Nonnull MdecCode code, int iNext32Bits) throws MdecException.ReadCorruption {
        return _acTable3.readAsTable3(code, iNext32Bits);
    }

    // ...................................................................................
    // Encoding

    public @Nonnull InstructionTable.InstructionCode getInstructionForCodeCount(int iCodeCount) {
        InstructionTable.InstructionCode matchingCode = null;
        for (InstructionTable.InstructionCode instructionCode : _instructionTable.getTable()) {
            if (instructionCode.getTotalCount() == iCodeCount) {
                // There should only be 1 matching instruction for the given AC codes
                if (matchingCode != null) {
                    throw new RuntimeException();
                }
                matchingCode = instructionCode;
            }
        }

        if (matchingCode == null) {
            throw new RuntimeException();
        }

        return matchingCode;
    }

    public @Nonnull String[] encodeDc(int iDC, int iPreviousDc) {
        return _dcTable.encodeDc(iDC, iPreviousDc);
    }

    public @Nonnull String[] encodeAcAsTable(@Nonnull MdecCode code, int iTable) {
        switch (iTable) {
            case 1: return _acTable1.encodeAsTable(code, iTable);
            case 2: return _acTable2.encodeAsTable(code, iTable);
            case 3: return _acTable3.encodeAsTable(code, iTable);
            default: throw new IllegalArgumentException();
        }
    }

}
