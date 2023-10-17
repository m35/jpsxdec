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

import java.util.ArrayList;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameBuilder;
import jpsxdec.util.DemuxedData;

/** @see NGaugeVideoInfo */
public class NGaugeSectorToFrame implements IdentifiedSectorListener<IdentifiedSector> {

    public interface Listener {
        void videoStart(@Nonnull NGaugeVideoInfo videoInfo);
        void frameComplete(@Nonnull DemuxedNGaugeFrame frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void videoBreak();
        void endOfSectors();
    }

    @Nonnull
    private final SectorRange _sectorRange;
    @Nonnull
    private final Listener _listener;

    @CheckForNull
    private NGaugeVideoInfo _videoInfo;
    @CheckForNull
    private SectorBasedFrameBuilder<SectorNGaugeVideo> _bldr;

    public NGaugeSectorToFrame(@Nonnull SectorRange sectorRange, @Nonnull Listener listener) {
        _sectorRange = sectorRange;
        _listener = listener;
    }

    @Override
    public @Nonnull Class<IdentifiedSector> getListeningFor() {
        return IdentifiedSector.class;
    }

    @Override
    public void feedSector(@Nonnull IdentifiedSector idSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (!_sectorRange.sectorIsInRange(idSector.getSectorNumber()))
            return;

        if (idSector instanceof SectorNGaugeVideoHeader) {
            // start of new video
            _videoInfo = ((SectorNGaugeVideoHeader)idSector).getVidInfo();
            _listener.videoStart(_videoInfo);
        } else if (idSector instanceof SectorNGaugeVideo && _videoInfo != null) {

            SectorNGaugeVideo vidSector = (SectorNGaugeVideo) idSector;
            int iFrameNumber = _videoInfo.calculateFrameNumber(vidSector.getSectorNumber());
            int iChunkNumber = _videoInfo.calculateChunkNumber(vidSector.getSectorNumber());

            if ((vidSector.isFrameFirstSector() && iChunkNumber != 0) ||
                (!vidSector.isFrameFirstSector() && iChunkNumber == 0))
            {
                // for some reason sector data does not align with the calculated chunk number??
                videoBreak(log); // kill the video in progress
                log.log(Level.SEVERE, I.N_GAUGE_DATA_CORRUPTION());
            } else {
                if (_bldr != null &&
                    !_bldr.addSectorIfPartOfFrame(vidSector, iChunkNumber, _videoInfo.iSectorsPerFrame,
                                                  vidSector.getSectorNumber(), iFrameNumber))
                {
                    endFrame(log);
                }
                if (_bldr == null) {
                    _bldr = new SectorBasedFrameBuilder<SectorNGaugeVideo>(vidSector, iChunkNumber,
                                                                           _videoInfo.iSectorsPerFrame,
                                                                           vidSector.getSectorNumber(),
                                                                           iFrameNumber, log);
                }
                if (_bldr.isFrameComplete())
                    endFrame(log);
            }
        }

    }

    private void endFrame(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_bldr != null) {
            ArrayList<SectorNGaugeVideo> sectors = _bldr.getNonNullChunks(log);
            DemuxedData<SectorNGaugeVideo> demux = new DemuxedData<SectorNGaugeVideo>(sectors);
            DemuxedNGaugeFrame frame = new DemuxedNGaugeFrame(_videoInfo, demux, _bldr.getHeaderFrameNumber());
            _bldr = null;
            _listener.frameComplete(frame, log);
        }
    }

    private void videoBreak(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        endFrame(log);
        _videoInfo = null;
        _listener.videoBreak();
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        endFrame(log);
        _videoInfo = null;
        _listener.endOfSectors();
    }

}
