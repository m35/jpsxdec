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

package jpsxdec.psxvideo.mdec;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.RGB;
import jpsxdec.psxvideo.PsxYCbCr_int;
import jpsxdec.psxvideo.mdec.idct.IDCT_int;

/** A full Java, integer based implementation of the PlayStation 1 MDEC chip.
 * This may not be as precise as the double-based implementation, but on cursory
 * examination, you can't really tell. It's also significantly faster.
 *<p>
 * WARNING: This class was not designed to be thread safe. Create a
 * separate instance of this class for each thread, or wrap its use with
 * synchronize. */
public class MdecDecoder_int extends MdecDecoder {

    private static final Logger log = Logger.getLogger(MdecDecoder_int.class.getName());
    protected Logger log() { return log; }

    protected final IDCT_int _idct;

    protected final int[] _CrBuffer;
    protected final int[] _CbBuffer;
    protected final int[] _LumaBuffer;

    protected final int[] _CurrentBlock = new int[64];
    protected final MdecInputStream.MdecCode _code = new MdecInputStream.MdecCode();

    protected final int _iMacBlockWidth;
    protected final int _iMacBlockHeight;

    /** Luma dimensions. */
    protected final int W, H;
    /** Chroma dimensions. */
    protected final int CW, CH;

    protected final int[] _aiLumaBlkOfsLookup;
    protected final int[] _aiChromaMacBlkOfsLookup;

    protected final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();

    public MdecDecoder_int(IDCT_int idct, int iWidth, int iHeight) {
        _idct = idct;
        
        _iMacBlockWidth = (iWidth + 15) / 16;
        _iMacBlockHeight = (iHeight + 15) / 16;
        W = _iMacBlockWidth * 16;
        H = _iMacBlockHeight * 16;
        CW = _iMacBlockWidth * 8;
        CH = _iMacBlockHeight * 8;
        _CrBuffer = new int[ CW*CH];
        _CbBuffer = new int[ _CrBuffer.length];
        _LumaBuffer = new int[ W*H];

        _aiChromaMacBlkOfsLookup = new int[_iMacBlockWidth * _iMacBlockHeight];
        _aiLumaBlkOfsLookup = new int[_iMacBlockWidth * _iMacBlockHeight * 4];

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
    }

    public void decode(MdecInputStream mdecInStream)
            throws DecodingException
    {

        int iCurrentBlockQscale;
        int iCurrentBlockVectorPosition;
        int iCurrentBlockNonZeroCount;
        int iCurrentBlockLastNonZeroPosition;

        int iMacBlk = 0, iBlock = 0;

        try {

            // decode all the macro blocks of the image
            for (int iMacBlkX = 0; iMacBlkX < _iMacBlockWidth; iMacBlkX ++)
            {
                for (int iMacBlkY = 0; iMacBlkY < _iMacBlockHeight; iMacBlkY ++)
                {
                    // debug
                    if (log().isLoggable(Level.FINEST))
                        log().finest(String.format("Decoding macro block %d (%d, %d)", iMacBlk, iMacBlkX, iMacBlkY));

                    //System.out.println(String.format("Uncompressing macro block %d (%d, %d)", iMacBlk, iMacBlkX, iMacBlkY));

                    for (iBlock = 0; iBlock < 6; iBlock++) {
                        Arrays.fill(_CurrentBlock, 0);
                        mdecInStream.readMdecCode(_code);

                        if (log().isLoggable(Level.FINEST))
                            log().finest("Qscale & DC " + _code);

                        if (_code.getBottom10Bits() != 0) {
                            _CurrentBlock[0] =
                                    _code.getBottom10Bits() * PSX_DEFAULT_QUANTIZATION_MATRIX[0];
                            iCurrentBlockNonZeroCount = 1;
                            iCurrentBlockLastNonZeroPosition = 0;
                        } else {
                            iCurrentBlockNonZeroCount = 0;
                            iCurrentBlockLastNonZeroPosition = -1;
                        }
                        iCurrentBlockQscale = _code.getTop6Bits();
                        iCurrentBlockVectorPosition = 0;

                        while (!mdecInStream.readMdecCode(_code)) {

                            if (log().isLoggable(Level.FINEST))
                                log().finest(_code.toString());

                            ////////////////////////////////////////////////////////
                            iCurrentBlockVectorPosition += _code.getTop6Bits() + 1;

                            int iRevZigZagPos;
                            try {
                                // Reverse Zig-Zag
                                iRevZigZagPos = MdecInputStream.REVERSE_ZIG_ZAG_LOOKUP_LIST[iCurrentBlockVectorPosition];
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                throw new DecodingException(String.format(
                                        "[MDEC] Run length out of bounds [%d] in macroblock %d (%d, %d) block %d",
                                        iCurrentBlockVectorPosition,
                                        iMacBlk, iMacBlkX, iMacBlkY, iBlock));
                            }
                            // Dequantize
                            _CurrentBlock[iRevZigZagPos] =
                                        (_code.getBottom10Bits()
                                      * PSX_DEFAULT_QUANTIZATION_MATRIX[iRevZigZagPos]
                                      * iCurrentBlockQscale + 4) >> 3;
                            //  i      >> 3  ==  (int)Math.floor(i / 8.0)
                            // (i + 4) >> 3  ==  (int)Math.round(i / 8.0)
                            iCurrentBlockNonZeroCount++;
                            iCurrentBlockLastNonZeroPosition = iRevZigZagPos;
                            ////////////////////////////////////////////////////////
                        }

                        if (log().isLoggable(Level.FINEST))
                            log().finest(_code.toString());

                        writeEndOfBlock(iMacBlk, iBlock,
                                iCurrentBlockNonZeroCount,
                                iCurrentBlockLastNonZeroPosition);
                    }

                    iMacBlk++;
                }
            }
        } catch (Throwable ex) {
            // fill in the remaining data with zeros
            int iTotalMacBlks = _iMacBlockWidth * _iMacBlockHeight;
            for (; iMacBlk < iTotalMacBlks; iMacBlk++) {
                for (; iBlock < 6; iBlock++) {
                    writeEndOfBlock(iMacBlk, iBlock, 0, 0);
                }
                iBlock = 0;
            }
            if (ex instanceof DecodingException) {
                throw (DecodingException)ex;
            } else {
                throw new DecodingException("Error decoding macro block " + iMacBlk + " block " + iBlock, ex);
            }
        }
    }


    private void writeEndOfBlock(int iMacroBlock, int iBlock,
                                 int iNonZeroCount, int iNonZeroPos)
    {

        if (log().isLoggable(Level.FINEST)) {
            log().finest("Pre-IDCT block");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.setLength(0);
                sb.append("[ ");
                for (int j = 0; j < 8; j++) {
                    sb.append(String.format( "%d, ", _CurrentBlock[j+i*8]));
                }
                sb.append("]");
                log().finest(sb.toString());
            }
        }

        int[] outputBuffer;
        int iOutOffset, iOutWidth;
        switch (iBlock) {
            case 0:
                outputBuffer = _CrBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            case 1:
                outputBuffer = _CbBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            default:
                outputBuffer = _LumaBuffer;
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

        if (log().isLoggable(Level.FINEST)) {
            log().finest("Post-IDCT block");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.setLength(0);
                sb.append("[ ");
                for (int j = 0; j < 8; j++) {
                    if (iNonZeroCount == 0)
                        sb.append("0, ");
                    else
                        sb.append(String.format( "%d, ", _CurrentBlock[j+i*8]));
                }
                sb.append("]");
                log().finest(sb.toString());
            }
        }
    }

    public void readDecodedRgb(int iDestWidth, int iDestHeight, int[] aiDest,
                               int iOutStart, int iOutStride)
    {
        if ((iDestWidth % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((iDestHeight % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr_int psxycc = new PsxYCbCr_int();
        final RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        final int W_x2 = W*2, iOutStride_x2 = iOutStride*2;
        
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0,
            iDestLineOfsStart = iOutStart;
        for (int iY=0; iY < iDestHeight;
             iY+=2,
             iLumaLineOfsStart+=W_x2, iChromaLineOfsStart+=CW,
             iDestLineOfsStart+=iOutStride_x2)
        {
            int iSrcLumaOfs1 = iLumaLineOfsStart,
                iSrcLumaOfs2 = iLumaLineOfsStart + W,
                iSrcChromaOfs = iChromaLineOfsStart,
                iDestOfs1 = iDestLineOfsStart,
                iDestOfs2 = iDestLineOfsStart + iOutStride;
            for (int iX=0;
                 iX < iDestWidth;
                 iX+=2, iSrcChromaOfs++)
            {
                psxycc.cr = _CrBuffer[iSrcChromaOfs];
                psxycc.cb = _CbBuffer[iSrcChromaOfs];

                psxycc.y1 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y2 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _LumaBuffer[iSrcLumaOfs2++];
                psxycc.y4 = _LumaBuffer[iSrcLumaOfs2++];

                psxycc.toRgb(rgb1, rgb2, rgb3, rgb4);

                aiDest[iDestOfs1++] = rgb1.toInt();
                aiDest[iDestOfs1++] = rgb2.toInt();
                aiDest[iDestOfs2++] = rgb3.toInt();
                aiDest[iDestOfs2++] = rgb4.toInt();
            }
        }
    }

}
