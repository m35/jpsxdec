/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter n Java
 * Copyright (C) 2019-2023  Michael Sabin
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

/**
A robust, extensible, real-time audio/video player in pure Java.

<h1>About</h1>

<p>
To my knowledge, only one other Java-only audio/video player has ever been written,
but it was to only play MPEG1 video.
</p>

<p>
Historically, the first real-time audio/video playback support for Java was the Java Media
Framework (JMF). It was closed source and not particularly successful.
Fast forward many years and we got JavaFX which can handle playback of more
modern audio/video formats. However it is not extensible so playback is limited
to using one of the support formats. If it was extensible I probably wouldn't
have written this.
</p>

<h1>Features</h1>

<ul>
 <li>Play video, audio, or both
 <li>Play, pause
 <li>Events when state changes
</ul>

<h1>Limitations</h1>

<ul>
 <li>No seeking.
 <li>Can't handle 'P' or 'B' kind of frames because it's design to drop frame packets if there is a buffer underrun.
 <li>Can't handle long gaps in audio or video (data reader needs to fill the gaps with silence).
 <li>Video with audio timing is dependent on the Java audio player real-time resolution, which may not be great on some platforms.
</ul>

<h1>Use</h1>

<p>
Create an implementation of {@link jpsxdec.util.player.IMediaDataReadProcessor}
and pass it to a new instance of {@link jpsxdec.util.player.PlayController}.
If there's video, get the {@link jpsxdec.util.player.PlayController#getVideoScreen()}
and put it in a Form. Probably add a button. Register an event listener to change the
state of the button (e.g. play, pause, at end).
</p>

<h1>Design</h1>

 <p>
 A sane audio and video player must have <em>at a minimum</em> 3 threads for any kind
 of sane playback design. This due to the fact that presenting the audio and video inherently
 involves delay. You have to wait until the exact time when a frame should be
 shown or the audio to play. If everything was put on hold for one of those
 to happen, there may not be enough time to go back and read more data,
 processes it, and have it ready for the next presentation.
 On the other hand, there can easily be intermittent lag in reading the data
 (disk slow down) or rendering the presentation (audio or display lag).
 So it would be impractical, if not impossible, for only a single thread
 to do everything.
 </p>

<h2>Threads</h2>

<p>
For this player's design, up to 5 threads are used in a straight-forward way.
</p>

<ul>
<li>1 thread {@link jpsxdec.util.player.ReaderThread} to read and demux the data</li>
<li>1 thread {@link jpsxdec.util.player.AudioPlayer} to feed audio into the {@link javax.sound.sampled.DataLine}
    (this only applies if there is audio)</li>
<li>1 thread {@link jpsxdec.util.player.VideoProcessorThread} to decode the frames (this only applies if there is video)</li>
<li>1 thread {@link jpsxdec.util.player.VideoPlayerThread} to display the frame to the screen (this only applies if there is video)</li>
<li>1 thread for player events using either Java's audio's thread
via {@link javax.sound.sampled.LineListener} added to
{@link javax.sound.sampled.SourceDataLine#addLineListener(javax.sound.sampled.LineListener)},
or this player's {@link jpsxdec.util.player.VideoClock} thread.
</li>
</ul>

<p>
Threads are connected by {@link jpsxdec.util.player.ClosableBoundedBlockingQueue}s.
Data is passed from one thread to another using them.
These queues hold all buffered data. These queues facilitate the pausing
of reading or writing that happens due to buffer overrrun or underrun.
</p>

<p>
Note that while the player has a processing thread between the reader and presenter,
audio does not. This was because the Playstation 1 XA format has contextual audio
(like 'P' frames) so packets can't be skipped. Also audio decoding should (hopefully) be very fast.
</p>

<p>
A separate thread is necessary for event notification to listeners because we
don't want the listeners to block the playback. Plus this is how the Java
audio system does it, so it must be a good idea.
Listeners are registered with
{@link jpsxdec.util.player.PlayController#addEventListener(jpsxdec.util.player.PlayController.PlayerListener)}.
When playing audio, instead of creating its own event thread,
it piggy-backs on the Java's audio event thread, and translates
 {@link javax.sound.sampled.LineListener#update} audio events into player events.
</p>


<h2>Video Timing</h2>

<p>
Java's audio framework handles the timing when audio should be played,
so we are only responsible for presenting the frames.
</p>

<p>
Each video frame is tagged with a presentation time.
A clock must be used to identify when a frame should appear.
That clock must have the ability to be paused either intentionally
or unintentionally because of buffer underrun.
The timer sleep between frames, just long enough until the desired presentation
time is met, with a little bit of a fudge factor.
</p>

<p>
When there is no audio, it uses {@link jpsxdec.util.player.VideoClock} which uses
{@link java.lang.System#nanoTime()}.
In this case, when the video is paused, the timer stops watching the clock
until the playback is unpaused. Unfortunately it doesn't handle frame
buffer underrun, so frames will be skipped until the input catches up.
The accuracy of {@link java.lang.System#nanoTime()} is very reliable,
so the presentation will be quite smooth.
</p>

<p>
When there is audio it uses {@link jpsxdec.util.player.AudioPlayer} which uses
{@link javax.sound.sampled.DataLine#getMicrosecondPosition()}.
Here, if the audio is paused or the queue is exhausted, the audio will stop,
which will in turn stop the video.
Unfortunately the accuracy of {@link javax.sound.sampled.DataLine#getMicrosecondPosition()}
may not be very reliable. For some hardware and platforms, the audio time doesn't flow evenly,
and may only increase chunks at a time. As a result the video may appear to
stutter. In some worst cases, the audio timer would reset back to 0 if the audio is
paused for too long.
</p>

<p>
It also would have been nice to instead rely on {@link javax.sound.sampled.LineListener}
to track all audio events. Unfortunately this doesn't even behave how the javadoc
says it should. Specifically, it doesn't send an event if there is buffer underrun.
There are bugs around it that have been open for over a decade.
</p>

<h2>Video display</h2>

<p>See {@link jpsxdec.util.player.VideoScreen}.</p>

<h2>Audio</h2>

<p>
Initially there was no separate audio thread. It just piped data directly
into the Java audio system and used its buffer. I later found that on
some systems the audio buffer is tiny, so was blocking the reader when it
tried to pipe it into the audio. So I had to create a separate thread with
its own bigger buffer to ensure that didn't happen. The thread just loops
moving its buffered data into the audio. It's found in
{@link jpsxdec.util.player.AudioPlayer}.
</p>

*/
package jpsxdec.util.player;

