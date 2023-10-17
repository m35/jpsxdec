/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.ngauge;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameReplace;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.util.DemuxedData;

/** Sectors have no header.
 * @see NGaugeVideoInfo */
public class SectorNGaugeVideo extends IdentifiedSector
        implements DemuxedData.Piece, SectorBasedFrameReplace.IReplaceableVideoSector
{

    private BitStreamUncompressor_STRv2.StrV2Header _frameHeader;

    public SectorNGaugeVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (subModeExistsAndMaskDoesNotEqual(SubMode.MASK_REAL_TIME |
                                             SubMode.MASK_FORM      |
                                             SubMode.MASK_TRIGGER   |
                                             SubMode.MASK_DATA      |
                                             SubMode.MASK_AUDIO     |
                                             SubMode.MASK_VIDEO,
                                             SubMode.MASK_DATA))
        {
            return;
        }

        byte[] ab = cdSector.getCdUserDataCopy();
        _frameHeader = new BitStreamUncompressor_STRv2.StrV2Header(ab, ab.length);

        setProbability(1);
    }

    @Override
    public int getVideoSectorHeaderSize() {
        return 0;
    }

    public boolean isFrameFirstSector() {
        return _frameHeader.isValid();
    }

    @Override
    public @Nonnull String getTypeName() {
        return "N-Gauge Video";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTypeName()).append(' ').append(cdToString());
        if (_frameHeader.isValid())
            sb.append(' ').append(_frameHeader);
        return sb.toString();
    }

    @Override
    public void replaceVideoSectorHeader(SectorBasedFrameAnalysis existingFrame, BitStreamAnalysis newFrame, byte[] abCurrentVidSectorHeader) {
        // no header to replace
    }

    @Override
    public int getDemuxPieceSize() {
        return getCdSector().getCdUserDataSize();
    }

    @Override
    public byte getDemuxPieceByte(int i) {
        return getCdSector().readUserDataByte(i);
    }

    @Override
    public void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos) {
        getCdSector().getCdUserDataCopy(0, abOut, iOutPos, getCdSector().getCdUserDataSize());
    }

}
