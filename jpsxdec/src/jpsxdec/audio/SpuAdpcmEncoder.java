/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2017  Michael Sabin
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

package jpsxdec.audio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.util.IO;

/** Encoder for PlayStation SPU ADPCM audio.
 * Can handle both mono and stereo {@link AudioInputStream}s.
 * Check {@link #isStereo()} to determine if one or two ADPCM streams will
 * be encoded. The appropriate number of output streams should be passed to
 * {@link #encode1SoundUnit(java.io.OutputStream[])}. */
public class SpuAdpcmEncoder implements Closeable {

    /** Source audio stream. */
    @Nonnull
    private final AudioShortReader _audioShortReader;

    /** Running ADPCM encoders(s). */
    @Nonnull
    private final SoundUnitEncoder[] _aoEncoders;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    private final LogContext _logContext = new LogContext();
    public static class LogContext implements IContextCopier {
        /** The number of PCM sample frames that have been read and encoded
         * (i.e. a stereo sample frame is only 1 sample frame).
         * Used to help find where in the input stream data was being encoded. */
        public long lngSampleFramesReadEncoded = 0;
        /** Audio channel being encoded (0 or 1). */
        public int iChannel = -1;

        private LogContext() {}

        public @Nonnull LogContext copy() {
            LogContext cpy = new LogContext();
            cpy.lngSampleFramesReadEncoded = lngSampleFramesReadEncoded;
            cpy.iChannel = iChannel;
            return cpy;
        }

        @Override
        public String toString() {
            return String.format("Sample frames read/encoded %d Channel %d",
                                 lngSampleFramesReadEncoded, iChannel);
        }
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Set by {@link #setPresetParameters(java.io.InputStream)}.
     *  Primarily for development/testing purposes. */
    @CheckForNull
    private InputStream _presetPrameters = null;

    public SpuAdpcmEncoder(@Nonnull AudioInputStream input) {
        AudioFormat fmt = input.getFormat();

        // will verify all format details are valid
        _audioShortReader = new AudioShortReader(input);

        int iChannels = fmt.getChannels();
        _aoEncoders = new SoundUnitEncoder[iChannels];
        for (int i = 0; i < iChannels; i++) {
            // SPU ADPCM is always 4 bits per sample
            _aoEncoders[i] = new SoundUnitEncoder(4, K0K1Filter.SPU);
        }
    }

    public boolean isStereo() {
        return _aoEncoders.length == 2;
    }

    public boolean isEof() {
        return _audioShortReader.isEof();
    }

    public void close() throws IOException {
        _audioShortReader.close();
    }
    
    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetPrameters = presetParameters;
    }

    /** Encodes one Sound Unit worth of samples 
     * ({@link SoundUnitDecoder#SAMPLES_PER_SOUND_UNIT}
     * to the supplied output stream(s).
     * <p>
     * The number of output streams must match the number of channels
     * in the source audio. If source audio is mono, only one output stream
     * should be supplied. If stereo, two output streams should be supplied.
     * The flag bits will be set to 0.
     * <p>
     * 28 sample frames will be read from the source audio stream
     * and 16 bytes will be written to each output stream.
     * @param aoOutputStreams Either 1 or 2 output streams based on {@link #isStereo()}.
     */
    public void encode1SoundUnit(@Nonnull OutputStream ... aoOutputStreams)
            throws IOException
    {
        encode1SoundUnit(0, aoOutputStreams);
    }

    /** See {@link #encode1SoundUnit(java.io.OutputStream...)}.
     * Allows caller to set custom flag bits.
     * @see #encode1SoundUnit(java.io.OutputStream...)
     */
    public void encode1SoundUnit(int iFlagBits, @Nonnull OutputStream ... aoOutputStreams)
            throws IOException
    {

        if (aoOutputStreams.length != _aoEncoders.length)
            throw new IllegalArgumentException();

        short[][] aasiPcmSoundUnitChannelSamples =
                _audioShortReader.readSoundUnitSamples(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);
        
        for (_logContext.iChannel = 0; _logContext.iChannel < _aoEncoders.length; _logContext.iChannel++) {

            SoundUnitEncoder.EncodedUnit encoded;
            if (_presetPrameters == null) {
                encoded = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                            aasiPcmSoundUnitChannelSamples[_logContext.iChannel], _logContext);
            } else {
                encoded = _aoEncoders[_logContext.iChannel].encodeSoundUnit(
                        aasiPcmSoundUnitChannelSamples[_logContext.iChannel],
                        _presetPrameters.read(), _logContext);
            }
            
            OutputStream os = aoOutputStreams[_logContext.iChannel];
            os.write(encoded.getSoundParameter());
            os.write(iFlagBits);
            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i+=2) {
                IO.writeInt4x2(os, encoded.abEncodedAdpcm[i+1], encoded.abEncodedAdpcm[i]);
            }
        }
        _logContext.iChannel = -1;
        _logContext.lngSampleFramesReadEncoded += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
    }
    
}
