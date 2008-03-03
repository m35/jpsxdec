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
 * AVIOLDINDEX.java
 */

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;

/** Represents the C AVIOLDINDEX structure.  
 * http://msdn2.microsoft.com/en-us/library/ms779634(VS.85).aspx */
public class AVIOLDINDEX extends AVIstruct {

    public static int AVIIF_KEYFRAME = 0x10;
    
    public static class _avioldindex_entry extends AVIstruct {
        public /*DWORD*/ int dwChunkId = 0;
        public /*DWORD*/ int dwFlags   = 0;
        public /*DWORD*/ int dwOffset  = 0;
        public /*DWORD*/ int dwSize    = 0;
        
        @Override
        public void write(RandomAccessFile raf) throws IOException {
            write32LE(raf, dwChunkId);
            write32LE(raf, dwFlags  );
            write32LE(raf, dwOffset );
            write32LE(raf, dwSize   );
        }

        @Override
        public int sizeof() {
            return 16;
        }
        
        public static _avioldindex_entry[] newarray(int i) {
            _avioldindex_entry[] a = new _avioldindex_entry[i];
            for (int j = 0; j < a.length; j++) {
                a[j] = new _avioldindex_entry();
            }
            return a;
        }
        
    }
    
    
    public /*FOURCC*/ final int fcc    = string2int("idx1");
    public /*DWORD */ final int cb     ;
    public _avioldindex_entry aIndex[] = null;    
    
    public AVIOLDINDEX(_avioldindex_entry[] a) {
        aIndex = a;
        cb = sizeof() - 8;
    }
    
    @Override
    public void write(RandomAccessFile raf) throws IOException {
        write32LE(raf, fcc);
        write32LE(raf, cb );
        for (_avioldindex_entry e : aIndex) {
            e.write(raf);
        }
    }

    @Override
    public int sizeof() {
        if (aIndex != null && aIndex.length > 0)
            return 8 + aIndex.length * aIndex[0].sizeof();
        else
            return 8;
    }
    
}
