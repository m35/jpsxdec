/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.DiscItemStrVideoWithFrame;
import jpsxdec.discitems.DiscItemXaAudioStream;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IVideoSectorWithFrameNumber;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;

/**
 * Searches for most common video streams.
 */
public class DiscIndexerStrVideoWithFrame extends DiscIndexer implements DiscIndexer.Identified {

    private static final Logger LOG = Logger.getLogger(DiscIndexerStrVideoWithFrame.class.getName());

    private static class VideoStreamIndex extends AbstractVideoStreamIndex<DiscItemStrVideoWithFrame> {

        public VideoStreamIndex(@Nonnull CdFileSectorReader cd, @Nonnull Logger errLog,
                                @Nonnull IVideoSectorWithFrameNumber vidSect)
        {
            super(cd, errLog, (IdentifiedSector)vidSect, true,
                  new DiscItemStrVideoWithFrame.Demuxer(
                                vidSect.getSectorNumber(), Integer.MAX_VALUE,
                                vidSect.getWidth(), vidSect.getHeight()));
        }

        @Override
        protected DiscItemStrVideoWithFrame createVideo(int iStartSector, int iEndSector,
                                                        int iWidth, int iHeight,
                                                        int iFrameCount,
                                                        @Nonnull FrameNumberFormat frameNumberFormat,
                                                        @Nonnull FrameNumber startFrame,
                                                        @Nonnull FrameNumber lastSeenFrameNumber,
                                                        int iSectors, int iPerFrame,
                                                        int iFrame1PresentationSector)
        {
            return new DiscItemStrVideoWithFrame(
                    _cd,
                    iStartSector, iEndSector,
                    iWidth, iHeight,
                    iFrameCount,
                    frameNumberFormat,
                    startFrame, lastSeenFrameNumber,
                    iSectors, iPerFrame,
                    iFrame1PresentationSector);
        }
    }


    @Nonnull
    private final Logger _errLog;
    @CheckForNull
    private VideoStreamIndex _currentStream;
    private final Collection<DiscItemStrVideoWithFrame> _completedVideos = new ArrayList<DiscItemStrVideoWithFrame>();

    public DiscIndexerStrVideoWithFrame(@Nonnull Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) {
        try {
            if (DiscItemStrVideoWithFrame.TYPE_ID.equals(fields.getType())) {
                return new DiscItemStrVideoWithFrame(getCd(), fields);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    public void indexingSectorRead(@Nonnull IdentifiedSector sector) {
        if (!(sector instanceof IVideoSectorWithFrameNumber))
            return;

        IVideoSectorWithFrameNumber vidSect = (IVideoSectorWithFrameNumber)sector;
        
        if (_currentStream != null) {
            boolean blnAccepted = _currentStream.sectorRead(sector);
            if (!blnAccepted) {
                DiscItemStrVideoWithFrame vid = _currentStream.endOfMovie();
                if (vid != null) {
                    _completedVideos.add(vid);
                    super.addDiscItem(vid);
                }
                _currentStream = null;
            }
        }
        if (_currentStream == null) {
            _currentStream = new VideoStreamIndex(getCd(), _errLog, vidSect);
        }
    }

    @Override
    public void indexingEndOfDisc() {
        if (_currentStream != null) {
            DiscItemStrVideoWithFrame vid = _currentStream.endOfMovie();
            if (vid != null) {
                _completedVideos.add(vid);
                super.addDiscItem(vid);
            }
            _currentStream = null;
        }
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
        if (_completedVideos.size() > 0)
            audioSplit(_completedVideos, allItems);
    }

    static void audioSplit(@Nonnull Collection<? extends DiscItemStrVideoStream> videos,
                           @Nonnull Collection<DiscItem> allItems)
    {
        List<DiscItemXaAudioStream> added = new ArrayList<DiscItemXaAudioStream>();

        for (Iterator<DiscItem> it = allItems.iterator(); it.hasNext();) {
            DiscItem item = it.next();
            if (item instanceof DiscItemXaAudioStream) {
                DiscItemXaAudioStream audio = (DiscItemXaAudioStream) item;
                for (DiscItemStrVideoStream video : videos) {
                    int iSector = video.splitAudio(audio);
                    if (iSector >= 0) {
                        DiscItemXaAudioStream[] aoSplit = audio.split(iSector);
                        it.remove();
                        added.add(aoSplit[0]);
                        added.add(aoSplit[1]);
                        break; // will continue later with split items
                    }
                }
            }
        }

        // now process the new items
        for (ListIterator<DiscItemXaAudioStream> it = added.listIterator(); it.hasNext();) {
            DiscItemXaAudioStream audio = it.next();
            for (DiscItemStrVideoStream video : videos) {
                int iSector = video.splitAudio(audio);
                if (iSector >= 0) {
                    DiscItemXaAudioStream[] aoSplit = audio.split(iSector);
                    it.remove();
                    it.add(aoSplit[0]);
                    it.add(aoSplit[1]);
                    it.previous(); // jump to before the split items
                    it.previous();
                    break; // will continue with split items
                }
            }
        }
        allItems.addAll(added);
    }

    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
