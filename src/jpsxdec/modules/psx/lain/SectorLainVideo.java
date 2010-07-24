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

package jpsxdec.modules.psx.lain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.psx.str.SectorSTR;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


// FIXME: this is dangerously close to FF7 video sectors
// checking for this type of sector first would be one solution to prevent false-positives
public class SectorLainVideo extends SectorSTR {

    private byte _bQuantizationScaleLumin;
    private byte _bQuantizationScaleChrom;

    public SectorLainVideo(CdSector cdSector) throws NotThisTypeException {
        super(cdSector); // will call overridden readHeader() method
    }

    @Override
    protected void readHeader(ByteArrayInputStream inStream) throws NotThisTypeException, IOException {
        _lngMagic = IO.readUInt32LE(inStream);
        if (_lngMagic != VIDEO_CHUNK_MAGIC)
            throw new NotThisTypeException();

        _iChunkNumber = IO.readSInt16LE(inStream);
        if (_iChunkNumber < 0)
            throw new NotThisTypeException();
        _iChunksInThisFrame = IO.readSInt16LE(inStream);
        if (_iChunksInThisFrame < 1)
            throw new NotThisTypeException();
        _iFrameNumber = IO.readSInt32LE(inStream);
        if (_iFrameNumber < 1)
            throw new NotThisTypeException();

        _lngUsedDemuxedSize = IO.readSInt32LE(inStream);
        if (_lngUsedDemuxedSize < 0)
            throw new NotThisTypeException();

        _iWidth = IO.readSInt16LE(inStream);
        if (_iWidth != 320)
            throw new NotThisTypeException();
        _iHeight = IO.readSInt16LE(inStream);
        if (_iHeight != 240)
            throw new NotThisTypeException();

        _bQuantizationScaleLumin = IO.readSInt8(inStream);
        if (_bQuantizationScaleLumin < 0)
            throw new NotThisTypeException();
        _bQuantizationScaleChrom = IO.readSInt8(inStream);
        if (_bQuantizationScaleChrom < 0)
            throw new NotThisTypeException();

        _iMagic3800 = IO.readUInt16LE(inStream);
        if (_iMagic3800 != 0x3800 && _iMagic3800 != 0x0000 && _iMagic3800 != _iFrameNumber)
            throw new NotThisTypeException();

        _iRunLengthCodeCount = IO.readUInt16LE(inStream);

        _iVersion = IO.readUInt16LE(inStream);
        if (_iVersion != 0)
            throw new NotThisTypeException();

        _lngFourZeros = IO.readUInt32LE(inStream);
        if (_lngFourZeros != 0)
            throw new NotThisTypeException();
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d ver:%d " +
            "{demux frame size=%d rlc=%d 3800=%04x qscaleL=%d qscaleC=%d 4*00=%08x}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iVersion,
            _lngUsedDemuxedSize,
            _iRunLengthCodeCount,
            _iMagic3800,
            _bQuantizationScaleLumin,
            _bQuantizationScaleChrom,
            _lngFourZeros
            );
    }

    @Override
    public JPSXModule getSourceModule() {
        return JPSXModuleLain.getModule();
    }

    @Override
    public String getTypeName() {
        return "Lain Video";
    }

    public int replaceFrameData(CDFileSectorReader cd,
                                byte[] abDemuxData, int iDemuxOfs,
                                int iLuminQscale, int iChromQscale,
                                int iMdecCodeCount)
            throws IOException
    {
        byte[] abSectUserData = getCDSector().getCdUserDataCopy();

        // no need to write demux size because it won't be any different
        // as it is just the total number of bytes of demuxed data available
        // among all the frame sectors
        abSectUserData[20] = (byte)iLuminQscale;
        abSectUserData[21] = (byte)iChromQscale;
        IO.writeInt16LE(abSectUserData, 24, (short)iMdecCodeCount);

        int iBytesToCopy = getIdentifiedUserDataSize();
        if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
            iBytesToCopy = abDemuxData.length - iDemuxOfs;

        // save the 0x3800/last movie frame number if it's the first chunk
        boolean blnIsFirstChunk = IO.readSInt16LE(abSectUserData, 4) == 0;
        short i3800orLastFrameNum=0;
        if (blnIsFirstChunk)
            i3800orLastFrameNum = IO.readSInt16LE(abSectUserData, 32+2);

        // bytes to copy might be 0, which is ok because we
        // still need to write the updated headers
        System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, 32, iBytesToCopy);

        // resore the 0x3800/last movie frame number
        if (blnIsFirstChunk)
            IO.writeInt16LE(abSectUserData, 32+2, i3800orLastFrameNum);

        cd.writeSector(getSectorNumber(), abSectUserData);

        return iBytesToCopy;
    }
}
