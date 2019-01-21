/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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

package jpsxdec.psxvideo.bitstreams;

import java.io.ByteArrayOutputStream;
import javax.annotation.Nonnull;

/** Rather sloppy bit writer.
 *  Writes 16 bits at a time to an internal buffer in little-endian or big-endian order. */
public class BitStreamWriter {

    private final ByteArrayOutputStream _buffer = new ByteArrayOutputStream();
    private int _iNextWordWrite = 0;
    private int _iBitIndex = 0;
    private boolean _blnIsBigEndian = true;

    /** Write a string of bits. 
     * @param s  String consisting of '1' or '0'.
     * @throws IllegalArgumentException if string contains anything besides '1' or '0'.
     */
    public void write(@Nonnull String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '0') {
                write(false);
            } else if (c == '1') {
                write(true);
            } else {
                throw new IllegalArgumentException(s);
            }
        }
    }

    /** Write one bit */
    public void write(boolean blnBit) {
        _iNextWordWrite <<= 1;
        if (blnBit) {
            _iNextWordWrite |= 1;
        }
        _iBitIndex++;
        if (_iBitIndex == 16) {
            write();
        }
    }

    public void write(long lngValue, int iBits) {
        if (iBits < 1 || iBits > Long.SIZE)
            throw new IllegalArgumentException("Invalid bit lengh to write " + iBits);
        for (long lngMask = 1L << (iBits-1); lngMask != 0; lngMask >>= 1) {
            write((lngValue & lngMask) != 0);
        }
    }

    /** Write the buffered 16 bits as big-endian or little-endian. */
    private void write() {
        if (_blnIsBigEndian) {
            _buffer.write((_iNextWordWrite >>> 8) & 255);
            _buffer.write(_iNextWordWrite & 255);
        } else {
            _buffer.write(_iNextWordWrite & 255);
            _buffer.write((_iNextWordWrite >>> 8) & 255);
        }
        _iBitIndex = 0;
        _iNextWordWrite = 0;
    }

    public void setBigEndian(boolean bln) {
        _blnIsBigEndian = bln;
    }

    public void setLittleEndian(boolean bln) {
        _blnIsBigEndian = !bln;
    }

    /** Returns the bytes generated from the written bits.
     * The stream position moves to the end of any partially complete word,
     * setting remaining bits to 0. */
    public @Nonnull byte[] toByteArray() {
        // flush
        if (_iBitIndex != 0) {
            _iNextWordWrite <<= 16 - _iBitIndex;
            write();
        }
        return _buffer.toByteArray();
    }

    /** Resets the writer to as if it was just created. */
    public void reset() {
        _buffer.reset();
        _iNextWordWrite = 0;
        _iBitIndex = 0;
    }

    /** Returns the start position of the current word being written.
     * This is pretty size to the size of the written bits, in bytes.
     * To get the actual size in bytes, complete the stream by calling
     * {@link #toByteArray()}  */
    public int getCurrentWordPosition() {
        return _buffer.size();
    }

    /** Exposes underlying buffer in case bytes need to be written.
     * @throws IllegalStateException if current write position is not 
     *                               on a word boundary. */
    public @Nonnull ByteArrayOutputStream exposeBuffer() {
        if (_iBitIndex != 0)
            throw new IllegalStateException();
        return _buffer;
    }
}
