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

import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.psx.str.IVideoSector;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSector.CDXAHeader.SubMode.DATA_AUDIO_VIDEO;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.psx.str.DiscItemSTRVideo;
import jpsxdec.modules.psx.video.encode.ParsedMdecImage;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Base class for Final Fantasy 9 movie (audio/video) sectors. */
public abstract class SectorFF9 extends IdentifiedSector {
        
    protected long _lngMagic;              //  0    [4 bytes]
    protected int  _iChunkNumber;          //  4    [2 bytes]
    protected int  _iChunksInThisFrame;    //  6    [2 bytes]
    protected int  _iFrameNumber;          //  8    [4 bytes]

    protected SectorFF9(CDSector oCDSect) throws NotThisTypeException {
        super(oCDSect);
    }

    /** Reads the header common to both audio and video sectors.
     *  The iForm parameter is the Mode 1 Form ?, since audio and video
     *  sectors use different form. Also, the magic number is different
     *  between audio and videos sectors.
     * 
     * @param iForm        Check for correct Mode 1 Form ?
     * @param iMagicNumber Magic number to check for.
     * @return             The input stream positioned just after the header.
     */
    protected InputStream readSharedHeader(CDSector cdSector, int iForm, int iMagicNumber)
            throws IOException, NotThisTypeException 
    {

        InputStream inStream = cdSector.getCDUserDataStream();
        
        // Without a sector header, we can't read video frames
        if (!cdSector.hasSectorHeader())
            throw new NotThisTypeException();
        if (cdSector.getSubMode().getDataAudioVideo() != DATA_AUDIO_VIDEO.DATA)
            throw new NotThisTypeException();
        // Mode 2 Form 1 or Form 2, depending on the type of sector
        if (cdSector.getSubMode().getForm() != iForm)
            throw new NotThisTypeException();

        _lngMagic = IO.readUInt32LE(inStream);
        // make sure the magic nubmer is correct
        if (_lngMagic != iMagicNumber)
            throw new NotThisTypeException();
        
        _iChunkNumber = IO.readSInt16LE(inStream);
        if (_iChunkNumber < 0)
            throw new NotThisTypeException();
        _iChunksInThisFrame = IO.readSInt16LE(inStream);
        if (_iChunksInThisFrame != 10)
            throw new NotThisTypeException();
        _iFrameNumber = IO.readSInt32LE(inStream);
        if (_iFrameNumber < 0)
            throw new NotThisTypeException();
        
        return inStream;
    }

    public long getActualChunkNumber() {
        return _iChunkNumber;
    }

    public long getActualChunksInFrame() {
        return _iChunksInThisFrame;
    }

    public int getFrameNumber() {
        return _iFrameNumber;
    }

    public JPSXModule getSourceModule() {
        return JPSXModuleSquare.getModule();
    }

    /**************************************************************************/
    /**************************************************************************/

    /** Final Fantasy 9 video chunk sector. */
    public static class PSXSectorFF9Video 
            extends SectorFF9
            implements IVideoSector 
    {

        // .. Static stuff .....................................................

        public static final int VIDEO_CHUNK_MAGIC = 0x00040160;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;

        // .. Instance .........................................................

        /* In the parent
        protected long _lngMagic;              //  0    [4 bytes]
        protected long _iChunkNumber;          //  4    [2 bytes]
        protected long _iChunksInThisFrame;    //  6    [2 bytes]
        protected long _iFrameNumber;          //  8    [4 bytes]
        */
        protected final long _lngUsedDemuxedSize;    //  12   [4 bytes]
        protected final int  _iWidth;                //  16   [2 bytes]
        protected final int  _iHeight;               //  18   [2 bytes]
        protected final int  _iRunLengthCodeCount;   //  20   [2 bytes]
        protected final int  _iMagic3800;            //  22   [2 bytes] always 0x3800
        protected final int  _iQuantizationScale;    //  24   [2 bytes]
        protected final int  _iVersion;              //  26   [2 bytes]
        protected final long _lngFourBytes;          //  28   [4 bytes] usually zero
        //   32 TOTAL

        public PSXSectorFF9Video(CDSector cdSector) throws NotThisTypeException
        {
            super(cdSector);

            try {
                // Read the shared header, requesting Mode 2 Form 2, and video magic number
                InputStream inStream = super.readSharedHeader(cdSector, 2, VIDEO_CHUNK_MAGIC);

                _lngUsedDemuxedSize = IO.readUInt32LE(inStream);
                if (_lngUsedDemuxedSize == -1) // check for overflow
                {
                    throw new NotThisTypeException();
                }

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
                if (_iQuantizationScale < 0)
                    throw new NotThisTypeException();
                _iVersion = IO.readUInt16LE(inStream);
                if (_iVersion != 2)
                    throw new NotThisTypeException();
                _lngFourBytes = IO.readUInt32LE(inStream); // usually zero

            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }

        // .. Public functions .................................................

        public String toString() {
            return "FF9Vid " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %dx%d ver:%d " + 
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
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
                _lngFourBytes
                );
        }

        /** Returns the chunk nubmer in order of how they should be demuxed
         * (i.e. 8, 7, 6, ..., 1, 0). [implements IVideoSector] */
        public int getChunkNumber() {
            return 9 - super._iChunkNumber;
        }
        /** [implements IVideoSector] */
        public int getChunksInFrame() {
            return super._iChunksInThisFrame - 2; // i.e. return 8
        }

        /** [implements IVideoSector] */
        public int getWidth() {
            return _iWidth;
        }

        /** [implements IVideoSector] */
        public int getHeight() {
            return _iHeight;
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
            return SECTOR_VIDEO;
        }
        
        public String getTypeName() {
            return "FF9Video";
        }

        public boolean matchesPrevious(IVideoSector prevSector) {
            if (!(prevSector instanceof PSXSectorFF9Video))
                return false;

            PSXSectorFF9Video prevFF9Vid = (PSXSectorFF9Video) prevSector;

            if (getWidth()  != prevFF9Vid.getWidth() ||
                getHeight() != prevFF9Vid.getHeight())
                return false;

            long lngNextChunk = prevFF9Vid.getActualChunkNumber() + 1;
            long lngNextFrame = prevFF9Vid.getFrameNumber();
            if (lngNextChunk >= prevFF9Vid.getActualChunksInFrame()) {
                lngNextChunk = 2;
                lngNextFrame++;
            }

            if (lngNextChunk != getActualChunkNumber() ||
                lngNextFrame != getFrameNumber())
                return false;

            if (getFrameNumber() == prevFF9Vid.getFrameNumber() &&
                getActualChunksInFrame() != prevFF9Vid.getActualChunksInFrame())
                return false;

            return true;
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
        public DiscItem createMedia(int iStartSector, int iStartFrame, int iFrame1LastSector)
        {
            return createMedia(iStartSector, iStartFrame, iFrame1LastSector, 10, 1);
        }

        public int replaceFrameData(CDFileSectorReader cd,
                                    byte[] abDemuxData, int iDemuxOfs,
                                    int iLuminQscale, int iChromQscale,
                                    int iMdecCodeCount)
                throws IOException
        {
            if (iLuminQscale != iChromQscale)
                throw new IllegalArgumentException();
            
            int iHeaderDemuxSize = (abDemuxData.length + 3) & ~3;

            byte[] abSectUserData = getCDSector().getCdUserDataCopy();

            IO.writeInt32LE(abSectUserData, 12, iHeaderDemuxSize / 4);
            IO.writeInt16LE(abSectUserData, 20, ParsedMdecImage.calculateHalfCeiling32(iMdecCodeCount) );
            IO.writeInt16LE(abSectUserData, 24, (short) iLuminQscale);

            int iBytesToCopy = getIdentifiedUserDataSize();
            if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
                iBytesToCopy = abDemuxData.length - iDemuxOfs;

            // bytes to copy might be 0, which is ok because we
            // still need to write the updated headers
            System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, FRAME_CHUNK_HEADER_SIZE, iBytesToCopy);

            cd.writeSector(getSectorNumber(), abSectUserData);

            return iBytesToCopy;
        }

    }

    /**************************************************************************/
    /**************************************************************************/
    
    /** Final Fantasy 9 audio sector. */
    public static class PSXSectorFF9Audio 
            extends SectorFF9
            implements ISquareAudioSector
    {

        // .. Static stuff .....................................................

        public static final int FF9_AUDIO_CHUNK_MAGIC = 0x00080160;
        public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;

        // .. Instance .........................................................

        /*
        private long _lngMagic;              //  0    [4 bytes]
        private long _iChunkNumber;          //  4    [2 bytes]
        private long _iChunksInThisFrame;    //  6    [2 bytes]
        private long _iFrameNumber;          //  8    [4 bytes]
        */
        
        // 116 bytes unknown
        protected final SquareAKAOstruct _akaoStruct;     // [80 bytes]

        //   208 TOTAL

        public PSXSectorFF9Audio(CDSector cdSector) throws NotThisTypeException
        {
            super(cdSector);
            
            try {
                // Read the shared header, requesting Mode 2 Form 1, and audio magic number
                InputStream inStream = super.readSharedHeader(cdSector, 1, FF9_AUDIO_CHUNK_MAGIC);

                IO.skip(inStream, 116);

                _akaoStruct = new SquareAKAOstruct(inStream);
                
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }

        // .. Public functions .................................................

        public String toString() {
            return "FF9Aud " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %s",
                _iFrameNumber,
                _iChunkNumber,
                _iChunksInThisFrame,
                _akaoStruct.toString()
                );
        }

        public int getAudioDataSize() {
            // using the AKAO tag is more reliable than other methods to
            // determine if there is any audio data
            if (_akaoStruct.AKAO == SquareAKAOstruct.AKAO_ID)
                return (int)_akaoStruct.BytesOfData;
            else
                return 0;
        }

        public int getSamplesPerSecond() {
            int iAudData = getAudioDataSize();
            if (iAudData == 1824 || iAudData == 1840)
                return 48000;
            else if (iAudData == 1680)
                return 44100;
            else if (iAudData == 0)
                return 0;
            else
                throw new RuntimeException(
                    "What kind of strange samples/second is this sector?");
        }

        public int getIdentifiedUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                    FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }

        public ByteArrayFPIS getIdentifiedUserDataStream() {
            return new ByteArrayFPIS(super.getCDSector().getCDUserDataStream(), 
                    FRAME_AUDIO_CHUNK_HEADER_SIZE, getIdentifiedUserDataSize());
        }

        public int getAudioChunkNumber() {
            return super._iChunkNumber;
        }

        public int getAudioChannel() {
            return (int)getAudioChunkNumber();
        }

        public int getAudioChunksInFrame() {
            return 2; // there are always only 2
        }
        
        public int getSectorType() {
            return SECTOR_AUDIO;
        }
        
        public String getTypeName() {
            return "FF9Audio";
        }

        public boolean isStereo() {
            return true;
        }

        public long getLeftSampleCount() {
            // if it's the 0th chunk, then it holds the left audio
            if (getAudioChunkNumber() == 0) 
                return getAudioDataSize();
            else
                return 0;
        }

        public long getRightSampleCount() {
            // if it's the 1th chunk, then it holds the right audio
            if (getAudioChunkNumber() == 1) 
                return getAudioDataSize();
            else
                return 0;
        }

        public boolean matchesPrevious(ISquareAudioSector oPrevSect) {
            if (!(oPrevSect instanceof PSXSectorFF9Audio))
                return false;

            PSXSectorFF9Audio oPrevFF9Aud = (PSXSectorFF9Audio) oPrevSect;

            if (getSamplesPerSecond() != oPrevFF9Aud.getSamplesPerSecond())
                return false;

            if (oPrevSect.getAudioChunkNumber() == 0) {
                if (getAudioChunkNumber() != 1 ||
                    oPrevSect.getFrameNumber() != getFrameNumber() ||
                    oPrevSect.getSectorNumber() + 1 != getSectorNumber())
                    return false;
            } else if (oPrevSect.getAudioChunkNumber() == 1) {
                if (getAudioChunkNumber() != 0 ||
                    oPrevSect.getFrameNumber() + 1 != getFrameNumber() ||
                    oPrevSect.getSectorNumber() + 9 != getSectorNumber())
                    return false;
            }

            return true;
        }

        public DiscItem createMedia(int iStartSector, long lngLeftSampleCount, long lngRightSampleCount, int iPeriod) {
            return new DiscItemSquareAudioStream(
                iStartSector, getSectorNumber(),
                lngLeftSampleCount, lngRightSampleCount,
                getSamplesPerSecond(), iPeriod - 1);
        }
     }
}
