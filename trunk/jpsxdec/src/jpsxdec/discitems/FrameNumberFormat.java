/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2016  Michael Sabin
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

package jpsxdec.discitems;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import static jpsxdec.discitems.FrameNumber.HEADER_PREFIX;
import static jpsxdec.discitems.FrameNumber.SECTOR_PREFIX;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Maintains information to format a {@link FrameNumber} for consistent display.
 * Generates {@link FrameNumberFormatter}s to format for a particular number. */
public class FrameNumberFormat {

    /** Builds up a {@link FrameNumberFormat} from a series of frames.
     * Call {@link #addFrame(jpsxdec.discitems.FrameNumber)} with every frame
     * in a video, then call {@link #getFormat()} to get the resulting format. */
    public static class Builder {
        private int _iMaxFrameIndex = 0;
        private int _iMaxSector = 0;
        private int _iMaxSectorDuplicate = 0;
        private int _iMaxHeaderFrame = 0;
        private int _iMaxHeaderDuplicate = 0;

        public void addFrame(@Nonnull FrameNumber frameNumber) {
            if (frameNumber.getIndex() > _iMaxFrameIndex)
                _iMaxFrameIndex = frameNumber.getIndex();
            if (frameNumber.getSector()> _iMaxSector)
                _iMaxSector = frameNumber.getSector();
            if (frameNumber.getSectorDuplicate()> _iMaxSectorDuplicate)
                _iMaxSectorDuplicate = frameNumber.getSectorDuplicate();
            if (frameNumber.getHeaderFrameNumber() > _iMaxHeaderFrame)
                _iMaxHeaderFrame = frameNumber.getHeaderFrameNumber();
            if (frameNumber.getHeaderFrameDuplicate() > _iMaxHeaderDuplicate)
                _iMaxHeaderDuplicate = frameNumber.getHeaderFrameDuplicate();
        }

        public @Nonnull FrameNumberFormat getFormat() {
            return new FrameNumberFormat(_iMaxFrameIndex, _iMaxSector, _iMaxSectorDuplicate,
                                         _iMaxHeaderFrame, _iMaxHeaderDuplicate);
        }
    }

    //==========================================================================

    public static enum Type {
        Index(I.FRAME_NUM_FMT_INDEX(), null),
        Sector(I.FRAME_NUM_FMT_SECTOR(), FrameNumber.SECTOR_PREFIX),
        Header(I.FRAME_NUM_FMT_HEADER(), FrameNumber.HEADER_PREFIX),
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

    //==========================================================================

    private final int _iIndexDigitCount;
    private final int _iSectorDigitCount;
    private final int _iSectorDuplicateDigitCount;
    private final int _iHeaderFrameDigitCount;
    private final int _iHeaderDuplicateDigitCount;

    private FrameNumberFormat(int iMaxFrameIndex, int iMaxSector, int iMaxSectorDuplicate,
                              int iMaxHeaderFrame, int iMaxHeaderDuplicate)
    {
        _iIndexDigitCount = String.valueOf(iMaxFrameIndex).length();
        _iSectorDigitCount = String.valueOf(iMaxSector).length();
        if (iMaxSectorDuplicate == 0)
            _iSectorDuplicateDigitCount = 0;
        else
            _iSectorDuplicateDigitCount = String.valueOf(iMaxSectorDuplicate).length();
        _iHeaderFrameDigitCount = String.valueOf(iMaxHeaderFrame).length();
        if (iMaxHeaderDuplicate == 0)
            _iHeaderDuplicateDigitCount = 0;
        else
            _iHeaderDuplicateDigitCount = String.valueOf(iMaxHeaderDuplicate).length();
    }

    /** Deserialize. */
    FrameNumberFormat(@Nonnull String sSerialized) throws NotThisTypeException {
        String[] as = Misc.regex(
                "^(\\d+)/"+SECTOR_PREFIX+"(\\d+)(\\.(\\d+))?(/"+HEADER_PREFIX+"(\\d+)(\\.(\\d+))?)?$",
                sSerialized);
        if (as == null)
            throw new NotThisTypeException(I.INVALID_FRAME_NUMBER_FORMAT(sSerialized));

        try {
            _iIndexDigitCount = Integer.parseInt(as[1]);
            _iSectorDigitCount = Integer.parseInt(as[2]);
            _iSectorDuplicateDigitCount = Misc.parseIntOrDefault(as[4], 0);
            _iHeaderFrameDigitCount = Misc.parseIntOrDefault(as[6], -1);
            _iHeaderDuplicateDigitCount = Misc.parseIntOrDefault(as[8],
                                          _iHeaderFrameDigitCount == -1 ? -1 : 0);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException(I.INVALID_FRAME_NUMBER_FORMAT(sSerialized));
        }
    }


    /** Serialize.
     * <p>
     * Format: index,@sector.dup,#header.dup
     */
    public @Nonnull String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(_iIndexDigitCount);
        sb.append('/');
        sb.append(SECTOR_PREFIX);
        sb.append(_iSectorDigitCount);
        if (_iSectorDuplicateDigitCount > 0) {
            sb.append('.');
            sb.append(_iSectorDuplicateDigitCount);
        }
        sb.append('/');
        sb.append(HEADER_PREFIX);
        sb.append(_iHeaderFrameDigitCount);
        if (_iHeaderDuplicateDigitCount > 0) {
            sb.append('.');
            sb.append(_iHeaderDuplicateDigitCount);
        }
        return sb.toString();
    }

    public @Nonnull FrameNumberFormatter makeFormatter(@Nonnull Type type) {
        switch (type) {
            case Index:
                return new FrameNumberFormatter.Index(_iIndexDigitCount);
            case Sector:
                return new FrameNumberFormatter.Sector(_iSectorDigitCount, _iSectorDuplicateDigitCount);
            case Header:
                return new FrameNumberFormatter.Header(_iHeaderFrameDigitCount, _iHeaderDuplicateDigitCount);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public String toString() {
        return serialize();
    }
}
