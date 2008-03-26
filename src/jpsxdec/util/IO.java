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
 * IO.java
 */

package jpsxdec.util;

import java.io.*;


/** Additional functions to read/write streams. */
public final class IO {
    
    /** When you want an input stream to have getFilePointer(). */
    public static class InputStreamWithFP 
            extends InputStream 
            implements IGetFilePointer 
    {

        private InputStream is;
        private long i = 0;
        
        public InputStreamWithFP(InputStream is) {
            this.is = is;
        }

        /** {@inheritDoc} */ @Override
        public int read() throws IOException {
            i++;
            return is.read();
        }

        public long getFilePointer() {
            return i;
        }
        
    }
    
    public static int ReadUInt8(InputStream oIS) throws IOException {
        int i = oIS.read();
        if (i < 0) throw new EOFException();
        return i;
    }
    
    /** Reads little-endian 16 bits from InputStream. 
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
    
    public static int ReadInt16BE(InputStream oIS) throws IOException 
    {
        int b1 = oIS.read();
        int b2 = oIS.read();
        int b3 = oIS.read();
        int b4 = oIS.read();
        if ((b1 | b2 | b3 | b4) < 0) throw new EOFException();
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0);
    }
    
    /** Reads little-endian 16 bits from RandomAccessFile. 
     *  Throws an exception if at the end of the file. */
    public static long ReadUInt16LE(RandomAccessFile oRAF) throws IOException 
    {
        int b1 = oRAF.read();
        if (b1 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt16LE");
        int b2 = oRAF.read();
        if (b2 < 0)
            throw new EOFException("Unexpected end of file in ReadUInt16LE");
        return (b2 << 8) | b1;
    }
    
    public static void WriteInt16LE(OutputStream oOS, long lng) 
            throws IOException 
    {
        oOS.write((int)(lng & 0xFF));
        oOS.write((int)((lng >>> 8) & 0xFF));
    }

    public static void WriteInt16LE(RandomAccessFile raf, int lng) 
            throws IOException 
    {
        raf.write((int)(lng & 0xFF));
        raf.write((int)((lng >>> 8) & 0xFF));
    }

    public static void WriteInt32LE(OutputStream oOS, long lng) 
            throws IOException 
    {
        oOS.write((int)( lng         & 0xFF));
        oOS.write((int)((lng >>>  8) & 0xFF));
        oOS.write((int)((lng >>> 16) & 0xFF));
        oOS.write((int)((lng >>> 24) & 0xFF));
    }
    
    /** Reads little-endian 32 bits from a RandomAccessFile.
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
        if (total == -1) throw 
               new IOException("Reading of unsigned 32 bits 0xFFFFFFFF -> -1");
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
    
    
    /** Because the read(byte[]) method won't always return the entire
     *  array for various reasons I don't really care about. */
    public static byte[] readByteArray(InputStream oIS, int iBytes) throws IOException {
        assert(iBytes > 0);
        byte[] ab = new byte[iBytes];
        int pos = oIS.read(ab);
        if (pos < 0) throw new EOFException();
        while (pos < iBytes) {
            int i = oIS.read(ab, pos, iBytes - pos);
            if (i < 0) throw new EOFException();
            pos += i;
        }
        return ab;
    }
    
    /** Because the read(byte[]) method won't always return the entire
     *  array for various reasons I don't really care about. */
    public static byte[] readByteArray(RandomAccessFile raf, int iBytes) throws IOException {
        assert(iBytes > 0);
        byte[] ab = new byte[iBytes];
        int pos = raf.read(ab);
        if (pos < 0) throw new EOFException();
        while (pos < iBytes) {
            int i = raf.read(ab, pos, iBytes - pos);
            if (i < 0) throw new EOFException();
            pos += i;
        }
        return ab;
    }

    public static void writeFile(String s, byte[] ab) throws IOException {
        FileOutputStream fos = new FileOutputStream(s);
        fos.write(ab);
        fos.close();
    }
    
    
    public static void writeIStoFile(InputStream is, String sFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(sFile);
        writeIStoOS(is, fos);
    }
    public static void writeIStoFile(InputStream is, File oFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(oFile);
        writeIStoOS(is, fos);
    }
    public static void writeIStoOS(InputStream is, OutputStream os) throws IOException {
        int i; byte[] b = new byte[2048];
        while ((i = is.read(b)) > 0)
            os.write(b, 0, i);
        os.close();
    }
    
    public static byte[] readFile(String sFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(sFile, "r");
        byte[] ab = readByteArray(raf, (int)raf.length());
        raf.close();
        return ab;
    }
    
    public static void main(String[] args) throws IOException {
        // test reading a -1
        byte[] ab = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        
        ByteArrayInputStream bais = new ByteArrayInputStream(ab);
        
        long i = ReadUInt32LE(bais);
        System.out.println(i);
        
    }
    
}
