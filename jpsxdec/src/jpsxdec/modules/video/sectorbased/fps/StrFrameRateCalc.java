/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased.fps;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;

/** Functions and classes to calculate the frame rate of standard STR movies.
 *
 * <h3>THE BASIC IDEA</h3>
 *
 * STR movies can be played back at single speed (75 sectors/second) or double
 * speed (150 sectors/second).
 * <p>
 * Each video frame spans a certain number of sectors. The number of
 * sectors it spans, and how fast the movie is played, will determine
 * how long it takes to read a whole frame.
 * <p>
 * I assume the prior frame is shown on the screen while the current frame is
 * being read, so we can know the prior frame's on-screen duration by looking
 * at how long it takes to read the current frame (this is of course assuming a
 * frame is displayed just after it is read).
 * <p>
 * e.g. Frame 3 spans 10 sectors, and the movie is being played at 150
 *      sectors/second. Frame 3 will take 10/150 = 0.06667 seconds to be read
 *      from the disc. This means that frame 2 is shown on the screen for
 *      0.06667 seconds.
 * <p>
 * Assuming all frames in the movie span the same number of sectors, then it
 * is easy to calculate the whole movie's frame rate.
 * <p>
 * e.g. All frames in the movie play at 0.06667 seconds/frame running at
 *      150 sectors/second. So 150 * 0.066667 = 10 frames/second. Or more
 *      directly: 150 sectors/second / 10 sectors/frame = 10 frames per second.
 * <p>
 * <code>sectors/second / sectors/frame = frames/second</code>
 *
 * <h3>FIND THE DISC SPEED</h3>
 *
 * So how can you know the disc speed the movie is being played at? Besides
 * hacking into the game to find what parameters are being passed to the CD-ROM,
 * you can also look at the audio for a clue.
 * <p>
 * Audio must be played back seamlessly--there cannot be any breaks. Each audio
 * sector generates a certain number of samples. As soon as those samples are
 * played, the next audio sector should arrive to provide the next chunk of
 * samples. By looking at how far apart audio samples are spaced, it will tell
 * us how fast the disc is spinning.
 * <p>
 * e.g. Each audio sector in this movie generates 4032 samples of audio, and is
 *      being played at 37800 samples/second. So each sector will generate
 *      4032/18900 = 0.21333 seconds of audio. If the movie is being played
 *      at 1x, then audio sectors must appear every 0.21333 * 75 = 16 sectors.
 *      If the movie is played at 2x, then audio must be every
 *      0.21333 * 150 = 32 sectors. Well lo-and-behold this example movie has
 *      audio every 32 sectors, so it must be playing at 150 sectors/second,
 *      or 2x.
 * <p>
 * <code>samples/audiosector / sectors/audiosector = samples/sector</code>
 * <p>
 * <code>samples/second  / samples/sector = sectors/second</code>
 * <p>
 * Unfortunately, if there is no audio, then you can only guess if the movie
 * is played at single speed or double speed. Although it's likely
 * that all movies from the same game use the same disc speed. So if there is
 * any audio on the disc, you may be able to use that.
 *
 * <h3>FIND SECTORS PER FRAME</h3>
 *
 * The simplest and most direct way to calculate this is to divide the total
 * number of sectors in the movie by the total number of frames.
 * <p>
 * <code>sectors/movie / frames/movie = sectors/frame</code>
 * <p>
 * This should always work, and will give you a pretty good result.
 * However, it is not always the most accurate:
 * <ul>
 * <li> If there is even one extra audio sector tacked on at the end of the movie
 *   then it can throw the result off. Just ignore that sector you say? But
 *   there's no way to know if that sector is tacked on, or if it replaces the
 *   last chunk of the last frame.
 * <li> If the movie is incomplete, and only a portion of the last frame is
 *   available.
 * </ul>
 * <p>
 * It would be nice if you could simply count how many sectors there are
 * between the first frame chunk of one frame, and the first frame chunk of
 * another frame. But here are two cases where that won't work:
 * <ul>
 * <li> If an audio sector falls on the first or last sector of a frame, then
 *   there's no obvious way to tell if that audio sector would have been
 *   the last chunk of the previous frame, or the first chunk of the
 *   next frame.
 * <li> If not all frames have a consistent number of sectors/frame.
 * </ul>
 * <p>
 * Perhaps you could scan the movie for a block of sectors that aren't
 * disrupted by any audio. For some movies that might work, but not for all of
 * them (some movies will always have at least one audio sector in every
 * frame).
 * <p>
 * The only way I know to most accurately find the frame rate is to walk
 * through the movie with an assumption of sectors/frame and sectors/audio.
 * If the movie deviates from that assumption, then try the next assumption.
 */

public class StrFrameRateCalc {

    private static final Logger LOG = Logger.getLogger(StrFrameRateCalc.class.getName());

    @CheckForNull
    private WholeNumberSectorsPerFrame _wholeFrameRate;
    @CheckForNull
    private LinkedList<InconsistentFrameSequence> _inconsistentFrameRate;
    private final FpsSequence.Builder _fpsBuilder = new FpsSequence.Builder();

    public StrFrameRateCalc(int iFirstFrameStartSector, int iFirstFrameEndSector) {
        _fpsBuilder.addFrame(iFirstFrameStartSector, iFirstFrameEndSector);
        _wholeFrameRate = new WholeNumberSectorsPerFrame(iFirstFrameEndSector);
        _inconsistentFrameRate = InconsistentFrameSequence.generate(iFirstFrameStartSector, iFirstFrameEndSector);
    }

    public void addFrame(int iNextFrameStartSector, int iNextFrameEndSector) {
        _fpsBuilder.addFrame(iNextFrameStartSector, iNextFrameEndSector);
        if (_wholeFrameRate != null)
            if (!_wholeFrameRate.matchesNextVideo(iNextFrameStartSector, iNextFrameEndSector))
                _wholeFrameRate = null; // failed to match any whole number frame rates
        // TODO: Log when whole frame and inconsistent frame rate matching fails
        if (_inconsistentFrameRate != null) {
            for (Iterator<InconsistentFrameSequence> it = _inconsistentFrameRate.iterator(); it.hasNext();) {
                if (!it.next().matchesNextVideo(iNextFrameStartSector, iNextFrameEndSector))
                    it.remove();
            }
            if (_inconsistentFrameRate.isEmpty())
                _inconsistentFrameRate = null; // failed to match any inconsistent frame rates
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (_wholeFrameRate != null || _inconsistentFrameRate != null) {
            sb.append("Possible frame rates: ");
            if (_wholeFrameRate != null)
                sb.append(Arrays.toString(_wholeFrameRate.getAllPossibleSectorsPerFrame()));
            if (_inconsistentFrameRate != null)
                sb.append(_inconsistentFrameRate);
        } else {
            sb.append("No matching variable rates.");
        }
        return sb.toString();
    }

    //--------------------------------------------------------------------------

    public @CheckForNull Fraction getSectorsPerFrame(int iStartSector, int iEndSector, int iFrameCount) {

        char cCaseFpsSeqMatch;
        char cCaseOriginalLogicMatch;
        char cCase1SectPerFrm;
        char cCaseOldAndNewSame;
        char cCaseHasNewFps;
        char cCaseIsVfr;
        char cCase1Frame;

        int iSectorSize = iEndSector - iStartSector + 1;
        float fltAverage = (float)iSectorSize / iFrameCount;

        StringBuilder sbLog = null;
        if (LOG.isLoggable(Level.INFO)) {
            sbLog = new StringBuilder(toString());
        }

        Fraction oldSectorPerFrame = null;
        if (_wholeFrameRate != null) {
            cCaseOriginalLogicMatch = '1';
            int[] aiPossibleSectorsPerFrame = _wholeFrameRate.getPossibleSectorsPerFrame();
            if (aiPossibleSectorsPerFrame != null) {
                oldSectorPerFrame = new Fraction(aiPossibleSectorsPerFrame[aiPossibleSectorsPerFrame.length-1]);
            }
        } else if (_inconsistentFrameRate != null) {
            cCaseOriginalLogicMatch = '1';
            for (InconsistentFrameSequence frameSeq : _inconsistentFrameRate) {
                LOG.log(Level.INFO, "Frame rate match for video in sectors {0,number,#}-{1,number,#} ({2,number,#} frames / {3,number,#} sectors): {4}",
                        new Object[]{iStartSector, iEndSector, iFrameCount, iSectorSize, frameSeq});
                Fraction frmSeqSpf = frameSeq.getSectorsPerFrame();
                if (oldSectorPerFrame == null)
                    oldSectorPerFrame = frmSeqSpf;
                else {
                    if (frmSeqSpf.getNumerator() < oldSectorPerFrame.getNumerator())
                        oldSectorPerFrame = frmSeqSpf;
                }
            }
        } else {
            cCaseOriginalLogicMatch = '0';
        }


        Fraction newFps;
        FpsSequence.Match newMatch = null;
        cCase1Frame = iFrameCount == 1 ? '1' : '0';

        if (iFrameCount == iSectorSize) {
            newFps = new Fraction(1);
            cCase1SectPerFrm = '1';
            cCaseFpsSeqMatch = 'x';
            cCaseHasNewFps = '1';
            cCaseIsVfr = '0';
        } else if (iFrameCount == 1) {
            newFps = new Fraction(iSectorSize);
            cCase1SectPerFrm = '0';
            cCaseFpsSeqMatch = 'x';
            cCaseHasNewFps = '1';
            cCaseIsVfr = '0';
        } else {
            cCase1SectPerFrm = '0';

            newMatch = _fpsBuilder.findMatch();
            if (newMatch != null) {
                LOG.log(Level.INFO, "Matching fps sequence: {0}", newMatch);
                if (false && newMatch.iMatchesAtSector != 0) {
                    System.out.println(newMatch);
                    System.out.println(newMatch.sBuiltSequence);
                }
                cCaseFpsSeqMatch = '1';
                cCaseHasNewFps = '1';

                newFps = newMatch.sectorPerFrame;

                if (newMatch.blnOneToOne) {
                    cCaseIsVfr = '0';
                } else {
                    cCaseIsVfr = '1';
                }
            } else {
                cCaseFpsSeqMatch = '0';
                cCaseHasNewFps = '0';
                cCaseIsVfr = 'x';

                newFps = null;
            }

        }

        if (Misc.objectEquals(newFps, oldSectorPerFrame)) {
            cCaseOldAndNewSame = '1';
        } else {
            cCaseOldAndNewSame = '0';
        }

        if (sbLog != null) {
            if (oldSectorPerFrame != null)
                sbLog.append(" Chose ").append(oldSectorPerFrame);
            LOG.info(sbLog.toString());
        }

        if (LOG.isLoggable(Level.INFO)) {
            String s = String.format("Sectors %d-%d (%d) frames %d average %s "
                    + "CASE%c%c%c%c%c%c%c "
                    + "old %s new %s",
                iStartSector, iEndSector, iSectorSize, iFrameCount, new Fraction(iSectorSize, iFrameCount),

                cCaseOriginalLogicMatch,
                cCase1Frame,
                cCase1SectPerFrm,
                cCaseFpsSeqMatch,
                cCaseIsVfr,
                cCaseHasNewFps,
                cCaseOldAndNewSame,

                oldSectorPerFrame, newFps
            );

            if (newMatch != null) {
                String s2 = String.format(" @%06d vfr %s %s", newMatch.iMatchesAtSector, !newMatch.blnOneToOne, newMatch.sDescription);
                s += s2;
            }

            LOG.info(s);
        }

        return newFps;
    }

}
