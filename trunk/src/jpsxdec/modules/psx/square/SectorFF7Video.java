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

package jpsxdec.modules.psx.square;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.psx.str.IVideoSector;
import jpsxdec.modules.psx.str.SectorSTR;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for FF7 (v1) video sectors. */
public class SectorFF7Video extends SectorSTR
        implements IVideoSector 
{
    private static final Logger log = Logger.getLogger(SectorFF7Video.class.getName());

    // .. Instance fields ..................................................

    private int _iUserDataStart;
    private long _lngUnknown8bytes;

    // .. Constructor .....................................................

    public SectorFF7Video(CDSector cdSector) throws NotThisTypeException {
        super(cdSector); // will call overridden readHeader() method
    }

    @Override
    protected void readHeader(ByteArrayInputStream inStream)
            throws NotThisTypeException, IOException
    {
        _lngMagic = IO.readUInt32LE(inStream);
        if (_lngMagic != VIDEO_CHUNK_MAGIC)
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
            // FIXME: this block is unfortunately necessary to prevent false-positives with Lain sectors
            // The alternative is to put Lain detection first

            // if movie height is 240, then the unknown data must all be 0
            if (_lngUnknown8bytes != 0)
                throw new NotThisTypeException();
            // and the real-time flag should be set
            if (!getCDSector().getSubMode().getRealTime())
                throw new NotThisTypeException();
        }

        _lngFourZeros = IO.readUInt32LE(inStream);
        if (_lngFourZeros != 0)
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
    }

    // .. Public functions .................................................

    public String toString() {

        String sRet = String.format("%s %s frame:%d chunk:%d/%d %dx%d " +
            "{unknown=%016x 4*00=%08x}",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _lngUnknown8bytes,
            _lngFourZeros
            );

        if (_iUserDataStart == FRAME_SECTOR_HEADER_SIZE) {
            return sRet;
        } else {
            return sRet + " + Camera data";
        }
    }

    public int getIdentifiedUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                _iUserDataStart;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                _iUserDataStart, getIdentifiedUserDataSize());
    }

    public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(_iUserDataStart, abOut,
                iOutPos, getIdentifiedUserDataSize());
    }

    public String getTypeName() {
        return "FF7 Video";
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleSquare.getModule();
    }

    public int replaceFrameData(CDFileSectorReader cd,
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

}


