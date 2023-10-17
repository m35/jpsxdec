/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import java.io.*;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.IO;
import org.junit.*;
import static org.junit.Assert.*;

/** Test bitstream decoding. */
public class STRv2 {

    /** Represents an AC variable length code. */
    private static class AcVariableLengthCode {
        public final String VariableLengthCode;
        public final int RunOfZeros;
        public final int AbsoluteLevel;

        public AcVariableLengthCode(String vlc, int run, int level) {
            VariableLengthCode = vlc;
            RunOfZeros = run;
            AbsoluteLevel = level;
        }
    }

    /** Sequence of bits indicating an escape code. */
    public final static String AC_ESCAPE_CODE = "000001";

    /** Sequence of bits indicating the end of a block.
     * Unlike the MPEG1 specification, these bits can, and often do appear as
     * the first and only variable-length-code in a block. */
    public final static String AC_END_OF_BLOCK = "10"; // bits 10


    /** STR v2 variable-length codes.  */
    private final static AcVariableLengthCode[] AC_VARIABLE_LENGTH_CODES_MPEG1 =
    {
                             //  Code               "Run" "Level"
/*  0 */new AcVariableLengthCode("11"                , 0 , 1  ),

/*  1 */new AcVariableLengthCode("011"               , 1 , 1  ),

/*  2 */new AcVariableLengthCode("0100"              , 0 , 2  ),
/*  3 */new AcVariableLengthCode("0101"              , 2 , 1  ),

/*  4 */new AcVariableLengthCode("00101"             , 0 , 3  ),
/*  5 */new AcVariableLengthCode("00110"             , 4 , 1  ),
/*  6 */new AcVariableLengthCode("00111"             , 3 , 1  ),

/*  7 */new AcVariableLengthCode("000100"            , 7 , 1  ),
/*  8 */new AcVariableLengthCode("000101"            , 6 , 1  ),
/*  9 */new AcVariableLengthCode("000110"            , 1 , 2  ),
/* 10 */new AcVariableLengthCode("000111"            , 5 , 1  ),

/* 11 */new AcVariableLengthCode("0000100"           , 2 , 2  ),
/* 12 */new AcVariableLengthCode("0000101"           , 9 , 1  ),
/* 13 */new AcVariableLengthCode("0000110"           , 0 , 4  ),
/* 14 */new AcVariableLengthCode("0000111"           , 8 , 1  ),

/* 15 */new AcVariableLengthCode("00100000"          , 13, 1  ),
/* 16 */new AcVariableLengthCode("00100001"          , 0 , 6  ),
/* 17 */new AcVariableLengthCode("00100010"          , 12, 1  ),
/* 18 */new AcVariableLengthCode("00100011"          , 11, 1  ),
/* 19 */new AcVariableLengthCode("00100100"          , 3 , 2  ),
/* 20 */new AcVariableLengthCode("00100101"          , 1 , 3  ),
/* 21 */new AcVariableLengthCode("00100110"          , 0 , 5  ),
/* 22 */new AcVariableLengthCode("00100111"          , 10, 1  ),

/* 23 */new AcVariableLengthCode("0000001000"        , 16, 1  ),
/* 24 */new AcVariableLengthCode("0000001001"        , 5 , 2  ),
/* 25 */new AcVariableLengthCode("0000001010"        , 0 , 7  ),
/* 26 */new AcVariableLengthCode("0000001011"        , 2 , 3  ),
/* 27 */new AcVariableLengthCode("0000001100"        , 1 , 4  ),
/* 28 */new AcVariableLengthCode("0000001101"        , 15, 1  ),
/* 29 */new AcVariableLengthCode("0000001110"        , 14, 1  ),
/* 30 */new AcVariableLengthCode("0000001111"        , 4 , 2  ),

/* 31 */new AcVariableLengthCode("000000010000"      , 0 , 11 ),
/* 32 */new AcVariableLengthCode("000000010001"      , 8 , 2  ),
/* 33 */new AcVariableLengthCode("000000010010"      , 4 , 3  ),
/* 34 */new AcVariableLengthCode("000000010011"      , 0 , 10 ),
/* 35 */new AcVariableLengthCode("000000010100"      , 2 , 4  ),
/* 36 */new AcVariableLengthCode("000000010101"      , 7 , 2  ),
/* 37 */new AcVariableLengthCode("000000010110"      , 21, 1  ),
/* 38 */new AcVariableLengthCode("000000010111"      , 20, 1  ),
/* 39 */new AcVariableLengthCode("000000011000"      , 0 , 9  ),
/* 40 */new AcVariableLengthCode("000000011001"      , 19, 1  ),
/* 41 */new AcVariableLengthCode("000000011010"      , 18, 1  ),
/* 42 */new AcVariableLengthCode("000000011011"      , 1 , 5  ),
/* 43 */new AcVariableLengthCode("000000011100"      , 3 , 3  ),
/* 44 */new AcVariableLengthCode("000000011101"      , 0 , 8  ),
/* 45 */new AcVariableLengthCode("000000011110"      , 6 , 2  ),
/* 46 */new AcVariableLengthCode("000000011111"      , 17, 1  ),

/* 47 */new AcVariableLengthCode("0000000010000"     , 10, 2  ),
/* 48 */new AcVariableLengthCode("0000000010001"     , 9 , 2  ),
/* 49 */new AcVariableLengthCode("0000000010010"     , 5 , 3  ),
/* 50 */new AcVariableLengthCode("0000000010011"     , 3 , 4  ),
/* 51 */new AcVariableLengthCode("0000000010100"     , 2 , 5  ),
/* 52 */new AcVariableLengthCode("0000000010101"     , 1 , 7  ),
/* 53 */new AcVariableLengthCode("0000000010110"     , 1 , 6  ),
/* 54 */new AcVariableLengthCode("0000000010111"     , 0 , 15 ),
/* 55 */new AcVariableLengthCode("0000000011000"     , 0 , 14 ),
/* 56 */new AcVariableLengthCode("0000000011001"     , 0 , 13 ),
/* 57 */new AcVariableLengthCode("0000000011010"     , 0 , 12 ),
/* 58 */new AcVariableLengthCode("0000000011011"     , 26, 1  ),
/* 59 */new AcVariableLengthCode("0000000011100"     , 25, 1  ),
/* 60 */new AcVariableLengthCode("0000000011101"     , 24, 1  ),
/* 61 */new AcVariableLengthCode("0000000011110"     , 23, 1  ),
/* 62 */new AcVariableLengthCode("0000000011111"     , 22, 1  ),

/* 63 */new AcVariableLengthCode("00000000010000"    , 0 , 31 ),
/* 64 */new AcVariableLengthCode("00000000010001"    , 0 , 30 ),
/* 65 */new AcVariableLengthCode("00000000010010"    , 0 , 29 ),
/* 66 */new AcVariableLengthCode("00000000010011"    , 0 , 28 ),
/* 67 */new AcVariableLengthCode("00000000010100"    , 0 , 27 ),
/* 68 */new AcVariableLengthCode("00000000010101"    , 0 , 26 ),
/* 69 */new AcVariableLengthCode("00000000010110"    , 0 , 25 ),
/* 70 */new AcVariableLengthCode("00000000010111"    , 0 , 24 ),
/* 71 */new AcVariableLengthCode("00000000011000"    , 0 , 23 ),
/* 72 */new AcVariableLengthCode("00000000011001"    , 0 , 22 ),
/* 73 */new AcVariableLengthCode("00000000011010"    , 0 , 21 ),
/* 74 */new AcVariableLengthCode("00000000011011"    , 0 , 20 ),
/* 75 */new AcVariableLengthCode("00000000011100"    , 0 , 19 ),
/* 76 */new AcVariableLengthCode("00000000011101"    , 0 , 18 ),
/* 77 */new AcVariableLengthCode("00000000011110"    , 0 , 17 ),
/* 78 */new AcVariableLengthCode("00000000011111"    , 0 , 16 ),

/* 79 */new AcVariableLengthCode("000000000010000"   , 0 , 40 ),
/* 80 */new AcVariableLengthCode("000000000010001"   , 0 , 39 ),
/* 81 */new AcVariableLengthCode("000000000010010"   , 0 , 38 ),
/* 82 */new AcVariableLengthCode("000000000010011"   , 0 , 37 ),
/* 83 */new AcVariableLengthCode("000000000010100"   , 0 , 36 ),
/* 84 */new AcVariableLengthCode("000000000010101"   , 0 , 35 ),
/* 85 */new AcVariableLengthCode("000000000010110"   , 0 , 34 ),
/* 86 */new AcVariableLengthCode("000000000010111"   , 0 , 33 ),
/* 87 */new AcVariableLengthCode("000000000011000"   , 0 , 32 ),
/* 88 */new AcVariableLengthCode("000000000011001"   , 1 , 14 ),
/* 89 */new AcVariableLengthCode("000000000011010"   , 1 , 13 ),
/* 90 */new AcVariableLengthCode("000000000011011"   , 1 , 12 ),
/* 91 */new AcVariableLengthCode("000000000011100"   , 1 , 11 ),
/* 92 */new AcVariableLengthCode("000000000011101"   , 1 , 10 ),
/* 93 */new AcVariableLengthCode("000000000011110"   , 1 , 9  ),
/* 94 */new AcVariableLengthCode("000000000011111"   , 1 , 8  ),

/* 95 */new AcVariableLengthCode("0000000000010000"  , 1 , 18 ),
/* 96 */new AcVariableLengthCode("0000000000010001"  , 1 , 17 ),
/* 97 */new AcVariableLengthCode("0000000000010010"  , 1 , 16 ),
/* 98 */new AcVariableLengthCode("0000000000010011"  , 1 , 15 ),
/* 99 */new AcVariableLengthCode("0000000000010100"  , 6 , 3  ),
/*100 */new AcVariableLengthCode("0000000000010101"  , 16, 2  ),
/*101 */new AcVariableLengthCode("0000000000010110"  , 15, 2  ),
/*102 */new AcVariableLengthCode("0000000000010111"  , 14, 2  ),
/*103 */new AcVariableLengthCode("0000000000011000"  , 13, 2  ),
/*104 */new AcVariableLengthCode("0000000000011001"  , 12, 2  ),
/*105 */new AcVariableLengthCode("0000000000011010"  , 11, 2  ),
/*106 */new AcVariableLengthCode("0000000000011011"  , 31, 1  ),
/*107 */new AcVariableLengthCode("0000000000011100"  , 30, 1  ),
/*108 */new AcVariableLengthCode("0000000000011101"  , 29, 1  ),
/*109 */new AcVariableLengthCode("0000000000011110"  , 28, 1  ),
/*110 */new AcVariableLengthCode("0000000000011111"  , 27, 1  )
    };

    static byte[] writeHeader(int iVersion, byte[] abBits)
            throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IO.writeInt16LE(os, 1); // mdec count (ignored)
        IO.writeInt16LE(os, 0x3800);
        IO.writeInt16LE(os, 1); // qscale
        IO.writeInt16LE(os, iVersion); // version
        os.write(abBits);
        return os.toByteArray();
    }

    @Test
    public void v2Dc() throws Exception {
        MdecCode code = new MdecCode();
        for (int i = -512; i < 512; i++) {
            BitStreamWriter bw = new BitStreamWriter();
            bw.write(i, 10);
            bw.write("0111111111"); // padding bits
            bw.write("0111111111"); // padding bits again
            bw.write("1111111111"); // not padding bits again

            byte[] ab = writeHeader(2, bw.toByteArray(BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER));
            BitStreamUncompressor_STRv2 v2 = BitStreamUncompressor_STRv2.makeV2(ab);
            v2.readMdecCode(code); // DC
            assertEquals(new MdecCode(1, i), code);
            assertTrue(v2.skipPaddingBits());
            assertTrue(v2.skipPaddingBits());
            assertFalse(v2.skipPaddingBits());
        }

    }

    @Test
    public void v2Ac() throws Exception {
        MdecCode code = new MdecCode();
        for (AcVariableLengthCode vlc : AC_VARIABLE_LENGTH_CODES_MPEG1) {
            for (int i = 0; i < 2; i++) {
                BitStreamWriter bw = new BitStreamWriter();
                bw.write(1, 10);
                bw.write(vlc.VariableLengthCode);
                bw.write(i != 0);
                bw.write("0111111111"); // padding bits
                bw.write("1111111111"); // not padding bits
                bw.write("0111111111"); // padding bits again

                byte[] ab = writeHeader(2, bw.toByteArray(BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER));
                BitStreamUncompressor_STRv2 v2 = BitStreamUncompressor_STRv2.makeV2(ab);
                v2.readMdecCode(code); // DC
                assertEquals(new MdecCode(1, 1), code);
                v2.readMdecCode(code); // AC
                assertEquals(new MdecCode(vlc.RunOfZeros, i == 0 ? vlc.AbsoluteLevel : -vlc.AbsoluteLevel), code);
                assertTrue(v2.skipPaddingBits());
                assertFalse(v2.skipPaddingBits());
                assertTrue(v2.skipPaddingBits());
            }
        }

    }

    @Test
    public void v2AcEscape() throws Exception {
        MdecCode code = new MdecCode();
        for (int iRun = 0; iRun < 63; iRun++) {
            for (int iAc = -512; iAc < 512; iAc++) {
                BitStreamWriter bw = new BitStreamWriter();
                bw.write(1, 10);
                bw.write(AC_ESCAPE_CODE);
                bw.write(iRun, 6);
                bw.write(iAc, 10);
                bw.write("1111111111"); // not padding bits
                bw.write("0111111111"); // padding bits
                bw.write("0111111111"); // padding bits again

                byte[] ab = writeHeader(2, bw.toByteArray(BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER));
                BitStreamUncompressor_STRv2 v2 = BitStreamUncompressor_STRv2.makeV2(ab);
                v2.readMdecCode(code); // DC
                assertEquals(new MdecCode(1, 1), code);
                v2.readMdecCode(code); // AC
                assertEquals(new MdecCode(iRun, iAc), code);
                assertFalse(v2.skipPaddingBits());
                assertTrue(v2.skipPaddingBits());
                assertTrue(v2.skipPaddingBits());
            }
        }
    }

    /** Added because initially the v2 logic didn't read these bits properly. */
    @Test
    public void badCode14bits() throws Exception {
        BitStreamWriter bw = new BitStreamWriter();
        bw.write("0000000001"); // DC
        bw.write("00000000000001110"); // Bad 14-bit AC
        byte[] ab = writeHeader(2, bw.toByteArray(BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER));
        BitStreamUncompressor_STRv2 v2 = BitStreamUncompressor_STRv2.makeV2(ab);

        MdecCode code = new MdecCode();
        v2.readMdecCode(code); // DC
        assertEquals(new MdecCode(1, 1), code);
        try {
            v2.readMdecCode(code); // AC
            fail("Expected exception");
        } catch (MdecException.ReadCorruption ex) {
            // expected fail
        }
    }

}
