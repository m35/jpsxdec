/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2019  Michael Sabin
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
import java.util.Iterator;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import static jpsxdec.psxvideo.mdec.MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;

/** Encodes a single macroblock into MDEC codes. */
public class MacroBlockEncoder implements Iterable<MdecCode> {

    private static final boolean DEBUG = false;

    private static final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            new int[MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX.length];
    static {
        System.arraycopy(MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX, 0,
                         PSX_DEFAULT_QUANTIZATION_MATRIX, 0,
                         MdecInputStream.PSX_DEFAULT_QUANTIZATION_MATRIX.length);
    }
    
    // TODO: Change to use a Forward DCT more closely resembling the PSX
    private final StephensIDCT _DCT = new StephensIDCT();

    private final double[][] _aadblYBlockVectors = new double[4][];
    @Nonnull
    private final double[] _adblCbBlockVector;
    @Nonnull
    private final double[] _adblCrBlockVector;

    @Nonnull
    private int[] _aiQscales, _aiSquashQscales;

    public final int X, Y;

    /** Energy of the macroblock.
     * It is calculated using my best guess as an approach. */
    private double _dblEnergy = 0;
    
    MacroBlockEncoder(@Nonnull PsxYCbCrImage ycbcr, int iMacroBlockX, int iMacroBlockY) {

        X = iMacroBlockX;
        Y = iMacroBlockY;

        // encode luma
        int iBlock = 0;
        for (int iBlockY = 0; iBlockY < 16; iBlockY+=8) {
            for (int iBlockX = 0; iBlockX < 16; iBlockX+=8) {
                double[] adblBlock = ycbcr.get8x8blockY(iMacroBlockX*16+iBlockX,
                                                        iMacroBlockY*16+iBlockY);
                _aadblYBlockVectors[iBlock] = preEncodeBlock(adblBlock, iBlock+2);
                iBlock++;
            }
        }
        
        // encode chroma
        if (DEBUG)
            System.out.println("Encoding macroblock Cb");
        double[] adblBlock = ycbcr.get8x8blockCb(iMacroBlockX*8, iMacroBlockY*8);
        _adblCbBlockVector = preEncodeBlock(adblBlock, 0);
        if (DEBUG)
            System.out.println("Encoding macroblock Cr");
        adblBlock = ycbcr.get8x8blockCr(iMacroBlockX*8, iMacroBlockY*8);
        _adblCrBlockVector = preEncodeBlock(adblBlock, 1);
        

    }

    private @Nonnull double[] preEncodeBlock(@Nonnull double[] adblBlock, int iBlock) {
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

        double[] aiBlock = preQuantizeZigZagBlock(adblBlock, iBlock);

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


    private @Nonnull double[] preQuantizeZigZagBlock(@Nonnull double[] adblBlock, int iBlock) {
        double[] adblVector = new double[8*8];
        // partially quantize it
        adblVector[0] = (int)Math.round(adblBlock[0]
                     / (double)PSX_DEFAULT_QUANTIZATION_MATRIX[0]);
        if (DEBUG)
            System.out.println(adblVector[0]);
        for (int i = 1; i < REVERSE_ZIG_ZAG_LOOKUP_LIST.length; i++) {
            int iZigZagPos = REVERSE_ZIG_ZAG_LOOKUP_LIST[i];
            // only use Luma blocks to calculate the energy
            if (iBlock >= 2)
                // put more weight on AC codes closer to the bottom-right of the DCT block
                _dblEnergy += adblBlock[iZigZagPos] * i * i;
            adblVector[i] = adblBlock[iZigZagPos] * 8.0 / PSX_DEFAULT_QUANTIZATION_MATRIX[iZigZagPos];
            if (DEBUG)
                System.out.println(adblVector[i]);
        }
        return adblVector;
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

    public @Nonnull Iterator<MdecCode> iterator() {
        if (_aiQscales == null || _aiSquashQscales == null)
            throw new IllegalStateException();
        ArrayList<MdecCode> codes = new ArrayList<MdecCode>();
        encodeBlock(_adblCrBlockVector, codes, _aiQscales[0], _aiSquashQscales[0]);
        encodeBlock(_adblCbBlockVector, codes, _aiQscales[1], _aiSquashQscales[1]);
        encodeBlock(_aadblYBlockVectors[0], codes, _aiQscales[2], _aiSquashQscales[2]);
        encodeBlock(_aadblYBlockVectors[1], codes, _aiQscales[3], _aiSquashQscales[3]);
        encodeBlock(_aadblYBlockVectors[2], codes, _aiQscales[4], _aiSquashQscales[4]);
        encodeBlock(_aadblYBlockVectors[3], codes, _aiQscales[5], _aiSquashQscales[5]);
        return codes.iterator();
    }

    // -------------------------------------------------------------------------

    private void encodeBlock(@Nonnull double[] adblVector,
                             @Nonnull ArrayList<MdecCode> out,
                             int iQscale, int iSquashQscale)
    {
        final MdecCode code = new MdecCode();
        code.setTop6Bits(iQscale);
        code.setBottom10Bits((int)Math.round(adblVector[0]));
        out.add(code.copy());
        if (DEBUG)
            System.out.println(code);

        for (int iVectorPos = 1; iVectorPos < adblVector.length;) {
            // find next non-zero AC coefficient
            int iZeroCount = 0;
            int iQuantVal = -1;
            while (iVectorPos < adblVector.length) {
                if (iQscale == iSquashQscale)
                    iQuantVal = (int)Math.round(adblVector[iVectorPos] / iQscale);
                else
                    iQuantVal = (int)Math.round(
                                     Math.round(adblVector[iVectorPos] / iSquashQscale) *
                                                iQscale / (double)iSquashQscale);
                if (iQuantVal == 0)
                    iZeroCount++;
                else
                    break;
                iVectorPos++;
            }

            if (iVectorPos >= adblVector.length)
                break;
            
            code.setTop6Bits(iZeroCount);
            code.setBottom10Bits(iQuantVal);
            out.add(code.copy());
            iVectorPos++;
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
