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

/** Video processor thread manages the conversion of video source data
 * to a presentation image. */
public class VideoProcessor implements Runnable {

    public static boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private MultiStateBlockingQueue<AbstractDecodableFrame> _framesProcessingQueue =
            new MultiStateBlockingQueue<AbstractDecodableFrame>(CAPACITY);
    private Thread _thread;
    
    private PlayController _controller;
    private VideoPlayer _vidPlayer;

    VideoProcessor(PlayController controller, VideoPlayer player) {
        _controller = controller;
        _vidPlayer = player;
    }

    public void run() {
        AbstractDecodableFrame decodeFrame;
        try {
            while ((decodeFrame = _framesProcessingQueue.take()) != null) {
                // check if this frame is part of current play sequence
                // and if we haven't passed presentation time
                if (_controller.shouldBeProcessed(decodeFrame.getPresentationTime(),
                                                  decodeFrame.getContigiousId()))
                {
                    if (DEBUG) System.out.println("Processor processing frame :)");
                    VideoPlayer.VideoFrame frame = _vidPlayer._videoFramePool.borrow();
                    frame.init(decodeFrame);
                    // decode frame
                    decodeFrame.decodeVideo(frame.Memory);
                    frame.MemImgSrc.newPixels();
                    // submit to vid player
                    _vidPlayer.addFrame(frame);

                    decodeFrame.returnToPool();
                } else {
                    System.out.println("Processor not processing frame :(");
                }
            }
        } catch (InterruptedException ex) {
            _framesProcessingQueue.stop();
        }
    }

    public void addFrame(AbstractDecodableFrame frame) {
        try {
            frame.setContiguiousId(_controller.getContiguousPlayUniqueId());
            _framesProcessingQueue.add(frame);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    void overwriteWhenFull() {
        _framesProcessingQueue.overwriteWhenFull();
    }

    void clearQueue() {
        _framesProcessingQueue.clear();
    }
    
    void play() {
        _framesProcessingQueue.play();
    }
    
    void pause() {
        _framesProcessingQueue.pause();
    }

    void stopWhenEmpty() {
        _framesProcessingQueue.stopWhenEmpty();
    }

    void startup() {
        _thread = new Thread(this, "Video Processor");
        _framesProcessingQueue.play();
        _thread.start();
    }

    void shutdown() {
        _framesProcessingQueue.stop();
    }


}
