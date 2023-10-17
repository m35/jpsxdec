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

package jpsxdec.psxvideo.mdec;

import java.util.Arrays;
import javax.annotation.Nonnull;
import jpsxdec.formats.RGB;
import jpsxdec.psxvideo.PsxYCbCr_int;
import jpsxdec.psxvideo.mdec.idct.IDCT_int;

/** A full Java, integer based implementation of the PlayStation 1 MDEC chip.
 * This may not be as precise as the double-based implementation, but on cursory
 * examination, you can't really tell. It's also significantly faster. */
public class MdecDecoder_int extends MdecDecoder {

    public static boolean LOG_CORRUPTION_STACK_TRACE = true;

    private final IDCT_int _idct;

    private final int[] _aiCrBuffer;
    private final int[] _aiCbBuffer;
    private final int[] _aiLumaBuffer;

    /** Matrix of 8x8 coefficient values. */
    protected final int[] _CurrentBlock = new int[64];

    public MdecDecoder_int(@Nonnull IDCT_int idct, int iWidth, int iHeight) {
        super(iWidth, iHeight);
        _idct = idct;

        _aiCrBuffer = new int[CW*CH];
        _aiCbBuffer = new int[_aiCrBuffer.length];
        _aiLumaBuffer = new int[W*H];
    }

    @Override
    public void decode(@Nonnull MdecInputStream sourceMdecInStream)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
    {
        Ac0Checker mdecInStream = Ac0Checker.wrapWithChecker(sourceMdecInStream, false);

        int iCurrentBlockQscale;
        int iCurrentBlockVectorPosition;
        int iCurrentBlockNonZeroCount;
        int iCurrentBlockLastNonZeroPosition;

        MdecContext context = new MdecContext(_iMacBlockHeight);

        try {

            // decode all the macro blocks of the image
            while (context.getTotalMacroBlocksRead() < _iTotalMacBlocks) {
                // debug
                assert !DEBUG || debugPrintln(String.format("############### Decoding macro block %d %s ###############",
                                              context.getTotalMacroBlocksRead(), context.getMacroBlockPixel()));

                for (int iBlock = 0; iBlock < MdecBlock.count(); iBlock++) {

                    assert !DEBUG || debugPrintln("=========== Decoding block "+context.getCurrentBlock()+" ===========");

                    Arrays.fill(_CurrentBlock, 0);
                    mdecInStream.readMdecCode(_code);

                    assert !DEBUG || debugPrintln("Qscale & DC " + _code);

                    if (_code.getBottom10Bits() != 0) {
                        _CurrentBlock[0] =
                                _code.getBottom10Bits() * _aiQuantizationTable[0];
                        iCurrentBlockNonZeroCount = 1;
                        iCurrentBlockLastNonZeroPosition = 0;
                    } else {
                        iCurrentBlockNonZeroCount = 0;
                        iCurrentBlockLastNonZeroPosition = -1;
                    }
                    assert !DEBUG || setPrequantValue(0, _code.getBottom10Bits());
                    iCurrentBlockQscale = _code.getTop6Bits();
                    iCurrentBlockVectorPosition = 0;

                    while (!mdecInStream.readMdecCode(_code)) {

                        assert !DEBUG || debugPrintln(_code.toString());

                        ////////////////////////////////////////////////////////
                        iCurrentBlockVectorPosition += _code.getTop6Bits() + 1;

                        int iRevZigZagMatrixPos;
                        try {
                            // Reverse Zig-Zag
                            iRevZigZagMatrixPos = MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST[iCurrentBlockVectorPosition];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            MdecContext.MacroBlockPixel macBlkXY = context.getMacroBlockPixel();
                            String sMsg = MdecException.RLC_OOB_IN_BLOCK_NAME(
                                    iCurrentBlockVectorPosition, context.getTotalMacroBlocksRead(),
                                    macBlkXY.x, macBlkXY.y,
                                    context.getCurrentBlock().ordinal(), context.getCurrentBlock().name());
                            throw new MdecException.ReadCorruption(sMsg, LOG_CORRUPTION_STACK_TRACE ? ex : null);
                        }

                        if (_code.getBottom10Bits() != 0) {

                            assert !DEBUG || setPrequantValue(iRevZigZagMatrixPos, _code.getBottom10Bits());
                            // Dequantize
                            _CurrentBlock[iRevZigZagMatrixPos] =
                                        (_code.getBottom10Bits()
                                      * _aiQuantizationTable[iRevZigZagMatrixPos]
                                      * iCurrentBlockQscale + 4) >> 3;
                            //  i      >> 3  ==  (int)Math.floor(i / 8.0)
                            // (i + 4) >> 3  ==  (int)Math.round(i / 8.0)
                            iCurrentBlockNonZeroCount++;
                            iCurrentBlockLastNonZeroPosition = iRevZigZagMatrixPos;

                        }
                        ////////////////////////////////////////////////////////
                        context.nextCode();
                    }

                    assert !DEBUG || debugPrintln(_code.toString());

                    writeEndOfBlock(context.getTotalMacroBlocksRead(), context.getCurrentBlock().ordinal(),
                            iCurrentBlockNonZeroCount,
                            iCurrentBlockLastNonZeroPosition);

                    context.nextCodeEndBlock();
                }
            }
        } finally {
            // in case an exception occurred
            // fill in any remaining data with zeros
            // pickup where decoding left off
            while (context.getTotalMacroBlocksRead() < _iTotalMacBlocks) {
                writeEndOfBlock(context.getTotalMacroBlocksRead(), context.getCurrentBlock().ordinal(), 0, 0);
                context.nextCodeEndBlock();
            }

            mdecInStream.logIfAny0AcCoefficient();
        }
    }

    private boolean debugPrintBlock(@Nonnull String sMsg) {
        System.out.println(sMsg);
        for (int i = 0; i < 8; i++) {
            System.out.print("[ ");
            for (int j = 0; j < 8; j++) {
                System.out.format( "%d, ", _CurrentBlock[j+i*8]);
            }
            System.out.print("]");
            System.out.println();
        }
        return true;
    }

    private void writeEndOfBlock(int iMacroBlock, int iBlock,
                                 int iNonZeroCount, int iNonZeroPos)
    {
        assert !DEBUG || debugPrintPrequantBlock();
        assert !DEBUG || debugPrintBlock("Pre-IDCT block");

        int[] outputBuffer;
        int iOutOffset, iOutWidth;
        switch (iBlock) {
            case 0:
                outputBuffer = _aiCrBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            case 1:
                outputBuffer = _aiCbBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            default:
                outputBuffer = _aiLumaBuffer;
                iOutOffset = _aiLumaBlkOfsLookup[iMacroBlock*4 + iBlock-2];
                iOutWidth = W;
        }
        if (iNonZeroCount == 0) {
            for (int i=0; i < 8; i++, iOutOffset += iOutWidth)
                Arrays.fill(outputBuffer, iOutOffset, iOutOffset + 8, 0);
        } else {
            if (iNonZeroCount == 1) {
                _idct.IDCT_1NonZero(_CurrentBlock, iNonZeroPos, 0, _CurrentBlock);
            } else {
                _idct.IDCT(_CurrentBlock, 0, _CurrentBlock);
            }
            // TODO: have IDCT write to the destination location directly
            for (int i=0, iSrcOfs=0; i < 8; i++, iSrcOfs+=8, iOutOffset += iOutWidth)
                System.arraycopy(_CurrentBlock, iSrcOfs, outputBuffer, iOutOffset, 8);
        }

        assert !DEBUG || debugPrintBlock("Post-IDCT block");

    }

    @Override
    public void readDecodedRgb(int iDestWidth, int iDestHeight, @Nonnull int[] aiDest,
                               int iOutStart, int iOutStride)
    {
        final PsxYCbCr_int psxycc = new PsxYCbCr_int();
        final RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        final int W_x2 = W*2, iOutStride_x2 = iOutStride*2;

        final int iDestWidthSub1 = iDestWidth - 1;
        final int iDestHeightSub1 = iDestHeight - 1;

        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0,
            iDestLineOfsStart = iOutStart;
        int iY=0;
        for (; iY < iDestHeightSub1; iY+=2,
             iLumaLineOfsStart+=W_x2, iChromaLineOfsStart+=CW,
             iDestLineOfsStart+=iOutStride_x2)
        {
            // writes 2 lines at a time
            int iSrcLumaOfs1 = iLumaLineOfsStart,
                iSrcLumaOfs2 = iLumaLineOfsStart + W,
                iSrcChromaOfs = iChromaLineOfsStart,
                iDestOfs1 = iDestLineOfsStart,
                iDestOfs2 = iDestLineOfsStart + iOutStride;

            int iX=0;
            for (; iX < iDestWidthSub1; iX+=2,
                 iSrcChromaOfs++)
            {
                psxycc.cr = _aiCrBuffer[iSrcChromaOfs];
                psxycc.cb = _aiCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _aiLumaBuffer[iSrcLumaOfs1++];
                psxycc.y2 = _aiLumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _aiLumaBuffer[iSrcLumaOfs2++];
                psxycc.y4 = _aiLumaBuffer[iSrcLumaOfs2++];

                psxycc.toRgb(rgb1, rgb2, rgb3, rgb4);

                aiDest[iDestOfs1++] = rgb1.toInt();
                aiDest[iDestOfs1++] = rgb2.toInt();
                aiDest[iDestOfs2++] = rgb3.toInt();
                aiDest[iDestOfs2++] = rgb4.toInt();
            }

            if (iX < iDestWidth) {
                // if the width is odd, add 2 pixels
                psxycc.cr = _aiCrBuffer[iSrcChromaOfs];
                psxycc.cb = _aiCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _aiLumaBuffer[iSrcLumaOfs1];
                psxycc.y2 = _aiLumaBuffer[iSrcLumaOfs1];
                psxycc.y3 = _aiLumaBuffer[iSrcLumaOfs2];
                psxycc.y4 = _aiLumaBuffer[iSrcLumaOfs2];

                psxycc.toRgb(rgb1, rgb2, rgb3, rgb4); // rgb2,4 ignored

                aiDest[iDestOfs1] = rgb1.toInt();
                aiDest[iDestOfs2] = rgb3.toInt();
            }
        }

        if (iY < iDestHeight) {
            // if the height is odd, write 1 line
            int iSrcLumaOfs1 = iLumaLineOfsStart,
                iSrcLumaOfs2 = iLumaLineOfsStart + W,
                iSrcChromaOfs = iChromaLineOfsStart,
                iDestOfs1 = iDestLineOfsStart;

            int iX=0;
            for (; iX < iDestWidthSub1; iX+=2,
                 iSrcChromaOfs++)
            {
                psxycc.cr = _aiCrBuffer[iSrcChromaOfs];
                psxycc.cb = _aiCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _aiLumaBuffer[iSrcLumaOfs1++];
                psxycc.y2 = _aiLumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _aiLumaBuffer[iSrcLumaOfs2++];
                psxycc.y4 = _aiLumaBuffer[iSrcLumaOfs2++];

                psxycc.toRgb(rgb1, rgb2, rgb3, rgb4); // rgb3,4 ignored

                aiDest[iDestOfs1++] = rgb1.toInt();
                aiDest[iDestOfs1++] = rgb2.toInt();
            }

            if (iX < iDestWidth) {
                // if the width is odd, add 1 pixel
                psxycc.cr = _aiCrBuffer[iSrcChromaOfs];
                psxycc.cb = _aiCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _aiLumaBuffer[iSrcLumaOfs1];
                psxycc.y2 = _aiLumaBuffer[iSrcLumaOfs1];
                psxycc.y3 = _aiLumaBuffer[iSrcLumaOfs2];
                psxycc.y4 = _aiLumaBuffer[iSrcLumaOfs2];

                psxycc.toRgb(rgb1, rgb2, rgb3, rgb4); // rgb2,3,4 ignored

                aiDest[iDestOfs1] = rgb1.toInt();
            }
        }
    }

}
