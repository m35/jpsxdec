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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;

/** A frame number containing a formatted index and start sector, and possibly
 *  a header frame number. */
public abstract class FrameNumber {

    static final char SECTOR_PREFIX = '@';
    static final char HEADER_PREFIX = '#';

    public static enum Type {
        Index(I.FRAME_NUM_FORMAT_INDEX(), null),
        Sector(I.FRAME_NUM_FORMAT_SECTOR(), SECTOR_PREFIX),
        Header(I.FRAME_NUM_FORMAT_HEADER(), HEADER_PREFIX),
        ;

        @Nonnull
        private final ILocalizedMessage _str;
        @CheckForNull
        private final Character _oChar;

        private Type(@Nonnull ILocalizedMessage str, @CheckForNull Character oChar) {
            _str = str;
            _oChar = oChar;
        }

        public static @CheckForNull Type fromCmdLine(@Nonnull String sCmdLine) {
            for (Type t : Type.values()) {
                if (t._str.equalsIgnoreCase(sCmdLine) ||
                    (sCmdLine.length() == 1 && t._oChar != null && t._oChar.charValue() == sCmdLine.charAt(0)))
                    return t;
            }
            return null;
        }

        public @Nonnull ILocalizedMessage getLocalizedName() {
            return _str;
        }

        @Override
        public String toString() {
            return _str.getLocalizedMessage();
        }
    }


    // -------------------------------------------------------------------------

    protected final int _iFrameIndex;
    protected final int _iFrameCount;

    @Nonnull
    final FrameNumberNumber _sectorNumber;
    @CheckForNull
    private final FrameNumberNumber _headerFrameNumber;

    FrameNumber(int iFrameIndex, int iFrameCount,
                @Nonnull FrameNumberNumber sectorNumber)
    {
        _iFrameIndex = iFrameIndex;
        _iFrameCount = iFrameCount;
        _sectorNumber = sectorNumber;
        _headerFrameNumber = null;
    }

    FrameNumber(int iFrameIndex, int iFrameCount,
                @Nonnull FrameNumberNumber sectorFrameNumber,
                @Nonnull FrameNumberNumber headerFrameNumber)
    {
        _iFrameIndex = iFrameIndex;
        _iFrameCount = iFrameCount;
        _sectorNumber = sectorFrameNumber;
        _headerFrameNumber = headerFrameNumber;
    }


    public @CheckForNull FormattedFrameNumber getNumber(@Nonnull Type numberType) {
        switch (numberType) {
            case Index: return getIndexNumber();
            case Sector: return getSectorNumber();
            case Header: return getHeaderNumber();
            default: throw new RuntimeException();
        }
    }
    public @CheckForNull ILocalizedMessage getDescription(@Nonnull Type numberType) {
        switch (numberType) {
            case Index: return getIndexDescription();
            case Sector: return getSectorDescription();
            case Header: return getHeaderDescription();
            default: throw new RuntimeException();
        }
    }
    public @CheckForNull String getUnformattedNumber(@Nonnull Type numberType) {
        switch (numberType) {
            case Index: return getUnformattedIndexNumber();
            case Sector: return getUnformattedSectorNumber();
            case Header: return getUnformattedHeaderNumber();
            default: throw new RuntimeException();
        }
    }

    public @Nonnull ILocalizedMessage getIndexDescription() {
        return I.FRAME_NUM_FORMATTER_FRAME(getUnformattedIndexNumber());
    }
    public @Nonnull ILocalizedMessage getSectorDescription() {
        return I.FRAME_NUM_FORMATTER_SECTOR(getUnformattedSectorNumber());
    }
    public @CheckForNull ILocalizedMessage getHeaderDescription() {
        if (_headerFrameNumber == null)
            return null;
        return I.FRAME_NUM_FORMATTER_FRAME(HEADER_PREFIX+getUnformattedHeaderNumber());
    }

    public @Nonnull FormattedFrameNumber getIndexNumber() {
        return new FormattedFrameNumber(_iFrameIndex, FrameNumberNumber.base10digitCount(_iFrameCount));
    }
    public @Nonnull FormattedFrameNumber getSectorNumber() {
        return new FormattedFrameNumber(SECTOR_PREFIX,
                _sectorNumber.getFrameValue(), _sectorNumber.getFrameDigitLength(),
                _sectorNumber.getDuplicateIndex(), _sectorNumber.getDuplicateDigitLength());
    }
    public @CheckForNull FormattedFrameNumber getHeaderNumber() {
        if (_headerFrameNumber == null)
            return null;
        return new FormattedFrameNumber(HEADER_PREFIX,
                _headerFrameNumber.getFrameValue(), _headerFrameNumber.getFrameDigitLength(),
                _headerFrameNumber.getDuplicateIndex(), _headerFrameNumber.getDuplicateDigitLength());
    }

    public @Nonnull String getUnformattedIndexNumber() {
        return String.valueOf(_iFrameIndex);
    }
    public @Nonnull String getUnformattedSectorNumber() {
        if (_sectorNumber.getDuplicateIndex() == 0)
            return String.valueOf(_sectorNumber.getFrameValue());
        else
            return _sectorNumber.getFrameValue() + "." + _sectorNumber.getDuplicateIndex();
    }
    public @CheckForNull String getUnformattedHeaderNumber() {
        if (_headerFrameNumber == null)
            return null;
        if (_sectorNumber.getDuplicateIndex() == 0)
            return String.valueOf(_headerFrameNumber.getFrameValue());
        else
            return _headerFrameNumber.getFrameValue() + "." + _headerFrameNumber.getDuplicateIndex();
    }

    public boolean equalValue(@Nonnull FrameNumber other) {
        boolean blnEqual = _iFrameIndex == other._iFrameIndex &&
                           _sectorNumber.equalValue(other._sectorNumber);
        if (!blnEqual)
            return false;

        if (_headerFrameNumber == null) {
            return other._headerFrameNumber == null;
        } else {
            if (other._headerFrameNumber == null)
                return false;
            return _headerFrameNumber.equalValue(other._headerFrameNumber);
        }
    }

    @Override
    public String toString() {
        FormattedFrameNumber headerNumber = getHeaderNumber();
        if (headerNumber == null)
            return getIndexNumber() + "/" + getSectorNumber();
        else
            return getIndexNumber() + "/" + getSectorNumber() + "/" + headerNumber;
    }
}
