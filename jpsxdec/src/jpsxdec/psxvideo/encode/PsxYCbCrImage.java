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

package jpsxdec.psxvideo.encode;


import java.awt.image.BufferedImage;
import javax.annotation.Nonnull;
import jpsxdec.formats.RGB;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.psxvideo.PsxYCbCr;

/**
 * Handles YCbCr image data of the PSX MDEC chip, which
 * is slightly different from JFIF (sometimes referred to as Pc.601) YCbCr.
 * Note that like JFIF YCbCr, the "color space" has a full range of 256 values.
 *<pre>
 * Y : -128 to 127
 * Cb: -128 to 127
 * Cr: -128 to 127
 *</pre>
 * @see jpsxdec.formats.Pc601YCbCr
 */
public class PsxYCbCrImage {

    private final int _iWidth;
    private final int _iHeight;

    private final double[] _adblY;
    private final double[] _adblCb;
    private final double[] _adblCr;

    /** @param iWidth - Width of image (in Luminance values)
     *  @param iHeight - Height of image (in Luminance values) */
    public PsxYCbCrImage(int iWidth, int iHeight) {
        if (iWidth < 2 || iHeight < 2 ||
           (iWidth % 2) != 0 || (iHeight % 2) != 0)
        {
            throw new IllegalArgumentException();
        }
        _iWidth = iWidth;
        _iHeight = iHeight;
        int iSize = iWidth * iHeight;
        _adblY  = new double[iSize];
        _adblCb = new double[iSize / 4];
        _adblCr = new double[iSize / 4];
    }

    public PsxYCbCrImage(@Nonnull BufferedImage bi) {
        this(new RgbIntImage(bi));
    }

    public PsxYCbCrImage(@Nonnull RgbIntImage rgb) {
        this(rgb.getWidth(), rgb.getHeight());

        RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        PsxYCbCr yuv = new PsxYCbCr();
        for (int x = 0; x < _iWidth; x+=2) {
            for (int y = 0; y < _iHeight; y+=2) {
                rgb1.set(rgb.get(x  , y  ));
                rgb2.set(rgb.get(x+1, y  ));
                rgb3.set(rgb.get(x  , y+1));
                rgb4.set(rgb.get(x+1, y+1));

                yuv.fromRgb(rgb1, rgb2, rgb3, rgb4);

                setY(x  , y  , yuv.y1);
                setY(x+1, y  , yuv.y2);
                setY(x  , y+1, yuv.y3);
                setY(x+1, y+1, yuv.y4);

                setCb(x/2, y/2, yuv.cb);
                setCr(x/2,y/2, yuv.cr);
            }
        }
    }

    /** Converts yuv image to a BufferedImage, converting, rounding, and
     * clamping RGB values. */
    public @Nonnull RgbIntImage toRgb() {
        PsxYCbCr ycc = new PsxYCbCr();
        RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        RgbIntImage rgb = new RgbIntImage(_iWidth, _iHeight);

        for (int iY = 0; iY < _iHeight; iY+=2) {
            for (int iX = 0; iX < _iWidth; iX+=2) {

                ycc.cb = getCbForY(iX, iY);
                ycc.cr = getCrForY(iX, iY);

                ycc.y1 = getY(iX  , iY  );
                ycc.y2 = getY(iX+1, iY  );
                ycc.y3 = getY(iX  , iY+1);
                ycc.y4 = getY(iX+1, iY+1);

                ycc.toRgb(rgb1, rgb2, rgb3, rgb4);

                rgb.set(iX  , iY  , rgb1.toInt());
                rgb.set(iX+1, iY  , rgb2.toInt());
                rgb.set(iX  , iY+1, rgb3.toInt());
                rgb.set(iX+1, iY+1, rgb4.toInt());
            }
        }

        return rgb;
    }

    public double getY(int iLumaX, int iLumaY) {
        return _adblY[iLumaX + iLumaY * _iWidth];
    }
    public double getCbForY(int iLumaX, int iLumaY) {
        return _adblCb[iLumaX/2 + (iLumaY/2)*(_iWidth/2)];
    }
    public double getCrForY(int iLumaX, int iLumaY) {
        return _adblCr[iLumaX/2 + (iLumaY/2)*(_iWidth/2)];
    }

    public void setY(int iLumaX, int iLumaY, double y) {
        _adblY[iLumaX + iLumaY * _iWidth] = y;
    }
    public void setCb(int iChromaX, int iChromaY, double cb) {
        _adblCb[iChromaX + iChromaY * (_iWidth/2)] = cb;
    }
    public void setCr(int iChromaX, int iChromaY, double cr) {
        _adblCr[iChromaX + iChromaY * (_iWidth/2)] = cr;
    }


    public int getLumaWidth() {
        return _iWidth;
    }

    public int getLumaHeight() {
        return _iHeight;
    }

    public int getChromaHeight() {
        return _iHeight / 2;
    }
    public int getChromaWidth() {
        return _iWidth / 2;
    }

    public @Nonnull BufferedImage CrToBufferedImage() {
        return doubleArrayToBufferedImage(getChromaWidth(), getChromaHeight(), _adblCr, 0);
    }
    public @Nonnull BufferedImage CbToBufferedImage() {
        return doubleArrayToBufferedImage(getChromaWidth(), getChromaHeight(), _adblCb, 1);
    }

    public @Nonnull BufferedImage YToBufferedImage() {
        return doubleArrayToBufferedImage(getLumaWidth(), getLumaHeight(), _adblY, 2);
    }

    private static @Nonnull BufferedImage doubleArrayToBufferedImage(int iWidth, int iHeight, @Nonnull double[] adbl, int iComponent) {
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        for (int iX = 0; iX < iWidth; iX++) {
            for (int iY = 0; iY < iHeight; iY++) {
                int c = (int)Math.round(adbl[iX + iY * iWidth]) + 128;
                if (c < 0)
                    c = 0;
                else if (c > 255)
                    c = 255;
                switch (iComponent) {
                    case 0: c <<= 16; break; // shift to red
                    case 1: break; // already at blue
                    case 2: c |= c << 8 | c << 16; break; // grayscale
                }
                bi.setRGB(iX, iY, c);
            }
        }
        return bi;
    }

    public @Nonnull double[] get8x8blockY(int iX, int iY) {
        if (iX < 0 || iX >= _iWidth)
            throw new IllegalArgumentException(iX + " X is out of bounds");
        if (iY < 0 || iY >= _iHeight)
            throw new IllegalArgumentException(iY + " Y is out of bounds");
        return get8x8block(iX, iY, _adblY, _iWidth);
    }

    public @Nonnull double[] get8x8blockCb(int iX, int iY) {
        return get8x8block(iX, iY, _adblCb, _iWidth / 2);
    }

    public @Nonnull double[] get8x8blockCr(int iX, int iY) {
        return get8x8block(iX, iY, _adblCr, _iWidth / 2);
    }

    private static @Nonnull double[] get8x8block(int iX, int iY, @Nonnull double[] adblComponent, int iWidth) {
        double[] adblBlock = new double[8*8];
        for (int iYofs = 0; iYofs < 8; iYofs++) {
            System.arraycopy(adblComponent, iX + (iY + iYofs) * iWidth, adblBlock, iYofs * 8, 8);
        }
        return adblBlock;
    }
}
