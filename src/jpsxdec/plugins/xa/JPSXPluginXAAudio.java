/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.xa;

import jpsxdec.plugins.JPSXPlugin;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.IndexingDemuxerIS;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;

/**
 * Watches for XA audio streams.
 * Tracks the channel numbers and maintains all the XA streams.
 * Adds them to the media list as they end.
 *
 */
public class JPSXPluginXAAudio extends JPSXPlugin {

    private static final Logger log = Logger.getLogger(JPSXPluginXAAudio.class.getName());

    private static JPSXPluginXAAudio SINGLETON;

    public static JPSXPluginXAAudio getPlugin() {
        if (SINGLETON == null)
            SINGLETON = new JPSXPluginXAAudio();
        return SINGLETON;
    }

    private static class AudioStreamIndex {

        private final int _iStartSector;

        private SectorXA _previousXA;
        private SectorXA _currentXA;

        public SectorXA getCurrent() { return _currentXA; }

        private long _lngSampleCount = 0;

        private int _iAudioStride = -1;

        public AudioStreamIndex(SectorXA first) {
            _currentXA = first;
            _iStartSector = first.getSectorNumber();
        }

        /**
         * @return true if the sector was accepted as part of this stream.
         */
        public boolean sectorRead(SectorXA newCurrent) {
            if (newCurrent.matchesPrevious(_currentXA) == null)
                return false;

            // if the previous sector's EOF bit was set, this stream is closed
            // -- unfortuantely this is unreliable --
            //if (_prevXA.getEOFBit())
            //    return false;

            // check the period
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
                if (log.isLoggable(Level.INFO))
                    log.info("Ignoring silent XA audio stream only 1 sector long at " + _iStartSector + " channel " + _currentXA.getChannel());
                return null;
            }
            _lngSampleCount += _currentXA.getSampleCount();
            return _currentXA.createMedia(_iStartSector, _iAudioStride, _lngSampleCount);
        }

        public DiscItem createMediaItemFromPrevious() {
            if (_previousXA == null) {
                if (log.isLoggable(Level.WARNING))
                    log.warning("Trying to create XA item from non-existant previous sector! Current sector is " + _iStartSector);
                return null;
            } else {
                return _currentXA.createMedia(_iStartSector, _iAudioStride, _lngSampleCount);
            }
        }

        public boolean ended(int iSectorNum) {
            return (_iAudioStride >= 0) &&
                   (iSectorNum > _currentXA.getSectorNumber() + _iAudioStride);
        }
    }


    AudioStreamIndex[] _aoChannels = new AudioStreamIndex[32];

    private JPSXPluginXAAudio() {
        
    }

    @Override
    public IdentifiedSector identifySector(CDSector oSect) {
        try { return new SectorXA(oSect); }
        catch (NotThisTypeException ex) {}
        try { return new SectorXANull(oSect); }
        catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization oSerial) {
        try {
            if (DiscItemXAAudioStream.TYPE_ID.equals(oSerial.getType()))
                super.addDiscItem(new DiscItemXAAudioStream(oSerial));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector sector) {
        if (sector instanceof SectorXA) {

            SectorXA audSect = (SectorXA)sector;

            AudioStreamIndex audStream = _aoChannels[audSect.getChannel()];
            if (audStream == null) {
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            } else if (!audStream.sectorRead(audSect)) {
                super.addDiscItem(audStream.createMediaItemFromCurrent());
                audStream = new AudioStreamIndex(audSect);
                _aoChannels[audSect.getChannel()] = audStream;
            }
        } else {
            // check for streams that are beyond their stride

            for (int i = 0; i < _aoChannels.length; i++) {
                AudioStreamIndex audStream = _aoChannels[i];
                if (audStream != null && audStream.ended(sector.getSectorNumber())) {
                    super.addDiscItem(audStream.createMediaItemFromCurrent());
                    _aoChannels[i] = null;
                }
            }
        }

        // This this sector's EOF bit is set, close all streams
        // -- unfortunately this isn't reliable
        //if (sector.getEOFBit())
            //indexing_endOfDisc();
    }

    @Override
    public void indexing_endOfDisc() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                super.addDiscItem(audStream.createMediaItemFromCurrent());
                _aoChannels[i] = null;
            }
        }
    }

    @Override
    public void indexing_static(IndexingDemuxerIS oIS) throws IOException {
        if (oIS.getSectorPosition() == 0) {

        }
    }

    public void indexing_endAllCurrent() {
        indexing_endOfDisc();
    }

    public void indexing_endAllBeforeCurrent() {
        for (int i = 0; i < _aoChannels.length; i++) {
            AudioStreamIndex audStream = _aoChannels[i];
            if (audStream != null) {
                DiscItem item = audStream.createMediaItemFromPrevious();
                if (item != null) {
                    super.addDiscItem(item);
                    SectorXA current = audStream.getCurrent();
                    _aoChannels[i] = new AudioStreamIndex(current);
                }
            }
        }
    }

    @Override
    public String getPluginDescription() {
        return "XA ADPCM audio decoding plugin for jPSXdec by Michael Sabin";
    }

    @Override
    public DemuxFrameUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        return null;
    }

}
