/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * LittleEndianIO.java
 *
 */

package jpsxdec.util;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.RandomAccessFile;


/** Functions to read little endian values from a stream. */
public final class LittleEndianIO {
    
    public class NeverThrowsIOException extends IOException {}
    
    /** Function to read little-endian 16 bits from InputStream. 
     *  Throws an exception if at the end of the stream. */
    public static long ReadUInt16LE(InputStream oIS) throws IOException 
    {
        int b1 = oIS.read();
        if (b1 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt16LE");
        int b2 = oIS.read();
        if (b2 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt16LE");
        return (b2 << 8) | b1;
    }
    
    /** Function to read little-endian 32 bits from a RandomAccessFile.
     *  Throws EOFException if at end of stream. 
     *  Throws IOException if 0xFFFFFFFF is read, causing an overflow to -1. */
    public static long ReadUInt32LE(RandomAccessFile oRAF) throws IOException {
        int b1 = oRAF.readUnsignedByte();
        if (b1 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b2 = oRAF.readUnsignedByte();
        if (b2 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b3 = oRAF.readUnsignedByte();
        if (b3 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b4 = oRAF.readUnsignedByte();
        if (b4 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        if (total == -1) throw new IOException("Reading of unsigned 32 bits 0xFFFFFFFF -> -1");
        return total;
    }
    
    /** Function to read little-endian 32 bits from an InputStream.
     *  Throws EOFException if at end of stream. 
     *  Note that if 0xFFFFFFFF is read, it will overflow to -1. */
    public static long ReadUInt32LE(InputStream oIS) throws IOException {
        // Note that if 0xFFFFFFFF, it will overflow to -1
        int b1 = oIS.read();
        if (b1 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b2 = oIS.read();
        if (b2 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b3 = oIS.read();
        if (b3 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        int b4 = oIS.read();
        if (b4 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt32LE");
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }
    
}
