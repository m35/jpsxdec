/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015  Michael Sabin
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.audio.XaAdpcmDecoder.AdpcmSoundGroup;
import jpsxdec.audio.XaAdpcmDecoder.AdpcmSoundUnit;
import static jpsxdec.audio.XaAdpcmDecoder.SoundUnit_K0;
import static jpsxdec.audio.XaAdpcmDecoder.SoundUnit_K1;
import jpsxdec.util.IO;

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
public class XaAdpcmEncoder {

    // =========================================================================
    // static

    /** Set this telemetry listener to receive information about each encoded
     *  Sound Unit. */
    @CheckForNull
    public static EncodingTelemetryListener LISTENER = null;

    /** Encoding equivalent to {@link AdpcmContext}. */
    private static class AdpcmEncodingContext {
        public double dblPrev1 = 0, dblPrev2 = 0;

        /** Updates the previous 2 samples with the new one. */
        public void update(double dblNewSample) {
            dblPrev2 = dblPrev1;
            dblPrev1 = dblNewSample;
        }

        public void update(@Nonnull AdpcmEncodingContext other) {
            dblPrev1 = other.dblPrev1;
            dblPrev2 = other.dblPrev2;
        }

        public @Nonnull AdpcmEncodingContext copy() {
            AdpcmEncodingContext c = new AdpcmEncodingContext();
            c.update(this);
            return c;
        }
    }

    // =========================================================================
    // instance

    /** Source audio stream. */
    @Nonnull
    private final AudioInputStream _sourceAudio;

    /** If source audio stream is stereo. */
    private final boolean _blnStereo;
    /** Sample rate of source/output stream. Must be 18900 or 37800 Hz */
    private final int _iSampleRate;
    /** true=encode to 4 bits/sample, false=encode to 8 bits/sample. */
    private final boolean _blnEncode4BitsElse8Bits;

    /** Running ADPCM encoding context(s). */
    @Nonnull
    private final AdpcmEncodingContext _runningLeftMonoContext;
    @CheckForNull
    private final AdpcmEncodingContext _runningRightContext;

    /** Count of sectors encoded. */
    private int _iEncodedSectorCount = 0;
    /** If source stream is ended. */
    private boolean _blnIsEof = false;

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
    public XaAdpcmEncoder(@Nonnull AudioInputStream ais, int iEncodeToAdpcmBitsPerSample) {
        AudioFormat fmt = ais.getFormat();

        if (fmt.getSampleSizeInBits() != 16)
            throw new UnsupportedOperationException("Bit size must be 16 " + fmt.getSampleRate());
        
        if (fmt.getSampleRate() == 37800)
            _iSampleRate = 37800;
        else if (fmt.getSampleRate() == 18900)
            _iSampleRate = 18900;
        else
            throw new UnsupportedOperationException("Unsupported sample rate " + fmt.getSampleRate()+", must be 18900 or 37800");

        if (fmt.getChannels() == 1) {
            _blnStereo = false;
            _runningLeftMonoContext = new AdpcmEncodingContext();
            _runningRightContext = null;
        } else if (fmt.getChannels() == 2) {
            _blnStereo = true;
            _runningLeftMonoContext = new AdpcmEncodingContext();
            _runningRightContext = new AdpcmEncodingContext();
        } else
            throw new UnsupportedOperationException("Unsupported channel count " + fmt.getChannels()+", must be 1 or 2");

        if (iEncodeToAdpcmBitsPerSample == 4)
            _blnEncode4BitsElse8Bits = true;
        else if (iEncodeToAdpcmBitsPerSample == 8)
            _blnEncode4BitsElse8Bits = false;
        else
            throw new IllegalArgumentException("Invalid encoding bits/sample " + iEncodeToAdpcmBitsPerSample+", must be 4 or 8");
            
        _sourceAudio = ais;
    }

    public boolean isStereo() {
        return _blnStereo;
    }

    public int getSampleRate() {
        return _iSampleRate;
    }

    public boolean isEof() {
        return _blnIsEof;
    }    

    /** Manually provide the filter and range parameters for every Sound Unit
     *  via a stream of bytes. Primarily for development/testing purposes. */
    public void setPresetParameters(@CheckForNull InputStream presetParameters) {
        _presetPrameters = presetParameters;
    }

    /** Does not include the last 20 bytes of the sector. */
    public void encode1Sector(@Nonnull OutputStream os) throws IOException {
        for (int iSoundGroup = 0; iSoundGroup < XaAdpcmDecoder.ADPCM_SOUND_GROUPS_PER_SECTOR; iSoundGroup++)
        {
            encodeSoundGroup(iSoundGroup, os);
        }
        _iEncodedSectorCount++;
    }
    
    private void encodeSoundGroup(int iSoundGroup, @Nonnull OutputStream os) throws IOException {
        SoundUnitEncoder[] encoders;
        if (_blnEncode4BitsElse8Bits)
            encoders = new SoundUnitEncoder[AdpcmSoundGroup.SOUND_UNITS_IN_4_BIT_SOUND_GROUP];
        else
            encoders = new SoundUnitEncoder[AdpcmSoundGroup.SOUND_UNITS_IN_8_BIT_SOUND_GROUP];

        if (_blnStereo) {
            for (int iSoundUnit = 0; iSoundUnit < encoders.length; ) {
                short[][] aasiPcmSoundUnitChannelSamples = readSoundUnitSamples();
                
                SoundUnitEncoder encoder = encodeSoundUnit(iSoundGroup, iSoundUnit, 
                                                           aasiPcmSoundUnitChannelSamples[0],
                                                           _runningLeftMonoContext);
                // update the encoding context
                _runningLeftMonoContext.update(encoder.getDecodedContext());
                encoders[iSoundUnit] = encoder;
                iSoundUnit++;

                encoder = encodeSoundUnit(iSoundGroup, iSoundUnit, 
                                          aasiPcmSoundUnitChannelSamples[1],
                                          _runningRightContext);
                // update the encoding context
                _runningRightContext.update(encoder.getDecodedContext());
                encoders[iSoundUnit] = encoder;
                iSoundUnit++;
            }
        } else {
            for (int iSoundUnit = 0; iSoundUnit < encoders.length; iSoundUnit++) {
                short[][] aasiPcmSoundUnitChannelSamples = readSoundUnitSamples();
                SoundUnitEncoder encoder = encodeSoundUnit(iSoundGroup, iSoundUnit, 
                                                           aasiPcmSoundUnitChannelSamples[0],
                                                           _runningLeftMonoContext);
                // update the encoding context
                _runningLeftMonoContext.update(encoder.getDecodedContext());
                encoders[iSoundUnit] = encoder;
            }
        }

        // write the sound parameters and the encoded samples
        if (_blnEncode4BitsElse8Bits) {
            // encoders.length == AdpcmSoundGroup.SOUND_UNITS_IN_4_BIT_SOUND_GROUP == 8
        
            // 0,1,2,3, 0,1,2,3, 4,5,6,7, 4,5,6,7
            for (int i = 0; i < 4; i++) {
                os.write(encoders[i].getSoundParameter());
            }
            for (int i = 0; i < 4; i++) {
                os.write(encoders[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(encoders[i].getSoundParameter());
            }
            for (int i = 4; i < 8; i++) {
                os.write(encoders[i].getSoundParameter());
            }

            // 1/0,3/2,5/4,7/6, 1/0,3/2,5/4,7/6, ...
            for (int i = 0; i < XaAdpcmDecoder.AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT; i++) {
                write2Nibbles(os, encoders[1].getEncodedAdpcm(i), encoders[0].getEncodedAdpcm(i));
                write2Nibbles(os, encoders[3].getEncodedAdpcm(i), encoders[2].getEncodedAdpcm(i));
                write2Nibbles(os, encoders[5].getEncodedAdpcm(i), encoders[4].getEncodedAdpcm(i));
                write2Nibbles(os, encoders[7].getEncodedAdpcm(i), encoders[6].getEncodedAdpcm(i));
            }
        } else {
            // encoders.length == AdpcmSoundGroup.SOUND_UNITS_IN_8_BIT_SOUND_GROUP == 4

            // 0,1,2,3, 0,1,2,3, 0,1,2,3, 0,1,2,3
            for (int iRepeat = 0; iRepeat < 4; iRepeat++) {
                for (int i = 0; i < 4; i++) {
                    os.write(encoders[i].getSoundParameter());
                }
            }
            
            // 0,1,2,3, 0,1,2,3, 0,1,2,3 ...
            for (int i = 0; i < XaAdpcmDecoder.AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT; i++) {
                os.write(encoders[0].getEncodedAdpcm(i));
                os.write(encoders[1].getEncodedAdpcm(i));
                os.write(encoders[2].getEncodedAdpcm(i));
                os.write(encoders[3].getEncodedAdpcm(i));
            }

        }
    }
        
    private static void write2Nibbles(@Nonnull OutputStream os, byte bTop, byte bBottom)
            throws IOException
    {
        os.write(((bTop&0xf)<<4) | (bBottom&0xf));
    }
    

    private @Nonnull SoundUnitEncoder encodeSoundUnit(int iSoundGroup, int iSoundUnit, 
                                                      @Nonnull short[] asiPcmSoundUnitSamples,
                                                      @Nonnull AdpcmEncodingContext context)
            throws IOException
    {
        // Encode with the supplied parameters, if provided
        if (_presetPrameters != null) {
            int iParameters = _presetPrameters.read();
            if (iParameters < 0)
                throw new UnsupportedOperationException();
            int iRange     = (byte)(iParameters & 0xF);
            int iFilterIdx = (byte)((iParameters >>> 4) & 0x3);
            
            SoundUnitEncoder encoder = new SoundUnitEncoder(
                    _iEncodedSectorCount, iSoundGroup, iSoundUnit,
                    _blnEncode4BitsElse8Bits, iFilterIdx, iRange, 
                    context.copy());
            if (!encoder.encode(asiPcmSoundUnitSamples))
                throw new IllegalStateException("Unable to encode Sound Group "+iSoundGroup+" Sound Unit "+iSoundUnit+" with Filter Index " + iFilterIdx + " Range " + iRange);
        
            return encoder;
        }
        
        SoundUnitEncoder best = null;

        // Otherwise, encode with all parameters and pick the best
        final int iMaxRange = 16-(_blnEncode4BitsElse8Bits ? 4 : 8);
        for (int iRange = iMaxRange; iRange >= 0; iRange--) {
            for (int iFilterIdx = 0; iFilterIdx < 4; iFilterIdx++) {

                SoundUnitEncoder encTry = new SoundUnitEncoder(
                        _iEncodedSectorCount, iSoundGroup, iSoundUnit,
                        _blnEncode4BitsElse8Bits, iFilterIdx, iRange, 
                        context.copy());
                
                if (!encTry.encode(asiPcmSoundUnitSamples))
                    continue;

                if (best == null || encTry.isBetterThan(best)) {
                    best = encTry;
                }
            }
        }

        if (best == null)
            throw new IllegalStateException("Unable to find a possible Filter Index and Range to encode Sound Group "+iSoundGroup+" Sound Unit "+iSoundUnit);
        
        // TODO: find better method to pick the right one
        //System.out.println(String.format("Sector %d SoundGroup %d SoundUnit %d chose Filter %d Range %d",
        //                   _iEncodedSectorCount, iSoundGroup, iSoundUnit, best._iFilterIndex, best._iRange));
        return best;
    }

    /** Encodes 1 sound unit. */
    private static class SoundUnitEncoder {
        /** Bits to encode to. */
        private final int _iAdpcmBitsPerSample;
        /** Min/max value for {@link #_iAdpcmBitsPerSample}. */
        private final int _iEncodeMax, _iEncodeMin;

        /** Sector number (from {@link XaAdpcmEncoder#_iEncodedSectorCount}). */
        private final int _iSector;
        /** Index of the sound group that this sound unit is a part of. */
        private final int _iSoundGroup;
        /** Index of this sound unit in the sound group it is a part of. */
        private final int _iSoundUnit;

        /** Filter index to use for encoding. */
        private final int _iFilterIndex;
        /** Range to use for encoding. */
        private final int _iRange;

        /** Encoding context, updated during encoding. */
        @Nonnull
        private final AdpcmEncodingContext _decodedPcmContext;

        /** Maximum difference encountered between the source PCM sample
         * and the sample that will be decoded from the encoded data. */
        private double _dblMaxDelta = 0;

        /** Encoded data goes here. */
        private final byte[] _abEncodedAdpcm = new byte[XaAdpcmDecoder.AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];


        public SoundUnitEncoder(int iSector, int iSoundGroup, int iSoundUnit,
                                boolean blnEncode4BitsElse8Bits,
                                int iFilterIndex, int iRange,
                                @Nonnull AdpcmEncodingContext context)
        {
            _iSector = iSector;
            _iSoundGroup = iSoundGroup;
            _iSoundUnit = iSoundUnit;
            
            if (blnEncode4BitsElse8Bits) {
                _iAdpcmBitsPerSample = 4;
                _iEncodeMin = -8;
                _iEncodeMax = 7;
            } else {
                _iAdpcmBitsPerSample = 8;
                _iEncodeMin = -128;
                _iEncodeMax = 127;
            }

            _iFilterIndex = iFilterIndex;
            _iRange = iRange;

            _decodedPcmContext = context;
        }

        public byte getSoundParameter() {
            return (byte) (((_iFilterIndex & 0x3) << 4) | (_iRange & 0xf));
        }
        
        public byte getEncodedAdpcm(int i) {
            return _abEncodedAdpcm[i];
        }

        /** Checks if this sound unit provides better encoding than the supplied
         *  sound unit. The decider. */
        public boolean isBetterThan(@Nonnull SoundUnitEncoder other) {
            return _dblMaxDelta < other._dblMaxDelta;
        }

        public @Nonnull AdpcmEncodingContext getDecodedContext() {
            return _decodedPcmContext;
        }
        
        public boolean encode(@Nonnull short[] asiPcmSoundUnitSamples) {
            SoundUnitEncodingTelemetry telemetry = LISTENER == null ?
                    null
                    :
                    new SoundUnitEncodingTelemetry(_iSector, _iSoundGroup, _iSoundUnit,
                                                   _iFilterIndex, _iRange);

            boolean blnOk = true;
            
            for (int i = 0; i < AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT; i++) {
                short siPcmSample = asiPcmSoundUnitSamples[i];

                double dblFiltered = siPcmSample - SoundUnit_K0[_iFilterIndex] * _decodedPcmContext.dblPrev1
                                                 - SoundUnit_K1[_iFilterIndex] * _decodedPcmContext.dblPrev2;

                // do bit shifting via mult/div                        
                int iBitsToShift = _iRange - (16-_iAdpcmBitsPerSample);
                double dblEncoded;
                if (iBitsToShift < 0)
                    dblEncoded = dblFiltered / (1 << -iBitsToShift);
                else if (iBitsToShift > 0)
                    dblEncoded = dblFiltered * (1 << iBitsToShift);
                else
                    dblEncoded = dblFiltered;

                long lngEncoded = Math.round(dblEncoded);
                // make sure the rounded value will fit in the bits available
                if (lngEncoded < _iEncodeMin || lngEncoded > _iEncodeMax) {
                    if (telemetry != null) {
                        telemetry.asiSourcePcmSamples[i] = siPcmSample;
                        telemetry.adblPrev1Samples[i] = _decodedPcmContext.dblPrev1;
                        telemetry.adblPrev2Samples[i] = _decodedPcmContext.dblPrev2;
                        telemetry.adblFilteredSamples[i] = dblFiltered;
                        telemetry.adblEncodedAdpcmSamples[i] = dblEncoded;
                        telemetry.sFailure = "Sample#"+i+" won't fit";
                    }
                    blnOk = false;
                    break;
                }

                byte bEncoded = (byte) lngEncoded;
                _abEncodedAdpcm[i] = bEncoded;

                // now decode what was just encoded -------------------

                // shift to the top of the short
                short siAdpcmShortTopSample = (short) (bEncoded << (16-_iAdpcmBitsPerSample));
                // shift sound data according to the range, keeping the sign
                int iUnRanged = (siAdpcmShortTopSample >> _iRange);

                // adjust according to the filter
                double dblDecodedPcm = iUnRanged + SoundUnit_K0[_iFilterIndex] * _decodedPcmContext.dblPrev1
                                                 + SoundUnit_K1[_iFilterIndex] * _decodedPcmContext.dblPrev2;

                _decodedPcmContext.update(dblDecodedPcm);

                double dblDelta = Math.abs(dblDecodedPcm - siPcmSample);
                if (dblDelta > _dblMaxDelta)
                    _dblMaxDelta = dblDelta;

                if (telemetry != null) {
                    telemetry.asiSourcePcmSamples[i]     = siPcmSample;
                    telemetry.adblPrev1Samples[i]        = _decodedPcmContext.dblPrev1;
                    telemetry.adblPrev2Samples[i]        = _decodedPcmContext.dblPrev2;
                    telemetry.adblFilteredSamples[i]     = dblFiltered;
                    telemetry.adblEncodedAdpcmSamples[i] = dblEncoded;
                    telemetry.abEncodedAdpcmSamples[i]   = bEncoded;
                    telemetry.asiShortTopSamples[i]      = siAdpcmShortTopSample;
                    telemetry.adblDecodedSamples[i]      = dblDecodedPcm;
                }
            }

            if (telemetry != null) {
                telemetry.dblMaxDelta = _dblMaxDelta;
                LISTENER.soundUnitEncoded(telemetry);
            }
            
            return blnOk;
        }
        
    }
    
    /** Read 1 sound unit (mono) or 2 sound units (stereo) worth of samples
     * from the source stream. If end of stream is reached, fills remaining
     * samples with 0 and sets {@link #_blnIsEof} to true.
     * 
     * @return 1 or 2 {@link AdpcmSoundUnit#SAMPLES_PER_SOUND_UNIT} arrays of shorts. */
    private @Nonnull short[][] readSoundUnitSamples() throws IOException {
        int iChannels = _blnStereo ? 2 : 1;
        byte[] abReadPcmSoundUnitSamples = new byte[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT*2*iChannels];
        short[][] aasiPcmSoundUnitChannelSamples = new short[iChannels][AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];

        int iStart = 0;
        int iLen = abReadPcmSoundUnitSamples.length;
        while (iLen > 0) {
            int iWrote = _sourceAudio.read(abReadPcmSoundUnitSamples, iStart, iLen);
            if (iWrote < 0) {
                _blnIsEof = true;
                break;
            }
            iLen -= iWrote;
        }
        if (_sourceAudio.getFormat().isBigEndian()) {
            for (int iInByte = 0, iOutSample = 0;
                 iInByte < abReadPcmSoundUnitSamples.length;
                 iOutSample++)
            {
                for (int iChannel = 0; iChannel < iChannels; iChannel++, iInByte+=2) {
                    aasiPcmSoundUnitChannelSamples[iChannel][iOutSample] =
                            IO.readSInt16BE(abReadPcmSoundUnitSamples, iInByte);
                }
            }
        } else {
            for (int iInByte = 0, iOutSample = 0;
                 iInByte < abReadPcmSoundUnitSamples.length;
                 iOutSample++)
            {
                for (int iChannel = 0; iChannel < iChannels; iChannel++, iInByte+=2) {
                    aasiPcmSoundUnitChannelSamples[iChannel][iOutSample] =
                            IO.readSInt16LE(abReadPcmSoundUnitSamples, iInByte);
                }
            }
        }
        return aasiPcmSoundUnitChannelSamples;
    }


    // =========================================================================
    // static

    /** For development testing of the encoder. */
    public static class SoundUnitEncodingTelemetry {
        public final int iSector;
        public final int iSoundGroup;
        public final int iSoundUnit;

        public final int iFilter;
        public final int iRange;
        
        public final short[]  asiSourcePcmSamples;
        public final double[] adblPrev1Samples;
        public final double[] adblPrev2Samples;
        public final double[] adblFilteredSamples;
        public final double[] adblEncodedAdpcmSamples;
        public final byte[]   abEncodedAdpcmSamples;
        public final short[]  asiShortTopSamples;
        public final double[] adblDecodedSamples;

        public double dblMaxDelta;
        public String sFailure;
        
        public SoundUnitEncodingTelemetry(int _iSector, int _iSoundGroup, int _iSoundUnit, int _iFilter, int _iRange) {
            iSector     = _iSector;
            iSoundGroup = _iSoundGroup;
            iSoundUnit  = _iSoundUnit;
            iFilter     = _iFilter;
            iRange      = _iRange;
            asiSourcePcmSamples     = new short[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            adblPrev1Samples        = new double[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            adblPrev2Samples        = new double[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            adblFilteredSamples     = new double[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            adblEncodedAdpcmSamples = new double[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            abEncodedAdpcmSamples   = new byte[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            asiShortTopSamples      = new short[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
            adblDecodedSamples      = new double[AdpcmSoundUnit.SAMPLES_PER_SOUND_UNIT];
        }
        
        public String toString(int i) {
            return String.format(
                    "Filter %d Range %d: Sample#%d %d filterd (Prev1 %f Prev2 %f) to %f rounded,ranged,shifted from %d to %d. Decoded to %f",
                    iFilter, iRange, i, asiSourcePcmSamples[i],
                    adblPrev1Samples[i], adblPrev2Samples[i], 
                    adblFilteredSamples[i],
                    asiShortTopSamples[i],
                    abEncodedAdpcmSamples[i],
                    adblDecodedSamples[i]
                    );            
        }
    }
    
    /** For development testing of the encoder. */
    public interface EncodingTelemetryListener {
        void soundUnitEncoded(SoundUnitEncodingTelemetry telemetry);
    }
}