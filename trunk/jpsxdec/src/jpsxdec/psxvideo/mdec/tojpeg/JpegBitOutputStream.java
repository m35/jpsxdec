/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2016  Michael Sabin
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

package jpsxdec.psxvideo.mdec.tojpeg;

import java.io.IOException;
import java.io.OutputStream;


class JpegBitOutputStream extends OutputStream {

    private int _iIndex;
    private int _iNextWrite;
    public OutputStream innerStream;

    private static final int BITS_PER_WRITE = 8;

    public void write(int iValue, int iBits) throws IOException {
        assert iBits != 0;
        assert (iValue & ~((1 << iBits) - 1)) == 0;

        _iNextWrite <<= iBits;
        _iNextWrite |= iValue;
        _iIndex += iBits;
        while (_iIndex > BITS_PER_WRITE) {
            write((_iNextWrite >> _iIndex - BITS_PER_WRITE) & 0xff);
            _iIndex -= BITS_PER_WRITE;
        }
    }

    public void reset() {
        _iIndex = 0;
        _iNextWrite = 0;
    }

    /** If there are bits remaining to write, writes them, filling
     *  the remaining bits with zeros. */
    @Override
    public void flush() throws IOException {
        if (_iIndex != 0) {
            _iNextWrite <<= BITS_PER_WRITE - _iIndex;
            write(_iNextWrite);
        }
        reset();
    }


    /** Encode 0xff as 0xff 0x00 */
    @Override
    public void write(int b) throws IOException {
        innerStream.write(b);
        if ((b & 0xff) == 0xff)
            innerStream.write(0);
    }

}
