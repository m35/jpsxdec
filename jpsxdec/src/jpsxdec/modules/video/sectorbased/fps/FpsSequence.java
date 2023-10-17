/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/**
 * Technically should be "sectors per frame sequence" but FPS is easier to say and type.
 *
 * The format
 * <pre>
 * '[' = first sector of a frame
 * '.' = any kind of sector between the first and last
 * ']' = last sector of a frame
 * '#' = a single sector frame
 * 'x' = non frame sector between frames
 * </pre>
 *
 * Predetermined sequences loaded from file.
 *
 * A sequence for a video is constructed during indexing, then it is
 * thoroughly compared to each predetermined sequence.
 */
public class FpsSequence {

    public static final boolean DEBUG = false;

    private static final Logger LOG = Logger.getLogger(FpsSequence.class.getName());

    private static final String FPS_SEQUENCE_RESOURCE = "FpsSequence.txt";

    @CheckForNull
    private static List<FpsSequence> FPS_SEQUENCES;

    // Don't synchronize the initial check, only the load, to minimize locking
    protected static @Nonnull List<FpsSequence> getFpsSequences() {
        if (FPS_SEQUENCES == null)
            loadFpsSequences();

        return FPS_SEQUENCES;
    }

    // Only synchronize the loading
    private synchronized static void loadFpsSequences() {
        // Double check if it's already loaded within the sync in case of race condition
        // (not that there'd be any real problem with loading the file twice,
        // still would like to avoid it)
        if (FPS_SEQUENCES != null)
            return;

        ArrayList<FpsSequence> seq = new ArrayList<FpsSequence>();

        InputStream is = FpsSequence.class.getResourceAsStream(FPS_SEQUENCE_RESOURCE);
        if (is == null)
            throw new RuntimeException("Unable to find fps resource " + FPS_SEQUENCE_RESOURCE);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII));
        try {
            String sLine;
            while ((sLine = reader.readLine()) != null) {
                String sDetails = sLine;
                String sDiagram = reader.readLine();
                String sBlankLine = reader.readLine();
                if (!sBlankLine.isEmpty())
                        throw new RuntimeException();
                String[] asDetails = sDetails.split("\t");
                if (asDetails.length > 3)
                        throw new RuntimeException();

                String sName = asDetails[0];
                String sSectorsPerFrame = asDetails[1];
                boolean blnLoops = false;
                if (asDetails.length == 3) {
                    if (!asDetails[2].equals("loops"))
                        throw new RuntimeException();
                    blnLoops = true;
                }
                int[] aiSectorsPerFrame = Misc.splitInt(sSectorsPerFrame, "/");
                Fraction sectorsPerFrame = new Fraction(aiSectorsPerFrame[0], aiSectorsPerFrame[1]);

                FpsSequence fps = new FpsSequence(sName, sDiagram, sectorsPerFrame, blnLoops);
                seq.add(fps);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            IO.closeSilently(reader, LOG);
        }

        seq.trimToSize();
        FPS_SEQUENCES = Collections.unmodifiableList(seq);
    }

    private static final char START_FRAME_SECTORS = '[';
    private static final char SECTOR_WITHIN_FRAME = '.';
    private static final char END_FRAME_SECTORS = ']';
    private static final char SINGLE_SECTOR_FRAME = '#';
    private static final char NON_FRAME_SECTOR = 'x';
    protected static boolean isFrameSector(char c) {
        switch (c) {
            case START_FRAME_SECTORS:
            case SECTOR_WITHIN_FRAME:
            case END_FRAME_SECTORS:
            case SINGLE_SECTOR_FRAME:
                return true;
        }
        return false;
    }

    public static class Builder {

        protected final StringBuilder _sectors = new StringBuilder();

        public void addFrame(int iFrameStartSector, int iFrameEndSectorInclusive) {
            if (iFrameStartSector < _sectors.length() || iFrameEndSectorInclusive < iFrameStartSector)
                throw new IllegalArgumentException();

            while (_sectors.length() < iFrameStartSector) {
                _sectors.append(NON_FRAME_SECTOR);
            }

            // sanity check
            int iExpectedEnd = _sectors.length() + (iFrameEndSectorInclusive - iFrameStartSector + 1);

            if (iFrameStartSector == iFrameEndSectorInclusive) {
                _sectors.append(SINGLE_SECTOR_FRAME);
            } else {
                _sectors.append(START_FRAME_SECTORS);
                while (_sectors.length() < iFrameEndSectorInclusive)
                    _sectors.append(SECTOR_WITHIN_FRAME);
                _sectors.append(END_FRAME_SECTORS);
            }

            // sanity check
            if (_sectors.length() != iExpectedEnd)
                throw new IllegalStateException("My logic is wrong");
        }

        public @CheckForNull Match findMatch() {
            if (_sectors.length() == 0)
                throw new IllegalStateException();

            String sBuiltSectorSequence = _sectors.toString();

            List<FpsSequence> seqs = getFpsSequences();
            for (FpsSequence seq : seqs) {
                FpsSequence.Match match = seq.matches(sBuiltSectorSequence);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }
    }

    public static class Match {
        @Nonnull
        public final String sDescription;
        @Nonnull
        public final Fraction sectorPerFrame;
        public final int iMatchesAtSector;
        /** This can be considered as "not variable frame rate", meaning
         * there is only one frame in the predetermined sequence for
         * each frame in the built sequence. If there are more than one
         * frame in the predetermined sequence, then it's basically a
         * variable frame rate. */
        public final boolean blnOneToOne;
        @Nonnull
        public final String sBuiltSequence;
        @Nonnull
        public final String sMatchingSequence;

        public Match(@Nonnull String sDescription, @Nonnull Fraction sectorPerFrame,
                     int iMatchesAtSector, boolean blnOneToOne,
                     @Nonnull String sBuiltSequence, @Nonnull String sMatchingSequence)
        {
            this.sDescription = sDescription;
            this.sectorPerFrame = sectorPerFrame;
            this.iMatchesAtSector = iMatchesAtSector;
            this.blnOneToOne = blnOneToOne;
            this.sBuiltSequence = sBuiltSequence;
            this.sMatchingSequence = sMatchingSequence;
        }

        public String getSequenceMatch() {
            int iEnd = iMatchesAtSector + sBuiltSequence.length();
            if (iEnd >= sMatchingSequence.length()) {
                // handle looped sequence
                StringBuilder sb = new StringBuilder();
                sb.append(sMatchingSequence, iMatchesAtSector, sMatchingSequence.length());
                while (sb.length() + sMatchingSequence.length() < sBuiltSequence.length()) {
                    sb.append(sMatchingSequence);
                }
                sb.append(sMatchingSequence, 0, sBuiltSequence.length() - sb.length());
                return sb.toString();
            } else {
                return sMatchingSequence.substring(iMatchesAtSector, iMatchesAtSector + sBuiltSequence.length());
            }
        }

        @Override
        public String toString() {
            return String.format("\"%s\" %s length %d matches@%d 1:1=%s",
                    sDescription, sectorPerFrame, sBuiltSequence.length(), iMatchesAtSector, blnOneToOne);
        }
    }


    @Nonnull
    protected final String _sDescription;
    @Nonnull
    protected final String _sVideoSectorSequence;
    @Nonnull
    protected final Fraction _sectorsPerFrame;
    protected final boolean _blnLoops;

    public FpsSequence(@Nonnull String sDescription,
                       @Nonnull String sVideoSectorSequence,
                       @Nonnull Fraction sectorsPerFrame, boolean blnLooping)
    {
        _sDescription = sDescription;
        _sectorsPerFrame = sectorsPerFrame;
        _sVideoSectorSequence = sVideoSectorSequence;
        _blnLoops = blnLooping;
    }

    public Fraction getSectorsPerFrame() {
        return _sectorsPerFrame;
    }


    /**
     * Searches for the "needle" sequence inside this "haystack" sequence.
     * @return null if does not match
     */
    protected @CheckForNull Match matches(@Nonnull String sNeedleSequence) {

        if (!_blnLoops && sNeedleSequence.length() > _sVideoSectorSequence.length()) {
            LOG.log(Level.WARNING, "Needle length {0,number,#} is longer than this haystack length {1,number,#} \"{2}\"",
                    new Object[]{sNeedleSequence.length(), _sVideoSectorSequence.length(), _sDescription});
            return null;
        }

        HaystackFactory haystackFactory = new HaystackFactory();
        // just to check for frame breaks
        SequenceStartingAt needleChecker = new SequenceStartingAt(sNeedleSequence, 0, false);

        NeedleStart:
        while (haystackFactory.hasNext(sNeedleSequence.length())) {

            SequenceStartingAt haystack = haystackFactory.next();

            int iNeedleFrameStart = -1;
            int iNeedleFrameEnd = -1;
            int iHaystackFrameStart = -1;
            int iHaystackFrameEnd = -1;
            boolean blnIsOneToOne = true;

            for (int iPosition = 0; iPosition < sNeedleSequence.length(); iPosition++) {
                char cHay = haystack.get(iPosition);
                char cNeedle = sNeedleSequence.charAt(iPosition);

                // check needle
                if (cNeedle == START_FRAME_SECTORS || cNeedle == SINGLE_SECTOR_FRAME) {
                    if (iNeedleFrameEnd >= 0) {
                        // ensure a break in hay
                        if (!haystack.containsBreak(iNeedleFrameEnd, iPosition))
                            continue NeedleStart;
                    }
                    iNeedleFrameEnd = -1;
                    if (iNeedleFrameStart >= 0)
                        throw new IllegalStateException();
                    iNeedleFrameStart = iPosition;
                }
                if (cNeedle == END_FRAME_SECTORS || cNeedle == SINGLE_SECTOR_FRAME) {
                    if (iNeedleFrameStart < 0)
                        throw new IllegalStateException();
                    // ensure a frame in hay
                    if (!haystack.containsFrameSector(iNeedleFrameStart, iPosition))
                        continue NeedleStart;

                    iNeedleFrameStart = -1;
                    if (iNeedleFrameEnd >= 0)
                        throw new IllegalStateException();
                    iNeedleFrameEnd = iPosition;
                }

                // check for variable frame rate
                // by checking if ONLY 1 needle frame exists in the haystack frame
                if (blnIsOneToOne) {
                    if (isFrameSector(cHay)) {
                        if (iHaystackFrameEnd >= 0) {
                            // if a haystack break exists within a needle frame
                            // then not all frames align between the two
                            // so the match won't be the actual frame rate,
                            // but some multiple (possible a fractional multiple)
                            if (!needleChecker.containsBreak(iHaystackFrameEnd, iPosition))
                                blnIsOneToOne = false;
                        }
                        iHaystackFrameEnd = -1;
                        if (iHaystackFrameStart < 0)
                            iHaystackFrameStart = iPosition;
                    }
                    if (cHay == END_FRAME_SECTORS || cHay == SINGLE_SECTOR_FRAME) {
                        if (iHaystackFrameStart >= 0) {
                            // if there is a haystack frame in a needle break
                            // that means there is 1 or more haystack breaks
                            // in a needle frame, therefore at least there is at least
                            // some multiple of haystack frames in a needle frame
                            if (!needleChecker.containsFrameSector(iHaystackFrameStart, iPosition))
                                blnIsOneToOne = false;
                        }
                        iHaystackFrameStart = -1;
                        iHaystackFrameEnd = iPosition;
                    }
                }

                // maybe also count how many needle frame breaks exist inside haystack frame
                // and check if there are only multiple haystack frames in a needle frame
                // that may mean that the vfr fps is a perfect multiple (like Alice)
            }

            // Match!
            Match match = new Match(_sDescription, _sectorsPerFrame, haystack.getStart(), blnIsOneToOne, sNeedleSequence, _sVideoSectorSequence);
            return match;
        }

        return null;
    }

    private class HaystackFactory {
        private int __iStart = 0;
        public boolean hasNext(int iNeedleLength) {
            if (_blnLoops)
                return __iStart < _sVideoSectorSequence.length(); // only iterate through a loop once
            else
                return __iStart + iNeedleLength <= _sVideoSectorSequence.length(); // continue until the needle hits the end of the haystack
        }
        public @Nonnull SequenceStartingAt next() {
            return new SequenceStartingAt(_sVideoSectorSequence, __iStart++, _blnLoops);
        }
    }

    private static class SequenceStartingAt {
        @Nonnull
        private final String _sSequence;
        private final boolean _blnLoops;
        private final int _iStart;
        private int _iToStringIndex = 0;

        public SequenceStartingAt(@Nonnull String sSequence, int iStart, boolean blnLoops) {
            _sSequence = sSequence;
            _blnLoops = blnLoops;
            _iStart = iStart;
        }

        public int getStart() {
            return _iStart;
        }

        public char get(int iPos) {
            int iLoopPos = loopPos(iPos + _iStart);
            _iToStringIndex = iLoopPos;
            char c = _sSequence.charAt(iLoopPos);
            return c;
        }

        private int loopPos(int i) {
            int iLoopPos = i % _sSequence.length();
            // sanity check: looping shouldn't actually happen if it's not loopable
            if (!_blnLoops && iLoopPos != i)
                throw new IllegalStateException();
            return iLoopPos;
        }

        public boolean containsFrameSector(int iStartSector, int iEndSector) {
            for (int i = iStartSector; i <= iEndSector; i++) {
                if (i < 0)
                    continue;
                char c = get(i);
                if (isFrameSector(c))
                    return true;
            }
            return false;
        }

        public boolean containsBreak(int iStartSector, int iEndSector) {
            if (iStartSector == iEndSector)
                throw new IllegalArgumentException();

            boolean blnFoundEndFrame = false;
            for (int i = iStartSector; i <= iEndSector; i++) {
                if (i < 0)
                    return true;
                char c = get(i);
                // Looking for:
                // #
                // x
                // ][
                // NOT .]
                // NOT [.
                switch (c) {
                    case SINGLE_SECTOR_FRAME:
                    case NON_FRAME_SECTOR:
                        return true;
                    case END_FRAME_SECTORS:
                        blnFoundEndFrame = true;
                        break;
                    case START_FRAME_SECTORS:
                        if (blnFoundEndFrame)
                            return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            if (_blnLoops) {
                return _sSequence.substring(0, _iToStringIndex) + ">" + _sSequence.substring(_iToStringIndex, _sSequence.length());
            } else {
                int iMin = Math.max(_iToStringIndex - 30, 0);
                int iMax = Math.min(_iToStringIndex+50, _sSequence.length());
                return _sSequence.substring(iMin, _iToStringIndex) + ">" + _sSequence.substring(_iToStringIndex, iMax);
            }
        }
    }

    @Override
    public String toString() {
        return _sDescription + " length " + _sVideoSectorSequence.length() + " " + _sectorsPerFrame;
    }

}
