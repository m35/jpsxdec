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

import javax.annotation.Nonnull;

/**
 * A video timer backed by the system clock.
 * Manages its own event thread and event queue.
 */
class VideoClock extends VideoTimer implements Runnable {

    @Nonnull
    private final Thread _eventThread;

    private long _lngStartTime = -1;
    private long _lngPausedTime = -1;


    // TODO want unbounded queue
    private final ClosableBoundedBlockingQueue<PlayController.Event> _eventQueue =
            new ClosableBoundedBlockingQueue<PlayController.Event>(100);

    public VideoClock() {
        _eventThread = new Thread(this, getClass().getName() + " event dispatcher");
    }

    @Override
    public synchronized void initPaused() {
        _eventThread.start();
    }

    @Override
    public synchronized void go() {
        if (_lngStartTime < 0) {
            // initial start
            _lngStartTime = System.nanoTime();
        } else if (_lngPausedTime >= 0) {
            // unpause
            // removed the amount of time paused from the start time
            _lngStartTime += System.nanoTime() - _lngPausedTime;
            _lngPausedTime = -1;
        } else {
            // already playing
            return;
        }
        // let the super change the state and notify waiters
        super.go();
        _eventQueue.addWithCapacityCheck(PlayController.Event.Play);
    }


    @Override
    public synchronized void pause() {
        _lngPausedTime = System.nanoTime();
        super.pause();
        _eventQueue.addWithCapacityCheck(PlayController.Event.Play);
    }

    @Override
    public synchronized long getNanoTime() {
        if (_lngStartTime < 0)
            return 0;
        else if (_lngPausedTime >= 0)
            return _lngPausedTime - _lngStartTime;
        else
            return System.nanoTime() - _lngStartTime;
    }

    // TODO: pause the playback if we are waiting for a frame to arrive?

    @Override
    public synchronized void videoDone() {
        // when the video is done, this timer is done
        super.terminate();
        try {
            _eventQueue.add(PlayController.Event.End);
            _eventQueue.closeWhenEmpty();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public synchronized void terminate() {
        _eventQueue.closeNow();
        super.terminate();
    }

    @Override
    public void run() {
        try {
            while (true) {
                PlayController.Event event = _eventQueue.take();
                if (event == null)
                    break;
                fire(event);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}


