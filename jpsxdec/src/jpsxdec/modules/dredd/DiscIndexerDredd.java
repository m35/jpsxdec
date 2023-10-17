/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DiscIndexerSectorBasedVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfoBuilder;


public class DiscIndexerDredd extends DiscIndexerSectorBasedVideo.SubIndexer
        implements DreddSectorToDreddFrame.Listener
{

    private static class VidBuilder {

        @Nonnull
        private final IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @Nonnull
        private final SectorBasedVideoInfoBuilder _strInfoBuilder;

        public VidBuilder(@Nonnull DemuxedDreddFrame firstFrame) {
            _strInfoBuilder = new SectorBasedVideoInfoBuilder(
                    firstFrame.getWidth(), firstFrame.getHeight(),
                    firstFrame.getStartSector(), firstFrame.getEndSector());
            _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(firstFrame.getStartSector());
        }

        /** @return if the frame was accepted as part of this video, otherwise start a new video. */
        public boolean addFrame(@Nonnull DemuxedDreddFrame frame) {
            if (frame.getWidth() != _strInfoBuilder.getWidth() ||
                frame.getHeight() != _strInfoBuilder.getHeight() ||
                frame.getStartSector() > _strInfoBuilder.getEndSector() + 100)
            {
                return false;
            }
            _strInfoBuilder.next(frame.getStartSector(), frame.getEndSector());
            _indexSectorFrameNumberBuilder.addFrameStartSector(frame.getStartSector());
            return true;
        }

        public @Nonnull DiscItemDreddVideoStream endOfMovie(@Nonnull ICdSectorReader cd) {
            return new DiscItemDreddVideoStream(cd,
                    _strInfoBuilder.getStartSector(), _strInfoBuilder.getEndSector(),
                    _strInfoBuilder.makeDims(),
                    _indexSectorFrameNumberBuilder.makeFormat(), _strInfoBuilder.makeStrVidInfo());
        }

    }

    @CheckForNull
    private VidBuilder _videoBuilder;

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        DreddSectorToDreddFrame s2f = new DreddSectorToDreddFrame(SectorRange.ALL, this);
        scs.addIdListener(s2f);
    }


    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemDreddVideoStream.TYPE_ID.equals(fields.getType()))
            return new DiscItemDreddVideoStream(getCd(), fields);
        return null;
    }

    @Override
    public void frameComplete(@Nonnull DemuxedDreddFrame frame, @Nonnull ILocalizedLogger log) {
        if (_videoBuilder != null && !_videoBuilder.addFrame(frame))
            videoBreak(log);
        if (_videoBuilder == null)
            _videoBuilder = new VidBuilder(frame);
    }

    @Override
    public void videoBreak(ILocalizedLogger log) {
        if (_videoBuilder == null)
            return;

        DiscItemDreddVideoStream video = _videoBuilder.endOfMovie(getCd());
        addVideo(video);
        _videoBuilder = null;
    }

    @Override
    public void endOfSectors(ILocalizedLogger log) {
        videoBreak(log);
    }

}

