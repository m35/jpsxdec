/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

package jpsxdec.discitems.savers;

import jpsxdec.util.Fraction;

/** Used to ensure the writing of both audio samples and video frames
 * matches the timing of the reading of each item. This also indicates
 * if the audio or video is behind the other, and by how much. */
public class AudioVideoSync extends VideoSync {

    private final AudioSync _audSync;
    private final Fraction _samplesPerFrame;
    private final int _iInitialFrameDelay;
    private final long _lngInitialSampleDelay;

    public AudioVideoSync(int iFirstVideoPresentationSector,
                          int iSectorsPerSecond,
                          Fraction sectorsPerFrame,
                          int iFirstAudioPresentationSector,
                          int iSamplesPerSecond,
                          boolean blnPreciseAv)
    {
        super(iFirstVideoPresentationSector,
              iSectorsPerSecond, sectorsPerFrame);
        _audSync = new AudioSync(iFirstAudioPresentationSector,
                                 iSectorsPerSecond, iSamplesPerSecond);

        _samplesPerFrame = super.getSecondsPerFrame().multiply(_audSync.getSamplesPerSecond());

        if (blnPreciseAv) {

            int iPresentationSectorDiff = iFirstAudioPresentationSector - iFirstVideoPresentationSector;

            Fraction initialSampleDelay = new Fraction(_audSync.getSamplesPerSecond(), getSectorsPerSecond()).multiply(iPresentationSectorDiff);
            if (initialSampleDelay.compareTo(0) < 0) {
                _iInitialFrameDelay = -(int) Math.floor(initialSampleDelay.divide(_samplesPerFrame).asDouble());
                _lngInitialSampleDelay = Math.round(initialSampleDelay.add(_samplesPerFrame.multiply(_iInitialFrameDelay)).asDouble());
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
    public int calculateFramesToCatchUp(int iVideoPresentationSector,
                                        long lngFramesWritten)
    {
        return super.calculateFramesToCatchUp(iVideoPresentationSector,
                                              lngFramesWritten - getInitialVideo());
    }

    public long calculateAudioToCatchUp(int iAudioPresentationSector,
                                       long lngSamplesWritten)
    {
        return _audSync.calculateAudioToCatchUp(iAudioPresentationSector,
                                               lngSamplesWritten - getInitialAudio());
    }

    public Fraction getSamplesPerSector() {
        return _audSync.getSamplesPerSector();
    }

    public int getSamplesPerSecond() {
        return _audSync.getSamplesPerSecond();
    }

    public long getInitialAudio() {
        return _lngInitialSampleDelay;
    }

    @Override
    public int getInitialVideo() {
        return _iInitialFrameDelay;
    }

}
