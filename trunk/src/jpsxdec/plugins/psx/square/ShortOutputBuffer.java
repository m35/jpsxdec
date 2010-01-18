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

package jpsxdec.plugins.psx.square;

import java.util.Arrays;

/** Very simple buffer that shorts can be written to. Two subclasses,
 *  BE and LE, saves the 16-bit short values as big-endian and little-endian,
 *  respectively. */
public abstract class ShortOutputBuffer {

    private byte[] _abBuf;
    private int _iPos = 0;

    /** Resets the write index to the start of the buffer. */
    public void reset(byte[] abBuf) {
        _abBuf = abBuf;
        _iPos = 0;
    }

    public byte[] getBuffer() {
        return _abBuf;
    }

    protected void write(int iByte) {
        _abBuf[_iPos] = (byte) iByte;
        _iPos++;
    }

    public abstract void write(short si);
    public abstract boolean isBigEndian();

    /** Fills the remaining unused buffer with zeros. */
    public void clearRemaining(int iEnd) {
        Arrays.fill(_abBuf, _iPos, iEnd, (byte) 0);
        _abBuf = null;
    }

    /** Shorts are written as Big Endian. */
    public static class BE extends ShortOutputBuffer {
        @Override
        public void write(short si) {
            super.write(si >> 8);
            super.write(si & 0xFF);
        }
        public boolean isBigEndian() {
            return true;
        }
    }

    /** Shorts are written as Little Endian. */
    public static class LE extends ShortOutputBuffer {
        @Override
        public void write(short si) {
            super.write(si & 0xFF);
            super.write(si >> 8);
        }
        public boolean isBigEndian() {
            return false;
        }
    }

}
