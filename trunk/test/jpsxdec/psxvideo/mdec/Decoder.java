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

package jpsxdec.psxvideo.mdec;

import java.io.InputStream;
import jpsxdec.formats.YCbCrImage;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;
import jpsxdec.psxvideo.mdec.idct.simple_idct;
import jpsxdec.util.IO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class Decoder {

    public Decoder() {
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

    private final int W = 320, H = 224;
    private final MdecInputStreamReader reader =
            new MdecInputStreamReader(Decoder.class.getResourceAsStream("FMV056[0]_320x224[133].mdec"));
    private final int[] aiRgb = new int[W*H];
    private final YCbCrImage ycbcr = new YCbCrImage(W, H);
    
    @Test
    public void double_psxIDCT_rgb() throws Exception {
        MdecDecoder_double mdec = new MdecDecoder_double(new PsxMdecIDCT_double(), W, H);
        mdec.decode(reader);
        mdec.readDecodedRgb(W, H, aiRgb);
        int[] aiExpected = IO.readBEIntArray(Decoder.class.getResourceAsStream("dbl_psx.rgb"), aiRgb.length);
        assertArrayEquals(aiExpected, aiRgb);
    }

    @Test
    public void double_stephenIDCT_rgb() throws Exception {
        MdecDecoder_double mdec = new MdecDecoder_double(new StephensIDCT(), W, H);
        mdec.decode(reader);
        mdec.readDecodedRgb(W, H, aiRgb);
        int[] aiExpected = IO.readBEIntArray(Decoder.class.getResourceAsStream("dbl_stephen.rgb"), aiRgb.length);
        assertArrayEquals(aiExpected, aiRgb);
    }

    @Test
    public void double_psxIDCT_ycbcr() throws Exception {
        MdecDecoder_double mdec = new MdecDecoder_double(new PsxMdecIDCT_double(), W, H);
        mdec.decode(reader);
        mdec.readDecoded_Rec601_YCbCr420(ycbcr);
        InputStream is = Decoder.class.getResourceAsStream("dbl_psx.yuv");
        byte[] abExpected;
        abExpected = new byte[ycbcr.getY().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getY());
        abExpected = new byte[ycbcr.getCb().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getCb());
        abExpected = new byte[ycbcr.getCr().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getCr());
        is.close();
    }

    @Test
    public void double_stephenIDCT_ycbcr() throws Exception {
        MdecDecoder_double mdec = new MdecDecoder_double(new StephensIDCT(), W, H);
        mdec.decode(reader);
        mdec.readDecoded_Rec601_YCbCr420(ycbcr);
        InputStream is = Decoder.class.getResourceAsStream("dbl_stephen.yuv");
        byte[] abExpected;
        abExpected = new byte[ycbcr.getY().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getY());
        abExpected = new byte[ycbcr.getCb().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getCb());
        abExpected = new byte[ycbcr.getCr().length];
        IO.readByteArray(is, abExpected);
        assertArrayEquals(abExpected, ycbcr.getCr());
        is.close();
    }
    
    @Test
    public void int_psxIDCT_rgb() throws Exception {
        MdecDecoder_int mdec = new MdecDecoder_int(new PsxMdecIDCT_int(), W, H);
        mdec.decode(reader);
        mdec.readDecodedRgb(W, H, aiRgb);
        int[] aiExpected = IO.readBEIntArray(Decoder.class.getResourceAsStream("int_psx.rgb"), aiRgb.length);
        assertArrayEquals(aiExpected, aiRgb);
    }

    @Test
    public void int_simpleIDCT_rgb() throws Exception {
        MdecDecoder_int mdec = new MdecDecoder_int(new simple_idct(), W, H);
        mdec.decode(reader);
        mdec.readDecodedRgb(W, H, aiRgb);
        int[] aiExpected = IO.readBEIntArray(Decoder.class.getResourceAsStream("int_simple.rgb"), aiRgb.length);
        assertArrayEquals(aiExpected, aiRgb);
    }

}