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

package jpsxdec.plugins.psx.tim;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map.Entry;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** The Playstation 1 TIM file. Used in many Playstation games.
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
        private final long _lngClutX;
        private final long _lngClutY;
        private final long _lngClutWidth;
        private final long _lngClutHeight;
        private final int[] _aiColorData;
        
        /** Read a CLUT from an InputStream. */
        public CLUT(InputStream is) 
                throws IOException, NotThisTypeException 
        {
            _lngLength = IO.readUInt32LE(is);
            if (_lngLength <= 0) throw new NotThisTypeException();
            _lngClutX = IO.readUInt16LE(is);
            _lngClutY = IO.readUInt16LE(is);
            _lngClutWidth = IO.readUInt16LE(is);
            if (_lngClutWidth == 0) throw new NotThisTypeException();
            _lngClutHeight = IO.readUInt16LE(is);
            if (_lngClutHeight == 0) throw new NotThisTypeException();
            
            if (_lngLength != (_lngClutWidth * _lngClutHeight * 2 + 12))
                throw new NotThisTypeException();
            
            _aiColorData = new int[(int)(_lngClutWidth * _lngClutHeight)];
            for (int i = 0; i < _aiColorData.length; i++) {
                _aiColorData[i] = (int)IO.readUInt16LE(is);
            }
        }
        
        /** Create a CLUT based on a ready-made CLUT palette. */
        public CLUT(int[] aiPalette) {
            _aiColorData = aiPalette;
            _lngClutX = 0;
            _lngClutY = 0;
            _lngClutWidth = aiPalette.length;
            _lngClutHeight = aiPalette.length / _lngClutWidth;
            _lngLength = _lngClutWidth * _lngClutHeight * 2 + 12;
        }

        /** Write the CLUT to a stream. */
        public void write(OutputStream os) throws IOException {
            IO.writeInt32LE(os, _lngLength);
            IO.writeInt16LE(os, _lngClutX);
            IO.writeInt16LE(os, _lngClutY);
            IO.writeInt16LE(os, _lngClutWidth);
            IO.writeInt16LE(os, _lngClutHeight);
            for (int i : _aiColorData) {
                IO.writeInt16LE(os, i);
            }
        }
        
        private int[] getColorData() {
            return _aiColorData;
        }

        private int getPaletteSize() {
            return _aiColorData.length;
        }
        
        public String toString() {
            return String.format(
                    "Len:%d X:%d Y:%d Width:%d Height:%d",
                    _lngLength,
                    _lngClutX,
                    _lngClutY,
                    _lngClutWidth,
                    _lngClutHeight);
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
     * 
     *  Note that if an image has more than 16 or 256 colors for 4 and 8 BPP,
     *  respectively, then this function will fail.
     * @param aiBuffImg      ARGB data  
     * @param iBitsPerPixel  4, 8, 16, 24  */
    public static Tim create(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel) {
        return new Tim(aiBuffImg, iWidth, iHeight, iBitsPerPixel);
    }
    
    /** Creates a TIM from a BufferedImage with the specified BPP. */
    public static Tim create(BufferedImage bi, int iBitsPerPixel) {
        
        Tim newTim;

        // read a new TIM file based on what we find about the BufferedImage
        switch (iBitsPerPixel) {

            case 4:
            case 8:
                if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
                        bi.getColorModel() instanceof IndexColorModel) {

                    IndexColorModel icm = (IndexColorModel) bi.getColorModel();

                    int[] aiPalette = new int[icm.getMapSize()];
                    icm.getRGBs(aiPalette);


                    int iWidth = bi.getWidth(), iHeight = bi.getHeight();

                    int[] aiTimImg = new int[iWidth * iHeight];
                    bi.getRaster().getPixels(0, 0, iWidth, iHeight, aiTimImg);

                    newTim = new Tim(aiTimImg, iWidth, iHeight, aiPalette);
                } else {
                    // pull the image into an RGB array
                    int[] aiBuffImg = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
                    newTim = new Tim(aiBuffImg, bi.getWidth(), bi.getHeight(), iBitsPerPixel);
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
        
        return newTim;

    }
    
    /** Creates a TIM with the same or similar color-model of a BufferedImage. */
    public static Tim create(BufferedImage bi) {
        Tim newTim;

        if (bi.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
                bi.getColorModel() instanceof IndexColorModel) {

            IndexColorModel icm = (IndexColorModel) bi.getColorModel();

            int[] aiPalette = new int[icm.getMapSize()];
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

        return newTim;
    }

            
    //--------------------------------------------------------------------------
    //-- Fields ----------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    private static int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };
    
    public final static int HEADER_SIZE = 12;
    
    /** The 0x10 tag at the start of every TIM file. */
    private final int _iTag;
    /** Version of the TIM file. The only known version is 0. */
    private final int _iVersion;
    private final long _lngUnknown1;
    /** 16-bit value specifying the bits-per-pixel and if it has a CLUT. */
    private final long _lngBpp_HasColorLookupTbl;
    private final long _lngUnknown2;
    
    /** The color lookup table for the TIM.  */
    private final CLUT _clut;
    
    /** Size of the data to follow. */
    private final long _lngImageLength;
    /** X position of the TIM file (often 0). */
    private final long _lngImageX;
    /** Y position of the TIM file (often 0). */
    private final long _lngImageY;
    /** Width of the image data in 16-bit values. */
    private final long _lngImageByteWidth;
    /** Height of the image in pixels. */
    private final long _lngImageHeight;
    /** The raw image data of the TIM. */
    private final byte[] _abImageData;
    
    /** Extrated and converted from @code _lngBpp_HasColorLookupTbl. */
    private final int _iBitsPerPixel;
    /** Extrated and converted from @code _lngBpp_HasColorLookupTbl. */
    private final boolean _blnHasColorLookupTable;
    /** Height of the image in pixels.
     * Calculated based on@code _lngImageWidth and @code _iBitsPerPixel. */
    private final int _iPixelWidth;
    
    //--------------------------------------------------------------------------
    //-- Constructors ----------------------------------------------------------
    //--------------------------------------------------------------------------
    
    /** Parse and deserialize a TIM file from a stream. */
    private Tim(InputStream inStream) throws IOException, NotThisTypeException {
        
        _iTag = IO.readUInt8(inStream);
        //System.err.println(String.format("%02x", _iTag));
        if (_iTag != 0x10)
            throw new NotThisTypeException();
        
        _iVersion = IO.readUInt8(inStream);
        //System.err.println(String.format("%02x", _iVersion));
        if (_iVersion != 0)
            throw new NotThisTypeException();
        
        _lngUnknown1 = IO.readUInt16LE(inStream);
        //System.err.println(String.format("%04x", _lngUnknown1));
        if (_lngUnknown1 != 0)
            throw new NotThisTypeException();
        
        _lngBpp_HasColorLookupTbl = IO.readUInt8(inStream);
        //System.err.println(String.format("%04x", _lngBpp_HasColorLookupTbl));
        if ((_lngBpp_HasColorLookupTbl & 0xF4FF) != 0)
            throw new NotThisTypeException();
        
        _lngUnknown2 = IO.readUInt16LE(inStream);
        //System.err.println(String.format("%04x", _lngUnknown2));
        if (_lngUnknown2 != 0)
            throw new NotThisTypeException();
        
        //-------------------------------------------------
        
        _iBitsPerPixel =
                BITS_PER_PIX[(int)((_lngBpp_HasColorLookupTbl >>> 8) & 3)];
        _blnHasColorLookupTable = (_lngBpp_HasColorLookupTbl & 0x800) != 0;
        
        if (_blnHasColorLookupTable)
            _clut = new CLUT(inStream);
        else
            _clut = null;
        
        _lngImageLength = IO.readUInt32LE(inStream);
        if (_lngImageLength <= 0) throw new NotThisTypeException();
        _lngImageX = IO.readUInt16LE(inStream);
        _lngImageY = IO.readUInt16LE(inStream);
        _lngImageByteWidth = IO.readUInt16LE(inStream);
        if (_lngImageByteWidth == 0) throw new NotThisTypeException();
        _lngImageHeight = IO.readUInt16LE(inStream);
        if (_lngImageHeight == 0) throw new NotThisTypeException();
        
        if (_lngImageLength != _lngImageByteWidth * _lngImageHeight * 2 + 12)
            throw new NotThisTypeException();
        
        _abImageData = IO.readByteArray(inStream,
                (int)(_lngImageByteWidth * _lngImageHeight) * 2);
        
        switch (_iBitsPerPixel) {
            case 4:  _iPixelWidth = (int)(_lngImageByteWidth * 2 * 2); break;
            case 8:  _iPixelWidth = (int)(_lngImageByteWidth * 2    ); break;
            case 16: _iPixelWidth = (int)(_lngImageByteWidth        ); break;
            case 24: _iPixelWidth = (int)(_lngImageByteWidth * 2 / 3); break;
            default: throw new RuntimeException("Impossible Tim BPP " + _iBitsPerPixel);
        }
    }


    /** Create a TIM file with the specified width/height, 
     *  bits-per-pixel, and array of RGB data. Generates a 16 or 24 bit
     *  TIM file (without a CLUT).
     * @param aiBuffImg  ARGB data
     */
    private Tim(int[] aiBuffImg, int iWidth, int iHeight, int iBitsPerPixel) 
    {
        if (iWidth * iHeight != aiBuffImg.length)
            throw new IllegalArgumentException("Dimensions do not match data.");
        
        _iPixelWidth   = iWidth;
        _lngImageHeight = iHeight;
        _iBitsPerPixel = iBitsPerPixel;
        
        int iBitsPerPixReverseLookup;
        switch (iBitsPerPixel) {
            case  4: 
                if (iWidth % 4 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 4");
                _lngImageByteWidth = iWidth / 2 / 2;
                _blnHasColorLookupTable = true;
                iBitsPerPixReverseLookup = 0; 
                break;
            case  8: 
                if (iWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                _lngImageByteWidth = iWidth / 2;
                _blnHasColorLookupTable = true;
                iBitsPerPixReverseLookup = 1; 
                break;
            case 16: 
                _lngImageByteWidth = iWidth;
                _blnHasColorLookupTable = false;
                iBitsPerPixReverseLookup = 2; 
                break;
            case 24: 
                if (iWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                _lngImageByteWidth = iWidth * 3 / 2;
                _blnHasColorLookupTable = false;
                iBitsPerPixReverseLookup = 3; 
                break;
            default:
                throw new IllegalArgumentException("Not a valid bits/pixel");
        }
        
        
        ExposedBAOS baos = new ExposedBAOS(
                (int)(_lngImageByteWidth * _lngImageHeight * 2));
        
        if (_blnHasColorLookupTable) {
            int[] aiPalette = convertToPalettedTim(aiBuffImg, _iBitsPerPixel, baos);
            _lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x800;
            _clut = new CLUT(aiPalette);
        } else {
            // convert the image data to TIM colors and write it using proper BPP
            convertToTim(aiBuffImg, iBitsPerPixel, baos);
            _lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x000;
            _clut = null;
        }
        
        _iTag = 0x10;
        _iVersion = 0;
        _lngUnknown1 = 0;
        _lngUnknown2 = 0;
        _lngImageX = 0;
        _lngImageY = 0;
        _lngImageLength = _lngImageByteWidth * _lngImageHeight * 2 + 12;
        
        _abImageData = baos.getBuffer();
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
        Hashtable<Integer, Integer> oColors = new Hashtable<Integer, Integer>();
 
        int iPaletteIdx = 0; // current palette index

        int[] aiTimImg = new int[aiBuffImg.length];
        
        // look at every pixel in the image
        for (int i = 0; i < aiBuffImg.length; i++) {
            // convert the rgb color into a TIM color
            int iRGB = aiBuffImg[i];
            Integer oiTIMColor = new Integer( color32toColor16(iRGB) );
            // add the color to the palette if it's not already
            int iPalColor;
            if (!oColors.containsKey(oiTIMColor)) {
                iPalColor = iPaletteIdx;
                oColors.put(oiTIMColor, new Integer(iPaletteIdx));
                iPaletteIdx++;

                // make sure we don't overflow the palette size
                if (iBitsPerPixel == 8 && oColors.size() > 256) 
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + oColors.size() + " colors into 256");
                else if (iBitsPerPixel == 4 && oColors.size() > 16) 
                    throw new IllegalArgumentException(
                    "Unable to squeeze " + oColors.size() + " colors into 16");
            } else {
                iPalColor = oColors.get(oiTIMColor);
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

        for (Entry<Integer, Integer> oii : oColors.entrySet()) {
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
    private Tim(int[] aiTimImg, int iWidth, int iHeight, int[] aiPalette) {
        if (iWidth * iHeight != aiTimImg.length)
            throw new IllegalArgumentException("Dimensions do not match data.");
        
        _iPixelWidth   = iWidth;
        
        if (aiPalette.length <= 16) {
            _iBitsPerPixel = 4;
        } else if (aiPalette.length <= 256) {
            _iBitsPerPixel = 8;
        } else {
            throw new IllegalArgumentException("Palette length is too big.");
        }
        
        _blnHasColorLookupTable = true;
        
        int iBitsPerPixReverseLookup;
        switch (_iBitsPerPixel) {
            case  4: 
                if (_iPixelWidth % 4 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 4");
                _lngImageByteWidth = _iPixelWidth / 2 / 2;
                iBitsPerPixReverseLookup = 0; 
                break;
            case  8: 
                if (_iPixelWidth % 2 != 0)
                    throw new IllegalArgumentException("Currently unable to handle width not divisible by 2");
                _lngImageByteWidth = _iPixelWidth / 2;
                iBitsPerPixReverseLookup = 1; 
                break;
            default:
                throw new RuntimeException("Not a valid bits/pixel");
        }
        _lngImageHeight = iHeight;
        
        
        // set all the header data
        _iTag = 0x10;
        _iVersion = 0;
        _lngUnknown1 = 0;
        
        // has CLUT
        _lngBpp_HasColorLookupTbl = (iBitsPerPixReverseLookup << 8) | 0x800;
        // convert the 32bit palette into TIM 16bit
        for (int i = 0; i < aiPalette.length; i++) {
            aiPalette[i] = color32toColor16(aiPalette[i]);
        }

        _clut = new CLUT(aiPalette);
        
        _lngUnknown2 = 0;
        _lngImageX = 0;
        _lngImageY = 0;
        _lngImageLength = _lngImageByteWidth * _lngImageHeight * 2 + 12;
        
        // finally write the image data with the proper bits per pixel
        ExposedBAOS baos = 
                new ExposedBAOS((int)(_lngImageByteWidth * _lngImageHeight * 2));
        
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
        if (_clut == null)
            return 1;
        else
            return _clut.getPaletteSize() / (1 << _iBitsPerPixel);
    }

    /** Width of TIM in pixels. */
    public int getWidth() {
        return _iPixelWidth;
    }
    
    /** Height of TIM in pixels. */
    public int getHeight() {
        return (int)_lngImageHeight;
    }
    
    /** @param iPalette  Which palette to use for the decoded image.
     *  @see #getPaletteCount() */
    public BufferedImage toBufferedImage(int iPalette) {

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
                        int[] aiColorData = _clut.getColorData();
                        for (int i = 0; i < 16; i++) {
                            baos.write(color16toColor4B(aiColorData[iPalette * 16 + i]));
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
                        int[] aiColorData = _clut.getColorData();
                        for (int i = 0; i < 256; i++) {
                            baos.write(color16toColor4B(aiColorData[iPalette * 256 + i]));
                        }
                    }
                    colorModel = new IndexColorModel(8, 256, baos.getBuffer(), 0, true);
                    break;

            }

            // Now write the image data    

            ByteArrayInputStream bais = new ByteArrayInputStream(_abImageData);
            BufferedImage bi;
            WritableRaster raster;

            //bi = new BufferedImage(m_iPixelWidth, m_iPixelHeight, BufferedImage.TYPE_INT_ARGB);
            int iByte, iColor16, iColor32;
            switch (_iBitsPerPixel) {
                case 4:
                    bi = new BufferedImage(_iPixelWidth, (int)_lngImageHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                    raster = bi.getRaster();
                    for (int y = 0; y < _lngImageHeight; y++) {
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
                    bi = new BufferedImage(_iPixelWidth, (int)_lngImageHeight, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                    raster = bi.getRaster();
                    for (int y = 0; y < _lngImageHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            iByte = IO.readUInt8(bais);
                            raster.setSample(x, y, 0, iByte);
                        }
                    }
                    break;
                case 16:
                    bi = new BufferedImage(_iPixelWidth, (int)_lngImageHeight, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < _lngImageHeight; y++) {
                        for (int x = 0; x < _iPixelWidth; x++) {
                            iColor16 = (int)IO.readUInt16LE(bais);
                            iColor32 = color16toColor32(iColor16);
                            bi.setRGB(x, y, iColor32);
                        }
                    }
                    break;
                case 24:
                    bi = new BufferedImage(_iPixelWidth, (int)_lngImageHeight, BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < _lngImageHeight; y++) {
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
    
    public int[] getColorData() {
        if (_clut != null)
            return _clut.getColorData().clone();
        else
            return null;
    }
    
    public byte[] getImageData() {
        return _abImageData.clone();
    }

    /** Writes a tim file. */
    public void write(OutputStream os) throws IOException {
        os.write(_iTag);
        os.write(_iVersion);
        IO.writeInt16LE(os, (short)_lngUnknown1);
        IO.writeInt16LE(os, (short)_lngBpp_HasColorLookupTbl);
        IO.writeInt16LE(os, (short)_lngUnknown2);
        
        if (_blnHasColorLookupTable)
            _clut.write(os);
        
        IO.writeInt32LE(os, _lngImageLength);
        IO.writeInt16LE(os, _lngImageX);
        IO.writeInt16LE(os, _lngImageY);
        IO.writeInt16LE(os, _lngImageByteWidth);
        IO.writeInt16LE(os, _lngImageHeight);
        
        os.write(_abImageData);
    }
    
    public String toString() {
        return String.format(
            "Tag:%x Ver:%d Unkn1:%d Unkn2:%d BPP:%d HasCLUT:%s [%s] Len:%d X:%d Y:%d %dx%d",
            _iTag,
            _iVersion,
            _lngUnknown1,
            _lngUnknown2,
            _iBitsPerPixel,
            _blnHasColorLookupTable ? "Yes" : "No",
            _clut != null ? _clut.toString() : "",
            _lngImageLength,
            _lngImageX,
            _lngImageY,
            _lngImageByteWidth,
            _lngImageHeight);
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
