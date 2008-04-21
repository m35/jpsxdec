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

import java.io.File;
import java.io.IOException;

public abstract class CDSectorReader {
    
    public static CDSectorReader Open(String sFile) throws IOException {
        int isWin = System.getProperty("os.name").toLowerCase().indexOf("windows");
        
        if (false) { // (isWin >= 0) {  // will re-enable this eventually
        
            File f = new File(sFile);
            String abs = f.getAbsolutePath().toUpperCase();
            boolean isDrive = false;
            for (File root : File.listRoots()) {
                if (abs.equals(root.getAbsolutePath().toUpperCase())) {
                    isDrive = true;
                    break;
                }
            }

            if (isDrive)
                return new CDDriveSectorReaderWin(abs.charAt(0));
            else
                return  new CDFileSectorReader(sFile);
            
        } else {
            return new CDFileSectorReader(sFile);
        }
        
    }
    

    public abstract boolean HasSectorHeader();

    public abstract void close() throws IOException;

    /**
     * Returns the actual offset in bytes from the start of the file/CD
     * to the start of iSector.
     */
    public abstract long getFilePointer(int iSector);

    /**
     * Returns the requested sector.
     */
    public abstract CDXASector getSector(int iSector) throws IOException, IndexOutOfBoundsException;

    public abstract String getSourceFile();

    /**
     * Returns the number of sectors in the file/CD
     */
    public abstract int size();
    
}