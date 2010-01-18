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

package jpsxdec.plugins.psx.str;

import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.xa.IDiscItemAudioSectorDecoder;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.util.FeedbackStream;

public class STRVideoSaver extends DiscItemSaver {

    private static final Logger log = Logger.getLogger(STRVideoSaver.class.getName());

    private DiscItemSTRVideo _vidItem;
    private DemuxMovieWriterBuilder _demuxBuilder;

    public STRVideoSaver(DiscItemSTRVideo vidStream) {
        super();
        _vidItem = vidStream;
        _demuxBuilder = new DemuxMovieWriterBuilder(_vidItem);
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
            DemuxMovieWriter oMovieWriter = _demuxBuilder.createDemuxWriter();

            oMovieWriter.setListener(pl);

            oMovieWriter.open();

            if (_demuxBuilder.getSaveAudio()) {
                startVideoAndAudio(oMovieWriter, pl);
            } else {
                startVideoOnly(oMovieWriter, pl);
            }
        }
    }

    private void startVideoOnly(DemuxMovieWriter movieWriter, ProgressListener pl)
            throws IOException
    {
        final int iStartSector;
        if (movieWriter.getStartFrame() != _vidItem.getStartFrame()) {
            // find sector that starts the frame
            iStartSector = _vidItem.seek(movieWriter.getStartFrame()).getSectorNumber();
        } else {
            iStartSector = _vidItem.getStartSector();
        }
        int iSector = iStartSector;

        int iFrame = movieWriter.getStartFrame();
        StrFramePushDemuxer demuxer = null;
        try {
            final double SECTOR_LENGTH = _vidItem.getEndSector() - iStartSector + 1;
            pl.progressStart("Writing " + movieWriter.getOutputFile());
            for (; iSector <= _vidItem.getEndSector(); iSector++) {
                pl.event("Frame " + iFrame);
                CDSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = _vidItem.identifySector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    IVideoSector vidSector = (IVideoSector) identifiedSector;
                    int iSectFrame = vidSector.getFrameNumber();
                    if (iSectFrame != iFrame) {
                        if (iSectFrame > movieWriter.getEndFrame()) {
                            break;
                        } else {
                            pl.progressUpdate((iSector - _vidItem.getStartSector()) / SECTOR_LENGTH);
                            iFrame = vidSector.getFrameNumber();
                        }
                    }
                    demuxer = addToDemux(movieWriter, demuxer, vidSector, iSector - iStartSector);
                }
            }
            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                if (demuxer != null && !demuxer.isEmpty()) {
                    movieWriter.writeFrame(demuxer.getDemuxFrame(), iSector - iStartSector);
                }
                movieWriter.close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

    /** Adds a video sector to a frame demuxer. It turns out to be more
     * complicated than you'd think. */
    private static StrFramePushDemuxer addToDemux(DemuxMovieWriter movieWriter,
                                                  StrFramePushDemuxer demuxer,
                                                  IVideoSector vidSector,
                                                  int iSectorsFromStart)
            throws IOException
    {
        if (demuxer == null) {
            // create the demuxer for the sector's frame
            demuxer = new StrFramePushDemuxer(vidSector.getFrameNumber());
        }
        if (demuxer.getFrameNumber() == vidSector.getFrameNumber()) {
            // add the sector if it is the same frame number
            demuxer.addChunk(vidSector);
        } else {
            // if sector has a different frame number, close off the demuxer
            DemuxImage demuxFrame = demuxer.getDemuxFrame();
            // create a new one with this new sector
            demuxer = new StrFramePushDemuxer();
            demuxer.addChunk(vidSector);
            // and send the finished frame thru the pipe
            // (wanted to wait in case of an error)
            movieWriter.writeFrame(demuxFrame, iSectorsFromStart);
        }
        if (demuxer.isFull()) {
            // send the image thru the pipe if it is complete
            DemuxImage demuxFrame = demuxer.getDemuxFrame();
            demuxer = null;
            movieWriter.writeFrame(demuxFrame, iSectorsFromStart);
        }
        return demuxer;
    }

    private void startVideoAndAudio(DemuxMovieWriter movieWriter, ProgressListener pl)
            throws IOException
    {
        IDiscItemAudioSectorDecoder audWriter = movieWriter.getAudioSectorDecoder();

        final int iStartSector = Math.min(_vidItem.getStartSector(), audWriter.getStartSector());
        int iSector = iStartSector;
        int iEndSector = Math.max(_vidItem.getEndSector(), audWriter.getEndSector());
        double SECTOR_LENGTH = iEndSector - iStartSector;
        
        StrFramePushDemuxer demuxer = null;
        try {
            pl.progressStart("Writing " + movieWriter.getOutputFile());

            int iFrame = movieWriter.getStartFrame();
            demuxer = new StrFramePushDemuxer(iFrame);
            for (; iSector <= iEndSector; iSector++) {
                pl.event("Frame " + iFrame);
                // TODO: fix this logic like above
                CDSector cdSector = _vidItem.getSourceCD().getSector(iSector);
                IdentifiedSector identifiedSector = JPSXPlugin.identifyPluginSector(cdSector);
                if (identifiedSector instanceof IVideoSector) {
                    if (_vidItem.getStartSector() <= iSector &&
                        iSector <= _vidItem.getEndSector()   &&
                        iFrame <= movieWriter.getEndFrame())
                    {
                        IVideoSector vidSector = (IVideoSector) identifiedSector;
                        demuxer = addToDemux(movieWriter, demuxer, vidSector, iSector - iStartSector);
                        iFrame = vidSector.getFrameNumber();
                    }
                } else if (identifiedSector != null) {
                    audWriter.feedSector(identifiedSector);
                }
                pl.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);
            }

            pl.progressEnd();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "", ex);
            pl.error(ex);
        } finally {
            try {
                if (demuxer != null && !demuxer.isEmpty()) {
                    movieWriter.writeFrame(demuxer.getDemuxFrame(), iSector - iStartSector);
                }
                movieWriter.close();
            } catch (Throwable ex) {
                log.log(Level.SEVERE, "", ex);
                pl.error(ex);
            }
        }
    }

}
