/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
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

package jpsxdec.util.player;

import java.awt.Canvas;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.Fraction;

/**
 * Primary public interface to controlling a player.
 *
 * Start by creating an instance with your {@link IMediaDataReadProcessor}, and the
 * dimensions and/or audio format.
 *
 * If there's video, get the {@link #getVideoScreen()} canvas and add it to a form.
 *
 * Optionally {@link #addEventListener(PlayerListener)} to respond to events and update
 * the form buttons, for example.
 *
 * Then call {@link #activate()} to initialize the player, but paused.
 * Call {@link #unpause()} to start the playback.
 */
public class PlayController {

    public static final Fraction PAL_ASPECT_RATIO = new Fraction(59, 54);
    public static final Fraction NTSC_ASPECT_RATIO = new Fraction(10, 11);
    public static final Fraction SQUARE_ASPECT_RATIO = new Fraction(1, 1);

    @CheckForNull
    private final AudioPlayer _audPlayer;
    @CheckForNull
    private final VideoPlayerThread _vidPlayer;

    @Nonnull
    private final VideoTimer _videoTimer;

    @Nonnull
    private final ReaderThread<?> _readerThread;

    @CheckForNull
    private final VideoProcessorThread<?> _videoProcessorThread;

    /**
     * Audio only.
     */
    public PlayController(@Nonnull IMediaDataReadProcessor<?> readProcessor, @Nonnull AudioFormat audioFormat) {
        this(readProcessor, null, audioFormat);
    }

    /**
     * Video only.
     */
    public PlayController(@Nonnull IMediaDataReadProcessor<?> readProcessor, int iWidth, int iHeight) {
        this(readProcessor, new Dims(iWidth, iHeight), null);
    }

    /**
     * Audio and video.
     */
    public <FRAME_TYPE> PlayController(@Nonnull IMediaDataReadProcessor<FRAME_TYPE> readProcessor, int iWidth, int iHeight, @Nonnull AudioFormat audioFormat) {
        this(readProcessor, new Dims(iWidth, iHeight), audioFormat);
    }

    /**
     * So I can conveniently use a common constructor.
     */
    private static class Dims {
        public final int iWidth;
        public final int iHeight;

        public Dims(int iWidth, int iHeight) {
            this.iWidth = iWidth;
            this.iHeight = iHeight;
        }
    }

    private  <FRAME_TYPE> PlayController(@Nonnull IMediaDataReadProcessor<FRAME_TYPE> readProcessor, @CheckForNull Dims dims, @CheckForNull AudioFormat audioFormat) {
        if (dims != null && (dims.iWidth < 1 || dims.iHeight < 1))
            throw new IllegalArgumentException();

        OutputStream audioOutputStream;
        if (audioFormat != null) {
            _audPlayer = new AudioPlayer(audioFormat);
            _videoTimer = _audPlayer;
            audioOutputStream = _audPlayer.getOutputStream();
        } else {
            _audPlayer = null;
            _videoTimer = new VideoClock();
            audioOutputStream = null;
        }

        VideoProcessorThread<FRAME_TYPE> videoProcessorThread;
        if (dims == null) {
            videoProcessorThread = null;
            _vidPlayer = null;
        } else {
            _vidPlayer = new VideoPlayerThread(_videoTimer, dims.iWidth, dims.iHeight);
            videoProcessorThread = new VideoProcessorThread<FRAME_TYPE>(_videoTimer, _vidPlayer, readProcessor);
        }
        _videoProcessorThread = videoProcessorThread;

        MediaDataWriter<FRAME_TYPE> mediaDataReadWriter = new MediaDataWriter<FRAME_TYPE>(this, audioOutputStream, videoProcessorThread);
        _readerThread = new ReaderThread<FRAME_TYPE>(this, mediaDataReadWriter, readProcessor, _audPlayer, videoProcessorThread);
    }

    /**
     * Initialize and start the reader and processor, but pause the player.
     */
    public void activate() throws PlayerException {
        try {
            _videoTimer.initPaused();
            if (_videoProcessorThread != null)
                _videoProcessorThread.start();
            if (_vidPlayer != null)
                _vidPlayer.startup();
            _readerThread.start();
        } catch (RuntimeException ex) {
            terminate();
        }
    }

    /**
     * Terminate all playback immediately (or as immediately as possible).
     */
    public void terminate() {
        // this will kill the audio player if the timer is the AudioPlayer
        _videoTimer.terminate();

        // can't kill the reader thread, we're dependent on the reader to exit
        // or trigger a StopPlayingException

        if (_vidPlayer != null)
            _vidPlayer.terminate();
        if (_videoProcessorThread != null)
            _videoProcessorThread.terminate();
    }

    public void pause() {
        _videoTimer.pause();
    }

    public void unpause() {
        _videoTimer.go();
    }

    public boolean isClosed() {
        return _videoTimer.isTerminated();
    }

    public boolean isPaused() {
        return _videoTimer.isPaused();
    }

    public boolean hasAudio() {
        return (_audPlayer != null);
    }
    public boolean hasVideo() {
        return (_vidPlayer != null);
    }

    /**
     * Only available if playing video.
     */
    public @CheckForNull Canvas getVideoScreen() {
        if (_vidPlayer != null)
            return _vidPlayer.getScreen();
        else
            return null;
    }

    /**
     * Adjust the rendered frame with this aspect ratio.
     */
    public void setAspectRatio(@Nonnull Fraction aspectRatio) {
        if (_vidPlayer != null)
            _vidPlayer.getScreen().setAspectRatio(aspectRatio);
    }

    /**
     * This is jPSXdec specific, can ignore.
     * @see VideoScreen#setSquashWidth(boolean)
     */
    public void setSquashWidth(boolean blnSquash) {
        if (_vidPlayer != null)
            _vidPlayer.getScreen().setSquashWidth(blnSquash);
    }


    // -------------------------------------------------------------------------
    // Listeners

    public void addEventListener(@Nonnull PlayerListener listener) {
        _videoTimer.addEventListener(listener);
    }
    public void removeEventListener(@Nonnull PlayerListener listener) {
        _videoTimer.removeEventListener(listener);
    }

    public static enum Event {
        Play,
        End,
        Pause,
    }

    public interface PlayerListener {
        /**
         * All events are fired from the same thread.
         * The implementer must not block!
         */
        void event(@Nonnull Event eEvent);
    }

}
