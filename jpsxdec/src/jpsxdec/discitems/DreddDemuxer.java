/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2017  Michael Sabin
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorDreddVideo;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.LoggedFailure;


/** Collects Judge Dredd sectors and generates frames.
 * This could be considered an internal demuxer.
 * Listener must be set before using this class. */
public class DreddDemuxer {

    public static interface Listener {
        void frameComplete(@Nonnull DemuxedDreddFrame frame) throws LoggedFailure;
        void endVideo();
    }


    public static class DemuxedDreddFrame extends AbstractDemuxedStrFrame<SectorDreddVideo> {
        private DemuxedDreddFrame(SectorDreddVideo firstChunk) {
            super(firstChunk);
        }
    }

    @CheckForNull
    private DemuxedDreddFrame _currentFrame;
    @CheckForNull
    private Listener _listener;

    /** @return if the sector is {@link SectorDreddVideo}. */
    public boolean feedSector(@Nonnull CdSector cdSector, 
                              @CheckForNull IdentifiedSector idSector,
                              @Nonnull ILocalizedLogger log) throws LoggedFailure
    {
        boolean blnAccepted;
        if (idSector instanceof SectorDreddVideo) {
            blnAccepted = true;
            SectorDreddVideo vidSector = (SectorDreddVideo) idSector;

            if (_currentFrame != null && !_currentFrame.addSector(vidSector, log))
                flush(log);
            if (_currentFrame == null)
                _currentFrame = new DemuxedDreddFrame(vidSector);
            if (_currentFrame.isFrameComplete())
                flush(log);
        } else {
            blnAccepted = false;
        }

        if (cdSector.hasSubHeader() &&
            cdSector.subModeMask(CdxaSubHeader.SubMode.MASK_EOF_MARKER) != 0)
        {
            if (_listener == null)
                throw new IllegalStateException("Frame listener must be set before feeding data");
            _listener.endVideo();
        }

        return blnAccepted;
    }

    public void flush(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_currentFrame == null)
            return;
        if (_listener == null)
            throw new IllegalStateException("Frame listener must be set before feeding data");
        _listener.frameComplete(_currentFrame);
        _currentFrame = null;
    }

    public void setFrameListener(@Nonnull Listener listener) {
        _listener = listener;
    }

}

