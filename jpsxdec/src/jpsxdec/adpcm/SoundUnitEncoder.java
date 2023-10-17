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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** Encodes 1 sound unit. */
public class SoundUnitEncoder {

    private static final Logger LOG = Logger.getLogger(SoundUnitEncoder.class.getName());

    // =========================================================================
    // static

    /** Set this telemetry listener to receive information about each encoded
     *  Sound Unit. */
    @CheckForNull
    public static TelemetryListener TELEMETRY_LISTENER = null;

    /** Encoded ADPCM sound unit. */
    public static class EncodedUnit {
        /** Sound parameter filter used to encode. */
        private final int _iFilterIndex;
        /** Sound parameter range used to encode. */
        private final int _iRange;
        /** If at least 1 ADPCM sample had to be clamped to fit. */
        private final boolean _blnHadToClamp;
        /** @see #getByteOrNibble(int) */
        @Nonnull
        private final byte[] _abEncodedAdpcm;

        private EncodedUnit(int iFilterIndex, int iRange, boolean blnHadToClamp,
                            @Nonnull byte[] abEncodedAdpcm)
        {
            _iFilterIndex = iFilterIndex;
            _iRange = iRange;
            _blnHadToClamp = blnHadToClamp;
            _abEncodedAdpcm = abEncodedAdpcm;
        }

        public byte getSoundParameter() {
            return (byte) (((_iFilterIndex & 0xf) << 4) | (_iRange & 0xf));
        }

        /** Encoded ADPCM data samples.
         * 8 bits/sample uses the whole byte.
         * 4 bits/sample uses the least significant nibble. */
        public byte getByteOrNibble(int i) {
            return _abEncodedAdpcm[i];
        }

        public boolean hadToClamp() {
            return _blnHadToClamp;
        }
    }

    /** Encoding equivalent to {@link AdpcmContext}. */
    private static class AdpcmEncodingContext {
        public double dblPrev1 = 0, dblPrev2 = 0;

        /** Updates the previous 2 samples with the new one. */
        public void update(double dblNewSample) {
            dblPrev2 = dblPrev1;
            dblPrev1 = dblNewSample;
        }

        /** Copy {@code other}'s context into this one. */
        public void update(@Nonnull AdpcmEncodingContext other) {
            dblPrev1 = other.dblPrev1;
            dblPrev2 = other.dblPrev2;
        }

        /** Clone this context. */
        public @Nonnull AdpcmEncodingContext copy() {
            AdpcmEncodingContext c = new AdpcmEncodingContext();
            c.update(this);
            return c;
        }
    }

    // =========================================================================
    // instance

    /** Running context for the life of this encoder. */
    private final AdpcmEncodingContext _context = new AdpcmEncodingContext();

    /** Filters to use for encoding.
     * Allows this encoder to encode for both XA ADPCM and SPU ADPCM. */
    @Nonnull
    private final K0K1Filter _filters;

    /** Bits to encode to. */
    private final int _iAdpcmBitsPerSample;
    /** Min/max value for {@link #_iAdpcmBitsPerSample}. */
    private final int _iEncodeMax, _iEncodeMin;
    /** Maximum range shift for {@link #_iAdpcmBitsPerSample}. */
    private final int _iMaxRange;


    /** Creates an encoder to encode with the supplied filters and
     * generate ADPCM samples with the supplied bits/sample.
     * Suppling the filters as a parameters allows this class to encode for both
     * XA ADPCM and SPU ADPCM.
     * @param iAdpcmBitsPerSample 4 or 8
     * */
    public SoundUnitEncoder(int iAdpcmBitsPerSample, @Nonnull K0K1Filter filters) {
        if (iAdpcmBitsPerSample == 4) {
            _iAdpcmBitsPerSample = 4;
            _iEncodeMin = -8;
            _iEncodeMax = 7;
            _iMaxRange = 16-4;
        } else if (iAdpcmBitsPerSample == 8) {
            _iAdpcmBitsPerSample = 8;
            _iEncodeMin = -128;
            _iEncodeMax = 127;
            _iMaxRange = 16-8;
        } else {
            throw new IllegalArgumentException("Bad bps " + iAdpcmBitsPerSample);
        }
        _filters = filters;
    }


    /** Encodes the PCM samples into an {@link EncodedUnit}.
     * Searches for the sound parameters with the best possible result.
     * @param asi28PcmSamples A {@link SoundUnitDecoder#SAMPLES_PER_SOUND_UNIT}
     *                        worth of 16-bit samples.
     * @param loggingContext  Object that will be used to give some context when logging.
     */
    @Nonnull EncodedUnit encodeSoundUnit(@Nonnull short[] asi28PcmSamples,
                                         @Nonnull IContextCopier loggingContext)
    {
        if (asi28PcmSamples.length != SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT)
            throw new IllegalArgumentException();

        FilterRangeEncoder best = null;
        FilterRangeEncoder zeroRangeFilter = null;

        for (int iRange = _iMaxRange; iRange >= 0; iRange--) {
            for (int iFilterIdx = 0; iFilterIdx < _filters.getCount(); iFilterIdx++) {

                FilterRangeEncoder encTry = new FilterRangeEncoder(iFilterIdx, iRange,
                                                                   _context.copy());

                if (iRange == 0 && iFilterIdx == 0)
                    zeroRangeFilter = encTry;

                if (encTry.encode(asi28PcmSamples, loggingContext))
                    continue;

                if (best == null || encTry.isBetterThan(best)) {
                    best = encTry;
                }
            }
        }

        // only if all encoding options resulted in clamped samples
        // do we default to the 0 range, 0 filter encoding
        if (best == null) {
            LOG.log(Level.WARNING,
                    "Had to clamp encoded samples to encode {0}", loggingContext);
            best = zeroRangeFilter;
        }

        _context.update(best.getDecodedContext());

        return best.makeEncodedUnit();
    }

    /** Encodes the PCM samples into an {@link EncodedUnit}.
     * Uses the supplied sound parameters. Primarily intended for debugging.
     * @param asiPcmSoundUnitSamples A {@link SoundUnitDecoder#SAMPLES_PER_SOUND_UNIT}
     *                               worth of 16-bit samples.
     * @param loggingContext  Object that will be used to give some context when logging.
     */
    private @Nonnull EncodedUnit encodeSoundUnit(@Nonnull short[] asiPcmSoundUnitSamples,
                                                 int iFilterIdx, int iRange,
                                                 @Nonnull IContextCopier loggingContext)
    {
        if (asiPcmSoundUnitSamples.length != SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT)
            throw new IllegalArgumentException();
        if (iFilterIdx < 0 || iFilterIdx >= _filters.getCount() ||
            iRange < 0 || iRange > _iMaxRange)
            throw new IllegalArgumentException();

        FilterRangeEncoder encoder = new FilterRangeEncoder(iFilterIdx, iRange,
                                                            _context.copy());
        if (encoder.encode(asiPcmSoundUnitSamples, loggingContext))
            LOG.log(Level.WARNING,
                    "{0}: Unable to encode with Filter Index {1} Range {2} without clamping",
                    new Object[]{loggingContext, iFilterIdx, iRange});
        _context.update(encoder.getDecodedContext());
        return encoder.makeEncodedUnit();
    }

    /** See {@link #encodeSoundUnit(short[], int, int, java.lang.String)}.
     * @param iParameters filter in top nibble, range in bottom nibble.
     * @see #encodeSoundUnit(short[], int, int, jpsxdec.audio.IContextCloner) */
    @Nonnull EncodedUnit encodeSoundUnit(@Nonnull short[] asiPcmSoundUnitSamples,
                                         int iParameters,
                                         @Nonnull IContextCopier loggingContext)
    {
        if (iParameters < 0)
            throw new IllegalArgumentException();
        int iFilterIdx = (byte)((iParameters >>> 4) & 0xf);
        int iRange     = (byte)(iParameters & 0xF);

        return encodeSoundUnit(asiPcmSoundUnitSamples, iFilterIdx, iRange, loggingContext);
    }

    /** Encodes PCM data for a particular filter and range combination.. */
    private class FilterRangeEncoder {

        /** Filter index to use for encoding. */
        private final int _iFilterIndex;
        /** Range to use for encoding. */
        private final int _iRange;

        /** Local copy of the encoding context, updated during encoding.
         * The running encoding context will be updated with this context
         * if this encoded sound unit is chosen.  */
        @Nonnull
        private final AdpcmEncodingContext _contextSnapshot;

        /** Encoded data goes here. */
        private final byte[] _abEncodedAdpcm = new byte[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];

        /** If at least 1 ADPCM sample had to be clamped to fit.
         * Try to avoid this this {@link FilterRangeEncoder} if clamping occurred. */
        private boolean _blnHadToClamp = false;

        /** Maximum difference encountered between the source PCM sample
         * and the sample that will be decoded from the encoded data. */
        private double _dblMaxDelta = 0;


        /** Will use the supplied sound parameters and a snapshot of the
         * running context for encoding. */
        private FilterRangeEncoder(int iFilterIndex, int iRange,
                                   @Nonnull AdpcmEncodingContext contextSnapshot)
        {
            _iFilterIndex = iFilterIndex;
            _iRange = iRange;
            _contextSnapshot = contextSnapshot;
        }

        /** Checks if this sound unit provides better encoding than the supplied
         *  sound unit.
         * TODO: This may not be the best way to decide the winning encoded sound unit,
         * but it should be a reasonable choice. */
        public boolean isBetterThan(@Nonnull FilterRangeEncoder other) {
            return _dblMaxDelta < other._dblMaxDelta;
        }

        /** Get the resulting context after encoding. */
        public @Nonnull AdpcmEncodingContext getDecodedContext() {
            return _contextSnapshot;
        }

        /** Make an {@link EncodedUnit} based of the encoding results
         * to be shared with the outside world */
        public @Nonnull EncodedUnit makeEncodedUnit() {
            return new EncodedUnit(_iFilterIndex, _iRange, _blnHadToClamp, _abEncodedAdpcm);
        }

        /** Returns if any samples were clamped. */
        private boolean encode(@Nonnull short[] asiPcmSoundUnitSamples,
                               @Nonnull IContextCopier loggingContext)
        {
            Telemetry telemetry = TELEMETRY_LISTENER == null ?
                    null
                    :
                    new Telemetry(loggingContext, _iFilterIndex, _iRange);

            for (int i = 0; i < SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT; i++) {
                short siPcmSample = asiPcmSoundUnitSamples[i];

                double dblFiltered = siPcmSample - _filters.getK0(_iFilterIndex) * _contextSnapshot.dblPrev1
                                                 - _filters.getK1(_iFilterIndex) * _contextSnapshot.dblPrev2;

                // do bit shifting via mult/div
                int iBitsToShift = _iRange - (16-_iAdpcmBitsPerSample);
                double dblRanged;
                if (iBitsToShift < 0)
                    dblRanged = dblFiltered / (1 << -iBitsToShift);
                else if (iBitsToShift > 0)
                    dblRanged = dblFiltered * (1 << iBitsToShift);
                else
                    dblRanged = dblFiltered;

                long lngRanged = Math.round(dblRanged);
                // check if the rounded value will fit in the bits available
                // if not, clamp it and flag the encoding as a failure
                if (lngRanged < _iEncodeMin || lngRanged > _iEncodeMax) {
                    if (lngRanged < _iEncodeMin)
                        lngRanged = _iEncodeMin;
                    else if (lngRanged > _iEncodeMax)
                        lngRanged = _iEncodeMax;

                    if (telemetry != null)
                        telemetry.sFailure = "Sample#"+i+"=" + lngRanged + " won't fit between " + _iEncodeMin + " and " + _iEncodeMax;
                    _blnHadToClamp = true;
                }
                byte bEncoded = (byte) lngRanged;
                _abEncodedAdpcm[i] = bEncoded;

                // now decode what was just encoded -------------------

                // shift to the top of the short
                short siAdpcmShortTopSample = (short) (bEncoded << (16-_iAdpcmBitsPerSample));
                // shift sound data according to the range, keeping the sign
                int iUnRanged = (siAdpcmShortTopSample >> _iRange);

                // adjust according to the filter
                double dblDecodedPcm = iUnRanged + _filters.getK0(_iFilterIndex) * _contextSnapshot.dblPrev1
                                                 + _filters.getK1(_iFilterIndex) * _contextSnapshot.dblPrev2;

                _contextSnapshot.update(dblDecodedPcm);

                double dblDelta = Math.abs(dblDecodedPcm - siPcmSample);
                if (dblDelta > _dblMaxDelta)
                    _dblMaxDelta = dblDelta;

                if (telemetry != null) {
                    telemetry.ablnSampleClamped[i]       = _blnHadToClamp;
                    telemetry.asiSourcePcmSamples[i]     = siPcmSample;
                    telemetry.adblPrev1Samples[i]        = _contextSnapshot.dblPrev1;
                    telemetry.adblPrev2Samples[i]        = _contextSnapshot.dblPrev2;
                    telemetry.adblFilteredSamples[i]     = dblFiltered;
                    telemetry.adblRangedSamples[i]       = dblRanged;
                    telemetry.abEncodedAdpcmSamples[i]   = bEncoded;
                    telemetry.asiShortTopSamples[i]      = siAdpcmShortTopSample;
                    telemetry.adblDecodedSamples[i]      = dblDecodedPcm;
                }
            }

            if (telemetry != null) {
                telemetry.dblMaxDelta = _dblMaxDelta;
                TELEMETRY_LISTENER.soundUnitEncoded(telemetry);
            }

            return _blnHadToClamp;
        }
    }

    // =========================================================================
    // static

    /** For development testing of the encoder. */
    public static class Telemetry {
        @Nonnull
        private final IContextCopier _loggingContext;

        public final int iFilter;
        public final int iRange;

        public final boolean[] ablnSampleClamped;
        public final short[]  asiSourcePcmSamples;
        public final double[] adblPrev1Samples;
        public final double[] adblPrev2Samples;
        public final double[] adblFilteredSamples;
        public final double[] adblRangedSamples;
        public final byte[]   abEncodedAdpcmSamples;
        public final short[]  asiShortTopSamples;
        public final double[] adblDecodedSamples;

        public double dblMaxDelta;
        public String sFailure;

        private Telemetry(@Nonnull IContextCopier loggingContextx, int _iFilter, int _iRange) {
            _loggingContext = loggingContextx;
            iFilter         = _iFilter;
            iRange          = _iRange;
            ablnSampleClamped       = new boolean[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            asiSourcePcmSamples     = new short[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            adblPrev1Samples        = new double[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            adblPrev2Samples        = new double[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            adblFilteredSamples     = new double[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            adblRangedSamples       = new double[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            abEncodedAdpcmSamples   = new byte[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            asiShortTopSamples      = new short[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
            adblDecodedSamples      = new double[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
        }

        public @Nonnull Object getLoggingContextCopy() {
            return _loggingContext.copy();
        }

        /**
         * @param iSample Sample between 0 and 28 (exclusive).
         */
        public String sample(int iSample) {
            return String.format("Filter %d Range %d: Sample#%d %d filtered (Prev1 %f Prev2 %f) => %f ranged => %f. Encoded %d shifted => %d decoded => %f%s",
                    iFilter, iRange, iSample, asiSourcePcmSamples[iSample],
                    adblPrev1Samples[iSample], adblPrev2Samples[iSample],
                    adblFilteredSamples[iSample],
                    adblRangedSamples[iSample],
                    abEncodedAdpcmSamples[iSample],
                    asiShortTopSamples[iSample],
                    adblDecodedSamples[iSample],
                    ablnSampleClamped[iSample] ? " FAILED" : ""
                    );
        }
    }

    /** For development testing of the encoder. */
    public interface TelemetryListener {
        void soundUnitEncoded(@Nonnull Telemetry telemetry);
    }
}

