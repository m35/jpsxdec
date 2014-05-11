/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemStrVideoWithFrame;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IVideoSectorWithFrameNumber;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;

/**
 * Searches for most common video streams.
 */
public class DiscIndexerStrVideoWithFrame extends DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexerStrVideoWithFrame.class.getName());

    private static class VideoStreamIndex extends AbstractVideoStreamIndex {
        private int _iStartFrame = -1;

        public VideoStreamIndex(Logger errLog, IVideoSectorWithFrameNumber vidSect) {
            super(errLog, (IdentifiedSector)vidSect, true);

            initDemuxer(new DiscItemStrVideoWithFrame.Demuxer(
                                vidSect.getSectorNumber(), Integer.MAX_VALUE,
                                vidSect.getWidth(), vidSect.getHeight()),
                        (IdentifiedSector)vidSect);
        }

        @Override
        public void frameComplete(IDemuxedFrame frame) {
            if (_iStartFrame < 0)
                _iStartFrame = frame.getFrame();
            super.frameComplete(frame);
        }

        @Override
        protected DiscItemStrVideoWithFrame createVideo(int iStartSector, int iEndSector,
                                                        int iWidth, int iHeight,
                                                        int iFrameCount,
                                                        int iLastSeenFrameNumber,
                                                        int iSectors, int iPerFrame,
                                                        int iFrame1PresentationSector)
        {
            if (_iStartFrame > 1)
                _errLog.log(Level.WARNING, "Video stream first frame is > 1: {0,number,#}", _iStartFrame);
            
            return new DiscItemStrVideoWithFrame(
                    iStartSector, iEndSector,
                    iWidth, iHeight,
                    iFrameCount,
                    _iStartFrame, iLastSeenFrameNumber,
                    iSectors, iPerFrame,
                    iFrame1PresentationSector);
        }
    }


    private final Logger _errLog;
    private VideoStreamIndex _currentStream;

    public DiscIndexerStrVideoWithFrame(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem deserializedLine) {
        try {
            if (DiscItemStrVideoWithFrame.TYPE_ID.equals(deserializedLine.getType())) {
                return new DiscItemStrVideoWithFrame(deserializedLine);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector sector) {
        if (!(sector instanceof IVideoSectorWithFrameNumber))
            return;
        

        IVideoSectorWithFrameNumber vidSect = (IVideoSectorWithFrameNumber)sector;
        
        if (_currentStream != null) {
            boolean blnAccepted = _currentStream.sectorRead(sector);
            if (!blnAccepted) {
                DiscItemVideoStream vid = _currentStream.endOfMovie();
                if (vid != null)
                    super.addDiscItem(vid);
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
            DiscItemVideoStream vid = _currentStream.endOfMovie();
            if (vid != null)
                super.addDiscItem(vid);
            _currentStream = null;
        }
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream inStream) throws IOException {
    }

    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
