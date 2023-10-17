/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.util;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;

/** Demuxes multiple pieces of data into a single piece of data.
 * Does not allow for null pieces, so either skip missing pieces,
 * or insert a dummy object indicating a piece is missing.
 * This class should be a blackbox to other other objects.
 * The only exception is the need to return the original pieces of data for the
 * purpose of replacing. */
public class DemuxedData<T extends DemuxedData.Piece> implements Iterable<T> {

    /** A piece (sector) of data. */
    public interface Piece {
        int getDemuxPieceSize();
        byte getDemuxPieceByte(int i);
        void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos);
        /** The CD sector number. */
        int getSectorNumber();
    }

    @Nonnull
    private final List<T> _pieces;

    private final int _iStartSector;
    private final int _iEndSector;

    private final int _iStartDataOffset;
    private final int _iEndDataOffset;
    private final int _iDataSize;

    /** No nulls allowed. Must have at least 1 piece. */
    public DemuxedData(@Nonnull List<T> pieces) {
        this(pieces, 0,
             pieces.isEmpty() ? -1 : pieces.get(pieces.size() - 1).getDemuxPieceSize());
    }
    /** No nulls allowed. Must have at least 1 piece. */
    public DemuxedData(@Nonnull List<T> pieces,
                       int iStartPieceOffset, int iEndPieceOffset)
    {
        if (pieces.isEmpty())
            throw new IllegalArgumentException();
        if (pieces.contains(null))
            throw new IllegalArgumentException();
        if (pieces.size() == 1) {
            if (iStartPieceOffset > iEndPieceOffset)
                throw new IllegalArgumentException();
            if (iStartPieceOffset < 0)
                throw new IllegalArgumentException();
            if (iEndPieceOffset > pieces.get(pieces.size()-1).getDemuxPieceSize())
                throw new IllegalArgumentException();
        } else {
            if (iStartPieceOffset < 0 || iEndPieceOffset > pieces.get(pieces.size()-1).getDemuxPieceSize())
                throw new IllegalArgumentException();
            if (iStartPieceOffset >= pieces.get(0).getDemuxPieceSize() || iEndPieceOffset <= 0)
                throw new IllegalArgumentException("Trim the pieces first");
        }

        _pieces = pieces;

        _iStartDataOffset = iStartPieceOffset;
        _iEndDataOffset = iEndPieceOffset;


        int iSize = 0;
        int iStartSector = Integer.MAX_VALUE;
        int iEndSector = Integer.MIN_VALUE;
        for (int iPiece = 0; iPiece < _pieces.size(); iPiece++) {
            T piece = _pieces.get(iPiece);
            // use only the part we care about
            int iStart = 0;
            if (iPiece == 0)
                iStart = _iStartDataOffset;
            int iEnd = piece.getDemuxPieceSize();
            if (iPiece == _pieces.size() - 1)
                iEnd = _iEndDataOffset;
            iSize += iEnd - iStart;
            int iSectorNumber = piece.getSectorNumber();
            if (iSectorNumber < iStartSector)
                iStartSector = iSectorNumber;
            if (iSectorNumber > iEndSector)
                iEndSector = iSectorNumber;
        }

        _iStartSector = iStartSector;
        _iEndSector = iEndSector;
        _iDataSize = iSize;
    }

    public int getPieceCount() {
        return _pieces.size();
    }
    public int getStartSector() {
        return _iStartSector;
    }
    public int getEndSector() {
        return _iEndSector;
    }

    /** Returns in the order of data, not necessarily in the order of sectors. */
    // TODO find some way to remove iterator here
    // it's used for printing the sectors and replacing them mainly I think
    @Override
    public @Nonnull Iterator<T> iterator() {
        return _pieces.iterator();
    }

    public int getStartDataOffset() {
        return _iStartDataOffset;
    }

    public int getEndDataOffset() {
        return _iEndDataOffset;
    }

    public int getDemuxSize() {
        return _iDataSize;
    }

    public @Nonnull byte[] copyDemuxData() {
        // use ExposedBAOS to avoid IOExceptions
        ExposedBAOS baos = new ExposedBAOS(getDemuxSize());

        byte[] abCopyBuff = null;
        for (int iPiece = 0; iPiece < _pieces.size(); iPiece++) {
            T piece = _pieces.get(iPiece);
            if (abCopyBuff == null || abCopyBuff.length < piece.getDemuxPieceSize())
                abCopyBuff = new byte[piece.getDemuxPieceSize()];
            piece.copyDemuxPieceData(abCopyBuff, 0); // copy out the whole data
            // use only the part we care about
            int iStart = 0;
            if (iPiece == 0)
                iStart = _iStartDataOffset;
            int iEnd = piece.getDemuxPieceSize();
            if (iPiece == _pieces.size() - 1)
                iEnd = _iEndDataOffset;
            baos.write(abCopyBuff, iStart, iEnd - iStart);
        }
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("Start sector %d@%d [%d pieces] End sector %d@%d = %d",
                _iStartSector, _iStartDataOffset, _pieces.size(),
                _iEndSector, _iEndDataOffset, _iDataSize);
    }
}
