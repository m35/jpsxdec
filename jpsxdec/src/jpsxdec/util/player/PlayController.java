/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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
import java.util.WeakHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import jpsxdec.util.Fraction;

/** Interface to controlling a player. */
public class PlayController {

    public static final Fraction PAL_ASPECT_RATIO = new Fraction(59, 54);
    public static final Fraction NTSC_ASPECT_RATIO = new Fraction(10, 11);
    public static final Fraction SQUARE_ASPECT_RATIO = new Fraction(1, 1);

    private static final boolean DEBUG = false;

    @Nonnull
    private AudioVideoReader _demuxReader;

    @CheckForNull
    private AudioPlayer _audPlayer;

    @CheckForNull
    private VideoProcessor _vidProcessor;
    @CheckForNull
    private VideoPlayer _vidPlayer;

    @Nonnull
    private IVideoTimer _vidTimer;
    

    public PlayController(@Nonnull AudioVideoReader reader) {
        AudioFormat format = reader.getAudioFormat();
        if (format == null && !reader.hasVideo())
            throw new IllegalArgumentException("No audio or video?");

        if (format != null) {
            _audPlayer = new AudioPlayer(format, this);
            _vidTimer = _audPlayer;
        }
        if (reader.hasVideo()) {
            if (_vidTimer == null) {
                _vidTimer = _vidPlayer = new VideoPlayer(null, this, reader.getVideoWidth(), reader.getVideoHeight());
            } else {
                _vidPlayer = new VideoPlayer(_vidTimer, this, reader.getVideoWidth(), reader.getVideoHeight());
            }

            _vidProcessor = new VideoProcessor(_vidTimer, _vidPlayer);
        }

        _demuxReader = reader;
        _demuxReader.init(_audPlayer, _vidProcessor);

    }

    public void start() throws LineUnavailableException {
        _demuxReader.startBuffering();
        if (_audPlayer != null)
            _audPlayer.startPaused();
        if (_vidProcessor != null)
            _vidProcessor.startBuffering();
        if (_vidPlayer != null)
            _vidPlayer.startPaused();
        java.awt.EventQueue.invokeLater(new NotifyLater(Event.Start));
    }

    public void stop() {
        synchronized (_vidTimer.getSyncObject()) {
            // stop feeding data for starters
            _demuxReader.stop();

            if (_audPlayer != null) {
                _audPlayer.stop();
            }

            if (_vidPlayer != null)
                // must stop the player first so processor won't block
                _vidPlayer.stop();
            if (_vidProcessor != null)
                _vidProcessor.stop();
        }
    }

    public void pause() {
        synchronized (_vidTimer.getSyncObject()) {
            if (_vidPlayer != null) {
                _vidPlayer.pause();
            }
            if (_audPlayer != null) {
                _audPlayer.pause();
            } else {
                // TODO: stop vidTimer?
            }
            java.awt.EventQueue.invokeLater(new NotifyLater(Event.Pause));
        }
    }


    public void unpause() {
        synchronized (_vidTimer.getSyncObject()) {
            if (_vidPlayer != null) {
                _vidPlayer.unpause();
            }
            if (_audPlayer != null) {
                _audPlayer.unpause();
            } else {
                // TODO: start vidTimer?
            }
            java.awt.EventQueue.invokeLater(new NotifyLater(Event.Unpause));
        }
    }

    public @CheckForNull Canvas getVideoScreen() {
        if (_vidPlayer != null)
            return _vidPlayer.getVideoCanvas();
        else
            return null;
    }

    public boolean hasVideo() {
        return (_vidPlayer != null);
    }

    public boolean hasAudio() {
        return (_audPlayer != null);
    }

    /** Adjust the rendered frame with this aspect ratio. */
    public void setAspectRatio(@Nonnull Fraction aspectRatio) {
        if (_vidPlayer != null)
            _vidPlayer.setAspectRatio(aspectRatio);
    }

    /** Squash oversized frames to fit in TV. */
    public void setSquashWidth(boolean blnSquash) {
        if (_vidPlayer != null)
            _vidPlayer.setSquashWidth(blnSquash);
    }

    void notifyDonePlaying() {
        synchronized (_vidTimer.getSyncObject()) {
            if ((_vidPlayer == null || _vidPlayer.isDone()) &&
                (_audPlayer == null || _audPlayer.isDone()))
            {
                java.awt.EventQueue.invokeLater(new NotifyLater(Event.Stop));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Listeners

    @CheckForNull
    private WeakHashMap<PlayerListener, Boolean> _listeners;

    public void addLineListener(@Nonnull PlayerListener listener) {
        if (_listeners == null)
            _listeners = new WeakHashMap<PlayerListener, Boolean>();
        _listeners.put(listener, Boolean.TRUE);
    }
    public void removeLineListener(@Nonnull PlayerListener listener) {
        if (_listeners != null)
            _listeners.remove(listener);
    }

    public static enum Event {
        Start,
        Stop,
        Pause,
        Unpause
    }
    public static interface PlayerListener {
        void update(@Nonnull Event eEvent);
    }

    private class NotifyLater implements Runnable {
        private final Event _eEvent;
        public NotifyLater(@Nonnull Event eEvent) {
            _eEvent = eEvent;
        }
        public void run() {
            if (_listeners == null)
                return;
            for (PlayerListener listener : _listeners.keySet()) {
                listener.update(_eEvent);
            }
        }
    }

}
