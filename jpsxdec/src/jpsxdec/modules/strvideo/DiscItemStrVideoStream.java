/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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

package jpsxdec.modules.strvideo;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DiscItemAudioStream;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DemuxedFrameWithNumberAndDims;
import jpsxdec.modules.video.sectorbased.DiscItemSectorBasedVideoStream;
import jpsxdec.modules.video.sectorbased.IVideoSectorWithFrameNumber;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfo;
import jpsxdec.modules.xa.DiscItemXaAudioStream;

/** Handles most variations of PlayStation video streams. Since it handles the
 * only real standard of video streams, we will call them "STR" videos.
 * The definition of a STR video is
 * - the demux frame data is divided up into sectors
 * - each video sector only contains video data
 * - the headers of these video sectors contain a sequential frame number
 * However, even if a video meets that definition, if it has some other
 * special properties, then another disc item type will need to be used. */
public class DiscItemStrVideoStream extends DiscItemSectorBasedVideoStream {

    public static final String TYPE_ID = "Video";

    @Nonnull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    public DiscItemStrVideoStream(@Nonnull CdFileSectorReader cd, int iStartSector,
                                  int iEndSector, @Nonnull Dimensions dim,
                                  @Nonnull IndexSectorFrameNumber.Format indexSectorFrameNumberFormat,
                                  @Nonnull SectorBasedVideoInfo strVidInfo,
                                  @Nonnull HeaderFrameNumber.Format headerFrameNumberFormat)
    {
        super(cd, iStartSector, iEndSector, dim, indexSectorFrameNumberFormat, strVidInfo);
        _headerFrameNumberFormat = headerFrameNumberFormat;
    }

    public DiscItemStrVideoStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _headerFrameNumberFormat = new HeaderFrameNumber.Format(fields);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _headerFrameNumberFormat.serialize(serial);
        return serial;
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

    @Override
    public int findAudioSplitPoint(@Nonnull DiscItemXaAudioStream audio) {
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
    public @Nonnull FrameNumber getStartFrame() {
        return _headerFrameNumberFormat.getStartFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public FrameNumber getEndFrame() {
        return _headerFrameNumberFormat.getEndFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull List<FrameNumber.Type> getFrameNumberTypes() {
        return Arrays.asList(FrameNumber.Type.Index, FrameNumber.Type.Header, FrameNumber.Type.Sector);
    }

    @Override
    public void fpsDump(@Nonnull PrintStream ps) throws CdFileSectorReader.CdReadException {

        SectorClaimSystem it = createClaimSystem();
        for (int iSector = 0; it.hasNext(); iSector++) {
            IIdentifiedSector isect = it.next(DebugLogger.Log).getClaimer();
            if (isect instanceof IVideoSectorWithFrameNumber) {
                IVideoSectorWithFrameNumber vidSect = (IVideoSectorWithFrameNumber) isect;
                ps.println(String.format("%-5d %-4d %d/%d",
                                        iSector,
                                        vidSect.getHeaderFrameNumber(),
                                        vidSect.getChunkNumber(),
                                        vidSect.getChunksInFrame()));
            } else {
                ps.println(String.format(
                        "%-5d X",
                        iSector));
            }

        }
    }

    @Override
    public @Nonnull ISectorClaimToDemuxedFrame makeDemuxer() {
        return new Demuxer(getStartSector(), getEndSector(),
                           _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat));
    }

    public static class Demuxer implements ISectorClaimToDemuxedFrame, DemuxedFrameWithNumberAndDims.Listener {

        private int _iStartSector = 0;
        private int _iEndSectorInclusive = Integer.MAX_VALUE;
        @Nonnull
        private final IFrameNumberFormatterWithHeader _frameNumberFormatter;
        @CheckForNull
        private IDemuxedFrame.Listener _listener;

        public Demuxer(int iStartSector, int iEndSectorInclusive,
                       @Nonnull IFrameNumberFormatterWithHeader frameNumberFormatter)
        {
            _iStartSector = iStartSector;
            _iEndSectorInclusive = iEndSectorInclusive;
            _frameNumberFormatter = frameNumberFormatter;
        }

        public void setFrameListener(@Nonnull IDemuxedFrame.Listener listener) {
            _listener = listener;
        }

        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            SectorClaimToStrVideoSector s2sv = scs.getClaimer(SectorClaimToStrVideoSector.class);
            s2sv.setRangeLimit(_iStartSector, _iEndSectorInclusive);
            s2sv.setListener(new StrVideoSectorToDemuxedStrFrame(this));
        }
        
        public void frameComplete(@Nonnull DemuxedFrameWithNumberAndDims frame, @Nonnull ILocalizedLogger log) {
            FrameNumber fn = _frameNumberFormatter.next(frame.getStartSector(),
                                                        frame.getHeaderFrameNumber(), log);
            frame.setFrame(fn);
            if (_listener != null)
                _listener.frameComplete(frame);
        }
        public void endOfSectors(@Nonnull ILocalizedLogger log) {
        }

    }
    
}
