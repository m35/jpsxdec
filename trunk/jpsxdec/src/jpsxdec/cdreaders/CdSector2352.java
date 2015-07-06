/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import java.util.logging.Logger;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.Misc;


/** 2352 sectors are the size found in BIN/CUE disc images and include the
 *  full raw header with {@link CdxaHeader} and {@link CdxaSubHeader}. */
public class CdSector2352 extends CdSector {

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private final CdxaHeader _header;
    private final CdxaSubHeader _subHeader;
    
    // Following the header are either [2324 bytes]
    // or [2048 bytes] of user data (depending on the mode/form).
    // Following that are [4 bytes] Error Detection Code (EDC)
    // or just 0x00000000.
    // If the user data was 2048, then final [276 bytes] are error correction

    private final int _iUserDataOffset;
    private final int _iUserDataSize;
    
    public CdSector2352(byte[] abSectorBytes, int iByteStartOffset, int iSectorIndex, long lngFilePointer)
    {
        super(abSectorBytes, iByteStartOffset, iSectorIndex, lngFilePointer);
        _header = new CdxaHeader(abSectorBytes, iByteStartOffset);
        // TODO: if the sync header is imperfect (but passable), but the subheader is all errors -> it's cd audio
        switch (_header.getType()) {
            case CD_AUDIO:
                _subHeader = null;
                _iUserDataOffset = _iByteStartOffset;
                _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_CD_AUDIO;
                break;
            case MODE1:
                // mode 1 sectors & tracks
                _subHeader = null;
                _iUserDataOffset = _iByteStartOffset + _header.getSize();
                _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1;
                break;
            default: // mode 2
                _subHeader = new CdxaSubHeader(abSectorBytes, _iByteStartOffset + CdxaHeader.SIZE);
                _iUserDataOffset = _iByteStartOffset + _header.getSize() + _subHeader.getSize();
                if (_subHeader.getSubMode().getForm() == 1)
                    _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1;
                else
                    _iUserDataSize = CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM2;
                break;
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

    @Override
    public boolean isCdAudioSector() {
        return _header.getType() == CdxaHeader.Type.CD_AUDIO;
    }

    public boolean isMode1() {
        return _header.getType() == CdxaHeader.Type.MODE1;
    }

    @Override
    public boolean hasHeaderSectorNumber() {
        return _header.getType() != CdxaHeader.Type.CD_AUDIO;
    }

    @Override
    public int getHeaderSectorNumber() {
        return _header.calculateSectorNumber();
    }
    
    //..........................................................................
    
    @Override
    public boolean hasSubHeader() {
        return _subHeader != null;
    }

    @Override
    public int getSubHeaderChannel() {
        if (_subHeader == null)
            return super.getSubHeaderChannel();
        else
            return _subHeader.getChannel();
    }

    @Override
    public int getSubHeaderFile() {
        if (_subHeader == null)
            return super.getSubHeaderFile();
        else
            return _subHeader.getFileNumber();
    }

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
    public long getUserDataFilePointer() {
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
        switch (_header.getType()) {
            case CD_AUDIO:
                return String.format("[Sector:%d CD Audio]", _iSectorIndex);
            case MODE1:
                return String.format("[Sector:%d (%d) M1]", 
                        _iSectorIndex, 
                        _header.calculateSectorNumber());
            default:
                return String.format("[Sector:%d (%d) M2 %s]", 
                        _iSectorIndex, 
                        _header.calculateSectorNumber(), 
                        _subHeader);
        }
    }

    @Override
    public byte[] rebuildRawSector(byte[] abNewUserData) {
        CdxaHeader.Type eType = _header.getType();
        
        if (eType == CdxaHeader.Type.MODE1)
            throw new UnsupportedOperationException("Rebuilding error correction for mode 1 not supported.");

        if (abNewUserData.length != _iUserDataSize)
            throw new IllegalArgumentException();

        if (eType == CdxaHeader.Type.CD_AUDIO)
            return abNewUserData.clone();

        // MODE2 form 1 & 2
        byte[] abRawData = getRawSectorDataCopy();
        System.arraycopy(abNewUserData, 0, abRawData, _header.getSize() + _subHeader.getSize(), _iUserDataSize);
        CdSector2352.rebuildErrorCorrection(abRawData, getSubMode().getForm());
        
        return abRawData;
    }
    
    public static void rebuildErrorCorrection(byte[] abRawData, int iForm) {
        if (iForm == 1) {
            // Sets EDC
            long lngEdc = SectorErrorCorrection.generateErrorDetectionAndCorrection(abRawData, 0x10, 0x818);
            abRawData[0x818  ] = (byte)(lngEdc & 0xff);
            abRawData[0x818+1] = (byte)((lngEdc >>  8) & 0xff);
            abRawData[0x818+2] = (byte)((lngEdc >> 16) & 0xff);
            abRawData[0x818+3] = (byte)((lngEdc >> 24) & 0xff);

            // save the binary coded decimal sector number
            byte[] bcd = Misc.copyOfRange(abRawData, 12, 12+4);
            // fill the binary coded decimal sector number with zeros
            Arrays.fill(abRawData, 12, 12+4, (byte)0);
            // fill the ECC P and ECC Q with zeros
            Arrays.fill(abRawData, 0x81C, 0x8C8, (byte)0);
            Arrays.fill(abRawData, 0x8C8, 0x930, (byte)0);

            // rebuild ECC P+Q
            SectorErrorCorrection.generateErrorCorrectionCode_P(abRawData, 12/*to 12+2064*/, abRawData, 0x81C/*to 0x8C8*/);
            SectorErrorCorrection.generateErrorCorrectionCode_Q(abRawData, 12/*to 12+4+0x800+4+8+L2_P*/, abRawData, 0x8C8/*to 0x930*/);

            // restore the binary coded decimal sector number
            System.arraycopy(bcd, 0, abRawData, 12, bcd.length);
        } else { // form 2
            // Sets EDC
            long lngEdc = SectorErrorCorrection.generateErrorDetectionAndCorrection(abRawData, 0x10, 0x92C);
            abRawData[0x92C  ] = (byte)(lngEdc & 0xff);
            abRawData[0x92C+1] = (byte)((lngEdc >>  8) & 0xff);
            abRawData[0x92C+2] = (byte)((lngEdc >> 16) & 0xff);
            abRawData[0x92C+3] = (byte)((lngEdc >> 24) & 0xff);
        }
    }
    
}
