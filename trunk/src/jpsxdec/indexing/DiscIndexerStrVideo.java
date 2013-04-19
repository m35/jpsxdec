/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.indexing.psxvideofps.StrFrameRateCalc;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;

/**
 * Searches for video sectors.
 *
 *  Theoretically it might be possible for there to be multiple video
 *  streams interleaved, but I've yet to see a case of it.
 */
public class DiscIndexerStrVideo extends DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexerStrVideo.class.getName());

    private IVideoSector _prevSector;
    private int _iStartSector;
    private int _iStartFrame;
    private int _iFrame1LastSector = -1;
    private StrFrameRateCalc _fpsCalc;
    private final Logger _errLog;

    public DiscIndexerStrVideo(Logger errLog) {
        _errLog = errLog;
    }

    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem deserializedLine) {
        try {
            if (DiscItemStrVideoStream.TYPE_ID.equals(deserializedLine.getType())) {
                return new DiscItemStrVideoStream(deserializedLine);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector sector) {
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
            _fpsCalc = new StrFrameRateCalc(_prevSector.getSectorNumber() - _iStartSector,
                                            _prevSector.getFrameNumber(),
                                            _prevSector.getChunkNumber(),
                                            _prevSector.getChunksInFrame());
        } else {
            _fpsCalc.nextVideo(_prevSector.getSectorNumber() - _iStartSector,
                                _prevSector.getFrameNumber(),
                                _prevSector.getChunkNumber(),
                                _prevSector.getChunksInFrame());
        }
        if (vidSect.matchesPrevious(_prevSector)) {
            if (_iFrame1LastSector < 0 && vidSect.getFrameNumber() > _iStartFrame)
                _iFrame1LastSector = _prevSector.getSectorNumber() - _iStartSector;
        } else {
            endOfMovie();
            _iStartFrame = vidSect.getFrameNumber();
            _iStartSector = vidSect.getSectorNumber();
        }
        
        _prevSector = vidSect;
    }

    private void endOfMovie() {
        Fraction sectorsPerFrame;
        if (_fpsCalc != null && (sectorsPerFrame = _fpsCalc.getSectorsPerFrame()) != null) {
            if (LOG.isLoggable(Level.INFO))
                LOG.info(_fpsCalc.toString());
            if (_iStartFrame > 1 && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Video stream first frame is not 0 or 1: " + _iStartFrame);
            }

            super.addDiscItem(new DiscItemStrVideoStream(
                    _iStartSector, _prevSector.getSectorNumber(),
                    _iStartFrame, _prevSector.getFrameNumber(),
                    _prevSector.getWidth(), _prevSector.getHeight(),
                    (int)sectorsPerFrame.getNumerator(),
                    (int)sectorsPerFrame.getDenominator(),
                    _iFrame1LastSector));
        } else {
            super.addDiscItem(new DiscItemStrVideoStream(
                    _iStartSector, _prevSector.getSectorNumber(),
                    _iStartFrame, _prevSector.getFrameNumber(),
                    _prevSector.getWidth(), _prevSector.getHeight(),
                    _prevSector.getSectorNumber() - _iStartSector + 1,
                    _prevSector.getFrameNumber() - _iStartFrame + 1,
                    _iFrame1LastSector));
        }
        _fpsCalc = null;
        _prevSector = null;
        _iFrame1LastSector = -1;
    }

    @Override
    public void indexingEndOfDisc() {
        if (_prevSector != null) {
            endOfMovie();
        }
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream inStream) throws IOException {
    }

    @Override
    public void indexGenerated(DiscIndex index) {
    }

}
