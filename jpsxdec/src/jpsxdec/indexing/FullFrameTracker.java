/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2017  Michael Sabin
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

package jpsxdec.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.indexing.psxvideofps.StrFrameRateCalc;
import jpsxdec.util.Fraction;

/** Aggregates information about all the frames in a video. */
public class FullFrameTracker {
    private static final Logger LOG = Logger.getLogger(FullFrameTracker.class.getName());

    private final int _iWidth, _iHeight;

    private final int _iStartSector;
    private int _iEndSector;
    
    @Nonnull
    private final StrFrameRateCalc _fpsCalc;
    private final int _iFrame1PresentationSector;

    /** Delegates some tracking to {@link MiniFrameTracker}. */
    @Nonnull
    private final MiniFrameTracker _miniTracker;

    public FullFrameTracker(int iWidth, int iHeight, @Nonnull FrameNumber frameNum, int iEndSector) {
        _miniTracker = new MiniFrameTracker(frameNum);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iStartSector = frameNum.getSector();
        _iEndSector = iEndSector;
        _iFrame1PresentationSector = iEndSector - _iStartSector;
        _fpsCalc = new StrFrameRateCalc(frameNum.getSector() - _iStartSector,
                                        iEndSector - _iStartSector);
    }

    public void next(@Nonnull FrameNumber frameNum, int iEndSector) {
        _miniTracker.next(frameNum);
        if (frameNum.getSector() < _iStartSector || iEndSector < _iEndSector)
            throw new IllegalArgumentException("Adding frame with strange sector range");
        _iEndSector = iEndSector;
        _fpsCalc.nextVideo(frameNum.getSector() - _iStartSector,
                           iEndSector - _iStartSector);
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public int getSectorLength() {
        return _iEndSector - _iStartSector + 1;
    }

    public int getFrame1PresentationSector() {
        return _iFrame1PresentationSector;
    }

    public @Nonnull FrameNumber getStartFrame() {
        return _miniTracker.getStartFrame();
    }

    public @Nonnull FrameNumber getEndFrame() {
        return _miniTracker.getEndFrame();
    }

    public @Nonnull FrameNumberFormat getFormat() {
        return _miniTracker.getFormat();
    }

    public int getFrameCount() {
        return _miniTracker.getFrameCount();
    }

    /** Watch out, once this is called, this object cannot accept any more frames. */
    public @Nonnull int[] getSectorsPerFrame() {
        Fraction sectorsPerFrame;
        int iSectors, iPerFrame;
        if ((sectorsPerFrame = _fpsCalc.getSectorsPerFrame()) != null) {
            iSectors  = (int) sectorsPerFrame.getNumerator();
            iPerFrame = (int) sectorsPerFrame.getDenominator();
        } else {
            iSectors  = getSectorLength();
            iPerFrame = _miniTracker.getFrameCount();
        }
        return new int[] { iSectors, iPerFrame };
    }

}

