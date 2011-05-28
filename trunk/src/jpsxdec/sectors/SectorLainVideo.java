/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.sectors;

import java.io.IOException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.IO;


// FIXME: this is dangerously close to FF7 video sectors
// checking for this type of sector first would be one solution to prevent false-positives
public class SectorLainVideo extends SectorAbstractVideo {

    // Magic 0x80010160                     //  0    [4 bytes]
    private int  _iChunkNumber;             //  4    [2 bytes]
    private int  _iChunksInThisFrame;       //  6    [2 bytes]
    private int  _iFrameNumber;             //  8    [4 bytes]
    private long _lngUsedDemuxedSize;       //  12   [4 bytes]
    // Width                                //  16   [2 bytes]
    // Height                               //  18   [2 bytes]
    private byte _bQuantizationScaleLumin;  //  20   [1 byte]
    private byte _bQuantizationScaleChrom;  //  21   [1 byte]
    private int  _iMagic3800;               //  22   [2 bytes]
    private int  _iRunLengthCodeCount;      //  24   [2 bytes]
    // Version 0                            //  26   [2 bytes]
    // FourZeros                            //  28   [2 bytes]
    //   32 TOTAL

    public int getSectorHeaderSize() { return 32; }


    public SectorLainVideo(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (cdSector.hasRawSectorHeader() &&
            cdSector.subModeMask(SubMode.MASK_DATA | SubMode.MASK_VIDEO) == 0)
        {
            return;
        }

        long lngMagic = cdSector.readUInt32LE(0);
        if (lngMagic != SectorStrVideo.VIDEO_SECTOR_MAGIC) return;

        _iChunkNumber = cdSector.readSInt16LE(4);
        if (_iChunkNumber < 0) return;
        _iChunksInThisFrame = cdSector.readSInt16LE(6);
        if (_iChunksInThisFrame < 1) return;
        _iFrameNumber = cdSector.readSInt32LE(8);
        if (_iFrameNumber < 1) return;
        _lngUsedDemuxedSize = cdSector.readSInt32LE(12);
        if (_lngUsedDemuxedSize < 0) return;
        int iWidth = cdSector.readSInt16LE(16);
        if (iWidth != 320) return;
        int iHeight = cdSector.readSInt16LE(18);
        if (iHeight != 240) return;

        _bQuantizationScaleLumin = cdSector.readUserDataByte(20);
        if (_bQuantizationScaleLumin < 0) // 1 movie has 0 qscale in header
            return;
        _bQuantizationScaleChrom = cdSector.readUserDataByte(21);
        if (_bQuantizationScaleChrom < 0) // 1 movie has 0 qscale in header
            return;

        _iMagic3800 = cdSector.readUInt16LE(22);
        if (_iMagic3800 != 0x3800 && _iMagic3800 != 0x0000 && _iMagic3800 != _iFrameNumber)
            return;

        _iRunLengthCodeCount = cdSector.readUInt16LE(24);
        if (_iRunLengthCodeCount < 0) // some movies report 0 code count
            return;

        int iVersion = cdSector.readUInt16LE(26);
        if (iVersion != 0) return;
        long lngFourZeros = cdSector.readUInt32LE(28);
        if (lngFourZeros != 0) return;

        // still possible it's a FF7 header
        // TODO: figure out probability with more details
        setProbability(90);
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d " +
            "{demux frame size=%d rlc=%d 3800=%04x qscaleL=%d qscaleC=%d}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _lngUsedDemuxedSize,
            _iRunLengthCodeCount,
            _iMagic3800,
            _bQuantizationScaleLumin,
            _bQuantizationScaleChrom
            );
    }

    @Override
    public String getTypeName() {
        return "Lain Video";
    }

    /** {@inheritDoc}
     * Lain needs special handling due to its unique header. */
    @Override
    public int replaceFrameData(CdFileSectorReader cd,
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

    public int getChunkNumber() {
        return _iChunkNumber;
    }

    public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }

    public int getHeight() {
        return 240;
    }

    public int getWidth() {
        return 320;
    }

    public boolean splitAudio() {
        return (getFrameNumber() == 1 && getChunkNumber() == 0);
    }
}
