/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
    
    static enum Type {
        CD_AUDIO, MODE1, MODE2
    }
    
    //..........................................................................

    private final int _iSyncHeaderErrorCount;

    private final int _iMinutesBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final boolean _blnMinutesBCD_ok;
    private final int _iSecondsBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final boolean _blnSecondsBCD_ok;
    private final int _iSectorsBCD; // [1 byte] timecode relative to start of disk as Binary Coded Decimal
    private final boolean _blnSectorsBCD_ok;
    private final int _iMode;       // [1 byte] PSX discs should always be Mode 2
    private final boolean _blnMode_ok;
    
    private final int _iByteErrorCount;
    
    private final Type _eType;

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

        if (!(_blnMinutesBCD_ok = isValidBinaryCodedDecimal(_iMinutesBCD)))
            iByteErrorCount++;
        if (!(_blnSecondsBCD_ok = isValidBinaryCodedDecimal(_iSecondsBCD)))
            iByteErrorCount++;
        if (!(_blnSectorsBCD_ok = isValidBinaryCodedDecimal(_iSectorsBCD)))
            iByteErrorCount++;
        if (!(_blnMode_ok = (_iMode >= 1 && _iMode <= 2)))
            iByteErrorCount++;
        
        _iByteErrorCount = iByteErrorCount;

        // TODO: Figure out if this is usable fuzzy logic
        boolean blnIsCdAudioSector = (!_blnMode_ok || _iSyncHeaderErrorCount > 4);
        if (blnIsCdAudioSector)
            _eType = Type.CD_AUDIO;
        else
            _eType = _iMode == 1 ? Type.MODE1 : Type.MODE2;
    }

    Type getType() {
        return _eType;
    }

    public int getSize() {
        return SIZE;
    }

    /** @return The sector number from the sector header, or -1 if header is corrupted.
     *  @throws UnsupportedOperationException if CD sector.  */
    public int calculateSectorNumber() {
        if (_eType == Type.CD_AUDIO)
            throw new UnsupportedOperationException(
                    "Unable to calculate header sector number from CD sector.");
        
        if (_blnMinutesBCD_ok && _blnSecondsBCD_ok && _blnSectorsBCD_ok) {
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
        if (_eType == Type.CD_AUDIO)
            return "CD sector";
        return String.format("Mode: %d Number: %02x' %02x\" %02xs = %d",
                _iMode, _iMinutesBCD, _iSecondsBCD, _iSectorsBCD, calculateSectorNumber());
    }

    void printErrors(int iSector, Logger logger) {
        if (_eType == Type.CD_AUDIO)
            return;
        
        if (_iSyncHeaderErrorCount > 0)
            logger.log(Level.WARNING, "Sector {0} {1} bytes in the sync header are corrupted", new Object[]{iSector, _iSyncHeaderErrorCount});
        if (!_blnMode_ok)
            logger.log(Level.WARNING, "Sector {0} Mode number is corrupted {1}", new Object[]{iSector, _iMode});
        if (!_blnMinutesBCD_ok)
            logger.log(Level.WARNING, "Sector {0} Minutes number is corrupted {1}", new Object[]{iSector, String.format("%02x", _iMinutesBCD)});
        if (!_blnSecondsBCD_ok)
            logger.log(Level.WARNING, "Sector {0} Seconds number is corrupted {1}", new Object[]{iSector, String.format("%02x", _iSecondsBCD)});
        if (!_blnSectorsBCD_ok)
            logger.log(Level.WARNING, "Sector {0} Seconds number is corrupted {1}", new Object[]{iSector, String.format("%02x", _iSectorsBCD)});
    }

    int getErrorCount() {
        return _iByteErrorCount;
    }
}
