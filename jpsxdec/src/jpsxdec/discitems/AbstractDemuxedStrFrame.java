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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IncompatibleException;

/** Building and consuming sector-based frames.
 * Intended to be used in two phases:
 * The building/constructing phase, and the consuming phase.
 * Some functions are only intended to be used during construction.
 * Consumers of this frame should not call the construction functions. */
public abstract class AbstractDemuxedStrFrame<T extends IVideoSector> implements IDemuxedFrame {

    private static final Logger LOG = Logger.getLogger(AbstractDemuxedStrFrame.class.getName());

    @Nonnull
    private T[] _aoChunks;
    private int _iChunksReceived;
    private int _iExpectedChunks;
    private int _iDemuxFrameSize;

    private final int _iWidth, _iHeight;
    private int _iStartSector;
    private int _iEndSector;
    private int _iLastSectorReceived;

    @CheckForNull
    private FrameNumber _frameNumber;

    // -------------------------------------------------------------------------
    // Buidling functions

    /** Start building the frame with the first sector. [BUILDING] */
    public AbstractDemuxedStrFrame(@Nonnull T firstChunk) {
        _iWidth = firstChunk.getWidth();
        _iHeight = firstChunk.getHeight();
        _iExpectedChunks = firstChunk.getChunksInFrame();
        if (firstChunk.getChunkNumber() > _iExpectedChunks) {
            // warn
            _iExpectedChunks = firstChunk.getChunkNumber();
        }
        _aoChunks = (T[]) new IVideoSector[_iExpectedChunks];
        _aoChunks[firstChunk.getChunkNumber()] = firstChunk;
        _iChunksReceived = 1;
        _iStartSector = _iEndSector = _iLastSectorReceived = firstChunk.getSectorNumber();
        _iDemuxFrameSize = firstChunk.getIdentifiedUserDataSize();
    }

    /** Adds another sector to this frame. [BUILDING]
     * @return if the sector was accepted by this frame. */
    public boolean addSector(@Nonnull T chunk, @Nonnull Logger log) {
        if (!isPartOfFrame(chunk))
            return false;

        int iExpectedCount = chunk.getChunksInFrame();
        int iChunkNumber = chunk.getChunkNumber();
        if (_iExpectedChunks != iExpectedCount) {
            I.DEMUX_FRAME_CHUNKS_CHANGED_FROM_TO(chunk.getSectorNumber(), _iExpectedChunks, iExpectedCount)
                    .log(log, Level.WARNING);
            _iExpectedChunks = iExpectedCount;
        }
        if (iChunkNumber >= iExpectedCount)
            I.DEMUX_CHUNK_NUM_GTE_CHUNKS_IN_FRAME(chunk.getSectorNumber(), iChunkNumber, iExpectedCount)
                    .log(log, Level.WARNING);

        // now add the chunk
        if (_aoChunks.length < iChunkNumber) {
            T[] ao = (T[]) new IVideoSector[iChunkNumber];
            System.arraycopy(_aoChunks, 0, ao, 0, _aoChunks.length);
            _aoChunks = ao;
        }
        _aoChunks[iChunkNumber] = chunk;
        _iChunksReceived++;

        int iSector = chunk.getSectorNumber();
        _aoChunks[iChunkNumber] = chunk;
        if (iSector < _iStartSector)
            _iStartSector = iSector;
        if (iSector > _iEndSector)
            _iEndSector = iSector;
        _iLastSectorReceived = iSector;
        _iDemuxFrameSize += chunk.getIdentifiedUserDataSize();

        return true;
    }

    /** Override this to add additional logic. [BUILDING] */
    protected boolean isPartOfFrame(@Nonnull T chunk) {
        return chunk.getWidth() == _iWidth &&
               chunk.getHeight() == _iHeight &&
               chunk.getSectorNumber() < _iLastSectorReceived + 50 &&
               _aoChunks[chunk.getChunkNumber()] == null;
    }

    /** [BUILDING] */
    public boolean isFrameComplete() {
        return _iChunksReceived >= _iExpectedChunks;
    }

    /** Sets the frame during its construction. [BUILDING]
     * Must be called before {@link #getFrame()} is called. */
    void setFrame(@Nonnull FrameNumber frameNumber) {
        _frameNumber = frameNumber;
    }

    //--------------------------------------------------------------------------
    // Consuming functions

    public int getWidth() { return _iWidth; }
    public int getHeight() { return _iHeight; }
    public int getStartSector() { return _iStartSector; }
    public int getEndSector() { return _iEndSector; }
    public int getPresentationSector() { return getEndSector(); }

    public @Nonnull FrameNumber getFrame() {
        if (_frameNumber == null)
            throw new IllegalStateException();
        return _frameNumber;
    }

    public int getDemuxSize() { return _iDemuxFrameSize; }
    public @Nonnull byte[] copyDemuxData(@CheckForNull byte[] abBuffer) {
        if (abBuffer == null || abBuffer.length < getDemuxSize())
            abBuffer = new byte[getDemuxSize()];

        int iPos = 0;
        for (int iChunk = 0; iChunk < _aoChunks.length; iChunk++) {
            IVideoSector chunk = _aoChunks[iChunk];
            if (chunk != null) {
                chunk.copyIdentifiedUserData(abBuffer, iPos);
                iPos += chunk.getIdentifiedUserDataSize();
            } else {
                LOG.log(Level.WARNING, "Frame {0} chunk {1,number,#} missing.", new Object[]{_frameNumber, iChunk});
            }
        }
        return abBuffer;
    }

    public void printSectors(@Nonnull FeedbackStream fbs) {
        for (T vidSector : _aoChunks) {
            fbs.println(vidSector);
        }
    }

    public void writeToSectors(@Nonnull byte[] abNewDemux,
                               int iUsedSize, int iMdecCodeCount,
                               @Nonnull CdFileSectorReader cd,
                               @Nonnull FeedbackStream fbs)
            throws IOException, IncompatibleException
    {
        int iDemuxOfs = 0;
        for (T vidSector : _aoChunks) {
            if (vidSector == null) {
                fbs.printlnWarn(I.CMD_FRAME_TO_REPLACE_MISSING_CHUNKS());
                continue;
            }
            byte[] abSectUserData = vidSector.getCdSector().getCdUserDataCopy();
            int iSectorHeaderSize = vidSector.checkAndPrepBitstreamForReplace(abNewDemux,
                    iUsedSize, iMdecCodeCount, abSectUserData);
            int iBytesToCopy = vidSector.getIdentifiedUserDataSize();
            if (iDemuxOfs + iBytesToCopy > abNewDemux.length)
                iBytesToCopy = abNewDemux.length - iDemuxOfs;
            // bytes to copy might be 0, which is ok because we
            // still need to write the updated headers
            System.arraycopy(abNewDemux, iDemuxOfs, abSectUserData, iSectorHeaderSize, iBytesToCopy);
            cd.writeSector(vidSector.getSectorNumber(), abSectUserData);
            iDemuxOfs += iBytesToCopy;
        }
    }

    @Override
    public String toString() {
        return String.format("Sectors %d-%d %dx%d %d chunks %d bytes",
                _iStartSector, _iEndSector,
                _iWidth, _iHeight, _aoChunks.length,
               _iDemuxFrameSize);
    }

    public int getChunkCount() {
        return _iChunksReceived;
    }
}
