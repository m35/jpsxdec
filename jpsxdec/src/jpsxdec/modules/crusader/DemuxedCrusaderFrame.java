/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2019  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.Fraction;

public class DemuxedCrusaderFrame implements IDemuxedFrame {

    private static final Logger LOG = Logger.getLogger(DemuxedCrusaderFrame.class.getName());
    
    private final int _iWidth, _iHeight;
    private final int _iHeaderFrameNumber;
    @Nonnull
    private final DemuxedData<CrusaderDemuxPiece> _demux;
    @CheckForNull
    private FrameNumber _frameNumber;
    /** The sector the frame should be presented.
     * This will be many sectors after where the frame was read. */
    private final int _iPresentationSector;

    public DemuxedCrusaderFrame(int iWidth, int iHeight,
                                int iHeaderFrameNumber,
                                @Nonnull DemuxedData<CrusaderDemuxPiece> demux,
                                int iPresentationSector)
    {
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iHeaderFrameNumber = iHeaderFrameNumber;
        _demux = demux;
        _iPresentationSector = iPresentationSector;
    }
    
    public @CheckForNull MdecInputStream getCustomFrameMdecStream() {
        return null;
    }

    public @Nonnull byte[] copyDemuxData() {
        return _demux.copyDemuxData();
    }

    public int getDemuxSize() {
        return _demux.getDemuxSize();
    }

    public int getStartSector() {
        return _demux.getStartSector();
    }

    public int getEndSector() {
        return _demux.getEndSector();
    }

    public @Nonnull FrameNumber getFrame() {
        if (_frameNumber == null)
            throw new IllegalStateException();
        return _frameNumber;
    }
    public void setFrame(@Nonnull FrameNumber fn) {
        _frameNumber = fn;
    }

    public int getHeaderFrameNumber() {
        return _iHeaderFrameNumber;
    }
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public @Nonnull Fraction getPresentationSector() {
        return new Fraction(_iPresentationSector);
    }

    public void printSectors(@Nonnull PrintStream ps) {
        boolean blnFirstPiece = true;
        for (Iterator<CrusaderDemuxPiece> iterator = _demux.iterator();
             iterator.hasNext();
             blnFirstPiece = false)
        {
            SectorCrusader vidSector = iterator.next().getSector();
            ps.print(vidSector);
            if (blnFirstPiece) {
                ps.print(" (start offset " + _demux.getStartDataOffset() + ")");
            } else if (!iterator.hasNext()) {
                ps.print(" (end offset " + _demux.getEndDataOffset() + ")");
            }
            ps.println();
        }
    }

    /**
     * @throws IllegalArgumentException
     *                  if {@code abNewDemux.length > } {@link #getDemuxSize()}
     */
    @Override
    public void writeToSectors(@Nonnull byte[] abNewDemux,
                               int iUsedSize_ignore, int iMdecCodeCount_ignore,
                               @Nonnull CdFileSectorReader cd,
                               @Nonnull ILocalizedLogger log)
             throws LoggedFailure, IllegalArgumentException
    {
        if (abNewDemux.length > getDemuxSize())
            throw new IllegalArgumentException(String.format(
                    "Frame %s: New frame size %d is larger than existing size %d",
                    _frameNumber, abNewDemux.length, getDemuxSize()));

        // not going to check that the bitstream is of any version


        int iDemuxOfs = 0;
        boolean blnFirstPiece = true;
        for (Iterator<CrusaderDemuxPiece> iterator = _demux.iterator();
             iterator.hasNext();
             blnFirstPiece = false)
        {
            SectorCrusader vidSector = iterator.next().getSector();

            if (vidSector == null) {
                log.log(Level.WARNING, I.CMD_FRAME_TO_REPLACE_MISSING_CHUNKS());
                continue;
            }
            int iBytesToCopy = SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE;
            int iCopyTo = SectorCrusader.HEADER_SIZE;
            if (blnFirstPiece) {
                iCopyTo += _demux.getStartDataOffset();
                iBytesToCopy -= _demux.getStartDataOffset();
            }
            if (iDemuxOfs + iBytesToCopy > abNewDemux.length)
                iBytesToCopy = abNewDemux.length - iDemuxOfs;

            if (iBytesToCopy == 0)
                break;

            try {
                cd.addPatch(vidSector.getSectorNumber(), iCopyTo, abNewDemux, iDemuxOfs, iBytesToCopy);
            } catch (DiscPatcher.WritePatchException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(cd.getTemporaryPatchFile().toString()), ex);
            }

            iDemuxOfs += iBytesToCopy;
        }
    }

    @Override
    public String toString() {
        return "Crusader "+_iWidth+"x"+_iHeight+" Frame "+_frameNumber+
               " Size "+getDemuxSize()+" PresSect "+_iPresentationSector;
    }
}
