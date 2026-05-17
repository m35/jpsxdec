/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2026  Michael Sabin
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
import javax.annotation.Nonnull;

/**
 * Used for writing bits to the stream, and escaping 0xff as 0xff00.
 */
class JpegBitOutputStream  {

    private int _iBufferedBitCount;
    private int _iNextBitsToWrite;
    @Nonnull
    private final OutputStream innerStream;

    private boolean _blnFinished = false;

    /**
     * After 8 bits, write the byte.
     */
    private static final int BITS_PER_WRITE8 = 8;

    public JpegBitOutputStream(@Nonnull OutputStream os) {
        innerStream = os;
    }

    public void writeBits(@Nonnull String sBitString) throws IOException {
        int iBitValue = Integer.parseInt(sBitString, 2);
        int iBitLength = sBitString.length();
        writeBits(iBitValue, iBitLength);
    }
    public void writeBits(int iValue, int iCountOfBitsInValue) throws IOException {
        if (_blnFinished)
            throw new IllegalStateException();
        assert iCountOfBitsInValue != 0;
        assert (iValue & ~((1 << iCountOfBitsInValue) - 1)) == 0; // bits outside of the count should be 1

        //Shift existing bits up and append the new bits
        _iNextBitsToWrite <<= iCountOfBitsInValue;
        _iNextBitsToWrite |= iValue;
        _iBufferedBitCount += iCountOfBitsInValue;

        while (_iBufferedBitCount >= BITS_PER_WRITE8) {
            int iTop8bits = (_iNextBitsToWrite >> (_iBufferedBitCount - BITS_PER_WRITE8)) & 0xff;
            writeEscaped8bits(iTop8bits);
            _iBufferedBitCount -= BITS_PER_WRITE8;
        }
    }

    /** If there are bits remaining to write, writes them, filling
     *  the remaining bits with zeros. */
    public void flush() throws IOException {
        if (_iBufferedBitCount != 0) {
            // Shift the remaining bits to the top of the byte
            _iNextBitsToWrite <<= BITS_PER_WRITE8 - _iBufferedBitCount;
            writeEscaped8bits(_iNextBitsToWrite);
        }
        _blnFinished = true;
    }


    /** Encode 0xff as 0xff00 */
    public void writeEscaped8bits(int b) throws IOException {
        innerStream.write(b);
        if ((b & 0xff) == 0xff) {
            innerStream.write(0);
        }
    }

}
