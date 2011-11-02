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

package jpsxdec.psxvideo;


import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.RGB;

/** 
 * Handles YCbCr image data of the PSX MDEC chip, which
 * is slightly different from JFIF Rec.601 YCbCr.
 * Note that like JFIF YCbCr, the "color space" has a full range of 256 values.
 *<pre>
 * Y : -128 to 127
 * Cb: -128 to 127
 * Cr: -128 to 127
 *</pre>
 */
public class PsxYCbCrImage {
    
    private static final Logger log = Logger.getLogger(PsxYCbCrImage.class.getName());

    private int _iWidth;
    private int _iHeight;
    
    private double[] _adblY;
    private double[] _adblCb;
    private double[] _adblCr;
    
    /** Creates a new instance of PsxYuvImage.
     * @param iWidth - Width of image (in Luminance values) 
     * @param iHeight - Height of image (in Luminance values) */
    public PsxYCbCrImage(int iWidth, int iHeight) {
        assert(iWidth > 0 && iHeight > 0 && 
               (iWidth  % 2) == 0 && 
               (iHeight % 2) == 0);
        _iWidth = iWidth;
        _iHeight = iHeight;
        int iSize = iWidth * iHeight;
        _adblY  = new double[iSize];
        _adblCb = new double[iSize / 4];
        _adblCr = new double[iSize / 4];
    }

    public PsxYCbCrImage(BufferedImage bi) {
        this(new RgbIntImage(bi));
    }

    public PsxYCbCrImage(RgbIntImage rgb) {
        _iWidth  = rgb.getWidth();
        _iHeight = rgb.getHeight();
        assert(_iWidth > 0 && _iHeight > 0 &&
               (_iWidth  % 2) == 0 &&
               (_iHeight % 2) == 0);

        int iSize = _iWidth * _iHeight;
        _adblY  = new double[iSize];
        iSize /= 4;
        _adblCb = new double[iSize];
        _adblCr = new double[iSize];

        RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();

        PsxYCbCr yuv = new PsxYCbCr();
        for (int x = 0; x < _iWidth; x+=2) {
            for (int y = 0; y < _iHeight; y+=2) {
                rgb1.set(rgb.get(x  , y  ));
                rgb2.set(rgb.get(x+1, y  ));
                rgb3.set(rgb.get(x  , y+1));
                rgb4.set(rgb.get(x+1, y+1));
                yuv.fromRgb(rgb1, rgb2, rgb3, rgb4);
                _adblY[ (x  ) + (y  ) * _iWidth ] = yuv.y1;
                _adblY[ (x+1) + (y  ) * _iWidth ] = yuv.y2;
                _adblY[ (x  ) + (y+1) * _iWidth ] = yuv.y3;
                _adblY[ (x+1) + (y+1) * _iWidth ] = yuv.y4;
                _adblCb[x/2 + (y/2) * (_iWidth/2)] = yuv.cb;
                _adblCr[x/2 + (y/2) * (_iWidth/2)] = yuv.cr;
            }
        }
    }

    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values. */
    public RgbIntImage toRgb() {
        int[] aiARGB = new int[_iWidth * _iHeight];

        PsxYCbCr ycc = new PsxYCbCr();
        RGB rgb1 = new RGB(), rgb2 = new RGB(), rgb3 = new RGB(), rgb4 = new RGB();
        
        for (int iY = 0; iY < _iHeight; iY+=2) {
            int iLinePos = iY * _iWidth;
            int iChromLinePos = (iY / 2) * (_iWidth / 2);
            for (int iX = 0; iX < _iWidth; iX+=2) {
                ycc.cb = _adblCb[iChromLinePos];
                ycc.cr = _adblCr[iChromLinePos];
                iChromLinePos++;

                ycc.y1 = _adblY[iLinePos         + iX  ];
                ycc.y2 = _adblY[iLinePos         + iX+1];
                ycc.y3 = _adblY[iLinePos+_iWidth + iX  ];
                ycc.y4 = _adblY[iLinePos+_iWidth + iX+1];

                ycc.toRgb(rgb1, rgb2, rgb3, rgb4);

                aiARGB[iLinePos +           iX  ] = rgb1.toInt();
                aiARGB[iLinePos +           iX+1] = rgb2.toInt();
                aiARGB[iLinePos + _iWidth + iX  ] = rgb3.toInt();
                aiARGB[iLinePos + _iWidth + iX+1] = rgb4.toInt();
            }
        }

        return new RgbIntImage(_iWidth, _iHeight, aiARGB);
    }

    public double getY(int iLuminX, int iLuminY) {
        return _adblY[iLuminX + iLuminY * _iWidth];
    }
    public double getCbForY(int iLuminX, int iLuminY) {
        return _adblCb[iLuminX/2 + (iLuminY/2)*(_iWidth/2)];
    }
    public double getCrForY(int iLuminX, int iLuminY) {
        return _adblCr[iLuminX/2 + (iLuminY/2)*(_iWidth/2)];
    }

    public void setY(int iLuminX, int iLuminY, double y) {
        _adblY[iLuminX + iLuminY * _iWidth] = y;
    }
    public void setCb(int iChromX, int iChromY, double cb) {
        _adblCb[iChromX + iChromY * (_iWidth/2)] = cb;
    }
    public void setCr(int iChromX, int iChromY, double cr) {
        _adblCr[iChromX + iChromY * (_iWidth/2)] = cr;
    }


    public int getLumaHeight() {
        return _iHeight;
    }

    public int getLumaWidth() {
        return _iWidth;
    }

    public int getChromHeight() {
        return _iHeight / 2;
    }
    public int getChromWidth() {
        return _iWidth / 2;
    }

    public BufferedImage CrToBufferedImage() {
        return doubleArrayToBufferedImage(getChromWidth(), getChromHeight(), _adblCr, 0);
    }
    public BufferedImage CbToBufferedImage() {
        return doubleArrayToBufferedImage(getChromWidth(), getChromHeight(), _adblCb, 1);
    }

    public BufferedImage YToBufferedImage() {
        return doubleArrayToBufferedImage(getLumaWidth(), getLumaHeight(), _adblY, 2);
    }

    private static BufferedImage doubleArrayToBufferedImage(int iWidth, int iHeight, double[] adbl, int iMod) {
        BufferedImage bi = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < iWidth; x++) {
            for (int y = 0; y < iHeight; y++) {
                int c = (int)(adbl[x + y * iWidth] + 128);
                if (c < 0) c = 0; else if (c > 255) c = 255;
                switch (iMod) {
                    case 0: c <<= 16; break; // shift to red
                    case 1: break; // already at blue
                    case 2: c |= c << 8 | c << 16; break; // grayscale
                }
                bi.setRGB(x, y, c);
            }
        }
        return bi;
    }

    public double[] get8x8blockY(int iX, int iY) {
        return get8x8block(iX, iY, _adblY, _iWidth);
    }

    public double[] get8x8blockCb(int iX, int iY) {
        return get8x8block(iX, iY, _adblCb, _iWidth / 2);
    }

    public double[] get8x8blockCr(int iX, int iY) {
        return get8x8block(iX, iY, _adblCr, _iWidth / 2);
    }

    private static double[] get8x8block(int iX, int iY, double[] adblComponent, int iWidth) {
        double[] adblBlock = new double[8*8];
        for (int iXofs = 0; iXofs < 8; iXofs++) {
            for (int iYofs = 0; iYofs < 8; iYofs++) {
                adblBlock[iXofs + iYofs * 8] =
                        adblComponent[iX + iXofs + (iY + iYofs) * iWidth];
            }
        }
        return adblBlock;
    }
}
