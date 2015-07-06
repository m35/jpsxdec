/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2015  Michael Sabin
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
import jpsxdec.i18n.I;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Represents a frame number extracted from sector headers.
 * In rare cases the frame number found in sector headers may be duplicated.
 * This handles the duplication gracefully. */
public class FrameNumber {

    public static final char SECTOR_PREFIX = '@';
    public static final char HEADER_PREFIX = '#';

    /** Generates sequence of {@link FrameNumber}s.
     * Duplicates are automatically detected. */
    public static class FactoryWithHeader {
        @CheckForNull
        private FrameNumber _lastNumber;

        public FrameNumber next(int iSector, int iHeaderFrameNumber) {
            if (_lastNumber == null) {
                _lastNumber = new FrameNumber(0, iSector, 0, iHeaderFrameNumber, 0);
            } else {
                final int iDupSector;
                if (iSector == _lastNumber._iStartSector)
                    iDupSector = _lastNumber._iStartSectorDuplicateIndex+1;
                else
                    iDupSector = 0;
                final int iDupFrame;
                if (iHeaderFrameNumber == _lastNumber._iHeaderFrameNumber) {
                    iDupFrame = _lastNumber._iHeaderDuplicateIndex+1;
                } else {
                    iDupFrame = 0;
                }
                _lastNumber = new FrameNumber(_lastNumber._iIndex+1,
                                              iSector, iDupSector,
                                              iHeaderFrameNumber, iDupFrame);
            }
            return _lastNumber;
        }
    }

    /** For serialization. */
    public static @Nonnull String toRange(@Nonnull FrameNumber start, @Nonnull FrameNumber end) {
        return start.serialize()+ "-" + end.serialize();
    }

    /** For serialization. */
    public static @Nonnull FrameNumber[] parseRange(@Nonnull String sHeaderFrameRange) throws NotThisTypeException {
        String[] as = sHeaderFrameRange.split("\\-");
        if (as.length != 2)
            throw new NotThisTypeException(I.INVALID_FRAME_RANGE(sHeaderFrameRange));
        FrameNumber[] ao = new FrameNumber[2];
        ao[0] = new FrameNumber(as[0]);
        ao[1] = new FrameNumber(as[1]);
        return ao;
    }

    // =========================================================================


    protected final int _iIndex;

    protected final int _iStartSector;
    protected final int _iStartSectorDuplicateIndex;

    private final int _iHeaderFrameNumber;
    private final int _iHeaderDuplicateIndex;

    public FrameNumber(int iIndex, int iStartSector, int iStartSectorDuplicateIndex,
                       int iHeaderFrameNumber, int iHeaderDuplicateIndex)
    {
        _iIndex = iIndex;
        _iStartSector = iStartSector;
        _iStartSectorDuplicateIndex = iStartSectorDuplicateIndex;
        _iHeaderFrameNumber = iHeaderFrameNumber;
        _iHeaderDuplicateIndex = iHeaderDuplicateIndex;
    }

    /** Deserialize/parse a header number from a string. */
    public FrameNumber(@Nonnull String sSerialized) throws NotThisTypeException {
        String[] as = Misc.regex("^(\\d+)/"+SECTOR_PREFIX+"(\\d+)(\\.(\\d+))?(/"+HEADER_PREFIX+"(\\d+)(\\.(\\d+))?)?$", sSerialized);
        if (as == null)
            throw new NotThisTypeException(I.INVALID_FRAME_NUMBER(sSerialized));

        try {
            _iIndex = Integer.parseInt(as[1]);
            _iStartSector = Integer.parseInt(as[2]);
            _iStartSectorDuplicateIndex = Misc.parseIntOrDefault(as[4], 0);
            _iHeaderFrameNumber = Misc.parseIntOrDefault(as[6], -1);
            _iHeaderDuplicateIndex = Misc.parseIntOrDefault(as[8],
                                     _iHeaderFrameNumber >= 0 ? 0 : -1);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException(I.INVALID_FRAME_NUMBER(sSerialized));
        }
    }

    public int getIndex() {
        return _iIndex;
    }

    public @Nonnull String getIndexString() {
        return String.valueOf(_iIndex);
    }

    public int getSector() {
        return _iStartSector;
    }

    public int getSectorDuplicate() {
        return _iStartSectorDuplicateIndex;
    }

    public @Nonnull String getSectorString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_iStartSector);
        if (_iStartSectorDuplicateIndex > 0) {
            sb.append('.');
            sb.append(_iStartSectorDuplicateIndex);
        }
        return sb.toString();
    }

    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }

    public int getHeaderFrameDuplicate() {
        return _iHeaderDuplicateIndex;
    }

    public @Nonnull String getHeaderFrameString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_iHeaderFrameNumber);
        if (_iHeaderDuplicateIndex > 0) {
            sb.append('.');
            sb.append(_iHeaderDuplicateIndex);
        }
        return sb.toString();
    }

    /** Serialize.
     * <p>
     * Format: index,@sector[.dup][,#header[.dup]]
     */
    public @Nonnull String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(_iIndex);
        sb.append('/');
        sb.append(SECTOR_PREFIX);
        sb.append(getSectorString());
        sb.append('/');
        sb.append(HEADER_PREFIX);
        sb.append(getHeaderFrameString());
        return sb.toString();
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + _iIndex;
        hash = 71 * hash + _iStartSector;
        hash = 71 * hash + _iStartSectorDuplicateIndex;
        hash = 71 * hash + _iHeaderFrameNumber;
        hash = 71 * hash + _iHeaderDuplicateIndex;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        
        final FrameNumber o = (FrameNumber) obj;
        return _iIndex == o._iIndex &&
               _iStartSector == o._iStartSector &&
               _iStartSectorDuplicateIndex == o._iStartSectorDuplicateIndex &&
               _iHeaderFrameNumber == o._iHeaderFrameNumber &&
               _iHeaderDuplicateIndex == o._iHeaderDuplicateIndex;
    }
}
