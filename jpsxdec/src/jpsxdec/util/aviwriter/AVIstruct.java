/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.util.aviwriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;


/** Super-class of the C structures used in the AVI file format. This provides
 *  helper functions, some required interface of the sub-classes, and the
 *  ability to easily go back and write the structure to a prior location
 *  in the AVI file. */
abstract class AVIstruct {

    public static void write32LE(RandomAccessFile raf, int i) throws IOException {
        raf.write(i & 0xFF);
        raf.write((i >>>  8) & 0xFF);
        raf.write((i >>> 16) & 0xFF);
        raf.write((i >>> 24) & 0xFF);
    }
    
    public static void write16LE(RandomAccessFile raf, short si) throws IOException {
        raf.write(si& 0xFF);
        raf.write((si >>> 8) & 0xFF);
    }
    
    public static int string2int(String s) {
        if (s.length() != 4) throw new IllegalArgumentException();
        try {
            return bytes2int(s.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
    
    public static int bytes2int(byte[] ab) {
        if (ab.length != 4) throw new IllegalArgumentException();
        
        return (ab[0] & 0xff) |
               ((ab[1] & 0xff) << 8 ) |
               ((ab[2] & 0xff) << 16) |
               ((ab[3] & 0xff) << 24);
    }
    
    public abstract void write(RandomAccessFile raf) throws IOException;
    public abstract int sizeof();
    
    private long _lngPlaceholder;
    
    public void makePlaceholder(RandomAccessFile raf) throws IOException {
        _lngPlaceholder = raf.getFilePointer();
        raf.write(new byte[this.sizeof()]);
    }
    
    public void goBackAndWrite(RandomAccessFile raf) throws IOException {
        long lngCurPos = raf.getFilePointer(); // save this pos
        raf.seek(_lngPlaceholder); // go back
        this.write(raf); // write the data
        raf.seek(lngCurPos); // return to current position
    }
    
}
