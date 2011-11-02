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

package jpsxdec.discitems;

import java.util.Iterator;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.FrameDemuxer;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.discitems.psxvideoencode.ReplaceFrames;
import jpsxdec.discitems.savers.DemuxedFrame;
import jpsxdec.discitems.savers.VideoSaverBuilder;
import jpsxdec.util.player.PlayController;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.Maths;
import jpsxdec.util.NotThisTypeException;

/** Represents all variations of PlayStation video streams. */
public class DiscItemVideoStream extends DiscItem {

    private static final Logger log = Logger.getLogger(DiscItemVideoStream.class.getName());

    public static final String TYPE_ID = "Video";
    
    private static final String FRAMES_KEY = "Frames";
    private static final String DIMENSIONS_KEY = "Dimensions";
    private static final String DISC_SPEED_KEY = "Disc Speed";
    private static final String SECTORSPERFRAME_KEY = "Sectors/Frame";
    private static final String FRAME1_LAST_SECTOR_KEY = "Frame 1 last sector";

    
    /** Width of video in pixels. */
    private final int _iWidth;
    /** Height of video in pixels. */
    private final int _iHeight;
    
    /** First video frame number. */
    private final int _iStartFrame;
    /** Last video frame number. */
    private final int _iEndFrame;

    private final long _lngSectors;
    private final long _lngPerFrame;

    private final int _iFrame1LastSector;

    private int _iDiscSpeed;

    public DiscItemVideoStream(int iStartSector, int iEndSector,
                            int iStartFrame, int iEndFrame,
                            int lngWidth, int lngHeight,
                            int iSectors, int iPerFrame,
                            int iFrame1LastSector)
    {
        super(iStartSector, iEndSector);

        _iStartFrame = iStartFrame;
        _iEndFrame = iEndFrame;
        _iWidth = lngWidth;
        _iHeight = lngHeight;
        // simplify the sectors/frame fraction
        int iGcd = Maths.gcd(iSectors, iPerFrame);
        _lngSectors = iSectors / iGcd;
        _lngPerFrame = iPerFrame / iGcd;
        _iFrame1LastSector = iFrame1LastSector;
        _iDiscSpeed = -1;
    }

    public DiscItemVideoStream(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        
        int[] ai = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = ai[0];
        _iHeight = ai[1];
        
        ai = fields.getIntRange(FRAMES_KEY);
        _iStartFrame = ai[0];
        _iEndFrame = ai[1];

        long[] alng = fields.getFraction(SECTORSPERFRAME_KEY);
        _lngSectors = alng[0];
        _lngPerFrame = alng[1];

        _iFrame1LastSector = fields.getInt(FRAME1_LAST_SECTOR_KEY);

        _iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
    }

    public DiscItemSerialization serialize() {
        DiscItemSerialization serial = super.superSerial(TYPE_ID);
        serial.addRange(FRAMES_KEY, _iStartFrame, _iEndFrame);
        serial.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        serial.addFraction(SECTORSPERFRAME_KEY, _lngSectors, _lngPerFrame);
        serial.addNumber(FRAME1_LAST_SECTOR_KEY, _iFrame1LastSector);
        if (_iDiscSpeed > 0)
            serial.addNumber(DISC_SPEED_KEY, _iDiscSpeed);
        return serial;
    }

    public int getStartFrame() {
        return _iStartFrame;
    }

    public int getEndFrame() {
        return _iEndFrame;
    }

    public int getWidth() {
        return _iWidth;
    }

    public boolean shouldBeCropped() {
        return (_iHeight % 16) != 0 ||
               (_iWidth  % 16) != 0;
    }

    public int getHeight() {
        return _iHeight;
    }

    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getInterestingDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%dx%d, %d frames, ",
                _iWidth, _iHeight, _iEndFrame - _iStartFrame + 1));
        if (_iDiscSpeed > 0) {
            formatFps(sb, _iDiscSpeed * 75);
        } else {
            formatFps(sb, 150);
            sb.append(" (or ");
            formatFps(sb, 75);
            sb.append(')');
        }
        return sb.toString();
    }

    private void formatFps(StringBuilder sb, int iSectorsPerSecond) {
        sb.append(formatFps(Fraction.divide(iSectorsPerSecond, getSectorsPerFrame())));
        sb.append(" fps = ");
        sb.append(formatTime(getSectorLength() / iSectorsPerSecond));
    }

    public static String formatFps(Fraction fps) {
        if (fps.getNumerator() % fps.getDenominator() == 0)
            return String.valueOf(fps.getNumerator() / fps.getDenominator());
        else {
            return String.format("%1.3f", fps.asDouble())
                    .replaceFirst("0+$", ""); // trim trailing zeros
        }
    }

    public static String formatTime(long lngSeconds) {
        long lngMin = lngSeconds / 60;
        StringBuilder sb = new StringBuilder();
        if (lngMin > 0) {
            sb.append(lngMin);
            sb.append(" min");
        }
        lngSeconds = lngSeconds % 60;
        if (lngSeconds > 0) {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(lngSeconds);
            sb.append(" sec");
        }
        return sb.toString();
    }
    

    /** 1 for 1x (75 sectors/second), 2 for 2x (150 sectors/second), or -1 if unknown. */
    public int getDiscSpeed() {
        return _iDiscSpeed;
    }

    public void setDiscSpeed(int iSpeed) {
        _iDiscSpeed = iSpeed;
    }

    public Fraction getSectorsPerFrame() {
        return new Fraction(_lngSectors, _lngPerFrame);
    }

    public int getPresentationStartSector() {
        return getStartSector() + _iFrame1LastSector;
    }

    /** @return Sector where the frame begins. */
    public IVideoSector seek(int iFrame) throws IOException {
        // clamp the desired frame
        if (iFrame < getStartFrame())
            iFrame = getStartFrame();
        else if (iFrame > getEndFrame())
            iFrame = getEndFrame();
        // calculate an estimate where the frame will land
        double percent = (iFrame - getStartFrame()) / (double)(getEndFrame() - getStartFrame()+1);
        percent -= 0.05;
        // jump to the sector
        // hopefully land near the frame
        int iSect = (int)
                ( (getEndSector() - getStartSector()) * (percent) )
                + getStartSector();
        if (iSect < getStartSector()) iSect = getStartSector();

        // now seek ahead until we read the desired frame
        CdSector cdSect = getSourceCD().getSector(iSect);
        IdentifiedSector identifiedSect = IdentifiedSector.identifySector(cdSect);
        while ( (!(identifiedSect instanceof IVideoSector) ||
                ((IVideoSector)identifiedSect).getFrameNumber() < iFrame)
                &&
                iSect < getSourceCD().getLength())
        {
            iSect++;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = IdentifiedSector.identifySector(cdSect);
        }

        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(identifiedSect instanceof IVideoSector) ||
               ((IVideoSector)identifiedSect).getFrameNumber() > iFrame ||
               ((IVideoSector)identifiedSect).getChunkNumber() > 0)
        {
            iSect--;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = IdentifiedSector.identifySector(cdSect);
        }
        
        return (IVideoSector) identifiedSect;
    }

    public VideoSaverBuilder makeSaverBuilder() {
        return new VideoSaverBuilder(this);
    }

    private DiscItemAudioStream[] _aoAudioStreams;

    /** Called by indexer after index has been created. */
    public void collectParallelAudio(DiscIndex index) {
        ArrayList<DiscItemAudioStream> parallelAudio = new ArrayList<DiscItemAudioStream>();
        for (DiscItem item : index) {
            if (item instanceof DiscItemAudioStream) {
                DiscItemAudioStream audItem = (DiscItemAudioStream) item;
                if (isAudioVideoAligned(audItem)) {
                    parallelAudio.add(audItem);
                    audItem.setPartOfVideo(true);
                    if (log.isLoggable(Level.INFO))
                        log.fine("Parallel audio: " + item.toString());
                }
            }
        }
        if (parallelAudio.size() > 0) {
            if (log.isLoggable(Level.INFO))
                log.fine("Added to this media item " + this.toString());
            _aoAudioStreams = parallelAudio.toArray(new DiscItemAudioStream[parallelAudio.size()]);

            // keep the list sorted in order found in disc index

            // if there is only 1 disc speed used by parallel audio, then
            // we can be confident the video should have the same speed
            if (_iDiscSpeed < 1) {
                _iDiscSpeed = _aoAudioStreams[0].getDiscSpeed();
                for (DiscItemAudioStream audio : _aoAudioStreams) {
                    if (audio.getDiscSpeed() != _iDiscSpeed) {
                        _iDiscSpeed = -1;
                        break;
                    }
                }
            }

        }
    }

    public boolean isAudioVideoAligned(DiscItem audioItem) {
        int iSectorsInside = getOverlap(audioItem);
        if (iSectorsInside == 0)
            return false;
        
        // basically if the majority of audio is inside the video,
        // then they're parallel. However, this misses the odd case where
        // one audio stream engulfs two video streams (though it is unlikely
        // because you can't start an audio stream in the middle).
        int iSecotrsOutside = audioItem.getSectorLength() - iSectorsInside;

        return iSectorsInside > iSecotrsOutside;
    }

    public boolean hasAudio() {
        return _aoAudioStreams != null && _aoAudioStreams.length > 0;
    }

    public int getParallelAudioStreamCount() {
        return _aoAudioStreams == null ? 0 : _aoAudioStreams.length;
    }

    public List<DiscItemAudioStream> getParallelAudioStreams() {
        if (_aoAudioStreams != null)
            return Arrays.asList(_aoAudioStreams);
        else
            return null;
    }

    private static class FormatGroup {
        private final ArrayList<DiscItemAudioStream> _items =
                new ArrayList<DiscItemAudioStream>();

        public FormatGroup(DiscItemAudioStream first) {
            _items.add(first);
        }
        
        public boolean addIfMatches(DiscItemAudioStream another) {
            if (another.hasSameFormat(_items.get(0))) {
                _items.add(another);
                return true;
            } else {
                return false;
            }
        }

        public void findLongest(
                final int iIndex, LongestStack curGroup)
        {
            for (int i = iIndex; i < _items.size(); i++) {
                DiscItemAudioStream potential = _items.get(i);
                boolean blnOverlapsAny = false;
                for (DiscItemAudioStream audItem : curGroup) {
                    if (potential.overlaps(audItem)) {
                        blnOverlapsAny = true;
                        break;
                    }
                }
                if (!blnOverlapsAny) {
                    curGroup.push(potential);
                    findLongest(i+1, curGroup);
                    curGroup.pop();
                }
            }
        }
    }

    private static class LongestStack implements Iterable<DiscItemAudioStream> {

        private final ArrayList<DiscItemAudioStream> _stack =
                new ArrayList<DiscItemAudioStream>();

        private int _iCurLen = 0;
        
        private int _iMaxLen = 0;
        private int _iStartSector = -1;
        private final ArrayList<DiscItemAudioStream> _longestCopy =
                new ArrayList<DiscItemAudioStream>();

        public int getMaxLen() {
            return _iMaxLen;
        }

        public int getStartSector() {
            return _iStartSector;
        }

        public ArrayList<DiscItemAudioStream> getLongestCopy() {
            return _longestCopy;
        }

        public void push(DiscItemAudioStream o) {
            _stack.add(o);
            _iCurLen += o.getSectorLength();
            if (_iCurLen > _iMaxLen) {
                _iMaxLen = _iCurLen;
                _longestCopy.clear();
                _longestCopy.addAll(_stack);
                _iStartSector = _longestCopy.get(0).getStartSector();
                for (int i = 1; i < _longestCopy.size(); i++) {
                    if (_longestCopy.get(i).getStartSector() < _iStartSector)
                        _iStartSector = _longestCopy.get(i).getStartSector();
                }
            }
        }

        public void pop() {
            DiscItemAudioStream removed = _stack.remove(_stack.size() - 1);
            _iCurLen -= removed.getSectorLength();
        }

        public Iterator<DiscItemAudioStream> iterator() {
            return _stack.iterator();
        }

    }

    private transient ArrayList<DiscItemAudioStream>
            _longestNonIntersectingAudioStreams;

    public List<DiscItemAudioStream> getLongestNonIntersectingAudioStreams() {
        if (!hasAudio())
            return null;

        if (_longestNonIntersectingAudioStreams != null)
            return _longestNonIntersectingAudioStreams;

        // I'd say maybe 99% of cases will only have 1
        if (_aoAudioStreams.length == 1) {
            _longestNonIntersectingAudioStreams = new ArrayList<DiscItemAudioStream>(1);
            _longestNonIntersectingAudioStreams.add(_aoAudioStreams[0]);
            return _longestNonIntersectingAudioStreams;
        }

        // split up by format
        ArrayList<FormatGroup> formatGroups = new ArrayList<FormatGroup>();
        for (DiscItemAudioStream audItem : _aoAudioStreams) {
            FormatGroup matchingGroup = null;
            for (FormatGroup formatGroup : formatGroups) {
                if (formatGroup.addIfMatches(audItem)) {
                    matchingGroup = formatGroup;
                    break;
                }
            }
            if (matchingGroup == null) {
                matchingGroup = new FormatGroup(audItem);
                formatGroups.add(matchingGroup);
            }
        }

        // find the longest combination of parallel audio streams
        LongestStack longest = null;
        for (FormatGroup formatGroup : formatGroups) {
            LongestStack stack = new LongestStack();
            formatGroup.findLongest(0, stack);
            if (longest == null) {
                longest = stack;
            } else if (stack.getMaxLen() > longest.getMaxLen()) {
                longest = stack;
            } else if (stack.getMaxLen() == longest.getMaxLen()) {
                // if more than one are of equal length, pick the one that starts first
                if (stack.getStartSector() < longest.getStartSector())
                    longest = stack;
            }
        }
        
        _longestNonIntersectingAudioStreams = longest.getLongestCopy();

        // TODO: remove this message when this code has been well tested
        System.out.println("Selected " + _longestNonIntersectingAudioStreams.size() +
                " of the " + _aoAudioStreams.length + " audio streams:");
        for (DiscItemAudioStream aud : _longestNonIntersectingAudioStreams) {
            System.out.println("  " + aud.toString());
        }

        return _longestNonIntersectingAudioStreams;
    }

    public void fpsDump(FeedbackStream Feedback) throws IOException {

        Feedback.println("Generating fps dump.");

        PrintStream ps = new PrintStream("fps.txt");

        try {
            DiscItemVideoStream vid = this;
            final int LENGTH = vid.getSectorLength();
            for (int iSector = 0; iSector < LENGTH; iSector++) {
                CdSector sector = vid.getRelativeSector(iSector);
                IdentifiedSector isect = IdentifiedSector.identifySector(sector);
                if (isect instanceof IVideoSector) {
                    IVideoSector vidSect = (IVideoSector) isect;
                    ps.println(String.format("%-5d %-4d %d/%d",
                                            iSector,
                                            vidSect.getFrameNumber(),
                                            vidSect.getChunkNumber(),
                                            vidSect.getChunksInFrame() ));
                } else {
                    ps.println(String.format(
                            "%-5d X",
                            iSector));
                }

            }
        } finally {
            ps.close();
        }
    }

    public void frameInfoDump(final FeedbackStream Feedback) throws IOException {
        DiscItemVideoStream vidItem = this;


        FrameDemuxer demuxer;
        demuxer = new FrameDemuxer(vidItem.getWidth(), vidItem.getHeight(),
                                   vidItem.getStartSector(), vidItem.getEndSector())
        {
            protected void frameComplete(DemuxedFrame frame) throws IOException {
                frame.printStats(Feedback);
            }
        };

        Feedback.setLevel(FeedbackStream.MORE);

        for (int iSector = 0;
             iSector < vidItem.getSectorLength();
             iSector++)
        {
            try {
                IdentifiedSector sector = vidItem.getRelativeIdentifiedSector(iSector);
                if (sector instanceof IVideoSector) {
                    demuxer.feedSector((IVideoSector) sector);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        demuxer.flush();

    }

    public void replaceFrames(FeedbackStream Feedback, String sXmlFile) throws IOException, NotThisTypeException, MdecException {
        DiscItem item = this;

        ReplaceFrames replacers = new ReplaceFrames(sXmlFile);
        replacers.replaceFrames((DiscItemVideoStream) item, getSourceCD(), Feedback);
    }

    public PlayController makePlayController() throws LineUnavailableException, UnsupportedAudioFileException, IOException {

        if (hasAudio()) {

            List<DiscItemAudioStream> audios = getLongestNonIntersectingAudioStreams();
            ISectorAudioDecoder decoder;
            if (audios.size() == 1)
                decoder = audios.get(0).makeDecoder(1.0);
            else
                decoder = new AudioStreamsCombiner(audios, 1.0);

            int iStartSector = Math.min(decoder.getStartSector(), getStartSector());
            int iEndSector = Math.max(decoder.getEndSector(), getEndSector());

            return new PlayController(new MediaPlayer(this, decoder, iStartSector, iEndSector));
        } else {
            return new PlayController(new MediaPlayer(this));
        }
    }
}
