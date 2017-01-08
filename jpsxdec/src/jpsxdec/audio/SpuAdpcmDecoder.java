/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

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

    /** Sound units take up 16 bytes:
     * <ul>
     * <li>1 byte for the sound parameter
     * <li>1 byte for SPU flags
     * <li>14 bytes of ADPCM sound samples
    * </ul>
    */
    public static final int SIZEOF_SOUND_UNIT = 16;

    /** Returns the number of PCM sample frames that will be generated
     * given the number of input ADPCM bytes
     * (assumes the input bytes are for a single channel). */
    public static int calculatePcmSampleFramesGenerated(int iInputBytes) {
        if ((iInputBytes % SIZEOF_SOUND_UNIT) != 0)
            throw new IllegalArgumentException("iInputBytes must be divisible by " + SIZEOF_SOUND_UNIT);

        // each sound unit takes 16 bytes
        int iSoundUnitCount = iInputBytes / SIZEOF_SOUND_UNIT;

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
    protected final SpuSoundUnitDecoder _leftOrMonoSoundUnit;

    /** Keeps track of the state of the decoding process so loggers can
     * more clearly report the state. */
    protected final LogContext _logContext = new LogContext();
    public static class LogContext implements IContextCopier {
        /** Total number of sound units read. */
        public int iSoundUnitsRead;
        /** The number of PCM sample frames that have been written to the output
         * stream (i.e. a stereo sample frame is only 1 sample frame). */
        public long lngSampleFramesWritten;
        /** Current audio channel being decoded. */
        public int iChannel;
        /** Set if the last decoding call encountered corruption in the sound parameters. */
        public boolean blnHadCorruption = false;
        public LogContext() {
            fullReset();
        }

        public @Nonnull LogContext copy() {
            LogContext cpy = new LogContext();
            cpy.iSoundUnitsRead = iSoundUnitsRead;
            cpy.lngSampleFramesWritten = lngSampleFramesWritten;
            cpy.iChannel = iChannel;
            cpy.blnHadCorruption = blnHadCorruption;
            return cpy;
        }

        /** Reset for context reset. */
        final public void fullReset() {
            lngSampleFramesWritten = 0;
            iSoundUnitsRead = 0;
            iChannel = -1;
            blnHadCorruption = false;
        }
        /** Reset at every decode call. */
        public void decodeReset() {
            iChannel = -1;
            blnHadCorruption = false;
        }

        @Override
        public String toString() {
            String s = String.format(
                    "Sound Units %d Sample Frames Written %d Channel %d",
                    iSoundUnitsRead, lngSampleFramesWritten, iChannel);
            if (blnHadCorruption)
                return s + " [corruption]";
            else
                return s;
        }
    }



    public SpuAdpcmDecoder(double dblVolume) {
        _leftOrMonoSoundUnit = new SpuSoundUnitDecoder(dblVolume, _logContext);
    }


    public double getVolume() {
        // assume both are the same
        return _leftOrMonoSoundUnit.getVolume();
    }

    public void setVolume(double dblVolume) {
        _leftOrMonoSoundUnit.setVolume(dblVolume);
    }

    /** Returns if the last call to
     * {@link #decode(java.io.InputStream, java.io.InputStream, int, java.io.OutputStream)}
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

    public void resetContext() {
        _logContext.fullReset();
        _leftOrMonoSoundUnit.resetContext();
    }

    /** Creates an audio format for this decoder.
     * The sample rate must be provided since the decoder doesn't track it. */
    abstract public @Nonnull AudioFormat getOutputFormat(int iSampleRate);

    public static class Mono extends SpuAdpcmDecoder {

        public Mono(double dblVolume) {
            super(dblVolume);
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat(int iSampleRate) {
            return new AudioFormat(iSampleRate, 16, 1, true, false);
        }
    
        /** Decodes a requested amount of audio data from the input stream
         * and writes it to the output stream. To know the amount of data that
         * will be written to the output stream, use
         *  {@link #calculatePcmBytesGenerated(int)}.
         * @param iNumBytes The number of bytes that will be read from the input stream.
         *                  Must be divisible by {@link #SIZEOF_SOUND_UNIT}.
         */
        public int decode(@Nonnull InputStream monoStream,
                          int iNumBytes, @Nonnull OutputStream out)
                throws IOException
        {
            _logContext.decodeReset();
            if ((iNumBytes % SIZEOF_SOUND_UNIT) > 0)
                throw new IllegalArgumentException("iNumBytes must be divisible by " + SIZEOF_SOUND_UNIT);
            _logContext.iChannel = 0;

            // each sound unit takes 16 bytes
            int iSoundUnitCount = iNumBytes / SIZEOF_SOUND_UNIT;

            int iOutputSize = 0;

            for (int iSoundUnitIdx = 0; 
                 iSoundUnitIdx < iSoundUnitCount;
                 iSoundUnitIdx++, _logContext.iSoundUnitsRead++)
            {
                short[] asiLeftSamples = _leftOrMonoSoundUnit.readSoundUnit(monoStream);

                for (int iSampleIdx = 0;
                     iSampleIdx < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
                     iSampleIdx++, _logContext.lngSampleFramesWritten++)
                {
                    IO.writeInt16LE(out, asiLeftSamples[iSampleIdx]);
                    iOutputSize += 2;
                }
            }
            _logContext.iChannel = -1;
            return iOutputSize;
        }
    }


    public static class Stereo extends SpuAdpcmDecoder {

        private final SpuSoundUnitDecoder _rightSoundUnit;

        public Stereo(double dblVolume) {
            super(dblVolume);
            _rightSoundUnit = new SpuSoundUnitDecoder(dblVolume, _logContext);
        }

        @Override
        public void resetContext() {
            super.resetContext();
            _rightSoundUnit.resetContext();
        }

        @Override
        public void setVolume(double dblVolume) {
            super.setVolume(dblVolume);
            _rightSoundUnit.setVolume(dblVolume);
        }

        @Override
        public @Nonnull AudioFormat getOutputFormat(int iSampleRate) {
            return new AudioFormat(iSampleRate, 16, 2, true, false);
        }

        /** Decodes a requested amount of audio data from the input stream
         * and writes it to the output stream. To know the amount of data that
         * will be written to the output stream, use
         *  {@link #calculatePcmBytesGenerated(int)}.
         * @param iNumBytes The number of bytes that will be read from each input stream.
         *                  Must be divisible by {@link SpuSoundUnitDecoder#SIZEOF_SOUND_UNIT}.
         */
        public int decode(@Nonnull InputStream leftStream,
                          @Nonnull InputStream rightStream,
                          int iNumBytes, @Nonnull OutputStream out)
                throws EOFException, IOException
        {
            _logContext.decodeReset();
            if ((iNumBytes % SIZEOF_SOUND_UNIT) > 0)
                throw new IllegalArgumentException("iNumBytes must be divisible by " + SIZEOF_SOUND_UNIT);

            // each sound unit takes 16 bytes
            int iSoundUnitCount = iNumBytes / SIZEOF_SOUND_UNIT;

            int iOutputSize = 0;

            for (int iSoundUnitIdx = 0; 
                 iSoundUnitIdx < iSoundUnitCount;
                 iSoundUnitIdx++, _logContext.iSoundUnitsRead++)
            {
                _logContext.iChannel = 0;
                short[] asiLeftSamples = _leftOrMonoSoundUnit.readSoundUnit(leftStream);
                _logContext.iChannel = 1;
                short[] asiRightSamples = _rightSoundUnit.readSoundUnit(rightStream);
                _logContext.iChannel = -1;

                for (int iSampleIdx = 0; 
                     iSampleIdx < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT;
                     iSampleIdx++, _logContext.lngSampleFramesWritten++)
                {
                    IO.writeInt16LE(out, asiLeftSamples[iSampleIdx]);
                    iOutputSize += 2;
                    IO.writeInt16LE(out, asiRightSamples[iSampleIdx]);
                    iOutputSize += 2;
                }
            }
            return iOutputSize;
        }
    }
    
    /**
    /** How to handle corrupted filter index.
     * Assumes only 1 bit of the relevant bits has been corrupted. */
    private final static byte[] FILTER_CORRUPTION_FIX = {
        // If the first 3 bits are withing range, just use them
        /* xxxxx000 -> 000 */ 0,
        /* xxxxx001 -> 001 */ 1,
        /* xxxxx010 -> 010 */ 2,
        /* xxxxx011 -> 011 */ 3,
        /* xxxxx100 -> 100 */ 4,

        // After that it gets interesting...

        // If the 100 bit is set and either 011 bits are set, what do we do?
        // Assuming corruption only toggled 1 bit it's hard to say which one
        // was corrupted.
        // In theory we may be able to check the samples along with the range
        // to see if we can determine which to use... which would be a ton of work
        // TODO: is there a way to choose corrupted filter in this case?
        /* xxxxx101 -> either 100 (4) or 001 (1) */ 1,
        /* xxxxx110 -> either 100 (4) or 010 (2) */ 2,

        // However, assuming corruption only toggled 1 bit
        // then the corrupted bit in 111 must be 100
        /* xxxxx111 -> 011 */ 3,        
    };

    /**
     * SPU ADPCM Sound Unit.
     * <p>
     * From <a href="http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu">
     * Nocash PSXSPX Playstation Specifications</a>
     * <blockquote>
     * Samples consist of one or more 16-byte blocks:
     * <pre>
     * 00h       Shift/Filter (reportedly same as for CD-XA) (see there)
     * 01h       Flag Bits (see below)
     * 02h       Compressed Data (LSBs=1st Sample, MSBs=2nd Sample)
     * 03h       Compressed Data (LSBs=3rd Sample, MSBs=4th Sample)
     * 04h       Compressed Data (LSBs=5th Sample, MSBs=6th Sample)
     * ...       ...
     * 0Fh       Compressed Data (LSBs=27th Sample, MSBs=28th Sample)
     * </pre>
     * </blockquote>
     */
    protected static class SpuSoundUnitDecoder extends SoundUnitDecoder {

        /**
         * Flag Bits (in 2nd byte of ADPCM Header).
         * <p>
         * From <a href="http://problemkaputt.de/psx-spx.htm#soundprocessingunitspu">
         * Nocash PSXSPX Playstation Specifications</a>
         * <blockquote>
         * <pre>
         * 0   Loop End    (0=No change, 1=Set ENDX flag and Jump to [1F801C0Eh+N*10h])
         * 1   Loop Repeat (0=Force Release and set ADSR Level to Zero; only if Bit0=1)
         * 2   Loop Start  (0=No change, 1=Copy current address to [1F801C0Eh+N*10h])
         * 3-7 Unknown    (usually 0)
         * </pre>
         * Possible combinations for Bit0-1 are:
         * <pre>
         * Code 0 = Normal     (continue at next 16-byte block)
         * Code 1 = End+Mute   (jump to Loop-address, set ENDX flag, Release, Env=0000h)
         * Code 2 = Ignored    (same as Code 0)
         * Code 3 = End+Repeat (jump to Loop-address, set ENDX flag)
         * </pre>
         * The Loop Start/End flags in the ADPCM Header allow to play one
         * or more sample block(s) in a loop, that can be either all block(s)
         * endless repeated, or only the last some block(s) of the sample.
         * <p>
         * There's no way to stop the output, so a one-shot sample must be
         * followed by dummy block (with Loop Start/End flags both set, and all
         * data nibbles set to zero; so that the block gets endless repeated,
         * but doesn't produce any sound).
         * </blockquote>
         * <p>
         * Not used in this decoder, but may be useful to be aware of.
         */
        private byte _bFlagBits;
        
        @Nonnull
        private final AdpcmContext _adpcmContext;
        private final short[] _asiPcmSampleBuffer = new short[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
        private final LogContext _logContext;

        public SpuSoundUnitDecoder(double dblVolume, @Nonnull LogContext logContext) {
            super(K0K1Filter.SPU);
            _adpcmContext = new AdpcmContext(dblVolume);
            _logContext = logContext;
        }

        /** @return a direct reference to the internal buffer
         *          (i.e. treat it as read-only). */
        public @Nonnull short[] readSoundUnit(@Nonnull InputStream inStream)
                throws EOFException, IOException
        {
            readAndSetSoundParamter(inStream);

            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i+=2) {
                // read a byte
                int iByte = inStream.read();
                if (iByte < 0)
                    throw new EOFException();

                // get the two ADPCM samples from it
                // shift the nibble into the top of a short
                addShiftedAdpcmSample((short)((iByte & 0x0F) << 12));
                addShiftedAdpcmSample((short)((iByte & 0xF0) <<  8));
            }

            decodeSoundUnit(_adpcmContext, _asiPcmSampleBuffer, _logContext);
            
            return _asiPcmSampleBuffer;
        }


        private void readAndSetSoundParamter(@Nonnull InputStream inStream)
                throws EOFException, IOException
        {
            int iSoundParameter = inStream.read();
            if (iSoundParameter < 0)
                throw new EOFException();

            byte bFilterIndex = (byte)((iSoundParameter >>> 4) & 0xF);
            byte bRange       = (byte)( iSoundParameter        & 0xF);

            boolean blnFilterIndexCorrupted = false;
            byte bBadFilterIndexValue = -1;
            boolean blnFlagBitsCorrupted = false;

            if (bFilterIndex > 4) {
                blnFilterIndexCorrupted = true;
                bBadFilterIndexValue = bFilterIndex;
                bFilterIndex = FILTER_CORRUPTION_FIX[bFilterIndex & 0x7];
            }

            // TODO: are there invalid range values?
            // technically any range value would work, however
            // a range of 15 would basically wipe out the sample to 1 or -1
            if (bRange > 12) {
                LOG.log(Level.INFO, "Range {0} > 12", bRange);
            }

            int iFlagsBits = inStream.read();
            if (iFlagsBits < 0)
                throw new EOFException();
            _bFlagBits = (byte)iFlagsBits;

            if ((_bFlagBits & ~7) != 0) {
                blnFlagBitsCorrupted = true;
            }

            if (blnFilterIndexCorrupted || blnFlagBitsCorrupted) {
                _logContext.blnHadCorruption = true;
                if (LOG.isLoggable(Level.WARNING)) {
                    StringBuilder sbLog = new StringBuilder("Bad SPU ADPCM header:");

                    if (blnFilterIndexCorrupted) {
                        sbLog.append(" Sound Parameter Filter Index[")
                             .append(bBadFilterIndexValue).append(" > 4, using ")
                             .append(bFilterIndex).append(']');
                    }

                    if (blnFlagBitsCorrupted) {
                        sbLog.append(" BitFlags[").append(Misc.bitsToString(_bFlagBits, 8))
                             .append("]");
                    }
                    sbLog.append(" at ").append(_logContext);

                    if (inStream instanceof ByteArrayFPIS)
                        sbLog.append(" FP ").append(((ByteArrayFPIS)inStream).getFilePointer()-1);
                    
                    LOG.warning(sbLog.toString());
                }
            }

            setFilterAndRange(bFilterIndex, bRange);
        }


        public void resetContext() {
            _adpcmContext.reset();
        }

        public double getVolume() {
            return _adpcmContext.getVolumeScale();
        }

        public void setVolume(double dblVolume) {
            _adpcmContext.setVolumeScale(dblVolume);
        }
    }

}
