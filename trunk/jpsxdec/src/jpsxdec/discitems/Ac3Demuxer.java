/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2016  Michael Sabin
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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.SectorAceCombat3Video;

/** Collects Ace Combat 3 sectors and generates frames. 
 * This could be considered an internal demuxer.
 * Listener must be set before using this class.
 *<p>
 * Ace Combat 3 demuxers need to only demux for a given channel.
 * To demux for another channel, create a separate demuxer.
 */
public class Ac3Demuxer {

    public static interface Listener {
        void frameComplete(@Nonnull DemuxedAc3Frame frame) throws IOException;
    }

    public static class DemuxedAc3Frame extends AbstractDemuxedStrFrame<SectorAceCombat3Video> {
        private final int _iInvFrameNumber;
        private final int _iChannel;

        private DemuxedAc3Frame(SectorAceCombat3Video firstChunk) {
            super(firstChunk);
            _iInvFrameNumber = firstChunk.getInvertedFrameNumber();
            _iChannel = firstChunk.getChannel();
        }

        @Override
        protected boolean isPartOfFrame(SectorAceCombat3Video chunk) {
            if (chunk.getChannel() != _iChannel)
                throw new IllegalArgumentException();
            return super.isPartOfFrame(chunk) &&
                    chunk.getInvertedFrameNumber() == _iInvFrameNumber;
        }

        public int getInvertedHeaderFrameNumber() {
            return _iInvFrameNumber;
        }

        public int getChannel() {
            return _iChannel;
        }

        @Override
        public String toString() {
            return super.toString() + " channel "+ _iChannel + " invframe " + _iInvFrameNumber;
        }        
    }

    private final int _iChannel;
    @CheckForNull
    private DemuxedAc3Frame _currentFrame;
    @CheckForNull
    private Listener _listener;

    public Ac3Demuxer(int iChannel) {
        _iChannel = iChannel;
    }

    /** @return if the sector was accepted as part of this video channel. */
    public boolean feedSector(@Nonnull IdentifiedSector idSector, @Nonnull Logger log) throws IOException {
        if (!(idSector instanceof SectorAceCombat3Video))
            return false;
        SectorAceCombat3Video vidSector = (SectorAceCombat3Video) idSector;
        if (vidSector.getChannel() != _iChannel)
            return false;

        if (_currentFrame != null && !_currentFrame.addSector(vidSector, log))
            flush(log);
        if (_currentFrame == null)
            _currentFrame = new DemuxedAc3Frame(vidSector);
        if (_currentFrame.isFrameComplete())
            flush(log);
        return true;
    }

    public void flush(@Nonnull Logger log) throws IOException {
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

