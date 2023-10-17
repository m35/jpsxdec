/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.awt.image.*;
import java.io.*;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

/** Simplest image format containing a buffer and dimensions. */
public class RgbIntImage {


    private final int _iWidth, _iHeight;
    @Nonnull
    private int[] _aiData;

    public RgbIntImage(@Nonnull BufferedImage bi) {
        this(bi.getWidth(), bi.getHeight());
        _aiData = new int[_iWidth * _iHeight];
        // TODO: I'm still uncertain about colormode/colormodel stuff
        bi.getRGB(0, 0, _iWidth, _iHeight, _aiData, 0, _iWidth);
    }

    public RgbIntImage(int iWidth, int iHeight, @Nonnull int[] aiData) {
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
    public @Nonnull int[] getData() { return _aiData; }

    public int get(int x, int y) {
        return _aiData[x + y * _iWidth];
    }

    public void set(int x, int y, int i) {
        _aiData[x + y * _iWidth] = i;
    }

    public @Nonnull BufferedImage toBufferedImage() {
        BufferedImage bi = new BufferedImage(_iWidth, _iHeight, BufferedImage.TYPE_INT_RGB);
        WritableRaster raster = bi.getRaster();
        raster.setDataElements(0, 0, _iWidth, _iHeight, _aiData);
        return bi;
    }

    /* Results show Raster.setDataElements() is nearly identical to
     * System.arraycopy(DataBufferInt), and are both about 20 times
     * faster than BufferedImage.setRGB()
     *
    public static void main(String[] args) throws IOException {
        final int WIDTH = 500, HEIGHT = 500, ITERATIONS = 1000;
        final String FMT = "png";
        int[] aiData = new int[WIDTH * HEIGHT];
        Random rand = new Random();
        for (int i = 0; i < aiData.length; i++) {
            aiData[i] = rand.nextInt();
        }

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            bi.setRGB(0, 0, WIDTH, HEIGHT, aiData, 0, WIDTH);
        }
        lngEnd = System.currentTimeMillis();
        System.out.println("BufferedImage.setRGB(): " + (lngEnd - lngStart));
        if (!ImageIO.write(bi, "png", new File("test-bi-setrgb."+FMT)))
            System.out.println("Failed to write test-bi-setrgb."+FMT);

        lngStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            bi.getRaster().setDataElements(0, 0, WIDTH, HEIGHT, aiData);
        }
        lngEnd = System.currentTimeMillis();
        System.out.println("Raster.setDataElements(): " + (lngEnd - lngStart));
        if (!ImageIO.write(bi, "png", new File("test-ras-setelm."+FMT)))
            System.out.println("Failed to write test-ras-setelm."+FMT);

        lngStart = System.currentTimeMillis();
        for (int i = 0; i < ITERATIONS; i++) {
            int[] aiBi = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
            System.arraycopy(aiData, 0, aiBi, 0, WIDTH*HEIGHT);
        }
        lngEnd = System.currentTimeMillis();
        System.out.println("System.arraycopy(DataBufferInt): " + (lngEnd - lngStart));
        if (!ImageIO.write(bi, "png", new File("test-ras-arycpy."+FMT)))
            System.out.println("Failed to write test-ras-arycpy."+FMT);
    }
    //*/

}
