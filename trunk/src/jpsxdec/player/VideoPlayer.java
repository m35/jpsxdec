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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


public class VideoPlayer implements Runnable {

    public static boolean DEBUG = false;

    private static final int CAPACITY = 20;

    private MultiStateBlockingQueue<VideoFrame> _frameDisplayQueue =
            new MultiStateBlockingQueue<VideoFrame>(CAPACITY);

    private PlayController _controller;
    private int _iWidth, _iHeight;

    private int _iZoom = 1;

    /** Only used for video only streams. */
    private Thread _thread;

    public VideoPlayer(PlayController controller, int iWidth, int iHeight) {
        _controller = controller;
        _iWidth = iWidth;
        _iHeight = iHeight;
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
                    while ((lngDiff = frame.PresentationTime - _controller.getCurrentPlayTime()) > 30) {
                        if (DEBUG) System.out.println("Player sleeping " + lngDiff);
                        Thread.sleep(lngDiff);
                    }
                    if (DEBUG) System.out.println("===Displaying frame===");
                    // update canvas with frame
                    _screen.updateRepaint(frame.Frame);
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
    }

    private VideoScreen _screen;
    public Canvas getVideoCanvas() {
        if (_screen == null)
            _screen = new VideoScreen();
        return _screen;
    }

    private class VideoScreen extends Canvas {

        /* ************************************************************
         * DEFINITELY WANT TO USE MemoryImageSource FOR THE IMAGE DATA
         ************************************************************* */
        private BufferedImage __lastImg;

        private Dimension __dims = updateDims();

        public Dimension updateDims() {
            return __dims = new Dimension(_iWidth * _iZoom, _iHeight * _iZoom * 59 / 54);
        }

        public void updateRepaint(BufferedImage bi) {
            __lastImg = bi;
            repaint();
        }

        @Override
        public void update(Graphics g) {
            if (g instanceof Graphics2D) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            g.drawImage(__lastImg, 0, 0, __dims.width, __dims.height, this);
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
    
}
