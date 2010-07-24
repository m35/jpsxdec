/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.SectorReadErrorException;
import jpsxdec.util.AdvancedIOIterator;

/** Class to walk a range of sectors of a CD and automatically convert them
 *  to PSXSectors before returning them. */
public class IdentifiedSectorRangeIterator implements AdvancedIOIterator<IdentifiedSector> {

    /** Interface for listener classes. */
    public static interface ISectorChangeListener {
        void CurrentSector(int i);
        void ReadError(SectorReadErrorException ex);
    }

    private CDFileSectorReader _cdReader;
    private int _iSectorIndex;
    private int _iStartSector, _iEndSector;
    private IdentifiedSector _cachedSector;
    private boolean _blnCached = false;
    private ISectorChangeListener _listener;
    
    public IdentifiedSectorRangeIterator(CDFileSectorReader cdReader) {
        this(cdReader, 0, cdReader.size()-1);
    }
    
    public IdentifiedSectorRangeIterator(CDFileSectorReader cdReader, int iStartSector, int iEndSector) {
        _cdReader = cdReader;
        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
        _iSectorIndex = iStartSector;
    }
    
    public CDFileSectorReader getSourceCD() {
        return _cdReader;
    }

    public IdentifiedSector peekNext() throws IOException {
        if (!hasNext()) 
            throw new NoSuchElementException();
        if (!_blnCached) {
            try {
                _cachedSector = JPSXModule.identifyModuleSector(_cdReader.getSector(_iSectorIndex));
                _blnCached = true;
            } catch (SectorReadErrorException ex) {
                if (_listener != null) _listener.ReadError(ex);
                return null;
            }
        }
        return _cachedSector;
    }
    
    public boolean hasNext() {
        return _iSectorIndex <= _iEndSector;
    }

    public IdentifiedSector next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        IdentifiedSector oPSXSect = null;
        if (!_blnCached) {
            try {
                oPSXSect = JPSXModule.identifyModuleSector(_cdReader.getSector(_iSectorIndex));
            } catch (SectorReadErrorException ex) {
                if (_listener != null) _listener.ReadError(ex);
            }
        } else {
            oPSXSect = _cachedSector;
            _cachedSector = null;
            _blnCached = false;
        }
        _iSectorIndex++;
        if (_listener != null) _listener.CurrentSector(_iSectorIndex);
        return oPSXSect;
    }
    
    public void skipNext() {
        if (!hasNext()) throw new NoSuchElementException();
        _blnCached = false;
        _cachedSector = null;
        _iSectorIndex++;
        if (_listener != null) _listener.CurrentSector(_iSectorIndex);
    }

    
    public int getIndex() {
        return _iSectorIndex;
    }
    
    public void gotoIndex(int i) {
        if (i < _iStartSector || i > _iEndSector+1) throw new NoSuchElementException();
        _blnCached = false;
        _cachedSector = null;
        _iSectorIndex = i;
    }

    /** Unable to remove sectors */ 
    public void remove() {throw new UnsupportedOperationException();}
    
    /** Set a listener to be notified whenever the sector is incremented. */
    public void setSectorChangeListener(ISectorChangeListener listener) {
        _listener = listener;
    }

}




