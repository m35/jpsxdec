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

package jpsxdec.modules.ac3;

import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameBuilder;

/** Collects Ace Combat 3 sectors of the same format/channel and generates a
 * single frame. Once the frame is generated, a new demuxer should be created
 * to demux the next frame. */
public class Ac3Demuxer {

    private final int _iWidth, _iHeight;
    private final int _iChannel;
    @Nonnull
    private final SectorBasedFrameBuilder<SectorAceCombat3Video> _bldr;

    public Ac3Demuxer(SectorAceCombat3Video firstChunk, @Nonnull ILocalizedLogger log) {
        _iWidth = firstChunk.getWidth();
        _iHeight = firstChunk.getHeight();
        _iChannel = firstChunk.getChannel();
        _bldr = new SectorBasedFrameBuilder<SectorAceCombat3Video>(firstChunk,
                firstChunk.getChunkNumber(), firstChunk.getChunksInFrame(),
                firstChunk.getSectorNumber(), firstChunk.getInvertedFrameNumber(),
                log);
    }

    public @Nonnull Ac3AddResult addSector(@Nonnull SectorAceCombat3Video chunk) {
        if (chunk.getChannel() != _iChannel)
            return Ac3AddResult.WrongChannel;

        boolean blnIsPartOfFrame = chunk.getWidth() == _iWidth &&
                                   chunk.getHeight() == _iHeight &&
                                   _bldr.addSectorIfPartOfFrame(chunk,
                                         chunk.getChunkNumber(), chunk.getChunksInFrame(),
                                         chunk.getSectorNumber(), chunk.getInvertedFrameNumber());
        if (!blnIsPartOfFrame)
            return Ac3AddResult.WrongFormat;

        return Ac3AddResult.Same;
    }

    public boolean isFrameComplete() {
        return _bldr.isFrameComplete();
    }

    public @Nonnull DemuxedAc3Frame finishFrame(@Nonnull ILocalizedLogger log) {
        return new DemuxedAc3Frame(_iWidth, _iHeight, _bldr.getHeaderFrameNumber(),
                                   _iChannel, _bldr.getNonNullChunks(log));
    }
}

