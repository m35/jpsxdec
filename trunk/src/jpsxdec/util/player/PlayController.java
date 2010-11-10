/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JComponent;

/** Interface to controlling a player. */
public class PlayController {

    private static final boolean DEBUG = false;

    private DemuxReader _demuxReader;

    private AudioProcessor _audProcessor;
    private AudioPlayer _audPlayer;

    private VideoProcessor _vidProcessor;
    private VideoPlayer _vidPlayer;

    private IVideoTimer _vidTimer;
    
    private final Object _playSync = new Object();


    public PlayController(IAudioVideoReader reader)
            throws LineUnavailableException
    {
        AudioFormat format = reader.getAudioFormat();
        if (format == null && !reader.hasVideo())
            throw new IllegalArgumentException("No audio or video?");

        if (format != null) {
            _audPlayer = new AudioPlayer(format, this);
            _vidTimer = _audPlayer;

            _audProcessor = new AudioProcessor(_audPlayer, _vidProcessor);

            _audProcessor.play();
            _audPlayer.pause(); // audio will start playing once it has data in the buffer
        }
        if (reader.hasVideo()) {
            if (_vidTimer == null) {
                _vidTimer = _vidPlayer = new VideoPlayer(null, this, reader.getVideoWidth(), reader.getVideoHeight());
            } else {
                _vidPlayer = new VideoPlayer(_vidTimer, this, reader.getVideoWidth(), reader.getVideoHeight());
            }
            _vidProcessor = new VideoProcessor(_vidTimer, _vidPlayer);
            _vidProcessor.play();
            _vidPlayer.pause();
        }

        _demuxReader = new DemuxReader(reader, _audProcessor, _vidProcessor, this);
        _demuxReader.play(); // start buffering

    }

    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;

    public void play() throws LineUnavailableException {
        _demuxReader.play();
        if (_audProcessor != null)
            _audProcessor.play();
        if (_audPlayer != null)
            _audPlayer.play();
        if (_vidProcessor != null)
            _vidProcessor.play();
        if (_vidPlayer != null)
            _vidPlayer.play();
        synchronized (_playSync) {
            _playSync.notify();
        }
    }
    
    public void pause() throws LineUnavailableException {
        synchronized (_playSync) {
            if (_vidPlayer != null) {
                _vidPlayer.pause();
            }
            if (_audPlayer != null) {
                _audPlayer.pause();
            } else {
                // TODO: stop vidTimer
            }
        }
    }

    public void stop() throws InterruptedException {
        synchronized (_playSync) {
            // stop feeding data for starters
            _demuxReader.stop();

            if (_audPlayer != null) {
                // must stop the player first so processor won't block
                _audPlayer.stop();
            }
            if (_audProcessor != null)
                _audProcessor.stop();

            if (_vidPlayer != null)
                // must stop the player first so processor won't block
                _vidPlayer.stop();
            if (_vidProcessor != null)
                _vidProcessor.stop();
        }
    }


    void endOfPlay() {
        synchronized (_playSync) {
            if (_audProcessor != null)
                _audProcessor.stopWhenEmpty();
            if (_vidProcessor != null)
                _vidProcessor.stopWhenEmpty();
            if (_vidPlayer != null)
                _vidPlayer.stopWhenEmpty();
        }
    }
    
    public JComponent getVideoScreen() {
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

    public int getVideoZoom() {
        if (_vidPlayer == null)
            return -1;
        return _vidPlayer.getZoom();
    }

    public void setVideoZoom(int iZoom) {
        if (_vidPlayer == null)
            return;
        _vidPlayer.setZoom(iZoom);
    }

    private WeakHashMap<PlayerListener, Boolean> _listeners;

    public void addLineListener(PlayerListener listener) {
        if (_listeners == null)
            _listeners = new WeakHashMap<PlayerListener, Boolean>();
        _listeners.put(listener, Boolean.TRUE);
    }
    public void removeLineListener(PlayerListener listener) {
        if (_listeners != null)
            _listeners.remove(listener);
    }

    public static enum Event {
        Start,
        Stop,
        Pause;
    }
    public static interface PlayerListener {
        void update(Event eEvent);
    }

    private class NotifyLater implements Runnable {
        private final Event _eEvent;
        public NotifyLater(Event _eEvent) {
            this._eEvent = _eEvent;
        }
        public void run() {
            for (PlayerListener listener : _listeners.keySet()) {
                listener.update(_eEvent);
            }
        }
    }

    void fireStarted() {
        synchronized (_playSync) {
            java.awt.EventQueue.invokeLater(new NotifyLater(Event.Start));
        }
    }

    void fireStopped() {
        synchronized (_playSync) {
            try {
                if (java.awt.EventQueue.isDispatchThread())
                    new NotifyLater(Event.Stop).run();
                else
                    java.awt.EventQueue.invokeAndWait(new NotifyLater(Event.Stop));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    void firePaused() {
        synchronized (_playSync) {
            java.awt.EventQueue.invokeLater(new NotifyLater(Event.Pause));
        }
    }
}
