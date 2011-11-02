/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.util.IO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class LainCompressor {

    public LainCompressor() {
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
    public void test() throws Exception {
        BitStreamUncompressor_Lain uncompressor = new BitStreamUncompressor_Lain();
        byte[] abOriginal = IO.readEntireStream(LainCompressor.class.getResourceAsStream("F001[0]_320x240[03].bs"));
        uncompressor.reset(abOriginal);
        ParsedMdecImage parsed = new ParsedMdecImage(320, 240);
        parsed.readFrom(uncompressor);
        byte[] abCompressed = uncompressor.makeCompressor().compress(parsed.getStream(), parsed.getMdecCodeCount());

        // Don't compare the last byte (-1) because the original frame ends at
        // an 8 bit boundary and clearly the same buffer was used when
        // compressing frames because residuals of previous buffers can be seen
        // in the unused bytes so if we keep comparing we'll have many discrepancies
        for (int i = 0; i < abCompressed.length - 1; i++) {
            try {
                assertEquals(String.format("@%d %02x != %02x", i, abOriginal[i], abCompressed[i]),
                        abOriginal[i], abCompressed[i]);
            } catch (AssertionError ex) {
                IO.writeFile("F001[0]_320x240[03]re.bs", abCompressed);
                throw ex;
            }
        }
    }

}