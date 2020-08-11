/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2020  Michael Sabin
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.IOIterator;


public class SectorClaimToDreddFrame extends SectorClaimSystem.SectorClaimer {

    private static final Logger LOG = Logger.getLogger(SectorClaimToDreddFrame.class.getName());

    public interface Listener {
        void frameComplete(@Nonnull DemuxedDreddFrame frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void videoBreak(@Nonnull ILocalizedLogger log);
        void endOfSectors(@Nonnull ILocalizedLogger log);
    }

    private static class ClaimedSectorCollector {
        @Nonnull
        private final DemuxedDreddFrame _frame;
        /** Barf hash map memory waste. */
        private final Map<CdSector, SectorDreddVideo> _frameSectors = new HashMap<CdSector, SectorDreddVideo>(10);
        private final int _iMaxFrameSectorIndex;

        public ClaimedSectorCollector(@Nonnull DreddDemuxer.FrameSectors frameSectors) {
            _frame = frameSectors.frame;
            int iMaxFrameSectorIndex = frameSectors.sectors.get(0).getCdSector().getSectorIndexFromStart();
            // i = 1 cuz first sector already claimed
            for (int i = 1; i < frameSectors.sectors.size(); i++) {
                SectorDreddVideo sector = frameSectors.sectors.get(i);
                _frameSectors.put(sector.getCdSector(), sector);
                if (sector.getCdSector().getSectorIndexFromStart() > iMaxFrameSectorIndex)
                    iMaxFrameSectorIndex = sector.getCdSector().getSectorIndexFromStart();
            }
            _iMaxFrameSectorIndex = iMaxFrameSectorIndex;
        }

        public boolean allClaimed() {
            return _frameSectors.isEmpty();
        }

        public @CheckForNull SectorDreddVideo claimSector(CdSector cdSector) {
            if (cdSector.getSectorIndexFromStart() > _iMaxFrameSectorIndex)
                throw new RuntimeException("My Dredd logic bad, sector beyond claimed sectors");

            SectorDreddVideo dreddSector = _frameSectors.remove(cdSector);
            return dreddSector;
        }

        public @Nonnull DemuxedDreddFrame getFrame() {
            return _frame;
        }
    }

    @CheckForNull
    private ClaimedSectorCollector _claimedSectors;

    @CheckForNull
    private Listener _listener;

    @CheckForNull
    private DreddDemuxer _nextDemuxer;

    public SectorClaimToDreddFrame() {
    }
    public SectorClaimToDreddFrame(@Nonnull Listener listener) {
        _listener = listener;
    }
    public void setListener(@CheckForNull Listener listener) {
        _listener = listener;
    }

    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws IOException, SectorClaimSystem.ClaimerFailure
    {
        try {
            beforeEofCheck(cs, peekIt, log);
        } catch (LoggedFailure ex) {
            throw new SectorClaimSystem.ClaimerFailure(ex);
        }
        // after processing the current sector, always check the EOF flag
        // the only way to know a video ends is by the EOF marker
        CdSectorXaSubHeader sh = cs.getSector().getSubHeader();
        if (sh != null &&
            sh.getSubMode().mask(CdSectorXaSubHeader.SubMode.MASK_END_OF_FILE) != 0)
        {
            if (_listener != null)
                _listener.videoBreak(log);
        }
    }

    /** Guts moved to this method so the caller can always check the EOF flag
     * at the end of handling the current sector. */
    private void beforeEofCheck(@Nonnull SectorClaimSystem.ClaimableSector cs,
                                @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                                @Nonnull ILocalizedLogger log)
            throws IOException, LoggedFailure
    {
        // claimed? ignore
        if (cs.isClaimed())
            return;

        // a frame was previously found that used this sector? claim the sector
        if (_claimedSectors != null) {
            if (_claimedSectors.allClaimed()) // should not happen
                throw new RuntimeException("_claimedSectors != null but all sectors are claimed");

            SectorDreddVideo dreddSector = _claimedSectors.claimSector(cs.getSector());
            if (dreddSector != null) {
                cs.claim(dreddSector);
            }

            // all sectors claimed? finally send the frame
            if (_claimedSectors.allClaimed()) {
                if (_listener != null && sectorIsInRange(cs.getSector().getSectorIndexFromStart())) {
                    _listener.frameComplete(_claimedSectors.getFrame(), log);
                }
                _claimedSectors = null;
            }
            return;
        }

        // looking for a new frame

        DreddDemuxer demuxer;
        if (_nextDemuxer != null) {
            // already identified a first possible sector, continue from there
            demuxer = _nextDemuxer;
            _nextDemuxer = null;

            // should equal the first sector in the demuxer
            if (!demuxer.matchesFirstSector(cs.getSector()))
                throw new RuntimeException("CD sector does not match first Dredd sector");
        } else {
            demuxer = DreddDemuxer.first(cs.getSector());
            // not claimed & not first dredd sector? ignore
            if (demuxer == null) {
                return;
            }
        }

        for (int iSectors = 0;
             peekIt.hasNext() &&
             iSectors < 15; // if we don't have a completed frame within 15 sectors, just give up
             iSectors++)
        {
            SectorClaimSystem.ClaimableSector next = peekIt.next();
            if (next.isClaimed())
                continue; // sector is claimed, skip

            _nextDemuxer = DreddDemuxer.first(next.getSector());
            if (_nextDemuxer != null) {
                // hold onto this new demuxeer while we try to finish the existing frame
                break;
            }

            boolean blnTryToFinishTheFrame = demuxer.addSector(next.getSector());
            if (blnTryToFinishTheFrame)
                break;
        }

        DreddDemuxer.FrameSectors frameSectors = demuxer.tryToFinishFrame();
        if (frameSectors != null) {
            cs.claim(frameSectors.sectors.get(0)); // claim the first sector
            _claimedSectors = new ClaimedSectorCollector(frameSectors);
        }
        // wait until all the frame sectors have been read to send frame to listener
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        // the end of sectors should not have moved since the frame was built
        // so there should never be a need to flush an existing frame
        if (_claimedSectors != null)
            throw new RuntimeException("Created Dredd frame still exists at end of sectors");
        if (_listener != null)
            _listener.endOfSectors(log);
        
    }
}
