/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * This does the following, but faster when used repeatedly (in theory).
 * <pre>
 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
 * ImageIO.write(bi, "png", baos);
 * baos.toByteArray();
 * </pre>
 */
public class FastImageIOwriteToBytes {

    private static final Logger LOG = Logger.getLogger(FastImageIOwriteToBytes.class.getName());

    @Nonnull
    private final ExposedBAOS _buffer;
    @Nonnull
    private final ImageWriter _imgWriter;
    @CheckForNull
    private ImageWriteParam _writeParam = null;

    /**
     * @throws UnsupportedOperationException if the image format is not supported on this platform.
     */
    public FastImageIOwriteToBytes(@Nonnull String sImageIOFormatName) throws UnsupportedOperationException {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(sImageIOFormatName);
        _imgWriter = iter.next();
        if (_imgWriter == null)
            throw new UnsupportedOperationException("Unable to create '"+sImageIOFormatName+"' images on this platform.");

        _buffer = new ExposedBAOS();
    }

    /** @see ImageWriter#getDefaultWriteParam() */
    public @Nonnull ImageWriteParam getDefaultWriteParam() {
        if (_writeParam == null)
            _writeParam = _imgWriter.getDefaultWriteParam();
        return _writeParam;
    }

    public @Nonnull byte[] toBytes(@Nonnull BufferedImage img) {
        convert(img);
        return _buffer.toByteArray();
    }

    /** Alternative to {@link #toBytes(java.awt.image.BufferedImage)} if you
     * want access to the written buffer directly instead of a copy
     * (to avoid an extra copy if it helps).
     * After calling this, use {@link #getWrittenBytes()} and {@link #getWrittenByteSize()}
     * to access the buffer directly. */
    public void convert(@Nonnull BufferedImage img) {
        _buffer.reset();
        // have to wrap the ByteArrayOutputStream with a MemoryCacheImageOutputStream
        MemoryCacheImageOutputStream imgOut = new MemoryCacheImageOutputStream(_buffer);
        // set our image writer's output stream
        _imgWriter.setOutput(imgOut);

        // wrap the BufferedImage with a IIOImage
        IIOImage imgIO = new IIOImage(img, null, null);
        try {
            // finally write the buffered image to the output stream
            // using our parameters (if any)
            _imgWriter.write(null, imgIO, _writeParam);
            // don't forget to flush
            imgOut.flush();
        } catch (IOException ex) {
            // we're writing to memory, so this shouldn't ever happen?
            throw new RuntimeException(ex);
        } finally {
            // clear image writer's output stream
            _imgWriter.setOutput(null);
            try {
                imgOut.close();
            } catch (IOException ex) {
                // we're writing to memory, so this shouldn't ever happen?
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    public @Nonnull byte[] getWrittenBytes() {
        return _buffer.getBuffer();
    }

    public int getWrittenByteSize() {
        return _buffer.size();
    }

    /**
     * Utility function (not actually used by this class).
     * Some Java versions will blow up if you try to write a JPEG with a
     * BufferedImage with transparency.
     * https://stackoverflow.com/questions/3432388/imageio-not-able-to-write-a-jpeg-file
     */
    public static @Nonnull BufferedImage ensureOpaque(@Nonnull BufferedImage bi) {
        if (bi.getTransparency() == BufferedImage.OPAQUE)
            return bi;
        // TODO: test if this is faster than using Graphics.drawImage()
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] pixels = new int[w * h];
        // it probably could be faster if these pixels were copied directly into
        // the new BI raster data since the format should be the same
        bi.getRGB(0, 0, w, h, pixels, 0, w);
        BufferedImage opaqueBi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        opaqueBi.setRGB(0, 0, w, h, pixels, 0, w);
        return opaqueBi;
    }
}
