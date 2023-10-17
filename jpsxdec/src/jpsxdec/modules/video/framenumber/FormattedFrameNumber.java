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

import javax.annotation.Nonnull;


/** A pre-formatted frame number with necessary leading and trailing zeros
 * to maintain a same string length with adjacent FormattedFrameNumbers. */
public class FormattedFrameNumber {

    private final char _cPrefix;
    private final int _iFrameValue;
    private final int _iFrameDigitLength;
    private final int _iDuplicateIndex;
    private final int _iDuplicateDigitLength;

    public FormattedFrameNumber(int iFrameValue) {
        this(iFrameValue, 0);
    }

    public FormattedFrameNumber(int iFrameValue, int iFrameDigitLength) {
        this(iFrameValue, iFrameDigitLength, 0, 0);
    }

    public FormattedFrameNumber(int iFrameValue, int iFrameDigitLength,
                                int iDuplicateIndex, int iDuplicateDigitLength)
    {
        this('\0', iFrameValue, iFrameDigitLength,
                   iDuplicateIndex, iDuplicateDigitLength);
    }
    public FormattedFrameNumber(char cPrefix,
                                int iFrameValue, int iFrameDigitLength,
                                int iDuplicateIndex, int iDuplicateDigitLength)
    {
        _cPrefix = cPrefix;
        _iFrameValue = iFrameValue;
        _iFrameDigitLength = iFrameDigitLength;
        _iDuplicateIndex = iDuplicateIndex;
        _iDuplicateDigitLength = iDuplicateDigitLength;
    }

    public int getFrameValue() {
        return _iFrameValue;
    }

    public int getDuplicateIndex() {
        return _iDuplicateIndex;
    }

    public @Nonnull String getMaxFormat() {
        StringBuilder sb = new StringBuilder();
        if (_cPrefix != '\0')
            sb.append(_cPrefix);

        if (_iFrameDigitLength == 0) {
            sb.append('0');
        } else {
            for (int i = 0; i < _iFrameDigitLength; i++) {
                sb.append('9');
            }
        }

        if (_iDuplicateDigitLength == 0) {
            if (_iDuplicateIndex > 0) {
                sb.append('.');
                sb.append('0');
            }
        } else {
            sb.append('.');
            for (int i = 0; i < _iFrameDigitLength; i++) {
                sb.append('9');
            }
        }

        return sb.toString();
    }

    public @Nonnull String getUnpaddedValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(_iFrameValue);
        if (_iDuplicateIndex > 0 || _iDuplicateDigitLength > 0)
            sb.append('.').append(_iDuplicateIndex);
        return sb.toString();
    }

    @Override
    public @Nonnull String toString() {
        String sPrefix;
        if (_cPrefix == '\0')
            sPrefix = "";
        else
            sPrefix = String.valueOf(_cPrefix);
        return sPrefix + FrameNumberNumber.format(_iFrameValue,
                                                  _iFrameDigitLength,
                                                  _iDuplicateIndex,
                                                  _iDuplicateDigitLength);
    }

    public boolean digitsAreOutOfBounds() {
        return FrameNumberNumber.base10digitCount(_iFrameValue) > _iFrameDigitLength ||
               FrameNumberNumber.base10digitCount(_iDuplicateIndex) > _iDuplicateDigitLength;
    }
}
