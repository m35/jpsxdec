/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

import java.util.ArrayList;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;

/** @see SPacket */
public class PolicenautsSectorToPacket implements IdentifiedSectorListener<SectorPolicenauts> {

    public interface Listener {
        void videoStart(int iWidth, int iHeight, @Nonnull ILocalizedLogger log);
        void feedPacket(@Nonnull SPacketData packet, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endOfSectors(@Nonnull ILocalizedLogger log);
    }

    public static void attachToSectorClaimer(@Nonnull SectorClaimSystem scs, @Nonnull Listener listener) {
        PolicenautsSectorToPacket thisListener = scs.getIdListener(PolicenautsSectorToPacket.class);
        if (thisListener == null) {
            thisListener = new PolicenautsSectorToPacket(listener);
            thisListener.addListener(listener);
            scs.addIdListener(thisListener);
        } else {
            thisListener.addListener(listener);
        }
    }

    private final ArrayList<Listener> _listeners = new ArrayList<Listener>();

    private PolicenautsSectorToPacket(@Nonnull Listener listener) {
        addListener(listener);
    }

    private void addListener(@Nonnull Listener newListener) {
        for (Listener listener : _listeners) {
            if (listener == newListener)
                return; // already listening
        }
        _listeners.add(newListener);
    }

    @Override
    public @Nonnull Class<SectorPolicenauts> getListeningFor() {
        return SectorPolicenauts.class;
    }

    @Override
    public void feedSector(@Nonnull SectorPolicenauts pnSector, @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        if (pnSector instanceof SectorPN_VMNK) {
            SectorPN_VMNK vmnk = (SectorPN_VMNK) pnSector;
            for (Listener listener : _listeners) {
                listener.videoStart(vmnk.getWidth(), vmnk.getHeight(), log);
            }
        }

        for (SPacketData sPacketData : pnSector) {
            for (Listener listener : _listeners) {
                listener.feedPacket(sPacketData, log);
            }
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        for (Listener listener : _listeners) {
            listener.endOfSectors(log);
        }
    }
}
