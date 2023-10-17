/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2018-2023  Michael Sabin
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

package jpsxdec.modules;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.DemuxedData;

/** Wraps a {@link CdSector} so it can be used as a demux piece. */
public class CdSectorDemuxPiece implements DemuxedData.Piece {
    @Nonnull
    private final CdSector _sector;
    private final int _iStartOffset;
    private final int _iEndOffset;

    public CdSectorDemuxPiece(@Nonnull CdSector sector) {
        this(sector, 0, sector.getCdUserDataSize());
    }
    public CdSectorDemuxPiece(@Nonnull CdSector sector, int iStartOffset) {
        this(sector, iStartOffset, sector.getCdUserDataSize());
    }
    public CdSectorDemuxPiece(@Nonnull CdSector sector, int iStartOffset, int iEndOffset) {
        if (iStartOffset < 0 || iEndOffset > sector.getCdUserDataSize())
            throw new IllegalArgumentException();
        _sector = sector;
        _iStartOffset = iStartOffset;
        _iEndOffset = iEndOffset;
    }

    @Override
    public int getDemuxPieceSize() {
        return _iEndOffset - _iStartOffset;
    }

    @Override
    public byte getDemuxPieceByte(int i) {
        if (i < 0 || _iStartOffset + i >= _iEndOffset)
            throw new IndexOutOfBoundsException();
        return _sector.readUserDataByte(_iStartOffset+i);
    }
    @Override
    public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        _sector.getCdUserDataCopy(_iStartOffset, abOut, iOutPos, getDemuxPieceSize());
    }

    @Override
    public int getSectorNumber() {
        return _sector.getSectorIndexFromStart();
    }

    public @Nonnull CdSector getSector() {
        return _sector;
    }

    @Override
    public String toString() {
        return String.format("%d-%d %s", _iStartOffset, _iEndOffset, _sector);
    }
}
