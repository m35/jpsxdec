/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.ByteArrayFPIS;


/** Alice In Cyber Land 'null' frame chunk sector. */
public class SectorAliceNullVideo extends IdentifiedSector {

    // .. Static stuff .....................................................

    public static final long ALICE_VIDEO_SECTOR_MAGIC = 0x00000160;
    public static final int ALICE_VIDEO_SECTOR_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    // Magic 0x00000160                  //  0    [4 bytes]
    private int  _iChunkNumber;          //  4    [2 bytes]
    private int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected int  _iFrameNumber;        //  8    [4 bytes]
    private long _lngUsedDemuxSize;      //  12   [4 bytes]
    // 16 bytes -- all zeros             //  16   [16 bytes]
    //   32 TOTAL


    public SectorAliceNullVideo(CdSector cdSector) {
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

        _iChunkNumber = cdSector.readSInt16LE(4);
        if (_iChunkNumber < 0)
                return;
        _iChunksInThisFrame = cdSector.readSInt16LE(6);
        if (_iChunksInThisFrame < 3)
                return;

        // null frames between movies have a frame number of 0xFFFF
        // the high bit signifies the end of a video
        _iFrameNumber = cdSector.readSInt32LE(8);
        if (_iFrameNumber < 0)
                return;

        _lngUsedDemuxSize = cdSector.readUInt32LE(12);

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
        if (_iFrameNumber == 0xFFFF)
            sb.append("NUL");
        else {
            sb.append(getFrameNumber());
            if (isEndFrame())
                sb.append("[End]");
        }
        sb.append(" chunk:").append(_iChunkNumber).append('/').append(_iChunksInThisFrame);
        sb.append(" {demux=").append(_lngUsedDemuxSize).append('}');
        return sb.toString();
    }

    public int getChunkNumber() {
        return _iChunkNumber;
    }

    public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getFrameNumber() {
        // the high bit signifies the end of a video
        return _iFrameNumber & 0x3FFF;
    }

    public boolean isEndFrame() {
        // the high bit signifies the end of a video
        return (_iFrameNumber & 0x8000) != 0;
    }

    public int getIdentifiedUserDataSize() {
        return super.getCDSector().getCdUserDataSize() -
            ALICE_VIDEO_SECTOR_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                ALICE_VIDEO_SECTOR_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(ALICE_VIDEO_SECTOR_HEADER_SIZE, abOut,
                iOutPos, getIdentifiedUserDataSize());
    }
    
    public int getSectorType() {
        return -1;
    }

    public String getTypeName() {
        return "AliceNull";
    }

}

