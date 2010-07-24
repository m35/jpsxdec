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

package jpsxdec.cdreaders;

import java.util.logging.Logger;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.NotThisTypeException;


/** Represents a single sector on a CD. */
public class CdSector2336 extends CdSector {

    private static final Logger log = Logger.getLogger(CdSector2336.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private final CdxaSubHeader _subHeader;
    private final int _iUserDataOffset;
    private final int _iUserDataSize;

    public CdSector2336(byte[] abSectorBytes, int iByteStartOffset, int iSectorIndex, long lngFilePointer, int iTolerance)
            throws NotThisTypeException
    {
        super(abSectorBytes, iByteStartOffset, iSectorIndex, lngFilePointer);
        try {
            _subHeader = new CdxaSubHeader(abSectorBytes, iByteStartOffset, iTolerance);
            _iUserDataOffset = _iByteStartOffset + _subHeader.getSize();
            if (_subHeader.getSubMode().getForm() == 1)
                _iUserDataSize = CDFileSectorReader.SECTOR_USER_DATA_SIZE_MODE1;
            else
                _iUserDataSize = CDFileSectorReader.SECTOR_USER_DATA_SIZE_MODE2;
        } catch (NotThisTypeException ex) {
            throw new NotThisTypeException("Sector " + iSectorIndex + " " + ex.getMessage());
        }
    }

    /** Returns the size of the 'user data' portion of the sector. */
    public int getCdUserDataSize() {
        return _iUserDataSize;
    }
    
    public byte readUserDataByte(int i) {
        return _abSectorBytes[_iUserDataOffset + i];
    }

    /** Returns copy of the 'user data' portion of the sector. */
    public byte[] getCdUserDataCopy() {
        byte[] ab = new byte[_iUserDataSize];
        getCdUserDataCopy(0, ab, 0, _iUserDataSize);
        return ab;
    }

    public void getCdUserDataCopy(int iSourcePos, byte[] abOut, int iOutPos, int iLength) {
        if (iLength > _iUserDataSize) throw new IndexOutOfBoundsException();
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
        byte[] ab = new byte[CDFileSectorReader.SECTOR_SIZE_2336_BIN_NOSYNC];
        System.arraycopy(_abSectorBytes, _iByteStartOffset, ab, 0, ab.length);
        return ab;
    }


    //..........................................................................
    
    public boolean hasSectorHeader() {
        return true;
    }

    //..........................................................................
    
    public int getFile() {
        return _subHeader.getFileNumber();
    }

    public int getChannel() {
        return _subHeader.getChannel();
    }

    //..........................................................................

    @Override
    public CdxaSubHeader.SubMode getSubMode() {
        return _subHeader.getSubMode();
    }

    public CdxaSubHeader.CodingInfo getCodingInfo() {
        return _subHeader.getCodingInfo();
    }

    /** Returns the coding info.bits_per_sample in the sector header,
     *  or -1 if it has no header. */
    public int getCodingInfo_BitsPerSample() {
        return _subHeader.getCodingInfo().getBitsPerSample();
    }

    /** Returns 0 for mono, 1 for stereo, or -1 if it has no header. */
    public boolean getCodingInfo_MonoStereo() {
        return _subHeader.getCodingInfo().isStereo();
    }

    /** Returns 37800 or 18900, or -1 if it has no header. */
    public int getCodingInfo_SampleRate() {
        return _subHeader.getCodingInfo().getSampleRate();
    }

    //..........................................................................
    
    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of the sector userdata.
     *  [implements IGetFilePointer] */
    public long getFilePointer() {
        return _lngFilePointer + _subHeader.getSize();
    }
    
    public String toString() {
        return String.format("[Sector:%d %s]", _iSectorIndex, _subHeader.toString());
    }
}
