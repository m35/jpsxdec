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
import java.util.TreeMap;
import java.util.logging.Logger;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAceCombat3VideoStream;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorAceCombat3Video;
import jpsxdec.util.NotThisTypeException;

/**
 * Searches for Ace Combat 3: Electrosphere video streams.
 * Only case I've seen that video streams are interleaved.
 */
public class DiscIndexerAceCombat3Video extends DiscIndexer implements DiscIndexer.Identified {

    
    private static class VideoStreamIndex extends AbstractVideoStreamIndex<DiscItemAceCombat3VideoStream> {
        
        private final int _iChannel;
        private final int _iMaxFrame;
        
        public VideoStreamIndex(Logger errLog, SectorAceCombat3Video vidSect) {
            super(errLog, vidSect, true);

            _iChannel = vidSect.getChannel();
            _iMaxFrame = vidSect.getInvertedFrameNumber();

            initDemuxer(new DiscItemAceCombat3VideoStream.Demuxer(
                                    vidSect.getSectorNumber(), Integer.MAX_VALUE,
                                    vidSect.getWidth(), vidSect.getHeight(),
                                    _iMaxFrame, _iChannel),
                        vidSect);
        }

        @Override
        protected DiscItemAceCombat3VideoStream createVideo(int iStartSector, int iEndSector,
                                                            int iWidth, int iHeight,
                                                            int iFrameCount,
                                                            FrameNumberFormat frameNumberFormat,
                                                            FrameNumber startFrame,
                                                            FrameNumber lastSeenFrameNumber,
                                                            int iSectors, int iPerFrame,
                                                            int iFrame1PresentationSector)
        {
            return new DiscItemAceCombat3VideoStream(
                    iStartSector, iEndSector,
                    iWidth, iHeight,
                    iFrameCount,
                    frameNumberFormat,
                    iSectors, iPerFrame,
                    iFrame1PresentationSector,
                    startFrame, lastSeenFrameNumber,
                    _iChannel);
        }

    }

    
    private final Logger _errLog;
    private final TreeMap<Integer, VideoStreamIndex> _activeStreams = new TreeMap<Integer, VideoStreamIndex>();
    private final Collection<DiscItemAceCombat3VideoStream> _completedVideos = new ArrayList<DiscItemAceCombat3VideoStream>();

    public DiscIndexerAceCombat3Video(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem deserializedLine) {
        try {
            if (DiscItemAceCombat3VideoStream.TYPE_ID.equals(deserializedLine.getType())) {
                return new DiscItemAceCombat3VideoStream(deserializedLine);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    public void indexingSectorRead(IdentifiedSector sector) {
        if (!(sector instanceof SectorAceCombat3Video)) {
            return;
        }

        SectorAceCombat3Video vidSect = (SectorAceCombat3Video)sector;
        
        Integer oiChannel = Integer.valueOf(vidSect.getChannel());
        VideoStreamIndex stream = _activeStreams.get(oiChannel);
        if (stream != null) {
            boolean blnAccepted = stream.sectorRead(vidSect);
            if (!blnAccepted) {
                DiscItemAceCombat3VideoStream vid = stream.endOfMovie();
                if (vid != null) {
                    _completedVideos.add(vid);
                    super.addDiscItem(vid);
                }
                _activeStreams.remove(oiChannel);
                stream = null;
            }
        }
        if (stream == null) {
            stream = new VideoStreamIndex(_errLog, vidSect);
            _activeStreams.put(oiChannel, stream);
        }
    }

    @Override
    public void indexingEndOfDisc() {
        for (VideoStreamIndex videoStreamIndex : _activeStreams.values()) {
            DiscItemAceCombat3VideoStream vid = videoStreamIndex.endOfMovie();
            if (vid != null) {
                _completedVideos.add(vid);
                super.addDiscItem(vid);
            }
        }
        _activeStreams.clear();
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
