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

/** Basic YCbCr image format with 4:2:0 chroma subsampling.
 *<p>
 * This image data is intended to be used to write yuv4mpeg2 files,
 * and AVI files using the YV12 codec.
 *<p>
 * The image data is internally treated as unsigned bytes
 * in the Rec BT 601 "color space", with the range of
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
 *      -http://en.wikipedia.org/wiki/CCIR_601
 *<p>
 * Any reading or writing to this class should follow this format.
 *<p>
 */
public class Yuv4mpeg2 {

    private int _iLuminWidth;
    private int _iLuminHeight;
    private int _iChromWidth;
    private int _iChromHeight;
    
    public final static String SUB_SAMPLING = "420jpeg";

    /** Holds luminance values.
     * Package private so Yuv4mpeg2Writer can access it. */
    byte[] _abY;
    /** Holds chrominance blue values with 4:2:0 subsampling.
     * Package private so Yuv4mpeg2Writer can access it. */
    byte[] _abCb;
    /** Holds chorminance red values with 4:2:0 subsampling.
     * Package private so Yuv4mpeg2Writer can access it. */
    byte[] _abCr;
    
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
        _abY  = new byte[_iLuminWidth * _iLuminHeight];
        _abCb = new byte[_iChromWidth * _iChromHeight];
        _abCr = new byte[_abCb.length];
    }

    /** Very slow and wasteful conversion. */
    public Yuv4mpeg2(BufferedImage rgb) {
        this(rgb.getWidth(), rgb.getHeight());
        
        for (int x = 0; x < _iLuminWidth; x+=2) {
            for (int y = 0; y < _iLuminHeight; y+=2) {
                Rec601YCbCr ycc = new Rec601YCbCr(new RGB(rgb.getRGB(x  , y  )),
                                                  new RGB(rgb.getRGB(x+1, y  )),
                                                  new RGB(rgb.getRGB(x  , y+1)),
                                                  new RGB(rgb.getRGB(x+1, y+1))
                                                 );
                _abY[ (x  ) + (y  ) * _iLuminWidth ] = rc(ycc.y1);
                _abY[ (x+1) + (y  ) * _iLuminWidth ] = rc(ycc.y2);
                _abY[ (x  ) + (y+1) * _iLuminWidth ] = rc(ycc.y3);
                _abY[ (x+1) + (y+1) * _iLuminWidth ] = rc(ycc.y4);
                _abCb[x/2 + (y/2) * _iChromWidth] = rc(ycc.cb);
                _abCr[x/2 + (y/2) * _iChromWidth] = rc(ycc.cr);
            }
        }
    }

    private static byte rc(double dbl) {
        if (dbl < 0)
            return 0;
        else if (dbl > 255)
            return (byte)255;
        else
            return (byte)Math.round(dbl);
    }

    public int getWidth() {
        return _iLuminWidth;
    }

    public int getHeight() {
        return _iLuminHeight;
    }

    public byte[] getY() {
        return _abY;
    }
    public byte[] getCb() {
        return _abCb;
    }
    public byte[] getCr() {
        return _abCr;
    }

    /** Sets a luminance value.
     * @param iLuminX  X lumin pixel to set.
     * @param iLuminY  Y lumin pixel to set.
     * @param bY     New value.
     */
    public void setY(int iLuminX, int iLuminY, byte bY) {
        _abY[iLuminX + iLuminY * _iLuminWidth] = bY;
    }
    /** Sets chrominance blue value.
     * @param iChromX  X chrom pixel (1/2 lumin width)
     * @param iChromY  Y chrom pixel (1/2 lumin width)
     * @param bCb    New value.
     */
    public void setCb(int iChromX, int iChromY, byte bCb) {
        _abCb[iChromX + iChromY * _iChromWidth] = bCb;
    }
    /** Sets chrominance red value.
     * @param iChromX  X chrom pixel (1/2 lumin width)
     * @param iChromY  Y chrom pixel (1/2 lumin width)
     * @param bCr    New value.
     */
    public void setCr(int iChromX, int iChromY, byte bCr) {
        _abCr[iChromX + iChromY * _iChromWidth] = bCr;
    }

    /** Set a block of luminance values 
     * @param iDestX  Top left corner where block starts (in Luminance pixels)
     * @param iDestY  Top left corner where block starts (in Luminance pixels)
     * @param iSrcWidth   Width of block (in Luminance pixels)
     * @param iSrcHeight  Height of block (in Luminance pixels)
     * @param abY  Array of block values with the color space -128 to +127.*/
    public void setY(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth,
                     int iCopyWidth, int iCopyHeight,
                     byte[] abY)
    {
        set(_abY, iDestX + iDestY * _iLuminWidth, _iLuminHeight,
            abY, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }

    public void setCb(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth,
                     int iCopyWidth, int iCopyHeight,
                     byte[] abCb)
    {
        set(_abCb, iDestX + iDestY * _iChromWidth, _iChromWidth,
            abCb, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }

    public void setCr(int iDestX, int iDestY,
                     int iSrcOfs, int iSrcWidth, 
                     int iCopyWidth, int iCopyHeight,
                     byte[] abCr)
    {
        set(_abCr, iDestX + iDestY * _iChromWidth, _iChromWidth,
            abCr, iSrcOfs, iSrcWidth,
            iCopyWidth, iCopyHeight);
    }


    private void set(byte[] abDest, int iDestOfs, int iDestWidth,
                     byte[] abSrc, int iSrcOfs, int iSrcWidth,
                     int iCopyWidth, int iCopyHeight)
    {
        for (int iLine = 0; iLine < iCopyHeight;
             iLine++, iDestOfs += iDestWidth, iSrcOfs += iSrcWidth)
        {
            System.arraycopy(abSrc, iSrcOfs, abDest, iDestOfs, iCopyWidth);
        }
    }
    
}
