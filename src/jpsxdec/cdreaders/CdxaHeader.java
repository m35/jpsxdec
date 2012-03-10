/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

    private final int _iMinutesBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final int _iSecondsBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final int _iSectorsBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final int _iMode;       // [1 byte] Should always be Mode 2 for PSX data tracks

    private final boolean _blnIsCdAudioSector;
    private final int _iSyncHeaderErrorCount;

    public CdxaHeader(byte[] abSectorData, int iStartOffset) {
        int iByteErrorCount = 0;
        for (int i = 0; i < SECTOR_SYNC_HEADER.length; i++) {
            if (abSectorData[iStartOffset + i] != SECTOR_SYNC_HEADER[i])
                iByteErrorCount++;
        }
        _iSyncHeaderErrorCount = iByteErrorCount;
        
        _iMinutesBCD = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 0] & 0xff;
        _iSecondsBCD = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 1] & 0xff;
        _iSectorsBCD = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 2] & 0xff;
        _iMode       = abSectorData[iStartOffset + SECTOR_SYNC_HEADER.length + 3] & 0xff;

        if (!isValidBinaryCodedDecimal(_iMinutesBCD))
            iByteErrorCount++;
        if (!isValidBinaryCodedDecimal(_iSecondsBCD))
            iByteErrorCount++;
        if (!isValidBinaryCodedDecimal(_iSectorsBCD))
            iByteErrorCount++;
        if (_iMode < 1 || _iMode > 2)
            iByteErrorCount++;

        // TODO: Figure out if this is usable fuzzy logic
        _blnIsCdAudioSector = (_iMode < 1 || _iMode > 2 || _iSyncHeaderErrorCount > 4);
    }

    public boolean isCdAudioSector() {
        return _blnIsCdAudioSector;
    }

    /** @return Sector number. If header is corrupted, 2.
     * @throws UnsupportedOperationException if CD sector.  */
    public int getMode() {
        if (_blnIsCdAudioSector)
            throw new UnsupportedOperationException("No Mode available for CD sectors.");
        
        if (_iMode < 1 || _iMode > 2)
            return 2;
        else
            return _iMode;
    }

    public int getSize() {
        return SIZE;
    }

    /** @return The sector number from the sector header, or -1 if header is corrupted.
     *  @throws UnsupportedOperationException if CD sector.  */
    public int calculateSectorNumber() {
        if (_blnIsCdAudioSector)
            throw new UnsupportedOperationException(
                    "Unable to calculate header sector number from CD sector.");
        
        if (isValidBinaryCodedDecimal(_iMinutesBCD) &&
            isValidBinaryCodedDecimal(_iSecondsBCD) &&
            isValidBinaryCodedDecimal(_iSectorsBCD))
        {
            return   binaryCodedDecimalToInt(_iMinutesBCD) * 60 * 75
                   + binaryCodedDecimalToInt(_iSecondsBCD) * 75
                   + binaryCodedDecimalToInt(_iSectorsBCD)
                   - 150;
        } else {
            return -1;
        }
    }

    private static boolean isValidBinaryCodedDecimal(int i) {
        return (i & 0xf)        <= 9 && 
               ((i >> 4) & 0xf) <= 9;
    }
    
    /** Converts Binary Coded Decimal (BCD) to integer. */
    private static int binaryCodedDecimalToInt(int i) {
        return ((i >> 4) & 0xf)*10 + (i & 0xf);
    }

    public String toString() {
        if (_blnIsCdAudioSector)
            return "CD sector";
        return String.format("Mode: %d Number: %02x' %02x\" %02xs = %d",
                _iMode, _iMinutesBCD, _iSecondsBCD, _iSectorsBCD, calculateSectorNumber());
    }

    void printErrors(int iSector, Logger logger) {
        if (_blnIsCdAudioSector)
            return;
        
        if (_iSyncHeaderErrorCount > 0)
            logger.warning("Sector " + iSector + " " + _iSyncHeaderErrorCount + " bytes in the sync header are corrupted");
        if (_iMode < 0 || _iMode > 2)
            logger.warning("Sector " + iSector + " Mode number is corrupted " + _iMode);
        if (!isValidBinaryCodedDecimal(_iMinutesBCD))
            logger.warning("Sector " + iSector + " Minutes number is corrupted " + String.format("%02x", _iMinutesBCD));
        if (!isValidBinaryCodedDecimal(_iSecondsBCD))
            logger.warning("Sector " + iSector + " Seconds number is corrupted " + String.format("%02x", _iSecondsBCD));
        if (!isValidBinaryCodedDecimal(_iSectorsBCD))
            logger.warning("Sector " + iSector + " Seconds number is corrupted " + String.format("%02x", _iSectorsBCD));
    }

    int getErrorCount() {
        return !_blnIsCdAudioSector && (_iMode < 0 || _iMode > 2) ? 1 : 0;
    }
}
