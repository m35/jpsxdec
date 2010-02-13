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

import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.util.IWidthHeight;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  Sectors need to be added ('pushed') in their proper order. */
public class FrameDemuxer implements IWidthHeight {

    private static final Logger log = Logger.getLogger(FrameDemuxer.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private IVideoSector[] _aoChunks;
    private int _iChunkCount = -1;

    private int _iFrame;
    
    private int _iDemuxFrameSize = 0;
    private IDemuxReceiver _demuxReceiver;
    private final int _iVideoStartSector, _iVideoEndSector;
    private byte[] _abDemuxBuff;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public FrameDemuxer(IDemuxReceiver demuxFeeder, int iVideoStartSector, int iVideoEndSector) {
        _iFrame = -1;
        _demuxReceiver = demuxFeeder;
        _iVideoStartSector = iVideoStartSector;
        _iVideoEndSector = iVideoEndSector;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** [IWidthHeight] */
    public int getWidth() {
        return _demuxReceiver.getWidth();
    }

    /** [IWidthHeight] */
    public int getHeight() {
        return _demuxReceiver.getHeight();
    }
    
    public int getDemuxFrameSize() {
        return _iDemuxFrameSize;
    }
    
    private boolean isFull() {
        if (_aoChunks == null)
            return false;
        
        for (IVideoSector chk : _aoChunks) {
            if (chk == null) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        if (_aoChunks == null)
            return true;

        for (IVideoSector chk : _aoChunks) {
            if (chk != null) return false;
        }
        return true;
    }

    public int getChunksInFrame() {
        return _aoChunks.length;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public void feedSector(IVideoSector chunk) throws IOException {

        if (chunk.getSectorNumber() < _iVideoStartSector ||
            chunk.getSectorNumber() > _iVideoEndSector)
            return;

        if (chunk.getWidth() != _demuxReceiver.getWidth())
            throw new IllegalArgumentException("Inconsistent width.");

        if (chunk.getHeight() != _demuxReceiver.getHeight())
            throw new IllegalArgumentException("Inconsistent height.");


        if (_iFrame < 0) {
            newFrame(chunk);
        } else if (_iFrame == chunk.getFrameNumber()) {
            continueFrame(chunk);
        } else {
            endFrame();
            newFrame(chunk);
        }
        
        if (isFull()) {
            endFrame();
        }
    }

    public void flush() throws IOException {
        endFrame();
    }

    private void continueFrame(IVideoSector chunk) {
        // for easy reference
        final int iChkNum = chunk.getChunkNumber();

        _iFrame = chunk.getFrameNumber();

        if (chunk.getChunksInFrame() != _iChunkCount) {
            // if the number of chunks in the frame suddenly changed
            throw new IllegalArgumentException("Number of chunks in this frame changed from " +
                          _aoChunks.length + " to " + _aoChunks.length);
        } else if (iChkNum >= _iChunkCount) {
            // if the chunk number is out of valid range
            throw new IllegalArgumentException("Frame chunk number " + iChkNum + " is outside the range of possible chunk numbers.");
        }

        // now add the chunk where it belongs in the list
        // but make sure we don't alrady have the chunk
        if (_aoChunks[iChkNum] != null)
            throw new IllegalArgumentException("Chunk number " + iChkNum + " already received.");

        _aoChunks[iChkNum] = chunk;
        // add the sector's data size to the total
        _iDemuxFrameSize += chunk.getPSXUserDataSize();

    }

    private void newFrame(IVideoSector chunk) {
        _iFrame = chunk.getFrameNumber();

        if (_aoChunks == null || _aoChunks.length < chunk.getChunksInFrame())
            _aoChunks = new IVideoSector[chunk.getChunksInFrame()];
        _iChunkCount = chunk.getChunksInFrame();
        _iDemuxFrameSize = 0;

        _aoChunks[chunk.getChunkNumber()] = chunk;
        // add the sector's data size to the total
        _iDemuxFrameSize += chunk.getPSXUserDataSize();
    }
    
    private void endFrame() throws IOException {
        // need at least 1 chunk to continue
        if (_aoChunks == null || _iDemuxFrameSize < 1 || _iChunkCount < 1) {
            log.warning("Frame " + _iFrame + " never received any frame chunks.");
            return;
        }

        if (_abDemuxBuff == null || _abDemuxBuff.length < _iDemuxFrameSize)
            _abDemuxBuff = new byte[_iDemuxFrameSize];

        int iEndSector = -1;
        int iPos = 0;
        for (int iChunk = 0; iChunk < _iChunkCount; iChunk++) {
            IVideoSector chunk = _aoChunks[iChunk];
            if (chunk != null) {
                chunk.copyIdentifiedUserData(_abDemuxBuff, iPos);
                iPos += chunk.getPSXUserDataSize();
                if (chunk.getSectorNumber() > iEndSector)
                    iEndSector = chunk.getSectorNumber();
                _aoChunks[iChunk] = null;
            } else {
                log.warning("Frame " + _iFrame + " chunk " + iChunk + " missing.");
            }
        }

        _demuxReceiver.receive(_abDemuxBuff, _iDemuxFrameSize, _iFrame, iEndSector);
        
        _iDemuxFrameSize = 0;
        _iChunkCount = 0;
        _iFrame = -1;
    }

}
