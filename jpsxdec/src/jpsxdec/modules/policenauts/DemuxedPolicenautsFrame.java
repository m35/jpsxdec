/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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

package jpsxdec.modules.policenauts;

import java.io.PrintStream;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.Fraction;


public class DemuxedPolicenautsFrame implements IDemuxedFrame {
    private final int _iWidth, _iHeight;
    private final SPacketData _data;
    @Nonnull
    private final FrameNumber _frameNumber;
    @Nonnull
    private final Fraction _presentationSector;

    public DemuxedPolicenautsFrame(int iWidth, int iHeight,
                                   @Nonnull SPacketData data,
                                   @Nonnull FrameNumber frameNumber,
                                   @Nonnull Fraction presentationSector)
    {
        _iWidth = iWidth;
        _iHeight = iHeight;
        _data = data;
        _frameNumber = frameNumber;
        _presentationSector = presentationSector;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public @Nonnull FrameNumber getFrame() {
        return _frameNumber;
    }

    public int getStartSector() {
        return _data.getStartSector();
    }

    public int getEndSector() {
        return _data.getEndSectorInclusive();
    }

    public @Nonnull Fraction getPresentationSector() {
        return _presentationSector;
    }

    public @CheckForNull MdecInputStream getCustomFrameMdecStream() {
        return null;
    }

    public int getDemuxSize() {
        return _data.getData().length;
    }

    public @Nonnull byte[] copyDemuxData() {
        return Arrays.copyOfRange(_data.getData(), 0, getDemuxSize());
    }

    public void printSectors(PrintStream ps) {
        // TODO?
    }

    public void writeToSectors(byte[] abNewDemux, int iNewUsedSize, int iNewMdecCodeCount, CdFileSectorReader cd, ILocalizedLogger log) throws LoggedFailure {
        throw new UnsupportedOperationException("Replacing Policenauts frames is not supported");
    }
}
