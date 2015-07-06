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

package jpsxdec.util;

import java.io.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedEOFException;
import jpsxdec.i18n.LocalizedFileNotFoundException;
import jpsxdec.i18n.LocalizedIOException;

/** Additional functions for reading, writing, and whatnot. */
public final class IO {

    /** When you want an input stream to have getFilePointer(). */
    public static class InputStreamWithFP 
            extends InputStream 
    {

        @Nonnull
        private final InputStream is;
        private long i = 0;
        
        public InputStreamWithFP(@Nonnull InputStream is) {
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
    
    //== 8-bit =================================================================

    public static int readUInt8(@Nonnull InputStream is) throws IOException {
        int i = is.read();
        if (i < 0) throw new EOFException();
        return i;
    }

    public static byte readSInt8(@Nonnull InputStream is) throws IOException {
        return (byte)(readUInt8(is));
    }

    //== 16-bit signed =========================================================

    /** Reads signed little-endian 16 bits from InputStream.
     *  @throws EOFException if at the end of the stream. */
    public static short readSInt16LE(@Nonnull InputStream is) throws IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readUInt16LE"));
        return (short)((b2 << 8) + (b1 << 0));
    }

    /** Reads signed little-endian 16 bits from RandomAccessFile.
     *  @throws EOFException if at the end of the file. */
    public static short readSInt16LE(@Nonnull RandomAccessFile raf) throws IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        return (short)((b2 << 8) + (b1 << 0));
    }

    public static short readSInt16LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xFF;
        int b2 = ab[i+1] & 0xFF;
        return (short)((b2 << 8) + (b1 << 0));
    }

    public static short readSInt16BE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i+1] & 0xFF;
        int b2 = ab[i  ] & 0xFF;
        return (short)((b2 << 8) + (b1 << 0));
    }
    
    //== 16-bit ================================================================

    /** Reads big-endian 16 bits from InputStream.
     *  @throws EOFException if at the end of the stream. */
    public static int readUInt16BE(@Nonnull InputStream is) throws IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readUInt16BE"));
        return (b1 << 8) | b2;
    }

    /** Reads little-endian 16 bits from InputStream. 
     *  @throws EOFException if at the end of the stream. */
    public static int readUInt16LE(@Nonnull InputStream is) throws IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readUInt16LE"));
        return (b2 << 8) | b1;
    }
    
    public static int readUInt16LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xFF;
        int b2 = ab[i+1] & 0xFF;
        return (b2 << 8) | b1;
    }

    /** Reads little-endian 16 bits from RandomAccessFile.
     * @throws EOFException if at the end of the file. */
    public static int readUInt16LE(@Nonnull RandomAccessFile raf) throws IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        return (b2 << 8) | b1;
    }
    
    public static void writeInt16LE(@Nonnull OutputStream os, int i)
            throws IOException 
    {
        os.write(i & 0xFF);
        os.write((i >>> 8) & 0xFF);
    }

    public static void writeInt16BE(@Nonnull OutputStream os, int i)
            throws IOException
    {
        os.write((i >>> 8) & 0xFF);
        os.write(i & 0xFF);
    }

    public static void writeInt16LE(@Nonnull RandomAccessFile raf, short si)
            throws IOException 
    {
        raf.write((int)(si & 0xFF));
        raf.write((int)((si >>> 8) & 0xFF));
    }

    public static void writeInt16LE(@Nonnull byte[] ab, int pos, short si) {
        ab[pos  ] = (byte)( si        & 0xFF);
        ab[pos+1] = (byte)((si >>> 8) & 0xFF);
    }

    public static void writeInt16BE(@Nonnull byte[] ab, int pos, short si) {
        ab[pos  ] = (byte)((si >>> 8) & 0xFF);
        ab[pos+1] = (byte)( si        & 0xFF);
    }

    //== 32-bit ================================================================

    public static int readSInt32BE(@Nonnull InputStream is) throws IOException {
        final int b1, b2, b3, b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readSInt32BE"));
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0);
    }


    public static void writeInt32LE(@Nonnull OutputStream os, long lng)
            throws IOException 
    {
        os.write((int)( lng         & 0xFF));
        os.write((int)((lng >>>  8) & 0xFF));
        os.write((int)((lng >>> 16) & 0xFF));
        os.write((int)((lng >>> 24) & 0xFF));
    }

    public static void writeInt32LE(@Nonnull RandomAccessFile os, long lng)
            throws IOException
    {
        os.write((int)( lng         & 0xFF));
        os.write((int)((lng >>>  8) & 0xFF));
        os.write((int)((lng >>> 16) & 0xFF));
        os.write((int)((lng >>> 24) & 0xFF));
    }

    public static void writeInt32BE(@Nonnull OutputStream os, int i)
            throws IOException
    {
        os.write((int)((i >>> 24) & 0xFF));
        os.write((int)((i >>> 16) & 0xFF));
        os.write((int)((i >>>  8) & 0xFF));
        os.write((int)( i         & 0xFF));
    }

    public static void writeInt32LE(@Nonnull byte[] ab, int pos, long lng) {
        ab[pos  ] = (byte)( lng         & 0xFF);
        ab[pos+1] = (byte)((lng >>>  8) & 0xFF);
        ab[pos+2] = (byte)((lng >>> 16) & 0xFF);
        ab[pos+3] = (byte)((lng >>> 24) & 0xFF);
    }

    /** Reads little-endian 32 bits from a RandomAccessFile.
     *  Throws EOFException if at end of stream. */
    public static long readUInt32LE(@Nonnull RandomAccessFile raf) throws IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        int b3 = raf.readUnsignedByte();
        long b4 = raf.readUnsignedByte();
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }
    
    public static long readUInt32LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i+0] & 0xFF;
        int b2 = ab[i+1] & 0xFF;
        int b3 = ab[i+2] & 0xFF;
        long b4 = ab[i+3] & 0xFF;
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }
    
    public static int readSInt32BE(@Nonnull byte[] ab, int i) {
        int b4 = ab[i+0] & 0xFF;
        int b3 = ab[i+1] & 0xFF;
        int b2 = ab[i+2] & 0xFF;
        int b1 = ab[i+3] & 0xFF;
        int total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }

    public static long readUInt32BE(@Nonnull byte[] ab, int i) {
        int b4 = ab[i+0] & 0xFF;
        int b3 = ab[i+1] & 0xFF;
        int b2 = ab[i+2] & 0xFF;
        long b1 = ab[i+3] & 0xFF;
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }
    
    /** Function to read little-endian 32 bits from an InputStream.
     *  Throws EOFException if at end of stream. */
    public static long readUInt32LE(@Nonnull InputStream is) throws IOException {
        final int b1, b2, b3;
        final long b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readUInt32LE"));
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }
    
    public static int readSInt32LE(@Nonnull InputStream is) throws IOException {
        final int b1, b2, b3, b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readUInt32LE"));
        int total = ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
        return total;
    }

    public static int readSInt32LE(@Nonnull RandomAccessFile raf) throws IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        int b3 = raf.readUnsignedByte();
        int b4 = raf.readUnsignedByte();
        int total = ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
        return total;
    }


    //== 64-bit ================================================================

    public static long readSInt64BE(@Nonnull InputStream is) throws IOException {
        long lngRet = 0;
        for (int i = 0; i < 8; i++) {
            int iByte = is.read();
            if (iByte < 0)
                throw new LocalizedEOFException(I.UNEXPECTE_EOF_IN_FUNCTION("readSInt64BE"));
            lngRet <<= 8;
            lngRet |= iByte;
        }
        return lngRet;
    }


    //== Other IO ==============================================================

    /** Because the {@link InputStream#read(byte[])}  method won't always return
     *  the entire array for reasons I don't really care about. */
    public static @Nonnull byte[] readByteArray(@Nonnull InputStream is, int iBytes) 
            throws IOException
    {
        assert(iBytes > 0);
        byte[] ab = new byte[iBytes];
        int pos = is.read(ab);
        if (pos < 0) throw new EOFException();
        while (pos < iBytes) {
            int i = is.read(ab, pos, iBytes - pos);
            if (i < 0) throw new EOFException();
            pos += i;
        }
        return ab;
    }

    /** Because the {@link InputStream#read(byte[])}  method won't always return
     *  the entire array for reasons I don't really care about. */
    public static void readByteArray(@Nonnull InputStream is, @Nonnull byte[] ab) 
            throws IOException
    {
        readByteArray(is, ab, 0, ab.length);
    }

    /** Because the {@link InputStream#read(byte[])}  method won't always return
     *  the entire array for reasons I don't really care about. */
    public static void readByteArray(@Nonnull InputStream is, @Nonnull byte[] ab,
                                     int iStart, int iCount)
            throws IOException
    {
        int iPos = iStart;
        while (iCount > 0) {
            int i = is.read(ab, iPos, iCount);
            if (i < 0) throw new EOFException();
            iPos += i;
            iCount -= i;
        }
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  return the entire array for reasons I don't really care about. */
    public static @Nonnull byte[] readByteArray(@Nonnull RandomAccessFile raf, int iBytes) 
            throws IOException
    {
        assert(iBytes > 0);
        byte[] ab = new byte[iBytes];
        readByteArray(raf, ab);
        return ab;
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  return the entire array for reasons I don't really care about. */
    public static void readByteArray(@Nonnull RandomAccessFile raf, @Nonnull byte[] abBuf) 
            throws IOException
    {
        readByteArray(raf, abBuf, 0, abBuf.length);
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  return the entire array for reasons I don't really care about. */
    public static void readByteArray(@Nonnull RandomAccessFile raf, @Nonnull byte[] abBuf,
                                     int iStart, int iLen)
            throws IOException
    {
        int iTotal = raf.read(abBuf, iStart, iLen);
        if (iTotal < 0) throw new EOFException();
        while (iTotal < iLen) {
            int iCount = raf.read(abBuf, iStart + iTotal, iLen - iTotal);
            if (iCount < 0) throw new EOFException();
            iTotal += iCount;
        }
    }

    public static @Nonnull int[] readBEIntArray(@Nonnull InputStream is, int iCount) 
            throws IOException
    {
        int[] ai = new int[iCount];
        for (int i = 0; i < ai.length; i++) {
            ai[i] = readSInt32BE(is);
        }
        return ai;
    }

    public static void skip(@Nonnull InputStream is, long lngTotal) throws IOException {
        while (lngTotal > 0) {
            long lngCount = is.skip(lngTotal);
            if (lngCount < 0)
                throw new EOFException();
            lngTotal -= lngCount;
        }
    }

    public static void skip(@Nonnull RandomAccessFile raf, int iTotal) throws IOException {
        while (iTotal > 0) {
            int iCount = raf.skipBytes(iTotal);
            if (iCount < 0)
                throw new EOFException();
            iTotal -= iCount;
        }
    }

    public static void writeFile(@Nonnull String sFile, @Nonnull byte[] ab) throws IOException {
        writeFile(new File(sFile), ab);
    }
    
    public static void writeFile(@Nonnull File file, @Nonnull byte[] ab) throws IOException {
        writeFile(file, ab, 0, ab.length);
    }
    
    public static void writeFile(@Nonnull File file, @Nonnull byte[] ab, int iStart, int iLen) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(ab, iStart, iLen);
        } finally {
            fos.close();
        }
    }

    public static void writeFileBE(@Nonnull String sFile, @Nonnull int[] ai) throws IOException {
        writeFileBE(new File(sFile), ai);
    }

    public static void writeFileBE(@Nonnull File file, @Nonnull int[] ai) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            for (int i : ai) {
                writeInt32BE(fos, i);
            }
        } finally {
            fos.close();
        }
    }
    
    public static void writeIStoFile(@Nonnull InputStream is, @Nonnull String sFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(sFile);
        try {
            writeIStoOS(is, fos);
        } finally {
            fos.close();
        }
    }
    public static void writeIStoFile(@Nonnull InputStream is, @Nonnull File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            writeIStoOS(is, fos);
        } finally {
            fos.close();
        }
    }
    public static void writeIStoOS(@Nonnull InputStream is, @Nonnull OutputStream os) throws IOException {
        int i; byte[] b = new byte[2048];
        while ((i = is.read(b)) > 0)
            os.write(b, 0, i);
    }
    
    public static @Nonnull byte[] readFile(@Nonnull String sFile) throws IOException {
        return readFile(new File(sFile));
    }
    
    public static @Nonnull byte[] readFile(@Nonnull File file) throws IOException {
        // using RandomAccessFile for easy access to file size
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            if (raf.length() > Integer.MAX_VALUE)
                throw new UnsupportedOperationException("Unable to read file larger than max array size.");
            return readByteArray(raf, (int)raf.length());
        } finally {
            raf.close();
        }
    }


    public static @Nonnull byte[] readEntireStream(@Nonnull InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeIStoOS(is, baos);
        return baos.toByteArray();
    }

    /** Returns a sorted list of image files found in the folder. */
    public static @CheckForNull File[] getSortedFileList(@Nonnull String sDirectory,
                                                         final String ... asExtensions)
            throws FileNotFoundException
    {
        File dir = new File(sDirectory);
        if (!dir.exists() || !dir.isDirectory())
            throw new LocalizedFileNotFoundException(I.DIR_DOES_NOT_EXIST(dir.getPath()));
        FilenameFilter oFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                for (String sExt : asExtensions) {
                    if (name.endsWith(sExt)) return true;
                }
                return false;
            }
        };
        File[] aoNames = dir.listFiles(oFilter);
        if (aoNames == null) {
            return null;
        } else {
            java.util.Arrays.sort(aoNames);
            return aoNames;
        }
    }

    public static void copyFile(@Nonnull File src, @Nonnull File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            writeIStoFile(in, dest);
        } finally {
            in.close();
        }
    }

    /** Creates the directory or throws {@link IOException} if anything goes wrong.
     * If dir is null does nothing. */
    public static void makeDirs(@CheckForNull File dir) throws IOException {
        if (dir == null)
            return;
        // create the dir if it doesn't exist
        if (!dir.exists() && !dir.mkdirs()) {
            throw new LocalizedIOException(I.UNABLE_TO_CREATE_DIR(dir));
        } else if (!dir.isDirectory()) {
            // if it does exist, it better not be a file
            throw new LocalizedIOException(I.CANNOT_CREATE_DIR_OVER_FILE(dir));
        }
    }

    public static void makeDirsForFile(@Nonnull File f) throws IOException {
        makeDirs(f.getParentFile());
    }

    private static final byte[] ZEROS = new byte[1024];
    public static void writeZeros(@Nonnull OutputStream os, int iCount)
            throws IOException
    {
        while (iCount > 0) {
            int iToWrite = ZEROS.length;
            if (iToWrite > iCount)
                iToWrite = iCount;
            os.write(ZEROS, 0, iToWrite);
            iCount -= iToWrite;
        }
    }

}
