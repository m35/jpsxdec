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

package jpsxdec.modules.policenauts;

import java.io.ByteArrayOutputStream;
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
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.packetbased.DiscItemPacketBasedVideoStream;
import jpsxdec.modules.video.packetbased.SectorClaimToAudioAndFrame;
import jpsxdec.util.Fraction;

public class DiscItemPolicenauts extends DiscItemPacketBasedVideoStream {

    public static final String TYPE_ID = "Policenauts";

    @Nonnull
    private final HeaderFrameNumber.Format _timestampFrameNumberFormat;

    public DiscItemPolicenauts(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector,
                               @Nonnull Dimensions dim,
                               @Nonnull IndexSectorFrameNumber.Format sectorIndexFrameNumberFormat,
                               @Nonnull HeaderFrameNumber.Format timestampFrameNumberFormat,
                               int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector, dim, sectorIndexFrameNumberFormat, iSoundUnitCount);
        _timestampFrameNumberFormat = timestampFrameNumberFormat;
    }

    public DiscItemPolicenauts(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _timestampFrameNumberFormat = new HeaderFrameNumber.Format(fields);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _timestampFrameNumberFormat.serialize(serial);
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
        return _timestampFrameNumberFormat.getStartFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public FrameNumber getEndFrame() {
        return _timestampFrameNumberFormat.getEndFrame(_indexSectorFrameNumberFormat);
    }

    @Override
    public @Nonnull List<FrameNumber.Type> getFrameNumberTypes() {
        return Arrays.asList(FrameNumber.Type.Index, FrameNumber.Type.Header, FrameNumber.Type.Sector);
    }

    @Override
    protected double getPacketBasedFpsInterestingDescription() {
        return SPacket.FRAMES_PER_SECOND.asDouble();
    }

    @Override
    public @Nonnull Fraction getSectorsPerFrame() {
        return SPacket.SECTORS150_PER_FRAME;
    }

    @Override
    public double getApproxDuration() {
        return getFrameCount() / SPacket.FRAMES_PER_SECOND.asDouble();
    }

    @Override
    public int getAudioSampleFramesPerSecond() {
        return SPacket.AUDIO_SAMPLE_FRAMES_PER_SECOND;
    }

    @Override
    public @Nonnull SectorClaimToAudioAndFrame makeAudioVideoDemuxer(double dblVolume) {
        return new Demuxer(getWidth(), getHeight(), getStartSector(), dblVolume,
                _timestampFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat));
    }

    public class Demuxer extends SectorClaimToAudioAndFrame
                         implements SectorClaimToPolicenauts.Listener
    {
        private final int _iWidth, _iHeight;
        @Nonnull
        private final IFrameNumberFormatterWithHeader _fnf;
        private final int _iStartSector;

        @CheckForNull
        private IDemuxedFrame.Listener _frameListener;
        @CheckForNull
        private DecodedAudioPacket.Listener _audioListener;

        @Nonnull
        private final SpuAdpcmDecoder.Mono _audioDecoder;
        private Fraction _zeroTimestampOffset = Fraction.ZERO;
        private boolean _blnPrevTimestampWas0 = false;
        private int _iPrevDuration = 0;

        private final ByteArrayOutputStream _pcmOut = new ByteArrayOutputStream();

        public Demuxer(int iWidth, int iHeight, int iStartSector, double dblVolume,
                       @Nonnull IFrameNumberFormatterWithHeader fnf)
        {
            _iWidth = iWidth;
            _iHeight = iHeight;
            _iStartSector = iStartSector;
            _audioDecoder = new SpuAdpcmDecoder.Mono(dblVolume);
            _fnf = fnf;
        }

        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            SectorClaimToPolicenauts s2cs = scs.getClaimer(SectorClaimToPolicenauts.class);
            s2cs.setListener(this);
            s2cs.setRangeLimit(getStartSector(), getEndSector());
        }

        public void videoStart(int iWidth, int iHeight, ILocalizedLogger log) {
            // not important here
        }

        @Override
        public void feedPacket(@Nonnull SPacketData packet, @Nonnull ILocalizedLogger log) throws LoggedFailure {

            if (packet.isAudio()) {
                if (_audioListener != null) {
                    _pcmOut.reset();
                    // The audio timestamp is a pain
                    // The first 3 audio packets all start at 0 and are 156 long
                    // after that, the numbers add up correctly
                    if (_blnPrevTimestampWas0 && packet.getTimestamp() == 0) {
                        _zeroTimestampOffset = _zeroTimestampOffset.add(SPacket.SECTORS150_PER_TIMESTAMP.multiply(_iPrevDuration));
                    }
                    //System.out.println(_zeroTimestampOffset + " -------- " + packet);
                    Fraction close = SPacket.SECTORS150_PER_TIMESTAMP.multiply(packet.getTimestamp()).add(_iStartSector).add(_zeroTimestampOffset);
                    _blnPrevTimestampWas0 = packet.getTimestamp() == 0;
                    _iPrevDuration = packet.getDuration();
                    packet.decodeAudio(_audioDecoder, _pcmOut);
                    DecodedAudioPacket aup = new DecodedAudioPacket(0, SPacket.AUDIO_FORMAT, close, _pcmOut.toByteArray());
                    _audioListener.audioPacketComplete(aup, log);
                }
            } else if (packet.isVideo()) {
                FrameNumber fn = _fnf.next(packet.getStartSector(), packet.getTimestamp(), log);
                if (_frameListener != null) {
                    _frameListener.frameComplete(new DemuxPolicenautsFrame(_iWidth, _iHeight, packet, fn,
                            SPacket.SECTORS150_PER_TIMESTAMP.multiply(packet.getTimestamp()).add(_iStartSector)));
                }
            }
        }

        public void endOfSectors(ILocalizedLogger log) {
            // not important here
        }

        public void setFrameListener(@CheckForNull IDemuxedFrame.Listener listener) {
            _frameListener = listener;
        }

        public void setAudioListener(@Nonnull DecodedAudioPacket.Listener listener) {
            _audioListener = listener;
        }

        public @Nonnull AudioFormat getOutputFormat() {
            return SPacket.AUDIO_FORMAT;
        }

        public double getVolume() {
            return _audioDecoder.getVolume();
        }

        public int getAbsolutePresentationStartSector() {
            return DiscItemPolicenauts.this.getStartSector();
        }

        public int getStartSector() {
            return DiscItemPolicenauts.this.getStartSector();
        }

        public int getEndSector() {
            return DiscItemPolicenauts.this.getEndSector();
        }

        public int getSampleFramesPerSecond() {
            return SPacket.AUDIO_SAMPLE_FRAMES_PER_SECOND;
        }

        public int getDiscSpeed() {
            return 2;
        }

    }


}
