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

package jpsxdec.modules.square;

import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.SectorBasedDemuxedFrameWithNumberAndDims;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameBuilder;

public class FF9Demuxer implements ISelfDemuxingVideoSector.IDemuxer {

    @Nonnull
    private final SectorBasedFrameBuilder<SectorFF9.SectorFF9Video> _bldr;

    private final int _iWidth, _iHeight;

    public FF9Demuxer(@Nonnull SectorFF9.SectorFF9Video firstChunk, @Nonnull ILocalizedLogger log) {
        _bldr = new SectorBasedFrameBuilder<SectorFF9.SectorFF9Video>(firstChunk,
                            firstChunk.getInvertedVideoChunkNumber(), firstChunk.getVideoChunksInFrame(),
                            firstChunk.getSectorNumber(), firstChunk.getHeaderFrameNumber(),
                            log);
        _iWidth = firstChunk.getWidth();
        _iHeight = firstChunk.getHeight();
    }

    @Override
    public boolean addSectorIfPartOfFrame(@Nonnull ISelfDemuxingVideoSector sector) {
        if (!(sector instanceof SectorFF9.SectorFF9Video))
            return false;
        return addFF9SectorIfPartOfFrame((SectorFF9.SectorFF9Video)sector);
    }

    public boolean addFF9SectorIfPartOfFrame(@Nonnull SectorFF9.SectorFF9Video chunk) {
        return chunk.getWidth() == _iWidth && chunk.getHeight() == _iHeight &&
               _bldr.addSectorIfPartOfFrame(chunk, chunk.getInvertedVideoChunkNumber(),
                                            chunk.getVideoChunksInFrame(),
                                            chunk.getSectorNumber(),
                                            chunk.getHeaderFrameNumber());
    }

    @Override
    public boolean isFrameComplete() {
        return _bldr.isFrameComplete();
    }

    @Override
    public @Nonnull SectorBasedDemuxedFrameWithNumberAndDims finishFrame(@Nonnull ILocalizedLogger log) {
        ArrayList<SectorFF9.SectorFF9Video> sectors = _bldr.getNonNullChunks(log);
        // FF9 frames chunks are in reverse order
        Collections.reverse(sectors);
        return new SectorBasedDemuxedFrameWithNumberAndDims(_iWidth, _iHeight,
                                                            _bldr.getHeaderFrameNumber(),
                                                            sectors);
    }
}
