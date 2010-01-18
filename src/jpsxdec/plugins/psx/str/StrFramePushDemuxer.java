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

import jpsxdec.plugins.psx.video.DemuxImage;
import java.util.AbstractList;
import java.util.logging.Logger;
import jpsxdec.util.IWidthHeight;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  Sectors need to be added ('pushed') in their proper order. */
public class StrFramePushDemuxer implements IWidthHeight {

    private static final Logger log = Logger.getLogger(StrFramePushDemuxer.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private IVideoSector[] _aoChunks;
            
    private int _iWidth = -1;
    private int _iHeight = -1;
    private int _iFrame;
    
    private int _iDemuxFrameSize = 0;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public StrFramePushDemuxer() {
        _iFrame = -1;
    }
    
    /** @param lngFrame  -1 for the frame of the first chunk received. */
    public StrFramePushDemuxer(int iFrame) {
        _iFrame = iFrame;
    }

    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** [IWidthHeight] */
    public int getWidth() {
        return _iWidth;
    }

    /** [IWidthHeight] */
    public int getHeight() {
        return _iHeight;
    }
    
    /** Returns the frame number being demuxer, or -1 if still unknown. */
    public int getFrameNumber() {
        return _iFrame;
    }
    
    public int getDemuxFrameSize() {
        return _iDemuxFrameSize;
    }
    
    public boolean isFull() {
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
    
    public void addChunk(IVideoSector chunk) {
        if (_iFrame < 0)
            _iFrame = chunk.getFrameNumber();
        else if (_iFrame != chunk.getFrameNumber())
            throw new IllegalArgumentException("Not all chunks have the same frame number");

        if (_iWidth < 0)
            _iWidth = chunk.getWidth();
        else if (_iWidth != chunk.getWidth())
            throw new IllegalArgumentException("Not all chunks of this frame have the same width");

        if (_iHeight < 0)
            _iHeight = chunk.getHeight();
        else if (_iHeight != chunk.getHeight())
            throw new IllegalArgumentException("Not all chunks of this frame have the same height");

        // for easy reference
        int iChkNum = chunk.getChunkNumber();
        
        // if this is the first chunk added
        if (_aoChunks == null)
            _aoChunks = new IVideoSector[chunk.getChunksInFrame()];
        else if (chunk.getChunksInFrame() != _aoChunks.length) {
            // if the number of chunks in the frame suddenly changed
            throw new IllegalArgumentException("Number of chunks in this frame changed from " + 
                          _aoChunks.length + " to " + _aoChunks.length);
        } else if (iChkNum >= _aoChunks.length) {
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
    
    public void addChunks(AbstractList<IVideoSector> oChks) {
        for (IVideoSector oChk : oChks) {
            addChunk(oChk);
        }
    }
    
    public DemuxImage getDemuxFrame() {
        byte[] ab = new byte[_iDemuxFrameSize];
        int iPos = 0;
        if (_aoChunks == null) {
            log.warning("Frame " + _iFrame + " never received any frame chunks.");
        } else {
            for (int iChunk = 0; iChunk < _aoChunks.length; iChunk++) {
                IVideoSector chunk = _aoChunks[iChunk];
                if (chunk != null) {
                    _aoChunks[iChunk].copyIdentifiedUserData(ab, iPos);
                    iPos += _aoChunks[iChunk].getPSXUserDataSize();
                } else {
                    log.warning("Frame " + _iFrame + " chunk " + iChunk + " missing.");
                }
            }
        }
        return new DemuxImage(_iWidth, _iHeight, _iFrame, ab);
    }

}
