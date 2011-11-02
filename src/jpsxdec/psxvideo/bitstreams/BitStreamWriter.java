/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import java.io.IOException;
import java.io.OutputStream;

/** Rather sloppy bit writer to an output stream.
 *  Writes bits to an OutputStream, 16 bits at a time,
 *  in little-endian or big-endian order. */
public class BitStreamWriter {

    private OutputStream _os;
    private int _iNextWrite = 0;
    protected int _iIndex;
    private boolean _blnIsBigEndian = true;

    public BitStreamWriter(OutputStream os) {
        _os = os;
    }

    /** Write a string of bits. 
     * @param s  String consisting of '1' or '0'.
     * @throws IllegalArgumentException if string contains anything besides '1' or '0'.
     */
    public void write(String s) throws IOException {
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
    public void write(boolean blnBit) throws IOException {
        _iNextWrite <<= 1;
        if (blnBit) {
            _iNextWrite |= 1;
        }
        _iIndex++;
        if (_iIndex == 16) {
            write();
        }
    }

    public void write(long lngValue, int iBits) throws IOException {
        if (iBits == 0) throw new IllegalArgumentException(iBits + " == " + 0);
        for (long lngMask = 1L << (iBits-1); lngMask != 0; lngMask >>= 1) {
            write((lngValue & lngMask) != 0);
        }
    }

    /** Closes the underlying stream after flushing the remaining bits. */
    public void close() throws IOException {
        flush();
        _os.close();
    }

    /** If there are bits remaining to write, writes them, filling
     *  the remaining bits of the word with zeros. */
    public void flush() throws IOException {
        if (_iIndex != 0) {
            _iNextWrite <<= 16 - _iIndex;
            write();
        }
    }

    /** Write the buffered 16 bits as big-endian or little-endian. */
    private void write() throws IOException {
        if (_blnIsBigEndian) {
            _os.write((_iNextWrite >>> 8) & 255);
            _os.write(_iNextWrite & 255);
        } else {
            _os.write(_iNextWrite & 255);
            _os.write((_iNextWrite >>> 8) & 255);
        }
        _iIndex = 0;
        _iNextWrite = 0;
    }

    public void setBigEndian(boolean bln) {
        _blnIsBigEndian = bln;
    }

    public void setLittleEndian(boolean bln) {
        _blnIsBigEndian = !bln;
    }

    public void write(byte[] ab) throws IOException {
        for (byte b : ab) {
            write(b, 8);
        }
    }
}
