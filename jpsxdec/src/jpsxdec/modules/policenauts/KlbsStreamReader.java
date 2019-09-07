/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.PushAvailableInputStream;


public class KlbsStreamReader {

    private static final int READ_HEADER = 1;
    private static final int READ_PACKET = 2;
    private static final int DONE = 3;

    private final int _iEntryCount;
    private final int _iKlbsStartSector;
    private final int _iKlbsEndSectorInclusive;

    @Nonnull
    private final PushAvailableInputStream<SectorPolicenauts> _sectorStream = new PushAvailableInputStream<SectorPolicenauts>();

    private List<SPacketPos> _sPackets;
    private int _iNextRequiredBytes = 0;
    private int _iState = READ_HEADER;
    private int _iPacketDataRead = 0;

    public KlbsStreamReader(@Nonnull SectorPN_KLBS klbsSector) {
        _iEntryCount = klbsSector.getEntryCount();
        _iKlbsStartSector = klbsSector.getSectorNumber();
        _iKlbsEndSectorInclusive = klbsSector.getEndSectorInclusive();

        _iNextRequiredBytes = _iEntryCount * SPacket.SIZEOF;
    }

    public boolean allRead() {
        return _iState == DONE;
    }

    public List<SPacketData> readSectorPackets(@Nonnull SectorPolicenauts sector, int iSkip)
            throws BinaryDataNotRecognized
    {
        try {
            ByteArrayFPIS is = sector.getCdSector().getCdUserDataStream();
            if (iSkip > 0)
                is.skip(iSkip);

            _sectorStream.addStream(is, sector);
            return doReadAllAvailablePackets(sector);
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    private @Nonnull List<SPacketData> doReadAllAvailablePackets(@Nonnull SectorPolicenauts sector)
            throws BinaryDataNotRecognized, IOException
    {

        List<SPacketData> finishedPackets = null;

        while (_sectorStream.available() >= _iNextRequiredBytes && _iState != DONE) {
            switch (_iState) {
                case READ_HEADER:
                    _sPackets = SPacketPos.readPackets(_sectorStream, _iEntryCount, _iKlbsStartSector, _iKlbsEndSectorInclusive);
                    _iNextRequiredBytes = _sPackets.get(0).getSize();
                    _iState = READ_PACKET;
                    break;

                case READ_PACKET:
                    SPacketPos packetPos = _sPackets.get(_iPacketDataRead);
                    skipZeroes(packetPos.getPaddingBeforeThisPacket());

                    SPacketData packetData = packetPos.read(_sectorStream);
                    assert _sectorStream.getCurrentMeta() == sector;
                    if (finishedPackets == null)
                        finishedPackets = new ArrayList<SPacketData>(3);
                    finishedPackets.add(packetData);

                    _iPacketDataRead++;
                    if (_iPacketDataRead >= _sPackets.size()) {
                        _iState = DONE;
                    } else {
                        SPacketPos nextPacket = _sPackets.get(_iPacketDataRead);
                        _iNextRequiredBytes = nextPacket.getPaddingBeforeThisPacket() + nextPacket.getSize();
                    }
                    break;
            }
        }

        if (finishedPackets == null)
            return Collections.emptyList();

        return finishedPackets;
    }

    private void skipZeroes(int iCount) throws IOException, BinaryDataNotRecognized {
        while (iCount > 0) {
            int iByte = _sectorStream.read();
            if (iByte != 0) {
                throw new BinaryDataNotRecognized("Expected 0 in sector %s but got %d",
                                                  _sectorStream.getCurrentMeta(),  iByte);
            }
            iCount--;
        }
    }

}
