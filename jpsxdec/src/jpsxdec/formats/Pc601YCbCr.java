/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

/** Hold the "full range" YCbCr color space with 4:2:0 chroma subsampling as
 * used by the JPEG image.
 * Known sometimes as:
 * - yuvj420*
 * - J420
 * - JFIF yuv
 * - jyuv
 * - Pc.601
 *
 * Four luma samples, and one chroma sub-sample are stored as doubles in the
 * "full range" of:
 *<pre>
 * Y : 0 to 255
 * Cb: 0 to 255
 * Cr: 0 to 255
 *</pre>
 */
public class Pc601YCbCr {
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

        double y =   ( 0.299 * r) + ( 0.587 * g) + ( 0.114 * b);
        cb      += ( (-0.299 * r) + (-0.587 * g) + ( 0.886 * b)) / 1.772 + 128;
        cr      += ( ( 0.701 * r) + (-0.587 * g) + (-0.114 * b)) / 1.402 + 128;

        return y;
    }

    // TODO to rgb

    @Override
    public String toString() {
        return String.format("([%f, %f, %f, %f] %f, %f)", y1, y2,  y3, y4, cb, cr);
    }

}
