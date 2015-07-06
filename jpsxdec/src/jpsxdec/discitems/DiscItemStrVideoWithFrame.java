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

package jpsxdec.discitems;

import java.io.IOException;
import java.io.PrintStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.IVideoSectorWithFrameNumber;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;

/** Handles most variations of PlayStation video streams.
 * Most video streams have video sectors with similar header information
 * that includes the frame number the sector belongs to. */
public class DiscItemStrVideoWithFrame extends DiscItemStrVideoStream {

    public static final String TYPE_ID = "Video";

    public DiscItemStrVideoWithFrame(@Nonnull CdFileSectorReader cd,
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

    public DiscItemStrVideoWithFrame(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
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
        if (!(child instanceof DiscItemAudioStream))
            return 0;

        int iOverlapPercent = child.getOverlap(this)*100 / child.getSectorLength();
        if (iOverlapPercent > 0)
            iOverlapPercent += 100;
        return iOverlapPercent;
    }

    private static final int AUDIO_SPLIT_THRESHOLD = 32;
    
    public int splitAudio(@Nonnull DiscItemXaAudioStream audio) {
        int iStartSector = getStartSector();
        // if audio crosses the start sector
        if (audio.getStartSector() < iStartSector - AUDIO_SPLIT_THRESHOLD &&
            audio.getEndSector()  >= iStartSector + AUDIO_SPLIT_THRESHOLD)
        {
            return iStartSector;
        } else {
            return -1;
        }
    }
    
    @Override
    public void fpsDump(@Nonnull PrintStream ps) throws IOException {
        final int LENGTH = getSectorLength();
        for (int iSector = 0; iSector < LENGTH; iSector++) {
            IdentifiedSector isect = getRelativeIdentifiedSector(iSector);
            if (isect instanceof IVideoSectorWithFrameNumber) {
                IVideoSectorWithFrameNumber vidSect = (IVideoSectorWithFrameNumber) isect;
                ps.println(String.format("%-5d %-4d %d/%d",
                                        iSector,
                                        vidSect.getFrameNumber(),
                                        vidSect.getChunkNumber(),
                                        vidSect.getChunksInFrame() ));
            } else {
                ps.println(String.format(
                        "%-5d X",
                        iSector));
            }

        }
    }

    @Override
    public @Nonnull ISectorFrameDemuxer makeDemuxer() {
        return new Demuxer(getStartSector(), getEndSector(), getWidth(), getHeight());
    }
    
    public static class Demuxer extends FrameDemuxer<IVideoSectorWithFrameNumber> {

        private final int _iWidth, _iHeight;

        public Demuxer(int iVideoStartSector, int iVideoEndSector, int iWidth, int iHeight) {
            super(iVideoStartSector, iVideoEndSector);
            if (iWidth < 1 || iHeight < 1)
                throw new IllegalArgumentException("Invalid dimensions " + iWidth + "x" + iHeight);
            _iWidth = iWidth;
            _iHeight = iHeight;
        }

        protected @CheckForNull IVideoSectorWithFrameNumber isVideoSector(@Nonnull IdentifiedSector sector) {
            if (!(sector instanceof IVideoSectorWithFrameNumber))
                return null;
            return (IVideoSectorWithFrameNumber) sector;
        }

        @Override
        protected boolean isPartOfVideo(@Nonnull IVideoSectorWithFrameNumber chunk) {
            return super.isPartOfVideo(chunk) &&
                   chunk.getWidth() == _iWidth &&
                   chunk.getHeight() == _iHeight;
        }

        @Override
        protected int getHeaderFrameNumber(@Nonnull IVideoSectorWithFrameNumber chunk) {
            return chunk.getFrameNumber();
        }

        @Override
        protected int getChunksInFrame(@Nonnull IVideoSectorWithFrameNumber chunk) {
            return chunk.getChunksInFrame();
        }

        @Override
        public int getWidth() {
            return _iWidth;
        }

        @Override
        public int getHeight() {
            return _iHeight;
        }
    }
    
}
