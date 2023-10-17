/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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
import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Abstract class inherited by all ISO9660 related structures. Provides handy
 *  functions for reading the structure data. */
public abstract class ISO9660Struct {

    /** ECMA119: 7.2.3 */
    protected static void magic4_bothendian(@Nonnull InputStream is, int i)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        if (read4_bothendian(is) != i) throw new BinaryDataNotRecognized();
    }

    /** Read two 16-bit values, first little-endian, then big-endian.
     * ECMA119: 7.2.3 */
    protected static int read4_bothendian(@Nonnull InputStream is)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        byte[] ab = readX(is, 4);
        if (ab[0] != ab[3] || ab[1] != ab[2]) throw new BinaryDataNotRecognized();
        return ((ab[2] & 0xff) << 8) | (ab[3] & 0xff);
    }

    /**  ECMA119: 7.3.3 */
    protected static void magic8_bothendian(@Nonnull InputStream is, long i)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        if (read8_bothendian(is) != i) throw new BinaryDataNotRecognized();
    }

    /** Read two 32-bit unsigned values, first little-endian, then big-endian.
     * ECMA119: 7.3.3 */
    protected static long read8_bothendian(@Nonnull InputStream is)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        long i = read4_LE(is);
        if (i != read4_BE(is)) throw new BinaryDataNotRecognized();
        return i;
    }

    /** Checks for a sequence of magic bytes. */
    protected static void magicX(@Nonnull InputStream is, byte[] ab)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        for (int i = 0; i < ab.length; i++) {
            int b = is.read();
            if (b < 0) throw new EOFException();
            if (ab[i] != b) throw new BinaryDataNotRecognized();
        }
    }

    /** Checks for a string of magic characters. */
    protected static void magicS(@Nonnull InputStream is, String s)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        magicX(is, Misc.stringToAscii(s));
    }

    protected static String readS(@Nonnull InputStream is, int i) throws EOFException, IOException {
        return Misc.asciiToString(readX(is, i));
    }

    /** Reads one byte, signed. ECMA119: 7.1.1 */
    protected static int read1(@Nonnull InputStream is) throws EOFException, IOException {
        int i = is.read();
        if (i < 0) throw new EOFException();
        return i;
    }

    /** Checks for a magic byte value. ECMA119: 7.1.1 */
    protected static void magic1(@Nonnull InputStream is, int b)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        int i = read1(is);
        if (i != b)
            throw new BinaryDataNotRecognized();
    }

    /** Reads X bytes. */
    protected static byte[] readX(@Nonnull InputStream is, int i) throws EOFException, IOException {
        if (i == 0)
            return new byte[0];
        return IO.readByteArray(is, i);
    }

    /** Checks for a sequence of X magic zeros. */
    protected static void magicXzero(@Nonnull InputStream is, int iCount)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        for (int i = 0; i < iCount; i++) {
            int b = is.read();
            if (b < 0) throw new EOFException();
            if (b != 0)
                throw new BinaryDataNotRecognized();
        }
    }

    /** Checks for magic little-endian 32-bit value. */
    protected static void magic4_LE(@Nonnull InputStream is, long i)
            throws EOFException, IOException, BinaryDataNotRecognized
    {
        if (i != read4_LE(is)) throw new BinaryDataNotRecognized();
    }

    /** Checks for magic big-endian 32-bit value. */
    protected static void magic4_BE(@Nonnull InputStream is, long i)
            throws IOException, BinaryDataNotRecognized
    {
        if (i != read4_BE(is)) throw new BinaryDataNotRecognized();
    }

    /** Reads a little-endian 32-bit unsigned value. ECMA119: 7.3.1 */
    protected static long read4_LE(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt32LE(is);
    }

    /** Reads a big-endian 32-bit unsigned value. ECMA119: 7.3.2 */
    protected static long read4_BE(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt32BE(is);
    }

    /** Reads a big-endian 16-bit value. ECMA119: 7.2.2 */
    protected static int read2_BE(@Nonnull InputStream is) throws EOFException, IOException {
        return IO.readUInt16BE(is);
    }

    /** '.' ECMA119: 7.4.3 */
    protected static final String SEPARATOR1 = Character.toString((char)0x2e);
    /** ';' ECMA119: 7.4.3 */
    protected static final String SEPARATOR2 = Character.toString((char)0x3b);

}
