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
 * CDSectorReader.java
 */

package jpsxdec;

import java.io.*;
import java.util.*;
import java.util.NoSuchElementException;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.NotThisTypeException;

/** This class encapsulates the reading of a CD. 
The term "CD" can mean an actual CD (not implemented yet), a CD image 
(BIN/CUE, ISO), or a file containing some sectors of a CD. Essentially 
they are all the same. This class does its best to guess what type of
file it is. 
*/
public class CDSectorReader {
    
    
    /** Full raw sector: 2352. */
    final static int SECTOR_RAW_AUDIO     = 2352;
    /** Raw sector without sync header: 2336. */
    final static int SECTOR_MODE2         = 2336;
    /** Normal sector data size: 2048. */
    final static int SECTOR_MODE1_OR_MODE2_FORM1 = 2048;
    /** XA Audio sector: 2324. */
    final static int SECTOR_MODE2_FORM2   = 2324;
    
    /*
    The different Modes that can exist on CD :

    Audio         (2352 bytes / block User Data, 2352 Bytes / block Raw data)
    Mode 1        (2048 bytes / block User Data, 2352 Bytes / block Raw data)
    Mode 2        (2336 bytes / block User Data, 2352 Bytes / block Raw data)
    Mode 2 Form 1 (2048 bytes / block User Data, 2352 Bytes / block Raw data)
    Mode 2 Form 2 (2324 bytes / block User Data, 2352 Bytes / block Raw data) 
     
    http://www.smart-projects.net/help.php?help=190
    */
    
    public static class CDXASector implements IGetFilePointer {
        CDXAHeader m_oHeader = null;
        int m_iSector = -1;
        long m_lngFilePointer;
        byte[] m_abSectorBytes;
        int m_iRawSectorSize;
        
        public CDXASector(int iRawSectorSize, byte[] abSectorBytes, int iSector, long lngFilePointer) 
                throws NotThisTypeException
        {
            m_iSector = iSector;
            m_lngFilePointer = lngFilePointer;
            m_abSectorBytes = abSectorBytes;
            m_iRawSectorSize = iRawSectorSize;
            
            DataInputStream oDIS = new DataInputStream(new ByteArrayInputStream(abSectorBytes));
            try {
                
                switch (iRawSectorSize) {
                    case SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                        break;
                    case SECTOR_MODE2:         // 2336
                        m_oHeader = new CDXAHeader(oDIS);
                        break;
                    case SECTOR_RAW_AUDIO:     // 2352
                        m_oHeader = new CDXAHeaderWithSync(oDIS);
                        break;
                    default: 
                        assert(false); // what kind of sector size is this?
                }
            } catch (NotThisTypeException ex) {
                throw new NotThisTypeException("Sector " + iSector + " " + ex.getMessage());
            } catch (EOFException ex) {
                // if we don't even have enough sector data to get the header
                // then we don't have enough to make a sector
                m_oHeader = null;
            } catch (IOException ex) {
                // this should never happen with a ByteArrayInputStream
                throw new RuntimeException("this should never happen with a ByteArrayInputStream");
            }
            
       }
    
        public byte[] getSectorData() {

            switch (m_iRawSectorSize) {
                case SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                    return m_abSectorBytes.clone();
                case SECTOR_MODE2:         // 2336
                    if (m_oHeader.submode.form == 2) 
                    {   // we're assuming mode == 2 here
                        return Arrays.copyOfRange(m_abSectorBytes, 
                                                  CDXAHeader.SIZE, 
                                                  CDXAHeader.SIZE + SECTOR_MODE2_FORM2);
                    } else {
                        return Arrays.copyOfRange(m_abSectorBytes, 
                                                  CDXAHeader.SIZE, 
                                                  CDXAHeader.SIZE + SECTOR_MODE1_OR_MODE2_FORM1);
                    }
                case SECTOR_RAW_AUDIO:     // 2352
                    if ((((CDXAHeaderWithSync) m_oHeader).mode == 2) &&
                        m_oHeader.submode.form == 2)
                    {
                        return Arrays.copyOfRange(m_abSectorBytes, 
                                                  CDXAHeaderWithSync.SIZE, 
                                                  CDXAHeaderWithSync.SIZE + SECTOR_MODE2_FORM2);
                    } else {
                        return Arrays.copyOfRange(m_abSectorBytes, 
                                                  CDXAHeaderWithSync.SIZE, 
                                                  CDXAHeaderWithSync.SIZE + SECTOR_MODE1_OR_MODE2_FORM1);
                    }
                default: 
                    assert(true); // mysterious sector size
                    return null;
            }
        }
        //..........................................................................

        public int getFile() {
            if (m_oHeader != null)
                return m_oHeader.file_number;
            else
                return -1;
        }
        public int getChannel() {
            if (m_oHeader != null)
                return m_oHeader.channel;
            else
                return -1;
        }
        int getSector() {
            return m_iSector;
        }
    
        //..........................................................................

        int getSubMode() {
            if (m_oHeader != null)
                return m_oHeader.submode.ToByte();
            else
                return -1; 
        }
        public int getSubMode_Audio() {
            if (m_oHeader != null)
                return m_oHeader.submode.audio ? 1 : 0;
            else
                return -1; 
        }
        public int getSubMode_Video() {
            if (m_oHeader != null)
                return m_oHeader.submode.video ? 1 : 0;
            else
                return -1; 
        }
        public int getSubMode_Data() {
            if (m_oHeader != null)
                return m_oHeader.submode.data ? 1 : 0;
            else
                return -1; 
        }
        public int getSubMode_RealTime() {
            if (m_oHeader != null)
                return m_oHeader.submode.real_time ? 1 : 0;
            else
                return -1;
        }
        public int getSubMode_EOFMarker() {
            if (m_oHeader != null)
                return m_oHeader.submode.eof_marker ? 1 : 0;
            else
                return -1;
        }

        public int getSubMode_Form() {
            if (m_oHeader != null)
                return m_oHeader.submode.form;
            else
                return -1;
        }

        //..........................................................................

        public int getCodingInfo() {
            if (m_oHeader != null) {
                return m_oHeader.coding_info.ToByte();
            } else {
                return -1;
            }
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

        boolean HasSectorHeader() {
            return m_oHeader != null;
        }
        
        //..........................................................................
        /** Returns the actual offset in bytes from the start of the file/CD 
         *  to the start of the sector userdata.
         *  implements util.IGetFilePointer */
        public long getFilePointer() {
            if (m_oHeader == null)
                return m_lngFilePointer;
            else
                return m_lngFilePointer + m_oHeader.getSize();
        }

    }
    
    private static class CDXAHeader {
        
        public final static int SIZE = 8;
        
        public int getSize() {
            return CDXAHeader.SIZE;
        }
        
        public int file_number;          // [1 byte] used to identify sectors 
                                         //          belonging to the same file
        public int channel;                // [1 byte] 0-15 for ADPCM audio
        public submode_t submode;          // [1 byte] 
        public coding_info_t coding_info;  // [1 byte]
        
        public int copy_of_file_number;    // [1 byte] =file_number
        public int copy_of_channel;        // [1 byte] =channel
        public int copy_of_submode;        // [1 byte] =submode
        public int copy_of_coding_info;    // [1 byte] =coding_info
        
        // Following the header are either [2324 bytes] 
        // or [2048 bytes] of user data (depending on the mode/form).
        // Following that are [4 bytes] Error Detection Code (EDC) 
        // or just 0x0000.
        // If the user data was 2048, then final [276 bytes] is error correction
        
        public CDXAHeader(DataInputStream oDIS) 
                throws IOException, NotThisTypeException 
        {
            ReadHeader(oDIS);
        }
        
        protected void ReadHeader(DataInputStream oDIS) 
            throws IOException, NotThisTypeException
        {
            file_number = oDIS.readUnsignedByte();
            channel     = oDIS.readUnsignedByte();

            submode     = new submode_t(oDIS.readUnsignedByte());
            coding_info = new coding_info_t(oDIS.readUnsignedByte());
            
            copy_of_file_number = oDIS.readUnsignedByte();
            copy_of_channel     = oDIS.readUnsignedByte();
            copy_of_submode     = oDIS.readUnsignedByte();
            copy_of_coding_info = oDIS.readUnsignedByte();
        }
        
        
        private static class submode_t {
            boolean eof_marker;     // bit 7:  0 for all sectors except 
                                    //         last sector of file
            boolean real_time;      // bit 6:  1 for real time mode
            byte form;              // bit 5:  0: form 1, 1: form 2
            boolean trigger;        // bit 4:  used for application
            boolean data;           // bit 3:  dependant on sector type, 
                                    //         0 for ADPCM sector
            boolean audio;          // bit 2:  dependant on sector type, 
                                    //         1 for ADPCM sector
            boolean video;          // bit 1:  dependant on sector type, 
                                    //         0 for ADPCM sector
            boolean end_of_record;  // bit 0:  identifies end of audio frame
            
            public submode_t(int b) {
                eof_marker    = (b & 0x80) > 0;
                real_time     = (b & 0x40) > 0;
                form          = (b & 0x20) == 0 ? (byte)1 : (byte)2;
                trigger       = (b & 0x10) > 0;
                data          = (b & 0x08) > 0;
                audio         = (b & 0x04) > 0;
                video         = (b & 0x02) > 0;
                end_of_record = (b & 0x01) > 0;
            }
            
            public int ToByte() {
                return (eof_marker    ? 0x80 : 0) |
                       (real_time     ? 0x40 : 0) |
                       (form == 2     ? 0x20 : 0) |
                       (trigger       ? 0x10 : 0) |
                       (data          ? 0x08 : 0) |
                       (audio         ? 0x04 : 0) |
                       (video         ? 0x02 : 0) |
                       (end_of_record ? 0x01 : 0);
            }
        }

        private static class coding_info_t {
            boolean reserved;          // bit 7:    =0 ?
            boolean emphasis;          // bit 6:    
            byte    bits_per_sample;   // bit 5,4: 00=4bits (B,C format)
                                       //          01=8bits
            byte    sample_rate;       // bit 3,2: 00=37.8kHz (A,B format) 
                                       //          01=18.9kHz
            byte    mono_stereo;       // bit 1,0: 00=mono 01=stereo,
                                       //          other values reserved 
            
            public coding_info_t(int b) {
                reserved    = (b & 0x80) > 0;
                emphasis    = (b & 0x40) > 0;
                bits_per_sample = (byte)((b >> 4) & 3);
                sample_rate =     (byte)((b >> 2) & 3);
                mono_stereo =     (byte)((b >> 0) & 3);
            }
            
            public int ToByte() {
               return 
                    (reserved    ? 0x80 : 0)     |
                    (emphasis    ? 0x40 : 0)     |
                    ((bits_per_sample & 3) << 4) |
                    ((sample_rate     & 3) << 2) |
                    ((mono_stereo     & 3) << 0);
            }
        }
    }
    
    private static class CDXAHeaderWithSync extends CDXAHeader {
        
        public final static int CD_SECTOR_MAGIC[] = 
                                new int[] {0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFF00};
        
        public final static int SIZE = 24;
        public int getSize() {
            return CDXAHeaderWithSync.SIZE;
        }
        
        // sync header [12 bytes]
        // movconv doesn't add the sync header
        public int SyncHeader1; // [4 bytes] 0x00FFFFFF
        public int SyncHeader2; // [4 bytes] 0xFFFFFFFF
        public int SyncHeader3; // [4 bytes] 0xFFFFFF00
        
        public int minutes;     // [1 byte] timecode relative to start of disk
        public int seconds;     // [1 byte] timecode relative to start of disk
        public int sectors;     // [1 byte] timecode relative to start of disk
        public int mode;        // [1 byte] Mode 2 for ...
        
        public CDXAHeaderWithSync(DataInputStream oDIS) 
                throws IOException, NotThisTypeException
        {
            super(oDIS);
        }
        
        protected void ReadHeader(DataInputStream oDIS) 
                throws IOException, NotThisTypeException
        {
            if ((SyncHeader1 = oDIS.readInt()) != CD_SECTOR_MAGIC[0]) 
                throw new NotThisTypeException("Sector missing sync");
            if ((SyncHeader2 = oDIS.readInt()) != CD_SECTOR_MAGIC[1]) 
                throw new NotThisTypeException("Sector missing sync");
            if ((SyncHeader3 = oDIS.readInt()) != CD_SECTOR_MAGIC[2]) 
                throw new NotThisTypeException("Sector missing sync");
            
            minutes = oDIS.readUnsignedByte();
            seconds = oDIS.readUnsignedByte();
            sectors = oDIS.readUnsignedByte();
            mode    = oDIS.readUnsignedByte();
            
            super.ReadHeader(oDIS);
        }
        
    }
    
    /* ---------------------------------------------------------------------- */
    /* Fields --------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    RandomAccessFile m_oInputFile;
    String m_sSourceFilePath;
    long m_lngFirstSectorOffset = -1;
    int m_iRawSectorTypeSize = -1;
    int m_iSectorCount = -1;
    
    int m_iSectorHeaderSize;
    
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public CDSectorReader(String sFile) throws IOException {
        m_sSourceFilePath = new File(sFile).getPath();
        m_oInputFile = new RandomAccessFile(sFile, "r");
        
        FindFirstSectorOffsetAndSize();
        if (m_lngFirstSectorOffset < 0) 
            throw new IOException("Unable to determine file format.");
        
        m_iSectorCount = (int)((m_oInputFile.length() - m_lngFirstSectorOffset) 
                            / m_iRawSectorTypeSize);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public String getSourceFile() {
        return m_sSourceFilePath;
    }

    //..........................................................................

    

    /** Returns the actual offset in bytes from the start of the file/CD 
     *  to the start of iSector. */
    public long getFilePointer(int iSector) {
        return iSector * m_iRawSectorTypeSize 
                + m_lngFirstSectorOffset 
                + m_iSectorHeaderSize;
    }

    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public boolean HasSectorHeader() {
        switch (m_iRawSectorTypeSize) {
            case SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                return false;
            case SECTOR_MODE2:         // 2336
                return true;
            case SECTOR_RAW_AUDIO:     // 2352
                return true;
            default: 
                throw new RuntimeException("Should never happen: what kind of sector size is this?");
        }
    }
    
    //..........................................................................

    /** Returns the number of sectors in the file/CD */
    public int size() {
        return m_iSectorCount;
    }
    
    public CDXASector getSector(int iSector) throws IOException {
        byte abSectorBuff[] = new byte[m_iRawSectorTypeSize];
        int iBytesRead = 0;
        
        CDXAHeader oSectorHeader = null;
        CDXASector oSector;
        
        long lngFileOffset = m_lngFirstSectorOffset 
                          + m_iRawSectorTypeSize * iSector;
        
        // in the very unlikely case this class is ever used in a
        // multi-threaded environment, this is the only part
        // that needs to be syncronized.
        synchronized(this) {
            m_oInputFile.seek(lngFileOffset);
            iBytesRead = m_oInputFile.read(abSectorBuff);
        }

        if (iBytesRead != abSectorBuff.length) {
            // if we only got part of a sector
        }
        
        try {
            return new CDXASector(m_iRawSectorTypeSize, abSectorBuff, iSector, lngFileOffset);
        } catch (NotThisTypeException ex) {
            // unable to create a CDXA sector from the data.
            // Some possible causes:
            //  - It's a raw CD audio sector
            //  - At the end of the CD and the last sector is incomplete
            return null;
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    private final static int VIDEO_FRAME_MAGIC = 0x60010180;
    
    // Magic numbers found in ISO files
    //private final static int  ISO_MAGIC_CD00_ = 0x43443030  ;
    //private final static byte ISO_MAGIC_____1 =         0x31;
    
    private final static int PARTIAL_CD_SECTOR_HEADER = 0x00014800;
    
    /** Searches through the first MODE2_FORM2_CDXA_SECTOR_SIZE*2 bytes 
     *  for a CD sync mark, a video chunk sector, or an audio chunk sector.
     *  If none are found, searches for the CD001 mark indicating an ISO.
     *  If that's not found, then we give up cuz there's no way to tell
     *  what type of file it is. 
     *  Note !This assumes the input file has the data aligned at every 4 bytes!
     */
    private void FindFirstSectorOffsetAndSize() throws IOException {
        m_oInputFile.seek(0);
        byte abFirstBytes[] = new byte[SECTOR_RAW_AUDIO*2];
        int iBytesRead = m_oInputFile.read(abFirstBytes);

        // Create a DataInputStream from the bytes
        DataInputStream oByteTester = 
                new DataInputStream(
                new ByteArrayInputStream(abFirstBytes));
        
        for (int i = 0; i < iBytesRead-16; i+=4) {

            oByteTester.mark(16);
            
            int iTestBytes1 = oByteTester.readInt();
            int iTestBytes2 = oByteTester.readInt();
            int iTestBytes3 = oByteTester.readInt();
            int iTestBytes4 = oByteTester.readInt();
            
            if (iTestBytes1 == CDXAHeaderWithSync.CD_SECTOR_MAGIC[0] && 
                iTestBytes2 == CDXAHeaderWithSync.CD_SECTOR_MAGIC[1] && 
                iTestBytes3 == CDXAHeaderWithSync.CD_SECTOR_MAGIC[2]) {
                // CD Sync Header
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = SECTOR_RAW_AUDIO;
                break;
            }
            
            // This logic is better associated with Video Frame stuff
            if (iTestBytes1 == VIDEO_FRAME_MAGIC) {
                // Video frame...hopefully
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = SECTOR_MODE1_OR_MODE2_FORM1;
                break;
            }
            
            // output from movconv creates sectors like this
            // hopefully this is the only format
            if (iTestBytes1 == PARTIAL_CD_SECTOR_HEADER && 
                iTestBytes2 == PARTIAL_CD_SECTOR_HEADER &&
                iTestBytes3 == VIDEO_FRAME_MAGIC) {
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = SECTOR_MODE2;
                break;
            }
            
            oByteTester.reset();
            oByteTester.skip(4);
        }
        
        if (m_lngFirstSectorOffset < 0) {
            // we couldn't figure out what it is, assuming ISO style
            m_lngFirstSectorOffset = 0;
            m_iRawSectorTypeSize = SECTOR_MODE1_OR_MODE2_FORM1;
            /*
            // Couldn't find anything in first part of the file, 
            // now search for ISO-9660 magic number
            
            // If this is a standard iso 9660 file, 
            // 'CD001' usually occurs at byte 0x8001, 0x8801, or 0x9001
    
            for (long lngOffset: new long[] {0x8001, 0x8801, 0x9001}) {
                m_oInputFile.seek(lngOffset);
                if (m_oInputFile.readInt()  == ISO_MAGIC_CD00_ && 
                    m_oInputFile.readByte() == ISO_MAGIC_____1   ) 
                {
                    m_lngFirstSectorOffset = 0;
                    m_iRawSectorTypeSize = SECTOR_MODE1_OR_MODE2_FORM1;
                    break;
                }
            }
            */
        }
        
        // Back up to the first sector in case we matched at the second sector
        if (m_lngFirstSectorOffset > 0) {
            m_lngFirstSectorOffset = 
                    m_lngFirstSectorOffset % m_iRawSectorTypeSize;
        }
        
    }

}
