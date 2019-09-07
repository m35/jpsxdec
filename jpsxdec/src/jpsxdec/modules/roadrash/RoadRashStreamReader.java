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

package jpsxdec.modules.roadrash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.PushAvailableInputStream;

public class RoadRashStreamReader {

    @Nonnull
    private final PushAvailableInputStream<CdSector> _sectorStream = new PushAvailableInputStream<CdSector>();

    @CheckForNull
    private RoadRashPacket.Header _header;

    private int _iCurrentPacketStartSector;
    private boolean _blnEnd = false;

    public boolean isEnd() {
        return _blnEnd;
    }

    public @Nonnull SectorRoadRash readSectorPackets(@Nonnull CdSector sector, int iSkip, @CheckForNull RoadRashPacket.VLC0 vlc)
            throws BinaryDataNotRecognized
    {
        if (_blnEnd)
            throw new IllegalStateException();

        try {
            ByteArrayFPIS is = sector.getCdUserDataStream();
            if (iSkip > 0)
                is.skip(iSkip);

            _sectorStream.addStream(is, sector);

            return doReadSectorPackets(sector, vlc);
        } catch (IOException ex) {
            throw new RuntimeException("Should not happen", ex);
        }
    }

    private @Nonnull SectorRoadRash doReadSectorPackets(@Nonnull CdSector sector, @CheckForNull RoadRashPacket.VLC0 vlc)
            throws BinaryDataNotRecognized, IOException
    {

        List<RoadRashPacketSectors> finishedPackets = null;
        if (vlc != null) {
            finishedPackets = new ArrayList<RoadRashPacketSectors>(5);
            finishedPackets.add(new RoadRashPacketSectors(vlc, sector.getSectorIndexFromStart(), sector.getSectorIndexFromStart()));
        }

        while (true) {
            if (_header == null) {
                if (_sectorStream.available() < RoadRashPacket.Header.SIZEOF)
                    break;

                _iCurrentPacketStartSector = _sectorStream.getCurrentMeta().getSectorIndexFromStart();
                _header = RoadRashPacket.Header.read(_sectorStream);
            } else if (_header.isEndPacket()) {
                // end of stream
                _blnEnd = true;
                break;
            } else {
                if (_sectorStream.available() < _header.getPayloadSize())
                    break;

                int iBefore = _sectorStream.available();

                RoadRashPacket packet = _header.readPacket(_sectorStream);

                int iAfter = _sectorStream.available();

                if (_header.getPayloadSize() != iBefore - iAfter)
                    throw new RuntimeException();

                assert _sectorStream.getCurrentMeta() == sector;

                if (finishedPackets == null)
                    finishedPackets = new ArrayList<RoadRashPacketSectors>(5);

                int iPacketEndSector = _sectorStream.getCurrentMeta().getSectorIndexFromStart();
                finishedPackets.add(new RoadRashPacketSectors(packet, _iCurrentPacketStartSector, iPacketEndSector));

                _header = null;
            }
        }

        int iStartEnd = 0;
        if (vlc != null)
            iStartEnd = SectorRoadRash.START;
        if (_blnEnd)
            iStartEnd += SectorRoadRash.END;

        SectorRoadRash rrSector = new SectorRoadRash(sector, iStartEnd);
        if (finishedPackets != null)
            rrSector.setPacketsEndingInThisSector(finishedPackets);

        return rrSector;
    }

}
