/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * PsxYuvInt.java
 */

package jpsxdec.mdec;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

public class PsxYuvInt {
    
    private int m_iWidth;
    private int m_iHeight;
    
    final private static String m_sChromaSubsampling = "420jpeg";
    
    private int[] m_aiY;
    private int[] m_aiCb;
    private int[] m_aiCr;
    
    private IOException m_oDecodingError;

    public IOException getDecodingError() {
        return m_oDecodingError;
    }

    void setDecodingError(IOException ex) {
        this.m_oDecodingError = ex;
    }
    
    /** Creates a new instance of PsxYuv. 
     * @param iWidth - Width of image (in Luminance values) 
     * @param iHeight - Height of image (in Luminance values) */
    public PsxYuvInt(int iWidth, int iHeight) {
        assert(iWidth > 0 && iHeight > 0 && 
               (iWidth  % 2) == 0 && 
               (iHeight % 2) == 0);
        m_iWidth = iWidth;
        m_iHeight = iHeight;
        int iSize = iWidth * iHeight;
        m_aiY  = new int[iSize];
        m_aiCb = new int[iSize / 4];
        m_aiCr = new int[iSize / 4];
    }
    
    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values. Uses default image type. */
    public BufferedImage toBufferedImage() {
        return toBufferedImage(BufferedImage
                //.TYPE_USHORT_565_RGB); 
                .TYPE_INT_RGB);
    }
    /** Converts yuv image to a BufferedImage, converting, rounding, and 
     * clamping RGB values. */
    public BufferedImage toBufferedImage(int iImgType) {
        int[] aiARGB = new int[m_iWidth * m_iHeight];
        
        YCbCr2RGB(m_aiY, m_aiCb, m_aiCr, aiARGB, m_iWidth);
        
        BufferedImage bi = new BufferedImage(m_iWidth, m_iHeight, iImgType);
        
        if (iImgType == BufferedImage.TYPE_INT_RGB) {
            // using the raster should be faster, but can only be used
            // if in the proper color space
            WritableRaster wr = bi.getRaster();
            wr.setDataElements(0, 0, m_iWidth, m_iHeight, aiARGB);
        } else {
            bi.setRGB(0, 0, m_iWidth, m_iHeight, aiARGB, 0, m_iWidth);
        }
        return bi;
    }
    
    
    private final static int CR_FAC = 0x166EA;             /* 1.402   * 2^16 */
    private final static int CB_FAC = 0x1C5A2;             /* 1.772   * 2^16 */
    private final static int CR_DIFF_FAC = 0xB6DC;         /* 0.7143 * 2^16 */
    private final static int CB_DIFF_FAC = 0x57FD;         /* 0.3437 * 2^16 */
    /** This function, and related constants, are originally from 
     *  Joerg Anders's MPG-1 player. Copyright Joerg Anders, licensed under
     *  GNU GENERAL PUBLIC LICENSE Version 2.
     *  http://vsr.informatik.tu-chemnitz.de/~jan/MPEG/MPEG_Play.html
     * <p>
     *  Modified to work with jpsxdec and the unique Playstation quirks.
     */
    private static void YCbCr2RGB(
            int PixelsY[], int PixelsCb[], int PixelsCr[], 
            int PixelsRGB[],
            int width) 
    {
        int red, green, blue, luminance, cr, cb, cr_green, cb_green, chrom_idx, j;

        /*  because one crominance value is applied to 4 luminace values,
         *  2 "pointers" are established, which point to the 2 lines containing
         *  the appropriate luminace values: */

        int lumin_idx1 = 0, lumin_idx2 = width;

        int end_of_line = width; // 

        int chrom_count = PixelsY.length >>> 2; // the size of the crominance values

        for (chrom_idx = 0; chrom_idx < chrom_count; chrom_idx++) {  // For all crominance values ...
            cb = PixelsCb[chrom_idx] - 128; // extract the
            cr = PixelsCr[chrom_idx] - 128; // chrominace information

            cr_green = cr * CR_DIFF_FAC;
            cb_green = cb * CB_DIFF_FAC;

            cb *= CB_FAC;
            cr *= CR_FAC;

            for (j = 0; j < 2; j++) { // apply to 2 neighbouring points
                luminance = PixelsY[lumin_idx1] << 16; // extract lum.
                red   = (luminance + cr);
                blue  = (luminance + cb) >> 16;
                green = (luminance - cr_green - cb_green) >> 8;

                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP

                PixelsRGB[lumin_idx1] = ( red | green | blue );

                lumin_idx1++; // next point in first line

                luminance = PixelsY[lumin_idx2] << 16; // extract lum.
                red = (luminance + cr);
                blue = (luminance + cb) >> 16;
                green = (luminance - cr_green - cb_green) >> 8;

                red   = (red   > 0xff0000) ? 0xff0000 : (red   < 0) ? 0 : red   & 0xff0000; //CLAMP
                green = (green > 0x00ff00) ? 0x00ff00 : (green < 0) ? 0 : green & 0xff00;   //CLAMP
                blue  = (blue  > 0x0000ff) ? 0x0000ff : (blue  < 0) ? 0 : blue;             //CLAMP

                PixelsRGB[lumin_idx2] =  ( red  | green  | blue);
                lumin_idx2++; // next point in second line

            }
            if (lumin_idx1 >= end_of_line) { // end of line ?
                lumin_idx1  = lumin_idx2;
                lumin_idx2  += width;
                end_of_line = lumin_idx2;
            }
        }
    }
    
    
    /** Set a block of luminance values.
     * @param iDestX - Top left corner where block starts (in Luminance pixels)
     * @param iDestY - Top left corner where block starts (in Luminance pixels)
     * @param iWidth - Width of block (in Luminance pixels)
     * @param iHeight - Height of block (in Luminance pixels)
     * @param adblY - Array of block values with the color space -128 to +127.*/
    public void setY(int iDestX, int iDestY, 
                     int iWidth, int iHeight, int[] adblY) 
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < m_iWidth && iDestY + iHeight < m_iHeight);
        assert((iDestX % 2) == 0 && (iDestY % 2) == 0);
        assert(adblY.length == iWidth * iHeight);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * m_iWidth);
                
        for (int iLine = 0; 
             iLine < iHeight; 
             iLine++, iDestLineStart += m_iWidth, iSrcLineStart += iWidth) 
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                m_aiY[iDestLineStart + iCol] = adblY[iSrcLineStart + iCol];
            }
        }
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
                        int[] dblCb, int[] dblCr) 
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < m_iWidth/2 && iDestY + iHeight < m_iHeight/2);
        assert(dblCb.length == iWidth * iHeight);
        assert(dblCr.length == iWidth * iHeight);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * m_iWidth / 2);
                
        for (int iLine = 0; iLine < iHeight; 
             iLine++, iDestLineStart += m_iWidth / 2, iSrcLineStart += iWidth) 
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                m_aiCb[iDestLineStart + iCol] = dblCb[iSrcLineStart + iCol];
                m_aiCr[iDestLineStart + iCol] = dblCr[iSrcLineStart + iCol];
            }
        }
    }
    
    /** Pastes the supplied image into this image. */
    public void putYuvImage(int iX, int iY, PsxYuvInt oYuv) {
        assert(iX > 0 && iY > 0 && 
               iX + oYuv.m_iWidth < m_iWidth && 
               iY + oYuv.m_iHeight < m_iHeight);
        setY(iX, iY, oYuv.m_iWidth, oYuv.m_iHeight, oYuv.m_aiY);
        setCbCr(iX / 2, iY / 2, 
                oYuv.m_iWidth / 2, oYuv.m_iHeight / 2, 
                oYuv.m_aiCb, oYuv.m_aiCr);
    }

    /** Converts the YUV image to BMP style array of BGR bytes. 
     *  Intended for writing uncompressed AVI frames. In theory this
     *  should be faster than converting to BufferedImage and then
     *  using ImageIO to write it out as BMP and extracting just the image
     *  data.
     *  <p>
     *  BMP images store the image data upside down, in BGR order, and the
     *  width padded to a 4 byte boundary. */
    public byte[] toRowReverseBGRArray() {
        // pad the width to a 4 byte boundary
        int iBmpWidth = (m_iWidth*3 + 3) & 0xFFFFFFFC;
        
        // allocate the BMP image
        byte[] ab = new byte[iBmpWidth * m_iHeight];

        // iY to hold the current YUV line being read,
        // iBmpLinePos hold the start of the current BMP line being written
        for (int iY = m_iHeight-1, iBmpLinePos = 0; 
             iY >= 0; 
             iY--, iBmpLinePos+=iBmpWidth) 
        {
            // iLuminLinePos holds the start of the current 'Y' line being read
            // iChromLinePos hold the start of the current 'Cb/Cr' line being read
            int iLuminLinePos = iY * m_iWidth;
            int iChromLinePos = (iY / 2) * (m_iWidth / 2);
            for (int iX = 0, iBmpPos = iBmpLinePos; iX < m_iWidth; iX++) {
                
                //System.out.print(iBmpPos + ", ");
                double y = m_aiY[iLuminLinePos + iX] + 128;
                double cb = m_aiCb[iChromLinePos + iX / 2];
                double cr = m_aiCr[iChromLinePos + iX / 2];
                
                int r = (int)jpsxdec.util.Math.round(y +                1.402  * cr);
                int g = (int)jpsxdec.util.Math.round(y - 0.3437  * cb - 0.7143 * cr);
                int b = (int)jpsxdec.util.Math.round(y + 1.772   * cb              );
                
                if (r < 0) {r = 0;} else {if (r > 255) r = 255;}
                if (g < 0) {g = 0;} else {if (g > 255) g = 255;}
                if (b < 0) {b = 0;} else {if (b > 255) b = 255;}
                
                ab[iBmpPos++] = (byte)b;
                ab[iBmpPos++] = (byte)g;
                ab[iBmpPos++] = (byte)r;
            }
            //System.out.println();
        }
        
        return ab;
    }
    
}
