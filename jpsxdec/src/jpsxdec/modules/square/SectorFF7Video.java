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

package jpsxdec.modules.square;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.strvideo.GenericStrVideoSector;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.IO;


/** Represents an FF7 video sector.
 * FF7 frames start with 40 bytes of unknown data.
 * These 40 bytes could be managed by the sector, or by the bitstream.
 * The game seems to let the bitstream manage the 40 bytes, but
 * I've opted to let the sector manage them. Those 40 bytes wouldn't mean
 * anything when frames are extracted by bitstreams, and would introduce
 * a whole new type of bitstream that has 40 unknown bytes.
 * Also those bytes shouldn't be touched when replacing frames. */
public class SectorFF7Video extends SectorAbstractVideo {

    public static final int FRAME_SECTOR_HEADER_SIZE = 32;

    // Magic is normal STR = 0x80010160
    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int  _iWidth;               //  16   [2 bytes]
    private int  _iHeight;              //  18   [2 bytes]
    private long _lngUnknown8bytes;     //  20   [8 bytes]
    // 4 zeroes                         //  28   [4 bytes]
    //   32 TOTAL
    // First chunk has 40 bytes of unknown data (camera data?)

    @Override
    public int getVideoSectorHeaderSize() { return _iUserDataStart; }

    private int _iUserDataStart;

    // .. Constructor .....................................................

    public SectorFF7Video(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;

        // SubMode flags "--2T-A--" should never be set
        // at least 1 movie doesn't have audio, video, or data flags set
        if (subModeExistsAndMaskDoesNotEqual(SubMode.MASK_FORM | SubMode.MASK_TRIGGER | SubMode.MASK_AUDIO, 0))
            return;

        if (_header.lngMagic != GenericStrVideoSector.STANDARD_STR_VIDEO_SECTOR_MAGIC) return;
        if (!_header.hasStandardChunkNumber()) return;
        if (_header.iChunksInThisFrame < 6 || _header.iChunksInThisFrame > 10) return;
        if (!_header.hasStandardFrameNumber()) return;
        if (_header.iUsedDemuxedSize < 1) return; // used demux size includes the 40 bytes of unknown data

        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 1) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 1) return;

        _lngUnknown8bytes = cdSector.readSInt64BE(20);

        int iFourZeros = cdSector.readSInt32LE(28);
        if (iFourZeros != 0) return;

        // check for 40 bytes of unknown data in chunk 0
        if (_header.iChunkNumber == 0) {
            if (cdSector.readUInt16LE(FRAME_SECTOR_HEADER_SIZE+2) == 0x3800)
                return;

            if (cdSector.readUInt16LE(FRAME_SECTOR_HEADER_SIZE+40+2) != 0x3800)
                return;

            _iUserDataStart = FRAME_SECTOR_HEADER_SIZE + 40;
        } else {
            _iUserDataStart = FRAME_SECTOR_HEADER_SIZE;
        }

        // the header is so vague it's easy to have false-positives
        // so you can't rely entirely on this sector identification
        setProbability(50);
    }
    // .. Public functions .................................................

    @Override
    public String toString() {
        String sRet = String.format("%s %s frame:%d chunk:%d/%d %dx%d {used demux=%d unknown=%016x}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _header.iUsedDemuxedSize,
            _lngUnknown8bytes
            );

        if (_header.iChunkNumber == 0) {
            return sRet + " + Unknown 40 bytes";
        } else {
            return sRet;
        }
    }

    @Override
    public @Nonnull String getTypeName() {
        return "FF7 Video";
    }

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
    {
        // there isn't much to change in FF7 sector headers
        // all sectors need the additional 40 unknown bytes in the demux size
        IO.writeInt32LE(abCurrentVidSectorHeader, 12, newFrame.calculateUsedBytesRoundUp4() + 40);
    }

    @Override
    public int getChunkNumber() {
        return _header.iChunkNumber;
    }

    @Override
    public int getChunksInFrame() {
        return _header.iChunksInThisFrame;
    }

    @Override
    public int getHeaderFrameNumber() {
        return _header.iFrameNumber;
    }

    @Override
    public int getHeight() {
        return _iHeight;
    }

    @Override
    public int getWidth() {
        return _iWidth;
    }

}


