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

package jpsxdec.formats;

import javax.annotation.Nonnull;

/** Holds a standard Rec.601 color space YCbCr color with 4:2:0 subsampling.
 * Four luma samples, and one chroma sub-sample are stored as doubles in the
 * standard range of
 *<pre>
 * Y : 16 to 235
 * Cb: 16 to 240
 * Cr: 16 to 240
 *</pre>
 *<blockquote>
 *  "In each 8 bit luminance sample, the value 16 is used for black and
 *  235 for white, to allow for overshoot and undershoot. The values 0
 *  and 255 are used for sync encoding. The Cb and Cr samples use the
 *  value 128 to encode a zero value, as used when encoding a white,
 *  grey or black area."
 *</blockquote>
 *      -http://en.wikipedia.org/wiki/Rec._601
 */
public class Rec601YCbCr {
    public double y1, y2, y3, y4, cb, cr;

    /** Performs simple bilinear downsampling interpolation for chroma components. */
    public void fromRgb(@Nonnull RGB rgb1, @Nonnull RGB rgb2, @Nonnull RGB rgb3, @Nonnull RGB rgb4) {
        cb = cr = 0;
        y1 = oneRgb(rgb1);
        y2 = oneRgb(rgb2);
        y3 = oneRgb(rgb3);
        y4 = oneRgb(rgb4);
        cb /= 4;
        cr /= 4;
    }
    private double oneRgb(@Nonnull RGB rgb) {
        int r = rgb.getR(), g = rgb.getG(), b = rgb.getB();

        double y = ( 0.257 * r) + ( 0.504 * g) + ( 0.098 * b)  + 16;
        cb      += (-0.148 * r) + (-0.291 * g) + ( 0.439 * b)  + 128;
        cr      += ( 0.439 * r) + (-0.368 * g) + (-0.071 * b)  + 128;
        return y;
    }

    public static void toRgb(double y, double cb, double cr, @Nonnull RGB rgb) {
        double y_16 = y - 16;
        double cb_128 = cb - 128;
        double cr_128 = cr - 128;
        rgb.setR( (y_16 * 1.164) +                     ( 1.596 * cr_128) );
        rgb.setG( (y_16 * 1.164) + (-0.391 * cb_128) + (-0.813 * cr_128) );
        rgb.setB( (y_16 * 1.164) + ( 2.018 * cb_128)                     );
    }

    public void toRgb(@Nonnull RGB rgb1, @Nonnull RGB rgb2, @Nonnull RGB rgb3, @Nonnull RGB rgb4) {
        double cb_128 = cb - 128;
        double cr_128 = cr - 128;
        double dblChromaRed   =                     ( 1.596 * cr_128);
        double dblChromaGreen = (-0.391 * cb_128) + (-0.813 * cr_128);
        double dblChromaBlue  = ( 2.018 * cb_128)                    ;

        double dblYshift;

        dblYshift = (y1 - 16) * 1.164;
        rgb1.setR(dblYshift + dblChromaRed  );
        rgb1.setG(dblYshift + dblChromaGreen);
        rgb1.setB(dblYshift + dblChromaBlue );

        dblYshift = (y2 - 16) * 1.164;
        rgb2.setR(dblYshift + dblChromaRed  );
        rgb2.setG(dblYshift + dblChromaGreen);
        rgb2.setB(dblYshift + dblChromaBlue );

        dblYshift = (y3 - 16) * 1.164;
        rgb3.setR(dblYshift + dblChromaRed  );
        rgb3.setG(dblYshift + dblChromaGreen);
        rgb3.setB(dblYshift + dblChromaBlue );

        dblYshift = (y4 - 16) * 1.164;
        rgb4.setR(dblYshift + dblChromaRed  );
        rgb4.setG(dblYshift + dblChromaGreen);
        rgb4.setB(dblYshift + dblChromaBlue );
    }

    @Override
    public String toString() {
        return String.format("([%f, %f, %f, %f] %f, %f)", y1, y2,  y3, y4, cb, cr);
    }

}
