/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** The PlayStation 1 TIM image. Used in many PlayStation games.
 * This is based on the excellent Q-gears documentation here:
 * http://wiki.qhimm.com/PSX/TIM_file
 * <p>
 * When converting to BufferedImages, the semi-transparent bit is represented
 * as alpha value of 254.
 * <p>
 * When creating a TIM image from a BufferedImage, alpha values from 1 to 254
 * are converted to having semi-transparent bit turned on.
 * @see #SEMI_TRANSPARENT  */
public class Tim {

    /** The Tim semi-transparent alpha bit will be converted to this 8-bit alpha value. */
    public static final int SEMI_TRANSPARENT = 254;

    /** Quickly reads a stream to determine if the data is a Tim image.
     * @return info about the Tim image, otherwise null. */
    public static TimInfo isTim(InputStream inStream) throws IOException {
        return CreateTim.isTim(inStream);
    }

    /** Parse and deserialize a TIM file from a stream. */
    public static Tim read(InputStream inStream) throws IOException, NotThisTypeException
    {
        return CreateTim.read(inStream);
    }

    /** Creates a TIM with the same or similar color-model of a BufferedImage. */
    public static Tim create(BufferedImage bi) {
        return CreateTim.create(bi, 0, 0, 0, 0);
    }

    /** Creates a TIM with the same or similar color-model of a {@link BufferedImage}.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.  */
    public static Tim create(BufferedImage bi, int iTimX, int iTimY, int iClutX, int iClutY) {
        return CreateTim.create(bi, iTimX, iTimY, iClutX, iClutY);
    }

    /** Creates a TIM from a BufferedImage with the specified bits-per-pixel.
     * @throws IllegalArgumentException if anything is weird.
     * @param iBitsPerPixel 4, 8, 16, or 24. */
    public static Tim create(BufferedImage bi, int iBitsPerPixel) {
        return CreateTim.create(bi, iBitsPerPixel, 0, 0, 0, 0);
    }

    /** Creates a TIM from a BufferedImage with the specified bits-per-pixel.
     * @throws IllegalArgumentException if anything is weird.
     * @param iBitsPerPixel 4, 8, 16, or 24.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate. Ignored if bpp is 16 or 24.
     * @param iClutY CLUT Y coordinate. Ignored if bpp is 16 or 24. */
    public static Tim create(BufferedImage bi, int iBitsPerPixel,
                             int iTimX, int iTimY,
                             int iClutX, int iClutY)
    {
        return CreateTim.create(bi, iBitsPerPixel, iTimX, iTimY, iClutX, iClutY);
    }

    /** Create a TIM image with a custom CLUT.
     * This is the most advanced method of creating a TIM image.
     * It allows one to create a TIM with multiple CLUT palettes.
     * @param iBitsPerPixel Either 4 or 8. */
    public static Tim create(BufferedImage bi, BufferedImage clutImg, int iBitsPerPixel)
    {
        return CreateTim.create(bi, 0, 0, clutImg, 0, 0, iBitsPerPixel);
    }

    /** Create a TIM image with a custom CLUT.
     * This is the most advanced method of creating a TIM image.
     * It allows one to create a TIM with multiple CLUT palettes.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.
     * @param iBitsPerPixel Either 4 or 8. */
    public static Tim create(BufferedImage bi, int iTimX, int iTimY,
                             BufferedImage clutImg, int iClutX, int iClutY,
                             int iBitsPerPixel)
    {
        return CreateTim.create(bi, iTimX, iTimY, clutImg, iClutX, iClutY, iBitsPerPixel);
    }

    //--------------------------------------------------------------------------
    //-- Fields ----------------------------------------------------------------
    //--------------------------------------------------------------------------

    /** Convert the 2-bit value found in the Tim header to its bits-per-pixel. 
     * <pre>
     * 00b = 4
     * 01b = 8
     * 10b = 16
     * 11b = 24
     * </pre>
     */
    static final int[] BITS_PER_PIX = new int[/*4*/] { 4, 8, 16, 24 };

    /** Size of the Tim header in bytes. */
    static final int HEADER_SIZE = 12;

    /** Magic 8-bit value at the start of Tim. */
    static final int TAG_MAGIC = 0x10;
    /** All Tims are version 0. */
    static final int VERSION_0 = 0;
    
    /** The color lookup table for the TIM. null if none. */
    private final CLUT _clut;
    
    /** X position of the Tim in pixels.
     * Not sure how it is used in the PSX, but it is often 0. */
    private final int _iTimX;
    /** Y position of the Tim in pixels.
     * Not sure how it is used in the PSX, but it is often 0. */
    private final int _iTimY;
    /** Width of the image in pixels. */
    private final int _iPixelWidth;
    /** Height of the image in pixels. */
    private final int _iPixelHeight;
    /** 4, 8, 16, or 24. */
    private final int _iBitsPerPixel;

    /** The raw image data of the TIM. Data differs depending on bits-per-pixel:
     * <ul>
     * <li>4bpp : 16 color paletted image, two 4-bit palette indexes per byte,
     *            in the order of 1/0, 3/2, etc
     * <li>8bpp : 256 color paletted image, one 8-bit palette index per byte.
     * <li>16bpp: ABGR1555 shorts in little endian.
     * <li>24bpp: RGB888 in big endian.
     * </ul>
     * @see #_iBitsPerPixel
     */
    private final byte[] _abImageData;
    
    //--------------------------------------------------------------------------
    //-- Constructors ----------------------------------------------------------
    //--------------------------------------------------------------------------
    
    
    /** Create a TIM image with the given data.
      * @param abTimImageData Raw Tim image data (stored directly).
      * @param iTimX X position of the Tim image.
      * @param iTimY Y position of the Tim image.
      * @param iBitsPerPixel 4, 8, 16, or 24.
      * @param clut Can be null if no color look-up table.
      */
    Tim(byte[] abTimImageData, int iTimX, int iTimY,
        int iPixelWidth, int iPixelHeight, int iBitsPerPixel,
        CLUT clut)
    {
        if (iPixelWidth < 1 || iPixelHeight < 1)
            throw new IllegalArgumentException("Invalid dimensions " + iPixelWidth + "x" + iPixelHeight);
        if (iTimX < 0 || iTimY < 0)
            throw new IllegalArgumentException("Invalid Tim X,Y (" + iTimX + ", " + iTimY + ")");
        _iPixelWidth = iPixelWidth;
        _iPixelHeight = iPixelHeight;
        _iTimX = iTimX;
        _iTimY = iTimY;
        _abImageData = abTimImageData;
        _iBitsPerPixel = iBitsPerPixel;
        _clut = clut;
        switch (iBitsPerPixel) {
            case 4: case 8: case 16: case 24: break;
            default: throw new IllegalArgumentException("Invalid bits-per-pixel " + iBitsPerPixel);
        }
        int iExpectedDataSize = calculateImageWordWidth() * _iPixelHeight * 2;
        if (iExpectedDataSize != abTimImageData.length)
            throw new IllegalArgumentException(
                    "Data size " + abTimImageData.length +
                    " != expected size " + iExpectedDataSize);
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
            return _clut.getPaletteLength() / iColorsForBitsPerPixel;
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

    /** Note: The Java API to save a {@link BufferedImage} to the disk
     * may change the palette order and indexes in the saved image.
     * @param iPalette  Which palette to use for the decoded image.
     * @see #getPaletteCount() */
    public BufferedImage toBufferedImage(int iPalette) {

        if (iPalette < 0 || iPalette >= getPaletteCount())
            throw new IllegalArgumentException("Palette index "+iPalette+" out of bounds");

        switch (_iBitsPerPixel) {
            case 4: return toBi4(iPalette);
            case 8: return toBi8(iPalette);
            case 16: return toBi16();
            case 24: return toBi24();
            default:
                throw new IllegalStateException("Impossible Tim BPP " + _iBitsPerPixel);
        }
    }

    /** Converts the CLUT  to a {@link BufferedImage}.
     * @return null if image has no CLUT. */
    public BufferedImage getClutImage() {
        if (_clut != null)
            return _clut.toBufferedImage();
        else
            return null;
    }

    /** Tries to replace this TIM's image data and palette data (if it has a CLUT)
     * with the image data of the buffered image.
     * @throws IllegalArgumentException if the BufferedImage data is incompatible.
     */
    public void replaceImageData(BufferedImage bi) {
        Tim newTim = create(bi, _iBitsPerPixel);
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
        if (_clut != null)
            System.arraycopy(newTim._clut._asiColorData, 0, _clut._asiColorData, 0, _clut._asiColorData.length);
    }

    /** Tries to replace this TIM's image data and palette data
     * with the image data of the buffered image and CLUT.
     * @throws IllegalArgumentException if the BufferedImage data is incompatible
     *                                  or there is no CLUT.
     */
    public void replaceImageData(BufferedImage bi, BufferedImage clut) {
        if (_clut == null)
            throw new IllegalArgumentException("Can't change the CLUT when Tim doesn't have a CLUT");
        Tim newTim = CreateTim.create(bi, _iTimX, _iTimY, clut, _clut.getX(), _clut.getY(), _iBitsPerPixel);
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
        System.arraycopy(newTim._clut._asiColorData, 0, _clut._asiColorData, 0, _clut._asiColorData.length);
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
        IO.writeInt16LE(os, _iTimX);
        IO.writeInt16LE(os, _iTimY);
        IO.writeInt16LE(os, calculateImageWordWidth());
        IO.writeInt16LE(os, _iPixelHeight);
        
        os.write(_abImageData);
    }

    /** Sorta the opposite of {@link #BITS_PER_PIX}. */
    private int calculateBpp_HasCLUT() {
        int iBitsPerPixReverseLookup;
        switch (_iBitsPerPixel) {
            case 4: iBitsPerPixReverseLookup = 0; break;
            case 8: iBitsPerPixReverseLookup = 1; break;
            case 16:iBitsPerPixReverseLookup = 2; break;
            case 24:iBitsPerPixReverseLookup = 3; break;
            default: throw new IllegalStateException("Unpossible!");
        }

        if (_clut != null)
            return iBitsPerPixReverseLookup | 0x08;
        else
            return iBitsPerPixReverseLookup;
    }

    /** Size of the Tim structure in bytes. */
    private long calculateImageLength() {
        return calculateImageWordWidth() * _iPixelHeight * 2 + HEADER_SIZE;
    }

    /** Width of the image data in 16-bit values. */
    private int calculateImageWordWidth() {
        switch (_iBitsPerPixel) {
            case 4: return _iPixelWidth / 2 / 2;
            case 8: return _iPixelWidth / 2;
            case 16:return _iPixelWidth;
            case 24:return _iPixelWidth * 3 / 2;
            default: throw new IllegalStateException("Invalid bits-per-pixel " + _iBitsPerPixel);
        }
    }
    
    public String toString() {
        String s = String.format(
            "%dx%d %dbpp xy(%d, %d) WWidth:%d Len:%d",
            _iPixelWidth,
            _iPixelHeight,
            _iBitsPerPixel,
            _iTimX,
            _iTimY,
            calculateImageWordWidth(),
            calculateImageLength());
        if (_clut == null)
            return s;
        else 
            return s + " CLUT[" + _clut + "]";
    }
    
    //--------------------------------------------------------------------------
    //-- Private functions -----------------------------------------------------
    //--------------------------------------------------------------------------


    /** Works the same as
     * <pre>
     * byte CONVERT_4_TO_8_BIT(int i) {
     *   return (byte)Math.round(i*15/255.0);
     * }
     * </pre> */
    private static final byte[] CONVERT_4_TO_8_BIT =
    {
        (byte)  0, (byte) 17, (byte) 34, (byte) 51,
        (byte) 68, (byte) 85, (byte)102, (byte)119,
        (byte)136, (byte)153, (byte)170, (byte)187,
        (byte)204, (byte)221, (byte)238, (byte)255,
    };
    static { assert CONVERT_4_TO_8_BIT.length == 16; }

    /** Fills buffer with RGBA8888 grayscale values.
     * Assumes buffer is 16*4 bytes long. */
    private static void build16GrayRgbaPalette(byte[] abPalette) {
        for (int i = 0; i < 16; i++) {
            byte bClr = CONVERT_4_TO_8_BIT[i];
            abPalette[i*4+0] = bClr; // r
            abPalette[i*4+1] = bClr; // g
            abPalette[i*4+2] = bClr; // b
            abPalette[i*4+3] = (byte)255; // a
        }
    }

    /** Fills buffer with RGBA8888 grayscale values.
     * Assumes buffer is 256*4 bytes long. */
    private static void build256GrayRgbaPalette(byte[] abPalette) {
        for (int i = 0; i < 256; i++) {
            byte bClr = (byte)i;
            abPalette[i*4+0] = bClr; // r
            abPalette[i*4+1] = bClr; // g
            abPalette[i*4+2] = bClr; // b
            abPalette[i*4+3] = (byte)255; // a
        }
    }

    /** Convert this 4 bpp Tim to a BufferedImage. */
    private BufferedImage toBi4(int iPalette) {

        byte[] abRgbaPalette = new byte[16 * 4];
        if (_clut == null) {
            build16GrayRgbaPalette(abRgbaPalette);
        } else {
            // convert CLUT to array of RGBA bytes
            for (int i = iPalette * 16, o = 0; o < abRgbaPalette.length; i++, o+=4) {
                int iArgb = color16toColor32(_clut.getColor(i));
                abRgbaPalette[o+0] = (byte)(iArgb >> 16);
                abRgbaPalette[o+1] = (byte)(iArgb >>  8);
                abRgbaPalette[o+2] = (byte)(iArgb      );
                abRgbaPalette[o+3] = (byte)(iArgb >> 24);
            }
        }

        IndexColorModel cm = new IndexColorModel(4, 16, abRgbaPalette, 0, true);

        WritableRaster raster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE,
                                                          _iPixelWidth, _iPixelHeight,
                                                          1, 4, null);
        byte[] abBufferPackedIndexes = ((DataBufferByte)raster.getDataBuffer()).getData();
        for (int i = 0; i < abBufferPackedIndexes.length; i++) {
            int b = _abImageData[i];
            // swap the nibbles
            abBufferPackedIndexes[i] = (byte)(((b >> 4) & 0x0f) | ((b << 4) & 0xf0));
        }
        return new BufferedImage(cm, raster, false, null);
    }

    /** Convert this 8 bpp Tim to a BufferedImage. */
    private BufferedImage toBi8(int iPalette) {
        byte[] abRgbaPalette = new byte[256 * 4];
        if (_clut == null) {
            build256GrayRgbaPalette(abRgbaPalette);
        } else {
            // convert CLUT to array of RGBA bytes
            for (int i = iPalette * 256, o = 0; o < abRgbaPalette.length; i++, o+=4) {
                int iArgb = color16toColor32(_clut.getColor(i));
                abRgbaPalette[o+0] = (byte)(iArgb >> 16);
                abRgbaPalette[o+1] = (byte)(iArgb >>  8);
                abRgbaPalette[o+2] = (byte)(iArgb      );
                abRgbaPalette[o+3] = (byte)(iArgb >> 24);
            }
        }

        IndexColorModel cm = new IndexColorModel(8, 256, abRgbaPalette, 0, true);
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                         _iPixelWidth, _iPixelHeight,
                                                         1, _iPixelWidth,
                                                         new int[] {0});

        WritableRaster raster = Raster.createWritableRaster(sm, null);
        byte[] abBufferIndexes = ((DataBufferByte)raster.getDataBuffer()).getData();
        System.arraycopy(_abImageData, 0, abBufferIndexes, 0, abBufferIndexes.length);
        return new BufferedImage(cm, raster, false, null);
    }

    /** Convert this 24 bpp Tim to a BufferedImage. */
    private BufferedImage toBi24() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] aiBits = {8, 8, 8};
        int[] aiChannelIdxes = {0, 1, 2};
        ColorModel cm = new ComponentColorModel(cs, aiBits, false, false,
                                                Transparency.OPAQUE,
                                                DataBuffer.TYPE_BYTE);
        int iScanlineStride = _iPixelWidth * 3;
        // TODO: Need to check this logic
        if (iScanlineStride % 2 != 0)
            iScanlineStride++;
        
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                               _iPixelWidth, _iPixelHeight,
                                                               iScanlineStride, 3,
                                                               aiChannelIdxes, null);
        byte[] abBufferRgb = ((DataBufferByte)raster.getDataBuffer()).getData();
        System.arraycopy(_abImageData, 0, abBufferRgb, 0, abBufferRgb.length);
        return new BufferedImage(cm, raster, false, null);
    }

    /** Convert this 16 bpp Tim to a BufferedImage. */
    private BufferedImage toBi16() {
        BufferedImage bi = new BufferedImage(_iPixelWidth, _iPixelHeight, BufferedImage.TYPE_INT_ARGB);
        int[] aiBufferRgba = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
        // convert 16-bit ABGR1555 image data to 32-bit ARGB8888
        for (int i = 0, o = 0; o < aiBufferRgba.length; i+=2, o++) {
            int iColor16 = IO.readUInt16LE(_abImageData, i);
            aiBufferRgba[o] = color16toColor32(iColor16);
        }
        return bi;
    }
    
    /** Works the same as
     * <pre>
     * int CONVERT_5_TO_8_BIT(int i) {
     *   return (int)Math.round((double)i / 31.0);
     * }
     * </pre> */
    private static final int[] CONVERT_5_TO_8_BIT = new int[/*32*/]
    {  0,   8,  16,  25,  33,  41,  49,  58,
      66,  74,  82,  90,  99, 107, 115, 123,
     132, 140, 148, 156, 165, 173, 181, 189,
     197, 206, 214, 222, 230, 239, 247, 255 };
    static { assert CONVERT_5_TO_8_BIT.length == 32; }

    /** Tim ABGR1555 to ARGB8888. */
    static int color16toColor32(int i16) {
        int b = CONVERT_5_TO_8_BIT[(i16 >>> 10) & 0x1F];
        int g = CONVERT_5_TO_8_BIT[(i16 >>>  5) & 0x1F];
        int r = CONVERT_5_TO_8_BIT[(i16       ) & 0x1F];
        int a;

        if (r == 0 && g == 0 && b == 0) {
            if ((i16 & 0x8000) == 0)
                // black, and the alpha bit is NOT set
                a = (byte)0; // totally transparent
            else
                // black, and the alpha bit IS set
                a = (byte)255; // totally opaque
        } else {
            if ((i16 & 0x8000) == 0)
                // some color, and the alpha bit is NOT set
                a = (byte)255; // totally opaque
            else
                // some color, and the alpha bit IS set
                a = (byte)SEMI_TRANSPARENT; // some variance of transparency
        }

        return a << 24 | r << 16 | g << 8 | b;
    }
    
}
