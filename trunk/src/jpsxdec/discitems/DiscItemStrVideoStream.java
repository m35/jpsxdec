/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.VideoSaverBuilderStr;
import jpsxdec.sectors.IVideoSector;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.Fraction;
import jpsxdec.util.Maths;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

/** Represents all variations of PlayStation video streams. */
public class DiscItemStrVideoStream extends DiscItemVideoStream {

    private static final Logger LOG = Logger.getLogger(DiscItemStrVideoStream.class.getName());

    public static final String TYPE_ID = "Video";
    
    private static final String DISC_SPEED_KEY = "Disc Speed";
    private static final String SECTORSPERFRAME_KEY = "Sectors/Frame";
    private static final String FRAME1_LAST_SECTOR_KEY = "Frame 1 last sector";

    /** The last sector of frame 1 relative to the start sector.
     * Important for syncing audio and video. */
    private final int _iFrame1LastSector;
    
    private final int _iSectors;
    private final int _iPerFrame;

    private int _iDiscSpeed = -1;
    private int _iAudioDiscSpeed = 0;

    private final SortedSet<DiscItemAudioStream> _audioStreams =
            new TreeSet<DiscItemAudioStream>();

    public DiscItemStrVideoStream(int iStartSector, int iEndSector,
                                  int iStartFrame, int iEndFrame,
                                  int iWidth, int iHeight,
                                  int iSectors, int iPerFrame,
                                  int iFrame1LastSector)
    {
        super(iStartSector, iEndSector, 
              iWidth, iHeight, 
              iStartFrame, iEndFrame);
        // ensure the sectors/frame fraction is simplied
        int iGcd = Maths.gcd(iSectors, iPerFrame);
        _iSectors = iSectors / iGcd;
        _iPerFrame = iPerFrame / iGcd;

        _iFrame1LastSector = iFrame1LastSector;
    }

    public DiscItemStrVideoStream(SerializedDiscItem fields) throws NotThisTypeException
    {
        super(fields);
        
        long[] alng = fields.getFraction(SECTORSPERFRAME_KEY);
        _iSectors = (int)alng[0];
        _iPerFrame = (int)alng[1];

        _iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
        _iFrame1LastSector = fields.getInt(FRAME1_LAST_SECTOR_KEY);
    }

    @Override
    public SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addFraction(SECTORSPERFRAME_KEY, _iSectors, _iPerFrame);
        serial.addNumber(FRAME1_LAST_SECTOR_KEY, _iFrame1LastSector);

        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed > 0)
            serial.addNumber(DISC_SPEED_KEY, iDiscSpeed);
        return serial;
    }

    @Override
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public int getDiscSpeed() {
        return _iDiscSpeed > 0 ? _iDiscSpeed : 
               _iAudioDiscSpeed > 0 ? _iAudioDiscSpeed :
                -1;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + _iFrame1LastSector;
    }
    
    @Override
    public Fraction getSectorsPerFrame() {
        return new Fraction(_iSectors, _iPerFrame);
    }
    

    @Override
    public int getParentRating(DiscItem child) {
        if (!(child instanceof DiscItemAudioStream))
            return 0;

        int iOverlapPercent = child.getOverlap(this)*100 / child.getSectorLength();
        if (iOverlapPercent > 0)
            iOverlapPercent += 100;
        return iOverlapPercent;
    }

    @Override
    public boolean addChild(DiscItem other) {
        if (getParentRating(other) == 0)
            return false;

        DiscItemAudioStream audItem = (DiscItemAudioStream) other;

        _audioStreams.add(audItem);

        // TODO: keep the list sorted in order found in disc index

        // if there is only 1 disc speed used by parallel audio, then
        // we can be confident the video should have the same speed
        if (_iAudioDiscSpeed == 0) {
            _iAudioDiscSpeed = audItem.getDiscSpeed();
        } else if (_iAudioDiscSpeed != -1) {
            if (_iAudioDiscSpeed != audItem.getDiscSpeed())
                _iAudioDiscSpeed = -1;
        }

        audItem.setPartOfVideo(true);
        return true;
    }

    @Override
    public boolean setIndexId(IndexId id) {
        IndexId childId = id.createChild();
        super.setIndexId(id);
        for (DiscItemAudioStream audio : _audioStreams) {
            if (audio.setIndexId(childId)) // TODO: warn on rejection?
                childId = childId.createNext();
        }
        return true;
    }

    @Override
    public int getChildCount() {
        return _audioStreams.size();
    }

    /** {@inheritDoc}
     * <p>
     * Actually returns Iterable<DiscItemAudioStream>. */
    @Override
    public Iterable<DiscItem> getChildren() {
        return (Iterable)_audioStreams;
    }

    public boolean hasAudio() {
        return !_audioStreams.isEmpty();
    }

    public List<DiscItemAudioStream> getParallelAudioStreams() {
        if (_audioStreams.isEmpty())
            return null;
        else
            return new ArrayList<DiscItemAudioStream>(_audioStreams);
    }

    @Override
    public String getInterestingDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%dx%d, %d frames, ",
                getWidth(), getHeight(), getEndFrame() - getStartFrame() + 1));
        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed > 0) {
            formatFps(sb, iDiscSpeed * 75);
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

    @Override
    public VideoSaverBuilderStr makeSaverBuilder() {
        return new VideoSaverBuilderStr(this);
    }


    private static class LongestStack {

        private final DiscItemAudioStream[] _aoAudioStreams;

        private final ArrayList<DiscItemAudioStream> _stack =
                new ArrayList<DiscItemAudioStream>();

        private int _iCurLen = 0;
        private int _iMaxLen = 0;
        private int _iStartSector = -1;
        
        private final ArrayList<DiscItemAudioStream> _longestCopy =
                new ArrayList<DiscItemAudioStream>();

        public LongestStack(Collection<DiscItemAudioStream> audioStreams) {
            _aoAudioStreams = audioStreams.toArray(
                    new DiscItemAudioStream[audioStreams.size()]);
        }

        private ArrayList<DiscItemAudioStream> getLongest() {
            return _longestCopy;
        }

        private void push(DiscItemAudioStream o) {
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

        private void pop() {
            DiscItemAudioStream removed = _stack.remove(_stack.size() - 1);
            _iCurLen -= removed.getSectorLength();
        }

        private boolean matchesFormat(DiscItemAudioStream potential) {
            return _stack.isEmpty() || _stack.get(0).hasSameFormat(potential);
        }

        private void findLongest(final int iIndex) {
            for (int i = iIndex; i < _aoAudioStreams.length; i++) {
                DiscItemAudioStream potential = _aoAudioStreams[i];
                if (!matchesFormat(potential))
                    continue;
                boolean blnOverlapsAny = false;
                for (DiscItemAudioStream audItem : _stack) {
                    if (potential.overlaps(audItem)) {
                        blnOverlapsAny = true;
                        break;
                    }
                }
                if (!blnOverlapsAny) {
                    push(potential);
                    findLongest(i+1);
                    pop();
                }
            }
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
        if (_audioStreams.size() == 1) {
            _longestNonIntersectingAudioStreams = new ArrayList<DiscItemAudioStream>(1);
            _longestNonIntersectingAudioStreams.add(_audioStreams.first());
            return _longestNonIntersectingAudioStreams;
        }

        // find the longest combination of parallel audio streams
        LongestStack longest = new LongestStack(_audioStreams);
        longest.findLongest(0);
        
        _longestNonIntersectingAudioStreams = longest.getLongest();

        // TODO: remove this message when this code has been well tested
        System.out.println("Selected " + _longestNonIntersectingAudioStreams.size() +
                " of the " + _audioStreams.size() + " audio streams:");
        for (DiscItemAudioStream aud : _longestNonIntersectingAudioStreams) {
            System.out.println("  " + aud.toString());
        }

        return _longestNonIntersectingAudioStreams;
    }

    public void fpsDump(PrintStream ps) throws IOException {
        final int LENGTH = getSectorLength();
            for (int iSector = 0; iSector < LENGTH; iSector++) {
            CdSector sector = getRelativeSector(iSector);
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
    }

    @Override
    public ISectorFrameDemuxer makeDemuxer() {
        return new FrameDemuxer(getWidth(), getHeight(), 
                                getStartSector(), getEndSector());
    }
    
    @Override
    public PlayController makePlayController() {

        if (hasAudio()) {

            List<DiscItemAudioStream> audios = getLongestNonIntersectingAudioStreams();
            ISectorAudioDecoder decoder;
            if (audios.size() == 1)
                decoder = audios.get(0).makeDecoder(1.0);
            else
                decoder = new AudioStreamsCombiner(audios, 1.0);

            int iStartSector = Math.min(decoder.getStartSector(), getStartSector());
            int iEndSector = Math.max(decoder.getEndSector(), getEndSector());

            return new PlayController(new MediaPlayer(this, makeDemuxer(), decoder, iStartSector, iEndSector));
        } else {
            return new PlayController(new MediaPlayer(this, makeDemuxer()));
        }
    }
    
}
