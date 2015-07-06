/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2015  Michael Sabin
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

package jpsxdec.discitems.savers;

import java.util.ArrayList;
import java.util.Map;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.util.NotThisTypeException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import testutil.PairList;


public class FrameLookupTest {

    public FrameLookupTest() {
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
    public void deserialize() throws Exception {
        PairList<String, FrameLookup> test = new PairList<String, FrameLookup>();
        test.add("#1", FrameLookup.byHeader(1));
        test.add("#2", FrameLookup.byHeader(2));
        test.add("#3", FrameLookup.byHeader(3));

        for (Map.Entry<String, FrameLookup> entry : test) {
            assertEquals(entry.getKey(), entry.getValue(), FrameLookup.deserialize(entry.getKey()));
            assertEquals(entry.getKey(), entry.getKey(), entry.getValue().toString());
        }
    }
    @Test
    public void badDeserialize() {
        String[] test = {
            " 1",
            "1 ",
            "\\n1",
            "1\\n",
            "-1",
            "1.0",
            "",
            "a",
        };
        for (String entry : test) {
            try {
                FrameLookup fl = FrameLookup.deserialize(entry);
                fail(entry);
            } catch (NotThisTypeException ex) {
            }
        }
    }

    @Test
    public void range() throws Exception {
        PairList<String, FrameLookup[]> test = new PairList<String, FrameLookup[]>();
        test.add("#1", new FrameLookup[] {FrameLookup.byHeader(1)});
        test.add("#1-#1", new FrameLookup[] {FrameLookup.byHeader(1), FrameLookup.byHeader(1)});
        test.add("#2-#3", new FrameLookup[] {FrameLookup.byHeader(2), FrameLookup.byHeader(3)});

        for (Map.Entry<String, FrameLookup[]> entry : test) {
            assertArrayEquals(entry.getKey(), entry.getValue(), FrameLookup.parseRange(entry.getKey()));
        }
    }
    @Test
    public void badRange() {
        String[] test = {
            "1-",
            "-1",
            "",
            "1.1-2",
            "-",
            "--",
            "1--3",
            "-1-3",
            "1-2-3",
            "a",
            "a-1",
            "a-1",
        };
        for (String entry : test) {
            try {
                FrameLookup[] fl = FrameLookup.parseRange(entry);
                fail(entry);
            } catch (NotThisTypeException ex) {
            }
        }
    }

    @Test
    public void compare() {
        ArrayList<Integer> expecteds = new ArrayList<Integer>();
        PairList<FrameLookup, FrameNumber> test = new PairList<FrameLookup, FrameNumber>();
        test.add(FrameLookup.byHeader(1), new FrameNumber(4, 10, 3, 2, 0)); expecteds.add(-1);
        test.add(FrameLookup.byHeader(1), new FrameNumber(5, 11, 4, 1, 0)); expecteds.add(0);
        test.add(FrameLookup.byHeader(1), new FrameNumber(6, 12, 5, 1, 2)); expecteds.add(0);
        test.add(FrameLookup.byHeader(2), new FrameNumber(7, 13, 6, 1, 0)); expecteds.add(1);
        test.add(FrameLookup.byHeader(2), new FrameNumber(8, 14, 7, 1, 2)); expecteds.add(1);
        for (int i = 0; i < test.size(); i++) {
            Map.Entry<FrameLookup, FrameNumber> entry = test.get(i);
            int expected = expecteds.get(i);
            assertEquals(expected, entry.getKey().compareTo(entry.getValue()));
        }
    }

}
