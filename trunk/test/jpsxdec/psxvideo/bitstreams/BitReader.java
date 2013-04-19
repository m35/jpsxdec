/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2013  Michael Sabin
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
    public void test() throws EOFException {
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

        ArrayBitReader abr = new ArrayBitReader(abTest, false);

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

        sb.setLength(0);
        abr = new ArrayBitReader(abTest, false);

        int i;
        for (i = rand.nextInt(31)+1; i < abr.getBitsRemaining(); i = rand.nextInt(31)+1) {
            String s = abr.peekBitsToString(i);
            System.out.println("Reading " + i + " " + s);
            sb.append(s);
            abr.skipBits(i);
        }
        i = abr.getBitsRemaining();
        if (i > 0) {
            String s = abr.peekBitsToString(i);
            System.out.println("Reading " + i + " " + s);
            sb.append(s);
            abr.skipBits(i);
        }

        final String READ_BITS = sb.toString();
        assertEquals(READ_BITS, READ_BITS, BIT_STRING);
        

        long lngPeek, lngRead;

        abr.reset(abTest, true, 0);

        assertEquals("Bits remaining", abr.getBitsRemaining(), 48);
        String sPeek = abr.peekBitsToString(31);
        lngPeek = abr.peekUnsignedBits(31);
        lngRead = abr.readUnsignedBits(31);
        assertEquals(lngPeek+" == "+lngRead, lngPeek, lngRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 17);
        lngPeek = abr.peekUnsignedBits(3);
        lngRead = abr.readUnsignedBits(3);
        assertEquals(lngPeek+" == "+lngRead, lngPeek, lngRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 14);
        lngPeek = abr.peekUnsignedBits(3);
        lngRead = abr.readUnsignedBits(3);
        assertEquals(lngPeek+" == "+lngRead, lngPeek, lngRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 11);
        lngPeek = abr.peekUnsignedBits(11);
        lngRead = abr.readUnsignedBits(11);
        assertEquals(lngPeek+" == "+lngRead, lngPeek, lngRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 0);

        abr.reset(abTest, true, 0);
        abr.skipBits(5);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 43);
        abr.skipBits(30);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 13);
        lngPeek = abr.peekUnsignedBits(13);
        lngRead = abr.readUnsignedBits(13);
        assertEquals(lngPeek+" == "+lngRead, lngPeek, lngRead);
        assertEquals("Bits remaining", abr.getBitsRemaining(), 0);
    }


}