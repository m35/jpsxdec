/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

/** Super class of the two different MDEC decoders: int and double. */
public abstract class MdecDecoder {

    public static boolean DEBUG = false;
    
    protected static final String[] BLOCK_NAMES = {
        "Cr", "Cb", "Y1", "Y2", "Y3", "Y4"
    };
    
    protected static boolean debugPrintln(String s) {
        System.out.println(s);
        return true;
    }

    protected final MdecInputStream.MdecCode _code = new MdecInputStream.MdecCode();

    protected final int _iMacBlockWidth;
    protected final int _iMacBlockHeight;

    /** Luma dimensions. */
    protected final int W, H;
    /** Chroma dimensions. */
    protected final int CW, CH;

    protected final int[] _aiLumaBlkOfsLookup;
    protected final int[] _aiChromaMacBlkOfsLookup;

    protected final int[] _aiQuantizationTable =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();
    
    protected final int[] _aiDebugPreqantBlock;

    protected MdecDecoder(int iWidth, int iHeight) {
        _iMacBlockWidth = Calc.macroblockDim(iWidth);
        _iMacBlockHeight = Calc.macroblockDim(iHeight);
        W = _iMacBlockWidth * 16;
        H = _iMacBlockHeight * 16;
        CW = _iMacBlockWidth * 8;
        CH = _iMacBlockHeight * 8;

        _aiChromaMacBlkOfsLookup = new int[_iMacBlockWidth * _iMacBlockHeight];
        _aiLumaBlkOfsLookup = new int[_iMacBlockWidth * _iMacBlockHeight * 4];

        // build a table that holds the starting index of every (macro)block
        // in the output buffer so we don't have to do this calculation during decoding
        int iMbIdx = 0;
        for (int iMbX=0; iMbX < _iMacBlockWidth; iMbX++) {
            for (int iMbY=0; iMbY < _iMacBlockHeight; iMbY++) {
                _aiChromaMacBlkOfsLookup[iMbIdx] = iMbX*8 + iMbY*8 * CW;
                int iBlkIdx = 0;
                for (int iBlkY=0; iBlkY < 2; iBlkY++) {
                    for (int iBlkX=0; iBlkX < 2; iBlkX++) {
                        _aiLumaBlkOfsLookup[iMbIdx*4+iBlkIdx] = iMbX*16 + iBlkX*8 +
                                                                (iMbY*16 + iBlkY*8) * W;
                        iBlkIdx++;
                    }
                }
                iMbIdx++;
            }
        }
        
        boolean blnAssert = false;
        assert blnAssert = true;
        
        if (blnAssert && DEBUG)
            _aiDebugPreqantBlock = new int[64];
        else
            _aiDebugPreqantBlock = null;
    }
    
    protected boolean clearPrequantTable() {
        Arrays.fill(_aiDebugPreqantBlock, 0);
        return true;
    }
    
    protected boolean setPrequantValue(int iPos, int iVal) {
        _aiDebugPreqantBlock[iPos] = iVal;
        return true;
    }
    
    protected boolean debugPrintPrequantBlock() {
        System.out.println("Pre-dequantization block");
        for (int i = 0; i < 8; i++) {
            System.out.print("[ ");
            for (int j = 0; j < 8; j++) {
                System.out.format( "%d, ", _aiDebugPreqantBlock[j+i*8]);
            }
            System.out.print("]");
            System.out.println();
        }
        return true;
    }

    /** Reads an image from the MdecInputStream and decodes it to an internal
     *  PSX YCbCr buffer. */
    abstract public void decode(MdecInputStream mdecStream)
            throws MdecException.Decode;
    
    /** Retrieve the contents of the internal PSX YCbCr buffer converted to RGB. */
    abstract public void readDecodedRgb(int iDestWidth, int iDestHeight, int[] aiDest,
                                        int iOutStart, int iOutStride);

    public void readDecodedRgb(int iDestWidth, int iDestHeight, int[] aiDest) {
        readDecodedRgb(iDestWidth, iDestHeight, aiDest, 0, iDestWidth);
    }

    public void setQuantizationTable(int[] aiNewTable) {
        if (aiNewTable.length != _aiQuantizationTable.length)
            throw new IllegalArgumentException("Incorrect table size");
        System.arraycopy(aiNewTable, 0, _aiQuantizationTable, 0, _aiQuantizationTable.length);
    }
}
