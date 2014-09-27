/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014  Michael Sabin
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
import java.util.logging.Logger;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemDreddVideo;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorDreddVideo;
import jpsxdec.util.NotThisTypeException;


public class DiscIndexerDredd extends DiscIndexer implements DiscIndexer.Identified {

    private static final Logger LOG = Logger.getLogger(DiscIndexerDredd.class.getName());

    private static class VideoStreamIndex extends AbstractVideoStreamIndex<DiscItemDreddVideo> {

        public VideoStreamIndex(Logger errLog, SectorDreddVideo vidSect) {
            super(errLog, vidSect, true);

            initDemuxer(new DiscItemDreddVideo.Demuxer(
                                vidSect.getSectorNumber(), Integer.MAX_VALUE,
                                vidSect.getWidth(), vidSect.getHeight()),
                        (IdentifiedSector)vidSect);
        }

        @Override
        protected DiscItemDreddVideo createVideo(int iStartSector, int iEndSector,
                                                 int iWidth, int iHeight,
                                                 int iFrameCount,
                                                 FrameNumberFormat frameNumberFormat,
                                                 FrameNumber startFrame,
                                                 FrameNumber lastSeenFrameNumber,
                                                 int iSectors, int iPerFrame,
                                                 int iFrame1PresentationSector)
        {
            return new DiscItemDreddVideo(
                    iStartSector, iEndSector,
                    iWidth, iHeight,
                    iFrameCount,
                    frameNumberFormat,
                    startFrame, lastSeenFrameNumber,
                    iSectors, iPerFrame,
                    iFrame1PresentationSector);
        }
    }


    private final Logger _errLog;
    private VideoStreamIndex _currentStream;
    private final Collection<DiscItemDreddVideo> _completedVideos = new ArrayList<DiscItemDreddVideo>();

    public DiscIndexerDredd(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem deserializedLine) {
        try {
            if (DiscItemDreddVideo.TYPE_ID.equals(deserializedLine.getType())) {
                return new DiscItemDreddVideo(deserializedLine);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    public void indexingSectorRead(IdentifiedSector sector) {
        if (!(sector instanceof SectorDreddVideo))
            return;

        SectorDreddVideo vidSect = (SectorDreddVideo)sector;
        
        if (_currentStream != null) {
            boolean blnAccepted = _currentStream.sectorRead(vidSect);
            if (!blnAccepted) {
                DiscItemDreddVideo vid = _currentStream.endOfMovie();
                if (vid != null) {
                    _completedVideos.add(vid);
                    super.addDiscItem(vid);
                }
                _currentStream = null;
            }
        }
        if (_currentStream == null) {
            _currentStream = new VideoStreamIndex(_errLog, vidSect);
        }
    }

    @Override
    public void indexingEndOfDisc() {
        if (_currentStream != null) {
            DiscItemDreddVideo vid = _currentStream.endOfMovie();
            if (vid != null) {
                _completedVideos.add(vid);
                super.addDiscItem(vid);
            }
            _currentStream = null;
        }
    }

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
        if (_completedVideos.size() > 0)
            DiscIndexerStrVideoWithFrame.audioSplit(_completedVideos, allItems);
    }

    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
