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

package jpsxdec.modules.psx.str;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.modules.DiscItemSaver;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.ProgressListener;
import jpsxdec.util.FeedbackStream;

public class STRVideoSaver extends DiscItemSaver {

    private static final Logger log = Logger.getLogger(STRVideoSaver.class.getName());

    private DiscItemSTRVideo _vidItem;
    private SectorMovieWriterBuilder _demuxBuilder;

    public STRVideoSaver(DiscItemSTRVideo vidStream) {
        super();
        _vidItem = vidStream;
        _demuxBuilder = new SectorMovieWriterBuilder(_vidItem);
    }

    public JPanel getOptionPane() {
        throw new UnsupportedOperationException("Removed until next version.");
    }

    //-----------------------------------

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        return _demuxBuilder.commandLineOptions(asArgs, fbs);
    }

    @Override
    public void printHelp(FeedbackStream fbs) {
        _demuxBuilder.printHelp(fbs);
    }

    @Override
    public void startSave(ProgressListener pl) throws IOException {
        boolean debug = false;
        if (debug) {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(getOptionPane(), BorderLayout.NORTH);
            frame.add(getOptionPane(), BorderLayout.SOUTH);
            frame.pack();
            frame.setVisible(true);

        } else {
            SectorMovieWriter movieWriter = _demuxBuilder.openMovieWriter();

            movieWriter.setListener(pl);

            // TODO: change to be movieWriter.saveAudio()
            if (_demuxBuilder.getSaveAudio()) {
                startVideoAndAudio(movieWriter, pl);
            } else {
                startVideoOnly(movieWriter, pl);
            }
        }
    }

    private void startVideoOnly(SectorMovieWriter movieWriter, ProgressListener pl)
            throws IOException
    {
        int iSector = movieWriter.getMovieStartSector();

        final double SECTOR_LENGTH = movieWriter.getMovieEndSector() - iSector + 1;

        int iCurrentFrame = movieWriter.getStartFrame();
        try {
            pl.progressStart("Writing " + movieWriter.getOutputFile());
            for (; iSector <= movieWriter.getMovieEndSector(); iSector++) {

                CDSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = _vidItem.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    IVideoSector vidSector = (IVideoSector) identifiedSector;
                    int iFrame = vidSector.getFrameNumber();
                    if (iFrame < movieWriter.getStartFrame())
                        continue;
                    else if (iFrame > movieWriter.getEndFrame())
                        break;
                    pl.event("Frame " + iCurrentFrame);

                    if (iFrame != iCurrentFrame) {
                        pl.progressUpdate((iSector - _vidItem.getStartSector()) / SECTOR_LENGTH);
                        iCurrentFrame = iFrame;
                    }

                    movieWriter.feedSectorForVideo(vidSector);
                }
            }
            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                movieWriter.close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

    private void startVideoAndAudio(SectorMovieWriter movieWriter, ProgressListener pl)
            throws IOException
    {
        final int iStartSector = movieWriter.getMovieStartSector();
        int iSector = iStartSector;
        final int iEndSector = movieWriter.getMovieEndSector();
        double SECTOR_LENGTH = iEndSector - iStartSector;
        
        try {
            pl.progressStart("Writing " + movieWriter.getOutputFile());

            int iFrame = movieWriter.getStartFrame();
            for (; iSector <= iEndSector; iSector++) {
                pl.event("Frame " + iFrame);

                CDSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = JPSXModule.identifyModuleSector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    if (_vidItem.getStartSector() <= iSector &&
                        iSector <= _vidItem.getEndSector()   &&
                        iFrame <= movieWriter.getEndFrame())
                    {
                        IVideoSector vidSector = (IVideoSector) identifiedSector;
                        movieWriter.feedSectorForVideo(vidSector);

                        iFrame = vidSector.getFrameNumber();
                    }
                } else if (identifiedSector != null) {
                    movieWriter.feedSectorForAudio(identifiedSector);
                }
                pl.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);
            }

            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                movieWriter.close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

}
