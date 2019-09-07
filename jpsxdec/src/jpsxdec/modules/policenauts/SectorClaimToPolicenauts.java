/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IOIterator;


public class SectorClaimToPolicenauts extends SectorClaimSystem.SectorClaimer  {

    public interface Listener {
        void videoStart(int iWidth, int iHeight, @Nonnull ILocalizedLogger log);
        void feedPacket(@Nonnull SPacketData packet, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        public void endOfSectors(@Nonnull ILocalizedLogger log);
    }


    private static final class KlbsSectorRange {
        /** Stream to read the packet data in this KLBS set of sectors.
         * Once all are read, the stream ends, but we want to stick around to 
         * collect any more sectors in this KLBS. */
        @CheckForNull
        private KlbsStreamReader _stream;
        private final int _iKlbsEndSectorInclusive;
        private boolean _blnAtEnd = false;

        public KlbsSectorRange(@Nonnull SectorPN_KLBS klbsSector, @Nonnull ILocalizedLogger log) {
            _stream = new KlbsStreamReader(klbsSector);
            _iKlbsEndSectorInclusive = klbsSector.getEndSectorInclusive();
            readIntoSector(klbsSector, SectorPN_KLBS.SIZEOF_KLBS_HEADER, log);
        }

        public @Nonnull SectorPolicenauts readSector(@Nonnull CdSector cdSector, @Nonnull ILocalizedLogger log) {
            if (_blnAtEnd)
                throw new AssertionError();
            boolean blnAtEnd = cdSector.getSectorIndexFromStart() >= _iKlbsEndSectorInclusive;
            SectorPolicenauts pnSector = new SectorPolicenauts(cdSector, blnAtEnd);
            readIntoSector(pnSector, 0, log);
            _blnAtEnd = blnAtEnd;
            return pnSector;
        }

        public void readIntoSector(@Nonnull SectorPolicenauts pnSector, int iSkip, @Nonnull ILocalizedLogger log) {
            if (_stream == null)
                return;
            if (_blnAtEnd)
                throw new AssertionError();

            try {
                List<SPacketData> finishedPackets = _stream.readSectorPackets(pnSector, iSkip);
                if (_stream.allRead())
                    _stream = null;
                pnSector.setPacketsEndingInThisSector(finishedPackets);
            } catch (BinaryDataNotRecognized ex) {
                log.log(Level.SEVERE, I.POLICENAUTS_DATA_CORRUPTION(), ex);
                _stream = null;
            }
        }
    }


    @CheckForNull
    private Listener _listener;
    @CheckForNull
    private KlbsSectorRange _currentKlbs;
    

    public void setListener(@CheckForNull Listener listener) {
        _listener = listener;
    }

    @Override
    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws SectorClaimSystem.ClaimerFailure
    {
        if (cs.isClaimed()) {
            checkCorruptionIfExistingKlbs(log);
            return;
        }

        SectorPN_VMNK vmnkSector = new SectorPN_VMNK(cs.getSector());
        if (vmnkSector.getProbability() == 100) {
            checkCorruptionIfExistingKlbs(log);
            cs.claim(vmnkSector);
            if (_listener != null)
                _listener.videoStart(vmnkSector.getWidth(), vmnkSector.getHeight(), log);
        } else {
            SectorPolicenauts pnSector = null;

            SectorPN_KLBS klbsSector = new SectorPN_KLBS(cs.getSector());
            if (klbsSector.getProbability() == 100) {
                checkCorruptionIfExistingKlbs(log);
                // new KLBS, set things up
                pnSector = klbsSector;
                _currentKlbs = new KlbsSectorRange(klbsSector, log);
                cs.claim(klbsSector);
            } else if (_currentKlbs != null) {
                pnSector = _currentKlbs.readSector(cs.getSector(), log);
                // all done?
                if (_currentKlbs._blnAtEnd)
                    _currentKlbs = null;
            }

            if (pnSector != null)
                cs.claim(pnSector);

            // notify listeners
            if (_listener != null && pnSector != null) {
                for (SPacketData sPacketData : pnSector) {

                    // Only send packets that are fully in the active sector range
                    // (in practice there should never be a packet crossing the border)
                    if (sectorIsInRange(sPacketData.getStartSector()) &&
                        sectorIsInRange(sPacketData.getEndSectorInclusive()))
                    {
                        try {
                            _listener.feedPacket(sPacketData, log);
                        } catch (LoggedFailure ex) {
                            throw new SectorClaimSystem.ClaimerFailure(ex);
                        }
                    }
                }
            }
        }
    }

    /** There should not already be an existing KLBS block we are tracking. */
    private void checkCorruptionIfExistingKlbs(@Nonnull ILocalizedLogger log) {
        if (_currentKlbs != null) {
            log.log(Level.WARNING, I.POLICENAUTS_DATA_CORRUPTION());
            _currentKlbs = null;
        }
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        if (_listener != null)
            _listener.endOfSectors(log);
    }

}
