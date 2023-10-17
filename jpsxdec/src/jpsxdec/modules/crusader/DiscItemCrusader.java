/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.packetbased.DiscItemPacketBasedVideoStream;
import jpsxdec.util.Fraction;

/** Crusader: No Remorse audio/video stream. */
public class DiscItemCrusader extends DiscItemPacketBasedVideoStream {

    public static final String TYPE_ID = "Crusader";

    @Nonnull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    public DiscItemCrusader(@Nonnull ICdSectorReader cd,
                            int iStartSector, int iEndSector,
                            @Nonnull Dimensions dim,
                            @Nonnull IndexSectorFrameNumber.Format sectorIndexFrameNumberFormat,
                            @Nonnull HeaderFrameNumber.Format headerFrameNumberFormat,
                            int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector, dim, sectorIndexFrameNumberFormat, iSoundUnitCount);
        _headerFrameNumberFormat = headerFrameNumberFormat;
    }

    public DiscItemCrusader(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
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
    public boolean hasIndependentBitstream() {
        return true;
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
    public @Nonnull Fraction getFramesPerSecond() {
        return new Fraction(CrusaderPacket.FRAMES_PER_SECOND);
    }

    @Override
    public int getAudioSampleFramesPerSecond() {
        return CrusaderPacket.CRUSADER_SAMPLE_FRAMES_PER_SECOND;
    }

    @Override
    public @Nonnull ISectorClaimToFrameAndAudio makeVideoAudioStream(double dblVolume) {
        return new Stream(dblVolume, _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat));
    }

    public class Stream implements ISectorClaimToFrameAndAudio, CrusaderPacketToFrameAndAudio.FrameListener {

        private final double _dblVolume;
        @Nonnull
        private final IFrameNumberFormatterWithHeader _frameNumberFormatter;
        @Nonnull
        private final CrusaderPacketToFrameAndAudio _cp2a;
        @Nonnull
        private final CrusaderSectorToCrusaderPacket _cs2cp;

        @CheckForNull
        private IDemuxedFrame.Listener _listener;

        public Stream(double dblVolume,
                      @Nonnull IFrameNumberFormatterWithHeader frameNumberFormatter)
        {
            _dblVolume = dblVolume;
            _frameNumberFormatter = frameNumberFormatter;
            _cp2a = new CrusaderPacketToFrameAndAudio(
                    dblVolume, DiscItemCrusader.this.getStartSector(), this);
            _cs2cp = new CrusaderSectorToCrusaderPacket(DiscItemCrusader.this.makeSectorRange(), _cp2a);
        }
        @Override
        public void setFrameListener(@CheckForNull IDemuxedFrame.Listener listener) {
            _listener = listener;
        }

        @Override
        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            scs.addIdListener(_cs2cp);
        }

        @Override
        public void frameComplete(@Nonnull DemuxedCrusaderFrame frame, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            frame.setFrame(_frameNumberFormatter.next(frame.getStartSector(), frame.getHeaderFrameNumber(), log));
            if (_listener != null)
                _listener.frameComplete(frame);
        }

        @Override
        public void videoEnd(@Nonnull ILocalizedLogger log) {
            // might happen at the end of saving a movie, don't care
        }

        @Override
        public void setAudioListener(@Nonnull DecodedAudioPacket.Listener listener) {
            _cp2a.setAudioListener(listener);
        }

        @Override
        public boolean hasAudio() {
            return true;
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat() {
            return CrusaderPacket.CRUSADER_AUDIO_FORMAT;
        }

        @Override
        public double getVolume() {
            return _dblVolume;
        }

        @Override
        public int getAbsolutePresentationStartSector() {
            return getStartSector();
        }

        @Override
        public int getStartSector() {
            return DiscItemCrusader.this.getStartSector();
        }

        @Override
        public int getEndSector() {
            return DiscItemCrusader.this.getEndSector();
        }

        @Override
        public int getSampleFramesPerSecond() {
            return CrusaderPacket.CRUSADER_SAMPLE_FRAMES_PER_SECOND;
        }

    }

}
