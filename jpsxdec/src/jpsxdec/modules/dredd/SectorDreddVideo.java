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

package jpsxdec.modules.dredd;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.DemuxedData;

/** Judge Dredd video sector.
 * <p>
 * Judge Dredd does not make video sector identification easy. Requires
 * contextual information about the surrounding sectors to
 * uniquely determine if a sector really is a Dredd video sector. */
public class SectorDreddVideo extends IdentifiedSector
        implements DemuxedData.Piece, SectorBasedFrameReplace.IReplaceableVideoSector
{

    /** Frame chunk number from first 4 bytes. */
    private final int _iChunk;
    /** Dredd sector header size is either 4 or 44. */
    private final int _iHeaderSize;

    @Override
    public int getVideoSectorHeaderSize() { return _iHeaderSize; }

    @CheckForNull
    DemuxedDreddFrame _dreddFrame;

    /** Performs initial, partial sector identification.
     * Additional verification is necessary which requires contextual information. */
    public SectorDreddVideo(@Nonnull CdSector cdSector, int iChunk, int iHeaderSize) {
        super(cdSector);

        _iChunk = iChunk;
        _iHeaderSize = iHeaderSize;
    }

    @Override
    public @Nonnull String getTypeName() {
        return "Dredd";
    }

    public int getChunkNumber() {
        return _iChunk;
    }

    public int getChunksInFrame() {
        if (_dreddFrame == null)
            throw new IllegalStateException();
        return _dreddFrame.getSectorCount();
    }

    @Override
    final public int getDemuxPieceSize() {
        return getCdSector().getCdUserDataSize() - getVideoSectorHeaderSize();
    }

    @Override
    final public byte getDemuxPieceByte(int i) {
        return getCdSector().readUserDataByte(getVideoSectorHeaderSize() + i);
    }

    @Override
    final public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        getCdSector().getCdUserDataCopy(getVideoSectorHeaderSize(), abOut,
                iOutPos, getDemuxPieceSize());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
            String.format("%s %s chunk %d", getTypeName(), cdToString(), getChunkNumber())
        );

        if (_dreddFrame != null) {
            sb.append(" frame ").append(_dreddFrame);
        }

        if (_iHeaderSize != 4)
            sb.append(" + Unknown data");

        return sb.toString();
    }

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
    {
        // nothing to replace
    }

}
