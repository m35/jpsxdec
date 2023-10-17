/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.io.InputStream;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IdentifiedSector;


/** Audio/video sectors for Crusader: No Remorse and a few other Electronic Arts games. */
public class SectorCrusader extends IdentifiedSector {

    private static final long MAGIC = 0xAABBCCDDL;
    public static final int HEADER_SIZE = 8;
    private static final int MAX_CRUSADER_SECTOR = 15524;

    /** Size of Crusader user data (assuming nothing is wrong with the sector). */
    public static final int CRUSADER_IDENTIFIED_USER_DATA_SIZE =
            CdSector.SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1 - HEADER_SIZE;

    private int _iCrusaderSectorNumber;

    public SectorCrusader(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        long lngMagic = cdSector.readUInt32BE(0);
        if (lngMagic != MAGIC)
            return;

        _iCrusaderSectorNumber = cdSector.readSInt32BE(4);
        if (_iCrusaderSectorNumber < 0 ||
            _iCrusaderSectorNumber > MAX_CRUSADER_SECTOR)
            return;

        if (_iCrusaderSectorNumber == 0) {
            int iFirstPacketMagic = cdSector.readSInt32BE(HEADER_SIZE);
            switch (iFirstPacketMagic) {
                case CrusaderPacket.MDEC_MAGIC:
                case CrusaderPacket.ad20_MAGIC:
                case CrusaderPacket.ad21_MAGIC:
                    break;
                default:
                    return;
            }
        }

        if (cdSector.getCdUserDataSize() != CdSector.SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1)
            throw new RuntimeException("Crusader sector size isn't right");

        setProbability(100);
    }


    @Override
    public @Nonnull String getTypeName() {
        return "Crusader";
    }

    public int getCrusaderSectorNumber() {
        return _iCrusaderSectorNumber;
    }

    public int getIdentifiedUserDataSize() {
        return CRUSADER_IDENTIFIED_USER_DATA_SIZE;
    }

    public byte readIdentifiedUserDataByte(int i) {
        if (i < 0 || i >= getIdentifiedUserDataSize())
            throw new IllegalArgumentException("Offset out of sector range " + i);
        return super.getCdSector().readUserDataByte(HEADER_SIZE + i);
    }

    @Override
    public String toString() {
        return getTypeName() + " " + getCdSector() + " Sect:" + _iCrusaderSectorNumber;
    }

    public @Nonnull InputStream getCrusaderDataStream() {
        return getCdSector().getCdUserDataStream(HEADER_SIZE);
    }

}
