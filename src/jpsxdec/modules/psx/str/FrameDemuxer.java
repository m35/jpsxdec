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

package jpsxdec.modules.psx.str;

import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.util.IWidthHeight;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  Sectors need to be added ('pushed') in their proper order. */
public abstract class FrameDemuxer implements IWidthHeight {

    private static final Logger log = Logger.getLogger(FrameDemuxer.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sectors outside of this range are rejected. */
    private final int _iVideoStartSector, _iVideoEndSector;
    /** Expected dimentions of the frame. */
    private final int _iWidth, _iHeight;

    /** Current frame number. */
    private int _iFrame = -1;

    private IVideoSector[] _aoChunks;
    private int _iChunkCount = -1;
    /** Size in bytes of the data contained in all  the demux sectors. */
    private int _iDemuxFrameSize = -1;

    /** Basically the last sector of the frame, which is assumed to be when
     * the frame will be displayed. */
    private int _iPresentationSector = -1;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public FrameDemuxer(int iWidth, int iHeight, 
                        int iVideoStartSector, int iVideoEndSector)
    {
        if (iWidth < 1 || iHeight < 1)
            throw new IllegalArgumentException("Invalid dimentions " + iWidth + "x" + iHeight);
        if (iVideoStartSector < 0 || iVideoEndSector < iVideoStartSector)
            throw new IllegalArgumentException("Invalid video sector range " + iVideoStartSector + "-" + iVideoEndSector);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iVideoStartSector = iVideoStartSector;
        _iVideoEndSector = iVideoEndSector;
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

    /** Size of the demuxed frame, or -1 if no sectors have been added. */
    public int getDemuxSize() {
        return _iDemuxFrameSize;
    }

    /** The last sector of the frame, or -1 if no sectors have been added. */
    public int getPresentationSector() {
        return _iPresentationSector;
    }

    /** The frame number of the demuxed frame, or -1 if no sectors have been added. */
    public int getFrame() {
        return _iFrame;
    }
    
    private boolean isFull() {
        if (_iChunkCount < 1)
            return false;

        for (int i = 0; i < _iChunkCount; i++) {
            if (_aoChunks[i] == null) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        if (_iChunkCount >= 1)
            return true;

        for (int i = 0; i < _iChunkCount; i++) {
            if (_aoChunks[i] != null) return false;
        }
        return true;
    }

    public int getChunksInFrame() {
        return _iChunkCount;
    }

    /** @throws ArrayIndexOutOfBoundsException if index is less-than 0 or
     *                                         greater-than {@link #getChunksInFrame()}. */
    public IVideoSector getChunk(int i) {
        if (_aoChunks == null)
            return null;
        return _aoChunks[i];
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public void feedSector(IVideoSector chunk) throws IOException {

        if (chunk.getSectorNumber() < _iVideoStartSector ||
            chunk.getSectorNumber() > _iVideoEndSector)
            return;

        if (chunk.getWidth() != _iWidth)
            throw new IllegalArgumentException("Inconsistent width.");

        if (chunk.getHeight() != _iHeight)
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

        if (chunk.getChunksInFrame() != _iChunkCount) {
            // if the number of chunks in the frame suddenly changed
            throw new IllegalArgumentException("Number of chunks in this frame changed from " +
                          _iChunkCount + " to " + chunk.getChunksInFrame());
        } else if (iChkNum < 0 || iChkNum >= _iChunkCount) {
            // if the chunk number is out of valid range
            throw new IllegalArgumentException("Frame chunk number " + iChkNum + " is outside the range of possible chunk numbers.");
        }

        // make sure we don't alrady have the chunk
        if (_aoChunks[iChkNum] != null)
            throw new IllegalArgumentException("Chunk number " + iChkNum + " already received.");

        // finally add the chunk where it belongs in the list
        _aoChunks[iChkNum] = chunk;
        // add the sector's data size to the total
        _iDemuxFrameSize += chunk.getIdentifiedUserDataSize();
        // update the presentation sector if it's larger
        if (chunk.getSectorNumber() > _iPresentationSector)
            _iPresentationSector = chunk.getSectorNumber();
    }

    private void newFrame(IVideoSector chunk) {
        _iFrame = chunk.getFrameNumber();
        _iChunkCount = chunk.getChunksInFrame();

        // ensure we have enough capacity for the expected chunks
        if (_aoChunks == null || _aoChunks.length < _iChunkCount)
            _aoChunks = new IVideoSector[_iChunkCount];

        // save the chunk
        _aoChunks[chunk.getChunkNumber()] = chunk;
        // add the sector's data size to the total
        _iDemuxFrameSize = chunk.getIdentifiedUserDataSize();
        // set the presentation sector to the only sector we have so far
        _iPresentationSector = chunk.getSectorNumber();
    }

    /** Clears the current saved frame. {@link #frameComplete()} is not called. */
    public void clear() {
        for (int i = 0; i < _aoChunks.length; i++) {
            _aoChunks[i] = null;
        }

        _iDemuxFrameSize = -1;
        _iChunkCount = -1;
        _iFrame = -1;
        _iPresentationSector = -1;
    }

    /** Copies the demux data into the supplied buffer.
     * @throws IllegalArgumentException if buffer is smaler than {@link #getDemuxSize()}.
     */
    public void copyDemuxData(byte[] abBuffer) {
        if (abBuffer.length < getDemuxSize())
            throw new IllegalArgumentException("Buffer not big enough for demux data");

        int iPos = 0;
        for (int iChunk = 0; iChunk < _iChunkCount; iChunk++) {
            IVideoSector chunk = _aoChunks[iChunk];
            if (chunk != null) {
                chunk.copyIdentifiedUserData(abBuffer, iPos);
                iPos += chunk.getIdentifiedUserDataSize();
            } else {
                log.warning("Frame " + _iFrame + " chunk " + iChunk + " missing.");
            }
        }
    }

    private void endFrame() throws IOException {
        if (_iChunkCount < 1) {
            return;
        }

        frameComplete();

        clear();
    }

    /** Called when no more sectors for a frame will be received. */
    abstract protected void frameComplete() throws IOException;

}
