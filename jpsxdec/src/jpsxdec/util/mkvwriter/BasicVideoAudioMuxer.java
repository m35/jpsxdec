/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2024-2026  Michael Sabin
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

package jpsxdec.util.mkvwriter;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/**
 * A slightly more than minimal implementation of a video, and optionally audio, multiplexer.
 * <p>
 * The simplest muxer implementation would blindly write frames and audio directly to the
 * mkv as it arrives.
 * <p>
 * This is a bit more sophisticated. It makes sure the frames and audio are sorted by
 * their timestamps, and combines any frames that have the same timestamp
 * as an audio packet into a single cluster.
 * <p>
 * Even more sophisticated would be to buffer the audio as a stream so every cluster could be written
 * with a frame and just the matching audio for that frame. But that's not needed for how it's
 * used in jPSXdec, so kept this implementation simple.
 */
class BasicVideoAudioMuxer implements Closeable {

    private static final Logger LOG = Logger.getLogger(BasicVideoAudioMuxer.class.getName());

    private final TreeSet<IHasTicks> _blockQueue = new TreeSet<IHasTicks>(new IHasTicksComparator());
    @Nonnull
    private final BasicMkvWriter _mkvWriter;
    @Nonnull
    private final MkvVideoFormat _videoFormat;
    @CheckForNull
    private final MkvAudioFormat _audioFormat;

    @CheckForNull
    private MkvTimestampScale.TicksError _lastFrameTime = null;
    @CheckForNull
    private TicksSpan _lastAudioSpan = null;

    // TODO implement some buffer limit so OOM doesn't become the limit
    // can just flush whatever we have even if the timestamps are out of sequence

    public BasicVideoAudioMuxer(@Nonnull BasicMkvWriter mkvWriter,
                                @Nonnull MkvVideoFormat videoFormat,
                                @CheckForNull MkvAudioFormat audioFormat)
    {
        _mkvWriter = mkvWriter;

        _videoFormat = videoFormat;
        _audioFormat = audioFormat;
    }

    public void addFrame(@Nonnull byte[] abFrame, @Nonnull Fraction timestampInSeconds) throws IOException {

        if (timestampInSeconds.compareTo(Fraction.ZERO) < 0)
            throw new IllegalArgumentException("Negative timestamp " + timestampInSeconds);

        if (!_videoFormat.isExactIfConstantFps(timestampInSeconds)) {
            throw new IllegalArgumentException("Frame timestamp " + timestampInSeconds + " not a multiple of the video CFR " + _videoFormat.getConstantSecondsPerFrame());
        }

        QueuedFrame frame = new QueuedFrame(abFrame, timestampInSeconds);

        if (_lastFrameTime != null) {
            frame.assertThisFrameIsAfterPrevious(_lastFrameTime);
        }

        if (!_blockQueue.add(frame))
            throw new IllegalArgumentException("A frame at time " + timestampInSeconds + " already exists in the queue");

        _lastFrameTime = frame.frameStart;

        flushQueuedBlocks(false);
    }


    public void addAudio(@Nonnull byte[] abAudio, @Nonnull Fraction timestampInSeconds) throws IOException {
        if (_audioFormat == null)
            throw new UnsupportedOperationException();

        if (timestampInSeconds.compareTo(Fraction.ZERO) < 0)
            throw new IllegalArgumentException("Negative timestamp " + timestampInSeconds);

        if (abAudio.length == 0) {
            LOG.warning("Received an empty audio packet, ignoring");
            return;
        }

        if (_lastAudioSpan == null) {
            if (!timestampInSeconds.equals(Fraction.ZERO))
                throw new IllegalArgumentException("First audio timestamp should be 0 but is " + timestampInSeconds);
        }

        QueuedAudio audio = new QueuedAudio(abAudio, timestampInSeconds);

        if (_lastAudioSpan != null) {
            audio.assertAtEndOfPrevious(_lastAudioSpan);
        }

        if (!_blockQueue.add(audio))
            // this should also be caught in the above time check
            throw new IllegalArgumentException("An audio packet at time " + timestampInSeconds + " already exists in the queue");

        _lastAudioSpan = audio.audioSpan;

        flushQueuedBlocks(false);
    }


    public void addSilence(int iSampleFrameCount, @Nonnull Fraction timestampInSeconds) throws IOException {
        if (_audioFormat == null)
            throw new UnsupportedOperationException();

        byte[] abSilence = _audioFormat.generateSilence(iSampleFrameCount);
        addAudio(abSilence, timestampInSeconds);
    }

    public Fraction getAudioTime() {
        if (_lastAudioSpan == null) {
            return Fraction.ZERO;
        } else {
            return _lastAudioSpan.spanEnd.timestampInSeconds;
        }
    }

    private void flushQueuedBlocks(boolean blnCloseStream) throws IOException {

        if (_audioFormat == null || blnCloseStream) {
            // If no audio, can write frame immediately
            // Or at the end, write everything remaining
            flushThisManyBlocks(_blockQueue.size());
            return;
        }

        String sSequence = queueToCharacterSequence();

        int iFlushThisManyBlocks = countBlocksToFlush(sSequence);

        if (iFlushThisManyBlocks == 0)
            return;

        flushThisManyBlocks(iFlushThisManyBlocks);
    }

    /** Creates a string of 'a' (audio) and 'f' (frame) from the items in the queue.
     * Much easier to debug rather than looking at the queue directly. */
    private @Nonnull String queueToCharacterSequence() {
        StringBuilder blockSequence = new StringBuilder();
        for (IHasTicks hasTime : _blockQueue) {
            char cBlockType;
            if (hasTime instanceof QueuedAudio)
                cBlockType = 'a';
            else if (hasTime instanceof QueuedFrame)
                cBlockType = 'f';
            else
                throw new IllegalStateException("???");
            blockSequence.append(cBlockType);
        }
        return blockSequence.toString();
    }


    /** Analyzes the queue entries as a string (much easier to debug) and finds how
     * many are ready to be flush to the mkv writer. */
    static int countBlocksToFlush(@Nonnull String sBlockTypeSequence) {
        // We could just write each block immediately, but it's nice that
        // they're sorted, and keeps mkvalidator from warning about non-sequential clusters.
        // "WRN0C2: The timecode of the Cluster at ##### is not incrementing (may be intentional)"

        /*
        This effectively performs this regex:
            ".*af+a"
        In English, find the last instance where the are frames surrounded by audio.
        The .* does a greedy match to find the last instance. However it is EXTREMELY
        slow.

        This implementation walks backward from the last character searching for
        1. The last 'a' in the string
        2. Any number of 'f' before that
        3. First 'a' before those 'f'

        It then returns the index of the last 'f', to flush all blocks upto but
        not including that last 'f'.

        Why ".*af+a" ?

        Given all frame blocks are in their own sequential order,
        and all audio blocks are in their own sequential order, but those orders
        are independent of each other. They can often get shuffled around as they are
        added. e.g. this sequence:
            Frame 1 with timestamp 0
            Frame 2 with timestamp 2
            Audio 1 with timestamp 1
        Before the audio is added, the queue will be [frame 1, frame 2]. After the
        audio is added, the queue will be [frame 1, audio 1, frame 2].

        Let's examine some sequences where we can't confirm they order of blocks:

        "aa" A frame could arrive before, in the middle, or after the audio blocks.

        "ff" An audio block could arrive before, in the middle, or after the frames.

        "af" An audio block could arrive after the 'a' but before the 'f'.

        "fa" A frame could arrive after the 'f' but before the 'a'.

        So we need at least 3 blocks to know for sure.

        "af+a" There's no way for an audio to arrive before the last audio, so all the
        frames before that last frame, along with the preceding audio, and everything
        before that can be flushed. This would leave "fa"

        Why not "fa+f"? This is due to how the blocks are sorted in the queue.

        We need to watch for when a frame block has the same timestamp as an audio block.
        In those cases, we need to merge those adjacent blocks to be written together in
        the same cluster. The queue sorts those cases by putting the frame first.
        Using "fa+f" would leave "af". That could slice between the audio and video blocks
        with the same timestamp.
        */

        if (sBlockTypeSequence.length() < 3)
            return 0;

        final int FOUND_A = 1, FOUND_FA = 2, FOUND_AFA = 3;

        int iState = 0;
        int iChopPoint = -9999999; // arbitrary invalid value
        for (int i = sBlockTypeSequence.length() - 1; i >= 0 && iState < 3; i--) {
            char c = sBlockTypeSequence.charAt(i);
            switch (iState) {
                case 0:
                    if (c == 'a') {
                        iState = FOUND_A;
                    }
                    break;
                case 1:
                    if (c == 'f') {
                        iState = FOUND_FA;
                        iChopPoint = i;
                    }
                    break;
                case 2:
                    if (c == 'a') {
                        iState = FOUND_AFA;
                    }
                    break;
            }
        }

        if (iState == FOUND_AFA) {
            return iChopPoint;
        } else {
            return 0;
        }
    }

    /** Gathers the blocks into clusters to flush, then sends them to the mkv writer. */
    private void flushThisManyBlocks(int iBlockCount) throws IOException {
        // Remove blocks from queue and add to list to flush
        ArrayList<IHasTicks> blocksToFlush = new ArrayList<IHasTicks>();
        for (int i = 0; i < iBlockCount; i++) {
            blocksToFlush.add(_blockQueue.pollFirst());
        }

        // Here we need to find if there is a pair of frame and audio that have
        // the same timestamp so they can be flushed together in one cluster.
        LinkedHashMap<Long, List<IHasTicks>> flushClusters = blocksToFlush
                .stream()
                .collect(
                    Collectors.groupingBy(
                        (IHasTicks hasTime) -> hasTime.getTimestampTicks(),
                        LinkedHashMap::new, // <-- to maintain order
                        Collectors.toList()
                    )
                );

        for (List<IHasTicks> clusterGroup : flushClusters.values()) {

            if (clusterGroup.size() != 1 && clusterGroup.size() != 2)
                throw new IllegalStateException("Should only be 1 or 2 blocks to combine");

            IHasTicks block = clusterGroup.get(0);
            QueuedAudio audio = null;
            QueuedFrame frame = null;

            if (block instanceof QueuedAudio)
                audio = (QueuedAudio) block;
            else if (block instanceof QueuedFrame)
                frame = (QueuedFrame) block;
            else
                throw new IllegalStateException("For completeness");

            if (clusterGroup.size() == 2) {
                block = clusterGroup.get(1);
                if (block instanceof QueuedAudio) {
                    if (audio != null)
                        throw new IllegalStateException("Can't have 2 audio blocks with the same time");
                    audio = (QueuedAudio) block;
                } else if (block instanceof QueuedFrame) {
                    if (frame != null)
                        throw new IllegalStateException("Can't have 2 frames with the same time");
                    frame = (QueuedFrame) block;
                } else {
                    throw new IllegalStateException("For completeness");
                }
            }

            writeCluster(frame, audio);
        }
    }

    private void writeCluster(@CheckForNull QueuedFrame frame,
                              @CheckForNull QueuedAudio audio)
            throws IOException
    {
        byte[] abFrame = null;
        long lngFrameTick = -1;
        if (frame != null) {
            abFrame = frame.abFrame;
            lngFrameTick = frame.getTimestampTicks();
        }

        byte[] abAudio = null;
        long lngAudioTick = -1;
        if (audio != null) {
            abAudio = audio.abAudio;
            lngAudioTick = audio.getTimestampTicks();
        }

        _mkvWriter.writeCluster(abFrame, lngFrameTick, abAudio, lngAudioTick);
    }


    @Override
    public void close() throws IOException {

        flushQueuedBlocks(true);

        /*
        Value for the mkv Duration element.
        "Duration of the Segment in nanoseconds based on TimestampScale"

        I'm not really sure how to exactly calculate it. I just track
        the max value of the Cluster timestamps + their duration.
        But when I remux with ffmpeg or mkvtoolnix, they have different values.
        */
        Fraction maxTime;

        if (_lastFrameTime != null) {
            Fraction finalFrameDuration;
            // Add to the last frame timestamp the duration of the frame
            if (_videoFormat.hasConstantFrameRate()) {
                finalFrameDuration = _videoFormat.getConstantSecondsPerFrame();
            } else {
                // length of the final frame is unknown when variable frame rate
                finalFrameDuration = Fraction.ZERO;
            }
            maxTime = _lastFrameTime.timestampInSeconds.add(finalFrameDuration);
        } else {
            LOG.severe("No frames were written to mkv!?");
            maxTime = Fraction.ZERO;
        }

        if (_lastAudioSpan != null) {
            if (_lastAudioSpan.spanEnd.timestampInSeconds.compareTo(maxTime) > 0) {
                maxTime = _lastAudioSpan.spanEnd.timestampInSeconds;
            }
        }

        MkvTimestampScale.TicksError maxTicks = MkvTimestampScale.toTicks(maxTime);

        _mkvWriter.finish(maxTicks.exactTicks.asFloat());
        _mkvWriter.close();
    }


    private static class TicksSpan {
        public final MkvTimestampScale.TicksError spanStart;
        public final MkvTimestampScale.TicksError spanEnd;

        public TicksSpan(@Nonnull Fraction startSecond, @Nonnull Fraction endSecond) {
            this.spanStart = MkvTimestampScale.toTicks(startSecond);
            this.spanEnd = MkvTimestampScale.toTicks(endSecond);
        }
    }


    private interface IHasTicks {
        long getTimestampTicks();
    }

    private static class QueuedFrame implements IHasTicks {
        @Nonnull
        public final byte[] abFrame;
        @Nonnull
        public final MkvTimestampScale.TicksError frameStart;

        public QueuedFrame(@Nonnull byte[] abFrame, @Nonnull Fraction timestampInSeconds) {
            this.abFrame = abFrame;
            frameStart = MkvTimestampScale.toTicks(timestampInSeconds);
        }

        public void assertThisFrameIsAfterPrevious(@Nonnull MkvTimestampScale.TicksError previousFrameTime) {
            if (frameStart.timestampInSeconds.compareTo(previousFrameTime.timestampInSeconds) <= 0)
                throw new IllegalArgumentException("Video frame timestamp is <= to the previous one");

            if (previousFrameTime.lngRoundedTicks == getTimestampTicks()) {
                // This is a very extreme case where the difference between the frame times is less than the timestamp tick resolution
                throw new IllegalArgumentException("A video packet at tick " +
                        previousFrameTime.lngRoundedTicks + " matches the previous tick");
            }

        }

        @Override
        public long getTimestampTicks() {
            return frameStart.lngRoundedTicks;
        }

        @Override
        public String toString() {
            return "F " + getTimestampTicks();
        }
    }

    private class QueuedAudio implements IHasTicks {
        @Nonnull
        public final byte[] abAudio;
        @Nonnull
        public final TicksSpan audioSpan;

        public QueuedAudio(@Nonnull byte[] abAudio, @Nonnull Fraction timestampInSeconds) {
            this.abAudio = abAudio;
            Fraction duration = _audioFormat.audioBytesToSeconds(this.abAudio.length);
            Fraction endSecond = timestampInSeconds.add(duration);
            audioSpan = new TicksSpan(timestampInSeconds, endSecond);
        }

        public void assertAtEndOfPrevious(@Nonnull TicksSpan prevAudioSpan) {
            if (!audioSpan.spanStart.timestampInSeconds.equals(prevAudioSpan.spanEnd.timestampInSeconds)) {
                throw new IllegalArgumentException("Given audio timestamp " + audioSpan.spanStart.timestampInSeconds +
                        " does not match the end of the previous timestamp: " + prevAudioSpan.spanEnd.timestampInSeconds);
            }

            if (prevAudioSpan.spanStart.lngRoundedTicks == getTimestampTicks()) {
                // This is a very extreme case where the audio sample rate is greater than the timestamp tick resolution
                // the previous audio packet was tiny
                // We could add an audio sample rate limit in the constructor, but I'd keep this check here anyway.
                throw new IllegalArgumentException("An audio packet at tick " +
                        prevAudioSpan.spanStart.lngRoundedTicks + " matches the previous tick");
            }
        }

        @Override
        public long getTimestampTicks() {
            return audioSpan.spanStart.lngRoundedTicks;
        }

        @Override
        public String toString() {
            return "A " + getTimestampTicks();
        }
    }

    private static class IHasTicksComparator implements Comparator<IHasTicks> {
        @Override
        public int compare(@Nonnull IHasTicks o1, @Nonnull IHasTicks o2) {

            int i = Long.compare(o1.getTimestampTicks(), o2.getTimestampTicks());
            if (i == 0) {
                // make sure video always comes first
                if (o1 instanceof QueuedFrame && o2 instanceof QueuedAudio) {
                    i = -1;
                } else if (o1 instanceof QueuedAudio && o2 instanceof QueuedFrame) {
                    i = 1;
                } else {
                    // Both are the same type at the same time
                    assert (o1 instanceof QueuedFrame && o2 instanceof QueuedFrame) ||
                           (o1 instanceof QueuedAudio && o2 instanceof QueuedAudio);
                    // Consider them equal
                }
            }
            return i;
        }
    }


}
