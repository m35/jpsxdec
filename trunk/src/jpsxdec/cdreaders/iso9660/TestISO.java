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
 * TestISO.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.*;
import java.util.ArrayList;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.util.NotThisTypeException;

public class TestISO {

    public static void main(String[] args) throws IOException, NotThisTypeException {
        
        if ( args.length < 1 ) {
            System.out.println("Need a CD image file to examine.");
            System.exit(-1);
        }
        
        CDSectorReader oCD = new CDFileSectorReader(args[0]);
        
        VolumePrimaryDescriptor vpd = 
                new VolumePrimaryDescriptor(oCD.getSector(16).getSectorDataStream());
        
        PathTableBE pt = 
                new PathTableBE(oCD.getSector((int)vpd.type_m_path_table).getSectorDataStream());
        
        ArrayList<ISOFile> oFileList = new ArrayList<ISOFile>();
        
        getFileList(vpd.root_directory_record, oCD, oFileList, new File(""));
        
        for (ISOFile ofile : oFileList) {
            System.out.println(ofile.getPath() 
                    + " " + ofile.getStartSector() + "-" + 
                    ofile.getEndSector());
        }
        
    }
    
    
    public static void getFileList(DirectoryRecord rec, CDSectorReader oCD, ArrayList<ISOFile> oFileList, File oDir) throws IOException {

        // return if this isn't a directory, or if an empty directory
        if ((rec.flags & 0x2) == 0 || rec.extent == 0 || rec.size == 0) return;
        
        for (int iSectorsRead = 0; iSectorsRead*2048 < rec.size; iSectorsRead++) {

            try {
                CDXASector oSect = oCD.getSector((int)rec.extent + iSectorsRead);
                System.out.println("Offset for " + rec.name + " extent: " + oSect.getFilePointer());
                ByteArrayInputStream sectorstream = oSect.getSectorDataStream();
                while (true) {
                    DirectoryRecord dr = 
                        new DirectoryRecord(sectorstream);
                    if (!dr.name.equals("\0") && !dr.name.equals("\1")) {
                        ISOFile drdir = new ISOFile(oDir, SanitizeFileName(dr.name), dr.extent, dr.size);
                        oFileList.add(drdir);
                        getFileList(dr, oCD, oFileList, drdir);
                    }
                }
            } catch (NotThisTypeException ex) {}
        }
    }
    
    private static String SanitizeFileName(String s) {
        if (s.endsWith(";1")) s = s.substring(0, s.length() - 2);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }
    
    

}
