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
 * Tim.java
 */

package jpsxdec.media;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Hashtable;
import java.util.Map.Entry;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** The Playstation 1 TIM file. Used in many Playstation games.
 * This is based on the excellent documentation here: 
 * http://wiki.qhimm.com/PSX/TIM_file
 * <p>
 * The semi-transparent bit used in TIM files is represented in BufferedImages
 * by any alpha value from 1 to 254 (written as 128).
 */
public class Tim {
    
    public static int DebugVerbose = 2;
    
    /** The TIM Color Lookup Table (CLUT). */
    public static class CLUT { 
        
        public final static int HEADER_SIZE = 12;
        
        final long m_lngLength;
        final long m_lngClutX;
        final long m_lngClutY;
        final long m_lngClutWidth;
        final long m_lngClutHeight;
        final int[] m_aiColorData;
        
        /** Read a CLUT from an InputStream. */
        public CLUT(InputStream is) 
                throws IOException, NotThisTypeException 
        {
            m_lngLength = IO.ReadUInt32LE(is);
            if (m_lngLength <= 0) throw new NotThisTypeException();
            m_lngClutX = IO.ReadUInt16LE(is);
            m_lngClutY = IO.ReadUInt16LE(is);
            m_lngClutWidth = IO.ReadUInt16LE(is);
            if (m_lngClutWidth == 0) throw new NotThisTypeException();
            m_lngClutHeight = IO.ReadUInt16LE(is);
            if (m_lngClutHeight == 0) throw new NotThisTypeException();
            
            if (m_lngLength != (m_lngClutWidth * m_lngClutHeight * 2 + 12))
                throw new NotThisTypeException();
            
            m_aiColorData = new int[(int)(m_lngClutWidth * m_lngClutHeight)];
            for (int i = 0; i < m_aiColorData.length; i++) {
                m_aiColorData[i] = (int)IO.ReadUInt16LE(is);
                if (DebugVerbose > 2)
                    System.err.print(String.format("%04x ", m_aiColorData[i]));
            }
            if (DebugVerbose > 2)
                System.err.println();
        }
        
        /** Create a CLUT based on a ready-made CLUT palette. */
        public CLUT(int[] aiPalette) {
            m_aiColorData = aiPalette;
            m_lngClutX = 0;
            m_lngClutY = 0;
            m_lngClutWidth = aiPalette.length;
            m_lngClutHeight = aiPalette.length / m_lngClutWidth;
            m_lngLength = m_lngClutWidth * m_lngClutHeight * 2 + 12;
        }

        /** Write the CLUT to a stream. */
        public void write(OutputStream os) throws IOException {
            IO.WriteInt32LE(os, m_lngLength);
            IO.WriteInt16LE(os, m_lngClutX);
            IO.WriteInt16LE(os, m_lngClutY);
            IO.WriteInt16LE(os, m_lngClutWidth);
            IO.WriteInt16LE(os, m_lngClutHeight);
            for (int i : m_aiColorData) {
                IO.WriteInt16LE(os, i);
            }
        }
        
        private int[] getColorData() {
            return m_aiColorData;
        }

        private int getPaletteSize() {
            return m_aiColorData.length;
        }
        
        public String toString() {
            return String.format(
                    "Len:%d X:%d Y:%d Width:%d Height:%d",
                    m_lngLength,
                    m_lngClutX,
                    m_lngClutY,
                    m_lngClutWidth,
                    m_lngClutHeight);
        }
    }
    
    //--------------------------------------------------------------------------
    //-- Fields ----------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    private static int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };
    
    public final static int HEADER_SIZE = 12;
    
    /** The 0x10 tag at the start of every TIM file. */
    final int m_iTag;
    /** Version of the TIM file. The only known version is 0. */
    final int m_iVersion;
    final long m_lngUnknown1;
    /** 16-bit value specifying the bits-per-pixel and if it has a CLUT. */
    final long m_lngBpp_HasColorLookupTbl;
    final long m_lngUnknown2;
    
    /** The color lookup table for the TIM.  */
    final CLUT m_oClut;
    
    /** Size of the data to follow. */
    final long m_lngImageLength;
    /** X position of the TIM file (often 0). */
    final long m_lngImageX;
    /** Y position of the TIM file (often 0). */
    final long m_lngImageY;
    /** Width of the image data in 16-bit values. */
    final long m_lngImageWidth;
    /** Height of the image in pixels. */
    final long m_lngImageHeight;
    /** The raw image data of the TIM. */
    final byte[] m_abImageData;
    
    /** Extrated and converted from m_lngBpp_HasColorLookupTbl. */
    final int m_iBitsPerPixel;
    /** Extrated and converted from m_lngBpp_HasColorLookupTbl. */
    final boolean m_blnHasColorLookupTable;
    /** Height of the image in pixels.
     * Calculated based on m_lngImageWidth and m_iBitsPerPixel. */
    final int m_iPixelWidth;
    /** Height of the image in pixels.
     * Same as m_lngImageHeight. */
    final int m_iPixelHeight;
    
    //--------------------------------------------------------------------------
    //-- Constructors ----------------------------------------------------------
    //--------------------------------------------------------------------------
    
    /** Parse and deserialize a TIM file from a stream. */
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
        if (m_lngUnknown2 != 0)
            throw new NotThisTypeException();
        
        //-------------------------------------------------
        
        m_iBitsPerPixel = BITS_PER_PIX[(int)((m_lngBpp_HasColorLookupTbl >>> 8) & 3)];
        m_blnHasColorLookupTable = (m_lngBpp_HasColorLookupTbl & 0x800) > 0;
        
        if (m_blnHasColorLookupTable)
            m_oClut = new CLUT(oDIS);
        else
            m_oClut = null;
        
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


    /** Create a TIM file with the specified width/height, 
     *  bits-per-pixel, and array of RGB data. Generates a 16 or 24 bit
     *  TIM file (without a CLUT).
     * @param aiBuffImg  ARGB data
     */
    public Tim(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel) {
        if (iWidth * iHeight != aiBuffImg.length)
            throw new IllegalArgumentException("Dimentions do not match data.");
        
        m_iPixelWidth   = iWidth;
        m_iPixelHeight  = iHeight;
        
        m_iBitsPerPixel = iBitsPerPixel;
        int iBitsPerPixReverseLookup;
        switch (m_iBitsPerPixel) {
            case 16: 
                m_lngImageWidth = m_iPixelWidth;
                iBitsPerPixReverseLookup = 2; 
                break;
            case 24: 
                if (m_iPixelWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                m_lngImageWidth = m_iPixelWidth * 3 / 2;
                iBitsPerPixReverseLookup = 3; 
                break;
            default:
                throw new IllegalArgumentException("Not a valid bits/pixel");
        }
        m_lngImageHeight = m_iPixelHeight;
        
        m_blnHasColorLookupTable = false;
            
        m_iTag = 0x10;
        m_iVersion = 0;
        m_lngUnknown1 = 0;
        // doesn't have a CLUT
        m_lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x000;
        m_oClut = null;
        m_lngUnknown2 = 0;
        m_lngImageX = 0;
        m_lngImageY = 0;
        m_lngImageLength = m_lngImageWidth * m_lngImageHeight * 2 + 12;
        
        ByteArrayOutputStream baos = 
                new ByteArrayOutputStream((int)(m_lngImageWidth * m_lngImageHeight * 2));
        
        // convert the image data to TIM colors and write it using proper BPP
        if (m_iBitsPerPixel == 24) {
            for (int i = 0; i < aiBuffImg.length; i++) {
                int iRGB = aiBuffImg[i];
                int iTIM = iRGB & 0xFFFFFF; // transparency is lost

                baos.write((iTIM >>> 16) & 0xFF);
                baos.write((iTIM >>>  8) & 0xFF);
                baos.write( iTIM         & 0xFF);
            }
        } else if (m_iBitsPerPixel == 16) {
            for (int i = 0; i < aiBuffImg.length; i++) {
                int iRGB = aiBuffImg[i];
                int iTIM = Color32toColor16(iRGB);
                
                baos.write( iTIM        & 0xFF);
                baos.write((iTIM >>> 8) & 0xFF);
            }
        }        
        
        m_abImageData = baos.toByteArray();
    }
    
    /** Create a TIM file with the specified width/height, 
     *  bits-per-pixel, and array of RGB data. Generates a 4 or 8 bit
     *  TIM file (with a CLUT). The palette is manually generated from
     *  the image data. 
     *  @param aiBuffImg   ARGB data
     *  @param blnUseClut  should always be true (use different constructor to not use a CLUT)
     */
    public Tim(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel, boolean blnUseClut) {
        if (!blnUseClut)
            throw new IllegalArgumentException("Use a different construtor if you don't want CLUT.");

        m_iPixelWidth   = iWidth;
        m_iPixelHeight  = iHeight;
        m_iBitsPerPixel = iBitsPerPixel;
        int iBitsPerPixReverseLookup;
        switch (m_iBitsPerPixel) {
            case  4: 
                if (m_iPixelWidth % 4 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 4");
                m_lngImageWidth = m_iPixelWidth / 2 / 2;
                iBitsPerPixReverseLookup = 0; 
                break;
            case  8: 
                if (m_iPixelWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                m_lngImageWidth = m_iPixelWidth / 2;
                iBitsPerPixReverseLookup = 1; 
                break;
            default:
                throw new IllegalArgumentException("Not a valid bits/pixel for clut");
        }
        m_lngImageHeight = m_iPixelHeight;
        
        
        int[] aiTimImg;
        aiTimImg = new int[m_iPixelWidth * m_iPixelHeight];
        int[] aiPalette = null;

        
        // use a Hashtable to store the palette
        Hashtable<Integer, Integer> oColors
                = new Hashtable<Integer, Integer>();
        int iPaletteIdx = 0; // current palette index

        // look at every pixel in the image
        for (int i = 0; i < aiBuffImg.length; i++) {
            // convert the rgb color into a TIM color
            int iRGB = aiBuffImg[i];
            Integer oiTIMColor = new Integer( Color32toColor16(iRGB) );
            // add the color to the palette if it's not already
            int iPalColor;
            if (!oColors.containsKey(oiTIMColor)) {
                iPalColor = iPaletteIdx;
                oColors.put(oiTIMColor, new Integer(iPaletteIdx));
                iPaletteIdx++;

                // make sure we don't overflow the palette size
                if (m_iBitsPerPixel == 8 && oColors.size() > 256) 
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + oColors.size() + " colors into 256");
                else if (m_iBitsPerPixel == 4 && oColors.size() > 16) 
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + oColors.size() + " colors into 16");
            } else {
                iPalColor = oColors.get(oiTIMColor);
            }
            // now change the image data to use the palette index
            aiTimImg[i] = iPalColor;
        }

        // make the palette array
        if (iBitsPerPixel == 8)
            aiPalette = new int[256];
        else // if (iBitsPerPixel == 4)
            aiPalette = new int[16];

        for (Entry<Integer, Integer> oii : oColors.entrySet()) {
            aiPalette[oii.getValue()] = oii.getKey();
        }

        m_blnHasColorLookupTable = true;
        
        // set all the header data
        m_iTag = 0x10;
        m_iVersion = 0;
        m_lngUnknown1 = 0;
        if (m_blnHasColorLookupTable) {
            m_lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x800;
            m_oClut = new CLUT(aiPalette);
        } else {
            m_lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x000;
            m_oClut = null;
        }
        m_lngUnknown2 = 0;
        m_lngImageX = 0;
        m_lngImageY = 0;
        m_lngImageLength = m_lngImageWidth * m_lngImageHeight * 2 + 12;
        
        // finally write the image data with the proper bits per pixel
        ByteArrayOutputStream baos = 
                new ByteArrayOutputStream((int)(m_lngImageWidth * m_lngImageHeight * 2));
        
        int iClr;
        switch (m_iBitsPerPixel) {
            case 4:
                for (int i = 0; i < aiTimImg.length; i++) {
                    iClr = aiTimImg[i];
                    i++;
                    iClr |= aiTimImg[i] << 4;
                    baos.write(iClr);
                }
                break;
            case 8:
                for (int i = 0; i < aiTimImg.length; i++) {
                    baos.write(aiTimImg[i]);
                }
                break;
        }
        
        m_abImageData = baos.toByteArray();
        
    }
    
     /** Create a TIM file from an array of pixels of palette indexes.
      *  Each array element represents one pixel.
      *  Generates a 4 or 8 bit TIM file, depending on the size of aiPalette.
     */
   public Tim(int[] aiTimImg, int iWidth, int iHeight, int[] aiPalette) {
        if (iWidth * iHeight != aiTimImg.length)
            throw new IllegalArgumentException("Dimentions do not match data.");
        
        m_iPixelWidth   = iWidth;
        m_iPixelHeight  = iHeight;
        
        if (aiPalette.length <= 16) {
            m_iBitsPerPixel = 4;
        } else if (aiPalette.length <= 256) {
            m_iBitsPerPixel = 8;
        } else {
            throw new IllegalArgumentException("Palette length is too big.");
        }
        
        m_blnHasColorLookupTable = true;
        
        int iBitsPerPixReverseLookup;
        switch (m_iBitsPerPixel) {
            case  4: 
                if (m_iPixelWidth % 4 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 4");
                m_lngImageWidth = m_iPixelWidth / 2 / 2;
                iBitsPerPixReverseLookup = 0; 
                break;
            case  8: 
                if (m_iPixelWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                m_lngImageWidth = m_iPixelWidth / 2;
                iBitsPerPixReverseLookup = 1; 
                break;
            default:
                throw new RuntimeException("Not a valid bits/pixel");
        }
        m_lngImageHeight = m_iPixelHeight;
        
        
        // set all the header data
        m_iTag = 0x10;
        m_iVersion = 0;
        m_lngUnknown1 = 0;
        
        // has CLUT
        m_lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x800;
        // convert the 32bit palette into TIM 16bit
        for (int i = 0; i < aiPalette.length; i++) {
            aiPalette[i] = Color32toColor16(aiPalette[i]);
        }

        m_oClut = new CLUT(aiPalette);
        
        m_lngUnknown2 = 0;
        m_lngImageX = 0;
        m_lngImageY = 0;
        m_lngImageLength = m_lngImageWidth * m_lngImageHeight * 2 + 12;
        
        // finally write the image data with the proper bits per pixel
        ByteArrayOutputStream baos = 
                new ByteArrayOutputStream((int)(m_lngImageWidth * m_lngImageHeight * 2));
        
        int iClr;
        switch (m_iBitsPerPixel) {
            case 4:
                for (int i = 0; i < aiTimImg.length; i++) {
                    iClr = aiTimImg[i];
                    i++;
                    iClr |= aiTimImg[i] << 4;
                    baos.write(iClr);
                }
                break;
            case 8:
                for (int i = 0; i < aiTimImg.length; i++) {
                    baos.write(aiTimImg[i]);
                }
                break;
        }
        
        m_abImageData = baos.toByteArray();
        
   }
   
   
   /** Creates a TIM from a BufferedImage with the specified BPP. */
   public Tim(BufferedImage bi, int iBitsPerPixel) {
       
       Tim newTim;
        
       // create a new TIM file based on what we find about the BufferedImage
       switch (iBitsPerPixel) {
           
           case 4:
           case 8:
               if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED && 
                   bi.getColorModel() instanceof IndexColorModel) 
               {

                   IndexColorModel icm = (IndexColorModel)bi.getColorModel();

                   int[] aiPalette = new int [icm.getMapSize()];
                   icm.getRGBs(aiPalette);


                   int iWidth = bi.getWidth(), iHeight = bi.getHeight();

                   int[] aiTimImg = new int[iWidth * iHeight];
                   bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

                   newTim = new Tim(aiTimImg, iWidth, iHeight, aiPalette);
               } else {
                   // pull the image into an RGB array
                   int[] aiBuffImg = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
                   newTim = new Tim(aiBuffImg, bi.getWidth(), bi.getHeight(), iBitsPerPixel, true);
               }
               break;
           case 16:
           case 24:
               // pull the image into an RGB array
               int[] aiBuffImg = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());

               newTim = new Tim(aiBuffImg, bi.getWidth(), bi.getHeight(), iBitsPerPixel);
               break;
           default:
               throw new IllegalArgumentException("Not a valid bits/pixel");
       }
        
       // then just copy over the values
       this.m_iTag                     = newTim.m_iTag;                    
       this.m_iVersion                 = newTim.m_iVersion;                
       this.m_lngUnknown1              = newTim.m_lngUnknown1;             
       this.m_lngBpp_HasColorLookupTbl = newTim.m_lngBpp_HasColorLookupTbl;
       this.m_lngUnknown2              = newTim.m_lngUnknown2;             
       this.m_oClut                    = newTim.m_oClut;                   
       this.m_lngImageLength           = newTim.m_lngImageLength;          
       this.m_lngImageX                = newTim.m_lngImageX;               
       this.m_lngImageY                = newTim.m_lngImageY;               
       this.m_lngImageWidth            = newTim.m_lngImageWidth;           
       this.m_lngImageHeight           = newTim.m_lngImageHeight;          
       this.m_abImageData              = newTim.m_abImageData;             
       this.m_iBitsPerPixel            = newTim.m_iBitsPerPixel;           
       this.m_blnHasColorLookupTable   = newTim.m_blnHasColorLookupTable;  
       this.m_iPixelWidth              = newTim.m_iPixelWidth;             
       this.m_iPixelHeight             = newTim.m_iPixelHeight;  
   
   }
    
    /** Creates a TIM with the same or similar color-model of a BufferedImage. */
    public Tim(BufferedImage bi) {
        Tim newTim;
        
        if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED && 
                bi.getColorModel() instanceof IndexColorModel) 
        {
            
            IndexColorModel icm = (IndexColorModel)bi.getColorModel();

            int[] aiPalette = new int [icm.getMapSize()];
            icm.getRGBs(aiPalette);
            
            int iWidth = bi.getWidth(), iHeight = bi.getHeight();
            
            int[] aiTimImg = new int[iWidth * iHeight];
            bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

            newTim = new Tim(aiTimImg, iWidth, iHeight, aiPalette);
            
        } else {

            // pull the image into an RGB array
            int[] aiBuffImg = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
            
            // just use 16 BPP since it's the most common
            newTim = new Tim(aiBuffImg, bi.getWidth(), bi.getHeight(), 16);
            
        }
        
        // then just copy over the values
        this.m_iTag                     = newTim.m_iTag;                    
        this.m_iVersion                 = newTim.m_iVersion;                
        this.m_lngUnknown1              = newTim.m_lngUnknown1;             
        this.m_lngBpp_HasColorLookupTbl = newTim.m_lngBpp_HasColorLookupTbl;
        this.m_lngUnknown2              = newTim.m_lngUnknown2;             
        this.m_oClut                    = newTim.m_oClut;                   
        this.m_lngImageLength           = newTim.m_lngImageLength;          
        this.m_lngImageX                = newTim.m_lngImageX;               
        this.m_lngImageY                = newTim.m_lngImageY;               
        this.m_lngImageWidth            = newTim.m_lngImageWidth;           
        this.m_lngImageHeight           = newTim.m_lngImageHeight;          
        this.m_abImageData              = newTim.m_abImageData;             
        this.m_iBitsPerPixel            = newTim.m_iBitsPerPixel;           
        this.m_blnHasColorLookupTable   = newTim.m_blnHasColorLookupTable;  
        this.m_iPixelWidth              = newTim.m_iPixelWidth;             
        this.m_iPixelHeight             = newTim.m_iPixelHeight;        
    }
    
    
    //--------------------------------------------------------------------------
    //-- Public functions ------------------------------------------------------
    //--------------------------------------------------------------------------

    public int getBitsPerPixel() {
        return m_iBitsPerPixel;
    }
    
    /** Each TIM file can have multiple palettes. The TIM data doesn't even
     *  have to use these palettes for drawing, but they usually do. */
    public int getPaletteCount() {
        if (m_oClut == null)
            return 1;
        else
            return m_oClut.getPaletteSize() / (1 << m_iBitsPerPixel);
    }

    public int getWidth() {
        return m_iPixelWidth;
    }
    
    public int getHeight() {
        return m_iPixelHeight;
    }
    
    /** @param iPalette  Which palette to use for the decoded image.
     *  @see #getPaletteCount() */
    public BufferedImage toBufferedImage(int iPalette) {

        try {

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
                            oBAOS.write(Color16toColor4B(aiColorData[iPalette * 16 + i]));
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
                            oBAOS.write(Color16toColor4B(aiColorData[iPalette * 256 + i]));
                        }
                    }
                    colorModel = new IndexColorModel(8, 256, oBAOS.toByteArray(), 0, true);
                    break;

            }

            // Now write the image data    

            ByteArrayInputStream oDIS = new ByteArrayInputStream(m_abImageData);
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
                            iByte = IO.ReadUInt8(oDIS);
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
                            iByte = IO.ReadUInt8(oDIS);
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
                            int r = IO.ReadUInt8(oDIS);
                            int g = IO.ReadUInt8(oDIS);
                            int b = IO.ReadUInt8(oDIS);
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
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public int[] getColorData() {
        if (m_oClut != null)
            return m_oClut.getColorData().clone();
        else
            return null;
    }
    
    public byte[] getImageData() {
        return m_abImageData.clone();
    }

    /** Writes a tim file. */
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(m_iTag);
        dos.writeByte(m_iVersion);
        dos.writeShort((short)m_lngUnknown1);
        dos.writeShort((short)m_lngBpp_HasColorLookupTbl);
        dos.writeShort((short)m_lngUnknown2);
        
        if (m_blnHasColorLookupTable)
            m_oClut.write(dos);
        
        IO.WriteInt32LE(dos, m_lngImageLength);
        IO.WriteInt16LE(dos, m_lngImageX);
        IO.WriteInt16LE(dos, m_lngImageY);
        IO.WriteInt16LE(dos, m_lngImageWidth);
        IO.WriteInt16LE(dos, m_lngImageHeight);
        
        dos.write(m_abImageData);
    }
    
    public String toString() {
        return String.format(
            "Tag:%x Ver:%d Unkn1:%d Unkn2:%d BPP:%d HasCLUT:%s [%s] Len:%d X:%d Y:%d",
            m_iTag,
            m_iVersion,
            m_lngUnknown1,
            m_lngUnknown2,
            m_iBitsPerPixel,
            m_blnHasColorLookupTable ? "Yes" : "No",
            m_oClut != null ? m_oClut.toString() : "",
            m_lngImageLength,
            m_lngImageX,
            m_lngImageY,
            m_lngImageWidth,
            m_lngImageHeight);
    }
    
    //--------------------------------------------------------------------------
    //-- Private functions -----------------------------------------------------
    //--------------------------------------------------------------------------
    
    
    /** Works the same as
     *  <pre>
     *  int CONVERT_5_TO_8_BIT(int i) {
     *    return (int)Math.round((double)i / 31.0);
     *  }
     *  </pre> */
    private static int[] CONVERT_5_TO_8_BIT = new int[/*32*/] 
   {  0,   8,  16,  25,  33,  41,  49,  58,  
     66,  74,  82,  90,  99, 107, 115, 123, 
    132, 140, 148, 156, 165, 173, 181, 189, 
    197, 206, 214, 222, 230, 239, 247, 255 };
    
    private static int Color32toColor16(int i) {
        int a = (i & 0xFF000000) >>> 24;
        int r = (i & 0x00FF0000) >>> (16 + 3);
        int g = (i & 0x0000FF00) >>> ( 8 + 3);
        int b = (i & 0x000000FF) >>> (     3);
        int bgr = (b << 10) | (g << 5) | r;
        if (a == 0) {
            // if totally transparent
            bgr = 0;
            a   = 0;
        } else if (a == 255) {
            // if totally opaque
            if (bgr == 0)
                // if totally transparent & black
                a = 1;
            else
                // if totally transparent & not black
                a = 0;
        } else {
            // if partially transparent
            a = 1;
        }
        return (a << 15) | bgr;
    }
    
    private static int Color16toColor32(int i) {
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
    
    private static byte[] Color16toColor4B(int i) {
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
    
    private static int RGBA(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }
    
}
