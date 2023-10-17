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

/** Used to ensure the writing of video frames matches the timing of the
 * reading of video frames.
 * All presentation sectors must be relative to the initial presentation sector
 * given in the constructor. */
public class VideoSync {

    /** Assuming frames are displayed after all parts are read from the disc,
     *  this is essentially the last sector of the first frame. */
    private final int _iFirstPresentationSector;

    @Nonnull
    private final DiscSpeed _discSpeed;
    @Nonnull
    private final Fraction _sectorsPerFrame;

    /** Precalculated from {@link #_discSpeed} and {@link #_sectorsPerFrame}
     * for quick reference. */
    @Nonnull
    private final Fraction _secondsPerFrame;

    public VideoSync(int iFirstPresentationSector,
                     @Nonnull DiscSpeed discSpeed,
                     @Nonnull Fraction sectorsPerFrame)
    {
        _iFirstPresentationSector = iFirstPresentationSector;
        _discSpeed = discSpeed;
        _sectorsPerFrame = sectorsPerFrame;

        _secondsPerFrame = _sectorsPerFrame.divide(_discSpeed.getSectorsPerSecond());
    }

    public int getSectorsPerSecond() {
        return _discSpeed.getSectorsPerSecond();
    }

    public @Nonnull Fraction getSecondsPerFrame() {
        return _secondsPerFrame;
    }

    public @Nonnull Fraction getSectorsPerFrame() {
        return _sectorsPerFrame;
    }

    private final static Fraction NegPoint5 = new Fraction(-5, 10);
    private final static Fraction NegPoint8 = new Fraction(-8, 10);

    public @Nonnull Fraction calcPresentationTime(@Nonnull Fraction framePresentationSector) {
        return framePresentationSector.subtract(_iFirstPresentationSector).divide(_discSpeed.getSectorsPerSecond());
    }

    /** Returns if the frame's presentation time is behind (negative value) or ahead (positive value) of the movie time.
     * The FPS is known, so the number of frames written tells us the movie time.
     * We compare that to the presentation time of the frame that is about to be written. */
    public int calculateFramesToCatchUp(@Nonnull Fraction framePresentationSector, long lngFramesWritten) {

        Fraction presentationTime = calcPresentationTime(framePresentationSector);
        Fraction movieTime = _secondsPerFrame.multiply(lngFramesWritten);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction framesDiff = timeDiff.divide(_secondsPerFrame);

        int iFrameCatchupNeeded;

        // [0.5, infinity) -> write frames to catch up
        // (-0.5, 0.5) -> Movie time == presentation time
        // (-infinity, -0.5] -> skip frames?

        if (framesDiff.compareTo(NegPoint5) > 0) { // movie time is (more or less) equal to, or behind presentation time
            // return 0, or positive number of frames to have movie time catchup to presentation time
            iFrameCatchupNeeded = (int)Math.round(framesDiff.asDouble());
        } else {
            // movie time is more than 1/2 a frame ahead
            if (framesDiff.compareTo(NegPoint8) > 0) {
                // movie time is technically more than 1/2 a frame ahead
                // however, this is bound to happen with 1001/100 sectors/frame movies
                // when the frame count breaks 3000. So in that case, this provides
                // a bit of leeway to save the user lots of warnings that can't be helped
                iFrameCatchupNeeded = 0;
            } else { // movie time is definitely ahead of disc time
                // return the negative number
                iFrameCatchupNeeded = (int)Math.round(framesDiff.asDouble());
            }
        }

        return iFrameCatchupNeeded;
    }

    /** Returns the number of initial frames that need to be written in order
     * to start the audio and video in sync. For {@link VideoSync} it is always 0,
     * but is overridden by {@link AudioVideoSync} for when there is audio. */
    public int getInitialVideo() {
        return 0;
    }

    public long getFpsNum() {
        // use seconds/frame, but flip the fraction
        return getSecondsPerFrame().getDenominator();
    }

    public long getFpsDenom() {
        // use seconds/frame, but flip the fraction
        return getSecondsPerFrame().getNumerator();
    }
}
