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

package jpsxdec.plugins.psx.square;

import jpsxdec.plugins.IdentifiedSector;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.plugins.psx.str.IVideoSector;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** This is the header for FF7 (v1) video sectors. */
public class SectorFF7Video extends IdentifiedSector
        implements IVideoSector 
{
    private static final Logger log = Logger.getLogger(SectorFF7Video.class.getName());

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
    protected long _lngRunLengthCodeCount; //  20   [2 bytes]
    protected long _lngMagic3800;          //  22   [2 bytes] always 0x3800
    protected int  _iQuantizationScale;    //  24   [2 bytes]
    protected long _lngVersion;            //  26   [2 bytes]
    protected long _lngFourZeros;          //  28   [4 bytes]
    //   32 TOTAL
    // .. Constructor .....................................................

    public SectorFF7Video(CDSector cdSector) throws NotThisTypeException {
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
            readHeader(cdSector.getCDUserDataStream());
        } catch (IOException ex) {
            throw new NotThisTypeException();
        }
    }

    private void readHeader(ByteArrayInputStream inStream)
            throws NotThisTypeException, IOException 
    {
        _lngMagic = IO.readUInt32LE(inStream);

        // TODO: This extra Chrono Cross hack is ugly, fix it
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
        
        _lngRunLengthCodeCount = IO.readUInt16LE(inStream);

        _lngMagic3800 = IO.readUInt16LE(inStream);
        // None of the FF7 vid sectors have the magic 3800.
        // it's usually 0000, but does have other values.
        // So just make sure it's not 3800.
        if (_lngMagic3800 == 0x3800)
            throw new NotThisTypeException();

        _iQuantizationScale = IO.readSInt16LE(inStream);
        // do not check the qscale because it can sometimes be invalid
        //if (_iQuantizationScale < 1)
        //    throw new NotThisTypeException();
        _lngVersion = IO.readUInt16LE(inStream);
        _lngFourZeros = IO.readUInt32LE(inStream);
    }

    // .. Public functions .................................................

    public String toString() {
        return String.format("FF7 Vid %s frame:%d chunk:%d/%d %dx%d ver:%d " +
            "{demux frame size=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
            super.toString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _lngVersion,
            _lngUsedDemuxedSize,
            _lngRunLengthCodeCount,
            _lngMagic3800,
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

    public int getPSXUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                FRAME_SECTOR_HEADER_SIZE;
    }

    public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                FRAME_SECTOR_HEADER_SIZE, getPSXUserDataSize());
    }

    public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(FRAME_SECTOR_HEADER_SIZE, abOut,
                iOutPos, getPSXUserDataSize());
    }

    public int getSectorType() {
        return SECTOR_VIDEO;
    }
    
    public String getTypeName() {
        return "Video";
    }

    public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(prevSector instanceof SectorFF7Video))
            return false;

        SectorFF7Video ff7VidSect = (SectorFF7Video) prevSector;

        if (getWidth()  != ff7VidSect.getWidth() ||
            getHeight() != ff7VidSect.getHeight())
               return false;

        long iNextChunk = ff7VidSect.getChunkNumber() + 1;
        long iNextFrame = ff7VidSect.getFrameNumber();
        if (iNextChunk >= ff7VidSect.getChunksInFrame()) {
            iNextChunk = 0;
            iNextFrame++;
        }

        if (iNextChunk != getChunkNumber() || iNextFrame != getFrameNumber())
            return false;

        if (ff7VidSect.getFrameNumber() == getFrameNumber() &&
            ff7VidSect.getChunksInFrame() != getChunksInFrame())
            return false;

        return true;
    }

    public DiscItem createMedia(int iStartSector, int iStartFrame, int iFrame1End)
    {
        int iSectors = getSectorNumber() - iStartSector;
        int iFrames = getFrameNumber() - iStartFrame;
        return createMedia(iStartSector, iStartFrame, iFrame1End, iSectors, iFrames);
    }
    public DiscItem createMedia(int iStartSector, int iStartFrame,
                                int iFrame1End,
                                int iSectors, int iPerFrame)
    {
        return new DiscItemSTRVideo(iStartSector, getSectorNumber(),
                                    iStartFrame, getFrameNumber(),
                                    getWidth(), getHeight(),
                                    iSectors, iPerFrame,
                                    iFrame1End);
    }


    public JPSXPlugin getSourcePlugin() {
        return JPSXPluginSquare.getPlugin();
    }

}

