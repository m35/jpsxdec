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

package jpsxdec.modules.video.save;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.util.Fraction;

/** Used to ensure the writing of audio samples matches the timing of the
 * reading of audio samples. */
public class AudioSync {

    private final int _iFirstPresentationSector;

    @Nonnull
    private final DiscSpeed _discSpeed;
    private final int _iSampleFramesPerSecond;

    @Nonnull
    private final Fraction _sampleFramesPerSector;

    public AudioSync(int iFirstPresentationSector,
                     @Nonnull DiscSpeed discSpeed,
                     int iSampleFramesPerSecond)
    {
        _discSpeed = discSpeed;
        _iSampleFramesPerSecond = iSampleFramesPerSecond;
        // samples/sector = samples/second / sectors/second
        _sampleFramesPerSector = new Fraction(_iSampleFramesPerSecond, _discSpeed.getSectorsPerSecond());

        _iFirstPresentationSector = iFirstPresentationSector;
    }

    public int getSectorsPerSecond() {
        return _discSpeed.getSectorsPerSecond();
    }

    public int getSampleFramesPerSecond() {
        return _iSampleFramesPerSecond;
    }

    public @Nonnull Fraction getSampleFramesPerSector() {
        return _sampleFramesPerSector;
    }

    /** Returns the number of sample frames needed to catch up. */
    public long calculateAudioToCatchUp(@Nonnull Fraction audioPresentationSector,
                                        long lngSampleFramesWritten)
    {
        Fraction presentationTime = audioPresentationSector.subtract(_iFirstPresentationSector).divide(_discSpeed.getSectorsPerSecond());
        Fraction movieTime = new Fraction(lngSampleFramesWritten, _iSampleFramesPerSecond);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction sampleDiff = timeDiff.multiply(_iSampleFramesPerSecond);

        long lngSampleFrameDifference = Math.round(sampleDiff.asDouble());

        return lngSampleFrameDifference;
    }

}
