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
import jpsxdec.sectors.IVideoSectorWithFrameNumber;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.LoggedFailure;


public class StrDemuxer {

    public static interface Listener {
        void frameComplete(@Nonnull DemuxedStrFrame frame) throws LoggedFailure;
    }

    public static class DemuxedStrFrame extends AbstractDemuxedStrFrame<IVideoSectorWithFrameNumber> {
        private final int _iFrameNumber;

        private DemuxedStrFrame(@Nonnull IVideoSectorWithFrameNumber firstChunk) {
            super(firstChunk);
            _iFrameNumber = firstChunk.getFrameNumber();
        }

        @Override
        protected boolean isPartOfFrame(@Nonnull IVideoSectorWithFrameNumber chunk) {
            return super.isPartOfFrame(chunk) &&
                   chunk.getFrameNumber() == _iFrameNumber;
        }

        public int getHeaderFrameNumber() {
            return _iFrameNumber;
        }
    }

    @CheckForNull
    private DemuxedStrFrame _currentFrame;
    @CheckForNull
    private Listener _listener;

    /** @return if sector is {@link IVideoSectorWithFrameNumber}. */
    public boolean feedSector(@CheckForNull IdentifiedSector idSector, 
                              @Nonnull ILocalizedLogger log) throws LoggedFailure
    {
        boolean blnAccepted;
        if (idSector instanceof IVideoSectorWithFrameNumber) {
            blnAccepted = true;
            IVideoSectorWithFrameNumber vidSector = (IVideoSectorWithFrameNumber) idSector;

            if (_currentFrame != null && !_currentFrame.addSector(vidSector, log))
                flush(log);
            if (_currentFrame == null)
                _currentFrame = new DemuxedStrFrame(vidSector);
            if (_currentFrame.isFrameComplete())
                flush(log);
        } else {
            blnAccepted = false;
        }
        // TODO: Check for EOF?
        // JPSXDEC-7 and JPSXDEC-9
        // Some videos may only be broken by the "EOF Marker" in the Sub Header: Sub Mode
        // However splitting at EOF breaks Alice & UmJammerLammy, so this can't
        // be done universally.
        // Unfortunately we don't want to put the EOF check in the Sectors
        // because we want to watch for EOF in any sector along the way
        //if (isEof(idSector))
        //    _listener.endVideo();
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

