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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;

/**
 * Uncompressed Device Independent Bitmap (DIB) implementation of AVI writer.
 * Writing frames via BufferedImages is removed due to the often unpredictable
 * and mysterious ColorSpace conversions that can happen depending on how
 * the data is accessed.
 */
public class AviWriterDIB extends AviWriter {

    /** Size of the frame data in bytes. Only applicable to DIB AVI.
     *  Each DIB frame submitted is compared to this value to ensure
     *  proper data. */
    private final int _iFrameByteSize;
    private final int _iLinePadding;

    // -------------------------------------------------------------------------
    // -- Constructors ---------------------------------------------------------
    // -------------------------------------------------------------------------

    public AviWriterDIB(final @Nonnull File outputfile,
                        final int iWidth, final int iHeight,
                        final long lngFrames, final long lngPerSecond)
            throws FileNotFoundException, IOException
    {
        this(outputfile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             null);
    }

    /** Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterDIB(final @Nonnull File outputfile,
                        final int iWidth, final int iHeight,
                        final long lngFrames, final long lngPerSecond,
                        final @CheckForNull AudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
       // Write 'DIB ' for Microsoft Device Independent Bitmap.  Note: Unfortunately,
       // at least 3 other fourcc codes are sometimes used for uncompressed
       // AVI videos: 'RGB ', 'RAW ', 0x00000000
        super(outputfile, iWidth, iHeight, lngFrames, lngPerSecond, audioFormat, true, "DIB ", BITMAPINFOHEADER.BI_RGB);

        int iLinePadding = (getWidth() * 3) & 3;
        if (iLinePadding != 0)
            iLinePadding = 4 - iLinePadding;
        _iLinePadding = iLinePadding;

        _iFrameByteSize = (getWidth() * 3 + iLinePadding) * getHeight() ;
    }

    // -------------------------------------------------------------------------
    // -- Writing functions ----------------------------------------------------
    // -------------------------------------------------------------------------

    /** @param abData  RGB image data stored at 24 bits/pixel (3 bytes/pixel) */
    public void writeFrameRGB(@Nonnull byte[] abData, int iStart, int iLineStride) throws IOException {
        final int WIDTH = getWidth(), HEIGHT = getHeight();

        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameByteSize)
            _abWriteBuffer = new byte[_iFrameByteSize];

        int iSrcLine = iStart + (HEIGHT-1) * iLineStride;
        int iDestPos = 0;
        for (int y = HEIGHT-1; y >= 0; y--) {
            int iSrcPos = iSrcLine;
            for (int x = 0; x < WIDTH; x++) {
                _abWriteBuffer[iDestPos] = abData[iSrcPos+2];
                iDestPos++;
                _abWriteBuffer[iDestPos] = abData[iSrcPos+1];
                iDestPos++;
                _abWriteBuffer[iDestPos] = abData[iSrcPos+0];
                iDestPos++;
                iSrcPos+=3;
            }
            iSrcLine -= iLineStride;
            for (int i = 0; i < _iLinePadding; i++) {
                _abWriteBuffer[iDestPos] = 0;
                iDestPos++;
            }
        }

        writeFrameChunk(_abWriteBuffer, 0, _iFrameByteSize);
    }

    /** @param aiData  RGB image data stored at RGB in the lower bytes of an int. */
    public void writeFrameRGB(@Nonnull int[] aiData, int iStart, int iLineStride) throws IOException {
        final int WIDTH = getWidth(), HEIGHT = getHeight();

        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameByteSize)
            _abWriteBuffer = new byte[_iFrameByteSize];

        int iSrcLine = iStart + (HEIGHT-1) * iLineStride;
        int iDestPos = 0;
        for (int y = HEIGHT-1; y >= 0; y--) {
            int iSrcPos = iSrcLine;
            for (int x = 0; x < WIDTH; x++) {
                int c = aiData[iSrcPos];
                _abWriteBuffer[iDestPos] = (byte)(c      );
                iDestPos++;
                _abWriteBuffer[iDestPos] = (byte)(c >>  8);
                iDestPos++;
                _abWriteBuffer[iDestPos] = (byte)(c >> 16);
                iDestPos++;
                iSrcPos++;
            }
            iSrcLine -= iLineStride;
            for (int i = 0; i < _iLinePadding; i++) {
                _abWriteBuffer[iDestPos] = 0;
                iDestPos++;
            }
        }

        writeFrameChunk(_abWriteBuffer, 0, _iFrameByteSize);
    }

    @Override
    public void writeBlankFrame() throws IOException {
        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameByteSize)
            _abWriteBuffer = new byte[_iFrameByteSize];

        Arrays.fill(_abWriteBuffer, 0, _iFrameByteSize, (byte)0);

        writeFrameChunk(_abWriteBuffer, 0, _iFrameByteSize);
    }

}
