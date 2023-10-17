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

import com.mortennobel.imagescaling.ResampleOp;
import java.util.Arrays;
import javax.annotation.Nonnull;
import jpsxdec.formats.Pc601YCbCr;
import jpsxdec.formats.Pc601YCbCrImage;
import jpsxdec.formats.RGB;
import jpsxdec.formats.Rec601YCbCr;
import jpsxdec.formats.Rec601YCbCrImage;
import jpsxdec.psxvideo.PsxYCbCr;
import jpsxdec.psxvideo.mdec.idct.IDCT_double;

/** A full Java, double-precision, floating point implementation of the
 *  PlayStation 1 MDEC chip with interpolation used in chroma upsampling.
 *<p>
 *  Default upsampling method is Bicubic.
 *<p>
 * This implementation also comes with additional methods for higher
 * quality YUV decoding. */
public class MdecDecoder_double extends MdecDecoder {

    @Nonnull
    private final IDCT_double _idct;

    @Nonnull
    private final double[] _adblDecodedCrBuffer;
    @Nonnull
    private final double[] _dblDecodedCbBuffer;
    @Nonnull
    private final double[] _adblDecodedLumaBuffer;

    /** Matrix of 8x8 coefficient values. */
    private final double[] _CurrentBlock = new double[64];

    /** Temp buffer for upsampled Cr. */
    @Nonnull
    private final double[] _adblTempUpsampledCr;
    /** Temp buffer for upsampled Cb. */
    @Nonnull
    private final double[] _adblTempUpsampledCb;

    private final ResampleOp _resampler = new ResampleOp();
    @Nonnull
    private ChromaUpsample _upsampler = ChromaUpsample.Bicubic;

    public MdecDecoder_double(@Nonnull IDCT_double idct, int iWidth, int iHeight) {
        super(iWidth, iHeight);
        _idct = idct;

        _adblDecodedCrBuffer = new double[CW * CH];
        _dblDecodedCbBuffer = new double[_adblDecodedCrBuffer.length];
        _adblDecodedLumaBuffer = new double[W * H];

        _adblTempUpsampledCb = new double[_adblDecodedLumaBuffer.length];
        _adblTempUpsampledCr = new double[_adblDecodedLumaBuffer.length];

        _resampler.setNumberOfThreads(1);
    }

    public void setUpsampler(@Nonnull ChromaUpsample u) {
        _upsampler = u;
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
                            throw new MdecException.ReadCorruption(MdecException.RLC_OOB_IN_BLOCK_NAME(
                                           iCurrentBlockVectorPosition,
                                           context.getTotalMacroBlocksRead(), macBlkXY.x, macBlkXY.y, context.getCurrentBlock().ordinal(), context.getCurrentBlock().name()),
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
                outputBuffer = _adblDecodedCrBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            case 1:
                outputBuffer = _dblDecodedCbBuffer;
                iOutOffset = _aiChromaMacBlkOfsLookup[iMacroBlock];
                iOutWidth = CW;
                break;
            default:
                outputBuffer = _adblDecodedLumaBuffer;
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

    @Override
    public void readDecodedRgb(int iDestWidth, int iDestHeight, @Nonnull int[] aiDest,
                               int iOutStart, int iOutStride)
    {
        switch (_upsampler) {
            case NearestNeighbor:
                nearestNeighborUpsample(_adblDecodedCrBuffer, _adblTempUpsampledCr);
                nearestNeighborUpsample(_dblDecodedCbBuffer, _adblTempUpsampledCb);
                break;
            case Bilinear:
                bilinearUpsample(_adblDecodedCrBuffer, _adblTempUpsampledCr);
                bilinearUpsample(_dblDecodedCbBuffer, _adblTempUpsampledCb);
                break;
            default:
                _resampler.setFilter(_upsampler._filter);
                _resampler.doFilter(_adblDecodedCrBuffer, CW, CH, _adblTempUpsampledCr);
                _resampler.doFilter(_dblDecodedCbBuffer, CW, CH, _adblTempUpsampledCb);
        }

        RGB rgb = new RGB();
        double y, cb, cr;

        for (int iY = 0, iSrcLineOfsStart=0, iDestLineOfsStart=iOutStart;
             iY < iDestHeight;
             iY++, iSrcLineOfsStart+=W, iDestLineOfsStart+=iOutStride)
        {
            for (int iX=0, iSrcOfs=iSrcLineOfsStart, iDestOfs=iDestLineOfsStart;
                 iX < iDestWidth;
                 iX++, iSrcOfs++, iDestOfs++)
            {
                y = _adblDecodedLumaBuffer[iSrcOfs];
                cb = _adblTempUpsampledCb[iSrcOfs];
                cr = _adblTempUpsampledCr[iSrcOfs];

                if (YUV_TESTS) {
                    System.err.println("###>>!! YUV_TEST CONVERTING TO Rec601 THEN TO RGB !!<<###");
                    Rec601YCbCr.toRgb(y, cb, cr, rgb);
                } else {
                    PsxYCbCr.toRgb(y, cb, cr, rgb);
                }

                aiDest[iDestOfs] = rgb.toInt();
            }
        }
    }

    public void readDecoded_Rec601_YCbCr420(@Nonnull Rec601YCbCrImage ycc) {

        final int WIDTH = ycc.getWidth(), HEIGHT = ycc.getHeight();

        if ((WIDTH % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr psxycc = new PsxYCbCr();
        final Rec601YCbCr recycc = new Rec601YCbCr();

        final int W2 = W*2;
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0;
        for (int iY=0; iY < HEIGHT; iY+=2, iLumaLineOfsStart+=W2, iChromaLineOfsStart+=CW) {
            int iSrcLumaOfs1 = iLumaLineOfsStart;
            int iSrcLumaOfs2 = iLumaLineOfsStart + W;
            int iSrcChromaOfs = iChromaLineOfsStart;
            for (int iX=0; iX < WIDTH; iX+=2, iSrcChromaOfs++) {

                psxycc.cr = _adblDecodedCrBuffer[iSrcChromaOfs];
                psxycc.cb = _dblDecodedCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _adblDecodedLumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _adblDecodedLumaBuffer[iSrcLumaOfs2++];
                psxycc.y2 = _adblDecodedLumaBuffer[iSrcLumaOfs1++];
                psxycc.y4 = _adblDecodedLumaBuffer[iSrcLumaOfs2++];

                psxycc.toRec_601_YCbCr(recycc);

                ycc.setYCbCr(iX, iY, recycc);
            }
        }
    }


    public void readDecoded_JFIF_YCbCr420(@Nonnull Pc601YCbCrImage ycc) {

        final int WIDTH = ycc.getWidth(), HEIGHT = ycc.getHeight();

        if ((WIDTH % 2) != 0)
            throw new IllegalArgumentException("Image width must be multiple of 2.");
        if ((HEIGHT % 2) != 0)
            throw new IllegalArgumentException("Image height must be multiple of 2.");

        final PsxYCbCr psxycc = new PsxYCbCr();
        final Pc601YCbCr pcycc = new Pc601YCbCr();

        final int W2 = W*2;
        int iLumaLineOfsStart = 0, iChromaLineOfsStart = 0;
        for (int iY=0; iY < HEIGHT; iY+=2, iLumaLineOfsStart+=W2, iChromaLineOfsStart+=CW) {
            int iSrcLumaOfs1 = iLumaLineOfsStart;
            int iSrcLumaOfs2 = iLumaLineOfsStart + W;
            int iSrcChromaOfs = iChromaLineOfsStart;
            for (int iX=0; iX < WIDTH; iX+=2, iSrcChromaOfs++) {

                psxycc.cr = _adblDecodedCrBuffer[iSrcChromaOfs];
                psxycc.cb = _dblDecodedCbBuffer[iSrcChromaOfs];

                psxycc.y1 = _adblDecodedLumaBuffer[iSrcLumaOfs1++];
                psxycc.y3 = _adblDecodedLumaBuffer[iSrcLumaOfs2++];
                psxycc.y2 = _adblDecodedLumaBuffer[iSrcLumaOfs1++];
                psxycc.y4 = _adblDecodedLumaBuffer[iSrcLumaOfs2++];

                psxycc.toRec_JFIF_YCbCr(pcycc);

                ycc.setYCbCr(iX, iY, pcycc);
            }
        }
    }


    private void nearestNeighborUpsample(@Nonnull double[] in, @Nonnull double[] out) {
        int outOfs = 0;
        int inOfs = 0;
        for (int inY=0; inY < CH; inY++) {
            // copy a line, scaling horizontally
            for (int inX=0; inX < CW; inX++) {
                out[outOfs++] = in[inOfs];
                out[outOfs++] = in[inOfs];
                inOfs++;
            }
            // duplicate that horizontally scaled line, thus scaling it vertically
            System.arraycopy(out, outOfs-W, out, outOfs, W);
            outOfs += W;
        }
    }

    private void bilinearUpsample(@Nonnull double[] in, @Nonnull double[] out) {
        // corners
        out[0   +  0   *W] = in[0    +  0    *CW];
        out[W-1 +  0   *W] = in[CW-1 +  0    *CW];
        out[0   + (H-1)*W] = in[0    + (CH-1)*CW];
        out[W-1 + (H-1)*W] = in[CW-1 + (CH-1)*CW];

        // vertical edges
        for (int i = 0; i < 2; i++) {
            int inX, outX;
            if (i == 0) {
                outX = 0;
                inX = 0;
            } else {
                outX = W - 1;
                inX = CW - 1;
            }
            for (int inY = 0; inY < CH-1; inY++) {
                double c1 = in[inX +  inY   *CW],
                       c2 = in[inX + (inY+1)*CW];
                int outY = 1 + inY*2;
                out[outX +  outY   *W] = c1 * 0.75 + c2 * 0.25;
                out[outX + (outY+1)*W] = c1 * 0.25 + c2 * 0.75;
            }
        }

        // horizontal edges
        for (int i = 0; i < 2; i++) {
            int inY, outY;
            if (i == 0) {
                outY = 0;
                inY = 0;
            } else {
                outY = H - 1;
                inY = CH - 1;
            }
            for (int inX = 0; inX < CW-1; inX++) {
                double c1 = in[inX   + inY*CW],
                       c2 = in[inX+1 + inY*CW];
                int outX = 1+ inX*2;
                out[outX   + outY*W] = c1 * 0.75 + c2 * 0.25;
                out[outX+1 + outY*W] = c1 * 0.25 + c2 * 0.75;
            }
        }

        // the meat in the middle
        for (int inY=0; inY < CH-1; inY++) {
            int inOfs = inY*CW;
            int outOfs = ((inY*2)+1)*W + 1;
            double c1, c2 = in[inOfs],
                   c3, c4 = in[inOfs+CW];
            inOfs++;
            for (int inX=0; inX < CW-1; inX++, inOfs++) {
                c1 = c2; c2 = in[inOfs];
                c3 = c4; c4 = in[inOfs+CW];
                double c1_c4_mul_3_16 = (c1 + c4) * (3. / 16.),
                       c2_c3_mul_3_16 = (c2 + c3) * (3. / 16.);
                out[outOfs  ]= c1 * (9. / 16.) + c2_c3_mul_3_16 + c4 * (1. / 16.);
                out[outOfs+W]= c1_c4_mul_3_16 + c2 * (1. / 16.) + c3 * (9. / 16.);
                outOfs++;
                out[outOfs  ]= c1_c4_mul_3_16 + c2 * (9. / 16.) + c3 * (1. / 16.);
                out[outOfs+W]= c1 * (1. / 16.) + c2_c3_mul_3_16 + c4 * (9. / 16.);
                outOfs++;
            }
        }

    }



}
