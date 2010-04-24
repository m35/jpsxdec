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

package jpsxdec.formats;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;

/** Simplest image format containing a buffer and dimentions. */
public class RgbIntImage {


    private final int _iWidth, _iHeight;
    private int[] _aiData;

    public RgbIntImage(BufferedImage bi) {
        this(bi.getWidth(), bi.getHeight());
        _aiData = new int[_iWidth * _iHeight];
        // TODO: I'm still uncertain about colormode/colormodel stuff
        bi.getRGB(0, 0, _iWidth, _iHeight, _aiData, 0, _iWidth);
    }

    public RgbIntImage(int iWidth, int iHeight, int[] aiData) {
        _iWidth = iWidth;
        _iHeight = iHeight;
        _aiData = aiData;
    }

    public RgbIntImage(int iWidth, int iHeight) {
        this(iWidth, iHeight, new int[iWidth * iHeight]);
    }

    public RgbIntImage(int iWidth, int iHeight, int iBufferSize) {
        this(iWidth, iHeight, new int[iBufferSize]);
    }

    public int getWidth() { return _iWidth; }
    public int getHeight() { return _iHeight; }
    public int[] getData() { return _aiData; }
    
    public int get(int x, int y) {
        return _aiData[x + y * _iWidth];
    }

    public void set(int x, int y, int i) {
        _aiData[x + y * _iWidth] = i;
    }

    public BufferedImage toBufferedImage() {
        // TODO: I'm still uncertain about colormode/colormodel stuff
        BufferedImage bi = new BufferedImage(_iWidth, _iHeight, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, _iWidth, _iHeight, _aiData, 0, _iWidth);
        return bi;
    }

}
