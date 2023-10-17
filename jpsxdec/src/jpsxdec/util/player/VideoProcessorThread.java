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
 * Video processor thread manages the conversion of video source data
 * to a presentation image.
 */
class VideoProcessorThread<FRAME_TYPE> implements Runnable {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final ClosableBoundedBlockingQueue<VideoFrame<FRAME_TYPE>> _framesProcessingQueue =
            new ClosableBoundedBlockingQueue<VideoFrame<FRAME_TYPE>>(CAPACITY);

    @Nonnull
    private final VideoTimer _vidTimer;
    @Nonnull
    private final VideoPlayerThread _vidPlayer;
    @Nonnull
    private final Thread _thisThread;

    @Nonnull
    private final IMediaDataReadProcessor<FRAME_TYPE> _reader;

    @Nonnull
    private final int[] _aiDrawBuffer;

    VideoProcessorThread(@Nonnull VideoTimer timer, @Nonnull VideoPlayerThread player,
                         @Nonnull IMediaDataReadProcessor<FRAME_TYPE> mediaDataWriter)
    {
        _vidTimer = timer;
        _vidPlayer = player;
        _reader = mediaDataWriter;
        _thisThread = new Thread(this, getClass().getName());
        _aiDrawBuffer = new int[_vidPlayer.getWidth() * _vidPlayer.getHeight()];
    }

    public void start() {
        _thisThread.start();
    }

    @Override
    public void run() {
        VideoFrame<FRAME_TYPE> frame;
        try {
            while ((frame = _framesProcessingQueue.take()) != null) {
                // check that we haven't passed presentation time
                //System.out.println("Checking if to process frame at " + decodeFrame.lngPresentationNanos);
                if (_vidTimer.shouldBeProcessed(frame.lngPresentationNanos))
                {
                    if (DEBUG) System.out.println("Processor processing frame :)");

                    // decode frame
                    _reader.processFrameThread(frame.frame, _aiDrawBuffer);

                    frame.setDecoded(_aiDrawBuffer, _vidPlayer.getWidth(), _vidPlayer.getHeight());
                    // submit to vid player, will block if player is full
                    _vidPlayer.addFrame(frame);
                } else {
                    System.out.println("Processor skipping frame :(");
                }
            }
        } catch (Throwable ex) {
            // this also handles StopPlayingException which should behave the same TODO verify to be sure
            ex.printStackTrace();
            terminate();
            return;
        }
        System.out.println("VideoProcessorThread ending");
        _vidPlayer.finish();
    }

    public void queueFrame(@Nonnull FRAME_TYPE frame, long lngPresentationNanos) throws StopPlayingException {
        if (DEBUG) System.out.println("Frame submitted for processing, present at " + lngPresentationNanos);
        try {
            if (!_framesProcessingQueue.add(new VideoFrame<FRAME_TYPE>(lngPresentationNanos, frame)))
                throw new StopPlayingException();
        } catch (InterruptedException ex) {
            throw new StopPlayingException(ex);
        }
    }

    public void finish() {
        System.out.println("VideoProcessorThread request to end");
        _framesProcessingQueue.closeWhenEmpty();
    }

    public void terminate() {
        _framesProcessingQueue.closeNow();
        _vidPlayer.terminate();
    }

}
