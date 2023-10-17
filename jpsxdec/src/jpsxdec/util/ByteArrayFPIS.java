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

package jpsxdec.util;

import java.io.ByteArrayInputStream;
import javax.annotation.Nonnull;

/** Custom ByteArrayInputStream with file pointer
 *  tracking and quick sub-stream generation. */
public class ByteArrayFPIS extends ByteArrayInputStream {

    private long _lngFP = 0;
    private final int _iBufStart;

    public ByteArrayFPIS(@Nonnull byte[] buf, int offset, int length) {
        super(buf, offset, length);
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull byte[] buf) {
        super(buf);
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull byte[] buf, int offset, int length, long filePos) {
        super(buf, offset, length);
        _lngFP = filePos;
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull byte[] buf, long filePos) {
        super(buf);
        _lngFP = filePos;
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull ByteArrayFPIS bafp, int offset, int length, long filePos) {
        super(bafp.buf, bafp.pos + offset, length);
        _lngFP = filePos;
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull ByteArrayFPIS bafp, int offset, int length) {
        super(bafp.buf, bafp.pos + offset, length);
        _lngFP = bafp._lngFP;
        _iBufStart = super.pos;
    }

    public ByteArrayFPIS(@Nonnull ByteArrayFPIS bafp) {
        super(bafp.buf, bafp.pos, bafp.count - bafp.pos);
        _lngFP = bafp._lngFP;
        _iBufStart = bafp._iBufStart;
    }

    /** Returns the position in the original file that the stream is pointing to. */
    public long getFilePointer() {
        return _lngFP + super.pos - _iBufStart;
    }

    /** Returns the number of bytes that have been read from the stream. */
    public int getOffset() {
        return super.pos - _iBufStart;
    }

    /** The original size of the stream. */
    public int getStreamByteSize() {
        return super.count - _iBufStart;
    }

    public @Nonnull ByteArrayFPIS copy() {
        return new ByteArrayFPIS(this);
    }

    @Override
    public String toString() {
        return String.format("FP:%d Stream:%d/%d Buf:%d/%d",
                             getFilePointer(),
                             getOffset(), getStreamByteSize(),
                             super.pos, super.buf.length);
    }

}
