/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
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
 * PsxYuv.java
 */

package jpsxdec.mdec;

import java.awt.image.BufferedImage;
//import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import jpsxdec.util.*;

/**
 * Class to handle YUV (YCbCr) image data of the PSX MDEC chip.
 * Converts the YUV data to either yuv4mpeg2 format, or RGB format
 * (BufferedImage).
 *
 * The outputted yuv4mpeg sub-format is 4:2:0 jpeg/mpeg1
 * (since that's closest to the format PSX uses).
 * Extentsion: .yuv or .y4m
 *
 *  "All image data is in the CCIR-601 Y'CbCr colorspace..."
 *      -http://linux.die.net/man/5/yuv4mpeg
 *
 *  "In each 8 bit luminance sample, the value 16 is used for black and 
 *  235 for white, to allow for overshoot and undershoot. The values 0 
 *  and 255 are used for sync encoding. The Cb and Cr samples use the 
 *  value 128 to encode a zero value, as used when encoding a white, 
 *  grey or black area."
 *      -http://en.wikipedia.org/wiki/CCIR_601
 *
 * While this class will output the proper color space of 0 to 255, it only 
 * accepts, and stores the information internally, in a color space of:
 * Y : -128 to +127
 * Cb: -128 to +127
 * Cr: -128 to +127
 * 
 * Also:
 * http://wiki.multimedia.cx/index.php?title=YUV4MPEG2
 *
 */
public class PsxYuv {
    
    private int m_iWidth;
    private int m_iHeight;
    
    final private static String m_sChromaSubsampling = "420jpeg";
    
    private double[] m_adblY;
    private double[] m_adblCb;
    private double[] m_adblCr;
    
    /** Creates a new instance of Yuv4mpeg2 
     * @param iWidth - Width of image (in Luminance values) 
     * @param iHeight - Height of image (in Luminance values) */
    public PsxYuv(int iWidth, int iHeight) {
        assert(iWidth > 0 && iHeight > 0 && 
               (iWidth  % 2) == 0 && 
               (iHeight % 2) == 0);
        m_iWidth = iWidth;
        m_iHeight = iHeight;
        int iSize = iWidth * iHeight;
        m_adblY  = new double[iSize];
        m_adblCb = new double[iSize / 4];
        m_adblCr = new double[iSize / 4];
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
        int[] aiRGB = new int[m_iWidth * m_iHeight];
        
        for (int iLinePos = 0, iY = 0; iY < m_iHeight; iLinePos += m_iWidth, iY++) {
            int iChromLinePos = iY / 2 * m_iWidth / 2;
            for (int iX = 0; iX < m_iWidth; iX++) {
                /* Only brifly mentioned in a few YCbCr texts, 
                 * the normal equations for converting YUV to RGB
                 * require a color space of:
                 * Y : 0 to 255
                 * Cb: -128 to +127
                 * Cr: -128 to +127
                 */
                double y = m_adblY[iLinePos + iX] + 128; // <-- fix the lumin!
                double cb = m_adblCb[iChromLinePos + iX / 2];
                double cr = m_adblCr[iChromLinePos + iX / 2];
                
                int r = (int)jpsxdec.util.Math.round(y +                1.402  * cr);
                int g = (int)jpsxdec.util.Math.round(y - 0.3437  * cb - 0.7143 * cr);
                int b = (int)jpsxdec.util.Math.round(y + 1.772   * cb              );
                
                if (r < 0) {r = 0;} else {if (r > 255) r = 255;}
                if (g < 0) {g = 0;} else {if (g > 255) g = 255;}
                if (b < 0) {b = 0;} else {if (b > 255) b = 255;}
                
                aiRGB[iLinePos + iX] = r << 16 | g << 8 | b;
            }
        }
        
        BufferedImage bi = new BufferedImage(m_iWidth, m_iHeight, iImgType);
        // using the raster below would be faster, but then we would be
        // limited to just one type of BuffereImage format.
        //WritableRaster wr = bi.getRaster();
        //wr.setDataElements(0, 0, m_iWidth, m_iHeight, aiRGB);
        bi.setRGB(0, 0, m_iWidth, m_iHeight, aiRGB, 0, m_iWidth);
        return bi;
    }
    
    /** Set a block of luminance values 
     * @param iDestX - Top left corner where block starts (in Luminance pixels)
     * @param iDestY - Top left corner where block starts (in Luminance pixels)
     * @param iWidth - Width of block (in Luminance pixels)
     * @param iHeight - Height of block (in Luminance pixels)
     * @param adblY - Array of block values with the color space -128 to +127.*/
    public void setY(int iDestX, int iDestY, 
                     int iWidth, int iHeight, double[] adblY) 
    {
        assert(iDestX > 0 && iDestY > 0 && iWidth > 0 && iHeight > 0);
        assert(iDestX + iWidth < m_iWidth && iDestY + iHeight < m_iHeight);
        assert((iDestX % 2) == 0 && (iDestY % 2) == 0);
        assert(adblY.length == iWidth * iHeight);
        
        int iSrcLineStart = 0;
        int iDestLineStart = (int)(iDestX + iDestY * m_iWidth);
                
        for (int iLine = 0; iLine < iHeight; 
             iLine++, iDestLineStart += m_iWidth, iSrcLineStart += iWidth) 
        {
            for (int iCol = 0; iCol < iWidth; iCol++) {
                m_adblY[iDestLineStart + iCol] = adblY[iSrcLineStart + iCol];
            }
        }
    }
    
    /** Set a block of Cr and Cb values
     * @param iDestX - Top left corner where block starts (in Chrominance pixels)
     * @param iDestY - Top left corner where block starts (in Chrominance pixels)
     * @param iWidth - Width of block (in Chrominance pixels)
     * @param iHeight - Height of block (in Chrominance pixels)
     * @param dblCb - Array of block values with the color space -128 to +127. 
     * @param dblCr - Array of block values with the color space -128 to +127.*/
    public void setCbCr(int iDestX, int iDestY, 
                        int iWidth, int iHeight, 
                        double[] dblCb, double[] dblCr) 
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
                m_adblCb[iDestLineStart + iCol] = dblCb[iSrcLineStart + iCol];
                m_adblCr[iDestLineStart + iCol] = dblCr[iSrcLineStart + iCol];
            }
        }
    }
    
    /** Write a yuv4mpeg2 image file. */
    public void Write(String sFile) throws IOException {
        BufferedOutputStream bof = 
                new BufferedOutputStream(new FileOutputStream(sFile));
        Write(bof);
        bof.close();
    }
    
    /** Write a yuv4mpeg2 image file. */
    public void Write(OutputStream os) throws IOException {
        OutputStreamWriter ow = new OutputStreamWriter(os, "US-ASCII");

        // write the header
        ow.write("YUV4MPEG2");
        ow.write(" W" + m_iWidth);
        ow.write(" H" + m_iHeight);
        ow.write(" C" + m_sChromaSubsampling);
        ow.write(" A1:1"); // aspect ratio 1:1
        ow.write(" F15:1"); // 15 fps
        ow.write(" Ip");  // none/progressive
        ow.write('\n');
        ow.write("FRAME");
        ow.write('\n');
        ow.flush();
        ow = null;
        
        // write the data
        // "The values 0 and 255 are used for sync encoding."
        // According to MrVacBob, analog encoding is dead, so it's not really important
        int i;
        for (double d : m_adblY) {
            i = (int)jpsxdec.util.Math.round(d) + 128;
            os.write(i < 0 ? 0 : (i > 255 ? 255 : i));
        } System.out.println();
        for (double d : m_adblCb) {
            i = (int)jpsxdec.util.Math.round(d) + 128;
            os.write(i < 0 ? 0 : (i > 255 ? 255 : i));
        }
        for (double d : m_adblCr) {
            i = (int)jpsxdec.util.Math.round(d) + 128;
            os.write(i < 0 ? 0 : (i > 255 ? 255 : i));
        }
    }

    /** Pastes the supplied image into this image. */
    public void putYuvImage(int iX, int iY, PsxYuv oYuv) {
        assert(iX > 0 && iY > 0 && 
               iX + oYuv.m_iWidth < m_iWidth && 
               iY + oYuv.m_iHeight < m_iHeight);
        setY(iX, iY, oYuv.m_iWidth, oYuv.m_iHeight, oYuv.m_adblY);
        setCbCr(iX / 2, iY / 2, 
                oYuv.m_iWidth / 2, oYuv.m_iHeight / 2, 
                oYuv.m_adblCb, oYuv.m_adblCr);
    }
    
}
