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

package jpsxdec.adpcm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jpsxdec.util.IO;
import org.junit.*;
import static org.junit.Assert.*;


public class SpuDecodeCorruption {

    public SpuDecodeCorruption() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }



    @Test
    public void testCorruption1() throws Exception {
        final int[] aiBytes = {
                0xff,0xff,0xfe,0xdc,0xba,0x98,0x76,0x54,
                0x32,0x10,0xef,0xcd,0xab,0x89,0x67,0x45
        };
        String[] sExpectedLog = {
            "Range {0} > 12",
            "Bad SPU ADPCM header: Sound Parameter Filter Index[15 > 4, using 3] BitFlags[11111111] at Sound Units 0 Sample Frames Written 0 Channel 0 [corruption]"
        };
        short[] asiExpectedDecode = {
            -1, -3, -4, -5, -5, -5, -4, -3, -1, 1, 2, 3, 2, 1, 0, -1, -3, -4, -5, -5, -4, -4, -3, -2, -1, 1, 1, 2
        };

        new LogTest(sExpectedLog, asiExpectedDecode) {
            @Override
            public void process(ByteArrayOutputStream actual) throws Exception {
                SpuAdpcmDecoder.Mono spu = new SpuAdpcmDecoder.Mono(1.0);
                assertEquals(16, aiBytes.length);
                spu.decode(ai2BAIS(aiBytes), 1, actual);
                assertEquals(28*aiBytes.length/8, actual.size());
                assertTrue("Not corrupted", spu.hadCorruption());
            }
        }.run();
    }

    @Test
    public void testCorruption2() throws Exception {
        final int[] aiBytes = {
                0x3e,0x0f,0xfe,0xdc,0xba,0x98,0x76,0x54,
                0x32,0x10,0xef,0xcd,0xab,0x89,0x67,0x45
        };
        String[] sExpectedLog = {
            "Range {0} > 12",
            "Bad SPU ADPCM header: BitFlags[00001111] at Sound Units 0 Sample Frames Written 0 Channel 0 [corruption]"
        };
        short[] asiExpectedDecode = {
            -1, -3, -4, -5, -6, -7, -8, -8, -4, 1, 7, 10, 9, 6, 1, -4, -7, -9, -9, -7, -4, -3, -3, -4, -2, 1, 4, 7
        };

        new LogTest(sExpectedLog, asiExpectedDecode) {
            @Override
            public void process(ByteArrayOutputStream actual) throws Exception {
                SpuAdpcmDecoder.Mono spu = new SpuAdpcmDecoder.Mono(1.0);
                assertEquals(16, aiBytes.length);
                spu.decode(ai2BAIS(aiBytes), 1, actual);
                assertEquals(28*aiBytes.length/8, actual.size());
                assertTrue("Not corrupted", spu.hadCorruption());
            }
        }.run();
    }

    @Test
    public void testCorruption3() throws Exception {
        final int[] aiBytes = {
                0x5d,0x00,0xfe,0xdc,0xba,0x98,0x76,0x54,
                0x32,0x10,0xef,0xcd,0xab,0x89,0x67,0x45
        };
        String[] sExpectedLog = {
            "Range {0} > 12",
            "Bad SPU ADPCM header: Sound Parameter Filter Index[5 > 4, using 1] at Sound Units 0 Sample Frames Written 0 Channel 0 [corruption]"
        };
        short[] asiExpectedDecode = {
            -1, -2, -4, -6, -8, -11, -14, -17, -13, -9, -7, -4, -3, -2, -2, -2, -3, -3, -5, -7, -9, -12, -15, -18, -14, -10, -7, -5
        };

        new LogTest(sExpectedLog, asiExpectedDecode) {
            @Override
            public void process(ByteArrayOutputStream actual) throws Exception {
                SpuAdpcmDecoder.Mono spu = new SpuAdpcmDecoder.Mono(1.0);
                assertEquals(16, aiBytes.length);
                spu.decode(ai2BAIS(aiBytes), 1, actual);
                assertEquals(28*aiBytes.length/8, actual.size());
                assertTrue("Not corrupted", spu.hadCorruption());
            }
        }.run();
    }

    private static ByteArrayInputStream ai2BAIS(int ... ai) {
        byte[] ab = new byte[ai.length];
        for (int i = 0; i < ab.length; i++) {
            ab[i] = (byte) ai[i];
        }
        return new ByteArrayInputStream(ab);
    }


     private abstract static class LogTest extends Handler {

        private final String[] _asExpectedLog;
        private final short[] _asiExpectedDecode;
        private int _i = 0;

        public LogTest(String[] asExpectedLog, short[] asiExpectedDecode) {
            _asExpectedLog = asExpectedLog;
            _asiExpectedDecode = asiExpectedDecode;
        }

        public void run() throws Exception {
            ByteArrayOutputStream actual = new ByteArrayOutputStream();
            Logger log = Logger.getLogger("");
            log.addHandler(this);
            try {
                process(actual);
            } finally {
                log.removeHandler(this);
            }
            System.out.println("Expected: "+Arrays.toString(_asiExpectedDecode));
            System.out.print("Actual: ");
            printShorts(actual.toByteArray());
            assertArrayEquals(_asiExpectedDecode, readSInt16LE(actual.toByteArray()));
        }

        abstract public void process(ByteArrayOutputStream actual)
                throws Exception;

        @Override
        public void publish(LogRecord record) {
            String s = record.getMessage();
            System.out.println("Expected log: " + _asExpectedLog[_i]);
            System.out.println("Actual log: " + s);
            assertEquals(_asExpectedLog[_i], s);
            _i++;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    private static short[] readSInt16LE(byte[] ab) {
        assertTrue((ab.length % 2) == 0);
        short[] as = new short[ab.length / 2];
        for (int i = 0; i < as.length; i++) {
            as[i] = IO.readSInt16LE(ab, i*2);
        }
        return as;
    }

    private static void printShorts(byte[] ab) {
        short[] as = readSInt16LE(ab);
        System.out.println(Arrays.toString(as));
    }

}
