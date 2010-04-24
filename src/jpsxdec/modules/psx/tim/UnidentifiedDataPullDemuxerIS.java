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

package jpsxdec.modules.psx.tim;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.IdentifiedSectorRangeIterator;
import jpsxdec.modules.UnidentifiedSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IGetFilePointer;

/** Demuxes a series of UnidentifiedSectorUnknownData into a solid stream.
 *  Sectors are pulled from an iterator as they are needed. */
public class UnidentifiedDataPullDemuxerIS extends InputStream implements IGetFilePointer
{
    
    IdentifiedSectorRangeIterator _identifiedSectIter;
    /** Holds the current sector's stream being read */
    ByteArrayFPIS _identifiedSectStream;
    boolean _blnAtEnd = false;
    
    // holds the marked position, both the sector and the stream
    int _iMarkIndex = -1;
    ByteArrayFPIS _markSectStream;

    public UnidentifiedDataPullDemuxerIS(IdentifiedSectorRangeIterator iterator)
            throws IOException 
    {
        _identifiedSectIter = iterator;
        IdentifiedSector oSect = _identifiedSectIter.peekNext();
        if (!(oSect instanceof UnidentifiedSector)) {
            _blnAtEnd = true;
            _identifiedSectStream = null;
        } else {
            _identifiedSectStream = oSect.getIdentifiedUserDataStream();
        }
    }

    /** [InputStream] */ @Override 
    public int read() throws IOException {
        if ((_identifiedSectStream == null) || _blnAtEnd)
            return -1;
        else {
            int i = _identifiedSectStream.read();
            while (i < 0) {
                _identifiedSectIter.skipNext();
                if (_identifiedSectIter.hasNext()) {
                    IdentifiedSector oSect = _identifiedSectIter.peekNext();
                    if (oSect instanceof UnidentifiedSector) {
                        _identifiedSectStream = oSect.getIdentifiedUserDataStream();
                        i = _identifiedSectStream.read();
                    } else {
                        _blnAtEnd = true;
                        return -1;
                    }
                } else {
                    _blnAtEnd = true;
                    return -1;
                }
            }
            return i;
        }
    }

    public CDSectorReader getSourceCD() {
        return _identifiedSectIter.getSourceCD();
    }

    /** [implements IGetFilePointer] */
    public long getFilePointer() {
        if (_identifiedSectStream == null)  // there were never any sectors
            return -1;
        else
            return _identifiedSectStream.getFilePointer();
    }

    public int getSectorNumber() {
        return _identifiedSectIter.getIndex();
    }

    /** @param readlimit ignored. You can read forever. */
    @Override /* [InputStream] */
    public void mark(int readlimit) {
        if (_identifiedSectStream != null) { // there were never any sectors to mark
            // save the sector index
            _iMarkIndex = _identifiedSectIter.getIndex();
            // and save the stream in case 
            // furthur reads move past the current stream
            _markSectStream = _identifiedSectStream;
            _markSectStream.mark(readlimit);
        }
    }

    /** [InputStream] */ @Override
    public void reset() throws IOException {
        if (_iMarkIndex < 0)
            throw new RuntimeException("Trying to reset when mark has not been called.");
        
        // go back to the sector we were at (if we've moved since then)
        if (_iMarkIndex != _identifiedSectIter.getIndex()) {
            _identifiedSectIter.gotoIndex(_iMarkIndex);
        }
        // restore the original stream (in case we moved on since then)
        _identifiedSectStream = _markSectStream;
        // and reset the original position
        _identifiedSectStream.reset();
        
        _iMarkIndex = -1;
        _markSectStream = null;
    }

    /** [InputStream] */ @Override
    public boolean markSupported() {
        return true;
    }

    public void skipToMoreUnknownData() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
