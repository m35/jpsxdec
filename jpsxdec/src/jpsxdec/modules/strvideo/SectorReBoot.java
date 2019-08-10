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
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.util.IO;


/** ReBoot video sector variant.
 * Thanks to XBrav for doing the research to make this possible. */
public class SectorReBoot extends SectorAbstractVideo {
    
    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int  _iWidth;                //  16   [2 bytes]
    private int  _iHeight;               //  18   [2 bytes]
    //  zeroes, in place of              //  20   [2 bytes]
    //    RunLengthCodeCount
    /** A number a little larger than the number of frames in the video. */
    private int  _iMoreThanFrameCount;   //  22   [2 bytes]
    // zeroes, in place of               //  24   [4 bytes]
    //   QuantizationScale
    //   Version
    // zeroes                            //  28   [4 bytes]
    //   32 TOTAL

    @Override
    public int getVideoSectorHeaderSize() { return 32; }
    
    public SectorReBoot(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;
        
        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh != null) {
            // only if it has a sector header should we check if it reports REALTIME | VIDEO
            if (sh.getSubMode().toByte() != (SubMode.MASK_REAL_TIME | SubMode.MASK_VIDEO))
                return;
        }
        
        if (_header.lngMagic != SectorStrVideo.VIDEO_SECTOR_MAGIC) return;
        if (!_header.hasStandardChunkNumber()) return;
        if (!_header.hasStandardChunksInFrame()) return;
        if (!_header.hasStandardFrameNumber()) return;
        if (!_header.hasStandardUsedDemuxSize()) return;
        // some non-video sectors look a little like video sectors but with 1x1 dimensions
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 2) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 2) return;
        int iZeroes = cdSector.readUInt16LE(20);
        if (iZeroes != 0) return;
        _iMoreThanFrameCount = cdSector.readUInt16LE(22);
        if (_iMoreThanFrameCount < 1) return;
        for (int i = 0; i < 8; i++) {
            if (cdSector.readUserDataByte(24 + i) != 0) return;
        }
        setProbability(100);
    }

    // .. Public methods ...................................................

    public @Nonnull String getTypeName() {
        return "ReBoot video";
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d " +
            "{demux frame size=%d %d>frame count}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _header.iUsedDemuxedSize,
            _iMoreThanFrameCount
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
    {
        // Most values are 0 in the header

        // Qscale is not required, so I guess no reason to restrict the frame type

        int iDemuxSizeForHeader = (iNewUsedSize + 3) & ~3;
        IO.writeInt32LE(abCurrentVidSectorHeader, 12, iDemuxSizeForHeader);
    }
}

