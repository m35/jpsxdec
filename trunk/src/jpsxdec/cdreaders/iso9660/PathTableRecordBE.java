
package jpsxdec.cdreaders.iso9660;

import java.io.*;
import jpsxdec.util.NotThisTypeException;

public class PathTableRecordBE extends ISOstruct {
    final public int index;
    
    final public int xa_len;
    final public long extent;
    final public int parent;
    final public String name;

    public PathTableRecordBE(InputStream is, int index) throws IOException, NotThisTypeException {
        this.index = index;
        
        int name_len = read1(is);
        if (name_len == 0) throw new NotThisTypeException();
        xa_len = read1(is);
        extent = read4_BE(is);
        parent = read2_BE(is);
        name   = readS(is, name_len);
        magicXzero(is, name_len % 2);
    }

}
