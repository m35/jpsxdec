/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * CDXASector.java
 */

package jpsxdec.cdreaders;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Represents a single sector on a CD. */
public class CDXASector implements IGetFilePointer {
    /** Full raw sector: 2352. */
    public final static int SECTOR_RAW_AUDIO     = 2352;
    /** Raw sector without sync header: 2336. */
    public final static int SECTOR_MODE2         = 2336;
    /** Normal sector data size: 2048. */
    public final static int SECTOR_MODE1_OR_MODE2_FORM1 = 2048;
    /** XA Audio sector: 2324. */
    public final static int SECTOR_MODE2_FORM2   = 2324;
    
    /** Sync header. */
    public final static int SECTOR_SYNC_HEADER[] = 
                            new int[] {0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFF00};
        
    /** Represents a raw CD header that doesn't have a sync header. */
    private static class CDXAHeader {
        
        public final static int SIZE = 8;
        
        public int getSize() {
            return CDXAHeader.SIZE;
        }
        
        public int file_number;            // [1 byte] used to identify sectors 
                                           //          belonging to the same file
        public int channel;                // [1 byte] 0-15 for ADPCM audio
        public SubMode submode;            // [1 byte] 
        public CodingInfo coding_info;     // [1 byte]
        
        public int copy_of_file_number;    // [1 byte] =file_number
        public int copy_of_channel;        // [1 byte] =channel
        public int copy_of_submode;        // [1 byte] =submode
        public int copy_of_coding_info;    // [1 byte] =coding_info
        
        // Following the header are either [2324 bytes] 
        // or [2048 bytes] of user data (depending on the mode/form).
        // Following that are [4 bytes] Error Detection Code (EDC) 
        // or just 0x0000.
        // If the user data was 2048, then final [276 bytes] is error correction
        
        public CDXAHeader(InputStream oIS) 
                throws IOException, NotThisTypeException 
        {
            ReadHeader(oIS);
        }
        
        protected void ReadHeader(InputStream oIS) 
            throws IOException, NotThisTypeException
        {
            file_number = IO.ReadUInt8(oIS);
            channel     = IO.ReadUInt8(oIS);
            if (channel >= 32) 
                throw new NotThisTypeException("XA audio sector is corrupted.");

            submode     = new SubMode(IO.ReadUInt8(oIS));
            coding_info = new CodingInfo(IO.ReadUInt8(oIS));
            
            copy_of_file_number = IO.ReadUInt8(oIS);
            copy_of_channel     = IO.ReadUInt8(oIS);
            copy_of_submode     = IO.ReadUInt8(oIS);
            copy_of_coding_info = IO.ReadUInt8(oIS);
        }
        
        
        public static class SubMode {
            /** bit 7:  0 for all sectors except last sector of a file. */
            public final byte eof_marker;     
            
            /** bit 6:  1 for real time mode. */
            public final byte real_time;      
            
            /** bit 5:  1 for form 1, 2 or form 2 
             * (original bit 0 = form 1, 1 = form 2). */
            public final byte form;              
            
            /** bit 4:  used for application. */
            public final byte trigger;        
            
            /** bit 3:  1 could mean data or video data.
             *          Mutually exclusive with bits 2 and 1. */
            public final byte data;           
            
            /** bit 2:  1 for ADPCM sector.
             *          Mutually exclusive with bits 3 and 1. */
            public final byte audio;          
            
            /** bit 1:  1 for video sector.
             *          Mutually exclusive with bits 3 and 2. */
            public final byte video;          
            
            /** bit 0:  identifies end of audio frame */                                                
            public final byte end_audio;  
            
            SubMode(int b) {
                eof_marker    = (byte)( (b >> 7) & 1);
                real_time     = (byte)( (b >> 6) & 1);
                form          = (byte)(((b >> 5) & 1) + 1);
                trigger       = (byte)( (b >> 4) & 1);
                data          = (byte)( (b >> 3) & 1);
                audio         = (byte)( (b >> 2) & 1);
                video         = (byte)( (b >> 1) & 1);
                end_audio     = (byte)( (b     ) & 1);
            }
            
            public byte toByte() {
                return (byte)
                       ((eof_marker    << 7) |
                        (real_time     << 6) |
                        ((form - 1)    << 5) |
                        (trigger       << 4) |
                        (data          << 3) |
                        (audio         << 2) |
                        (video         << 1) |
                        (end_audio     ));
            }
            
            public String toString() {
                StringBuilder oSb = new StringBuilder(8);
                
                oSb.append(eof_marker    == 1 ? '1' : '0');
                oSb.append(real_time     == 1 ? '1' : '0');
                oSb.append(form          == 1 ? '0' : '1');
                oSb.append(trigger       == 1 ? '1' : '0');
                oSb.append(data          == 1 ? '1' : '0');
                oSb.append(audio         == 1 ? '1' : '0');
                oSb.append(video         == 1 ? '1' : '0');
                oSb.append(end_audio     == 1 ? '1' : '0');
                
                return oSb.toString();
            }
        }

        public static class CodingInfo {
            /** bit 7:    =0 ?  */
            public final boolean reserved;    
            
            /** bit 6:          */
            public final boolean emphasis;          
            
            /** bits 5,4: 00=4bits (B,C format)
             *            01=8bits */
            public final byte    bits_per_sample;   
            
            /** bits 3,2: 00=37.8kHz (A,B format) 
             *            01=18.9kHz */
            public final byte    sample_rate;       
            
            /** bits 1,0: 00=mono 01=stereo,
             *            other values reserved */
            public final byte    mono_stereo;       
            
            CodingInfo(int b) {
                reserved           = (b & 0x80) > 0;
                emphasis           = (b & 0x40) > 0;
                bits_per_sample = (byte)((b >> 4) & 3);
                sample_rate =     (byte)((b >> 2) & 3);
                mono_stereo =     (byte)((b >> 0) & 3);
            }
            
            public int toByte() {
               return 
                    (reserved    ? 0x80 : 0)     |
                    (emphasis    ? 0x40 : 0)     |
                    ((bits_per_sample & 3) << 4) |
                    ((sample_rate     & 3) << 2) |
                    ((mono_stereo     & 3) << 0);
            }
        }
    }
    
    /** Represents a raw CD header with a sync header. */
    private static class CDXAHeaderWithSync extends CDXAHeader {
        
        public final static int SIZE = 24;
        
        @Override
        public int getSize() {
            return CDXAHeaderWithSync.SIZE;
        }
        
        // sync header [12 bytes]
        public int SyncHeader1; // [4 bytes] 0x00FFFFFF
        public int SyncHeader2; // [4 bytes] 0xFFFFFFFF
        public int SyncHeader3; // [4 bytes] 0xFFFFFF00
        
        public int minutes;     // [1 byte] timecode relative to start of disk
        public int seconds;     // [1 byte] timecode relative to start of disk
        public int sectors;     // [1 byte] timecode relative to start of disk
        public int mode;        // [1 byte] Should always be Mode 2 for PSX data tracks
        
        public CDXAHeaderWithSync(InputStream oIS) 
                throws IOException, NotThisTypeException
        {
            super(oIS);
        }
        
        @Override
        protected void ReadHeader(InputStream oIS) 
                throws IOException, NotThisTypeException
        {
            if ((SyncHeader1 = IO.ReadInt16BE(oIS)) != SECTOR_SYNC_HEADER[0]) 
                throw new NotThisTypeException("Sector missing sync");
            if ((SyncHeader2 = IO.ReadInt16BE(oIS)) != SECTOR_SYNC_HEADER[1]) 
                throw new NotThisTypeException("Sector missing sync");
            if ((SyncHeader3 = IO.ReadInt16BE(oIS)) != SECTOR_SYNC_HEADER[2]) 
                throw new NotThisTypeException("Sector missing sync");
            
            minutes = IO.ReadUInt8(oIS);
            seconds = IO.ReadUInt8(oIS);
            sectors = IO.ReadUInt8(oIS);
            mode    = IO.ReadUInt8(oIS);
            
            super.ReadHeader(oIS);
        }
        
    }
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private CDXAHeader m_oHeader = null;
    private int m_iSector = -1;
    private long m_lngFilePointer;
    private byte[] m_abSectorBytes;
    private int m_iRawSectorSize;
    private int m_iUserDataStart;
    private int m_iUserDataSize;

    public CDXASector(int iRawSectorSize, byte[] abSectorBytes, 
                      int iSector, long lngFilePointer) 
            throws NotThisTypeException 
    {
        super();
        m_iSector = iSector;
        m_lngFilePointer = lngFilePointer;
        m_abSectorBytes = abSectorBytes;
        m_iRawSectorSize = iRawSectorSize;
        ByteArrayInputStream oBAIS = new ByteArrayInputStream(abSectorBytes);
        try {
            switch (m_iRawSectorSize) {
                case SECTOR_MODE1_OR_MODE2_FORM1:
                    // 2048
                    m_iUserDataStart = 0;
                    m_iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    break;
                case SECTOR_MODE2:
                    m_oHeader = new CDXAHeader(oBAIS);
                    m_iUserDataStart = CDXAHeader.SIZE;
                    if (m_oHeader.submode.form == 1)
                        m_iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    else
                        m_iUserDataSize = SECTOR_MODE2_FORM2;
                    break;
                case SECTOR_RAW_AUDIO:
                    m_oHeader = new CDXAHeaderWithSync(oBAIS);
                    m_iUserDataStart = CDXAHeaderWithSync.SIZE;
                    if (m_oHeader.submode.form == 1)
                        m_iUserDataSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    else
                        m_iUserDataSize = SECTOR_MODE2_FORM2;
                    break;
                default:
                    throw new RuntimeException("Should never happen: what kind of sector size is this?");
            }
        } catch (NotThisTypeException ex) {
            throw new NotThisTypeException("Sector " + iSector + " " + ex.getMessage());
        } catch (EOFException ex) {
            // if we don't even have enough sector data to get the header
            // then we don't have enough to make a sector
            m_oHeader = null;
        } catch (IOException ex) {
            throw new RuntimeException("this should never happen with a ByteArrayInputStream");
        }
    }

    /** Returns copy of the 'user data' portion of the sector. */
    public byte[] getSectorData() {
        return jpsxdec.util.Misc.copyOfRange(m_abSectorBytes, m_iUserDataStart, m_iUserDataSize);
    }
    
    /** Returns the size of the 'user data' portion of the sector. */
    public int getUserDataSize() {
        return m_iUserDataSize;
    }
    
    /** Returns an InputStream of the sector user data. */
    public ByteArrayFPIS getSectorDataStream() {
        return new ByteArrayFPIS(m_abSectorBytes, m_iUserDataStart, m_iUserDataSize, m_lngFilePointer);
    }
    
    /** Returns the entire raw sector data, with raw header/footer and 
     * everything that is has. */
    public byte[] getRawSectorData() {
        return m_abSectorBytes.clone();
    }

    //..........................................................................
    
    public int getFile() {
        if (m_oHeader != null) {
            return m_oHeader.file_number;
        } else {
            return -1;
        }
    }

    public int getChannel() {
        if (m_oHeader != null) {
            return m_oHeader.channel;
        } else {
            return -1;
        }
    }

    public int getSector() {
        // TODO: Would I rather return the sector number in the header (if available?)
        return m_iSector; 
    }

    //..........................................................................
    
    public CDXAHeader.SubMode getSubMode() {
        if (m_oHeader != null)
            return m_oHeader.submode;
        else
            return null;
    }

    //..........................................................................
    
    public CDXAHeader.CodingInfo getCodingInfo() {
        if (m_oHeader != null)
            return m_oHeader.coding_info;
        else
            return null;
    }

    public int getCodingInfo_BitsPerSample() {
        if (m_oHeader != null) {
            return m_oHeader.coding_info.bits_per_sample;
        } else {
            return -1;
        }
    }

    public int getCodingInfo_MonoStereo() {
        if (m_oHeader != null) {
            return m_oHeader.coding_info.mono_stereo;
        } else {
            return -1;
        }
    }

    public int getCodingInfo_SampleRate() {
        if (m_oHeader != null) {
            return m_oHeader.coding_info.sample_rate;
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
            return m_lngFilePointer;
        } else {
            return m_lngFilePointer + m_oHeader.getSize();
        }
    }
    
    public String toString() {
        return m_oHeader.toString();
    }
}
