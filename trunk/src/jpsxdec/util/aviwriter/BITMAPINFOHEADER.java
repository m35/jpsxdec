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
 * BITMAPINFOHEADER.java
 */

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;

/** Represents the C BITMAPINFOHEADER structure. 
 * http://msdn2.microsoft.com/en-us/library/ms532290(VS.85).aspx */
public class BITMAPINFOHEADER extends AVIstruct {
    
    public final static int BI_RGB = 0;

    public final /*DWORD*/ int   biSize           = sizeof();
    public       /*LONG */ int   biWidth          = 0;
    public       /*LONG */ int   biHeight         = 0;
    public final /*WORD */ short biPlanes         = 1;
    public       /*WORD */ short biBitCount       = 0;
    public       /*DWORD*/ int   biCompression    = 0;
    public       /*DWORD*/ int   biSizeImage      = 0;
    public       /*LONG */ int   biXPelsPerMeter  = 0;
    public       /*LONG */ int   biYPelsPerMeter  = 0;
    public       /*DWORD*/ int   biClrUsed        = 0;
    public       /*DWORD*/ int   biClrImportant   = 0;

    @Override
    public void write(RandomAccessFile raf) throws IOException {
        /*DWORD*/ write32LE(raf, biSize         );
        /*LONG */ write32LE(raf, biWidth        );
        /*LONG */ write32LE(raf, biHeight       );
        /*WORD */ write16LE(raf, biPlanes       );
        /*WORD */ write16LE(raf, biBitCount     );
        /*DWORD*/ write32LE(raf, biCompression  );
        /*DWORD*/ write32LE(raf, biSizeImage    );
        /*LONG */ write32LE(raf, biXPelsPerMeter);
        /*LONG */ write32LE(raf, biYPelsPerMeter);
        /*DWORD*/ write32LE(raf, biClrUsed      );
        /*DWORD*/ write32LE(raf, biClrImportant );
    }

    @Override
    public int sizeof() {
        return 40;
    }
    
}
