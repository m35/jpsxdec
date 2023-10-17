/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.eavideo;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatter;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.packetbased.DiscItemPacketBasedVideoStream;
import jpsxdec.util.Fraction;

/** An audio/video stream found in some Electronic Arts games. */
public class DiscItemEAVideo extends DiscItemPacketBasedVideoStream {

    public static final String TYPE_ID = "EA Video";
    private static final int SECTORS_PER_FRAME = 10;
    private static final int FPS = 15;

    /** Will be null if all the header frames were the same number. */
    @CheckForNull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    public DiscItemEAVideo(@Nonnull ICdSectorReader cd,
                            int iStartSector, int iEndSector,
                            @Nonnull Dimensions dim,
                            @Nonnull IndexSectorFrameNumber.Format sectorIndexFrameNumberFormat,
                            @CheckForNull HeaderFrameNumber.Format headerFrameNumberFormat,
                            int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector, dim, sectorIndexFrameNumberFormat, iSoundUnitCount);
        _headerFrameNumberFormat = headerFrameNumberFormat;
    }

    public DiscItemEAVideo(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        if (fields.hasField(HeaderFrameNumber.Format.HEADER_FORMAT_KEY))
            _headerFrameNumberFormat = new HeaderFrameNumber.Format(fields);
        else
            _headerFrameNumberFormat = null;
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        if (_headerFrameNumberFormat != null)
            _headerFrameNumberFormat.serialize(serial);
        return serial;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean hasIndependentBitstream() {
        return false;
    }

    @Override
    public @Nonnull FrameNumber getStartFrame() {
        if (_headerFrameNumberFormat == null)
            return _indexSectorFrameNumberFormat.getStartFrame();
        else
            return _headerFrameNumberFormat.getStartFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull FrameNumber getEndFrame() {
        if (_headerFrameNumberFormat == null)
            return _indexSectorFrameNumberFormat.getEndFrame();
        else
            return _headerFrameNumberFormat.getEndFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull List<FrameNumber.Type> getFrameNumberTypes() {
        if (_headerFrameNumberFormat == null)
            return Arrays.asList(FrameNumber.Type.Index, FrameNumber.Type.Sector);
        else
            return Arrays.asList(FrameNumber.Type.Index, FrameNumber.Type.Header, FrameNumber.Type.Sector);
    }

    @Override
    public @Nonnull Fraction getFramesPerSecond() {
        return new Fraction(FPS);
    }

    @Override
    public int getAudioSampleFramesPerSecond() {
        if (hasAudio())
            return EAVideoPacket.SAMPLE_FRAMES_PER_SECOND;
        else
            return -1;
    }

    @Override
    public @Nonnull ISectorClaimToFrameAndAudio makeVideoAudioStream(double dblVolume) {
        if (_headerFrameNumberFormat != null)
            return new Stream(dblVolume, _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat), null);
        else
            return new Stream(dblVolume, null, _indexSectorFrameNumberFormat.makeFormatter());
    }

    public class Stream implements ISectorClaimToFrameAndAudio, EASectorToEAPacket.Listener {
        // only one of these two will not be null
        @CheckForNull
        private final IFrameNumberFormatterWithHeader _fnfwh;
        @CheckForNull
        private final IFrameNumberFormatter _fnf;

        @Nonnull
        private final SpuAdpcmDecoder.Stereo _audioDecoder;

        @CheckForNull
        private IDemuxedFrame.Listener _frameListener;
        @CheckForNull
        private DecodedAudioPacket.Listener _audioListener;

        @CheckForNull
        private EAVideoPacket.VLC0 _vlcPacket;

        private final SectorRange _sectorRange = makeSectorRange();

        public Stream(double dblVolume,
                      @CheckForNull IFrameNumberFormatterWithHeader fnfwh,
                      @CheckForNull IFrameNumberFormatter fnf)
        {
            _audioDecoder = new SpuAdpcmDecoder.Stereo(dblVolume);
            _fnfwh = fnfwh;
            _fnf = fnf;
        }

        @Override
        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            EASectorToEAPacket.attachToSectorClaimer(scs, this);
        }

        @Override
        public void feedPacket(@Nonnull EAVideoPacketSectors packet, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            // Only process packets that are fully in the active sector range
            // (in practice there should never be a packet crossing the border)
            if (!_sectorRange.sectorIsInRange(packet.iStartSector) ||
                !_sectorRange.sectorIsInRange(packet.iEndSector))
            {
                return;
            }

            if (packet.packet instanceof EAVideoPacket.AU) {
                EAVideoPacket.AU au = (EAVideoPacket.AU)packet.packet;
                DecodedAudioPacket aup = au.decode(_audioDecoder);
                if (_audioListener != null)
                    _audioListener.audioPacketComplete(aup, log);
            } else if (packet.packet instanceof EAVideoPacket.MDEC) {
                EAVideoPacket.MDEC mdecPacket = (EAVideoPacket.MDEC) packet.packet;

                int iFrameNumber = mdecPacket.getFrameNumber();
                FrameNumber fn;
                if (_fnf != null) {
                    fn = _fnf.next(packet.iStartSector, log);
                    iFrameNumber = fn.getIndexNumber().getFrameValue();
                } else {
                    fn = _fnfwh.next(packet.iStartSector, mdecPacket.getFrameNumber(), log);
                }

                if (_vlcPacket != null && _frameListener != null) {
                    // problem!!!: we don't have a frame number in some cases
                    // But is there anything we can really do about it?
                    // The location of the packet can't really tell us anything
                    // So I guess we're stuck just using the frame index :P
                    int iPresentationSector = iFrameNumber * SECTORS_PER_FRAME + DiscItemEAVideo.this.getStartSector();
                    _frameListener.frameComplete(new DemuxedEAFrame(packet, mdecPacket, _vlcPacket, fn, new Fraction(iPresentationSector)));
                }
            } else if (packet.packet instanceof EAVideoPacket.VLC0) {
                _vlcPacket = (EAVideoPacket.VLC0)packet.packet;
            }

        }

        @Override
        public void endVideo(@Nonnull ILocalizedLogger log) {
            // probably doesn't matter
        }

        @Override
        public void setFrameListener(@CheckForNull IDemuxedFrame.Listener listener) {
            _frameListener = listener;
        }

        @Override
        public void setAudioListener(@CheckForNull DecodedAudioPacket.Listener listener) {
            _audioListener = listener;
        }

        @Override
        public boolean hasAudio() {
            return DiscItemEAVideo.this.hasAudio();
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat() {
            return EAVideoPacket.EA_VIDEO_AUDIO_FORMAT;
        }

        @Override
        public double getVolume() {
            return _audioDecoder.getVolume();
        }

        @Override
        public int getAbsolutePresentationStartSector() {
            return DiscItemEAVideo.this.getStartSector();
        }

        @Override
        public int getStartSector() {
            return DiscItemEAVideo.this.getStartSector();
        }

        @Override
        public int getEndSector() {
            return DiscItemEAVideo.this.getEndSector();
        }

        @Override
        public int getSampleFramesPerSecond() {
            return EAVideoPacket.SAMPLE_FRAMES_PER_SECOND;
        }

    }
}
