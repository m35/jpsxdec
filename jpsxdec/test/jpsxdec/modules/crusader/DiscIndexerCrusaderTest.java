/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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

package jpsxdec.modules.crusader;

import jpsxdec.modules.crusader.DiscIndexerCrusader.DimCounter;
import jpsxdec.modules.video.Dimensions;
import org.junit.*;
import static org.junit.Assert.*;


public class DiscIndexerCrusaderTest {

    public DiscIndexerCrusaderTest() {
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
    public void testDimCounterSort() {
        compareMinToMax(320, 240, 1,
                        240, 176, 2);
        compareMinToMax(320, 240, 1,
                        240, 176, 1);
        compareMinToMax(240, 176, 1,
                        320, 240, 2);

        compareMinToMax(100, 200, 1,
                        120, 150, 2);
        compareMinToMax(120, 150, 1,
                        100, 200, 2);

        compareMinToMax(120, 150, 2,
                        320, 240, 2);
        compareMinToMax(120, 150, 2,
                        240, 176, 2);

        compareMinToMax(1000, 200, 2,
                        120, 150, 2);
        compareMinToMax(100, 2000, 2,
                        120, 150, 2);

        compareMinToMax(100, 200, 2,
                        120, 150, 2);
    }

    private static void compareMinToMax(int iWidthMin, int iHeightMin, int iCountMin,
                                        int iWidthMax, int iHeightMax, int iCountMax)
    {
        DimCounter min = new DimCounter(new Dimensions(iWidthMin, iHeightMin), iCountMin);
        DimCounter max = new DimCounter(new Dimensions(iWidthMax, iHeightMax), iCountMax);
        assertEquals(1, max.compareTo(min));
        assertEquals(-1, min.compareTo(max));
    }

    @Test
    public void testCompareAspectRatio() {
        compareWorstToBest(1,1,     240,176);
        compareWorstToBest(320,240, 240,176);
        compareWorstToBest(320,224, 240,176);
        compareWorstToBest(320,224, 320,240);
        compareWorstToBest(300,200, 320,240);
    }

    private static void compareWorstToBest(int iWorstWidth, int iWorstHeight,
                                           int iBestWidth, int iBestHeight)
    {
        Dimensions worst = new Dimensions(iWorstWidth, iWorstHeight);
        Dimensions best = new Dimensions(iBestWidth, iBestHeight);
        assertEquals(1, DiscIndexerCrusader.compareAspectRatio(best, worst));
        assertEquals(-1, DiscIndexerCrusader.compareAspectRatio(worst, best));
    }


}
