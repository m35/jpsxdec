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

package jpsxdec.modules.ac3;

import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DiscIndexerSectorBasedVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfoBuilder;

/** Searches for Ace Combat 3: Electrosphere video streams.
 * Only case I've seen that video streams are interleaved. */
public class DiscIndexerAceCombat3Video extends DiscIndexerSectorBasedVideo.SubIndexer
        implements IdentifiedSectorListener<SectorAceCombat3Video>
{

    private static class VidBuilder {

        @Nonnull
        private final IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @Nonnull
        private final HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;
        @Nonnull
        private final SectorBasedVideoInfoBuilder _strInfoBuilder;
        private final int _iEndFrame;
        private final int _iChannel;
        @Nonnull
        private int _iPrevInvertedFrameNumber;

        public VidBuilder(@Nonnull DemuxedAc3Frame firstFrame) {
            _iEndFrame = _iPrevInvertedFrameNumber = firstFrame.getInvertedHeaderFrameNumber();
            _strInfoBuilder = new SectorBasedVideoInfoBuilder(
                    firstFrame.getWidth(), firstFrame.getHeight(),
                    firstFrame.getStartSector(), firstFrame.getEndSector());
            _iChannel = firstFrame.getChannel();
            _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(firstFrame.getStartSector());
            _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(
                                          _iEndFrame - firstFrame.getInvertedHeaderFrameNumber());
        }

        /** @return if the frame was accepted as part of this video, otherwise start a new video. */
        public boolean addFrame(@Nonnull DemuxedAc3Frame frame) {
            if (frame.getWidth() != _strInfoBuilder.getWidth() ||
                frame.getHeight() != _strInfoBuilder.getHeight() ||
                frame.getStartSector() > _strInfoBuilder.getEndSector() + 100 ||
                frame.getInvertedHeaderFrameNumber() > _iPrevInvertedFrameNumber)
            {
                return false;
            }
            _iPrevInvertedFrameNumber = frame.getInvertedHeaderFrameNumber();
            _strInfoBuilder.next(frame.getStartSector(), frame.getEndSector());
            _indexSectorFrameNumberBuilder.addFrameStartSector(frame.getStartSector());
            _headerFrameNumberBuilder.addHeaderFrameNumber(_iEndFrame - frame.getInvertedHeaderFrameNumber());
            return true;
        }

        public @Nonnull DiscItemAceCombat3VideoStream endOfMovie(@Nonnull ICdSectorReader cd) {
            return new DiscItemAceCombat3VideoStream(cd,
                    _strInfoBuilder.getStartSector(), _strInfoBuilder.getEndSector(),
                    _strInfoBuilder.makeDims(),
                    _indexSectorFrameNumberBuilder.makeFormat(), _strInfoBuilder.makeStrVidInfo(),
                    _headerFrameNumberBuilder.makeFormat(),
                    _iEndFrame, _iChannel);
        }

    }

    /** Takes care of demuxing and generating videos for a particular channel. */
    private static class Ac3Channel implements SectorAc3VideoToDemuxedAc3Frame.Listener {

        @Nonnull
        private final DiscIndexerAceCombat3Video _indexer;
        @Nonnull
        private final SectorAc3VideoToDemuxedAc3Frame _sac3v2dac3frame;
        @CheckForNull
        private VidBuilder _videoBuilder;

        public Ac3Channel(@Nonnull DiscIndexerAceCombat3Video indexer, int iChannel) {
            _indexer = indexer;
            _sac3v2dac3frame = new SectorAc3VideoToDemuxedAc3Frame(iChannel, SectorRange.ALL, this);
        }

        public void feedSector(@Nonnull SectorAceCombat3Video vidSector) throws LoggedFailure {
            Ac3AddResult result = _sac3v2dac3frame.feedSector(vidSector, _indexer._errLog);
            if (result == Ac3AddResult.WrongChannel)
                throw new RuntimeException("AC3 sector was not accepted for some reason.");
        }

        @Override
        public void frameComplete(@Nonnull DemuxedAc3Frame frame, @Nonnull ILocalizedLogger log) {
            if (_videoBuilder != null && !_videoBuilder.addFrame(frame))
                endVideo();
            if (_videoBuilder == null)
                _videoBuilder = new VidBuilder(frame);
        }

        public void endVideo() {
            if (_videoBuilder == null)
                return;

            DiscItemAceCombat3VideoStream video = _videoBuilder.endOfMovie(_indexer.getCd());
            _indexer.addVideo(video);
            _videoBuilder = null;
        }

    }



    @Nonnull
    private final ILocalizedLogger _errLog;
    private final TreeMap<Integer, Ac3Channel> _activeStreams = new TreeMap<Integer, Ac3Channel>();

    public DiscIndexerAceCombat3Video(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemAceCombat3VideoStream.TYPE_ID.equals(fields.getType()))
            return new DiscItemAceCombat3VideoStream(getCd(), fields);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(this);
    }

    @Override
    public @Nonnull Class<SectorAceCombat3Video> getListeningFor() {
        return SectorAceCombat3Video.class;
    }

    @Override
    public void feedSector(@Nonnull SectorAceCombat3Video vidSector,
                           @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        int iChannel = vidSector.getChannel();
        Ac3Channel channel = _activeStreams.get(iChannel);
        if (channel == null) {
            channel = new Ac3Channel(this, iChannel);
            _activeStreams.put(iChannel, channel);
        }
        channel.feedSector(vidSector);
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        for (Ac3Channel channel : _activeStreams.values()) {
            channel.endVideo();
        }
        _activeStreams.clear();
    }

}
