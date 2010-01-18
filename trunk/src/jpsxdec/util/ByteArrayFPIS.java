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

package jpsxdec.util;

import java.io.ByteArrayInputStream;

/** Custom ByteArrayInputStream with file pointer 
 *  tracking and quick sub-stream generation. */
public class ByteArrayFPIS extends ByteArrayInputStream implements IGetFilePointer {

    private long m_lngFP = 0;
    private final int m_iBufStart;
    
    public ByteArrayFPIS(byte[] buf, int offset, int length) {
        super(buf, offset, length);
        m_iBufStart = super.pos;
    }

    public ByteArrayFPIS(byte[] buf) {
        super(buf);
        m_iBufStart = super.pos;
    }
    
    public ByteArrayFPIS(byte[] buf, int offset, int length, long filePos) {
        super(buf, offset, length);
        m_lngFP = filePos;
        m_iBufStart = super.pos;
    }

    public ByteArrayFPIS(byte[] buf, long filePos) {
        super(buf);
        m_lngFP = filePos;
        m_iBufStart = super.pos;
    }

    public ByteArrayFPIS(ByteArrayFPIS bafp, int offset, int length, long filePos) {
        super(bafp.buf, bafp.pos + offset, length);
        m_lngFP = filePos;
        m_iBufStart = super.pos;
    }
    
    public ByteArrayFPIS(ByteArrayFPIS bafp, int offset, int length) {
        super(bafp.buf, bafp.pos + offset, length);
        m_lngFP = bafp.m_lngFP;
        m_iBufStart = super.pos;
    }

    public ByteArrayFPIS(ByteArrayFPIS bafp) {
        this(bafp, 0, bafp.size());
    }
    
    public long getFilePointer() {
        return m_lngFP + super.pos;
    }

    public int getOffset() {
        return super.pos - m_iBufStart;
    }
    
    public ByteArrayFPIS copy() {
        return new ByteArrayFPIS(super.buf, super.pos, super.count, m_lngFP);
    }
    
    public int size() {
        return super.count - super.pos;
    }

    public String toString() {
        return "FP:" + getFilePointer() + " BufOfs:" + getOffset() + " Pos:" + super.pos;
    }

}
