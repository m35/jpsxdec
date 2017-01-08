/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2017  Michael Sabin
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

package jpsxdec.sectors;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.CrusaderDemuxer;
import jpsxdec.util.ByteArrayFPIS;


/** Audio/video sectors for Crusader: No Remorse. */
public class SectorCrusader extends IdentifiedSector {

    private static final long MAGIC = 0xAABBCCDDL;
    public static final int HEADER_SIZE = 8;
    private static final int MAX_CRUSADER_SECTOR = 15524;
    
    /** Size of Crusader user data (assuming nothing is wrong with the sector). */
    public static final int CRUSADER_IDENTIFIED_USER_DATA_SIZE = 
            CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1 - HEADER_SIZE;
    
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

        if (super.getCdSector().getCdUserDataSize() != CdFileSectorReader.SECTOR_USER_DATA_SIZE_FORM1)
            throw new RuntimeException("Crusader sector size isn't right");
        
        setProbability(100);
    }

    
    public @Nonnull String getTypeName() {
        return "Crusader";
    }

    public int getCrusaderSectorNumber() {
        return _iCrusaderSectorNumber;
    }
    
    public int getIdentifiedUserDataSize() {
        return CRUSADER_IDENTIFIED_USER_DATA_SIZE;
    }

    public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(), 
                                 HEADER_SIZE, getIdentifiedUserDataSize());
    }
    
    public byte readIdentifiedUserDataByte(int i) {
        if (i < 0 || i >= getIdentifiedUserDataSize())
            throw new IllegalArgumentException("Offset out of sector range " + i);
        return super.getCdSector().readUserDataByte(HEADER_SIZE + i);
    }

    /** Copies the identified user data portion of the sector data to the
     *  output buffer. */
    public void copyIdentifiedUserData(int iSrcPos, @Nonnull byte[] abOut, int iOutPos, int iSize) {
        if (iSize < 0 || iSize > getIdentifiedUserDataSize())
            throw new IndexOutOfBoundsException();
        super.getCdSector().getCdUserDataCopy(HEADER_SIZE + iSrcPos, abOut,
                iOutPos, iSize);
    }

    
    public String toString() {
        return getTypeName() + " " + getCdSector().toString() + " Sect:" + _iCrusaderSectorNumber;
    }

    /** Used by {@link CrusaderDemuxer} to read demuxed chunk identifier (4 chars). */
    public @Nonnull String readMagic(int iIdentifiedUserDataOffset) {
        if (iIdentifiedUserDataOffset < 0 || iIdentifiedUserDataOffset > getIdentifiedUserDataSize()-4)
            throw new IndexOutOfBoundsException();
        byte[] abMagic = new byte[4];
        getCdSector().getCdUserDataCopy(HEADER_SIZE+iIdentifiedUserDataOffset, abMagic, 0, 4);
        return new String(abMagic);
    }

    public int readSInt32BE(int iIdentifiedUserDataOffset) {
        if (iIdentifiedUserDataOffset < 0 || iIdentifiedUserDataOffset > getIdentifiedUserDataSize()-4)
            throw new IndexOutOfBoundsException();
        return getCdSector().readSInt32BE(HEADER_SIZE + iIdentifiedUserDataOffset);
    }

    public int readSInt16BE(int iIdentifiedUserDataOffset) {
        if (iIdentifiedUserDataOffset < 0 || iIdentifiedUserDataOffset > getIdentifiedUserDataSize()-2)
            throw new IndexOutOfBoundsException();
        return getCdSector().readSInt16BE(HEADER_SIZE + iIdentifiedUserDataOffset);
    }

}
