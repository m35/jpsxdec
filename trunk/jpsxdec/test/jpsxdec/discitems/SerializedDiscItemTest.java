/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2015  Michael Sabin
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

import jpsxdec.util.NotThisTypeException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class SerializedDiscItemTest {

    public SerializedDiscItemTest() {
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
    public void testSerialize_Index_Id() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;

        int iIndex;
        String sIndexId;
        SerializedDiscItem sdi;
        
        iIndex = -1;
        sIndexId = null;
        sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        try {
            serializeDeserialize(sdi);
            fail("Should not be able to deserialize withoud id# an index id");
        } catch (NotThisTypeException ex) {
        }

        iIndex = 0;
        sIndexId = null;
        sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        try {
            serializeDeserialize(sdi);
            fail("Should not be able to deserialize withoud id# an index id");
        } catch (NotThisTypeException ex) {
        }

        iIndex = -1;
        sIndexId = "taco";
        sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        try {
            serializeDeserialize(sdi);
            fail("Should not be able to deserialize withoud id# an index id");
        } catch (NotThisTypeException ex) {
        }

        iIndex = 4;
        sIndexId = "taco";
        sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        serializeDeserialize(sdi);
    }

    private void serializeDeserialize(SerializedDiscItem sdi) throws NotThisTypeException {
        String s = sdi.serialize();
        SerializedDiscItem newSdi = new SerializedDiscItem(s);
        assertEquals(sdi, newSdi);
    }

    @Test()
    public void testSerialize_BadSectorRange() {
        String sType = "Fish";
        int iIndex = 0;
        String sIndexId = "asd";
        try {
            SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, -1, 0);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, 0, -1);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
        try {
            SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, -1, -1);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void test_AddBad() {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);

        try {
            sdi.addString("", "z");
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            sdi.addDimensions("a", -1, 0);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            sdi.addDimensions("a", 0, -1);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            sdi.addRange("a", 0, -1);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            sdi.addRange("a", -1, 0);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            sdi.addFraction("a", -1, 0);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            sdi.addFraction("a", 0, -1);
            fail("Expepcted IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void test_AddString() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        addGetString(sdi, "a", "");
        addGetString(sdi, "b", "a");
        addGetString(sdi, "c", "_");
    }

    private void addGetString(SerializedDiscItem sdi, String sKey, String sValue) throws NotThisTypeException {
        sdi.addString(sKey, sValue);
        assertEquals(sValue, sdi.getString(sKey));

        try {
            sdi.getDimensions(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getFraction(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getInt(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLong(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getIntRange(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLongRange(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
    }

    @Test
    public void test_AddNumber() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        addGetInt(sdi, "x", -1);
        serializeDeserialize(sdi);
        addGetInt(sdi, "y", 0);
        serializeDeserialize(sdi);
        addGetInt(sdi, "z", 1);
        serializeDeserialize(sdi);
    }

    private void addGetInt(SerializedDiscItem sdi, String sKey, int iValue) throws NotThisTypeException {
        sdi.addNumber(sKey, iValue);
        assertEquals(iValue, sdi.getInt(sKey));
        assertEquals(iValue, sdi.getLong(sKey));
        sdi.getString(sKey);

        try {
            sdi.getDimensions(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getFraction(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getIntRange(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLongRange(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
    }

    @Test
    public void test_AddRange() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        addGetRange(sdi, "x", 0, 0);
        serializeDeserialize(sdi);
        addGetRange(sdi, "y", 0, 1);
        serializeDeserialize(sdi);
        addGetRange(sdi, "z", 1, 0);
        serializeDeserialize(sdi);
    }

    private void addGetRange(SerializedDiscItem sdi, String sKey, int iStart, int iEnd) throws NotThisTypeException {
        sdi.addRange(sKey, iStart, iEnd);
        assertArrayEquals(new int[] {iStart, iEnd}, sdi.getIntRange(sKey));
        assertArrayEquals(new int[] {iStart, iEnd}, sdi.getDimensions(sKey));
        assertArrayEquals(new long[] {iStart, iEnd}, sdi.getLongRange(sKey));
        assertArrayEquals(new long[] {iStart, iEnd}, sdi.getFraction(sKey));
        sdi.getString(sKey);
        try {
            sdi.getInt(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLong(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
    }

    @Test
    public void test_AddFraction() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        addGetFraction(sdi, "x", 0, 0);
        serializeDeserialize(sdi);
        addGetFraction(sdi, "y", 0, 1);
        serializeDeserialize(sdi);
        addGetFraction(sdi, "z", 1, 0);
        serializeDeserialize(sdi);
        addGetFraction(sdi, "a", Long.MAX_VALUE, 0);
        serializeDeserialize(sdi);
        addGetFraction(sdi, "b", 0, Long.MAX_VALUE);
        serializeDeserialize(sdi);
        addGetFraction(sdi, "c", Long.MAX_VALUE, Long.MAX_VALUE);
        serializeDeserialize(sdi);
    }

    private void addGetFraction(SerializedDiscItem sdi, String sKey, long lngNum, long lngDenom) throws NotThisTypeException {
        sdi.addFraction(sKey, lngNum, lngDenom);
        assertArrayEquals(new long[] {lngNum, lngDenom}, sdi.getFraction(sKey));
        sdi.getString(sKey);
        try {
            sdi.getInt(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLong(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
    }

    @Test
    public void testAddDimensions() throws NotThisTypeException {
        String sType = "Fish";
        int iSectorStart = 0;
        int iSectorEnd = 0;
        int iIndex = 0;
        String sIndexId = "taco";
        SerializedDiscItem sdi = new SerializedDiscItem(sType, iIndex, sIndexId, iSectorStart, iSectorEnd);
        addGetDimensions(sdi, "x", 0, 0);
        serializeDeserialize(sdi);
        addGetDimensions(sdi, "y", 0, 1);
        serializeDeserialize(sdi);
        addGetDimensions(sdi, "z", 1, 0);
        serializeDeserialize(sdi);
    }

    private void addGetDimensions(SerializedDiscItem sdi, String sKey, int iWidth, int iHeight) throws NotThisTypeException {
        sdi.addDimensions(sKey, iWidth, iHeight);
        assertArrayEquals(new int[] {iWidth, iHeight}, sdi.getDimensions(sKey));
        assertArrayEquals(new int[] {iWidth, iHeight}, sdi.getIntRange(sKey));
        assertArrayEquals(new int[] {iWidth, iHeight}, sdi.getDimensions(sKey));
        assertArrayEquals(new long[] {iWidth, iHeight}, sdi.getLongRange(sKey));
        assertArrayEquals(new long[] {iWidth, iHeight}, sdi.getFraction(sKey));
        sdi.getString(sKey);
        try {
            sdi.getInt(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
        try {
            sdi.getLong(sKey);
            fail("Expepcted NotThisTypeException");
        } catch (NotThisTypeException e) {}
    }

}
