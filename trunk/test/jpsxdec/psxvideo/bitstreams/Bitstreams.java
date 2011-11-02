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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Test bitstream decoding. */
public class Bitstreams {

    /** Represents an AC variable length code. */
    public static class ACVariableLengthCode {
        public final String VariableLengthCode;
        public final int RunOfZeros;
        public final int AbsoluteLevel;

        public ACVariableLengthCode(String vlc, int run, int level)
        {
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
    public final static ACVariableLengthCode[] AC_VARIABLE_LENGTH_CODES_MPEG1 =
    {
                             //  Code               "Run" "Level"
/*  0 */new ACVariableLengthCode("11"                , 0 , 1  ),

/*  1 */new ACVariableLengthCode("011"               , 1 , 1  ),

/*  2 */new ACVariableLengthCode("0100"              , 0 , 2  ),
/*  3 */new ACVariableLengthCode("0101"              , 2 , 1  ),

/*  4 */new ACVariableLengthCode("00101"             , 0 , 3  ),
/*  5 */new ACVariableLengthCode("00110"             , 4 , 1  ),
/*  6 */new ACVariableLengthCode("00111"             , 3 , 1  ),

/*  7 */new ACVariableLengthCode("000100"            , 7 , 1  ),
/*  8 */new ACVariableLengthCode("000101"            , 6 , 1  ),
/*  9 */new ACVariableLengthCode("000110"            , 1 , 2  ),
/* 10 */new ACVariableLengthCode("000111"            , 5 , 1  ),

/* 11 */new ACVariableLengthCode("0000100"           , 2 , 2  ),
/* 12 */new ACVariableLengthCode("0000101"           , 9 , 1  ),
/* 13 */new ACVariableLengthCode("0000110"           , 0 , 4  ),
/* 14 */new ACVariableLengthCode("0000111"           , 8 , 1  ),

/* 15 */new ACVariableLengthCode("00100000"          , 13, 1  ),
/* 16 */new ACVariableLengthCode("00100001"          , 0 , 6  ),
/* 17 */new ACVariableLengthCode("00100010"          , 12, 1  ),
/* 18 */new ACVariableLengthCode("00100011"          , 11, 1  ),
/* 19 */new ACVariableLengthCode("00100100"          , 3 , 2  ),
/* 20 */new ACVariableLengthCode("00100101"          , 1 , 3  ),
/* 21 */new ACVariableLengthCode("00100110"          , 0 , 5  ),
/* 22 */new ACVariableLengthCode("00100111"          , 10, 1  ),

/* 23 */new ACVariableLengthCode("0000001000"        , 16, 1  ),
/* 24 */new ACVariableLengthCode("0000001001"        , 5 , 2  ),
/* 25 */new ACVariableLengthCode("0000001010"        , 0 , 7  ),
/* 26 */new ACVariableLengthCode("0000001011"        , 2 , 3  ),
/* 27 */new ACVariableLengthCode("0000001100"        , 1 , 4  ),
/* 28 */new ACVariableLengthCode("0000001101"        , 15, 1  ),
/* 29 */new ACVariableLengthCode("0000001110"        , 14, 1  ),
/* 30 */new ACVariableLengthCode("0000001111"        , 4 , 2  ),

/* 31 */new ACVariableLengthCode("000000010000"      , 0 , 11 ),
/* 32 */new ACVariableLengthCode("000000010001"      , 8 , 2  ),
/* 33 */new ACVariableLengthCode("000000010010"      , 4 , 3  ),
/* 34 */new ACVariableLengthCode("000000010011"      , 0 , 10 ),
/* 35 */new ACVariableLengthCode("000000010100"      , 2 , 4  ),
/* 36 */new ACVariableLengthCode("000000010101"      , 7 , 2  ),
/* 37 */new ACVariableLengthCode("000000010110"      , 21, 1  ),
/* 38 */new ACVariableLengthCode("000000010111"      , 20, 1  ),
/* 39 */new ACVariableLengthCode("000000011000"      , 0 , 9  ),
/* 40 */new ACVariableLengthCode("000000011001"      , 19, 1  ),
/* 41 */new ACVariableLengthCode("000000011010"      , 18, 1  ),
/* 42 */new ACVariableLengthCode("000000011011"      , 1 , 5  ),
/* 43 */new ACVariableLengthCode("000000011100"      , 3 , 3  ),
/* 44 */new ACVariableLengthCode("000000011101"      , 0 , 8  ),
/* 45 */new ACVariableLengthCode("000000011110"      , 6 , 2  ),
/* 46 */new ACVariableLengthCode("000000011111"      , 17, 1  ),

/* 47 */new ACVariableLengthCode("0000000010000"     , 10, 2  ),
/* 48 */new ACVariableLengthCode("0000000010001"     , 9 , 2  ),
/* 49 */new ACVariableLengthCode("0000000010010"     , 5 , 3  ),
/* 50 */new ACVariableLengthCode("0000000010011"     , 3 , 4  ),
/* 51 */new ACVariableLengthCode("0000000010100"     , 2 , 5  ),
/* 52 */new ACVariableLengthCode("0000000010101"     , 1 , 7  ),
/* 53 */new ACVariableLengthCode("0000000010110"     , 1 , 6  ),
/* 54 */new ACVariableLengthCode("0000000010111"     , 0 , 15 ),
/* 55 */new ACVariableLengthCode("0000000011000"     , 0 , 14 ),
/* 56 */new ACVariableLengthCode("0000000011001"     , 0 , 13 ),
/* 57 */new ACVariableLengthCode("0000000011010"     , 0 , 12 ),
/* 58 */new ACVariableLengthCode("0000000011011"     , 26, 1  ),
/* 59 */new ACVariableLengthCode("0000000011100"     , 25, 1  ),
/* 60 */new ACVariableLengthCode("0000000011101"     , 24, 1  ),
/* 61 */new ACVariableLengthCode("0000000011110"     , 23, 1  ),
/* 62 */new ACVariableLengthCode("0000000011111"     , 22, 1  ),

/* 63 */new ACVariableLengthCode("00000000010000"    , 0 , 31 ),
/* 64 */new ACVariableLengthCode("00000000010001"    , 0 , 30 ),
/* 65 */new ACVariableLengthCode("00000000010010"    , 0 , 29 ),
/* 66 */new ACVariableLengthCode("00000000010011"    , 0 , 28 ),
/* 67 */new ACVariableLengthCode("00000000010100"    , 0 , 27 ),
/* 68 */new ACVariableLengthCode("00000000010101"    , 0 , 26 ),
/* 69 */new ACVariableLengthCode("00000000010110"    , 0 , 25 ),
/* 70 */new ACVariableLengthCode("00000000010111"    , 0 , 24 ),
/* 71 */new ACVariableLengthCode("00000000011000"    , 0 , 23 ),
/* 72 */new ACVariableLengthCode("00000000011001"    , 0 , 22 ),
/* 73 */new ACVariableLengthCode("00000000011010"    , 0 , 21 ),
/* 74 */new ACVariableLengthCode("00000000011011"    , 0 , 20 ),
/* 75 */new ACVariableLengthCode("00000000011100"    , 0 , 19 ),
/* 76 */new ACVariableLengthCode("00000000011101"    , 0 , 18 ),
/* 77 */new ACVariableLengthCode("00000000011110"    , 0 , 17 ),
/* 78 */new ACVariableLengthCode("00000000011111"    , 0 , 16 ),

/* 79 */new ACVariableLengthCode("000000000010000"   , 0 , 40 ),
/* 80 */new ACVariableLengthCode("000000000010001"   , 0 , 39 ),
/* 81 */new ACVariableLengthCode("000000000010010"   , 0 , 38 ),
/* 82 */new ACVariableLengthCode("000000000010011"   , 0 , 37 ),
/* 83 */new ACVariableLengthCode("000000000010100"   , 0 , 36 ),
/* 84 */new ACVariableLengthCode("000000000010101"   , 0 , 35 ),
/* 85 */new ACVariableLengthCode("000000000010110"   , 0 , 34 ),
/* 86 */new ACVariableLengthCode("000000000010111"   , 0 , 33 ),
/* 87 */new ACVariableLengthCode("000000000011000"   , 0 , 32 ),
/* 88 */new ACVariableLengthCode("000000000011001"   , 1 , 14 ),
/* 89 */new ACVariableLengthCode("000000000011010"   , 1 , 13 ),
/* 90 */new ACVariableLengthCode("000000000011011"   , 1 , 12 ),
/* 91 */new ACVariableLengthCode("000000000011100"   , 1 , 11 ),
/* 92 */new ACVariableLengthCode("000000000011101"   , 1 , 10 ),
/* 93 */new ACVariableLengthCode("000000000011110"   , 1 , 9  ),
/* 94 */new ACVariableLengthCode("000000000011111"   , 1 , 8  ),

/* 95 */new ACVariableLengthCode("0000000000010000"  , 1 , 18 ),
/* 96 */new ACVariableLengthCode("0000000000010001"  , 1 , 17 ),
/* 97 */new ACVariableLengthCode("0000000000010010"  , 1 , 16 ),
/* 98 */new ACVariableLengthCode("0000000000010011"  , 1 , 15 ),
/* 99 */new ACVariableLengthCode("0000000000010100"  , 6 , 3  ),
/*100 */new ACVariableLengthCode("0000000000010101"  , 16, 2  ),
/*101 */new ACVariableLengthCode("0000000000010110"  , 15, 2  ),
/*102 */new ACVariableLengthCode("0000000000010111"  , 14, 2  ),
/*103 */new ACVariableLengthCode("0000000000011000"  , 13, 2  ),
/*104 */new ACVariableLengthCode("0000000000011001"  , 12, 2  ),
/*105 */new ACVariableLengthCode("0000000000011010"  , 11, 2  ),
/*106 */new ACVariableLengthCode("0000000000011011"  , 31, 1  ),
/*107 */new ACVariableLengthCode("0000000000011100"  , 30, 1  ),
/*108 */new ACVariableLengthCode("0000000000011101"  , 29, 1  ),
/*109 */new ACVariableLengthCode("0000000000011110"  , 28, 1  ),
/*110 */new ACVariableLengthCode("0000000000011111"  , 27, 1  )
    };

    public Bitstreams() {
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
    public void v2DC() throws IOException, NotThisTypeException, MdecException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamUncompressor_STRv2 v2 = new BitStreamUncompressor_STRv2();
        MdecCode code = new MdecCode();
        for (int i = -512; i < 512; i++) {
            baos.reset();
            IO.writeInt16LE(baos, 1);
            IO.writeInt16LE(baos, 0x3800);
            IO.writeInt16LE(baos, 1);
            IO.writeInt16LE(baos, 2);
            BitStreamWriter bw = new BitStreamWriter(baos);
            bw.setLittleEndian(true);
            bw.write(i, 10);
            bw.close();

            v2.reset(baos.toByteArray());
            v2.readMdecCode(code); // DC
            assertEquals(new MdecCode(1, i), code);
        }

    }

    @Test
    public void v2AC() throws IOException, NotThisTypeException, MdecException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamUncompressor_STRv2 v2 = new BitStreamUncompressor_STRv2();
        MdecCode code = new MdecCode();
        for (ACVariableLengthCode vlc : AC_VARIABLE_LENGTH_CODES_MPEG1) {
            for (int i = 0; i < 1; i++) {
                baos.reset();
                IO.writeInt16LE(baos, 1);
                IO.writeInt16LE(baos, 0x3800);
                IO.writeInt16LE(baos, 1);
                IO.writeInt16LE(baos, 2);
                BitStreamWriter bw = new BitStreamWriter(baos);
                bw.setLittleEndian(true);
                bw.write(1, 10);
                bw.write(vlc.VariableLengthCode);
                bw.write(i != 0);
                bw.close();

                v2.reset(baos.toByteArray());
                v2.readMdecCode(code); // DC
                assertEquals(new MdecCode(1, 1), code);
                v2.readMdecCode(code); // AC
                assertEquals(new MdecCode(vlc.RunOfZeros, i == 0 ? vlc.AbsoluteLevel : -vlc.AbsoluteLevel), code);
            }
        }

    }

    @Test
    public void v2ACEscape() throws IOException, NotThisTypeException, MdecException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamUncompressor_STRv2 v2 = new BitStreamUncompressor_STRv2();
        MdecCode code = new MdecCode();
        for (int iRun = 0; iRun < 63; iRun++) {
            for (int iAc = -512; iAc < 512; iAc++) {
                baos.reset();
                IO.writeInt16LE(baos, 1);
                IO.writeInt16LE(baos, 0x3800);
                IO.writeInt16LE(baos, 1);
                IO.writeInt16LE(baos, 2);
                BitStreamWriter bw = new BitStreamWriter(baos);
                bw.setLittleEndian(true);
                bw.write(1, 10);
                bw.write(AC_ESCAPE_CODE);
                bw.write(iRun, 6);
                bw.write(iAc, 10);
                bw.close();

                v2.reset(baos.toByteArray());
                v2.readMdecCode(code); // DC
                assertEquals(new MdecCode(1, 1), code);
                v2.readMdecCode(code); // AC
                assertEquals(new MdecCode(iRun, iAc), code);
            }
        }
    }

    @Test
    public void badCode14() throws MdecException, IOException, NotThisTypeException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitStreamUncompressor_STRv2 v2 = new BitStreamUncompressor_STRv2();
        MdecCode code = new MdecCode();
        baos.reset();
        IO.writeInt16LE(baos, 1);
        IO.writeInt16LE(baos, 0x3800);
        IO.writeInt16LE(baos, 1);
        IO.writeInt16LE(baos, 2);
        BitStreamWriter bw = new BitStreamWriter(baos);
        bw.setLittleEndian(true);
        bw.write(1,  10); // DC
        bw.write(14, 17); // Bad AC
        bw.close();

        v2.reset(baos.toByteArray());
        v2.readMdecCode(code); // DC
        assertEquals(new MdecCode(1, 1), code);
        try {
            v2.readMdecCode(code); // AC
            fail("Expected exception");
        } catch (MdecException ex) {
            // good
        }
   }

}