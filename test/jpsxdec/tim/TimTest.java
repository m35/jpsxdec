/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

package jpsxdec.tim;

import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class TimTest {

    public TimTest() {
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
    public void recreate16bpp() throws IOException, NotThisTypeException {
        InputStream is = TimTest.class.getResourceAsStream("16BPP.TIM");
        byte[] abExpected = IO.readEntireStream(is);
        is.close();
        Tim expected = Tim.read(new ByteArrayInputStream(abExpected));

        File temp = File.createTempFile("timtest", ".png");
        ImageIO.write(expected.toBufferedImage(0), "png", temp);
        BufferedImage bi = ImageIO.read(temp);

        Tim actual = Tim.create(bi, expected.getBitsPerPixel());

        /* // Debug
        OutputStream fos = new FileOutputStream("tim2.tim");
        t2.write(new DataOutputStream(fos));
        fos.close();
        //*/

        assertArrayEquals(expected.getColorData(), actual.getColorData());
        assertArrayEquals(expected.getImageData(), actual.getImageData());
    }
    
    @Test
    public void rewrite16bpp() throws IOException, NotThisTypeException {
        InputStream is = TimTest.class.getResourceAsStream("16BPP.TIM");
        byte[] abTim = IO.readEntireStream(is);
        is.close();
        
        ByteArrayFPIS bais = new ByteArrayFPIS(abTim);
        Tim expectedTim = Tim.read(bais);
        System.out.println("Read " + bais.getOffset() + " bytes of " + abTim.length);
        ByteArrayOutputStream actual = new ByteArrayOutputStream(abTim.length);
        expectedTim.write(actual);

        byte[] abExpected = Misc.copyOfRange(abTim, 0, bais.getOffset());
        assertArrayEquals(abExpected, actual.toByteArray());
    }
    

    @Test
    public void readStrange16_4bpp() throws IOException, NotThisTypeException {
        InputStream is = TimTest.class.getResourceAsStream("00015063_.tim");
        byte[] abTim = IO.readEntireStream(is);
        is.close();

        ByteArrayFPIS bais = new ByteArrayFPIS(abTim);
        Tim tim = Tim.read(bais);
        int iBytesRead = bais.getOffset();
        System.out.println("Read " + bais.getOffset() + " bytes of " + abTim.length);
        assertEquals(66592, iBytesRead);

        assertEquals(4, tim.getBitsPerPixel());
        assertEquals(512, tim.getWidth());
        assertEquals(256, tim.getHeight());
        assertEquals(32, tim.getPaletteCount());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tim.write(baos);
        
        byte[] abExpected = Misc.copyOfRange(abTim, 0, iBytesRead);
        byte[] abActual = baos.toByteArray();
        abActual[4] = 0x0a; // set it to 16bpp + CLUT
        assertArrayEquals(abExpected, abActual);
    }
    
    @Test
    public void readStrange16_8bpp() throws IOException, NotThisTypeException {
        InputStream is = TimTest.class.getResourceAsStream("00015289_.tim");
        byte[] abTim = IO.readEntireStream(is);
        is.close();

        ByteArrayFPIS bais = new ByteArrayFPIS(abTim);
        Tim tim = Tim.read(bais);
        int iBytesRead = bais.getOffset();
        System.out.println("Read " + bais.getOffset() + " bytes of " + abTim.length);
        assertEquals(247328, iBytesRead);

        assertEquals(8, tim.getBitsPerPixel());
        assertEquals(512, tim.getWidth());
        assertEquals(480, tim.getHeight());
        assertEquals(3, tim.getPaletteCount());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tim.write(baos);
        
        byte[] abExpected = Misc.copyOfRange(abTim, 0, iBytesRead);
        byte[] abActual = baos.toByteArray();
        abActual[4] = 0x0a; // set it to 16bpp + CLUT
        assertArrayEquals(abExpected, abActual);
    }
    
}