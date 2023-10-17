/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.ngauge;

import java.io.PrintStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.sectorbased.ISectorBasedDemuxedFrame;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.util.DemuxedData;

/** @see NGaugeVideoInfo */
public class DemuxedNGaugeFrame implements ISectorBasedDemuxedFrame {

    @Nonnull
    private final NGaugeVideoInfo _vidInfo;
    @Nonnull
    private final DemuxedData<SectorNGaugeVideo> _demux;
    private final int _iCalculatedFrameNumber;

    /** Secretly set after this object is created but before it is passed on
     * during the decoding process. */
    FrameNumber _frameNumber;

    public DemuxedNGaugeFrame(@Nonnull NGaugeVideoInfo vidInfo,
                              @Nonnull DemuxedData<SectorNGaugeVideo> demux,
                              int iCalculatedFrameNumber)
    {
        _vidInfo = vidInfo;
        _demux = demux;
        _iCalculatedFrameNumber = iCalculatedFrameNumber;
    }

    @Override
    public int getWidth() {
        return _vidInfo.iWidth;
    }

    @Override
    public int getHeight() {
        return _vidInfo.iHeight;
    }

    public int getCalculatedFrameNumber() {
        return _iCalculatedFrameNumber;
    }

    @Override
    public @Nonnull FrameNumber getFrame() {
        return _frameNumber;
    }

    @Override
    public int getStartSector() {
        return _demux.getStartSector();
    }

    @Override
    public int getEndSector() {
        return _demux.getEndSector();
    }

    @Override
    public int getPresentationSector() {
        return getEndSector();
    }

    @Override
    public int getDemuxSize() {
        return _demux.getDemuxSize();
    }

    @Override
    public @Nonnull byte[] copyDemuxData() {
        return _demux.copyDemuxData();
    }

    @Override
    public @CheckForNull IBitStreamUncompressor getCustomFrameMdecStream() {
        return null;
    }

    @Override
    public void printSectors(@Nonnull PrintStream ps) {
        for (SectorNGaugeVideo sdv : _demux) {
            ps.println(sdv);
        }
    }

    @Override
    public void writeToSectors(@Nonnull SectorBasedFrameAnalysis existingFrame,
                               @Nonnull BitStreamAnalysis newFrame,
                               @Nonnull DiscPatcher patcher,
                               @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        SectorBasedFrameReplace.writeToSectors(existingFrame, newFrame, patcher, log, _demux);
    }

}
