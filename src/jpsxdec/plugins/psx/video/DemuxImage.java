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

    private final int _iWidth, _iHeight, _iSourceFrame;
    private final byte[] _abData;

    public DemuxImage(int iWidth, int iHeight, int iFrame, File oSource) throws IOException {
        _iWidth = iWidth;
        _iHeight= iHeight;
        _iSourceFrame = iFrame;
        _abData = IO.readFile(oSource);
    }
    public DemuxImage(int iWidth, int iHeight, File oSource) throws IOException {
        this(iWidth, iHeight, -1, oSource);
    }

    public DemuxImage(int iWidth, int iHeight, int iFrame, byte[] abDemuxBuffer) {
        _iWidth = iWidth;
        _iHeight= iHeight;
        _iSourceFrame = iFrame;

        _abData = abDemuxBuffer;
    }


    public DemuxImage(int iWidth, int iHeight, int iFrame, InputStream oIS, int iExpectedDataSize) throws IOException {
        _iWidth = iWidth;
        _iHeight= iHeight;
        _iSourceFrame = iFrame;

        // copy as much from the stream as possible
        _abData = new byte[iExpectedDataSize];
        int pos = oIS.read(_abData);
        while (pos < iExpectedDataSize) {
            int i = oIS.read(_abData, pos, iExpectedDataSize - pos);
            if (i < 0) break;
            pos += i;
        }
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getActualWidth() {
        return (_iWidth + 15) & (~15);
    }

    public int getActualHeight() {
        return (_iHeight + 15) & (~15);
    }

    /** Size of the underlying demux data (currently equal to the array size). */
    public int getBufferSize() {
        return _abData.length;
    }

    public int getFrameNumber() {
        return _iSourceFrame;
    }

    public byte[] getData() {
        return _abData;
    }

}
