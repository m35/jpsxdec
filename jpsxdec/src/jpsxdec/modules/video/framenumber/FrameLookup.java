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

import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;

/** Used to lookup a frame number using index, sector, or frame header number. */
public class FrameLookup {

    @Nonnull
    private final FrameNumber.Type _type;

    private final int _iFrameValue;
    private final int _iDuplicateIndex;

    /** Assumes a duplicate index of 0. */
    public FrameLookup(@Nonnull FrameNumber.Type type, int iFrameValue) {
        this(type, iFrameValue, 0);
    }
    public FrameLookup(@Nonnull FrameNumber.Type type, int iFrameValue, int iDuplicateIndex) {
        _type = type;
        _iFrameValue = iFrameValue;
        _iDuplicateIndex = iDuplicateIndex;
    }

    private static final String REGEX =
        //|--------------------- 1 ----------------------------------| |-2--|  |-4--|
        "^(["+FrameNumber.SECTOR_PREFIX+FrameNumber.HEADER_PREFIX+"])?(\\d+)(.(\\d+))?$";
    private static final Pattern PARSER = Pattern.compile(REGEX);

    public FrameLookup(@Nonnull String s) throws LocalizedDeserializationFail {

        String[] as = Misc.regex(PARSER, s);
        if (as == null)
            throw new LocalizedDeserializationFail(I.FRAME_NUM_INVALID(s));
        try {
            _iFrameValue = Integer.parseInt(as[2]);
            if (as[4] == null)
                _iDuplicateIndex = 0;
            else
                _iDuplicateIndex = Integer.parseInt(as[4]);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Regex should prevent this");
        }
        if (as[1] == null)
            _type = FrameNumber.Type.Index;
        else if (as[1].charAt(0) == FrameNumber.SECTOR_PREFIX)
            _type = FrameNumber.Type.Sector;
        else if (as[1].charAt(0) == FrameNumber.HEADER_PREFIX)
            _type = FrameNumber.Type.Header;
        else
            throw new RuntimeException("Should not happen");
    }

    public @Nonnull String serialize() {
        String sNumber;
        if (_iDuplicateIndex == 0) {
            sNumber = String.valueOf(_iFrameValue);
        } else {
            sNumber = _iFrameValue + "." + _iDuplicateIndex;
        }
        switch (_type) {
            case Index:
                return sNumber;
            case Sector:
                return FrameNumber.SECTOR_PREFIX + sNumber;
            case Header:
                return FrameNumber.HEADER_PREFIX + sNumber;
            default:
                throw new RuntimeException();
        }
    }

    public @Nonnull FrameNumber.Type getType() {
        return _type;
    }

    public @Nonnull FrameCompareIs compareTo(FrameNumber frameNumber) {
        FormattedFrameNumber ffn;
        switch (_type) {
            case Index:
                ffn = frameNumber.getIndexNumber();
                break;
            case Sector:
                ffn = frameNumber.getSectorNumber();
                break;
            case Header:
                ffn = frameNumber.getHeaderNumber();
                if (ffn == null)
                    return FrameCompareIs.INVALID;
                break;
            default: throw new RuntimeException();
        }
        int i = Integer.compare(_iFrameValue, ffn.getFrameValue());
        if (i == 0)
            i = Integer.compare(_iDuplicateIndex, ffn.getDuplicateIndex());

        if (i < 0)
            return FrameCompareIs.LESSTHAN;
        else if (i > 0)
            return FrameCompareIs.GREATERTHAN;
        else
            return FrameCompareIs.EQUAL;
    }

    @Override
    public String toString() {
        return serialize();
    }
}
