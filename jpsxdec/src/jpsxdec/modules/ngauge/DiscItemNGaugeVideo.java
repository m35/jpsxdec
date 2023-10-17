/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.ngauge;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.sectorbased.DiscItemSectorBasedVideoStream;
import jpsxdec.modules.video.sectorbased.SectorBasedVideoInfo;
import jpsxdec.modules.video.sectorbased.SectorClaimToSectorBasedDemuxedFrame;
import jpsxdec.modules.xa.DiscItemXaAudioStream;

/** It would be nice to use the existing normal STR disc item, but we need the
 * header sector as part of the disc item to properly identify the video stream.
 *
 * @see NGaugeVideoInfo */
public class DiscItemNGaugeVideo extends DiscItemSectorBasedVideoStream {

    private static final Logger LOG = Logger.getLogger(DiscItemNGaugeVideo.class.getName());

    public static final String TYPE_ID = "N-Gauge Video";

    @Nonnull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    public DiscItemNGaugeVideo(@Nonnull ICdSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull Dimensions dim,
                               @Nonnull IndexSectorFrameNumber.Format indexSectorFrameNumberFormat,
                               @Nonnull HeaderFrameNumber.Format headerFrameNumberFormat,
                               @Nonnull SectorBasedVideoInfo vidInfo)
    {
        super(cd, iStartSector, iEndSector, dim, indexSectorFrameNumberFormat, vidInfo);
        _headerFrameNumberFormat = headerFrameNumberFormat;
    }

    public DiscItemNGaugeVideo(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
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
    public @Nonnull DiscSpeed getDiscSpeed() {
        return DiscSpeed.SINGLE;
    }

    @Override
    public int findAudioSplitPoint(@Nonnull DiscItemXaAudioStream audio) {
        return -1; // there should be no audio
    }

    @Override
    public @Nonnull FrameNumber getStartFrame() {
        return _headerFrameNumberFormat.getStartFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull FrameNumber getEndFrame() {
        return _headerFrameNumberFormat.getEndFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull List<FrameNumber.Type> getFrameNumberTypes() {
        return Arrays.asList(FrameNumber.Type.Index, FrameNumber.Type.Header, FrameNumber.Type.Sector);
    }

    @Override
    public boolean hasIndependentBitstream() {
        return true;
    }

    @Override
    public @Nonnull SectorClaimToSectorBasedDemuxedFrame makeDemuxer() {
        return new Demuxer(makeSectorRange(), _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat));
    }

    private static class Demuxer extends SectorClaimToSectorBasedDemuxedFrame implements NGaugeSectorToFrame.Listener {

        @Nonnull
        private final IFrameNumberFormatterWithHeader _frameNumberFormatter;

        public Demuxer(@Nonnull SectorRange sectorRange,
                       @Nonnull IFrameNumberFormatterWithHeader frameNumberFormatter)
        {
            super(sectorRange);
            _frameNumberFormatter = frameNumberFormatter;
        }

        @Override
        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            NGaugeSectorToFrame s2f = new NGaugeSectorToFrame(_sectorRange, this);
            scs.addIdListener(s2f);
        }

        @Override
        public void videoStart(NGaugeVideoInfo videoInfo) {
        }

        @Override
        public void frameComplete(@Nonnull DemuxedNGaugeFrame frame, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            FrameNumber fn = _frameNumberFormatter.next(frame.getStartSector(),
                                                        frame.getCalculatedFrameNumber(), log);
            frame._frameNumber = fn;
            demuxedFrameComplete(frame);
        }

        @Override
        public void videoBreak() {
            LOG.warning("N-Gauge Unten Kibun Game - Gatan Goton video break while saving");
        }

        @Override
        public void endOfSectors() {
        }

    }

}
