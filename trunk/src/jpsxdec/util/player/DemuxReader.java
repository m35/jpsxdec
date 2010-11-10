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

/** The reading thread. */
class DemuxReader implements Runnable {

    public static boolean DEBUG = false;

    IAudioVideoReader _reader;

    private AudioProcessor _audioProcessor;
    private VideoProcessor _videoProcessor;
    private PlayController _controller;
    
    private Thread _thread;


    private final PlayingState _state = new PlayingState(PlayingState.State.STOPPED);

    public DemuxReader(IAudioVideoReader reader, AudioProcessor audProc, VideoProcessor vidProc, PlayController controller) {
        _reader = reader;
        _audioProcessor = audProc;
        _videoProcessor = vidProc;
        _controller = controller;
    }

    public void run() {
        try {
            while (true) {
                synchronized (_state) {
                    if (_state.get() == PlayingState.State.PAUSED) {
                        if (DEBUG) System.out.println("Pausing reader");
                        _state.waitForChange();
                    }
                    if (_state.get() == PlayingState.State.STOPPED)
                        return;
                }
                // now playing
                
                // XXX: how can demuxer be told to stop when it's blocking for more to read?
                int iProgress = _reader.readNext(_videoProcessor, _audioProcessor);
                if (DEBUG) System.out.println("Progress " + iProgress);
                if (iProgress < 0) {
                    System.out.println("Reader says it is the end. Telling everyone to stop when empty.");
                    _controller.endOfPlay();
                    break; // break out of loop
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        _state.set(PlayingState.State.STOPPED);

    }

    public void play() {
        synchronized (_state) {
            switch (_state.get()) {
                case PAUSED:
                    _state.set(PlayingState.State.PLAYING);
                    break;
                case STOPPED:
                    _thread = new Thread(this, getClass().getName());
                    _thread.start();
                    _state.set(PlayingState.State.PLAYING);
                    break;
            }
        }
    }

    public void pause() {
        synchronized (_state) {
            switch (_state.get()) {
                case PLAYING:
                    _state.set(PlayingState.State.PAUSED);
                    break;
                case STOPPED:
                    _thread = new Thread(this, getClass().getName());
                    _thread.start();
                    _state.set(PlayingState.State.PAUSED);
                    break;
            }
        }
    }

    public void stop() throws InterruptedException {
        synchronized (_state) {
            switch (_state.get()) {
                case PLAYING:
                case PAUSED:
                    _state.set(PlayingState.State.STOPPED);
                    break;
            }
        }
    }
    
}
