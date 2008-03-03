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

package jpsxdec.cdreaders;

import java.io.*;
import java.util.*;
import jpsxdec.util.NotThisTypeException;

/** Encapsulates the reading of a CD. 
 *  The term "CD" can mean an actual CD (not implemented yet), a CD image 
 *  (BIN/CUE, ISO), or a file containing some sectors of a CD. The resulting 
 *  data is mostly the same. This class does its best to guess what type of
 *  file it is. */
public class CDSectorReader {
    
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
    
    /** Opens a CD file for reading. */
    public CDSectorReader(String sFile) throws IOException {
        this(sFile, false);
    }

    public CDSectorReader(String sFile, boolean blnAllowWrites) throws IOException {
        m_sSourceFilePath = new File(sFile).getPath();
        if (blnAllowWrites)
            m_oInputFile = new RandomAccessFile(sFile, "rw");
        else
            m_oInputFile = new RandomAccessFile(sFile, "r");
        
        FindFirstSectorOffsetAndSize();
        if (m_lngFirstSectorOffset < 0) 
            throw new IOException("Unable to determine file format.");
        
        m_iSectorCount = (int)((m_oInputFile.length() - m_lngFirstSectorOffset) 
                            / m_iRawSectorTypeSize);
    }

    public void close() throws IOException {
        m_oInputFile.close();
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
            case CDXASector.SECTOR_MODE1_OR_MODE2_FORM1: // 2048
                return false;
            case CDXASector.SECTOR_MODE2:         // 2336
                return true;
            case CDXASector.SECTOR_RAW_AUDIO:     // 2352
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
    
    //..........................................................................
    
    /** Returns the requested sector. */
    public CDXASector getSector(int iSector) throws IOException {
        byte abSectorBuff[] = new byte[m_iRawSectorTypeSize];
        int iBytesRead = 0;
        
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
    
    public void writeSector(int iSector, byte[] abSrcUserData) throws IOException {
        
        CDXASector oSect = getSector(iSector);
        
        if (oSect.getSectorData().length != abSrcUserData.length)
            throw new IOException("Data to write is not the right size");
        
        long lngUserDataOfs = oSect.getFilePointer();
        
        synchronized(this) {
            m_oInputFile.seek(lngUserDataOfs);
            m_oInputFile.write(abSrcUserData);
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
        byte abFirstBytes[] = new byte[CDXASector.SECTOR_RAW_AUDIO*2];
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
            
            if (iTestBytes1 == CDXASector.SECTOR_SYNC_HEADER[0] && 
                iTestBytes2 == CDXASector.SECTOR_SYNC_HEADER[1] && 
                iTestBytes3 == CDXASector.SECTOR_SYNC_HEADER[2]) {
                // CD Sync Header
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = CDXASector.SECTOR_RAW_AUDIO;
                break;
            }
            
            // This logic is better associated with Video Frame stuff
            if (iTestBytes1 == VIDEO_FRAME_MAGIC) {
                // Video frame...hopefully
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = CDXASector.SECTOR_MODE1_OR_MODE2_FORM1;
                break;
            }
            
            // output from movconv creates sectors like this
            // hopefully this is the only format
            if (iTestBytes1 == PARTIAL_CD_SECTOR_HEADER && 
                iTestBytes2 == PARTIAL_CD_SECTOR_HEADER &&
                iTestBytes3 == VIDEO_FRAME_MAGIC) {
                m_lngFirstSectorOffset = i;
                m_iRawSectorTypeSize = CDXASector.SECTOR_MODE2;
                break;
            }
            
            oByteTester.reset();
            oByteTester.skip(4);
        }
        
        if (m_lngFirstSectorOffset < 0) {
            // we couldn't figure out what it is, assuming ISO style
            m_lngFirstSectorOffset = 0;
            m_iRawSectorTypeSize = CDXASector.SECTOR_MODE1_OR_MODE2_FORM1;
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

    @Override
    public String toString() {
        return "SourceFile|" + m_sSourceFilePath 
                + "|" + m_iRawSectorTypeSize 
                + "|" + m_iSectorCount 
                + "|" + m_lngFirstSectorOffset;
    }
    
    

}
