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

/** AVI Writer for Rec.601 YCbCr with 4:2:0 chroma subsampling, i.e.
 * FOURCC 'YV12'. */
public class AviWriterYV12 extends AviWriter {

    private final int _iFrameByteSize, _iFrameYByteSize, _iFrameCByteSize;

    /** Dimensions must be a multiple of 2. */
    public AviWriterYV12(final @Nonnull File outFile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond)
            throws FileNotFoundException, IOException
    {
        this(outFile,
             iWidth, iHeight,
             lngFrames, lngPerSecond,
             null);
    }

    /** Dimensions must be a multiple of 2.
     * Audio data must be signed 16-bit PCM in little-endian order. */
    public AviWriterYV12(final @Nonnull File outFile,
                         final int iWidth, final int iHeight,
                         final long lngFrames, final long lngPerSecond,
                         final @CheckForNull AudioFormat audioFormat)
            throws FileNotFoundException, IOException
    {
        super(outFile, iWidth, iHeight, lngFrames, lngPerSecond, audioFormat,
                false, "YV12", AVIstruct.string2int("YV12"));

        if (((iWidth | iHeight) & 1) != 0) {
            closeSilentlyDueToError();
            throw new IllegalArgumentException("Dimensions must be divisible by 2");
        }

        _iFrameYByteSize = iWidth * iHeight;
        _iFrameCByteSize = iWidth * iHeight / 4;

        _iFrameByteSize = _iFrameYByteSize + _iFrameCByteSize * 2;
    }

    public void write(@Nonnull byte[] abY, @Nonnull byte[] abCb, @Nonnull byte[] abCr)
            throws IOException
    {

        if (abY.length < _iFrameYByteSize)
            throw new IllegalArgumentException("Y data wrong size.");
        if (abCb.length < _iFrameCByteSize)
            throw new IllegalArgumentException("Cb data wrong size.");
        if (abCr.length < _iFrameCByteSize)
            throw new IllegalArgumentException("Cr data wrong size.");

        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameByteSize)
            _abWriteBuffer = new byte[_iFrameByteSize];

        System.arraycopy(abY, 0, _abWriteBuffer, 0, _iFrameYByteSize);
        System.arraycopy(abCr, 0, _abWriteBuffer, _iFrameYByteSize, _iFrameCByteSize);
        System.arraycopy(abCb, 0, _abWriteBuffer, _iFrameYByteSize+_iFrameCByteSize, _iFrameCByteSize);

        writeFrameChunk(_abWriteBuffer, 0, _iFrameByteSize);
    }

    @Override
    public void writeBlankFrame() throws IOException {
        if (_abWriteBuffer == null || _abWriteBuffer.length < _iFrameByteSize)
            _abWriteBuffer = new byte[_iFrameByteSize];

        Arrays.fill(_abWriteBuffer, 0, _iFrameYByteSize, (byte)0);
        Arrays.fill(_abWriteBuffer, _iFrameYByteSize, _iFrameYByteSize + _iFrameCByteSize*2, (byte)128);

        writeFrameChunk(_abWriteBuffer, 0, _iFrameByteSize);
    }

}
