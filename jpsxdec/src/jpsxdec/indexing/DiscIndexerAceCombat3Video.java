/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2017  Michael Sabin
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

package jpsxdec.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.Ac3Demuxer;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAceCombat3VideoStream;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorAceCombat3Video;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.LoggedFailure;

/**
 * Searches for Ace Combat 3: Electrosphere video streams.
 * Only case I've seen that video streams are interleaved.
 */
public class DiscIndexerAceCombat3Video extends DiscIndexer implements DiscIndexer.Identified {

    private static final Logger LOG = Logger.getLogger(DiscIndexerAceCombat3Video.class.getName());

    private static class VidBuilder {

        private final FrameNumber.FactoryWithHeader _frameNumberFactory = new FrameNumber.FactoryWithHeader();
        @Nonnull
        private final FullFrameTracker _frameTracker;
        private final int _iEndFrame;
        private final int _iChannel;
        @Nonnull
        private int _iLastInvertedFrameNumber;

        public VidBuilder(@Nonnull Ac3Demuxer.DemuxedAc3Frame firstFrame) {
            _iEndFrame = _iLastInvertedFrameNumber = firstFrame.getInvertedHeaderFrameNumber();
            _frameTracker = new FullFrameTracker(
                    firstFrame.getWidth(), firstFrame.getHeight(),
                    _frameNumberFactory.next(firstFrame.getStartSector(),
                                             _iEndFrame - firstFrame.getInvertedHeaderFrameNumber()),
                    firstFrame.getEndSector());
            _iChannel = firstFrame.getChannel();
        }

        /** @return if the frame was accepted as part of this video, otherwise start a new video. */
        public boolean addFrame(@Nonnull Ac3Demuxer.DemuxedAc3Frame frame) {
            if (frame.getWidth() != _frameTracker.getWidth() ||
                frame.getHeight() != _frameTracker.getHeight() ||
                frame.getStartSector() > _frameTracker.getEndSector() + 100 ||
                frame.getInvertedHeaderFrameNumber() > _iLastInvertedFrameNumber)
            {
                return false;
            }
            _iLastInvertedFrameNumber = frame.getInvertedHeaderFrameNumber();
            _frameTracker.next(_frameNumberFactory.next(frame.getStartSector(), 
                                                        _iEndFrame - frame.getInvertedHeaderFrameNumber()),
                               frame.getEndSector());
            return true;
        }

        public @Nonnull DiscItemAceCombat3VideoStream endOfMovie(@Nonnull CdFileSectorReader cd) {
            int[] aiSectorsPerFrame = _frameTracker.getSectorsPerFrame();
            return new DiscItemAceCombat3VideoStream(cd,
                    _frameTracker.getStartSector(), _frameTracker.getEndSector(),
                    _frameTracker.getWidth(), _frameTracker.getHeight(),
                    _frameTracker.getFrameCount(),
                    _frameTracker.getFormat(),
                    aiSectorsPerFrame[0], aiSectorsPerFrame[1],
                    _frameTracker.getFrame1PresentationSector(),
                    _frameTracker.getStartFrame(), _frameTracker.getEndFrame(),
                    _iChannel);
        }

    }

    /** Takes care of demuxing and generating videos for a particular channel. */
    private static class Ac3Channel implements Ac3Demuxer.Listener {

        @Nonnull
        private final DiscIndexerAceCombat3Video _indexer;
        @Nonnull
        private final Ac3Demuxer _demuxer;
        @CheckForNull
        private VidBuilder _videoBuilder;

        public Ac3Channel(@Nonnull DiscIndexerAceCombat3Video indexer, int iChannel) {
            _indexer = indexer;
            _demuxer = new Ac3Demuxer(iChannel);
            _demuxer.setFrameListener(this);
        }

        public void feedSector(@Nonnull SectorAceCombat3Video vidSector) {
            try {
                if (!_demuxer.feedSector(vidSector, _indexer._errLog))
                    throw new RuntimeException("AC3 sector was not accepted for some reason.");
            } catch (LoggedFailure ex) {
                // we know where the completed frames are going
                // so this should never happen
                throw new RuntimeException("Should not happen", ex);
            }
        }

        // [implements Ac3Demuxer.Listener]
        public void frameComplete(@Nonnull Ac3Demuxer.DemuxedAc3Frame frame) {
            if (_videoBuilder != null && !_videoBuilder.addFrame(frame))
                endVideo();
            if (_videoBuilder == null)
                _videoBuilder = new VidBuilder(frame);
        }

        public void endVideo() {
            if (_videoBuilder == null)
                return;

            DiscItemAceCombat3VideoStream video = _videoBuilder.endOfMovie(_indexer.getCd());
            _indexer._completedVideos.add(video);
            _indexer.addDiscItem(video);
            _videoBuilder = null;
        }

    }



    @Nonnull
    private final ILocalizedLogger _errLog;
    private final TreeMap<Integer, Ac3Channel> _activeStreams = new TreeMap<Integer, Ac3Channel>();
    private final Collection<DiscItemAceCombat3VideoStream> _completedVideos = new ArrayList<DiscItemAceCombat3VideoStream>();

    public DiscIndexerAceCombat3Video(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws DeserializationFail
    {
        if (DiscItemAceCombat3VideoStream.TYPE_ID.equals(fields.getType()))
            return new DiscItemAceCombat3VideoStream(getCd(), fields);
        return null;
    }

    public void indexingSectorRead(@Nonnull CdSector cdSector, 
                                   @CheckForNull IdentifiedSector idSector)
    {
        if (!(idSector instanceof SectorAceCombat3Video))
            return;
        SectorAceCombat3Video vidSector = (SectorAceCombat3Video) idSector;
        Integer oiChannel = vidSector.getChannel();
        Ac3Channel channel = _activeStreams.get(oiChannel);
        if (channel == null) {
            channel = new Ac3Channel(this, oiChannel);
            _activeStreams.put(oiChannel, channel);
        }
        channel.feedSector(vidSector);
    }


    @Override
    public void indexingEndOfDisc() {
        for (Ac3Channel channel : _activeStreams.values()) {
            channel.endVideo();
        }
        _activeStreams.clear();
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
        if (_completedVideos.size() > 0)
            DiscIndexerStrVideoWithFrame.audioSplit(_completedVideos, allItems);
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
