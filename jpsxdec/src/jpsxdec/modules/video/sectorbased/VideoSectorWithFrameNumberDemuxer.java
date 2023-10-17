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
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.DemuxedData;


public class VideoSectorWithFrameNumberDemuxer implements ISelfDemuxingVideoSector.IDemuxer {

    @Nonnull
    private final SectorBasedFrameBuilder<IVideoSectorWithFrameNumber> _bldr;

    private final int _iWidth, _iHeight;

    public VideoSectorWithFrameNumberDemuxer(@Nonnull IVideoSectorWithFrameNumber firstChunk,
                                             @Nonnull ILocalizedLogger log)
    {
        _bldr = new SectorBasedFrameBuilder<IVideoSectorWithFrameNumber>(firstChunk,
                            firstChunk.getChunkNumber(), firstChunk.getChunksInFrame(),
                            firstChunk.getSectorNumber(), firstChunk.getHeaderFrameNumber(),
                            log);
        _iWidth = firstChunk.getWidth();
        _iHeight = firstChunk.getHeight();
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getHeaderFrameNumber() {
        return _bldr.getHeaderFrameNumber();
    }

    @Override
    public boolean addSectorIfPartOfFrame(@Nonnull ISelfDemuxingVideoSector sector) {
        // would be nice if we could make this builder generic, but there's no way
        // of knowing if this sector is a subclass of <T> at run time
        if (!(sector instanceof IVideoSectorWithFrameNumber))
            return false;
        IVideoSectorWithFrameNumber chunk = (IVideoSectorWithFrameNumber) sector;
        return addSectorWithFameNumberIfPartOfFrame(chunk);
    }

    public boolean addSectorWithFameNumberIfPartOfFrame(@Nonnull IVideoSectorWithFrameNumber chunk) {
        return chunk.getWidth() == _iWidth && chunk.getHeight() == _iHeight &&
               _bldr.addSectorIfPartOfFrame(chunk,
               chunk.getChunkNumber(), chunk.getChunksInFrame(),
               chunk.getSectorNumber(), chunk.getHeaderFrameNumber());
    }

    @Override
    public boolean isFrameComplete() {
        return _bldr.isFrameComplete();
    }

    final protected @Nonnull List<IVideoSectorWithFrameNumber> getNonNullChunks(@Nonnull ILocalizedLogger log) {
        return _bldr.getNonNullChunks(log);
    }

    @Override
    public @Nonnull SectorBasedDemuxedFrameWithNumberAndDims finishFrame(@Nonnull ILocalizedLogger log) {
        List<IVideoSectorWithFrameNumber> sectors = getNonNullChunks(log);

        // need to wrap the sectors in something compatible with IReplaceableVideoSector
        List<SectorBasedFrameReplace.IReplaceableVideoSector> wrappedSectors =
                new ArrayList<SectorBasedFrameReplace.IReplaceableVideoSector>(sectors.size());

        for (IVideoSectorWithFrameNumber vidSector : sectors) {
            wrappedSectors.add(new VideoSectorReplaceableDemuxPiece(vidSector));
        }

        return new SectorBasedDemuxedFrameWithNumberAndDims(_iWidth, _iHeight,
                                                            _bldr.getHeaderFrameNumber(),
                                                            wrappedSectors);
    }

    /** {@link IVideoSectorWithFrameNumber} is used for a variety of video sector types
     * of similar nature. But we need them to implement both {@link DemuxedData.Piece}
     * and {@link SectorBasedFrameReplace.IReplaceableVideoSector}, so we wrap
     * them here to bridge the gap. */
    private static class VideoSectorReplaceableDemuxPiece
            implements DemuxedData.Piece, SectorBasedFrameReplace.IReplaceableVideoSector
    {
        @Nonnull
        private final IVideoSectorWithFrameNumber _vidSector;

        public VideoSectorReplaceableDemuxPiece(@Nonnull IVideoSectorWithFrameNumber vidSector) {
            _vidSector = vidSector;
        }

        @Override
        public int getDemuxPieceSize() {
            return _vidSector.getDemuxPieceSize();
        }

        @Override
        public byte getDemuxPieceByte(int i) {
            return _vidSector.getDemuxPieceByte(i);
        }

        @Override
        public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
            _vidSector.copyDemuxPieceData(abOut, iOutPos);
        }

        @Override
        public int getSectorNumber() {
            return _vidSector.getSectorNumber();
        }

        @Override
        public int getVideoSectorHeaderSize() {
            return _vidSector.getVideoSectorHeaderSize();
        }

        @Override
        public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                             @Nonnull BitStreamAnalysis newFrame,
                                             @Nonnull byte[] abCurrentVidSectorHeader)
                throws LocalizedIncompatibleException
        {
            _vidSector.replaceVideoSectorHeader(existingFrame, newFrame, abCurrentVidSectorHeader);
        }

        @Override
        public @Nonnull CdSector getCdSector() {
            return _vidSector.getCdSector();
        }

        @Override
        public String toString() {
            return "VideoSectorReplaceableDemuxPiece[" + _vidSector + "]";
        }
    }

}
