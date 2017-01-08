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

import javax.annotation.Nonnull;
import jpsxdec.util.ByteArrayFPIS;


/** 2048 sectors are standard .iso size that excludes any raw header info. */
public class CdSector2048 extends CdSector {

    public CdSector2048(@Nonnull byte[] abSectorBytes, int iByteStartOffset, 
                        int iSectorIndex, long lngFilePointer)
    {
        super(abSectorBytes, iByteStartOffset, iSectorIndex, lngFilePointer);
        // TODO: verify bytes are minimum size
    }


    /** Returns the size of the 'user data' portion of the sector. */
    public int getCdUserDataSize() {
        return CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1;
    }
    
    public byte readUserDataByte(int i) {
        if (i < 0 || i >= CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1) throw new IndexOutOfBoundsException();
        return _abSectorBytes[_iByteStartOffset + i];
    }

    /** Returns copy of the 'user data' portion of the sector. */
    public @Nonnull byte[] getCdUserDataCopy() {
        byte[] ab = new byte[CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1];
        getCdUserDataCopy(0, ab, 0, CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1);
        return ab;
    }

    public void getCdUserDataCopy(int iSourcePos, @Nonnull byte[] abOut, int iOutPos, int iLength) {
        if (iSourcePos < 0 || iSourcePos + iLength > CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1 ||
            iOutPos    < 0 || iOutPos    + iLength > abOut.length)
        {
            throw new IndexOutOfBoundsException();
        }
        System.arraycopy(_abSectorBytes, _iByteStartOffset + iSourcePos, 
                abOut, iOutPos,
                iLength);
    }
    
    /** Returns an InputStream of the 'user data' portion of the sector. */
    public @Nonnull ByteArrayFPIS getCdUserDataStream() {
        return new ByteArrayFPIS(_abSectorBytes, _iByteStartOffset, CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1, _lngFilePointer);
    }

    /** Returns direct reference to the underlying sector data, with raw
     * header/footer and everything it has. */
    public @Nonnull byte[] getRawSectorDataCopy() {
        return getCdUserDataCopy();
    }

    //..........................................................................

    @Override
    public boolean hasSubHeader() {
        return false;
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
        return true;
    }

    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of the sector userdata.
     *  [implements IGetFilePointer] */
    public long getUserDataFilePointer() {
        return _lngFilePointer;
    }

    @Override
    public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abUserData) {
        return abUserData.clone();
    }
    
    @Override
    public boolean hasHeaderErrors() {
        return false;
    }

    @Override
    public int getErrorCount() {
        return 0;
    }
    
    public String toString() {
        return String.format("[Sector:%d M1]", _iSectorIndex);
    }

}
