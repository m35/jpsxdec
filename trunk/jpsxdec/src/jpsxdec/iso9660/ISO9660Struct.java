/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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

package jpsxdec.iso9660;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.NotThisTypeException;

/** Abstract class inherited by all ISO9660 related structures. Provides handy 
 *  functions for reading the structure data. */
public abstract class ISO9660Struct {
    
    /** ECMA119: 7.2.3 */
    protected static void magic4_bothendian(InputStream is, int i) 
            throws IOException, NotThisTypeException 
    {
        if (read4_bothendian(is) != i) throw new NotThisTypeException();
    }
    
    /** Read two 16-bit values, first little-endian, then big-endian. 
     * ECMA119: 7.2.3 */
    protected static int read4_bothendian(InputStream is) 
            throws IOException, NotThisTypeException 
    {
        byte[] ab = readX(is, 4);
        if (ab[0] != ab[3] || ab[1] != ab[2]) throw new NotThisTypeException();
        return ((ab[2] & 0xff) << 8) | (ab[3] & 0xff);
    }
    
    /**  ECMA119: 7.3.3 */
    protected static void magic8_bothendian(InputStream is, long i) 
            throws IOException, NotThisTypeException 
    {
        if (read8_bothendian(is) != i) throw new NotThisTypeException();
    }
    
    /** Read two 32-bit values, first little-endian, then big-endian. 
     * ECMA119: 7.3.3 */
    protected static long read8_bothendian(InputStream is) 
            throws IOException, NotThisTypeException 
    {
        long i = read4_LE(is);
        if (i != read4_BE(is)) throw new NotThisTypeException();
        return i;
    }

    /** Checks for a sequence of magic bytes. */
    protected static void magicX(InputStream is, byte[] ab) 
            throws IOException, NotThisTypeException 
    {
        for (int i = 0; i < ab.length; i++) {
            int b = is.read();
            if (b < 0) throw new EOFException();
            if (ab[i] != b) throw new NotThisTypeException();
        }
    }
    
    /** Checks for a string of magic characters. */
    protected static void magicS(InputStream is, String s) 
            throws IOException, NotThisTypeException 
    {
        magicX(is, s.getBytes("US-ASCII"));
    }
    
    protected static String readS(InputStream is, int i) throws IOException {
        return new String(readX(is, i), "US-ASCII");
    }
    
    /** Reads one byte. ECMA119: 7.1.1 */
    protected static int read1(InputStream is) throws IOException {
        int i = is.read();
        if (i < 0) throw new EOFException();
        return i;
    }
    
    /** Checks for a magic byte value. ECMA119: 7.1.1 */
    protected static void magic1(InputStream is, int b) 
            throws IOException, NotThisTypeException 
    {
        int i = read1(is);
        if (i != b) 
            throw new NotThisTypeException();
    }
    
    /** Reads X bytes. */
    protected static byte[] readX(InputStream is, int i) throws IOException {
        byte[] ab = new byte[i];
        i = is.read(ab);
        if (i < 0) throw new EOFException();
        while (i < ab.length) {
            int x = is.read(ab, i, ab.length - i);
            if (x < 0) throw new EOFException();
            i += x;
        }
        return ab;
    }
    
    /** Checks for a sequence of X magic zeros. */
    protected static void magicXzero(InputStream is, int iCount) 
            throws IOException, NotThisTypeException 
    {
        for (int i = 0; i < iCount; i++) {
            int b = is.read();
            if (b < 0) throw new EOFException();
            if (b != 0) 
                throw new NotThisTypeException();
        }
    }
    
    /** Checks for magic little-endian 32-bit value. */
    protected static void magic4_LE(InputStream is, long i) 
            throws IOException, NotThisTypeException 
    {
        if (i != read4_LE(is)) throw new NotThisTypeException();
    }
    
    /** Checks for magic big-endian 32-bit value. */
    protected static void magic4_BE(InputStream is, long i) 
            throws IOException, NotThisTypeException 
    {
        if (i != read4_BE(is)) throw new NotThisTypeException();
    }
    
    /** Reads a little-endian 32-bit value. ECMA119: 7.3.1 */
    protected static long read4_LE(InputStream is) throws IOException {
        byte[] ab = readX(is, 4);
        return ((ab[3] & 0xFF) << 24) | 
               ((ab[2] & 0xFF) << 16) | 
               ((ab[1] & 0xFF) << 8 ) | 
                (ab[0] & 0xFF);
    }
    
    /** Reads a big-endian 32-bit value. ECMA119: 7.3.2 */
    protected static long read4_BE(InputStream is) throws IOException {
        byte[] ab = readX(is, 4);
        return ((ab[0] & 0xFF) << 24) | 
               ((ab[1] & 0xFF) << 16) | 
               ((ab[2] & 0xFF) << 8 ) | 
                (ab[3] & 0xFF);
    }
    
    /** Reads a big-endian 16-bit value. ECMA119: 7.2.2 */
    protected static int read2_BE(InputStream is) throws IOException {
        byte[] ab = readX(is, 2);
        return ((ab[0] & 0xff) << 8) | (ab[1] & 0xff);
    }
    
}
