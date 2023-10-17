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

package jpsxdec.formats;

import javax.annotation.Nonnull;

/**
 * Technically a superclass of {@link Rec601YCbCrImage} and {@link Pc601YCbCrImage},
 * but don't interchange them or bad colors will happen! This superclass is
 * for convenience since the only difference is the RGB color conversion.
 */
abstract class YCbCrImage {

    private final int _iLumaWidth;
    private final int _iLumaHeight;
    private final int _iChromaWidth;
    private final int _iChromaHeight;

    /** Holds luminance values. */
    @Nonnull
    private final byte[] _abY;
    /** Holds chroma blue values with 4:2:0 subsampling. */
    @Nonnull
    private final byte[] _abCb;
    /** Holds chroma red values with 4:2:0 subsampling. */
    @Nonnull
    private final byte[] _abCr;

    /**@param iLumaWidth   Width of image (in luma pixels)
     * @param iLumaHeight  Height of image (in luma pixels) */
    public YCbCrImage(int iLumaWidth, int iLumaHeight) {
        if (iLumaWidth < 2 || iLumaHeight < 2 ||
           (iLumaWidth % 2) != 0 || (iLumaHeight % 2) != 0)
        {
            throw new IllegalArgumentException("Dimensions must be even");
        }
        _iLumaWidth  = iLumaWidth;
        _iLumaHeight = iLumaHeight;
        _iChromaWidth = iLumaWidth / 2;
        _iChromaHeight = iLumaHeight / 2;
        _abY  = new byte[_iLumaWidth * _iLumaHeight];
        _abCb = new byte[_iChromaWidth * _iChromaHeight];
        _abCr = new byte[_abCb.length];
    }

    public int getWidth() {
        return _iLumaWidth;
    }

    public int getHeight() {
        return _iLumaHeight;
    }

    // expose the internal buffers
    public @Nonnull byte[] getYBuff() {
        return _abY;
    }
    public @Nonnull byte[] getCbBuff() {
        return _abCb;
    }
    public @Nonnull byte[] getCrBuff() {
        return _abCr;
    }

    /** Sets a luminance value.
     * @param iLumaX  X luma pixel to set.
     * @param iLumaY  Y luma pixel to set.
     */
    protected void setYDbl(int iLumaX, int iLumaY, double dblY) {
        _abY[iLumaX + iLumaY * _iLumaWidth] = roundClampByteCast(dblY);
    }
    protected int getY(int iLumaX, int iLumaY) {
        return _abY[iLumaX + iLumaY * _iLumaWidth] & 0xff;
    }

    /** Sets chroma blue value.
     * @param iChromaX  X chroma pixel (1/2 luma width)
     * @param iChromaY  Y chroma pixel (1/2 luma width)
     */
    protected void setCbDbl(int iChromaX, int iChromaY, double dblCb) {
        _abCb[iChromaX + iChromaY * _iChromaWidth] = roundClampByteCast(dblCb);
    }
    protected int getCb(int iChromaX, int iChromaY) {
        return _abCb[iChromaX + iChromaY * _iChromaWidth] & 0xff;
    }

    /** Sets chroma red value.
     * @param iChromaX  X chroma pixel (1/2 luma width)
     * @param iChromaY  Y chroma pixel (1/2 luma width)
     */
    protected void setCrDbl(int iChromaX, int iChromaY, double dblCr) {
        _abCr[iChromaX + iChromaY * _iChromaWidth] = roundClampByteCast(dblCr);
    }
    protected int getCr(int iChromaX, int iChromaY) {
        return _abCr[iChromaX + iChromaY * _iChromaWidth] & 0xff;
    }

    private static byte roundClampByteCast(double dbl) {
        long lng = Math.round(dbl);
        if (lng < 0)
            return (byte)0;
        else if (lng > 255)
            return (byte)255;
        else
            return (byte)(lng);
    }
}
