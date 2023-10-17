/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.policenauts;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;

/** Wraps a {@link SPacket} with information about its position of the data
 * on the disc. */
public class SPacketPos {

    private static final Logger LOG = Logger.getLogger(SPacketPos.class.getName());

    private static final int TIME_STAMP_OUT_OF_ORDER_LEEWAY = 15;

    public static @Nonnull List<SPacketPos> readPackets(@Nonnull InputStream is, int iEntryCount,
                                                        int iKlbsStartSector, int iKlbsEndSectorInclusive)
            throws IOException, BinaryDataNotRecognized
    {
        ArrayList<SPacketPos> sPackets = new ArrayList<SPacketPos>();
        for (int i = 0; i < iEntryCount; i++) {
            SPacket packet = new SPacket(is);

            SPacketPos packetPos = new SPacketPos(packet, iKlbsStartSector, iKlbsEndSectorInclusive, i);
            if (i > 0) {
                SPacketPos prev = sPackets.get(i - 1);
                prev.checkAgainstNextPacket(packetPos);
            }
            sPackets.add(packetPos);
        }
        return sPackets;
    }


    @Nonnull
    private final SPacket _sPacket;

    private final int _iKlbsStartSectorNum;
    private final int _iKlbsEndSectorNumInclusive;
    private final int _iIndexInKlbs;
    private final int _iStartSector;
    private final int _iStartSectorOfs;

    /** 1 FMV has an incorrect size, so we'll correct it later. */
    private int _iCorrectedSize;

    private int _iPaddingBeforeThisPacket = 0;
    private int _iPaddingAfterThisPacket = 0;

    public SPacketPos(@Nonnull SPacket sPacket, int iKlbsStartSectorNum, int iKlbsEndSectorNumInclusive, int iIndexInKlbs) {
        _sPacket = sPacket;
        _iKlbsStartSectorNum = iKlbsStartSectorNum;
        _iKlbsEndSectorNumInclusive = iKlbsEndSectorNumInclusive;
        _iIndexInKlbs = iIndexInKlbs;
        _iStartSector = iKlbsStartSectorNum + (sPacket.getOffset() / CdSector.SECTOR_SIZE_2048_ISO);
        _iStartSectorOfs = sPacket.getOffset() % CdSector.SECTOR_SIZE_2048_ISO;

        _iCorrectedSize = sPacket.getSize();
    }

    public int getSize() {
        return _iCorrectedSize;
    }

    public int getPaddingBeforeThisPacket() {
        return _iPaddingBeforeThisPacket;
    }

    public int getTimestamp() {
        return _sPacket.getTimestamp();
    }

    public int getDuration() {
        return _sPacket.getDuration();
    }

    public boolean isVideo() {
        return _sPacket.getType() == SPacket.Type.SCIPPDTS;
    }

    public boolean isAudio() {
        return _sPacket.getType() == SPacket.Type.SDNSSDTS;
    }

    public int getKlbsStartSectorNum() {
        return _iKlbsStartSectorNum;
    }

    public int getKlbsEndSectorNum() {
        return _iKlbsEndSectorNumInclusive;
    }

    public void checkAgainstNextPacket(@Nonnull SPacketPos nextPacket) throws BinaryDataNotRecognized  {
        if (nextPacket.getPacket().getOffset() < _sPacket.getOffset())
            throw new BinaryDataNotRecognized();

        // Some timestamps can be a little before the next one
        if (nextPacket.getPacket().getTimestamp() < _sPacket.getTimestamp()) {
            LOG.log(Level.INFO, "{0} < {1}", new Object[]{nextPacket, _sPacket});
            if (nextPacket.getPacket().getTimestamp() + TIME_STAMP_OUT_OF_ORDER_LEEWAY < _sPacket.getTimestamp()) {
                throw new BinaryDataNotRecognized("Next packet timestamp %i < current packet timestamp %i",
                                                  nextPacket.getPacket().getTimestamp(), _sPacket.getTimestamp());
            }
        }

        int iBytesBetweenPacketStarts = nextPacket._sPacket.getOffset() - _sPacket.getOffset();
        if (_sPacket.getSize() > iBytesBetweenPacketStarts) {
            LOG.log(Level.WARNING, "{0} packet size exceeds room {1,number,#} (expected for MV000003.MOV on disc 2)",
                                   new Object[]{this, iBytesBetweenPacketStarts});
            if (isAudio() && !(iBytesBetweenPacketStarts % SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT == 0))
                throw new BinaryDataNotRecognized();
            _iCorrectedSize = iBytesBetweenPacketStarts;
        }
        _iPaddingAfterThisPacket = iBytesBetweenPacketStarts - _iCorrectedSize;
        nextPacket._iPaddingBeforeThisPacket = _iPaddingAfterThisPacket;
    }

    /** Reads the packet data and returns a {@link SPacketData}. */
    public @Nonnull SPacketData read(@Nonnull InputStream is) throws EOFException, IOException {
        byte[] abData = IO.readByteArray(is, _iCorrectedSize);
        return new SPacketData(this, abData);
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSectorInclusive() {
        int iSector = _iStartSector + (_iStartSectorOfs + _iCorrectedSize) / CdSector.SECTOR_SIZE_2048_ISO;
        assert iSector == _iKlbsStartSectorNum + (_sPacket.getOffset() + _iCorrectedSize) / CdSector.SECTOR_SIZE_2048_ISO;
        if (((_iStartSectorOfs + _iCorrectedSize) % CdSector.SECTOR_SIZE_2048_ISO) == 0)
            return iSector - 1;
        return iSector;
    }

    /** The packet data is padded to a 4 byte boundary, so there is often
     * a few bytes between packets */
    public int bytesToThisPacket(int iFromSector, int iFromSectorOffset) {
        int iSectorDiff = _iStartSector - iFromSector;
        int iOfsDiff = _iStartSectorOfs - iFromSectorOffset;
        int i = iSectorDiff * CdSector.SECTOR_SIZE_2048_ISO + iOfsDiff;
        return i;
    }

    public @Nonnull SPacket getPacket() {
        return _sPacket;
    }

    @Override
    public String toString() {
        return String.format("Start %d [%d] sector.ofs %d.%d %s",
                             _iKlbsStartSectorNum, _iIndexInKlbs, _iStartSector, _iStartSectorOfs, _sPacket);
    }

}
