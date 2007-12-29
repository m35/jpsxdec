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
 * Tim.java
 */

package jpsxdec.media;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import javax.imageio.ImageIO;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

public class Tim {
    
    public static int DebugVerbose = 2;
    
    public static class CLUT { 
        
        public final static int HEADER_SIZE = 12;
        
        long m_lngLength;
        long m_lngClutX;
        long m_lngClutY;
        long m_lngClutWidth;
        long m_lngClutHeight;
        int[] m_aiColorData;
        
        
        public CLUT(DataInputStream oDIS) 
                throws IOException, NotThisTypeException 
        {
            m_lngLength = IO.ReadUInt32LE(oDIS);
            if (m_lngLength <= 0) throw new NotThisTypeException();
            m_lngClutX = IO.ReadUInt16LE(oDIS);
            m_lngClutY = IO.ReadUInt16LE(oDIS);
            m_lngClutWidth = IO.ReadUInt16LE(oDIS);
            if (m_lngClutWidth == 0) throw new NotThisTypeException();
            m_lngClutHeight = IO.ReadUInt16LE(oDIS);
            if (m_lngClutHeight == 0) throw new NotThisTypeException();
            
            if (m_lngLength != (m_lngClutWidth * m_lngClutHeight * 2 + 12))
                throw new NotThisTypeException();
            
            m_aiColorData = new int[(int)(m_lngClutWidth * m_lngClutHeight)];
            for (int i = 0; i < m_aiColorData.length; i++) {
                m_aiColorData[i] = (int)IO.ReadUInt16LE(oDIS);
                if (DebugVerbose > 2)
                    System.err.print(String.format("%04x ", m_aiColorData[i]));
            }
            if (DebugVerbose > 2)
                System.err.println();
        }

        private int[] getColorData() {
            return m_aiColorData;
        }

        private int getPaletteSize() {
            return m_aiColorData.length;
        }
    }
    
    //***********************************************************************
    
    private static int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };
    
    public final static int HEADER_SIZE = 12;
    
    int m_iTag;
    int m_iVersion;
    long m_lngUnknown1;
    long m_lngBpp_HasColorLookupTbl;
    long m_lngUnknown2;
    
    CLUT m_oClut;
    
    long m_lngImageLength;
    long m_lngImageX;
    long m_lngImageY;
    long m_lngImageWidth;
    long m_lngImageHeight;
    byte[] m_abImageData;
    
    
    int m_iBitsPerPixel;
    boolean m_blnHasColorLookupTable;
    int m_iPixelWidth;
    int m_iPixelHeight;
    
    public Tim(InputStream oIS) throws IOException, NotThisTypeException {
        
        DataInputStream oDIS = new DataInputStream(oIS);
        m_iTag = oDIS.readUnsignedByte();
        //System.err.println(String.format("%02x", m_iTag));
        if (m_iTag != 0x10) 
            throw new NotThisTypeException();
        
        m_iVersion = oDIS.readUnsignedByte();
        //System.err.println(String.format("%02x", m_iVersion));
        if (m_iVersion != 0) 
            throw new NotThisTypeException();
        
        m_lngUnknown1 = oDIS.readUnsignedShort();
        //System.err.println(String.format("%04x", m_lngUnknown1));
        if (m_lngUnknown1 != 0)
            throw new NotThisTypeException();
        
        m_lngBpp_HasColorLookupTbl = oDIS.readUnsignedShort();
        //System.err.println(String.format("%04x", m_lngBpp_HasColorLookupTbl));
        if ((m_lngBpp_HasColorLookupTbl & 0xF4FF) != 0)
            throw new NotThisTypeException();
        
        m_lngUnknown2 = oDIS.readUnsignedShort();
        //System.err.println(String.format("%04x", m_lngUnknown2));
        if (m_lngUnknown1 != 0)
            throw new NotThisTypeException();
        
        //-------------------------------------------------
        
        m_iBitsPerPixel = BITS_PER_PIX[(int)((m_lngBpp_HasColorLookupTbl >>> 8) & 3)];
        m_blnHasColorLookupTable = (m_lngBpp_HasColorLookupTbl & 0x800) > 0;
        
        if (m_blnHasColorLookupTable)
            m_oClut = new CLUT(oDIS);
        
        m_lngImageLength = IO.ReadUInt32LE(oDIS);
        if (m_lngImageLength <= 0) throw new NotThisTypeException();
        m_lngImageX = IO.ReadUInt16LE(oDIS);
        m_lngImageY = IO.ReadUInt16LE(oDIS);
        m_lngImageWidth = IO.ReadUInt16LE(oDIS);
        if (m_lngImageWidth == 0) throw new NotThisTypeException();
        m_lngImageHeight = IO.ReadUInt16LE(oDIS);
        if (m_lngImageHeight == 0) throw new NotThisTypeException();
        
        if (m_lngImageLength != m_lngImageWidth * m_lngImageHeight * 2 + 12)
            throw new NotThisTypeException();
        
        m_abImageData = 
                IO.readByteArray(oDIS, (int)(m_lngImageWidth * m_lngImageHeight) * 2);
        
        switch (m_iBitsPerPixel) {
            case 4:
                m_iPixelWidth = (int)(m_lngImageWidth * 2 * 2);
                break;
            case 8:
                m_iPixelWidth = (int)(m_lngImageWidth * 2);
                break;
            case 16:
                m_iPixelWidth = (int)(m_lngImageWidth);
                break;
            case 24:
                m_iPixelWidth = (int)(m_lngImageWidth * 2 / 3);
                break;
            default:
                throw new RuntimeException("This should never happen");
        }
        m_iPixelHeight = (int)m_lngImageHeight;
    }
    
    public int getPaletteCount() {
        if (m_oClut == null)
            return 1;
        else
            return m_oClut.getPaletteSize() / (1 << m_iBitsPerPixel);
    }
    
    public BufferedImage toBufferedImage(int iPalette) throws IOException {

        // setup the palette
        IndexColorModel colorModel = null;
        ByteArrayOutputStream oBAOS;
        switch (m_iBitsPerPixel) {
            case 4:
                // convert CLUT to array of RGBA bytes
                oBAOS = new ByteArrayOutputStream(16 * 4);
                if (m_oClut == null) {
                    for (int i = 0; i < 256; i+=16) {
                        oBAOS.write(i); // r
                        oBAOS.write(i); // g
                        oBAOS.write(i); // b
                        if (i == 0)
                            oBAOS.write(255);
                        else
                            oBAOS.write(0);
                    }
                } else {
                    int[] aiColorData = m_oClut.getColorData();
                    for (int i = 0; i < 16; i++) {
                        oBAOS.write(Color16toColor4(aiColorData[iPalette * 16 + i]));
                    }
                }
                colorModel = new IndexColorModel(4, 16, oBAOS.toByteArray(), 0, true);
                break;
            case 8:
                // convert CLUT to array of RGBA bytes
                oBAOS = new ByteArrayOutputStream(256 * 4);
                if (m_oClut == null) {
                    for (int i = 0; i < 256; i++) {
                        oBAOS.write(i); // r
                        oBAOS.write(i); // g
                        oBAOS.write(i); // b
                        if (i == 0)
                            oBAOS.write(255);
                        else
                            oBAOS.write(0);
                    }
                } else {
                    int[] aiColorData = m_oClut.getColorData();
                    for (int i = 0; i < 256; i++) {
                        oBAOS.write(Color16toColor4(aiColorData[iPalette * 256 + i]));
                    }
                }
                colorModel = new IndexColorModel(8, 256, oBAOS.toByteArray(), 0, true);
                break;
                
        }
        
        // Now write the image data    
        
        DataInputStream oDIS = new DataInputStream(new ByteArrayInputStream(m_abImageData));
        BufferedImage bi;
        WritableRaster raster;
        
        //bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_INT_ARGB);
        int iByte, iColor16, iColor32;
        switch (m_iBitsPerPixel) {
            case 4:
                bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                raster = bi.getRaster();
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iByte = oDIS.readUnsignedByte();
                        int iNibble = iByte & 0xF;
                        raster.setSample(x, y, 0, iNibble);
                        
                        x++;
                        if (x < m_iPixelWidth) { // in case of odd width
                            iNibble = (iByte >>> 4) & 0xF;
                            raster.setSample(x, y, 0, iNibble);
                        }
                    }
                }
                break;
            case 8:
                bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                raster = bi.getRaster();
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iByte = oDIS.readUnsignedByte();
                        raster.setSample(x, y, 0, iByte);
                        if (DebugVerbose > 2)
                            System.err.print(String.format("%02x ",iByte));
                    }
                    if (DebugVerbose > 2)
                        System.err.println();
                }
                break;
            case 16:
                bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iColor16 = (int)IO.ReadUInt16LE(oDIS);
                        iColor32 = Color16toColor32(iColor16);
                        bi.setRGB(x, y, iColor32);
                    }
                }
                break;
            case 24:
                bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        int r = oDIS.readUnsignedByte();
                        int g = oDIS.readUnsignedByte();
                        int b = oDIS.readUnsignedByte();
                        iColor32 = RGBA(r, g, b, 255);
                        bi.setRGB(x, y, iColor32);
                    }
                    // TODO: Need to check this logic
                    if ((m_iPixelWidth % 2) == 1) // in case of odd width
                        oDIS.skip(1);
                }
                break;
            default:
                throw new RuntimeException("This should never happen");
        }
        
        return bi;
    }

    /** Works the same as
     *  <code>
     *  int CONVERT_5_TO_8_BIT(int i) {
     *    return (int)Math.round((double)i / 31.0);
     *  }
     *  </code> */
    private static int[] CONVERT_5_TO_8_BIT = new int[/*32*/] 
   {  0,   8,  16,  25,  33,  41,  49,  58,  
     66,  74,  82,  90,  99, 107, 115, 123, 
    132, 140, 148, 156, 165, 173, 181, 189, 
    197, 206, 214, 222, 230, 239, 247, 255 };
    
    private int Color16toColor32(int i) {
        int b = CONVERT_5_TO_8_BIT[(i >>> 10) & 0x1F];
        int g = CONVERT_5_TO_8_BIT[(i >>>  5) & 0x1F];
        int r = CONVERT_5_TO_8_BIT[(i >>>  0) & 0x1F];
        
        int a;
        if (r == 0 && g == 0 && b == 0) {
            if ((i & 0x8000) == 0)
                // black, and the alpha bit is NOT set
                a = 0; // totally transparent
            else 
                // black, and the alpha bit IS set
                a = 255; // totally opaque
        } else {
            if ((i & 0x8000) == 0)
                // some color, and the alpha bit is NOT set
                a = 255; // totally opaque
            else 
                // some color, and the alpha bit IS set
                a = 128; // some variance of transparency (using 128)
        }
        
        return RGBA(r, g, b, a);
    }
    
    private byte[] Color16toColor4(int i) {
        byte[] abRGBA = new byte[4];
        abRGBA[2] = (byte)CONVERT_5_TO_8_BIT[(i >>> 10) & 0x1F]; // b
        abRGBA[1] = (byte)CONVERT_5_TO_8_BIT[(i >>>  5) & 0x1F]; // g
        abRGBA[0] = (byte)CONVERT_5_TO_8_BIT[(i >>>  0) & 0x1F]; // r
        
        if (abRGBA[0] == 0 && abRGBA[1] == 0 && abRGBA[2] == 0) {
            if ((i & 0x8000) == 0)
                // black, and the alpha bit is NOT set
                abRGBA[3] = (byte)0; // totally transparent
            else 
                // black, and the alpha bit IS set
                abRGBA[3] = (byte)255; // totally opaque
        } else {
            if ((i & 0x8000) == 0)
                // some color, and the alpha bit is NOT set
                abRGBA[3] = (byte)255; // totally opaque
            else 
                // some color, and the alpha bit IS set
                abRGBA[3] = (byte)128; // some variance of transparency (using 128)
        }
        
        return abRGBA;
    }
    
    private int RGBA(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }
    
}
