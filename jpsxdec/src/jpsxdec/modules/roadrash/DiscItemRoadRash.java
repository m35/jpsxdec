/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

package jpsxdec.modules.roadrash;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatter;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.packetbased.DiscItemPacketBasedVideoStream;
import jpsxdec.modules.video.packetbased.SectorClaimToAudioAndFrame;
import jpsxdec.util.Fraction;

/** A Road Rash audio/video stream. */
public class DiscItemRoadRash extends DiscItemPacketBasedVideoStream {

    public static final String TYPE_ID = "RoadRash";
    private static final int SECTORS_PER_FRAME = 10;
    private static final int FPS = 15;

    /** Will be null if all the header frames were the same number. */
    @CheckForNull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    public DiscItemRoadRash(@Nonnull CdFileSectorReader cd,
                            int iStartSector, int iEndSector,
                            @Nonnull Dimensions dim,
                            @Nonnull IndexSectorFrameNumber.Format sectorIndexFrameNumberFormat,
                            @CheckForNull HeaderFrameNumber.Format headerFrameNumberFormat,
                            int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector, dim, sectorIndexFrameNumberFormat, iSoundUnitCount);
        _headerFrameNumberFormat = headerFrameNumberFormat;
    }

    public DiscItemRoadRash(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
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
    protected double getPacketBasedFpsInterestingDescription() {
        return FPS;
    }

    @Override
    public @Nonnull Fraction getSectorsPerFrame() {
        return new Fraction(SECTORS_PER_FRAME);
    }

    @Override
    public double getApproxDuration() {
        return getFrameCount() / (double)FPS;
    }

    @Override
    public int getAudioSampleFramesPerSecond() {
        if (hasAudio())
            return RoadRashPacket.SAMPLE_FRAMES_PER_SECOND;
        else
            return -1;
    }

    @Override
    public @Nonnull SectorClaimToAudioAndFrame makeAudioVideoDemuxer(double dblVolume) {
        if (_headerFrameNumberFormat != null)
            return new Demuxer(dblVolume, _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat), null);
        else
            return new Demuxer(dblVolume, null, _indexSectorFrameNumberFormat.makeFormatter());
    }

    public class Demuxer extends SectorClaimToAudioAndFrame
                         implements SectorClaimToRoadRash.Listener
    {
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
        private RoadRashPacket.VLC0 _vlcPacket;

        public Demuxer(double dblVolume,
                       @CheckForNull IFrameNumberFormatterWithHeader fnfwh,
                       @CheckForNull IFrameNumberFormatter fnf)
        {
            _audioDecoder = new SpuAdpcmDecoder.Stereo(dblVolume);
            _fnfwh = fnfwh;
            _fnf = fnf;
        }

        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            SectorClaimToRoadRash s2cs = scs.getClaimer(SectorClaimToRoadRash.class);
            s2cs.setListener(this);
            s2cs.setRangeLimit(getStartSector(), getEndSector());
        }

        public void feedPacket(@Nonnull RoadRashPacketSectors packet, @Nonnull ILocalizedLogger log) throws LoggedFailure {
            if (packet.packet instanceof RoadRashPacket.AU) {
                RoadRashPacket.AU au = (RoadRashPacket.AU)packet.packet;
                DecodedAudioPacket aup = au.decode(_audioDecoder);
                if (_audioListener != null)
                    _audioListener.audioPacketComplete(aup, log);
            } else if (packet.packet instanceof RoadRashPacket.MDEC) {
                RoadRashPacket.MDEC mdecPacket = (RoadRashPacket.MDEC) packet.packet;

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
                    int iPresentationSector = iFrameNumber * SECTORS_PER_FRAME + DiscItemRoadRash.this.getAbsolutePresentationStartSector();
                    _frameListener.frameComplete(new DemuxedRoadRashFrame(packet, mdecPacket, _vlcPacket, fn, new Fraction(iPresentationSector)));
                }
            } else if (packet.packet instanceof RoadRashPacket.VLC0) {
                _vlcPacket = (RoadRashPacket.VLC0)packet.packet;
            }
            
        }

        public void endVideo(@Nonnull ILocalizedLogger log) {
            // probably doesn't matter
        }

        public void setFrameListener(@CheckForNull IDemuxedFrame.Listener listener) {
            _frameListener = listener;
        }

        public void setAudioListener(@CheckForNull DecodedAudioPacket.Listener listener) {
            _audioListener = listener;
        }

        public @Nonnull AudioFormat getOutputFormat() {
            return RoadRashPacket.ROAD_RASH_AUDIO_FORMAT;
        }

        public double getVolume() {
            return _audioDecoder.getVolume();
        }

        public int getAbsolutePresentationStartSector() {
            return DiscItemRoadRash.this.getStartSector();
        }

        public int getStartSector() {
            return DiscItemRoadRash.this.getStartSector();
        }

        public int getEndSector() {
            return DiscItemRoadRash.this.getEndSector();
        }

        public int getSampleFramesPerSecond() {
            return RoadRashPacket.SAMPLE_FRAMES_PER_SECOND;
        }

        public int getDiscSpeed() {
            return 2;
        }

    }
}
