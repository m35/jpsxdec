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


/** 2048 sectors are standard .iso size that excludes any raw header info. */
public class CdSector2048 extends CdSector {

    public CdSector2048(int iSectorIndex, @Nonnull byte[] abSectorBytes,
                        int iByteStartOffset, int iFilePointer)
    {
        super(iSectorIndex, abSectorBytes, iByteStartOffset, iFilePointer);
        if (iByteStartOffset + SECTOR_SIZE_2048_ISO > abSectorBytes.length)
            throw new IllegalArgumentException();
    }

    @Override
    public int getRawCdSectorSize() {
        return SECTOR_SIZE_2048_ISO;
    }

    @Override
    public int getCdUserDataSize() {
        return SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1;
    }

    @Override
    protected int getHeaderDataSize() {
        return 0;
    }

    @Override
    public @Nonnull Type getType() {
        return Type.UNKNOWN2048;
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
    public @CheckForNull CdSectorXaSubHeader getSubHeader() {
        return null;
    }

    @Override
    public boolean hasHeaderErrors() {
        return false;
    }

    @Override
    public int getErrorCount() {
        return 0;
    }

    @Override
    public @Nonnull byte[] rebuildRawSector(@Nonnull byte[] abUserData) {
        return abUserData.clone();
    }

    @Override
    public String toString() {
        return String.format("[Sector:%d 2048]", getSectorIndexFromStart());
    }

}
