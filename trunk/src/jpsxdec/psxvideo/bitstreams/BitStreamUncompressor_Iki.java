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

package jpsxdec.psxvideo.bitstreams;

import java.util.Arrays;
import java.util.Iterator;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


public class BitStreamUncompressor_Iki extends BitStreamUncompressor_STRv2 {

    private int _iMdecCodeCount;
    private int _iWidth, _iHeight;
    private int _iCompressedDataSize;
    
    private byte[] _abQscaleDcLookupTable;

    private int _iBlockCount;
    private int _iCurrentBlock;

    @Override
    protected void readHeader(byte[] abFrameData, ArrayBitReader bitReader) throws NotThisTypeException {
        _iMdecCodeCount = IO.readUInt16LE(abFrameData, 0);
        int iMagic3800 = IO.readUInt16LE(abFrameData, 2);
        _iWidth = IO.readSInt16LE(abFrameData, 4);
        _iHeight = IO.readSInt16LE(abFrameData, 6);
        _iCompressedDataSize = IO.readUInt16LE(abFrameData, 8);

        if (_iMdecCodeCount < 0 || iMagic3800 != 0x3800 || _iWidth < 1 || _iHeight < 1 || _iCompressedDataSize < 1)
            throw new NotThisTypeException();

        int iMacroBlockCount = ((_iWidth + 15) / 16) * ((_iHeight + 15) / 16);
        _iBlockCount = iMacroBlockCount * 6; // 6 blocks in a macroblock
        int iQscaleDcLookupTableSize = _iBlockCount * 2; // 2 bytes per block

        if (_abQscaleDcLookupTable == null || _abQscaleDcLookupTable.length < iQscaleDcLookupTableSize)
            _abQscaleDcLookupTable = new byte[iQscaleDcLookupTableSize];

        ikiLzsUncompress(abFrameData, 10, _abQscaleDcLookupTable, iQscaleDcLookupTableSize);

        bitReader.reset(abFrameData, true, 10 + _iCompressedDataSize);

        _iCurrentBlock = 0;
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int _iMdecCodeCount = IO.readUInt16LE(abFrameData, 0);
        int iMagic3800 = IO.readUInt16LE(abFrameData, 2);
        int _iWidth = IO.readSInt16LE(abFrameData, 4);
        int _iHeight = IO.readSInt16LE(abFrameData, 6);
        int _iCompressedDataSize = IO.readUInt16LE(abFrameData, 8);

        return !(_iMdecCodeCount < 0 || iMagic3800 != 0x3800 || _iWidth < 1 || _iHeight < 1 || _iCompressedDataSize < 1);
    }

    @Override
    protected void readQscaleAndDC(MdecCode code) throws MdecException.Uncompress {
        if (_iCurrentBlock >= _iBlockCount)
            throw new MdecException.Uncompress("End of stream");
        readBlockQscaleAndDC(code, _iCurrentBlock);
        _iCurrentBlock++;
    }

    private void readBlockQscaleAndDC(MdecCode code, int iBlock) {
        int b1 = _abQscaleDcLookupTable[iBlock] & 0xff;
        int b2 = _abQscaleDcLookupTable[iBlock+_iBlockCount] & 0xff;
        code.set((b1 << 8) | b2);
    }

    /** .iki videos utilize yet another LZSS compression format that is
     * different from both FF7 and Lain.   */
    private static void ikiLzsUncompress(byte[] abSrc, int iSrcPosition,
                                         byte[] abDest, int iUncompressedSize)
    {
        int iDestPosition = 0;

        while (iDestPosition < iUncompressedSize) {
            
            int iFlags = abSrc[iSrcPosition++];

            if (DEBUG)
                System.err.println("Flags " + Misc.bitsToString(iFlags, 8));

            for (int iBit = 0; iBit < 8; iBit++, iFlags >>= 1) {

                if (DEBUG)
                    System.err.format("[InPos: %d OutPos: %d] Flags %02x: bit %02x: ",
                                      iSrcPosition, iDestPosition, iFlags, 1 << iBit );

                if ((iFlags & 1) == 0) {
                    byte b = abSrc[iSrcPosition++];

                    if (DEBUG)
                        System.err.println(String.format("{Byte %02x}", b));

                    abDest[iDestPosition++] = b;
                } else {
                    int iCopySize = (abSrc[iSrcPosition++] & 0xff) + 3;

                    int iCopyOffset = (abSrc[iSrcPosition++] & 0xff);
                    if ((iCopyOffset & 0x80) != 0) {
                        iCopyOffset = ((iCopyOffset & 0x7f) << 8) | (abSrc[iSrcPosition++] & 0xff);
                    }
                    iCopyOffset++;

                    if (DEBUG)
                        System.err.println(
                                "Copy " + iCopySize + " bytes from " + (iDestPosition - (iCopyOffset + 1)) + "(-"+iCopyOffset+")");

                    for (; iCopySize > 0; iCopySize--) {
                        abDest[iDestPosition] = abDest[iDestPosition - iCopyOffset];
                        iDestPosition++;
                    }
                }
                
                if (iDestPosition >= iUncompressedSize)
                    break;
            }
        }
        if (DEBUG)
            System.err.println("Src pos at end: " + iSrcPosition);
    }

    @Override
    public String getName() {
        return "Iki";
    }

    @Override
    public int getQscale() {
        throw new UnsupportedOperationException("Getting quantization scale for iki is not possible");
    }

    @Override
    public Iterator<int[]> qscaleIterator(boolean blnStartAt1) {
        final int[] aiQscales = new int[_iBlockCount];
        if (blnStartAt1) {
            Arrays.fill(aiQscales, 1);
        } else {
            MdecCode code = new MdecCode();
            for (int i = 0; i < _iBlockCount; i++) {
                readBlockQscaleAndDC(code, i);
                aiQscales[i] = code.getTop6Bits();
            }
        }

        return new Iterator<int[]>() {
            
            public boolean hasNext() { 
                int iMin = aiQscales[0];
                for (int i = 1; i < aiQscales.length; i++) {
                    if (aiQscales[i] < iMin)
                        iMin = aiQscales[i];
                }
                return iMin < 64;
            }

            public int[] next() {
                // TODO: finish iki encoding stuff
                /* maybe try encoding each macblk
                 * at different qscales
                 * and see how much it changes
                 * those that change a lot are weighted less than those that change a litte
                 *
                 */
                throw new UnsupportedOperationException("Not implemented yet");
            }

            public void remove() { throw new UnsupportedOperationException(); }
        };
    }



    public String toString() {
        // find the minimum and maximum quantization scales used
        int iMinQscale = 64, iMaxQscale = 0;
        MdecCode code = new MdecCode();
        for (int i = 0; i < _iBlockCount; i++) {
            readBlockQscaleAndDC(code, i);
            int iQscale = code.getTop6Bits();
            if (iQscale < iMinQscale)
                iMinQscale = iQscale;
            if(iQscale > iMaxQscale)
                iMaxQscale = iQscale;
        }
        return String.format("%s Qscale=%d-%d Offset=%d MB=%d.%d Mdec count=%d",
                getName(), iMinQscale, iMaxQscale,
                getWordPosition(),
                getCurrentMacroBlock(), getCurrentMacroBlockSubBlock(),
                getMdecCodeCount());
    }

    @Override
    public BitStreamCompressor_Iki makeCompressor() {
        throw new UnsupportedOperationException();
    }

    // =========================================================================

    public static class BitStreamCompressor_Iki extends BitstreamCompressor_STRv2 {
        private BitStreamCompressor_Iki() {}
    }

}
