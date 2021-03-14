/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2020  Michael Sabin
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

package jpsxdec.adpcm;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

/** Encodes audio data into the PlayStation/CD-i/Green Book ADPCM format,
 *  one sector at a time.
 * <p>
 * The input is an audio stream at a matching quality
 * (16 bits/sample at 37800 Hz or 18900 Hz in mono or stereo).
 * The output is the "user data" for a Mode 2 Form 2 real-time XA audio sector.
 * <p>
 * Will encode audio as long as source data is available. If there is no
 * more source data, silence is encoded. Check for EOF condition with
 * {@link #isEof()}. */
public class XaAdpcmEncoder implements Closeable {

    /** Source audio stream. */
    @Nonnull
    private final AudioShortReader _audioShortReader;

    /** Sample rate of source/output stream. Must be 18900 or 37800 Hz */
    private final int _iSampleRate;
    /** true=encode to 4 bits/sample, false=encode to 8 bits/sample. */
    private final boolean _blnEncode4BitsElse8Bits;

    /** Running ADPCM encoders(s), an encoder for each channel (1 or 2). */
    @Nonnull
    private final SoundUnitEncoder[] _aoEncoders;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    private final LogContext _logContext = new LogContext();

    public long getSampleFramesRead() {
        return _audioShortReader.getSampleFramesRead();
    }

    public static class LogContext implements IContextCopier {
        /** The number of sectors that have been encoded. */
        public int iEncodedSectorCount = 0;
        /** The sound group being encoded. */
        public int iSoundGroup = -1;
        /** The sound unit being encoded. */
        public int iSoundUnit = -1;
        /** Audio channel being encoded (0 or 1). */
        public int iChannel = -1;
        /** The number of PCM sample frames that have been read
         * (i.e. a stereo sample frame is only 1 sample frame).
         * Used to help find where in the input stream data was being encoded. */
        public long lngSamplesFramesRead = 0;

        private LogContext() {}

        @Override
        public @Nonnull LogContext copy() {
            LogContext cpy = new LogContext();
            cpy.iEncodedSectorCount = iEncodedSectorCount;
            cpy.iSoundGroup = iSoundGroup;
            cpy.iSoundUnit = iSoundUnit;
            cpy.iChannel = iChannel;
            cpy.lngSamplesFramesRead = lngSamplesFramesRead;
            return cpy;
        }

        @Override
        public String toString() {
            return String.format(
                    "Sector %d Sound Group.Unit %d.%d Channel %d after Sample Frame %d",
                    iEncodedSectorCount, iSoundGroup, iSoundUnit, iChannel, lngSamplesFramesRead);
        }
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Set by {@link #setPresetParameters(java.io.InputStream)}.
     *  Primarily for development/testing purposes. */
    @CheckForNull
    private InputStream _presetPrameters = null;

    /** Create a new encoder for the given audio stream, to be encoded with the
     * given bits.
     * @param ais Must be 16 bits/sample at 37800 Hz or 18900 Hz in mono or stereo.
     * @param iEncodeToAdpcmBitsPerSample Must be 4 or 8
     * @throws UnsupportedOperationException if source audio format is incorrect.
     */
    public XaAdpcmEncoder(@Nonnull AudioInputStream ais, int iEncodeToAdpcmBitsPerSample)
            throws IncompatibleException
    {
        if (iEncodeToAdpcmBitsPerSample == 4)
            _blnEncode4BitsElse8Bits = true;
        else if (iEncodeToAdpcmBitsPerSample == 8)
            _blnEncode4BitsElse8Bits = false;
        else // programmer error
            throw new IllegalArgumentException("Invalid encoding bits/sample "+iEncodeToAdpcmBitsPerSample+", must be 4 or 8");

        AudioFormat fmt = ais.getFormat();
        if (Math.abs(fmt.getSampleRate() - 37800) > 0.1f)
            _iSampleRate = 37800;
        else if (Math.abs(fmt.getSampleRate() - 18900) > 0.1f)
            _iSampleRate = 18900;
        else
            throw new IncompatibleException("Unsupported sample rate "+fmt.getSampleRate()+", must be 18900 or 37800");

        // will verify all other format details are valid
        _audioShortReader = new AudioShortReader(ais);

        int iChannels = fmt.getChannels();
        _aoEncoders = new SoundUnitEncoder[iChannels];
        for (int i = 0; i < iChannels; i++) {
            _aoEncoders[i] = new SoundUnitEncoder(iEncodeToAdpcmBitsPerSample,
                                                  K0K1Filter.XA);
        }
    }

    public boolean isStereo() {
        return _aoEncoders.length == 2;
    }

    public int getSampleRate() {
        return _iSampleRate;
    }

    public boolean isEof() {
        return _audioShortReader.isEof();
    }

    @Override
    public void close() throws IOException {
        _audioShortReader.close();
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetPrameters = presetParameters;
    }

    /** From the source audio stream, reads either 4032 sample frames
     * (for 4 bits/sample), or 2016 sample frames (for 8 bits/sample).
     * And writes 2304 bytes to the provided output stream. Note that the
     * last 20 bytes of the sector are not included in the output.
     * If the end of the audio stream is reached, silence will be written
     * for the remainder to the output. */
    public void encode1Sector(@Nonnull OutputStream os) throws IOException {
        for (_logContext.iSoundGroup = 0;
             _logContext.iSoundGroup < XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR;
             _logContext.iSoundGroup++)
        {
            encodeSoundGroup(os);
        }
        _logContext.iSoundGroup = -1;
        _logContext.iEncodedSectorCount++;
    }

    private void encodeSoundGroup(@Nonnull OutputStream os) throws IOException {
        SoundUnitEncoder.EncodedUnit[] aoEncoded;
        if (_blnEncode4BitsElse8Bits)
            aoEncoded = new SoundUnitEncoder.EncodedUnit[XaAdpcmDecoder.SOUND_UNITS_IN_4_BIT_SOUND_GROUP];
        else
            aoEncoded = new SoundUnitEncoder.EncodedUnit[XaAdpcmDecoder.SOUND_UNITS_IN_8_BIT_SOUND_GROUP];

        for (_logContext.iSoundUnit = 0; _logContext.iSoundUnit < aoEncoded.length;) {
            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSoundUnitSamples(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            for (_logContext.iChannel = 0;
                 _logContext.iChannel < _aoEncoders.length;
                 _logContext.iChannel++, _logContext.iSoundUnit++)
            {
                if (_presetPrameters == null) {
                    aoEncoded[_logContext.iSoundUnit] = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                                aasiPcmSoundUnitChannelSamples[_logContext.iChannel], _logContext);
                } else {
                    aoEncoded[_logContext.iSoundUnit] = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                                aasiPcmSoundUnitChannelSamples[_logContext.iChannel],
                                _presetPrameters.read(), _logContext);
                }
            }
            _logContext.iChannel = -1;
            _logContext.lngSamplesFramesRead += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }
        _logContext.iSoundUnit = -1;

        // write the sound parameters and the encoded samples
        if (_blnEncode4BitsElse8Bits) {
            // aoEncoded.length == AdpcmSoundGroup.SOUND_UNITS_IN_4_BIT_SOUND_GROUP == 8

            // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7
            for (int i = 0; i < 4; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 0; i < 4; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(aoEncoded[i].getSoundParameter());
            }

            // 1/0,3/2,5/4,7/6, 1/0,3/2,5/4,7/6, ...
            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i++) {
                IO.writeInt4x2(os, aoEncoded[1].abEncodedAdpcm[i], aoEncoded[0].abEncodedAdpcm[i]);
                IO.writeInt4x2(os, aoEncoded[3].abEncodedAdpcm[i], aoEncoded[2].abEncodedAdpcm[i]);
                IO.writeInt4x2(os, aoEncoded[5].abEncodedAdpcm[i], aoEncoded[4].abEncodedAdpcm[i]);
                IO.writeInt4x2(os, aoEncoded[7].abEncodedAdpcm[i], aoEncoded[6].abEncodedAdpcm[i]);
            }
        } else {
            // aoEncoded.length == AdpcmSoundGroup.SOUND_UNITS_IN_8_BIT_SOUND_GROUP == 4

            // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
            for (int iRepeat = 0; iRepeat < 4; iRepeat++) {
                for (int i = 0; i < 4; i++) {
                    os.write(aoEncoded[i].getSoundParameter());
                }
            }

            // 0,1,2,3, 0,1,2,3, 0,1,2,3 ...
            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i++) {
                os.write(aoEncoded[0].abEncodedAdpcm[i]);
                os.write(aoEncoded[1].abEncodedAdpcm[i]);
                os.write(aoEncoded[2].abEncodedAdpcm[i]);
                os.write(aoEncoded[3].abEncodedAdpcm[i]);
            }

        }
    }

}
