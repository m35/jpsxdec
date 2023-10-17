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

package jpsxdec.modules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.ac3.SectorClaimToSectorAc3Video;
import jpsxdec.modules.cdaudio.SectorClaimToSectorCdAudio;
import jpsxdec.modules.crusader.SectorClaimToSectorCrusader;
import jpsxdec.modules.dredd.SectorClaimToDreddFrame;
import jpsxdec.modules.eavideo.SectorClaimToEAVideo;
import jpsxdec.modules.iso9660.SectorClaimToSectorISO9660;
import jpsxdec.modules.ngauge.SectorClaimToNGaugeSector;
import jpsxdec.modules.policenauts.SectorClaimToPolicenauts;
import jpsxdec.modules.square.SectorClaimToFF7Sector;
import jpsxdec.modules.square.SectorClaimToSquareAudioSector;
import jpsxdec.modules.strvideo.SectorClaimToStrVideoSector;
import jpsxdec.modules.xa.SectorClaimToSectorXaAudio;
import jpsxdec.util.BufferedIOIterator;
import jpsxdec.util.IOIterator;

/** The final and universal way to identify and handle CD sectors.
 * <p>
 * Originally each sector could be uniquely identified individually by its contents.
 * Then came along Judge Dredd and Gran Turismo which required some contextual
 * awareness. After hacking those to work, then comes Policenatus that just
 * blew all assumptions out the window.
 * <p>
 * This new approach allows multiple "sector claimers" to watch sectors
 * as they are being read from the disc. Each claimer not only gets a chance to
 * examine the sector, but also peek ahead if necessary to decide whether
 * to "claim" the sector as its own.
 * <p>
 * The "claimers" are prompted in the order they are added to this system.
 * Once a sector is claimed, later claimers will know and can respond accordingly.
 */
public class SectorClaimSystem {

    public static @Nonnull SectorClaimSystem create(@Nonnull ICdSectorReader cd) {
        return create(cd, 0);
    }
    public static @Nonnull SectorClaimSystem create(@Nonnull ICdSectorReader cd,
                                                    int iStartSector)
    {
        return create(cd, iStartSector, cd.getSectorCount()-1);
    }
    public static @Nonnull SectorClaimSystem create(@Nonnull ICdSectorReader cd,
                                                    int iStartSector,
                                                    int iEndSectorInclusive)
    {
        SectorClaimSystem scs = new SectorClaimSystem(cd, iStartSector, iEndSectorInclusive);
        // XA, CD audio, and ISO sectors are part of the core CD specification, so should come first
        scs.addClaimer(new SectorClaimToSectorXaAudio());
        scs.addClaimer(new SectorClaimToSectorCdAudio());
        scs.addClaimer(new SectorClaimToSectorISO9660());

        scs.addClaimer(new SectorClaimToNGaugeSector());
        scs.addClaimer(new SectorClaimToFF7Sector()); // FF7 needs to be before other video sector types (see javadoc)
        scs.addClaimer(new SectorClaimToStrVideoSector());
        scs.addClaimer(new SectorClaimToSquareAudioSector());
        scs.addClaimer(new SectorClaimToSectorAc3Video());

        scs.addClaimer(new SectorClaimToSectorCrusader());
        scs.addClaimer(new SectorClaimToDreddFrame());
        scs.addClaimer(new SectorClaimToPolicenauts());
        scs.addClaimer(new SectorClaimToEAVideo());
        return scs;
    }

    /** On each {@link #next(jpsxdec.util.ILocalizedLogger)}, a sector claimer
     * will be fed the 'current' sector.
     * The claimer can peek ahead as much as it likes and claim those. */
    public interface SectorClaimer {

        /** Called once for every sector, regardless of whether future sectors were peeked. */
        void sectorRead(@Nonnull ClaimableSector cs,
                        @Nonnull IOIterator<ClaimableSector> peekIt,
                        @Nonnull ILocalizedLogger log)
                throws IOException;

        void endOfSectors(@Nonnull ILocalizedLogger log);
    }



    private static class InternalSector {
        @Nonnull
        public final CdSector sector;
        @CheckForNull
        public IIdentifiedSector claimer;
        public InternalSector(CdSector sector) {
            this.sector = sector;
        }
    }

    /** A sector in the process of being claimed. */
    public static class ClaimableSector {
        @Nonnull
        private final InternalSector _inner;
        private ClaimableSector(@Nonnull InternalSector inner) {
            _inner = inner;
        }
        public @Nonnull CdSector getSector() {
            return _inner.sector;
        }
        public @CheckForNull IIdentifiedSector getClaimer() {
            return _inner.claimer;
        }
        public boolean isClaimed() {
            return _inner.claimer != null;
        }
        public void claim(@Nonnull IIdentifiedSector claimer) {
            _inner.claimer = claimer;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(_inner.sector);
            if (_inner.claimer == null)
                sb.append(" unclaimed");
            else
                sb.append(_inner.claimer);
            return sb.toString();
        }
    }

    // =========================================================================

    @Nonnull
    private final ICdSectorReader _cd;
    @Nonnull
    private final ArrayList<SectorClaimInception> _iterators = new ArrayList<SectorClaimInception>();
    @Nonnull
    private final List<IdentifiedSectorListener<? extends IIdentifiedSector>> _idSectorListeners = new ArrayList<IdentifiedSectorListener<? extends IIdentifiedSector>>();


    @Nonnull
    private BufferedIOIterator<ClaimableSector> _outerMostIterator;
    @Nonnull // only non-null when being used
    private ILocalizedLogger _log;

    SectorClaimSystem(@Nonnull ICdSectorReader cd) {
        this(cd, 0, cd.getSectorCount() - 1);
    }

    private SectorClaimSystem(@Nonnull ICdSectorReader cd, int iStartSector,
                              int iEndSectorInclusive)
    {
        _cd = cd;
        CoreSectorIterator core = new CoreSectorIterator(cd, iStartSector, iEndSectorInclusive+1);
        _outerMostIterator = new BufferedIOIterator<ClaimableSector>(core);
    }

    void addClaimer(@Nonnull SectorClaimer claimer) {
        SectorClaimInception wrapping = new SectorClaimInception(_outerMostIterator, claimer);
        _outerMostIterator = new BufferedIOIterator<ClaimableSector>(wrapping);
        _iterators.add(wrapping);
    }

    public void addIdListener(@Nonnull IdentifiedSectorListener<? extends IIdentifiedSector> newListener) {
        for (IdentifiedSectorListener<? extends IIdentifiedSector> listener : _idSectorListeners) {
            if (listener == newListener)
                return; // already listening
        }
        _idSectorListeners.add(newListener);
    }

    public @CheckForNull <T extends IdentifiedSectorListener<? extends IIdentifiedSector>> T getIdListener(@Nonnull Class<T> clazz) {
        for (IdentifiedSectorListener<? extends IIdentifiedSector> listener : _idSectorListeners) {
            if (listener.getClass() == clazz) {
                T listenerCast = clazz.cast(listener);
                return listenerCast;
            }
        }
        return null;
    }

    public @Nonnull File getSourceCdFile() {
        return _cd.getSourceFile();
    }

    /** You can continue to call {@link #next(jpsxdec.util.ILocalizedLogger)} until this returns false. */
    public boolean hasNext() {
        return _outerMostIterator.hasNext();
    }

    public @Nonnull IIdentifiedSector next(@Nonnull ILocalizedLogger log)
            throws CdException.Read, LoggedFailure
    {
        try {
            _log = log;
            ClaimableSector next;
            try {
                next = _outerMostIterator.next();
            } catch (IOException ex) {
                if (ex instanceof CdException.Read)
                    throw (CdException.Read)ex;
                throw new CdException.Read(getSourceCdFile(), ex);
            }

            IIdentifiedSector idSector = next.getClaimer();
            if (idSector == null)
                idSector = new UnidentifiedSector(next.getSector());

            for (IdentifiedSectorListener<? extends IIdentifiedSector> listener : _idSectorListeners) {
                feedSectorToListener(listener, idSector, log);
            }

            return idSector;
        } finally {
            _log = null;
        }
    }

    /** Helper method to capture and maintain the generic {@link IIdentifiedSector} type
     *  between the listener and the sector type it expects. */
    private static <T extends IIdentifiedSector> void feedSectorToListener(@Nonnull IdentifiedSectorListener<T> listener,
                                                                           @Nonnull IIdentifiedSector idSector,
                                                                           @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        Class<T> listeningFor = listener.getListeningFor();
        if (listeningFor.isInstance(idSector)) {
            // feed the sector only if it is the type the listener expexts
            T idSectorCast = listeningFor.cast(idSector);
            listener.feedSector(idSectorCast, log);
        }
    }

    /** Tell everything to finish processing the sectors that have been read. */
    public void flush(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        for (SectorClaimInception claimer : _iterators) {
            claimer._claimer.endOfSectors(log);
        }

        for (IdentifiedSectorListener<? extends IIdentifiedSector> listener : _idSectorListeners) {
            listener.endOfFeedSectors(log);
        }
    }

    // =========================================================================

    /** Core iterator that reads directly from the CD. */
    private static class CoreSectorIterator implements IOIterator<ClaimableSector> {
        @Nonnull
        private final ICdSectorReader _cd;
        private int _iSector;
        private final int _iEndSector;

        public CoreSectorIterator(@Nonnull ICdSectorReader cd, int iStartSector, int iEndSector) {
            _cd = cd;
            _iSector = iStartSector;
            _iEndSector = iEndSector;
        }
        @Override
        public boolean hasNext() {
            return _iSector < _iEndSector;
        }
        @Override
        public @Nonnull ClaimableSector next() throws IOException {
            return new ClaimableSector(new InternalSector(_cd.getSector(_iSector++)));
        }
    }

    /** Recursively wraps iterators around each other,
     * each one feeding off the nested one, and each buffering as it goes. */
    private class SectorClaimInception implements IOIterator<ClaimableSector> {
        @Nonnull
        private final BufferedIOIterator<ClaimableSector> _nestedIter;
        @Nonnull
        private final SectorClaimer _claimer;

        public SectorClaimInception(@Nonnull BufferedIOIterator<ClaimableSector> nested,
                                    @Nonnull SectorClaimer claimer)
        {
            _nestedIter = nested;
            _claimer = claimer;
        }

        @Override
        public boolean hasNext() {
            return _nestedIter.hasNext();
        }
        @Override
        public @Nonnull ClaimableSector next() throws IOException {
            BufferedIOIterator<ClaimableSector> copy = _nestedIter.copy();
            ClaimableSector cs = copy.next();
            _claimer.sectorRead(cs, copy, _log);
            return _nestedIter.next();
        }
    }

}
