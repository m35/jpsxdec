/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import jpsxdec.psxvideo.PsxYCbCrImage;
import java.io.EOFException;
import jpsxdec.psxvideo.mdec.DecodingException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;
import static jpsxdec.psxvideo.mdec.MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST;

public class MdecEncoder {

    private static final boolean DEBUG = false;

    private int[][] _aaiYBlocks;
    private int[][] _aaiCbBlocks;
    private int[][] _aaiCrBlocks;
    private int _iMacBlockWidth;
    private int _iMacBlockHeight;
    private int _iLuminQscale;
    private int _iChromQscale;
    private StephensIDCT _DCT = new StephensIDCT();

    private static int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();


    public MdecEncoder(PsxYCbCrImage ycbcr, int iLuminQscale, int iChromQscale) {

        if (ycbcr.getLuminWidth() % 16 != 0 || ycbcr.getLuminHeight() % 16 != 0)
            throw new IllegalArgumentException();

        _iMacBlockWidth = ycbcr.getLuminWidth() / 16;
        _iMacBlockHeight = ycbcr.getLuminHeight() / 16;
        _iLuminQscale = iLuminQscale;
        _iChromQscale = iChromQscale;

        _aaiYBlocks = new int[_iMacBlockWidth * _iMacBlockHeight * 4][];
        _aaiCbBlocks = new int[_iMacBlockWidth * _iMacBlockHeight][];
        _aaiCrBlocks = new int[_iMacBlockWidth * _iMacBlockHeight][];

        // encode luminance
        int iBlock = 0;
        for (int iBlockX = 0; iBlockX < _iMacBlockWidth*2; iBlockX++) {
            for (int iBlockY = 0; iBlockY < _iMacBlockHeight*2; iBlockY++) {
                double[] adblBlock = ycbcr.get8x8blockY(iBlockX*8, iBlockY*8);
                _aaiYBlocks[iBlockX + iBlockY * _iMacBlockWidth*2] = encodeBlock(adblBlock, _iLuminQscale);
                iBlock++;
            }
        }
        // encode chrominance
        iBlock = 0;
        for (int iBlockX = 0; iBlockX < _iMacBlockWidth; iBlockX++) {
            for (int iBlockY = 0; iBlockY < _iMacBlockHeight; iBlockY++) {
                double[] adblBlock = ycbcr.get8x8blockCb(iBlockX*8, iBlockY*8);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cb");
                _aaiCbBlocks[iBlockX + iBlockY * _iMacBlockWidth] = encodeBlock(adblBlock, _iChromQscale);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cr");
                adblBlock = ycbcr.get8x8blockCr(iBlockX*8, iBlockY*8);
                _aaiCrBlocks[iBlockX + iBlockY * _iMacBlockWidth] = encodeBlock(adblBlock, _iChromQscale);
                iBlock++;
            }
        }

    }

    private int[] encodeBlock(double[] adblBlock, int iQscale) {
        if (DEBUG) {
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
        _DCT.forwardDCT(adblBlock);

        if (DEBUG) {
            System.out.println("Post DCT (Pre zig-zag & quant)");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.format("%1.3f ", adblBlock[x + y * 8]);
                }
                System.out.println("]");
            }
        }

        int[] aiBlock = quantizeBlock(adblBlock, iQscale);
        
        if (DEBUG) {
            System.out.println("Final block");
            for (int y = 0; y < 8; y++) {
                System.out.print("[ ");
                for (int x = 0; x < 8; x++) {
                    System.out.print(aiBlock[x + y * 8] + " ");
                }
                System.out.println("]");
            }
        }

        return aiBlock;
    }

    private int[] quantizeBlock(double[] adblBlock, int iQscale) {
        int[] aiBlock = new int[8*8];
        // quantize it
        if (DEBUG)
            System.out.format("[0] = [0]%1.3f / %d = ", adblBlock[0], PSX_DEFAULT_QUANTIZATION_MATRIX[0]);
        aiBlock[0] = (int)Math.round(adblBlock[0]
                     / (double)PSX_DEFAULT_QUANTIZATION_MATRIX[0]);
        if (DEBUG)
            System.out.println(aiBlock[0]);
        for (int i = 1; i < REVERSE_ZIG_ZAG_LOOKUP_LIST.length; i++) {
            int iZigZagPos = REVERSE_ZIG_ZAG_LOOKUP_LIST[i];
            if (DEBUG) {
                System.out.format("[%d] = [%d]%1.3f / (%d * %d) * 8 = ",
                        i, iZigZagPos, adblBlock[iZigZagPos],
                        PSX_DEFAULT_QUANTIZATION_MATRIX[iZigZagPos],
                        iQscale);
            }
            aiBlock[i] = (int)Math.round(adblBlock[iZigZagPos] * 8.0 /
                         (PSX_DEFAULT_QUANTIZATION_MATRIX[iZigZagPos] * iQscale));
            if (DEBUG)
                System.out.println(aiBlock[i]);
        }
        return aiBlock;
    }

    public MdecInputStream getStream() {
        return new EncodedMdecInputStream();
    }

    private class EncodedMdecInputStream extends MdecInputStream {

        private int __iMacroBlockX=0, __iMacroBlockY=0, __iBlock=0, __iVectorPos=0;
        
        public boolean readMdecCode(MdecCode code) throws DecodingException, EOFException {
            int[] aiBlock;
            switch (__iBlock) {
                case 0: aiBlock = _aaiCrBlocks[ __iMacroBlockX +  __iMacroBlockY * _iMacBlockWidth]; break;
                case 1: aiBlock = _aaiCbBlocks[ __iMacroBlockX +  __iMacroBlockY * _iMacBlockWidth]; break;
                case 2: aiBlock = _aaiYBlocks[__iMacroBlockX * 2    +  __iMacroBlockY * 2     * _iMacBlockWidth * 2]; break;
                case 3: aiBlock = _aaiYBlocks[__iMacroBlockX * 2 +1 +  __iMacroBlockY * 2     * _iMacBlockWidth * 2]; break;
                case 4: aiBlock = _aaiYBlocks[__iMacroBlockX * 2    + (__iMacroBlockY * 2 +1) * _iMacBlockWidth * 2]; break;
                case 5: aiBlock = _aaiYBlocks[__iMacroBlockX * 2 +1 + (__iMacroBlockY * 2 +1) * _iMacBlockWidth * 2]; break;
                default: throw new IllegalStateException();
            }

            if (__iVectorPos == 0) { // qscale & dc
                if (__iBlock < 2)
                    code.setTop6Bits(_iChromQscale);
                else
                    code.setTop6Bits(_iLuminQscale);
                code.setBottom10Bits(aiBlock[0]);
                __iVectorPos++;
                if (DEBUG)
                    System.out.println(code);
                return false;
            } else {
                int iZeroCount = 0;
                while (__iVectorPos < aiBlock.length && aiBlock[__iVectorPos] == 0) {
                    iZeroCount++;
                    __iVectorPos++;
                }

                if (__iVectorPos < aiBlock.length) {
                    code.setTop6Bits(iZeroCount);
                    code.setBottom10Bits(aiBlock[__iVectorPos]);
                    __iVectorPos++;
                    if (DEBUG)
                        System.out.println(code);
                    return false;
                } else { // end of block
                    code.setToEndOfData();
                    __iVectorPos = 0;

                    // increment indexes
                    __iBlock++;
                    if (__iBlock >= 6) {
                        __iBlock = 0;
                        __iMacroBlockY++;
                        if (__iMacroBlockY >= _iMacBlockHeight) {
                            __iMacroBlockY = 0;
                            __iMacroBlockX++;
                        }
                    }
                    if (DEBUG)
                        System.out.println(code);
                    return true;
                }
            }
        }

    }

}
