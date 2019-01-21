/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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

package jpsxdec.modules.xa;

import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.IOIterator;


public class SectorClaimToSectorXaAudio extends SectorClaimSystem.SectorClaimer {
    public interface Listener {
        void feedXaSector(@Nonnull CdSector cdSector,
                          @CheckForNull SectorXaAudio xaSector,
                          @Nonnull ILocalizedLogger log);
        void xaEof(int iChannel);

        public void endOfSectors(@Nonnull ILocalizedLogger log);
    }

    @CheckForNull
    private final ArrayList<Listener> _listeners = new ArrayList<Listener>();

    public SectorClaimToSectorXaAudio() {
    }
    public void addListener(@CheckForNull Listener listener) {
        _listeners.add(listener);
    }

    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws IOException
    {
        if (cs.isClaimed())
            return;
        CdSector cdSector = cs.getSector();
        SectorXaAudio xaSect = null;

        SectorXaNull nullSect = new SectorXaNull(cdSector);
        if (nullSect.getProbability() > 0) {
            cs.claim(nullSect);
        } else {
            SectorXaAudio possibleXa = new SectorXaAudio(cdSector);
            if (possibleXa.getProbability() > 0) {
                xaSect = possibleXa;
                cs.claim(possibleXa);
            }
        }

        if (sectorIsInRange(cs.getSector().getSectorIndexFromStart())) {
            for (Listener listener : _listeners) {
                listener.feedXaSector(cdSector, xaSect, log);
            }

            CdSectorXaSubHeader sh = cdSector.getSubHeader();
            if (sh != null && sh.getSubMode().getEofMarker() &&
                sh.getChannel() <= SectorXaAudio.MAX_VALID_CHANNEL)
            {
                // if the sector's EOF bit was set, this stream is closed
                // this is important for many games
                for (Listener listener : _listeners) {
                    listener.xaEof(sh.getChannel());
                }
            }
        }
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        for (Listener listener : _listeners) {
            listener.endOfSectors(log);
        }
    }
}
