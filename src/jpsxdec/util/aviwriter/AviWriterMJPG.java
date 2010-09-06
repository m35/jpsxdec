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

package jpsxdec.util.aviwriter;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.sound.sampled.AudioFormat;
import jpsxdec.util.ExposedBAOS;

/**
 * MJPG implementation of AVI writer. It's really just a JPEG file stuffed
 * into each frame. The output seems playable on vanilla Windows XP systems,
 * and of course VLC.
 * <p>
 * According to <a href="http://en.wikipedia.org/wiki/MJPEG">Wikipedia</a>:
 * <blockquote>
 * "there is no document that defines a single exact format that is
 * universally recognized as a complete specification of “Motion JPEG” for
 * use in all contexts."
 * </blockquote>
 * <p>
 * I owe my MJPG understanding to the jpegtoavi program.
 * http://sourceforge.net/projects/jpegtoavi/
 * <p>
 * Random list of codecs
 * http://www.oltenia.ro/download/pub/windows/media/video/tools/GSpot/gspot22/GSpot22.dat
 * <p>
 * http://www.alexander-noe.com/video/documentation/avi.pdf
 */
public class AviWriterMJPG extends AviWriter {

    /** Hold's true if system can write "jpeg" images. */
    private final static boolean CAN_ENCODE_JPEG;
    static {
        // check if the system can write "jpeg" images
        boolean bln = false;
        for (String s : ImageIO.getReaderFormatNames()) {
            if (s.equals("jpeg")) {
                bln = true;
                break;
            }
        }
        CAN_ENCODE_JPEG = bln;
    }
    
    // -------------------------------------------------------------------------
    // -- Fields ---------------------------------------------------------------
    // -------------------------------------------------------------------------
    

    /** The image writer used to convert the BufferedImages to JPEG. */
    private final ImageWriter _imgWriter;
    /** Only used not using default quality level. */
    private final ImageWriteParam _writeParams;

    
    // -------------------------------------------------------------------------
    // -- Constructors ---------------------------------------------------------
    // -------------------------------------------------------------------------
    
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterMJPG(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond)
            throws IOException
    {
        this(oOutputfile, iWidth, iHeight, lngFrames, lngPerSecond, -1, null);
    }
    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterMJPG(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final AudioFormat oAudioFormat)
            throws IOException
    {
        this(oOutputfile, iWidth, iHeight, lngFrames, lngPerSecond, -1, oAudioFormat);
    }
    public AviWriterMJPG(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final float fltLossyQuality)
            throws IOException
    {
        this(oOutputfile, iWidth, iHeight, lngFrames, lngPerSecond, fltLossyQuality, null);
    }
    public AviWriterMJPG(final File oOutputfile,
                     final int iWidth, final int iHeight,
                     final long lngFrames, final long lngPerSecond,
                     final float fltLossyQuality,
                     final AudioFormat oAudioFormat)
            throws IOException
    {
        super(oOutputfile, iWidth, iHeight, lngFrames, lngPerSecond, oAudioFormat, true, "MJPG", AVIstruct.string2int("MJPG"));

        if (!CAN_ENCODE_JPEG)
            throw new UnsupportedOperationException("Unable to create 'jpeg' images on this platform.");

        Iterator<ImageWriter> oIter = ImageIO.getImageWritersByFormatName("jpeg");
        _imgWriter = oIter.next();

        if (fltLossyQuality < 0 || fltLossyQuality > 1) {
            _writeParams = null;
        } else {
            // TODO: Make sure thumbnails are not being created in the jpegs
            _writeParams = _imgWriter.getDefaultWriteParam();

            _writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // 0 for lowest qulaity, 1 for highest
            _writeParams.setCompressionQuality(fltLossyQuality);
        }

    }

    // -------------------------------------------------------------------------
    // -- Writing functions ----------------------------------------------------
    // -------------------------------------------------------------------------

    /** Converts a BufferedImage to proper avi format and writes it. */
    public void writeFrame(BufferedImage bi) throws IOException {
        if (getWidth() != bi.getWidth())
            throw new IllegalArgumentException("AviWriter: Frame width doesn't match" +
                    " (was " + getWidth() + ", now " + bi.getWidth() + ").");
        
        if (getHeight() != bi.getHeight())
            throw new IllegalArgumentException("AviWriter: Frame height doesn't match" +
                    " (was " + getHeight() + ", now " + bi.getHeight() + ").");

        ExposedBAOS out = image2MJPEG(bi);

        writeFrameChunk(out.getBuffer(), 0, out.size());
    }


    @Override
    public void writeBlankFrame() throws IOException {
        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        writeFrame(bi);
    }

    // -------------------------------------------------------------------------
    // -- Private functions ----------------------------------------------------
    // -------------------------------------------------------------------------
    
    /** Converts a BufferedImage into a frame to be written into a MJPG avi. */
    private ExposedBAOS image2MJPEG(BufferedImage img) throws IOException {
        ExposedBAOS jpgStream = writeImageToBytes(img, new ExposedBAOS());
        //IO.writeFile("test.bin", abJpg); // debug
        JPEG2MJPEG(jpgStream.getBuffer());
        return jpgStream;
    }

    private ExposedBAOS writeImageToBytes(BufferedImage img, ExposedBAOS out) throws IOException {
        // have to wrap the ByteArrayOutputStream with a MemoryCacheImageOutputStream
        MemoryCacheImageOutputStream imgOut = new MemoryCacheImageOutputStream(out);
        // set our image writer's output stream
        _imgWriter.setOutput(imgOut);

        // wrap the BufferedImage with a IIOImage
        IIOImage imgIO = new IIOImage(img, null, null);
        // finally write the buffered image to the output stream
        // using our parameters (if any)
        _imgWriter.write(null, imgIO, _writeParams);
        // don't forget to flush
        imgOut.flush();
        imgOut.close();

        // clear image writer's output stream
        _imgWriter.setOutput(null);

        // return the result
        return out;
    }
    
    /** Converts JPEG file data to be used in an MJPG AVI. */
    private static void JPEG2MJPEG(byte [] ab) throws IOException {
        if (ab[6] != 'J' || ab[7] != 'F' || ab[8] != 'I' || ab[9] != 'F')
            throw new IOException("JFIF header not found in jpeg data, unable to write frame to AVI.");
        // http://cekirdek.pardus.org.tr/~ismail/ffmpeg-docs/mjpegdec_8c-source.html#l00869
        // ffmpeg treats the JFIF and AVI1 header differently. It's probably
        // safer to stick with standard JFIF header since that's what JPEG uses.
        /*
        ab[6] = 'A';
        ab[7] = 'V';
        ab[8] = 'I';
        ab[9] = '1';
        */
    }

}
