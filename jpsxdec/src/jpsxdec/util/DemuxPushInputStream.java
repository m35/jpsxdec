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
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A stream built by pushing pieces of data into it.
 *
 * The end of once piece = the start of the next.
 * It is important that once hitting the end of a sector, to stay there
 * and not pull the extra sector in case there are no more (leading to EOF).
 * When requesting the demux span, it is also important to trim off
 * the start in case it is the end of a piece (which is the same as the
 * start of the next one).
 *
 * A plain {@link IOException} should never be thrown since all the data is already
 * available.
 */
public class DemuxPushInputStream<T extends DemuxedData.Piece> extends InputStream {

    private static final Logger LOG = Logger.getLogger(DemuxPushInputStream.class.getName());

    /** Thrown when the stream is still open but the available data has
     * been exhausted. */
    public static class NeedsMoreData extends IOException {

    }


    private final BufferedPushIterator<T> _queue = new BufferedPushIterator<T>();
    @Nonnull
    private CopyablePieceSequenceStream<T> _readStream;
    @CheckForNull
    private CopyablePieceSequenceStream<T> _markedStream;

    private int _iMarkReadLimit;

    private boolean _blnClosed = false;

    public DemuxPushInputStream(@Nonnull T firstPiece) {
        _queue.add(firstPiece);
        _readStream = new CopyablePieceSequenceStream<T>(_queue.iterator());
    }

    /** @throws IllegalStateException if the stream is closed. */
    public void addPiece(@Nonnull T piece) throws IllegalStateException {
        if (_blnClosed)
            throw new IllegalStateException("Stream is closed.");
        _queue.add(piece);
        _readStream.addAvailable(piece.getDemuxPieceSize());
        if (_markedStream != null)
            _markedStream.addAvailable(piece.getDemuxPieceSize());
    }

    // -------------------------------------------------------------------------
    // InputStream functions

    @Override
    public int read() throws NeedsMoreData {
        if (_iMarkReadLimit < 0) {
            _markedStream = null;
        }

        int i = _readStream.read();
        if (i < 0) {
            if (_blnClosed)
                return -1;
            else
                throw new NeedsMoreData();
        }

        if (_markedStream != null)
            _iMarkReadLimit--;

        return i;
    }

    @Override
    public long skip(long lngBytesToSkip) throws NeedsMoreData {
        if (_iMarkReadLimit < lngBytesToSkip) {
            _markedStream = null;
        }

        long lngBytesSkipped = _readStream.skip(lngBytesToSkip);
        if (lngBytesSkipped < lngBytesToSkip) {
            if (_blnClosed)
                return lngBytesSkipped;
            else
                throw new NeedsMoreData();
        }

        if (_markedStream != null)
            _iMarkReadLimit -= lngBytesSkipped;

        return lngBytesSkipped == 0 ? -1 : lngBytesSkipped;
    }

    @Override
    public int read(byte[] b) throws NeedsMoreData {
        try {
            return super.read(b);
        } catch (NeedsMoreData ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws NeedsMoreData {
        try {
            return super.read(b, off, len);
        } catch (NeedsMoreData ex) {
            throw ex;
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readlimit) {
        _markedStream = _readStream.copy();
        _iMarkReadLimit = readlimit;
        int iAvailable = available();
        if (_blnClosed && iAvailable < readlimit) {
            // Logging this is WAY too loud
            // Maybe there's a smarter way to check for this kind of situation
            // but for now it'll be disabled
            //LOG.log(Level.WARNING, "Marking {0,number,#} more than is available {1,number,#}",
            //                       new Object[] {readlimit, iAvailable});
        }
    }

    @Override
    public void reset() {
        if (_markedStream == null)
            return;
        _readStream = _markedStream;
        _markedStream = null;
        _iMarkReadLimit = 0;
    }

    @Override
    public int available() {
        return _readStream.available();
    }

    /** No more data can be added, and any reads beyond the data available
     * will return end-of-stream. */
    @Override
    public void close() {
        _blnClosed = true;
    }

    // -------------------------------------------------------------------------
    // Non InputStream functions

    public boolean isEof() {
        return _readStream.isEof();
    }

    public @Nonnull T getCurrentPiece() {
        return _readStream.getCurrentPiece();
    }

    public int getOffsetInCurrentPiece() {
        return _readStream.getCurrentPieceOffset();
    }

    /** @throws IllegalStateException if there isn't an existing mark. */
    public @Nonnull DemuxedData<T> getMarkToReadDemux() {
        if (_markedStream == null)
            throw new IllegalStateException();

        BufferedPushIterator.Iter<T> startIterator = _markedStream._pieceIterator;
        int iStartOfs = _markedStream.getCurrentPieceOffset();

        BufferedPushIterator.Iter<T> endIterator = _readStream._pieceIterator;
        int iEndOfs = _readStream.getCurrentPieceOffset();

        List<T> pieces = startIterator.getElementSpanTo(endIterator);

        assert !pieces.isEmpty();

        // trim off the first sector if the span starts at the end of it
        int iStartSector = 0;
        if (iStartOfs == pieces.get(0).getDemuxPieceSize()) {
            iStartOfs = 0;
            iStartSector = 1;
        }

        ArrayList<T> sectorSpan = new ArrayList<T>(pieces.size() - iStartSector);
        for (int i = iStartSector; i < pieces.size(); i++) {
            sectorSpan.add(pieces.get(i));
        }
        return new DemuxedData<T>(sectorSpan, iStartOfs, iEndOfs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_readStream);
        if (_markedStream != null) {
            sb.append(" [marked ")
              .append(_markedStream)
              .append(" remaining ")
              .append(_iMarkReadLimit)
              .append("]");
        }
        if (_blnClosed)
            sb.append(" closed");
        else
            sb.append(" open");
        return sb.toString();
    }



    // -------------------------------------------------------------------------

    private static class PieceInputStream<T extends DemuxedData.Piece> extends InputStream {
        @Nonnull
        private final T _piece;
        private int _iReadHead;

        public PieceInputStream(@Nonnull T piece) {
            _piece = piece;
            _iReadHead = 0;
        }

        /** Copy constructor. */
        private PieceInputStream(@Nonnull T piece, int iReadHead) {
            _piece = piece;
            _iReadHead = iReadHead;
        }

        public @Nonnull PieceInputStream<T> copy() {
            return new PieceInputStream<T>(_piece, _iReadHead);
        }

        public boolean isEof() {
            return available() <= 0;
        }

        //......................................................................
        @Override
        public int read() {
            if (isEof())
                return -1;
            byte b = _piece.getDemuxPieceByte(_iReadHead);
            _iReadHead++;
            return b & 0xff;
        }

        @Override
        public long skip(long lngBytesToSkip) {
            if (isEof())
                return -1;
            int iSkippableBytes = available();
            long lngBytesSkipped;
            if (iSkippableBytes <= lngBytesToSkip) {
                lngBytesSkipped = iSkippableBytes;
                _iReadHead = _piece.getDemuxPieceSize();
            } else {
                lngBytesSkipped = lngBytesToSkip;
                _iReadHead += lngBytesToSkip;
            }
            return lngBytesSkipped;
        }

        @Override
        public int available() {
            return _piece.getDemuxPieceSize() - _iReadHead;
        }
        //......................................................................

        public @Nonnull T getPiece() {
            return _piece;
        }

        public int getReadOffset() {
            return _iReadHead;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("byte ")
              .append(_iReadHead)
              .append(" of ")
              .append(_piece.getDemuxPieceSize());
            return sb.toString();
        }

    }

    private static class CopyablePieceSequenceStream<T extends DemuxedData.Piece> extends InputStream {

        @Nonnull
        private final BufferedPushIterator.Iter<T> _pieceIterator;
        /** The end of once piece = the start of the next. */
        @Nonnull
        private PieceInputStream<T> _currentPieceStream;

        private int _iAvailable = 0;

        public CopyablePieceSequenceStream(@Nonnull BufferedPushIterator.Iter<T> pieceInterator) {
            _pieceIterator = pieceInterator;
            _currentPieceStream = new PieceInputStream<T>(_pieceIterator.next());
            // find out how much is initially available
            _iAvailable = _currentPieceStream.available();
            BufferedPushIterator.Iter<T> it = _pieceIterator.copy();
            while (it.hasNext()) {
                _iAvailable += it.next().getDemuxPieceSize();
            }
        }

        /** Copy constructor. */
        public CopyablePieceSequenceStream(@Nonnull BufferedPushIterator.Iter<T> pieceIterator,
                                           @Nonnull PieceInputStream<T> currentPieceStream,
                                           int iAvailable)
        {
            _pieceIterator = pieceIterator.copy();
            _currentPieceStream = currentPieceStream.copy();
            _iAvailable = iAvailable;
        }

        public @Nonnull CopyablePieceSequenceStream<T> copy() {
            return new CopyablePieceSequenceStream<T>(_pieceIterator, _currentPieceStream, _iAvailable);
        }

        public boolean isEof() {
            return _currentPieceStream.isEof() && !_pieceIterator.hasNext();
        }

        //......................................................................
        @Override
        public int read() {
            if (isEof())
                return -1;
            skipEofStreams();
            if (isEof())
                return -1;
            _iAvailable--;
            return _currentPieceStream.read();
        }

        private void skipEofStreams() {
            while (_currentPieceStream.isEof() && _pieceIterator.hasNext()) {
                _currentPieceStream = new PieceInputStream<T>(_pieceIterator.next());
            }
        }

        @Override
        public long skip(long lngBytesToSkip) {
            if (isEof())
                return -1;
            long lngTotalBytesSkipped = 0;
            while (lngTotalBytesSkipped < lngBytesToSkip && !isEof()) {
                skipEofStreams();
                long lngBytesSkipped = _currentPieceStream.skip(lngBytesToSkip - lngTotalBytesSkipped);
                if (lngBytesSkipped > 0)
                    lngTotalBytesSkipped += lngBytesSkipped;
            }
            _iAvailable -= lngTotalBytesSkipped;
            return lngTotalBytesSkipped == 0 ? -1 : lngTotalBytesSkipped;
        }

        @Override
        public int available() {
            return _iAvailable;
        }
        //......................................................................

        public void addAvailable(int iAddedBytes) {
            _iAvailable += iAddedBytes;
        }

        public int getCurrentPieceOffset() {
            return _currentPieceStream.getReadOffset();
        }

        public @Nonnull T getCurrentPiece() {
            return _currentPieceStream.getPiece();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(_currentPieceStream);
            if (_pieceIterator.hasNext())
                sb.append(" has next");
            else
                sb.append(" iterator end");
            return sb.toString();
        }
    }

}

