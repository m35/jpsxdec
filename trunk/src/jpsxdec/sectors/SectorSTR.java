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

package jpsxdec.sectors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for standard v2 and v3 video frame chunk sectors. */
final public class SectorSTR extends SectorAbstractVideo {
    
    private static final Logger log = Logger.getLogger(SectorSTR.class.getName());
    protected Logger log() { return log; }

    // .. Static stuff .....................................................

    public static final long VIDEO_CHUNK_MAGIC = 0x80010160;

    // .. Additional fields ...............................................
    
    // Magic 0x80010160                 //  0    [4 bytes]
    // ChunkNumber                      //  4    [2 bytes]
    // ChunksInThisFrame                //  6    [2 bytes]
    // FrameNumber                      //  8    [4 bytes]
    // UsedDemuxedSize                  //  12   [4 bytes]
    // Width                            //  16   [2 bytes]
    // Height                           //  18   [2 bytes]
    // RunLengthCodeCount               //  20   [2 bytes]
    private int  _iQuantizationScale;   //  24   [2 bytes]
    private int  _iVersion;             //  26   [2 bytes]
    private long _lngFourZeros;         //  28   [4 bytes]
    //   32 TOTAL
    
    // .. Constructor .....................................................

    public SectorSTR(CdSector cdSector) throws NotThisTypeException {
        super(cdSector);
        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (cdSector.hasRawSectorHeader()) {
            if (!(cdSector.getSubMode().getData() || cdSector.getSubMode().getVideo()))
            {
                throw new NotThisTypeException();
            }
        }
        try {
            ByteArrayInputStream inStream = cdSector.getCdUserDataStream();

            long lngMagic = IO.readUInt32LE(inStream);
            if (lngMagic != VIDEO_CHUNK_MAGIC)
                throw new NotThisTypeException();

            _iChunkNumber = IO.readSInt16LE(inStream);
            if (_iChunkNumber < 0)
                throw new NotThisTypeException();
            _iChunksInThisFrame = IO.readSInt16LE(inStream);
            if (_iChunksInThisFrame < 1)
                throw new NotThisTypeException();
            _iFrameNumber = IO.readSInt32LE(inStream);
            if (_iFrameNumber < 0)
                throw new NotThisTypeException();

            _lngUsedDemuxedSize = IO.readSInt32LE(inStream);
            if (_lngUsedDemuxedSize < 0)
                throw new NotThisTypeException();

            _iWidth = IO.readSInt16LE(inStream);
            if (_iWidth < 1)
                throw new NotThisTypeException();
            _iHeight = IO.readSInt16LE(inStream);
            if (_iHeight < 1)
                throw new NotThisTypeException();

            _iRunLengthCodeCount = IO.readUInt16LE(inStream);

            long _iMagic3800 = IO.readUInt16LE(inStream);
            if (_iMagic3800 != 0x3800)
                throw new NotThisTypeException();

            _iQuantizationScale = IO.readSInt16LE(inStream);
            if (_iQuantizationScale < 1)
                throw new NotThisTypeException();
            _iVersion = IO.readUInt16LE(inStream);
            if (_iVersion != 2 && _iVersion != 3)
                throw new NotThisTypeException();
            _lngFourZeros = IO.readUInt32LE(inStream);
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    // .. Public methods ...................................................

    public String getTypeName() {
        return "STR";
    }

    final public String toString() {
        return String.format("%s %s frame:%d chunk:%d/%d %dx%d ver:%d " +
            "{demux frame size=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iVersion,
            _lngUsedDemuxedSize,
            _iRunLengthCodeCount,
            _iQuantizationScale,
            _lngFourZeros
            );
    }

    @Override
    public int getSectorHeaderSize() {
        return 32;
    }

}

