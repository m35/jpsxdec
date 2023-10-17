/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.PsxRgb;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

/** Private functions to generate a {@link Tim} image. */
class CreateTim {

    /**
     * Quickly reads a stream to determine if the data is a Tim image.
     * @return info about the Tim image, otherwise null.
     */
    public static @CheckForNull TimInfo isTim(@Nonnull InputStream inStream)
            throws EOFException, IOException
    {
        TimValidator validator = new TimValidator();

        if (!validator.tagIsValid(inStream))
            return null;
        if (!validator.versionIsValid(inStream))
            return null;
        if (!validator.unknownIsValid(inStream))
            return null;
        if (!validator.bppAndClutIsValid(inStream))
            return null;
        if (!validator.unknownIsValid(inStream))
            return null;

        int iPaletteCount;
        if (validator.hasClut()) {
            if (!validator.clutByteSizeIsValid(inStream))
                return null;
            if (!validator.clutXisValid(inStream))
                return null;
            if (!validator.clutYisValid(inStream))
                return null;
            if (!validator.clutPixelWidthIsValid(inStream))
                return null;
            if (!validator.clutPixelHeightIsValid(inStream))
                return null;

            if (!validator.clutIsConsistent())
                return null;

            IO.skip(inStream, validator.getClutImageDataByteSize());

            iPaletteCount = Tim.calcPaletteCount(validator.getClutImageDataWordSize(), validator.getBitPerPixel());
       } else {
            iPaletteCount = 1;
        }


        if (!validator.timByteSizeIsValid(inStream))
            return null;
        if (!validator.timXisValid(inStream))
            return null;
        if (!validator.timYisValid(inStream))
            return null;
        if (!validator.wordWidthIsValid(inStream))
            return null;
        if (!validator.pixelHeightIsValid(inStream))
            return null;

        TimValidator.TimConsistency consistency = validator.timIsConsistent();
        if (consistency == TimValidator.TimConsistency.INCONSISTENT)
            return null;

        IO.skip(inStream, validator.getImageDataByteSize());

        return new TimInfo(iPaletteCount, validator.getBitPerPixel(),
                           validator.getPixelWidth(), validator.getPixelHeight());
    }

    /**
     * Parse and deserialize a TIM file from a stream.
     */
    public static @Nonnull Tim read(@Nonnull InputStream inStream)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        TimValidator validator = new TimValidator();

        if (!validator.tagIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.versionIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.unknownIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.bppAndClutIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.unknownIsValid(inStream))
            throw new BinaryDataNotRecognized();

        CLUT clut = null;
        if (validator.hasClut()) {
            if (!validator.clutByteSizeIsValid(inStream))
                throw new BinaryDataNotRecognized();
            if (!validator.clutXisValid(inStream))
                throw new BinaryDataNotRecognized();
            if (!validator.clutYisValid(inStream))
                throw new BinaryDataNotRecognized();
            if (!validator.clutPixelWidthIsValid(inStream))
                throw new BinaryDataNotRecognized();
            if (!validator.clutPixelHeightIsValid(inStream))
                throw new BinaryDataNotRecognized();

            if (!validator.clutIsConsistent())
                throw new BinaryDataNotRecognized();

            short[] asiColorData = new short[validator.getClutImageDataWordSize()];
            for (int i = 0; i < asiColorData.length; i++)
                asiColorData[i] = IO.readSInt16LE(inStream);

            clut = new CLUT(asiColorData, validator.getClutX(), validator.getClutY(), validator.getClutPixelWidth(), validator.getClutPixelHeight());
       }

        if (!validator.timByteSizeIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.timXisValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.timYisValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.wordWidthIsValid(inStream))
            throw new BinaryDataNotRecognized();
        if (!validator.pixelHeightIsValid(inStream))
            throw new BinaryDataNotRecognized();

        TimValidator.TimConsistency consistency = validator.timIsConsistent();

        if (consistency == TimValidator.TimConsistency.INCONSISTENT)
            throw new BinaryDataNotRecognized();

        byte[] abImageData = IO.readByteArray(inStream, validator.getImageDataByteSize());
        return new Tim(abImageData, validator.getTimX(), validator.getTimY(),
                       validator.getWordWidth(), validator.getPixelHeight(),
                       validator.getBitPerPixel(), clut,
                       consistency == TimValidator.TimConsistency.INCONSISTENT_BUT_VALID);
    }

    /**
     * Creates a TIM with the same or similar color-model of a {@link BufferedImage}.
     *
     * {@link BufferedImage} bpp map:
     * {@code 32 -> 16} (no CLUT)
     * {@code 8 -> 8} (256 color CLUT)
     * {@code 4 -> 4} (16 color CLUT)
     *
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.
     */
    public static @Nonnull Tim create(@Nonnull BufferedImage bi,
                                      int iTimX, int iTimY,
                                      int iClutX, int iClutY)
    {
        final int iSrcBpp = findBitsPerPixel(bi);

        int iTargetBpp;

        if (iSrcBpp == 32)
            // use 16 BPP (instead of 24) since it's the most common
            // if 24 bpp is needed, use other function
            iTargetBpp = 16;
        else
            iTargetBpp = iSrcBpp;

        try {
            return create(bi, iTargetBpp, iTimX, iTimY, iClutX, iClutY);
        } catch (IncompatibleException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    /**
     * Creates a TIM from a BufferedImage with the specified bits-per-pixel.
     *
     * @param iBitsPerPixel 4, 8, 16, or 24.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate. Ignored if bpp is 16 or 24.
     * @param iClutY CLUT Y coordinate. Ignored if bpp is 16 or 24.
     *
     * @throws IncompatibleException if unable to convert an 8 bpp {@link BufferedImage}
     *                               to a 4 bpp Tim because there are too many colors used.
     */
    public static @Nonnull Tim create(@Nonnull BufferedImage bi, int iBitsPerPixel,
                                      int iTimX, int iTimY,
                                      int iClutX, int iClutY)
            throws IncompatibleException
    {
        TimValidator validator = new TimValidator();
        if (!validator.bppIsValid(iBitsPerPixel))
            throw new IllegalArgumentException();
        if (!validator.timXisValid(iTimX))
            throw new IllegalArgumentException();
        if (!validator.timYisValid(iTimY))
            throw new IllegalArgumentException();
        if (!validator.pixelWidthIsValid(bi.getWidth()))
            throw new IllegalArgumentException();
        if (!validator.pixelHeightIsValid(bi.getHeight()))
            throw new IllegalArgumentException();

        int iPaletteLength;
        switch (validator.getBitPerPixel()) {
            case 16: return create16(bi, validator);
            case 24: return create24(bi, validator);
            case 4:
                iPaletteLength = 16;
                break;
            case 8:
                iPaletteLength = 256;
                break;
            default: throw new RuntimeException();
        }

        final int iSrcBpp = findBitsPerPixel(bi);

        Tim tim;

        if (iSrcBpp <= validator.getBitPerPixel()) {
            // src = 4, dest = 4
            // src = 4, dest = 8
            // src = 8, dest = 8
            IndexColorModel icm = (IndexColorModel) bi.getColorModel();
            short[] asiClut = extractPalette(icm, iPaletteLength);
            CLUT clut = new CLUT(asiClut, validator.getClutX(), validator.getClutY(), iPaletteLength, 1);

            byte[] abIndexes = extractIndexes(bi, validator.getBitPerPixel() == 4);
            tim = new Tim(abIndexes, validator.getTimX(), validator.getTimY(),
                          validator.getWordWidth(), validator.getPixelHeight(),
                          validator.getBitPerPixel(), clut);
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
                        throw new IncompatibleException("Unable to fit image into 16 colors");
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
                asiClut[i] = PsxRgb.ARGB8888toPsxABGR1555(ai256Palette[aiMapper[i]]);
            }
            CLUT clut = new CLUT(asiClut, validator.getClutX(), validator.getClutY(), iPaletteLength, 1);

            tim = new Tim(abTimImg, validator.getTimX(), validator.getTimY(), validator.getWordWidth(), validator.getPixelHeight(), validator.getBitPerPixel(), clut);

        } else {
            // src = 32, dest = 4
            // src = 32, dest = 8
            tim = createPalettedTim(bi, validator.getBitPerPixel() == 4, validator);
        }

        return tim;
    }

    /**
     * Create a TIM image with a custom CLUT.
     * This is the most advanced method of creating a TIM image.
     * It allows you to create a TIM with multiple CLUT palettes.
     *
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.
     * @param iBitsPerPixel Either 4 or 8.
     *
     * @throws IllegalArgumentException if
     *      the {@link BufferedImage} does not have an {@link IndexColorModel},
     *      or the colors of the palette indexes in the {@link BufferedImage} do not match the first palette in the CLUT.
     */
    public static @Nonnull Tim create(@Nonnull BufferedImage bi, int iTimX, int iTimY,
                                      @Nonnull BufferedImage clutImg, int iClutX, int iClutY,
                                      int iBitsPerPixel)
    {
        TimValidator validator = new TimValidator();
        if (!validator.bppIsValid(iBitsPerPixel, true))
            throw new IllegalArgumentException();

        int iPaletteSize;
        if (iBitsPerPixel == 4)
            iPaletteSize = 16;
        else if (iBitsPerPixel == 8)
            iPaletteSize = 256;
        else
            throw new IllegalArgumentException("Unable to create a paletted Tim with " + iBitsPerPixel + " bpp");

        if (!validator.clutXisValid(iClutX))
            throw new IllegalArgumentException();
        if (!validator.clutYisValid(iClutY))
            throw new IllegalArgumentException();
        if (!validator.clutPixelWidthIsValid(clutImg.getWidth()))
            throw new IllegalArgumentException();
        if (!validator.clutPixelHeightIsValid(clutImg.getHeight()))
            throw new IllegalArgumentException();

        if (!validator.timXisValid(iTimX))
            throw new IllegalArgumentException();
        if (!validator.timYisValid(iTimY))
            throw new IllegalArgumentException();
        if (!validator.pixelWidthIsValid(bi.getWidth()))
            throw new IllegalArgumentException();
        if (!validator.pixelHeightIsValid(bi.getHeight()))
            throw new IllegalArgumentException();

        ColorModel cm = bi.getColorModel();
        if (!(cm instanceof IndexColorModel))
            throw new IllegalArgumentException("Image must have IndexColorModel");
        IndexColorModel icm = (IndexColorModel)cm;

        int[] aiPaletteArgb = new int[iPaletteSize];
        icm.getRGBs(aiPaletteArgb);

        // verify that the image palette matches the clut first palette
        int[] aiClut = clutImg.getRGB(0, 0, clutImg.getWidth(), clutImg.getHeight(), null, 0, clutImg.getWidth());
        for (int i = 0; i < aiPaletteArgb.length; i++) {
            if (aiPaletteArgb[i] != aiClut[i])
                throw new IllegalArgumentException("Image palette does not match CLUT colors");
        }

        // convert the 32bit RGBA8888 palette into TIM 16bit ABGR1555
        short[] asiClut = new short[aiClut.length];
        for (int i = 0; i < aiClut.length; i++) {
            asiClut[i] = PsxRgb.ARGB8888toPsxABGR1555(aiClut[i]);
        }
        CLUT clut = new CLUT(asiClut, validator.getClutX(), validator.getClutY(), validator.getClutPixelWidth(), validator.getClutPixelHeight());

        byte[] abTimImg = extractIndexes(bi, iBitsPerPixel == 4);
        return new Tim(abTimImg, validator.getTimX(), validator.getTimY(), validator.getWordWidth(), validator.getPixelHeight(), validator.getBitPerPixel(), clut);
    }

    //--------------------------------------------------------------------------
    //-- Private ---------------------------------------------------------------
    //--------------------------------------------------------------------------

    /** If image has a palette &lt;= 16 colors: 4bpp, &lt;= 256 colors: 8bpp, otherwise 32 bpp.
     * @return 4, 8, or 32. */
    private static int findBitsPerPixel(@Nonnull BufferedImage bi) {
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
    private static @Nonnull byte[] extractIndexes(@Nonnull BufferedImage bi,
                                                  boolean bln4bppOrNot8bpp)
    {
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
    private static @Nonnull short[] extractPalette(@Nonnull IndexColorModel icm, int iLen) {
        int[] aiPalette = new int[iLen];
        icm.getRGBs(aiPalette);
        // convert the 32bit ARGB8888 palette into TIM 16bit ABGR1555
        short[] asiClut = new short[aiPalette.length];
        for (int i = 0; i < aiPalette.length; i++) {
            asiClut[i] = PsxRgb.ARGB8888toPsxABGR1555(aiPalette[i]);
        }
        return asiClut;
    }

    /** Convert image to 16 bpp Tim. */
    private static @Nonnull Tim create16(@Nonnull BufferedImage bi, @Nonnull TimValidator validator) {
        int[] aiArgb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
        byte[] abTim16 = new byte[aiArgb.length * 2];
        for (int i = 0, o = 0; i < aiArgb.length; i++, o+=2) {
            int iArgb = aiArgb[i];
            short siTimAbgr = PsxRgb.ARGB8888toPsxABGR1555(iArgb);
            IO.writeInt16LE(abTim16, o, siTimAbgr);
        }
        return new Tim(abTim16, validator.getTimX(), validator.getTimY(), validator.getWordWidth(), validator.getPixelHeight(), 16, null);
    }

    /** Convert image to 24 bpp Tim. */
    private static @Nonnull Tim create24(@Nonnull BufferedImage bi, @Nonnull TimValidator validator) {
        int[] aiArgb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());
        byte[] abTim24 = new byte[aiArgb.length * 3];
        for (int i = 0, o = 0; i < aiArgb.length; i++, o+=3) {
            int iArgb = aiArgb[i];
            abTim24[o+0]= (byte)(iArgb >> 16);
            abTim24[o+1]= (byte)(iArgb >>  8);
            abTim24[o+2]= (byte)(iArgb      );
        }
        return new Tim(abTim24, validator.getTimX(), validator.getTimY(), validator.getWordWidth(), validator.getPixelHeight(), 24, null);
    }

    /**
     * Manually generates a palette from the image.
     * Ignores its {@link IndexColorModel} if it has one.
     * @throws IllegalArgumentException if the image has too many colors to fit into the palette.
     */
    private static @Nonnull Tim createPalettedTim(@Nonnull BufferedImage bi,
                                                  boolean bln4bppOrNot8bpp,
                                                  @Nonnull TimValidator validator)
    {
        int iPaletteSize;
        if (bln4bppOrNot8bpp) {
            iPaletteSize = 16;
        } else {
            iPaletteSize = 256;
        }

        int[] aiArgb = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());

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

        CLUT clut = pal.makeClut(validator.getClutX(), validator.getClutY());

        int iBpp = bln4bppOrNot8bpp ? 4 : 8;
        return new Tim(abTimImg, validator.getTimX(), validator.getTimY(), validator.getWordWidth(), validator.getPixelHeight(), iBpp, clut);
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
        @Nonnull
        public final short[] _asiPalette;

        /** Number of values in {@link #_asiPalette} that are meaningful. */
        private int _iColorCount;

        /** Image converted to ABGR1555 Tim colors. */
        @Nonnull
        private final short[] _asiTim16Image;

        /** Converts the image to ABGR1555 Tim colors and generates the palette.
         * @param aiArgb ARGB8888 image data.
         * @param iPaletteSize Palette size, 16 or 256. */
        public PaletteMaker(@Nonnull int[] aiArgb, int iPaletteSize) {
            // make 2 copies of the image data converted to Tim 16bpp
            _asiTim16Image = new short[aiArgb.length];
            for (int i = 0; i < aiArgb.length; i++) {
                _asiTim16Image[i] = PsxRgb.ARGB8888toPsxABGR1555(aiArgb[i]);
            }
            // using copyof seems to be faster than .clone()
            // and faster than copying the values in the for loop
            short[] asiSortedTim = Arrays.copyOf(_asiTim16Image, _asiTim16Image.length);

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
            return Arrays.binarySearch(_asiPalette, 0, _iColorCount, siTim16);
        }

        public @Nonnull CLUT makeClut(int iClutX, int iClutY) {
            return new CLUT(_asiPalette, iClutX, iClutY, _asiPalette.length, 1);
        }
    }

}
