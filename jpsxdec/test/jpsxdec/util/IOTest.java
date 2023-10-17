/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016-2023  Michael Sabin
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

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class IOTest {

    @Rule
    public TemporaryFolder TMP_FOLDER = new TemporaryFolder();

    @Test
    public void testMakeDirs() throws IOException {
        File dir = TMP_FOLDER.getRoot();
        IO.makeDirs(dir);
        File subDir = new File(dir, "test");
        IO.makeDirs(subDir);
        File file = TMP_FOLDER.newFile();
        try {
            IO.makeDirs(file);
            Assert.fail("Expected " + LocalizedFileNotFoundException.class);
        } catch (LocalizedFileNotFoundException ex) {
        }
    }

    private static class TestIS extends InputStream {

        private final int _iSize;
        private int _iBytesRead = 0;

        public TestIS(int iSize) {
            _iSize = iSize;
        }

        @Override
        public int read() throws IOException {
            if (_iBytesRead >= _iSize)
                return -1;

            int r = _iBytesRead & 0xff;
            _iBytesRead++;
            return r;
        }
    }

    private static class TestRAF extends RandomAccessFile {

        private final TestIS _mockStream;

        public TestRAF(File name, int iSize) throws FileNotFoundException {
            super(name, "r");
            _mockStream = new TestIS(iSize);
        }

        @Override
        public int read() throws IOException {
            return _mockStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return _mockStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return _mockStream.read(b, off, len);
        }

        public void setLength(long newLength) { throw new UnsupportedOperationException(); }
        public long length() { throw new UnsupportedOperationException(); }
        public void seek(long pos) { throw new UnsupportedOperationException(); }
        public long getFilePointer() { throw new UnsupportedOperationException(); }
        public void write(byte[] b, int off, int len) { throw new UnsupportedOperationException(); }
        public void write(byte[] b) { throw new UnsupportedOperationException(); }
        public void write(int b) { throw new UnsupportedOperationException(); }
        public int skipBytes(int n) { throw new UnsupportedOperationException(); }
    }

    @Test
    public void testReadByteArray_InputStream() throws IOException {
        TestIS is = new TestIS(7);
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,5,6}, IO.readByteArray(is, 7));

        is = new TestIS(7);
        Assert.assertArrayEquals(new byte[] {}, IO.readByteArray(is, 0));
        Assert.assertEquals(0, is._iBytesRead);

        try {
            IO.readByteArray(new TestIS(7), 8);
            Assert.fail("Expected " + EOFException.class);
        } catch (EOFException ex) {
            ex.printStackTrace(System.out);
        }
    }

    @Test
    public void testReadByteArrayMax_InputStream() throws IOException {

        TestIS is = new TestIS(7);
        byte[] abBuffer = new byte[7];
        try {
            IO.readByteArrayMax(is, abBuffer, 0, 0);
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace(System.out);
        }

        is = new TestIS(7);
        abBuffer = new byte[7];

        Assert.assertEquals(5, IO.readByteArrayMax(is, abBuffer, 0, 5));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,0,0}, abBuffer);

        is = new TestIS(7);
        abBuffer = new byte[7];
        Assert.assertEquals(7, IO.readByteArrayMax(is, abBuffer, 0, 7));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,5,6}, abBuffer);

        is = new TestIS(8);
        abBuffer = new byte[7];
        try {
            IO.readByteArrayMax(is, abBuffer, 0, 8);
            Assert.fail("Expected " + IndexOutOfBoundsException.class);
        } catch (IndexOutOfBoundsException ex) { // the exact exception may be implementation specific
            ex.printStackTrace(System.out);
        }

        is = new TestIS(5);
        abBuffer = new byte[7];
        Assert.assertEquals(5, IO.readByteArrayMax(is, abBuffer, 0, 7));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,0,0}, abBuffer);
    }

    @Test
    public void testReadByteArray_RandomAccessFile() throws IOException {
        File rafFile = new File(TMP_FOLDER.getRoot(), "raf1");
        Assert.assertTrue(rafFile.createNewFile());

        TestRAF is = new TestRAF(rafFile, 7);
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,5,6}, IO.readByteArray(is, 7));
        is.close();

        is = new TestRAF(rafFile, 7);
        Assert.assertArrayEquals(new byte[] {}, IO.readByteArray(is, 0));
        Assert.assertEquals(0, is._mockStream._iBytesRead);
        is.close();

        is = new TestRAF(rafFile, 7);
        try {
            IO.readByteArray(is, 8);
            Assert.fail("Expected " + EOFException.class);
        } catch (EOFException ex) {
            ex.printStackTrace(System.out);
        }
        is.close();
    }

    @Test
    public void testReadByteArrayMax_RandomAccessFile() throws IOException {
        File rafFile = new File(TMP_FOLDER.getRoot(), "raf2");
        Assert.assertTrue(rafFile.createNewFile());

        TestRAF is = new TestRAF(rafFile, 7);
        byte[] abBuffer = new byte[7];
        try {
            IO.readByteArrayMax(is, abBuffer, 0, 0);
            Assert.fail("Expected " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace(System.out);
        }
        is.close();

        is = new TestRAF(rafFile, 7);
        abBuffer = new byte[7];
        Assert.assertEquals(5, IO.readByteArrayMax(is, abBuffer, 0, 5));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,0,0}, abBuffer);
        is.close();

        is = new TestRAF(rafFile, 7);
        abBuffer = new byte[7];
        Assert.assertEquals(7, IO.readByteArrayMax(is, abBuffer, 0, 7));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,5,6}, abBuffer);
        is.close();

        is = new TestRAF(rafFile, 8);
        abBuffer = new byte[7];
        try {
            IO.readByteArrayMax(is, abBuffer, 0, 8);
            Assert.fail("Expected " + IndexOutOfBoundsException.class);
        } catch (IndexOutOfBoundsException ex) { // the exact exception may be implementation specific
            ex.printStackTrace(System.out);
        }
        is.close();

        is = new TestRAF(rafFile, 5);
        abBuffer = new byte[7];
        Assert.assertEquals(5, IO.readByteArrayMax(is, abBuffer, 0, 7));
        Assert.assertArrayEquals(new byte[] {0,1,2,3,4,0,0}, abBuffer);
        is.close();
    }

}
