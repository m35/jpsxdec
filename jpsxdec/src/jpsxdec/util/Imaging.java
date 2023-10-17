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

package jpsxdec.util;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import javax.annotation.Nonnull;

/** Creates more predictable image color spaces.
 *<p>
 * Java can sometimes create images that don't always output the exact
 * bytes you put into them. These helper functions try to alleviate that. */
public class Imaging {
    public static final ColorModel LINEAR_RGB_INT;
    public static final IndexColorModel LINEAR_GRAY_INDEXED;
    static {
        ColorSpace clrSpace = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        LINEAR_RGB_INT = new DirectColorModel(
                clrSpace,
                24,
                0x00ff0000,
                0x0000ff00,
                0x000000ff,
                0x00000000,
                false, // isAlphaPreMultiplied
                DataBuffer.TYPE_INT);

        byte[] abPal = new byte[256];
        for (int i = 0; i < abPal.length; i++)
            abPal[i] = (byte)i;
        LINEAR_GRAY_INDEXED = new IndexColorModel(
                8, // bits
                abPal.length,
                abPal, abPal, abPal);
    }
    public static @Nonnull BufferedImage createLinearRgbInt(@Nonnull int[] aiRgb, int iWidth, int iHeight) {
        WritableRaster oRaster = LINEAR_RGB_INT.createCompatibleWritableRaster(iWidth, iHeight);
        oRaster.setDataElements(0, 0, iWidth, iHeight, aiRgb);
        return new BufferedImage(
                LINEAR_RGB_INT,
                oRaster,
                false,  // isRasterPremultiplied : no difference if true or false
                null); // properties
    }
    public static @Nonnull BufferedImage createLinearGrayIndexed256(@Nonnull byte[] abGray, int iWidth, int iHeight) {
        WritableRaster raster = LINEAR_GRAY_INDEXED.createCompatibleWritableRaster(iWidth, iHeight);
        raster.setDataElements(0, 0, iWidth, iHeight, abGray);
        return new BufferedImage(
                LINEAR_GRAY_INDEXED,
                raster,
                false,
                null);
    }

}
