/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

package jpsxdec.tim;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** The PlayStation 1 TIM file. Used in many PlayStation games.
 * This is based on the excellent Q-gears documentation here:
 * http://wiki.qhimm.com/PSX/TIM_file
 * <p>
 * When creating a TIM file from a BufferedImage, any alpha value from 1 to 254
 * is converted to having semi-transparent bit turned on. When writing
 * BufferedImages, the semi-transparent bit is written as alpha value of 128.
 */
public class Tim {
    
    /** The TIM Color Lookup Table (CLUT). */
    public static class CLUT { 
        
        public final static int HEADER_SIZE = 12;
        
        private final long _lngLength;
        private final int _iClutX;
        private final int _iClutY;
        private final int _iClutWidth;
        private final int _iClutHeight;
        /** Array of unsigned 16-bit values. */
        private final int[] _aiColorData;
        
        /** Read a CLUT from an InputStream. */
        public CLUT(InputStream is) 
                throws IOException, NotThisTypeException 
        {
            _lngLength = IO.readUInt32LE(is);
            if (_lngLength <= 0) throw new NotThisTypeException();
            _iClutX = IO.readUInt16LE(is);
            _iClutY = IO.readUInt16LE(is);
            _iClutWidth = IO.readUInt16LE(is);
            if (_iClutWidth == 0) throw new NotThisTypeException();
            _iClutHeight = IO.readUInt16LE(is);
            if (_iClutHeight == 0) throw new NotThisTypeException();
            
            if (_lngLength != (_iClutWidth * _iClutHeight * 2 + 12))
                throw new NotThisTypeException();
            
            _aiColorData = new int[(int)(_iClutWidth * _iClutHeight)];
            for (int i = 0; i < _aiColorData.length; i++) {
                _aiColorData[i] = (int)IO.readUInt16LE(is);
            }
        }
        
        /** Create a CLUT based on a ready-made CLUT palette. */
        public CLUT(int[] aiPalette, int iX, int iY, int iWidth) {
            _aiColorData = aiPalette;
            _iClutX = iX;
            _iClutY = iY;
            _iClutWidth = iWidth;
            _iClutHeight = aiPalette.length / _iClutWidth;
            _lngLength = _iClutWidth * _iClutHeight * 2 + 12;
        }

        /** Write the CLUT to a stream. */
        public void write(OutputStream os) throws IOException {
            IO.writeInt32LE(os, _lngLength);
            IO.writeInt16LE(os, _iClutX);
            IO.writeInt16LE(os, _iClutY);
            IO.writeInt16LE(os, _iClutWidth);
            IO.writeInt16LE(os, _iClutHeight);
            for (int i : _aiColorData) {
                IO.writeInt16LE(os, i);
            }
        }

        public BufferedImage toBufferedImage() {
            int[] ai = new int[_aiColorData.length];
            for (int i = 0; i < ai.length; i++) {
                ai[i] = color16toColor32(_aiColorData[i]);
            }

            BufferedImage bi = new BufferedImage(_iClutWidth, _iClutHeight, BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, _iClutWidth, _iClutHeight, ai, 0, _iClutWidth);

            return bi;
        }
        
        /** Returns unsigned 16-bit color value. */
        public int getColor(int iIndex) {
            return _aiColorData[iIndex];
        }

        private int getPaletteSize() {
            return _aiColorData.length;
        }
        
        public String toString() {
            return String.format(
                    "Len:%d X:%d Y:%d Width:%d Height:%d",
                    _lngLength,
                    _iClutX,
                    _iClutY,
                    _iClutWidth,
                    _iClutHeight);
        }

        public int getWidth() {
            return _iClutWidth;
        }
    }
    
    //--------------------------------------------------------------------------
    //-- Creators --------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    /** Parse and deserialize a TIM file from a stream. */
    public static Tim read(InputStream inStream)
            throws IOException, NotThisTypeException
    {
        return new Tim(inStream);
    }
    
    /** Create a TIM file with the specified width/height, 
     *  bits-per-pixel, and array of RGB data. Generates a 16 or 24 bit
     *  TIM file (without a CLUT), or a 4 or 8 bit TIM file (with a CLUT). 
     *  The CLUT palette is automatically generated from the image data.
     *<p>
     *  Note that if an image has more than 16 or 256 colors for 4 and 8 BPP,
     *  respectively, then this function will fail.
     * @param aiBuffImg      ARGB data  
     * @param iBitsPerPixel  4, 8, 16, 24  */
    public static Tim create(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel) {
        return new Tim(aiBuffImg, iWidth, iHeight, iBitsPerPixel);
    }
    
    /** Creates a TIM from a BufferedImage with the specified bits-per-pixel. 
     * @throws IllegalArgumentException if unable to squeeze all the colors into 
     * the palette.     */
    public static Tim create(BufferedImage bi, int iBitsPerPixel) {
        
        Tim newTim;

        final int iSrcBpp;
        
        if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
            bi.getColorModel() instanceof IndexColorModel)
        {
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();
            int iPaletteSize = icm.getMapSize();
            if (iPaletteSize <= 16)
                iSrcBpp = 4;
            else if (iPaletteSize <= 256)
                iSrcBpp = 8;
            else
                iSrcBpp = 24; 
        } else {
            iSrcBpp = 24; // it might actually be 16bpp, but we'd treat it the same
        }
        
        final int PALETTE_COPY = 0;
        final int TRUE_COLOR_PARSE = 1;
        final int iConversionType;
        
        switch (iSrcBpp) {
            case 4:
                if (iBitsPerPixel <= 8)
                    iConversionType = PALETTE_COPY;
                else
                    iConversionType = TRUE_COLOR_PARSE;
                break;
            case 8:
                if (iBitsPerPixel == 8)
                    iConversionType = PALETTE_COPY;
                else 
                    iConversionType = TRUE_COLOR_PARSE;
                break;
            default: // 24
                    iConversionType = TRUE_COLOR_PARSE;
                break;
        }
        
        final int iWidth = bi.getWidth(), iHeight = bi.getHeight();
        
        if (iConversionType == PALETTE_COPY) {
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();

            int[] aiPalette = new int[icm.getMapSize()];
            icm.getRGBs(aiPalette);

            int[] aiTimImg = new int[iWidth * iHeight];
            bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

            if (iBitsPerPixel == 4) {
                newTim = new Tim(aiTimImg, 0, 0, iWidth, iHeight, aiPalette, 0, 0, 16, 4);
            } else { // (iBitsPerPixel == 8)
                newTim = new Tim(aiTimImg, 0, 0, iWidth, iHeight, aiPalette, 0, 0, 256, 8);
            }
        } else {
            // pull the image into an RGB array
            int[] aiBuffImg = bi.getRGB(0, 0, iWidth, iHeight, null, 0, bi.getWidth());
            newTim = new Tim(aiBuffImg, iWidth, iHeight, iBitsPerPixel);
        }
        
        return newTim;

    }
    
    /** Creates a TIM with the same or similar color-model of a BufferedImage. */
    public static Tim create(BufferedImage bi) {
        Tim newTim;

        if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
            bi.getColorModel() instanceof IndexColorModel)
        {
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();

            int[] aiPalette = new int[icm.getMapSize()];
            icm.getRGBs(aiPalette);

            int iWidth = bi.getWidth(), iHeight = bi.getHeight();

            int[] aiTimImg = new int[iWidth * iHeight];
            bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

            if (aiPalette.length <= 16) {
                newTim = new Tim(aiTimImg, 0, 0, iWidth, iHeight, aiPalette, 0, 0, 16, 4);
            } else if (aiPalette.length <= 256) {
                newTim = new Tim(aiTimImg, 0, 0, iWidth, iHeight, aiPalette, 0, 0, 256, 8);
            } else {
                throw new IllegalArgumentException("Palette length is too big.");
            }

        } else {

            // pull the image into an RGB array
            int[] aiBuffImg = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());

            // just use 16 BPP since it's the most common
            newTim = new Tim(aiBuffImg, bi.getWidth(), bi.getHeight(), 16);

        }

        return newTim;
    }

    public static Tim create(BufferedImage bi, BufferedImage clut, int iBitsPerPixel)
    {
        return create(bi, 0, 0, clut, 0, 0, iBitsPerPixel);
    }
    
    /** Create a TIM image with a custom CLUT.
     * This is the most advanced method of creating a TIM image.
     * It allows one to create a TIM with multiple CLUT palettes.  */
    public static Tim create(BufferedImage bi, int iX, int iY, 
                             BufferedImage clut, int iClutX, int iClutY,
                             int iBitsPerPixel)
    {
        if (iBitsPerPixel != 8 && iBitsPerPixel != 4)
            throw new IllegalArgumentException();

        if (clut.getWidth() % 16 != 0)
            throw new IllegalArgumentException();

        Tim newTim;

        if ((bi.getType() == BufferedImage.TYPE_BYTE_INDEXED ||
            bi.getType() == BufferedImage.TYPE_BYTE_BINARY) &&
            bi.getColorModel() instanceof IndexColorModel)
        {
            int iWidth = bi.getWidth(), iHeight = bi.getHeight();

            int[] aiTimImg = new int[iWidth * iHeight];
            bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

            int[] aiClut = clut.getRGB(0, 0, clut.getWidth(), clut.getHeight(), null, 0, clut.getWidth());

            newTim = new Tim(aiTimImg, iX, iY, iWidth, iHeight,
                             aiClut, iClutX, iClutY, clut.getWidth(), iBitsPerPixel);
            
        } else {
            throw new UnsupportedOperationException();
        }

        return newTim;
    }
            
    //--------------------------------------------------------------------------
    //-- Fields ----------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    public static int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };
    
    public final static int HEADER_SIZE = 12;
    
    public static final int TAG_MAGIC = 0x10;
    public static final int VERSION_0 = 0;
    
    /** The color lookup table for the TIM.  */
    private final CLUT _clut;
    
    /** X position of the TIM file (often 0). */
    private final int _iImageX;
    /** Y position of the TIM file (often 0). */
    private final int _iImageY;
    /** Height of the image in pixels. */
    private final int _iPixelHeight;
    /** The raw image data of the TIM. */
    private final byte[] _abImageData;
    
    /** 4, 8, 16, or 24. */
    private final int _iBitsPerPixel;
    /** Height of the image in pixels.
     * Calculated based on ImageWordWidth and {@link #_iBitsPerPixel}. */
    private final int _iPixelWidth;
    
    //--------------------------------------------------------------------------
    //-- Constructors ----------------------------------------------------------
    //--------------------------------------------------------------------------
    
    /** Parse and deserialize a TIM file from a stream. */
    private Tim(InputStream inStream) throws IOException, NotThisTypeException {
        
        int iTag = IO.readUInt8(inStream);
        //System.err.println(String.format("%02x", _iTag));
        if (iTag != TAG_MAGIC) // 0x10
            throw new NotThisTypeException();
        
        int iVersion = IO.readUInt8(inStream);
        //System.err.println(String.format("%02x", _iVersion));
        if (iVersion != VERSION_0)
            throw new NotThisTypeException();
        
        int iUnknown1 = IO.readUInt16LE(inStream);
        //System.err.println(String.format("%04x", _lngUnknown1));
        if (iUnknown1 != 0)
            throw new NotThisTypeException();
        
        int iBpp_blnHasColorLookupTbl = IO.readUInt16LE(inStream);
        //System.err.println(String.format("%04x", _lngBpp_HasColorLookupTbl));
        if ((iBpp_blnHasColorLookupTbl & 0xFFF4) != 0)
            throw new NotThisTypeException();
        
        int iUnknown2 = IO.readUInt16LE(inStream);
        //System.err.println(String.format("%04x", _lngUnknown2));
        if (iUnknown2 != 0)
            throw new NotThisTypeException();
        
        //-------------------------------------------------
        
        boolean blnHasColorLookupTable = (iBpp_blnHasColorLookupTbl & 0x8) != 0;
        
        if (blnHasColorLookupTable) {
            _clut = new CLUT(inStream);
            int iBpp = BITS_PER_PIX[iBpp_blnHasColorLookupTbl & 3];
            // User CUE reported an issue with some strange TIM images from
            // "Guardian's Crusade" that report 16 bits-per-pixel, but also 
            // have a CLUT (bug "JPSXDEC-4").
            // Turns out the image data was actually 4 or 8 bits-per-pixels.
            // The CLUT width seemed to correlate with the bits-per-pixel
            // (width < 256 : 4 bits/pixel, width >= 256 : 8 bits/pixel)
            // No clear way to handle this case, so we'll change the code
            // to at least handle these unique images the best we can.
            switch (iBpp) {
                case 16:
                    if (_clut.getWidth() < 256)
                        _iBitsPerPixel = 4;
                    else
                        _iBitsPerPixel = 8;
                    Logger.getLogger(Tim.class.getName()).log(Level.WARNING, "TIM reports 16 bits/pixel, but it also has a CLUT. Assuming " + _iBitsPerPixel + " bits/pixel");
                    break;
                case 24:
                    Logger.getLogger(Tim.class.getName()).log(Level.WARNING, "TIM reports 24 bits/pixel, but it also has a CLUT. Assuming 8 bits/pixel");
                    _iBitsPerPixel = 8;
                    break;
                default: // 4 or 8
                    _iBitsPerPixel = iBpp;
                    break;
            }
        } else {
            _clut = null;
            _iBitsPerPixel = BITS_PER_PIX[iBpp_blnHasColorLookupTbl & 3];
        } 
        
        
        
        long lngImageLength = IO.readUInt32LE(inStream);
        if (lngImageLength == 0) throw new NotThisTypeException();
        _iImageX = IO.readUInt16LE(inStream);
        _iImageY = IO.readUInt16LE(inStream);
        int iImageWordWidth = IO.readUInt16LE(inStream);
        if (iImageWordWidth == 0) throw new NotThisTypeException();
        _iPixelHeight = IO.readUInt16LE(inStream);
        if (_iPixelHeight == 0) throw new NotThisTypeException();
        
        if (lngImageLength != iImageWordWidth * _iPixelHeight * 2 + HEADER_SIZE)
            throw new NotThisTypeException();
        
        _abImageData = IO.readByteArray(inStream,
                (iImageWordWidth * _iPixelHeight) * 2);
        
        switch (_iBitsPerPixel) {
            case 4:  _iPixelWidth = (int)(iImageWordWidth * 2 * 2); break;
            case 8:  _iPixelWidth = (int)(iImageWordWidth * 2    ); break;
            case 16: _iPixelWidth = (int)(iImageWordWidth        ); break;
            case 24: _iPixelWidth = (int)(iImageWordWidth * 2 / 3); break;
            default: throw new RuntimeException("Impossible Tim BPP " + _iBitsPerPixel);
        }
    }


    /** Create a TIM file with the specified width/height, 
     *  bits-per-pixel, and array of RGB data. Generates a 16 or 24 bit
     *  TIM file (without a CLUT).
     * @param aiBuffImg  ARGB data
     */
    private Tim(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel) {
        if (iWidth * iHeight != aiBuffImg.length)
            throw new IllegalArgumentException("Dimensions do not match data.");
        
        _iPixelWidth   = iWidth;
        _iPixelHeight  = iHeight;
        _iBitsPerPixel = iBitsPerPixel;
        
        int iImageWordWidth;
        boolean _blnHasColorLookupTable;
        switch (iBitsPerPixel) {
            case  4: 
                if (iWidth % 4 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 4");
                iImageWordWidth = _iPixelWidth / 2 / 2;
                _blnHasColorLookupTable = true;
                break;
            case  8: 
                if (iWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                iImageWordWidth = _iPixelWidth / 2;
                _blnHasColorLookupTable = true;
                break;
            case 16: 
                iImageWordWidth = _iPixelWidth;
                _blnHasColorLookupTable = false;
                break;
            case 24: 
                if (iWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                iImageWordWidth = _iPixelWidth * 3 / 2;
                _blnHasColorLookupTable = false;
                break;
            default:
                throw new IllegalArgumentException("Not a valid bits/pixel");
        }
        
        
        ExposedBAOS baos = new ExposedBAOS(iImageWordWidth * _iPixelHeight * 2);
        
        if (_blnHasColorLookupTable) {
            int[] aiPalette = convertToPalettedTim(aiBuffImg, _iBitsPerPixel, baos);
            _clut = new CLUT(aiPalette, 0, 0, aiPalette.length);
        } else {
            // convert the image data to TIM colors and write it using proper BPP
            convertToTim(aiBuffImg, iBitsPerPixel, baos);
            _clut = null;
        }
        
        _iImageX = 0;
        _iImageY = 0;
        
        _abImageData = baos.getBuffer();
    }
    
    private long calculateImageLength() {
        return calculateImageWordWidth() * _iPixelHeight * 2 + HEADER_SIZE;
    }
    
    private int calculateBpp_HasCLUT() {
        int iBitsPerPixReverseLookup;
        switch (_iBitsPerPixel) {
            case 4: iBitsPerPixReverseLookup = 0; break;
            case 8: iBitsPerPixReverseLookup = 1; break;
            case 16:iBitsPerPixReverseLookup = 2; break;
            case 24:iBitsPerPixReverseLookup = 3; break;
            default: throw new RuntimeException("Unpossible!");
        }
        
        if (_clut != null)
            return iBitsPerPixReverseLookup | 0x08;
        else
            return iBitsPerPixReverseLookup | 0x00;
    }
    
    private int calculateImageWordWidth() {
        switch (_iBitsPerPixel) {
            case 4: return _iPixelWidth / 2 / 2;
            case 8: return _iPixelWidth / 2;    
            case 16:return _iPixelWidth;        
            case 24:return _iPixelWidth *3 / 2; 
            default: throw new RuntimeException("Unpossible!");
        }
    }
    
    private static void convertToTim(
            int[] aiBuffImg, int iBitsPerPixel, ByteArrayOutputStream baos) 
    {
        if (iBitsPerPixel == 24) {
            for (int i = 0; i < aiBuffImg.length; i++) {
                int iRGB = aiBuffImg[i];
                int iTIM = iRGB & 0xFFFFFF; // transparency is lost

                baos.write((iTIM >>> 16) & 0xFF);
                baos.write((iTIM >>>  8) & 0xFF);
                baos.write( iTIM         & 0xFF);
            }
        } else if (iBitsPerPixel == 16) {
            for (int i = 0; i < aiBuffImg.length; i++) {
                int iRGB = aiBuffImg[i];
                int iTIM = color32toColor16(iRGB);
                
                baos.write( iTIM        & 0xFF);
                baos.write((iTIM >>> 8) & 0xFF);
            }
        }        
    }
        
    private static int[] convertToPalettedTim(
            int[] aiBuffImg, int iBitsPerPixel, ByteArrayOutputStream baos) 
    {
        // use a Hashtable to store the palette
        HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
 
        int iPaletteIdx = 0; // current palette index

        int[] aiTimImg = new int[aiBuffImg.length];
        
        // look at every pixel in the image
        for (int i = 0; i < aiBuffImg.length; i++) {
            // convert the rgb color into a TIM color
            int iRGB = aiBuffImg[i];
            Integer oiTIMColor = Integer.valueOf( color32toColor16(iRGB) );
            // add the color to the palette if it's not already
            int iPalColor;
            if (!colors.containsKey(oiTIMColor)) {
                iPalColor = iPaletteIdx;
                colors.put(oiTIMColor, Integer.valueOf(iPaletteIdx));
                iPaletteIdx++;

                // make sure we don't overflow the palette size
                if (iBitsPerPixel == 8 && colors.size() > 256)
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + colors.size() + " colors into 256");
                else if (iBitsPerPixel == 4 && colors.size() > 16)
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + colors.size() + " colors into 16");
            } else {
                iPalColor = colors.get(oiTIMColor);
            }
            // write the palette index to the Tim image data
            aiTimImg[i] = iPalColor;
        }
        
        int[] aiPalette;
        // make the palette array
        if (iBitsPerPixel == 8)
            aiPalette = new int[256];
        else // if (iBitsPerPixel == 4)
            aiPalette = new int[16];

        for (Entry<Integer, Integer> oii : colors.entrySet()) {
            aiPalette[oii.getValue()] = oii.getKey();
        }
        
        int iClr;
        switch (iBitsPerPixel) {
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
        
        return aiPalette;
    }
    
    /** Create a TIM file from an array of pixels of palette indexes.
      *  Each array element represents one pixel.
      *  Generates a 4 or 8 bit TIM file, depending on the size of aiPalette.
      */
    private Tim(int[] aiTimImg, int iImageX, int iImageY, int iWidth, int iHeight,
                int[] aiPalette, int iClutX, int iClutY, int iPaletteWidth, int iBitsPerPixel)
    {
        if (iWidth * iHeight != aiTimImg.length)
            throw new IllegalArgumentException("Dimensions do not match data.");
        
        _iPixelWidth = iWidth;
        
        _iBitsPerPixel = iBitsPerPixel;

        _iPixelHeight = iHeight;
        
        // has CLUT
        // convert the 32bit palette into TIM 16bit
        for (int i = 0; i < aiPalette.length; i++) {
            aiPalette[i] = color32toColor16(aiPalette[i]);
        }

        _clut = new CLUT(aiPalette, iClutX, iClutY, iPaletteWidth);
        
        _iImageX = iImageX;
        _iImageY = iImageY;
        
        // finally create the image data with the proper bits per pixel
        ExposedBAOS baos = new ExposedBAOS(calculateImageWordWidth() * _iPixelHeight * 2);
        
        int iClr;
        switch (_iBitsPerPixel) {
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
        
        _abImageData = baos.getBuffer();

    }
   
    
    //--------------------------------------------------------------------------
    //-- Public functions ------------------------------------------------------
    //--------------------------------------------------------------------------

    /** Bits-per-pixel: 4, 8, 16, or 24. */
    public int getBitsPerPixel() {
        return _iBitsPerPixel;
    }
    
    /** If the TIM is paletted and has a CLUT, returns the number of CLUT
     * palettes. Otherwise if the TIM is paletted and has not CLUT, or if the
     * TIM is true-color, returns 1.
     * <p>
     * Each TIM file can have multiple palettes. The TIM data doesn't even
     * have to use these palettes for drawing, but they usually do. */
    public int getPaletteCount() {
        if ((_iBitsPerPixel == 4 || _iBitsPerPixel == 8) && _clut != null) {
            int iColorsForBitsPerPixel = (1 << _iBitsPerPixel);
            return _clut.getPaletteSize() / iColorsForBitsPerPixel;
        } else {
            return 1;
        }
    }

    /** Width of TIM in pixels. */
    public int getWidth() {
        return _iPixelWidth;
    }
    
    /** Height of TIM in pixels. */
    public int getHeight() {
        return _iPixelHeight;
    }

    /** Unfortunately the palette order and indexes may be changed when
     *  saving the BufferedImage to a file.
     * @param iPalette  Which palette to use for the decoded image.
     *  @see #getPaletteCount() */
    public BufferedImage toBufferedImage(int iPalette) {

        if (iPalette >= getPaletteCount())
            throw new IllegalArgumentException("Palette index out of bounds");

        try {

            // setup the palette
            IndexColorModel colorModel = null;
            ExposedBAOS baos;
            switch (_iBitsPerPixel) {
                case 4:
                    // convert CLUT to array of RGBA bytes
                    baos = new ExposedBAOS(16 * 4);
                    if (_clut == null) {
                        for (int i = 0; i < 256; i+=16) {
                            baos.write(i); // r
                            baos.write(i); // g
                            baos.write(i); // b
                            if (i == 0)
                                baos.write(255);
                            else
                                baos.write(0);
                        }
                    } else {
                        for (int i = 0; i < 16; i++) {
                            baos.write(color16toColor4B(_clut.getColor(iPalette * 16 + i)));
                        }
                    }
                    colorModel = new IndexColorModel(4, 16, baos.getBuffer(), 0, true);
                    break;
                case 8:
                    // convert CLUT to array of RGBA bytes
                    baos = new ExposedBAOS(256 * 4);
                    if (_clut == null) {
                        for (int i = 0; i < 256; i++) {
                            baos.write(i); // r
                            baos.write(i); // g
                            baos.write(i); // b
                            if (i == 0)
                                baos.write(255);
                            else
                                baos.write(0);
                        }
                    } else {
                        for (int i = 0; i < 256; i++) {
                            baos.write(color16toColor4B(_clut.getColor(iPalette * 256 + i)));
                        }
                    }
                    colorModel = new IndexColorModel(8, 256, baos.getBuffer(), 0, true);
                    break;

            }

            // Now write the image data    

            ByteArrayInputStream bais = new ByteArrayInputStream(_abImageData);
            BufferedImage bi;
            WritableRaster raster;

            int iByte, iColor16, iColor32;
            switch (_iBitsPerPixel) {
                case 4:
                    bi = new BufferedImage(_iPixelWidth, _iPixelHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                    raster = bi.getRaster();
                    for (int y = 0; y < _iPixelHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            iByte = IO.readUInt8(bais);
                            int iNibble = iByte & 0xF;
                            raster.setSample(x, y, 0, iNibble);

                            x++;
                            if (x < _iPixelWidth) { // in case of odd width
                                iNibble = (iByte >>> 4) & 0xF;
                                raster.setSample(x, y, 0, iNibble);
                            }
                        }
                    }
                    break;
                case 8:
                    bi = new BufferedImage(_iPixelWidth, _iPixelHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                    raster = bi.getRaster();
                    for (int y = 0; y < _iPixelHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            iByte = IO.readUInt8(bais);
                            raster.setSample(x, y, 0, iByte);
                        }
                    }
                    break;
                case 16:
                    bi = new BufferedImage(_iPixelWidth, _iPixelHeight, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < _iPixelHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            iColor16 = (int)IO.readUInt16LE(bais);
                            iColor32 = color16toColor32(iColor16);
                            bi.setRGB(x, y, iColor32);
                        }
                    }
                    break;
                case 24:
                    bi = new BufferedImage(_iPixelWidth, _iPixelHeight, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < _iPixelHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            int r = IO.readUInt8(bais);
                            int g = IO.readUInt8(bais);
                            int b = IO.readUInt8(bais);
                            iColor32 = rgba(r, g, b, 255);
                            bi.setRGB(x, y, iColor32);
                        }
                        // TODO: Need to check this logic
                        if ((_iPixelWidth % 2) == 1) // in case of odd width
                            IO.skip(bais, 1);
                    }
                    break;
                default:
                    throw new RuntimeException("Impossible Tim BPP " + _iBitsPerPixel);
            }

            return bi;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public BufferedImage getClutImage() {
        if (_clut != null)
            return _clut.toBufferedImage();
        else
            return null;
    }
    
    public int[] getColorData() {
        if (_clut != null)
            return _clut._aiColorData.clone();
        else
            return null;
    }
    
    public byte[] getImageData() {
        return _abImageData.clone();
    }
    
    /** Tries to replace this TIM's image data and palette data (if it has a CLUT)
     * with the image data of the buffered image.
     * @throws IllegalArgumentException if the BufferedImage data is incompatible.
     */
    public void replaceImageData(BufferedImage bi) {
        Tim newTim = create(bi, _iBitsPerPixel);
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
        if (_clut != null)
            System.arraycopy(newTim._clut._aiColorData, 0, _clut._aiColorData, 0, _clut._aiColorData.length);
    }

    /** Tries to replace this TIM's image data and palette data
     * with the image data of the buffered image and CLUT.
     * @throws IllegalArgumentException if the BufferedImage data is incompatible
     *                                  or there is no CLUT.
     */
    public void replaceImageData(BufferedImage bi, BufferedImage clut) {
        if (_clut == null)
            throw new IllegalArgumentException("Can't change the CLUT when Tim doesn't have a CLUT");
        Tim newTim = create(bi, _iImageX, _iImageY, clut, _clut._iClutX, _clut._iClutY, _iBitsPerPixel);
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
        System.arraycopy(newTim._clut._aiColorData, 0, _clut._aiColorData, 0, _clut._aiColorData.length);
    }

    /** Writes TIM image to the stream. */
    public void write(OutputStream os) throws IOException {
        os.write(TAG_MAGIC);
        os.write(VERSION_0);
        IO.writeInt16LE(os, 0); // Unknown 1
        IO.writeInt16LE(os, calculateBpp_HasCLUT());
        IO.writeInt16LE(os, 0); // Unknown 2
        
        if (_clut != null)
            _clut.write(os);
        
        IO.writeInt32LE(os, calculateImageLength());
        IO.writeInt16LE(os, _iImageX);
        IO.writeInt16LE(os, _iImageY);
        IO.writeInt16LE(os, calculateImageWordWidth());
        IO.writeInt16LE(os, _iPixelHeight);
        
        os.write(_abImageData);
    }
    
    public String toString() {
        return String.format(
            "%dx%d BPP:%d%sX:%d Y:%d WWidth:%d Len:%d",
            _iPixelWidth,
            _iPixelHeight,
            _iBitsPerPixel,
            _clut != null ? " ["+_clut.toString()+"] " : " ",
            _iImageX,
            _iImageY,
            calculateImageWordWidth(),
            calculateImageLength());
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
    
    private static int color32toColor16(int i) {
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
    
    private static int color16toColor32(int i) {
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
        
        return rgba(r, g, b, a);
    }
    
    private static byte[] color16toColor4B(int i) {
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
    
    private static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }
    
}
