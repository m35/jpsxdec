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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.CommonBitStreamCompressing;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;

/** Not a completely uniform implementation of an Aconcagua compressor, but still fully functional. */
public class BitStreamCompressor_Aconcagua implements BitStreamCompressor {

    public static boolean DEBUG = false;

    private final int _iMacroBlockCount;

    @Nonnull
    private final AconcaguaHuffmanTables _tables;

    private final int _iQuantizationScale;

    public BitStreamCompressor_Aconcagua(@Nonnull AconcaguaHuffmanTables tables, int iQuantizationScale) {
        _tables = tables;
        _iQuantizationScale = iQuantizationScale;
        _iMacroBlockCount = Calc.macroblocks(320, 208);
    }

    private int _iPreviousDcCr;
    private int _iPreviousDcCb;
    private int _iPreviousDcSetInY1UsedInY2Y3;
    private int _iPreviousDcSetInY3UsedInY1Y4;

    @Override
    public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
            throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
    {
        _iPreviousDcCr = 0;
        _iPreviousDcCb = 0;
        _iPreviousDcSetInY1UsedInY2Y3 = 0;
        _iPreviousDcSetInY3UsedInY1Y4 = 0;

        int iFrameQscale = -1; // qscale will be set on first block read

        final MdecCode code = new MdecCode();
        MdecContext context = new MdecContext(BitStreamUncompressor_Aconcagua.MACROBLOCK_HEIGHT);
        AconcaguaBitWriter writer = new AconcaguaBitWriter();

        while (context.getTotalMacroBlocksRead() < _iMacroBlockCount) {

            if (DEBUG) System.out.println(context);

            inStream.readMdecCode(code);
            int iQscale = code.getTop6Bits();
            if (iFrameQscale < 0)
                iFrameQscale = iQscale;
            else if (iQscale != iFrameQscale)
                throw new IncompatibleException(String.format("Inconsistent qscale value: current %d != new %d", iQscale, iFrameQscale));

            writer.writeBits(encodeDC(code.getBottom10Bits(), context.getCurrentBlock()));

            writer.writeBits(encodeAcBlock(context, inStream));

            MdecContext.MacroBlockPixel pixel = context.getMacroBlockPixel();
            if (context.getCurrentBlock().ordinal() == 0 && pixel.y == 0) {
                // start of a new column of macroblocks (or at the end)
                _iPreviousDcCr = 0;
                _iPreviousDcCb = 0;
                _iPreviousDcSetInY1UsedInY2Y3 = 0;
                _iPreviousDcSetInY3UsedInY1Y4 = 0;
                writer.resetColumn();
            }
        }

        return writer.toBytes();
    }

    private @Nonnull String[] encodeDC(int iDC, @Nonnull MdecBlock block) {

        String[] asBits;

        if (DEBUG) System.out.println(block + " DC " + iDC);

        switch (block) {
            case Cr:
                asBits = _tables.encodeDc(iDC, _iPreviousDcCr);
                _iPreviousDcCr = iDC;
                break;
            case Cb:
                asBits = _tables.encodeDc(iDC, _iPreviousDcCb);
                _iPreviousDcCb = iDC;
                break;
            case Y1:
                if (DEBUG) System.out.println("DC "+iDC+" - prev _iPreviousDcSetInY3UsedInY1Y4 "+_iPreviousDcSetInY3UsedInY1Y4);
                asBits = _tables.encodeDc(iDC, _iPreviousDcSetInY3UsedInY1Y4);
                _iPreviousDcSetInY1UsedInY2Y3 = iDC;
                break;
            case Y2:
                asBits = _tables.encodeDc(iDC, _iPreviousDcSetInY1UsedInY2Y3);
                break;
            case Y3:
                asBits = _tables.encodeDc(iDC, _iPreviousDcSetInY1UsedInY2Y3);
                _iPreviousDcSetInY3UsedInY1Y4 = iDC;
                if (DEBUG) System.out.println("_iPreviousDcSetInY3UsedInY1Y4 = "+_iPreviousDcSetInY3UsedInY1Y4);
                break;
            case Y4: default:
                asBits = _tables.encodeDc(iDC, _iPreviousDcSetInY3UsedInY1Y4);
                break;
        }

        return asBits;
    }

    private @Nonnull String[] encodeAcBlock(MdecContext context, @Nonnull MdecInputStream mdecInputStream)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {
        MdecCode mdecCode = new MdecCode();
        ArrayList<MdecCode> mdecCodes = new ArrayList<MdecCode>();
        while (!mdecInputStream.readMdecCode(mdecCode)) {
            mdecCodes.add(mdecCode.copy());
            context.nextCode();
        }
        context.nextCodeEndBlock();

        InstructionTable.InstructionCode instructionCode = _tables.getInstructionForCodeCount(mdecCodes.size());
        String[] asShortestEncode = encodeAcBlockWithInstruction(mdecCodes, instructionCode);

        return asShortestEncode;
    }

    private @Nonnull String[] encodeAcBlockWithInstruction(@Nonnull List<MdecCode> blockAcCodes,
                                                           @Nonnull InstructionTable.InstructionCode instructionCode)
    {
        if (instructionCode.getTotalCount() != blockAcCodes.size())
            throw new IllegalArgumentException();

        ArrayList<String> bits = new ArrayList<String>();

        if (DEBUG) System.out.println(blockAcCodes.size() + " AC codes");
        if (DEBUG) System.out.println(instructionCode);
        bits.add(instructionCode.getBits());
        int iCode = 0;
        for (int i = 0; i < instructionCode.getTable1Count(); i++) {
            bits.addAll(Arrays.asList(_tables.encodeAcAsTable(blockAcCodes.get(iCode++), 1)));
        }
        for (int i = 0; i < instructionCode.getTable2Count(); i++) {
            bits.addAll(Arrays.asList(_tables.encodeAcAsTable(blockAcCodes.get(iCode++), 2)));
        }
        for (int i = 0; i < instructionCode.getTable3Count(); i++) {
            bits.addAll(Arrays.asList(_tables.encodeAcAsTable(blockAcCodes.get(iCode++), 3)));
        }

        return bits.toArray(new String[bits.size()]);
    }

    @Override
    public @CheckForNull byte[] compressFull(int iMaxSize,
                                             @Nonnull String sFrameDescription,
                                             @Nonnull MdecEncoder encoder,
                                             @Nonnull ILocalizedLogger log)
            throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
    {
        return CommonBitStreamCompressing.singleQscaleCompressFull(iMaxSize, sFrameDescription, encoder, this, log);
    }

    @Override
    public @CheckForNull byte[] compressPartial(int iMaxSize,
                                                @Nonnull String sFrameDescription,
                                                @Nonnull MdecEncoder encoder,
                                                @Nonnull ILocalizedLogger log)
            throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
    {
        return CommonBitStreamCompressing.singleQscaleCompressPartial(iMaxSize, sFrameDescription, encoder, this, _iQuantizationScale, log);
    }


    private static class AconcaguaBitWriter {

        private final ByteArrayOutputStream _out = new ByteArrayOutputStream();

        private final StringBuilder _currentInt = new StringBuilder();

        public void writeBits(@Nonnull String... asBits) {
            for (String sBit : asBits) {
                _currentInt.insert(0, sBit);
                int iEnd = _currentInt.length();
                if (iEnd >= 32) {
                    // 32 bits ready to be flushed to the output stream
                    int iStart = iEnd - 32;
                    String si = _currentInt.substring(iStart, iEnd);
                    BigInteger bi = new BigInteger(si, 2);
                    int i = bi.intValue();

                    try {
                        IO.writeInt32LE(_out, i);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    _currentInt.delete(iStart, iEnd);
                    if (DEBUG) System.out.println("-----------------------------------");
                }
            }
        }

        public void resetColumn() {
            if (_currentInt.length() == 0)
                return;

            int iZerosToAdd = 32 - _currentInt.length();
            writeBits(Misc.dup('0', iZerosToAdd));
        }

        public @Nonnull byte[] toBytes() {
            return _out.toByteArray();
        }
    }

}
