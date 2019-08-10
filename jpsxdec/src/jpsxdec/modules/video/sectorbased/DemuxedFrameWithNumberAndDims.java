/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import java.io.PrintStream;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.Fraction;

/** A demuxed frame with a frame number acquired from the sectors it consists of. */
public class DemuxedFrameWithNumberAndDims implements IDemuxedFrame {

    public interface Listener {
        void frameComplete(@Nonnull DemuxedFrameWithNumberAndDims frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endOfSectors(@Nonnull ILocalizedLogger log);
    }

    @Nonnull
    private final DemuxedData<SectorBasedFrameReplace.IReplaceableVideoSector> _demux;
    private final int _iWidth, _iHeight;
    private final int _iHeaderFrameNumber;

    @CheckForNull
    private FrameNumber _frameNumber;

    public DemuxedFrameWithNumberAndDims(int iWidth, int iHeight, int iHeaderFrameNumber,
                                         @Nonnull List<? extends SectorBasedFrameReplace.IReplaceableVideoSector> sectors)
    {
        _demux = new DemuxedData<SectorBasedFrameReplace.IReplaceableVideoSector>(sectors);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iHeaderFrameNumber = iHeaderFrameNumber;
    }

    public void setFrame(@Nonnull FrameNumber frameNumber) {
        _frameNumber = frameNumber;
    }
    public @Nonnull FrameNumber getFrame() {
        if (_frameNumber == null)
            throw new IllegalStateException();
        return _frameNumber;
    }

    public @CheckForNull MdecInputStream getCustomFrameMdecStream() {
        return null;
    }

    public int getWidth() { return _iWidth; }
    public int getHeight() { return _iHeight; }
    public int getStartSector() { return _demux.getStartSector(); }
    public int getEndSector() { return _demux.getEndSector(); }
    public @Nonnull Fraction getPresentationSector() { return new Fraction(getEndSector()); }
    public int getHeaderFrameNumber() { return _iHeaderFrameNumber; }

    public int getDemuxSize() { return _demux.getDemuxSize(); }
    public @Nonnull byte[] copyDemuxData() {
        return _demux.copyDemuxData();
    }

    public void printSectors(@Nonnull PrintStream ps) {
        for (SectorBasedFrameReplace.IReplaceableVideoSector vidSector : _demux) {
            ps.println(vidSector);
        }
    }

    public void writeToSectors(@Nonnull byte[] abNewDemux,
                               int iNewUsedSize, int iNewMdecCodeCount,
                               @Nonnull CdFileSectorReader cd,
                               @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        SectorBasedFrameReplace.writeToSectors(abNewDemux, iNewUsedSize, iNewMdecCodeCount, cd, log,
                                               _demux);
    }

    @Override
    public String toString() {
        return String.format("Frame %d sectors %d-%d %dx%d chunks:%d bytes:%d",
                getHeaderFrameNumber(),
                getStartSector(), getEndSector(),
                getWidth(), getHeight(), _demux.getPieceCount(),
               _demux.getDemuxSize());
    }

    // internal debugging
    public int getChunkCount() {
        return _demux.getPieceCount();
    }

}
