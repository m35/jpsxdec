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

package jpsxdec.modules.granturismo;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.SectorBasedDemuxedFrameWithNumberAndDims;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameBuilder;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Iki;
import jpsxdec.util.DemuxedData;

/** Constructs a single frame. Create a new instance for each frame. */
public class GranTurismoDemuxer implements ISelfDemuxingVideoSector.IDemuxer {

    private static final Logger LOG = Logger.getLogger(GranTurismoDemuxer.class.getName());

    @Nonnull
    private final SectorBasedFrameBuilder<SectorGTVideo> _bldr;

    /** Must have the first (0) chunk to know the frame dimensions. */
    public GranTurismoDemuxer(@Nonnull SectorGTVideo chunk0, @Nonnull ILocalizedLogger log) {
        if (chunk0.getChunkNumber() != 0)
            throw new IllegalArgumentException();
        _bldr = new SectorBasedFrameBuilder<SectorGTVideo>(chunk0,
                            chunk0.getChunkNumber(), chunk0.getChunksInFrame(),
                            chunk0.getSectorNumber(), chunk0.getHeaderFrameNumber(),
                            log);
    }

    @Override
    public boolean addSectorIfPartOfFrame(@Nonnull ISelfDemuxingVideoSector sector) {
        if (!(sector instanceof SectorGTVideo))
            return false;
        return addGTSectorIfPartOfFrame((SectorGTVideo)sector);
    }

    public boolean addGTSectorIfPartOfFrame(@Nonnull SectorGTVideo chunk) {
        return _bldr.addSectorIfPartOfFrame(chunk,
                chunk.getChunkNumber(), chunk.getChunksInFrame(),
                chunk.getSectorNumber(), chunk.getHeaderFrameNumber());
    }

    @Override
    public boolean isFrameComplete() {
        return _bldr.isFrameComplete();
    }

    /** Returns null if the sector data somehow didn't contain a recognizable frame.
     * Discard this object after this function is called. */
    @Override
    public @CheckForNull SectorBasedDemuxedFrameWithNumberAndDims finishFrame(@Nonnull ILocalizedLogger log) {
        List<SectorGTVideo> sectors = _bldr.getNonNullChunks(log);
        DemuxedData<SectorGTVideo> demux = new DemuxedData<SectorGTVideo>(sectors);
        byte[] abBitstream = demux.copyDemuxData();
        // extract the frame dimensions from the iki bitstream header
        BitStreamUncompressor_Iki.IkiHeader header =
                BitStreamUncompressor_Iki.makeIkiHeader(abBitstream, abBitstream.length);
        if (header == null) {
            LOG.log(Level.WARNING, "Invalid GT header frame {0}", _bldr.getHeaderFrameNumber());
            return null;
        } else {
            return new SectorBasedDemuxedFrameWithNumberAndDims(header.getWidth(), header.getHeight(),
                                                                _bldr.getHeaderFrameNumber(), sectors);
        }
    }
}
