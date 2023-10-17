/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.Misc;

/** Represents a single un-interleaved sound unit found among the interleaved
 * sound units in a XA ADPCM. */
public class XaAdpcmSoundUnit implements IAdpcmSoundUnit {

    private static final Logger LOG = Logger.getLogger(XaAdpcmSoundUnit.class.getName());

    public static class Builder {
        private final SoundParameterBuilder _soundParameterBuilder = new SoundParameterBuilder();
        private int _iRedundantSoundParameterCount = 0;

        private final short[] _asiShiftedAdpcmSamples = new short[SoundUnitDecoder.SAMPLES_PER_SOUND_UNIT];
        private int _iSampleIndex = 0;

        public Builder() {
        }

        public void addRedundantParameter(int iSoundUnitParameter) {
            _soundParameterBuilder.add(_iRedundantSoundParameterCount, iSoundUnitParameter);
            _iRedundantSoundParameterCount++;
        }

        public void addShiftedAdpcmSample(short siShiftedSample) {
            _asiShiftedAdpcmSamples[_iSampleIndex] = siShiftedSample;
            _iSampleIndex++;
        }

        public @Nonnull XaAdpcmSoundUnit build(@Nonnull XaAdpcmDecoder.LogContext logContext) {
            return new XaAdpcmSoundUnit(_soundParameterBuilder.getBestParameter(logContext), _asiShiftedAdpcmSamples);
        }

    }

    private final int _iBestSoundUnitParameter;
    private final short[] _asiShiftedAdpcmSamples;

    public XaAdpcmSoundUnit(int iBestSoundUnitParameter,
                            @Nonnull short[] asiShiftedAdpcmSamples)
    {
        _iBestSoundUnitParameter = iBestSoundUnitParameter;
        _asiShiftedAdpcmSamples = asiShiftedAdpcmSamples;
    }

    @Override
    public int getRange() {
        return _iBestSoundUnitParameter & 0xf;
    }

    @Override
    public int getUncorruptedFilterIndex() {
        return (_iBestSoundUnitParameter >> 4) & 0xf;
    }

    @Override
    public short getShiftedAdpcmSample(int i) {
        return _asiShiftedAdpcmSamples[i];
    }


    /** Picks the best sound parameter.
     * Sound units contain redundant values of the sound parameters,
     * Ideally all sound parameters should have valid values, and the redundant
     * values should be identical. However, if corruption occurs, we can use
     * the redundancy to our advantage by picking the best possible
     * candidate. This doesn't guarantee the value is correct, but maybe
     * it's more correct? */
    private static class SoundParameterBuilder {

        @Nonnull
        private final ArrayList<SoundParameter> _allParameters = new ArrayList<SoundParameter>(4);
        @Nonnull
        private final ArrayList<SoundParameter> _uniqueParameters = new ArrayList<SoundParameter>(4);

        public SoundParameterBuilder() {
        }

        /** Add a redundant sound parameter value for this class to choose the
         * best one from. */
        public void add(int iIndex, int iValue) {
            SoundParameter param = new SoundParameter(iIndex, iValue);
            _allParameters.add(param);
            for (SoundParameter uniqueParam : _uniqueParameters) {
                if (uniqueParam.addIfEquals(param))
                    return;
            }
            _uniqueParameters.add(param);
        }

        /** Returns best parameter of all the parameters that were added. */
        public int getBestParameter(@Nonnull XaAdpcmDecoder.LogContext logContext) {
            boolean blnCorruption = false;
            if (_uniqueParameters.size() > 1) {
                // corruption!
                blnCorruption = true;
                // sort the best parameter to the top
                Collections.sort(_uniqueParameters);
            }
            SoundParameter chosen = _uniqueParameters.get(0);
            int iChosen = chosen.getValue();
            // also corruption if the first (chosen) one is bad
            blnCorruption = blnCorruption || !chosen.isValid();
            if (blnCorruption) {
                // corruption!
                logContext.blnHadCorruption = true;
                if (LOG.isLoggable(Level.WARNING)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(logContext).append(" sound parameter corrupted: [");
                    for (int i = 0; i < _allParameters.size(); i++) {
                        if (i != 0)
                            sb.append(", ");
                        sb.append(_allParameters.get(i));
                    }
                    sb.append("]. Chose ").append(chosen);
                    if (!chosen.isValid()) {
                        iChosen = chosen.getFixedValue();
                        sb.append(" corrected to ").append(iChosen);
                    }
                    LOG.log(Level.WARNING, sb.toString());
                }
            }

            return iChosen;
        }

    }

    /** ADPCM sound parameter. */
    private static class SoundParameter implements Comparable<SoundParameter> {
        /** Index of this redundant sound parameter. */
        private final int _iIndex;
        private final int _iValue;
        private final boolean _blnIsValid;
        /** Stores any other redundant sound parameters with the same value. */
        private final ArrayList<SoundParameter> _duplicates = new ArrayList<SoundParameter>(3);

        public SoundParameter(int iIndex, int iValue) {
            _iIndex = iIndex;
            _iValue = iValue;
            _blnIsValid = (_iValue & 0xC0) == 0;
        }

        public int getIndex() {
            return _iIndex;
        }

        public int getValue() {
            return _iValue;
        }

        public int getFixedValue() {
            int iFilterIndex = (_iValue >>> 4) & 0xf;
            int iRange =        _iValue        & 0xF;
            if (iFilterIndex > 3)
                iFilterIndex = 3;
            return(iFilterIndex << 4) | iRange;
        }

        public boolean isValid() {
            return _blnIsValid;
        }

        /** Checks if other {@link SoundParameter} has the same value.
         * If so, add it to the internal list of duplicates.
         * @return if other {@link SoundParameter} has the same value. */
        public boolean addIfEquals(@Nonnull SoundParameter other) {
            if (other._iValue == _iValue) {
                _duplicates.add(other);
                return true;
            }
            return false;
        }

        /** Sorts by:
         * <ol>
         *  <li>valid values
         *      <ol><li>number of duplicates</ol>
         *  <li>then by index
         * </ol>
         * This should put the best SoundParameter as the first item.
         * @return -1 or 1. Never 0.
         */
        @Override
        public int compareTo(@Nonnull SoundParameter other) {
            if (_iValue == other._iValue)
                throw new IllegalStateException("I did something wrong");

            if (_blnIsValid && !other._blnIsValid)
                return -1;
            if (!_blnIsValid && other._blnIsValid)
                return 1;

            if (_blnIsValid) {
                // both are valid
                // sort by who has the most duplicates
                if (_duplicates.size() > other._duplicates.size())
                    return -1;
                else if (_duplicates.size() < other._duplicates.size())
                    return 1;
            } // just sort invalid values by index

            if (_iIndex < other._iIndex)
                return -1;
            else
                return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Should never happen");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(_iIndex).append(']');
            for (SoundParameter dup : _duplicates) {
                sb.append(",[").append(dup._iIndex).append(']');
            }
            sb.append('=').append(Misc.bitsToString(_iValue, 8));
            if (!_blnIsValid)
                sb.append(" (bad)");
            return sb.toString();
        }

    }


}
