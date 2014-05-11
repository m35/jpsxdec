/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
class VideoProcessor implements Runnable {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final ObjectPlayStream<IDecodableFrame> _framesProcessingQueue =
            new ObjectPlayStream<IDecodableFrame>(CAPACITY);
    private Thread _thread;
    
    private final IVideoTimer _vidTimer;
    private final VideoPlayer _vidPlayer;

    public VideoProcessor(IVideoTimer timer, VideoPlayer player) {
        _vidTimer = timer;
        _vidPlayer = player;
        _framesProcessingQueue.writerClose();
        _framesProcessingQueue.readerClose();
    }

    public void run() {
        IDecodableFrame decodeFrame;
        int[] aiImage = new int[_vidPlayer.getWidth() * _vidPlayer.getHeight()];
        try {
            while ((decodeFrame = _framesProcessingQueue.read()) != null) {
                // check that we haven't passed presentation time
                if (_vidTimer.shouldBeProcessed(decodeFrame.getPresentationTime()))
                {
                    if (DEBUG) System.out.println("Processor processing frame :)");
                    VideoPlayer.VideoFrame frame = _vidPlayer._videoFramePool.borrow();
                    frame.PresentationTime = decodeFrame.getPresentationTime();
                    // decode frame
                    decodeFrame.decodeVideo(aiImage);
                    frame.Img.setRGB(0, 0,
                            frame.Img.getWidth(), frame.Img.getHeight(),
                            aiImage, 0, frame.Img.getWidth());
                    // submit to vid player
                    // will block if player is full
                    _vidPlayer.addFrame(frame);
                } else {
                    System.out.println("Processor skipping frame :(");
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            _framesProcessingQueue.readerClose();
            _vidPlayer.writerClose();
        }
    }

    public void writeFrame(IDecodableFrame frame) {
        try {
            _framesProcessingQueue.write(frame);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    public void startBuffering() {
        synchronized (_framesProcessingQueue.getSyncObject()) {
            if (_framesProcessingQueue.isReaderClosed()) {
                _framesProcessingQueue.writerOpen();
                _framesProcessingQueue.readerOpen();
                _thread = new Thread(this, getClass().getName());
                _thread.start();
            }
        }
    }
    
    /** Make sure player is stopped before calling this method or it will deadlock. */
    public void stop() {
        _framesProcessingQueue.readerClose();
    }

    public void writerClose() {
        _framesProcessingQueue.writerClose();
    }

}
