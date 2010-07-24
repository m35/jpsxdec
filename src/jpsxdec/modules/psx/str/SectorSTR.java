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

package jpsxdec.modules.psx.str;

import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.JPSXModule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaSubHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.psx.video.encode.ParsedMdecImage;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for standard v2 and v3 video frame chunk sectors. */
public class SectorSTR extends IdentifiedSector implements IVideoSector {
    
    private static final Logger log = Logger.getLogger(SectorSTR.class.getName());
    protected Logger log() { return log; }

    // .. Static stuff .....................................................

    public static final long VIDEO_CHUNK_MAGIC = 0x80010160;
    public static final int FRAME_SECTOR_HEADER_SIZE = 32;

    // .. Instance fields ..................................................

    protected long _lngMagic;              //  0    [4 bytes]
    protected int  _iChunkNumber;          //  4    [2 bytes]
    protected int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected int  _iFrameNumber;          //  8    [4 bytes]
    protected long _lngUsedDemuxedSize;    //  12   [4 bytes]
    protected int  _iWidth;                //  16   [2 bytes]
    protected int  _iHeight;               //  18   [2 bytes]
    protected int  _iRunLengthCodeCount;   //  20   [2 bytes]
    protected int  _iMagic3800;            //  22   [2 bytes] always 0x3800
    protected int  _iQuantizationScale;    //  24   [2 bytes]
    protected int  _iVersion;              //  26   [2 bytes]
    protected long _lngFourZeros;          //  28   [4 bytes]
    //   32 TOTAL
    // .. Constructor .....................................................

    public SectorSTR(CdSector cdSector) throws NotThisTypeException {
        super(cdSector);
        // only if it has a sector header should we check if it reports DATA or VIDEO
        if (cdSector.hasSectorHeader()) {
            DATA_AUDIO_VIDEO eType = cdSector.getSubMode().getDataAudioVideo();
            if (eType != DATA_AUDIO_VIDEO.DATA &&
                eType != DATA_AUDIO_VIDEO.VIDEO)
            {
                throw new NotThisTypeException();
            }
        }
        try {
            readHeader(cdSector.getCdUserDataStream());
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    /** Child classes override this method so most of this class can be reused. */
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
        if (_lngUsedDemuxedSize < 0)
            throw new NotThisTypeException();

        _iWidth = IO.readSInt16LE(inStream);
        if (_iWidth < 1)
            throw new NotThisTypeException();
        _iHeight = IO.readSInt16LE(inStream);
        if (_iHeight < 1)
            throw new NotThisTypeException();
        
        _iRunLengthCodeCount = IO.readUInt16LE(inStream);

        _iMagic3800 = IO.readUInt16LE(inStream);
        if (_iMagic3800 != 0x3800)
            throw new NotThisTypeException();

        _iQuantizationScale = IO.readSInt16LE(inStream);
        if (_iQuantizationScale < 1)
            throw new NotThisTypeException();
        _iVersion = IO.readUInt16LE(inStream);
        if (_iVersion != 2 && _iVersion != 3)
            throw new NotThisTypeException();
        _lngFourZeros = IO.readUInt32LE(inStream);
    }

    // .. Public functions .................................................

    public String toString() {
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
            _iMagic3800,
            _iQuantizationScale,
            _lngFourZeros
            );
    }

    public int getChunkNumber() {
        return _iChunkNumber;
    }

    public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getIdentifiedUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                FRAME_SECTOR_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                FRAME_SECTOR_HEADER_SIZE, getIdentifiedUserDataSize());
    }

    public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(FRAME_SECTOR_HEADER_SIZE, abOut,
                iOutPos, getIdentifiedUserDataSize());
    }

    public int getSectorType() {
        return SECTOR_VIDEO;
    }
    
    public String getTypeName() {
        return "STR";
    }

    public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(prevSector.getClass().equals(prevSector.getClass())))
            return false;

        if (prevSector.getFrameNumber() == getFrameNumber() &&
            prevSector.getChunksInFrame() != getChunksInFrame())
            return false;

        if (getWidth()  != prevSector.getWidth() ||
            getHeight() != prevSector.getHeight())
               return false;

        /*  This logic is accurate, but not forgiving at all
        long iNextChunk = prevSector.getChunkNumber() + 1;
        long iNextFrame = prevSector.getFrameNumber();
        if (iNextChunk >= prevSector.getChunksInFrame()) {
            iNextChunk = 0;
            iNextFrame++;
        }

        if (iNextChunk != getChunkNumber() || iNextFrame != getFrameNumber())
            return false;
        */

        // softer logic
        if (prevSector.getFrameNumber() == getFrameNumber())
            return true;
        else if (prevSector.getFrameNumber() == getFrameNumber() - 1)
            return true;
        else
            return false;
    }

    public DiscItem createMedia(int iStartSector, int iStartFrame, int iFrame1LastSector)
    {
        int iSectors = getSectorNumber() - iStartSector + 1;
        int iFrames = getFrameNumber() - iStartFrame + 1;
        return createMedia(iStartSector, iStartFrame, 
                           iFrame1LastSector,
                           iSectors, iFrames);
    }

    public DiscItem createMedia(int iStartSector, int iStartFrame,
                                int iFrame1LastSector,
                                int iSectors, int iPerFrame)
    {
        return new DiscItemSTRVideo(iStartSector, getSectorNumber(),
                                    iStartFrame, getFrameNumber(),
                                    getWidth(), getHeight(),
                                    iSectors, iPerFrame,
                                    iFrame1LastSector);
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleVideo.getModule();
    }

    public int replaceFrameData(CDFileSectorReader cd,
                                byte[] abDemuxData, int iDemuxOfs,
                                int iLuminQscale, int iChromQscale,
                                int iMdecCodeCount)
            throws IOException
    {
        if (iLuminQscale != iChromQscale)
            throw new IllegalArgumentException();
        
        int iDemuxSizeForHeader = (abDemuxData.length + 3) & ~3;

        byte[] abSectUserData = getCDSector().getCdUserDataCopy();

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abSectUserData, 20, ParsedMdecImage.calculateHalfCeiling32(iMdecCodeCount));
        IO.writeInt16LE(abSectUserData, 24, (short)iLuminQscale);

        int iBytesToCopy = getIdentifiedUserDataSize();
        if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
            iBytesToCopy = abDemuxData.length - iDemuxOfs;

        // bytes to copy might be 0, which is ok because we
        // still need to write the updated headers
        System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, 32, iBytesToCopy);

        cd.writeSector(getSectorNumber(), abSectUserData);

        return iBytesToCopy;
    }
}

