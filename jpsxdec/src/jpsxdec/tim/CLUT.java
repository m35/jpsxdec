/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2015  Michael Sabin
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
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/** The TIM Color Lookup Table (CLUT). */
class CLUT {

    /** Size of the CLUT header in bytes. */
    public final static int HEADER_SIZE = 12;

    /** Dimensions of the CLUT in pixels. */
    private final int _iClutWidth, _iClutHeight;
    /** X position of the CLUT in pixels.
        * Not sure how it is used in the PSX, but it is often 0. */
    private final int _iClutX;
    /** Y position of the CLUT in pixels.
        * Not sure how it is used in the PSX, but it is often 0. */
    private final int _iClutY;
    /** Tim ABGR1555. */
    @Nonnull
    final short[] _asiColorData;

    /** Read a CLUT from an InputStream. */
    public CLUT(@Nonnull InputStream is) throws IOException, NotThisTypeException {
        long lngLength = IO.readUInt32LE(is);
        if (lngLength == 0) throw new NotThisTypeException();
        _iClutX = IO.readUInt16LE(is);
        _iClutY = IO.readUInt16LE(is);
        _iClutWidth = IO.readUInt16LE(is);
        if (_iClutWidth == 0) throw new NotThisTypeException();
        _iClutHeight = IO.readUInt16LE(is);
        if (_iClutHeight == 0) throw new NotThisTypeException();

        if (lngLength != calculateLength())
            throw new NotThisTypeException();

        _asiColorData = new short[_iClutWidth * _iClutHeight];
        for (int i = 0; i < _asiColorData.length; i++)
            _asiColorData[i] = IO.readSInt16LE(is);
    }

    /** Create a CLUT based on a ready-made CLUT palette. */
    public CLUT(@Nonnull short[] asiPalette, int iX, int iY, int iPixelWidth) {
        if (iX < 0 || iY < 0)
            throw new IllegalArgumentException("Invalid CLUT X,Y (" + iX + ", " + iY + ")");
        if (iPixelWidth < 1)
            throw new IllegalArgumentException("Invalid CLUT width " + iPixelWidth);
        if (asiPalette.length % iPixelWidth != 0)
            throw new IllegalArgumentException("CLUT size "+asiPalette.length+
                                                " not divisible by CLUT width "+iPixelWidth);
        _asiColorData = asiPalette;
        _iClutX = iX;
        _iClutY = iY;
        _iClutWidth = iPixelWidth;
        _iClutHeight = asiPalette.length / _iClutWidth;
    }

    /** Write the CLUT to a stream. */
    public void write(@Nonnull OutputStream os) throws IOException {
        IO.writeInt32LE(os, calculateLength());
        IO.writeInt16LE(os, _iClutX);
        IO.writeInt16LE(os, _iClutY);
        IO.writeInt16LE(os, _iClutWidth);
        IO.writeInt16LE(os, _iClutHeight);
        for (short i : _asiColorData)
            IO.writeInt16LE(os, i);
    }

    /** Returns the length of the CLUT structure in bytes. */
    private long calculateLength() {
        return _iClutWidth * _iClutHeight * 2 + 12;
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

    /** Returns width of the CLUT in pixels.
        * This is useful to know for some strange Tims found in the wild. */
    public int getWidth() {
        return _iClutWidth;
    }

    public @Nonnull BufferedImage toBufferedImage() {
        BufferedImage bi = new BufferedImage(_iClutWidth, _iClutHeight, BufferedImage.TYPE_INT_ARGB);
        int[] aiBufferArgb = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < aiBufferArgb.length; i++) {
            aiBufferArgb[i] = Tim.color16toColor32(_asiColorData[i]);
        }
        return bi;
    }

    public String toString() {
        return String.format(
                "%dx%d xy(%d, %d) Len:%d",
                _iClutWidth,
                _iClutHeight,
                _iClutX,
                _iClutY,
                calculateLength());
    }

}
