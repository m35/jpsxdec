/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents a raw CD header with a sync header and sector header. */
public class CdxaHeader {

    public static final int SIZE = 16;

    // sync header [12 bytes]
    // [4 bytes] 0x00FFFFFF
    // [4 bytes] 0xFFFFFFFF
    // [4 bytes] 0xFFFFFF00

    /** Sync header. */
    public final static int[] SECTOR_SYNC_HEADER_INT = new int[]
                                {0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFF00};

    /** Sync header. */
    public final static byte[] SECTOR_SYNC_HEADER = {
        (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00
    };

    private final int _iMinutes;     // [1 byte] timecode relative to start of disk
    private final int _iSeconds;     // [1 byte] timecode relative to start of disk
    private final int _iSectors;     // [1 byte] timecode relative to start of disk
    private final int _iMode;        // [1 byte] Should always be Mode 2 for PSX data tracks
    /** Holds bit flags indicating which sync header bytes were wrong. */
    private final boolean _blnIsCdAudioSector;

    public CdxaHeader(byte[] abSectorData, int iStartOffset) {
        int iErrCount = 0;
        for (int i = 0; i < SECTOR_SYNC_HEADER.length; i++) {
            if (abSectorData[iStartOffset + i] != SECTOR_SYNC_HEADER[i])
                iErrCount++;
        }
        
        _iMinutes = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 0] & 0xff;
        _iSeconds = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 1] & 0xff;
        _iSectors = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 2] & 0xff;
        _iMode    = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 3] & 0xff;

        _blnIsCdAudioSector = (_iMode < 1 || _iMode > 2 || iErrCount > 4);
    }

    public boolean isCdAudioSector() {
        return _blnIsCdAudioSector;
    }

    public int getMode() {
        return _iMode;
    }

    public int getSize() {
        return SIZE;
    }

    public int calculateSectorNumber() {
        return   binaryCodedDecimalToInt(_iMinutes) * 60 * 75
               + binaryCodedDecimalToInt(_iSeconds) * 75
               + binaryCodedDecimalToInt(_iSectors)
               - 150;
    }

    /** Converts Binary Coded Decimal (BCD) to integer. */
    private static int binaryCodedDecimalToInt(int i) {
        return ((i >> 4) & 0xf)*10 + (i & 0xf);
    }

    public String toString() {
        return "Header sector:" + calculateSectorNumber();
    }

    void printErrors(int iSector, Logger logger) {
        if (!_blnIsCdAudioSector && (_iMode < 0 || _iMode > 2))
            logger.warning("Sector " + iSector + " Mode number is corrupted " + _iMode);
        // TODO: also report any sync header error
    }

    int getErrorCount() {
        return !_blnIsCdAudioSector && (_iMode < 0 || _iMode > 2) ? 1 : 0;
    }
}
