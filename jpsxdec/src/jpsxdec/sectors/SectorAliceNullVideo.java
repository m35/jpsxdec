/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.ByteArrayFPIS;


/** Alice In Cyber Land 'null' frame chunk sector. */
public class SectorAliceNullVideo extends IdentifiedSector {

    // .. Static stuff .....................................................

    public static final long ALICE_VIDEO_SECTOR_MAGIC = 0x00000160;
    public static final int ALICE_VIDEO_SECTOR_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    protected final SectorAbstractVideo.CommonVideoSectorFirst16bytes _header =
            new SectorAbstractVideo.CommonVideoSectorFirst16bytes();
    // 16 bytes -- all zeros               //  16   [16 bytes]
    //   32 TOTAL


    public SectorAliceNullVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (cdSector.hasSubHeader() &&
            cdSector.subModeMask(SubMode.MASK_DATA | SubMode.MASK_VIDEO) == 0)
        {
            return;
        }
        
        if (cdSector.readUInt32LE(0) != ALICE_VIDEO_SECTOR_MAGIC)
                return;

        if (_header.readChunkNumberStandard(cdSector))
                return;
        _header.readChunksInFrame(cdSector);
        if (_header.iChunksInThisFrame < 3)
                return;

        // null frames between movies have a frame number of 0xFFFF
        // the high bit signifies the end of a video
        if (_header.readFrameNumberStandard(cdSector))
                return;

        _header.readUsedDemuxSize(cdSector);

        // make sure all 16 bytes are zero
        for (int i = 16; i < 32; i++)
            if (cdSector.readUserDataByte(i) != 0)
                return;

        setProbability(100);
    }

    // .. Public functions .................................................

    public String toString() {
        StringBuilder sb = 
                new StringBuilder(getTypeName()).append(' ').append(super.toString());
        sb.append(" frame:");
        if (_header.iFrameNumber == 0xFFFF)
            sb.append("NUL");
        else {
            sb.append(getFrameNumber());
            if (isEndFrame())
                sb.append("[End]");
        }
        sb.append(" chunk:").append(_header.iChunkNumber).append('/').append(_header.iChunksInThisFrame);
        sb.append(" {demux=").append(_header.iUsedDemuxedSize).append('}');
        return sb.toString();
    }

    public int getChunkNumber() {
        return _header.iChunkNumber;
    }

    public int getChunksInFrame() {
        return _header.iChunksInThisFrame;
    }

    public int getFrameNumber() {
        // the high bit signifies the end of a video
        return _header.iFrameNumber & 0x3FFF;
    }

    public boolean isEndFrame() {
        // the high bit signifies the end of a video
        return (_header.iFrameNumber & 0x8000) != 0;
    }

    public int getIdentifiedUserDataSize() {
        return super.getCdSector().getCdUserDataSize() -
            ALICE_VIDEO_SECTOR_HEADER_SIZE;
    }

    public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                ALICE_VIDEO_SECTOR_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    public void copyIdentifiedUserData(@Nonnull byte[] abOut, int iOutPos) {
        super.getCdSector().getCdUserDataCopy(ALICE_VIDEO_SECTOR_HEADER_SIZE, abOut,
                iOutPos, getIdentifiedUserDataSize());
    }
    
    public @Nonnull String getTypeName() {
        return "AliceNull";
    }

}

