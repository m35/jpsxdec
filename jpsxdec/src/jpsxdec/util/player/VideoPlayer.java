/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/** Video player thread manages the actual display of video frames. */
class VideoPlayer implements Runnable, IVideoTimer {

    private static final boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private final ObjectPlayStream<VideoFrame> _frameDisplayQueue =
            new ObjectPlayStream<VideoFrame>(CAPACITY);

    private final int _iWidth, _iHeight;

    /** Only used for video only streams. */
    @CheckForNull
    private Thread _thread;

    @Nonnull
    private final VideoScreen _screen;
    @Nonnull
    private final IVideoTimer _vidTimer;
    @Nonnull
    private final PlayController _controller;

    public VideoPlayer(@CheckForNull IVideoTimer vidTimer, @Nonnull PlayController controller,
                       int iWidth, int iHeight)
    {
        if (vidTimer == null)
            _vidTimer = this;
        else
            _vidTimer = vidTimer;
        _controller = controller;
        _iWidth = iWidth;
        _iHeight = iHeight;
        _screen = new VideoScreen();
        _frameDisplayQueue.writerClose();
        _frameDisplayQueue.readerClose();
    }

    public void run() {
        try {
            VideoFrame frame;
            while ((frame = _frameDisplayQueue.read()) != null) {
                boolean blnPresent = _vidTimer.waitToPresent(frame);
                if (!blnPresent) {
                    System.out.println("Timer says to discard frame");
                } else {
                    if (DEBUG) System.out.println("===Displaying frame=== @" + frame.PresentationTime);
                    _screen.updateImage(frame);
                }
            }

            if (DEBUG) System.out.println("Player received no frame for display, stopping");
            
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            _frameDisplayQueue.readerClose();
            _controller.notifyDonePlaying();
        }
    }

    public boolean isDone() {
        return _frameDisplayQueue.isReaderClosed();
    }

    public void addFrame(VideoFrame frame) {
        try {
            _frameDisplayQueue.write(frame);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void startPaused() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isReaderClosed()) {
                _frameDisplayQueue.writerOpen();
                _frameDisplayQueue.readerPause();
                _thread = new Thread(this, getClass().getName());
                _thread.start();
                _blnTimerStarting = true;
            }
        }
    }
    
    public void stop() {
        _frameDisplayQueue.readerClose();
    }

    public void pause() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isReaderOpen()) {
                _lngTimerPausedTime = System.nanoTime();
                _frameDisplayQueue.readerPause();
            }
        }
    }

    public void unpause() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isReaderOpenPaused()) {
                _frameDisplayQueue.readerOpen();
                _lngTimerStartTime += System.nanoTime() - _lngTimerPausedTime;
            }
        }
    }

    public void writerClose() {
        _frameDisplayQueue.writerClose();
    }

    public @Nonnull Canvas getVideoCanvas() {
        return _screen;
    }

    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    /** Adjust the rendered frame with this aspect ratio. */
    public void setAspectRatio(@Nonnull Fraction aspectRatio) {
        _screen.setAspectRatio(aspectRatio);
    }

    /** Squash oversized frames to fit in TV. */
    public void setSquashWidth(boolean blnSquash) {
        _screen.setSquashWidth(blnSquash);
    }

    // ----------------------------------------------------------------------
    
    private class VideoScreen extends Canvas {

        /** Adjust the rendered frame with this aspect ratio. */
        @Nonnull
        private Fraction __aspectRatio = PlayController.PAL_ASPECT_RATIO;

        @Nonnull
        private Dimension __minDims;

        /** Squash oversized frames to fit in TV. */
        private boolean __blnSquashWidth = false;
        @Nonnull
        private Object __renderingHintInterpolation = 
                RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                //RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        @CheckForNull
        private BufferStrategy __buffStrategy;
        @CheckForNull
        private VideoFrame __currentFrame;

        public VideoScreen() {
            setBackground(Color.BLACK);
            updateDims();
        }

        public void setAspectRatio(@Nonnull Fraction aspectRatio) {
            __aspectRatio = aspectRatio;
            updateDims();
        }

        /** Squash oversized frames to fit in TV. */
        public void setSquashWidth(boolean blnSquash) {
            __blnSquashWidth = blnSquash;
            updateDims();
        }

        private void updateDims() {
            __minDims = new Dimension(getSrcWidth(),
                (int)(_iHeight * __aspectRatio.getNumerator() / __aspectRatio.getDenominator()));
        }

        private void updateImage(@Nonnull VideoFrame frame) {
            synchronized (getTreeLock()) {
                if (__currentFrame != null)
                    __currentFrame.returnToPool();
                __currentFrame = frame;
                if (__currentFrame == null)
                    return;
                if (!isDisplayable()) {
                    // can't use or create BufferStrategy unless it is visible
                    System.out.println("Trying to play frame when canvas is hidden");
                    return;
                }
                if (this.getWidth() == 0 || this.getHeight() == 0) {
                    return;
                }

                if (__buffStrategy == null) {
                    createBufferStrategy(2);
                    System.out.println("BufferStrategy created");
                    __buffStrategy = getBufferStrategy();
                }
                Graphics g = __buffStrategy.getDrawGraphics();
                paint(g);
                g.dispose();
                __buffStrategy.show();
            }
        }

        private int getSrcWidth() {
            if (__blnSquashWidth && _iWidth > 320) {
                return 320;
            } else {
                return _iWidth;
            }
        }

        @Override
        public void paint(@Nonnull Graphics g) {
            final int iWinW = this.getWidth();
            final int iWinH = this.getHeight();
            g.setColor(Color.black);
            g.fillRect(0, 0, iWinW, iWinH);
            float fltConvertAspectRatio = (getSrcWidth()  * __aspectRatio.getDenominator()  ) /
                                   (float)(_iHeight       * __aspectRatio.getNumerator());
            float fltWinAspectRatio = iWinW / (float)iWinH;
            int iDispW, iDispH;
            if (fltConvertAspectRatio > fltWinAspectRatio) {
                iDispW = iWinW;
                iDispH = (int) (iDispW / fltConvertAspectRatio);
            } else {
                iDispH = iWinH;
                iDispW = (int) (iDispH * fltConvertAspectRatio);
            }
            int iOfsX = (iWinW - iDispW) / 2;
            int iOfsY = (iWinH - iDispH) / 2;
            if (g instanceof Graphics2D) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, __renderingHintInterpolation);
            }
            // if painting from Swing GUI, don't want Video thread to
            // return frame to pool in the middle of us painting
            synchronized (getTreeLock()) {
                if (__currentFrame != null)
                    g.drawImage(__currentFrame.Img, iOfsX, iOfsY, iDispW, iDispH, null);
            }
        }

        @Override
        public Dimension getMaximumSize() {
            return null;
        }

        @Override
        public Dimension getMinimumSize() {
            return __minDims;
        }

        @Override
        public Dimension getPreferredSize() {
            return __minDims;
        }

    }

    // ----------------------------------------------------------------------

    public final VideoFramePool _videoFramePool = new VideoFramePool();
    public class VideoFramePool extends ObjectPool<VideoFrame> {

        @Override
        protected VideoFrame createNewObject() {
            return new VideoFrame();
        }
    }


    class VideoFrame {
        @Nonnull
        public final BufferedImage Img;
        public long PresentationTime;

        public VideoFrame() {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            Img = gc.createCompatibleImage(_iWidth, _iHeight, Transparency.OPAQUE);
        }

        public void returnToPool() {
            _videoFramePool.giveBack(this);
        }
    }

    // ----------------------------------------------------------------------

    private static final int FRAME_DELAY_FUDGE_TIME = 50;

    private long _lngTimerStartTime;
    private long _lngTimerPausedTime;
    private boolean _blnTimerStarting;

    public long getNanoPlayTime() {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isReaderOpen()) {
                if (_blnTimerStarting)
                    return 0;
                else
                    return System.nanoTime() - _lngTimerStartTime;
            } else if (_frameDisplayQueue.isReaderOpenPaused()) {
                return _lngTimerPausedTime - _lngTimerStartTime;
            } else /* stopped */ {
                return -1;
            }
        }
    }

    public @Nonnull Object getSyncObject() {
        return _frameDisplayQueue.getSyncObject();
    }

    public boolean shouldBeProcessed(long lngPresentationTime) {
        synchronized (_frameDisplayQueue.getSyncObject()) {
            if (_frameDisplayQueue.isReaderOpenPaused()) {
                return true;
            } else if (_frameDisplayQueue.isReaderOpen()) {
                long lngPos = getNanoPlayTime();
                if (DEBUG) System.out.println("Play time = " + lngPos + " vs. Pres time = " + lngPresentationTime);
                return (lngPresentationTime > lngPos);
            } else /* stopped */ {
                return false;
            }
        }
    }


    public boolean waitToPresent(@Nonnull VideoPlayer.VideoFrame frame) {
        try {
            synchronized (_frameDisplayQueue.getSyncObject()) {
                while (true) {
                    if (_frameDisplayQueue.isReaderOpenPaused()) {
                        _frameDisplayQueue.getSyncObject().wait();
                        // now loop again to see the new state
                    } else if (_frameDisplayQueue.isReaderOpen()) {
                        if (_blnTimerStarting) {
                            if (DEBUG) System.out.println("Timer is started, player is now playing");
                            _blnTimerStarting = false;
                            _lngTimerStartTime = System.nanoTime();
                            return true;
                        } else {
                            long lngPos = getNanoPlayTime();
                            long lngSleepTime;
                            if ((lngSleepTime = frame.PresentationTime - lngPos) > FRAME_DELAY_FUDGE_TIME) {
                                lngSleepTime -= FRAME_DELAY_FUDGE_TIME;
                                _frameDisplayQueue.getSyncObject().wait(lngSleepTime / 1000000, (int)(lngSleepTime % 1000000));
                                // now loop again to see if the state changed while waiting
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
