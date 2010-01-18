/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.video;


import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.RgbIntImage.RGB;
import jpsxdec.formats.Yuv4mpeg2.CCIR601YCbCr;

/** 
 * Class to handle YUV (YCbCr) image data of the PSX MDEC chip, which
 * is slightly different from standard (CCIR-601) YCbCr. */
public class PsxYuvImage {
    
    private static final Logger log = Logger.getLogger(PsxYuvImage.class.getName());

    private int _iWidth;
    private int _iHeight;
    
    private double[] _adblY;
    private double[] _adblCb;
    private double[] _adblCr;
    
    /** Creates a new instance of PsxYuvImage.
     * @param iWidth - Width of image (in Luminance values) 
     * @param iHeight - Height of image (in Luminance values) */
    public PsxYuvImage(int iWidth, int iHeight) {
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

    public PsxYuvImage(BufferedImage bi) {
        this(new RgbIntImage(bi));
    }

    public PsxYuvImage(RgbIntImage rgb) {
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

        for (int x = 0; x < _iWidth; x++) {
            for (int y = 0; y < _iHeight; y++) {
                int iRgb = rgb.get(x, y);
                PSXYCbCr ycc = new PSXYCbCr(new RGB((iRgb >> 16) & 0xFF,
                                                    (iRgb >>  8) & 0xFF,
                                                    (iRgb      ) & 0xFF));
                _adblY[ x + y * _iWidth ] = ycc.y - 128.0;
                _adblCb[x/2 + (y/2) * (_iWidth/2)] += ycc.cb;
                _adblCr[x/2 + (y/2) * (_iWidth/2)] += ycc.cr;
            }
        }

        for (int i = 0; i < _adblCb.length; i++) {
            _adblCb[i] = _adblCb[i] / 4.0;
            _adblCr[i] = _adblCr[i] / 4.0;
        }
    }

    public static class PSXYCbCr {
        public double y, cb, cr;

        public PSXYCbCr() {
        }

        public PSXYCbCr(double _y, double _cb, double _cr) {
            this.y = _y;
            this.cb = _cb;
            this.cr = _cr;
        }

        public PSXYCbCr(RGB rgb) {
            y  = rgb.r *      0.299        + rgb.g *        0.587       + rgb.b * 0.114;
            cb = rgb.r * -714300.0/4231951 + rgb.g * -1402000.0/4231951 + rgb.b * 0.5;
            cr = rgb.r *       0.5         + rgb.g * -1772000.0/4231951 + rgb.b * -343700.0/4231951;
        }

        public RGB toRgb() {
            return new RGB(
                    y +128 +                   ( 1.402  * cr),
                    y +128 + (-0.3437  * cb) + (-0.7143 * cr),
                    y +128 + (1.772    * cb)                 );
        }


        /** PSX YCbCr to CCIR-601 YCbCr conversion matrix.
         * <pre>
         * CCIR-601    PSX
         * [  Y   ]   [ Y  ]   [ 1  -3415973/13224846875  1242172/13224846875 ]
         * [  Cb  ] = [ Cb ] * [ 0   105814197/105798775      -5608/105798775 ]
         * [  Cr  ]   [ Cr ]   [ 0       19492/105798775  105791687/105798775 ]
         *
         * Y  = Y + Cb * -3415973/13224846875 + Cr * 1242172/13224846875
         * Cb =     Cb * 105814197/105798775  + Cr * -5608/105798775
         * Cr =     Cb * 19492/105798775      + Cr * 105791687/105798775
         * </pre>
         */
        private static final double Y2 = -3415973.0 / 13224846875.0;
        private static final double Y3 = 1242172.0  / 13224846875.0;
                
        private static final double CB2 = 105814197.0 / 105798775.0;
        private static final double CB3 = -5608.0 / 105798775.0;

        private static final double CR2 = 19492.0 / 105798775.0;
        private static final double CR3 = 105791687.0 / 105798775.0;

        public CCIR601YCbCr toCCIR601YCbCr() {
            return bigChange(y, cb, cr);
        }

        public String toString() {
            return String.format("(%f, %f, %f)", y, cb, cr);
        }

        public static double psxChrom_to_y4mCb(double dblPsxCb, double dblPsxCr) {
            return dblPsxCb * CB2 + dblPsxCr * CB3;
        }

        public static double psxChrom_to_y4mCr(double dblPsxCb, double dblPsxCr) {
            return dblPsxCb * CR2 + dblPsxCr * CR3;
        }

        public static double psxYuv_to_y4mLumin(double dblPsxY, double dblPsxCb, double dblPsxCr) {
            return dblPsxY + dblPsxCb * Y2  + dblPsxCr * Y3;
        }

        public static CCIR601YCbCr bigChange(double dblY, double dblCb, double dblCr) {
            return new CCIR601YCbCr(
                    dblY + dblCb * Y2  + dblCr * Y3,
                           dblCb * CB2 + dblCr * CB3,
                           dblCb * CR2 + dblCr * CR3);
        }

    }

    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values. */
    public RgbIntImage toRgb() {
        int[] aiARGB = new int[_iWidth * _iHeight];
        
        for (int iLinePos = 0, iY = 0; iY < _iHeight; iLinePos += _iWidth, iY++) {
            int iChromLinePos = (iY / 2) * (_iWidth / 2);
            for (int iX = 0; iX < _iWidth; iX++) {
                /* Only brifly mentioned in a few YCbCr texts, 
                 * the normal equations for converting YUV to RGB
                 * require a color space of:
                 * Y : 0 to 255
                 * Cb: -128 to +127
                 * Cr: -128 to +127
                 */
                double y = _adblY[iLinePos + iX] + 128; // <-- fix the lumin!
                double cb = _adblCb[iChromLinePos + iX / 2];
                double cr = _adblCr[iChromLinePos + iX / 2];
                
                int r = (int)Math.round(y +                1.402  * cr);
                int g = (int)Math.round(y - 0.3437  * cb - 0.7143 * cr);
                int b = (int)Math.round(y + 1.772   * cb              );
                
                if (r < 0) {r = 0;} else {if (r > 255) r = 255;}
                if (g < 0) {g = 0;} else {if (g > 255) g = 255;}
                if (b < 0) {b = 0;} else {if (b > 255) b = 255;}
                
                aiARGB[iLinePos + iX] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }

        return new RgbIntImage(_iWidth, _iHeight, aiARGB);
    }

    public void setY(int iDestX, int iDestY,
                     int iWidth, int iHeight, double[] adblY)
    {
        setY(iDestX, iDestY, iWidth, iHeight, adblY, 0);
    }

    /** Set a block of luminance values.
     * @param iDestX - Top left corner where block starts (in Luminance pixels)
     * @param iDestY - Top left corner where block starts (in Luminance pixels)
     * @param iWidth - Width of block (in Luminance pixels)
     * @param iHeight - Height of block (in Luminance pixels)
     * @param adblY - Array of block values with the color space -128 to +127.*/
    public void setY(int iDestX, int iDestY, 
                     int iWidth, int iHeight,
                     double[] adblY, int iBufOfs)
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < _iWidth && iDestY + iHeight < _iHeight);
        assert((iDestX % 2) == 0 && (iDestY % 2) == 0);
        assert(adblY.length == iWidth * iHeight);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * _iWidth);
                
        for (int iLine = 0; 
             iLine < iHeight; 
             iLine++, iDestLineStart += _iWidth, iSrcLineStart += iWidth)
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                _adblY[iDestLineStart + iCol] = adblY[iBufOfs + iSrcLineStart + iCol];
            }
        }
    }

    public void setY(int iDestX, int iDestY,
                     int iWidth, int iHeight,
                     int[] adblY)
    {
        setY(iDestX, iDestY, iWidth, iHeight, adblY, 0);
        assert(adblY.length == iWidth * iHeight);
    }
    
    public void setY(int iDestX, int iDestY, 
                     int iWidth, int iHeight, 
                     int[] adblY, int iBufOfs)
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < _iWidth && iDestY + iHeight < _iHeight);
        assert((iDestX % 2) == 0 && (iDestY % 2) == 0);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * _iWidth);
                
        for (int iLine = 0; 
             iLine < iHeight; 
             iLine++, iDestLineStart += _iWidth, iSrcLineStart += iWidth)
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                _adblY[iDestLineStart + iCol] = adblY[iBufOfs + iSrcLineStart + iCol];
            }
        }
    }
    
    public void setCbCr(int iDestX, int iDestY,
                        int iWidth, int iHeight,
                        double[] dblCb, double[] dblCr)
    {
        assert(dblCb.length == iWidth * iHeight);
        assert(dblCr.length == iWidth * iHeight);

        setCbCr(iDestX, iDestY, iWidth, iHeight, dblCb, 0, dblCr, 0);
    }

    /** Set a block of Cr and Cb values.
     * @param iDestX - Top left corner where block starts (in Chrominance pixels)
     * @param iDestY - Top left corner where block starts (in Chrominance pixels)
     * @param iWidth - Width of block (in Chrominance pixels)
     * @param iHeight - Height of block (in Chrominance pixels)
     * @param dblCb - Array of block values with the color space -128 to +127. 
     * @param dblCr - Array of block values with the color space -128 to +127.*/
    public void setCbCr(int iDestX, int iDestY,
                        int iWidth, int iHeight,
                        double[] dblCb, int iCbOfs,
                        double[] dblCr, int iCrOfs)
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < _iWidth/2 && iDestY + iHeight < _iHeight/2);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * _iWidth / 2);
                
        for (int iLine = 0; iLine < iHeight; 
             iLine++, iDestLineStart += _iWidth / 2, iSrcLineStart += iWidth)
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                _adblCb[iDestLineStart + iCol] = dblCb[iCbOfs + iSrcLineStart + iCol];
                _adblCr[iDestLineStart + iCol] = dblCr[iCrOfs + iSrcLineStart + iCol];
            }
        }
    }
    
    public void setCbCr(int iDestX, int iDestY, 
                        int iWidth, int iHeight, 
                        int[] dblCb, int[] dblCr) 
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < _iWidth/2 && iDestY + iHeight < _iHeight/2);
        assert(dblCb.length == iWidth * iHeight);
        assert(dblCr.length == iWidth * iHeight);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * _iWidth / 2);
                
        for (int iLine = 0; iLine < iHeight; 
             iLine++, iDestLineStart += _iWidth / 2, iSrcLineStart += iWidth)
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                _adblCb[iDestLineStart + iCol] = dblCb[iSrcLineStart + iCol];
                _adblCr[iDestLineStart + iCol] = dblCr[iSrcLineStart + iCol];
            }
        }
    }
    
    /** Pastes the supplied image into this image. */
    public void putYuvImage(int iX, int iY, PsxYuvImage oYuv) {
        assert(iX > 0 && iY > 0 && 
               iX + oYuv._iWidth < _iWidth &&
               iY + oYuv._iHeight < _iHeight);
        setY(iX, iY, oYuv._iWidth, oYuv._iHeight, oYuv._adblY);
        setCbCr(iX / 2, iY / 2, 
                oYuv._iWidth / 2, oYuv._iHeight / 2,
                oYuv._adblCb, oYuv._adblCr);
    }

    public int getLuminHeight() {
        return _iHeight;
    }

    public int getLuminWidth() {
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
        return doubleArrayToBufferedImage(getLuminWidth(), getLuminHeight(), _adblY, 2);
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
