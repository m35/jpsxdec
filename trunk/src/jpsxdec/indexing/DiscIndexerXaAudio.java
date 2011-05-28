/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import jpsxdec.discitems.DiscItemXAAudioStream;
import jpsxdec.sectors.SectorXAAudio;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.discitems.DiscItemSerialization;
import jpsxdec.discitems.DiscItem;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorAliceVideo;
import jpsxdec.util.NotThisTypeException;

/**
 * Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end.
 *
 */
public class DiscIndexerXaAudio extends DiscIndexer {

    private static final Logger log = Logger.getLogger(DiscIndexerXaAudio.class.getName());

    private Logger _errLog;

    public DiscIndexerXaAudio(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public void mediaListGenerated(DiscIndex aThis) {
        
    }

    /** Tracks the indexing of one audio stream in one channel. */
    private class AudioStreamIndex {

        /** First sector of the audio stream. */
        private final int _iStartSector;

        /** Last sector before _currentXA that was a part of this stream. */
        private SectorXAAudio _previousXA;
        /** Last sector (or 'current' sector, if you will) that was a part of this stream.
         Is never null. */
        private SectorXAAudio _currentXA;

        /** Get the last (or 'current') sector that was part of this stream.
         May be null. */
        public SectorXAAudio getCurrent() { return _currentXA; }

        /** Count of how many sample are found in the stream. */
        private long _lngSampleCount = 0;

        /** Number of sectors between XA sectors that are part of this stream.
         * Should only ever be 4, 8, 16, or 32. */
        private int _iAudioStride = -1;

        public AudioStreamIndex(SectorXAAudio first) {
            _currentXA = first;
            _iStartSector = first.getSectorNumber();
        }

        /**
         * @return true if the sector was accepted as part of this stream.
         */
        public boolean sectorRead(SectorXAAudio newCurrent) {
            // if the previous ('current') sector's EOF bit was set, this stream is closed
            // this is important for Silent Hill and R4 Ridge Racer
            if (_currentXA.getCDSector().getSubMode().getEofMarker())
                return false;

            if (!newCurrent.matchesPrevious(_currentXA))
                return false;

            // check the stride
            int iStride = newCurrent.getSectorNumber() - _currentXA.getSectorNumber();
            if (_iAudioStride < 0)
                _iAudioStride = iStride;
            else if (iStride != _iAudioStride)
                return false;

            _previousXA = _currentXA;
            _lngSampleCount += _previousXA.getSampleCount();
            _currentXA = newCurrent;

            return true; // the sector was accepted
        }

        public DiscItem createMediaItemFromCurrent() {
            if (_previousXA == null && _currentXA.isAllQuiet()) {
                if (_errLog.isLoggable(Level.INFO)) {
                    _errLog.info("Ignoring a silent XA audio stream that is only 1 sector long at sector " + _iStartSector + ", channel " + _currentXA.getChannel());
                }
                return null;
            }
            _lngSampleCount += _currentXA.getSampleCount();
            return new DiscItemXAAudioStream(
                _iStartSector, _currentXA.getSectorNumber(),
                _currentXA.getChannel(),
                _lngSampleCount,
                _currentXA.getSamplesPerSecond(),
                _currentXA.isStereo(), _currentXA.getBitsPerSample(),
                _iAudioStride);
        }

        public DiscItem createMediaItemFromPrevious() {
            if (_previousXA == null) {
                if (log.isLoggable(Level.WARNING))
                    log.warning("Trying to create XA item from non-existant previous sector! Current sector is " + _iStartSector);
                return null;
            } else {
                return new DiscItemXAAudioStream(
                    _iStartSector, _currentXA.getSectorNumber(),
                    _currentXA.getChannel(),
                    _lngSampleCount,
                    _currentXA.getSamplesPerSecond(),
                    _currentXA.isStereo(), _currentXA.getBitsPerSample(),
                    _iAudioStride);
            }
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _currentXA.getSectorNumber() + _iAudioStride);
        }
    }


    private final AudioStreamIndex[/*32*/] _aoChannels = new AudioStreamIndex[32];

    @Override
    public DiscItem deserializeLineRead(DiscItemSerialization oSerial) {
        try {
            if (DiscItemXAAudioStream.TYPE_ID.equals(oSerial.getType())) {
                return new DiscItemXAAudioStream(oSerial);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector sector) {
        if (sector instanceof SectorXAAudio) {

            SectorXAAudio audSect = (SectorXAAudio)sector;

            AudioStreamIndex audStream = _aoChannels[audSect.getChannel()];
            if (audStream == null) {
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            } else if (!audStream.sectorRead(audSect)) {
                DiscItem item = audStream.createMediaItemFromCurrent();
                if (item != null)
                    super.addDiscItem(item);
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            }
        } else {

            // Alice in Cyberland, FF7, and probably others need to split
            // audio at the start of movies because there's no other
            // indicator of audio splitting
            if (sector instanceof IVideoSector && ((IVideoSector)sector).splitAudio()) {
                indexingEndOfDisc();
            }
            // check for streams that are beyond their stride

            for (int i = 0; i < _aoChannels.length; i++) {
                AudioStreamIndex audStream = _aoChannels[i];
                if (audStream != null && audStream.ended(sector.getSectorNumber())) {
                    DiscItem item = audStream.createMediaItemFromCurrent();
                    if (item != null)
                        super.addDiscItem(item);
                    _aoChannels[i] = null;
                }
            }
        }

    }

    @Override
    public void indexingEndOfDisc() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                DiscItem item = audStream.createMediaItemFromCurrent();
                if (item != null)
                    super.addDiscItem(item);
                _aoChannels[i] = null;
            }
        }
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream oIS) throws IOException {
    }

}
