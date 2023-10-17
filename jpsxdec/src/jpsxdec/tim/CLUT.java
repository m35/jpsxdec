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
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.PsxRgb;
import jpsxdec.util.IO;

/** The TIM Color Lookup Table (CLUT). */
class CLUT {

    /**
     * Size of the CLUT header in bytes.
     */
    public final static int HEADER_SIZE = 12;

    /**
     * Dimensions of the CLUT in pixels.
     * CLUTs are always 16 bits-per-pixel, so the width in the header is the pixel width.
     */
    private final int _iClutPixelWidth, _iClutPixelHeight;

    /**
     * X,Y position of the CLUT in pixels.
     * This can be used to indicate where to copy the CLUT into the VRAM.
     */
    private final int _iClutX, _iClutY;

    /**
     * Tim 16 bit pixels ABGR1555.
     */
    @Nonnull
    final short[] _asiColorData;

    CLUT(@Nonnull short[] asiPalette, int iX, int iY, int iPixelWidth, int iPixelHeight) {
        // The caller should validate the parameters
        _asiColorData = asiPalette;
        _iClutX = iX;
        _iClutY = iY;
        _iClutPixelWidth = iPixelWidth;
        _iClutPixelHeight = iPixelHeight;
    }

    /** Write the CLUT to a stream. */
    public void write(@Nonnull OutputStream os) throws IOException {
        IO.writeInt32LE(os, calculateClutSizeInBytes());
        IO.writeInt16LE(os, _iClutX);
        IO.writeInt16LE(os, _iClutY);
        IO.writeInt16LE(os, _iClutPixelWidth);
        IO.writeInt16LE(os, _iClutPixelHeight);
        for (short i : _asiColorData)
            IO.writeInt16LE(os, i);
    }

    /**
     * Returns the length of the CLUT structure in bytes.
     */
    private int calculateClutSizeInBytes() {
        return _asiColorData.length * 2 + HEADER_SIZE;
    }

    /** Returns Tim ABGR1555 color value. */
    public short getColor(int iIndex) {
        return _asiColorData[iIndex];
    }

    /** Returns the number of colors in the CLUT. */
    public int getPaletteLength() {
        return _asiColorData.length;
    }

    public int getX() {
        return _iClutX;
    }

    public int getY() {
        return _iClutY;
    }

    public @Nonnull BufferedImage toBufferedImage() {
        BufferedImage bi = new BufferedImage(_iClutPixelWidth, _iClutPixelHeight, BufferedImage.TYPE_INT_ARGB);
        int[] aiBufferArgb = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < aiBufferArgb.length; i++) {
            aiBufferArgb[i] = PsxRgb.psxABGR1555toARGB8888(_asiColorData[i], Tim.SEMI_TRANSPARENT);
        }
        return bi;
    }

    @Override
    public String toString() {
        return String.format(
                "%dx%d xy(%d, %d) Size:%d",
                _iClutPixelWidth,
                _iClutPixelHeight,
                _iClutX,
                _iClutY,
                calculateClutSizeInBytes());
    }

}
