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

package jpsxdec.modules.square;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorRange;


public class SquareAudioSectorToSquareAudioSectorPair implements IdentifiedSectorListener<ISquareAudioSector> {
    public interface Listener {
        void pairDone(@Nonnull SquareAudioSectorPair pair, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void endOfSectors(@Nonnull ILocalizedLogger log);
    }

    @Nonnull
    private final SectorRange _sectorRange;
    @Nonnull
    private final Listener _listener;

    @CheckForNull
    private ISquareAudioSector _leftAudioSector;

    public SquareAudioSectorToSquareAudioSectorPair(@Nonnull SectorRange sectorRange, @Nonnull Listener listener) {
        _sectorRange = sectorRange;
        _listener = listener;
    }

    @Override
    public @Nonnull Class<ISquareAudioSector> getListeningFor() {
        return ISquareAudioSector.class;
    }

    @Override
    public void feedSector(@Nonnull ISquareAudioSector audSector, @Nonnull ILocalizedLogger log) throws LoggedFailure {

        if (_leftAudioSector != null) {
            if (isPair(_leftAudioSector, audSector)) {
                _listener.pairDone(new SquareAudioSectorPair(
                                   _leftAudioSector, audSector,
                                   _leftAudioSector.getHeaderFrameNumber(),
                                   _leftAudioSector.getSampleFramesPerSecond(),
                                   _leftAudioSector.getSoundUnitCount(),
                                   _leftAudioSector.getSectorNumber(),
                                   audSector.getSectorNumber()),
                                   log); // both != null
                _leftAudioSector = null;
            } else {
                    leftOnlyDone(log);
                _leftAudioSector = audSector;
            }
        } else {
            if (audSector.isLeftChannel()) {
                _leftAudioSector = audSector;
            } else {
                _listener.pairDone(new SquareAudioSectorPair(
                                   null, audSector,
                                   audSector.getHeaderFrameNumber(),
                                   audSector.getSampleFramesPerSecond(),
                                   audSector.getSoundUnitCount(),
                                   audSector.getSectorNumber(),
                                   audSector.getSectorNumber()),
                                   log); // left == null
            }
        }
    }

    private void leftOnlyDone(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        _listener.pairDone(new SquareAudioSectorPair(
                           _leftAudioSector, null,
                           _leftAudioSector.getHeaderFrameNumber(),
                           _leftAudioSector.getSampleFramesPerSecond(),
                           _leftAudioSector.getSoundUnitCount(),
                           _leftAudioSector.getSectorNumber(),
                           _leftAudioSector.getSectorNumber()),
                           log); // right == null
    }

    private static boolean isPair(@Nonnull ISquareAudioSector left, @Nonnull ISquareAudioSector right) {
        return  left.getClass() == right.getClass() &&
                left.isLeftChannel() &&
               !right.isLeftChannel() &&
                left.getHeaderFrameNumber() == right.getHeaderFrameNumber() &&
                left.getSampleFramesPerSecond() == right.getSampleFramesPerSecond() &&
                left.getSoundUnitCount()== right.getSoundUnitCount()&&
                left.getSectorNumber() + 1 == right.getSectorNumber();
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_leftAudioSector != null)
            leftOnlyDone(log);
        _listener.endOfSectors(log);
        _leftAudioSector = null;
    }

}
