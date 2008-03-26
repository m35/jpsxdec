/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * StrFpsCalc.java
 */

package jpsxdec.media;

import java.util.Iterator;
import java.util.LinkedList;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorAudio2048;
import jpsxdec.sectortypes.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSectorFrameChunk;
import jpsxdec.sectortypes.PSXSectorNull;
import jpsxdec.util.Fraction;

/** Functions and classes to calculate the frame rate of standard STR movies.
 *  Used exclusively by {@link PSXMediaSTR}.
 * <p>
 * Wow, this turned out to be way more complicated than I expected.
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
 * hacking into the game to find what paramerets are being passed to the CD-ROM,
 * you can also look at the audio for a clue.
 * <p>
 * Audio must be played back seamlessly--there cannot be any breaks. Each audio
 * sector generates a certain number of samples. As soon as those samples are
 * played, the next audio sector should arrive to provide the next chunk of
 * samples. By looking at how far apart audio samples are spaced, will tell us
 * how fast the disc is spinning.
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
 * is played at single speed or double speed. Although its probably very likely
 * that all movies from the same game use the same disc speed. So if there are 
 * other STR movies with audio, you may be able to use that.
 * 
 * <h3>FIND SECTORS PER FRAME</h3>
 * 
 * The simplist and most direct way to calculate this is to divide the total
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
 *   the first last chunk of the previous frame, or the first chunk of the
 *   next frame.
 * <li> If not all frames have a consisent number of sectors/frame.
 * </ul>
 * <p>
 * Perhaps you could scan the movie for a block of sectors that aren't
 * disrupted by any audio. For some movies that might work, but not for all of
 * them (some movies will aways have at least one audio sector in every
 * frame).
 * <p>
 * The only way I know to most accurately find the frame rate is to walk
 * through the movie with an assumption of sectors/frame and sectors/audio.
 * If the movie deviates from that assumption, then try the next assumption.
 * <p>
 * And that's what the following code does.
 * 
 */

public class StrFpsCalc {

    /** Generates a sequence of frame chunk numbers and audio chunks 
     *  corresponding to a specified sectors/frame and sectors/audio. 
     *  It generates the sequence until it loops. */
    public static class FrameSequence {
        
        public final int SectorsPerFrame;
        public final int SectorsPerAudio;
        public final byte[] Sequence;
        
        /** If there is no audio. */
        public FrameSequence(int iSectPerFrame) {
            SectorsPerAudio = -1;
            SectorsPerFrame = iSectPerFrame;
            Sequence = new byte[SectorsPerFrame];
            for (byte b = 0; b < Sequence.length; b++) {
                Sequence[b] = b;
            }
        }
        
        public FrameSequence(int iSectPerFrame, int iSectPerAudio) {
            SectorsPerFrame = iSectPerFrame;
            SectorsPerAudio = iSectPerAudio;
            
            byte bFrameChunkIdx = 0;
            byte bSectorsSinceLastAud = 0;
            byte bPeriodCount = 0;
            int i;
            // the longest possible sequence are these values multiplied
            byte[] ab = new byte[SectorsPerFrame * SectorsPerAudio];

            for (i = 0; ; i++) {
                if (bSectorsSinceLastAud == SectorsPerAudio - 1) {
                    ab[i] = -1;
                    bSectorsSinceLastAud = 0;
                } else {
                    ab[i] = bFrameChunkIdx;
                    bFrameChunkIdx++;
                    bSectorsSinceLastAud++;
                }
                bPeriodCount++;
                if (bPeriodCount == SectorsPerFrame) {
                    bPeriodCount = 0;
                    bFrameChunkIdx = 0;
                }
                // if the next frame is going to start the loop
                if (bFrameChunkIdx == 0 && 
                    bSectorsSinceLastAud == 0 && 
                    bPeriodCount == 0) 
                {
                    break;
                }
            }
            if (i == ab.length - 1)
                Sequence = ab;
            else
                Sequence = jpsxdec.util.Misc.copyOfRange(ab, 0, i+1);
        }
    }
    
    /** This class will walk a sequence FrameSequence and compare it to
     * submitted PSXSectors. It will report if the submitted sectors
     * follow the sequence or not. */
    public static class FrameSequenceWalker {

        private FrameSequence m_oSequence;
        private int m_iPos;
        private boolean m_blnIsPossible = true;
        
        public FrameSequenceWalker(FrameSequence oSequence, int iStart) {
            m_oSequence = oSequence;
            m_iPos = iStart + 1;
            if (m_iPos >= m_oSequence.Sequence.length)
                m_iPos = 0;
        }
        
        /** @return if the sector followed the sequence. */
        public boolean Next(PSXSector oPsxSect) {
            if (!m_blnIsPossible) return false;
            
            if (oPsxSect instanceof PSXSectorNull) {
                // don't compare, skip it
            } else if (oPsxSect instanceof PSXSectorAudio2048 || 
                       oPsxSect instanceof PSXSectorAudioChunk) 
            {
                // if this shouldn't be an audio sector, then fail
                if (m_oSequence.Sequence[m_iPos] != -1) {
                    m_blnIsPossible = false;
                    return false;
                }
            } else if (oPsxSect instanceof PSXSectorFrameChunk) {
                PSXSectorFrameChunk oChk = (PSXSectorFrameChunk)oPsxSect;

                // if there are more chunks in this frame than there are in the 
                // expected frame period, then there's no way this could be possible
                if (oChk.getChunksInFrame() > m_oSequence.SectorsPerFrame) {
                    m_blnIsPossible = false;
                    return false;
                }
                
                // if the current chunk does not match the expect chunk
                // then this fails
                if (m_oSequence.Sequence[m_iPos] != oChk.getChunkNumber()) {
                    m_blnIsPossible = false;
                    return false;
                }
            }            
            m_iPos++;
            if (m_iPos >= m_oSequence.Sequence.length) // loop sequence
                m_iPos = 0;
            return true;
        }
        
        public int getSectorsPerFrame() {
            return m_oSequence.SectorsPerFrame;
        }
        
        public int getSectorsPerAudio() {
            return m_oSequence.SectorsPerAudio;
        }
        
    }
    
    /** Filters an array of of FrameSequences to only the possible sequences
     *  that would match the submitted PSXSector.
     *  Returns a LinkedList of only valid FrameSequences. */
    public static LinkedList<FrameSequenceWalker> GenerateSequenceWalkers(
            PSXSectorFrameChunk oChk, 
            FrameSequence[] aoPossibleChunkSequences) 
    {
        LinkedList<FrameSequenceWalker> oList = new LinkedList<FrameSequenceWalker>();
        for (FrameSequence oSeq : aoPossibleChunkSequences) {
            if (oChk.getChunksInFrame() <= oSeq.SectorsPerFrame) {
                for (int i = 0; i < oSeq.Sequence.length; i++) {
                    if (oSeq.Sequence[i] == oChk.getChunkNumber())
                        oList.add(new FrameSequenceWalker(oSeq, i));
                }
            }
        }
        return oList;
    }
    
    /** Calculates the sectors/second based on the audio format and the
     *  sectors/audio. */
    public static int GetSectorsPerSecond(long iSectorsBetweenAudio, long iAudioSamplesPerSec, long iAudioChannels) {
        if (4032 / iAudioChannels * 75 == iSectorsBetweenAudio * iAudioSamplesPerSec)
            return 75;
        if (4032 / iAudioChannels * 150 == iSectorsBetweenAudio * iAudioSamplesPerSec)
            return 150;
        throw new RuntimeException("error calculating frame rate");
    }
    
    /** Represents frames/second, including the associated disc speed. */
    public static class FramesPerSecond extends Fraction {
        private final int m_iDiscSpeed;
        
        public FramesPerSecond(Fraction frac, long lngDiscSpeed) {
            super(frac);
            m_iDiscSpeed = lngDiscSpeed == 75 ? 1 : 2;
        }
        
        public FramesPerSecond(long iNum, long iDenom, long lngDiscSpeed) {
            super(iNum, iDenom);
            m_iDiscSpeed = lngDiscSpeed == 75 ? 1 : 2;
        }
        
        public int discSpeed() {
            return m_iDiscSpeed;
        }
        
        public String toString() {
            return m_iDiscSpeed + "x " + super.numerator() + "/" + super.denominator();
        }
    }
    
    //--------------------------------------------------------------------------
    
    /** Narrows down the possible FrameSequences of a movie to just one. */
    public static FramesPerSecond FigureOutFps(
            LinkedList<StrFpsCalc.FrameSequenceWalker> oSequences, 
            long lngSectorsPerSecond, 
            long lngFramesPerMovie, 
            long lngSectorsPerMovie) 
    {
        if (oSequences.size() == 0) {
            // no known sequence  :(
            
            // (frames/movie / sectors/movie) * sectors/second
            // = (frames/movie * movies/sector) * sectors/second
            // = frames/sector * sectors/second
            // = frames/second
            return new FramesPerSecond(
                    lngFramesPerMovie * lngSectorsPerSecond, 
                    lngSectorsPerMovie,
                    lngSectorsPerSecond);
            
        } else if (oSequences.size() == 1) {
            
            long lngSectorsPerFrame = oSequences.getFirst().getSectorsPerFrame();
            
            // sectors/second / sectors/frame
            // = sectors/second * frames/sector
            // = frames/second
            return new FramesPerSecond(
                    lngSectorsPerSecond, lngSectorsPerFrame, 
                    lngSectorsPerSecond);
            
        } else { // more than one match
            
            if (lngFramesPerMovie == 1) {
                // if only one frame, just set it to the sector length
                long lngSectorsPerFrame = lngFramesPerMovie;
                return new FramesPerSecond(
                        lngSectorsPerSecond, lngSectorsPerFrame,
                        lngSectorsPerSecond);
            } else {
                // pick the closest to the known ones
                // manually calculate the sectors/frame
                double dblEstimate = (double)lngFramesPerMovie / 
                                             lngSectorsPerMovie;
                // choose the closest of the remaining matches to this estimate
                FrameSequenceWalker oClosest = 
                        ChooseClosestSectorsPerFrame(oSequences, dblEstimate);
                
                long lngSectorsPerFrame = oClosest.getSectorsPerFrame();

                return new FramesPerSecond(
                        lngSectorsPerSecond, lngSectorsPerFrame,
                        lngSectorsPerSecond);
            }

        }//Sequence.size()
        
    }
    
    /** If there is more than one valid FrameSequence, chose the closest one
     *  to the fall-back equation. */
    private static FrameSequenceWalker ChooseClosestSectorsPerFrame(
            LinkedList<StrFpsCalc.FrameSequenceWalker> oSequences, 
            double dblEstimate) 
    {
        FrameSequenceWalker oClosest = null;
        double dblHowClose = 9001; // just assume it's over 9000
        for (Iterator<FrameSequenceWalker> it = oSequences.iterator(); it.hasNext();) {
            FrameSequenceWalker oWalker = it.next();
            if (oClosest == null) {
                oClosest = oWalker;
                dblHowClose = Math.abs(oWalker.getSectorsPerFrame() - dblEstimate);
            } else {
                double dbl = Math.abs(oWalker.getSectorsPerFrame() - dblEstimate);
                if (dbl < dblHowClose) {
                    oClosest = oWalker;
                    dblHowClose = dbl;
                }
            }
        }
        // we have a winner
        return oClosest;
        
    }
    
    
    
}
