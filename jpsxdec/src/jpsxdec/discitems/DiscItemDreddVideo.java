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

package jpsxdec.discitems;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.sectors.SectorDreddVideo;
import jpsxdec.util.NotThisTypeException;

/** Represents Judge Dredd video streams.
 * Mostly identical to {@link DiscItemStrVideoWithFrame}
 * except how it splits the audio. */
public class DiscItemDreddVideo extends DiscItemStrVideoStream {

    public static final String TYPE_ID = "Dredd";

    public DiscItemDreddVideo(@Nonnull CdFileSectorReader cd,
                              int iStartSector, int iEndSector,
                              int iWidth, int iHeight,
                              int iFrameCount,
                              @Nonnull FrameNumberFormat frameNumberFormat,
                              @Nonnull FrameNumber startFrame,
                              @Nonnull FrameNumber endFrame,
                              int iSectors, int iPerFrame,
                              int iFirstFrameLastSector)
    {
        super(cd, iStartSector, iEndSector,
              iWidth, iHeight,
              iFrameCount,
              frameNumberFormat,
              startFrame, endFrame,
              iSectors, iPerFrame,
              iFirstFrameLastSector);
    }

    public DiscItemDreddVideo(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws NotThisTypeException
    {
        super(cd, fields);
    }


    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public int getParentRating(@Nonnull DiscItem child) {
        if (!(child instanceof DiscItemXaAudioStream))
            return 0;

        int iOverlapPercent = child.getOverlap(this)*100 / child.getSectorLength();
        if (iOverlapPercent > 0)
            iOverlapPercent += 100;
        return iOverlapPercent;
    }

    private static final int AUDIO_SPLIT_THRESHOLD = 32;

    @Override
    public int splitAudio(@Nonnull DiscItemXaAudioStream audio) {
        int iStartSector = getStartSector();
        // if audio crosses the start sector
        if (audio.getStartSector() < iStartSector - AUDIO_SPLIT_THRESHOLD &&
            audio.getEndSector()  >= iStartSector)
        {
            return iStartSector - 2;
        } else {
            return -1;
        }
    }
    
    @Override
    public void fpsDump(@Nonnull PrintStream ps) throws IOException {
        IdentifiedSectorIterator it = identifiedSectorIterator();
        int iFrameIndex = 0;
        int iLastChunk = -1;
        for (int iSector = 0; it.hasNext(); iSector++) {
            IdentifiedSector isect = it.next();
            if (isect instanceof SectorDreddVideo) {
                SectorDreddVideo vidSect = (SectorDreddVideo) isect;
                ps.println(String.format("%-5d %-4d %d/%d",
                                        iSector,
                                        iFrameIndex,
                                        vidSect.getChunkNumber(),
                                        vidSect.getChunksInFrame() ));
                if (vidSect.getChunkNumber() < iLastChunk)
                    iFrameIndex++;
                iLastChunk = vidSect.getChunkNumber();
            } else {
                ps.println(String.format(
                        "%-5d X",
                        iSector));
            }

        }
    }

    @Override
    public @Nonnull ISectorFrameDemuxer makeDemuxer() {
        return new Demuxer();
    }
    
    /** Public facing (external) demuxer for Judge Dredd.
     * Wraps {@link DreddDemuxer} and sets the {@link FrameNumber} for completed
     * frames before passing them onto the {@link ICompletedFrameListener}. */
    public static class Demuxer implements ISectorFrameDemuxer, DreddDemuxer.Listener {

        private final DreddDemuxer _demuxer = new DreddDemuxer();
        private final FrameNumber.FactoryNoHeader _frameNumberFactory = new FrameNumber.FactoryNoHeader();
        @CheckForNull
        private ICompletedFrameListener _listener;

        public Demuxer() {
            _demuxer.setFrameListener(this);
        }

        public boolean feedSector(@Nonnull IdentifiedSector idSector, @Nonnull Logger log) throws IOException {
            return _demuxer.feedSector(idSector.getCdSector(), idSector, log);
        }
        public void flush(@Nonnull Logger log) throws IOException {
            _demuxer.flush(log);
        }
        
        public void setFrameListener(@Nonnull ICompletedFrameListener listener) {
            _listener = listener;
        }

        // [implements DreddDemuxer.Listener]
        public void frameComplete(@Nonnull DreddDemuxer.DemuxedDreddFrame frame) throws IOException {
            frame.setFrame(_frameNumberFactory.next(frame.getStartSector()));
            if (_listener == null)
                throw new IllegalStateException();
            _listener.frameComplete(frame);
        }
        // [implements DreddDemuxer.Listener]
        public void endVideo() {
            // this could happen on the last sector of the movie if that's
            // what triggered its ending
        }

    }
    
}
