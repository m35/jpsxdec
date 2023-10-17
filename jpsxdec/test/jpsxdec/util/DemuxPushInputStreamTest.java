/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import jpsxdec.util.DemuxedDataTest.DDPiece;
import org.junit.*;
import static org.junit.Assert.*;


public class DemuxPushInputStreamTest {

    public DemuxPushInputStreamTest() {
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
    public void test11() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 10);
        DDPiece p2 = new DDPiece(20, 29);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);
        x.close();
        assertEquals(20, x.skip(30));
        assertEquals(-1, x.skip(30));
    }

    @Test
    public void test10() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 10);
        DDPiece p2 = new DDPiece(20, 29);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);

        x.mark(18);
        boolean blnLooped = true;
        do {
            assertEquals(15, x.skip(15));
            assertEquals(5, x.available());
            assertEquals(25, x.read());
            blnLooped = !blnLooped;
            x.reset();
        } while (!blnLooped);

        x.close();
        assertEquals(26, x.read());
        assertEquals(27, x.read());
        assertEquals(28, x.read());
        assertEquals(29, x.read());
        assertEquals(-1, x.read());

    }

    @Test(expected = DemuxPushInputStream.NeedsMoreData.class)
    public void test9() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 10);
        DDPiece p2 = new DDPiece(20, 29);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);

        assertEquals(15, x.skip(15));
        assertEquals(5, x.available());
        assertEquals(25, x.read());
        assertEquals(26, x.read());
        assertEquals(27, x.read());
        assertEquals(28, x.read());
        assertEquals(29, x.read());
        x.read(); // throw
    }

    @Test
    public void test8() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 10);
        DDPiece p2 = new DDPiece(20, 29);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);

        x.mark(7);
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(4, x.read());
        assertEquals(5, x.read());
        DemuxedData<DDPiece> d = x.getMarkToReadDemux();
        assertEquals(1, d.getPieceCount());
        assertEquals(5, d.getDemuxSize());
        assertEquals(0, d.getStartDataOffset());
        assertEquals(5, d.getEndDataOffset());
        assertEquals(p1.getSectorNumber(), d.getEndSector());
        assertEquals(p1.getSectorNumber(), d.getStartSector());
    }


    @Test
    public void test7() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(new byte[0]);
        DDPiece p2 = new DDPiece(7, 11);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);

        x.mark(5);
        try {
            x.read();
            fail();
        } catch (DemuxPushInputStream.NeedsMoreData ex) {
        }
        x.reset();

        x.mark(5);
        x.addPiece(p2);
        assertEquals(7, x.read());

        x.reset();
        x.mark(5);
        assertEquals(7, x.read());
    }

    @Test
    public void test6() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 3);
        DDPiece p2 = new DDPiece(7, 11);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);

        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        x.mark(5);
        assertEquals(7, x.read());
        assertEquals(8, x.read());
        DemuxedData<DDPiece> d = x.getMarkToReadDemux();
        assertEquals(1, d.getPieceCount());
        assertEquals(2, d.getDemuxSize());
        assertEquals(0, d.getStartDataOffset());
        assertEquals(2, d.getEndDataOffset());
        assertEquals(p2.getSectorNumber(), d.getEndSector());
        assertEquals(p2.getSectorNumber(), d.getStartSector());
    }

    @Test
    public void test5() throws DemuxPushInputStream.NeedsMoreData {
        DDPiece p1 = new DDPiece(1, 3);
        DDPiece p2 = new DDPiece(7, 11);
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(p1);
        x.addPiece(p2);

        assertEquals(1, x.read());
        x.mark(5);
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(7, x.read());
        assertEquals(8, x.read());
        DemuxedData<DDPiece> d = x.getMarkToReadDemux();
        assertEquals(2, d.getPieceCount());
        assertEquals(4, d.getDemuxSize());
        assertEquals(1, d.getStartDataOffset());
        assertEquals(2, d.getEndDataOffset());
        assertEquals(p1.getSectorNumber(), d.getStartSector());
        assertEquals(p2.getSectorNumber(), d.getEndSector());
    }

    @Test
    public void test4() throws DemuxPushInputStream.NeedsMoreData {
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(new DDPiece(1, 3));
        x.addPiece(new DDPiece(7, 11));
        x.mark(4);
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(7, x.read());
        assertEquals(8, x.read());
        assertEquals(9, x.read());
        x.reset(); // arguably should be earlier
        assertEquals(10, x.read());
    }

    @Test
    public void test3() throws DemuxPushInputStream.NeedsMoreData {
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(new DDPiece(1, 3));
        assertEquals(3, x.available());
        x.addPiece(new DDPiece(7, 11));
        assertEquals(8, x.available());
        x.mark(5);
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(7, x.read());
        assertEquals(8, x.read());
        assertEquals(3, x.available());
        x.reset();
        assertEquals(8, x.available());
        x.mark(5);
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(7, x.read());
        assertEquals(8, x.read());
        x.reset();
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        assertEquals(7, x.read());
        assertEquals(8, x.read());
    }

    @Test
    public void test2() throws DemuxPushInputStream.NeedsMoreData {
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(new DDPiece(1, 3));
        assertEquals(3, x.available());
        x.mark(5);
        assertEquals(3, x.available());
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
        x.reset();
        assertEquals(3, x.available());
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
    }

    @Test
    public void test1() throws DemuxPushInputStream.NeedsMoreData {
        DemuxPushInputStream<DDPiece> x = new DemuxPushInputStream<DDPiece>(new DDPiece(1, 3));
        assertEquals(3, x.available());
        assertEquals(1, x.read());
        assertEquals(2, x.read());
        assertEquals(3, x.read());
    }


}
