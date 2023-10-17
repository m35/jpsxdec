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

package jpsxdec.modules.video.sectorbased;

import java.util.ArrayList;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.log.ILocalizedLogger;

/** Collects video sectors for a frame. */
public class SectorBasedFrameBuilder<T> {

    @Nonnull
    private final T[] _aoChunks;
    private int _iLastChunkReceived;

    private final int _iFrameStartSector;
    private int _iFrameEndSector;

    private final int _iHeaderFrameNumber;

    /** Start building the frame with the first sector. */
    public SectorBasedFrameBuilder(@Nonnull T firstChunk,
                                   int iChunkNumber, int iExpectedChunks,
                                   int iSector, int iHeaderFrameNumber,
                                   @Nonnull ILocalizedLogger log)
    {
        int iChunkCount;
        if (iChunkNumber < iExpectedChunks) {
            iChunkCount = iExpectedChunks;
        } else {
            // if this happens, the incoming data is pretty messed up
            // this logic will effectively immediately end the frame
            log.log(Level.WARNING,
                I.FRAME_NUM_CORRUPTED(String.valueOf(iHeaderFrameNumber)));
            iChunkCount = iChunkNumber + 1;
        }

        @SuppressWarnings("unchecked")
        T[] suppressed = (T[]) new Object[iChunkCount];
        _aoChunks = suppressed;

        _aoChunks[iChunkNumber] = firstChunk;
        _iLastChunkReceived = iChunkNumber;
        _iFrameStartSector = _iFrameEndSector = iSector;
        _iHeaderFrameNumber = iHeaderFrameNumber;
    }

    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }

    /** Adds another sector to this frame if possible.
     * @return if sector was accepted as part of this frame. */
    public boolean addSectorIfPartOfFrame(@Nonnull T chunk,
                                          int iChunkNumber, int iExpectedCount,
                                          int iSectorNumber, int iHeaderFrameNumber)
    {
        if (iSectorNumber < _iFrameStartSector)
            throw new IllegalArgumentException("Receiving sectors out of order?");
        boolean blnIsPartOfFrame =
                iHeaderFrameNumber == _iHeaderFrameNumber &&
                iChunkNumber > _iLastChunkReceived &&
                iChunkNumber < _aoChunks.length &&
                iExpectedCount == _aoChunks.length &&
                _aoChunks[iChunkNumber] == null &&
                // arbitrarily large value to stop adding sectors to this frame
                // saves frame rate detection from having to check too many sectors
                iSectorNumber < _iFrameEndSector + 50;

        if (!blnIsPartOfFrame)
            return false;

        _iFrameEndSector = iSectorNumber;
        _iLastChunkReceived = iChunkNumber;

        _aoChunks[iChunkNumber] = chunk;
        return true;
    }

    /** A frame is considered complete when the last chunk number received
     * is the last chunk in the frame. */
    public boolean isFrameComplete() {
        return _iLastChunkReceived >= _aoChunks.length - 1;
    }

    /** Returns the frame sectors, skipping null gaps. */
    public @Nonnull ArrayList<T> getNonNullChunks(@Nonnull ILocalizedLogger log) {
        ArrayList<T> nonNullChunks = new ArrayList<T>();

        for (int iChunk = 0; iChunk < _aoChunks.length; iChunk++) {
            T chunk = _aoChunks[iChunk];
            if (chunk == null) {
                log.log(Level.WARNING, I.MISSING_CHUNK_FRAME_IN_SECTORS(_iFrameStartSector, _iFrameEndSector, iChunk));
            } else {
                nonNullChunks.add(chunk);
            }
        }
        return nonNullChunks;
    }

    @Override
    public String toString() {
        return String.format("Sectors %d-%d %d/%d chunks",
                _iFrameStartSector, _iFrameEndSector,
                _iLastChunkReceived, _aoChunks.length);
    }
}
