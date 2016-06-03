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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemXaAudioStream;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorXaAudio;
import jpsxdec.util.NotThisTypeException;

/**
 * Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end.
 *
 */
public class DiscIndexerXaAudio extends DiscIndexer implements DiscIndexer.Identified {

    private static final Logger LOG = Logger.getLogger(DiscIndexerXaAudio.class.getName());

    @Nonnull
    private final Logger _errLog;

    public DiscIndexerXaAudio(@Nonnull Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public void listPostProcessing(Collection<DiscItem> allItems) {
    }

    @Override
    public void indexGenerated(DiscIndex index) {        
    }

    /** Tracks the indexing of one audio stream in one channel. */
    private static class AudioStreamIndex {

        /** First sector of the audio stream. */
        private final int _iStartSector;

        /** Last sector before {@link #_currentXA} that was a part of this stream. */
        @CheckForNull
        private SectorXaAudio _previousXA;
        /** Last sector (or 'current' sector, if you will) that was a part of this stream.
         *  Is never null. */
        @Nonnull
        private SectorXaAudio _currentXA;

        /** Get the last (or 'current') sector that was part of this stream.
         *  May be null. */
        public @Nonnull SectorXaAudio getCurrent() { return _currentXA; }

        /** Number of sectors between XA sectors that are part of this stream.
         * Should only ever be 1, 2, 4, 8, 16, or 32
         * (enforced by {@link SectorXAAudio#matchesPrevious(jpsxdec.sectors.IdentifiedSector)}).
         * Is -1 until 2nd sector is discovered. */
        private int _iAudioStride = -1;

        @Nonnull
        private final Logger _errLog;

        public AudioStreamIndex(@Nonnull SectorXaAudio first, @Nonnull Logger errLog) {
            _currentXA = first;
            _iStartSector = first.getSectorNumber();
            _errLog = errLog;
        }

        /**
         * @return true if the sector was accepted as part of this stream,
         *         or false if the stream is finished.
         */
        public boolean sectorRead(@Nonnull SectorXaAudio newCurrent) {

            if (!newCurrent.matchesPrevious(_currentXA))
                return false;

            // check the stride
            int iStride = newCurrent.getSectorNumber() - _currentXA.getSectorNumber();
            if (_iAudioStride < 0)
                _iAudioStride = iStride;
            else if (iStride != _iAudioStride)
                return false;

            _previousXA = _currentXA;
            _currentXA = newCurrent;

            return true; // the sector was accepted
        }

        public void createMediaItem(@Nonnull DiscIndexer adder) {
            if (_previousXA == null && _currentXA.isSilent()) {
                I.IGNORING_SILENT_XA_SECTOR(_iStartSector, _currentXA.getChannel()).log(_errLog, Level.INFO);
                return;
            }
            adder.addDiscItem(new DiscItemXaAudioStream(
                              adder.getCd(),
                              _iStartSector, _currentXA.getSectorNumber(),
                              _currentXA.getChannel(),
                              _currentXA.getSamplesPerSecond(),
                              _currentXA.isStereo(), _currentXA.getBitsPerSample(),
                              _iAudioStride));
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _currentXA.getSectorNumber() + _iAudioStride);
        }
    }


    private final AudioStreamIndex[] _aoChannels = 
            new AudioStreamIndex[SectorXaAudio.MAX_VALID_CHANNEL+1];

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem serial) {
        try {
            if (DiscItemXaAudioStream.TYPE_ID.equals(serial.getType())) {
                return new DiscItemXaAudioStream(getCd(), serial);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    public void indexingSectorRead(@Nonnull CdSector cdSector,
                                   @CheckForNull IdentifiedSector idSector)
    {
        if (idSector instanceof SectorXaAudio) {

            SectorXaAudio audSect = (SectorXaAudio)idSector;

            AudioStreamIndex audStream = _aoChannels[audSect.getChannel()];
            if (audStream == null) {
                _aoChannels[audSect.getChannel()] = new AudioStreamIndex(audSect, _errLog);
            } else if (!audStream.sectorRead(audSect)) {
                audStream.createMediaItem(this);
                _aoChannels[audSect.getChannel()] = new AudioStreamIndex(audSect, _errLog);
            }
        } else {

            // check for streams that are beyond their stride and close them
            for (int i = 0; i < _aoChannels.length; i++) {
                AudioStreamIndex audStream = _aoChannels[i];
                if (audStream != null && audStream.ended(cdSector.getSectorNumberFromStart())) {
                    audStream.createMediaItem(this);
                    _aoChannels[i] = null;
                }
            }
        }

        if (cdSector.hasSubHeader()              &&
            cdSector.getSubMode().getEofMarker() &&
            cdSector.getSubHeaderChannel() < _aoChannels.length)
        {
            // if the sector's EOF bit was set, this stream is closed
            // this is important for many games
            AudioStreamIndex audStream = _aoChannels[cdSector.getSubHeaderChannel()];
            if (audStream != null) {
                audStream.createMediaItem(this);
                _aoChannels[cdSector.getSubHeaderChannel()] = null;
            }
        }

    }

    @Override
    public void indexingEndOfDisc() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                audStream.createMediaItem(this);
                _aoChannels[i] = null;
            }
        }
    }

}
