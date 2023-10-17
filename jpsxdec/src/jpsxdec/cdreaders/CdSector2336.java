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

import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


/** 2336 sectors only include the raw {@link CdSectorXaSubHeader}, but not the
 *  {@link CdSectorHeader}. */
public class CdSector2336 extends CdSector {

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Nonnull
    private final CdSectorXaSubHeader _subHeader;
    private final int _iUserDataSize;
    @Nonnull
    private final Type _type;

    public CdSector2336(int iSectorIndex, @Nonnull byte[] abSectorBytes,
                        int iByteStartOffset, int iFilePointer)
    {
        super(iSectorIndex, abSectorBytes, iByteStartOffset, iFilePointer);
        if (iByteStartOffset + SECTOR_SIZE_2336_BIN_NOSYNC > abSectorBytes.length)
            throw new IllegalArgumentException();
        _subHeader = new CdSectorXaSubHeader(iSectorIndex, abSectorBytes, iByteStartOffset);
        if (_subHeader.getSubMode().getForm() == 1) {
            _iUserDataSize = SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1;
            _type = Type.MODE2FORM1;
        } else {
            _iUserDataSize = SECTOR_USER_DATA_SIZE_MODE2FORM2;
            _type = Type.MODE2FORM2;
        }
    }

    @Override
    public int getRawCdSectorSize() {
        return SECTOR_SIZE_2336_BIN_NOSYNC;
    }

    @Override
    public int getCdUserDataSize() {
        return _iUserDataSize;
    }

    @Override
    protected int getHeaderDataSize() {
        return CdSectorXaSubHeader.SIZEOF;
    }

    @Override
    public @Nonnull Type getType() {
        return _type;
    }
    @Override
    public boolean isCdAudioSector() {
        return false;
    }

    @Override
    public @CheckForNull CdSectorHeader getHeader() {
        return null;
    }
    @Override
    public @Nonnull CdSectorXaSubHeader getSubHeader() {
        return _subHeader;
    }

    @Override
    public boolean hasHeaderErrors() {
        return _subHeader.hasErrors();
    }

    @Override
    public int getErrorCount() {
        return _subHeader.getErrorCount();
    }

    @Override
    public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abNewUserData) {
        byte[] abRawData = getRawSectorDataCopy();
        System.arraycopy(abNewUserData, 0, abRawData, CdSectorXaSubHeader.SIZEOF, _iUserDataSize);
        Logger.getLogger(CdSector2336.class.getName()).info("No need to rebuild 2336 EDC");

        return abRawData;
    }


    @Override
    public String toString() {
        return String.format("[Sector:%d M2 %s]", getSectorIndexFromStart(), getSubHeader());
    }

}
