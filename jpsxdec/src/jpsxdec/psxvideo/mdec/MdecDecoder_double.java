/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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
import jpsxdec.formats.RGB;
import jpsxdec.formats.Rec601YCbCr;
import jpsxdec.formats.YCbCrImage;
import jpsxdec.psxvideo.PsxYCbCr;
import jpsxdec.psxvideo.mdec.idct.IDCT_double;

/** A full Java, double-precision, floating point implementation of the
 *  PlayStation 1 MDEC chip. This implementation also comes with additional
 *  methods for higher quality YUV decoding. */
public class MdecDecoder_double extends MdecDecoder {

    protected final IDCT_double _idct;

    protected final double[] _CrBuffer;
    protected final double[] _CbBuffer;
    protected final double[] _LumaBuffer;

    /** Matrix of 8x8 coefficient values. */
    protected final double[] _CurrentBlock = new double[64];

    public MdecDecoder_double(IDCT_double idct, int iWidth, int iHeight) {
        super(iWidth, iHeight);
        _idct = idct;
        
        _CrBuffer = new double[ CW*CH];
        _CbBuffer = new double[ _CrBuffer.length];
        _LumaBuffer = new double[ W*H];
    }

    public void decode(MdecInputStream mdecInStream)
            throws MdecException.EndOfStream, MdecException.ReadCorruption
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
                    assert !DEBUG || debugPrintln(String.format("############### Decoding macro block %d (%d, %d) ###############",
                                                  iMacBlk, iMacBlkX, iMacBlkY));

                    for (iBlock = 0; iBlock < 6; iBlock++) {

                        assert !DEBUG || debugPrintln(String.format("=========== Decoding block %s ===========",
                                                      BLOCK_NAMES[iBlock]));
                        
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
                                throw new MdecException.ReadCorruption(MdecException.RLC_OOB_IN_BLOCK_NAME(
                                               iCurrentBlockVectorPosition,
                                               iMacBlk, iMacBlkX, iMacBlkY, iBlock, BLOCK_NAMES[iBlock]),
                                               ex);
                            }

                            if (_code.getBottom10Bits() != 0) {

                                assert !DEBUG || setPrequantValue(iRevZigZagMatrixPos, _code.getBottom10Bits());
                                // Dequantize
                                _CurrentBlock[iRevZigZagMatrixPos] =
                                            (_code.getBottom10Bits()
                                          * _aiQuantizationTable[iRevZigZagMatrixPos]
                                          * iCurrentBlockQscale) / 8.0;
                                iCurrentBlockNonZeroCount++;
                                iCurrentBlockLastNonZeroPosition = iRevZigZagMatrixPos;

                            }
                            ////////////////////////////////////////////////////////
                        }

                        assert !DEBUG || debugPrintln(_code.toString());

                        writeEndOfBlock(iMacBlk, iBlock,
                                iCurrentBlockNonZeroCount,
                                iCurrentBlockLastNonZeroPosition);
                    }

                    iMacBlk++;
                }
            }
        } finally {
            // in case an exception occured
            // fill in any remaining data with zeros
            int iTotalMacBlks = _iMacBlockWidth * _iMacBlockHeight;
            // pickup where decoding left off
            for (; iMacBlk < iTotalMacBlks; iMacBlk++) {
                for (; iBlock < 6; iBlock++) {
                    writeEndOfBlock(iMacBlk, iBlock, 0, 0);
                }
                iBlock = 0;
            }
        }
    }

    private boolean debugPrintBlock(String sMsg) {
        System.out.println(sMsg);
        for (int i = 0; i < 8; i++) {
            System.out.print("[ ");
            for (int j = 0; j < 8; j++) {
                System.out.format( "%1.3f, ", _CurrentBlock[j+i*8]);
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

        double[] outputBuffer;
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

        assert !DEBUG || debugPrintBlock("Post-IDCT block");

    }

    final static boolean YUV_TESTS = false;
    
    public void readDecodedRgb(int iDestWidth, int iDestHeight, int[] aiDest,
                               int iOutStart, int iOutStride)
    {
        if ((iDestWidth % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((iDestHeight % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final Rec601YCbCr rec601ycc = new Rec601YCbCr(); // for YUV_TESTS
        final PsxYCbCr psxycc = new PsxYCbCr();
        final RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        final int W_x2 = W*2, iOutStride_x2 = iOutStride*2;
        
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0,
            iDestLineOfsStart = iOutStart;
        for (int iY=0; iY < iDestHeight;
             iY+=2,
             iLumaLineOfsStart+=W_x2, iChromaLineOfsStart+=CW,
             iDestLineOfsStart+=iOutStride_x2)
        {
            // writes 2 lines at a time
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

                if (YUV_TESTS) {
                    System.err.println("###>>!! YUV_TEST CONVERTING TO Rec601 THEN TO RGB !!<<###");
                    psxycc.toRec_601_YCbCr(rec601ycc);
                    rec601ycc.toRgb(rgb1, rgb2, rgb3, rgb4);
                } else {
                    psxycc.toRgb(rgb1, rgb2, rgb3, rgb4);
                }

                aiDest[iDestOfs1++] = rgb1.toInt();
                aiDest[iDestOfs1++] = rgb2.toInt();
                aiDest[iDestOfs2++] = rgb3.toInt();
                aiDest[iDestOfs2++] = rgb4.toInt();
            }
        }
    }

    public void readDecoded_Rec601_YCbCr420(YCbCrImage ycc) {

        final int WIDTH = ycc.getWidth(), HEIGHT = ycc.getHeight();

        if ((WIDTH % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr psxycc = new PsxYCbCr();
        final Rec601YCbCr recycc = new Rec601YCbCr();

        final int W2 = W*2;
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0;
        for (int iY=0, iCY=0; iY < HEIGHT; iY+=2, iCY++, iLumaLineOfsStart+=W2, iChromaLineOfsStart+=CW) {
            int iSrcLumaOfs1 = iLumaLineOfsStart;
            int iSrcLumaOfs2 = iLumaLineOfsStart + W;
            int iSrcChromaOfs = iChromaLineOfsStart;
            for (int iX=0, iCX=0; iX < WIDTH; iX+=2, iCX++, iSrcChromaOfs++) {

                psxycc.cr = _CrBuffer[iSrcChromaOfs];
                psxycc.cb = _CbBuffer[iSrcChromaOfs];

                psxycc.y1 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _LumaBuffer[iSrcLumaOfs2++];
                psxycc.y2 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y4 = _LumaBuffer[iSrcLumaOfs2++];

                psxycc.toRec_601_YCbCr(recycc);

                ycc.setY( iX+0 , iY+0 , clamp(recycc.y1));
                ycc.setY( iX+1 , iY+0 , clamp(recycc.y2));
                ycc.setY( iX+0 , iY+1 , clamp(recycc.y3));
                ycc.setY( iX+1 , iY+1 , clamp(recycc.y4));
                ycc.setCb( iCX , iCY , clamp(recycc.cb));
                ycc.setCr( iCX , iCY , clamp(recycc.cr));

            }
        }
    }


    /**
     * @see PsxYCbCr#toRec_JFIF_YCbCr(jpsxdec.formats.Rec601YCbCr)
     */
    public void readDecoded_JFIF_YCbCr420(YCbCrImage ycc) {

        final int WIDTH = ycc.getWidth(), HEIGHT = ycc.getHeight();

        if ((WIDTH % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr psxycc = new PsxYCbCr();
        final Rec601YCbCr recycc = new Rec601YCbCr();

        final int W2 = W*2;
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0;
        for (int iY=0, iCY=0; iY < HEIGHT; iY+=2, iCY++, iLumaLineOfsStart+=W2, iChromaLineOfsStart+=CW) {
            int iSrcLumaOfs1 = iLumaLineOfsStart;
            int iSrcLumaOfs2 = iLumaLineOfsStart + W;
            int iSrcChromaOfs = iChromaLineOfsStart;
            for (int iX=0, iCX=0; iX < WIDTH; iX+=2, iCX++, iSrcChromaOfs++) {

                psxycc.cr = _CrBuffer[iSrcChromaOfs];
                psxycc.cb = _CbBuffer[iSrcChromaOfs];

                psxycc.y1 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _LumaBuffer[iSrcLumaOfs2++];
                psxycc.y2 = _LumaBuffer[iSrcLumaOfs1++];
                psxycc.y4 = _LumaBuffer[iSrcLumaOfs2++];

                psxycc.toRec_JFIF_YCbCr(recycc);

                ycc.setY( iX+0 , iY+0 , clamp(recycc.y1));
                ycc.setY( iX+1 , iY+0 , clamp(recycc.y2));
                ycc.setY( iX+0 , iY+1 , clamp(recycc.y3));
                ycc.setY( iX+1 , iY+1 , clamp(recycc.y4));
                ycc.setCb( iCX , iCY , clamp(recycc.cb));
                ycc.setCr( iCX , iCY , clamp(recycc.cr));

            }
        }
    }

    private static byte clamp(double dbl) {
        long lng = Math.round(dbl);
        if (lng < 0)
            return (byte)0;
        else if (lng > 255)
            return (byte)255;
        else
            return (byte)(lng);
    }


}
