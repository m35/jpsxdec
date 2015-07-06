/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2015  Michael Sabin
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

package jpsxdec.indexing;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumberFormat;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.indexing.psxvideofps.StrFrameRateCalc;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.Fraction;

/** Tracks a single video stream. */
public abstract class AbstractVideoStreamIndex<T extends DiscItemVideoStream> 
        implements ISectorFrameDemuxer.ICompletedFrameListener
{
    
    private static final Logger LOG = Logger.getLogger(AbstractVideoStreamIndex.class.getName());

    @Nonnull
    protected final CdFileSectorReader _cd;
    @Nonnull
    protected final Logger _errLog;
    private final int _iStartSector;
    @Nonnull
    private final ISectorFrameDemuxer _demuxer;
    private final boolean _blnCalcFps;

    private int _iEndSector;
    private int _iFrame1PresentationSector = -1;
    @CheckForNull
    private FrameNumber _startFrame = null;
    @CheckForNull
    private FrameNumber _lastSeenFrameNumber = null;
    private int _iFrameCount = 0;
    @CheckForNull
    private StrFrameRateCalc _fpsCalc;

    private final FrameNumberFormat.Builder _frameNumFormatBldr = new FrameNumberFormat.Builder();

    public AbstractVideoStreamIndex(@Nonnull CdFileSectorReader cd, @Nonnull Logger errLog,
                                    @Nonnull IdentifiedSector vidSect, boolean blnCalcFps,
                                    @Nonnull ISectorFrameDemuxer demuxer)
    {
        _cd = cd;
        _errLog = errLog;
        _iEndSector = _iStartSector = vidSect.getSectorNumber();
        _blnCalcFps = blnCalcFps;
        
        _demuxer = demuxer;
        _demuxer.setFrameListener(this);

        try {
            if (!_demuxer.feedSector(vidSect, _errLog))
                throw new RuntimeException("Why wasn't the sector accepted?");
        } catch (IOException ex) {
            // we know where the completed frames are going
            // so this should never happen
            throw new RuntimeException("Should never happen", ex);
        }
    }

    public boolean sectorRead(@Nonnull IdentifiedSector vidSect) {

        try {
            if (!_demuxer.feedSector(vidSect, _errLog))
                return false;
        } catch (IOException ex) {
            // we know where the completed frames are going
            // so this should never happen
            throw new RuntimeException("Should never happen", ex);
        }

        return true;
    }

    public void frameComplete(@Nonnull IDemuxedFrame frame) {

        if (_iFrame1PresentationSector < 0)
            _iFrame1PresentationSector = frame.getPresentationSector() - _iStartSector;
        if (frame.getFrame().getHeaderFrameDuplicate() > 0) {
            I.DUP_HDR_FRM_NUM(frame.getFrame().getHeaderFrameNumber()).log(_errLog, Level.INFO);
        }
        if (_blnCalcFps) {
            if (_fpsCalc == null) {
                _fpsCalc = new StrFrameRateCalc(frame.getStartSector() - _iStartSector,
                                                frame.getEndSector() - _iStartSector);
            } else {
                _fpsCalc.nextVideo(frame.getStartSector() - _iStartSector,
                                   frame.getEndSector() - _iStartSector);
            }
        }

        _iEndSector = frame.getEndSector();
        _lastSeenFrameNumber = frame.getFrame();
        if (_startFrame == null)
            _startFrame = _lastSeenFrameNumber;

        _frameNumFormatBldr.addFrame(_lastSeenFrameNumber);
        _iFrameCount++;
    }

    abstract protected @Nonnull T createVideo(
                    int iStartSector, int iEndSector,
                    int iWidth, int iHeight,
                    int iFrameCount,
                    @Nonnull FrameNumberFormat frameNumberFormat,
                    @Nonnull FrameNumber startFrame,
                    @Nonnull FrameNumber lastSeenFrameNumber,
                    int iSectors, int iPerFrame,
                    int iFrame1PresentationSector);

    public @CheckForNull T endOfMovie() {
        try {
            _demuxer.flush(_errLog);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        if (_iFrameCount < 1)
            return null;

        Fraction sectorsPerFrame;
        int iSectors, iPerFrame;
        if (_fpsCalc != null && (sectorsPerFrame = _fpsCalc.getSectorsPerFrame()) != null) {
            if (LOG.isLoggable(Level.INFO))
                LOG.info(_fpsCalc.toString());
            iSectors  = (int) sectorsPerFrame.getNumerator();
            iPerFrame = (int) sectorsPerFrame.getDenominator();
        } else {
            iSectors  = _iEndSector - _iStartSector + 1;
            iPerFrame = _iFrameCount;
        }

        // _startFrame and _lastSeenFrameNumber != null because _iFrameCount > 0
        return createVideo(_iStartSector, _iEndSector,
                           _demuxer.getWidth(), _demuxer.getHeight(),
                           _iFrameCount,
                           _frameNumFormatBldr.getFormat(),
                           _startFrame, _lastSeenFrameNumber,
                           iSectors, iPerFrame,
                           _iFrame1PresentationSector);
    }
}
