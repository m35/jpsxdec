/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import jpsxdec.i18n.I;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IncompatibleException;

/** Demuxes a series of frame chunk sectors into a solid stream.
 *  This is surprisingly more complicated that it seems. */
public class DemuxedStrFrame implements IDemuxedFrame {

    private static final Logger LOG = Logger.getLogger(DemuxedStrFrame.class.getName());

    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Dimensions of the frame. */
    private final int _iWidth, _iHeight;

    /** Frame number of the frame. */
    @Nonnull
    private final FrameNumber _frameNumber;

    @Nonnull
    private final IVideoSector[] _aoChunks;

    /** Size in bytes of the data contained in all the demux sectors. */
    private final int _iDemuxFrameSize;
    
    private final int _iMinSector;

    /** Basically the last sector of the frame, which we assume is when
     * the frame will be displayed. */
    private final int _iPresentationSector;

    /* ---------------------------------------------------------------------- */
    /* Constructors---------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Initialize the frame with an initial sector. */
    public DemuxedStrFrame(@Nonnull FrameNumber frameNumber, int iWidth, int iHeight,
                           @Nonnull IVideoSector[] aoChunks, int iChunksInFrame)
    {
        _frameNumber = frameNumber;
        _iWidth = iWidth;
        _iHeight = iHeight;
        _aoChunks = new IVideoSector[iChunksInFrame];
        int iMin = Integer.MAX_VALUE, iMax = 0, iSize = 0;
        for (int i = 0; i < aoChunks.length; i++) {
            IVideoSector chunk = aoChunks[i];
            if (chunk != null) {
                int iSector = chunk.getSectorNumber();
                if (iSector < iMin)
                    iMin = iSector;
                if (iSector > iMax)
                    iMax = iSector;
                iSize += chunk.getIdentifiedUserDataSize();
                _aoChunks[i] = chunk;
            }
        }
        if (iSize < 1)
            throw new IllegalArgumentException();
        _iDemuxFrameSize = iSize;
        _iMinSector = iMin;
        _iPresentationSector = iMax;
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

    public int getDemuxSize() {
        return _iDemuxFrameSize;
    }

    public int getPresentationSector() {
        return _iPresentationSector;
    }

    public FrameNumber getFrame() {
        return _frameNumber;
    }
    
    public int getChunksInFrame() {
        return _aoChunks.length;
    }

    /** Returns the video sector.
     * @throws ArrayIndexOutOfBoundsException if index is less-than 0 or
     *                                        greater-than {@link #getChunksInFrame()}. */
    public @CheckForNull IVideoSector getChunk(int i) {
        return _aoChunks[i];
    }


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
        for (int i=0; i < getChunksInFrame(); i++) {
            fbs.println(getChunk(i));
        }
    }

    public void writeToSectors(@Nonnull byte[] abNewDemux,
                               int iUsedSize, int iMdecCodeCount,
                               @Nonnull CdFileSectorReader cd,
                               @Nonnull FeedbackStream fbs)
            throws IOException, IncompatibleException
    {
        int iDemuxOfs = 0;
        for (int i = 0; i < getChunksInFrame(); i++) {
            IVideoSector vidSector = getChunk(i);
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
        return "STR "+_iWidth+"x"+_iHeight+" Frame "+_frameNumber+
               " Chunks "+_aoChunks.length+
               " DemuxSize "+_iDemuxFrameSize+" PresSect "+_iPresentationSector;
    }

    public int getStartSector() {
        return _iMinSector;
    }
    public int getEndSector() {
        return getPresentationSector();
    }

}
