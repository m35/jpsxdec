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

package jpsxdec.cdreaders;

import jpsxdec.util.ByteArrayFPIS;

public abstract class  CdSector {

    protected final int _iSectorIndex;
    protected final long _lngFilePointer;
    protected final byte[] _abSectorBytes;
    protected final int _iByteStartOffset;


    protected CdSector(byte[] abSectorBytes, int iByteStartOffset,
                        int iSectorIndex, long lngFilePointer)
    {
        _iSectorIndex = iSectorIndex;
        _lngFilePointer = lngFilePointer;
        _abSectorBytes = abSectorBytes;
        _iByteStartOffset = iByteStartOffset;
    }

    /**
     * @return The sector index from the start of the file.
     */
    final public int getSectorNumberFromStart() {
        return _iSectorIndex;
    }

    /**
     * Returns copy of the 'user data' portion of the sector.
     */
    abstract public byte[] getCdUserDataCopy();

    abstract public void getCdUserDataCopy(int iSourcePos, byte[] abOut, int iOutPos, int iLength);

    /**
     * Returns the size of the 'user data' portion of the sector.
     */
    abstract public int getCdUserDataSize();


    /**
     * Returns the actual offset in bytes from the start of the file/CD
     * to the start of the sector userdata.
     * [implements IGetFilePointer]
     */
    abstract public long getFilePointer();

    /**
     * Returns direct reference to the underlying sector data, with raw
     * header/footer and everything it has.
     */
    abstract public byte[] getRawSectorDataCopy();

    abstract public boolean hasSectorHeader();

    abstract public byte readUserDataByte(int i);

    abstract public ByteArrayFPIS getCdUserDataStream();

    /**
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public int getChannel() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return 4 or 8.
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public int getCodingInfo_BitsPerSample() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return If coding is stereo.
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public boolean getCodingInfo_MonoStereo() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return 37800 or 18900.
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public int getCodingInfo_SampleRate() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public int getFile() {
        throw new UnsupportedOperationException();
    }


    /**
     * @return The sector number from the sector header.
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public int getHeaderSectorNumber() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public CdxaSubHeader.SubMode getSubMode() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException when the sector doesn't have a header.
     */
    public CdxaSubHeader.CodingInfo getCodingInfo() {
        throw new UnsupportedOperationException();
    }


}
