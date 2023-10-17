/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.IOIterator;


public class SectorClaimToDreddFrame implements SectorClaimSystem.SectorClaimer {

    private static class ClaimedSectorCollector {
        private final Map<Integer, SectorDreddVideo> _frameSectors = new TreeMap<Integer, SectorDreddVideo>();
        private final int _iMaxFrameSectorIndex;

        public ClaimedSectorCollector(@Nonnull List<SectorDreddVideo> frameSectors) {
            int iMaxFrameSectorIndex = frameSectors.get(0).getCdSector().getSectorIndexFromStart();
            // i = 1 cuz first sector already claimed
            for (int i = 1; i < frameSectors.size(); i++) {
                SectorDreddVideo sector = frameSectors.get(i);
                _frameSectors.put(sector.getCdSector().getSectorIndexFromStart(), sector);
                if (sector.getCdSector().getSectorIndexFromStart() > iMaxFrameSectorIndex)
                    iMaxFrameSectorIndex = sector.getCdSector().getSectorIndexFromStart();
            }
            _iMaxFrameSectorIndex = iMaxFrameSectorIndex;
        }

        public boolean allClaimed() {
            return _frameSectors.isEmpty();
        }

        public @CheckForNull SectorDreddVideo claimSectorIfDredd(@Nonnull SectorClaimSystem.ClaimableSector cs) {
            if (allClaimed()) // should not happen
                throw new RuntimeException("_claimedSectors != null but all sectors are claimed");

            if (cs.getSector().getSectorIndexFromStart() > _iMaxFrameSectorIndex)
                throw new RuntimeException("My Dredd logic bad, sector beyond claimed sectors");

            SectorDreddVideo dreddSector = _frameSectors.remove(cs.getSector().getSectorIndexFromStart());
            if (dreddSector != null) {
                cs.claim(dreddSector);
            }
            return dreddSector;
        }
    }

    @CheckForNull
    private ClaimedSectorCollector _claimedSectors;

    @CheckForNull
    private DreddDemuxer _nextDemuxer;

    @Override
    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws IOException
    {
        SectorDreddVideo dreddSector = dreddSectorLookAheadCheck(cs, peekIt);
    }

    private @CheckForNull SectorDreddVideo dreddSectorLookAheadCheck(
                @Nonnull SectorClaimSystem.ClaimableSector cs,
                @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt)
            throws IOException
    {
        // claimed? ignore
        if (cs.isClaimed())
            return null;

        // a frame was previously found that used this sector? claim the sector
        if (_claimedSectors != null) {
            SectorDreddVideo dreddSector = _claimedSectors.claimSectorIfDredd(cs);
            if (_claimedSectors.allClaimed())
                _claimedSectors = null;
            return dreddSector;
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
                return null;
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
                // hold onto this new demuxer while we try to finish the existing frame
                break;
            }

            boolean blnTryToFinishTheFrame = demuxer.addSector(next.getSector());
            if (blnTryToFinishTheFrame)
                break;
        }

        List<SectorDreddVideo> frameSectors = demuxer.tryToFinishFrame();
        SectorDreddVideo dreddSector = null;
        if (frameSectors != null) {
            dreddSector = frameSectors.get(0);
            cs.claim(dreddSector); // claim the first sector
            _claimedSectors = new ClaimedSectorCollector(frameSectors);
        }
        return dreddSector;
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        // the end of sectors should not have moved since the frame was built
        // so there should never be a need to flush an existing frame
        if (_claimedSectors != null)
            throw new RuntimeException("Created Dredd frame still remains at end of sectors");
    }
}
