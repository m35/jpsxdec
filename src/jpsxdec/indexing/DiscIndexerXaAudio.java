/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemXaAudioStream;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorXaAudio;
import jpsxdec.util.NotThisTypeException;

/**
 * Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end.
 *
 */
public class DiscIndexerXaAudio extends DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexerXaAudio.class.getName());

    private final Logger _errLog;

    public DiscIndexerXaAudio(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public void indexGenerated(DiscIndex aThis) {
        
    }

    /** Tracks the indexing of one audio stream in one channel. */
    private static class AudioStreamIndex {

        /** First sector of the audio stream. */
        private int _iStartSector;

        /** Last sector before {@link #_currentXA} that was a part of this stream. */
        private SectorXaAudio _previousXA;
        /** Last sector (or 'current' sector, if you will) that was a part of this stream.
         *  Is never null. */
        private SectorXaAudio _currentXA;

        /** Get the last (or 'current') sector that was part of this stream.
         *  May be null. */
        public SectorXaAudio getCurrent() { return _currentXA; }

        /** Number of sectors between XA sectors that are part of this stream.
         * Should only ever be 1, 2, 4, 8, 16, or 32
         * (enforced by {@link SectorXAAudio#matchesPrevious(jpsxdec.sectors.IdentifiedSector)}).
         * Is -1 until 2nd sector is discovered. */
        private int _iAudioStride = -1;

        private final Logger _errLog;

        public AudioStreamIndex(SectorXaAudio first, Logger errLog) {
            _currentXA = first;
            _iStartSector = first.getSectorNumber();
            _errLog = errLog;
        }

        /**
         * @return true if the sector was accepted as part of this stream,
         *         or false if the stream is finished.
         */
        public boolean sectorRead(SectorXaAudio newCurrent) {

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

        public void createMediaItemFromCurrent(DiscIndexer adder) {
            if (_previousXA == null && _currentXA.isAllQuiet()) {
                if (_errLog.isLoggable(Level.INFO)) {
                    _errLog.log(Level.INFO, "Ignoring a silent XA audio stream that is only 1 sector long at sector {0,number,#}, channel {1,number,#}", // I18N
                            new Object[]{_iStartSector, _currentXA.getChannel()});
                }
                return;
            }
            adder.addDiscItem(new DiscItemXaAudioStream(
                              _iStartSector, _currentXA.getSectorNumber(),
                              _currentXA.getChannel(),
                              _currentXA.getSamplesPerSecond(),
                              _currentXA.isStereo(), _currentXA.getBitsPerSample(),
                              _iAudioStride));
        }

        public void createMediaItemFromPrevious(DiscIndexer adder) {
            if (_previousXA == null) {
                // this should only happen due to programmer error
                if (LOG.isLoggable(Level.WARNING))
                    LOG.log(Level.WARNING, "Trying to create XA item from non-existant previous sector! Current sector is {0,number,#}", _iStartSector);
                return;
            } 
            DiscItemXaAudioStream ret = new DiscItemXaAudioStream(
                _iStartSector, _previousXA.getSectorNumber(),
                _currentXA.getChannel(),
                _currentXA.getSamplesPerSecond(),
                _currentXA.isStereo(), _currentXA.getBitsPerSample(),
                _iAudioStride);
            _previousXA = null;
            _iStartSector = _currentXA.getSectorNumber();
            _iAudioStride = -1;
            adder.addDiscItem(ret);
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _currentXA.getSectorNumber() + _iAudioStride);
        }
    }


    private final AudioStreamIndex[] _aoChannels = 
            new AudioStreamIndex[SectorXaAudio.MAX_VALID_CHANNEL+1];

    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem oSerial) {
        try {
            if (DiscItemXaAudioStream.TYPE_ID.equals(oSerial.getType())) {
                return new DiscItemXaAudioStream(oSerial);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector sector) {
        if (sector instanceof SectorXaAudio) {

            SectorXaAudio audSect = (SectorXaAudio)sector;

            AudioStreamIndex audStream = _aoChannels[audSect.getChannel()];
            if (audStream == null) {
                _aoChannels[audSect.getChannel()] = new AudioStreamIndex(audSect, _errLog);
            } else if (!audStream.sectorRead(audSect)) {
                audStream.createMediaItemFromCurrent(this);
                _aoChannels[audSect.getChannel()] = new AudioStreamIndex(audSect, _errLog);
            }
        } else {

            // Alice in Cyberland, FF7, and probably others need to split
            // audio at the start of movies because there's no other
            // indicator of audio splitting
            if (sector instanceof IVideoSector) {
                switch (((IVideoSector)sector).splitXaAudio()) {
                    case IVideoSector.SPLIT_XA_AUDIO_CURRENT:
                        indexingEndOfDisc();
                        break;
                    case IVideoSector.SPLIT_XA_AUDIO_PREVIOUS:
                        for (AudioStreamIndex audStream : _aoChannels) {
                            if (audStream != null) {
                                audStream.createMediaItemFromPrevious(this);
                            }
                        }
                        break;
                }
            }

            // check for streams that are beyond their stride
            for (int i = 0; i < _aoChannels.length; i++) {
                AudioStreamIndex audStream = _aoChannels[i];
                if (audStream != null && audStream.ended(sector.getSectorNumber())) {
                    audStream.createMediaItemFromCurrent(this);
                    _aoChannels[i] = null;
                }
            }
        }

        CdSector cdSector = sector.getCdSector();
        if (cdSector.hasSubHeader() && cdSector.getSubMode().getEofMarker()) {
            // if the sector's EOF bit was set, this stream is closed
            // this is important for many games
            AudioStreamIndex audStream = _aoChannels[cdSector.getSubHeaderChannel()];
            if (audStream != null) {
                audStream.createMediaItemFromCurrent(this);
                _aoChannels[cdSector.getSubHeaderChannel()] = null;
            }
        }

    }

    @Override
    public void indexingEndOfDisc() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                audStream.createMediaItemFromCurrent(this);
                _aoChannels[i] = null;
            }
        }
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream is) throws IOException {
    }

}
