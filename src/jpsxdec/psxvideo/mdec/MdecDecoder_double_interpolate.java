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

package jpsxdec.psxvideo.mdec;

import com.mortennobel.imagescaling.ResampleFilter;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import jpsxdec.formats.RGB;
import jpsxdec.psxvideo.PsxYCbCr;
import jpsxdec.psxvideo.mdec.idct.IDCT_double;

/** A full Java, double-precision, floating point implementation of the
 *  PlayStation 1 MDEC chip with interpolation used in chroma upsampling. 
 *<p>
 *  Default upsampling method is Bilinear.
 *<p>
 * To understand how that is helpful, read up on how 4:2:0 YCbCr format
 * works, and how it is then converted to RGB.
 */
public class MdecDecoder_double_interpolate extends MdecDecoder_double {

    /** Temp buffer for upsampled Cr. */
    private final double[] _adblUpCr;
    /** Temp buffer for upsampled Cb. */
    private final double[] _adblUpCb;

    private final ResampleOp _resampler;
    private Upsampler _upsampler = Upsampler.Bilinear;

    public enum Upsampler {
        /** i.e. Box */ NearestNeighbor(null),
        /** i.e. Triangle */ Bilinear(null),
        Bicubic(ResampleFilters.getBiCubicFilter()),
        Bell(ResampleFilters.getBellFilter()),
        Mitchell(ResampleFilters.getMitchellFilter()),
        BSpline(ResampleFilters.getBSplineFilter()),
        Lanczos3(ResampleFilters.getLanczos3Filter()),
        Hermite(ResampleFilters.getHermiteFilter());

        private final ResampleFilter _filter;
        private Upsampler(ResampleFilter filter) {
            _filter = filter;
        }
    }

    public MdecDecoder_double_interpolate(IDCT_double idct, int iWidth, int iHeight) {
        super(idct, iWidth, iHeight);

        _adblUpCb = new double[W * H];
        _adblUpCr = new double[W * H];

        _resampler = new ResampleOp();
        _resampler.setNumberOfThreads(1);
    }

    public void setResampler(Upsampler u) {
        _upsampler = u;
    }

    @Override
    public void readDecodedRgb(int iDestWidth, int iDestHeight, int[] aiDest,
                               int iOutStart, int iOutStride)
    {
        switch (_upsampler) {
            case NearestNeighbor:
                nearestNeighborUpsample(_CrBuffer, _adblUpCr);
                nearestNeighborUpsample(_CbBuffer, _adblUpCb);
                break;
            case Bilinear:
                bilinearUpsample(_CrBuffer, _adblUpCr);
                bilinearUpsample(_CbBuffer, _adblUpCb);
                break;
            default:
                _resampler.setFilter(_upsampler._filter);
                _resampler.doFilter(_CrBuffer, CW, CH, _adblUpCr);
                _resampler.doFilter(_CbBuffer, CW, CH, _adblUpCb);
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
                y = _LumaBuffer[iSrcOfs];
                cb = _adblUpCb[iSrcOfs];
                cr = _adblUpCr[iSrcOfs];
                PsxYCbCr.toRgb(y, cb, cr, rgb);
                aiDest[iDestOfs] = rgb.toInt();
            }
        }
    }

    private void nearestNeighborUpsample(double[] in, double[] out) {
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

    private void bilinearUpsample(double[] in, double[] out) {
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
