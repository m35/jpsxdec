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

package jpsxdec.modules.ac3;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.DemuxedData;

/** Represents a video sector from the game Ace Combat 3 Electrosphere. */
public class SectorAceCombat3Video extends IdentifiedSector
        implements DemuxedData.Piece, SectorBasedFrameReplace.IReplaceableVideoSector
{

    // Always 1                        // 0  [1 byte]
    private int _iChunkNumber;         // 1  [1 byte]
    private int _iChunksInThisFrame;   // 2  [2 bytes]
    private int _iUnknown1;            // 4  [2 bytes]
    /** The frame number descends from the total frame count down to 0. */
    private int _iInvertedFrame;       // 6  [2 bytes]
    private int _iWidth,               // 8  [2 bytes]
                _iHeight;              // 10 [2 bytes]
    // Always 0                        // 12 [4 bytes]
    private long _lngUnknown2;         // 16 [8 bytes]
    private long _lngUnknown3;         // 24 [8 bytes]

    @Override
    public int getVideoSectorHeaderSize() { return 32; }


    public SectorAceCombat3Video(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.getSubHeader() == null) return;

        if (cdSector.readUserDataByte(0) != 0x01) return;

        _iChunkNumber = cdSector.readUserDataByte(1);
        if (_iChunkNumber < 0) return;
        _iChunksInThisFrame = cdSector.readSInt16LE(2);
        if (_iChunksInThisFrame < 1) return;

        _iUnknown1 = cdSector.readUInt16LE(4);

        _iInvertedFrame = cdSector.readUInt16LE(6);
        if (_iInvertedFrame < 0) return;

        _iWidth = cdSector.readSInt16LE(8);
        _iHeight = cdSector.readSInt16LE(10);
        if (!((_iWidth == 304 && _iHeight == 224) ||
              (_iWidth == 320 && _iHeight == 176) ||
              (_iWidth == 128 && _iHeight == 96 )))
            return;

        if (cdSector.readSInt32BE(12) != 0)
            return;

        _lngUnknown2 = cdSector.readSInt64BE(16);
        _lngUnknown3 = cdSector.readSInt64BE(24);

        setProbability(100);
    }

    @Override
    public @Nonnull String getTypeName() {
        return "AC3";
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getChunkNumber() {
        return _iChunkNumber;
    }

    public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getInvertedFrameNumber() {
        return _iInvertedFrame;
    }

    public int getChannel() {
        CdSectorXaSubHeader sh = getCdSector().getSubHeader();
        assert sh != null; // subheader confirmed to exist in constructor
        return sh.getChannel();
    }

    @Override
    public int getDemuxPieceSize() {
        return getCdSector().getCdUserDataSize() - getVideoSectorHeaderSize();
    }

    @Override
    public byte getDemuxPieceByte(int i) {
        return getCdSector().readUserDataByte(getVideoSectorHeaderSize() + i);
    }

    @Override
    public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        getCdSector().getCdUserDataCopy(getVideoSectorHeaderSize(), abOut,
                iOutPos, getDemuxPieceSize());
    }

    @Override
    public String toString() {
        return String.format("%s %s inv-frame: %d chunk:%d/%d %dx%d ?1:%04x ?2:%016x ?3:%016x",
            getTypeName(),
            cdToString(),
            _iInvertedFrame,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iUnknown1,
            _lngUnknown2,
            _lngUnknown3
            );
    }

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
    {
        // I was unable to identify any correlation between the AC3 sector
        // headers and the frame data they contain (except dimensions),
        // so hopefully no changes to the sector headers are necessary
        // when replacing frame data.
    }
}
