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
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for FF7 (v1) video sectors. */
public class SectorFF7Video extends SectorAbstractVideo {
    
    private static final Logger log = Logger.getLogger(SectorFF7Video.class.getName());

    public static final int FRAME_SECTOR_HEADER_SIZE = 32;
    
    // .. Additional fields ...............................................

    // Magic 0x80010160                 //  0    [4 bytes]
    // ChunkNumber                      //  4    [2 bytes]
    // ChunksInThisFrame                //  6    [2 bytes]
    // FrameNumber                      //  8    [4 bytes]
    // UsedDemuxedSize                  //  12   [4 bytes]
    // Width                            //  16   [2 bytes]
    // Height                           //  18   [2 bytes]
    private long _lngUnknown8bytes;     //  20   [8 bytes]
    // FourZeros                        //  28   [4 bytes]
    //   32 TOTAL

    private int _iUserDataStart;
    
    // .. Constructor .....................................................

    // SubMode flags "--FT-A--" should never be set
    private static final int SUB_MODE_MASK = 
            (SubMode.MASK_FORM | SubMode.MASK_TRIGGER | SubMode.MASK_AUDIO);

    public SectorFF7Video(CdSector cdSector) throws NotThisTypeException {
        super(cdSector);

        if (cdSector.hasRawSectorHeader()) {
            SubMode sm = cdSector.getSubMode();
            if ((sm.toByte() & SUB_MODE_MASK) != 0)
            {
                throw new NotThisTypeException();
            }

        }
        try {

            ByteArrayInputStream inStream = cdSector.getCdUserDataStream();

            long lngMagic = IO.readUInt32LE(inStream);
            if (lngMagic != SectorSTR.VIDEO_CHUNK_MAGIC)
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
            if (_lngUsedDemuxedSize < 2000)
                throw new NotThisTypeException();

            _iWidth = IO.readSInt16LE(inStream);
            if (_iWidth != 320 && _iWidth != 640)
                throw new NotThisTypeException();
            _iHeight = IO.readSInt16LE(inStream);
            if (_iHeight != 224 && _iHeight != 192 && _iHeight != 240)
                throw new NotThisTypeException();

            _lngUnknown8bytes = IO.readSInt64BE(inStream);

            if (_iHeight == 240) {
                // this block is unfortunately necessary to prevent false-positives with Lain sectors
                // An alternative might be to put Lain detection first

                // if movie height is 240, then the unknown data must all be 0
                if (_lngUnknown8bytes != 0)
                    throw new NotThisTypeException();

                // and there can only be 10 chunks for frames that are really high
                if (_iChunksInThisFrame == 10 && _iFrameNumber < 900)
                    throw new NotThisTypeException();
            }


            long lngFourZeros = IO.readUInt32LE(inStream);
            if (lngFourZeros != 0)
                throw new NotThisTypeException();

            if (_iChunkNumber == 0) {
                inStream.skip(2);
                if (IO.readUInt16LE(inStream) != 0x3800) {
                    IO.skip(inStream, 40 - 4 + 2);
                    if (IO.readUInt16LE(inStream) != 0x3800)
                        throw new NotThisTypeException();
                    _iUserDataStart = FRAME_SECTOR_HEADER_SIZE + 40;
                } else {
                    _iUserDataStart = FRAME_SECTOR_HEADER_SIZE;
                }
            } else {
                _iUserDataStart = FRAME_SECTOR_HEADER_SIZE;
            }
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }

    }

    // .. Public functions .................................................

    public String toString() {

        String sRet = String.format("%s %s frame:%d chunk:%d/%d %dx%d {unknown=%016x}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _lngUnknown8bytes
            );

        if (_iUserDataStart == FRAME_SECTOR_HEADER_SIZE) {
            return sRet;
        } else {
            return sRet + " + Camera data";
        }
    }

    public String getTypeName() {
        return "FF7 Video";
    }

    public int replaceFrameData(CdFileSectorReader cd,
                                byte[] abDemuxData, int iDemuxOfs,
                                int iLuminQscale, int iChromQscale,
                                int iMdecCodeCount)
            throws IOException
    {
        if (iLuminQscale != iChromQscale)
            throw new IllegalArgumentException();

        // TODO: Only frames with camera data need the +40
        // so how do I let this sector know that the frame started with camera data?
        int iDemuxSizeForHeader = ((abDemuxData.length + 3) & ~3) + 40;

        byte[] abSectUserData = getCDSector().getCdUserDataCopy();

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);

        int iBytesToCopy = getIdentifiedUserDataSize();
        if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
            iBytesToCopy = abDemuxData.length - iDemuxOfs;

        // bytes to copy might be 0, which is ok because we
        // still need to write the updated headers
        System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, _iUserDataStart, iBytesToCopy);

        cd.writeSector(getSectorNumber(), abSectUserData);

        return iBytesToCopy;
    }

    @Override
    public int getSectorHeaderSize() {
        return _iUserDataStart;
    }

}


