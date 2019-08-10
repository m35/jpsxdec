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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.CdSectorDemuxPiece;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.DemuxPushInputStream;
import jpsxdec.util.DemuxPushInputStream.NeedsMoreData;
import jpsxdec.util.IO;
import jpsxdec.util.IOIterator;

public class SectorClaimToRoadRash extends SectorClaimSystem.SectorClaimer {

    private static final Logger LOG = Logger.getLogger(SectorClaimToRoadRash.class.getName());

    public interface Listener {
        void feedPacket(@Nonnull RoadRashPacketSectors packet, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endVideo(@Nonnull ILocalizedLogger log);
    }

    @CheckForNull
    private Listener _listener;

    public SectorClaimToRoadRash() {
    }
    public SectorClaimToRoadRash(@Nonnull Listener listener) {
        _listener = listener;
    }
    public void setListener(@CheckForNull Listener listener) {
        _listener = listener;
    }

    @CheckForNull
    private DemuxPushInputStream<CdSectorDemuxPiece> _sectorStream;
    private boolean _blnVidEnd = false;

    private @Nonnull List<RoadRashPacketSectors> readAsManyPacketsAsPossible() {
        List<RoadRashPacketSectors> finishedPackets = new ArrayList<RoadRashPacketSectors>();
        if (_sectorStream == null) throw new IllegalStateException();

        if (_sectorStream.available() < RoadRashPacket.MIN_PACKET_SIZE)
            return finishedPackets;

        while (true) {
            _sectorStream.mark(RoadRashPacket.MAX_PACKET_SIZE);
            int iStartSector = _sectorStream.getCurrentPiece().getSectorNumber();

            try {
                RoadRashPacket packet = RoadRashPacket.readPacket(_sectorStream);

                if (packet != null) {
                    int iEndSector = _sectorStream.getCurrentPiece().getSectorNumber();
                    finishedPackets.add(new RoadRashPacketSectors(packet, iStartSector, iEndSector));
                } else {

                    // possibly end of stream
                    _sectorStream.reset();

                    // check for 8 zeroes
                    int iIsZero = IO.readSInt32BE(_sectorStream);
                    if (iIsZero == 0) {
                        _sectorStream.mark(RoadRashPacket.MAX_PACKET_SIZE);
                        iIsZero = IO.readSInt32BE(_sectorStream);
                        if (iIsZero == 0) {
                            // 8 zeroes, definitely end of stream
                            _blnVidEnd = true;
                            return finishedPackets;
                        }

                        // so there were 4 zeroes, but now there is some non-zero data
                        // don't end the stream, but this is definitely data corruption
                        LOG.warning("Corrupted Road Rash stream");
                        _sectorStream.reset(); // do another loop
                    }
                }

            } catch (NeedsMoreData ex) {
                // backup and try again when there's more data
                _sectorStream.reset();
                return finishedPackets;
            } catch (EOFException ex) {
                // stream was closed and we hit the end
                return finishedPackets;
            } catch (IOException ex) {
                throw new RuntimeException("Should not happen");
            }

        }
    }

    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws SectorClaimSystem.ClaimerFailure
    {
        CdSector cdSector = cs.getSector();
        if (cs.getClaimer() != null || cdSector.isCdAudioSector()) {
            // close any existing stream
            endVideo(log);
            return;
        }

        int iStartEnd = 0;

        if (_sectorStream == null) {
            // No current movie

            long lngMagic = cdSector.readUInt32BE(0);
            if (lngMagic == RoadRashPacket.MAGIC_VLC0) {
                // Probably a movie

                DemuxPushInputStream<CdSectorDemuxPiece> stream = new DemuxPushInputStream<CdSectorDemuxPiece>(new CdSectorDemuxPiece(cdSector));
                RoadRashPacket firstPacket;
                try {
                    firstPacket = RoadRashPacket.readPacket(stream);
                } catch (IOException ex) {
                    throw new RuntimeException("Should not happen");
                }

                if (firstPacket instanceof RoadRashPacket.VLC0) {
                    // Definitely a movie
                    // send first packet to listener
                    _sectorStream = stream;
                    iStartEnd = SectorRoadRash.START;
                    if (_listener != null) {
                        try {
                            _listener.feedPacket(new RoadRashPacketSectors(firstPacket,
                                    cdSector.getSectorIndexFromStart(), cdSector.getSectorIndexFromStart()), log);
                        } catch (LoggedFailure ex) {
                            throw new SectorClaimSystem.ClaimerFailure(ex);
                        }
                    }
                }
            }

        } else {
            // add to existing stream
            _sectorStream.addPiece(new CdSectorDemuxPiece(cdSector));
        }

        if (_sectorStream != null) {

            List<RoadRashPacketSectors> finishedPackets = readAsManyPacketsAsPossible();

            List<RoadRashPacket> rrps = new ArrayList<RoadRashPacket>(finishedPackets.size());
            for (RoadRashPacketSectors finishedPacket : finishedPackets) {
                if (finishedPacket.packet instanceof RoadRashPacket.VLC0) {
                    throw new RuntimeException("Should not happen unless data is corrupted");
                }
                
                if (_listener != null) {
                    // Only send packets that are fully in the active sector range
                    // (in practice there should never be a packet crossing the border)
                    if (sectorIsInRange(finishedPacket.iStartSector) &&
                        sectorIsInRange(finishedPacket.iEndSector))
                    {
                        try {
                            _listener.feedPacket(finishedPacket, log);
                        } catch (LoggedFailure ex) {
                            throw new SectorClaimSystem.ClaimerFailure(ex);
                        }
                    }
                }
                
                rrps.add(finishedPacket.packet);
            }

            if (_blnVidEnd)
                iStartEnd |= SectorRoadRash.END;
            cs.claim(new SectorRoadRash(cdSector, rrps, iStartEnd));
            if (_blnVidEnd)
                endVideo(log);
        }
    }

    private void endVideo(@Nonnull ILocalizedLogger log) {
        if (_sectorStream != null) {
            _blnVidEnd = false;
            _sectorStream = null;
            if (_listener != null)
                _listener.endVideo(log);
        }
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        endVideo(log);
    }
}
