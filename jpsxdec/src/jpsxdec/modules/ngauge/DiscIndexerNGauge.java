/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.ngauge;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DiscIndexerSectorBasedVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfo;
import jpsxdec.util.Fraction;

/** @see NGaugeVideoInfo */
public class DiscIndexerNGauge extends DiscIndexerSectorBasedVideo.SubIndexer implements NGaugeSectorToFrame.Listener {

    private class VideoBuilder {

        @Nonnull
        private final NGaugeVideoInfo _vidInfo;
        private final int _iFirstFrameLastSector;
        @Nonnull
        private final IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @Nonnull
        private final HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;

        private int _iEndSector;

        public VideoBuilder(@Nonnull NGaugeVideoInfo vidInfo, @Nonnull DemuxedNGaugeFrame firstFrame) {
            _vidInfo = vidInfo;
            _iEndSector = vidInfo.iFirstFrameStartSector;
            _iFirstFrameLastSector = firstFrame.getEndSector() - _vidInfo.iVideoStartSector;

            _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(firstFrame.getStartSector());
            _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(firstFrame.getCalculatedFrameNumber());
        }

        public void addFrame(@Nonnull DemuxedNGaugeFrame frame) {
            _iEndSector = frame.getEndSector();
            _indexSectorFrameNumberBuilder.addFrameStartSector(frame.getStartSector());
            _headerFrameNumberBuilder.addHeaderFrameNumber(frame.getCalculatedFrameNumber());
        }

        public void create() {
            addVideo(new DiscItemNGaugeVideo(getCd(),
                     _vidInfo.iVideoStartSector,
                     _iEndSector,
                     new Dimensions(_vidInfo.iWidth, _vidInfo.iHeight),
                     _indexSectorFrameNumberBuilder.makeFormat(),
                     _headerFrameNumberBuilder.makeFormat(),
                     new SectorBasedVideoInfo(new Fraction(_vidInfo.iSectorsPerFrame), _iFirstFrameLastSector)));
        }
    }

    @CheckForNull
    private NGaugeVideoInfo _videoInfo;
    @CheckForNull
    private VideoBuilder _currentVid;

    @Nonnull
    private final NGaugeSectorToFrame _s2f;

    public DiscIndexerNGauge() {
        _s2f = new NGaugeSectorToFrame(SectorRange.ALL, this);
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(_s2f);
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        if (DiscItemNGaugeVideo.TYPE_ID.equals(fields.getType()))
            return new DiscItemNGaugeVideo(getCd(), fields);
        return null;
    }

    @Override
    public void videoStart(@Nonnull NGaugeVideoInfo videoInfo) {
        videoBreak();
        _videoInfo = videoInfo;
    }


    @Override
    public void frameComplete(@Nonnull DemuxedNGaugeFrame frame, @Nonnull ILocalizedLogger log) {
        if (_videoInfo == null) {
            // received a frame when there's no video?
            // this would be a bug
            throw new IllegalStateException("Received NGauge frame when there's no video");
        } else {
            if (_currentVid == null) {
                _currentVid = new VideoBuilder(_videoInfo, frame);
            } else {
                _currentVid.addFrame(frame);
            }
        }
    }

    @Override
    public void videoBreak() {
        if (_currentVid != null) {
            _currentVid.create();
            _currentVid = null;
        }
        _videoInfo = null;
    }

    @Override
    public void endOfSectors() {
        videoBreak();
    }
}
