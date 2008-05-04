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
 * ISOstruct.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.*;
import jpsxdec.util.NotThisTypeException;

/** Abstract class inherited by all ISO9660 related structurs. Provides handy 
 *  functions for reading the structure data. */
public abstract class ISOstruct {
    
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
        return (ab[2] << 8) | ab[3];
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
        return (ab[0] << 8) | ab[1];
    }
    
}
