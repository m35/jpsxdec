/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.discitems;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;

/** Demuxes a series of UnidentifiedSectorUnknownData into a solid stream.
 *  Sectors are pulled from an iterator as they are needed. */
public class DemuxedSectorInputStream extends InputStream
{

    private final CdFileSectorReader _cd;
    private int _iSector;
    private int _iOffset;

    private CdSector _currentSector;
    

    public DemuxedSectorInputStream(CdFileSectorReader cd, int iSector, int iOffset)
            throws IOException
    {
        _cd = cd;
        _iSector = iSector;
        _iOffset = iOffset;
        _currentSector = _cd.getSector(iSector);
    }

    @Override // [InputStream]
    public int read() throws IOException {
        if (_iOffset >= _currentSector.getCdUserDataSize()) {
            if (_iSector + 1 >= _cd.getLength())
                return -1;
            else {
                _iSector++;
                _currentSector = _cd.getSector(_iSector);
                _iOffset = 0;
            }
        }
        int iByte = _currentSector.readUserDataByte(_iOffset) & 0xff;
        _iOffset++;
        return iByte;
    }

    public CdFileSectorReader getSourceCd() {
        return _cd;
    }

    public long getFilePointer() {
        return _currentSector.getFilePointer() + _iOffset;
    }

    public int getSectorNumber() {
        return _iSector;
    }

}
