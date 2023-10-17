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

package jpsxdec.modules.eavideo;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IOIterator;
import jpsxdec.util.PushAvailableInputStream;

public class SectorClaimToEAVideo implements SectorClaimSystem.SectorClaimer {

    private static class EAVideoStreamReader implements Closeable {

        @Nonnull
        private final PushAvailableInputStream<CdSector> _sectorStream = new PushAvailableInputStream<CdSector>();

        @CheckForNull
        private EAVideoPacket.Type _headerType;

        @CheckForNull
        private EAVideoPacket.Header _header;

        private int _iCurrentPacketStartSector;
        private boolean _blnEnd = false;

        public boolean isEnd() {
            return _blnEnd;
        }

        public @Nonnull SectorEAVideo readSectorPackets(@Nonnull CdSector sector, @CheckForNull EAVideoPacket.VLC0 vlc)
                throws BinaryDataNotRecognized
        {
            if (_blnEnd)
                throw new IllegalStateException();

            try {
                ByteArrayFPIS is = sector.getCdUserDataStream();
                if (vlc != null) {
                    is.skip(EAVideoPacket.VLC0.SIZEOF);
                }

                _sectorStream.addStream(is, sector);

                return readSectorPacketsThrowsIOEx(sector, vlc);
            } catch (IOException ex) {
                throw new RuntimeException("Should not happen", ex);
            }
        }

        private @Nonnull SectorEAVideo readSectorPacketsThrowsIOEx(@Nonnull CdSector sector, @CheckForNull EAVideoPacket.VLC0 vlc)
                throws BinaryDataNotRecognized, IOException
        {

            List<EAVideoPacketSectors> finishedPackets = null;
            if (vlc != null) {
                finishedPackets = new ArrayList<EAVideoPacketSectors>(5);
                finishedPackets.add(new EAVideoPacketSectors(vlc, sector.getSectorIndexFromStart(), sector.getSectorIndexFromStart()));
            }

            while (true) {

                if (_header == null) {

                    if (_headerType == null) {
                        // enough data to read the header type?
                        if (_sectorStream.available() < EAVideoPacket.Type.SIZEOF)
                            break;
                        _iCurrentPacketStartSector = _sectorStream.getCurrentMeta().getSectorIndexFromStart();
                        _headerType = EAVideoPacket.readHeaderType(_sectorStream);

                        // end of stream encountered?
                        if (_headerType == EAVideoPacket.Type.ZEROES) {
                            _blnEnd = true;
                            break;
                        }
                    } else {
                        // enough data to read the header size?
                        if (_sectorStream.available() < _headerType.bytesNeededToFinishHeader())
                            break;
                        _header = _headerType.readHeader(_sectorStream);
                        _headerType = null;
                    }
                } else {
                    // enough data to read the header payload?
                    if (_sectorStream.available() < _header.getPayloadSize())
                        break;

                    int iBefore = _sectorStream.available();

                    EAVideoPacket packet = _header.readPacket(_sectorStream);

                    int iAfter = _sectorStream.available();

                    if (_header.getPayloadSize() != iBefore - iAfter)
                        throw new RuntimeException();
                    _header = null;

                    assert _sectorStream.getCurrentMeta() == sector;

                    if (finishedPackets == null)
                        finishedPackets = new ArrayList<EAVideoPacketSectors>(5);

                    int iPacketEndSector = _sectorStream.getCurrentMeta().getSectorIndexFromStart();
                    finishedPackets.add(new EAVideoPacketSectors(packet, _iCurrentPacketStartSector, iPacketEndSector));

                }
            }

            int iStartEnd = 0;
            if (vlc != null)
                iStartEnd = SectorEAVideo.START;
            if (_blnEnd)
                iStartEnd += SectorEAVideo.END;

            SectorEAVideo rrSector = new SectorEAVideo(sector, iStartEnd);
            if (finishedPackets != null)
                rrSector.setPacketsEndingInThisSector(finishedPackets);

            return rrSector;
        }

        @Override
        public void close() throws IOException {
            _sectorStream.close();
        }

    }


    @CheckForNull
    private EAVideoStreamReader _sectorStream;

    @Override
    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
    {
        CdSector cdSector = cs.getSector();
        if (cs.isClaimed() || cdSector.isCdAudioSector()) {
            // close any existing stream
            endVideo(log);
            return;
        }

        try {

            SectorEAVideo eaSector = null;

            if (_sectorStream == null) {
                // No current movie
                long lngMagic = cdSector.readUInt32BE(0);
                if (lngMagic == EAVideoPacket.MAGIC_VLC0) {

                    // we've found a header, now make sure the whole VLC packet is valid
                    EAVideoPacket.VLC0 vlcPacket;
                    try {
                        vlcPacket = EAVideoPacket.readVlc0(cdSector.getCdUserDataStream());
                    } catch (IOException ex) {
                        throw new RuntimeException("Should not happen", ex);
                    }
                    if (vlcPacket != null) {
                        // new video
                        _sectorStream = new EAVideoStreamReader();
                        eaSector = _sectorStream.readSectorPackets(cdSector, vlcPacket);
                    }
                }

            } else {
                // add to existing stream
                eaSector = _sectorStream.readSectorPackets(cdSector, null);
            }

            if (eaSector != null) {
                cs.claim(eaSector);
            }

            if (_sectorStream != null && _sectorStream.isEnd()) {
                endVideo(log);
            }
        } catch (BinaryDataNotRecognized ex) {
            log.log(Level.SEVERE, I.EA_VIDEO_DATA_CORRUPTION(), ex);
            _sectorStream = null;
        }
    }

    private void endVideo(@Nonnull ILocalizedLogger log) {
        if (_sectorStream != null) {
            try {
                _sectorStream.close();
            } catch (IOException ex) {
                throw new RuntimeException("Shouldn't happen", ex);
            }
            _sectorStream = null;
        }
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        endVideo(log);
    }

}
