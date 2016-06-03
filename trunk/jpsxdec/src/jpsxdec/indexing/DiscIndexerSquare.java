/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

package jpsxdec.indexing;

import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSquareAudioStream;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.ISquareAudioSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;

/** Watches for Square's unique audio format streams. */
public class DiscIndexerSquare extends DiscIndexer implements DiscIndexer.Identified {

    private int _iAudioStartSector;
    private long _lngAudioLeftSampleCount;
    private long _lngAudioRightSampleCount;

    @CheckForNull
    private ISquareAudioSector _prevAudioSect;

    private int _iPrevLeftAudioSectorNum = -1;
    /** -1 indicates no period has been calculated yet. */
    private int _iLeftAudioPeriod = -1;

    @Nonnull
    private Logger _errLog;

    public DiscIndexerSquare(@Nonnull Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) {
        try {
            if (DiscItemSquareAudioStream.TYPE_ID.equals(fields.getType())) {
                return new DiscItemSquareAudioStream(getCd(), fields);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    /**
     * All known games that use Square audio run their streaming media at 2x
     * speed.
     */
    public void indexingSectorRead(@Nonnull CdSector cdSector,
                                   @CheckForNull IdentifiedSector idSector)
    {
        if (!(idSector instanceof ISquareAudioSector))
            return;

        ISquareAudioSector audioSect = (ISquareAudioSector)idSector;

        // FF9 has some movies without audio
        if (audioSect.getLeftSampleCount() == 0 && audioSect.getRightSampleCount() == 0)
            return;

        boolean blnNewVid = false;
        if (audioSect.getAudioChannel() == 0) {
            if (_iPrevLeftAudioSectorNum >= 0) {
                int iPeriod = audioSect.getSectorNumber() - _iPrevLeftAudioSectorNum;
                if (_iLeftAudioPeriod < 0)
                    _iLeftAudioPeriod = iPeriod;
                else if (_iLeftAudioPeriod != iPeriod)
                    blnNewVid = true;
            }
            _iPrevLeftAudioSectorNum = audioSect.getSectorNumber();
        }

        if (blnNewVid || _prevAudioSect == null || !audioSect.matchesPrevious(_prevAudioSect))
        {
            if (_prevAudioSect != null) {
                // submit the audio stream that just ended
                super.addDiscItem(new DiscItemSquareAudioStream(
                    getCd(),
                    _iAudioStartSector, _prevAudioSect.getSectorNumber(),
                    _lngAudioLeftSampleCount, _lngAudioRightSampleCount,
                    _prevAudioSect.getSamplesPerSecond(), _iLeftAudioPeriod - 2));
            }
            // reset the current audio stream being tracked
            _iAudioStartSector = audioSect.getSectorNumber();
            _lngAudioLeftSampleCount = 0;
            _lngAudioRightSampleCount = 0;
            _iLeftAudioPeriod = -1;
        }

        _lngAudioLeftSampleCount += audioSect.getLeftSampleCount();
        _lngAudioRightSampleCount += audioSect.getRightSampleCount();

        _prevAudioSect = audioSect;
    }

    @Override
    public void indexingEndOfDisc() {
        if (_prevAudioSect != null) {
            // submit the audio stream
            super.addDiscItem(new DiscItemSquareAudioStream(
                getCd(),
                _iAudioStartSector, _prevAudioSect.getSectorNumber(),
                _lngAudioLeftSampleCount, _lngAudioRightSampleCount,
                _prevAudioSect.getSamplesPerSecond(), _iLeftAudioPeriod - 2));
        }
    }

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }
    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
