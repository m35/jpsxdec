/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2017  Michael Sabin
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

package jpsxdec.sectors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.IO;
import jpsxdec.util.BinaryDataNotRecognized;

/** Contains the logic to identify{@link CdSector}s.
 * Some sector identification requires contextual information,
 * hence the need for an iterator. */
public abstract class IdentifiedSectorIterator {

    public static IdentifiedSectorIterator create(@Nonnull CdFileSectorReader cd) {
        return create(cd, 0);
    }
    public static IdentifiedSectorIterator create(@Nonnull CdFileSectorReader cd, 
                                                  int iStartSector)
    {
        return create(cd, iStartSector, cd.getLength()-1);
    }
    public static IdentifiedSectorIterator create(@Nonnull CdFileSectorReader cd,
                                                  int iStartSector,
                                                  int iEndSectorInclusive)
    {
        return new Dredd(cd, iStartSector, iEndSectorInclusive);
    }

    @Nonnull
    private final CdFileSectorReader _cd;

    protected IdentifiedSectorIterator(@Nonnull CdFileSectorReader cd) {
        _cd = cd;
    }

    public @Nonnull File getSourceCdFile() {
        return _cd.getSourceFile();
    }

    /** {@link #next()} must be called before calling this.
     * @return the last return value of {@link #next()}. */
    public abstract @CheckForNull IdentifiedSector current();
    /** {@link #next()} must be called before calling this.
     * @return the last read sector. */
    public abstract @Nonnull CdSector currentCd();
    public abstract boolean hasNext();
    /** Moves to the next sector and tries to identify it.
     * @return null if sector could not be identified. */
    public abstract @CheckForNull IdentifiedSector next() throws IOException;


    /** Wraps {@link BaseWithGT} and adds contextual Dredd identification. */
    private static class Dredd extends IdentifiedSectorIterator{

        private static class SectorPair {
            public final CdSector cdSector;
            public IdentifiedSector idSector;

            public SectorPair(CdSector cdSector, IdentifiedSector idSector) {
                this.cdSector = cdSector;
                this.idSector = idSector;
            }

            public SectorPair(IdentifiedSector idSector) {
                this.cdSector = idSector.getCdSector();
                this.idSector = idSector;
            }

            public SectorPair(CdSector cdSector) {
                this.cdSector = cdSector;
            }

            @Override
            public String toString() {
                if (idSector != null)
                    return idSector.toString();
                else
                    return cdSector.toString();
            }
        }

        @Nonnull
        private final BaseWithGT _it;
        @CheckForNull
        private SectorPair _current;
        private final LinkedList<SectorPair> _queue = new LinkedList<SectorPair>();
        @CheckForNull
        private SectorDreddVideo _remainingDredd;

        private Dredd(@Nonnull CdFileSectorReader cd, int iStartSector, int iEndSectorInclusive) {
            super(cd);
            _it = new BaseWithGT(cd, iStartSector, iEndSectorInclusive);
        }

        public @CheckForNull IdentifiedSector current() {
            if (_current == null)
                throw new IllegalStateException("next() should have been called first");
            return _current.idSector;
        }

        public @Nonnull CdSector currentCd() {
            if (_current == null)
                throw new IllegalStateException("next() should have been called first");
            return _current.cdSector;
        }
        
        public boolean hasNext() {
            return _it.hasNext() || !_queue.isEmpty();
        }

        public @CheckForNull IdentifiedSector next() throws IOException {
            if (!hasNext())
                throw new NoSuchElementException();

            if (!_queue.isEmpty()) { // start by emptying the queue first
                _current = _queue.poll();
            } else {
                if (_remainingDredd != null) { // continue a possible Dredd frame
                    _current = new SectorPair(_remainingDredd);
                    _remainingDredd = null;
                    if (!queueDredd())
                        clearOutDread();
                } else {
                    IdentifiedSector id = _it.next();
                    if (id != null) { // just return any identified sector
                        _current = new SectorPair(id);
                    } else {
                        CdSector cd = _it.currentCd();
                        SectorDreddVideo firstDredd = new SectorDreddVideo(cd);
                        if (firstDredd.getProbability() > 0 &&
                            firstDredd.getChunkNumber() == 0)
                        { // got a possible first sector
                            _current = new SectorPair(firstDredd);
                            if (!queueDredd())
                                clearOutDread();
                        } else { // just return unidentified sector
                            _current = new SectorPair(cd);
                        }
                    }
                }
            }

            return _current.idSector;
        }

        /** Replaces all queued identified Dredd sectors with unidentified sectors. */
        private void clearOutDread() {
            if (_current.idSector instanceof SectorDreddVideo)
                _current.idSector = null;
            for (SectorPair p : _queue) {
                if (p.idSector instanceof SectorDreddVideo)
                    p.idSector = null;
            }
        }

        /** Seeks ahead for Dredd sectors and adds them to the queue.
         * {@link #_current} should have the first possible Dredd sector
         * (chunk = #0).
         * 
         * @return if sectors for a full Dredd frame were identified.
         *         If false, caller should {@link #clearOutDread()}.
         */
        private boolean queueDredd() throws IOException {
            int iChunk = 1;
            for (int iSectors = 0; _it.hasNext() && iChunk < SectorDreddVideo.MAX_CHUNKS_PER_FRAME; iSectors++) {
                if (iSectors >= 15) // should have identified a full frame by now
                    break;
                _it.next();
                SectorPair next = new SectorPair(_it.currentCd(), _it.current());
                _queue.offer(next);
                if (next.idSector == null) { // skip identified sectors
                    SectorDreddVideo nextDreddVid = new SectorDreddVideo(next.cdSector);
                    if (nextDreddVid.getProbability() > 0) { // skip unidentified sectors that are definitely not Dredd
                        if (nextDreddVid.getChunkNumber() == iChunk) { // the chunk sequence continues
                            next.idSector = nextDreddVid;
                            iChunk++;
                        } else if (nextDreddVid.getChunkNumber() == 0) { // possible start of a new frame
                            // don't really like removing after adding here
                            _queue.removeLast();
                            _remainingDredd = nextDreddVid;
                            break;
                        } else { // chunk out of sequence
                            break;
                        }
                    }
                }
            }
            if (iChunk < SectorDreddVideo.MIN_CHUNKS_PER_FRAME-1)
                return false;

            // demux the frame
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); // TODO: optimization, reuse the buffer
            try {
                IO.writeIStoOS(_current.idSector.getIdentifiedUserDataStream(), baos);
                for (SectorPair pair : _queue) {
                    if (pair.idSector instanceof SectorDreddVideo)
                        IO.writeIStoOS(((SectorDreddVideo)pair.idSector).getIdentifiedUserDataStream(), baos);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Should not happen", ex);
            }
            // find and set the heights
            try {
                int iHeight = SectorDreddVideo.getHeight(baos.toByteArray()); // TODO: optimization, use exposed BAOS
                ((SectorDreddVideo)_current.idSector).setHeightChunks(iHeight, iChunk);
                for (SectorPair pair : _queue) {
                    if (pair.idSector instanceof SectorDreddVideo)
                        ((SectorDreddVideo)pair.idSector).setHeightChunks(iHeight, iChunk);
                }
                return true;
            } catch (BinaryDataNotRecognized ex) {
                return false;
            }
        }

    }

    /** Basic non-contextual sector identification, along with contextual
     * Gran Turismo identification. */
    private static class BaseWithGT extends IdentifiedSectorIterator {

        @Nonnull
        private final CdFileSectorReader _cd;
        private int _iCurrentSector;
        private final int _iEndSectorInclusive;

        @CheckForNull
        private IdentifiedSector _currentId;
        /** Will be null until {@link #next()} is called the first time. */
        @CheckForNull
        private CdSector _currentCd;

        /** Contextual Gran Turismo sector identification. */
        @CheckForNull
        private SectorGTVideo _lastGtChunk0;

        private BaseWithGT(@Nonnull CdFileSectorReader cd,
                            int iStartSector, int iEndSectorInclusive)
        {
            super(cd);
            _cd = cd;
            _iCurrentSector = iStartSector;
            _iEndSectorInclusive = iEndSectorInclusive;
        }

        public boolean hasNext() {
            return _iCurrentSector <= _iEndSectorInclusive;
        }

        public @CheckForNull IdentifiedSector next() throws IOException {
            if (!hasNext())
                throw new NoSuchElementException();

            _currentCd = _cd.getSector(_iCurrentSector);
            _iCurrentSector++;

            // sorted in order of likelyhood of encountering (my best guess)
            if ((_currentId = new SectorXaAudio(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorXaNull(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorStrVideo(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorISO9660DirectoryRecords(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorISO9660VolumePrimaryDescriptor(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorCdAudio(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorFF8.SectorFF8Video(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorFF8.SectorFF8Audio(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorFF9.SectorFF9Video(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorFF9.SectorFF9Audio(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorIkiVideo(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorChronoXAudio(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorChronoXVideo(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorChronoXVideoNull(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorAceCombat3Video(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorLainVideo(_currentCd)).getProbability() > 0) return _currentId;
            if ((_currentId = new SectorCrusader(_currentCd)).getProbability() > 0) return _currentId;

            // contextual GT
            SectorGTVideo gt2Vid = new SectorGTVideo(_currentCd, _lastGtChunk0);
            if (gt2Vid.getProbability() > 0) {
                if (gt2Vid.getChunkNumber() == 0)
                    _lastGtChunk0 = gt2Vid;
                _currentId = gt2Vid;
                return _currentId;
            }

            // FF7 has such a vague header, it can easily be falsely identified
            // when it should be one of the headers above
            if ((_currentId = new SectorFF7Video(_currentCd)).getProbability() > 0) return _currentId;

            // special handling for Alice
            SectorAliceNullVideo nullAlice = new SectorAliceNullVideo(_currentCd);
            if (nullAlice.getProbability() > 0) {
                _currentId = new SectorAliceVideo(_currentCd);
                if (_currentId.getProbability() == 0)
                    _currentId = nullAlice;
                return _currentId;
            }

            _currentId = null;
            return null;
        }

        public @CheckForNull IdentifiedSector current() {
            return _currentId;
        }

        public @Nonnull CdSector currentCd() {
            if (_currentCd == null)
                throw new IllegalStateException("next() should have been called first");
            return _currentCd;
        }

    }
}
