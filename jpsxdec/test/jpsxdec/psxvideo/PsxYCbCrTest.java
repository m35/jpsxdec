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

import jpsxdec.formats.RGB;
import org.junit.Assert;
import org.junit.Test;

public class PsxYCbCrTest {

    @Test
    public void testFloatToRgb() {
        RGB rgb = new RGB();

        RGB rgb1 = new RGB();
        RGB rgb2 = new RGB();
        RGB rgb3 = new RGB();
        RGB rgb4 = new RGB();

        PsxYCbCr instance = new PsxYCbCr();

        long lngFail = 0;
        for (int y = -128; y < 128; y++) {
            for (int cb = -128; cb < 128; cb++) {
                for (int cr = -128; cr < 128; cr++) {
                    // all Y values the same
                    instance.y1 = instance.y2 = instance.y3 = instance.y4 = y;
                    instance.cb = cb;
                    instance.cr = cr;

                    instance.toRgb(rgb1, rgb2, rgb3, rgb4);

                    // so all rgb values should be the same
                    Assert.assertEquals(rgb1, rgb2);
                    Assert.assertEquals(rgb1, rgb3);
                    Assert.assertEquals(rgb1, rgb4);

                    // also check that the static method has the same result
                    PsxYCbCr.toRgb(y, cb, cr, rgb);
                    if (!rgb1.equals(rgb)) {
                        System.out.println("("+y+", "+cb+", "+cr+") -> " + rgb1 + " != " + rgb);
                        lngFail++;
                    }
                }
            }
        }
        Assert.assertEquals(0, lngFail);
    }


    @Test
    public void testIntConstants() {

        long _1_402   = 91893;
        long _0_3437  = 22525;
        long _0_7143  = 46812;
        long _1_772   = 116224;

        Assert.assertEquals(_1_402 , PsxYCbCr_int._1_402);
        Assert.assertEquals(_0_3437, PsxYCbCr_int._0_3437);
        Assert.assertEquals(_0_7143, PsxYCbCr_int._0_7143);
        Assert.assertEquals(_1_772 , PsxYCbCr_int._1_772);

        Assert.assertEquals(_1_402 , Math.round(1.402  * PsxYCbCr_int.FIXED_MULT) + 12);
        Assert.assertEquals(_0_3437, Math.round(0.3437 * PsxYCbCr_int.FIXED_MULT)     );
        Assert.assertEquals(_0_7143, Math.round(0.7143 * PsxYCbCr_int.FIXED_MULT)     );
        Assert.assertEquals(_1_772 , Math.round(1.772  * PsxYCbCr_int.FIXED_MULT) + 94);

    }

}