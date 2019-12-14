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

package jpsxdec.modules.eavideo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IOIterator;

public class SectorClaimToEAVideo extends SectorClaimSystem.SectorClaimer {

    private static final Logger LOG = Logger.getLogger(SectorClaimToEAVideo.class.getName());

    public interface Listener {
        void feedPacket(@Nonnull EAVideoPacketSectors packet, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endVideo(@Nonnull ILocalizedLogger log);
    }

    @CheckForNull
    private Listener _listener;

    public SectorClaimToEAVideo() {
    }
    public SectorClaimToEAVideo(@Nonnull Listener listener) {
        _listener = listener;
    }
    public void setListener(@CheckForNull Listener listener) {
        _listener = listener;
    }

    @CheckForNull
    private EAVideoStreamReader _sectorStream;

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

        try {

            SectorEAVideo rrSector = null;

            if (_sectorStream == null) {
                // No current movie
                long lngMagic = cdSector.readUInt32BE(0);
                if (lngMagic == EAVideoPacket.MAGIC_VLC0) {

                    // we've found a header, now make sure the whole VLC packet is valid
                    EAVideoPacket.VLC0 vlc;
                    try {
                        vlc = EAVideoPacket.readVlc0(cdSector.getCdUserDataStream());
                    } catch (IOException ex) {
                        throw new RuntimeException("Should not happen");
                    }
                    if (vlc != null) {
                        // new video
                        _sectorStream = new EAVideoStreamReader();
                        rrSector = _sectorStream.readSectorPackets(cdSector, EAVideoPacket.VLC0.SIZEOF, vlc);
                        // tell listener to end any existing videos
                        if (_listener != null)
                            _listener.endVideo(log);
                    }
                }

            } else {
                // add to existing stream
                rrSector = _sectorStream.readSectorPackets(cdSector, 0, null);
            }

            if (rrSector != null) {

                cs.claim(rrSector);

                for (EAVideoPacketSectors finishedPacket : rrSector) {
                    

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

                }
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
            _sectorStream = null;
            if (_listener != null)
                _listener.endVideo(log);
        }
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        endVideo(log);
    }
}
