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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.savers.VideoSaverBuilder.VideoFormat;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.Fraction;
import jpsxdec.util.ProgressListener;
import jpsxdec.util.TaskCanceledException;

/** Superclass of all the {@link VideoSavers}.
 * Takes care of the sector reading and passing to the subclass handling. */
public abstract class VideoSaver implements IDiscItemSaver {

    private static final Logger log = Logger.getLogger(VideoSaver.class.getName());

    private ProgressListener _progress;
    protected File _directory;
    protected final VideoSaverBuilderSnapshot _snap;

    public VideoSaver(VideoSaverBuilderSnapshot snap) {
        super();
        _snap = snap;
    }

    public String getInput() {
        return _snap.videoItem.getIndexId().serialize();
    }

    public String getOutputSummary() {
        String sStart = _snap.videoFormat.formatPostfix(_snap.videoItem, _snap.saveStartFrame);
        String sEnd = _snap.videoFormat.formatPostfix(_snap.videoItem, _snap.saveEndFrame);
        if (sStart.equals(sEnd)) {
            return _snap.baseName + sStart;
        } else {
            return _snap.baseName + sStart + " - " + _snap.baseName + sEnd;
        }
    }

    final public File getOutputFile(int i) {
        if (i >= getOutputFileCount())
            throw new IllegalArgumentException();
        return new File(_snap.baseName + _snap.videoFormat.formatPostfix(_snap.videoItem, _snap.saveStartFrame + i));
    }

    final public int getOutputFileCount() {
        String sStart = _snap.videoFormat.formatPostfix(_snap.videoItem, _snap.saveStartFrame);
        String sEnd = _snap.videoFormat.formatPostfix(_snap.videoItem, _snap.saveEndFrame);
        if (sStart.equals(sEnd)) {
            return 1;
        } else {
            return _snap.saveEndFrame - _snap.saveStartFrame + 1;
        }
    }

    public Fraction getFps() {
        return Fraction.divide(
                _snap.singleSpeed ? 75 : 150,
                _snap.videoItem.getSectorsPerFrame());
    }

    // TODO: clean this up using the subclasses
    public void printSelectedOptions(PrintStream ps) {
        ps.format("Disc speed: %s (%s fps)", _snap.singleSpeed ? "1x" : "2x", DiscItemVideoStream.formatFps(getFps()));
        ps.println();
        ps.println("Video format: " + _snap.videoFormat);
        if (_snap.videoFormat.hasCompression())
            ps.println("JPG quality: " + Math.round(_snap.jpgCompression*100) + "%");
        ps.println("Frames: " + _snap.saveStartFrame + "-" + _snap.saveEndFrame);
        if (_snap.videoItem.shouldBeCropped() &&
            _snap.videoFormat != VideoFormat.IMGSEQ_DEMUX &&
            _snap.videoFormat != VideoFormat.IMGSEQ_MDEC)
        {
            ps.println("Cropping: " + (_snap.crop ? "Yes" : "No"));
        }
        /*
        if (getPreciseFrameTiming_enabled())
            ps.println("Precise FPS: " + (_snap.preciseFrameTiming ? "Yes" : "No"));
        */
        if (_snap.videoFormat != VideoFormat.IMGSEQ_DEMUX &&
            _snap.videoFormat != VideoFormat.IMGSEQ_MDEC)
        {
            ps.println("Decode quality: " + _snap.decodeQuality);
        }
        if (_snap.audioDecoder != null) {
            ps.println("With audio item(s):");
            for (DiscItemAudioStream item : _snap.audioDecoder.getSourceItems()) {
                ps.println(item);
            }
            ps.println("Precise audio/video sync: " + (_snap.preciseAvSync ? "Yes" : "No"));
        }

        String sStartFile = _snap.videoFormat.formatPostfix(_snap.videoItem, getStartFrame());
        String sEndFile = _snap.videoFormat.formatPostfix(_snap.videoItem, getEndFrame());
        if (sStartFile.equals(sEndFile)) {
            ps.println("Saving as: " + _snap.baseName + sStartFile);
        } else {
            ps.println("Saving as: " + _snap.baseName + sStartFile + " - " + _snap.baseName + sEndFile);
        }
    }


    public void startSave(ProgressListener pl, File dir) throws IOException, TaskCanceledException {
        try {
            _progress = pl;
            _directory = dir;

            initialize(); // tell the children to setup

            if (_snap.audioDecoder != null) {
                startVideoAndAudio(pl);
            } else {
                startVideoOnly(pl);
            }
        } finally {
            _progress = null;
        }
    }

    private void startVideoOnly(ProgressListener pl)
            throws IOException, TaskCanceledException
    {
        int iSector = getMovieStartSector();

        final double SECTOR_LENGTH = getMovieEndSector() - iSector + 1;

        int iCurrentFrame = getStartFrame();
        try {
            for (; iSector <= getMovieEndSector(); iSector++) {

                CdSector cdSector = _snap.videoItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = _snap.videoItem.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    IVideoSector vidSector = (IVideoSector) identifiedSector;
                    int iSectorFrame = vidSector.getFrameNumber();
                    if (iSectorFrame < getStartFrame())
                        continue;
                    else if (iSectorFrame > getEndFrame())
                        break;
                    
                    if (pl.seekingEvent())
                        pl.event("Frame " + iCurrentFrame);

                    if (iSectorFrame != iCurrentFrame) {
                        pl.progressUpdate((iSector - _snap.videoItem.getStartSector()) / SECTOR_LENGTH);
                        iCurrentFrame = iSectorFrame;
                    }

                    feedSectorForVideo(vidSector);
                }
            }
            pl.progressEnd();
        } finally {
            try {
                close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "Error closing saving process", ex);
                pl.getLog().log(Level.SEVERE, "Error closing saving process", ex);
            }
        }
    }

    private void startVideoAndAudio(ProgressListener pl)
            throws IOException, TaskCanceledException
    {
        final int iStartSector = getMovieStartSector();
        int iSector = iStartSector;
        final int iEndSector = getMovieEndSector();
        double SECTOR_LENGTH = iEndSector - iStartSector;

        try {
            int iCurrentFrame = getStartFrame();
            for (; iSector <= iEndSector; iSector++) {
                
                if (pl.seekingEvent())
                    pl.event("Frame " + iCurrentFrame);

                CdSector cdSector = _snap.videoItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = IdentifiedSector.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    if (_snap.videoItem.getStartSector() <= iSector &&
                        iSector <= _snap.videoItem.getEndSector()   &&
                        iCurrentFrame <= getEndFrame())
                    {
                        IVideoSector vidSector = (IVideoSector) identifiedSector;
                        feedSectorForVideo(vidSector);

                        iCurrentFrame = vidSector.getFrameNumber();
                    }
                } else if (identifiedSector != null) {
                    feedSectorForAudio(identifiedSector);
                }
                pl.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);
            }

            pl.progressEnd();
        } finally {
            try {
                close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "Error closing saving process", ex);
                pl.getLog().log(Level.SEVERE, "Error closing saving process", ex);
            }
        }
    }

    protected ProgressListener getListener() {
        return _progress;
    }

    abstract protected void initialize() throws IOException;

    abstract protected void close() throws IOException;

    abstract protected void feedSectorForVideo(IVideoSector sector) throws IOException;

    abstract protected void feedSectorForAudio(IdentifiedSector sector) throws IOException;

    abstract public int getStartFrame();

    abstract public int getEndFrame();

    abstract public int getMovieStartSector();

    abstract public int getMovieEndSector();

}
