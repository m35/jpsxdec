/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2020  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.DemuxedData;

/** Crusader audio/video streams are found in a contiguous series of sectors.
 * This wraps each sector in a {@link DemuxedData} piece for demuxing.
 * It also handles the case where a sector is missing and essentially
 * just returns a sector full of 0. */
public class CrusaderDemuxPiece implements DemuxedData.Piece {

    private static final int CRUSADER_USERDATA_SIZE = 2040;

    @CheckForNull
    private final SectorCrusader _sector;
    private final int _iCdSectorNumber;

    public CrusaderDemuxPiece(@Nonnull SectorCrusader sector) {
        _sector = sector;
        _iCdSectorNumber = _sector.getSectorNumber();
    }

    public CrusaderDemuxPiece(int iCdSectorNumber) {
        _sector = null;
        _iCdSectorNumber = iCdSectorNumber;
    }
    
    public int getDemuxPieceSize() {
        if (_sector == null)
            return CRUSADER_USERDATA_SIZE;
        else
            return _sector.getIdentifiedUserDataSize();
    }
    public byte getDemuxPieceByte(int i) {
        if (_sector == null)
            return 0;
        else
            return _sector.readIdentifiedUserDataByte(i);
    }

    public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        if (_sector != null)
            _sector.copyIdentifiedUserData(0, abOut, iOutPos, _sector.getIdentifiedUserDataSize());
        else
            Arrays.fill(abOut, iOutPos, iOutPos + CRUSADER_USERDATA_SIZE, (byte)0);
    }

    public @CheckForNull SectorCrusader getSector() {
        return _sector;
    }

    public int getSectorNumber() {
        return _iCdSectorNumber;
    }

    @Override
    public String toString() {
        if (_sector != null)
            return _sector.toString();
        else
            return "Crusader [CD sector "  + _iCdSectorNumber + "] missing";
    }
}
