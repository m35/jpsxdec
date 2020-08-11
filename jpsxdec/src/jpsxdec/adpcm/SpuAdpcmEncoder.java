/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2020  Michael Sabin
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
import javax.sound.sampled.AudioInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

/** Encoder for PlayStation SPU ADPCM audio. */
public abstract class SpuAdpcmEncoder implements Closeable {

    /** Source audio stream. */
    @Nonnull
    protected final AudioShortReader _audioShortReader;

    /** Running ADPCM encoders(s). */
    @Nonnull
    protected final SoundUnitEncoder _leftOrMonoEncoder;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    protected final LogContext _logContext = new LogContext();
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
    protected InputStream _presetPrameters = null;

    protected SpuAdpcmEncoder(@Nonnull AudioInputStream input) throws IncompatibleException {
        // will verify all format details but stereo are valid
        _audioShortReader = new AudioShortReader(input);

        // SPU ADPCM is always 4 bits per sample
        _leftOrMonoEncoder = new SoundUnitEncoder(4, K0K1Filter.SPU);
    }

    abstract public boolean isStereo();

    public boolean isEof() {
        return _audioShortReader.isEof();
    }

    public void close() throws IOException {
        _audioShortReader.close();
    }

    public long getSampleFramesReadAndEncoded() {
        return _logContext.lngSampleFramesReadEncoded;
    }
    
    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetPrameters = presetParameters;
    }

    public static class Mono extends SpuAdpcmEncoder {

        public Mono(@Nonnull AudioInputStream input) throws IncompatibleException {
            super(input);

            if (input.getFormat().getChannels() != 1)
                throw new IncompatibleException();
        }

        public boolean isStereo() {
            return false;
        }

        /** @see Stereo#encode1SoundUnit(byte, java.io.OutputStream, byte, java.io.OutputStream)  */
        public boolean encode1SoundUnit(byte bFlagBits, @Nonnull OutputStream spuOutputStream)
                throws IOException
        {
            if (_audioShortReader.isEof())
                return false;
            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSoundUnitSamples(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            encode1SoundUnitChannel(_leftOrMonoEncoder, aasiPcmSoundUnitChannelSamples[0], 
                                    bFlagBits, spuOutputStream);
            return true;
        }

    }


    public static class Stereo extends SpuAdpcmEncoder {

        @Nonnull
        private final SoundUnitEncoder _rightChannel;

        public Stereo(@Nonnull AudioInputStream input) throws IncompatibleException {
            super(input);

            if (input.getFormat().getChannels() != 2)
                throw new IncompatibleException();

            _rightChannel = new SoundUnitEncoder(4, K0K1Filter.SPU);
        }

        public boolean isStereo() {
            return true;
        }


        /** Encodes one Sound Unit worth of samples
         * ({@link SoundUnitDecoder#SAMPLES_PER_SOUND_UNIT}
         * to the supplied output streams.
         * <p>
         * 28 sample frames will be read from the source audio stream
         * and 16 bytes will be written to each output stream, setting
         * the flag bits to the given value.
         * <p>
         * Stops encoding anything if the source audio is empty.
         * @return if the source audio stream is empty.  */
        public boolean encode1SoundUnit(byte bLeftFlagBits, @Nonnull OutputStream leftSpuStream,
                                        byte bRightFlagBits, @Nonnull OutputStream rightSpuStream)
                throws IOException
        {
            if (_audioShortReader.isEof())
                return false;

            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSoundUnitSamples(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            _logContext.iChannel = 0;
            encode1SoundUnitChannel(_leftOrMonoEncoder, aasiPcmSoundUnitChannelSamples[_logContext.iChannel],
                                    bLeftFlagBits, leftSpuStream);
            _logContext.iChannel = 1;
            encode1SoundUnitChannel(_rightChannel, aasiPcmSoundUnitChannelSamples[_logContext.iChannel],
                                    bRightFlagBits, rightSpuStream);

            _logContext.iChannel = -1;
            _logContext.lngSampleFramesReadEncoded += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
            return true;
        }
    }

    protected void encode1SoundUnitChannel(@Nonnull SoundUnitEncoder encoder,
                                           @Nonnull short[] asiPcmSoundUnitChannelSamples,
                                           byte bFlagBits, @Nonnull OutputStream spuStream)
            throws IOException
    {
        SoundUnitEncoder.EncodedUnit encoded;
        if (_presetPrameters == null) {
            encoded = encoder.encodeSoundUnit(
                        asiPcmSoundUnitChannelSamples, _logContext);
        } else {
            encoded = encoder.encodeSoundUnit(
                    asiPcmSoundUnitChannelSamples,
                    _presetPrameters.read(), _logContext);
        }

        spuStream.write(encoded.getSoundParameter() & 0xff);
        spuStream.write(bFlagBits & 0xff);
        for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i+=2) {
            IO.writeInt4x2(spuStream, encoded.abEncodedAdpcm[i+1], encoded.abEncodedAdpcm[i]);
        }
    }

}
