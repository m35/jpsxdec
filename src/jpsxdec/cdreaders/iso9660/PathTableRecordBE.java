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
 * PathTableRecordBE.java
 */

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
