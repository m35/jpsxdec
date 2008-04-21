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
 * CDDriveSectorReaderWin.java
 */

package jpsxdec.cdreaders;

import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileSystemView;
import jpsxdec.nativeclass.RawCdRead;
import jpsxdec.util.NotThisTypeException;


public class CDDriveSectorReaderWin extends CDSectorReader {

    private char m_cSrcDrive;
    private int m_iDiscSize;
    private int m_iFirstBufferedSector = -1;
    private int m_iBufferedSectorLength = -1;
    private final byte[] m_abBufferedSectors = new byte[SECTORS_TO_BUFFER * RawCdRead.RAW_SECTOR_SIZE];
    
    private final static int SECTORS_TO_BUFFER = 128;

    public CDDriveSectorReaderWin(char cDrive) throws IOException {
        RawCdRead.Open(cDrive);
        m_cSrcDrive = cDrive;
        m_iDiscSize = RawCdRead.GetSectorCount();
    }
    
    @Override
    public boolean HasSectorHeader() {
        return true;
    }

    @Override
    public void close() throws IOException {
        RawCdRead.Close();
    }

    @Override
    public long getFilePointer(int iSector) {
        return iSector * RawCdRead.RAW_SECTOR_SIZE;
    }

    /** Returns the requested sector. */
    @Override
    public CDXASector getSector(final int iSector) throws IOException, IndexOutOfBoundsException {
        if (iSector < 0 || iSector >= m_iDiscSize)
            throw new IndexOutOfBoundsException("Sector not in bounds of CD");
        
        if (m_iFirstBufferedSector >= 0) {
            if (iSector < m_iFirstBufferedSector ||
                iSector >= m_iFirstBufferedSector + m_iBufferedSectorLength)
                m_iFirstBufferedSector = -1;
        }
        
        if (m_iFirstBufferedSector < 0) {
            m_iFirstBufferedSector = iSector;
            if (iSector + SECTORS_TO_BUFFER > m_iDiscSize)
                m_iBufferedSectorLength = m_iDiscSize - iSector;
            else
                m_iBufferedSectorLength = SECTORS_TO_BUFFER;
            
            RawCdRead.ReadSector(iSector, m_abBufferedSectors, m_iBufferedSectorLength);
            /*
            FileOutputStream fos = new FileOutputStream("test.dat");
            fos.write(m_abBufferedSectors);
            fos.close();
            */
        }
        
        byte abSectorBuff[] = new byte[RawCdRead.RAW_SECTOR_SIZE];
        System.arraycopy(m_abBufferedSectors, 
                (iSector - m_iFirstBufferedSector) * RawCdRead.RAW_SECTOR_SIZE,
                abSectorBuff, 0, RawCdRead.RAW_SECTOR_SIZE);
        
        try {
            return new CDXASector(RawCdRead.RAW_SECTOR_SIZE, abSectorBuff, iSector, getFilePointer(iSector));
        } catch (NotThisTypeException ex) {
            // unable to create a CDXA sector from the data.
            // Some possible causes:
            //  - It's a raw CD audio sector
            //  - XA audio data is incorrect (corrupted)
            return null;
        }
    }

    @Override
    public String getSourceFile() {
        FileSystemView fsv = FileSystemView.getFileSystemView();

        String s = fsv.getSystemDisplayName( new File(m_cSrcDrive + ":") );
        
        return s.replaceAll("[^\\w-]", "");
        
    }

    @Override
    public int size() {
        return m_iDiscSize;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
}
