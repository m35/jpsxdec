/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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
import java.util.logging.Logger;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  This is surprisingly more complicated that it seems. */
public class FrameDemuxer implements ISectorFrameDemuxer {

    private static final Logger log = Logger.getLogger(FrameDemuxer.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sectors outside of this range are ignored. */
    private final int _iVideoStartSector, _iVideoEndSector;
    /** Expected dimensions of the frame. */
    private final int _iWidth, _iHeight;

    private DemuxedStrFrame _current;

    private ICompletedFrameListener _listener;

    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sectors outside of the start and end sectors (inclusive) are simply ignored. */
    public FrameDemuxer(int iWidth, int iHeight, 
                        int iVideoStartSector, int iVideoEndSector)
    {
        if (iWidth < 1 || iHeight < 1)
            throw new IllegalArgumentException("Invalid dimensions " + iWidth + "x" + iHeight);
        if (iVideoStartSector < 0 || iVideoEndSector < iVideoStartSector)
            throw new IllegalArgumentException("Invalid video sector range " + iVideoStartSector + "-" + iVideoEndSector);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iVideoStartSector = iVideoStartSector;
        _iVideoEndSector = iVideoEndSector;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public void feedSector(IdentifiedSector sector) throws IOException {
        if (sector.getSectorNumber() < _iVideoStartSector ||
            sector.getSectorNumber() > _iVideoEndSector)
            return;

        if (!(sector instanceof IVideoSector))
            return;
        
        IVideoSector chunk = (IVideoSector) sector;

        if (chunk.getWidth() != _iWidth)
            throw new IllegalArgumentException("Inconsistent width.");

        if (chunk.getHeight() != _iHeight)
            throw new IllegalArgumentException("Inconsistent height.");

        // is it the first 
        if (_current == null) {
            _current = new DemuxedStrFrame(chunk);
        } else {
            boolean blnRejected = _current.addChunk(chunk);
            if (blnRejected) {
                _listener.frameComplete(_current);
                _current = new DemuxedStrFrame(chunk);
            }
        }

        if (_current.isFull()) {
            _listener.frameComplete(_current);
            _current = null;
        }

    }

    public void flush() throws IOException {
        if (_current != null) {
            _listener.frameComplete(_current);
            _current = null;
        }
    }

    public void setFrameListener(ICompletedFrameListener listener) {
        _listener = listener;
    }
    
}
