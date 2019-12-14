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
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.util.IO;


/** This is the sector for standard v2 and v3 video frame chunk sectors
 * (which some minor variations).  We will call these "STR" sectors since
 * they follow the only real standard of video streams. */
public class SectorStrVideo extends SectorAbstractVideo {
    
    /** The most common video sector magic number.
     * Shared by most types of video sectors. */
    public static final long VIDEO_SECTOR_MAGIC = 0x80010160L;


    @Nonnull
    private final VideoSectorCommon16byteHeader _header; //  0    [16 bytes]
    private int  _iWidth;                                //  16   [2 bytes]
    private int  _iHeight;                               //  18   [2 bytes]
    private int  _iMdecCodeCount;                        //  20   [2 bytes]
    private int  _iMagic3800;                            //  22   [2 bytes]
    private int  _iQuantizationScale;                    //  24   [2 bytes]
    private int  _iVersion;                              //  26   [2 bytes]
    private long _lng0orFrameStartSector;                //  28   [4 bytes]
    //   32 TOTAL

    @Override
    public int getVideoSectorHeaderSize() { return 32; }
    
    public SectorStrVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;
        
        // only if it has a sector header should we check if it reports DATA or VIDEO
        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh != null) {
            if (sh.getSubMode().mask(SubMode.MASK_DATA | SubMode.MASK_VIDEO) == 0)
                return;
            if (sh.getSubMode().mask(SubMode.MASK_FORM) != 0)
                return;
        }
        
        if (_header.lngMagic != VIDEO_SECTOR_MAGIC) return;
        if (!_header.hasStandardChunkNumber()) return;
        if (!_header.hasStandardChunksInFrame()) return;
        // normal STR sectors have frame number starting at 1
        // but this STR sector also covers similar sectors that may not follow that
        if (!_header.hasStandardFrameNumber()) return;
        if (!_header.hasStandardUsedDemuxSize()) return;
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 1) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 1) return;
        _iMdecCodeCount = cdSector.readUInt16LE(20);
        if (_iMdecCodeCount < 1) return;
        _iMagic3800 = cdSector.readUInt16LE(22);
        // [SLUS-00684] Jackie Chan Stuntmaster has 0 in place of the 3800
        // Not much point creating a separate sector type just for that
        if (_iMagic3800 != 0x3800 && _iMagic3800 != 0) return;
        _iQuantizationScale = cdSector.readSInt16LE(24);
        if (_iQuantizationScale < 1) return;
        _iVersion = cdSector.readUInt16LE(26);
        // Some games claim to be v1 when they work just like v2
        // e.g. Tekken 2 and FF Tactics
        if (_iVersion < 1 || _iVersion > 3) return;

        // Usually 0, but for some videos the value is the sector that the frame
        // started at, relative to the start of the video
        // (Super Puzzle Fighter 2 and Resident Evil, so may be found in other Capcom games)
        _lng0orFrameStartSector = cdSector.readUInt32LE(28);

        setProbability(100);
    }


    public @Nonnull String getTypeName() {
        return "STR";
    }

    public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d ver:%d qscale:%d 3800:%04x" +
            "{demux size:%d rlc:%d 0:%d}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iVersion,
            _iQuantizationScale,
            _iMagic3800,
            _header.iUsedDemuxedSize,
            _iMdecCodeCount,
            _lng0orFrameStartSector
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
        replaceStrVideoSectorHeader(abNewDemuxData, iNewUsedSize,
                                    iNewMdecCodeCount, abCurrentVidSectorHeader);
    }

    /** Replace the header data found in standard STR sectors (and some variants). */
    // TODO maybe move this into a shared space so other packages don't depend on this one?
    // although this logic is very STR specific, so maybe it's right
    public static void replaceStrVideoSectorHeader(@Nonnull byte[] abNewDemuxData, int iNewUsedSize,
                                                   int iNewMdecCodeCount, @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        int iQscale;

        BitStreamUncompressor_STRv2.StrV2Header v2Header = new BitStreamUncompressor_STRv2.StrV2Header(abNewDemuxData, iNewUsedSize);
        if (v2Header.isValid()) {
            iQscale = v2Header.getQuantizationScale();
        } else {
            BitStreamUncompressor_STRv3.StrV3Header v3Header = new BitStreamUncompressor_STRv3.StrV3Header(abNewDemuxData, iNewUsedSize);
            if (v3Header.isValid()) {
                iQscale = v3Header.getQuantizationScale();
            } else {
                throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_V2_V3());
            }
        }

        int iDemuxSizeForHeader = (iNewUsedSize + 3) & ~3;

        IO.writeInt32LE(abCurrentVidSectorHeader, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abCurrentVidSectorHeader, 20,
                Calc.calculateHalfCeiling32(iNewMdecCodeCount));
        IO.writeInt16LE(abCurrentVidSectorHeader, 24, (short)(iQscale));
    }

}

