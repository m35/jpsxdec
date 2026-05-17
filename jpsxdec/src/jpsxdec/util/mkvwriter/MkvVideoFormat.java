/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2024-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;

import java.awt.image.BufferedImage;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/**
 * Gives the {@link BasicMkvWriter} and {@link BasicVideoAudioMuxer} the information
 * they need to set up and verify frame inputs.
 */
class MkvVideoFormat {

    private static class ConstantFps {
        @Nonnull
        private final Fraction framesPerSecond;
        @Nonnull
        private final Fraction secondsPerFrame;
        private final long lngNanosecondsPerFrame; // Will have error

        public ConstantFps(@Nonnull Fraction framesPerSecond) {
            this.framesPerSecond = framesPerSecond;
            secondsPerFrame = framesPerSecond.reciprocal();
            lngNanosecondsPerFrame = Fraction.divide(MkvTimestampScale.NANOSECONDS_PER_SECOND, framesPerSecond).asRoundedLong();
        }

    }

    private final int _iWidth;
    private final int _iHeight;

    @CheckForNull
    private final ConstantFps _constantFps;

    @Nonnull
    private final String _sCodecId;
    @CheckForNull
    private final String _sUncompressedFourCC;
    private final boolean _blnHasFullRangeYuvColor;
    /** These bytes can be used by a codec for whatever it wants.
     * (for the codecs implemented here, this will always be a {@code BITMAPINFOHEADER}). */
    @CheckForNull
    private final byte[] _abPrivateBytes;



    public MkvVideoFormat(int iWidth, int iHeight,
                          @CheckForNull Fraction constantFrameRate,
                          @Nonnull String sCodecId,
                          @CheckForNull String sUncompressedFourCC,
                          boolean blnHasFullRangeYuvColor,
                          @CheckForNull byte[] abPrivateBytes)
    {
        _iWidth = iWidth;
        _iHeight = iHeight;
        if (constantFrameRate == null) {
            _constantFps = null;
        } else {
            _constantFps = new ConstantFps(constantFrameRate);
        }
        _sCodecId = sCodecId;
        _sUncompressedFourCC = sUncompressedFourCC;
        _blnHasFullRangeYuvColor = blnHasFullRangeYuvColor;
        _abPrivateBytes = abPrivateBytes;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public void assertDimensionsMatch(@Nonnull BufferedImage bi) {
        assertDimensionsMatch(bi.getWidth(), bi.getHeight());
    }
    public void assertDimensionsMatch(int iWidth, int iHeight) {
        if (_iWidth != iWidth || _iHeight != iHeight)
            throw new IllegalArgumentException(String.format(
                    "The given dimensions %dx%d do not match the video dimensions %dx%d",
                    iWidth, iHeight, _iWidth, _iHeight));
    }

    public boolean hasConstantFrameRate() {
        return _constantFps != null;
    }

    public @Nonnull Fraction getConstantSecondsPerFrame() {
        if (_constantFps == null)
            throw new UnsupportedOperationException("Variable frame rate videos do not have a constant frame rate");
        return _constantFps.secondsPerFrame;
    }

    /**
     * DefaultDuration
     * "Number of nanoseconds (not scaled via TimestampScale) per frame"
     */
    public long getDefaultDuration() {
        if (_constantFps == null)
            throw new UnsupportedOperationException("Variable frame rate videos do not have a set nanosecond per frame");
        return _constantFps.lngNanosecondsPerFrame;
    }

    /**
     * Returns true if the video has variable frame rate, or the provided time is an
     * exact multiple of the constant frame rate.
     * Otherwise (it has a CFR and is not a multiple of it) false.
     */
    public boolean isExactIfConstantFps(@Nonnull Fraction second) {
        if (_constantFps == null)
            return true;
        Fraction frameNumber = _constantFps.framesPerSecond.multiply(second);
        return frameNumber.isWholeNumber();
    }

    /**
     * "An ID corresponding to the codec, see Matroska codec RFC for more info."
     */
    public @Nonnull String getCodecId() {
        return _sCodecId;
    }

    public @CheckForNull String getUncompressedFourCC() {
        return _sUncompressedFourCC;
    }

    public boolean hasFullRangeYuvColor() {
        return _blnHasFullRangeYuvColor;
    }

    /**
     * "Private data only known to the codec."
     */
    public @CheckForNull byte[] getPrivateBytes() {
        return _abPrivateBytes;
    }
}
