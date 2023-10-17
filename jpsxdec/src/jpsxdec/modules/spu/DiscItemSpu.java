/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

package jpsxdec.modules.spu;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.adpcm.SoundUnitDecoder;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DemuxedSectorInputStream;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.ExposedBAOS;
import jpsxdec.util.Misc;

/** Represents a PlayStation Sound Processing Unit (SPU) audio clip.
 * There's no way to know the sample rate of SPU clips, so the user
 * needs to listen to the clip and choose the right sample rate.
 * It would be ideal to save that new sample rate in the index.
 * TODO: on sample rate change, signal that the index has changed and should be saved again
 */
public class DiscItemSpu extends DiscItem implements DiscItem.IHasStartOffset {

    public static final String TYPE_ID = "SPU";

    /** The sample rate of SPU clips is unknown, but most seem to be of this
     * frequency (stolen from PSound). */
    private static final int DEFAULT_SAMPLE_RATE = 22050;

    private final static String START_OFFSET_KEY = "Start Offset";
    private final int _iStartOffset;
    private final static String END_OFFSET_KEY = "End Offset";
    private final int _iEndOffset;
    private final static String SOUNDUNIT_COUNT_KEY = "Sound Unit Count";
    private final int _iSoundUnitCount;
    private final static String SAMPLE_RATE_KEY = "Sample Rate";
    private int _iSampleRate;

    public DiscItemSpu(@Nonnull ICdSectorReader cd,
                       int iStartSector, int iStartOffset,
                       int iEndSector, int iEndOffset,
                       int iSoundUnitCount)
    {
        super(cd, iStartSector, iEndSector);
        _iStartOffset = iStartOffset;
        _iEndOffset = iEndOffset;
        _iSoundUnitCount = iSoundUnitCount;
        _iSampleRate = DEFAULT_SAMPLE_RATE;
    }

    public DiscItemSpu(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _iStartOffset = fields.getInt(START_OFFSET_KEY);
        _iEndOffset = fields.getInt(END_OFFSET_KEY);
        _iSoundUnitCount = fields.getInt(SOUNDUNIT_COUNT_KEY);
        _iSampleRate = fields.getInt(SAMPLE_RATE_KEY);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem ser = super.serialize();
        ser.addNumber(START_OFFSET_KEY, _iStartOffset);
        ser.addNumber(END_OFFSET_KEY, _iEndOffset);
        ser.addNumber(SOUNDUNIT_COUNT_KEY, _iSoundUnitCount);
        ser.addNumber(SAMPLE_RATE_KEY, _iSampleRate);
        return ser;
    }

    @Override
    public int getStartOffset() {
        return _iStartOffset;
    }

    public int getSoundUnitCount() {
        return _iSoundUnitCount;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public @Nonnull GeneralType getType() {
        return GeneralType.Sound;
    }

    public int getSampleRate() {
        return _iSampleRate;
    }

    /** Unlike all other {@link DiscItem}s, a field could be changed after
     * creation.
     * TODO: notify GUI that the description has changed and should be refreshed */
    public void setSampleRate(int iSampleRate) {
        _iSampleRate = iSampleRate;
    }

    public @Nonnull AudioFormat getFormat() {
        return new AudioFormat(_iSampleRate, 16, 1, true, false);
    }


    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        int iPcmSampleCount = _iSoundUnitCount * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        double dblApproxDuration = iPcmSampleCount / (double)_iSampleRate;
        Date secs = Misc.dateFromSeconds(Math.max((int)dblApproxDuration, 1));
        return new UnlocalizedMessage(MessageFormat.format("{0} samples at {1} Hz = {2,time,m:ss}", iPcmSampleCount, _iSampleRate, secs)); // I18N
    }

    @Override
    public @Nonnull SpuSaverBuilder makeSaverBuilder() {
        return new SpuSaverBuilder(this);
    }

    public @Nonnull InputStream getSpuStream() {
        return new DemuxedSectorInputStream(getSourceCd(),
                                            getStartSector(), getStartOffset());
    }

    public @Nonnull AudioInputStream getAudioStream(double dblVolume) {
        InputStream stream = getSpuStream();
        SpuInputStream spuStream = new SpuInputStream(stream, dblVolume);
        AudioInputStream ais = new AudioInputStream(spuStream, getFormat(),
                (long)_iSoundUnitCount*SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);
        return ais;
    }

    private static class SpuInputStream extends InputStream {
        private static final int BYTES_PER_SOUND_UNIT = SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT*2;

        @Nonnull
        private final InputStream _spuIn;
        @Nonnull
        private final SpuAdpcmDecoder.Mono _decoder;
        private final ExposedBAOS _decodeBuffer = new ExposedBAOS(BYTES_PER_SOUND_UNIT);
        private int _iReadOfs = BYTES_PER_SOUND_UNIT;

        public SpuInputStream(@Nonnull InputStream spuIn, double dblVolume) {
            _spuIn = spuIn;
            _decoder = new SpuAdpcmDecoder.Mono(dblVolume);
        }

        @Override
        public int read() throws IOException {
            if (_iReadOfs >= BYTES_PER_SOUND_UNIT) {
                _decodeBuffer.reset();
                _decoder.decode(_spuIn, 1, _decodeBuffer);
                _iReadOfs = 0;
            }
            byte b = _decodeBuffer.getBuffer()[_iReadOfs];
            _iReadOfs++;
            return b & 0xff;
        }

    }

}
