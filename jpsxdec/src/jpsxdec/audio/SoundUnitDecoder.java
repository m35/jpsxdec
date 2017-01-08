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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


public class SoundUnitDecoder {

    /** Set this global telemetry listener to receive information about each
     * decoded Sound Unit. */
    @CheckForNull
    public static TelemetryListener TELEMETRY_LISTENER = null;

    public static final int SAMPLES_PER_SOUND_UNIT = 28;

    @Nonnull
    private final K0K1Filter _filterTable;

    /** Holds the ADPCM samples for this sound-unit,
     * each sample shifted to the top of the short. */
    private final short[] _asiShiftedAdpcmBuffer = new short[SAMPLES_PER_SOUND_UNIT];
    /** Writing buffer index. Reset at the start/end of decoding. */
    private int _iWritePos = 0;

    /** The 'filter' index parameter. Which entry in {@link #_filterTable}
     * to multiply the previous two ADPCM samples by. */
    private int _iFilterIndex = -1;
    /** The 'range' parameter. How many bits to shift the ADPCM sample to
     * the right. */
    private int _iRange = -1;


    public SoundUnitDecoder(@Nonnull K0K1Filter filterTable) {
        _filterTable = filterTable;
    }

    public void setParameters(int iParameter) {
        setFilterAndRange(
            (iParameter >>> 4) & 0xf,
             iParameter        & 0xF
        );
    }
    public void setFilterAndRange(int iFilterIndex, int iRange) {
        if (iFilterIndex < 0 || iFilterIndex >= _filterTable.getCount())
            throw new IllegalArgumentException();
        _iFilterIndex = iFilterIndex;
        _iRange = iRange;
        _iWritePos = 0;
    }
    
    /** Write an ADPCM sample to the internal buffer.
     * The sample should be shifted to the top of the short. */
    public void addShiftedAdpcmSample(short siShiftedAdpcmSample) {
        _asiShiftedAdpcmBuffer[_iWritePos] = siShiftedAdpcmSample;
        _iWritePos++;
    }

    /** Taking de-interleaved sound unit data, the sound parameter for that
     *  unit, and the decoding context (for the previous 2 samples read),
     *  we can convert an entire ADPCM sound unit to PCM samples.
     * Converts sound samples as they are read. */
    void decodeSoundUnit(@Nonnull AdpcmContext context,
                         @Nonnull short[] asiPcmOutBuffer,
                         @Nonnull IContextCopier loggingContext)
    {
        Telemetry telemetry = null;
            if (TELEMETRY_LISTENER != null)
                telemetry = new Telemetry(loggingContext, _iFilterIndex, _iRange);
        for (int i = 0; i < SAMPLES_PER_SOUND_UNIT; i++) {
            short siAdpcmShortTopSample = _asiShiftedAdpcmBuffer[i];

            // shift sound data according to the range, keeping the sign
            int iUnRanged = _asiShiftedAdpcmBuffer[i] >> _iRange;

            // adjust according to the filter
            double dblDecodedPcm =
                iUnRanged + _filterTable.getK0(_iFilterIndex) * context.getPreviousPCMSample1()
                          + _filterTable.getK1(_iFilterIndex) * context.getPreviousPCMSample2();

            if (telemetry != null) {
                telemetry.asiSourceAdpcmSamples[i] = siAdpcmShortTopSample;
                telemetry.aiUnRangedSamples[i] = iUnRanged;
                telemetry.adblPrev1Samples[i] = context.getPreviousPCMSample1();
                telemetry.adblPrev2Samples[i] = context.getPreviousPCMSample2();
                telemetry.adblDecodedPcmSamples[i] = dblDecodedPcm;
            }
            
            // let the context scale, round, and clamp
            short siPcmSample = context.saveScaleRoundClampPCMSample(dblDecodedPcm);
            // finally return the polished sample
            asiPcmOutBuffer[i] = siPcmSample;
        }
        _iWritePos = 0;

        if (telemetry != null && TELEMETRY_LISTENER != null)
            TELEMETRY_LISTENER.soundUnitDecoded(telemetry);
    }

    // =========================================================================
    // static

    /** For development testing of the decoder. */
    public static class Telemetry {
        @Nonnull
        private final IContextCopier _loggingContext;

        public final int iFilter;
        public final int iRange;

        public final short[]  asiSourceAdpcmSamples;
        public final double[] adblPrev1Samples;
        public final double[] adblPrev2Samples;
        public final int[]    aiUnRangedSamples;
        public final double[] adblDecodedPcmSamples;

        private Telemetry(@Nonnull IContextCopier loggingContext, int _iFilter, int _iRange) {
            _loggingContext = loggingContext;
            iFilter         = _iFilter;
            iRange          = _iRange;
            asiSourceAdpcmSamples   = new short[SAMPLES_PER_SOUND_UNIT];
            adblPrev1Samples        = new double[SAMPLES_PER_SOUND_UNIT];
            adblPrev2Samples        = new double[SAMPLES_PER_SOUND_UNIT];
            aiUnRangedSamples       = new int[SAMPLES_PER_SOUND_UNIT];
            adblDecodedPcmSamples   = new double[SAMPLES_PER_SOUND_UNIT];
        }

        public @Nonnull Object getLoggingContextCopy() {
            return _loggingContext.copy();
        }

        /**
         * @param iSample Sample between 0 and 28 (exclusive).
         */
        public String sample(int iSample) {
            return String.format(
                    "Filter %d Range %d: Sample#%d %d unranged => %d filtered (Prev1 %f Prev2 %f) => %f",
                    iFilter, iRange, iSample, asiSourceAdpcmSamples[iSample],
                    aiUnRangedSamples[iSample],
                    adblPrev1Samples[iSample], adblPrev2Samples[iSample],
                    adblDecodedPcmSamples[iSample]
                    );
        }

        public String toString() {
            return String.format("%s Filter.Range %d.%d",
                    _loggingContext,
                    iFilter,
                    iRange
            );
        }
    }

    /** For development testing of the encoder. */
    public interface TelemetryListener {
        void soundUnitDecoded(@Nonnull Telemetry telemetry);
    }

}
