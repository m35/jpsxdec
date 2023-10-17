/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.policenauts;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.Misc;

/** @see SPacket */
public class SectorPN_KLBS extends SectorPolicenauts {

    public static final int SIZEOF_KLBS_HEADER = 32;
    public static final int KLBS_SECTOR_COUNT = 128;

    // Zeroes                  // 8 bytes  @0
    // "KLBS"                  // 4 bytes  @8
    private int _iSize;        // 4 bytes  @12 always = 262144 = 128 * 2048
    private int _iEntryCount;  // 4 bytes  @16
    // Entry count again       // 4 bytes  @20
    // Zeroes                  // 8 bytes  @24
    // ... after this are _iEntryCount SPackets

    public SectorPN_KLBS(@Nonnull CdSector cdSector) {
        super(cdSector, false);
        if (isSuperInvalidElseReset()) return;

        for (int i = 0; i < 8; i++) {
            if (cdSector.readUserDataByte(i) != 0) return;
        }

        byte[] abKlbs = new byte[4];
        cdSector.getCdUserDataCopy(8, abKlbs, 0, 4);
        String sKlbs = Misc.asciiToString(abKlbs);
        if (!"KLBS".equals(sKlbs)) return;

        _iSize = cdSector.readSInt32LE(12);
        if (_iSize != KLBS_SECTOR_COUNT * CdSector.SECTOR_SIZE_2048_ISO /*262144*/)
            return;
        _iEntryCount = cdSector.readSInt32LE(16);
        if (_iEntryCount < 1 || _iEntryCount > 83)
            return;
        int iEntryCount2 = cdSector.readSInt32LE(20);
        if (iEntryCount2 != _iEntryCount) return;
        for (int i = 24; i < SIZEOF_KLBS_HEADER; i++) {
            if (cdSector.readUserDataByte(i) != 0) return;
        }

        setProbability(100);
    }

    public int getSectorLength() {
        return KLBS_SECTOR_COUNT;
    }

    public int getEndSectorInclusive() {
        return getSectorNumber() + KLBS_SECTOR_COUNT - 1;
    }

    public int getEntryCount() {
        return _iEntryCount;
    }

    @Override
    public String getTypeName() {
        return "Policenauts KLBS";
    }

    @Override
    public String toString() {
        return String.format("%s %s size:%d entries:%d", getTypeName(), super.toString(), _iSize, _iEntryCount);
    }
}
