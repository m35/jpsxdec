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

package jpsxdec.psxvideo.bitstreams;

import java.io.EOFException;
import java.util.Random;
import jpsxdec.util.Misc;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class BitReader {

    public BitReader() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testReadFixed() throws EOFException {
        final Random rand = new Random();

        byte[] abTest = new byte[6];
        rand.nextBytes(abTest);

        StringBuilder sb = new StringBuilder();
        for (byte b : abTest) {
            sb.append(Misc.bitsToString(b, 8));
        }
        final String BIT_STRING = sb.toString();
        System.out.println(BIT_STRING);
        sb.setLength(0);

        ArrayBitReader abr = new ArrayBitReader(abTest, abTest.length, false);

        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println("Reading " + 16);
        sb.append(abr.peekBitsToString(16));
        abr.skipBits(16);
        System.out.println(sb);

        int iPeek, iRead;

        abr.reset(abTest, abTest.length, true, 0);

        assertEquals("Bits remaining", abr.getBitsRemaining(), 48);
        String sPeek = abr.peekBitsToString(31);
        iPeek = abr.peekUnsignedBits(31);
        iRead = abr.readUnsignedBits(31);
        assertEquals(iPeek+" == "+iRead, iPeek, iRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 17);
        iPeek = abr.peekUnsignedBits(3);
        iRead = abr.readUnsignedBits(3);
        assertEquals(iPeek+" == "+iRead, iPeek, iRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 14);
        iPeek = abr.peekUnsignedBits(3);
        iRead = abr.readUnsignedBits(3);
        assertEquals(iPeek+" == "+iRead, iPeek, iRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 11);
        iPeek = abr.peekUnsignedBits(11);
        iRead = abr.readUnsignedBits(11);
        assertEquals(iPeek+" == "+iRead, iPeek, iRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 0);

        abr.reset(abTest, abTest.length, true, 0);
        abr.skipBits(5);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 43);
        abr.skipBits(30);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 13);
        iPeek = abr.peekUnsignedBits(13);
        iRead = abr.readUnsignedBits(13);
        assertEquals(iPeek+" == "+iRead, iPeek, iRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 0);

    }

    @Test
    public void testMod() {
        for (int i = -500; i <= 0; i++) {
            assertEquals(i % 16, -((-i) & 0xf));
            assertEquals(-(i / 16)*2, ((-i) >> 4) << 1);
        }
    }


    @Test
    public void testReadRandom() throws EOFException {
        final Random rand = new Random();
        
        byte[] abTest = new byte[8];
        rand.nextBytes(abTest);

        StringBuilder sb = new StringBuilder();
        for (byte b : abTest) {
            sb.append(Misc.bitsToString(b, 8));
        }
        final String BIT_STRING = sb.toString();
        System.out.println(BIT_STRING);
        sb.setLength(0);

        sb.setLength(0);
        ArrayBitReader abr = new ArrayBitReader(abTest, abTest.length, false);

        int iRead = 0;
        int i;
        abr.skipBits(0);
        assertEquals("peekSignedBits(0)", 0, abr.peekSignedBits(0));
        for (i = rand.nextInt(31); abr.getBitsRemaining() > 0; i = rand.nextInt(31)) {
            String s = abr.peekBitsToString(i);
            System.out.println("Peeked " + i + " " + s);
            sb.append(s);
            System.out.println("@" + iRead + " Skipping " + i);
            try {
                abr.skipBits(i);
                iRead += i;
                assertEquals("getBitsReamining()", BIT_STRING.length()-iRead, abr.getBitsRemaining());
                assertEquals("getBitsRead()", iRead, abr.getBitsRead());
            } catch (EOFException ex) {
                assertTrue(iRead + i > BIT_STRING.length());
            }
        }
        assertEquals("getBitsReamining()", 0, abr.getBitsRemaining());
        assertEquals("getBitsRead()", BIT_STRING.length(), abr.getBitsRead());
        abr.skipBits(0);
        assertEquals("peekSignedBits(0)", 0, abr.peekSignedBits(0));
        assertEquals("getBitsReamining()", 0, abr.getBitsRemaining());
        assertEquals("getBitsRead()", BIT_STRING.length(), abr.getBitsRead());

        final String READ_BITS = sb.toString();
        assertTrue(READ_BITS, BIT_STRING.startsWith(READ_BITS));
    }

    @Test
    public void testPerformance() throws EOFException {
        byte[] abData = new byte[100000];
        ArrayBitReader[] aoReaders = {
            new LoopSkip(),
            new ModSkip(),
            new BitSkip(),
        };
        long[] alngDuration = new long[aoReaders.length];

        for (int i = 0; i < aoReaders.length; i++) {
            ArrayBitReader reader = aoReaders[i];
            long lngStart, lngEnd;
            lngStart = System.currentTimeMillis();
            for (int iTimes = 0; iTimes < 5000; iTimes++) {
                reader.reset(abData, abData.length, true, 0);
                try {
                    int iSkipBits = 0;
                    for (;; iSkipBits = (iSkipBits + 1) & 0x1F) {
                        reader.skipBits(iSkipBits);
                    }
                } catch (EOFException ex) {
                }
            }
            lngEnd = System.currentTimeMillis();
            alngDuration[i] = lngEnd - lngStart;
        }

        for (int i = 0; i < aoReaders.length; i++) {
            System.out.println(aoReaders[i].getClass()+" "+alngDuration[i]);
        }

        assertTrue(alngDuration[2] + " !< " + alngDuration[0], alngDuration[2] < alngDuration[0]);
        assertTrue(alngDuration[2] + " !< " + alngDuration[1], alngDuration[2] < alngDuration[1]);

    }
    

    private static class LoopSkip extends ArrayBitReader {
        @Override
        public void skipBits(int iCount) throws EOFException {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iByteOffset += -(_iBitsLeft / 16)*2;
                _iBitsLeft = _iBitsLeft % 16;
                if (_iBitsLeft < 0) {
                    _iBitsLeft += 16;
                    _iByteOffset += 2;
                }
                if (_iByteOffset > _iDataSize) {
                    _iBitsLeft = 0;
                    _iByteOffset = _iDataSize;
                    throw new EOFException();
                } else if (_iBitsLeft > 0) {
                    _siCurrentWord = readWord(_iByteOffset-2);
                }
            }
        }
    }

    private static class BitSkip extends ArrayBitReader {
        @Override
        public void skipBits(int iCount) throws EOFException {

            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iByteOffset += ((-_iBitsLeft) >> 4) << 1;
                _iBitsLeft = -((-_iBitsLeft) & 0xf);
                if (_iByteOffset > _iDataSize) {
                    _iBitsLeft = 0;
                    _iByteOffset = _iDataSize;
                    throw new EOFException();
                } else if (_iBitsLeft < 0) {
                    if (_iByteOffset == _iDataSize) {
                        _iBitsLeft = 0;
                        throw new EOFException();
                    }
                    _iBitsLeft += 16;
                    _siCurrentWord = readWord(_iByteOffset);
                    _iByteOffset += 2;
                }
            }
        }
    }

    private static class ModSkip extends ArrayBitReader {
        @Override
        public void skipBits(int iCount) throws EOFException {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iByteOffset += -(_iBitsLeft / 16)*2;
                _iBitsLeft = _iBitsLeft % 16;
                if (_iByteOffset > _iDataSize) {
                    _iBitsLeft = 0;
                    _iByteOffset = _iDataSize;
                    throw new EOFException();
                } else if (_iBitsLeft < 0) {
                    if (_iByteOffset == _iDataSize) {
                        _iBitsLeft = 0;
                        throw new EOFException();
                    }
                    _iBitsLeft += 16;
                    _siCurrentWord = readWord(_iByteOffset);
                    _iByteOffset += 2;
                }
            }
        }
    }

}