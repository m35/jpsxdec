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

package jpsxdec.util.player;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;

/** User created object to do the actual reading of audio or video chunks.
 * Also indicates the format of the chucks being read.  */
public abstract class AudioVideoReader implements Runnable {

    @CheckForNull
    private AudioPlayer _audioPlayer;
    @CheckForNull
    private VideoProcessor _videoProcessor;
    private int _iProgress = -1;

    @CheckForNull
    private Thread _thread;

    /** Only stopped or playing. */
    private final PlayingState _state = new PlayingState(PlayingState.State.STOPPED);

    void init(@CheckForNull AudioPlayer audPlay, @CheckForNull VideoProcessor vidProc) {
        _audioPlayer = audPlay;
        _videoProcessor = vidProc;
    }

    public void writeAudio(@Nonnull byte[] abData, int iStart, int iLength) {
        _audioPlayer.write(abData, iStart, iLength);
    }

    public void writeSilence(long lngSamples) {
        _audioPlayer.writeSilence(lngSamples);
    }

    public void writeFrame(@Nonnull IDecodableFrame frame) {
        _videoProcessor.writeFrame(frame);
    }

    /** @param iProgress  0 to 100. */
    public void setReadProgress(int iProgress) {
        _iProgress = iProgress;
    }

    protected boolean stillPlaying() {
        return _state.get() != PlayingState.State.STOPPED;
    }

    final public void run() {
        try {
            demuxThread();
        } finally {
            System.out.println("[AudioVideoReader] At end, telling everyone to stop when empty");
            if (_videoProcessor != null)
                _videoProcessor.writerClose();
            if (_audioPlayer != null)
                _audioPlayer.drainAndClose();
            _state.set(PlayingState.State.STOPPED);
        }
    }

    void startBuffering() {
        synchronized (_state) {
            if (_state.get() == PlayingState.State.STOPPED) {
                _state.set(PlayingState.State.PLAYING);
                _thread = new Thread(this, getClass().getName());
                _thread.start();
            }
        }
    }

    void stop() {
        synchronized (_state) {
            if (_state.get() == PlayingState.State.PLAYING) {
                _state.set(PlayingState.State.STOPPED);
            }
        }
    }

    abstract protected void demuxThread();
    /** Return null if no audio. */
    abstract public @CheckForNull AudioFormat getAudioFormat();
    abstract public boolean hasVideo();
    abstract public int getVideoWidth();
    abstract public int getVideoHeight();
    abstract public double getDuration();
}
