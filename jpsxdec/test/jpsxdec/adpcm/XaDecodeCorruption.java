/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import org.junit.*;
import static org.junit.Assert.*;


public class XaDecodeCorruption {

    public XaDecodeCorruption() {
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
        testCorruption(55, 1,
                1, 0x80, 3, 0xff,
                "Sector 55 Sound Group.Unit 1.0 after Sample Frame 56 [corruption] sound parameter corrupted: [[0]=00000001, [1]=10000000 (bad), [2]=00000011, [3]=11111111 (bad)]. Chose [0]=00000001");
    }
    @Test
    public void testCorruption2() throws Exception {
        testCorruption(42, 3,
                1, 3, 4, 4,
                "Sector 42 Sound Group.Unit 3.0 after Sample Frame 168 [corruption] sound parameter corrupted: [[0]=00000001, [1]=00000011, [2],[3]=00000100, [3]=00000100]. Chose [2],[3]=00000100");
    }
    @Test
    public void testCorruption3() throws Exception {
        testCorruption(7, 0,
                0xa3, 0x61, 0x7f, 0x7f,
                "Sector 7 Sound Group.Unit 0.0 after Sample Frame 0 [corruption] sound parameter corrupted: [[0]=10100011 (bad), [1]=01100001 (bad), [2],[3]=01111111 (bad), [3]=01111111 (bad)]. Chose [0]=10100011 (bad) corrected to 51");
    }


    public void testCorruption(int iSector, int iSoundGroup,
            int iParam1, int iParam2, int iParam3, int iParam4, final String sExpected)
            throws Exception
    {
        final int iBitsPerSample = 8;
        byte[] abTest = new byte[CdSector.SECTOR_USER_DATA_SIZE_MODE2FORM2 - 20];
        abTest[XaAdpcmDecoder.SIZEOF_SOUND_GROUP*iSoundGroup+0] = (byte) iParam1;
        abTest[XaAdpcmDecoder.SIZEOF_SOUND_GROUP*iSoundGroup+4] = (byte) iParam2;
        abTest[XaAdpcmDecoder.SIZEOF_SOUND_GROUP*iSoundGroup+8] = (byte) iParam3;
        abTest[XaAdpcmDecoder.SIZEOF_SOUND_GROUP*iSoundGroup+12] = (byte) iParam4;
        InputStream inStream = new ByteArrayInputStream(abTest);
        OutputStream out = new ByteArrayOutputStream();
        LogTest handler = new LogTest() {
            @Override
            public void publish(LogRecord record) {
                String s = record.getMessage();
                System.out.println(sExpected);
                System.out.println(s);
                assertEquals(sExpected, s);
            }
        };
        Logger log = Logger.getLogger(XaAdpcmDecoder.class.getName());
        XaAdpcmDecoder instance = new XaAdpcmDecoder(iBitsPerSample, true, 1.0);
        log.addHandler(handler);
        try {
            instance.decode(inStream, out, iSector);
            assertTrue(instance.hadCorruption());
        } finally {
            log.removeHandler(handler);
        }
        assertEquals(1008, instance.getSampleFramesWritten());
        assertEquals(1008, XaAdpcmDecoder.pcmSampleFramesGeneratedFromXaAdpcmSector(iBitsPerSample, true));
    }

    private static abstract class LogTest extends Handler {
        @Override
        abstract public void publish(LogRecord record);

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
