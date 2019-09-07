/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.Fraction;

/** 
 * Primary public interface to controlling a player.
 *
 * Start by creating an instance, specifying the video dimensions or
* {@link AudioFormat}, or both. Make your own reader that implements
* {@link IMediaDataReader} and register it with
* {@link PlayController#setReader(IMediaDataReader)}
* This reader will supply all video frames, PCM audio data, or both.
* If there is video, also create a frame processor that implements
* {@link IFrameProcessor} and register it with
* {@link PlayController#setVidProcressor(IFrameProcessor)}.
* Add {@link PlayerListener}s to listen to {@link Event}s.
*
* After that is complete call {@link #activate()} to initialize and start the
* player, but paused. Run {@link #unpause()} to start the playback.
*/
public class PlayController {

    public static final Fraction PAL_ASPECT_RATIO = new Fraction(59, 54);
    public static final Fraction NTSC_ASPECT_RATIO = new Fraction(10, 11);
    public static final Fraction SQUARE_ASPECT_RATIO = new Fraction(1, 1);

    @CheckForNull
    private AudioPlayer _audPlayer;
    @CheckForNull
    private VideoPlayer _vidPlayer;

    @Nonnull
    private final VideoTimer  _videoTimer;

    @CheckForNull
    private final VideoProcessor _videoProcessorThread;
    @Nonnull
    private final ReaderThread _readerThread;
    

    public PlayController(@Nonnull AudioFormat audioFormat) {
        _audPlayer = new AudioPlayer(audioFormat);
        _videoTimer = _audPlayer;
        _vidPlayer = null;
        _videoProcessorThread = null;
        _readerThread = new ReaderThread(_audPlayer, null, this);
    }

    public PlayController(int iWidth, int iHeight) {
        this(iWidth, iHeight, null);
    }

    public PlayController(int iWidth, int iHeight, @CheckForNull AudioFormat audioFormat) {
        if (audioFormat != null) {
            _audPlayer = new AudioPlayer(audioFormat);
            _videoTimer = _audPlayer;
        } else {
            _audPlayer = null;
            _videoTimer = new VideoClock();
        }
        _vidPlayer = new VideoPlayer(_videoTimer, iWidth, iHeight);
        _videoProcessorThread = new VideoProcessor(_videoTimer, _vidPlayer);
        _readerThread = new ReaderThread(_audPlayer, _videoProcessorThread, this);
    }

    public void setReader(@Nonnull IMediaDataReader reader) {
        _readerThread.setReader(reader);
    }

    public void setVidProcressor(@Nonnull IFrameProcessor processor) {
        if (_videoProcessorThread == null)
            throw new IllegalArgumentException();
        _videoProcessorThread.setProcessor(processor);
    }


    /**
     * Initialize and start the player, but paused.
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
        // (hopefully) single instruction (here) no don't need synchronized
        return _videoTimer.isTerminated();
    }

    public boolean isPaused() {
        // (hopefully) single instruction (here) no don't need synchronized
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
     * Should only be called by the {@link IMediaDataReader}.
     * Write PCM audio data here in the format that was provided in the
     * constructor.
     * Note that the {@link OutputStream} may throw an {@link IOException}
     * if the audio playback has been terminated.
     *
     * Only available if playing audio.
     */
    public @CheckForNull OutputStream getAudioOutputStream() {
        if (_audPlayer != null)
            return _audPlayer.getOutputStream();
        else
            return null;
    }

    /** 
     * Should only be called by the {@link IMediaDataReader}.
     * Write completed frames to this writer.
     */
    public @CheckForNull IPreprocessedFrameWriter getFrameWriter() {
        return _videoProcessorThread;
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

    public static interface PlayerListener {
        void event(@Nonnull Event eEvent);
    }

}
