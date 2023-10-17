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

package jpsxdec.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Read from a normal IO iterator of demux pieces as a stream. */
public class DemuxPullInputStream<T extends DemuxedData.Piece> extends InputStream {

    @Nonnull
    private BufferedIOIterator<T> _readIt;
    @CheckForNull
    private BufferedIOIterator<T> _markedIt;
    private int _iReadOffset = 0;
    private int _iMarkedOffset;
    private int _iMarkReadLimit;

    public DemuxPullInputStream(@Nonnull IOIterator<T> sourceIterator) {
        _readIt = new BufferedIOIterator<T>(sourceIterator);
    }

    // -------------------------------------------------------------------------

    private @CheckForNull T currentPiece() throws IOException {
        T c;
        // get current (peekPrevious) if available
        if (_readIt.hasPrevious())
            c = _readIt.peekPrevious();
        // or if it's the very start, get the immediate next
        else if (_readIt.hasNext())
            c = _readIt.next();
        // otherwise there is no more
        else
            c = null;
        return c;
    }

    private boolean moveToNextPiece() throws IOException {
        if (_readIt.hasNext()) {
            _readIt.next();
            return true;
        } else {
            return false;
        }
    }

    // -------------------------------------------------------------------------

    @Override
    public int read() throws IOException {
        if (_iMarkReadLimit < 0) {
            _markedIt = null;
        }

        T c = currentPiece();
        if (c == null)
            return -1;

        while (_iReadOffset >= c.getDemuxPieceSize()) {
            if (!moveToNextPiece())
                return -1;
            _iReadOffset = 0;
            c = currentPiece();
            assert c != null;
        }

        int b = c.getDemuxPieceByte(_iReadOffset);
        _iReadOffset++;
        if (_markedIt != null) {
            _iMarkReadLimit--;
        }
        return b;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readlimit) {
        _markedIt = _readIt.copy();
        _iMarkedOffset = _iReadOffset;
        _iMarkReadLimit = readlimit;
    }

    @Override
    public void reset() {
        if (_markedIt == null)
            return;
        _readIt = _markedIt;
        _markedIt = null;
        _iReadOffset = _iMarkedOffset;
    }

    @Override
    public int available() throws IOException {
        T c = currentPiece();
        if (c == null)
            return 0;
        int iAvailable = c.getDemuxPieceSize() - _iReadOffset;
        // it's confirmed that the source has something
        // collect the available data in any buffered entries
        BufferedIOIterator<T> it = _readIt.copy();
        while (it.isNextBuffered()) {
            c = it.next();
            iAvailable += c.getDemuxPieceSize();
        }
        return iAvailable;
    }

    @Override
    public void close() {
        // do something?
    }

    // -------------------------------------------------------------------------

    public @CheckForNull DemuxedData<T> getMarkToReadDemux() {
        if (_markedIt == null)
            throw new IllegalStateException();

        int iStartOfs = _iMarkedOffset;
        int iEndOfs = _iReadOffset;
        List<T> pieces = _markedIt.getElementSpanTo(_readIt);

        if (pieces.isEmpty()) {
            return null;
        } else {
            // trim off the first sector if the span starts at the end of it
            int iStartSector = 0;
            if (iStartOfs == pieces.get(0).getDemuxPieceSize()) {
                iStartOfs = 0;
                iStartSector = 1;
            }

            ArrayList<T> src = new ArrayList<T>(pieces.size() - iStartSector);
            for (int i = iStartSector; i < pieces.size(); i++) {
                src.add(pieces.get(i));
            }
            return new DemuxedData<T>(src, iStartOfs, iEndOfs);
        }
    }

}



