/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * PSXSector.java
 */

package jpsxdec.sectortypes;

import java.io.*;
import javax.sound.sampled.AudioFormat;
import jpsxdec.*;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IO;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.NotThisTypeException;

/** Base class for all PSX sector types. Encapsulates raw sectors with
 *  special meaning. Note that this doesn't store the whole underlying raw 
 *  sector data in order to save memory. */
public abstract class PSXSector extends InputStream implements IGetFilePointer {
    
    /** Interface that should be implemented by all video sector classes. 
     *  Used primarily in the demuxers. */
    public static interface IVideoChunkSector extends IGetFilePointer {
        long getChunkNumber();
        
        long getChunksInFrame();
        
        long getFrameNumber();
        
        long getHeight();
        
        long getWidth();
        
        public int read() throws IOException;
        
    }
    
    /** Identify the type of the supplied sector. */
    public static PSXSector SectorIdentifyFactory(CDXASector oCDSect) {
        if (oCDSect == null) return null;
        
        PSXSector oPsxSector;
        
        try {
            oPsxSector = new PSXSectorNull(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFrameChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorAudioChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF8FrameChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF8AudioChunk(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF9Video(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorFF9Audio(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        try {
            oPsxSector = new PSXSectorAudio2048(oCDSect);
            return oPsxSector;
        } catch (NotThisTypeException ex) {}
        
        // we dunno what this sector is, default to PSXSectorUnknownData
        oPsxSector = new PSXSectorUnknownData(oCDSect);
        return oPsxSector;
        
    }
    
    /**************************************************************************/
    /**************************************************************************/
    
    /** Save the basic sector info. */
    private long m_iFile = -1;
    /** Save the basic sector info. */
    private int m_iChannel = -1;
    /** Save the basic sector info. */
    private int m_iSector = -1;
    /** The original file offset where the sector was found. */
    private long m_lngSectorFilePointer;
    
    /** The 'user data' portion of the sector (without the raw headers). */
    private byte[] m_abUserData;
    /** Number of bytes read from the sector data (like ByteArrayInputStream). */
    private int m_iStreamPos = 0;
    /** Demuxed start position of the sector data (like ByteArrayInputStream). */
    private int m_iSectorDataStart;
    /** Size of the demux sector data. */
    private int m_iSectorDataLength;
    
    
    public PSXSector(CDXASector oCDSect) {
        m_iSector = oCDSect.getSector();
        m_iFile = oCDSect.getFile();
        m_iChannel = oCDSect.getChannel();
        m_lngSectorFilePointer = oCDSect.getFilePointer();
        m_abUserData = oCDSect.getSectorData();
        m_iSectorDataStart = getDemuxedDataStart(m_abUserData);
        m_iSectorDataLength = getDemuxedDataLength(m_abUserData);
    }
    
    /** Extend this function in sub-classes to define what bytes
     * are part of the demuxed stream. */
    abstract int getDemuxedDataStart(byte abUserData[]);
    /** Extend this function in sub-classes to define what bytes
     * are part of the demuxed stream. */
    abstract int getDemuxedDataLength(byte abUserData[]);
    
    public int getPsxUserDataSize() {
        return getDemuxedDataLength(m_abUserData); 
    }
   
    @Override /* [InputStream] */
    final public int read() throws IOException {
        if (m_iStreamPos < m_iSectorDataLength) {
            int iByte =
                    m_abUserData[m_iSectorDataStart + m_iStreamPos] & 0xff;
            m_iStreamPos++;
            return iByte;
        } else {
            return -1;
        }
    }
    
    /* [implements IGetFilePointer] */
    final public long getFilePointer() {
        return m_lngSectorFilePointer
                + m_iSectorDataStart
                + m_iStreamPos;
    }
    
    public int getSectorPos() {
        return m_iStreamPos;
    }
    /** Provides seeking capabilities. */
    public void setSectorPos(int iPos) {
        assert(iPos >= 0 && iPos <= m_iSectorDataLength);
        m_iStreamPos = iPos;
    }
    
    /** Returns a string description of the sector type, or null if 
     *  the sector contains unknown data */
    public String toString() {
        return String.format("[Sector:%d File:%d Channel:%d]",
                m_iSector, m_iFile, m_iChannel);
    }
    
    public long getFile() {
        return m_iFile;
    }
    
    public long getChannel() {
        return m_iChannel;
    }
    
    public int getSector() {
        return m_iSector;
    }
    
    
    /**************************************************************************/
    /**************************************************************************/
    
    /** XA files have lots of audio channels. When a channel is no longer used
     * because the audio is finished, it is filled with what I call
     * 'null' sectors. These sectors have absolutely no SubMode flags set. */
    public static class PSXSectorNull extends PSXSector {
        
        public PSXSectorNull(CDXASector oCD)
                throws NotThisTypeException 
        {
            super(oCD);
            if (!oCD.hasSectorHeader() || oCD.getSubMode().toByte() != 0) {
                throw new NotThisTypeException();
            }
            super.m_iChannel = -1;
        }
        
        public String toString() {
            return String.format("Null  [Sector:%d]", getSector());
        }
        
        int getDemuxedDataStart(byte[] abUserData) {
            return 0;
        }
        
        int getDemuxedDataLength(byte[] abUserData) {
            return 1; // just making the sector 1 byte long for no reason
        }
        
    }
    
    /** If all else fails, we don't know what kind of data it is */
    public static class PSXSectorUnknownData extends PSXSector {
        public PSXSectorUnknownData(CDXASector oCD) {
            super(oCD);
        }
        
        public String toString() {
            return null;
        }
        
        int getDemuxedDataStart(byte[] abUserData) {
            return 0;
        }
        
        int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length; 
        }
        
    }
    
    /** This is the header for standard v2 and v3 video frame chunk sectors.
     *  Also covers FF7 (v1) frame chunk sectors, and Lain frame chunk sectors. */
    public static class PSXSectorFrameChunk extends PSXSector implements IVideoChunkSector {
        
        // .. Static stuff .....................................................
        
        public static final int VIDEO_CHUNK_MAGIC = 0x60010180;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;
        
        // .. Instance .........................................................
        
        // * = has public getters
        protected long m_lngMagic;                //  0    [4 bytes - signed]
        protected long m_lngChunkNumber;          //* 4    [2 bytes]
        protected long m_lngChunksInThisFrame;    //* 6    [2 bytes]
        protected long m_lngFrameNumber;          //  8    [4 bytes]
        protected long m_lngChunkDuration;        //  12   [4 bytes] (frame duration?)
        protected long m_lngWidth;                //* 16   [2 bytes]
        protected long m_lngHeight;               //* 18   [2 bytes]
        protected long m_lngRunLengthCodeCount;   //  20   [2 bytes]
        protected long m_lngHeader3800;           //  22   [2 bytes] always 0x3800
        protected long m_lngQuantizationScale;    //  24   [2 bytes]
        protected long m_lngVersion;              //  26   [2 bytes]
        protected long m_lngFourZeros;            //  28   [4 bytes]
        //   32 TOTAL
        // .. Constructor .....................................................
        
        public PSXSectorFrameChunk(CDXASector oCD)
        throws NotThisTypeException {
            super(oCD);
            if (  oCD.hasSectorHeader() && 
                ( (oCD.getSubMode().video != 1) &&
                  (oCD.getSubMode().data  != 1) ) ) 
            {
                throw new NotThisTypeException();
            }
            try {
                
                ReadHeader(
                        new DataInputStream(new ByteArrayInputStream(oCD.getSectorData())));
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }
        
        protected void ReadHeader(DataInputStream oDIS)
                throws NotThisTypeException, IOException 
        {
            m_lngMagic = oDIS.readInt();
            if (m_lngMagic != VIDEO_CHUNK_MAGIC)
                throw new NotThisTypeException();
            
            m_lngChunkNumber = IO.ReadUInt16LE(oDIS);
            m_lngChunksInThisFrame = IO.ReadUInt16LE(oDIS);
            // Note that reading these 32 bit values could overflow to -1
            // if that should happen, then this couldn't be a frame chunk
            m_lngFrameNumber = IO.ReadUInt32LE(oDIS);
            if (m_lngFrameNumber == -1) // check for overflow
                throw new NotThisTypeException();
            
            // We aren't verifying this value because we also want to accept
            // FF7 frame chunk sectors.
            m_lngChunkDuration = IO.ReadUInt32LE(oDIS);
            //if (m_lngChunkDuration == -1) // check for overflow
            //    throw new NotThisTypeException();
            
            m_lngWidth = IO.ReadUInt16LE(oDIS);
            m_lngHeight = IO.ReadUInt16LE(oDIS);
            m_lngRunLengthCodeCount = IO.ReadUInt16LE(oDIS);
            
            // We aren't verifying this value because we also want to accept
            // FF7 frame chunk sectors.
            m_lngHeader3800 = IO.ReadUInt16LE(oDIS);
            //if (m_lngHeader3800 != 0x3800)
            //    throw new NotThisTypeException();
            
            m_lngQuantizationScale = IO.ReadUInt16LE(oDIS);
            m_lngVersion = IO.ReadUInt16LE(oDIS);
            m_lngFourZeros = IO.ReadUInt32LE(oDIS);
        }
        
        // .. Public functions .................................................
        
        public String toString() {
            return "Video " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %dx%d ver:%d " + 
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
                m_lngFrameNumber,
                m_lngChunkNumber,
                m_lngChunksInThisFrame,
                m_lngWidth,
                m_lngHeight,
                m_lngVersion,
                m_lngChunkDuration,
                m_lngRunLengthCodeCount,
                m_lngHeader3800,
                m_lngQuantizationScale,
                m_lngFourZeros
                );
        }
        
        public long getChunkNumber() {
            return m_lngChunkNumber;
        }
        
        public long getChunksInFrame() {
            return m_lngChunksInThisFrame;
        }
        
        public long getFrameNumber() {
            return m_lngFrameNumber;
        }
        
        public long getWidth() {
            return m_lngWidth;
        }
        
        public long getHeight() {
            return m_lngHeight;
        }
        
        /** Want to skip the frame header */
        protected int getDemuxedDataStart(byte[] abUserData) {
            return FRAME_CHUNK_HEADER_SIZE;
        }
        protected int getDemuxedDataLength(byte[] abUserData) {
            return 2048 - FRAME_CHUNK_HEADER_SIZE;
        }
        
    }
    
    
    /** Standard audio sector for STR for XA. */
    public static class PSXSectorAudioChunk extends PSXSector /*implements IAudioSector*/ {
        
        private int m_iSamplesPerSec;
        private int m_iBitsPerSample;
        /** 1 for mono, 2 for stereo. */
        private int m_iMonoStereo;
        
        public PSXSectorAudioChunk(CDXASector oCD) throws NotThisTypeException {
            super(oCD);
            
            if (!oCD.hasSectorHeader())
                throw new NotThisTypeException();
            if (oCD.getSubMode().form != 2)
                throw new NotThisTypeException();
            if (oCD.getSubMode().audio != 1)
                throw new NotThisTypeException();
            
            switch(oCD.getCodingInfo_SampleRate()) {
                case 0: m_iSamplesPerSec = 37800; break;
                case 1: m_iSamplesPerSec = 18900; break;
                default: throw new NotThisTypeException();
            }
            switch(oCD.getCodingInfo_BitsPerSample()) {
                case 0: m_iBitsPerSample = 4; break;
                case 1: m_iBitsPerSample = 8; break;
                default: throw new NotThisTypeException();
            }
            switch(oCD.getCodingInfo_MonoStereo()) {
                case 0: m_iMonoStereo = 1; break; // mono (1 channel)
                case 1: m_iMonoStereo = 2; break; // stereo (2 channels)
                default: throw new NotThisTypeException();
            }
        }
        
        public String toString() {
            return "Audio " + super.toString() +
                    String.format(
                    " bits/sample:%d channels:%d samples/sec:%d",
                    m_iBitsPerSample,
                    m_iMonoStereo,
                    m_iSamplesPerSec);
        }
        
        /* [implements IAudioSector] */
        public boolean matches(Object o) {
            if ( this == o ) return true;
            if ( !(o instanceof PSXSectorAudioChunk) ) return false;
            PSXSectorAudioChunk oAudChnk = (PSXSectorAudioChunk)o;
            return
                    (this.m_iBitsPerSample == oAudChnk.m_iBitsPerSample &&
                    this.m_iMonoStereo == oAudChnk.m_iMonoStereo &&
                    this.m_iSamplesPerSec == oAudChnk.m_iSamplesPerSec);
        }
        
        /* [implements IAudioSector] */
        public int getSamplesPerSecond() {
            return m_iSamplesPerSec;
        }
        
        /* [implements IAudioSector] */
        public int getBitsPerSample() {
            return m_iBitsPerSample;
        }
        
        /* [implements IAudioSector] */
        public int getMonoStereo() {
            return m_iMonoStereo;
        }
        
        public AudioFormat getAudioFormat() {
            return new AudioFormat(
                    m_iSamplesPerSec,
                    16,        // we're turning it into a 16 bit signed pcm
                    m_iMonoStereo, // stereo?
                    true,      // signed
                    false);    // little-endian (not big-endian)            
        }
        
        /* [extends PSXSector] */
        protected int getDemuxedDataStart(byte abUserData[]) {
            return 0;
        }
        /** The last 20 bytes of the sector are unused. */
        protected int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length - 20;
        }
        
        /** Returns the length of the sector in samples (i.e. 4032 / channels) */
        public long getSampleLength() {
            return 4032 / m_iMonoStereo;
        }
    }
    
    
    /** By far the slowest at identifying the sector type. For ISO files only. */
    public static class PSXSectorAudio2048 extends PSXSector {
        
        int m_iBitsPerSample = -1;
        
        public PSXSectorAudio2048(CDXASector oCD)
                throws NotThisTypeException 
        {
            super(oCD);
            if (oCD.hasSectorHeader()) throw new NotThisTypeException();
            
            byte abSect[] = oCD.getSectorData();
            DataInputStream oDIS =
                    new DataInputStream(new ByteArrayInputStream(abSect));
            int aiSndParams[] = new int[4];
            
            for (int i = 0; i < 16; i++) {
                try {
                    aiSndParams[0] = oDIS.readInt();
                    aiSndParams[1] = oDIS.readInt();
                    aiSndParams[2] = oDIS.readInt();
                    aiSndParams[3] = oDIS.readInt();
                } catch (IOException ex) {
                    // i guess only if the sector data isn't long enough
                    // not that it really matters for this type of sector
                    throw new NotThisTypeException();
                }
                
                // if all the bytes are zero, then this isn't audio
                /* umm...wait...why not?
                if (aiSndParams[0] == 0 && aiSndParams[1] == 0 &&
                        aiSndParams[2] == 0 && aiSndParams[3] == 0) {
                    throw new NotThisTypeException();
                }
                */
                // if we don't know the bits/sample yet
                if (m_iBitsPerSample < 0) {
                    // parameter bytes for 4 bits/sample
                    if (aiSndParams[0] == aiSndParams[1] &&
                            aiSndParams[2] == aiSndParams[3]) {
                        // however, only if all 4 ints are not equal can we be sure
                        // (if they're equal then it could be either 4 or 8 bps)
                        if (aiSndParams[0] != aiSndParams[2])
                            m_iBitsPerSample = 4;
                        
                    }
                    // parameter bytes for 8 bits/sample
                    else if (aiSndParams[0] == aiSndParams[2] &&
                            aiSndParams[1] == aiSndParams[3]) {
                        m_iBitsPerSample = 8;
                    } else
                        throw new NotThisTypeException();
                    
                }
                // if it's 4 bits/sample and the parameters don't fit the pattern
                else if (m_iBitsPerSample == 4 &&
                        (aiSndParams[0] != aiSndParams[1] ||
                        aiSndParams[2] != aiSndParams[3])) {
                    throw new NotThisTypeException();
                }
                // if it's 8 bits/sample and the parameters don't fit the pattern
                else if (m_iBitsPerSample == 8 &&
                        (aiSndParams[0] != aiSndParams[2] ||
                        aiSndParams[1] != aiSndParams[3])) {
                    throw new NotThisTypeException();
                }
                try {
                    oDIS.skip(128 - 16);
                } catch (IOException ex) {
                    // i guess only if the sector data isn't long enough
                    throw new NotThisTypeException();
                }
            } // for
            
            // if it made it this far, then there is a very good chance
            // this is an audio sector
            
            // At this point we will probably know the bits/sample.
            // If this is a movie, we can also narrow down the samples/sec 
            // & mono/stereo to at most 2 choices by seeing how often the STR
            // has audio data. 
            // But for XA, there's absolutely no way we can figure out if there 
            // are 32 audio channels or fewer.
            
            // At 4 bits/sample
            // If it is every 32 sectors, then it must be mono @ 18900 samples/sec
            // If it is every 16 sectors, then it must be either
            //                               stereo @ 18900 samples/sec
            //                              or mono @ 37800 samples/sec
            // If it is every 8 sectors, then it must be stereo @ 37800 samples/sec
            
            // At 8 bits/sample
            // It could never be 32 sectors
            // If it is every 16 sectors, then it must be mono @ 18900 samples/sec
            // If it is every 8 sectors, then it must be either
            //                                  stereo @ 18900 samples/sec
            //                                 or mono @ 37800 samples/sec
            // If it is every 4 sectors, then it must be stereo @ 37800 samples/sec
            
            // I'm believe the above is correct, but I haven't really checked.
            // Again, not that any of this really matters because we can't
            // decode the audio anyway.
        }
        
        public String toString() {
            return "Audio2048 " + super.toString() +
                    String.format(
                    " bits/sample:%d channels:? samples/sec:?",
                    m_iBitsPerSample);
        }
        
        int getDemuxedDataStart(byte[] abUserData) {
            return 0;
        }
        
        int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length; // should be 2048
        }
    }
    
    
    /** Base class for FF8 movie (audio/video) sectors. */
    public static abstract class PSXSectorFF8Abstract extends PSXSector {
        
        public static final int SIZE = 8;
        
        protected final char achHead[] = new char[4]; // [4 bytes] "SM*\1"
        protected int        iSectorNumber;           // [1 byte]
        protected int        iSectorsInAVFrame;       // [1 byte]
        protected long       lngFrameNumber;          // [2 bytes]
        
        public PSXSectorFF8Abstract(CDXASector oCD) throws NotThisTypeException 
        {
            super(oCD);
            // it always seems to be flagged as data (even audio sectors)
            if (oCD.hasSectorHeader() && oCD.getSubMode().data != 1)
                throw new NotThisTypeException();
            try {
                
                ReadHeader(
                        new DataInputStream(new ByteArrayInputStream(oCD.getSectorData())));
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }
        
        private void ReadHeader(DataInputStream oDIS)
                throws IOException, NotThisTypeException 
        {
            char c;
            c = (char)oDIS.readByte();
            if ((achHead[0] = c) != 'S') throw new NotThisTypeException();
            c = (char)oDIS.readByte();
            if ((achHead[1] = c) != 'M') throw new NotThisTypeException();
            c = (char)oDIS.readByte();
            achHead[2] = c; // 'J' or video, 'N' left audio and 'R' for right audio
            c = (char)oDIS.readByte();
            if ((achHead[3] = c) != '\1') throw new NotThisTypeException();
            // There appear to be 10 sectors for every frame
            // First two sectors are audio (left, right channels)
            // Then the remaining 8 sectors is the video frame, always
            // 320x224.
            iSectorNumber = oDIS.readUnsignedByte(); // 0 to iSectorsInAVFrame
            iSectorsInAVFrame = oDIS.readUnsignedByte(); // always 9?
            if (iSectorsInAVFrame == 0) throw new NotThisTypeException();
            lngFrameNumber = IO.ReadUInt16LE(oDIS); // starts @ 0
        }
        
        public long getFrameNumber() {
            return lngFrameNumber;
        }
        
        public long getFF8ChunkNumber() {
            return iSectorNumber;
        }
        
    }
    
    /** FF8 video sector. */
    public static class PSXSectorFF8FrameChunk
            extends PSXSectorFF8Abstract
            implements IVideoChunkSector 
    {
        
        
        public PSXSectorFF8FrameChunk(CDXASector oCD)
                throws NotThisTypeException 
        {
            super(oCD);
            if (achHead[2] != 'J') throw new NotThisTypeException();
        }
        
        /** [implements IVideoSector] */
        public long getWidth() {
            return 320;
        }
        
        /** [implements IVideoSector] */
        public long getHeight() {
            return 224;
        }
        
        /** [implements IVideoSector] */
        public long getChunkNumber() {
            return iSectorNumber+2;
        }
        
        /** [implements IVideoSector] */
        public long getChunksInFrame() {
            return iSectorsInAVFrame-1;
        }
        
        
        /** Want to skip the header bytes.
         * [extends PSXSector] */
        protected int getDemuxedDataStart(byte abUserData[]) {
            return PSXSectorFF8Abstract.SIZE;
        }
        /** [extends PSXSector] */
        protected int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length - PSXSectorFF8Abstract.SIZE;
        }
        
        public String toString() {
            return "VideoFF8 " + super.toString() +
                    String.format(
                    " frame:%d chunk:%d/%d %dx%d",
                    lngFrameNumber,
                    iSectorNumber,
                    iSectorsInAVFrame,
                    320, 224);
        }
    }
    
    /** FF8 audio sector. */
    public static class PSXSectorFF8AudioChunk 
            extends PSXSectorFF8Abstract
            implements IFF8or9AudioSector
    {
        private int m_iChannel;
        
        public PSXSectorFF8AudioChunk(CDXASector oCD)
                throws NotThisTypeException 
        {
            super(oCD);
            switch (achHead[2]) {
                case 'N':
                    m_iChannel = 1;
                    break;
                case 'R':
                    m_iChannel = 2;
                    break;
                default:
                    throw new NotThisTypeException();
            }
        }
        
        public int getSamplesPerSecond() {
            return 44100;
        }
        
        public int getBitsPerSample() {
            return 4;
        }
        
        /** @return 1 for left channel, 2 for right channel */
        public int isLeftOrRightChannel() {
            return m_iChannel;
        }
        
        /** Want to skip the header bytes. 
         *  [extends PSXSector] */
        protected int getDemuxedDataStart(byte abUserData[]) {
            // audio data starts at 360 past the header
            return PSXSectorFF8Abstract.SIZE + 360;
        }
        /** [extends PSXSector] */
        protected int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length - (PSXSectorFF8Abstract.SIZE + 360);
        }
        
        public String toString() {
            return "AudioFF8 " + super.toString() +
                    String.format(
                    " frame:%d chunk:%d/%d channel:%d (bits/sample:4 channels:1 samples/sec:44100)",
                    super.lngFrameNumber, 
                    super.iSectorNumber,
                    super.iSectorsInAVFrame,
                    m_iChannel);
        }
        
        public long getSampleLength() {
            return 2940 / 2;
        }
    }
    
    
    public abstract static class PSXSectorFF9Abstract extends PSXSector {
        
        protected long m_lngMagic;                //  0    [4 bytes]
        protected long m_lngChunkNumber;          //* 4    [2 bytes]
        protected long m_lngChunksInThisFrame;    //* 6    [2 bytes]
        protected long m_lngFrameNumber;          //  8    [4 bytes]
        
        protected PSXSectorFF9Abstract(CDXASector oCDSect, int iForm) throws NotThisTypeException {
            super(oCDSect);
            
            if (!oCDSect.hasSectorHeader()) throw new NotThisTypeException();
            if (oCDSect.getSubMode().data != 1) throw new NotThisTypeException();
            if (oCDSect.getSubMode().form != iForm) throw new NotThisTypeException();
            
            try {
                
                ReadHeader(oCDSect.getSectorDataStream());
                
            } catch (IOException ex) {
                throw new NotThisTypeException();
            }
        }
        
        protected void ReadHeader(InputStream oIS) throws IOException, NotThisTypeException {
            
            m_lngMagic = IO.ReadUInt32LE(oIS);
            m_lngChunkNumber = IO.ReadUInt16LE(oIS);
            m_lngChunksInThisFrame = IO.ReadUInt16LE(oIS);
            // Note that reading these 32 bit values could overflow to -1
            // if that should happen, then this couldn't be a frame chunk
            m_lngFrameNumber = IO.ReadUInt32LE(oIS);
            if (m_lngFrameNumber == -1) // check for overflow
                throw new NotThisTypeException();
        }
        
        public long getFF9ChunkNumber() {
            return m_lngChunkNumber;
        }
        
        public long getChunksInFrame() {
            return m_lngChunksInThisFrame;
        }
        
        public long getFrameNumber() {
            return m_lngFrameNumber;
        }
        
    }
    
    public static class PSXSectorFF9Video extends PSXSectorFF9Abstract {
        
        // .. Static stuff .....................................................
        
        public static final int VIDEO_CHUNK_MAGIC = 0x00040160;
        public static final int FRAME_CHUNK_HEADER_SIZE = 32;
        
        // .. Instance .........................................................
        
        /*
        protected long m_lngMagic;                //  0    [4 bytes]
        protected long m_lngChunkNumber;          //  4    [2 bytes]
        protected long m_lngChunksInThisFrame;    //  6    [2 bytes]
        protected long m_lngFrameNumber;          //  8    [4 bytes]
        */
        protected long m_lngChunkDuration;        //  12   [4 bytes] (frame duration?)
        protected long m_lngWidth;                //  16   [2 bytes]
        protected long m_lngHeight;               //  18   [2 bytes]
        protected long m_lngRunLengthCodeCount;   //  20   [2 bytes]
        protected long m_lngHeader3800;           //  22   [2 bytes] always 0x3800
        protected long m_lngQuantizationScale;    //  24   [2 bytes]
        protected long m_lngVersion;              //  26   [2 bytes]
        protected long m_lngFourZeros;            //  28   [4 bytes]
        //   32 TOTAL
        
        public PSXSectorFF9Video(CDXASector oCDSect) throws NotThisTypeException {
            super(oCDSect, 2); // will call ReadHeader()
        }
        
        @Override
        protected void ReadHeader(InputStream oIS) throws IOException, NotThisTypeException {
            super.ReadHeader(oIS);

            // first make sure the magic nubmer is correct
            if (m_lngMagic != VIDEO_CHUNK_MAGIC)
                throw new NotThisTypeException();

            // then continue reading the header
            
            m_lngChunkDuration = IO.ReadUInt32LE(oIS);
            if (m_lngChunkDuration == -1) // check for overflow
                throw new NotThisTypeException();

            m_lngWidth = IO.ReadUInt16LE(oIS);
            m_lngHeight = IO.ReadUInt16LE(oIS);
            m_lngRunLengthCodeCount = IO.ReadUInt16LE(oIS);

            m_lngHeader3800 = IO.ReadUInt16LE(oIS);
            if (m_lngHeader3800 != 0x3800)
                throw new NotThisTypeException();

            m_lngQuantizationScale = IO.ReadUInt16LE(oIS);
            m_lngVersion = IO.ReadUInt16LE(oIS);
            if (m_lngVersion != 2)
                throw new NotThisTypeException();
            m_lngFourZeros = IO.ReadUInt32LE(oIS);
            // the 4 zeros aren't always zero
            //if (m_lngFourZeros != 0)
            //    throw new NotThisTypeException();
        }
        
        // .. Public functions .................................................
        
        public String toString() {
            return "FF9Vid " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d %dx%d ver:%d " + 
                "{dur=%d rlc=%d 3800=%04x qscale=%d 4*00=%08x}",
                m_lngFrameNumber,
                m_lngChunkNumber,
                m_lngChunksInThisFrame,
                m_lngWidth,
                m_lngHeight,
                m_lngVersion,
                m_lngChunkDuration,
                m_lngRunLengthCodeCount,
                m_lngHeader3800,
                m_lngQuantizationScale,
                m_lngFourZeros
                );
        }
        
        public long getWidth() {
            return m_lngWidth;
        }
        
        public long getHeight() {
            return m_lngHeight;
        }
        
        protected int getDemuxedDataStart(byte abUserData[]) {
            // skipping the frame header
            return FRAME_CHUNK_HEADER_SIZE;
        }
        protected int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length - FRAME_CHUNK_HEADER_SIZE;
        }
        
    }
    
    public static class PSXSectorFF9Audio 
            extends PSXSectorFF9Abstract 
            implements IFF8or9AudioSector
    {
        
        // .. Static stuff .....................................................
        
        public static final int FF9_AUDIO_CHUNK_MAGIC = 0x00080160;
        public static final int FRAME_AUDIO_CHUNK_HEADER_SIZE = 208;
        
        // .. Instance .........................................................
        
        /*
        private long m_lngMagic;                //  0    [4 bytes]
        private long m_lngChunkNumber;          //  4    [2 bytes]
        private long m_lngChunksInThisFrame;    //  6    [2 bytes]
        private long m_lngFrameNumber;          //  8    [4 bytes]
        */
        // 116 bytes unknown
        private long m_lngAKAOor0;              //       [4 bytes]
        private long m_lngFrameNumSub1or0;      //       [4 bytes]
        // 72 bytes unknown
        //   208 TOTAL
        
        public PSXSectorFF9Audio(CDXASector oCD) throws NotThisTypeException {
            super(oCD, 1); // calls ReadHeader()
        }
        
        @Override
        protected void ReadHeader(InputStream oIS) throws IOException, NotThisTypeException {
            super.ReadHeader(oIS);

            // first make sure the magic nubmer is correct
            if (m_lngMagic != FF9_AUDIO_CHUNK_MAGIC)
                throw new NotThisTypeException();

            oIS.skip(116);

            m_lngAKAOor0 = IO.ReadUInt32LE(oIS);
            m_lngFrameNumSub1or0 = IO.ReadUInt32LE(oIS);

            oIS.skip(72);
                
        }
        
        // .. Public functions .................................................
        
        public String toString() {
            return "FF9Aud " + super.toString() +
                String.format(
                " frame:%d chunk:%d/%d ?:%04x frame-1:%d",
                m_lngFrameNumber,
                m_lngChunkNumber,
                m_lngChunksInThisFrame,
                m_lngAKAOor0,
                m_lngFrameNumSub1or0
                );
        }
        
        /** Want to skip the frame header */
        protected int getDemuxedDataStart(byte abUserData[]) {
            return FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }
        protected int getDemuxedDataLength(byte[] abUserData) {
            return abUserData.length - FRAME_AUDIO_CHUNK_HEADER_SIZE;
        }
        
        /** @return 1 for left channel, 2 for right channel */
        public int isLeftOrRightChannel() {
            return (int)m_lngChunkNumber + 1;
        }
    }
    
    // TODO: Remove this when we get FF9 audio working
    public static interface IFF8or9AudioSector {
        int isLeftOrRightChannel();
    }
}


