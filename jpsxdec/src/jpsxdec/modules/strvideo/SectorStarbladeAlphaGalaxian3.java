/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;


/** Video sector type used in Starblade Alpha and Galaxian 3. */
public class SectorStarbladeAlphaGalaxian3 extends SectorAbstractVideo {

    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int _iWidth;                //  16   [2 bytes]
    private int _iHeight;               //  18   [2 bytes]

    // Difference between used demux verses exact used demux
    private int _iExactUsedDemux;       //  20   [2 bytes]
    private int _iUsedDemuxDifference;  //  22   [2 bytes]

    private int _iSharedFrameUnknown;   //  24   [2 bytes]

    // 0x3800                           //  26   [2 bytes]
    // zeroes                           //  28   [4 bytes]
    // 32 TOTAL

    // First chunk also has:
    private int _i1stChunkUnknown;      //  32   [4 bytes]
    //   36 TOTAL

    @Override
    public int getVideoSectorHeaderSize() {
        if (_header.iChunkNumber == 0)
            return 36;
        else
            return 32;
    }

    public SectorStarbladeAlphaGalaxian3(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;

        // only if it has a sector header should we check if it reports VIDEO
        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh != null) {
            if (sh.getSubMode().mask(SubMode.MASK_VIDEO) == 0)
                return;
            if (sh.getSubMode().mask(SubMode.MASK_FORM) != 0)
                return;
        }

        if (_header.lngMagic != GenericStrVideoSector.STANDARD_STR_VIDEO_SECTOR_MAGIC) return;
        if (!_header.hasStandardChunkNumber()) return;
        if (!_header.hasStandardChunksInFrame()) return;
        if (_header.iFrameNumber < 1) return;
        if (!_header.hasStandardUsedDemuxSize()) return;
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 1) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 1) return;

        _iExactUsedDemux = cdSector.readUInt16LE(20);
        _iUsedDemuxDifference = cdSector.readSInt16LE(22);
        int iCalculatedDifference = _header.iUsedDemuxedSize - _iExactUsedDemux - 4;
        if (iCalculatedDifference != _iUsedDemuxDifference)
            return;

        _iSharedFrameUnknown = cdSector.readSInt16LE(24);
        int iMagic3800 = cdSector.readUInt16LE(26);
        if (iMagic3800 != 0x3800) return;
        int iZeroes = cdSector.readSInt32LE(28);
        if (iZeroes != 0) return;
        _i1stChunkUnknown = cdSector.readSInt32LE(32);

        setProbability(100);
    }

    // .. Public methods ...................................................

    @Override
    public @Nonnull String getTypeName() {
        return "Starblade Alpha/Galaxian 3";
    }

    @Override
    public String toString() {
        String s = String.format("%s %s frame:%d chunk:%d/%d %dx%d demux{used:%d exact:%d diff:%d} ??:%d",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth, _iHeight,
            _header.iUsedDemuxedSize,
            _iExactUsedDemux,
            _iUsedDemuxDifference,
            _iSharedFrameUnknown);
        if (_header.iChunkNumber == 0)
            return s + String.format(" 1st??:%08x", _i1stChunkUnknown);
        else
            return s;
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

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        throw new UnsupportedOperationException("Replacing Starblade Alpha video is not implemented");
    }

}

