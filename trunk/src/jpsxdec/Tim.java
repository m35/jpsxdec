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
 *
 */

package jpsxdec;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import jpsxdec.util.LittleEndianIO;
import jpsxdec.util.NotThisTypeException;

public class Tim {
    
    public static int DebugVerbose = 2;
    
    private static int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };
    
    public static void Test() {
        try {
            FileInputStream oFIS = new FileInputStream("siteadecode.bin");
            Tim t = new Tim(oFIS);
            ImageIO.write(t.toBufferedImage(), "png", new File("timtest.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
    
    
    public static class CLUT { 
        long m_lngLength;
        long m_lngClutX;
        long m_lngClutY;
        long m_lngClutWidth;
        long m_lngClutHeight;
        int[] m_aiColorData;
        
        
        public CLUT(DataInputStream oDIS) throws IOException {
            m_lngLength = LittleEndianIO.ReadUInt32LE(oDIS);
            m_lngClutX = LittleEndianIO.ReadUInt16LE(oDIS);
            m_lngClutY = LittleEndianIO.ReadUInt16LE(oDIS);
            m_lngClutWidth = LittleEndianIO.ReadUInt16LE(oDIS);
            m_lngClutHeight = LittleEndianIO.ReadUInt16LE(oDIS);
            m_aiColorData = new int[(int)(m_lngClutWidth * m_lngClutHeight)];
            for (int i = 0; i < m_aiColorData.length; i++) {
                m_aiColorData[i] = (int)LittleEndianIO.ReadUInt16LE(oDIS);
                if (DebugVerbose > 2)
                    System.err.print(String.format("%04x ", m_aiColorData[i]));
            }
            if (DebugVerbose > 2)
                System.err.println();
        }

        private int[] getColorData() {
            return m_aiColorData;
        }
    }
    
    //***********************************************************************
    
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
        if (m_iTag != 0x10) throw new NotThisTypeException();
        
        m_iVersion = oDIS.readUnsignedByte();
        if (m_iVersion != 0) throw new NotThisTypeException();
        
        m_lngUnknown1 = oDIS.readUnsignedShort();
        
        m_lngBpp_HasColorLookupTbl = oDIS.readUnsignedShort();
        
        m_lngUnknown2 = oDIS.readUnsignedShort();
        
        //-------------------------------------------------
        
        m_iBitsPerPixel = BITS_PER_PIX[(int)((m_lngBpp_HasColorLookupTbl >>> 8) & 3)];
        m_blnHasColorLookupTable = (m_lngBpp_HasColorLookupTbl & 0x800) > 0;
        
        if (m_blnHasColorLookupTable)
            m_oClut = new CLUT(oDIS);
        
        m_lngImageLength = LittleEndianIO.ReadUInt32LE(oDIS);
        m_lngImageX = LittleEndianIO.ReadUInt16LE(oDIS);
        m_lngImageY = LittleEndianIO.ReadUInt16LE(oDIS);
        m_lngImageWidth = LittleEndianIO.ReadUInt16LE(oDIS);
        m_lngImageHeight = LittleEndianIO.ReadUInt16LE(oDIS);
        
        m_abImageData = new byte[(int)(m_lngImageWidth * m_lngImageHeight) * 2];
        
        if (oDIS.read(m_abImageData) != m_abImageData.length)
            throw new IOException("Unexpcted end of stream.");
        
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
    
    public BufferedImage toBufferedImage() throws IOException {
        
        DataInputStream oDIS = new DataInputStream(new ByteArrayInputStream(m_abImageData));
        BufferedImage bi;
        int[] aiClut = null;
        if (m_oClut == null) {
            // only create a grayscale palette if the color is indexed
            if (m_iBitsPerPixel == 4 || m_iBitsPerPixel == 8) {
                aiClut = new int[(int)Math.pow(2, m_iBitsPerPixel)];
                throw new UnsupportedOperationException("Haven't done this yet");
            }
        } else {
            aiClut = m_oClut.getColorData();
        }
            
        
        bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_INT_ARGB);
        int iByte, iColor16, iColor32;
        switch (m_iBitsPerPixel) {
            case 4:
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iByte = oDIS.readUnsignedByte();
                        
                        int iNibble = iByte & 0xF;
                        iColor16 = aiClut[iNibble];
                        iColor32 = Color16toColor32(iColor16);
                        bi.setRGB(x, y, iColor32);
                        
                        x++;
                        iNibble = (iByte >>> 4) & 0xF;
                        iColor16 = aiClut[iNibble];
                        iColor32 = Color16toColor32(iColor16);
                        bi.setRGB(x, y, iColor32);
                    }
                }
                break;
            case 8:
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iByte = oDIS.readUnsignedByte();
                        if (DebugVerbose > 2)
                            System.err.print(String.format("%02x ",iByte));
                        iColor16 = aiClut[iByte];
                        iColor32 = Color16toColor32(iColor16);
                        bi.setRGB(x, y, iColor32);
                    }
                    if (DebugVerbose > 2)
                        System.err.println();
                }
                break;
            case 16:
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        iColor16 = oDIS.readUnsignedShort();
                        iColor32 = Color16toColor32(iColor16);
                        bi.setRGB(x, y, iColor32);
                    }
                }
                break;
            case 24:
                for (int y = 0; y < m_iPixelHeight; y++) {
                    for (int x = 0; x < m_iPixelWidth; x++) {
                        int r,g,b;
                        r = oDIS.readUnsignedByte();
                        g = oDIS.readUnsignedByte();
                        b = oDIS.readUnsignedByte();
                        iColor32 = RGBA(r, g, b, 255);
                        bi.setRGB(x, y, iColor32);
                        
                        x++;
                        r = oDIS.readUnsignedByte();
                        g = oDIS.readUnsignedByte();
                        b = oDIS.readUnsignedByte();
                        iColor32 = RGBA(r, g, b, 255);
                        bi.setRGB(x, y, iColor32);
                    }
                }
                break;
            default:
                throw new RuntimeException("This should never happen");
        }
        
        return bi;
    }

  static int[] CONVERT_5_TO_8_BIT = new int[/*32*/] 
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
                a = 128; // some variance of transparency (represented by 128)
        }
        
        return RGBA(r, g, b, a);
    }
    
    private int RGBA(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }
    
}
