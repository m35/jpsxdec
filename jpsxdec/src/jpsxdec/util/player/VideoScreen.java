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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.Fraction;

/**
 * Canvas where video frames will appear on the screen.
 *
 * Java has the ability to run full screen and double buffered, so displaying a
 * small panel shouldn't be a problem. My research found more than one way to do
 * it. I went with using a {@link BufferStrategy} with 2 buffers,
 * and it has proved to be reliable, so didn't test alternatives.
 *
 * It exposes the option to change the video's aspect ratio.
 */
class VideoScreen extends Canvas {

    private final int _iWidth, _iHeight;

    /** Adjust the rendered frame with this aspect ratio. */
    @Nonnull
    private Fraction _aspectRatio = PlayController.PAL_ASPECT_RATIO;

    @Nonnull
    private Dimension _minDims;

    @Nonnull
    private Object _renderingHintInterpolation =
            RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            //RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
    @CheckForNull
    private transient BufferStrategy _buffStrategy;
    @CheckForNull
    private transient VideoFrame<?> _currentFrame;

    public VideoScreen(int iWidth, int iHeight) {
        _iWidth = iWidth;
        _iHeight = iHeight;
        setBackground(Color.BLACK);
        updateDims();
    }

    public void setAspectRatio(@Nonnull Fraction aspectRatio) {
        _aspectRatio = aspectRatio;
        updateDims();
    }

    public void setRenderingHint(Object interpolation) {
        _renderingHintInterpolation = interpolation;
    }

    //--------------------------------------------------------------------------
    /**
     * This is jPSXdec specific, can ignore.
     * Some PlayStation videos had an excessively long width, over 600 pixels,
     * but the PlayStation's display only allowed 320 pixel width.
     * The games would squash the width to fit into 320 pixels.
     * This causes this video player to do the same.
     */
    public void setSquashWidth(boolean blnSquash) {
        _blnSquashWidth = blnSquash;
        updateDims();
    }
    /** Squash oversized frames to fit in TV. */
    private boolean _blnSquashWidth = false;
    private int getSrcWidth() {
        if (_blnSquashWidth && _iWidth > 320) {
            return 320;
        } else {
            return _iWidth;
        }
    }
    //--------------------------------------------------------------------------

    private void updateDims() {
        _minDims = new Dimension(getSrcWidth(),
            (int)(_iHeight * _aspectRatio.getNumerator() / _aspectRatio.getDenominator()));
    }

    public void updateImage(@Nonnull VideoFrame<?> frame) {
        synchronized (getTreeLock()) {
            _currentFrame = frame;
            if (!isDisplayable()) {
                // can't use or create BufferStrategy unless it is visible
                System.out.println("Trying to play frame when canvas is hidden");
                return;
            }
            if (this.getWidth() == 0 || this.getHeight() == 0) {
                return;
            }

            if (_buffStrategy == null) {
                createBufferStrategy(2);
                System.out.println("BufferStrategy created");
                _buffStrategy = getBufferStrategy();
            }

            Graphics g = _buffStrategy.getDrawGraphics();
            paint(g);
            g.dispose();
            _buffStrategy.show();
        }
    }

    @Override
    public void paint(@Nonnull Graphics g) {
        final int iWinW = this.getWidth();
        final int iWinH = this.getHeight();

        // Clear the screen with black
        g.setColor(Color.black);
        g.fillRect(0, 0, iWinW, iWinH);

        float fltConvertAspectRatio = (getSrcWidth()  * _aspectRatio.getDenominator()  ) /
                               (float)(_iHeight       * _aspectRatio.getNumerator());
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
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, _renderingHintInterpolation);
        }

        if (_currentFrame != null)
            g.drawImage(_currentFrame.image, iOfsX, iOfsY, iDispW, iDispH, null);
    }

    @Override
    public Dimension getMaximumSize() {
        return null;
    }

    @Override
    public Dimension getMinimumSize() {
        return _minDims;
    }

    @Override
    public Dimension getPreferredSize() {
        return _minDims;
    }

}
