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

package jpsxdec.plugins.psx.video.encode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes bits to an OutputStream, 16 bits at a time,
 * in little-endian or big-endian order.
 */
public class BitStreamWriter {

    private OutputStream m_os;
    private int m_iNextWrite = 0;
    private int m_iIndex;
    private boolean m_blnIsBigEndian = true;

    public BitStreamWriter(OutputStream os) {
        super();
        m_os = os;
    }

    /** Write a string of bits. */
    public void write(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '0') {
                write(false);
            } else if (c == '1') {
                write(true);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /** Write one bit */
    public void write(boolean blnBit) throws IOException {
        m_iNextWrite <<= 1;
        if (blnBit) {
            m_iNextWrite |= 1;
        }
        m_iIndex++;
        if (m_iIndex == 16) {
            write();
        }
    }

    public void write(long lngValue, int iBits) throws IOException {
        if (iBits == 0) throw new IllegalArgumentException();
        for (int iMask = 1 << (iBits-1); iMask > 0; iMask >>= 1) {
            write((lngValue & iMask) > 0);
        }
    }

    /** Closes the underlying stream after flushing the remaining bits. */
    public void close() throws IOException {
        flush();
        m_os.close();
    }

    /** If there are bits remaining to write, writes them, filling
     *  the remaining bits of the word with zeros. */
    void flush() throws IOException {
        if (m_iIndex != 0) {
            m_iNextWrite <<= 16 - m_iIndex;
            write();
        }
    }

    /** Write the buffered 16 bits as big-endian or little-endian. */
    private void write() throws IOException {
        if (m_blnIsBigEndian) {
            m_os.write((m_iNextWrite >>> 8) & 255);
            m_os.write(m_iNextWrite & 255);
        } else {
            m_os.write(m_iNextWrite & 255);
            m_os.write((m_iNextWrite >>> 8) & 255);
        }
        m_iIndex = 0;
        m_iNextWrite = 0;
    }

    public void setBigEndian(boolean bln) {
        m_blnIsBigEndian = bln;
    }

    public void setLittleEndian(boolean bln) {
        m_blnIsBigEndian = !bln;
    }

    public void writeInt16LE(int i) throws IOException {
        write(i, 16);
    }

    public void write(byte[] abCameraData) throws IOException {
        for (byte b : abCameraData) {
            write(b, 8);
        }
    }
}
