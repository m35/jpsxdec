/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.modules.strvideo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.SectorBasedDemuxedFrameWithNumberAndDims;

/** Converts video sectors to video frames. */
public class StrVideoSectorToDemuxedStrFrame implements IdentifiedSectorListener<ISelfDemuxingVideoSector> {

    @Nonnull
    private final SectorRange _sectorRange;
    @Nonnull
    private final SectorBasedDemuxedFrameWithNumberAndDims.Listener _listener;

    @CheckForNull
    private ISelfDemuxingVideoSector.IDemuxer _currentFrame;

    public StrVideoSectorToDemuxedStrFrame(@Nonnull SectorRange sectorRange,
                                           @Nonnull SectorBasedDemuxedFrameWithNumberAndDims.Listener listener)
    {
        _sectorRange = sectorRange;
        _listener = listener;
    }

    @Override
    public @Nonnull Class<ISelfDemuxingVideoSector> getListeningFor() {
        return ISelfDemuxingVideoSector.class;
    }

    @Override
    public void feedSector(@Nonnull ISelfDemuxingVideoSector vidSector,
                           @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        if (!_sectorRange.sectorIsInRange(vidSector.getSectorNumber()))
            return;

        if (_currentFrame != null && !_currentFrame.addSectorIfPartOfFrame(vidSector))
            endFrame(log);
        if (_currentFrame == null)
            _currentFrame = vidSector.createDemuxer(log);
        if (_currentFrame.isFrameComplete())
            endFrame(log);

        // TODO: Check for EOF?
        // JPSXDEC-7 and JPSXDEC-9
        // Some videos may only be broken by the "EOF Marker" in the Sub Header: Sub Mode
        // However splitting at EOF breaks Alice & UmJammerLammy, so this can't
        // be done universally.
        // Unfortunately we don't want to put the EOF check in the Sectors
        // because we want to watch for EOF in any sector along the way
        //if (isEof(idSector))
        //    _listener.endVideo();
    }

    private void endFrame(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_currentFrame == null)
            return;
        SectorBasedDemuxedFrameWithNumberAndDims frame = _currentFrame.finishFrame(log);
        if (frame != null)
            _listener.frameComplete(frame, log);
        _currentFrame = null;
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        endFrame(log);
        _listener.endOfSectors(log);
    }

}

