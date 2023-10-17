/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.PsxRgb;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

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
    public static @CheckForNull TimInfo isTim(@Nonnull InputStream inStream)
            throws EOFException, IOException
    {
        return CreateTim.isTim(inStream);
    }

    /** Parse and deserialize a TIM file from a stream. */
    public static @Nonnull Tim read(@Nonnull InputStream inStream)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        return CreateTim.read(inStream);
    }

    /**
     * Creates a TIM with the same or similar color-model of a {@link BufferedImage}.
     * @param iTimX Tim X coordinate.
     * @param iTimY Tim Y coordinate.
     * @param iClutX CLUT X coordinate.
     * @param iClutY CLUT Y coordinate.
     */
    public static @Nonnull Tim create(@Nonnull BufferedImage bi,
                                      int iTimX, int iTimY,
                                      int iClutX, int iClutY)
    {
        return CreateTim.create(bi, iTimX, iTimY, iClutX, iClutY);
    }

    /**
     * Creates a TIM from a BufferedImage with the specified bits-per-pixel.
     * @param iBitsPerPixel 4, 8, 16, or 24.
     *
     * @throws IncompatibleException if unable to convert an 8 bpp {@link BufferedImage}
     *                               to a 4 bpp Tim because there are too many colors used.
     */
    public static @Nonnull Tim create(@Nonnull BufferedImage bi, int iBitsPerPixel) throws IncompatibleException {
        return CreateTim.create(bi, iBitsPerPixel, 0, 0, 0, 0);
    }

    //--------------------------------------------------------------------------
    //-- Constants -------------------------------------------------------------
    //--------------------------------------------------------------------------

    /** Size of the Tim header in bytes. */
    static final int HEADER_SIZE = 12;

    /** Magic 8-bit value at the start of Tim. */
    static final int TAG_MAGIC = 0x10;
    /** All Tim images are version 0. */
    static final int VERSION_0 = 0;

    /** I believe this is the smallest possible size of a Tim file. 1x1 pixels, no CLUT. */
    public static final int MINIMUM_TIM_SIZE = HEADER_SIZE + 10;

    //--------------------------------------------------------------------------
    //-- Fields ----------------------------------------------------------------
    //--------------------------------------------------------------------------

    private final int _iBitsPerPixel;

    /** The color lookup table for the TIM. null if none. */
    @CheckForNull
    private final CLUT _clut;

    /**
     * Position of the Tim in pixels.
     * This can be used to indicate the location the image should be copied to in the VRAM.
     */
    private final int _iTimX, _iTimY;

    /**
     * Width of the image in words (2 bytes).
     * The pixel width is calculated with {@link #_iBitsPerPixel}.
     */
    private final int _iWordWidth;
    /**
     * Height of the image in pixels.
     */
    private final int _iPixelHeight;

    /**
     * The raw image data of the TIM. Data differs depending on bits-per-pixel:
     * <ul>
     * <li>4bpp : 16 color paletted image, two 4-bit palette indexes per byte,
     *            in the order of 1/0, 3/2, etc
     * <li>8bpp : 256 color paletted image, one 8-bit palette index per byte.
     * <li>16bpp: ABGR1555 shorts in little endian.
     * <li>24bpp: RGB888 in big endian.
     * </ul>
     * @see #_iBitsPerPixel
     */
    @Nonnull
    private final byte[] _abImageData;

    private final boolean _blnTimHasIssues;

    //..........................................................................


    Tim(@Nonnull byte[] abTimImageData, int iTimX, int iTimY,
        int iWordWidth, int iPixelHeight, int iBitsPerPixel,
        @CheckForNull CLUT clut)
    {
        this(abTimImageData, iTimX, iTimY, iWordWidth, iPixelHeight, iBitsPerPixel, clut, false);
    }

    Tim(@Nonnull byte[] abTimImageData, int iTimX, int iTimY,
        int iWordWidth, int iPixelHeight, int iBitsPerPixel,
        @CheckForNull CLUT clut, boolean blnTimHasIssues)
    {
        // The caller should validate the parameters
        _abImageData = abTimImageData;
        _iTimX = iTimX;
        _iTimY = iTimY;
        _iWordWidth = iWordWidth;
        _iPixelHeight = iPixelHeight;
        _iBitsPerPixel = iBitsPerPixel;
        _clut = clut;
        _blnTimHasIssues = blnTimHasIssues;
    }


    //--------------------------------------------------------------------------
    //-- Public functions ------------------------------------------------------
    //--------------------------------------------------------------------------

    /**
     * Bits-per-pixel: 4, 8, 16, or 24.
     */
    public int getBitsPerPixel() {
        return _iBitsPerPixel;
    }

    /**
     * If the TIM is paletted and has a CLUT, returns the number of CLUT
     * palettes. Otherwise if the TIM is paletted and has not CLUT, or if the
     * TIM is true-color, returns 1.
     * <p>
     * Each TIM file can have multiple palettes. The TIM data doesn't even
     * need to use these palettes for drawing, but they usually do.
     */
    public int getPaletteCount() {
        if (_clut == null)
            return 1;
        return calcPaletteCount(_clut._asiColorData.length, _iBitsPerPixel);
    }

    static int calcPaletteCount(int iClutColorDataLength, int iBitsPerPixel) {
        if (iBitsPerPixel == 4 || iBitsPerPixel == 8) {
            int iColorsForBitsPerPixel = 1 << iBitsPerPixel;
            return iClutColorDataLength / iColorsForBitsPerPixel;
        } else {
            return 1;
        }
    }

    /** Width of TIM in pixels. */
    public int getPixelWidth() {
        return calculatePixelWidth(_iWordWidth, _iBitsPerPixel);
    }

    static int calculatePixelWidth(int iWordWidth, int iBitsPerPixel) {
        switch (iBitsPerPixel) {
            case 4:  return iWordWidth * 2 * 2;
            case 8:  return iWordWidth * 2    ;
            case 16: return iWordWidth        ;
            case 24: return iWordWidth * 2 / 3;
            default: throw new RuntimeException("Impossible Tim BPP " + iBitsPerPixel);
        }
    }

    /** Height of TIM in pixels. */
    public int getPixelHeight() {
        return _iPixelHeight;
    }

    /**
     * Note!: The Java API to save a {@link BufferedImage} to the disk
     * may change the palette order and indexes in the saved image.
     *
     * @param iPalette  Which palette to use for the decoded image.
     *
     * @see #getPaletteCount()
     */
    public @Nonnull BufferedImage toBufferedImage(int iPalette) {
        if (iPalette < 0 || iPalette >= getPaletteCount())
            throw new IllegalArgumentException("Palette index "+iPalette+" out of bounds");

        switch (_iBitsPerPixel) {
            case 4: return toBi4(iPalette);
            case 8: return toBi8(iPalette);
            case 16: return toBi16();
            case 24: return toBi24();
            default:throw new RuntimeException();
        }
    }

    /**
     * Converts the CLUT (color lookup table) to a {@link BufferedImage}.
     *
     * @return null if image has no CLUT.
     */
    public @CheckForNull BufferedImage getClutImage() {
        if (_clut != null)
            return _clut.toBufferedImage();
        else
            return null;
    }

    /**
     * If the Tim has inconsistent data, but is still usable.
     */
    public boolean timHasIssues() {
        return _blnTimHasIssues;
    }

    /**
     * Tries to replace this TIM's image data and palette data (if it has a CLUT)
     * with the image data of the buffered image.
     *
     * @throws IncompatibleException if the BufferedImage data is incompatible.
     */
    public void replaceImageData(@Nonnull BufferedImage bi) throws IncompatibleException {
        Tim newTim = create(bi, _iBitsPerPixel);

        // if this has a CLUT, then the newly created tim should also have a CLUT
        if (_clut != null) {
            if (newTim._clut == null)
                throw new IncompatibleException();
            assert _clut._asiColorData.length == newTim._clut._asiColorData.length;
            System.arraycopy(newTim._clut._asiColorData, 0, _clut._asiColorData, 0, _clut._asiColorData.length);
        } else {
            if (newTim._clut != null)
                throw new IncompatibleException();
        }

        assert _abImageData.length == newTim._abImageData.length;
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
    }

    /**
     * Tries to replace this TIM's image data and palette data
     * with the image data of the buffered image and CLUT.
     * @throws IncompatibleException if the BufferedImage data is incompatible
     *                               or there is no CLUT.
     */
    public void replaceImageData(@Nonnull BufferedImage bi, @Nonnull BufferedImage clut) throws IncompatibleException {
        if (_clut == null)
            throw new IncompatibleException("Can't change the CLUT when Tim doesn't have a CLUT");

        Tim newTim = CreateTim.create(bi, _iTimX, _iTimY, clut, _clut.getX(), _clut.getY(), _iBitsPerPixel);

        assert _abImageData.length == newTim._abImageData.length;
        System.arraycopy(newTim._abImageData, 0, _abImageData, 0, _abImageData.length);
        assert _clut._asiColorData.length == newTim._clut._asiColorData.length;
        System.arraycopy(newTim._clut._asiColorData, 0, _clut._asiColorData, 0, _clut._asiColorData.length);
    }

    /** Writes TIM image to the stream. */
    public void write(@Nonnull OutputStream os) throws IOException {
        os.write(TAG_MAGIC);
        os.write(VERSION_0);
        IO.writeInt16LE(os, 0); // Unknown 1
        IO.writeInt16LE(os, calculateBpp_HasCLUT());
        IO.writeInt16LE(os, 0); // Unknown 2

        if (_clut != null)
            _clut.write(os);

        IO.writeInt32LE(os, calculateByteSize());
        IO.writeInt16LE(os, _iTimX);
        IO.writeInt16LE(os, _iTimY);
        IO.writeInt16LE(os, _iWordWidth);
        IO.writeInt16LE(os, _iPixelHeight);

        os.write(_abImageData);
    }

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
    private int calculateByteSize() {
        return HEADER_SIZE + _abImageData.length;
    }

    public enum Mismatch {
        Dimensions,
        BitsPerPixel,
        PaletteCount,
        HasClut,
        MissingClut,
        ClutWidth,
        ClutXY,
        ClutPaletteLength
    }

    /** Compares properties with other Tim and returns the first discovered
     * difference. this does not compare the image and CLUT contents,
     * only the properties of them.
     * @return null if both Tims have exactly the same properties. */
    public @CheckForNull Mismatch matches(@Nonnull Tim other) {
        if (_iWordWidth != other._iWordWidth ||
            getPixelHeight() != other.getPixelHeight())
            return Mismatch.Dimensions;
        if (getBitsPerPixel() != other.getBitsPerPixel())
            return Mismatch.BitsPerPixel;
        if (getPaletteCount() != other.getPaletteCount())
            return Mismatch.PaletteCount;
        CLUT otherClut = other._clut;
        if (_clut == null && otherClut != null)
            return Mismatch.HasClut;
        if (_clut != null) {
            if (otherClut == null)
               return Mismatch.MissingClut;
            if (_clut.getX() != otherClut.getX() ||
                _clut.getY() != otherClut.getY())
                return Mismatch.ClutXY;
            if (_clut.getPaletteLength() != otherClut.getPaletteLength())
                return Mismatch.ClutPaletteLength;
        }
        return null;
    }

    @Override
    public String toString() {
        String s = String.format(
            "%dx%d %dbpp xy(%d, %d) WordWidth:%d Size:",
            getPixelWidth(),
            getPixelHeight(),
            getBitsPerPixel(),
            _iTimX,
            _iTimY,
            _iWordWidth);
        StringBuilder sb = new StringBuilder(s);

        int iActualByteSize = calculateByteSize();
        int iRequiredByteSize = _iWordWidth * 2 * _iPixelHeight + HEADER_SIZE;

        if (iActualByteSize == iRequiredByteSize) {
            sb.append(iActualByteSize);
        } else {
            sb.append(String.format("%d=%d+%d", iActualByteSize, iRequiredByteSize, iActualByteSize - iRequiredByteSize));
        }

        if (_clut != null) {
            sb.append(" CLUT[").append(_clut).append("]");
        }

        return sb.toString();
    }

    //--------------------------------------------------------------------------

    /**
     * Convert this 4 bpp Tim to a BufferedImage with 16 color palette.
     */
    private @Nonnull BufferedImage toBi4(int iPalette) {

        byte[] abRgbaPalette = new byte[16 * 4];
        if (_clut == null) {
            PsxRgb.fill16GrayRgbaPalette(abRgbaPalette);
        } else {
            // convert CLUT to array of RGBA bytes
            for (int i = iPalette * 16, o = 0; o < abRgbaPalette.length; i++, o+=4) {
                int iArgb = PsxRgb.psxABGR1555toARGB8888(_clut.getColor(i), SEMI_TRANSPARENT);
                abRgbaPalette[o+0] = (byte)(iArgb >> 16);
                abRgbaPalette[o+1] = (byte)(iArgb >>  8);
                abRgbaPalette[o+2] = (byte)(iArgb      );
                abRgbaPalette[o+3] = (byte)(iArgb >> 24);
            }
        }

        IndexColorModel cm = new IndexColorModel(4, 16, abRgbaPalette, 0, true);

        WritableRaster raster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE,
                                                          getPixelWidth(), getPixelHeight(),
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
    private @Nonnull BufferedImage toBi8(int iPalette) {
        byte[] abRgbaPalette = new byte[256 * 4];
        if (_clut == null) {
            PsxRgb.fill256GrayRgbaPalette(abRgbaPalette);
        } else {
            // convert CLUT to array of RGBA bytes
            for (int i = iPalette * 256, o = 0; o < abRgbaPalette.length; i++, o+=4) {
                int iArgb = PsxRgb.psxABGR1555toARGB8888(_clut.getColor(i), SEMI_TRANSPARENT);
                abRgbaPalette[o+0] = (byte)(iArgb >> 16);
                abRgbaPalette[o+1] = (byte)(iArgb >>  8);
                abRgbaPalette[o+2] = (byte)(iArgb      );
                abRgbaPalette[o+3] = (byte)(iArgb >> 24);
            }
        }

        IndexColorModel cm = new IndexColorModel(8, 256, abRgbaPalette, 0, true);
        SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                                         getPixelWidth(), getPixelHeight(),
                                                         1, getPixelWidth(),
                                                         new int[] {0});

        WritableRaster raster = Raster.createWritableRaster(sm, null);
        byte[] abBufferIndexes = ((DataBufferByte)raster.getDataBuffer()).getData();
        System.arraycopy(_abImageData, 0, abBufferIndexes, 0, abBufferIndexes.length);
        return new BufferedImage(cm, raster, false, null);
    }

    /** Convert this 24 bpp Tim to a BufferedImage. */
    private @Nonnull BufferedImage toBi24() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] aiBits = {8, 8, 8};
        int[] aiChannelIdxes = {0, 1, 2};
        ColorModel cm = new ComponentColorModel(cs, aiBits, false, false,
                                                Transparency.OPAQUE,
                                                DataBuffer.TYPE_BYTE);

        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                                                               getPixelWidth(), getPixelHeight(),
                                                               getPixelWidth() * 3, 3,
                                                               aiChannelIdxes, null);
        byte[] abBufferRgb = ((DataBufferByte)raster.getDataBuffer()).getData();
        System.arraycopy(_abImageData, 0, abBufferRgb, 0, abBufferRgb.length);
        return new BufferedImage(cm, raster, false, null);
    }

    /** Convert this 16 bpp Tim to a BufferedImage. */
    private @Nonnull BufferedImage toBi16() {
        BufferedImage bi = new BufferedImage(getPixelWidth(), getPixelHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] aiBufferRgba = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
        // convert 16-bit ABGR1555 image data to 32-bit ARGB8888
        for (int i = 0, o = 0; o < aiBufferRgba.length; i+=2, o++) {
            int iColor16 = IO.readUInt16LE(_abImageData, i);
            aiBufferRgba[o] = PsxRgb.psxABGR1555toARGB8888(iColor16, SEMI_TRANSPARENT);
        }
        return bi;
    }

}
