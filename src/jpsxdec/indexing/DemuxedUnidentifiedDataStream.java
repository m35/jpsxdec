/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
import java.util.LinkedList;
import java.util.ListIterator;
import jpsxdec.cdreaders.CdSector;


public class DemuxedUnidentifiedDataStream extends InputStream {

    private static class BufferedUnidentfifiedSectorWalker {
        private DiscriminatingSectorIterator _sectorReader;

        private final LinkedList<CdSector> _buffer =
                new LinkedList<CdSector>();

        /** Should never be null */
        private ListIterator<CdSector> _iter;

        private CdSector _currentSector;
        private CdSector _currentHead;

        public BufferedUnidentfifiedSectorWalker(DiscriminatingSectorIterator sectIter) throws IOException {
            _sectorReader = sectIter;
            _iter = _buffer.listIterator();
            _currentSector = _currentHead = _sectorReader.nextUnidentified();
        }

        public boolean headHasMore() throws IOException {
            return !_buffer.isEmpty() || _sectorReader.hasNextUnidentified();
        }

        public CdSector moveNextHead() throws IOException {
            if (!headHasMore())
                throw new RuntimeException();
            if (!_buffer.isEmpty()) {
                _currentHead = _buffer.removeFirst();
            } else {
                _currentHead = _sectorReader.nextUnidentified();
            }
            return _currentHead;
        }

        public void reset() throws IOException {
            _currentSector = _currentHead;
            _iter = _buffer.listIterator();
        }


        public CdSector currentHead() {
            return _currentHead;
        }

        // ...................................................

        public boolean hasMore() throws IOException {
            return _iter.hasNext() || _sectorReader.hasNextUnidentified();
        }

        public CdSector moveNext() throws IOException {
            if (_iter.hasNext()) {
                return _currentSector = _iter.next();
            } else {
                _currentSector = _sectorReader.nextUnidentified();
                _iter.add(_currentSector);
                return _currentSector;
            }
        }

        public CdSector current() {
            return _currentSector;
        }

    }


    private BufferedUnidentfifiedSectorWalker _readBuffer;

    private int _iStartingOffset;
    private int _iCurrentOffset;

    private boolean _blnHeadAtEnd;
    private boolean _blnAtEnd;

    public DemuxedUnidentifiedDataStream(DiscriminatingSectorIterator sectorIterator) throws IOException {
        _readBuffer = new BufferedUnidentfifiedSectorWalker(sectorIterator);
        _iCurrentOffset = _iStartingOffset = 0;
        _blnAtEnd = false;
        _blnHeadAtEnd = !_readBuffer.headHasMore();
    }

    public int getCurrentSectorOffset() {
        return _iCurrentOffset;
    }

    @Override
    public void reset() throws IOException {
        _readBuffer.reset();
        _iCurrentOffset = _iStartingOffset;
        _blnAtEnd = _blnHeadAtEnd;
    }

    public int getCurrentSector() {
        return _readBuffer.current().getSectorNumberFromStart();
    }

    public void incrementStartAndReset(int i) throws IOException {

        if (_blnHeadAtEnd)
            return;

        _iStartingOffset += i;
        
        CdSector currentHead = _readBuffer.currentHead();
        if (_iStartingOffset >= currentHead.getCdUserDataSize()) {
            if (_readBuffer.headHasMore()) {
                _iStartingOffset -= currentHead.getCdUserDataSize();
                _readBuffer.moveNextHead();
            } else {
                _blnHeadAtEnd = true;
                return;
            }
        }

        reset();
    }

    public boolean headHasMore() throws IOException {
        return !_blnHeadAtEnd;
    }


    @Override
    public int read() throws IOException {
        if (_blnAtEnd)
            return -1;

        CdSector sector = _readBuffer.current();

        int iReturn = sector.readUserDataByte(_iCurrentOffset) & 0xff;

        _iCurrentOffset++;

        if (_iCurrentOffset >= sector.getCdUserDataSize()) {
            if (_readBuffer.hasMore()) {
                _readBuffer.moveNext();
                _iCurrentOffset = 0;
            } else {
                _blnAtEnd = true;
            }
        }

        return iReturn;

    }

    @Override
    public long skip(long n) throws IOException {
        if (_blnAtEnd)
            return -1;

        _iCurrentOffset += n;

        int iCurSectSize = _readBuffer.current().getCdUserDataSize();
        while (_iCurrentOffset >= iCurSectSize) {
            if (_readBuffer.hasMore()) {
                _iCurrentOffset -= iCurSectSize;
                _readBuffer.moveNext();
                iCurSectSize = _readBuffer.current().getCdUserDataSize();
            } else {
                int iNotSkipped = _iCurrentOffset - iCurSectSize;
                _iCurrentOffset = iCurSectSize;
                _blnAtEnd = true;
                return n - iNotSkipped;
            }
        }

        return n;

    }



}
