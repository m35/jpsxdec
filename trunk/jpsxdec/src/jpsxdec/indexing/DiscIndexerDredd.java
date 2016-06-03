/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2016  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemDreddVideo;
import jpsxdec.discitems.DreddDemuxer;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;


public class DiscIndexerDredd extends DiscIndexer
        implements DiscIndexer.Identified, DreddDemuxer.Listener
{

    private static final Logger LOG = Logger.getLogger(DiscIndexerDredd.class.getName());

    private static class VidBuilder {

        private final FrameNumber.FactoryNoHeader _frameNumberFactory = new FrameNumber.FactoryNoHeader();
        @Nonnull
        private final FullFrameTracker _frameTracker;
        @Nonnull
        private final Logger _errLog;

        public VidBuilder(@Nonnull DreddDemuxer.DemuxedDreddFrame firstFrame, @Nonnull Logger errLog) {
            _errLog = errLog;
            _frameTracker = new FullFrameTracker(
                    firstFrame.getWidth(), firstFrame.getHeight(),
                    _frameNumberFactory.next(firstFrame.getStartSector()),
                    firstFrame.getEndSector());
        }

        /** @return if the frame was accepted as part of this video, otherwise start a new video. */
        public boolean addFrame(@Nonnull DreddDemuxer.DemuxedDreddFrame frame) {
            if (frame.getWidth() != _frameTracker.getWidth() ||
                frame.getHeight() != _frameTracker.getHeight() ||
                frame.getStartSector() > _frameTracker.getEndSector() + 100)
            {
                return false;
            }
            _frameTracker.next(_frameNumberFactory.next(frame.getStartSector()),
                               frame.getEndSector());
            return true;
        }

        public @Nonnull DiscItemDreddVideo endOfMovie(@Nonnull CdFileSectorReader cd) {
            int[] aiSectorsPerFrame = _frameTracker.getSectorsPerFrame();

            return new DiscItemDreddVideo(cd,
                    _frameTracker.getStartSector(), _frameTracker.getEndSector(),
                    _frameTracker.getWidth(), _frameTracker.getHeight(),
                    _frameTracker.getFrameCount(),
                    _frameTracker.getFormat(),
                    _frameTracker.getStartFrame(), _frameTracker.getEndFrame(),
                    aiSectorsPerFrame[0], aiSectorsPerFrame[1],
                    _frameTracker.getFrame1PresentationSector());
        }

    }
    
    @Nonnull
    private final Logger _errLog;
    private final Collection<DiscItemDreddVideo> _completedVideos = new ArrayList<DiscItemDreddVideo>();
    private final DreddDemuxer _videoDemuxer = new DreddDemuxer();
    @CheckForNull
    private VidBuilder _videoBuilder;


    public DiscIndexerDredd(@Nonnull Logger errLog) {
        _errLog = errLog;
        _videoDemuxer.setFrameListener(this);
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) {
        try {
            if (DiscItemDreddVideo.TYPE_ID.equals(fields.getType())) {
                return new DiscItemDreddVideo(getCd(), fields);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    public void indexingSectorRead(@Nonnull CdSector cdSector,
                                   @CheckForNull IdentifiedSector idSector)
    {
        try {
            // will check if it's a sector we care about
            // if not, the sector will be ignored
            _videoDemuxer.feedSector(cdSector, idSector, _errLog);
        } catch (IOException ex) {
            _errLog.log(Level.SEVERE, null, ex);
        }
    }

    // [implements DreddDemuxer.Listener]
    public void frameComplete(@Nonnull DreddDemuxer.DemuxedDreddFrame frame) {
        if (_videoBuilder != null && !_videoBuilder.addFrame(frame))
            endVideo();
        if (_videoBuilder == null)
            _videoBuilder = new VidBuilder(frame, _errLog);
    }

    // [implements DreddDemuxer.Listener]
    public void endVideo() {
        if (_videoBuilder == null)
            return;

        DiscItemDreddVideo video = _videoBuilder.endOfMovie(getCd());
        _completedVideos.add(video);
        super.addDiscItem(video);
        _videoBuilder = null;
    }

    @Override
    public void indexingEndOfDisc() {
        endVideo();
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

