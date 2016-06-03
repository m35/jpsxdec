/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2016  Michael Sabin
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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.psxvideo.mdec.idct.IDCT_double;
import jpsxdec.psxvideo.mdec.idct.StephensIDCT;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import static org.junit.Assert.*;
import org.junit.Test;


public class STRv3 {

    public STRv3() {
    }

    @Test
    public void encodeDc511() throws Exception {
        int iQscale = 1;
        MdecCode code = new MdecCode();
        ArrayList<MdecCode> codes = new ArrayList<MdecCode>();

        // cr
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // cb
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y1
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y2
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y3
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y4
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        MdecInputStreamIterator in = new MdecInputStreamIterator(codes.iterator());
        BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3 comp =
                new BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3();
        byte[] abBitstream = comp.compress(in, 16, 16);
        BitStreamUncompressor_STRv3 out = new BitStreamUncompressor_STRv3();
        out.reset(abBitstream, abBitstream.length);
        // TODO: uncompress this
        try {
            out.skipMacroBlocks(16, 16);
            fail("Expected exception");
        } catch (MdecException.Uncompress ex) {

        }
    }

    @Test
    public void encodeDcMaxDiff() throws Exception {
        int iWidth = 16;
        int iHeight = 32;
        int iQscale = 1;
        MdecCode code = new MdecCode();
        ArrayList<MdecCode> codes = new ArrayList<MdecCode>();

        // cr
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // cb
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y1
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y2
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y3
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y4
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // cr
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // cb
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y1
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y2
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y3
        code.setBits(iQscale, -512);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        // y4
        code.setBits(iQscale, 511);
        codes.add(code.copy());
        code.setToEndOfData();
        codes.add(code.copy());

        MdecInputStreamIterator in = new MdecInputStreamIterator(codes.iterator());
        /*
        TODO: Encode this image and see the result
        TODO: Debug STRv3 video, break at MDEC in to see how it was decoded
        */

        BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3 comp =
                new BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3();
        try {
            byte[] abBitstream = comp.compress(in, iWidth, iHeight);
            fail("Should not be able to compress this");
        } catch (MdecException.Compress ex) {
            // expected
        }
        
    }

    public static class MdecInputStreamIterator extends MdecInputStream {

        private final Iterator<MdecCode> _codes;

        public MdecInputStreamIterator(Iterator<MdecCode> codes) {
            _codes = codes;
        }

        @Override
        public boolean readMdecCode(MdecCode code) {
            code.set(_codes.next());
            return code.isEOD();
        }
    }

    @Test
    public void encodeTestMax() throws Exception {
        BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3 comp =
                new BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3();

        IDCT_double i = new StephensIDCT();
        double[] blk = new double[64];
        Arrays.fill(blk, -128.0);
        double[] out = new double[64];
        i.IDCT(blk, 0, out);

        BufferedImage bi = ImageIO.read(STRv3.class.getResource("testmax.png"));
        PsxYCbCrImage yuv = new PsxYCbCrImage(bi);
        MdecEncoder enc = new MdecEncoder(yuv, bi.getWidth(), bi.getHeight());
        int[] aiQscales = {1,1,1,1,1,1};
        for (MacroBlockEncoder mbenc : enc) {
            mbenc.setToFullEncode(aiQscales);
        }
        byte[] abBs = comp.compress(enc.getStream(), bi.getWidth(), bi.getHeight());
        byte[] abExpected = IO.readEntireStream(getClass().getResourceAsStream("testmax-EXPECTED.bs"));
        assertArrayEquals(abExpected, abBs);
    }


    @Test
    public void v3ChromaDc() throws IOException, NotThisTypeException {
        testStreamFile("v3_DC_CHROMA.txt.gz");
    }

    @Test
    public void v3LumaDc() throws IOException, NotThisTypeException {
        testStreamFile("v3_DC_LUMA.txt.gz");
    }

    public static void testStreamFile(String sFile) throws IOException, NotThisTypeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamUncompressor_STRv3 v3 = new BitStreamUncompressor_STRv3();
        MdecCode code = new MdecCode();

        InputStream is = STRv3.class.getResourceAsStream(sFile);
        is = new GZIPInputStream(is);
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
        String sLine;
        while ((sLine = lnr.readLine()) != null) {
            //System.out.println(sLine);
            String[] as = sLine.split("\t", -1);
            assertEquals(sLine, 2, as.length);
            String sBits = as[0].replace("+", "");
            String[] asCodes = as[1].split("\\+");
            BitStreamWriter bw = STRv2.writeHeader(baos, 3);
            bw.write(sBits);
            bw.close();
            v3.reset(baos.toByteArray(), baos.size());
            for (String sCode : asCodes) {
                try {
                    v3.readMdecCode(code);
                    if (!sCode.equals(code.toString()))
                        System.out.println("Line "+lnr.getLineNumber()+": "+sLine);
                } catch (MdecException.Read ex) {
                    if (!sCode.startsWith("!")) {
                        AssertionError e = new AssertionError("Line "+lnr.getLineNumber()+": "+sLine);
                        e.initCause(ex);
                        throw e;
                    }
                }
            }
        }
        lnr.close();
    }



    private static abstract class Str3DcGenerator {
        private final BitStreamUncompressor_STRv3 _v3 = new BitStreamUncompressor_STRv3();
        private final MdecCode _code = new MdecCode();
        private final StringBuilder _mdecCodesRead = new StringBuilder();
        private final ByteArrayOutputStream _baos = new ByteArrayOutputStream();
        private BitStreamWriter _bw;
        private final ArrayList<String> _mdecCodeBitsWritten = new ArrayList<String>();


        abstract protected int getDcCodeBitLength();

        /** Write a previous block to setup the state for the next block of interest. */
        abstract protected void writeMinMaxBlock(boolean blnWriteMax) throws IOException;

        /** If block is expected to fail due to unrecognized bit sequence. */
        abstract protected boolean shouldFailUnknownCode(int iBits);

        /** Write bits to git bitstream into the state to test. */
        abstract protected void writeSetupBits() throws IOException;

        private MdecCode nextCode() throws MdecException.Read {
            _v3.readMdecCode(_code);
            if (_mdecCodesRead.length() > 0)
                _mdecCodesRead.append('+');
            _mdecCodesRead.append(_code);
            if (_code.getBottom10Bits() % 4 != 0)
                throw new IllegalStateException(_code.toString());
            return _code;
        }

        private void initFrame() throws IOException {
            _bw = STRv2.writeHeader(_baos, 3);
            _mdecCodeBitsWritten.clear();
            _mdecCodesRead.setLength(0);
        }

        private void closeAndReset() throws IOException, NotThisTypeException {
            _bw.close();
            _v3.reset(_baos.toByteArray(), _baos.size());
            _baos.reset();
            _mdecCodesRead.setLength(0);
        }

        protected void writeBits(String ... asBits) throws IOException {
            for (String s : asBits) {
                _mdecCodeBitsWritten.add(s);
                _bw.write(s);
            }
       }

        public void generate(String sFile) throws Exception {
            PrintStream ps = new PrintStream(
                    new BufferedOutputStream(
                    new FileOutputStream(sFile)));

            for (int iBits = 0; iBits < 1 << getDcCodeBitLength(); iBits++) {
                ps.print(genFirstBlock(iBits));
                ps.print('\n');
                ps.print(genSecondBlock(iBits, true));
                ps.print('\n');
                ps.print(genSecondBlock(iBits, false));
                ps.print('\n');
            }
            ps.close();
        }

        private String genFirstBlock(int iBits) throws Exception {
            String sBits = Misc.bitsToString(iBits, getDcCodeBitLength());
            // write the bits in the first block
            initFrame();
            writeSetupBits();
            writeBits(sBits);
            closeAndReset();

            // now read the resulting codes
            return readCodes(iBits, sBits);
        }

        private String genSecondBlock(int iBits, boolean blnFirstIsPositive) throws Exception {
            String sBits = Misc.bitsToString(iBits, getDcCodeBitLength());
            // setup a previous block to put the relative value back in bounds
            initFrame();
            writeSetupBits();
            writeMinMaxBlock(blnFirstIsPositive);
            writeBits(sBits);
            closeAndReset();

            // now read the resulting codes
            return readCodes(iBits, sBits);
        }

        private String readCodes(int iBits, String sBits) throws Exception {
            for (int i = 0; i < _mdecCodeBitsWritten.size() - 1; i++) {
                nextCode();
            }

            try {
                nextCode();
                assertFalse("These bits should fail "+sBits + " "+Integer.toHexString(iBits),
                        _mdecCodeBitsWritten.size() == 1 && shouldFailUnknownCode(iBits));
                return getLine(null);
            } catch (MdecException.Uncompress ex) {
                if (shouldFailUnknownCode(iBits))
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Unknown"));
                else
                    assertTrue(ex.getMessage(), ex.getMessage().contains("bounds"));
                return getLine(ex);
            }
        }

        private String getLine(MdecException ex) {
            String sLine = Misc.join(_mdecCodeBitsWritten, "+");
            if (ex != null) {
                if (_mdecCodesRead.length() == 0)
                    return sLine + "\t!"+ex.getMessage();
                else
                    return sLine +'\t'+_mdecCodesRead+"+!"+ex.getMessage();
            } else {
                return sLine +'\t'+_mdecCodesRead;
            }
        }

    }


    private static class DcGeneratorChroma extends Str3DcGenerator {
        protected int getDcCodeBitLength() {
            return 16;
        }

        protected void writeMinMaxBlock(boolean blnWriteMax) throws IOException {
            if (blnWriteMax) { // cr
                writeBits("1111110"+"1111111");
            } else {
                writeBits("11111110"+"01111111");
            }
            writeBits("10");  // eob

            writeBits("00" , "10"); // cb
            writeBits("100", "10"); // y1
            writeBits("100", "10"); // y2
            writeBits("100", "10"); // y3
            writeBits("100", "10"); // y4
        }

        protected boolean shouldFailUnknownCode(int iBits) {
            return (iBits & 0xff00) == 0xff00;
        }

        protected void writeSetupBits() throws IOException {
            // already at chroma blocks, no need to setup
        }

    }


    private static class DcGeneratorLuma extends Str3DcGenerator {
        protected int getDcCodeBitLength() {
            return 15;
        }

        protected void writeMinMaxBlock(boolean blnWritePositive) throws IOException {
            if (blnWritePositive) { // y1
                writeBits("111110"+"1111111", "10");
            } else {
                writeBits("1111110"+"01111111", "10");
            }
        }

        protected boolean shouldFailUnknownCode(int iBits) {
            return (iBits & 0x7f00) == 0x7f00;
        }

        protected void writeSetupBits() throws IOException {
            // write empty chroma blocks to get to luma
            // STRv3 DC Chroma '00' VLC bits mean DC = 0
            // '10' means EOB
            writeBits("00", "10",  // cr
                      "00", "10"); // cb
        }
    }

    public static void main(String[] args) throws Exception {
        new DcGeneratorChroma().generate("v3_DC_CHROMA.txt");
        new DcGeneratorLuma().generate("v3_DC_LUMA.txt");
    }



}
