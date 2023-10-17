/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.cdreaders;

import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.util.Misc;


public class SerializedDisc {

    public final static String SERIALIZATION_START = "Filename:";

    @Nonnull
    private final String _sFilename;
    private final int _iSectorSize;
    private final int _iSectorCount;
    private final int _iStartOffset;

    public SerializedDisc(@Nonnull String sFilename, int iSectorSize, int iSectorCount, int iStartOffset) {
        _sFilename = sFilename;
        _iSectorSize = iSectorSize;
        _iSectorCount = iSectorCount;
        _iStartOffset = iStartOffset;
    }

    public SerializedDisc(@Nonnull String sSerialization) throws LocalizedDeserializationFail {
        String[] asValues = Misc.regex(SERIALIZATION_START + "([^|]+)\\|Sector size:(\\d+)\\|Sector count:(\\d+)\\|First sector offset:(\\d+)$",
                                       sSerialization);
        if (asValues == null || asValues.length != 5)
            throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization));

        try {
            _iSectorSize = Integer.parseInt(asValues[2]);
            _iSectorCount = Integer.parseInt(asValues[3]);
            _iStartOffset = Integer.parseInt(asValues[4]);
        } catch (NumberFormatException ex) {
            throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization), ex);
        }

        switch (_iSectorSize) {
            case CdSector.SECTOR_SIZE_2048_ISO:
            case CdSector.SECTOR_SIZE_2336_BIN_NOSYNC:
            case CdSector.SECTOR_SIZE_2352_BIN:
            case CdSector.SECTOR_SIZE_2448_BIN_SUBCHANNEL:
                break;
            default:
                throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization));
        }

        if (_iSectorCount < 1 || (((long)_iSectorCount + 1) * _iSectorSize + _iStartOffset) > Integer.MAX_VALUE)
            throw new LocalizedDeserializationFail(I.CD_DESERIALIZE_FAIL(sSerialization));

        _sFilename = asValues[1];
    }

    public @Nonnull String serializeString() {
        return String.format(SERIALIZATION_START + "%s|Sector size:%d|Sector count:%d|First sector offset:%d",
                _sFilename,
                _iSectorSize,
                _iSectorCount,
                _iStartOffset);
    }

    public boolean matches(@Nonnull String sSerialization) {
        SerializedDisc other;
        try {
            other = new SerializedDisc(sSerialization);
        } catch (LocalizedDeserializationFail ex) {
            return false;
        }

        return _sFilename.equals(other._sFilename) &&
               _iSectorSize == other._iSectorSize &&
               _iSectorCount == other._iSectorCount &&
               _iStartOffset == other._iStartOffset;
    }

    public @Nonnull String getFilename() {
        return _sFilename;
    }

    public int getSectorSize() {
        return _iSectorSize;
    }

    public int getSectorCount() {
        return _iSectorCount;
    }

    public int getStartOffset() {
        return _iStartOffset;
    }

    @Override
    public String toString() {
        return serializeString();
    }
}
