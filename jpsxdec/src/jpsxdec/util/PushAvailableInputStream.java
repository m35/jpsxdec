/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2020  Michael Sabin
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
import javax.annotation.Nonnull;

/***
 * Demuxed push input stream that depends entirely on the accuracy of
 * the provided {@link InputStream#available()} bytes.
 * Also that reading and skipping complete entirely in one call.
 * Always check {@link #available()} before reading data.
 * Reading more bytes than are currently available in the stream will
 * throw a {@link IllegalStateException}.
 * @param <META> A meta object that will be associated with each input stream.
 */
public class PushAvailableInputStream<META> extends InputStream {

    private static class Pair<T> {
        @Nonnull
        private final InputStream is;
        @Nonnull
        private final T meta;

        public Pair(@Nonnull InputStream is, @Nonnull T meta) {
            this.is = is;
            this.meta = meta;
        }
    }

    private final Queue<Pair<META>> _pieces = new ArrayDeque<Pair<META>>();
    /** The amount of available bytes beyond the current stream. */
    private int _iPendingAvailable = 0;

    public void addStream(@Nonnull InputStream is, @Nonnull META meta) throws IOException {
        // remove the current head if it is empty, and skip any additional empty pieces
        while (!_pieces.isEmpty() && _pieces.peek().is.available() == 0)
            _pieces.remove();

        if (!_pieces.isEmpty()) {
            // if the queue is empty, don't add to the pending available
            // its available count will be in the stream itself
            _iPendingAvailable += is.available();
        }
        _pieces.add(new Pair<META>(is, meta));
    }

    @Override
    public int available() throws IOException {
        return _iPendingAvailable +
               (_pieces.isEmpty() ? 0 : _pieces.peek().is.available());
    }

    private final byte[] _abRead1 = new byte[1];

    @Override
    public int read() throws IOException {
        read(_abRead1, 0, 1);
        return _abRead1[0] & 0xff;
    }

    @Override
    public int read(@Nonnull byte[] abBuff, int iOffsetInBuff, int iBytesToRead) throws IOException {
        int iBytesRemaining = iBytesToRead;

        while (true) {
            if (_pieces.isEmpty())
                throw new IllegalStateException("First check if anything is available before reading");
            int iAvailable = _pieces.peek().is.available();
            if (iBytesRemaining > iAvailable) {
                _pieces.peek().is.read(abBuff, iOffsetInBuff, iAvailable);
                iBytesRemaining -= iAvailable;
                iOffsetInBuff += iAvailable;
                moveToNextPiece();
            } else {
                if (iBytesRemaining == 1)
                    abBuff[iOffsetInBuff] = (byte)_pieces.peek().is.read();
                else if (iBytesRemaining > 0)
                    _pieces.peek().is.read(abBuff, iOffsetInBuff, iBytesRemaining);
                break;
            }
        }

        return iBytesToRead;
    }

    @Override
    public long skip(long lngBytesToSkip) throws IOException {
        long lngBytesRemaining = lngBytesToSkip;

        while (true) {
            if (_pieces.isEmpty())
                throw new IllegalStateException("First check if anything is available before reading");
            int iAvailable = _pieces.peek().is.available();
            if (lngBytesRemaining > iAvailable) {
                lngBytesRemaining -= iAvailable;
                moveToNextPiece();
            } else {
                if (lngBytesRemaining > 0)
                    _pieces.peek().is.skip(lngBytesRemaining);
                break;
            }
        }

        return lngBytesToSkip;
    }

    private void moveToNextPiece() throws IOException {
        _pieces.remove();
        // technically if the rest of the streams in the queue are of size 0
        // it would be a valid case. but in practice we would never
        // want that to be the case, so also fail then
        if (_pieces.isEmpty())
            throw new IllegalStateException("First check if anything is available before reading");
        _iPendingAvailable -= _pieces.peek().is.available();
    }

    public @Nonnull META getCurrentMeta() {
        if (_pieces.isEmpty())
            throw new IllegalStateException("There is no available stream + meta data");
        return _pieces.peek().meta;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

}
