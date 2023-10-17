/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.ngauge;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IdentifiedSector;

/** Sector before the video. Contains a 16 byte header followed by all zeroes.
 * @see NGaugeVideoInfo */
public class SectorNGaugeVideoHeader extends IdentifiedSector {

    @Nonnull
    private NGaugeVideoInfo _vidInfo;

    public SectorNGaugeVideoHeader(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset())
            return;

        if (cdSector.isCdAudioSector())
            return;

        if (cdSector.getCdUserDataSize() != CdSector.SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1)
            return;

        _vidInfo = NGaugeVideoInfo.checkForInfoHeader(cdSector);
        if (_vidInfo == null)
            return;

        for (int i = 16; i < cdSector.getCdUserDataSize(); i++) {
            if (cdSector.readUserDataByte(i) != 0)
                return;
        }

        setProbability(100);
    }

    public @Nonnull NGaugeVideoInfo getVidInfo() {
        return _vidInfo;
    }

    @Override
    public @Nonnull String getTypeName() {
        return "N-Gauge Vid Header";
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", getTypeName(), cdToString(), _vidInfo);
    }

}
