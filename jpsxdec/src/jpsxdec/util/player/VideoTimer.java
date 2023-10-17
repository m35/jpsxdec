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
import jpsxdec.util.player.PlayController.PlayerListener;

/** Abstract timer for when to present video frames. */
abstract class VideoTimer {

    /**
     * This tries to consider that there is a delay between the decision
     * to display a frame, and the frame actually being displayed.
     * This is a total guess.
     * */
    private static final int FRAME_DELAY_NANO_FUDGE_TIME = 50;

    private static final boolean DEBUG = false;

    public enum ShowFrame {
        /** The frame should be displayed at its given time. */
        YES,
        /** The frame should not be displayed, but discarded. */
        NO,
        /** The player has been closed so caller should shutdown. */
        CLOSED
    }

    private enum State {
        RUNNING,
        PAUSED,
        TERMINATED
    }

    @Nonnull
    private State _state = State.PAUSED;
    private final ThreadSafeEventListeners _listeners = new ThreadSafeEventListeners();

    public synchronized void go() {
        _state = State.RUNNING;
        this.notifyAll();
    }

    public void pause() {
        // (hopefully) single instruction so thread safety is not needed
        _state = State.PAUSED;
    }

    public boolean isPaused() {
        // (hopefully) single instruction so thread safety is not needed
        return _state == State.PAUSED;
    }

    public synchronized void terminate() {
        _state = State.TERMINATED;
        System.out.println("Notifying timer waiters that state has changed to terminate");
        this.notifyAll();
    }

    abstract void initPaused() throws PlayerException;
    abstract public void videoDone();
    abstract protected long getNanoTime();


    /** Returns if a frame that will appear at the presentation time should be processed.
     * This is to avoid processing frames if their presentation time has already passed. */
    final public synchronized boolean shouldBeProcessed(long lngPresentationNanos) {
        if (_state == State.RUNNING || _state == State.PAUSED) {
            long lngPlayTime = getNanoTime();
            if (DEBUG) System.out.println("Processing: Current nano time " + lngPlayTime + ", presentation nano time " + lngPresentationNanos);
            return lngPresentationNanos >= lngPlayTime;
        } else {
            return false;
        }
    }

    // TODO also wait for the video frame blocking queue if we want to pause if we're blocked because we're waiting for frames to be read?
    /**
     * When this function returns {@link ShowFrame#YES} the frame should be displayed immediately.
     * Otherwise discard the frame if {@link ShowFrame#NO} is returned, or shutdown if
     * {@link ShowFrame#CLOSED} is returned.
     * */
    final public synchronized @Nonnull ShowFrame waitToPresentFrame(long lngPresentationNanos) throws InterruptedException {
        while (true) {
            if (_state == State.TERMINATED) {
                return ShowFrame.CLOSED;
            } else if (_state == State.PAUSED) {
                // this MUST be interrupted by terminate or go
                this.wait();
                // TODO is there a way to regularly check if this thread is blocked forever
                // like I do in other places?
            } else {
                long lngPos = getNanoTime();
                if (DEBUG) System.out.println("Presenting: Current nano time " + lngPos + ", presentation nano time " + lngPresentationNanos);
                long lngSleepNanos;
                // careful with the math here so we don't have to worry about int overflow
                if ((lngSleepNanos = lngPresentationNanos - lngPos) > FRAME_DELAY_NANO_FUDGE_TIME) {
                    lngSleepNanos -= FRAME_DELAY_NANO_FUDGE_TIME;
                    // this CAN be interrupted by terminate (or go)
                    this.wait(lngSleepNanos / 1000000, (int)(lngSleepNanos % 1000000));
                    // loop once more to see if the state changed
                } else {
                    return ShowFrame.YES;
                }
            }
        }
    }

    public boolean isTerminated() {
        // (hopefully) single instruction so synchronized is not needed
        return _state == State.TERMINATED;
    }

    public void addEventListener(@Nonnull PlayerListener listener) {
        // listener object manages its own thread safety
        _listeners.addEventListener(listener);
    }
    public void removeEventListener(@Nonnull PlayerListener listener) {
        // listener object manages its own thread safety
        _listeners.removeEventListener(listener);
    }
    protected void fire(@Nonnull PlayController.Event event) {
        // listener object manages its own thread safety
        // the caller is responsible for ensuring events are fired in the order the occurred,
        // and not fire new events until previous ones are complete.
        _listeners.fire(event);
    }

}
