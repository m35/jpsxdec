/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Iki;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.util.IO;


public class SectorIkiVideo extends SectorAbstractVideo {
    
    // .. Fields ..........................................................

    // Magic is normal STR = 0x80010160
    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int  _iWidth;                //  16   [2 bytes]
    private int  _iHeight;               //  18   [2 bytes]
    private int  _iRunLengthCodeCount;   //  20   [2 bytes]
    // 0x3800                            //  22   [2 bytes]
    // Width again                       //  24   [2 bytes]
    // Height again                      //  26   [2 bytes]
    private long _lngFourZeros;          //  28   [4 bytes]
    //   32 TOTAL

    @Override
    public int getVideoSectorHeaderSize() { return 32; }
    
    public SectorIkiVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;
        
        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (subModeMaskMatch(SubMode.MASK_DATA | SubMode.MASK_VIDEO, 0))
            return;

        if (_header.lngMagic != SectorStrVideo.VIDEO_SECTOR_MAGIC) return;
        if (!_header.hasStandardChunkNumber()) return;
        if (!_header.hasStandardChunksInFrame()) return;
        if (!_header.hasStandardFrameNumber()) return;
        if (!_header.hasStandardUsedDemuxSize()) return;
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 1) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 1) return;
        _iRunLengthCodeCount = cdSector.readUInt16LE(20);
        if (_iRunLengthCodeCount < 1) return;
        int iMagic3800 = cdSector.readUInt16LE(22);
        if (iMagic3800 != 0x3800) return;
        int iWidth = cdSector.readSInt16LE(24);
        if (iWidth != _iWidth) return;
        int iHeight = cdSector.readUInt16LE(26);
        if (iHeight != _iHeight) return;
        _lngFourZeros = cdSector.readUInt32LE(28);
        if (_lngFourZeros != 0) return;

        setProbability(100);
    }

    // .. Public methods ...................................................

    public @Nonnull String getTypeName() {
        return "IKI Video";
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d " +
            "{demux frame size=%d rlc=%d ??=%08x}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _header.iUsedDemuxedSize,
            _iRunLengthCodeCount,
            _lngFourZeros
            );
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
        return _iHeight;
    }

    public int getWidth() {
        return _iWidth;
    }


    public void replaceVideoSectorHeader(@Nonnull byte[] abNewDemuxData, int iNewUsedSize,
                                         int iNewMdecCodeCount, @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        BitStreamUncompressor_Iki.IkiHeader header = new BitStreamUncompressor_Iki.IkiHeader(abNewDemuxData, iNewUsedSize);
        if (!header.isValid())
            throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_IKI());
        
        if (header.getWidth() != _iWidth || header.getHeight() != _iHeight) {
            throw new LocalizedIncompatibleException(I.REPLACE_FRAME_IKI_DIMENSIONS_MISMATCH(
                                            header.getWidth(), header.getHeight(), _iWidth, _iHeight));
        }

        int iDemuxSizeForHeader = (iNewUsedSize + 3) & ~3;

        IO.writeInt32LE(abCurrentVidSectorHeader, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abCurrentVidSectorHeader, 20,
                Calc.calculateHalfCeiling32(iNewMdecCodeCount));
    }

}

