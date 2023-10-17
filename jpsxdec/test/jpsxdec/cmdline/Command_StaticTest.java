/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.cmdline;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStreamReader;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class Command_StaticTest {


    @Test
    public void testValidate() {
        Command_Static testSubject = new Command_Static();
        assertNull(testSubject.validate("bs"));
        assertNull(testSubject.validate("mdec"));
        assertNotNull(testSubject.validate("invalid"));
    }

    @Test
    public void testBadArgs() throws CommandLineException {
        testBadArgs("-dim", "bad");
        testBadArgs("-dim", "1");
        testBadArgs("-dim", "x");
        testBadArgs("-dim", "1xa");
        testBadArgs("-dim", "0x0x0");
        testBadArgs("-dim", "16x16", "-fmt", "bad format");
        testBadArgs("-dim", "16x16", "-fmt", "avi:rgb");
        testBadArgs("-dim", "16x16", "-fmt", "mdec");
        testBadArgs("-dim", "16x16", "-fmt", "png", "-q", "high");
        testBadArgs("-dim", "16x16", "-fmt", "png", "-up", "bad upsample");
    }

    private void testBadArgs(String... asArgs) {
        ArgParser ap = new ArgParser(asArgs);
        Command_Static testSubject = new Command_Static();
        FeedbackStream fbs = new FeedbackStream();
        InFileAndIndexArgs inFileIdx = new InFileAndIndexArgs(null, null, fbs);

        assertNull(testSubject.validate("mdec"));
        testSubject.init(new ArgParser(new String[] {"-static"}), inFileIdx, fbs);

        try {
            testSubject.execute(ap);
            fail("Expected " + CommandLineException.class);
        } catch (CommandLineException ex) {
            // expected
        }
    }

    private static final String TEST_BS_FILE = Command_StaticTest.class.getSimpleName() + ".bs";
    private static final String TEST_MDEC_FILE = Command_StaticTest.class.getSimpleName() + ".mdec";

    @Test
    public void testBs2Mdec() throws Exception {
        // put an input test file in a temp directory
        File inFile = testutil.Util.resourceAsTempFile(Command_StaticTest.class, TEST_BS_FILE);

        ArgParser ap = new ArgParser(new String[] {"-dim", "16x16", "-fmt", "mdec"});
        FeedbackStream fbs = new FeedbackStream();
        Command_Static testSubject = new Command_Static();
        InFileAndIndexArgs inFileIdx = new InFileAndIndexArgs(inFile.getPath(), null, fbs);
        assertNull(testSubject.validate("bs"));

        // run command
        testSubject.init(new ArgParser(new String[] {"-static"}), inFileIdx, fbs);
        testSubject.execute(ap);
        // I guess check the output file
        File expectedFile = new File(Command_StaticTest.class.getSimpleName()+"_16x16.mdec");
        assertTrue(expectedFile.exists());
        expectedFile.delete();
    }

    @Test
    public void testBs2Png() throws Exception {
        // put an input test file in a temp directory
        File inFile = testutil.Util.resourceAsTempFile(Command_StaticTest.class, TEST_BS_FILE);

        ArgParser ap = new ArgParser(new String[] {"-dim", "16x16", "-fmt", "png", "-up", "nearestneighbor"});
        FeedbackStream fbs = new FeedbackStream();
        Command_Static testSubject = new Command_Static();
        InFileAndIndexArgs inFileIdx = new InFileAndIndexArgs(inFile.getPath(), null, fbs);
        assertNull(testSubject.validate("bs"));

        // run command
        testSubject.init(new ArgParser(new String[] {"-static"}), inFileIdx, fbs);
        testSubject.execute(ap);
        // I guess check the output file
        File expectedFile = new File(Command_StaticTest.class.getSimpleName()+".png");
        assertTrue(expectedFile.exists());
        expectedFile.delete();
    }


    @Test
    public void testMdec2Jpg() throws Exception {
        // put an input test file in a temp directory
        File inFile = testutil.Util.resourceAsTempFile(Command_StaticTest.class, TEST_MDEC_FILE);

        ArgParser ap = new ArgParser(new String[] {"-dim", "16x16", "-fmt", "jpg", "-up", "nearestneighbor"});
        FeedbackStream fbs = new FeedbackStream();
        Command_Static testSubject = new Command_Static();
        InFileAndIndexArgs inFileIdx = new InFileAndIndexArgs(inFile.getPath(), null, fbs);
        assertNull(testSubject.validate("mdec"));

        // run command
        testSubject.init(new ArgParser(new String[] {"-static"}), inFileIdx, fbs);
        testSubject.execute(ap);
        // I guess check the output file
        File expectedFile = new File(Command_StaticTest.class.getSimpleName()+".jpg");
        assertTrue(expectedFile.exists());
        expectedFile.delete();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("======================================================");
        System.out.println("================ Generating test data ================");
        System.out.println("======================================================");
        // A blank image with a single macro block (16x16)
        final List<MdecCode> codes = Arrays.asList(
                new MdecCode(1, 0), new MdecCode().setToEndOfData(),
                new MdecCode(1, 0), new MdecCode().setToEndOfData(),
                new MdecCode(1, 0), new MdecCode().setToEndOfData(),
                new MdecCode(1, 0), new MdecCode().setToEndOfData(),
                new MdecCode(1, 0), new MdecCode().setToEndOfData(),
                new MdecCode(1, 0), new MdecCode().setToEndOfData());

        MdecInputStream mis = new ArrayListMdecInputStream(codes);
        FileOutputStream fos = new FileOutputStream(TEST_MDEC_FILE);
        MdecInputStreamReader.writeMdecBlocks(mis, fos, 6);
        fos.close();

        mis = new ArrayListMdecInputStream(codes);
        BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2 x =
                new BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2(1, -1);
        byte[] abData = x.compress(mis);
        IO.writeFile(TEST_BS_FILE, abData);
    }

    private static class ArrayListMdecInputStream implements MdecInputStream {
        private final Iterator<MdecCode> it;
        public ArrayListMdecInputStream(List<MdecCode> codes) {
            it = codes.iterator();
        }
        public boolean readMdecCode(MdecCode code) {
            code.setFrom(it.next());
            return code.isEOD();
        }
    }
}
