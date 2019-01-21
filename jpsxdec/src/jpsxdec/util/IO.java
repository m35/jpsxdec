/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;

// TODO: unify write(types)

/** Additional functions for reading, writing, and whatnot. */
public final class IO {

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

    public static void writeInt4x2(@Nonnull OutputStream stream, byte bTop, byte bBottom)
            throws IOException
    {
        stream.write(((bTop&0xf)<<4) | (bBottom&0xf));
    }

    //== 8-bit =================================================================

    public static int readUInt8(@Nonnull InputStream stream) throws EOFException, IOException {
        int i = stream.read();
        if (i < 0)
            throw new EOFException();
        return i;
    }

    public static byte readSInt8(@Nonnull InputStream stream) throws EOFException, IOException {
        return (byte)(readUInt8(stream));
    }

    // #########################################################################
    // #########################################################################

    //== 16-bit == little-endian == signed == read =============================

    public static short readSInt16LE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0)
            throw new EOFException();
        return SInt16LE(b1, b2);
    }

    public static short readSInt16LE(@Nonnull RandomAccessFile stream) throws EOFException, IOException {
        int b1, b2;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0)
            throw new EOFException();
        return SInt16LE(b1, b2);
    }

    public static short readSInt16LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xff;
        int b2 = ab[i+1] & 0xff;
        return SInt16LE(b1, b2);
    }

    public static short SInt16LE(int b1, int b2) {
        return (short)(((b2 & 0xff) << 8) | (b1 & 0xff));
    }

    //== 16-bit == little-endian == unsigned == read ===========================

    public static int readUInt16LE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0)
            throw new EOFException();
        return UInt16LE(b1, b2);
    }

    public static int readUInt16LE(@Nonnull RandomAccessFile stream) throws EOFException, IOException {
        int b1, b2;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0)
            throw new EOFException();
        return UInt16LE(b1, b2);
    }

    public static int readUInt16LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xff;
        int b2 = ab[i+1] & 0xff;
        return UInt16LE(b1, b2);
    }

    public static int UInt16LE(int b1, int b2) {
        return (((b2 & 0xff) << 8) | (b1 & 0xff));
    }

    //== 16-bit == big-endian == signed == read ================================

    public static short readSInt16BE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i  ] & 0xff;
        int b2 = ab[i+1] & 0xff;
        return SInt16BE(b1, b2);
    }

    public static short SInt16BE(int b1, int b2) {
        return (short)(((b1 & 0xff) << 8) | (b2 & 0xff));
    }

    //== 16-bit == big-endian == unsigned == read ==============================

    public static int readUInt16BE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0)
            throw new EOFException();
        return UInt16BE(b1, b2);
    }

    public static int UInt16BE(int b1, int b2) {
        return (((b1 & 0xff) << 8) | (b2 & 0xff));
    }

    //== 16-bit == little-endian == write ======================================

    public static void writeInt16LE(@Nonnull RandomAccessFile stream, short si) throws IOException {
        stream.write(si & 0xff);
        stream.write((si >>> 8) & 0xff);
    }

    public static void writeInt16LE(@Nonnull OutputStream stream, int i) throws IOException {
        stream.write(i & 0xff);
        stream.write((i >>> 8) & 0xff);
    }

    public static void writeInt16LE(@Nonnull byte[] ab, int pos, short si) {
        ab[pos  ] = (byte)( si        & 0xff);
        ab[pos+1] = (byte)((si >>> 8) & 0xff);
    }

    //== 16-bit == big-endian == write =========================================

    public static void writeInt16BE(@Nonnull OutputStream stream, int i) throws IOException {
        stream.write((i >>> 8) & 0xff);
        stream.write(i & 0xff);
    }

    public static void writeInt16BE(@Nonnull byte[] ab, int pos, short si) {
        ab[pos  ] = (byte)((si >>> 8) & 0xff);
        ab[pos+1] = (byte)( si        & 0xff);
    }

    // #########################################################################
    // #########################################################################
    
    //== 32-bit == little-endian == signed == read =============================

    public static int readSInt32LE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return SInt32LE(b1, b2, b3, b4);
    }

    public static int readSInt32LE(@Nonnull RandomAccessFile stream) throws EOFException, IOException {
        int b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return SInt32LE(b1, b2, b3, b4);
    }

    public static int readSInt32LE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i+0] & 0xff;
        int b2 = ab[i+1] & 0xff;
        int b3 = ab[i+2] & 0xff;
        int b4 = ab[i+3] & 0xff;
        return SInt32LE(b1, b2, b3, b4);
    }

    public static int SInt32LE(int b1, int b2, int b3, int b4) {
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    //== 32-bit == little-endian == unsigned == read ===========================

    public static long readUInt32LE(@Nonnull RandomAccessFile stream) throws EOFException, IOException {
        long b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return UInt32LE(b1, b2, b3, b4);
    }

    public static long readUInt32LE(@Nonnull InputStream stream) throws EOFException, IOException {
        long b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return UInt32LE(b1, b2, b3, b4);
    }

    public static long readUInt32LE(@Nonnull byte[] ab, int i) {
        long b1 = ab[i+0] & 0xffL;
        long b2 = ab[i+1] & 0xffL;
        long b3 = ab[i+2] & 0xffL;
        long b4 = ab[i+3] & 0xffL;
        return UInt32LE(b1, b2, b3, b4);
    }

    public static long UInt32LE(long b1, long b2, long b3, long b4) {
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    //== 32-bit == big-endian == signed == read ================================
    
    public static int readSInt32BE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return SInt32BE(b1, b2, b3, b4);
    }

    public static int readSInt32BE(@Nonnull byte[] ab, int i) {
        int b1 = ab[i+0] & 0xff;
        int b2 = ab[i+1] & 0xff;
        int b3 = ab[i+2] & 0xff;
        int b4 = ab[i+3] & 0xff;
        return SInt32BE(b1, b2, b3, b4);
    }

    public static int SInt32BE(int b1, int b2, int b3, int b4) {
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    //== 32-bit == big-endian == unsigned == read ==============================

    public static long readUInt32BE(@Nonnull byte[] ab, int i) {
        long b4 = ab[i+0] & 0xff;
        long b3 = ab[i+1] & 0xff;
        long b2 = ab[i+2] & 0xff;
        long b1 = ab[i+3] & 0xff;
        long total = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        return total;
    }

    public static long readUInt32BE(@Nonnull InputStream stream) throws EOFException, IOException {
        int b1, b2, b3, b4;
        if ((b1 = stream.read()) < 0 ||
            (b2 = stream.read()) < 0 ||
            (b3 = stream.read()) < 0 ||
            (b4 = stream.read()) < 0)
            throw new EOFException();
        return SInt32BE(b1, b2, b3, b4);
    }

    //== 32-bit == little-endian == write ======================================

    public static void writeInt32LE(@Nonnull OutputStream stream, long lng) throws IOException {
        int i = (int)lng;
        stream.write( i         & 0xff);
        stream.write((i >>>  8) & 0xff);
        stream.write((i >>> 16) & 0xff);
        stream.write((i >>> 24) & 0xff);
    }

    public static void writeInt32LE(@Nonnull RandomAccessFile stream, long lng) throws IOException {
        int i = (int)lng;
        stream.write( i         & 0xff);
        stream.write((i >>>  8) & 0xff);
        stream.write((i >>> 16) & 0xff);
        stream.write((i >>> 24) & 0xff);
    }

    public static void writeInt32LE(@Nonnull byte[] ab, int pos, long lng) {
        int i = (int)lng;
        ab[pos  ] = (byte)( i         & 0xff);
        ab[pos+1] = (byte)((i >>>  8) & 0xff);
        ab[pos+2] = (byte)((i >>> 16) & 0xff);
        ab[pos+3] = (byte)((i >>> 24) & 0xff);
    }

    //== 32-bit == big-endian == write =========================================

    public static void writeInt32BE(@Nonnull OutputStream stream, int i) throws IOException {
        stream.write((int)((i >>> 24) & 0xff));
        stream.write((int)((i >>> 16) & 0xff));
        stream.write((int)((i >>>  8) & 0xff));
        stream.write((int)( i         & 0xff));
    }

    // #########################################################################
    // #########################################################################

    //== 64-bit ================================================================

    public static long readSInt64BE(@Nonnull InputStream stream) throws EOFException, IOException {
        long lngRet = 0;
        for (int i = 0; i < 8; i++) {
            int iByte = stream.read();
            if (iByte < 0)
                throw new EOFException();
            lngRet = (lngRet << 8) | iByte;
        }
        return lngRet;
    }

    public static long readSInt64BE(@Nonnull byte[] ab, int i) {
        long lngRet = 0;
        for (int j = 0; j < 8; j++) {
            byte iByte = ab[i+j];
            lngRet = (lngRet << 8) | iByte;
        }
        return lngRet;
    }

    // #########################################################################
    // #########################################################################

    //== read byte array (int) =================================================

    /** Because the {@link InputStream#read(byte[])} method won't always read
     *  the entire array in one call. */
    public static @Nonnull byte[] readByteArray(@Nonnull InputStream stream,
                                                int iBytesToRead) 
            throws EOFException, IOException
    {
        assert iBytesToRead > 0;
        byte[] ab = new byte[iBytesToRead];
        readByteArray(stream, ab);
        return ab;
    }

    public static @Nonnull byte[] readByteArray(@Nonnull RandomAccessFile stream,
                                                int iBytesToRead)
            throws EOFException, IOException
    {
        assert iBytesToRead > 0;
        byte[] ab = new byte[iBytesToRead];
        readByteArray(stream, ab);
        return ab;
    }

    //== read byte array (byte[]) ==============================================

    public static void readByteArray(@Nonnull InputStream stream,
                                     @Nonnull byte[] abBuffer) 
            throws EOFException, IOException
    {
        readByteArray(stream, abBuffer, 0, abBuffer.length);
    }

    public static void readByteArray(@Nonnull RandomAccessFile stream,
                                     @Nonnull byte[] abBuffer)
            throws EOFException, IOException
    {
        readByteArray(stream, abBuffer, 0, abBuffer.length);
    }

    //== read byte array (byte[], pos, len) ====================================

    public static void readByteArray(@Nonnull InputStream stream,
                                     @Nonnull byte[] abBuffer,
                                     int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iBytesRead = readByteArrayMax(stream, abBuffer, iStartOffset, iBytesToRead);
        if (iBytesRead < iBytesToRead)
            throw new EOFException();
    }
    
    public static void readByteArray(@Nonnull RandomAccessFile stream,
                                     @Nonnull byte[] abBuffer,
                                     int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iBytesRead = readByteArrayMax(stream, abBuffer, iStartOffset, iBytesToRead);
        if (iBytesRead < iBytesToRead)
            throw new EOFException();
    }

    //== read byte array max ===================================================

    /** Read as much as possible and return the number of bytes read.
     * If 0 is returned, it is at the end of the stream. Never returns -1. */
    public static int readByteArrayMax(@Nonnull InputStream stream,
                                       @Nonnull byte[] abBuffer,
                                       int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iRemainingBytes = iBytesToRead;
        int iBytesRead = stream.read(abBuffer, iStartOffset, iRemainingBytes);
        if (iBytesRead < 0)
            return 0;
        iRemainingBytes -= iBytesRead;
        while (iRemainingBytes > 0) {
            iStartOffset += iBytesRead;
            iBytesRead = stream.read(abBuffer, iStartOffset, iRemainingBytes);
            if (iBytesRead < 0)
                break;
            iRemainingBytes -= iBytesRead;
        }
        return iBytesToRead - iRemainingBytes;
    }

    /** @see #readByteArrayMax(java.io.InputStream, byte[], int, int)  */
    public static int readByteArrayMax(@Nonnull RandomAccessFile stream,
                                       @Nonnull byte[] abBuffer,
                                       int iStartOffset, int iBytesToRead)
            throws EOFException, IOException
    {
        int iRemainingBytes = iBytesToRead;
        int iBytesRead = stream.read(abBuffer, iStartOffset, iRemainingBytes);
        if (iBytesRead < 0)
            return 0;
        iRemainingBytes -= iBytesRead;
        while (iRemainingBytes > 0) {
            iStartOffset += iBytesRead;
            iBytesRead = stream.read(abBuffer, iStartOffset, iRemainingBytes);
            if (iBytesRead < 0)
                break;
            iRemainingBytes -= iBytesRead;
        }
        return iBytesToRead - iRemainingBytes;
    }

    //== skip ==================================================================

    public static void skip(@Nonnull InputStream stream, long lngTotal)
            throws EOFException, IOException
    {
        long lngBytesSkipped = skipEofCheck(stream.skip(lngTotal), -1);
        lngTotal -= lngBytesSkipped;
        while (lngTotal > 0) {
            lngBytesSkipped = skipEofCheck(stream.skip(lngTotal), lngBytesSkipped);
            lngTotal -= lngBytesSkipped;
        }
    }

    /**
     * Check for end-of-stream condition when skipping.
     * {@link InputStream#skip(long)} is never supposed to return -1,
     * and may return 0 even if not at the end of the stream.
     * <p>
     * Two conditions will trigger a {@link EOFException}:
     * <ul>
     * <li> The stream returned negative bytes read.
     * <li> Twice in a row, the stream returned 0 bytes read.
     * </ul>
     * @param lngBytesRead    Bytes read from the last read call
     * @param iPrevBytesRead  Bytes read from the previous to last read call
     * @return {@code lngBytesRead}
     */
    private static long skipEofCheck(long lngBytesRead, long lngPrevBytesRead) throws EOFException {
        if (lngBytesRead < 0)
            throw new EOFException();
        if (lngBytesRead == 0 && lngPrevBytesRead == 0)
            throw new EOFException();
        return lngBytesRead;
    }

    public static void skip(@Nonnull RandomAccessFile stream, int iTotal)
            throws EOFException, IOException
    {
        int iBytesSkipped = skipEofCheck(stream.skipBytes(iTotal), -1);
        iTotal -= iBytesSkipped;
        while (iTotal > 0) {
            iBytesSkipped = skipEofCheck(stream.skipBytes(iTotal), iBytesSkipped);
            iTotal -= iBytesSkipped;
        }
    }

    /**
     * Check for end-of-stream condition when skipping.
     * {@link RandomAccessFile#skipBytes(int)} will return -1 at EOF.
     * Could it return return 0 even when not at the end of the stream?
     * <p>
     * Two conditions will trigger a {@link EOFException}:
     * <ul>
     * <li> The stream returned negative bytes read.
     * <li> Twice in a row, the stream returned 0 bytes read.
     * </ul>
     * @param iBytesRead      Bytes read from the last read call
     * @param iPrevBytesRead  Bytes read from the previous to last read call
     * @return {@code iBytesRead}
     */
    private static int skipEofCheck(int iBytesRead, int iPrevBytesRead) throws EOFException {
        if (iBytesRead < 0)
            throw new EOFException();
        if (iBytesRead == 0 && iPrevBytesRead == 0)
            throw new EOFException();
        return iBytesRead;
    }

    public static int skipMax(@Nonnull InputStream stream, int iTotal) throws IOException {
        long lngPrevBytesSkipped = stream.skip(iTotal);
        if (lngPrevBytesSkipped < 0)
            return 0;
        long lngBytesRemain = iTotal - lngPrevBytesSkipped;
        while (lngBytesRemain > 0) {
            long lngBytesSkipped = stream.skip(lngBytesRemain);
            if (lngBytesSkipped < 0 || (lngBytesSkipped == 0 && lngPrevBytesSkipped == 0))
                return (int) (iTotal - lngBytesRemain);
            lngBytesRemain -= lngBytesSkipped;
            lngPrevBytesSkipped = lngBytesSkipped;
        }
        return iTotal;
    }

    // #########################################################################
    // #########################################################################

    //== write entire ==========================================================

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

    public static void writeIStoFile(@Nonnull InputStream stream, @Nonnull String sFile)
            throws FileNotFoundException, IOException
    {
        writeIStoFile(stream, new File(sFile));
    }
    public static void writeIStoFile(@Nonnull InputStream stream, @Nonnull File file)
            throws FileNotFoundException, IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            writeIStoOS(stream, fos);
        } finally {
            fos.close(); // XXX: could mask a thrown exception
        }
    }
    public static void writeIStoOS(@Nonnull InputStream is, @Nonnull OutputStream os) throws IOException {
        int i; byte[] b = new byte[2048];
        while ((i = is.read(b)) > 0)
            os.write(b, 0, i);
    }
    
    //== read entire ===========================================================

    public static @Nonnull byte[] readFile(@Nonnull String sFile) throws FileNotFoundException, IOException {
        return readFile(new File(sFile));
    }
    
    public static @Nonnull byte[] readFile(@Nonnull File file) throws FileNotFoundException, IOException {
        // using RandomAccessFile for easy access to file size
        RandomAccessFile stream = new RandomAccessFile(file, "r");
        try {
            if (stream.length() > Integer.MAX_VALUE)
                throw new UnsupportedOperationException("Unable to read file larger than max array size.");
            return readByteArray(stream, (int)stream.length());
        } finally {
            stream.close(); // XXX: could mask a thrown exception
        }
    }

    public static @Nonnull byte[] readEntireStream(@Nonnull InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeIStoOS(stream, baos);
        return baos.toByteArray();
    }

    // #########################################################################
    // #########################################################################

    //== other =================================================================

    public static @Nonnull int[] readBEIntArray(@Nonnull InputStream stream, int iCount)
            throws EOFException, IOException
    {
        int[] ai = new int[iCount];
        for (int i = 0; i < ai.length; i++) {
            ai[i] = readSInt32BE(stream);
        }
        return ai;
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
    public static void writeZeros(@Nonnull OutputStream stream, int iCount) throws IOException
    {
        while (iCount > 0) {
            int iToWrite = ZEROS.length;
            if (iToWrite > iCount)
                iToWrite = iCount;
            stream.write(ZEROS, 0, iToWrite);
            iCount -= iToWrite;
        }
    }

    public static void writeZeros(@Nonnull RandomAccessFile stream, int iCount) throws IOException
    {
        while (iCount > 0) {
            int iToWrite = ZEROS.length;
            if (iToWrite > iCount)
                iToWrite = iCount;
            stream.write(ZEROS, 0, iToWrite);
            iCount -= iToWrite;
        }
    }

    public static class ZeroInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(@Nonnull byte[] b, int off, int len) throws IOException {
            Arrays.fill(b, off, len, (byte)0);
            return len;
        }

        @Override
        public long skip(long n) throws IOException {
            return n;
        }
    }
}
