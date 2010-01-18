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

package jpsxdec.formats;

import java.awt.image.BufferedImage;
import jpsxdec.formats.RgbIntImage.RGB;

/** Basic YCbCr image format with 4:2:0 chroma subsampling.
 *<p>
 * This image data is intended to be used to write yuv4mpeg2 files,
 * and AVI files using the YV12 codec.
 *<p>
 * The image data is internally maintained as doubles, with a color space
 * CCIR_601, but with a general range of
 *<pre>
 * Y : -128 to +127
 * Cb: -128 to +127
 * Cr: -128 to +127
 *</pre>
 * Any reading or writing to this class should follow this format.
 * Therefore data from an IDCT can be copied into this class directly, but
 * the data needs to be shifted +128, rounded and clamped before being wrtten
 * to yuv4mpeg2 or AVI.
 *<p>
 *  "All image data is in the CCIR-601 Y'CbCr colorspace..."
 *      -http://linux.die.net/man/5/yuv4mpeg
 *<p>
 *<blockquote>
 *  "In each 8 bit luminance sample, the value 16 is used for black and 
 *  235 for white, to allow for overshoot and undershoot. The values 0 
 *  and 255 are used for sync encoding. The Cb and Cr samples use the 
 *  value 128 to encode a zero value, as used when encoding a white, 
 *  grey or black area."
 * </blockquote>
 *      -http://en.wikipedia.org/wiki/CCIR_601
 * <p>
 * Also:
 * http://wiki.multimedia.cx/index.php?title=YUV4MPEG2
 *
 */
public class Yuv4mpeg2 {

    /** Holds a standard CCIR_601 color space YCbCr color, except the values
     * are in the general range of
     *<pre>
     * Y : -128 to +127
     * Cb: -128 to +127
     * Cr: -128 to +127
     *</pre>
     * Has methods to convert to and from RGB. */
    public static class CCIR601YCbCr {
        public double y, cb, cr;

        public CCIR601YCbCr(double _y, double _cb, double _cr) {
            this.y = _y;
            this.cb = _cb;
            this.cr = _cr;
        }

        public CCIR601YCbCr(RGB rgb) {
            y =   0.299  * (rgb.r - 128) + 0.587  * (rgb.g - 128) + 0.114  * (rgb.b - 128);
            cb = -0.1687 * (rgb.r - 128) - 0.3313 * (rgb.g - 128) + 0.5    * (rgb.b - 128);
            cr =  0.5    * (rgb.r - 128) - 0.4187 * (rgb.g - 128) - 0.0813 * (rgb.b - 128);
        }

        public RGB toRgb() {
            return new RGB(
                    y +128 +                    (  1.402  * cr),
                    y +128 + (-0.34414  * cb) + (-0.71414 * cr),
                    y +128 + (1.772     * cb)
           );
        }
        public String toString() {
            return String.format("(%f, %f, %f)", y, cb, cr);
        }
    }


    private int _iLuminWidth;
    private int _iLuminHeight;
    private int _iChromWidth;
    private int _iChromHeight;
    
    public final static String SUB_SAMPLING = "420jpeg";

    /** Holds luminance values.
     * Package private so Yuv4mpeg2Writer can access it. */
    double[] _adblY;
    /** Holds chrominance blue values, subsampled like jpeg or mpeg1.
     * Package private so Yuv4mpeg2Writer can access it. */
    double[] _adblCb;
    /** Holds chorminance red values, subsampled like jpeg or mpeg1.
     * Package private so Yuv4mpeg2Writer can access it. */
    double[] _adblCr;
    
    /** Creates a new instance of Yuv4mpeg2 
     * @param iSrcWidth - Width of image (in Luminance values)
     * @param iSrcHeight - Height of image (in Luminance values) */
    public Yuv4mpeg2(int iWidth, int iHeight) {
        if (iWidth < 2 || iHeight < 2 ||
               (iWidth % 2) != 0 ||
               (iHeight % 2) != 0) {
            throw new IllegalArgumentException("Invalid y4m dimensions.");
        }
        _iLuminWidth  = iWidth;
        _iLuminHeight = iHeight;
        _iChromWidth = iWidth / 2;
        _iChromHeight = iHeight / 2;
        _adblY  = new double[_iLuminWidth * _iLuminHeight];
        _adblCb = new double[_iChromWidth * _iChromHeight];
        _adblCr = new double[_adblCb.length];
    }
    
    public int getWidth() {
        return _iLuminWidth;
    }

    public int getHeight() {
        return _iLuminHeight;
    }

    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values. Uses default image type. */
    public BufferedImage toBufferedImage() {
        return toBufferedImage(BufferedImage
                //.TYPE_USHORT_565_RGB); 
                .TYPE_INT_RGB);
    }
    
    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values */
    public BufferedImage toBufferedImage(int iImgType) {
        int[] aiRGB = new int[_iLuminWidth * _iLuminHeight];
        
        for (int iLinePos = 0, iY = 0; iY < _iLuminHeight; iLinePos += _iLuminWidth, iY++) {
            for (int iX = 0; iX < _iLuminWidth; iX++) {

                double y = _adblY[iLinePos + iX] + 128;
                double cb = _adblCb[iLinePos + iX / 2];
                double cr = _adblCr[iLinePos + iX / 2];
                
                int r = (int)jpsxdec.util.Maths.round(y +                1.402  * cr);
                int g = (int)jpsxdec.util.Maths.round(y - 0.3437  * cb - 0.7143 * cr);
                int b = (int)jpsxdec.util.Maths.round(y + 1.772   * cb              );
                
                if (r < 0) {r = 0;} else {if (r > 255) r = 255;}
                if (g < 0) {g = 0;} else {if (g > 255) g = 255;}
                if (b < 0) {b = 0;} else {if (b > 255) b = 255;}
                
                aiRGB[iLinePos + iX] = r << 16 | g << 8 | b;
            }
        }
        
        BufferedImage bi = new BufferedImage(_iLuminWidth, _iLuminHeight, iImgType);
        // using the raster below would be faster, but then we would be
        // limited to just one type of color space.
        //WritableRaster wr = bi.getRaster();
        //wr.setDataElements(0, 0, m_iWidth, m_iHeight, aiRGB);
        bi.setRGB(0, 0, _iLuminWidth, _iLuminHeight, aiRGB, 0, _iLuminWidth);
        return bi;
    }

    /** Sets a luminance value.
     * @param iLuminX  X lumin pixel to set.
     * @param iLuminY  Y lumin pixel to set.
     * @param dblY     New value.
     */
    public void setY(int iLuminX, int iLuminY, double dblY) {
        _adblY[iLuminX + iLuminY * _iLuminWidth] = dblY;
    }
    /** Sets chrominance blue value.
     * @param iChromX  X chrom pixel (1/2 lumin width)
     * @param iChromY  Y chrom pixel (1/2 lumin width)
     * @param dblCb    New value.
     */
    public void setCb(int iChromX, int iChromY, double dblCb) {
        _adblCb[iChromX + iChromY * _iChromWidth] = dblCb;
    }
    /** Sets chrominance red value.
     * @param iChromX  X chrom pixel (1/2 lumin width)
     * @param iChromY  Y chrom pixel (1/2 lumin width)
     * @param dblCr    New value.
     */
    public void setCr(int iChromX, int iChromY, double dblCr) {
        _adblCr[iChromX + iChromY * _iChromWidth] = dblCr;
    }

    /** Set a block of luminance values 
     * @param iDestX  Top left corner where block starts (in Luminance pixels)
     * @param iDestY  Top left corner where block starts (in Luminance pixels)
     * @param iSrcWidth   Width of block (in Luminance pixels)
     * @param iSrcHeight  Height of block (in Luminance pixels)
     * @param adblCb  Array of block values with the color space -128 to +127.*/
    public void setY(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth,
                     int iCopyWidth, int iCopyHeight,
                     double[] adblY)
    {
        set(_adblY, iDestX + iDestY * _iLuminWidth, _iLuminHeight,
            adblY, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }

    public void setCb(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth,
                     int iCopyWidth, int iCopyHeight,
                     double[] adblCb)
    {
        set(_adblCb, iDestX + iDestY * _iChromWidth, _iChromWidth,
            adblCb, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }

    public void setCr(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth, 
                     int iCopyWidth, int iCopyHeight,
                     double[] adblCr)
    {
        set(_adblCr, iDestX + iDestY * _iChromWidth, _iChromWidth,
            adblCr, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }


    private void set(double[] adblDest, int iDestOfs, int iDestWidth,
                     double[] adblSrc, int iSrcOfs, int iSrcWidth, 
                     int iCopyWidth, int iCopyHeight)
    {
        for (int iLine = 0; iLine < iCopyHeight;
             iLine++, iDestOfs += iDestWidth, iSrcOfs += iSrcWidth)
        {
            System.arraycopy(adblSrc, iSrcOfs, adblDest, iDestOfs, iCopyWidth);
        }
    }
    
}
