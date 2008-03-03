
package jpsxdec.cdreaders.iso9660;

import java.io.*;
import java.util.ArrayList;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.NotThisTypeException;

public class TestISO {

    public static void main(String[] args) throws IOException, NotThisTypeException {
        
        
        CDSectorReader oCD =
            //new CDSectorReader("..\\..\\disc1.iso");
            new CDSectorReader("E:\\LainPSX\\DISCS\\disc2.iso");
        
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
