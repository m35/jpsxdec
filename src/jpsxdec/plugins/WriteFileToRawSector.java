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
 * StrDiffCombine.java
 */

package jpsxdec.plugins;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteFileToRawSector {

    final static int SECTOR_SIZE = 2352;
    
    public static void main(String[] args) throws IOException {
        WriteFileToSector(
                "C:\\Documents and Settings\\Michael\\Desktop\\mdec\\jpsxdec\\enc\\Dc1046.str",
                "C:\\Documents and Settings\\Michael\\Desktop\\LainImg\\disc2hack.bin",
                91138
                );
    }
    
    static void WriteFileToSector(String sInFile, String sFileToChange, int iSector) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(sFileToChange, "rw");
        FileInputStream fis = new FileInputStream(sInFile);
        
        raf.seek(iSector * SECTOR_SIZE);
        
        byte[] ab = new byte[1024 * 4];
        int i;
        while ((i = fis.read(ab)) > 0) {
            raf.write(ab, 0, i);
        }
        
        raf.close();
        fis.close();
            
    }

}
