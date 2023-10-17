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
import jpsxdec.formats.RGB;
import jpsxdec.util.Maths;

/**
 * Integer version of {@link PsxYCbCr} for faster decoding.
 *<pre>
 * Y : -128 to 127
 * Cb: -128 to 127
 * Cr: -128 to 127
 *</pre>
 * @see PsxYCbCr
 */
public class PsxYCbCr_int {

    public int y1, y2, y3, y4, cb, cr;

    public static final int FIXED_BITS = 16;
    public static final double FIXED_MULT = 1L << FIXED_BITS;

    // these values will simulate accurate hardware YCbCr conversion.
    // they weren't very difficult to find because the YCbCr->RGB conversion
    // equation is quite forgiving. There are many values which would
    // generate perfect hardware emulation.
    public static final long _1_402   = 91893;  // Math.round(1.402  * FIXED_MULT)+12;
    public static final long _0_3437  = 22525;  // Math.round(0.3437 * FIXED_MULT);
    public static final long _0_7143  = 46812;  // Math.round(0.7143 * FIXED_MULT);
    public static final long _1_772   = 116224; // Math.round(1.772  * FIXED_MULT)+94;

    public static void toRgb(int y, int cb, int cr, @Nonnull RGB rgb) {
        int Yshift = y + 128;
        long c_r =                 _1_402  * cr,
             c_g = -_0_3437 * cb - _0_7143 * cr,
             c_b =  _1_772  * cb;
        rgb.setR(Yshift + (int)Maths.shrRound(c_r, FIXED_BITS));
        rgb.setG(Yshift + (int)Maths.shrRound(c_g, FIXED_BITS));
        rgb.setB(Yshift + (int)Maths.shrRound(c_b, FIXED_BITS));
    }

    final public void toRgb(@Nonnull RGB rgb1, @Nonnull RGB rgb2, @Nonnull RGB rgb3, @Nonnull RGB rgb4) {
        int iChromaRed   = (int)Maths.shrRound(                      _1_402  * cr , FIXED_BITS);
        int iChromaGreen = (int)Maths.shrRound( -(_0_3437 * cb)  -  (_0_7143 * cr), FIXED_BITS);
        int iChromaBlue  = (int)Maths.shrRound(   _1_772  * cb                    , FIXED_BITS);
        int iYshift;

        iYshift = y1 + 128;
        rgb1.setR(iYshift + iChromaRed);
        rgb1.setG(iYshift + iChromaGreen);
        rgb1.setB(iYshift + iChromaBlue);

        iYshift = y2 + 128;
        rgb2.setR(iYshift + iChromaRed);
        rgb2.setG(iYshift + iChromaGreen);
        rgb2.setB(iYshift + iChromaBlue);

        iYshift = y3 + 128;
        rgb3.setR(iYshift + iChromaRed);
        rgb3.setG(iYshift + iChromaGreen);
        rgb3.setB(iYshift + iChromaBlue);

        iYshift = y4 + 128;
        rgb4.setR(iYshift + iChromaRed);
        rgb4.setG(iYshift + iChromaGreen);
        rgb4.setB(iYshift + iChromaBlue);
    }

    @Override
    public String toString() {
        return String.format( "([%d, %d, %d, %d] %d, %d)" , y1, y2, y3, y4, cb, cr);
    }

}
