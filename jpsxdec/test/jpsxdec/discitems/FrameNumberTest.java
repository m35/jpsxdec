/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2017  Michael Sabin
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

package jpsxdec.discitems;

import java.util.Map;
import jpsxdec.util.DeserializationFail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import testutil.PairList;

public class FrameNumberTest {

    public FrameNumberTest() {
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
    public void range() throws Exception {
        PairList<String, FrameNumber[]> test = new PairList<String, FrameNumber[]>();
        test.add("1/@2/#3-4/@5/#6", new FrameNumber[]{new FrameNumber(1,2,0,3,0), new FrameNumber(4,5,0,6,0)});
        test.add("7/@8.9/#10.11-12/@13.14/#15.16", new FrameNumber[]{new FrameNumber(7,8,9,10,11), new FrameNumber(12,13,14,15,16)});
        
        for (Map.Entry<String, FrameNumber[]> entry : test) {
            assertArrayEquals(entry.getKey(), entry.getValue(), FrameNumber.parseRange(entry.getKey()));
            FrameNumber[] aoRng = entry.getValue();
            assertEquals(entry.getKey(), entry.getKey(), FrameNumber.toRange(aoRng[0], aoRng[1]));
        }
    }
    @Test
    public void badRange() {
        String[] test = {
            "1-",
            "-1",
            "",
            "-",
            "--",
            "1--2",
            "-1-2",
            "1-2-3",
            "a",
            "a-1",
            "a-1",
        };
        for (String entry : test) {
            try {
                FrameNumber[] ao = FrameNumber.parseRange(entry);
                fail(entry);
            } catch (DeserializationFail ex) {
            }
        }
    }

    @Test
    public void testSerialize() throws Exception {
        FrameNumber[] aoTests = {
            new FrameNumber(1, 2, 0, 3, 0),
            new FrameNumber(1, 2, 1, 3, 0),
            new FrameNumber(1, 2, 0, 3, 1),
            new FrameNumber(1, 2, 1, 3, 1),
            //new FrameNumber(1, 2, 0, -1, 1),
            //new FrameNumber(1, 2, 1, -1, 1),
        };

        for (FrameNumber fn : aoTests) {
            assertEquals(fn.serialize(), new FrameNumber(fn.serialize()).serialize());
        }
    }

    @Test
    public void serialize() throws Exception {
        PairList<String, FrameNumber> test = new PairList<String, FrameNumber>();
        test.add("1/@1/#1", new FrameNumber(1, 1, 0, 1, 0));
        test.add("2/@3/#5.6", new FrameNumber(2, 3, 0, 5, 6));
        test.add("2/@3.4/#5", new FrameNumber(2, 3, 4, 5, 0));
        test.add("2/@3.4/#5.6", new FrameNumber(2, 3, 4, 5, 6));
        test.add("21/@32.43/#54.65", new FrameNumber(21, 32, 43, 54, 65));

        for (Map.Entry<String, FrameNumber> entry : test) {
            assertEquals(entry.getKey(), entry.getValue(), new FrameNumber(entry.getKey()));
            assertEquals(entry.getKey(), entry.getKey(), entry.getValue().serialize());
        }
    }

    @Test
    public void badSerialize() {
        String[] test = {
            "",
            "1/@1./#1",
            "1/@1/#1.",
            "1/#1/@1",
            "1/1/1",
            "//",
            "1/@1/#1 ",
            " 1/@1/#1",
            "1/@1/#1\\n",
            "\\n1/@1/#1",
        };
        for (String entry : test) {
            try {
                FrameNumber h = new FrameNumber(entry);
                fail(entry);
            } catch (DeserializationFail ex) {
            }
        }
    }


}
