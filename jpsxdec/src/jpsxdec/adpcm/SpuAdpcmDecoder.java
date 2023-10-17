/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.IO;

/** PlayStation
 * Sound Processing Unit (SPU)
 * Adaptive Differential Pulse Code Modulation (ADPCM)
 * decoder.
 *<p>
 * From the amazing
 * <a href="http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu">
 * Nocash PSXSPX Playstation Specifications</a>
 * <blockquote>
 * The SPU supports only ADPCM compressed samples.
 * <p>
 * The PSX supports two ADPCM formats: SPU-ADPCM, and
 * XA-ADPCM. XA-ADPCM is decompressed by the CDROM Controller, and sent directly
 * to the sound mixer, without needing to store the data in SPU RAM, nor needing
 * to use a Voice channel.
 * <p>
 * The actual decompression algorithm is the same for both formats. However, the
 * XA nibbles are arranged in different order, and XA uses 2x28 nibbles per
 * block (instead of 2x14), XA blocks can contain mono or stereo data, XA
 * supports only two sample rates, and, XA doesn't support looping.
 * </blockquote>
 * <p>
 * This format is confirmed to be used directly with videos in
 * FF8, FF9, Chrono Cross, and Crusader: No Remorse.
 */
public abstract class SpuAdpcmDecoder {

    private static final Logger LOG = Logger.getLogger(SpuAdpcmDecoder.class.getName());

    // =========================================================================
    // static

    /** Returns the number of PCM sample frames that will be generated
     * given the number of input ADPCM bytes
     * (assumes the input bytes are for a single channel). */
    public static int calculatePcmSampleFramesGenerated(int iInputBytes) {
        if ((iInputBytes % SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT) != 0)
            throw new IllegalArgumentException("iInputBytes must be divisible by " + SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT);

        // each sound unit takes 16 bytes
        int iSoundUnitCount = iInputBytes / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT;

        return iSoundUnitCount * SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
    }

    /** Returns the number of PCM bytes that will be generated
     * given the number of input ADPCM bytes. */
    public static int calculatePcmBytesGenerated(int iInputBytes) {
        return calculatePcmSampleFramesGenerated(iInputBytes) * 2;
    }

    // =========================================================================
    // instance

    @Nonnull
    protected final SpuSoundUnitDecoder _leftOrMonoSoundUnitDecoder;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    protected final LogContext _logContext = new LogContext();
    public static class LogContext implements IContextCopier {
        /** Total number of sound units read. */
        public int iSoundUnitsDecoded;
        /** The number of PCM sample frames that have been written to the output
         * stream (i.e. a stereo sample frame is only 1 sample frame). */
        public long lngSampleFramesWritten;
        /** Current audio channel being decoded. */
        public int iChannel;
        /** Set if the last decoding call encountered corruption in the sound parameters. */
        public boolean blnHadCorruption = false;
        public LogContext() {
            lngSampleFramesWritten = 0;
            iSoundUnitsDecoded = 0;
            iChannel = -1;
            blnHadCorruption = false;
        }

        @Override
        public @Nonnull LogContext copy() {
            LogContext cpy = new LogContext();
            cpy.iSoundUnitsDecoded = iSoundUnitsDecoded;
            cpy.lngSampleFramesWritten = lngSampleFramesWritten;
            cpy.iChannel = iChannel;
            cpy.blnHadCorruption = blnHadCorruption;
            return cpy;
        }

        /** Reset at every decode call. */
        public void decodeReset() {
            iChannel = -1;
            blnHadCorruption = false;
        }

        @Override
        public String toString() {
            String s = String.format("Sound Units %d Sample Frames Written %d Channel %d",
                    iSoundUnitsDecoded, lngSampleFramesWritten, iChannel);
            if (blnHadCorruption)
                return s + " [corruption]";
            else
                return s;
        }
    }


    protected SpuAdpcmDecoder(double dblVolume) {
        _leftOrMonoSoundUnitDecoder = new SpuSoundUnitDecoder(dblVolume, _logContext);
    }

    public double getVolume() {
        // assume both are the same
        return _leftOrMonoSoundUnitDecoder.getVolume();
    }

    /** Returns if the last call to {@code decode(...)}
     * encountered corruption in the sound parameters. */
    public boolean hadCorruption() {
        return _logContext.blnHadCorruption;
    }

    /** Returns the number of PCM sample frames that have been written to the
     * output stream (i.e. a stereo sample frame is only 1 sample frame).
     * Reset when {@link #resetContext()} is called. */
    public long getSampleFramesWritten() {
        return _logContext.lngSampleFramesWritten;
    }

    abstract public int getBytesPerSampleFrame();

    /** Creates an audio format for this decoder.
     * The sample rate must be provided since the decoder doesn't track it. */
    abstract public @Nonnull AudioFormat getOutputFormat(int iSampleFramesPerSecond);

    public static class Mono extends SpuAdpcmDecoder {

        public Mono(double dblVolume) {
            super(dblVolume);
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat(int iSampleFramesPerSecond) {
            return new AudioFormat(iSampleFramesPerSecond, 16, 1, true, false);
        }

        @Override
        public int getBytesPerSampleFrame() {
            return 2;
        }

        /** Decodes a requested amount of sound units from the input stream
         * and writes it to the output stream. To know the amount of data that
         * will be written to the output stream, use
         *  {@link #calculatePcmBytesGenerated(int)}.
         * @param iSoundUnitCount The number of {@link SpuAdpcmSoundUnit#SIZEOF_SOUND_UNIT}
         *                        byte sound units that will be read from the input stream.
         * @return Number of PCM sample frames decoded.
         */
        public int decode(@Nonnull InputStream spuStream,
                          int iSoundUnitCount, @Nonnull OutputStream pcmOut)
                throws IOException
        {
            int iTotal = 0;
            for (int iSoundUnit = 0; iSoundUnit < iSoundUnitCount; iSoundUnit++) {
                SpuAdpcmSoundUnit soundUnit = new SpuAdpcmSoundUnit(spuStream);
                iTotal =+ decode(soundUnit, pcmOut);
            }
            return iTotal;
        }

        /** Decodes a single sound unit and writes it to the output stream.
         * To know the amount of data that will be written to the output stream,
         * use {@link #calculatePcmBytesGenerated(int)}.
         * @return Number of PCM sample frames decoded. */
        public int decode(@Nonnull SpuAdpcmSoundUnit soundUnit, @Nonnull OutputStream out)
                throws IOException
        {
            _logContext.decodeReset();
            _logContext.iChannel = 0;
            _leftOrMonoSoundUnitDecoder.decodeSoundUnit(soundUnit);
            _logContext.iSoundUnitsDecoded++;

            for (int iSampleIdx = 0;
                 iSampleIdx < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
                 iSampleIdx++, _logContext.lngSampleFramesWritten++)
            {
                IO.writeInt16LE(out, _leftOrMonoSoundUnitDecoder.getDecodedPcmSample(iSampleIdx));
            }

            _logContext.iChannel = -1;
            return SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }
    }


    public static class Stereo extends SpuAdpcmDecoder {

        @Nonnull
        private final SpuSoundUnitDecoder _rightSoundUnitDecoder;

        public Stereo(double dblVolume) {
            super(dblVolume);
            _rightSoundUnitDecoder = new SpuSoundUnitDecoder(dblVolume, _logContext);
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat(int iSampleFramesPerSecond) {
            return new AudioFormat(iSampleFramesPerSecond, 16, 2, true, false);
        }

        @Override
        public int getBytesPerSampleFrame() {
            return 4;
        }

        /** @see Mono#decode(java.io.InputStream, int, java.io.OutputStream) */
        public int decode(@Nonnull InputStream leftSpuStream,
                          @Nonnull InputStream rightSpuStream,
                          int iSoundUnitCount, @Nonnull OutputStream pcmOut)
                throws EOFException, IOException
        {
            int iTotal = 0;
            for (int iSoundUnit = 0; iSoundUnit < iSoundUnitCount; iSoundUnit++) {
                SpuAdpcmSoundUnit leftUnit = new SpuAdpcmSoundUnit(leftSpuStream);
                SpuAdpcmSoundUnit rightUnit = new SpuAdpcmSoundUnit(rightSpuStream);
                iTotal += decode(leftUnit, rightUnit, pcmOut);
            }
            return iTotal;
        }

        /** @see Mono#decode(jpsxdec.adpcm.SpuAdpcmSoundUnit, java.io.OutputStream) */
        public int decode(@Nonnull SpuAdpcmSoundUnit leftSoundUnit,
                          @Nonnull SpuAdpcmSoundUnit rightSoundUnit,
                          @Nonnull OutputStream out)
                throws IOException
        {
            _logContext.decodeReset();

            _logContext.iChannel = 0;
            _leftOrMonoSoundUnitDecoder.decodeSoundUnit(leftSoundUnit);
            _logContext.iSoundUnitsDecoded++;

            _logContext.iChannel = 1;
            _rightSoundUnitDecoder.decodeSoundUnit(rightSoundUnit);
            _logContext.iSoundUnitsDecoded++;

            _logContext.iChannel = -1;

            for (int iSampleIdx = 0;
                 iSampleIdx < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
                 iSampleIdx++, _logContext.lngSampleFramesWritten++)
            {
                IO.writeInt16LE(out, _leftOrMonoSoundUnitDecoder.getDecodedPcmSample(iSampleIdx));
                IO.writeInt16LE(out, _rightSoundUnitDecoder.getDecodedPcmSample(iSampleIdx));
            }
            return SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
        }
    }


    protected static class SpuSoundUnitDecoder {
        private final SoundUnitDecoder _decoder = new SoundUnitDecoder(K0K1Filter.SPU);
        @Nonnull
        private final AdpcmContext _adpcmContext;
        private final short[] _asiPcmSampleBuffer = new short[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
        private final LogContext _logContext;

        public SpuSoundUnitDecoder(double dblVolume, @Nonnull LogContext logContext) {
            _adpcmContext = new AdpcmContext(dblVolume);
            _logContext = logContext;
        }

        /** Decodes into an internal buffer. */
        public void decodeSoundUnit(@Nonnull SpuAdpcmSoundUnit soundUnit) {
            String sCorruptionLog = soundUnit.getCorruptionLog();
            if (sCorruptionLog != null) {
                _logContext.blnHadCorruption = true;
                if (LOG.isLoggable(Level.WARNING)) {
                    StringBuilder sbLog = new StringBuilder(sCorruptionLog);
                    sbLog.append(" at ").append(_logContext);

                    LOG.warning(sbLog.toString());
                }
            }
            _decoder.decodeSoundUnit(_adpcmContext, soundUnit, _asiPcmSampleBuffer, _logContext);
        }

        public double getVolume() {
            return _adpcmContext.getVolumeScale();
        }

        public short getDecodedPcmSample(int i) {
            return _asiPcmSampleBuffer[i];
        }
    }

}
