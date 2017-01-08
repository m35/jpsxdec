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

import jpsxdec.util.DebugLogger;
import jpsxdec.util.DeserializationFail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class FrameNumberFormatTest {

    public FrameNumberFormatTest() {
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
    public void serialize() throws Exception {
        String[] tests = {
            "0/@0/#0", // hmm
            "1/@1/#1",
            "1/@1.2/#1.3",
            "1/@1.2/#1",
            "1/@1/#1.3",
            "123/@456.78/#912.55",
        };
        for (String s : tests) {
            assertEquals(s, new FrameNumberFormat(s).serialize());
        }
    }

    @Test
    public void badSerialize() {
        String[] tests = {
            "",
            "/",
            "//",
            " 1/@1/#1",
            "1/@1/#1 ",
            "\\n1/@1/#1",
            "1/@1/#1\\n",
        };
        for (String s : tests) {
            try {
                FrameNumberFormat fnf = new FrameNumberFormat(s);
                fail(s);
            } catch (DeserializationFail ex) {
            }
        }
    }

    @Test
    public void dupHeader() throws Exception {
        FrameNumberFormatter.Header fnf = new FrameNumberFormatter.Header(1, 1);
        String actual = fnf.formatNumber(new FrameNumber(0, 0, 0, 1, 0), DebugLogger.Log);
        assertEquals("1.0", actual);
    }

    @Test
    public void dupSector() throws Exception {
        FrameNumberFormatter.Sector fnf = new FrameNumberFormatter.Sector(1, 1);
        String actual = fnf.formatNumber(new FrameNumber(0, 1, 0, 0, 0), DebugLogger.Log);
        assertEquals("1.0", actual);
    }
}
