/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

import jpsxdec.psxvideo.mdec.MdecInputStream;
import static jpsxdec.psxvideo.mdec.MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;

/** Encodes a {@link PsxYCbCrImage} into an {@link MdecInputStream} at the
 *  specified quantization scale. After encoding, the MdecInputStream
 *  will most likely be then compressed as a bitstream.  */
public class MdecEncoder {

    private static final boolean DEBUG = false;

    private final double[][] _aadblYBlockVectors;
    private final double[][] _aadblCbBlockVectors;
    private final double[][] _aadblCrBlockVectors;
    private final int _iMacBlockWidth;
    private final int _iMacBlockHeight;
    // TODO: Change to use a Forward DCT more closely resembling the PSX
    private StephensIDCT _DCT = new StephensIDCT();

    private static final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();


    /**
     * The luma and chroma quantization scales are separate primarily for
     * handling Lain bitstream format. All other video formats should use the
     * same value for luma and chroma.
     */
    public MdecEncoder(PsxYCbCrImage ycbcr) {

        if (ycbcr.getLumaWidth() % 16 != 0 || ycbcr.getLumaHeight() % 16 != 0)
            throw new IllegalArgumentException();

        _iMacBlockWidth = ycbcr.getLumaWidth() / 16;
        _iMacBlockHeight = ycbcr.getLumaHeight() / 16;

        _aadblYBlockVectors = new double[_iMacBlockWidth * _iMacBlockHeight * 4][];
        _aadblCbBlockVectors = new double[_iMacBlockWidth * _iMacBlockHeight][];
        _aadblCrBlockVectors = new double[_iMacBlockWidth * _iMacBlockHeight][];

        // encode luma
        int iBlock = 0;
        for (int iBlockX = 0; iBlockX < _iMacBlockWidth*2; iBlockX++) {
            for (int iBlockY = 0; iBlockY < _iMacBlockHeight*2; iBlockY++) {
                double[] adblBlock = ycbcr.get8x8blockY(iBlockX*8, iBlockY*8);
                _aadblYBlockVectors[iBlockX + iBlockY * _iMacBlockWidth*2] = encodeBlock(adblBlock);
                iBlock++;
            }
        }
        // encode chroma
        iBlock = 0;
        for (int iBlockX = 0; iBlockX < _iMacBlockWidth; iBlockX++) {
            for (int iBlockY = 0; iBlockY < _iMacBlockHeight; iBlockY++) {
                double[] adblBlock = ycbcr.get8x8blockCb(iBlockX*8, iBlockY*8);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cb");
                _aadblCbBlockVectors[iBlockX + iBlockY * _iMacBlockWidth] = encodeBlock(adblBlock);
                if (DEBUG)
                    System.out.println("Encoding macroblock " + iBlock + " Cr");
                adblBlock = ycbcr.get8x8blockCr(iBlockX*8, iBlockY*8);
                _aadblCrBlockVectors[iBlockX + iBlockY * _iMacBlockWidth] = encodeBlock(adblBlock);
                iBlock++;
            }
        }

    }

    private double[] encodeBlock(double[] adblBlock) {
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

        double[] aiBlock = preQuantizeZigZagBlock(adblBlock);

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

    private double[] preQuantizeZigZagBlock(double[] adblBlock) {
        double[] adblVector = new double[8*8];
        // partially quantize it
        adblVector[0] = (int)Math.round(adblBlock[0]
                     / (double)PSX_DEFAULT_QUANTIZATION_MATRIX[0]);
        if (DEBUG)
            System.out.println(adblVector[0]);
        for (int i = 1; i < REVERSE_ZIG_ZAG_LOOKUP_LIST.length; i++) {
            int iZigZagPos = REVERSE_ZIG_ZAG_LOOKUP_LIST[i];
            adblVector[i] = adblBlock[iZigZagPos] * 8.0 / PSX_DEFAULT_QUANTIZATION_MATRIX[iZigZagPos];
            if (DEBUG)
                System.out.println(adblVector[i]);
        }
        return adblVector;
    }

    public MdecInputStream getStream(int[] aiBlockQscales) {
        return new EncodedMdecInputStream(aiBlockQscales);
    }

    private class EncodedMdecInputStream extends MdecInputStream {

        private int __iMacroBlockX=0, __iMacroBlockY=0, __iBlock=0, __iVectorPos=0;
        private int __iQscale;

        private final int[] __aiBlockQscales;
        private int __iQscaleIndex = 0;

        public EncodedMdecInputStream(int[] aiBlockQscales) {
            __aiBlockQscales = aiBlockQscales;
        }

        private int nextQscale() {
            int iNextQscale = __aiBlockQscales[__iQscaleIndex];
            if (iNextQscale < 1 || iNextQscale > 63)
                throw new RuntimeException("Invalid qscale " + iNextQscale);
            __iQscaleIndex++;
            return iNextQscale;
        }

        public boolean readMdecCode(MdecCode code) {
            double[] adblVector;
            switch (__iBlock) {
                case 0: adblVector = _aadblCrBlockVectors[ __iMacroBlockX +  __iMacroBlockY * _iMacBlockWidth]; break;
                case 1: adblVector = _aadblCbBlockVectors[ __iMacroBlockX +  __iMacroBlockY * _iMacBlockWidth]; break;
                case 2: adblVector = _aadblYBlockVectors[(__iMacroBlockX * 2   ) +  __iMacroBlockY * 2     * _iMacBlockWidth * 2]; break;
                case 3: adblVector = _aadblYBlockVectors[(__iMacroBlockX * 2 +1) +  __iMacroBlockY * 2     * _iMacBlockWidth * 2]; break;
                case 4: adblVector = _aadblYBlockVectors[ __iMacroBlockX * 2     + (__iMacroBlockY * 2 +1) * _iMacBlockWidth * 2]; break;
                case 5: adblVector = _aadblYBlockVectors[ __iMacroBlockX * 2 +1  + (__iMacroBlockY * 2 +1) * _iMacBlockWidth * 2]; break;
                default: throw new IllegalStateException();
            }

            if (__iVectorPos == 0) { // qscale & dc
                __iQscale = nextQscale();
                code.setTop6Bits(__iQscale);
                code.setBottom10Bits((int)Math.round(adblVector[0]));
                __iVectorPos++;
                if (DEBUG)
                    System.out.println(code);
                return false;
            } else {
                int iZeroCount = 0;
                int iQuantVal = -1;
                while (__iVectorPos < adblVector.length) {
                    iQuantVal = (int)Math.round(adblVector[__iVectorPos] / __iQscale);
                    if (iQuantVal == 0)
                        iZeroCount++;
                    else
                        break;
                    __iVectorPos++;
                }

                if (__iVectorPos < adblVector.length) {
                    code.setTop6Bits(iZeroCount);
                    code.setBottom10Bits(iQuantVal);
                    __iVectorPos++;
                    if (DEBUG)
                        System.out.println(code);
                    return false;
                } else { // end of block
                    code.setToEndOfData();
                    __iVectorPos = 0;

                    incremntIndexes();
                    if (DEBUG)
                        System.out.println(code);
                    return true;
                }
            }
        }

        private void incremntIndexes() {
            __iBlock++;
            if (__iBlock >= 6) {
                __iBlock = 0;
                __iMacroBlockY++;
                if (__iMacroBlockY >= _iMacBlockHeight) {
                    __iMacroBlockY = 0;
                    __iMacroBlockX++;
                }
            }
        }

    }

}
