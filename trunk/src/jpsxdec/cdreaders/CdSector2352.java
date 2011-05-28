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

package jpsxdec.cdreaders;

import java.util.logging.Logger;
import jpsxdec.util.ByteArrayFPIS;


/** 2352 sectors are the size found in BIN/CUE disc images and include the
 *  full raw header with {@link CdxaHeader} and {@link CdxaSubHeader}. */
public class CdSector2352 extends CdSector {

    private static final Logger log = Logger.getLogger(CdSector2352.class.getName());
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private final CdxaHeader _header;
    private final CdxaSubHeader _subHeader;
    private final int _iUserDataOffset;
    private final int _iUserDataSize;

    public CdSector2352(byte[] abSectorBytes, int iByteStartOffset, int iSectorIndex, long lngFilePointer)
    {
        super(abSectorBytes, iByteStartOffset, iSectorIndex, lngFilePointer);
        _header = new CdxaHeader(abSectorBytes, iByteStartOffset);
        // TODO: if the sync header is imperfect (but passable), but the subheader is all errors -> it's cd audio
        if (_header.isCdAudioSector()) {
            _subHeader = null;
            _iUserDataOffset = _iByteStartOffset;
            _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_CD_AUDIO;
        } else {
            _subHeader = new CdxaSubHeader(abSectorBytes, _iByteStartOffset + CdxaHeader.SIZE);
            _iUserDataOffset = _iByteStartOffset + _header.getSize() + _subHeader.getSize();
            if (_subHeader.getSubMode().getForm() == 1)
                _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1;
            else
                _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM2;
        }
    }

    /** Returns the size of the 'user data' portion of the sector. */
    public int getCdUserDataSize() {
        return _iUserDataSize;
    }
    
    public byte readUserDataByte(int i) {
        if (i < 0 || i >= _iUserDataSize) throw new IndexOutOfBoundsException();
        return _abSectorBytes[_iUserDataOffset + i];
    }

    /** Returns copy of the 'user data' portion of the sector. */
    public byte[] getCdUserDataCopy() {
        byte[] ab = new byte[_iUserDataSize];
        getCdUserDataCopy(0, ab, 0, _iUserDataSize);
        return ab;
    }

    public void getCdUserDataCopy(int iSourcePos, byte[] abOut, int iOutPos, int iLength) {
        if (iSourcePos < 0 || iSourcePos + iLength > _iUserDataSize) throw new IndexOutOfBoundsException();
        System.arraycopy(_abSectorBytes, _iUserDataOffset + iSourcePos,
                abOut, iOutPos,
                iLength);
    }
    
    /** Returns an InputStream of the 'user data' portion of the sector. */
    public ByteArrayFPIS getCdUserDataStream() {
        return new ByteArrayFPIS(_abSectorBytes, _iUserDataOffset, _iUserDataSize, _lngFilePointer);
    }

    @Override
    public byte[] getRawSectorDataCopy() {
        byte[] ab = new byte[CdFileSectorReader.SECTOR_SIZE_2352_BIN];
        System.arraycopy(_abSectorBytes, _iByteStartOffset, ab, 0, ab.length);
        return ab;
    }

    //..........................................................................
    
    public boolean hasRawSectorHeader() {
        return true;
    }

    @Override
    public boolean isCdAudioSector() {
        return _header.isCdAudioSector();
    }

    /** @return The sector number from the sector header,
     *          or -1 if not available. */
    public int getHeaderSectorNumber() {
        return _header.calculateSectorNumber();
    }

    //..........................................................................
    
    public int getChannel() {
        if (_subHeader == null)
            return super.getChannel();
        else
            return _subHeader.getChannel();
    }

    //..........................................................................

    @Override
    public CdxaSubHeader.SubMode getSubMode() {
        if (_subHeader == null)
            return super.getSubMode();
        else
            return _subHeader.getSubMode();
    }

    @Override
    public int subModeMask(int i) {
        if (_subHeader == null)
            return super.subModeMask(i);
        else
            return _subHeader.getSubMode().toByte() & i;
    }

    @Override
    public CdxaSubHeader.CodingInfo getCodingInfo() {
        if (_subHeader == null)
            return super.getCodingInfo();
        else
            return _subHeader.getCodingInfo();
    }

    //..........................................................................
    
    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of the sector userdata.
     *  [implements IGetFilePointer] */
    public long getFilePointer() {
        return _lngFilePointer + _header.getSize() + _subHeader.getSize();
    }

    @Override
    public void printErrors(Logger logger) {
        _header.printErrors(_iSectorIndex, logger);
        if (_subHeader != null)
            _subHeader.printErrors(_iSectorIndex, logger);
    }

    @Override
    public int getErrorCount() {
        if (_subHeader == null)
            return _header.getErrorCount();
        else
            return _header.getErrorCount() + _subHeader.getErrorCount();
    }
    
    public String toString() {
        if (_header.isCdAudioSector())
            return String.format("[Sector:%d CD Audio]", _iSectorIndex);
        else
            return String.format("[Sector:%d (%d) %s]", _iSectorIndex, _header.calculateSectorNumber(), _subHeader);
    }
}
