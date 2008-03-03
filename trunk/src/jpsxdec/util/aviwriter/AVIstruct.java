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
 * AVIstruct.java
 */

package jpsxdec.util.aviwriter;

import java.io.*;

/** Super-class of the C structures used in the AVI file format. This provides
 *  helper functions, some required interface of the sub-classes, and the
 *  ability to easily go back and write the structure to a prior location
 *  in the AVI file. */
public abstract class AVIstruct {

    public static void write32LE(RandomAccessFile raf, int i) throws IOException {
        raf.write(i & 0xFF);
        raf.write((i >>>  8) & 0xFF);
        raf.write((i >>> 16) & 0xFF);
        raf.write((i >>> 24) & 0xFF);
    }
    
    public static void write16LE(RandomAccessFile raf, short i) throws IOException {
        raf.write(i& 0xFF);
        raf.write((i >>> 8) & 0xFF);
    }
    
    public static int string2int(String s) {
        if (s.length() != 4) throw new IllegalArgumentException();
        try {
            return bytes2int(s.getBytes("UTF8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    public static int bytes2int(byte[] ab) {
        if (ab.length != 4) throw new IllegalArgumentException();
        
        return (ab[0]) | 
               (ab[1] << 8 ) | 
               (ab[2] << 16) | 
               (ab[3] << 24);
    }
    
    public abstract void write(RandomAccessFile raf) throws IOException;
    public abstract int sizeof();
    
    private long m_lngPlaceholder;
    
    public void makePlaceholder(RandomAccessFile raf) throws IOException {
        m_lngPlaceholder = raf.getFilePointer();
        raf.write(new byte[this.sizeof()]);
    }
    
    public void goBackAndWrite(RandomAccessFile raf) throws IOException {
        long lngCurPos = raf.getFilePointer(); // save this pos
        raf.seek(m_lngPlaceholder); // go back
        this.write(raf); // write the data
        raf.seek(lngCurPos); // return to current position
    }
    
}
