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

package jpsxdec.player;

import java.awt.Canvas;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


public class PlayController {

    public static boolean DEBUG = false;

    private DemuxReader _demuxReader;

    private AudioProcessor _audProcessor;
    private SourceDataLine _audPlayer;

    private VideoProcessor _vidProcessor;
    private VideoPlayer _vidPlayer;

    private VideoTimer _vidTimer;
    
    private long _lngContiguousPlayUniqueId = 0;
    private final Object _oTimeSync = new Object();


    public PlayController(IAudioVideoReader reader)
            throws LineUnavailableException
    {
        AudioFormat format = reader.getAudioFormat();
        if (format == null && !reader.hasVideo())
            throw new IllegalArgumentException("No audio or video?");

        if (reader.hasVideo()) {
            _vidPlayer = new VideoPlayer(this, reader.getVideoWidth(), reader.getVideoHeight());
            _vidProcessor = new VideoProcessor(this, _vidPlayer);
            _vidProcessor.startup();
            _vidPlayer.startup();
        }
        if (format == null) {
            _vidTimer = new VideoTimer();
        } else {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            _audPlayer = (SourceDataLine) AudioSystem.getLine(info);
            _audPlayer.open(format);

            _audProcessor = new AudioProcessor(_audPlayer, _vidProcessor);
            _audProcessor.startup();
        }

        _demuxReader = new DemuxReader(reader, _audProcessor, _vidProcessor, this);
        _demuxReader.startup();

    }

    public long getContiguousPlayUniqueId() {
        synchronized (_oTimeSync) {
            return _lngContiguousPlayUniqueId;
        }
    }

    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;

    public long getCurrentPlayTime() {
        synchronized (_oTimeSync) {
            long lngPos;
            if (_audPlayer != null) {
                //lngPos = _audPlayer.getMicrosecondPosition();
                lngPos = (long)(_audPlayer.getLongFramePosition() / _audPlayer.getFormat().getSampleRate() * 1000);
            } else {
                lngPos = _vidTimer.getPlayTimeAndStart();
            }
            if (DEBUG) System.out.println("Current play time " + lngPos);
            return lngPos;
        }
    }

    public boolean shouldBeProcessed(long lngPresentationTime, long lngContiguousPlayUniqueId) {
        synchronized (_oTimeSync) {
            if (lngContiguousPlayUniqueId != _lngContiguousPlayUniqueId)
                return false;

            long lngPos;
            if (_audPlayer != null) {
                lngPos = _audPlayer.getMicrosecondPosition() / 1000;
            } else {
                lngPos = _vidTimer.getPlayTime();
            }
            if (lngPos == 0) // if not started playing, then process all you like
                return true;
            if (DEBUG) System.out.println("Playtime = " + lngPos + " vs. PresTime = " + lngPresentationTime);
            return lngPos < lngPresentationTime;
        }
    }

    public void play() {
        _demuxReader.play();
        if (_audProcessor != null)
            _audProcessor.play();
        if (_audPlayer != null)
            _audPlayer.start();
        if (_vidProcessor != null)
            _vidProcessor.play();
        if (_vidPlayer != null)
            _vidPlayer.play();
    }
    
    public void pause() {
        if (_vidPlayer != null) {
            _vidPlayer.pause();
        }
        if (_audPlayer != null) {
            _audPlayer.stop();
        }
    }

    void endOfPlay() {
        if (_audProcessor != null)
            _audProcessor.stopWhenEmpty();
        if (_vidProcessor != null)
            _vidProcessor.stopWhenEmpty();
        if (_vidPlayer != null)
            _vidPlayer.stopWhenEmpty();
    }
    
    public Canvas getVideoScreen() {
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

}
