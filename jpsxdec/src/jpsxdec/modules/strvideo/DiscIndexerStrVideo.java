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

package jpsxdec.modules.strvideo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DiscIndexerSectorBasedVideo;
import jpsxdec.modules.video.sectorbased.DiscItemSectorBasedVideoStream;
import jpsxdec.modules.video.sectorbased.SectorBasedDemuxedFrameWithNumberAndDims;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfoBuilder;

/** Searches for most common video streams. */
public class DiscIndexerStrVideo extends DiscIndexerSectorBasedVideo.SubIndexer
        implements SectorBasedDemuxedFrameWithNumberAndDims.Listener
{

    /** Builds a single stream. */
    private static class VidBuilder {

        @Nonnull
        private final IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @Nonnull
        private final HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;
        @Nonnull
        private final SectorBasedVideoInfoBuilder _vidInfoBuilder;
        private int _iLastFrameNumber;
        private final boolean _blnHasSpecialBs;


        public VidBuilder(@Nonnull SectorBasedDemuxedFrameWithNumberAndDims firstFrame) {
            _iLastFrameNumber = firstFrame.getHeaderFrameNumber();
            _vidInfoBuilder = new SectorBasedVideoInfoBuilder(
                    firstFrame.getWidth(), firstFrame.getHeight(),
                    firstFrame.getStartSector(), firstFrame.getEndSector());
            _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(firstFrame.getStartSector());
            _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(firstFrame.getHeaderFrameNumber());
            _blnHasSpecialBs = firstFrame.getCustomFrameMdecStream() != null;
        }

        /** @return if the frame was accepted as part of this video, otherwise start a new video. */
        public boolean addFrame(@Nonnull SectorBasedDemuxedFrameWithNumberAndDims frame) {
            if (frame.getWidth() != _vidInfoBuilder.getWidth() ||
                frame.getHeight() != _vidInfoBuilder.getHeight() ||
                frame.getStartSector() > _vidInfoBuilder.getEndSector() + 100 ||
                frame.getHeaderFrameNumber() < _iLastFrameNumber ||
                frame.getHeaderFrameNumber() > _iLastFrameNumber + 1000) // **
            {
                // ** JPSXDEC-7 and JPSXDEC-9
                // For these two bugs, the video can be broken by detecting
                // a huge gap in frame numbers
                return false;
            }
            if (_blnHasSpecialBs && frame.getCustomFrameMdecStream() == null)
                return false;
            _iLastFrameNumber = frame.getHeaderFrameNumber();
            _vidInfoBuilder.next(frame.getStartSector(), frame.getEndSector());
            _indexSectorFrameNumberBuilder.addFrameStartSector(frame.getStartSector());
            _headerFrameNumberBuilder.addHeaderFrameNumber(frame.getHeaderFrameNumber());
            return true;
        }

        public @Nonnull DiscItemSectorBasedVideoStream endOfMovie(@Nonnull ICdSectorReader cd) {
            return new DiscItemStrVideoStream(cd,
                    _vidInfoBuilder.getStartSector(), _vidInfoBuilder.getEndSector(),
                    _vidInfoBuilder.makeDims(),
                    _indexSectorFrameNumberBuilder.makeFormat(),
                    _vidInfoBuilder.makeStrVidInfo(),
                    _headerFrameNumberBuilder.makeFormat(),
                    !_blnHasSpecialBs);
        }

    }

    private final StrVideoSectorToDemuxedStrFrame _videoDemuxer = new StrVideoSectorToDemuxedStrFrame(SectorRange.ALL, this);
    @CheckForNull
    private VidBuilder _videoBuilder;

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemStrVideoStream.TYPE_ID.equals(fields.getType()))
            return new DiscItemStrVideoStream(getCd(), fields);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(_videoDemuxer);
    }

    @Override
    public void frameComplete(@Nonnull SectorBasedDemuxedFrameWithNumberAndDims frame, @Nonnull ILocalizedLogger log) {
        if (_videoBuilder != null && !_videoBuilder.addFrame(frame))
            endOfVideo(log);
        if (_videoBuilder == null)
            _videoBuilder = new VidBuilder(frame);
    }
    private void endOfVideo(@Nonnull ILocalizedLogger log) {
        if (_videoBuilder == null)
            return;

        DiscItemSectorBasedVideoStream video = _videoBuilder.endOfMovie(getCd());
        addVideo(video);
        _videoBuilder = null;
    }
    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        endOfVideo(log);
    }

}
