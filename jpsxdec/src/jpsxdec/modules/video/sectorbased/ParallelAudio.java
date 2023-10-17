/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.IndexId;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;

/** Manages audio streams that run alongside a video stream. */
public class ParallelAudio {

    private static final Logger LOG = Logger.getLogger(ParallelAudio.class.getName());

    /** {@link java.util.SortedSet} */
    private final TreeSet<DiscItemSectorBasedAudioStream> _audioStreams =
            new TreeSet<DiscItemSectorBasedAudioStream>();

    /** -1 = confirmed a mix of disc speed
     *   0 = yet to encounter any disc speed
     *   1 = x1
     *   2 = 2x. */
    private int _iAudioDiscSpeed = 0;

    public @CheckForNull DiscSpeed getAudioDiscSpeed() {
        switch (_iAudioDiscSpeed) {
            case 1: return DiscSpeed.SINGLE;
            case 2: return DiscSpeed.DOUBLE;
            default: return null;
        }
    }

    public boolean addChild(@Nonnull DiscItem other, int iParentRating) {
        if (iParentRating == 0)
            return false;

        // getParentRating should already confirm this is DiscItemAudioStream
        DiscItemSectorBasedAudioStream audItem = (DiscItemSectorBasedAudioStream) other;

        _audioStreams.add(audItem);

        // TODO: keep the list sorted in order found in disc index

        DiscSpeed audioItemDiscSpeed = audItem.getDiscSpeed();

        // if there is only 1 disc speed used by parallel audio, then
        // we can be confident the video should have the same speed
        if (audioItemDiscSpeed != null) {
            if (_iAudioDiscSpeed == 0)
                _iAudioDiscSpeed = audioItemDiscSpeed.getSpeed();
            else if (_iAudioDiscSpeed != audioItemDiscSpeed.getSpeed())
                _iAudioDiscSpeed = -1;
        }

        audItem.setPartOfVideo(true);
        return true;
    }

    public boolean setIndexId(@Nonnull IndexId childId) {
        for (DiscItemSectorBasedAudioStream audio : _audioStreams) {
            if (audio.setIndexId(childId)) // TODO: warn on rejection?
                childId = childId.createNext();
        }
        return true;
    }

    public int getChildCount() {
        return _audioStreams.size();
    }

    public @Nonnull Iterable<DiscItemSectorBasedAudioStream> getChildren() {
        return _audioStreams;
    }

    public boolean hasAudio() {
        return !_audioStreams.isEmpty();
    }

    public @CheckForNull List<DiscItemSectorBasedAudioStream> getParallelAudioStreams() {
        if (_audioStreams.isEmpty())
            return null;
        else
            return new ArrayList<DiscItemSectorBasedAudioStream>(_audioStreams);
    }

    public int getCount() {
        return _audioStreams.size();
    }


    private static class AudioStreamFormatBucket implements Comparator<DiscItemSectorBasedAudioStream>{
        private final TreeSet<DiscItemSectorBasedAudioStream> _streams = new TreeSet<DiscItemSectorBasedAudioStream>(this);
        private final DiscItemSectorBasedAudioStream _formatChecker;

        public AudioStreamFormatBucket(DiscItemSectorBasedAudioStream first) {
            _streams.add(first);
            _formatChecker = first;
        }

        public boolean addIfMatches(DiscItemSectorBasedAudioStream as) {
            if (!_formatChecker.hasSameFormat(as))
                return false;

            _streams.add(as);
            return true;
        }

        public int calculateLongest() {
            // https://en.wikipedia.org/wiki/Interval_scheduling
            Iterator<DiscItemSectorBasedAudioStream> it = _streams.iterator();
            DiscItemSectorBasedAudioStream earliestFinishingTime = it.next();
            int iLength = earliestFinishingTime.getSectorLength();
            while (it.hasNext()) {
                DiscItemSectorBasedAudioStream contender = it.next();
                if (contender.overlaps(earliestFinishingTime)) {
                    it.remove();
                } else {
                    earliestFinishingTime = contender;
                    iLength += earliestFinishingTime.getSectorLength();
                }
            }
            return iLength;
        }

        /** Sort by earliest finishing time. */
        @Override
        public int compare(DiscItemSectorBasedAudioStream o1, DiscItemSectorBasedAudioStream o2) {
            return Integer.compare(o1.getEndSector(), o2.getEndSector());
        }

        private ArrayList<DiscItemSectorBasedAudioStream> getStreams() {
            ArrayList<DiscItemSectorBasedAudioStream> streams = new ArrayList<DiscItemSectorBasedAudioStream>(_streams);
            // sort according to our convention
            Collections.sort(streams);
            return streams;
        }
    }

    @CheckForNull
    private transient ArrayList<DiscItemSectorBasedAudioStream>
            _longestNonIntersectingAudioStreams;

    public @CheckForNull List<DiscItemSectorBasedAudioStream> getLongestNonIntersectingAudioStreams() {
        if (!hasAudio())
            return null;

        if (_longestNonIntersectingAudioStreams != null)
            return _longestNonIntersectingAudioStreams;

        // I'd say maybe 99% of cases will only have 1
        if (_audioStreams.size() == 1) {
            _longestNonIntersectingAudioStreams = new ArrayList<DiscItemSectorBasedAudioStream>(1);
            _longestNonIntersectingAudioStreams.add(_audioStreams.first());
            return _longestNonIntersectingAudioStreams;
        }

        // === find the longest combination of parallel audio streams ===

        // group all the streams into buckets of the same format
        ArrayList<AudioStreamFormatBucket> streamBuckets = new ArrayList<AudioStreamFormatBucket>();
        Iterator<DiscItemSectorBasedAudioStream> streamIt = _audioStreams.iterator();
        // add the first stream
        streamBuckets.add(new AudioStreamFormatBucket(streamIt.next()));

        NextStream:
        while (streamIt.hasNext()) {
            DiscItemSectorBasedAudioStream as = streamIt.next();
            // try to add this stream to each bucket
            for (AudioStreamFormatBucket bucket : streamBuckets) {
                if (bucket.addIfMatches(as))
                    continue NextStream; // found a bucket, onto the next stream
            }
            // no existing bucket, create a new one
            streamBuckets.add(new AudioStreamFormatBucket(as));
        }

        // find the longest bucket
        Iterator<AudioStreamFormatBucket> bucketIt = streamBuckets.iterator();
        AudioStreamFormatBucket longestBucket = bucketIt.next();
        int iLongestBucket = longestBucket.calculateLongest();
        while (bucketIt.hasNext()) {
            AudioStreamFormatBucket contender = bucketIt.next();
            int iContenderLength = contender.calculateLongest();
            if (iContenderLength > iLongestBucket) {
                longestBucket = contender;
                iLongestBucket = iContenderLength;
            }
        }

        _longestNonIntersectingAudioStreams = longestBucket.getStreams();

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

}
