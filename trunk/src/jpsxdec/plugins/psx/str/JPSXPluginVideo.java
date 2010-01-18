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

package jpsxdec.plugins.psx.str;

import jpsxdec.plugins.psx.str.fps.STRFrameRateCalc;
import jpsxdec.plugins.JPSXPlugin;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.IndexingDemuxerIS;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.DiscIndex;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_STRv2;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor_STRv3;
import jpsxdec.plugins.xa.JPSXPluginXAAudio;
import jpsxdec.util.Fraction;

/**
 * Searches for video sectors
 *
 * Theorhetically it might be possible for there to be multiple video
 *  streams interleaved, but I've yet to see a case of it.
 *  Plus, if logo.iki is ever figured out, its video chunks claim to
 *  each be on a different channel.
 */
public class JPSXPluginVideo extends JPSXPlugin {

    private static final Logger log = Logger.getLogger(JPSXPluginVideo.class.getName());

    private static JPSXPluginVideo SINGLETON;

    public static JPSXPluginVideo getPlugin() {
        if (SINGLETON == null)
            SINGLETON = new JPSXPluginVideo();
        return SINGLETON;
    }

    private IVideoSector _prevSector;
    private int _iStartSector;
    private int _iStartFrame;
    private int _iFirstSectorAfterFrame1 = -1;
    private STRFrameRateCalc _fpsCalc;

    private JPSXPluginVideo() {}

    @Override
    public IdentifiedSector identifySector(CDSector oSect) {
        try {
            return new SectorSTR(oSect);
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void deserialize_lineRead(DiscItemSerialization oSerial) {
        try {
            if (DiscItemSTRVideo.TYPE_ID.equals(oSerial.getType()))
                super.addDiscItem(new DiscItemSTRVideo(oSerial));
        } catch (NotThisTypeException ex) {}
    }

    @Override
    public void indexing_sectorRead(IdentifiedSector sector) {
        if (!(sector instanceof IVideoSector)) {
            // don't care if there is a break in the streaming data
            // specifically for the logo.iki case
            return;
        }

        IVideoSector vidSect = (IVideoSector)sector;

        // first time?
        if (_prevSector == null) {
            _prevSector = vidSect;
            _iStartFrame = vidSect.getFrameNumber();
            _iStartSector = vidSect.getSectorNumber();
            return;
        }

        if (_fpsCalc == null) {
            _fpsCalc = new STRFrameRateCalc(_prevSector.getFrameNumber(),
                                             _prevSector.getChunkNumber(),
                                             _prevSector.getChunksInFrame());
        } else {
            _fpsCalc.nextVideo(_prevSector.getSectorNumber() - _iStartSector,
                                _prevSector.getFrameNumber(),
                                _prevSector.getChunkNumber(),
                                _prevSector.getChunksInFrame());
        }
        if (vidSect.matchesPrevious(_prevSector)) {
            if (_iFirstSectorAfterFrame1 < 0 && vidSect.getFrameNumber() > 1)
                _iFirstSectorAfterFrame1 = _prevSector.getSectorNumber() + 1 - _iStartSector;
        } else {
            endOfMovie();
            _iStartFrame = vidSect.getFrameNumber();
            _iStartSector = vidSect.getSectorNumber();
            if (vidSect instanceof SectorSTR)
                JPSXPluginXAAudio.getPlugin().indexing_endAllCurrent();
        }
        
        _prevSector = vidSect;
    }

    private void endOfMovie() {
        Fraction oSectorsPerFrame;
        if (_fpsCalc != null && (oSectorsPerFrame = _fpsCalc.getSectorsPerFrame()) != null) {
            log.info(_fpsCalc.toString());
            if (_iStartFrame > 1 && log.isLoggable(Level.WARNING)) {
                log.warning("Video stream first frame is not 0 or 1: " + _iStartFrame);
            }
            super.addDiscItem(_prevSector.createMedia(_iStartSector, _iStartFrame,
                                                       _iFirstSectorAfterFrame1,
                                                       (int)oSectorsPerFrame.getNumerator(),
                                                       (int)oSectorsPerFrame.getDenominator()));
        } else {
            super.addDiscItem(_prevSector.createMedia(_iStartSector, _iStartFrame, _iFirstSectorAfterFrame1));
        }
        _fpsCalc = null;
        _prevSector = null;
        _iFirstSectorAfterFrame1 = -1;
    }

    @Override
    public void indexing_endOfDisc() {
        if (_prevSector != null) {
            endOfMovie();
        }
    }

    @Override
    public void indexing_static(IndexingDemuxerIS inStream) throws IOException {
    }

    @Override
    public void mediaListGenerated(DiscIndex index) {
        super.mediaListGenerated(index);

        for (DiscItem item : index) {
            if (item instanceof DiscItemSTRVideo)
                ((DiscItemSTRVideo)item).collectParallelAudio(index);
        }

    }

    @Override
    public String getPluginDescription() {
        return "STR video decoding plugin for jPSXdec by Michael Sabin";
    }

    @Override
    public DemuxFrameUncompressor identifyVideoFrame(byte[] abHeaderBytes, long lngFrameNum) {
        if (DemuxFrameUncompressor_STRv2.checkHeader(abHeaderBytes))
            return new DemuxFrameUncompressor_STRv2();
        else if (DemuxFrameUncompressor_STRv3.checkHeader(abHeaderBytes))
            return new DemuxFrameUncompressor_STRv3();
        else
            return null;
    }


}
