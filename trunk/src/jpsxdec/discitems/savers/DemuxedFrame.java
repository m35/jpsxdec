/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.discitems.savers;

import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.FeedbackStream;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  This is surprisingly more complicated that it seems. */
public class DemuxedFrame {

    private static final Logger log = Logger.getLogger(DemuxedFrame.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Dimensions of the frame. */
    private final int _iWidth, _iHeight;

    /** Frame number of the frame. */
    private final int _iFrame;

    private IVideoSector[] _aoChunks;

    private int _iChunksReceived;

    /** Size in bytes of the data contained in all the demux sectors. */
    private int _iDemuxFrameSize;

    /** Basically the last sector of the frame, which we assume is when
     * the frame will be displayed. */
    private int _iPresentationSector;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Initialize the frame with an initial sector. */
    DemuxedFrame(IVideoSector firstChunk)
    {
        _iFrame = firstChunk.getFrameNumber();
        _iWidth = firstChunk.getWidth();
        _iHeight = firstChunk.getHeight();

        _aoChunks = new IVideoSector[firstChunk.getChunksInFrame()];

        // save the chunk
        _aoChunks[firstChunk.getChunkNumber()] = firstChunk;
        // add the sector's data size to the total
        _iDemuxFrameSize = firstChunk.getIdentifiedUserDataSize();
        // set the presentation sector to the only sector we have so far
        _iPresentationSector = firstChunk.getSectorNumber();

        _iChunksReceived = 1;

    }

    boolean addChunk(IVideoSector chunk) {
        if (chunk.getFrameNumber() != _iFrame || isFull())
            return true;

        // for easy reference
        final int iChkNum = chunk.getChunkNumber();

        if (chunk.getChunksInFrame() != _aoChunks.length) {
            // if the number of chunks in the frame suddenly changed
            throw new IllegalArgumentException("Number of chunks in this frame changed from " +
                          _aoChunks.length + " to " + chunk.getChunksInFrame());
        } else if (iChkNum < 0 || iChkNum >= _aoChunks.length) {
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

        _iChunksReceived++;
        
        return false;
    }

    boolean isFull() {
        return _iChunksReceived == _aoChunks.length;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    /** Size of the demuxed frame. */
    public int getDemuxSize() {
        return _iDemuxFrameSize;
    }

    /** The last sector of the frame. */
    public int getPresentationSector() {
        return _iPresentationSector;
    }

    /** The frame number of the demuxed frame. */
    public int getFrame() {
        return _iFrame;
    }
    
    public int getChunksInFrame() {
        return _aoChunks.length;
    }

    /** Returns the video sector.
     * @throws ArrayIndexOutOfBoundsException if index is less-than 0 or
     *                                        greater-than {@link #getChunksInFrame()}. */
    public IVideoSector getChunk(int i) {
        return _aoChunks[i];
    }


    /** Returns the contiguous demux copied into a buffer. If the supplied
     * buffer is not null and is big enough to fit the demuxed data, it is used,
     * otherwise a new buffer is created and returned.
     * @param abBuffer Optional buffer to copy the demuxed data into. */
    public byte[] copyDemuxData(byte[] abBuffer) {
        if (abBuffer == null || abBuffer.length < getDemuxSize())
            abBuffer = new byte[getDemuxSize()];

        int iPos = 0;
        for (int iChunk = 0; iChunk < _aoChunks.length; iChunk++) {
            IVideoSector chunk = _aoChunks[iChunk];
            if (chunk != null) {
                chunk.copyIdentifiedUserData(abBuffer, iPos);
                iPos += chunk.getIdentifiedUserDataSize();
            } else {
                log.warning("Frame " + _iFrame + " chunk " + iChunk + " missing.");
            }
        }
        return abBuffer;
    }


    public void printStats(FeedbackStream fbs) {
        try {
            ParsedMdecImage parsed = new ParsedMdecImage(getWidth(), getHeight());
            byte[] abBitStream = new byte[getDemuxSize()];
            copyDemuxData(abBitStream);
            BitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(abBitStream);
            uncompressor.reset(abBitStream);
            parsed.readFrom(uncompressor);
            fbs.indent();
            fbs.printlnMore("Bitstream info: " + uncompressor);
            fbs.printlnMore("Available demux size: " + getDemuxSize());
            for (int i=0; i < getChunksInFrame(); i++) {
                System.out.println(getChunk(i));
            }
            fbs.outdent();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void writeToSectors(byte[] abNewDemux,
                               int iUsedSize, int iMdecCodeCount,
                               CdFileSectorReader cd, FeedbackStream fbs)
            throws IOException
    {
        int iDemuxOfs = 0;
        for (int i = 0; i < getChunksInFrame(); i++) {
            IVideoSector vidSector = getChunk(i);
            if (vidSector == null) {
                fbs.printlnWarn("Trying to replace a frame with missing chunks??");
                continue;
            }

            byte[] abSectUserData = vidSector.getCDSector().getCdUserDataCopy();
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



}
