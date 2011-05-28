/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.MemoryImageSource;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import jpsxdec.util.Fraction;

/** Video player thread manages the actual display of video frames. */
class VideoPlayer implements Runnable, IVideoTimer {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final MultiStateBlockingQueue<VideoFrame> _frameDisplayQueue =
            new MultiStateBlockingQueue<VideoFrame>(CAPACITY);

    private final int _iWidth, _iHeight;

    private int _iZoom = 1;

    /** Only used for video only streams. */
    private Thread _thread;

    private final VideoScreen _screen;
    private final IVideoTimer _vidTimer;
    private final PlayController _controller;

    public VideoPlayer(IVideoTimer vidTimer, PlayController controller, int iWidth, int iHeight) {
        if (vidTimer == null)
            _vidTimer = this;
        else
            _vidTimer = vidTimer;
        _controller = controller;
        _iWidth = iWidth;
        _iHeight = iHeight;
        _screen = new VideoScreen();
        _frameDisplayQueue.stop();
    }

    public void run() {
        try {
            VideoFrame frame;
            while (true) {
                
                if ((frame = _frameDisplayQueue.take()) == null) {
                    if (DEBUG) System.out.println("Player received no frame for display, stopping");
                    return;
                }
                boolean blnPresent = _vidTimer.waitToPresent(frame);
                if (!blnPresent) {
                    System.out.println("Timer says to discard frame");
                } else {
                    if (DEBUG) System.out.println("===Displaying frame=== @" + frame.PresentationTime);
                    //System.out.println(frame.PresentationTime + "\t" + System.nanoTime());
                    _screen.updateImage(frame);
                }
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            _frameDisplayQueue.stop();
            _controller.fireStopped();
        }
    }

    public void addFrame(VideoFrame frame) {
        try {
            _frameDisplayQueue.add(frame);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void play() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isPaused()) {
                _frameDisplayQueue.play();
                _lngTimerStartTime += System.nanoTime() - _lngTimerPausedTime;
            } else if (_frameDisplayQueue.isStopped()) {
                _thread = new Thread(this, getClass().getName());
                _thread.start();
                _blnTimerStarting = true;
                _frameDisplayQueue.play();
            }
        }
    }
    
    public void pause() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isPlaying()) {
                _lngTimerPausedTime = System.nanoTime();
                _frameDisplayQueue.pause();
            } else if (_frameDisplayQueue.isStopped()) {
                _thread = new Thread(this, getClass().getName());
                _thread.start();
                _blnTimerStarting = true;
                _frameDisplayQueue.pause();
            }
        }
    }

    void stop() throws InterruptedException {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isPlaying() || _frameDisplayQueue.isPaused())
               _frameDisplayQueue.stop();
        }
    }

    void stopWhenEmpty() {
        _frameDisplayQueue.stopWhenEmpty();
    }

    public int getZoom() {
        return _iZoom;
    }

    public void setZoom(int iZoom) {
        if (iZoom < 1 || iZoom > 3) {
            throw new IllegalArgumentException("Invalid zoom scale " + iZoom);
        }
        _iZoom = iZoom;
        _screen.updateDims();
    }

    public JComponent getVideoCanvas() {
        return _screen;
    }

    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    private static final Fraction PAL_ASPECT_RATIO = new Fraction(59, 54);
    private static final Fraction SQUARE_ASPECT_RATIO = new Fraction(1, 1);

    private class VideoScreen extends JComponent {

        private final AtomicReference<VideoFrame> _frame = new AtomicReference<VideoFrame>();

        private Fraction __aspectRatio = PAL_ASPECT_RATIO;

        private Dimension __dims;

        private Object __renderingHintInterpolation = 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                //RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

        public VideoScreen() {
            updateDims();
            setOpaque(true);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return true;
        }

        private void updateDims() {
            __dims = new Dimension(
                    _iWidth * _iZoom,
                    (int)(_iHeight * _iZoom *
                    __aspectRatio.getNumerator() / __aspectRatio.getDenominator()));
        }

        private void updateImage(VideoFrame frame) {
            VideoFrame oldFrame = _frame.getAndSet(frame);
            repaint();
            if (oldFrame != null)
                oldFrame.returnToPool();
        }

        @Override
        public void update(Graphics g) {
            paint(g);
        }

        @Override
        public void paint(Graphics g) {
            if (g == null) {
                System.out.println("Frame not drawn because Graphics is null");
                return;
            }
            g.setColor(Color.black);
            // XXX: possible race condition
            // if the event thread gets the current frame, then the
            // VideoPlayer thread returns it to the pool, and then it is picked
            // up again and changed before the event thread can draw it
            VideoFrame frame = _frame.get();
            if (frame != null) {
                int iCW = getWidth();
                int iCH = getHeight();
                int iOfsX = (iCW - __dims.width) / 2;
                int iOfsY = (iCH - __dims.height) / 2;
                g.fillRect(0, 0, iCW, iOfsY);
                g.fillRect(0, iOfsY + __dims.height, iCW, iCH - __dims.height - iOfsY);
                g.fillRect(0, iOfsY, iOfsX, __dims.height);
                g.fillRect(iOfsX+__dims.width, iOfsY, iCW - __dims.width - iOfsX, __dims.height);
                if (g instanceof Graphics2D) {
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, __renderingHintInterpolation);
                }
                g.drawImage(frame.Img, iOfsX, iOfsY, __dims.width, __dims.height, null);
            } else {
                if (DEBUG) System.out.println("Frame not drawn because Img is null");
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        @Override
        public Dimension getMaximumSize() {
            return null;
        }

        @Override
        public Dimension getMinimumSize() {
            return __dims;
        }

        @Override
        public Dimension getPreferredSize() {
            return __dims;
        }

    }


    public final VideoFramePool _videoFramePool = new VideoFramePool();
    public class VideoFramePool extends ObjectPool<VideoFrame> {

        @Override
        protected VideoFrame createNewObject() {
            return new VideoFrame();
        }
    }


    class VideoFrame {
        public int[] Memory;
        public MemoryImageSource MemImgSrc;
        private Image Img;

        public long PresentationTime;
        public long ContigusPlayUniqueId;

        public void init(AbstractDecodableFrame decodeFrame) {
            if (Memory == null) {
                Memory = new int[_iWidth * _iHeight];
                MemImgSrc = new MemoryImageSource(_iWidth, _iHeight, Memory, 0, _iWidth);
                MemImgSrc.setAnimated(true);
                MemImgSrc.setFullBufferUpdates(true);
                Img = _screen.createImage(MemImgSrc);
                Img.setAccelerationPriority(1.0f);
            }
            PresentationTime = decodeFrame.getPresentationTime();
            ContigusPlayUniqueId = decodeFrame.getContigiousId();
        }

        public void returnToPool() {
            _videoFramePool.giveBack(this);
        }
    }

    // ----------------------------------------------------------------------


    private long _lngTimerContiguousPlayId;
    private long _lngTimerStartTime;
    private long _lngTimerPausedTime;
    private boolean _blnTimerStarting;

    public long getContiguousPlayId() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            return _lngTimerContiguousPlayId;
        }
    }


    private long getPlayTime() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isPlaying()) {
                if (_blnTimerStarting)
                    return 0;
                else
                    return System.nanoTime() - _lngTimerStartTime;
            } else if (_frameDisplayQueue.isPaused()) {
                return _lngTimerPausedTime - _lngTimerStartTime;
            } else /* stopped */ {
                return -1;
            }
        }
    }

    public boolean shouldBeProcessed(long lngContiguousPlayId, long lngPresentationTime) {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (lngContiguousPlayId != _lngTimerContiguousPlayId)
                return false;

            if (_frameDisplayQueue.isPaused()) {
                return true;
            } else if (_frameDisplayQueue.isPlaying()) {
                long lngPos = getPlayTime();
                if (DEBUG) System.out.println("Play time = " + lngPos + " vs. Pres time = " + lngPresentationTime);
                return (lngPresentationTime > lngPos);
            } else /* stopped */ {
                return false;
            }
        }
    }


    public boolean waitToPresent(VideoPlayer.VideoFrame frame) {
        try {
            synchronized (_frameDisplayQueue.getSyncObject()) {
                while (true) {
                    if (frame.ContigusPlayUniqueId != _lngTimerContiguousPlayId)
                        return false;

                    if (_frameDisplayQueue.isPaused()) {
                        _frameDisplayQueue.getSyncObject().wait();
                        // now loop again to see the new state or Contiguous Id
                    } else if (_frameDisplayQueue.isPlaying()) {
                        if (_blnTimerStarting) {
                            if (DEBUG) System.out.println("Timer is started, player is now playing");
                            _blnTimerStarting = false;
                            _lngTimerStartTime = System.nanoTime();
                            return true;
                        } else {
                            long lngPos = getPlayTime();
                            long lngSleepTime;
                            if ((lngSleepTime = frame.PresentationTime - lngPos) > 15) {
                                _frameDisplayQueue.getSyncObject().wait(lngSleepTime / 1000000, (int)(lngSleepTime % 1000000));
                                // now loop again to see if the state changed while waiting
                                // or Contiguous Id changed
                            } else {
                                return true;
                            }
                        }
                    } else /* stopped */ {
                        return false;
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
