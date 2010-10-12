/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.util.ProgressListener;

/** Superclass of all the {@link VideoSavers}.
 * Takes care of the sector reading and passing to the subclass handling. */
public abstract class VideoSaver implements IDiscItemSaver {

    private static final Logger log = Logger.getLogger(VideoSaver.class.getName());

    private final DiscItemVideoStream _vidItem;
    private final DiscItemAudioStream _parallelAudio;
    private ProgressListener _progress;

    public VideoSaver(VideoSaverBuilderSnapshot snap) {
        super();
        _vidItem = snap.videoItem;
        _parallelAudio = snap.parallelAudio;
    }

    //-----------------------------------

    public void startSave(ProgressListener pl) throws IOException {

        _progress = pl;

        if (_parallelAudio != null) {
            startVideoAndAudio(pl);
        } else {
            startVideoOnly(pl);
        }
    }

    private void startVideoOnly(ProgressListener pl)
            throws IOException
    {
        int iSector = getMovieStartSector();

        final double SECTOR_LENGTH = getMovieEndSector() - iSector + 1;

        int iCurrentFrame = getStartFrame();
        try {
            for (; iSector <= getMovieEndSector(); iSector++) {

                CdSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = _vidItem.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    IVideoSector vidSector = (IVideoSector) identifiedSector;
                    int iFrame = vidSector.getFrameNumber();
                    if (iFrame < getStartFrame())
                        continue;
                    else if (iFrame > getEndFrame())
                        break;
                    pl.event("Frame " + iCurrentFrame);

                    if (iFrame != iCurrentFrame) {
                        pl.progressUpdate((iSector - _vidItem.getStartSector()) / SECTOR_LENGTH);
                        iCurrentFrame = iFrame;
                    }

                    feedSectorForVideo(vidSector);
                }
            }
            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

    private void startVideoAndAudio(ProgressListener pl)
            throws IOException
    {
        final int iStartSector = getMovieStartSector();
        int iSector = iStartSector;
        final int iEndSector = getMovieEndSector();
        double SECTOR_LENGTH = iEndSector - iStartSector;

        try {
            int iFrame = getStartFrame();
            for (; iSector <= iEndSector; iSector++) {
                pl.event("Frame " + iFrame);

                CdSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = IdentifiedSector.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    if (_vidItem.getStartSector() <= iSector &&
                        iSector <= _vidItem.getEndSector()   &&
                        iFrame <= getEndFrame())
                    {
                        IVideoSector vidSector = (IVideoSector) identifiedSector;
                        feedSectorForVideo(vidSector);

                        iFrame = vidSector.getFrameNumber();
                    }
                } else if (identifiedSector != null) {
                    feedSectorForAudio(identifiedSector);
                }
                pl.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);
            }

            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

    protected ProgressListener getListener() {
        return _progress;
    }


    abstract public void close() throws IOException;

    abstract public void feedSectorForVideo(IVideoSector sector) throws IOException;

    abstract public void feedSectorForAudio(IdentifiedSector sector) throws IOException;

    abstract public int getStartFrame();

    abstract public int getEndFrame();

    abstract public int getMovieStartSector();

    abstract public int getMovieEndSector();

}
