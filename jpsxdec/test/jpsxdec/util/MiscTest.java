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

package jpsxdec.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import org.junit.*;
import static org.junit.Assert.*;

public class MiscTest {

    @Test
    public void testObjEq() {
        String x = "x";
        assertTrue(Misc.objectEquals(null, null));
        assertTrue(Misc.objectEquals(x, x));
        assertTrue(Misc.objectEquals(new ArrayList(), new ArrayList()));

        assertFalse(Misc.objectEquals("a", null));
        assertFalse(Misc.objectEquals(null, "b"));
        assertFalse(Misc.objectEquals("a", "b"));
    }

    @Test
    public void testDateFromSeconds() {
        assertEquals("0:00", MessageFormat.format("{0,time,m:ss}", Misc.dateFromSeconds(0)));
        assertEquals("0:01", MessageFormat.format("{0,time,m:ss}", Misc.dateFromSeconds(1)));
        assertEquals("1:30", MessageFormat.format("{0,time,m:ss}", Misc.dateFromSeconds(90)));
    }

    @Test
    @Ignore("TODO fix over 1 hour")
    public void testDateFromSeconds_over2hours() {
        String s = MessageFormat.format("{0,time,m:ss}", Misc.dateFromSeconds(60 * 60 * 2 + 60 * 5 + 10));
        assertEquals("2:05:10", s);
    }

    @Test
    public void testZeroPadString() {
        assertEquals("abcdef", Misc.zeroPadString("abcdef", 0, false));
        assertEquals("", Misc.zeroPadString("abcdef", 0, true));

        assertEquals("abcdef", Misc.zeroPadString("abcdef", 3, false));
        assertEquals("def", Misc.zeroPadString("abcdef", 3, true));

        assertEquals("abcdef", Misc.zeroPadString("abcdef", 6, false));
        assertEquals("abcdef", Misc.zeroPadString("abcdef", 6, true));

        assertEquals("0abcdef", Misc.zeroPadString("abcdef", 7, false));
        assertEquals("0abcdef", Misc.zeroPadString("abcdef", 7, true));

        String s = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000abcdef";

        assertEquals(s, Misc.zeroPadString("abcdef", 100, false));
        assertEquals(s, Misc.zeroPadString("abcdef", 100, true));
    }
}
