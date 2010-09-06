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

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import jpsxdec.formats.RgbIntImage;


class VideoPlayer implements Runnable {

    public static boolean DEBUG = false;

    private static final int CAPACITY = 50;

    private MultiStateBlockingQueue<VideoFrame> _frameDisplayQueue =
            new MultiStateBlockingQueue<VideoFrame>(CAPACITY);

    private PlayController _controller;
    private int _iWidth, _iHeight;

    private int _iZoom = 1;

    /** Only used for video only streams. */
    private Thread _thread;

    private VideoScreen _screen;

    public VideoPlayer(PlayController controller, int iWidth, int iHeight) {
        _controller = controller;
        _iWidth = iWidth;
        _iHeight = iHeight;
        _screen = new VideoScreen();
    }

    public void run() {
        try {
            VideoFrame frame;
            while ((frame = _frameDisplayQueue.take()) != null) {
                // check if this frame is part of current play sequence
                // if presentation time is less-than-equal now, show frame
                if (frame.ContigusPlayUniqueId == _controller.getContiguousPlayUniqueId())
                {
                    // otherwise sleep until time to display
                    long lngDiff;
                    while ((lngDiff = frame.PresentationTime - _controller.getCurrentPlayTime()) > 15) {
                        if (DEBUG) System.out.println("Player sleeping " + lngDiff);
                        Thread.sleep(lngDiff);
                    }
                    if (DEBUG) System.out.println("===Displaying frame===");
                    // update canvas with frame
                    _screen.updateRepaint(frame);
                } else {
                    if (DEBUG) System.out.println("Different contigouous id");
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            _frameDisplayQueue.stop();
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
        _frameDisplayQueue.play();
    }
    
    public void clearQueue() {
        _frameDisplayQueue.clear();
    }
    
    public void pause() {
        _frameDisplayQueue.pause();
    }

    public void stopWhenEmpty() {
        _frameDisplayQueue.stopWhenEmpty();
    }

    public void startup() {
        _thread = new Thread(this, "Video Player");
        _thread.start();
    }

    public void shutdown() {
        _frameDisplayQueue.stop();
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

    public Canvas getVideoCanvas() {
        return _screen;
    }

    public int getWidth() {
        return _iWidth;
    }
    public int getHeight() {
        return _iHeight;
    }

    private class VideoScreen extends Canvas {

        /* ************************************************************
         * Use MemoryImageSource? Or maybe it's not so helpful?
         * 	MemoryImageSource ret = new MemoryImageSource(width, height, pixels, 0, width);
            ret.setAnimated(true);
            ret.setFullBufferUpdates(true);
         ************************************************************* */
        private VideoFrame __lastFrame;
        final private Object __drawSync = new Object();

        private Dimension __dims = updateDims();

        public Dimension updateDims() {
            return __dims = new Dimension(_iWidth * _iZoom, _iHeight * _iZoom * 59 / 54);
        }

        public void updateRepaint(VideoFrame frame) {
            if (__lastFrame == null) {
                __lastFrame = frame;
            } else {
                // XXX: race condition
                VideoFrame lastFrame;
                synchronized (__drawSync) {
                    lastFrame = __lastFrame;
                    __lastFrame = frame;
                }
                lastFrame.returnToPool();
            }
            update(getGraphics());
            //Thread.yield();
        }

        @Override
        public void update(Graphics g) {
            if (g instanceof Graphics2D) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }
            // XXX: possible deadlock here
            synchronized (__drawSync) {
                g.drawImage(__lastFrame.Img, 0, 0, __dims.width, __dims.height, this);
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
        /** MemoryImageSource is tricky if you've never used it before.
         * http://rsb.info.nih.gov/plasma/ */
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
            Img.flush(); // XXX: possible deadlock here
            _videoFramePool.giveBack(this);
        }
    }

}
