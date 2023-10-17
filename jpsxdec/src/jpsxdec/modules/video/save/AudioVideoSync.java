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

/** Used to ensure the writing of both audio samples and video frames
 * matches the timing of the reading of each item. This also indicates
 * if the audio or video is behind the other, and by how much. */
public class AudioVideoSync extends VideoSync {

    private final @Nonnull AudioSync _audSync;
    private final @Nonnull Fraction _sampleFramesPerVideoFrame;
    private final int _iInitialFrameDelay;
    private final long _lngInitialSampleDelay;

    public AudioVideoSync(int iFirstVideoPresentationSector,
                          @Nonnull DiscSpeed discSpeed,
                          @Nonnull Fraction sectorsPerFrame,
                          int iFirstAudioPresentationSector,
                          int iSampleFramesPerSecond,
                          boolean blnPreciseAv)
    {
        super(iFirstVideoPresentationSector, discSpeed, sectorsPerFrame);
        _audSync = new AudioSync(iFirstAudioPresentationSector,
                                 discSpeed, iSampleFramesPerSecond);

        _sampleFramesPerVideoFrame = super.getSecondsPerFrame().multiply(_audSync.getSampleFramesPerSecond());

        if (blnPreciseAv) {

            int iPresentationSectorDiff = iFirstAudioPresentationSector - iFirstVideoPresentationSector;

            Fraction initialSampleDelay = new Fraction(_audSync.getSampleFramesPerSecond(), getSectorsPerSecond()).multiply(iPresentationSectorDiff);
            if (initialSampleDelay.compareTo(0) < 0) {
                _iInitialFrameDelay = -(int) Math.floor(initialSampleDelay.divide(_sampleFramesPerVideoFrame).asDouble());
                _lngInitialSampleDelay = Math.round(initialSampleDelay.add(_sampleFramesPerVideoFrame.multiply(_iInitialFrameDelay)).asDouble());
            } else {
                _lngInitialSampleDelay = Math.round(initialSampleDelay.asDouble());
                _iInitialFrameDelay = 0;
            }
        } else {
            _lngInitialSampleDelay = 0;
            _iInitialFrameDelay = 0;
        }

    }

    @Override
    public int calculateFramesToCatchUp(@Nonnull Fraction framePresentationSector,
                                        long lngFramesWritten)
    {
        return super.calculateFramesToCatchUp(framePresentationSector,
                                              lngFramesWritten - getInitialVideo());
    }

    /**@see AudioSync#calculateAudioToCatchUp(jpsxdec.util.Fraction, long)  */
    public long calculateAudioToCatchUp(@Nonnull Fraction audioPresentationSector,
                                        long lngSampleFramesWritten)
    {
        return _audSync.calculateAudioToCatchUp(audioPresentationSector,
                                                lngSampleFramesWritten - getInitialAudio());
    }

    public @Nonnull Fraction getSamplesFramesPerSector() {
        return _audSync.getSampleFramesPerSector();
    }

    public int getSamplesFramesPerSecond() {
        return _audSync.getSampleFramesPerSecond();
    }

    public long getInitialAudio() {
        return _lngInitialSampleDelay;
    }

    @Override
    public int getInitialVideo() {
        return _iInitialFrameDelay;
    }

}
