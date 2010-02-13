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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

/** Writes a series of Yuv4mpeg2 objects to a Yuv4mpeg file. */
public class Yuv4mpeg2Writer {
    
    /** Width of the image in luminance samples. */
    private final int _iWidth;
    /** Height of the image in luminance samples. */
    private final int _iHeight;
    /** Stream to write text data. */
    private OutputStreamWriter _txtWriter;
    /** Stream to write binary data. */
    private BufferedOutputStream _outStream;
    
    /** Creates a new instance of PsxYuvImage.
     * @param iWidth - Width of image (in Luminance values) 
     * @param iHeight - Height of image (in Luminance values) */
    public Yuv4mpeg2Writer(File file, int iWidth, int iHeight,
                           int iFrames, int iPerSecond,
                           String sChromaSubsampling)
           throws IOException
    {
        assert(iWidth > 0 && iHeight > 0 && 
               (iWidth  % 2) == 0 && 
               (iHeight % 2) == 0);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _outStream = new BufferedOutputStream(new FileOutputStream(file));
        _txtWriter = new OutputStreamWriter(_outStream, "US-ASCII");

        // write the header
        _txtWriter.write("YUV4MPEG2");
        _txtWriter.write(" W" + _iWidth);
        _txtWriter.write(" H" + _iHeight);
        _txtWriter.write(" C" + sChromaSubsampling);
        // TODO: Try NTSC or PAL aspect ratio if known
        _txtWriter.write(" A1:1"); // aspect ratio 1:1
        _txtWriter.write(" F" + iFrames + ":" + iPerSecond);
        _txtWriter.write(" Ip");  // none/progressive
        _txtWriter.write('\n');
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    /** Write a yuv4mpeg2 image file. */
    public void writeFrame(Yuv4mpeg2 yuv) throws IOException {

        if (yuv.getWidth() != _iWidth || yuv.getHeight() != _iHeight)
            throw new IllegalArgumentException("Yuv dimmensions are different.");

        _txtWriter.write("FRAME");
        _txtWriter.write('\n');
        _txtWriter.flush();

        // write the data
        _outStream.write(yuv._abY, 0, yuv._abY.length);
        _outStream.write(yuv._abCb, 0, yuv._abCb.length);
        _outStream.write(yuv._abCr, 0, yuv._abCr.length);
    }

    public void close() throws IOException {
        _txtWriter.flush();
        _txtWriter.close();
    }

}
