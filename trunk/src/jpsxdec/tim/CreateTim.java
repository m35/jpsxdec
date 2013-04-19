/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013  Michael Sabin
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
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** Functions to generate a {@link Tim} image. */
class CreateTim {

    private static final Logger LOG = Logger.getLogger(CreateTim.class.getName());
    
    //--------------------------------------------------------------------------
    //-- Creators --------------------------------------------------------------
    //--------------------------------------------------------------------------

    /** Quickly reads a stream to determine if the data is a Tim image.
     * @return info about the Tim image, otherwise null. */
    public static TimInfo isTim(InputStream inStream) throws IOException {
        // tag
        if (IO.readUInt8(inStream) != Tim.TAG_MAGIC)
            return null;
        // version
        if (IO.readUInt8(inStream) != Tim.VERSION_0)
            return null;
        // unkn 1
        if (IO.readUInt16LE(inStream) != 0)
            return null;

        int iBpp_blnHasColorLookupTbl = IO.readUInt16LE(inStream);
        if ((iBpp_blnHasColorLookupTbl & 0xFFF4) != 0)
            return null;
        // unkn 2
        if (IO.readUInt16LE(inStream) != 0)
            return null;

        //-------------------------------------------------

        int iBitsPerPixel = Tim.BITS_PER_PIX[iBpp_blnHasColorLookupTbl & 3];

        final int iPaletteCount;
        // has CLUT
        if ((iBpp_blnHasColorLookupTbl & 8) != 0) {

            long lngLength = IO.readUInt32LE(inStream);
            if (lngLength <= 0)
                return null;

            // clut x,y
            IO.skip(inStream, 4);

            int iClutWidth = IO.readUInt16LE(inStream);
            if (iClutWidth == 0)
                return null;

            int iClutHeight = IO.readUInt16LE(inStream);
            if (iClutHeight == 0)
                return null;

            if (lngLength != (iClutWidth * iClutHeight * 2 + 12))
                return null;

            // User CUE reported an issue with some strange TIM images from
            // "Guardian's Crusade" that report 16 bits-per-pixel, but also
            // have a CLUT (bug "JPSXDEC-4").
            // See read() for more details.
            if (iBitsPerPixel == 16) {
                if (iClutWidth < 256)
                    iBitsPerPixel = 4;
                else
                    iBitsPerPixel = 8;
                LOG.log(Level.WARNING, "TIM reports 16 bits/pixel, but it also has a CLUT. Assuming " + iBitsPerPixel + " bits/pixel");
            } else if (iBitsPerPixel == 24) {
                LOG.log(Level.WARNING, "TIM reports 24 bits/pixel, but it also has a CLUT. Assuming 8 bits/pixel");
                iBitsPerPixel = 8;
            }

            iPaletteCount = (iClutWidth * iClutHeight) / (1 << iBitsPerPixel);

            IO.skip(inStream, iClutWidth * iClutHeight * 2);
        } else {
            iPaletteCount = 1;
        }

        long lngImageLength = IO.readUInt32LE(inStream);
        if (lngImageLength == 0)
            return null;

        // image x,y
        IO.skip(inStream, 4);

        int iImageWordWidth = IO.readUInt16LE(inStream);
        if (iImageWordWidth == 0)
            return null;

        int iImageHeight = IO.readUInt16LE(inStream);
        if (iImageHeight == 0)
            return null;

        if (lngImageLength != iImageWordWidth * iImageHeight * 2 + 12)
            return null;

        IO.skip(inStream, (iImageWordWidth * iImageHeight) * 2);

        int iPixelWidth;
        switch (iBitsPerPixel) {
            case 4:  iPixelWidth = iImageWordWidth * 2 * 2; break;
            case 8:  iPixelWidth = iImageWordWidth * 2    ; break;
            case 16: iPixelWidth = iImageWordWidth        ; break;
            case 24: iPixelWidth = iImageWordWidth * 2 / 3; break;
            default: throw new RuntimeException("Impossible Tim BPP " + iBitsPerPixel);
        }

        return new TimInfo(iPaletteCount, iBitsPerPixel, iPixelWidth, iImageHeight);
    }


    /** Parse and deserialize a TIM file from a stream. */
    public static Tim read(InputStream inStream) throws IOException, NotThisTypeException
    {
        final boolean DBG = false;
        int iTag = IO.readUInt8(inStream);
        if (DBG) System.err.println(String.format("%02x", iTag));
        if (iTag != Tim.TAG_MAGIC) // 0x10
            throw new NotThisTypeException();

        int iVersion = IO.readUInt8(inStream);
        if (DBG) System.err.println(String.format("%02x", iVersion));
        if (iVersion != Tim.VERSION_0)
            throw new NotThisTypeException();

        int iUnknown1 = IO.readUInt16LE(inStream);
        if (DBG) System.err.println(String.format("%04x", iUnknown1));
        if (iUnknown1 != 0)
            throw new NotThisTypeException();

        int iBpp_blnHasColorLookupTbl = IO.readUInt16LE(inStream);
        if (DBG) System.err.println(String.format("%04x", iBpp_blnHasColorLookupTbl));
        if ((iBpp_blnHasColorLookupTbl & 0xFFF4) != 0)
            throw new NotThisTypeException();

        int iUnknown2 = IO.readUInt16LE(inStream);
        if (DBG) System.err.println(String.format("%04x", iUnknown2));
        if (iUnknown2 != 0)
            throw new NotThisTypeException();

        //-------------------------------------------------

        boolean blnHasColorLookupTable = (iBpp_blnHasColorLookupTbl & 0x8) != 0;
        int iBitsPerPixel = Tim.BITS_PER_PIX[iBpp_blnHasColorLookupTbl & 3];

        CLUT clut = null;
        if (blnHasColorLookupTable) {
            clut = new CLUT(inStream);
            // User CUE reported an issue with some strange TIM images from
            // "Guardian's Crusade" that report 16 bits-per-pixel, but also
            // have a CLUT (bug "JPSXDEC-4").
            // Turns out the image data was actually 4 or 8 bits-per-pixels.
            // The CLUT width seemed to correlate with the bits-per-pixel
            // (width < 256 : 4 bits/pixel, width >= 256 : 8 bits/pixel)
            // No clear way to handle this case, so we'll change the code
            // to at least handle these unique images the best we can.
            if (iBitsPerPixel == 16) {
                if (clut.getWidth() < 256)
                    iBitsPerPixel = 4;
                else
                    iBitsPerPixel = 8;
                LOG.log(Level.WARNING, "TIM reports 16 bits/pixel, but it also has a CLUT. Assuming {0} bits/pixel", iBitsPerPixel);
            } else if (iBitsPerPixel == 24) {
                LOG.log(Level.WARNING, "TIM reports 24 bits/pixel, but it also has a CLUT. Assuming 8 bits/pixel");
                iBitsPerPixel = 8;
            }
        }

        long lngImageLength = IO.readUInt32LE(inStream);
        if (lngImageLength == 0) throw new NotThisTypeException();
        int iTimX = IO.readUInt16LE(inStream);
        int iTimY = IO.readUInt16LE(inStream);
        int iImageWordWidth = IO.readUInt16LE(inStream);
        if (iImageWordWidth == 0) throw new NotThisTypeException();
        int iPixelHeight = IO.readUInt16LE(inStream);
        if (iPixelHeight == 0) throw new NotThisTypeException();

        if (lngImageLength != iImageWordWidth * iPixelHeight * 2 + Tim.HEADER_SIZE)
            throw new NotThisTypeException();

        byte[] abImageData = IO.readByteArray(inStream, (iImageWordWidth * iPixelHeight) * 2);

        int iPixelWidth;
        switch (iBitsPerPixel) {
            case 4:  iPixelWidth = iImageWordWidth * 2 * 2; break;
            case 8:  iPixelWidth = iImageWordWidth * 2    ; break;
            case 16: iPixelWidth = iImageWordWidth        ; break;
            case 24: iPixelWidth = iImageWordWidth * 2 / 3; break;
            default: throw new RuntimeException("Impossible Tim BPP " + iBitsPerPixel);
        }
        return new Tim(abImageData, iTimX, iTimY, iPixelWidth, iPixelHeight, iBitsPerPixel, clut);
    }
    
    /** Creates a TIM with the same or similar color-model of a {@link BufferedImage}.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.
     */
    public static Tim create(BufferedImage bi, int iTimX, int iTimY, int iClutX, int iClutY) {
        int iSrcBpp = findBitsPerPixel(bi);
        if (iSrcBpp == 32)
            iSrcBpp = 16; // use 16 BPP (instead of 24) since it's the most common
        else if (iSrcBpp == 4 && bi.getWidth() % 4 != 0) {
            LOG.warning("Trying to create 4bpp Tim with width not divisible by 4. Converting to 8bpp.");
            iSrcBpp = 8; // 4bpp width must be divisible by 4, so try the next higher bpp
        }

        if (iSrcBpp == 8 && bi.getWidth() % 2 != 0) {
            LOG.warning("Trying to create 8bpp Tim with uneven width. Converting to 16bpp.");
            iSrcBpp = 16; // 16bpp is the only one that might allow for an even width (untested)
        }
        return create(bi, iSrcBpp, iTimX, iTimY, iClutX, iClutY);
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
        int iPaletteLength;
        switch (iBitsPerPixel) {
            case 16: return create16(bi, iTimX, iTimY);
            case 24: return create24(bi, iTimX, iTimY);

            case 4:
                if (bi.getWidth() % 4 != 0)
                    throw new IllegalArgumentException(
                        "Image width ("+bi.getWidth()+") is not divisible by 4 so cannot be converted to 4bpp Tim");
                iPaletteLength = 16;
                break;
            case 8: 
                if (bi.getWidth() % 2 != 0)
                    throw new IllegalArgumentException(
                        "Image width ("+bi.getWidth()+") is not divisible by 2 so cannot be converted to 8bpp Tim");
                iPaletteLength = 256;
                break;
                
            default: throw new IllegalArgumentException("Invalid bits per pixel " + iBitsPerPixel);
        }

        final int iSrcBpp = findBitsPerPixel(bi);
        
        Tim tim;

        if (iSrcBpp <= iBitsPerPixel) {
            // src = 4, dest = 4
            // src = 4, dest = 8
            // src = 8, dest = 8
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();
            short[] asiClut = extractPalette(icm, iPaletteLength);
            CLUT clut = new CLUT(asiClut, iClutX, iClutY, iPaletteLength);

            byte[] abIndexes = extractIndexes(bi, iBitsPerPixel == 4);
            tim = new Tim(abIndexes, iTimX, iTimY, bi.getWidth(), bi.getHeight(), iBitsPerPixel, clut);
        } else if (iSrcBpp == 8) {
            // src = 8, dest = 4
            
            // find used indexes
            int[] aiIndexes = bi.getRaster().getPixels(0, 0, bi.getWidth(), bi.getHeight(), (int[])null);
            Arrays.sort(aiIndexes);
            int[] aiMapper = new int[256];
            aiMapper[aiIndexes[0]] = 0;
            int iNewPalSize = 1;
            for (int i = 1; i < aiIndexes.length; i++) {
                int iIdx = aiIndexes[i];
                if (iIdx != aiIndexes[i-1]) {
                    aiMapper[iIdx] = iNewPalSize;
                    iNewPalSize++;
                    if (iNewPalSize > 16)
                        throw new IllegalArgumentException("Unable to fit image into 16 colors");
                }
            }

            // map current palette indexes to new indexes
            byte[] abTimImg = new byte[aiIndexes.length / 2];
            for (int o = 0, i = 0; o < abTimImg.length; o++) {
                int iPaletteIdx = aiMapper[aiIndexes[i]];
                i++;
                iPaletteIdx |= aiMapper[aiIndexes[i]];
                i++;
                abTimImg[o] = (byte)iPaletteIdx;
            }

            // update palette
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();
            int[] ai256Palette = new int[256];
            icm.getRGBs(ai256Palette);
            short[] asiClut = new short[16];
            for (int i = 0; i < iNewPalSize; i++) {
                asiClut[i] = color32toColor16(ai256Palette[aiMapper[i]]);
            }
            CLUT clut = new CLUT(asiClut, iClutX, iClutY, iPaletteLength);

            tim = new Tim(abTimImg, iTimX, iTimY, bi.getWidth(), bi.getHeight(), iBitsPerPixel, clut);

        } else {
            // src = 32, dest = 4
            // src = 32, dest = 8
            tim = createPalettedTim(bi, iBitsPerPixel == 4, iTimX, iTimY, iClutX, iClutY);
        }
        
        return tim;
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
        int iPaletteSize;
        if (iBitsPerPixel == 4)
            iPaletteSize = 16;
        else if (iBitsPerPixel == 8)
            iPaletteSize = 256;
        else
            throw new IllegalArgumentException("Unable to create a paletted Tim with " + iBitsPerPixel + " bpp");

        ColorModel cm = bi.getColorModel();
        if (!(cm instanceof IndexColorModel))
            throw new IllegalArgumentException("Image must have IndexColorModel");
        IndexColorModel icm = (IndexColorModel)cm;
        
        int[] aiPaletteArgb = new int[iPaletteSize];
        icm.getRGBs(aiPaletteArgb);

        int iWidth = bi.getWidth(), iHeight = bi.getHeight();

        // verify that the image palette matches the clut first palette
        int[] aiClut = clutImg.getRGB(0, 0, clutImg.getWidth(), clutImg.getHeight(), null, 0, clutImg.getWidth());
        for (int i = 0; i < aiPaletteArgb.length; i++) {
            if (aiPaletteArgb[i] != aiClut[i])
                throw new IllegalArgumentException("Image palette does not match CLUT colors");
        }

        // convert the 32bit RGBA8888 palette into TIM 16bit ABGR1555
        short[] asiClut = new short[aiClut.length];
        for (int i = 0; i < aiClut.length; i++) {
            asiClut[i] = color32toColor16(aiClut[i]);
        }
        CLUT c = new CLUT(asiClut, iClutX, iClutY, clutImg.getWidth());

        byte[] abTimImg = extractIndexes(bi, iBitsPerPixel == 4);
        return new Tim(abTimImg, iTimX, iTimY, iWidth, iHeight, iBitsPerPixel, c);
    }

    //--------------------------------------------------------------------------
    //-- Private ---------------------------------------------------------------
    //--------------------------------------------------------------------------

    /** If image has a palette &lt;= 16 colors: 4bpp, &lt;= 256 colors: 8bpp, otherwise 32 bpp.
     * @return 4, 8, or 32. */
    private static int findBitsPerPixel(BufferedImage bi) {
        if (bi.getColorModel() instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();
            int iPaletteSize = icm.getMapSize();
            if (iPaletteSize <= 16)
                return 4;
            else if (iPaletteSize <= 256)
                return 8;
            else
                return 32;
        } else {
            return 32;
        }
    }

    /** Returns Tim image data consisting of 4 or 8 bpp indexes.
     * If 4 bpp, assumes image width is divisible by 2. */
    private static byte[] extractIndexes(BufferedImage bi, boolean bln4bppOrNot8bpp) {
        int[] aiIndexes = bi.getRaster().getPixels(0, 0, bi.getWidth(), bi.getHeight(), (int[])null);

        byte[] abIndexes;
        if (bln4bppOrNot8bpp) {
            assert bi.getWidth() % 2 == 0; // should be checked by caller
            abIndexes = new byte[aiIndexes.length / 2];
            for (int i = 0, o = 0; i < aiIndexes.length; o++) {
                int iIdx1 = aiIndexes[i];
                i++;
                if (iIdx1 >= 16)
                    throw new IllegalArgumentException("Caller should have ensured palette index "+iIdx1+" is < 16");
                int iIdx2 = aiIndexes[i];
                i++;
                if (iIdx2 >= 16)
                    throw new IllegalArgumentException("Caller should have ensured palette index "+iIdx2+" is < 16");
                abIndexes[o] = (byte)((iIdx2 << 4) | iIdx1);
            }
        } else { // 8bpp
            abIndexes = new byte[aiIndexes.length];
            for (int i = 0; i < aiIndexes.length; i++) {
                int iIdx = aiIndexes[i];
                if (iIdx >= 256)
                    throw new IllegalArgumentException("Caller should have ensured palette index "+iIdx+" is < 256");
                abIndexes[i] = (byte)iIdx;
            }
            return abIndexes;
        }
        return abIndexes;
    }

    /** Returns ABGR1555 array with iLen length.
     * Ignores if palette is actually larger or smaller. */
    private static short[] extractPalette(IndexColorModel icm, int iLen) {
        int[] aiPalette = new int[iLen];
        icm.getRGBs(aiPalette);
        // convert the 32bit ARGB8888 palette into TIM 16bit ABGR1555
        short[] asiClut = new short[aiPalette.length];
        for (int i = 0; i < aiPalette.length; i++) {
            asiClut[i] = color32toColor16(aiPalette[i]);
        }
        return asiClut;
    }

    /** Convert image to 16 bpp Tim. */
    private static Tim create16(BufferedImage bi, int iTimX, int iTimY) {
        int[] aiArgb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
        byte[] abTim16 = new byte[aiArgb.length * 2];
        for (int i = 0, o = 0; i < aiArgb.length; i++, o+=2) {
            int iArgb = aiArgb[i];
            short siTimAbgr = color32toColor16(iArgb);
            IO.writeInt16LE(abTim16, o, siTimAbgr);
        }
        return new Tim(abTim16, iTimX, iTimY, bi.getWidth(), bi.getHeight(), 16, null);
    }

    /** Convert image to 24 bpp Tim. */
    private static Tim create24(BufferedImage bi, int iTimX, int iTimY) {
        final int iWidth = bi.getWidth(), iHeight = bi.getHeight();
        int[] aiArgb = bi.getRGB(0, 0, iWidth, iHeight, null, 0, iWidth);
        byte[] abTim24;
        if (iWidth % 2 != 0)
            throw new IllegalArgumentException("24-bit Tim must have even width");
        abTim24 = new byte[aiArgb.length * 3];
        for (int i = 0, o = 0; i < aiArgb.length; i++, o+=3) {
            int iArgb = aiArgb[i];
            abTim24[o+0]= (byte)(iArgb >> 16);
            abTim24[o+1]= (byte)(iArgb >>  8);
            abTim24[o+2]= (byte)(iArgb      );
        }
        return new Tim(abTim24, iTimX, iTimY, iWidth, iHeight, 24, null);
    }

    /** Manually generates a palette from the image.
     * Ignores its {@link IndexColorModel} if it has one.
     * @throws IllegalArgumentException if the image has too many colors to fit into the palette.
     */
    private static Tim createPalettedTim(BufferedImage bi, boolean bln4bppOrNot8bpp,
                                         int iTimX, int iTimY, int iClutX, int iClutY)
    {
        final int iWidth = bi.getWidth(), iHeight = bi.getHeight();
        
        int iPaletteSize;
        if (bln4bppOrNot8bpp) {
            assert iWidth % 4 == 0; // should be checked by caller
            iPaletteSize = 16;
        } else {
            assert iWidth % 2 == 0; // should be checked by caller
            iPaletteSize = 256;
        }

        int[] aiArgb = bi.getRGB(0, 0, iWidth, iHeight, null, 0, iWidth);

        PaletteMaker pal = new PaletteMaker(aiArgb, iPaletteSize);

        // now replace colors with indexes
        byte[] abTimImg;
        if (bln4bppOrNot8bpp)
            abTimImg = new byte[aiArgb.length / 2];
        else
            abTimImg = new byte[aiArgb.length];

        for (int o = 0, i = 0; o < abTimImg.length; o++) {
            int iPaletteIdx = pal.getPixelPaletteIndex(i);
            i++;
            // if it's 4bpp, add the other nibble index
            if (bln4bppOrNot8bpp) {
                iPaletteIdx |= pal.getPixelPaletteIndex(i) << 4;
                i++;
            }
            // write the palette index(es) to the Tim image data
            abTimImg[o] = (byte)iPaletteIdx;
        }

        CLUT clut = pal.makeClut(iClutX, iClutY);

        return new Tim(abTimImg, iTimX, iTimY, iWidth, iHeight, bln4bppOrNot8bpp ? 4 : 8, clut);
    }

    /** Little class to break up the palette generation logic in
     * {@link #createPalettedTim(java.awt.image.BufferedImage, boolean, int, int, int, int)}.
     * Consistently generates the same palette.
     * <p>
     * I also wrote a version that uses a Set to gather and sort the unique colors,
     * then dumped into a Map for retrieval. That approach turned out twice as
     * slow as this one.
     */
    private static class PaletteMaker {
        
        /** Generated Tim ABGR1555 palette.
         * Either 16 (for 4bpp) or 256 (for 8bpp) in length.
         * Only the first {@link #_iColorCount} values are meaningful, the rest are 0.
         * The meaningful values are sorted for binary search lookup. */
        public final short[] _asiPalette;

        /** Number of values in {@link #_asiPalette} that are meaningful. */
        private int _iColorCount;

        /** Image converted to ABGR1555 Tim colors. */
        private final short[] _asiTim16Image;

        /** Converts the image to ABGR1555 Tim colors and generates the palette.
         * @param aiArgb ARGB8888 image data.
         * @param iPaletteSize Palette size, 16 or 256. */
        public PaletteMaker(int[] aiArgb, int iPaletteSize) {
            // make 2 copies of the image data converted to Tim 16bpp
            _asiTim16Image = new short[aiArgb.length];
            for (int i = 0; i < aiArgb.length; i++) {
                _asiTim16Image[i] = color32toColor16(aiArgb[i]);
            }
            short[] asiSortedTim = new short[_asiTim16Image.length];
            // using arraycopy seems to be faster than .clone()
            // and faster than copying the values in the for loop
            System.arraycopy(_asiTim16Image, 0, asiSortedTim, 0, asiSortedTim.length);

            // sort one copy to extract the unique palette items
            Arrays.sort(asiSortedTim);

            // save just the unique colors into the palette
            _asiPalette = new short[iPaletteSize];
            _asiPalette[0] = asiSortedTim[0];
            _iColorCount = 1;
            for (int i = 1; i < asiSortedTim.length; i++) {
                short siColor = asiSortedTim[i];
                if (siColor != asiSortedTim[i-1]) {
                    _asiPalette[_iColorCount] = siColor;
                    _iColorCount++;
                    if (_iColorCount > iPaletteSize)
                        throw new IllegalArgumentException("Unable to fit image into " + iPaletteSize + " colors");
                }
            }
        }

        /** Get the palette index for a pixel in the image.
         * @param iPixelIndex Offset of the pixel.
         * @return Palette index or -1 if not found. */
        public int getPixelPaletteIndex(int iPixelIndex) {
            short siTim16 = _asiTim16Image[iPixelIndex];
            return binarySearch(siTim16);
        }

        /** Java 5 doesn't have binary search in a range. */
        private int binarySearch(short siKey) {
            int iMin = 0;
            int iMax = _iColorCount;
            // continue searching while [min,max] is not empty
            while (iMax >= iMin) {
                int iMid = (iMin + iMax) >>> 1;

                // determine which subarray to search
                if (_asiPalette[iMid] < siKey) // change min index to search upper subarray
                    iMin = iMid + 1;
                else if (_asiPalette[iMid] > siKey) // change max index to search lower subarray
                    iMax = iMid - 1;
                else // key found at index mid
                    return iMid;
            }
            // key not found
            return -1;
        }

        public CLUT makeClut(int iClutX, int iClutY) {
            return new CLUT(_asiPalette, iClutX, iClutY, _asiPalette.length);
        }
    }
    
    /** Works the same as
     * <pre>
     * int CONVERT_8_TO_5_BIT(int i) {
     *   return (int)Math.round(i*255/31.0);
     * }
     * </pre> */
    private static final int[] CONVERT_8_TO_5_BIT =
    {
        0,  0,  0,  0,  0,
        1,  1,  1,  1,  1,  1,  1,  1,
        2,  2,  2,  2,  2,  2,  2,  2,
        3,  3,  3,  3,  3,  3,  3,  3,
        4,  4,  4,  4,  4,  4,  4,  4,  4,
        5,  5,  5,  5,  5,  5,  5,  5,
        6,  6,  6,  6,  6,  6,  6,  6,
        7,  7,  7,  7,  7,  7,  7,  7,
        8,  8,  8,  8,  8,  8,  8,  8,
        9,  9,  9,  9,  9,  9,  9,  9,  9,
        10, 10, 10, 10, 10, 10, 10, 10,
        11, 11, 11, 11, 11, 11, 11, 11,
        12, 12, 12, 12, 12, 12, 12, 12,
        13, 13, 13, 13, 13, 13, 13, 13, 13,
        14, 14, 14, 14, 14, 14, 14, 14,
        15, 15, 15, 15, 15, 15, 15, 15,
        16, 16, 16, 16, 16, 16, 16, 16,
        17, 17, 17, 17, 17, 17, 17, 17,
        18, 18, 18, 18, 18, 18, 18, 18, 18,
        19, 19, 19, 19, 19, 19, 19, 19,
        20, 20, 20, 20, 20, 20, 20, 20,
        21, 21, 21, 21, 21, 21, 21, 21,
        22, 22, 22, 22, 22, 22, 22, 22, 22,
        23, 23, 23, 23, 23, 23, 23, 23,
        24, 24, 24, 24, 24, 24, 24, 24,
        25, 25, 25, 25, 25, 25, 25, 25,
        26, 26, 26, 26, 26, 26, 26, 26,
        27, 27, 27, 27, 27, 27, 27, 27, 27,
        28, 28, 28, 28, 28, 28, 28, 28,
        29, 29, 29, 29, 29, 29, 29, 29,
        30, 30, 30, 30, 30, 30, 30, 30,
        31, 31, 31, 31, 31,
    }; static { assert CONVERT_8_TO_5_BIT.length == 256; }

    /** ARGB8888 to Tim ABGR1555. */
    private static short color32toColor16(int i) {
        int a = (i >>> 24) & 0xFF;
        int r = CONVERT_8_TO_5_BIT[(i >>> 16) & 0xFF];
        int g = CONVERT_8_TO_5_BIT[(i >>>  8) & 0xFF];
        int b = CONVERT_8_TO_5_BIT[(i       ) & 0xFF];
        int bgr = (b << 10) | (g << 5) | r;
        if (a == 0) {
            // if totally transparent
            bgr = 0;
            a   = 0;
        } else if (a == 255) {
            // if totally opaque
            if (bgr == 0) // if totally opaque & black
                a = 1;
            else // if totally opaque & not black
                a = 0;
        } else {
            // if partially transparent
            a = 1;
        }
        return (short)((a << 15) | bgr);
    }

}
