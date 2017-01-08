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

package jpsxdec.indexing;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.TaskCanceledException;

/** Individually iterates over {@link CdSector}s that cannot be identified
 * until it hits a sector that can be identified, then stops until
 * {@link #seekToNextUnidentified()} is called.
 * <p>
 * {@link #seekToNextUnidentified()} must be called first, then
 * {@link #nextUnidentified()} must continually be called until
 * it returns null, and repeat.
 * <p>
 * Subclass can implement
 * {@link #sectorRead(jpsxdec.cdreaders.CdSector, jpsxdec.sectors.IdentifiedSector)}
 * to be notified about every sector read. */
public abstract class UnidentifiedSectorIterator {
    @Nonnull
    private final IdentifiedSectorIterator _sectorIter;
    @CheckForNull
    private CdSector _nextUnidentified;
    @CheckForNull
    private TaskCanceledException _taskCanceled;

    public UnidentifiedSectorIterator(@Nonnull CdFileSectorReader cd) {
        _sectorIter = IdentifiedSectorIterator.create(cd);
    }

    abstract protected void sectorRead(@Nonnull CdSector cdSector,
                                       @CheckForNull IdentifiedSector idSector)
            throws TaskCanceledException;

    /** @throws IllegalStateException if not at the end of an unidentified sequence. */
    public boolean seekToNextUnidentified() throws IOException, TaskCanceledException {
        checkTaskCanceled();
        if (_nextUnidentified != null)
            throw new IllegalStateException();
        while (_sectorIter.hasNext()) {
            // try to maintain a consistent state in case
            // TaskCanceledException is thrown
            if (_sectorIter.next() == null)
                _nextUnidentified = _sectorIter.currentCd();
            sectorRead(_sectorIter.currentCd(), _sectorIter.current());
            if (_nextUnidentified != null)
                return true;
        }
        return false;
    }

    /** Gets the next {@link CdSector} in sequence that cannot be identified,
     * or null if at the end of a sequence. If {@link TaskCanceledException}
     * is thrown during read, it is stored and {@link #checkTaskCanceled()}
     * should be called as soon as possible. */
    public @CheckForNull CdSector nextUnidentified() throws IOException {
        if (_nextUnidentified == null) {
            return null;
        } else {
            CdSector next = _nextUnidentified;
            _nextUnidentified = null;
            if (_sectorIter.hasNext()) {
                if (_sectorIter.next() == null)
                    _nextUnidentified = _sectorIter.currentCd();
                try {
                    sectorRead(_sectorIter.currentCd(), _sectorIter.current());
                } catch (TaskCanceledException ex) {
                    _taskCanceled = ex;
                }
            }
            return next;
        }
    }

    public boolean atEndOfDisc() {
        return _nextUnidentified == null && !_sectorIter.hasNext();
    }

    public void checkTaskCanceled() throws TaskCanceledException {
        if (_taskCanceled != null)
            throw _taskCanceled;
    }
}

