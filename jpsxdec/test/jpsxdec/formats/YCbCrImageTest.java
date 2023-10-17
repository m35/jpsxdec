/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
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

package jpsxdec.formats;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jpsxdec.util.IO;
import jpsxdec.util.aviwriter.AviWriterYV12;
import org.junit.Assert;
import org.junit.Test;
import testutil.Util;

public class YCbCrImageTest {

    private static final boolean GENERATE_EXPECTED = false;

    // TODO do a PSNR test of jpsxdec output and ffmpeg (and others) output

    private static BufferedImage readTestImage() {
        InputStream is = YCbCrImageTest.class.getResourceAsStream("testchartntscdv.png");
        BufferedImage bi;
        try {
            bi = ImageIO.read(is);
            is.close();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        return bi;
    }

    @Test
    public void testToPc601() throws Exception {
        BufferedImage bi = readTestImage();

        Pc601YCbCrImage actualYuv = new Pc601YCbCrImage(bi);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(actualYuv.getYBuff());
        baos.write(actualYuv.getCbBuff());
        baos.write(actualYuv.getCrBuff());
        baos.close();

        byte[] abActualYuv = baos.toByteArray();

        final String sExpectedFileName = getClass().getSimpleName()+".pc601-expected.yuv";

        if (GENERATE_EXPECTED) {
            IO.writeFile(sExpectedFileName, abActualYuv);

            String sMkvFileName = sExpectedFileName + ".mkv";

            File mkvFile = new File(sMkvFileName);
            /*
            MkvJYuvWriter mkv = new MkvJYuvWriter(mkvFile, bi.getWidth(), bi.getHeight(), new Fraction(15, 1), true);
            mkv.writeYCbCrFrame(actualYuv.getYBuff(), actualYuv.getCbBuff(), actualYuv.getCrBuff(), Fraction.ZERO);
            mkv.finish();
            mkv.close();
            */
            System.out.println("ffmpeg -i "+sMkvFileName+" "+sMkvFileName+"[%d].png");
            Assert.fail("visually compare the .png ffmpeg generates with the original and the other yuv format");
        } else {
            byte[] abExpectedYuv = Util.readResource(YCbCrImageTest.class, sExpectedFileName);
            Assert.assertArrayEquals(abExpectedYuv, abActualYuv);
        }
    }

    @Test
    public void testToRec601() throws Exception {
        BufferedImage bi = readTestImage();

        Rec601YCbCrImage actualYuv = new Rec601YCbCrImage(bi);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(actualYuv.getYBuff());
        baos.write(actualYuv.getCbBuff());
        baos.write(actualYuv.getCrBuff());
        baos.close();
        byte[] abActualYuv = baos.toByteArray();

        final String sExpectedFileName = getClass().getSimpleName()+".rec601-expected.yuv";

        if (GENERATE_EXPECTED) {
            IO.writeFile(sExpectedFileName, abActualYuv);

            final String sAviFileName = sExpectedFileName+".avi";

            File aviFile = new File(sAviFileName);
            AviWriterYV12 avi = new AviWriterYV12(aviFile, bi.getWidth(), bi.getHeight(), 15, 1);
            avi.write(actualYuv.getYBuff(), actualYuv.getCbBuff(), actualYuv.getCrBuff());
            avi.close();
            System.out.println("ffmpeg -i "+sAviFileName+" "+sAviFileName+"[%d].png");
            Assert.fail("visually compare the .png ffmpeg generates with the original and the other yuv format");
        } else {
            byte[] abExpectedYuv = Util.readResource(YCbCrImageTest.class, sExpectedFileName);
            Assert.assertArrayEquals(abExpectedYuv, abActualYuv);

            int i = 0;
            for (int y = 0; y < actualYuv.getHeight(); y++) {
                for (int x = 0; x < actualYuv.getWidth(); x++) {
                    Assert.assertEquals("Y(" + x + ", " + y  + ")", abExpectedYuv[i++] & 0xff, actualYuv.getY(x, y));
                }
            }
            for (int y = 0; y < actualYuv.getHeight()/2; y++) {
                for (int x = 0; x < actualYuv.getWidth()/2; x++) {
                    Assert.assertEquals("Cb(" + x + ", " + y  + ")", abExpectedYuv[i++] & 0xff, actualYuv.getCb(x, y));
                }
            }
            for (int y = 0; y < actualYuv.getHeight()/2; y++) {
                for (int x = 0; x < actualYuv.getWidth()/2; x++) {
                    Assert.assertEquals("Cr(" + x + ", " + y  + ")", abExpectedYuv[i++] & 0xff, actualYuv.getCr(x, y));
                }
            }
            Assert.assertEquals(abExpectedYuv.length, i);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testError1() {
        Pc601YCbCrImage pc = new Pc601YCbCrImage(5, 6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testError2() {
        Rec601YCbCrImage rec = new Rec601YCbCrImage(5, 6);
    }

    @Test
    public void testRec601toBI() throws Exception {
        Rec601YCbCrImage yuv = new Rec601YCbCrImage(readTestImage());

        BufferedImage actualBi = yuv.toBufferedImage();

        RgbIntImage actualRgb = new RgbIntImage(actualBi);

        final String EXPECTED_FILE = getClass().getSimpleName() + ".rec601-expected.rgb";

        if (GENERATE_EXPECTED) {
            ImageIO.write(actualBi, "png", new File(EXPECTED_FILE + ".png"));
            Util.writeIntArrayBE(EXPECTED_FILE, actualRgb.getData());
            Assert.fail();
        } else {
            int[] aiExpectedRgb = Util.readIntArrayBE(Util.readResource(YCbCrImageTest.class, EXPECTED_FILE));

            Assert.assertArrayEquals(aiExpectedRgb, actualRgb.getData());
        }
    }
}