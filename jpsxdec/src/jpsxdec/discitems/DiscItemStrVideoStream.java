/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.VideoSaverBuilderStr;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.Fraction;
import jpsxdec.util.Maths;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

/** Represents sector-based PlayStation video streams. */
public abstract class DiscItemStrVideoStream extends DiscItemVideoStream {

    private static final Logger LOG = Logger.getLogger(DiscItemStrVideoStream.class.getName());

    private static final String FRAMES_KEY = "Frames";
    /** First video frame number. */
    @Nonnull
    private final FrameNumber _startFrame;
    /** Last video frame number. */
    @Nonnull
    private final FrameNumber _endFrame;

    private static final String SECTORSPERFRAME_KEY = "Sectors/Frame";
    private final int _iSectors;
    private final int _iPerFrame;

    private static final String FRAME1_LAST_SECTOR_KEY = "1st frame end sector";
    /** The last sector of frame 1 relative to the start sector.
     * Important for syncing audio and video. */
    private final int _iFirstFrameLastSector;

    private static final String DISC_SPEED_KEY = "Disc Speed";
    private int _iDiscSpeed = -1;
    private int _iAudioDiscSpeed = 0;

    private final SortedSet<DiscItemAudioStream> _audioStreams =
            new TreeSet<DiscItemAudioStream>();

    public DiscItemStrVideoStream(@Nonnull CdFileSectorReader cd,
                                  int iStartSector, int iEndSector,
                                  int iWidth, int iHeight,
                                  int iFrameCount,
                                  @Nonnull FrameNumberFormat frameNumberFormat,
                                  @Nonnull FrameNumber startFrame,
                                  @Nonnull FrameNumber endFrame,
                                  int iSectors, int iPerFrame,
                                  int iFirstFrameLastSector)
    {
        super(cd, iStartSector, iEndSector,
              iWidth, iHeight, 
              iFrameCount,
              frameNumberFormat);

        _startFrame = startFrame;
        _endFrame = endFrame;

        // ensure the sectors/frame fraction is simplied
        int iGcd = Maths.gcd(iSectors, iPerFrame);
        _iSectors = iSectors / iGcd;
        _iPerFrame = iPerFrame / iGcd;

        _iFirstFrameLastSector = iFirstFrameLastSector;
    }

    public DiscItemStrVideoStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws NotThisTypeException
    {
        super(cd, fields);

        FrameNumber[] ao = FrameNumber.parseRange(fields.getString(FRAMES_KEY));
        _startFrame = ao[0];
        _endFrame = ao[1];

        long[] alng = fields.getFraction(SECTORSPERFRAME_KEY);
        _iSectors = (int)alng[0];
        _iPerFrame = (int)alng[1];

        _iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
        _iFirstFrameLastSector = fields.getInt(FRAME1_LAST_SECTOR_KEY);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addString(FRAMES_KEY, FrameNumber.toRange(_startFrame, _endFrame));
        serial.addFraction(SECTORSPERFRAME_KEY, _iSectors, _iPerFrame);
        serial.addNumber(FRAME1_LAST_SECTOR_KEY, _iFirstFrameLastSector);

        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed > 0)
            serial.addNumber(DISC_SPEED_KEY, iDiscSpeed);
        return serial;
    }

    public @Nonnull FrameNumber getStartFrame() {
        return _startFrame;
    }

    public @Nonnull FrameNumber getEndFrame() {
        return _endFrame;
    }

    @Override
    public int getDiscSpeed() {
        return _iDiscSpeed > 0 ? _iDiscSpeed : 
               _iAudioDiscSpeed > 0 ? _iAudioDiscSpeed :
                -1;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + _iFirstFrameLastSector;
    }
    
    @Override
    public @Nonnull Fraction getSectorsPerFrame() {
        return new Fraction(_iSectors, _iPerFrame);
    }
    

    @Override
    public boolean addChild(@Nonnull DiscItem other) {
        if (getParentRating(other) == 0)
            return false;

        // getParentRating should already confirm this is DiscItemAudioStream
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
    public boolean setIndexId(@Nonnull IndexId id) {
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
    public @Nonnull Iterable<DiscItem> getChildren() {
        return (Iterable)_audioStreams;
    }

    public boolean hasAudio() {
        return !_audioStreams.isEmpty();
    }

    public @CheckForNull List<DiscItemAudioStream> getParallelAudioStreams() {
        if (_audioStreams.isEmpty())
            return null;
        else
            return new ArrayList<DiscItemAudioStream>(_audioStreams);
    }

    @Override
    public double getApproxDuration() {
        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed < 1)
            iDiscSpeed = 2;
        return getSectorLength() / (double)(iDiscSpeed * 75);
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        int iDiscSpeed = getDiscSpeed();
        int iFrameCount = getFrameCount();
        if (iDiscSpeed > 0) {
            int iSectorsPerSecond = iDiscSpeed * 75;
            Date secs = new Date(0, 0, 0, 0, 0, Math.max(getSectorLength() / iSectorsPerSecond, 1));
            return I.GUI_STR_VIDEO_DETAILS(
                          getWidth(), getHeight(),
                          iFrameCount,
                          Fraction.divide(iSectorsPerSecond, getSectorsPerFrame()).asDouble(),
                          secs);
        } else {
            Date secs150 = new Date(0, 0, 0, 0, 0, Math.max(getSectorLength() / 150, 1));
            Date secs75 = new Date(0, 0, 0, 0, 0, Math.max(getSectorLength() / 75, 1));
            return I.GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS(
                          getWidth(), getHeight(),
                          iFrameCount,
                          Fraction.divide(150, getSectorsPerFrame()).asDouble(),
                          secs150,
                          Fraction.divide(75, getSectorsPerFrame()).asDouble(),
                          secs75);
        }                
    }
    

    @Override
    public @Nonnull VideoSaverBuilderStr makeSaverBuilder() {
        return new VideoSaverBuilderStr(this);
    }


    private static class LongestStack {

        @Nonnull
        private final DiscItemAudioStream[] _aoAudioStreams;

        private final ArrayList<DiscItemAudioStream> _stack =
                new ArrayList<DiscItemAudioStream>();

        private int _iCurLen = 0;
        private int _iMaxLen = 0;
        private int _iStartSector = -1;
        
        private final ArrayList<DiscItemAudioStream> _longestCopy =
                new ArrayList<DiscItemAudioStream>();

        public LongestStack(@Nonnull Collection<DiscItemAudioStream> audioStreams) {
            _aoAudioStreams = audioStreams.toArray(
                    new DiscItemAudioStream[audioStreams.size()]);
        }

        private @Nonnull ArrayList<DiscItemAudioStream> getLongest() {
            return _longestCopy;
        }

        private void push(@Nonnull DiscItemAudioStream o) {
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

        private boolean matchesFormat(@Nonnull DiscItemAudioStream potential) {
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

    @CheckForNull
    private transient ArrayList<DiscItemAudioStream>
            _longestNonIntersectingAudioStreams;

    public @CheckForNull List<DiscItemAudioStream> getLongestNonIntersectingAudioStreams() {
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

        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Selected {0,number,#} of the {1,number,#} audio streams:",
                    new Object[] {
                        _longestNonIntersectingAudioStreams.size(),
                        _audioStreams.size()
                    }
            );
            for (int i = 0; i < _longestNonIntersectingAudioStreams.size(); i++) {
                LOG.log(Level.INFO, "{0,number,#}. {1}", new Object[] {
                    i, _longestNonIntersectingAudioStreams.get(i)
                });
            }
        }

        return _longestNonIntersectingAudioStreams;
    }

    abstract public int splitAudio(@Nonnull DiscItemXaAudioStream audio);
    
    abstract public void fpsDump(@Nonnull PrintStream ps) throws IOException;

    public void fpsDump2(@Nonnull final PrintStream ps) throws IOException {
        ISectorFrameDemuxer demuxer = makeDemuxer();
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
            public void frameComplete(IDemuxedFrame frame) throws IOException {
                ps.println((frame.getStartSector()-getStartSector())+"-"+
                           (frame.getEndSector()-getStartSector()));
            }
        });
        IdentifiedSectorIterator it = identifiedSectorIterator();
        while (it.hasNext()) {
            IdentifiedSector isect = it.next();
            if (isect != null)
                demuxer.feedSector(isect, LOG);
        }
    }


    @Override
    public @Nonnull PlayController makePlayController() {

        if (hasAudio()) {

            List<DiscItemAudioStream> audios = getLongestNonIntersectingAudioStreams();
            if (audios == null)
                throw new IllegalStateException("Should never happen");
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
