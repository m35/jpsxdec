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
 * Manages the actual display of video frames.
 */
class VideoPlayerThread implements Runnable {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final ClosableBoundedBlockingQueue<VideoFrame<?>> _frameDisplayQueue =
            new ClosableBoundedBlockingQueue<VideoFrame<?>>(CAPACITY);

    private final int _iWidth, _iHeight;

    @Nonnull
    private final VideoScreen _screen;
    @Nonnull
    private final VideoTimer _vidTimer;

    @Nonnull
    private final Thread _thisThread;

    public VideoPlayerThread(@Nonnull VideoTimer vidTimer,
                             int iWidth, int iHeight)
    {
        _vidTimer = vidTimer;
        _iWidth = iWidth;
        _iHeight = iHeight;
        _screen = new VideoScreen(_iWidth, _iHeight);
        _thisThread = new Thread(this, getClass().getName());
    }

    public void startup() {
        _thisThread.start();
    }

    @Override
    public void run() {
        try {
            VideoFrame<?> frame;
            while ((frame = _frameDisplayQueue.take()) != null) {
                VideoTimer.ShowFrame showFrame = _vidTimer.waitToPresentFrame(frame.lngPresentationNanos);
                if (showFrame == VideoTimer.ShowFrame.CLOSED) {
                    break;
                } else if (showFrame == VideoTimer.ShowFrame.NO) {
                    System.out.println("Timer says to discard frame");
                } else {
                    // show frame must be YES
                    if (DEBUG) System.out.println("===Displaying frame=== @" + frame.lngPresentationNanos);
                    _screen.updateImage(frame);
                }
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            _vidTimer.terminate();
        } finally {
            System.out.println("VideoPlayer ending");

            _frameDisplayQueue.closeNow();
            _vidTimer.videoDone();
        }
    }

    public void addFrame(@Nonnull VideoFrame<?> frame) throws InterruptedException {
        _frameDisplayQueue.add(frame);
    }

    public @Nonnull VideoScreen getScreen() {
        return _screen;
    }

    public void terminate() {
        _frameDisplayQueue.closeNow();
        _vidTimer.terminate();
    }

    public void pause() {
        _vidTimer.pause();
    }

    public void unpause() {
        _vidTimer.go();
    }

    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    public void finish() {
        System.out.println("VideoPlayer request to end (if not already terminated)");
        _frameDisplayQueue.closeWhenEmpty();
    }
}
