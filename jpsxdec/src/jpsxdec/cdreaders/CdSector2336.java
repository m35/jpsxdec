/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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
import javax.annotation.Nonnull;
import jpsxdec.util.ByteArrayFPIS;


/** 2336 sectors only include the raw {@link CdxaSubHeader}, but not the
 *  {@link CdxaHeader}. */
public class CdSector2336 extends CdSector {

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Nonnull
    private final CdxaSubHeader _subHeader;
    private final int _iUserDataOffset;
    private final int _iUserDataSize;

    public CdSector2336(@Nonnull byte[] abSectorBytes, int iByteStartOffset,
                        int iSectorIndex, long lngFilePointer)
    {
        super(abSectorBytes, iByteStartOffset, iSectorIndex, lngFilePointer);

        _subHeader = new CdxaSubHeader(iSectorIndex, abSectorBytes, iByteStartOffset);
        _iUserDataOffset = _iByteStartOffset + _subHeader.getSize();
        if (_subHeader.getSubMode().getForm() == 1)
            _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1;
        else
            _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM2;
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
    public @Nonnull byte[] getCdUserDataCopy() {
        byte[] ab = new byte[_iUserDataSize];
        getCdUserDataCopy(0, ab, 0, _iUserDataSize);
        return ab;
    }

    public void getCdUserDataCopy(int iSourcePos, @Nonnull byte[] abOut, int iOutPos, int iLength) {
        if (iSourcePos < 0 || iSourcePos + iLength > _iUserDataSize ||
            iOutPos    < 0 || iOutPos    + iLength > abOut.length)
        {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(_abSectorBytes, _iUserDataOffset + iSourcePos,
                abOut, iOutPos,
                iLength);
    }
    
    /** Returns an InputStream of the 'user data' portion of the sector. */
    public @Nonnull ByteArrayFPIS getCdUserDataStream() {
        return new ByteArrayFPIS(_abSectorBytes, _iUserDataOffset, _iUserDataSize, _lngFilePointer);
    }

    @Override
    public @Nonnull byte[] getRawSectorDataCopy() {
        byte[] ab = new byte[CdFileSectorReader.SECTOR_SIZE_2336_BIN_NOSYNC];
        System.arraycopy(_abSectorBytes, _iByteStartOffset, ab, 0, ab.length);
        return ab;
    }


    //..........................................................................
    
    @Override
    public boolean hasSubHeader() {
        return true;
    }

    @Override
    public boolean hasHeaderSectorNumber() {
        return false;
    }

    public boolean isCdAudioSector() {
        return false;
    }

    @Override
    public boolean isMode1() {
        return false;
    }

    //..........................................................................
    
    @Override
    public int getSubHeaderChannel() {
        return _subHeader.getChannel();
    }
    @Override
    public int getSubHeaderFile() {
        return _subHeader.getFileNumber();
    }


    //..........................................................................

    @Override
    public @Nonnull CdxaSubHeader.SubMode getSubMode() {
        return _subHeader.getSubMode();
    }

    @Override
    public int subModeMask(int i) {
        return _subHeader.getSubMode().toByte() & i;
    }

    @Override
    public @Nonnull CdxaSubHeader.CodingInfo getCodingInfo() {
        return _subHeader.getCodingInfo();
    }

    //..........................................................................
    
    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of the sector userdata.
     *  [implements IGetFilePointer] */
    public long getUserDataFilePointer() {
        return _lngFilePointer + _subHeader.getSize();
    }

    @Override
    public boolean hasHeaderErrors() {
        return _subHeader.hasErrors();
    }

    @Override
    public int getErrorCount() {
        return _subHeader.getErrorCount();
    }
    
    public String toString() {
        return String.format("[Sector:%d M2 %s]", _iSectorIndex, _subHeader);
    }

    @Override
    public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abNewUserData) {
        byte[] abRawData = getRawSectorDataCopy();
        System.arraycopy(abNewUserData, 0, abRawData, _subHeader.getSize(), _iUserDataSize);
        Logger.getLogger(CdSector2336.class.getName()).info("No need to rebuild 2336 EDC");

        return abRawData;
    }
    
    
}
