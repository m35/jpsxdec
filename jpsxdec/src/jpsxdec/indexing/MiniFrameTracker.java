/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2016  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.FrameNumberFormat;

/** Aggregates a minimal amount of information about all the frames in a video. */
public class MiniFrameTracker {
    
    @Nonnull
    private final FrameNumber _startFrame;
    @Nonnull
    private FrameNumber _endFrame;

    private int _iFrameCount;

    private final FrameNumberFormat.Builder _frameNumFormatBldr = new FrameNumberFormat.Builder();

    public MiniFrameTracker(@Nonnull FrameNumber frameNum) {
        _startFrame = _endFrame = frameNum;
        _frameNumFormatBldr.addFrame(frameNum);
        _iFrameCount = 1;
    }

    public void next(@Nonnull FrameNumber frameNum) {
        _endFrame = frameNum;
        _frameNumFormatBldr.addFrame(frameNum);
        _iFrameCount++;
    }

    public @Nonnull FrameNumber getStartFrame() {
        return _startFrame;
    }

    public @Nonnull FrameNumber getEndFrame() {
        return _endFrame;
    }

    public @Nonnull FrameNumberFormat getFormat() {
        return _frameNumFormatBldr.getFormat();
    }

    public int getFrameCount() {
        return _iFrameCount;
    }

}
