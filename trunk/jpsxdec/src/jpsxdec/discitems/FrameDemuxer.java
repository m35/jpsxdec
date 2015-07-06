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

package jpsxdec.discitems;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;

/** Demuxes a series of STR-like frame chunk sectors into a solid stream. */
public abstract class FrameDemuxer<T extends IVideoSector> implements ISectorFrameDemuxer {

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sectors outside of this range are ignored. */
    private final int _iVideoStartSector;
    private final int _iVideoEndSector;

    /** Sector type used in this video.
     * Set by first video sector accepted. All other sector types are ignored. */
    private Class _videoSectorType = null;
    /** Sector number of the last accepted video sector.
     * Too much gap between video sectors causes problems with frame rate calc. */
    private int _iLastSeenSectorNumber = -1;

    private int _iLastSeenFrameNumber = -1;

    /** Chunks of the current frame.
     * Created and grown as needed. */
    private IVideoSector[] _aoCurrent;
    private FrameNumber _currentFrame = null;
    private int _iChunksInFrame = -1;
    private int _iChunksReceived = 0;

    private final FrameNumber.FactoryWithHeader _frameNumberFactory = new FrameNumber.FactoryWithHeader();

    private ICompletedFrameListener _listener;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sectors outside of the start and end sectors (inclusive) are simply ignored. */
    public FrameDemuxer(int iVideoStartSector, int iVideoEndSector) {
        if (iVideoStartSector < 0 || iVideoEndSector < iVideoStartSector)
            throw new IllegalArgumentException("Invalid video sector range " + iVideoStartSector + "-" + iVideoEndSector);
        _iVideoStartSector = iVideoStartSector;
        _iVideoEndSector = iVideoEndSector;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public int getStartSector() {
        return _iVideoStartSector;
    }

    public int getEndSector() {
        return _iVideoEndSector;
    }

    abstract public int getWidth();
    abstract public int getHeight();

    private void ensureCapacity(int iSize) {
        if (_aoCurrent == null) {
            _aoCurrent = new IVideoSector[iSize];
        } else if (_aoCurrent.length < iSize) {
            IVideoSector[] ao = new IVideoSector[iSize];
            System.arraycopy(_aoCurrent, 0, ao, 0, _aoCurrent.length);
            _aoCurrent = ao;
        }
    }

    public boolean feedSector(IdentifiedSector sector, Logger log) throws IOException {
        T chunk = isVideoSector(sector);
        if (chunk == null)
            return false;

        if (!isPartOfVideo(chunk))
            return false;

        if (!isPartOfFrame(chunk)) {
            // frame is done
            flush(log);
        }
        _iLastSeenFrameNumber = getHeaderFrameNumber(chunk);
        if (_currentFrame == null)
            _currentFrame = _frameNumberFactory.next(chunk.getSectorNumber(), _iLastSeenFrameNumber);

        int iChunksInFrame = getChunksInFrame(chunk);

        if (_iChunksInFrame >= 0 &&_iChunksInFrame != iChunksInFrame)
            log.log(Level.WARNING, "Chunks in frame {0} changed from {1} to {2}", // I18N
                    new Object[]{_currentFrame, _iChunksInFrame, iChunksInFrame});
        _iChunksInFrame = iChunksInFrame;

        // now add the chunk
        if (chunk.getChunkNumber() >= iChunksInFrame) {
            log.log(Level.WARNING, "Chunk number {0} is >= chunks in frame {1}", // I18N
                    new Object[]{chunk.getChunkNumber(), iChunksInFrame});
            ensureCapacity(chunk.getChunkNumber());
        } else {
            ensureCapacity(iChunksInFrame);
        }
        _aoCurrent[chunk.getChunkNumber()] = chunk;
        _iChunksReceived++;
        // really must push frames once all chunks are received,
        // in case of duplicate frame numbers it saves warnings above
        if (_iChunksReceived == _iChunksInFrame)
            flush(log);

        return true;
    }

    final public void flush(Logger log) throws IOException {
        if (_iChunksReceived > 0) {
            _listener.frameComplete(
                new DemuxedStrFrame(_currentFrame,
                                    getWidth(), getHeight(),
                                    _aoCurrent, _iChunksInFrame)
            );
            Arrays.fill(_aoCurrent, null);
            _currentFrame = null;
            _iChunksInFrame = -1;
            _iChunksReceived = 0;
        }
    }

    public void setFrameListener(ICompletedFrameListener listener) {
        _listener = listener;
    }

    /* ---------------------------------------------------------------------- */
    /* Protected Functions -------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    abstract protected T isVideoSector(IdentifiedSector sector);
    abstract protected int getHeaderFrameNumber(T chunk);
    abstract protected int getChunksInFrame(T chunk);

    /** Override to add additional checks. */
    protected boolean isPartOfVideo(T chunk) {
        if (chunk.getSectorNumber() < _iVideoStartSector)
            return false;
        if (chunk.getSectorNumber() > _iVideoEndSector)
            return false;

        if (_iLastSeenFrameNumber >= 0 && getHeaderFrameNumber(chunk) < _iLastSeenFrameNumber)
            return false;

        if (_videoSectorType == null)
            _videoSectorType = chunk.getClass();
        else if (!_videoSectorType.equals(chunk.getClass()))
            return false;

        // FPS calculation can't handle huge breaks between frame sectors
        if (_iLastSeenSectorNumber >= 0 && chunk.getSectorNumber() > _iLastSeenSectorNumber + 100)
            return false;
        _iLastSeenSectorNumber = chunk.getSectorNumber();
        return true;
    }

    /** Override to add additional checks. */
    protected boolean isPartOfFrame(T chunk) {
        // different frame number
        if (_currentFrame != null && _currentFrame.getHeaderFrameNumber() != getHeaderFrameNumber(chunk))
            return false;
        // chunk already exists
        if (_aoCurrent != null &&
            chunk.getChunkNumber() < _aoCurrent.length &&
            _aoCurrent[chunk.getChunkNumber()] != null)
        {
            // log warning?
            return false;
        }

        return true;
    }

}
