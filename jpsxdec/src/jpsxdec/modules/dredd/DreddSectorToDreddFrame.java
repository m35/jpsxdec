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

package jpsxdec.modules.dredd;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorRange;

public class DreddSectorToDreddFrame implements IdentifiedSectorListener<IIdentifiedSector> {

    public interface Listener {
        void frameComplete(@Nonnull DemuxedDreddFrame frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void videoBreak(@Nonnull ILocalizedLogger log);
        void endOfSectors(@Nonnull ILocalizedLogger log);
    }


    @Nonnull
    private final SectorRange _sectorRange;
    @Nonnull
    private final Listener _listener;

    public DreddSectorToDreddFrame(@Nonnull SectorRange sectorRange, @Nonnull Listener listener) {
        _sectorRange = sectorRange;
        _listener = listener;
    }

    @Override
    public @Nonnull Class<IIdentifiedSector> getListeningFor() {
        return IIdentifiedSector.class;
    }

    @Override
    public void feedSector(@Nonnull IIdentifiedSector idSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
        SectorDreddVideo dreddSector;
        if (idSector instanceof SectorDreddVideo)
            dreddSector = (SectorDreddVideo) idSector;
        else
            dreddSector = null;

        if (dreddSector != null && dreddSector._dreddFrame != null && _sectorRange.sectorIsInRange(idSector.getSectorNumber())) {
            _listener.frameComplete(dreddSector._dreddFrame, log);
        }

        // after processing the current sector, always check the EOF flag
        // the only way to know a video ends is by the EOF marker
        CdSectorXaSubHeader sh = idSector.getCdSector().getSubHeader();
        if (sh != null &&
            sh.getSubMode().mask(CdSectorXaSubHeader.SubMode.MASK_END_OF_FILE) != 0)
        {
            _listener.videoBreak(log);
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        _listener.endOfSectors(log);
    }
}
