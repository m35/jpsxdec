/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2019  Michael Sabin
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
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.player.MediaPlayer;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IFrameNumberFormatterWithHeader;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.util.Fraction;
import jpsxdec.util.player.PlayController;

/** Crusader: No Remorse audio/video stream. */
public class DiscItemCrusader extends DiscItemVideoStream {

    public static final String TYPE_ID = "Crusader";
    private static final Fraction SECTORS_PER_FRAME = new Fraction(10);
    private static final int FPS = 15;

    @Nonnull
    private final HeaderFrameNumber.Format _headerFrameNumberFormat;

    private static final String INITIAL_PRES_SECTOR_KEY = "Initial presentation sector";
    private final int _iRelativeInitialFramePresentationSector;

    private static final String SOUND_UNIT_COUNT_KEY = "Sound unit count";
    private final int _iSoundUnitCount;

    public DiscItemCrusader(@Nonnull CdFileSectorReader cd,
                            int iStartSector, int iEndSector,
                            @Nonnull Dimensions dim,
                            @Nonnull IndexSectorFrameNumber.Format sectorIndexFrameNumberFormat,
                            @Nonnull HeaderFrameNumber.Format headerFrameNumberFormat,
                            int iInitialPresentationSector,
                            int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector, dim, sectorIndexFrameNumberFormat);
        _headerFrameNumberFormat = headerFrameNumberFormat;
        _iRelativeInitialFramePresentationSector = iInitialPresentationSector;
        _iSoundUnitCount = iSoundUnitCount;
    }
    
    public DiscItemCrusader(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _headerFrameNumberFormat = new HeaderFrameNumber.Format(fields);
        _iSoundUnitCount = fields.getInt(SOUND_UNIT_COUNT_KEY);
        _iRelativeInitialFramePresentationSector = fields.getInt(INITIAL_PRES_SECTOR_KEY);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _headerFrameNumberFormat.serialize(serial);
        serial.addNumber(SOUND_UNIT_COUNT_KEY, _iSoundUnitCount);
        serial.addNumber(INITIAL_PRES_SECTOR_KEY, _iRelativeInitialFramePresentationSector);
        return serial;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
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
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        int iFrames = getFrameCount();
        Date secs = new Date(0, 0, 0, 0, 0, Math.max(iFrames / FPS, 1));
        return I.GUI_CRUSADER_VID_DETAILS(getWidth() ,getHeight(), iFrames, FPS, secs);
    }
    
    @Override
    public @Nonnull VideoSaverBuilderCrusader makeSaverBuilder() {
        return new VideoSaverBuilderCrusader(this);
    }
    
    @Override
    public int getDiscSpeed() {
        return 2; // pretty sure it plays back at 2x
    }

    @Override
    public @Nonnull Fraction getSectorsPerFrame() {
        return SECTORS_PER_FRAME;
    }

    @Override
    public int getAbsolutePresentationStartSector() {
        return getStartSector();
    }

    @Override
    public double getApproxDuration() {
        return getFrameCount() / (double)FPS;
    }

    @Override
    public @Nonnull Demuxer makeDemuxer() {
        return makeDemuxer(1.0);
    }

    public @Nonnull Demuxer makeDemuxer(double dblVolume) {
        return new Demuxer(dblVolume, _headerFrameNumberFormat.makeFormatter(_indexSectorFrameNumberFormat));
    }

    @Override
    public @Nonnull PlayController makePlayController() {
        Demuxer demuxer = makeDemuxer();
        return new PlayController(new MediaPlayer(this, demuxer, demuxer, getStartSector(), getEndSector()));
    }

    public class Demuxer implements ISectorClaimToDemuxedFrame, ISectorAudioDecoder,
                                    SectorClaimToSectorCrusader.Listener,
                                    CrusaderPacketToFrameAndAudio.FrameListener
    {

        private final double _dblVolume;
        @Nonnull
        private final IFrameNumberFormatterWithHeader _frameNumberFormatter;
        @CheckForNull
        private IDemuxedFrame.Listener _listener;
        @Nonnull
        private final CrusaderPacketToFrameAndAudio _cp2a;
        @Nonnull
        private final CrusaderSectorToCrusaderPacket _cs2cp;

        public Demuxer(double dblVolume,
                       @Nonnull IFrameNumberFormatterWithHeader frameNumberFormatter)
        {
            _dblVolume = dblVolume;
            _frameNumberFormatter = frameNumberFormatter;
            int iAbsoluteInitialFramePresentationSector = _iRelativeInitialFramePresentationSector +
                                                          DiscItemCrusader.this.getAbsolutePresentationStartSector();
            _cp2a = new CrusaderPacketToFrameAndAudio(
                    dblVolume, iAbsoluteInitialFramePresentationSector, this);
            _cs2cp = new CrusaderSectorToCrusaderPacket(_cp2a);
        }
        public void setFrameListener(@CheckForNull IDemuxedFrame.Listener listener) {
            _listener = listener;
        }

        public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
            SectorClaimToSectorCrusader s2cs = scs.getClaimer(SectorClaimToSectorCrusader.class);
            s2cs.setListener(this);
            s2cs.setRangeLimit(getStartSector(), getEndSector());
        }

        public void sectorRead(@Nonnull SectorCrusader sector, @Nonnull ILocalizedLogger log) {
            _cs2cp.sectorRead(sector, log);
        }
        public void endOfSectors(@Nonnull ILocalizedLogger log) {
            _cs2cp.endVideo(log);
        }


        public void frameComplete(@Nonnull DemuxedCrusaderFrame frame, @Nonnull ILocalizedLogger log) {
            frame.setFrame(_frameNumberFormatter.next(frame.getStartSector(), frame.getHeaderFrameNumber(), log));
            if (_listener != null)
                _listener.frameComplete(frame);
        }

        public void videoEnd(@Nonnull ILocalizedLogger log, int iStartSector,
                             int iEndSector)
        {
            // might happen at the end of saving a movie, don't care
        }

        public void setAudioListener(@Nonnull DecodedAudioPacket.Listener listener) {
            _cp2a.setAudioListener(listener);
        }

        public @Nonnull AudioFormat getOutputFormat() {
            return CrusaderPacketToFrameAndAudio.CRUSADER_AUDIO_FORMAT;
        }

        public double getVolume() {
            return _dblVolume;
        }

        public int getAbsolutePresentationStartSector() {
            return DiscItemCrusader.this.getAbsolutePresentationStartSector();
        }

        public int getStartSector() {
            return DiscItemCrusader.this.getStartSector();
        }

        public int getEndSector() {
            return DiscItemCrusader.this.getEndSector();
        }

        public int getSampleFramesPerSecond() {
            return CrusaderPacketToFrameAndAudio.CRUSADER_SAMPLES_PER_SECOND;
        }

        public int getDiscSpeed() {
            return 2;
        }

    }

}
