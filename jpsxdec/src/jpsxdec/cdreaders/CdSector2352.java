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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


/** 2352 sectors are the size found in BIN/CUE disc images and include the
 *  full raw header with {@link CdSectorHeader} and {@link CdSectorXaSubHeader}.
 */
public class CdSector2352 extends CdSector {

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    @CheckForNull
    private final CdSectorHeader _header;
    @CheckForNull
    private final CdSectorXaSubHeader _subHeader;

    // Following the header are either [2324 bytes]
    // or [2048 bytes] of user data (depending on the mode/form).
    // Following that are [4 bytes] Error Detection Code (EDC)
    // or just 0x00000000.
    // If the user data was 2048, then final [276 bytes] are error correction

    private final int _iHeaderSize;
    private final int _iUserDataSize;
    @Nonnull
    private final Type _type;

    public CdSector2352(int iSectorIndex, @Nonnull byte[] abSectorBytes,
                        int iByteStartOffset, int iFilePointer)
    {
        super(iSectorIndex, abSectorBytes, iByteStartOffset, iFilePointer);
        if (iByteStartOffset + SECTOR_SIZE_2352_BIN > abSectorBytes.length)
            throw new IllegalArgumentException();
        CdSectorHeader header = new CdSectorHeader(iSectorIndex, abSectorBytes, iByteStartOffset);
        // TODO: if the sync header is imperfect (but passable), but the subheader is all errors -> it's cd audio
        switch (header.getType()) {
            case CD_AUDIO:
                _header = null;
                _subHeader = null;
                _iHeaderSize = 0;
                _iUserDataSize = SECTOR_USER_DATA_SIZE_CD_AUDIO;
                _type = Type.CD_AUDIO;
                break;
            case MODE1:
                // mode 1 sectors & tracks
                _header = header;
                _subHeader = null;
                _iHeaderSize = CdSectorHeader.SIZEOF;
                _iUserDataSize = SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1;
                _type = Type.MODE1;
                break;
            default: assert header.getType() == CdSectorHeader.Type.MODE2;
                _header = header;
                _subHeader = new CdSectorXaSubHeader(iSectorIndex, abSectorBytes,
                                                     iByteStartOffset + CdSectorHeader.SIZEOF);
                _iHeaderSize = CdSectorHeader.SIZEOF + CdSectorXaSubHeader.SIZEOF;
                if (_subHeader.getSubMode().getForm() == 1) {
                    _iUserDataSize = SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1;
                    _type = Type.MODE2FORM1;
                } else {
                    _iUserDataSize = SECTOR_USER_DATA_SIZE_MODE2FORM2;
                    _type = Type.MODE2FORM2;
                }
                break;
        }
    }

    @Override
    public int getRawCdSectorSize() {
        return SECTOR_SIZE_2352_BIN;
    }

    @Override
    public int getCdUserDataSize() {
        return _iUserDataSize;
    }

    @Override
    protected int getHeaderDataSize() {
        return _iHeaderSize;
    }

    @Override
    public Type getType() {
        return _type;
    }
    @Override
    public boolean isCdAudioSector() {
        return _type == Type.CD_AUDIO;
    }

    @Override
    public @CheckForNull CdSectorHeader getHeader() {
        return _header;
    }

    @Override
    public @CheckForNull CdSectorXaSubHeader getSubHeader() {
        return _subHeader;
    }

    @Override
    public boolean hasHeaderErrors() {
        return (_header != null && _header.hasErrors()) ||
               (_subHeader != null && _subHeader.hasErrors());
    }

    @Override
    public int getErrorCount() {
        int iCount = 0;
        if (_header != null)
            iCount += _header.getErrorCount();
        if (_subHeader != null)
            iCount += _subHeader.getErrorCount();
        return iCount;
    }

    @Override
    public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abNewUserData) {
        if (_type == Type.MODE1)
            throw new UnsupportedOperationException("Rebuilding error correction for mode 1 not supported.");

        if (abNewUserData.length != _iUserDataSize)
            throw new IllegalArgumentException();

        if (_type == Type.CD_AUDIO)
            return abNewUserData.clone();

        // MODE2 form 1 & 2
        byte[] abRawData = getRawSectorDataCopy();
        System.arraycopy(abNewUserData, 0, abRawData, _iHeaderSize, _iUserDataSize);
        SectorErrorCorrection.rebuildErrorCorrection(abRawData, _subHeader.getSubMode().getForm());

        return abRawData;
    }

    @Override
    public String toString() {
        switch (_type) {
            case CD_AUDIO:
                return String.format("[Sector:%d CD Audio]", getSectorIndexFromStart());
            case MODE1:
                return String.format("[Sector:%d %s]", getSectorIndexFromStart(), getHeader());
            default: assert _type == Type.MODE2FORM1 || _type == Type.MODE2FORM2;
                return String.format("[Sector:%d %s %s]", getSectorIndexFromStart(), getHeader(), getSubHeader());
        }
    }

}
