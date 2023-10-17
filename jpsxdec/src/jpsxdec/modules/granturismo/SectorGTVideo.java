/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015-2023  Michael Sabin
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

package jpsxdec.modules.granturismo;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.video.sectorbased.ISelfDemuxingVideoSector;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.modules.video.sectorbased.VideoSectorCommon16byteHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.IO;


/** Represents a Gran Turismo 1 and 2 video sector. */
public class SectorGTVideo extends IdentifiedSector
        implements DemuxedData.Piece,
                   ISelfDemuxingVideoSector,
                   SectorBasedFrameReplace.IReplaceableVideoSector
{

    // .. Static stuff .....................................................

    public static final int HEADER_SIZE = 32;

    private static final long GT_MAGIC = 0x53490160;

    // .. Fields ..........................................................

    @Nonnull
    private final VideoSectorCommon16byteHeader _header;
    private int  _iTotalFrames;         //  16   [2 bytes]
    /** seems 0x8000 for first chunk and 0x4000 for last chunk (0xc000 for both). */
    private int  _iChunkBitFlags;       //  18   [2 bytes]
    // 12 zeroes                        //  20   [12 bytes]
    //   32 TOTAL

    @Override
    final public int getVideoSectorHeaderSize() { return HEADER_SIZE; }


    public SectorGTVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        _header = new VideoSectorCommon16byteHeader(cdSector);
        if (isSuperInvalidElseReset()) return;

        // the DATA flag should be set
        if (subModeExistsAndMaskDoesNotEqual(SubMode.MASK_DATA, SubMode.MASK_DATA))
            return;

        if (_header.lngMagic != GT_MAGIC)
            return;

        if (!_header.hasStandardChunkNumber())
            return;
        if (!_header.hasStandardChunksInFrame())
            return;
        if (_header.iFrameNumber < 1)
            return;
        if (!_header.hasStandardUsedDemuxSize())
            return;
        _iTotalFrames = cdSector.readSInt16LE(16);
        if (_iTotalFrames < 1)
            return;
        _iChunkBitFlags = cdSector.readUInt16LE(18);
        if ((_iChunkBitFlags & ~0xc000) != 0)
            return;

        int iZero1 = cdSector.readSInt32LE(20);
        int iZero2 = cdSector.readSInt32LE(24);
        int iZero3 = cdSector.readSInt32LE(28);
        if (iZero1 != 0 || iZero2 != 0 || iZero3 != 0)
            return;

        setProbability(100);
    }

    // .. Public methods ...................................................

    @Override
    public @Nonnull String getTypeName() {
        return "GT Video";
    }

    @Override
    public String toString() {
        return String.format("%s %s frame:%d/%d chunk:%d/%d flags:%04x {demux frame size=%d}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _iTotalFrames,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iChunkBitFlags,
            _header.iUsedDemuxedSize
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

    @Override
    public @Nonnull GranTurismoDemuxer createDemuxer(@Nonnull ILocalizedLogger log) {
        return new GranTurismoDemuxer(this, log);
    }

    @Override
    public int getDemuxPieceSize() {
        return getCdSector().getCdUserDataSize() - getVideoSectorHeaderSize();
    }

    @Override
    public byte getDemuxPieceByte(int i) {
        return getCdSector().readUserDataByte(i);
    }

    @Override
    public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        getCdSector().getCdUserDataCopy(getVideoSectorHeaderSize(),
                                        abOut, iOutPos, getDemuxPieceSize());
    }

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        IO.writeInt32LE(abCurrentVidSectorHeader, 12, newFrame.calculateUsedBytesRoundUp4());
    }

}

