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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.*;
import static org.junit.Assert.*;

public class DemuxedDataTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private static int iSectorCounter = 0;

    @Before
    public void setUp() {
        iSectorCounter = 0;
    }

    @After
    public void tearDown() {
    }

    static class DDPiece implements DemuxedData.Piece {
        private final byte[] _abData;
        private final int _iSector = iSectorCounter++;
        public DDPiece(byte[] ab) {
            _abData = ab;
        }
        public DDPiece(int iStart, int iEnd) {
            _abData = sequenceArray(iStart, iEnd);
        }
        public int getDemuxPieceSize() {
            return _abData.length;
        }
        public byte getDemuxPieceByte(int i) {
            return _abData[i];
        }
        public void copyDemuxPieceData(byte[] abOut, int iOutPos) {
            System.arraycopy(_abData, 0, abOut, iOutPos, _abData.length);
        }
        public int getSectorNumber() {
            return _iSector;
        }
    }

    private static byte[] sequenceArray(int iStart, int iEnd) {
        byte[] ab = new byte[iEnd - iStart + 1];
        for (int i = 0; i < ab.length; i++) {
            ab[i] = (byte)(iStart + i);
        }
        return ab;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_empty1() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                new ArrayList<DDPiece>());
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_empty2() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                new ArrayList<DDPiece>(), 0, 0);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_badidx1() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), -1, 5);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_badidx2() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), 5, 20);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_badidx3() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), 5, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_trim1() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(new byte[0]),
                new DDPiece(1, 4)
        ));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_trim2() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4),
                new DDPiece(new byte[0])
        ));
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_trim3() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4),
                new DDPiece(10, 13)
        ), 4, 2);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testBadConstruction_trim4() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4),
                new DDPiece(10, 13)
        ), 0, 0);
    }

    @Test
    public void testEmptyData_emptyArray0() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(new byte[0])));
        assertEquals(0, demux.getDemuxSize());
        assertEquals(0, demux.getStartDataOffset());
        assertEquals(0, demux.getEndDataOffset());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }
    @Test
    public void testEmptyData_emptyArray1() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(new byte[0])), 0, 0);
        assertEquals(0, demux.getDemuxSize());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }
    public void testEmptyData_emptyArray2() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(new byte[0]), new DDPiece(new byte[0])), 0, 0);
        assertEquals(0, demux.getDemuxSize());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }
    @Test
    public void testEmptyData_0() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), 0, 0);
        assertEquals(0, demux.getDemuxSize());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }
    @Test
    public void testEmptyData_mid() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), 5, 5);
        assertEquals(0, demux.getDemuxSize());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }
    @Test
    public void testEmptyData_max() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(
                Arrays.asList(new DDPiece(0, 10)), 11, 11);
        assertEquals(0, demux.getDemuxSize());
        assertArrayEquals(new byte[0], demux.copyDemuxData());
    }

    @Test
    public void testGetPieceCount1() {
        DDPiece p1 = new DDPiece(0, 10);
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(p1));
        assertEquals(1, demux.getPieceCount());
        assertEquals(p1.getSectorNumber(), demux.getStartSector());
        assertEquals(p1.getSectorNumber(), demux.getEndSector());
        Iterator<DDPiece> it = demux.iterator();
        assertSame(p1, it.next());
        assertFalse(it.hasNext());
    }
    @Test
    public void testGetPieceCount2() {
        DDPiece p1 = new DDPiece(0, 10);
        DDPiece p2 = new DDPiece(20, 30);
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(p1, p2));
        assertEquals(2, demux.getPieceCount());
        assertEquals(p1.getSectorNumber(), demux.getStartSector());
        assertEquals(p2.getSectorNumber(), demux.getEndSector());
        Iterator<DDPiece> it = demux.iterator();
        assertSame(p1, it.next());
        assertSame(p2, it.next());
        assertFalse(it.hasNext());
    }


    @Test
    public void testDemux1a() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4)
        ));
        assertEquals(4, demux.getDemuxSize());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, demux.copyDemuxData());
    }
    @Test
    public void testDemux1b() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4)
        ), 1, 3);
        assertEquals(2, demux.getDemuxSize());
        assertArrayEquals(new byte[] {2, 3}, demux.copyDemuxData());
    }

    @Test
    public void testDemux2a() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4),
                new DDPiece(10, 13)
        ));
        assertEquals(8, demux.getDemuxSize());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 10, 11, 12, 13}, demux.copyDemuxData());
    }

    @Test
    public void testDemux2b() {
        DemuxedData<DDPiece> demux = new DemuxedData<DDPiece>(Arrays.asList(
                new DDPiece(1, 4),
                new DDPiece(10, 13)
        ), 2, 2);
        assertEquals(4, demux.getDemuxSize());
        assertArrayEquals(new byte[] {3, 4, 10, 11}, demux.copyDemuxData());
    }

}
