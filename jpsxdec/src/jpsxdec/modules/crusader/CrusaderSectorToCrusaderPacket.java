/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorRange;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.PushAvailableInputStream;


public class CrusaderSectorToCrusaderPacket implements IdentifiedSectorListener<SectorCrusader> {

    public interface PacketListener {
        void packetComplete(@Nonnull CrusaderPacket packet,
                            @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endOfVideo(@Nonnull ILocalizedLogger log);
    }

    @CheckForNull
    private PushAvailableInputStream<SectorCrusader> _stream = new PushAvailableInputStream<SectorCrusader>();

    @CheckForNull
    private PacketListener _packetListener;

    @Nonnull
    private final SectorRange _sectorRange;

    private int _iPrevCrusaderSector = -1;

    @CheckForNull
    private CrusaderPacket.HeaderType _nextPacketType;

    public CrusaderSectorToCrusaderPacket(@Nonnull SectorRange sectorRange, @Nonnull PacketListener listener) {
        _sectorRange = sectorRange;
        _packetListener = listener;
    }
    public void setListener(@CheckForNull PacketListener listener) {
        _packetListener = listener;
    }

    @Override
    public @Nonnull Class<SectorCrusader> getListeningFor() {
        return SectorCrusader.class;
    }

    @Override
    public void feedSector(@Nonnull SectorCrusader idSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {
        try {
            if (!_sectorRange.sectorIsInRange(idSector.getSectorNumber()))
                return;

            if (idSector.getCrusaderSectorNumber() != _iPrevCrusaderSector + 1) {
                if (_packetListener != null)
                    _packetListener.endOfVideo(log);
                _iPrevCrusaderSector = -1;
                _stream = new PushAvailableInputStream<SectorCrusader>();
            }

            if (_stream != null) {
                _stream.addStream(idSector.getCrusaderDataStream(), idSector);
                readPackets(log);
            }
            _iPrevCrusaderSector = idSector.getCrusaderSectorNumber();
        } catch (IOException ex) {
            throw new RuntimeException("Shouldn't happen", ex);
        } catch (BinaryDataNotRecognized ex) {
            // data corruption
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_packetListener != null)
            _packetListener.endOfVideo(log);
    }

    private void readPackets(@Nonnull ILocalizedLogger log) throws IOException, BinaryDataNotRecognized, LoggedFailure {
        while (true) {
            if (_nextPacketType == null) {
                if (_stream.available() < CrusaderPacket.HeaderType.SIZEOF)
                    return;

                _nextPacketType = CrusaderPacket.HeaderType.read(_stream);
                if (_nextPacketType == null) {
                    // either no more packets, or corruption
                    // either way, end the stream
                    _stream = null;
                    return;
                }

            } else {
                if (_stream.available() < _nextPacketType.getRemainingPacketSize())
                    return;

                int iStartSector = _stream.getCurrentMeta().getSectorNumber();
                CrusaderPacket packet = _nextPacketType.readPacket(_stream);
                int iEndSector = _stream.getCurrentMeta().getSectorNumber();
                packet.setSectors(iStartSector, iEndSector);
                _nextPacketType = null;
                if (_packetListener != null)
                    _packetListener.packetComplete(packet, log);
            }
        }
    }

}
