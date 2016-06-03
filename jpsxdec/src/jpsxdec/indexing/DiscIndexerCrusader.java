/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2016  Michael Sabin
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
import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.CrusaderDemuxer;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemCrusader;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorCrusader;
import jpsxdec.util.NotThisTypeException;


/** Identify Crusader: No Remorse audio/video streams. */
public class DiscIndexerCrusader extends DiscIndexer implements DiscIndexer.Identified {

    /** Tracks a single video stream. Object dies with stream ends. */
    private static class VidBuilder implements ISectorFrameDemuxer.ICompletedFrameListener {
        @Nonnull
        private final Logger _errLog;

        /** Don't need {@link FullFrameTracker} since the {@link #_demuxer} will
         * track most of it for us, and its start/end sectors will include
         * the audio. */
        @CheckForNull
        private MiniFrameTracker _frameTracker;
        private final CrusaderDemuxer _demuxer = new CrusaderDemuxer();


        public VidBuilder(@Nonnull Logger errLog,
                          @Nonnull SectorCrusader vidSect)
        {
            _errLog = errLog;
            _demuxer.setFrameListener(this);

            try {
                if (!_demuxer.feedSector(vidSect, _errLog))
                    throw new RuntimeException("Why wasn't the sector accepted?");
            } catch (IOException ex) {
                // we know where the completed frames are going
                // so this should never happen
                throw new RuntimeException("Should never happen", ex);
            }
        }

        public boolean feedSector(@Nonnull SectorCrusader vidSect) {

            try {
                if (!_demuxer.feedSector(vidSect, _errLog))
                    return false;
            } catch (IOException ex) {
                // we know where the completed frames are going
                // so this should never happen
                throw new RuntimeException("Should never happen", ex);
            }

            return true;
        }

        // [implements ICompletedFrameListener]
        public void frameComplete(@Nonnull IDemuxedFrame frame) {
            if (_frameTracker == null)
                _frameTracker = new MiniFrameTracker(frame.getFrame());
            else
                _frameTracker.next(frame.getFrame());
        }

        public @CheckForNull DiscItemCrusader endOfMovie(@Nonnull CdFileSectorReader cd) {
            try {
                _demuxer.flush(_errLog);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            if (_frameTracker == null)
                return null;

            return new DiscItemCrusader(cd, 
                    _demuxer.getStartSector(), _demuxer.getEndSector(),
                    _demuxer.getWidth(), _demuxer.getHeight(),
                    _frameTracker.getFrameCount(),
                    _frameTracker.getFormat(),
                    _frameTracker.getStartFrame(),
                    _frameTracker.getEndFrame());
        }
    }
    

    @Nonnull
    private final Logger _errLog;
    @CheckForNull
    private VidBuilder _currentStream;

    public DiscIndexerCrusader(@Nonnull Logger errLog) {
        _errLog = errLog;
    }

    public void indexingSectorRead(@Nonnull CdSector cdSector,
                                   @CheckForNull IdentifiedSector idSector)
    {
        if (!(idSector instanceof SectorCrusader))
            return;

        SectorCrusader vidSect = (SectorCrusader)idSector;

        if (_currentStream != null) {
            boolean blnAccepted = _currentStream.feedSector(vidSect);
            if (!blnAccepted) {
                DiscItemCrusader vid = _currentStream.endOfMovie(getCd());
                if (vid != null)
                    super.addDiscItem(vid);
                _currentStream = null;
            }
        }
        if (_currentStream == null) {
            _currentStream = new VidBuilder(_errLog, vidSect);
        }

    }

    @Override
    public void indexingEndOfDisc() {
        if (_currentStream != null) {
            DiscItemCrusader vid = _currentStream.endOfMovie(getCd());
            if (vid != null)
                super.addDiscItem(vid);
            _currentStream = null;
        }
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) {
        try {
            if (DiscItemCrusader.TYPE_ID.equals(fields.getType())) {
                return new DiscItemCrusader(getCd(), fields);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
