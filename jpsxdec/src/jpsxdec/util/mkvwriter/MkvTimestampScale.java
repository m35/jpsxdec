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

import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;


/**
 * TimestampScale.
 *<p>
 * Base unit for Segment Ticks and Track Ticks, in nanoseconds. A {@code TimestampScale} value
 * of 1000000 means scaled timestamps in the Segment are expressed in milliseconds; see
 * timestamps on how to interpret timestamps.
 *<p>
 * https://www.matroska.org/technical/elements.html#TimestampScale
 *<p>
 * The {@code TimestampScale} is a floating value, which is usually 1.0. But when it’s not, the
 * multiplied Block Timestamp is a floating values in nanoseconds. The Matroska Reader
 * SHOULD use the nearest rounding value in nanosecond to get the proper nanosecond
 * timestamp of a Block. This allows some clever TimestampScale values to have more
 * refined timestamp precision per frame.
 *<p>
 * https://matroska.org/technical/notes.html#timestamps
 */
class MkvTimestampScale {

    public static final long NANOSECONDS_PER_SECOND = 1000000000;

    /**
     * Value for the mkv {@code TimestampScale}.
     * "Timestamp scale in nanoseconds".
     *<p>
     * "All timestamp values in Matroska are expressed in multiples of a tick."
     * https://matroska.org/technical/notes.html#timestamp-ticks
     *<p>
     * "The default Track Tick duration is one millisecond."
     * https://matroska.org/technical/notes.html#timestampscale-rounding
     *<p>
     * ffmpeg and MKVToolNix v.84 use 1000000 (1 millisecond) by default.
     */
    private static final long DEFAULT_NANOS_PER_TICK = 1000000;

    private static final MkvTimestampScale DEFAULT_TIME_SCALE;
    static {
        DEFAULT_TIME_SCALE = new MkvTimestampScale(DEFAULT_NANOS_PER_TICK);
        assert (NANOSECONDS_PER_SECOND % DEFAULT_TIME_SCALE._lngNanosPerTick) == 0; // Make sure this never changes
    }

    public static @Nonnull TicksError toTicks(@Nonnull Fraction seconds) {
        return DEFAULT_TIME_SCALE.calcSecondsToTicks(seconds);
    }

    // -----------------------------------------------------------------------------------


    private final long _lngNanosPerTick;
    @Nonnull
    private final Fraction _exactTicksPerSecond;

    public MkvTimestampScale(long lngNanosPerTick) {
        _lngNanosPerTick = lngNanosPerTick;
        _exactTicksPerSecond = new Fraction(NANOSECONDS_PER_SECOND, _lngNanosPerTick);
    }

    public static long getNanosPerTick() {
        return DEFAULT_TIME_SCALE._lngNanosPerTick;
    }

    public @Nonnull TicksError calcSecondsToTicks(@Nonnull Fraction timestampInSeconds) {
        return new TicksError(timestampInSeconds, _exactTicksPerSecond);
    }

    /**
     * Converting an exact timestamp in fractional seconds to ticks leaves room for
     * rounding errors. Capture all those details here.
     */
    public static class TicksError {
        @Nonnull
        public final Fraction timestampInSeconds;
        @Nonnull
        public final Fraction exactTicks;
        public final long lngRoundedTicks;
        @Nonnull
        public final Fraction roundedTimestampInSeconds;
        @Nonnull
        public final Fraction errorTicks;
        @Nonnull
        public final Fraction errorSeconds;

        public TicksError(@Nonnull Fraction timestampInSeconds, @Nonnull Fraction exactTicksPerSecond) {
            this.timestampInSeconds = timestampInSeconds;

            exactTicks = timestampInSeconds.multiply(exactTicksPerSecond);
            lngRoundedTicks = exactTicks.asRoundedLong();

            roundedTimestampInSeconds = Fraction.divide(lngRoundedTicks, exactTicksPerSecond);

            // +/- difference
            errorTicks = Fraction.subtract(lngRoundedTicks, exactTicks);
            errorSeconds = roundedTimestampInSeconds.subtract(timestampInSeconds);
        }
    }

}
