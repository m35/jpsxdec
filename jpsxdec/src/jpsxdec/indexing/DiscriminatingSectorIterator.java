/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

/** Iterates over either the identified or unidentified sectors of a CD.  */
public class DiscriminatingSectorIterator {

    public static interface SectorReadListener {
        public void sectorRead(@Nonnull CdSector sect);
    }

    @Nonnull
    private final CdFileSectorReader _cd;

    private int _iNextSector;
    /** Will be either {@link IdentifiedSector} or
     * {@link CdSector} if sector cannot be identified. */
    @CheckForNull
    private Object _nextSector;

    /** Optional listener that is notified only once for each sector read. */
    @CheckForNull
    private SectorReadListener _listener;

    public DiscriminatingSectorIterator(@Nonnull CdFileSectorReader cd) {
        this(cd, 0);
    }

    public DiscriminatingSectorIterator(@Nonnull CdFileSectorReader cd, int iSector) {
        _cd = cd;
        _iNextSector = iSector;
    }

    public void setListener(@Nonnull SectorReadListener listener) {
        _listener = listener;
    }

    // ..........................................................

    public boolean hasNextUnidentified() throws IOException {
        ensureNextIsBuffered();
        return _nextSector instanceof CdSector;
    }
    public boolean hasNextIdentified() throws IOException {
        ensureNextIsBuffered();
        return _nextSector instanceof IdentifiedSector;
    }

    // ..........................................................

    public @Nonnull CdSector nextUnidentified() throws IOException {
        if (!hasNextUnidentified())
            throw new RuntimeException();

        // hasNextUnidentified() confirms _nextSector != null
        CdSector cdSector = (CdSector) _nextSector;
        _nextSector = null;
        return cdSector;
    }
    public @Nonnull IdentifiedSector nextIdentified() throws IOException {
        if (!hasNextIdentified())
            throw new RuntimeException();

        // hasNextIdentified() confirms _nextSector != null
        IdentifiedSector identifiedSector = (IdentifiedSector) _nextSector;
        _nextSector = null;
        return identifiedSector;
    }

    // ..........................................................

    private void ensureNextIsBuffered() throws IOException {
        if (_nextSector == null && !isEndOfDisc()) {

            CdSector cdSector = _cd.getSector(_iNextSector);
            _nextSector = cdSector;

            if (_listener != null)
                _listener.sectorRead(cdSector);

            IdentifiedSector idSector = IdentifiedSector.identifySector(cdSector);

            if (idSector != null) {
                _nextSector = idSector;
            }
            _iNextSector++;
        }
    }

    // ..........................................................

    public boolean isEndOfDisc() {
        return _iNextSector >= _cd.getLength();
    }

}



