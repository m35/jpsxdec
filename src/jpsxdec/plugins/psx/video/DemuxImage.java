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

package jpsxdec.plugins.psx.video;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.Misc;
import jpsxdec.util.IO;


public class DemuxImage {

    private final int m_iWidth, m_iHeight, m_iSourceFrame;
    private final byte[] m_abData;

    public DemuxImage(int iWidth, int iHeight, int iFrame, File oSource) throws IOException {
        m_iWidth = iWidth;
        m_iHeight= iHeight;
        m_iSourceFrame = iFrame;
        m_abData = IO.readFile(oSource);
    }
    public DemuxImage(int iWidth, int iHeight, File oSource) throws IOException {
        this(iWidth, iHeight, -1, oSource);
    }

    public DemuxImage(int iWidth, int iHeight, int iFrame, byte[] oSource) {
        this(iWidth, iHeight, iFrame, oSource, 0, oSource.length);
    }

    public DemuxImage(int iWidth, int iHeight, int iFrame, byte[] oSource, int iStart, int iLen) {
        m_iWidth = iWidth;
        m_iHeight= iHeight;
        m_iSourceFrame = iFrame;
        m_abData = Misc.copyOfRange(oSource, iStart, iLen);
    }

    public DemuxImage(int iWidth, int iHeight, int iFrame, InputStream oIS, int iExpectedDataSize) throws IOException {
        m_iWidth = iWidth;
        m_iHeight= iHeight;
        m_iSourceFrame = iFrame;

        // copy as much from the stream as possible
        m_abData = new byte[iExpectedDataSize];
        int pos = oIS.read(m_abData);
        while (pos < iExpectedDataSize) {
            int i = oIS.read(m_abData, pos, iExpectedDataSize - pos);
            if (i < 0) break;
            pos += i;
        }
    }

    public int getWidth() {
        return m_iWidth;
    }

    public int getHeight() {
        return m_iHeight;
    }

    public int getActualWidth() {
        return (m_iWidth + 15) & (~15);
    }

    public int getActualHeight() {
        return (m_iHeight + 15) & (~15);
    }

    /** Size of the underlying demux data (currently equal to the array size). */
    public int getBufferSize() {
        return m_abData.length;
    }

    public int getFrameNumber() {
        return m_iSourceFrame;
    }

    public byte[] getData() {
        return m_abData;
    }

}
