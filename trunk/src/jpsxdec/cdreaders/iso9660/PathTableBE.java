
package jpsxdec.cdreaders.iso9660;

import java.io.*;
import java.util.ArrayList;
import jpsxdec.util.NotThisTypeException;

public class PathTableBE extends ArrayList<PathTableRecordBE> {

    public PathTableBE(InputStream is) throws IOException {

        try {
            for (int index = 1; true ; index++) {
                PathTableRecordBE ptr = new PathTableRecordBE(is, index);
                super.add(ptr);
            }
        } catch (NotThisTypeException ex) {}
        super.trimToSize();
        
    }
    
}
