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

import java.awt.image.BufferedImage;
import javax.annotation.Nonnull;

/** A YCbCr image in so called "Pc.601" color space.
 * @see Pc601YCbCr */
public class Pc601YCbCrImage extends YCbCrImage {

    public Pc601YCbCrImage(int iWidth, int iHeight) {
        super(iWidth, iHeight);
    }

    public Pc601YCbCrImage(@Nonnull BufferedImage rgb) {
        super(rgb.getWidth(), rgb.getHeight());

        Pc601YCbCr ycc = new Pc601YCbCr();
        RGB rgb1 = new RGB();
        RGB rgb2 = new RGB();
        RGB rgb3 = new RGB();
        RGB rgb4 = new RGB();

        for (int x = 0; x < getWidth(); x+=2) {
            for (int y = 0; y < getHeight(); y+=2) {
                rgb1.set(rgb.getRGB(x  , y  ));
                rgb2.set(rgb.getRGB(x+1, y  ));
                rgb3.set(rgb.getRGB(x  , y+1));
                rgb4.set(rgb.getRGB(x+1, y+1));
                ycc.fromRgb(rgb1, rgb2, rgb3, rgb4);

                setYCbCr(x, y , ycc);
            }
        }
    }

    public void setYCbCr(int iLumaX, int iLumaY, @Nonnull Pc601YCbCr ycbcr) {
        setYDbl( iLumaX+0 , iLumaY+0 , ycbcr.y1);
        setYDbl( iLumaX+1 , iLumaY+0 , ycbcr.y2);
        setYDbl( iLumaX+0 , iLumaY+1 , ycbcr.y3);
        setYDbl( iLumaX+1 , iLumaY+1 , ycbcr.y4);
        int iChromaX = iLumaX / 2;
        int iChromaY = iLumaY / 2;
        setCbDbl( iChromaX , iChromaY  , ycbcr.cb);
        setCrDbl( iChromaX , iChromaY  , ycbcr.cr);
    }

}
