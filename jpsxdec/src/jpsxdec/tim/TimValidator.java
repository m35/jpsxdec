/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2020-2023  Michael Sabin
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/**
 * Combination builder and validator.
 */
public class TimValidator {

    private static final Logger LOG = Logger.getLogger(TimValidator.class.getName());

    /**
     * The PSX VRAM pixel word width is 1024, but some Tim images have even longer
     * width. So be flexible, but don't allow for huge sizes.
     */
    private static final int MAX_TIM_WORD_WIDTH = 16384;
    /**
     * The PSX VRAM pixel height is 512, but some Tim images have even higher
     * heights. So be flexible, but don't allow for huge sizes.
     */
    private static final int MAX_TIM_HEIGHT = 8192;


    public boolean tagIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt8(is) == Tim.TAG_MAGIC; // 0x10
    }

    public boolean versionIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt8(is) == Tim.VERSION_0;
    }

    public boolean unknownIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt16LE(is) == 0;
    }

    private int _iBitsPerPixel = -1;
    private boolean _blnHasClut;

    public boolean bppAndClutIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        int iBpp_hasClut = IO.readUInt16LE(is);
        if ((iBpp_hasClut & 0xFFF4) != 0)
            return false;

        _blnHasClut = (iBpp_hasClut & 0x8) != 0;
        /*
        Convert the 2-bit value found in the Tim header to its bits-per-pixel.
        00b = 4
        01b = 8
        10b = 16
        11b = 24
         */
        switch (iBpp_hasClut & 3) {
            case 0: _iBitsPerPixel = 4; break;
            case 1: _iBitsPerPixel = 8; break;
            case 2: _iBitsPerPixel = 16; break;
            case 3: _iBitsPerPixel = 24; break;
            default: throw new RuntimeException();
        }
        return true;
    }
    public boolean bppIsValid(int iBitsPerPixel) {
        return bppIsValid(iBitsPerPixel,
                          iBitsPerPixel == 4 || iBitsPerPixel == 8);
    }
    public boolean bppIsValid(int iBitsPerPixel, boolean blnHasClut) {
        // It's possible that 24 bits-per-pixel has a CLUT

        switch (iBitsPerPixel) {
            case 4: case 8: case 16: case 24:
                _iBitsPerPixel = iBitsPerPixel;
                break;
            default:
                return false;
        }

        _blnHasClut = blnHasClut;

        return true;
    }
    public int getBitPerPixel() {
        return _iBitsPerPixel;
    }
    public boolean hasClut() {
        return _blnHasClut;
    }


    /**
     * Size of the Tim image data and header, excluding the CLUT.
     */
    private int _iTimByteSize = -1;
    public boolean timByteSizeIsValid(@Nonnull InputStream is) throws EOFException, IOException {

        final int MAX_POSSIBLE_TIM_DATA_SIZE = MAX_TIM_WORD_WIDTH * 2 * MAX_TIM_HEIGHT + Tim.HEADER_SIZE;

        int iImageDataByteSize = IO.readSInt32LE(is);
        if (iImageDataByteSize <= 0 || iImageDataByteSize > MAX_POSSIBLE_TIM_DATA_SIZE)
            return false;

        _iTimByteSize = iImageDataByteSize;

        return true;
    }
    public int getImageDataByteSize() {
        return _iTimByteSize - Tim.HEADER_SIZE;
    }


    private int _iTimX = -1;
    public boolean timXisValid(@Nonnull InputStream is) throws EOFException, IOException {
        return timXisValid(IO.readUInt16LE(is));
    }
    public boolean timXisValid(int iTimX) {
        if (iTimX < 0 || iTimX >= MAX_TIM_WORD_WIDTH)
            return false;

        _iTimX = iTimX;
        return true;
    }
    public int getTimX() {
        return _iTimX;
    }

    private int _iTimY = -1;
    public boolean timYisValid(@Nonnull InputStream is) throws EOFException, IOException {
        return timYisValid(IO.readUInt16LE(is));
    }
    public boolean timYisValid(int iTimY) {
        if (iTimY < 0 || iTimY >= MAX_TIM_HEIGHT)
            return false;

        _iTimY = iTimY;
        return true;
    }
    public int getTimY() {
        return _iTimY;
    }

    private int _iWordWidth = -1;
    public boolean wordWidthIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return wordWidthIsValid(IO.readUInt16LE(is));
    }
    public boolean wordWidthIsValid(int iWordWidth) {
        if (iWordWidth <= 0 || iWordWidth > MAX_TIM_WORD_WIDTH)
            return false;

        _iWordWidth = iWordWidth;
        return true;
    }
    public int getWordWidth() {
        return _iWordWidth;
    }

    public boolean pixelWidthIsValid(int iPixelWidth) {
        final int iWordWidth;
        if (_iBitsPerPixel == 16)
            iWordWidth = iPixelWidth;
        else if (_iBitsPerPixel == 4) {
            if (iPixelWidth % 4 != 0)
                return false;
            iWordWidth = iPixelWidth / 2 / 2;
        } else {
            if (iPixelWidth % 2 != 0)
                return false;
            else if (_iBitsPerPixel == 8)
                iWordWidth = iPixelWidth / 2;
            else if (_iBitsPerPixel == 24)
                iWordWidth = iPixelWidth * 3 / 2;
            else
                throw new IllegalStateException("Invalid bits-per-pixel " + _iBitsPerPixel);
        }
        return wordWidthIsValid(iWordWidth);
    }
    public int getPixelWidth() {
        return Tim.calculatePixelWidth(_iWordWidth, _iBitsPerPixel);
    }

    private int _iPixelHeight = -1;
    public boolean pixelHeightIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return pixelHeightIsValid(IO.readUInt16LE(is));
    }
    public boolean pixelHeightIsValid(int iPixelHeight) {
        if (iPixelHeight <= 0 || iPixelHeight > MAX_TIM_HEIGHT)
            return false;
        _iPixelHeight = iPixelHeight;
        return true;
    }
    public int getPixelHeight() {
        return _iPixelHeight;
    }

    public static enum TimConsistency {
        TOTALLY_CONSISTENT,
        INCONSISTENT_BUT_VALID,
        INCONSISTENT
    }

    public @Nonnull TimConsistency timIsConsistent() {
        int iExpectedTimByteSize = _iWordWidth * 2 * _iPixelHeight + Tim.HEADER_SIZE;

        TimConsistency consistency = TimConsistency.TOTALLY_CONSISTENT;

        if (_iTimByteSize != iExpectedTimByteSize) {
            // Github issue #29 "TIM image not found in file"
            // The image length in the header was 2 bytes longer than the calculated length.
            // The rest of the data was a valid Tim image.
            // Further research shows +2 bytes is not uncommon among games,
            // but only +2, no other amount. So allow for +2 as well.
            if (_iTimByteSize < iExpectedTimByteSize || _iTimByteSize > iExpectedTimByteSize + 2)
                return TimConsistency.INCONSISTENT;

            LOG.log(Level.WARNING, "Tim data requires only {0,number,#} bytes but header says Tim is {1,number,#} bytes",
                                   new Object[]{iExpectedTimByteSize, _iTimByteSize});

            consistency = TimConsistency.INCONSISTENT_BUT_VALID;
        }

        // Also include the trailing data in the read.
        // That will allow writing an identical TIM image.

        if (_blnHasClut) {
            // User CUE reported an issue with some strange TIM images from
            // "Guardian's Crusade" that report 16 bits-per-pixel, but also
            // have a CLUT (bug "JPSXDEC-4").
            // Turns out the image data was actually 4 or 8 bits-per-pixels.
            // The CLUT width seemed to correlate with the bits-per-pixel
            // (width < 256 : 4 bits/pixel, width >= 256 : 8 bits/pixel)
            // No clear way to handle this case, so we'll change the code
            // to at least handle these unique images the best we can.
            if (_iBitsPerPixel == 16) {
                if (_iClutPixelWidth < 256)
                    _iBitsPerPixel = 4;
                else
                    _iBitsPerPixel = 8;
                LOG.log(Level.WARNING, "TIM reports 16 bits/pixel, but it also has a CLUT. Assuming {0,number,#} bits/pixel", _iBitsPerPixel);
                consistency = TimConsistency.INCONSISTENT_BUT_VALID;
            } else if (_iBitsPerPixel == 24) {
                LOG.log(Level.WARNING, "TIM reports 24 bits/pixel, but it also has a CLUT. Assuming 8 bits/pixel");
                _iBitsPerPixel = 8;
                consistency = TimConsistency.INCONSISTENT_BUT_VALID;
            }
        }

        return consistency;
    }

    // =========================================================================
    // CLUT

    /**
     * Size of the entire CLUT structure, in bytes.
     */
    private int _iClutByteSize = -1;
    public boolean clutByteSizeIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        final int CLUT_MAX_BYTE_SIZE = MAX_TIM_WORD_WIDTH * 2 * MAX_TIM_HEIGHT + CLUT.HEADER_SIZE;

        int iClutByteSize = IO.readSInt32LE(is);
        if (iClutByteSize <= 0 || iClutByteSize > CLUT_MAX_BYTE_SIZE || (iClutByteSize % 2 != 0))
            return false;
        _iClutByteSize = iClutByteSize;
        return true;
    }
    public int getClutImageDataByteSize() {
        return _iClutByteSize - CLUT.HEADER_SIZE;
    }
    public int getClutImageDataWordSize() {
        return getClutImageDataByteSize() / 2;
    }


    private int _iClutX = -1;
    public boolean clutXisValid(@Nonnull InputStream is) throws EOFException, IOException {
        return clutXisValid(IO.readUInt16LE(is));
    }
    public boolean clutXisValid(int iClutX) {
        if (iClutX < 0 || iClutX >= MAX_TIM_WORD_WIDTH)
            return false;
        _iClutX = iClutX;
        return true;
    }
    public int getClutX() {
        return _iClutX;
    }


    private int _iClutY = -1;
    public boolean clutYisValid(@Nonnull InputStream is) throws EOFException, IOException {
        return clutYisValid(IO.readUInt16LE(is));
    }
    public boolean clutYisValid(int iClutY) {
        if (iClutY < 0 || iClutY >= MAX_TIM_HEIGHT)
            return false;

        _iClutY = iClutY;
        return true;
    }
    public int getClutY() {
        return _iClutY;
    }


    private int _iClutPixelWidth = -1;
    public boolean clutPixelWidthIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return clutPixelWidthIsValid(IO.readUInt16LE(is));
    }
    public boolean clutPixelWidthIsValid(int iClutWidth) {
        if (iClutWidth <= 0 || iClutWidth > MAX_TIM_WORD_WIDTH)
            return false;

        _iClutPixelWidth = iClutWidth;
        return true;
    }
    public int getClutPixelWidth() {
        return _iClutPixelWidth;
    }


    private int _iClutPixelHeight = -1;
    public boolean clutPixelHeightIsValid(@Nonnull InputStream is) throws EOFException, IOException {
        return clutPixelHeightIsValid(IO.readUInt16LE(is));
    }
    public boolean clutPixelHeightIsValid(int iClutHeight) {
        if (iClutHeight <= 0 || iClutHeight > MAX_TIM_HEIGHT)
            return false;
        _iClutPixelHeight = iClutHeight;
        return true;
    }
    public int getClutPixelHeight() {
        return _iClutPixelHeight;
    }


    public boolean clutIsConsistent() {
        int iExpectedClutByteSize = _iClutPixelWidth * 2 * _iClutPixelHeight + CLUT.HEADER_SIZE;

        // We've give the Tim size some flexibility after the bug report,
        // but for now we'll keep the CLUT size rigid.
        if (iExpectedClutByteSize != _iClutByteSize)
            return false;

        // In theory 4bb CLUT width should always be a multiple of 16
        // and 8bpp CLUT should always be a multiple of 256
        // however, that is not always the case, so we'll allow for any
        // CLUT dimensions

        return true;
    }

}
