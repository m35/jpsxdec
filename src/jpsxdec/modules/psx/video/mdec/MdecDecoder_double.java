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

package jpsxdec.modules.psx.video.mdec;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.RGB;
import jpsxdec.formats.Yuv4mpeg2;
import jpsxdec.formats.Rec601YCbCr;
import jpsxdec.modules.psx.video.PsxYCbCr;
import jpsxdec.modules.psx.video.mdec.idct.IDCT_double;

/** A full Java, double-precision, floating point implementation of the
 *  PlayStation 1 MDEC chip. This implementation also comes with additional
 *  methods for higher quality YUV decoding.
 * <p>
 * The data is decoded to internal buffers and stored sequentially in very
 * tall buffers, 8 pixels wide.
 * <pre>
 *   LuminBuffer   CbBuffer    CrBuffer
 *     +----+       +----+      +----+
 *     | Y1 |    #1 | Cb |   #1 | Cr |
 *     +----+   ----+----+  ----+----+
 *     | Y2 |    #2 | Cb |   #2 | Cr |
 *  #1 +----+   ----+----+  ----+----+
 *     | Y3 |       |    |      |    |
 *     +----+        ...         ...
 *     | Y4 |
 * ----+----+
 *     | Y1 |
 *     +----+
 *     | Y2 |
 *  #2 +----+
 *     | Y3 |
 *     +----+
 *     | Y4 |
 *     +----+
 *     |    |
 *      ...
 *</pre>
 * The data can then be read into various image formats.
 *<p>
 * WARNING: This class was not designed to be thread safe. Create a
 * separate instance of this class for each thread, or wrap its use with
 * syncronize. */
public class MdecDecoder_double extends MdecDecoder {

    private static final Logger log = Logger.getLogger(MdecDecoder_double.class.getName());
    protected Logger log() { return log; }

    protected final IDCT_double _idct;

    protected final double[] _CrBuffer;
    protected final double[] _CbBuffer;
    protected final double[] _LuminBuffer;

    protected final double[] _CurrentBlock = new double[64];
    protected final MdecInputStream.MdecCode _code = new MdecInputStream.MdecCode();

    protected int _iMacBlockWidth;
    protected int _iMacBlockHeight;

    protected final int[] PSX_DEFAULT_QUANTIZATION_MATRIX =
            MdecInputStream.getDefaultPsxQuantMatrixCopy();

    public MdecDecoder_double(IDCT_double idct, int iWidth, int iHeight) {
        _idct = idct;
        

        _iMacBlockWidth = (iWidth + 15) / 16;
        _iMacBlockHeight = (iHeight + 15) / 16;
        _CrBuffer = new double[ _iMacBlockWidth * _iMacBlockHeight * 64];
        _CbBuffer = new double[ _CrBuffer.length];
        _LuminBuffer = new double[ _iMacBlockWidth * _iMacBlockHeight * 256];
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
                                      * iCurrentBlockQscale) / 8.0;
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
                    sb.append(String.format( "%1.3f, ", _CurrentBlock[j+i*8]));
                }
                sb.append("]");
                log().finest(sb.toString());
            }
        }


        double[] outputBuffer;
        int iOffset;
        switch (iBlock) {
            case 0:
                outputBuffer = _CrBuffer;
                iOffset = iMacroBlock * 64;
                break;
            case 1:
                outputBuffer = _CbBuffer;
                iOffset = iMacroBlock * 64;
                break;
            default:
                outputBuffer = _LuminBuffer;
                iOffset = iMacroBlock * 256 + (iBlock - 2) * 64;
        }
        if (iNonZeroCount == 0) {
            Arrays.fill(outputBuffer, iOffset, iOffset + 64, 0);
        } else if (iNonZeroCount == 1) {
            _idct.IDCT_1NonZero(_CurrentBlock, iNonZeroPos,
                                iOffset, outputBuffer);
        } else {
            _idct.IDCT(_CurrentBlock, iOffset, outputBuffer);
        }

        if (log().isLoggable(Level.FINEST)) {
            log().finest("Post-IDCT block");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.setLength(0);
                sb.append("[ ");
                for (int j = 0; j < 8; j++) {
                    sb.append(String.format( "%1.3f, ", outputBuffer[iOffset+ j+i*8]));
                }
                sb.append("]");
                log().finest(sb.toString());
            }
        }
    }

    final static boolean YUV_TESTS = false;

    public void readDecodedRgb(RgbIntImage rgbImg) {

        final int WIDTH = rgbImg.getWidth(), HEIGHT = rgbImg.getHeight();

        if ((WIDTH % 16) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 16.");
        // TODO: add handling for widths not divisible by 16
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final Rec601YCbCr y4mycc = new Rec601YCbCr(); // for YUV_TESTS
        final PsxYCbCr psxycc = new PsxYCbCr();
        final RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        int iChromOfs, iLuminOfs, iBlockHeight;
        for (int iX = 0, iXblk = 0; iX < WIDTH; iX+=16, iXblk += _iMacBlockHeight) {
            iLuminOfs = (8*8*4) * iXblk;
            iChromOfs = (8*8) * iXblk;
            for (int iY = 0; iY < HEIGHT; iY+=16) {

                if (iY + 16 > HEIGHT)
                    iBlockHeight = HEIGHT - iY;
                else
                    iBlockHeight = 16;

                for (int iCy = 0; iCy < iBlockHeight; iCy+=2) {
                    for (int iCx = 0; iCx < 16; iCx+=2) {

                        LuminSubSampleIndexes aiLuminIdxs = LUMIN_SUBSAMPLING_SEQUENCE[iChromOfs & 63];

                        psxycc.y1 = _LuminBuffer[iLuminOfs + aiLuminIdxs.TL];
                        psxycc.y2 = _LuminBuffer[iLuminOfs + aiLuminIdxs.TR];
                        psxycc.y3 = _LuminBuffer[iLuminOfs + aiLuminIdxs.BL];
                        psxycc.y4 = _LuminBuffer[iLuminOfs + aiLuminIdxs.BR];
                        psxycc.cr = _CrBuffer[iChromOfs];
                        psxycc.cb = _CbBuffer[iChromOfs];

                        if (YUV_TESTS) {
                            System.err.println("###>>!! YUV_TEST CONVERTING TO Rec601 THEN TO RGB !!<<###");
                            psxycc.toRec601YCbCr(y4mycc);
                            y4mycc.toRgb(rgb1, rgb2, rgb3, rgb4);
                        } else {
                            psxycc.toRgb(rgb1, rgb2, rgb3, rgb4);
                        }

                        rgbImg.set( iX+iCx+0 , iY+iCy+0 , rgb1.toInt());
                        rgbImg.set( iX+iCx+1 , iY+iCy+0 , rgb2.toInt());
                        rgbImg.set( iX+iCx+0 , iY+iCy+1 , rgb3.toInt());
                        rgbImg.set( iX+iCx+1 , iY+iCy+1 , rgb4.toInt());

                        iChromOfs++;
                    }
                }

                iLuminOfs += 8*8*4;
            }
        }
    }

    public void readDecodedYuv4mpeg2(Yuv4mpeg2 yuv) {

        final int WIDTH = yuv.getWidth(), HEIGHT = yuv.getHeight();

        if ((WIDTH % 16) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 16.");
        // TODO: add handling for widths not divisible by 16
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr psxycc = new PsxYCbCr();
        final Rec601YCbCr y4mycc = new Rec601YCbCr();

        int iChromOfs, iLuminOfs, iBlockHeight;
        for (int iX = 0, iXblk = 0; iX < WIDTH; iX+=16, iXblk++) {
            iLuminOfs = (_iMacBlockHeight * 8*8*4) * iXblk;
            iChromOfs = (_iMacBlockHeight * 8*8) * iXblk;
            for (int iY = 0; iY < HEIGHT; iY+=16) {

                if (iY + 16 > HEIGHT)
                    iBlockHeight = HEIGHT - iY;
                else
                    iBlockHeight = 16;

                for (int iCy = 0; iCy < iBlockHeight; iCy+=2) {
                    for (int iCx = 0; iCx < 16; iCx+=2) {

                        LuminSubSampleIndexes aiLuminIdxs = LUMIN_SUBSAMPLING_SEQUENCE[iChromOfs & 63];

                        psxycc.y1 = _LuminBuffer[iLuminOfs + aiLuminIdxs.TL];
                        psxycc.y2 = _LuminBuffer[iLuminOfs + aiLuminIdxs.TR];
                        psxycc.y3 = _LuminBuffer[iLuminOfs + aiLuminIdxs.BL];
                        psxycc.y4 = _LuminBuffer[iLuminOfs + aiLuminIdxs.BR];
                        psxycc.cr = _CrBuffer[iChromOfs];
                        psxycc.cb = _CbBuffer[iChromOfs];

                        if (YUV_TESTS) {
                            System.err.println("###>>!! YUV_TEST WRITING PSX YCbCr VALUES DIRECTLY !!<<###");
                            y4mycc.y1 = psxycc.y1;
                            y4mycc.y2 = psxycc.y2;
                            y4mycc.y3 = psxycc.y3;
                            y4mycc.y4 = psxycc.y4;
                            y4mycc.cb = psxycc.cb;
                            y4mycc.cr = psxycc.cr;
                        } else {
                            psxycc.toRec601YCbCr(y4mycc);
                        }

                        yuv.setY( iX+iCx+0 , iY+iCy+0 , clamp(y4mycc.y1));
                        yuv.setY( iX+iCx+1 , iY+iCy+0 , clamp(y4mycc.y2));
                        yuv.setY( iX+iCx+0 , iY+iCy+1 , clamp(y4mycc.y3));
                        yuv.setY( iX+iCx+1 , iY+iCy+1 , clamp(y4mycc.y4));
                        yuv.setCb( (iX+iCx)/2 , (iY+iCy)/2 , clamp(y4mycc.cb));
                        yuv.setCr( (iX+iCx)/2 , (iY+iCy)/2 , clamp(y4mycc.cr));

                        iChromOfs++;
                    }
                }

                iLuminOfs += 8*8*4;
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
