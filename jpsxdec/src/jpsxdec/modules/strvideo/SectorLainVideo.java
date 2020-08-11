/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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

package jpsxdec.modules.strvideo;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Lain;
import jpsxdec.util.IO;


public class SectorLainVideo extends SectorAbstractVideo {

    // Magic is normal STR = 0x80010160
    // Lain's used demux size is always just the total demux size
    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    // Width                                //  16   [2 bytes]
    // Height                               //  18   [2 bytes]    
    private byte _bQuantizationScaleLuma;   //  20   [1 byte]
    private byte _bQuantizationScaleChroma; //  21   [1 byte]
    private int  _iMagic3800;               //  22   [2 bytes]
    private int  _iRunLengthCodeCount;      //  24   [2 bytes]
    // Version 0                            //  26   [2 bytes]
    // FourZeros                            //  28   [2 bytes]
    //   32 TOTAL

    public int getVideoSectorHeaderSize() { return 32; }


    public SectorLainVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;

        // if it has a sector header, make sure DATA flag are set
        if (subModeExistsAndMaskDoesNotEqual(SubMode.MASK_DATA, SubMode.MASK_DATA))
            return;

        if (_header.lngMagic != SectorStrVideo.VIDEO_SECTOR_MAGIC) return;

        if (!_header.hasStandardChunkNumber()) return;
        // this detail helps to avoid matching FF7 video sectors
        if (_header.iChunksInThisFrame != 9 && _header.iChunksInThisFrame != 10) return;
        if (_header.iFrameNumber < 1) return;
        // Lain's used demux size is always just the total demux size
        // this detail helps to avoid matching FF7 video sectors
        if (_header.iUsedDemuxedSize != 18144 && _header.iUsedDemuxedSize != 20160) return;
        int iWidth = cdSector.readSInt16LE(16);
        if (iWidth != 320) return;
        int iHeight = cdSector.readSInt16LE(18);
        if (iHeight != 240) return;

        _bQuantizationScaleLuma = cdSector.readUserDataByte(20);
        if (_bQuantizationScaleLuma < 0) // 1 movie has 0 qscale in header
            return;
        _bQuantizationScaleChroma = cdSector.readUserDataByte(21);
        if (_bQuantizationScaleChroma < 0) // 1 movie has 0 qscale in header
            return;

        _iMagic3800 = cdSector.readUInt16LE(22);
        if (_iMagic3800 != 0x3800 && _iMagic3800 != 0x0000 &&
            _iMagic3800 != _header.iFrameNumber)
            return;

        _iRunLengthCodeCount = cdSector.readUInt16LE(24);
        if (_iRunLengthCodeCount < 0) // some movies report 0 code count
            return;

        int iVersion = cdSector.readUInt16LE(26);
        if (iVersion != 0) return;
        int iFourZeros = cdSector.readSInt32LE(28);
        if (iFourZeros != 0) return;

        setProbability(90);
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d " +
            "{demux size=%d rlc=%d 3800=%04x qscaleL=%d qscaleC=%d}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _header.iUsedDemuxedSize,
            _iRunLengthCodeCount,
            _iMagic3800,
            _bQuantizationScaleLuma,
            _bQuantizationScaleChroma
            );
    }

    public @Nonnull String getTypeName() {
        return "Lain Video";
    }

    /** {@inheritDoc}
     * Lain needs special handling due to its unique header. */
    public void replaceVideoSectorHeader(@Nonnull byte[] abNewDemuxData, int iNewUsedSize,
                                         int iNewMdecCodeCount, @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        BitStreamUncompressor_Lain.LainHeader header =
                new BitStreamUncompressor_Lain.LainHeader(abNewDemuxData, iNewUsedSize);
        if (!header.isValid())
            throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_LAIN());

        // no need to update the demux size because it won't be any different
        // as it is just the total number of bytes of demuxed data available
        // among all the frame sectors
        abCurrentVidSectorHeader[20] = (byte)header.getLumaQscale();
        abCurrentVidSectorHeader[21] = (byte)header.getChromaQscale();
        IO.writeInt16LE(abCurrentVidSectorHeader, 24, (short)iNewMdecCodeCount);
    }

    public int getChunkNumber() {
        return _header.iChunkNumber;
    }

    public int getChunksInFrame() {
        return _header.iChunksInThisFrame;
    }

    public int getHeaderFrameNumber() {
        return _header.iFrameNumber;
    }

    public int getHeight() {
        return 240;
    }

    public int getWidth() {
        return 320;
    }

}
