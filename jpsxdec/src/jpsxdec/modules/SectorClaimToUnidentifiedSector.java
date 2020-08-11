/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2020  Michael Sabin
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

package jpsxdec.modules;

import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.util.IOIterator;

/** Watches for sequences of sectors that are not identified, and are not CD audio. */
public class SectorClaimToUnidentifiedSector extends SectorClaimSystem.SectorClaimer {

    public interface Listener {
        void feedSector(@Nonnull CdSector sector);
        void endOfUnidentified();
    }

    private final ArrayList<Listener> _listeners = new ArrayList<Listener>();

    private boolean _blnInUnidentified = false;

    public SectorClaimToUnidentifiedSector() {
    }
    public SectorClaimToUnidentifiedSector(@Nonnull Listener listener) {
        _listeners.add(listener);
    }
    public void addListener(@Nonnull Listener listener) {
        _listeners.add(listener);
    }
    public void removeListener(@Nonnull Listener listener) {
        _listeners.remove(listener);
    }

    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws IOException
    {
        CdSector cdSector = cs.getSector();
        IIdentifiedSector idSector = cs.getClaimer();

        if (idSector != null || cdSector.isCdAudioSector() || !sectorIsInRange(cdSector.getSectorIndexFromStart())) {
            if (_blnInUnidentified) {
                for (Listener listener : _listeners) {
                    listener.endOfUnidentified();
                }
            }
        } else {
            for (Listener listener : _listeners) {
                listener.feedSector(cdSector);
            }
            _blnInUnidentified = true;
        }
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        for (Listener listener : _listeners) {
            listener.endOfUnidentified();
        }
    }
}
