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

import java.util.concurrent.atomic.AtomicInteger;


public class DemuxReader implements Runnable {

    public static boolean DEBUG = false;

    private static final int STATE_STOPPED = 1;
    private static final int STATE_PLAYING = 4;
    private static final int STATE_PAUSED  = 6;

    IAudioVideoReader _reader;

    private AudioProcessor _audioProcessor;
    private VideoProcessor _videoProcessor;
    private PlayController _controller;
    
    private Thread _thread;

    private final Object _oPaused = new Object();

    private AtomicInteger _oiState = new AtomicInteger(STATE_PAUSED);

    public DemuxReader(IAudioVideoReader reader, AudioProcessor audProc, VideoProcessor vidProc, PlayController controller) {
        _reader = reader;
        _audioProcessor = audProc;
        _videoProcessor = vidProc;
        _controller = controller;
    }

    public void run() {
        while (true) {
            switch (_oiState.get()) {
                case STATE_PAUSED:
                    try {
                        if (DEBUG) System.out.println("Pausing reader");
                        synchronized (_oPaused) {
                            _oPaused.wait();
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                        return;
                    }
                    break;
                case STATE_STOPPED:
                    if (DEBUG) System.out.println("Stopping reader");
                    return;
                case STATE_PLAYING:
                    // proces a sector
                    // crap, how do you stop when waiting for more to read?
                    boolean blnContinue = true;
                    try {
                        blnContinue = _reader.readNext(_videoProcessor, _audioProcessor);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                    if (!blnContinue) {
                        System.out.println("Reader says it is the end. Telling everyone to stop when empty.");
                        _oiState.set(STATE_STOPPED);
                        _controller.endOfPlay();
                        return;
                    }
            }
        }
        
    }

    public void startup() {
        // start the thread
        _oiState.set(STATE_PLAYING);
        _thread = new Thread(this, "Demux Reader");
        _thread.start();
    }

    public void pause() {
        _oiState.set(STATE_PAUSED);
    }

    public void stop() {
        _oiState.set(STATE_STOPPED);
    }
    
    public void play() {
        _oiState.set(STATE_PLAYING);
        synchronized (_oPaused) {
            _oPaused.notify();
        }
    }
}
