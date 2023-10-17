/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;


/** Represents a single sector on a CD. */
public abstract class CdSector {

    /** Normal iso sector data size: 2048. */
    public final static int SECTOR_SIZE_2048_ISO            = 2048;
    /** Raw sector without sync header: 2336. */
    public final static int SECTOR_SIZE_2336_BIN_NOSYNC     = 2336;
    /** Full raw sector: 2352. */
    public final static int SECTOR_SIZE_2352_BIN            = 2352;
    /** Full raw sector with sub-channel data: 2442. */
    public final static int SECTOR_SIZE_2448_BIN_SUBCHANNEL = 2448;


    /** Data sector payload size for Mode 1 and Mode 2 Form 1: 2048. */
    public final static int SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1 = 2048;
    /** Payload size for and Mode 2 Form 2 (usually XA audio): 2324. */
    public final static int SECTOR_USER_DATA_SIZE_MODE2FORM2       = 2324;
    /** CD audio sector payload size: 2352. */
    public final static int SECTOR_USER_DATA_SIZE_CD_AUDIO         = 2352;

    public enum Type {
        CD_AUDIO(SECTOR_USER_DATA_SIZE_CD_AUDIO),
        UNKNOWN2048(SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1),
        MODE1(SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1),
        MODE2FORM1(SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1),
        MODE2FORM2(SECTOR_USER_DATA_SIZE_MODE2FORM2);

        private final int _iSectorUserDataSize;
        private Type(int iSectorUserDataSize) {
            _iSectorUserDataSize = iSectorUserDataSize;
        }

        public int getSectorUserDataSize() {
            return _iSectorUserDataSize;
        }
    }

    // =========================================================================

    private final int _iSectorIndex;
    @Nonnull
    private final byte[] _abSectorBytes;
    /** Offset in {@link #_abSectorBytes} where this sector begins. */
    private final int _iByteStartOffset;
    /** Byte offset of this sector in the source file. */
    private final int _iFilePointer;

    public CdSector(int iSectorIndex, @Nonnull byte[] abSectorBytes, int iByteStartOffset,
                    int iFilePointer) {
        _iSectorIndex = iSectorIndex;
        _abSectorBytes = abSectorBytes;
        _iByteStartOffset = iByteStartOffset;
        _iFilePointer = iFilePointer;
    }

    // .........................................................................
    // to override

    abstract public int getRawCdSectorSize();
    abstract public int getCdUserDataSize();
    abstract protected int getHeaderDataSize();

    abstract public @Nonnull Type getType();
    abstract public boolean isCdAudioSector();

    abstract public @CheckForNull CdSectorHeader getHeader();
    abstract public @CheckForNull CdSectorXaSubHeader getSubHeader();

    abstract public boolean hasHeaderErrors();
    abstract public int getErrorCount();

    abstract public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abNewUserData);

    @Override
    abstract public @Nonnull String toString();

    // .........................................................................

    public int getSectorIndexFromStart() {
        return _iSectorIndex;
    }

    /** Returns the actual offset in bytes from the start of the file/CD
     * to the start of the sector userdata. */
    //[implements IGetFilePointer]
    final public int getUserDataFilePointer() {
        return _iFilePointer + getHeaderDataSize();
    }

    /** Returns copy of the 'user data' portion of the sector. */
    final public @Nonnull byte[] getCdUserDataCopy() {
        int iStart = _iByteStartOffset + getHeaderDataSize();
        return Arrays.copyOfRange(_abSectorBytes,
                                  iStart, iStart + getCdUserDataSize());
    }

    /** Copies a block of bytes out of the user data portion of this CD sector to the supplied array.
     * @throws IndexOutOfBoundsException If the source or destination bounds are violated. */
    final public void getCdUserDataCopy(int iSourcePos, @Nonnull byte[] abOut, int iOutPos, int iLength)
            throws IndexOutOfBoundsException
    {
        if (iSourcePos < 0 || iSourcePos+iLength > getCdUserDataSize() ||
            iLength    < 0 || iOutPos   +iLength > abOut.length)
            throw new IndexOutOfBoundsException();
        int iStart = _iByteStartOffset + getHeaderDataSize() + iSourcePos;
        System.arraycopy(_abSectorBytes, iStart, abOut, iOutPos, iLength);
    }

    /** Returns a copy of the underlying sector data, with raw
     * header/footer and everything it has. */
    final public @Nonnull byte[] getRawSectorDataCopy() {
        return Arrays.copyOfRange(_abSectorBytes,
                                  _iByteStartOffset,
                                  _iByteStartOffset+getRawCdSectorSize());
    }

    /** Returns an InputStream of the 'user data' portion of the sector. */
    final public @Nonnull ByteArrayFPIS getCdUserDataStream() {
        return getCdUserDataStream(0);
    }

    /** Returns an InputStream of the 'user data' portion of the sector. */
    final public @Nonnull ByteArrayFPIS getCdUserDataStream(int iStartOffset) {
        if (iStartOffset < 0 || iStartOffset > getCdUserDataSize())
            throw new IllegalArgumentException();
        int iStart = _iByteStartOffset + getHeaderDataSize() + iStartOffset;
        return new ByteArrayFPIS(_abSectorBytes, iStart, getCdUserDataSize() - iStartOffset, getUserDataFilePointer());
    }

    // .........................................................................
    // readers

    final public byte readUserDataByte(int i) {
        checkIndex(i);
        return _abSectorBytes[_iByteStartOffset + getHeaderDataSize() + i];
    }

    final public short readSInt16LE(int i) {
        checkIndex(i);
        return IO.readSInt16LE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public short readSInt16BE(int i) {
        checkIndex(i);
        return IO.readSInt16BE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public int readUInt16LE(int i) {
        checkIndex(i);
        return IO.readUInt16LE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public long readUInt32LE(int i) {
        checkIndex(i);
        return IO.readUInt32LE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public long readUInt32BE(int i) {
        checkIndex(i);
        return IO.readUInt32BE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public int readSInt32LE(int i) {
        checkIndex(i);
        return IO.readSInt32LE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public int readSInt32BE(int i) {
        checkIndex(i);
        return IO.readSInt32BE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    final public long readSInt64BE(int i) {
        checkIndex(i);
        return IO.readSInt64BE(_abSectorBytes, _iByteStartOffset + getHeaderDataSize() + i);
    }

    /** Helper function to ensure index is within the size of the sector data.
     * @throws IndexOutOfBoundsException If index is out of bounds of
     *                                   the sector data. */
    private void checkIndex(int i) {
        if (i < 0 || i >= getCdUserDataSize())
            throw new IndexOutOfBoundsException();
    }

}
