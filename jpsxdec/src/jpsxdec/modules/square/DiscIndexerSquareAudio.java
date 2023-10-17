/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;

/** Watches for Square's unique audio format streams.
 * All known games that use Square audio run their streaming media at 2x
 * disc speed. */
public class DiscIndexerSquareAudio extends DiscIndexer
        implements SquareAudioSectorToSquareAudioSectorPair.Listener
{

    private static class AudioBuilder {
        private final int _iSampleFramesPerSecond;
        private final int _iStartSector;
        private int _iEndSector;
        private int _iSoundUnitCount;

        @Nonnull
        private SquareAudioSectorPair _prevPair;
        private int _iMinimumSectorsBetweenPair = -1;

        public AudioBuilder(@Nonnull SquareAudioSectorPair firstPair) {
            _prevPair = firstPair;
            _iSampleFramesPerSecond = firstPair.getSampleFramesPerSecond();
            _iStartSector = firstPair.getStartSector();
            _iEndSector = firstPair.getEndSector();
            _iSoundUnitCount = firstPair.getSoundUnitCount();
        }

        public boolean pairDone(@Nonnull SquareAudioSectorPair pair) {
            if (!pair.matchesPrevious(_prevPair))
                return false;

            _iSoundUnitCount += pair.getSoundUnitCount();
            _iEndSector = pair.getEndSector();
            int iSectorsBetweenPair = pair.getStartSector() - _prevPair.getEndSector();
            if (_iMinimumSectorsBetweenPair < 0)
                _iMinimumSectorsBetweenPair = iSectorsBetweenPair;
            else
                _iMinimumSectorsBetweenPair = Math.min(_iMinimumSectorsBetweenPair, iSectorsBetweenPair);
            _prevPair = pair;
            return true;
        }

        public @Nonnull DiscItemSquareAudioStream makeStream(@Nonnull ICdSectorReader cd) {

            int iSectorsPastEnd = 1;
            if (_iMinimumSectorsBetweenPair >= 0)
                iSectorsPastEnd = _iMinimumSectorsBetweenPair;

            return new DiscItemSquareAudioStream(cd,
                    _iStartSector, _iEndSector,
                    _iSoundUnitCount, _iSampleFramesPerSecond,
                    iSectorsPastEnd);
        }

    }

    @Nonnull
    private final ILocalizedLogger _errLog;
    private final SquareAudioSectorToSquareAudioSectorPair _sas2sasp =
            new SquareAudioSectorToSquareAudioSectorPair(SectorRange.ALL, this);
    @CheckForNull
    private AudioBuilder _audBldr;

    public DiscIndexerSquareAudio(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemSquareAudioStream.TYPE_ID.equals(fields.getType()))
            return new DiscItemSquareAudioStream(getCd(), fields);
        return null;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(_sas2sasp);
    }


    @Override
    public void pairDone(@Nonnull SquareAudioSectorPair pair,
                         @Nonnull ILocalizedLogger log)
    {
        // FF9 has some movies without audio, so the audio sectors have no data
        // ignore those sectors
        if (pair.getSoundUnitCount() == 0)
            return;

        if (_audBldr != null) {
            boolean ok = _audBldr.pairDone(pair);
            if (!ok) {
                addDiscItem(_audBldr.makeStream(getCd()));
                _audBldr = null;
            }
        }
        if (_audBldr == null) {
            _audBldr = new AudioBuilder(pair);
        }
    }
    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        if (_audBldr != null) {
            // submit the in-progress audio stream
            addDiscItem(_audBldr.makeStream(getCd()));
            _audBldr = null;
        }
    }


    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }
    @Override
    public boolean filterChild(DiscItem parent, DiscItem child) {
        return false;
    }
    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
