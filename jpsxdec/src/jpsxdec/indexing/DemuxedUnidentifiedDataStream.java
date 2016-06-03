/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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
import java.io.InputStream;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;

/** Demuxes a sequence of unidentified sectors together into an
 * {@link InputStream}. Class dies at the end of a sequence and a new class
 * should be created for the next sequence.
 * <p>
 * Does not support traditional {@link InputStream} mark/reset.
 * The reading is always buffered ("marked") by default.
 * <p>
 * Calling {@link #resetMark()} will reset to the mark and immediately mark again.
 * Calling {@link #resetSkipMark(int)} will reset to the mark,
 * skip the indicated bytes, and mark again.
 * <p>
 * Do not use object anymore after {@link #resetSkipMark(int)} returns false.
 */
public class DemuxedUnidentifiedDataStream extends InputStream {

    private static class ArrayQueue {

        @Nonnull
        private CdSector[] _ao = new CdSector[16];
        private int _iHead = 0, _iTail = 0, _iSize = 0;

        public ArrayQueue() {
            this(16);
        }

        public ArrayQueue(int iInitialSize) {
            _ao = new CdSector[iInitialSize];
        }

        public @Nonnull CdSector get(int i) {
            if (i >= _iSize)
                throw new IndexOutOfBoundsException();
            i = wrap(i + _iHead);
            return _ao[i];
        }

        public @Nonnull CdSector dequeue() {
            if (_iSize == 0)
                throw new NoSuchElementException();
            CdSector e = _ao[_iHead];
            _ao[_iHead] = null;
            _iHead = wrap(_iHead+1);
            _iSize--;
            return e;
        }

        private int wrap(int i) {
            if (i >= _ao.length)
                return i - _ao.length;
            return i;
        }

        public void enqueue(@Nonnull CdSector cdSector) {
            if (_iSize >= _ao.length) {
                // if full, head == tail
                assert _iHead == _iTail;
                CdSector[] ao = new CdSector[_ao.length * 2];
                if (_iHead == 0) {
                    System.arraycopy(_ao, _iHead, ao, 0, _iSize);
                } else {
                    System.arraycopy(_ao, _iHead, ao, 0, _ao.length - _iHead);
                    System.arraycopy(_ao, 0, ao, _ao.length - _iHead, _iTail);
                }
                _ao = ao;
                _iHead = 0;

                _ao[_iSize] = cdSector;
                _iSize++;
                _iTail = _iSize;
            } else {
                _ao[_iTail] = cdSector;
                _iTail = wrap(_iTail+1);
                _iSize++;
            }
        }

        public int size() {
            return _iSize;
        }
    }

    private static class BufferedUnidentifiedSectorIterator {
        @Nonnull
        private final UnidentifiedSectorIterator _sectorIter;

        private int _iReadPos;

        private final ArrayQueue _buffer = new ArrayQueue(64);

        public BufferedUnidentifiedSectorIterator(@Nonnull UnidentifiedSectorIterator sectorIter) {
            _sectorIter = sectorIter;
            _iReadPos = 0;
        }

        public @CheckForNull CdSector nextUnidentified() throws IOException {
            if (_iReadPos < _buffer.size()) {
                CdSector c = _buffer.get(_iReadPos);
                _iReadPos++;
                return c;
            } else {
                CdSector c = _sectorIter.nextUnidentified();
                if (c != null) {
                    _buffer.enqueue(c);
                    _iReadPos++;
                }
                return c;
            }
        }

        public void reset() {
            _iReadPos = 0;
        }

        public boolean atEndOfDisc() {
            return _sectorIter.atEndOfDisc() && _buffer.size() == 0;
        }

        public void dropHead() {
            if (_iReadPos == 0 || _buffer.size() == 0)
                throw new IllegalStateException();
            _buffer.dequeue();
            _iReadPos--;
        }
    }


    @Nonnull
    private final BufferedUnidentifiedSectorIterator _readBuffer;

    /** The starting 'mark' of the first sector in the {@link #_readBuffer}.
     * Will be -1 after {@link #resetSkipMark(int)} at the end of the
     * unidentified sequence. */
    private int _iStartingOffset = 0;

    /** The current offset in the {@link #_current} sector.
     * Will == {@link #_current}.getCdUserDataSize() at the end of the stream,
     * or -1 after {@link #resetSkipMark(int)} at the end of the
     * unidentified sequence. */
    private int _iCurrentOffset = 0;
    
    /** Will always be valid until after {@link #resetSkipMark(int)} at the end
     * of the unidentified sequence, then null. */
    @CheckForNull
    private CdSector _current;

    /** {@link UnidentifiedSectorIterator} must be at the start of an
     * unidentified sequence, otherwise throws {@link IllegalStateException}.
     * @throws IllegalStateException */
    public DemuxedUnidentifiedDataStream(@Nonnull UnidentifiedSectorIterator sectorIterator) 
            throws IOException
    {
        _readBuffer = new BufferedUnidentifiedSectorIterator(sectorIterator);
        _current = _readBuffer.nextUnidentified();
        if (_current == null)
            throw new IllegalStateException();
    }

    /** Will return invalid after {@link #resetSkipMark(int)} returns false. */
    public int getCurrentSectorOffset() {
        return _iCurrentOffset;
    }

    /** Do not call after {@link #resetSkipMark(int)} returns false.
     * @throws IllegalStateException */
    public int getCurrentSector() {
        if (_current == null)
            throw new IllegalStateException();
        return _current.getSectorNumberFromStart();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Do not call this");
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException("Do not call this");
    }

    public void resetMark() throws IOException {
        _readBuffer.reset();
        _current = _readBuffer.nextUnidentified();
        if (_current == null)
            throw new IllegalStateException();
        _iCurrentOffset = _iStartingOffset;
    }

    public boolean resetSkipMark(int iSkip) throws IOException {
        _readBuffer.reset();
        _current = _readBuffer.nextUnidentified();
        if (_current == null) {
            _iCurrentOffset = -1;
            _iStartingOffset = -1;
            return false;
        }
        _iStartingOffset += iSkip;
        while (_iStartingOffset >= _current.getCdUserDataSize()) {
            _iStartingOffset -= _current.getCdUserDataSize();
            _readBuffer.dropHead();
            _current = _readBuffer.nextUnidentified();
            if (_current == null) {
                _iCurrentOffset = -1;
                _iStartingOffset = -1;
                return false;
            }
        }
        _iCurrentOffset = _iStartingOffset;
        return true;
    }


    /** Do not call after {@link #resetSkipMark(int)} returns false.
     * @throws IllegalStateException */
    @Override
    public int read() throws IOException {
        if (_current == null)
            throw new IllegalStateException();

        // 'while' in strange case CdSector has 0 user data size
        while (_iCurrentOffset >= _current.getCdUserDataSize()) {
            CdSector next = _readBuffer.nextUnidentified();
            if (next != null) {
                _iCurrentOffset = 0;
                _current = next;
            } else {
                // keep the current sector current once we hit the end so
                // getCurrentSector() can return the last read sector
                return -1;
            }
        }

        int iReturn = _current.readUserDataByte(_iCurrentOffset) & 0xff;
        _iCurrentOffset++;
        return iReturn;
    }

    /** Do not call after {@link #resetSkipMark(int)} returns false.
     * @throws IllegalStateException */
    @Override
    public long skip(final long n) throws IOException {
        if (_current == null)
            throw new IllegalStateException();

        long lngRemain = n;
        while (lngRemain > 0) {
            int iSectorRemain = _current.getCdUserDataSize() - _iCurrentOffset;
            if (lngRemain <= iSectorRemain) {
                _iCurrentOffset += lngRemain;
                lngRemain = 0;
            } else {
                lngRemain -= iSectorRemain;
                _iCurrentOffset += iSectorRemain;
                CdSector next = _readBuffer.nextUnidentified();
                if (next != null) {
                    _iCurrentOffset = 0;
                    _current = next;
                } else {
                    // keep the current sector current once we hit the end so
                    // getCurrentSector() can return the last read sector
                    break;
                }
            }
        }
        if (n == lngRemain) // if unable to skip anything at all
            return -1; // then we're at end of stream
        else
            return n - lngRemain; // otherwise return how many bytes skipped
    }
}
