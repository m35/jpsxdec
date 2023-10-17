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

package jpsxdec.modules.video.framenumber;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;

/** Manages a "value" and "duplicates of that value" type of numbering.
 * This is used for numbering both frame start sectors, and header frame numbers.
 * In some cases, it's possible for more than one frame to begin in the same
 * sector, and for multiple frames to have the same header frame number.
 * Hence the need for maintain a "duplicate" value. */
class FrameNumberNumber {

    private static final Logger LOG = Logger.getLogger(FrameNumberNumber.class.getName());

    /** Even though a frame number doesn't really need to know the minimum and
     * maximum possible values, it helps with debugging, and for generating
     * warning with all the details. */
    private final int _iFrameMinValue,
                      _iFrameValue,
                      _iFrameMaxValue;

    /** Likewise, keeping the max possible duplicate value helps with
     * debugging and messages. */
    private final int _iDuplicateIndex,
                      _iDuplicateMax;

    public FrameNumberNumber(int iFrameMinValue, int iFrameValue, int iFrameMaxValue,
                             int iDuplicateIndex, int iDuplicateMax)
    {
        if (iFrameMinValue < 0 || iFrameValue < 0 || iFrameMaxValue < 0 ||
            iDuplicateIndex < 0 || iDuplicateMax < 0)
        {
            throw new IllegalArgumentException();
        }
        _iFrameMinValue = iFrameMinValue;
        _iFrameValue = iFrameValue;
        _iFrameMaxValue = iFrameMaxValue;
        _iDuplicateIndex = iDuplicateIndex;
        _iDuplicateMax = iDuplicateMax;
    }

    public int getFrameValue() {
        return _iFrameValue;
    }

    public int getExpectedFrameMinValue() {
        return _iFrameMinValue;
    }

    public int getExpectedFrameMaxValue() {
        return _iFrameMaxValue;
    }

    public int getExpectedDuplicateMax() {
        return _iDuplicateMax;
    }

    public int getFrameDigitLength() {
        return base10digitCount(_iFrameMaxValue);
    }

    public int getDuplicateIndex() {
        return _iDuplicateIndex;
    }

    public int getDuplicateDigitLength() {
        return base10digitCount(_iDuplicateMax);
    }

    public boolean frameNumberOutOfMaxBounds() {
        return _iFrameValue > _iFrameMaxValue;
    }

    public boolean frameNumberOutOfMinBounds() {
        return _iFrameValue < _iFrameMinValue;
    }

    public boolean tooManyDuplicates() {
        return _iDuplicateIndex > _iDuplicateMax;
    }

    public boolean equalValue(@Nonnull FrameNumberNumber other) {
        return _iFrameValue == other._iFrameValue &&
               _iDuplicateIndex == other._iDuplicateIndex;
    }

    @Override
    public String toString() {
        return format(_iFrameValue, getFrameDigitLength(),
                      _iDuplicateIndex, getDuplicateDigitLength());
    }

    /** Utility function returns the number of base-10 digits a value will consist of. */
    static int base10digitCount(int i) {
        if (i < 0)
            throw new IllegalArgumentException(""+i);
        int iDigits = 0;
        while (i != 0) {
            iDigits++;
            i /= 10;
        }
        return iDigits;
    }

    /** Shared function to generate a number in the format of %0?d[.%0?d]. */
    static @Nonnull String format(int iMajorValue, int iMajorDigits, int iMinorValue, int iMinorDigits) {
        String sMajor = Misc.intToPadded0String(iMajorValue, iMajorDigits);
        if (iMinorDigits == 0 && iMinorValue == 0) {
            return sMajor;
        } else {
            return sMajor + "." + Misc.intToPadded0String(iMinorValue, iMinorDigits);
        }
    }

    // -------------------------------------------------------------------------

    public static class Format {

        public static class Builder {
            private final int _iStartFrameValue;

            private int _iEndFrameValue;
            private int _iDuplicateMax = 0;

            private int _iDuplicate = 0;

            public Builder(int iStartFrameValue) {
                if (iStartFrameValue < 0)
                    throw new IllegalArgumentException();
                _iStartFrameValue = iStartFrameValue;
                _iEndFrameValue = iStartFrameValue;
            }

            public void addNumber(int iFrameValue) {
                if (iFrameValue < 0 || iFrameValue < _iEndFrameValue)
                    // videos should have ended if the frame number got smaller
                    throw new IllegalArgumentException("Negative frame value, or in reverse? " + iFrameValue + " < " + _iEndFrameValue);

                if (iFrameValue == _iEndFrameValue) {
                    _iDuplicate++;
                    LOG.log(Level.INFO, "Duplicate header or sector frame number {0,number,#}", iFrameValue);
                    if (_iDuplicate > _iDuplicateMax)
                        _iDuplicateMax = _iDuplicate;
                } else {
                    _iEndFrameValue = iFrameValue;
                    _iDuplicate = 0;
                }
            }

            public int getStartFrameValue() {
                return _iStartFrameValue;
            }

            public int getLastFrameValue() {
                return _iEndFrameValue;
            }

            public @Nonnull Format makeFormat() {
                return new Format(_iStartFrameValue, _iEndFrameValue,
                                  _iDuplicate, _iDuplicateMax);
            }

            @Override
            public String toString() {
                return _iStartFrameValue+"-"+_iEndFrameValue+"."+_iDuplicate+"'"+_iDuplicateMax;
            }
        }


        private final int _iStartFrameValue;
        private final int _iEndFrameValue;
        private final int _iEndDuplicate;
        private final int _iDuplicateMax;

        public Format(int iStartFrameValue, int iEndFrameValue,
                      int iEndDuplicate, int iDuplicateMax)
        {
            if (iStartFrameValue < 0 || iEndFrameValue < 0 ||
                iEndDuplicate < 0 || iDuplicateMax < 0)
            {
                throw new IllegalArgumentException();
            }

            _iStartFrameValue = iStartFrameValue;
            _iEndFrameValue = iEndFrameValue;
            _iEndDuplicate = iEndDuplicate;
            _iDuplicateMax = iDuplicateMax;
        }

        private static final Pattern SERIALIZED_FORMAT = Pattern.compile(
              //  ( 1  ) ( 2  )       ( 4  ) ( 5  )
                "^(\\d+)-(\\d+)"+"(\\.(\\d+)/(\\d+))?$");

        public Format(@Nonnull String sSerialized) throws LocalizedDeserializationFail {
            String[] as = Misc.regex(SERIALIZED_FORMAT, sSerialized);
            if (as == null)
                throw new LocalizedDeserializationFail(I.FRAME_NUM_INVALID(sSerialized));
            try {
                _iStartFrameValue = Integer.parseInt(as[1]);
                _iEndFrameValue = Integer.parseInt(as[2]);
                _iEndDuplicate = as[4] == null ? 0 : Integer.parseInt(as[4]);
                _iDuplicateMax = as[5] == null ? 0 : Integer.parseInt(as[5]);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Regex should prevent this", ex);
            }
        }

        public @Nonnull String serialize() {
            StringBuilder sb = new StringBuilder();
            sb.append(_iStartFrameValue).append('-').append(_iEndFrameValue);
            if (_iEndDuplicate > 0 || _iDuplicateMax > 0) {
                sb.append('.').append(_iEndDuplicate)
                  .append('/').append(_iDuplicateMax);
            }
            return sb.toString();
        }

        public @Nonnull FrameNumberNumber getStart() {
            return  new FrameNumberNumber(_iStartFrameValue, _iStartFrameValue, _iEndFrameValue,
                                          0, _iDuplicateMax);
        }

        public @Nonnull FrameNumberNumber getEnd() {
            return new FrameNumberNumber(_iStartFrameValue, _iEndFrameValue, _iEndFrameValue,
                                         _iEndDuplicate, _iDuplicateMax);
        }

        public @Nonnull Formatter makeFormatter() {
            return new Formatter(this);
        }

        @Override
        public String toString() {
            return serialize();
        }

    }

    public static class Formatter {
        private final Format _format;

        private int _iPrevFrameValue = -1;
        private int _iDuplicateValue = 0;

        public Formatter(@Nonnull Format format) {
            _format = format;
        }

        public @Nonnull FrameNumberNumber next(int iFrameValue) {
            if (iFrameValue < 0)
                throw new IllegalArgumentException();

            if (iFrameValue == _iPrevFrameValue) {
                _iDuplicateValue++;
            } else {
                _iPrevFrameValue = iFrameValue;
                _iDuplicateValue = 0;
            }

            return new FrameNumberNumber(_format._iStartFrameValue, iFrameValue,
                                         _format._iEndFrameValue, _iDuplicateValue,
                                         _format._iDuplicateMax);
        }
    }


}
