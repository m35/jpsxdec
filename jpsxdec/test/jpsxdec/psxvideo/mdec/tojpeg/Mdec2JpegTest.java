/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.psxvideo.mdec.tojpeg;

import java.io.ByteArrayOutputStream;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import org.junit.*;
import static org.junit.Assert.*;
import testutil.Util;

public class Mdec2JpegTest {

    public Mdec2JpegTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private static class MStream implements MdecInputStream {

        private final MdecCode[] _codes;
        private int _i = 0;

        public MStream(MdecCode[] _codes) {
            this._codes = _codes;
        }

        @Override
        public boolean readMdecCode(MdecCode code) {
            code.setFrom(_codes[_i++]);
            return code.isEOD();
        }
    }

    @Test
    public void acEnergyOverflow() throws Exception {
        MdecCode EOD = new MdecCode();
        EOD.setToEndOfData();
        MdecCode[] stream = {
            // Cr
            new MdecCode(10, 0), // Qscale, DC
            new MdecCode(62, 500),
            EOD,
        };

        Mdec2Jpeg jpeg = new Mdec2Jpeg(16, 16);
        try {
            jpeg.readMdec(new MStream(stream));
            fail("Too much energy exception should have been thrown");
        } catch (MdecException.TooMuchEnergy decode) {
            assertEquals(decode.getClass(), MdecException.TooMuchEnergy.class);
        }
    }

    @Test
    public void dcEnergyOverflow() throws Exception {
        MdecCode EOD = new MdecCode();
        EOD.setToEndOfData();

        MdecCode invalid9999 = new MdecCode(1, 511);
        Util.setField(invalid9999, "_iBottom10Bits", 9999); // mwahahahahaha

        MdecCode[] stream = {
            // Cr
            new MdecCode(1, -512), // Qscale, DC
            EOD,
            // Cb
            new MdecCode(1, 511), // Qscale, DC
            EOD,
            // Y1
            new MdecCode(1, -512), // Qscale, DC
            EOD,
            // Y2
            new MdecCode(1, 511), // Qscale, DC
            EOD,
            // Y3
            new MdecCode(1, -512), // Qscale, DC
            EOD,
            // Y4
            new MdecCode(1, -512), // Qscale, DC
            EOD,
        };

        Mdec2Jpeg jpeg = new Mdec2Jpeg(16, 16);
        jpeg.readMdec(new MStream(stream));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jpeg.writeJpeg(os);

        stream[stream.length-2] = invalid9999;
        jpeg.readMdec(new MStream(stream));
        try {
            jpeg.writeJpeg(os);
            fail("Expected assertion error");
        } catch (AssertionError ex) {
            ex.printStackTrace();
        }
    }



}
