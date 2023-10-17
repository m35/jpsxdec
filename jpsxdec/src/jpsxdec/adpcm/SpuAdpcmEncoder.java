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

package jpsxdec.adpcm;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.formats.Signed16bitLittleEndianLinearPcmAudioInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;

/** Encoder for PlayStation SPU ADPCM audio. */
public abstract class SpuAdpcmEncoder implements Closeable {

    /** Source audio stream. */
    @Nonnull
    protected final Signed16bitLittleEndianLinearPcmAudioInputStream _audioShortReader;

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

        @Override
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
    protected InputStream _presetParameters = null;

    protected SpuAdpcmEncoder(@Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream input) throws IncompatibleException {
        _audioShortReader = input;

        // SPU ADPCM is always 4 bits per sample
        _leftOrMonoEncoder = new SoundUnitEncoder(4, K0K1Filter.SPU);
    }

    abstract public boolean isStereo();

    @Override
    public void close() throws IOException {
        _audioShortReader.close();
    }

    public long getSampleFramesReadAndEncoded() {
        return _logContext.lngSampleFramesReadEncoded;
    }

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetParameters = presetParameters;
    }

    public static class Mono extends SpuAdpcmEncoder {

        public Mono(@Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream input) throws IncompatibleException {
            super(input);

            if (input.isStereo())
                throw new IncompatibleException();
        }

        @Override
        public boolean isStereo() {
            return false;
        }

        /** @see Stereo#encode1SoundUnit(byte, byte) */
        public @Nonnull byte[] encode1SoundUnit(byte bFlagBits)
                throws EOFException, IOException
        {
            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSampleFrames(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            _logContext.iChannel = 0;
            byte[] abEncodedAdpcm = encode1SoundUnitChannel(_leftOrMonoEncoder, bFlagBits, aasiPcmSoundUnitChannelSamples[0]);
            _logContext.iChannel = -1;
            _logContext.lngSampleFramesReadEncoded += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;

            return abEncodedAdpcm;
        }

    }


    public static class Stereo extends SpuAdpcmEncoder {

        @Nonnull
        private final SoundUnitEncoder _rightChannel;

        public Stereo(@Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream input) throws IncompatibleException {
            super(input);

            if (!input.isStereo())
                throw new IncompatibleException();

            _rightChannel = new SoundUnitEncoder(4, K0K1Filter.SPU);
        }

        @Override
        public boolean isStereo() {
            return true;
        }


        /** Encodes one Sound Unit worth of samples
         * ({@link SoundUnitDecoder#SAMPLES_PER_SOUND_UNIT}
         * to the supplied output streams.
         * <p>
         * 28 sample frames will be read from the underlying source audio stream
         * and return two arrays of 16 bytes, setting the flag bits to the given values. */
        public byte[][] encode1SoundUnit(byte bLeftFlagBits, byte bRightFlagBits)
                throws EOFException, IOException
        {
            short[][] aasiPcmSoundUnitChannelSamples =
                    _audioShortReader.readSampleFrames(SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT);

            byte[][] aabEncodedAdpcm = new byte[2][];

            _logContext.iChannel = 0;
            aabEncodedAdpcm[_logContext.iChannel] = encode1SoundUnitChannel(_leftOrMonoEncoder, bLeftFlagBits, aasiPcmSoundUnitChannelSamples[_logContext.iChannel]);
            _logContext.iChannel = 1;
            aabEncodedAdpcm[_logContext.iChannel] = encode1SoundUnitChannel(_rightChannel, bRightFlagBits, aasiPcmSoundUnitChannelSamples[_logContext.iChannel]);

            _logContext.iChannel = -1;
            _logContext.lngSampleFramesReadEncoded += SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;

            return aabEncodedAdpcm;
        }
    }

    protected byte[] encode1SoundUnitChannel(@Nonnull SoundUnitEncoder encoder,
                                             byte bFlagBits,
                                             @Nonnull short[] asi28PcmChannelSamples)
    {
        SoundUnitEncoder.EncodedUnit encoded;
        if (_presetParameters == null) {
            encoded = encoder.encodeSoundUnit(asi28PcmChannelSamples, _logContext);
        } else {
            int iParameter;
            try {
                iParameter = IO.readSInt8(_presetParameters);
            } catch (IOException ex) {
                throw new RuntimeException("Error reading input parameter", ex);
            }
            encoded = encoder.encodeSoundUnit(asi28PcmChannelSamples, iParameter, _logContext);
        }

        byte[] abEncoded = new byte[SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT];
        abEncoded[0] = encoded.getSoundParameter();
        abEncoded[1] = bFlagBits;
        for (int iIn = 0, iOut = 2; iIn < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; iIn+=2, iOut++) {
            int iTop4Bits    = encoded.getByteOrNibble(iIn + 1) & 0xf;
            int iBottom4Bits = encoded.getByteOrNibble(iIn    ) & 0xf;
            int iByte = (iTop4Bits << 4) | iBottom4Bits;

            abEncoded[iOut] = (byte) iByte;
        }
        return abEncoded;
    }

}
