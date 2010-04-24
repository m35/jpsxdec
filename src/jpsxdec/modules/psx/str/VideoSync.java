/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules.psx.str;

import java.util.logging.Logger;
import jpsxdec.util.Fraction;

public class VideoSync {

    private static final Logger log = Logger.getLogger(VideoSync.class.getName());

    private final int _iFirstPresentationSector;
    
    private final int _iSectorsPerSecond;
    private final Fraction _sectorsPerFrame;
    
    private final Fraction _secondsPerFrame;
    
    public VideoSync(int iFirstPresentationSector,
                     int iSectorsPerSecond,
                     Fraction sectorsPerFrame)
    {
        _iFirstPresentationSector = iFirstPresentationSector;
        _iSectorsPerSecond = iSectorsPerSecond;
        _sectorsPerFrame = sectorsPerFrame;

        _secondsPerFrame = _sectorsPerFrame.divide(_iSectorsPerSecond);
    }

    public int getSectorsPerSecond() {
        return _iSectorsPerSecond;
    }

    public Fraction getSecondsPerFrame() {
        return _secondsPerFrame;
    }

    public Fraction getSectorsPerFrame() {
        return _sectorsPerFrame;
    }

    private static Fraction NegPoint5 = new Fraction(-5, 10);
    private static Fraction NegPoint8 = new Fraction(-8, 10);

    public int calculateFramesToCatchUp(int iFramePresentationSector, long lngFramesWritten) {

        Fraction presentationTime = new Fraction(iFramePresentationSector - _iFirstPresentationSector, _iSectorsPerSecond);
        Fraction movieTime = _secondsPerFrame.multiply(lngFramesWritten);
        Fraction timeDiff = presentationTime.subtract(movieTime);
        Fraction framesDiff = timeDiff.divide(_secondsPerFrame);

        int iFrameCatchupNeeded = 0;

        if (framesDiff.compareTo(NegPoint5) > 0) { // presentation time is equal, or ahead of movie time
            iFrameCatchupNeeded = (int)Math.round(framesDiff.asDouble());
        } else if (framesDiff.compareTo(NegPoint8) > 0) {
            // movie time is technically more than 1/2 a frame ahead
            // however, this is bound to happen with 1001/100 sectors/frame movies
            // when the frame count breaks 3000. So in that case, this provides
            // a bit of leeway to save the user lots of warnings that can't be helped
            log.warning(String.format("Frame is written %1.3f seconds ahead.", -timeDiff.asDouble()));
            iFrameCatchupNeeded = 0;
        } else { // movie time is definitely ahead of disc time
            double dblTimeDiff = timeDiff.asDouble();
            log.warning(String.format("Frame is written %1.3f seconds ahead.", -dblTimeDiff));
            // return the negative number
            iFrameCatchupNeeded = (int)Math.round(dblTimeDiff);
        }

        return iFrameCatchupNeeded;
    }

    public int getInitialVideo() {
        return 0;
    }

    public long getFpsNum() {
        return getSecondsPerFrame().getDenominator();
    }

    public long getFpsDenom() {
        return getSecondsPerFrame().getNumerator();
    }
}
