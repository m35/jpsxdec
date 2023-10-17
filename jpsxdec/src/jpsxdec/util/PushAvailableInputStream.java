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

package jpsxdec.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/***
 * Demuxed push input stream that depends entirely on the accuracy of
 * the provided {@link InputStream#available()} bytes.
 * Always check {@link #available()} before reading data.
 * Reading more bytes than are currently available in the stream will
 * throw a {@link IllegalStateException}.
 * @param <META> A meta object that will be associated with each input stream.
 */
public class PushAvailableInputStream<META> extends InputStream {

    private static class ISPair<T> {
        @Nonnull
        private final InputStream is;
        @Nonnull
        private final T meta;

        public ISPair(@Nonnull InputStream is, @Nonnull T meta) {
            this.is = is;
            this.meta = meta;
        }
    }

    private final Queue<ISPair<META>> _pieces = new ArrayDeque<ISPair<META>>();
    /** The amount of available bytes beyond the current stream. */
    private int _iPendingAvailable = 0;

    public void addStream(@Nonnull InputStream is, @Nonnull META meta) throws IOException {
        // don't filter empty streams because their meta may still be needed

        if (!_pieces.isEmpty()) {
            // if the queue is empty, don't add to the pending available
            // its available count will be in the stream itself
            _iPendingAvailable += is.available();
        }
        _pieces.add(new ISPair<META>(is, meta));
    }

    @Override
    public int available() throws IOException {
        return _iPendingAvailable +
               (_pieces.isEmpty() ? 0 : _pieces.peek().is.available());
    }

    /** Used to read a single byte (so I can just utilize the other read()). */
    private final byte[] _abRead1Byte = new byte[1];

    @Override
    public int read() throws IOException {
        readOrSkip(_abRead1Byte, 0, 1);
        return _abRead1Byte[0] & 0xff;
    }

    /** Will either return the number of bytes requested in the parameter,
     * or throw {@link IllegalStateException} because there isn't enough
     * bytes to read (you should have checked {@link #available()} first). */
    @Override
    public int read(@Nonnull byte[] abBuff, int iOffsetInBuff, int iBytesToRead) throws IOException {
        readOrSkip(abBuff, iOffsetInBuff, iBytesToRead);
        return iBytesToRead;
    }

    /** Will either return the number of bytes requested in the parameter,
     * or throw {@link IllegalStateException} because there isn't enough
     * bytes to skip (you should have checked {@link #available()} first). */
    @Override
    public long skip(long lngBytesToSkip) throws IOException {
        readOrSkip(null, 0, lngBytesToSkip);
        return lngBytesToSkip;
    }

    /** Common function to handle both reading and skipping since so much of the code is the same. */
    private void readOrSkip(@CheckForNull byte[] abBuffer, int iOffsetInBuff, long lngBytes) throws IOException {
        if (_pieces.isEmpty())
            throw new IllegalStateException("First check if anything is available before reading");

        long lngBytesRemaining = lngBytes;

        while (lngBytesRemaining > 0) {
            while (_pieces.peek().is.available() == 0) {
                _pieces.remove();
                if (_pieces.isEmpty())
                    throw new IllegalStateException("First check if anything is available before reading");
                _iPendingAvailable -= _pieces.peek().is.available();
            }

            int iAvailable = _pieces.peek().is.available();
            int iMaxBytesToRead = (int)Math.min(lngBytesRemaining, iAvailable); // result will always be int
            int iBytesActuallyRead;
            if (abBuffer == null)
                iBytesActuallyRead = (int)IO.skipMax(_pieces.peek().is, iMaxBytesToRead);
            else
                iBytesActuallyRead = IO.readByteArrayMax(_pieces.peek().is, abBuffer, iOffsetInBuff, iMaxBytesToRead);

            if (iMaxBytesToRead != iBytesActuallyRead)
                throw new IllegalStateException("Piece lied about the available bytes");

            iOffsetInBuff += iAvailable;
            lngBytesRemaining -= iAvailable;

            // don't remove an exhausted stream here because its meta may still be needed
        }
    }

    public @Nonnull META getCurrentMeta() {
        if (_pieces.isEmpty())
            throw new IllegalStateException("There is no available stream + meta data");
        return _pieces.peek().meta;
    }

    @Override
    public void close() throws IOException {
        for (ISPair<META> _piece : _pieces) {
            _piece.is.close();
        }
        _pieces.clear();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

}
