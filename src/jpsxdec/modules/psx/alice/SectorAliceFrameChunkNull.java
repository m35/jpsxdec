/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules.psx.alice;

import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.JPSXModule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Alice In Cyber Land frame chunk sector. */
public class SectorAliceFrameChunkNull extends IdentifiedSector
{

    // .. Static stuff .....................................................

    public static final long VIDEO_CHUNK_MAGIC = 0x00000160;
    public static final int FRAME_CHUNK_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    // Magic                                   //  0    [4 bytes]
    private final int  _iChunkNumber;          //  4    [2 bytes]
    private final int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected final int  _iFrameNumber;        //  8    [4 bytes]
    private final long _lngUsedDemuxSize;      //  12   [4 bytes]
    // 16 bytes -- all zeros                   //  16   [16 bytes]
    //   32 TOTAL
    
    // .. Constructor .....................................................

    public SectorAliceFrameChunkNull(CDSector cdSector) throws NotThisTypeException
    {
        super(cdSector);
        if (cdSector.hasSectorHeader()) {
            // if it has a header, the sector type MUST be data or video
            // (anything else is ignored)
            if (cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.DATA &&
                cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.VIDEO)
            {
                throw new NotThisTypeException();
            }
        }
        try {
            ByteArrayFPIS bais = cdSector.getCDUserDataStream();
            if (IO.readUInt32LE(bais) != VIDEO_CHUNK_MAGIC)
                throw new NotThisTypeException();

            _iChunkNumber = IO.readSInt16LE(bais);
            if (_iChunkNumber < 0)
                throw new NotThisTypeException();
            _iChunksInThisFrame = IO.readSInt16LE(bais);
            if (_iChunksInThisFrame < 3)
                throw new NotThisTypeException();
            _iFrameNumber = IO.readSInt32LE(bais);

            // null frames between movies have a frame number of 0xFFFF
            // the high bit signifies the end of a video
            if (_iFrameNumber < 0)
                throw new NotThisTypeException();

            _lngUsedDemuxSize = IO.readUInt32LE(bais);

            // make sure all 16 bytes are zero
            for (int i = 0; i < 16; i++)
                if (bais.read() != 0)
                    throw new NotThisTypeException();

        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    // .. Public functions .................................................

    public String toString() {
        StringBuilder sb = new StringBuilder(getTypeName());
        sb.append(' ');
        sb.append(super.toString());
        sb.append(" frame:");
        if (_iFrameNumber == 0xFFFF)
            sb.append("NUL");
        else {
            sb.append(getFrameNumber());
            if (isEndFrame())
                sb.append("[End]");
        }
        sb.append(" chunk:");
        sb.append(_iChunkNumber);
        sb.append('/');
        sb.append(_iChunksInThisFrame);
        sb.append(" {demux=");
        sb.append(_lngUsedDemuxSize);
        sb.append('}');
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
            FRAME_CHUNK_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                FRAME_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(FRAME_CHUNK_HEADER_SIZE, abOut,
                iOutPos, getIdentifiedUserDataSize());
    }
    
    public int getSectorType() {
        return -1;
    }

    public String getTypeName() {
        return "AliceNull";
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleAlice.getModule();
    }

}

