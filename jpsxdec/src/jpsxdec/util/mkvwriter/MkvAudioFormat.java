/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2026  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/**
 * Simple 16-bit PCM audio format. Only options are the sample rate, and if it's stereo.
 */
class MkvAudioFormat {

    private final boolean _blnStereo;
    private final int _iSampleFramesPerSecond;

    private final int _iAudioSampleFrameSizeInBytes;

    public MkvAudioFormat(boolean blnStereo, int iAudioSampleFramesPerSecond) {
        if (iAudioSampleFramesPerSecond < 1)
            throw new IllegalArgumentException("Invalid audio sample rate: " + iAudioSampleFramesPerSecond);
        _blnStereo = blnStereo;
        _iSampleFramesPerSecond = iAudioSampleFramesPerSecond;

        _iAudioSampleFrameSizeInBytes = getAudioChannels() * 2; // 16bit PCM audio
    }

    public int getAudioChannels() {
        return _blnStereo ? 2 : 1;
    }

    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    public @Nonnull Fraction audioBytesToSeconds(int iByteLength) {
        int iSampleFrameCount = audioBytesToSampleFrames(iByteLength);
        return new Fraction(iSampleFrameCount, _iSampleFramesPerSecond);
    }

    /**
     * @throws IllegalArgumentException if the byte length is not a multiple of the sample frame size.
     */
    public int audioBytesToSampleFrames(int iByteLength) {
        if (iByteLength % _iAudioSampleFrameSizeInBytes != 0) {
            throw new IllegalArgumentException(iByteLength + " is not a multiple of " +  _iAudioSampleFrameSizeInBytes);
        }

        int iSampleFrameCount = iByteLength / _iAudioSampleFrameSizeInBytes;
        return iSampleFrameCount;
    }

    public @Nonnull byte[] generateSilence(int iSampleFrameCount) {
        return new byte[iSampleFrameCount * _iAudioSampleFrameSizeInBytes];
    }

}
