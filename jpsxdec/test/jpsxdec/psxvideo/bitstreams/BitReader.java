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

package jpsxdec.psxvideo.bitstreams;

import java.util.Random;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;
import org.junit.*;
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
    public void testReadFixed() throws MdecException.EndOfStream {
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

        ArrayBitReader abr = new ArrayBitReader(abTest, BitStreamUncompressor_Lain.BIG_ENDIAN_ORDER, 0, abTest.length);

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

        abr = new ArrayBitReader(abTest, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER, 0, abTest.length);

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

        abr = new ArrayBitReader(abTest, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER, 0, abTest.length);
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
    public void testReadRandom() throws MdecException.EndOfStream {
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
        ArrayBitReader abr = new ArrayBitReader(abTest, BitStreamUncompressor_Lain.BIG_ENDIAN_ORDER, 0, abTest.length);

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
            } catch (MdecException.EndOfStream ex) {
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
    public void testPerformance() {
        byte[] abData = new byte[100000];
        long[] alngDuration = new long[MAKERS.length];

        for (int i = 0; i < MAKERS.length; i++) {
            long lngStart, lngEnd;
            lngStart = System.currentTimeMillis();
            for (int iTimes = 0; iTimes < 5000; iTimes++) {
                ArrayBitReader reader = MAKERS[i].make(abData, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER, 0, abData.length);
                try {
                    int iSkipBits = 0;
                    for (;; iSkipBits = (iSkipBits + 1) & 0x1F) {
                        reader.skipBits(iSkipBits);
                    }
                } catch (MdecException.EndOfStream ex) {
                }
            }
            lngEnd = System.currentTimeMillis();
            alngDuration[i] = lngEnd - lngStart;
        }

        for (int i = 0; i < MAKERS.length; i++) {
            System.out.println(MAKERS[i]+" "+alngDuration[i]);
        }

        assertTrue(alngDuration[1] + " !< " + alngDuration[0] + " if this fails it's because there is a faster way to implement the bit reader",
                   alngDuration[1] < alngDuration[0]);
        assertTrue(alngDuration[1] + " !< " + alngDuration[2] + " if this fails it's because there is a faster way to implement the bit reader",
                   alngDuration[1] < alngDuration[2]);
        assertTrue(alngDuration[1] + " !< " + alngDuration[3] + " if this fails it's because there is a faster way to implement the bit reader",
                   alngDuration[1] < alngDuration[3]);
    }

    private interface ReaderMaker {
        ArrayBitReader make(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset);
    }
    private static final ReaderMaker[] MAKERS = {
        new ReaderMaker() {
            public ArrayBitReader make(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
                return new PreCheckBitSkip(abData, byteOrder, iStartOffset, iEndOffset);
            }
            public String toString() { return PreCheckBitSkip.class.getSimpleName(); }
        },
        new ReaderMaker() {
            // This is the one implemented in ArrayBitReader
            public ArrayBitReader make(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
                return new PostCheckBitSkip(abData, byteOrder, iStartOffset, iEndOffset);
            }
            public String toString() { return PostCheckBitSkip.class.getSimpleName(); }
        },
        new ReaderMaker() {
            public ArrayBitReader make(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
                return new PreCheckModSkip(abData, byteOrder, iStartOffset, iEndOffset);
            }
            public String toString() { return PreCheckModSkip.class.getSimpleName(); }
        },
        new ReaderMaker() {
            public ArrayBitReader make(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
                return new PostCheckModSkip(abData, byteOrder, iStartOffset, iEndOffset);
            }
            public String toString() { return PostCheckModSkip.class.getSimpleName(); }
        },
    };

    private static class PreCheckBitSkip extends ArrayBitReader {

        public PreCheckBitSkip(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
            super(abData, byteOrder, iStartOffset, iEndOffset);
        }

        @Override
        public void skipBits(int iCount) throws MdecException.EndOfStream {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iCurrentOffset += ((-_iBitsLeft) >> 4) << 1;
                _iBitsLeft = -((-_iBitsLeft) & 0xf);
                if (_iBitsLeft < 0) {
                    _iBitsLeft += 16;
                    _iCurrentOffset += 2;
                }
                if (_iCurrentOffset > _iEndOffset) {
                    _iBitsLeft = 0;
                    _iCurrentOffset = _iEndOffset;
                    throw new MdecException.EndOfStream();
                } else if (_iBitsLeft > 0) {
                    _siCurrentShort = readShort(_iCurrentOffset-2);
                }
            }
        }
    }

    // This is the one implemented in ArrayBitReader
    private static class PostCheckBitSkip extends ArrayBitReader {

        public PostCheckBitSkip(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
            super(abData, byteOrder, iStartOffset, iEndOffset);
        }

        @Override
        public void skipBits(int iCount) throws MdecException.EndOfStream {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iCurrentOffset += ((-_iBitsLeft) >> 4) << 1;
                _iBitsLeft = -((-_iBitsLeft) & 0xf);
                if (_iCurrentOffset > _iEndOffset) {
                    _iBitsLeft = 0;
                    _iCurrentOffset = _iEndOffset;
                    throw new MdecException.EndOfStream();
                } else if (_iBitsLeft < 0) {
                    if (_iCurrentOffset == _iEndOffset) {
                        _iBitsLeft = 0;
                        throw new MdecException.EndOfStream();
                    }
                    _iBitsLeft += 16;
                    _siCurrentShort = readShort(_iCurrentOffset);
                    _iCurrentOffset += 2;
                }
            }
        }
    }

    private static class PreCheckModSkip extends ArrayBitReader {

        public PreCheckModSkip(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
            super(abData, byteOrder, iStartOffset, iEndOffset);
        }

        @Override
        public void skipBits(int iCount) throws MdecException.EndOfStream {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iCurrentOffset += -(_iBitsLeft / 16)*2;
                _iBitsLeft = _iBitsLeft % 16;
                if (_iBitsLeft < 0) {
                    _iBitsLeft += 16;
                    _iCurrentOffset += 2;
                }
                if (_iCurrentOffset > _iEndOffset) {
                    _iBitsLeft = 0;
                    _iCurrentOffset = _iEndOffset;
                    throw new MdecException.EndOfStream(_iCurrentOffset + " > " + _iEndOffset);
                } else if (_iBitsLeft > 0) {
                    _siCurrentShort = readShort(_iCurrentOffset-2);
                }
            }
        }
    }

    private static class PostCheckModSkip extends ArrayBitReader {

        public PostCheckModSkip(byte[] abData, IByteOrder byteOrder, int iStartOffset, int iEndOffset) {
            super(abData, byteOrder, iStartOffset, iEndOffset);
        }

        @Override
        public void skipBits(int iCount) throws MdecException.EndOfStream {
            _iBitsLeft -= iCount;
            if (_iBitsLeft < 0) {
                _iCurrentOffset += -(_iBitsLeft / 16)*2;
                _iBitsLeft = _iBitsLeft % 16;
                if (_iCurrentOffset > _iEndOffset) {
                    _iBitsLeft = 0;
                    _iCurrentOffset = _iEndOffset;
                    throw new MdecException.EndOfStream();
                } else if (_iBitsLeft < 0) {
                    if (_iCurrentOffset == _iEndOffset) {
                        _iBitsLeft = 0;
                        throw new MdecException.EndOfStream();
                    }
                    _iBitsLeft += 16;
                    _siCurrentShort = readShort(_iCurrentOffset);
                    _iCurrentOffset += 2;
                }
            }
        }
    }

}
