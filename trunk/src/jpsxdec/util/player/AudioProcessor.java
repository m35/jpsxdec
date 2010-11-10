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

/** Audio process thread manages the conversion of audio data to playable
 * PCM audio. */
public class AudioProcessor implements Runnable {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final MultiStateBlockingQueue<IDecodableAudioChunk>  _audioProcessingQueue =
            new MultiStateBlockingQueue<IDecodableAudioChunk>(CAPACITY);

    private final AudioPlayer _audPlayer;
    private final VideoProcessor _vidProc;

    private Thread _thread;

    AudioProcessor(AudioPlayer audioPlayer, VideoProcessor vidProc) {
        _audPlayer = audioPlayer;
        _vidProc = vidProc;
        if (vidProc != null)
            vidProc.overwriteWhenFull();
        _audioProcessingQueue.stop();
    }

    public final void addDecodableAudioChunk(IDecodableAudioChunk audioChunk) {
        try {
            synchronized (_audioProcessingQueue.getSyncObject()) {
                boolean blnWasEmpty = _audioProcessingQueue.isEmpty();
                _audioProcessingQueue.add(audioChunk);
                if (_vidProc != null && blnWasEmpty) {
                    if (DEBUG) System.out.println(Thread.currentThread().getName() + " audio queue is no longer empty, telling video queue to block");
                    _vidProc.blockWhenFull();
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public final void run() {
        try {
            IDecodableAudioChunk audChunk;
            while (true) {
                synchronized (_audioProcessingQueue.getSyncObject()) {
                    audChunk = _audioProcessingQueue.take();
                    if (audChunk == null)
                        break;
                    if (_vidProc != null && _audioProcessingQueue.isEmpty()) {
                        if (DEBUG) System.out.println(Thread.currentThread().getName() + " audio queue empty, telling video queue to overwrite");
                        _vidProc.overwriteWhenFull();
                    }
                }
                // decode audio chunk and let it feed it to the player,
                // will block if audio player is full or paused
                audChunk.decodeAudio(_audPlayer);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            _audioProcessingQueue.stop();
            _audPlayer.drain();
            _audPlayer.stop();
        }
    }

    final void play() {
        synchronized (_audioProcessingQueue.getSyncObject()) {
            if (_audioProcessingQueue.isPaused()) {
                _audioProcessingQueue.play();
            } else if (_audioProcessingQueue.isStopped()) {
                _thread = new Thread(this, getClass().getName());
                _thread.start();
                _audioProcessingQueue.play();
            }
        }
    }

    final void pause() {
        synchronized (_audioProcessingQueue.getSyncObject()) {
            if (_audioProcessingQueue.isPlaying()) {
                _audioProcessingQueue.pause();
            } else if (_audioProcessingQueue.isStopped()) {
                _thread = new Thread(this, getClass().getName());
                _thread.start();
                _audioProcessingQueue.pause();
            }
        }
    }

    /** Make sure player is stopped before calling this method or it will deadlock. */
    final void stop() throws InterruptedException {
        synchronized (_audioProcessingQueue.getSyncObject()) {
            if (_audioProcessingQueue.isPlaying() || _audioProcessingQueue.isStopped()) {
                _audioProcessingQueue.stop();
            }
        }
    }

    void stopWhenEmpty() {
        _audioProcessingQueue.stopWhenEmpty();
    }


}
