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

package jpsxdec.cdreaders;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Represents a single sector on a CD. */
public class CDSector implements IGetFilePointer {

    private static final Logger log = Logger.getLogger(CDSector.class.getName());

    /** Full raw sector: 2352. */
    public final static int SECTOR_RAW_AUDIO     = 2352;
    /** Raw sector without sync header: 2336. */
    public final static int SECTOR_MODE2         = 2336;
    /** Normal sector data size: 2048. */
    public final static int SECTOR_MODE1_OR_MODE2_FORM1 = 2048;

    
    /** XA Audio sector: 2324. */
    public final static int SECTOR_MODE2_FORM2   = 2324;
    
    /** Sync header. */
    public final static int[] SECTOR_SYNC_HEADER_INT = new int[]
                                {0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFF00};

    /** Sync header. */
    public final static byte[] SECTOR_SYNC_HEADER = new byte[]
                            {(byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                             (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                             (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00};

    /** Represents a raw CD header without a sync header and sector header. */
    public static class CDXAHeader {

        public static final int SIZE = 8;

        public int getSize() {
            return CDXAHeader.SIZE;
        }
        
        private int file_number;            // [1 byte] used to identify sectors
                                            //          belonging to the same file
        public int getFileNumber() { return file_number; }
        private int channel;                // [1 byte] 0-15 for ADPCM audio
        public int getChannel() { return channel; }
        private SubMode submode;            // [1 byte]
        public SubMode getSubMode() { return submode; }
        private CodingInfo coding_info;     // [1 byte]
        public CodingInfo getCodingInfo() { return coding_info; }
        
        // Following the header are either [2324 bytes] 
        // or [2048 bytes] of user data (depending on the mode/form).
        // Following that are [4 bytes] Error Detection Code (EDC) 
        // or just 0x0000.
        // If the user data was 2048, then final [276 bytes] is error correction
        
        public CDXAHeader(InputStream oIS)
                throws IOException, NotThisTypeException
        {
            readHeader(oIS);
        }

        protected void readHeader(InputStream oIS)
            throws IOException, NotThisTypeException
        {
            file_number = IO.readUInt8(oIS);
            // Ace Combat 3 has several sectors with channel 255
            // They seem to be "null" sectors
            channel     = IO.readUInt8(oIS);
            submode     = new SubMode(IO.readUInt8(oIS));
            coding_info = new CodingInfo(IO.readUInt8(oIS));

            int b;
            if (file_number != (b = IO.readUInt8(oIS)))
                throw new NotThisTypeException(String.format("XA audio sector file number copy is corrupted: %d != %d", file_number, b));
            if (channel != (b = IO.readUInt8(oIS))) {
                String sWarn = String.format("XA audio sector channel copy is corrupted: %d != %d", channel, b);
                log.warning(sWarn);
                // Alundra 2 has the first channel correct, but the copy sometimes == 1 (unless my image is just bad)
                //throw new NotThisTypeException(sWarn);
            }
            if (submode.toByte() != (b = IO.readUInt8(oIS)))
                throw new NotThisTypeException(String.format("XA audio sector submode copy is corrupted: 0x%02x != 0x%02x", submode.toByte(), b));
            if (coding_info.toByte() != (b = IO.readUInt8(oIS)))
                throw new NotThisTypeException(String.format("XA audio sector coding_info copy is corrupted: 0x%02x != 0x%02x", coding_info.toByte(), b));
        }
        
        
        public static class SubMode {
            /** Sub-mode in its original bits. */
            private final int submode;

            /** bit 7:  0 for all sectors except last sector of a file. */
            private final boolean eof_marker;
            public boolean getEofMarker() { return eof_marker; }
            
            /** bit 6:  1 for real time mode. */
            private final boolean real_time;
            public boolean getRealTime() { return real_time; }
            
            /** bit 5:  0 = form 1, 1 = form 2. */
            private final int form;
            /** Form 1 or Form 2. */
            public int getForm() { return form; }
            
            /** bit 4:  used for application. */
            private final boolean trigger;
            public boolean getTrigger() { return trigger; }

            public static enum DATA_AUDIO_VIDEO {
                /** Sector contains data (or video). */ DATA,
                /** Sector contains ADPCM audio.     */ AUDIO,
                /** Sector contains video data.      */ VIDEO,
                /** What I call a "null" sector. Basically
                  * the contents are ignored.        */ NULL
            }
            /** bit 3:  1 could mean data or video data.
             *          Mutually exclusive with bits 2 and 1.
             *  bit 2:  1 for ADPCM sector.
             *          Mutually exclusive with bits 3 and 1.
             *  bit 1:  1 for video sector.
             *          Mutually exclusive with bits 3 and 2. */
            private final DATA_AUDIO_VIDEO data_audio_video;
            public DATA_AUDIO_VIDEO getDataAudioVideo() {
                return data_audio_video;
            }
            
            /** bit 0:  identifies end of audio frame */                                                
            private final boolean end_audio;
            public boolean getEndAudio() { return end_audio; }
            
            SubMode(int b) throws NotThisTypeException {
                submode = b;

                eof_marker    = (b & 0x80) > 0;
                real_time     = (b & 0x40) > 0;
                form          = ((b >> 5) & 1) + 1;
                trigger       = (b & 0x10) > 0;
                int iBits = (b >> 1) & 7;
                switch (iBits) {
                    case 4: data_audio_video = DATA_AUDIO_VIDEO.DATA; break;
                    case 2: data_audio_video = DATA_AUDIO_VIDEO.AUDIO; break;
                    case 1: data_audio_video = DATA_AUDIO_VIDEO.VIDEO; break;
                    case 0: data_audio_video = DATA_AUDIO_VIDEO.NULL; break;
                    default: throw new NotThisTypeException(
                            "CD sector submode data|audio|video is corrupted: " + Misc.bitsToString(iBits, 3) + "b");
                }
                end_audio     = (b & 1) > 0;
            }

            public int toByte() { return submode; }
            
            public String toString() { return Misc.bitsToString(submode, 8); }
        }

        public static class CodingInfo {
            /** Coding info in its original bits. */
            private final int codinginfo;

            /** bit 7:    =0 ?  */
            private final boolean reserved;
            public boolean getReserved() { return reserved; }

            /** bit 6:          */
            private final boolean emphasis;
            public boolean getEmphasis() { return emphasis; }

            /** bits 5,4: 00=4bits (B,C format)
             *            01=8bits */
            private final int     bits_per_sample;
            public int getBitsPerSample() { return bits_per_sample; }

            /** bits 3,2: 00=37.8kHz (A,B format) 
             *            01=18.9kHz */
            private final int     sample_rate;
            public int getSampleRate() { return sample_rate; }
            
            /** bits 1,0: 00=mono 01=stereo,
             *            other values reserved */
            private final boolean mono_stereo;
            public boolean isStereo() { return mono_stereo; }
            
            CodingInfo(int iByte) throws NotThisTypeException {
                codinginfo = iByte;

                reserved           = (iByte & 0x80) > 0;
                emphasis           = (iByte & 0x40) > 0;
                int iBits = (iByte >> 4) & 3;
                switch (iBits) {
                    case 0: bits_per_sample = 4; break;
                    case 1: bits_per_sample = 8; break;
                    default: throw new NotThisTypeException(
                            "CD sector coding info bits/sample is corrupted: " + iBits);
                }
                iBits = (iByte >> 2) & 3;
                switch (iBits) {
                    case 0: sample_rate = 37800; break;
                    case 1: sample_rate = 18900; break;
                    default: throw new NotThisTypeException(
                            "CD sector coding info sample rate is corrupted: " + iBits);
                }
                iBits = (iByte >> 0) & 3;
                switch (iBits) {
                    case 0: mono_stereo = false; break;
                    case 1: mono_stereo = true;  break;
                    default: throw new NotThisTypeException(
                            "CD sector coding info mono/stereo is corrupted: " + iBits);
                }

            }
            
            public int toByte() {
               return codinginfo;
            }

            public String toString() {
                return String.format("%s %d bits/sample %d samples/sec",
                                     mono_stereo ? "Stereo" : "Mono",
                                     bits_per_sample, sample_rate);
            }
        }

        public String toString() {
            return String.format("File.Channel:%d.%d Subcode:%s",
                        file_number, channel, submode.toString());
        }
    }
    
    /** Represents a raw CD header with a sync header and sector header. */
    public static class CDXAHeaderWithSync extends CDXAHeader {

        public static final int SIZE = 24;

        @Override
        public int getSize() {
            return CDXAHeaderWithSync.SIZE;
        }
        
        // sync header [12 bytes]
        // [4 bytes] 0x00FFFFFF
        // [4 bytes] 0xFFFFFFFF
        // [4 bytes] 0xFFFFFF00
        
        private int minutes;     // [1 byte] timecode relative to start of disk
        private int seconds;     // [1 byte] timecode relative to start of disk
        private int sectors;     // [1 byte] timecode relative to start of disk
        private int mode;        // [1 byte] Should always be Mode 2 for PSX data tracks
        
        public CDXAHeaderWithSync(InputStream oIS) 
                throws IOException, NotThisTypeException
        {
            super(oIS);
        }
        
        @Override
        protected void readHeader(InputStream oIS)
                throws IOException, NotThisTypeException
        {
            int iErrCount = (IO.readSInt32BE(oIS) != SECTOR_SYNC_HEADER_INT[0]) ? 1 : 0;
            iErrCount += (IO.readSInt32BE(oIS) != SECTOR_SYNC_HEADER_INT[1]) ? 1 : 0;
            iErrCount += (IO.readSInt32BE(oIS) != SECTOR_SYNC_HEADER_INT[2]) ? 1 : 0;
            if (iErrCount > 1) {
                throw new NotThisTypeException("Sector sync is missing/corrupted");
            } else if (iErrCount == 1) {
                log.warning("Sector sync is corrupted");
            }
            
            minutes = IO.readUInt8(oIS);
            seconds = IO.readUInt8(oIS);
            sectors = IO.readUInt8(oIS);
            mode    = IO.readUInt8(oIS);
            if (mode != 2) {
                //throw new NotThisTypeException("mode "+mode+" sector (should be Mode 2)");
            }
            
            super.readHeader(oIS);
        }
        
        public int calculateSectorNumber() {
            return   binaryCodedDecimalToInt(minutes) * 60 * 75
                   + binaryCodedDecimalToInt(seconds) * 75
                   + binaryCodedDecimalToInt(sectors)
                   - 150;
        }

        /** Converts Binary Coded Decimal (BCD) to integer. */
        private static int binaryCodedDecimalToInt(int i) {
            return ((i >> 4) & 0xf)*10 + (i & 0xf);
        }

        public String toString() {
            return String.format("(%d) %s",
                    calculateSectorNumber(), super.toString());
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    private CDXAHeader m_oHeader = null;
    private final int m_iSectorIndex;
    private final long _lngFilePointer;
    private final byte[] _abSectorBytes;
    private final int m_iRawSectorSize;
    private final int _iUserDataStart;
    private final int _iUserDataSize;


    public CDSector(int iRawSectorSize, byte[] abSectorBytes,
                    int iSectorIndex, long lngFilePointer)
            throws NotThisTypeException 
    {
        m_iSectorIndex = iSectorIndex;
        _lngFilePointer = lngFilePointer;
        _abSectorBytes = abSectorBytes;
        m_iRawSectorSize = iRawSectorSize;
        ByteArrayInputStream oBAIS = new ByteArrayInputStream(abSectorBytes);
        try {
            switch (m_iRawSectorSize) {
                case SECTOR_MODE1_OR_MODE2_FORM1:
                    // 2048
                    _iUserDataStart = 0;
                    _iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    break;
                case SECTOR_MODE2:
                    m_oHeader = new CDXAHeader(oBAIS);
                    _iUserDataStart = m_oHeader.getSize();
                    if (m_oHeader.getSubMode().getForm() == 1)
                        _iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    else
                        _iUserDataSize = SECTOR_MODE2_FORM2;
                    break;
                case SECTOR_RAW_AUDIO:
                    m_oHeader = new CDXAHeaderWithSync(oBAIS);
                    _iUserDataStart = m_oHeader.getSize();
                    if (m_oHeader.getSubMode().getForm() == 1)
                        _iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    else
                        _iUserDataSize = SECTOR_MODE2_FORM2;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid raw sector size.");
            }
        } catch (NotThisTypeException ex) {
            throw new NotThisTypeException("Sector " + iSectorIndex + " " + ex.getMessage());
        } catch (EOFException ex) {
            // if we don't even have enough sector data to get the header,
            // then we don't have enough to make a sector
            throw new NotThisTypeException("Sector " + iSectorIndex + " " + ex.getMessage());
        } catch (IOException ex) {
            throw new RuntimeException("This should never happen with a ByteArrayInputStream.");
        }
    }

    /** Returns the size of the 'user data' portion of the sector. */
    public int getCdUserDataSize() {
        return _iUserDataSize;
    }
    
    /** Returns copy of the 'user data' portion of the sector. */
    public byte[] getCdUserDataCopy() {
        return jpsxdec.util.Misc.copyOfRange(_abSectorBytes, _iUserDataStart, _iUserDataStart+_iUserDataSize);
    }

    public byte readUserDataByte(int i) {
        return _abSectorBytes[_iUserDataStart + 1];
    }

    public void getCdUserDataCopy(int iSourcePos, byte[] abOut, int iOutPos, int iLength) {
        System.arraycopy(_abSectorBytes, _iUserDataStart + iSourcePos, abOut, iOutPos, iLength);
    }
    
    /** Returns an InputStream of the 'user data' portion of the sector. */
    public ByteArrayFPIS getCDUserDataStream() {
        return new ByteArrayFPIS(_abSectorBytes, _iUserDataStart, _iUserDataSize, _lngFilePointer);
    }
    
    /** Returns direct reference to the underlying sector data, with raw
     * header/footer and everything it has. */
    public byte[] getRawSectorData() {
        return _abSectorBytes.clone();
    }

    //..........................................................................
    
    public int getFile() {
        if (m_oHeader != null) {
            return m_oHeader.getFileNumber();
        } else {
            return -1;
        }
    }

    public int getChannel() {
        if (m_oHeader != null) {
            return m_oHeader.getChannel();
        } else {
            return -1;
        }
    }

    /** @return The sector index from the start of the file. */
    public int getSectorNumberFromStart() {
        return m_iSectorIndex;
    }
    
    /** @return The sector number from the sector header, 
     *          or -1 if not available. */
    public int getHeaderSectorNumber() {
        if (m_oHeader instanceof CDXAHeaderWithSync) {
            return ((CDXAHeaderWithSync)m_oHeader).calculateSectorNumber();
        } else {
            // FIXME: Is -1 a valid sector number for a CD?
            return -1;
        }
    }

    //..........................................................................

    /** Returns the submode in the sector header, or null if it has no header. */
    public CDXAHeader.SubMode getSubMode() {
        if (m_oHeader != null)
            return m_oHeader.getSubMode();
        else
            return null;
    }

    //..........................................................................
    
    /** Returns the coding info in the sector header, or null if it has no header. */
    public CDXAHeader.CodingInfo getCodingInfo() {
        if (m_oHeader != null)
            return m_oHeader.getCodingInfo();
        else
            return null;
    }

    /** Returns the coding info.bits_per_sample in the sector header,
     *  or -1 if it has no header. */
    public int getCodingInfo_BitsPerSample() {
        if (m_oHeader != null) {
            return m_oHeader.getCodingInfo().getBitsPerSample();
        } else {
            return -1;
        }
    }

    /** Returns 0 for mono, 1 for stereo, or -1 if it has no header. */
    public int getCodingInfo_MonoStereo() {
        if (m_oHeader != null) {
            return m_oHeader.getCodingInfo().isStereo() ? 1 : 0;
        } else {
            return -1;
        }
    }

    /** Returns 37800 or 18900, or -1 if it has no header. */
    public int getCodingInfo_SampleRate() {
        if (m_oHeader != null) {
            return m_oHeader.getCodingInfo().getSampleRate();
        } else {
            return -1;
        }
    }

    //..........................................................................
    
    public boolean hasSectorHeader() {
        return m_oHeader != null;
    }

    //..........................................................................
    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of the sector userdata.
     *  [implements IGetFilePointer] */
    public long getFilePointer() {
        if (m_oHeader == null) {
            return _lngFilePointer;
        } else {
            return _lngFilePointer + m_oHeader.getSize();
        }
    }
    
    public String toString() {
        if (m_oHeader == null)
            return String.format("[Sector:%d]", m_iSectorIndex);
        else
            return String.format("[Sector:%d %s]",
                    m_iSectorIndex, m_oHeader.toString());
    }
}
