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

package jpsxdec.modules.eavideo;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;

public class EASectorToEAPacket implements IdentifiedSectorListener<IIdentifiedSector> {

    public static void attachToSectorClaimer(@Nonnull SectorClaimSystem scs, @Nonnull Listener listener) {
        EASectorToEAPacket s2p = scs.getIdListener(EASectorToEAPacket.class);
        if (s2p == null) {
            s2p = new EASectorToEAPacket(listener);
            scs.addIdListener(s2p);
        } else {
            s2p.addListener(listener);
        }
    }

    public interface Listener {
        void feedPacket(@Nonnull EAVideoPacketSectors packet, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endVideo(@Nonnull ILocalizedLogger log);
    }

    private final ArrayList<Listener> _listeners = new ArrayList<Listener>();

    private EASectorToEAPacket(@Nonnull Listener listener) {
        _listeners.add(listener);
    }

    private void addListener(@Nonnull Listener newListener) {
        for (Listener listener : _listeners) {
            if (listener == newListener)
                return; // already listening
        }
        _listeners.add(newListener);
    }

    @Override
    public @Nonnull Class<IIdentifiedSector> getListeningFor() {
        return IIdentifiedSector.class;
    }

    @Override
    public void feedSector(@Nonnull IIdentifiedSector idSector, @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        if (idSector instanceof SectorEAVideo) {
            SectorEAVideo eaSector = (SectorEAVideo) idSector;
            for (EAVideoPacketSectors finishedPacket : eaSector) {
                for (Listener listener : _listeners) {
                    listener.feedPacket(finishedPacket, log);
                }
            }
        } else {
            endVideo(log);
        }
    }

    public void endVideo(@Nonnull ILocalizedLogger log) {
        for (Listener listener : _listeners) {
            listener.endVideo(log);
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        endVideo(log);
    }

}
