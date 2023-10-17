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

package jpsxdec.util.aviwriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.imageio.ImageWriteParam;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.FastImageIOwriteToBytes;

/**
 * MJPG implementation of AVI writer. It's really just a JPEG file stuffed
 * into each frame. The output seems playable on vanilla Windows XP systems,
 * and of course VLC.
 * <p>
 * According to <a href="http://en.wikipedia.org/wiki/MJPEG">Wikipedia</a>:
 * <blockquote>
 * "there is no document that defines a single exact format that is
 * universally recognized as a complete specification of "Motion JPEG" for
 * use in all contexts."
 * </blockquote>
 * <p>
 * I owe much of my MJPG understanding to the
 * <a href="http://sourceforge.net/projects/jpegtoavi/">jpegtoavi program</a>.
 * According to Microsoft's original MJPEG spec, every JPEG frame should use
 * the default JPEG huffman tables, although most decoders are more lenient than
 * that. VirtualDub's Motion JPEG decoder specifically does require default
 * huffman tables, and also requires the frame dimensions to be multiples of 16.
 */
public class AviWriterMJPG extends AviWriter {

    // -------------------------------------------------------------------------
    // -- Fields ---------------------------------------------------------------
    // -------------------------------------------------------------------------

    @Nonnull
    private final FastImageIOwriteToBytes _img2bytes;

    // -------------------------------------------------------------------------
    // -- Constructors ---------------------------------------------------------
    // -------------------------------------------------------------------------

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterMJPG(final @Nonnull File outputfile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond)
            throws FileNotFoundException, IOException
    {
        this(outputfile, iWidth, iHeight, lngFrames, lngPerSecond, -1, null);
    }
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterMJPG(final @Nonnull File outputfile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond,
                         final @CheckForNull AudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        this(outputfile, iWidth, iHeight, lngFrames, lngPerSecond, -1, audioFormat);
    }
    public AviWriterMJPG(final @Nonnull File outputfile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond,
                         final float fltLossyQuality)
            throws FileNotFoundException, IOException
    {
        this(outputfile, iWidth, iHeight, lngFrames, lngPerSecond, fltLossyQuality, null);
    }
    public AviWriterMJPG(final @Nonnull File outputfile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond,
                         final float fltLossyQuality,
                         final @CheckForNull AudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        super(outputfile, iWidth, iHeight, lngFrames, lngPerSecond, audioFormat, true, "MJPG", AVIstruct.string2int("MJPG"));

        _img2bytes = new FastImageIOwriteToBytes("jpeg");

        if (fltLossyQuality >= 0 && fltLossyQuality <= 1) {
            // TODO: Make sure thumbnails are not being created in the jpegs
            ImageWriteParam writeParams = _img2bytes.getDefaultWriteParam();

            writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // 0 for lowest quality, 1 for highest
            writeParams.setCompressionQuality(fltLossyQuality);
        }
    }

    // -------------------------------------------------------------------------
    // -- Writing functions ----------------------------------------------------
    // -------------------------------------------------------------------------

    /** Converts a BufferedImage to JPEG and writes it. */
    public void writeFrame(@Nonnull BufferedImage bi) throws IOException {
        if (getWidth() != bi.getWidth())
            throw new IllegalArgumentException("AviWriter: Frame width doesn't match" +
                    " (was " + getWidth() + ", now " + bi.getWidth() + ").");

        if (getHeight() != bi.getHeight())
            throw new IllegalArgumentException("AviWriter: Frame height doesn't match" +
                    " (was " + getHeight() + ", now " + bi.getHeight() + ").");

        bi = FastImageIOwriteToBytes.ensureOpaque(bi);
        _img2bytes.convert(bi);

        writeFrameChunk(_img2bytes.getWrittenBytes(), 0, _img2bytes.getWrittenByteSize());
    }

    /** @param abJpeg Must be a jpeg image */
    public void writeFrame(@Nonnull byte[] abJpeg, int iStart, int iSize) throws IOException {
        writeFrameChunk(abJpeg, iStart, iSize);
    }

    @Override
    public void writeBlankFrame() throws IOException {
        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        writeFrame(bi);
    }

}
