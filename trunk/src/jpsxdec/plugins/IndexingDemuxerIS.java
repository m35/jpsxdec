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

package jpsxdec.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IGetFilePointer;

/** Demuxes a series of UnidentifiedSectorUnknownData into a solid stream.
 *  In the process of reading the sectors, they are also passed to
 *  SectorIndexers.
 *
 * 
 */
public class IndexingDemuxerIS extends InputStream implements IGetFilePointer
{
    private static final Logger log = Logger.getLogger(IndexingDemuxerIS.class.getName());

    private CDSectorReader _sourceCd;
    
    private int _iCurSectNum;
    private final JPSXPlugin[] _aoPlugin;
    private int _iMaxReadSectNum;

    /** Holds the current sector's stream being read
     *  If it is null, it means it has yet to encounter
     *  a sector of unknown data. */
    private ByteArrayFPIS _psxSectStream;

    private static final int NO_UNKNOWN_DATA_FOUND = 0;
    private static final int READING_UNKNOWN_DATA = 1;
    private static final int END_OF_UNKNOWN_DATA = 2;
    private static final int END_OF_DISC = 3;
    private int _iState;
    
    // holds the marked position, both the sector and the stream
    private int _iMarkSectNum = -1;
    private ByteArrayFPIS _markSectStream;
    private int _iMarkState = -1;

    public IndexingDemuxerIS(CDSectorReader cdReader, JPSXPlugin[] aoPlugins)
            throws IOException 
    {
        _sourceCd = cdReader;
        _iCurSectNum = 0;
        _iMaxReadSectNum = -1;
        _aoPlugin = aoPlugins;

        IdentifiedSector oSect = identifySector(_sourceCd.getSector(_iCurSectNum));
        if (oSect instanceof UnidentifiedSectorUnknownData) {
            _iState = READING_UNKNOWN_DATA;
            _psxSectStream = oSect.getIdentifiedUserDataStream();
        } else {
            _iState = NO_UNKNOWN_DATA_FOUND;
            _psxSectStream = null;
        }
    }

    private IdentifiedSector identifySector(CDSector cdSector) {
        IdentifiedSector oPSXSect;
        for (JPSXPlugin oIndexer : _aoPlugin) {
            oPSXSect = oIndexer.identifySector(cdSector);
            if (oPSXSect != null) {
                notifyListeners(oPSXSect);
                return oPSXSect;
            }
        }
        oPSXSect = new UnidentifiedSectorUnknownData(cdSector);
        notifyListeners(oPSXSect);
        return oPSXSect;
    }
    private void notifyListeners(IdentifiedSector cdSector) {
        if (_iCurSectNum > _iMaxReadSectNum) {
            String sSectStr = cdSector.toString();
            if (log.isLoggable(Level.FINE))
                log.fine("Read sector " + (sSectStr == null ?
                    cdSector.getSectorNumber() : sSectStr) );

            for (JPSXPlugin oIndexer : _aoPlugin)
                oIndexer.indexing_sectorRead(cdSector);
            _iMaxReadSectNum = _iCurSectNum;
        }
    }



    /** [InputStream] */ @Override 
    public int read() throws IOException {
        if (_iState != READING_UNKNOWN_DATA)
            return -1;
        else {
            int i = _psxSectStream.read();
            while (i < 0) {
                moveToNextSector();
                if (_iState != READING_UNKNOWN_DATA)
                    return -1;
                i = _psxSectStream.read();
            }
            return i;
        }
    }

    @Override
    public long skip(long lng) throws IOException {
        if (_iState != READING_UNKNOWN_DATA)
            return -1;
        else {
            long lngSkipTotal = 0;
            while (lngSkipTotal < lng) {
                long lngSkipped = _psxSectStream.skip(lng);
                if (lngSkipped >= 1) {
                    lngSkipTotal += lngSkipped;
                } else {
                    moveToNextSector();
                    if (_iState != READING_UNKNOWN_DATA)
                        break;
                }
            }
            return lngSkipTotal;
        }
    }


    /* [implements IGetFilePointer] */
    public long getFilePointer() {
        if (_psxSectStream == null)  // there were never any sectors
            return -1;
        else
            return _psxSectStream.getFilePointer();
    }

    public int getSectorPosition() {
        if (_psxSectStream != null)
            return _psxSectStream.getOffset();
        else
            return -1;
    }

    public int getSectorNumber() {
        return _iCurSectNum;
    }

    /** @param readlimit ignored. You can read forever. */
    @Override /* [InputStream] */
    public void mark(int readlimit) {
        if (_psxSectStream != null) { // we've yet to encounter unknown data, so nothing to mark
            // save the sector index
            _iMarkSectNum = _iCurSectNum;
            // and save the stream in case 
            // furthur reads move past the current stream
            _markSectStream = _psxSectStream;
            _markSectStream.mark(readlimit);
            _iMarkState = _iState;
        } else {
            System.out.println("???");
        }
    }

    @Override /* [InputStream] */
    public void reset() throws IOException {
        if (_iMarkSectNum < 0)
            throw new IllegalStateException("Trying to reset when mark has not been called.");
        
        // go back to the sector we were at (if we've moved since then)
        if (_iMarkSectNum != _iCurSectNum) {
            _iCurSectNum = _iMarkSectNum;
        }
        // restore the original stream (in case we moved on since then)
        _psxSectStream = _markSectStream;
        // and reset the original position
        _psxSectStream.reset();
        _iState = _iMarkState;
        
        _iMarkSectNum = -1;
        _markSectStream = null;
        _iMarkState = -1;
    }

    @Override /* [InputStream] */
    public boolean markSupported() {
        return true;
    }

    public void skipToMoreUnknownData() throws IOException {
        while (_iState != READING_UNKNOWN_DATA && _iState != END_OF_DISC) {
            moveToNextSector();
        }
    }

    private int moveToNextSector() throws IOException {
        if (!hasNextSector()) {
            _iState = END_OF_DISC;
        } else {
            _iCurSectNum++;

            IdentifiedSector oSect = identifySector(_sourceCd.getSector(_iCurSectNum));
            if (oSect.getSectorType() == IdentifiedSector.SECTOR_UNKNOWN) {
                _psxSectStream = oSect.getIdentifiedUserDataStream();
                _iState = READING_UNKNOWN_DATA;
            } else {
                _iState = END_OF_UNKNOWN_DATA;
            }
        }
        return _iState;
    }

    public void skipToNextSector() throws IOException {
        moveToNextSector();
    }

    private boolean hasNextSector() {
        return _iCurSectNum < _sourceCd.size()-1;
    }
    public boolean atEndOfDisc() {
        return _iState == END_OF_DISC;
    }
    public boolean atEndOfUnknownDataStream() {
        return _iState != READING_UNKNOWN_DATA;
    }

}
