/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.cdreaders;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/** Abstraction for reading bytes that may all reside in RAM,
 * or may be backed by a file that is read in buffered chunks. */
public class BufferedBytesReader implements Closeable {

    @Nonnull
    private final File _sourceFile;
    @CheckForNull
    private RandomAccessFile _randomAccessFile;
    private final int _iFileLength;

    private int _iBufferSize = 0;

    @CheckForNull
    private byte[] _abReadBuffer = null;
    private int _iFileOffsetOfBufferStart = Integer.MIN_VALUE;
    private int _iSeekOfs = Integer.MIN_VALUE;

    /** Make this buffer backed by a file and read using a buffer of the given length. */
    public BufferedBytesReader(@Nonnull File sourceFile, @Nonnull RandomAccessFile raf, int iLength) {
        _sourceFile = sourceFile;
        _randomAccessFile = raf;
        _iFileLength = iLength;
    }

    /** Crate a buffer that is not backed by a file but instead exists entirely in RAM. */
    public BufferedBytesReader(@Nonnull File sourceFile, @Nonnull byte[] abWholeFile) {
        _sourceFile = sourceFile;
        _randomAccessFile = null;
        _iFileLength = abWholeFile.length;

        _abReadBuffer = abWholeFile;
        _iFileOffsetOfBufferStart = 0;
    }

    /** The buffer will be updated with the given size the next time it is reallocated. */
    public void setBufferSize(int iSize) {
        _iBufferSize = iSize;
    }

    /** @see RandomAccessFile#length()  */
    public int length() {
        // I suppose it's somehow possible that the file size changes after construction
        // but the only proper response to that is blowing up
        return _iFileLength;
    }

    /** Reads and buffers AT LEAST the number of bytes requested (it may be more).
     * @throws IllegalArgumentException if the requested reading extends beyond the end of the file. */
    public void readAndBuffer(int iPosition, int iSize) throws CdException.Read {
        if (iPosition < 0 || iSize < 1 || iPosition + iSize > _iFileLength)
            throw new IllegalArgumentException("Out of bounds");

        if (_randomAccessFile != null) {
            if (_abReadBuffer == null || iPosition < _iFileOffsetOfBufferStart || iPosition + iSize > _iFileOffsetOfBufferStart + _abReadBuffer.length) {

                // If requested size is more than the normal buffer size
                int iReadSize = Math.max(_iBufferSize, iSize);
                // Don't buffer beyond the end of the file
                int iBuffSize = Math.min(iReadSize, _iFileLength - iPosition);

                byte[] abBulkReadCache;
                try {
                    _randomAccessFile.seek(iPosition);
                    abBulkReadCache = IO.readByteArray(_randomAccessFile, iBuffSize);
                } catch (IOException ex) {
                    throw new CdException.Read(_sourceFile, ex);
                }

                _iFileOffsetOfBufferStart = iPosition;
                _abReadBuffer = abBulkReadCache;
            }
        }

        _iSeekOfs = iPosition - _iFileOffsetOfBufferStart;
    }

    @Nonnull
    public File getSourceFile() {
        return _sourceFile;
    }

    /** Treat the buffer as read-only, and the contents are guaranteed never to change. */
    public @Nonnull byte[] getBuffer() {
        if (_abReadBuffer == null)
            throw new IllegalStateException("readAndBuffer should be called first");
        return _abReadBuffer;
    }

    public int getBufferReadOffset() {
        if (_iSeekOfs < 0)
            throw new IllegalStateException("readAndBuffer should be called first");
        return _iSeekOfs;
    }

    /** @throws UnsupportedOperationException if the reader is not backed by a file. */
    void reopenForWriting() throws CdFileSectorReader.CdReopenException {
        if (_randomAccessFile == null)
            throw new UnsupportedOperationException();

        _abReadBuffer = null;
        try {
            _randomAccessFile.close();
            _randomAccessFile = new RandomAccessFile(_sourceFile, "rw");
        } catch (IOException ex) {
            throw new CdFileSectorReader.CdReopenException(_sourceFile, ex);
        }
    }

    /** @throws UnsupportedOperationException if the reader is not backed by a file. */
    void write(int iPosition, @Nonnull byte[] abBytesToWrite) throws CdFileSectorReader.CdWriteException {
        if (_randomAccessFile == null)
            throw new UnsupportedOperationException();

        _abReadBuffer = null;
        try {
            _randomAccessFile.seek(iPosition);
            _randomAccessFile.write(abBytesToWrite);
        } catch (IOException ex) {
            throw new CdFileSectorReader.CdWriteException(_sourceFile, ex);
        }
    }

    /** Free the buffer and close the backed file if there is one. */
    @Override
    public void close() throws IOException {
        _abReadBuffer = null;
        if (_randomAccessFile != null)
            _randomAccessFile.close();
    }

}
