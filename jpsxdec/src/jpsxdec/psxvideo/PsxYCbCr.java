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

package jpsxdec.psxvideo;

import javax.annotation.Nonnull;
import jpsxdec.formats.Pc601YCbCr;
import jpsxdec.formats.RGB;
import jpsxdec.formats.Rec601YCbCr;


/** Holds YCbCr image color from the PSX MDEC chip.
 *<p>
 * The MDEC chip sub-samples the image data 4:2:0, which is the same way MPEG1
 * and JPEG sample the color information. To keep things simpler,
 * four luma samples, and one chroma sub-sample are stored in this
 * class.
 *<p>
 * Technically YCbCr does not have a "color space," but I will use the term
 * here to refer to the range of valid values.
 *<p>
 * There are 3 different YCbCr "color spaces" to be aware of:
 *
 * <h5>The Rec.601 YCbCr color space</h5>
 * This is the standard colors space defined for most signals, including the
 * most common uncompressed YCbCr fourcc codec: YV12. It has a range of
 *<pre>
 * Y : 16 to 235
 * Cb: 16 to 240
 * Cr: 16 to 240
 *</pre>
 * You can easily find the equation to convert this YCbCr color space to RGB.
 *
 * <h5>The <b>JFIF</b> Rec.601 YCbCr color space</h5>
 * This is the color space used by the JPEG (JFIF) format.
 * It has a range of
 *<pre>
 * Y : 0 to 255
 * Cb: 0 to 255
 * Cr: 0 to 255
 *</pre>
 * You can check the JFIF standard for the equation to turn this
 * YCbCr color space to RGB (or just search around the web).

 * <h5>The PSX MDEC YCbCr color space</h5>
 * This is very similar to the JFIF Rec.601 YCbCr color space, but has a
 * slightly different equation to convert to RGB. It shares the same
 * range as the JFIF YCbCr color space, but for this implementation, I found
 * it easier to center the color space at 0. As such, this class holds its
 * color information as doubles with the range of
 *<pre>
 * Y : -128 to 127
 * Cb: -128 to 127
 * Cr: -128 to 127
 *</pre>
 *
 * @see Rec601YCbCr
 * @see Pc601YCbCr
 * @see PsxYCbCr_int
 */
public class PsxYCbCr {

    public double y1, y2, y3, y4, cb, cr;

    public void fromRgb(@Nonnull RGB rgb1, @Nonnull RGB rgb2, @Nonnull RGB rgb3, @Nonnull RGB rgb4) {
        cb = cr = 0;
        y1 = oneRgb(rgb1);
        y2 = oneRgb(rgb2);
        y3 = oneRgb(rgb3);
        y4 = oneRgb(rgb4);
        cb /= 4.0;
        cr /= 4.0;
    }
    private double oneRgb(@Nonnull RGB rgb) {
        int r_128 = rgb.getR() - 128;
        int g_128 = rgb.getG() - 128;
        int b_128 = rgb.getB() - 128;
        double y = r_128 *  0.299   + g_128 *  0.587   + b_128 *  0.114;
        cb      += r_128 * -0.16871 + g_128 * -0.33130 + b_128 *  0.5;
        cr      += r_128 *  0.5     + g_128 * -0.4187  + b_128 * -0.0813;
        return y;
    }

    /** Legacy decoders of the past did the conversion incorrectly.
     * If you want jPSXdec to also do it wrong, set this value to true. */
    private static final boolean INCORRECTLY_SWAP_CB_CR_LIKE_PSXMC = false;
    static {
        if (INCORRECTLY_SWAP_CB_CR_LIKE_PSXMC) {
            for (int i=0; i<10; i++)
                System.err.println("#### >> !! USING INCORRECTLY_SWAP_CB_CR_LIKE_PSXMC !! << ####");
        }
    }

    public static void toRgb(double y, double cb, double cr, @Nonnull RGB rgb) {
        double dblChromaRed, dblChromaGreen, dblChromaBlue;
        // MUST store chroma first like in instance method or result is slightly different
        if (INCORRECTLY_SWAP_CB_CR_LIKE_PSXMC) {
            // this math is wrong, wrong, WRONG
            // unfortunately it is used by the majority of existing decoders
            dblChromaRed   = ( 1.772  * cr)                 ;
            dblChromaGreen = (-0.3437 * cr) + (-0.7143 * cb);
            dblChromaBlue  =                  ( 1.402  * cb);
        } else {
            dblChromaRed   =                  ( 1.402  * cr);
            dblChromaGreen = (-0.3437 * cb) + (-0.7143 * cr);
            dblChromaBlue  = ( 1.772  * cb)                 ;
        }
        double dblYshift = y + 128;
        rgb.setR(dblYshift + dblChromaRed  );
        rgb.setG(dblYshift + dblChromaGreen);
        rgb.setB(dblYshift + dblChromaBlue );
    }

    final public void toRgb(@Nonnull RGB rgb1, @Nonnull RGB rgb2, @Nonnull RGB rgb3, @Nonnull RGB rgb4) {
        double dblChromaRed, dblChromaGreen, dblChromaBlue;
        if (INCORRECTLY_SWAP_CB_CR_LIKE_PSXMC) {
            // this math is wrong, wrong, WRONG
            // unfortunately it is used by the majority of existing decoders
            dblChromaRed   = ( 1.772  * cr)                 ;
            dblChromaGreen = (-0.3437 * cr) + (-0.7143 * cb);
            dblChromaBlue  =                  ( 1.402  * cb);
        } else {
            dblChromaRed   =                  ( 1.402  * cr);
            dblChromaGreen = (-0.3437 * cb) + (-0.7143 * cr);
            dblChromaBlue  = ( 1.772  * cb)                 ;
        }

        double dblYshift;

        dblYshift = y1 + 128;
        rgb1.setR(dblYshift + dblChromaRed  );
        rgb1.setG(dblYshift + dblChromaGreen);
        rgb1.setB(dblYshift + dblChromaBlue );

        dblYshift = y2 + 128;
        rgb2.setR(dblYshift + dblChromaRed  );
        rgb2.setG(dblYshift + dblChromaGreen);
        rgb2.setB(dblYshift + dblChromaBlue );

        dblYshift = y3 + 128;
        rgb3.setR(dblYshift + dblChromaRed  );
        rgb3.setG(dblYshift + dblChromaGreen);
        rgb3.setB(dblYshift + dblChromaBlue );

        dblYshift = y4 + 128;
        rgb4.setR(dblYshift + dblChromaRed  );
        rgb4.setG(dblYshift + dblChromaGreen);
        rgb4.setB(dblYshift + dblChromaBlue );
    }

    /** PSX YCbCr direct conversion to Rec.601 YCbCr.
     * <pre>
     *                              PSX
     * [ R ]   [ PSX YCbCr  ]   ( [ Y   ]   [128] )
     * [ G ] = [     ->     ] * ( [ Cb  ] + [ 0 ] )
     * [ B ]   [ RGB Matrix ]   ( [ Cr  ]   [ 0 ] )
     *
     *                               Rec.601
     * [ R ]   [ Rec.601 YCbCr ]   ( [ Y   ]   [ 16] )
     * [ G ] = [       ->      ] * ( [ Cb  ] - [128] )
     * [ B ]   [   RGB Matrix  ]   ( [ Cr  ]   [128] )
     *
     * Rec.601                                              PSX
     * [ Y   ]   [ Rec.601 YCbCr ]^-1   [ PSX YCbCr  ]   ( [ Y  ]   [128] )   [ 16]
     * [ Cb  ] = [       ->      ]    * [     ->     ] * ( [ Cb ] + [ 0 ] ) + [128]
     * [ Cr  ]   [   RGB Matrix  ]      [ RGB Matrix ]   ( [ Cr ]   [ 0 ] )   [128]
     *
     * [ Rec.601 YCbCr ]^-1   [ PSX YCbCr  ]   [ 250/291  -488509/2660418030  -82738/1330209015 ]
     * [       ->      ]    * [     ->     ] = [ 0           4014411/4571165        164/4571165 ]
     * [   RGB Matrix  ]      [ RGB Matrix ]   [ 0             3673/27426990    8031459/9142330 ]
     * </pre>
     * Unfortunately this conversion doesn't take into account chroma
     * upsampling interpolation.
     */
    public void toRec_601_YCbCr(@Nonnull Rec601YCbCr recYuv) {
        double dblYChroma = cb * (-488509./2660418030.) + cr * (-82738./1330209015.) + 16;

        recYuv.y1 = (y1+128)*(250./291.) + dblYChroma;
        recYuv.y2 = (y2+128)*(250./291.) + dblYChroma;
        recYuv.y3 = (y3+128)*(250./291.) + dblYChroma;
        recYuv.y4 = (y4+128)*(250./291.) + dblYChroma;

        recYuv.cb = cb * (4014411./4571165.) + cr *     (164./4571165.) + 128;
        recYuv.cr = cb *   (3673./27426990.) + cr * (8031459./9142330.) + 128;
    }

    /**
     * PlayStation 1 MDEC YCbCr is extremely similar to the YCbCr used
     * in the JPEG format. You could probably get away with just copying the
     * values directly. But here we'll try to tweak the colors to cover the
     * difference.
     * @see Pc601YCbCr
     */
    public void toRec_JFIF_YCbCr(@Nonnull Pc601YCbCr pcYuv) {
        double dblYChroma = cb * -3415973./13224846875. + cr * 1242172./13224846875.;

        pcYuv.y1 = y1+128 + dblYChroma;
        pcYuv.y2 = y2+128 + dblYChroma;
        pcYuv.y3 = y3+128 + dblYChroma;
        pcYuv.y4 = y4+128 + dblYChroma;

        pcYuv.cb = cb * 105814197./105798775.  + cr *     -5608./105798775. + 128;
        pcYuv.cr = cb *     19492./105798775.  + cr * 105791687./105798775. + 128;
    }

    @Override
    public String toString() {
        return String.format("([%f, %f, %f, %f] %f, %f)", y1, y2, y3, y4, cb, cr);
    }
}

