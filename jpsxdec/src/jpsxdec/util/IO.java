/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.LocalizedFileNotFoundException;

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

    /** Closes a {@link Closeable} resource, suppressing any {@link IOException}
     * thrown. If thrown, it is logged to the given logger and returns the thrown
     * exception. Returns null if no exception is thrown.
     * By accepting a null resource, it also handles the case that the given
     * resource was never created. In this case it does nothing and returns null. */
    public static @CheckForNull IOException closeSilently(@CheckForNull Closeable obj, 
                                                          @Nonnull Logger log)
    {
        if (obj != null) {
            try {
                obj.close();
                return null;
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Exception closing object " + obj, ex);
                return ex;
            }
        } else {
            return null;
        }
    }
    
    //== 4-bit =================================================================

    public static void writeInt4x2(@Nonnull OutputStream os, byte bTop, byte bBottom)
            throws IOException
    {
        os.write(((bTop&0xf)<<4) | (bBottom&0xf));
    }

    //== 8-bit =================================================================

    public static int readUInt8(@Nonnull InputStream is) throws EOFException, IOException {
        int i = is.read();
        if (i < 0) throw new EOFException("readUInt8");
        return i;
    }

    public static byte readSInt8(@Nonnull InputStream is) throws EOFException, IOException {
        return (byte)(readUInt8(is));
    }

    //== 16-bit signed =========================================================

    /** Reads signed little-endian 16 bits from InputStream.
     *  @throws EOFException if at the end of the stream. */
    public static short readSInt16LE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new EOFException("readUInt16LE");
        return (short)((b2 << 8) + (b1 << 0));
    }

    /** Reads signed little-endian 16 bits from RandomAccessFile.
     *  @throws EOFException if at the end of the file. */
    public static short readSInt16LE(@Nonnull RandomAccessFile raf) throws EOFException, IOException {
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
    public static int readUInt16BE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new EOFException("readUInt16BE");
        return (b1 << 8) | b2;
    }

    /** Reads little-endian 16 bits from InputStream. 
     *  @throws EOFException if at the end of the stream. */
    public static int readUInt16LE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2;
        if ((b1 = is.read()) < 0 || (b2 = is.read()) < 0)
            throw new EOFException("readUInt16LE");
        return (b2 << 8) | b1;
    }
    
    public static int readUInt16LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xFF;
        int b2 = ab[i+1] & 0xFF;
        return (b2 << 8) | b1;
    }

    /** Reads little-endian 16 bits from RandomAccessFile.
     * @throws EOFException if at the end of the file. */
    public static int readUInt16LE(@Nonnull RandomAccessFile raf) throws EOFException, IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        return (b2 << 8) | b1;
    }
    
    public static void writeInt16LE(@Nonnull OutputStream os, int i) throws IOException {
        os.write(i & 0xFF);
        os.write((i >>> 8) & 0xFF);
    }

    public static void writeInt16BE(@Nonnull OutputStream os, int i) throws IOException {
        os.write((i >>> 8) & 0xFF);
        os.write(i & 0xFF);
    }

    public static void writeInt16LE(@Nonnull RandomAccessFile raf, short si) throws IOException {
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

    public static int readSInt32BE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2, b3, b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new EOFException("readSInt32BE");
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0);
    }


    public static void writeInt32LE(@Nonnull OutputStream os, long lng) throws IOException {
        os.write((int)( lng         & 0xFF));
        os.write((int)((lng >>>  8) & 0xFF));
        os.write((int)((lng >>> 16) & 0xFF));
        os.write((int)((lng >>> 24) & 0xFF));
    }

    public static void writeInt32LE(@Nonnull RandomAccessFile os, long lng) throws IOException {
        os.write((int)( lng         & 0xFF));
        os.write((int)((lng >>>  8) & 0xFF));
        os.write((int)((lng >>> 16) & 0xFF));
        os.write((int)((lng >>> 24) & 0xFF));
    }

    public static void writeInt32BE(@Nonnull OutputStream os, int i) throws IOException {
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
    public static long readUInt32LE(@Nonnull RandomAccessFile raf) throws EOFException, IOException {
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
    public static long readUInt32LE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2, b3;
        final long b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new EOFException("readUInt32LE");
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }
    
    public static int readSInt32LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i+0] & 0xFF;
        int b2 = ab[i+1] & 0xFF;
        int b3 = ab[i+2] & 0xFF;
        int b4 = ab[i+3] & 0xFF;
        int total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }

    public static int readSInt32LE(@Nonnull InputStream is) throws EOFException, IOException {
        final int b1, b2, b3, b4;
        if ((b1 = is.read()) < 0 ||
            (b2 = is.read()) < 0 ||
            (b3 = is.read()) < 0 ||
            (b4 = is.read()) < 0)
            throw new EOFException("readUInt32LE");
        int total = ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
        return total;
    }

    public static int readSInt32LE(@Nonnull RandomAccessFile raf) throws EOFException, IOException {
        int b1 = raf.readUnsignedByte();
        int b2 = raf.readUnsignedByte();
        int b3 = raf.readUnsignedByte();
        int b4 = raf.readUnsignedByte();
        int total = ((b4 << 24) + (b3 << 16) + (b2 << 8) + (b1 << 0));
        return total;
    }


    //== 64-bit ================================================================

    public static long readSInt64BE(@Nonnull InputStream is) throws EOFException, IOException {
        long lngRet = 0;
        for (int i = 0; i < 8; i++) {
            int iByte = is.read();
            if (iByte < 0)
                throw new EOFException("readSInt64BE");
            lngRet <<= 8;
            lngRet |= iByte;
        }
        return lngRet;
    }


    //== Other IO ==============================================================

    /** Because the {@link InputStream#read(byte[])} method won't always read
     *  the entire array in one call. */
    public static @Nonnull byte[] readByteArray(@Nonnull InputStream is, 
                                                int iBytesToRead) 
            throws EOFException, IOException
    {
        byte[] abBuffer = new byte[iBytesToRead];
        readByteArray(is, abBuffer);
        return abBuffer;
    }

    /** Because the {@link InputStream#read(byte[])} method won't always read
     *  the entire array in one call. */
    public static void readByteArray(@Nonnull InputStream is, 
                                     @Nonnull byte[] abBuffer) 
            throws EOFException, IOException
    {
        readByteArray(is, abBuffer, 0, abBuffer.length);
    }

    /** Because the {@link InputStream#read(byte[])} method won't always read
     *  the entire array in one call. */
    public static void readByteArray(@Nonnull InputStream is, 
                                     @Nonnull byte[] abBuffer,
                                     int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iBytesRead = readByteArrayMax(is, abBuffer, iStartOffset, iBytesToRead);
        if (iBytesRead < iBytesToRead)
            throw new EOFException("readByteArray");
    }
    
    /** Read as much as possible and return the number of bytes read. */
    public static int readByteArrayMax(@Nonnull InputStream is, 
                                       @Nonnull byte[] abBuffer,
                                       int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iTotalBytesRead = is.read(abBuffer, iStartOffset, iBytesToRead);
        if (iTotalBytesRead < 0) 
            throw new EOFException("readMaxByteArray");
        while (iTotalBytesRead < iBytesToRead) {
            int iBytesRead = is.read(abBuffer, iStartOffset + iTotalBytesRead, 
                                     iBytesToRead - iTotalBytesRead);
            if (iBytesRead < 0) 
                break;
            iTotalBytesRead += iBytesRead;
        }
        return iTotalBytesRead;
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  read the entire array in one call. */
    public static @Nonnull byte[] readByteArray(@Nonnull RandomAccessFile raf, 
                                                int iBytesToRead) 
            throws EOFException, IOException
    {
        assert(iBytesToRead > 0);
        byte[] ab = new byte[iBytesToRead];
        readByteArray(raf, ab);
        return ab;
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  read the entire array in one call. */
    public static void readByteArray(@Nonnull RandomAccessFile raf, 
                                     @Nonnull byte[] abBuffer) 
            throws EOFException, IOException
    {
        readByteArray(raf, abBuffer, 0, abBuffer.length);
    }

    /** Because the {@link RandomAccessFile#read(byte[])} method won't always
     *  read the entire array in one call. */
    public static void readByteArray(@Nonnull RandomAccessFile raf, 
                                     @Nonnull byte[] abBuffer,
                                     int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iBytesRead = readByteArrayMax(raf, abBuffer, iStartOffset, iBytesToRead);
        if (iBytesRead < iBytesToRead)
            throw new EOFException("readByteArray");
    }
    
    /** Read as much as possible and return the number of bytes read. */
    public static int readByteArrayMax(@Nonnull RandomAccessFile raf, 
                                       @Nonnull byte[] abBuffer,
                                       int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iTotalBytesRead = raf.read(abBuffer, iStartOffset, iBytesToRead);
        if (iTotalBytesRead < 0) 
            throw new EOFException("readMaxByteArray");
        while (iTotalBytesRead < iBytesToRead) {
            int iBytesRead = raf.read(abBuffer, iStartOffset + iTotalBytesRead, 
                                      iBytesToRead - iTotalBytesRead);
            if (iBytesRead < 0) 
                break;
            iTotalBytesRead += iBytesRead;
        }
        return iTotalBytesRead;
    }
    

    public static @Nonnull int[] readBEIntArray(@Nonnull InputStream is, int iCount) 
            throws EOFException, IOException
    {
        int[] ai = new int[iCount];
        for (int i = 0; i < ai.length; i++) {
            ai[i] = readSInt32BE(is);
        }
        return ai;
    }

    public static void skip(@Nonnull InputStream is, long lngTotal) 
            throws EOFException, IOException
    {
        while (lngTotal > 0) {
            long lngCount = is.skip(lngTotal);
            if (lngCount < 0)
                throw new EOFException("skip");
            lngTotal -= lngCount;
        }
    }

    public static void skip(@Nonnull RandomAccessFile raf, int iTotal) 
            throws EOFException, IOException
    {
        while (iTotal > 0) {
            int iCount = raf.skipBytes(iTotal);
            if (iCount < 0)
                throw new EOFException("skip");
            iTotal -= iCount;
        }
    }

    public static void writeFile(@Nonnull String sFile, @Nonnull byte[] ab) 
            throws FileNotFoundException, IOException
    {
        writeFile(new File(sFile), ab);
    }
    
    public static void writeFile(@Nonnull File file, @Nonnull byte[] ab) 
            throws FileNotFoundException, IOException
    {
        writeFile(file, ab, 0, ab.length);
    }
    
    public static void writeFile(@Nonnull File file, @Nonnull byte[] ab, int iStart, int iLen) 
            throws FileNotFoundException, IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(ab, iStart, iLen);
        } finally {
            fos.close(); // XXX: could mask a thrown exception
        }
    }

    public static void writeIStoFile(@Nonnull InputStream is, @Nonnull String sFile) 
            throws FileNotFoundException, IOException
    {
        writeIStoFile(is, new File(sFile));
    }
    public static void writeIStoFile(@Nonnull InputStream is, @Nonnull File file) 
            throws FileNotFoundException, IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            writeIStoOS(is, fos);
        } finally {
            fos.close(); // XXX: could mask a thrown exception
        }
    }
    public static void writeIStoOS(@Nonnull InputStream is, @Nonnull OutputStream os) throws IOException {
        int i; byte[] b = new byte[2048];
        while ((i = is.read(b)) > 0)
            os.write(b, 0, i);
    }
    
    public static @Nonnull byte[] readFile(@Nonnull String sFile) throws FileNotFoundException, IOException {
        return readFile(new File(sFile));
    }
    
    public static @Nonnull byte[] readFile(@Nonnull File file) throws FileNotFoundException, IOException {
        // using RandomAccessFile for easy access to file size
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try {
            if (raf.length() > Integer.MAX_VALUE)
                throw new UnsupportedOperationException("Unable to read file larger than max array size.");
            return readByteArray(raf, (int)raf.length());
        } finally {
            raf.close(); // XXX: could mask a thrown exception
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
            throw new FileNotFoundException(sDirectory);
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

    /** Creates the directory or throws {@link LocalizedFileNotFoundException}
     * if anything goes wrong.
     * If dir is null does nothing. If the directory already exists, nothing
     * will change in the file system. If there is an error, some part of the
     * directory structure may have been created.
     *
     * @throws LocalizedFileNotFoundException 
     *              Contains user visible details of the failure.
     *              Either there was some unknown error that occurred,
     *              or a file already exists with that name.
     * 
     * @see File#mkdirs()
     */
    public static void makeDirs(@CheckForNull File dir) throws LocalizedFileNotFoundException {
        if (dir == null)
            return;
        if (!dir.mkdirs()) { // try to create the directory
            // failed for some reason
            if (!dir.exists()) // if nothing exists with that name
                throw new LocalizedFileNotFoundException(I.UNABLE_TO_CREATE_DIR(dir));
            else if (!dir.isDirectory()) // if something does exist, and it is a file
                throw new LocalizedFileNotFoundException(I.CANNOT_CREATE_DIR_OVER_FILE(dir));
        }
    }

    public static void makeDirsForFile(@Nonnull File f) throws LocalizedFileNotFoundException {
        makeDirs(f.getParentFile());
    }

    private static final byte[] ZEROS = new byte[1024];
    public static void writeZeros(@Nonnull OutputStream os, int iCount) throws IOException
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
