/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2013  Michael Sabin
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
import jpsxdec.discitems.CrusaderDemuxer;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemCrusader;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorCrusader;
import jpsxdec.util.NotThisTypeException;


/** Identify Crusader: No Remorse audio/video streams. */
public class DiscIndexerCrusader extends DiscIndexer 
    implements ISectorFrameDemuxer.ICompletedFrameListener 
{

    private final Logger _errLog;
    
    private CrusaderDemuxer _demuxer;
    
    private int _iStartFrame = -1;
    private int _iEndFrame = -1;
    
    public DiscIndexerCrusader(Logger log) {
        _errLog = log;
        _demuxer = new CrusaderDemuxer();
        _demuxer.setFrameListener(this);
    }

    @Override
    public void indexingSectorRead(IdentifiedSector identifiedSector) {
        if (!(identifiedSector instanceof SectorCrusader))
            return;

        try {
            boolean blnAccepted = _demuxer.indexFeedSector(identifiedSector, _errLog);
            while (!blnAccepted) {
                addDiscItem(new DiscItemCrusader(_demuxer.getStartSector(), _demuxer.getEndSector(), 
                        _demuxer.getWidth(), _demuxer.getHeight(), 
                        _iStartFrame, _iEndFrame));
                _iStartFrame = _iEndFrame = -1;
                _demuxer = new CrusaderDemuxer();
                _demuxer.setFrameListener(this);
                blnAccepted = _demuxer.indexFeedSector(identifiedSector, _errLog);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Should never happen", ex);
        }
    }

    public void frameComplete(IDemuxedFrame frame) {
        if (_iStartFrame == -1)
            _iStartFrame = frame.getFrame();
        _iEndFrame = frame.getFrame();
    }

    @Override
    public void indexingEndOfDisc() {
        try {
            _demuxer.flush(_errLog);
            if (_demuxer.foundAPayload()) {
                addDiscItem(new DiscItemCrusader(_demuxer.getStartSector(), _demuxer.getEndSector(), 
                                                 _demuxer.getWidth(), _demuxer.getHeight(), 
                                                 _iStartFrame, _iEndFrame));
            }
        } catch (IOException ex) {
            _errLog.log(Level.SEVERE, "Error flushing Crusader movie", ex);
            throw new RuntimeException("Error flushing Crusader movie", ex);
        }
    }

    @Override
    public void indexGenerated(DiscIndex index) {
    }
    
    @Override
    public DiscItem deserializeLineRead(SerializedDiscItem deserializedLine) {
        try {
            if (DiscItemCrusader.TYPE_ID.equals(deserializedLine.getType())) {
                return new DiscItemCrusader(deserializedLine);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream is) throws IOException {
    }

}
