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

package jpsxdec.discitems;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.sectors.SectorAceCombat3Video;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.LoggedFailure;

/** Represents all variations of PlayStation video streams. */
public class DiscItemAceCombat3VideoStream extends DiscItemStrVideoStream {

    public static final String TYPE_ID = "AC3Vid";
    
    private static final String CHANNEL_KEY = "Channel";
    private final int _iChannel;

    public DiscItemAceCombat3VideoStream(@Nonnull CdFileSectorReader cd,
                                         int iStartSector, int iEndSector,
                                         int iWidth, int iHeight,
                                         int iFrameCount,
                                         @Nonnull FrameNumberFormat frameNumberFormat,
                                         int iSectors, int iPerFrame,
                                         int iFirstFrameLastSector,
                                         @Nonnull FrameNumber startFrame,
                                         @Nonnull FrameNumber endFrame,
                                         int iChannel)
    {
        super(cd, iStartSector, iEndSector,
              iWidth, iHeight,
              iFrameCount,
              frameNumberFormat,
              startFrame, endFrame,
              iSectors, iPerFrame,
              iFirstFrameLastSector);
        _iChannel = iChannel;
    }

    public DiscItemAceCombat3VideoStream(@Nonnull CdFileSectorReader cd,
                                         @Nonnull SerializedDiscItem fields)
            throws DeserializationFail
    {
        super(cd, fields);
        _iChannel = fields.getInt(CHANNEL_KEY);
    }


    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addNumber(CHANNEL_KEY, _iChannel);
        return serial;
    }
       


    @Override
    public int getParentRating(@Nonnull DiscItem child) {
        if (!(child instanceof DiscItemXaAudioStream))
            return 0;
        if (((DiscItemXaAudioStream)child).getChannel() != _iChannel)
            return 0;

        int iOverlapPercent = child.getOverlap(this)*100 / child.getSectorLength();
        if (iOverlapPercent > 0)
            iOverlapPercent += 100;
        return iOverlapPercent;
    }

    private static final int AUDIO_SPLIT_THRESHOLD = 32;


    public int splitAudio(@Nonnull DiscItemXaAudioStream audio) {
        if (audio.getChannel() != _iChannel)
            return -1;
        
        int iStartSector = getStartSector();
        // if audio crosses the start sector
        if (audio.getStartSector() < iStartSector - AUDIO_SPLIT_THRESHOLD &&
            audio.getEndSector()  >= iStartSector)
        {
            return iStartSector - 1;
        } else {
            return -1;
        }
    }

    
    @Override
    public void fpsDump(@Nonnull PrintStream ps) throws IOException {
        IdentifiedSectorIterator it = identifiedSectorIterator();
        for (int iSector = 0; it.hasNext(); iSector++) {
            IdentifiedSector isect = it.next();
            if (isect instanceof SectorAceCombat3Video) {
                SectorAceCombat3Video vidSect = (SectorAceCombat3Video) isect;
                ps.println(String.format("%-5d %-4d %d/%d",
                                         iSector,
                                         getEndFrame().getHeaderFrameNumber() - vidSect.getInvertedFrameNumber(),
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
        return new Demuxer(getEndFrame().getHeaderFrameNumber(), _iChannel);
    }

    /** Public facing (external) demuxer for Ace Combat 3.
     * Wraps {@link Ac3Demuxer} and sets the {@link FrameNumber} for completed 
     * frames before passing them onto the {@link ICompletedFrameListener}. */
    public static class Demuxer implements ISectorFrameDemuxer, Ac3Demuxer.Listener {

        private final int _iEndFrame;
        @Nonnull
        private final Ac3Demuxer _demuxer;
        private final FrameNumber.FactoryWithHeader _frameNumberFactory = new FrameNumber.FactoryWithHeader();
        @CheckForNull
        private ICompletedFrameListener _listener;

        public Demuxer(int iEndFrame, int iChannel) {
            _iEndFrame = iEndFrame;
            _demuxer = new Ac3Demuxer(iChannel);
            _demuxer.setFrameListener(this);
        }

        public boolean feedSector(@Nonnull IdentifiedSector idSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            // TODO: limit to the range of sectors in this video?
            return _demuxer.feedSector(idSector, log);
        }
        public void flush(@Nonnull ILocalizedLogger log) throws LoggedFailure {
            _demuxer.flush(log);
        }

        public void setFrameListener(@Nonnull ICompletedFrameListener listener) {
            _listener = listener;
        }

        public void frameComplete(@Nonnull Ac3Demuxer.DemuxedAc3Frame frame) throws LoggedFailure {
            frame.setFrame(_frameNumberFactory.next(frame.getStartSector(), 
                                                    _iEndFrame - frame.getInvertedHeaderFrameNumber()));
            if (_listener == null)
                throw new IllegalStateException();
            _listener.frameComplete(frame);
        }

    }

}
