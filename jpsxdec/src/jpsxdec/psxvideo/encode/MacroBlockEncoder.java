/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.psxvideo.encode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import static jpsxdec.psxvideo.mdec.MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;

/** Encodes a single macroblock into MDEC codes. */
public class MacroBlockEncoder implements Iterable<MdecCode> {

    private static final boolean DEBUG = false;

    private static final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            Arrays.copyOf(MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX,
                          MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX.length);

    private final PsxMdecIDCT_double _DCT = new PsxMdecIDCT_double();

    private final double[][] _aadblEncodedBlocks = new double[MdecBlock.count()][];

    @CheckForNull
    private int[] _aiQscales, _aiSquashQscales;

    private final int _iMacroBlockX, _iMacroBlockY;

    /** Energy of the macroblock.
     * It is calculated using my best guess as an approach. */
    private double _dblEnergy = 0;

    MacroBlockEncoder(@Nonnull PsxYCbCrImage ycbcr, int iMacroBlockX, int iMacroBlockY) {
        _iMacroBlockX = iMacroBlockX;
        _iMacroBlockY = iMacroBlockY;

        _aadblEncodedBlocks[MdecBlock.Cr.ordinal()] = encodeBlock(ycbcr.get8x8blockCr(iMacroBlockX*8, iMacroBlockY*8), MdecBlock.Cr);
        _aadblEncodedBlocks[MdecBlock.Cb.ordinal()] = encodeBlock(ycbcr.get8x8blockCb(iMacroBlockX*8, iMacroBlockY*8), MdecBlock.Cb);
        _aadblEncodedBlocks[MdecBlock.Y1.ordinal()] = encodeBlock(ycbcr.get8x8blockY(iMacroBlockX*16+0, iMacroBlockY*16+0), MdecBlock.Y1);
        _aadblEncodedBlocks[MdecBlock.Y2.ordinal()] = encodeBlock(ycbcr.get8x8blockY(iMacroBlockX*16+8, iMacroBlockY*16+0), MdecBlock.Y2);
        _aadblEncodedBlocks[MdecBlock.Y3.ordinal()] = encodeBlock(ycbcr.get8x8blockY(iMacroBlockX*16+0, iMacroBlockY*16+8), MdecBlock.Y3);
        _aadblEncodedBlocks[MdecBlock.Y4.ordinal()] = encodeBlock(ycbcr.get8x8blockY(iMacroBlockX*16+8, iMacroBlockY*16+8), MdecBlock.Y4);
    }

    public int getMacroBlockX() {
        return _iMacroBlockX;
    }

    public int getMacroBlockY() {
        return _iMacroBlockY;
    }

    private @Nonnull double[] encodeBlock(@Nonnull double[] adblBlock, @Nonnull MdecBlock block) {

        if (DEBUG) {
            System.out.println("Encoding " + block);
            System.out.println("Pre DCT");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.format("%1.3f ", adblBlock[x + y * 8]);
                }
                System.out.println("]");
            }
        }

        // perform the discrete cosine transform
        double[] adblEncodedBlock = new double[adblBlock.length];
        _DCT.DCT(adblBlock, 0, adblEncodedBlock);

        if (DEBUG) {
            System.out.println("Post DCT");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.format("%1.3f ", adblEncodedBlock[x + y * 8]);
                }
                System.out.println("]");
            }
        }

        // only use Luma blocks to calculate the energy
        if (block.isLuma()) {
            for (int i = 1; i < REVERSE_ZIG_ZAG_LOOKUP_LIST.length; i++) {
                int iZigZagPos = REVERSE_ZIG_ZAG_LOOKUP_LIST[i];
                // give more weight to the AC codes closer to the bottom-right of the DCT block
                _dblEnergy += adblEncodedBlock[iZigZagPos] * i * i;
            }
        }

        return adblEncodedBlock;
    }

    // -------------------------------------------------------------------------

    public double getEnergy() {
        return _dblEnergy;
    }

    public void setToFullEncode(@Nonnull int[] aiQscales) {
        if (aiQscales.length != 6)
            throw new IllegalArgumentException();
        _aiSquashQscales = _aiQscales = aiQscales.clone();
    }
    public void setToPartialEncode(@Nonnull int[] aiQscales, @Nonnull int[] aiSquashQscales) {
        if (aiQscales.length != 6)
            throw new IllegalArgumentException();
        _aiQscales = aiQscales.clone();
        _aiSquashQscales = aiSquashQscales.clone();
    }

    @Override
    public @Nonnull Iterator<MdecCode> iterator() {
        if (_aiQscales == null || _aiSquashQscales == null)
            throw new IllegalStateException();

        ArrayList<MdecCode> codes = new ArrayList<MdecCode>();
        for (MdecBlock block : MdecBlock.list()) {
            quantizeBlock(_aadblEncodedBlocks[block.ordinal()], codes,
                          _aiQscales[block.ordinal()], _aiSquashQscales[block.ordinal()]);
        }
        return codes.iterator();
    }

    // -------------------------------------------------------------------------

    private static void quantizeBlock(@Nonnull double[] adblDctBlock,
                                      @Nonnull ArrayList<MdecCode> out,
                                      int iQscale, int iSquashQscale)
    {
        final MdecCode code = new MdecCode();
        code.setTop6Bits(iQscale);
        code.setBottom10Bits((int)Math.round(adblDctBlock[0] / PSX_DEFAULT_QUANTIZATION_MATRIX[0]));
        out.add(code.copy());
        if (DEBUG)
            System.out.println(code);

        Outer:
        for (int iBlockPos = 1;;) {
            // find next non-zero AC coefficient
            int iZeroCount = 0, iAcCoff;
            while (true) {
                if (iBlockPos >= adblDctBlock.length)
                    break Outer;

                int iZigZagPos = REVERSE_ZIG_ZAG_LOOKUP_LIST[iBlockPos];
                iBlockPos++;

                double dblQuant = adblDctBlock[iZigZagPos] * 8.0 / PSX_DEFAULT_QUANTIZATION_MATRIX[iZigZagPos];
                if (iQscale == iSquashQscale)
                    iAcCoff = (int)Math.round(dblQuant / iQscale);
                else
                    iAcCoff = (int)Math.round(
                                     Math.round(dblQuant / iSquashQscale) *
                                                iQscale / (double)iSquashQscale);
                if (iAcCoff == 0)
                    iZeroCount++;
                else
                    break;
            }

            code.setTop6Bits(iZeroCount);
            code.setBottom10Bits(iAcCoff);
            out.add(code.copy());
            if (DEBUG)
                System.out.println(code);
        }

        // end of block
        code.setToEndOfData();
        out.add(code.copy());
        if (DEBUG)
            System.out.println(code);
    }


}
