/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.crusader;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.util.Fraction;

public class DemuxedCrusaderFrame implements IDemuxedFrame {

    private final int _iPresentationSector;
    @Nonnull
    private final CrusaderPacket.Video _packet;
    @CheckForNull
    private FrameNumber _frameNumber;

    public DemuxedCrusaderFrame(CrusaderPacket.Video packet,
                                int iPresentationSector)
    {
        _packet = packet;
        _iPresentationSector = iPresentationSector;
    }

    @Override
    public @CheckForNull IBitStreamUncompressor getCustomFrameMdecStream() {
        return null;
    }

    @Override
    public @Nonnull byte[] copyDemuxData() {
        return _packet.copyPayload();
    }

    @Override
    public int getDemuxSize() {
        return _packet.getPayloadSize();
    }

    @Override
    public int getStartSector() {
        return _packet.getStartSector();
    }

    @Override
    public int getEndSector() {
        return _packet.getEndSector();
    }

    @Override
    public @Nonnull FrameNumber getFrame() {
        if (_frameNumber == null)
            throw new IllegalStateException();
        return _frameNumber;
    }
    void setFrame(@Nonnull FrameNumber fn) {
        _frameNumber = fn;
    }

    public int getHeaderFrameNumber() {
        return _packet.getFrameNumber();
    }

    @Override
    public int getWidth() {
        return _packet.getWidth();
    }

    @Override
    public int getHeight() {
        return _packet.getHeight();
    }

    @Override
    public @Nonnull Fraction getPresentationSector() {
        return new Fraction(_iPresentationSector);
    }

    @Override
    public String toString() {
        return "Crusader "+getWidth()+"x"+getHeight()+" Frame "+_frameNumber+
               " Size "+getDemuxSize()+" PresSect "+_iPresentationSector;
    }
}
