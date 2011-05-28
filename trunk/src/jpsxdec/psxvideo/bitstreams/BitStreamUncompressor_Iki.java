package jpsxdec.psxvideo.bitstreams;

/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import java.io.EOFException;
import java.io.File;
import javax.imageio.ImageIO;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.psxvideo.mdec.DecodingException;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


public class BitStreamUncompressor_Iki extends BitStreamUncompressor_STRv2 {


    public final static String FRAME = "0100";

    public static void main(String[] args) throws Exception {
        BitStreamUncompressor_Iki drag = new BitStreamUncompressor_Iki();
        byte[] abFile = IO.readFile("STR\\DEMOH[0]_320x192["+FRAME+"].bs");
        drag.reset(abFile);
        IO.writeFile(FRAME+"-j.lzs", drag._abQscaleDcLookupTable);
        MdecDecoder_double decoder = new MdecDecoder_double(new PsxMdecIDCT_double(), drag._iWidth, drag._iHeight);
        decoder.decode(drag);

        RgbIntImage rgb = new RgbIntImage(drag._iWidth, drag._iHeight);
        decoder.readDecodedRgb(drag._iWidth, drag._iHeight, rgb.getData());
        ImageIO.write(rgb.toBufferedImage(), "png", new File(FRAME+"-j.png"));
    }

    private int _iWidth, _iHeight, _iMdecCodeCount, _iCompressedDataSize, _iLookupTableHalfSize;
    private byte[] _abQscaleDcLookupTable;
    


    @Override
    public int getChromQscale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLuminQscale() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void readHeader(byte[] abFrameData, int iStart, ArrayBitReader bitReader) throws NotThisTypeException {
        _iMdecCodeCount = IO.readUInt16LE(abFrameData, 0);
        int i3800 = IO.readUInt16LE(abFrameData, 2);
        _iWidth = IO.readSInt16LE(abFrameData, 4);
        _iHeight = IO.readSInt16LE(abFrameData, 6);
        _iCompressedDataSize = IO.readUInt16LE(abFrameData, 8);

        if (_iMdecCodeCount < 0 || i3800 != 0x3800 || _iWidth < 1 || _iHeight < 1 || _iCompressedDataSize < 1)
            throw new NotThisTypeException();

        int iMacroBlockCount = ((_iWidth + 15) / 16) * ((_iHeight + 15) / 16);
        int iQscaleDcLookupTableSize = iMacroBlockCount * 6 /* blocks/macroblock */ * 2 /* bytes per block */;

        if (_abQscaleDcLookupTable == null || _abQscaleDcLookupTable.length < iQscaleDcLookupTableSize)
            _abQscaleDcLookupTable = new byte[iQscaleDcLookupTableSize];

        ikiLzsUncompress(abFrameData, 10, _abQscaleDcLookupTable, iQscaleDcLookupTableSize);

        _iLookupTableHalfSize = iQscaleDcLookupTableSize / 2;

        bitReader.reset(abFrameData, true, 10 + _iCompressedDataSize);
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int _iMdecCodeCount = IO.readUInt16LE(abFrameData, 0);
        int i3800 = IO.readUInt16LE(abFrameData, 2);
        int _iWidth = IO.readSInt16LE(abFrameData, 4);
        int _iHeight = IO.readSInt16LE(abFrameData, 6);
        int _iCompressedDataSize = IO.readUInt16LE(abFrameData, 8);

        return !(_iMdecCodeCount < 0 || i3800 != 0x3800 || _iWidth < 1 || _iHeight < 1 || _iCompressedDataSize < 1);
    }

    @Override
    protected void readQscaleDC(MdecCode code) throws EOFException, DecodingException {
        int iCurrentBlock = getCurrentBlock();
        int b1 = _abQscaleDcLookupTable[iCurrentBlock] & 0xff;
        int b2 = _abQscaleDcLookupTable[iCurrentBlock+_iLookupTableHalfSize] & 0xff;
        code.set((b1 << 8) | b2);
    }

    public static final boolean DEBUG = false;

    /** .iki videos utilize yet another LZSS compression format that is
     * different from both FF7 and Lain.
     */
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

}
