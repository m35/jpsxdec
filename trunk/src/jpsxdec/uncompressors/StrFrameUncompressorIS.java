/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * StrFrameUncompresserIS.java
 */

package jpsxdec.uncompressors;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import jpsxdec.mdec.MDEC;
import jpsxdec.demuxers.StrFramePullDemuxerIS;
import jpsxdec.mdec.MDEC.Mdec16Bits;
import jpsxdec.uncompressors.BufferedBitReader;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IWidthHeight;

/** Class to decode/uncompress the demuxed video frame data from a 
 *  Playstation disc. This class handles every known variation
 *  of compressed zero-run-length codes, and accompanying headers. */
public class StrFrameUncompressorIS 
    //extends InputStream 
    implements /*IGetFilePointer,*/ IWidthHeight
{
    /*########################################################################*/
    /*## Static stuff ########################################################*/
    /*########################################################################*/
    
    public static int DebugVerbose = 2;
    
    /* ---------------------------------------------------------------------- */
    /* STR version 3 frames stuff ------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    // The following is stuff specific for version 3 frames
    
    /** Holds information for the version 3 DC variable length code lookup */
    protected static class DCVariableLengthCode {
        public String VariableLengthCode;
        public int DC_Length;
        public int DC_DifferentialLookup[];
        
        /** Constructor */
        public DCVariableLengthCode(String vlc, int len, int differential[]) {
            VariableLengthCode = vlc;
            DC_Length = len;
            DC_DifferentialLookup = differential;
        }
    }
    
    // .........................................................................
    
    /* From the offical MPEG-1 ISO standard specification (ISO 11172).
     * Specifically table 2-D.12 in 11172-2. 
     * These tables are only used for version 3 STR frames. */
     
    /** The longest of all the DC Chrominance variable-length-codes is 8 bits */
    protected final static int DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE = 8;
    
    /** Table of DC Chrominance (Cr, Cb) variable length codes */
    protected final static 
                DCVariableLengthCode DC_Chrominance_VariableLengthCodes[] = 
    {                    // code,  length,  differential lookup list
new DCVariableLengthCode("11111110" , 8, DCDifferential(-255, -128,  128, 255)),
new DCVariableLengthCode("1111110"  , 7, DCDifferential(-127,  -64,   64, 127)),
new DCVariableLengthCode("111110"   , 6, DCDifferential( -63,  -32,   32,  63)),
new DCVariableLengthCode("11110"    , 5, DCDifferential( -31,  -16,   16,  31)),
new DCVariableLengthCode("1110"     , 4, DCDifferential( -15,   -8,    8,  15)),
new DCVariableLengthCode("110"      , 3, DCDifferential(  -7,   -4,    4,   7)),
new DCVariableLengthCode("10"       , 2, DCDifferential(  -3,   -2,    2,   3)),
new DCVariableLengthCode("01"       , 1, DCDifferential(  -1,   -1,    1,   1)),
new DCVariableLengthCode("00"       , 0, null)
    };
    
    
    /** The longest of all the DC Luminance variable-length-codes is 7 bits */
    protected final static int DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE = 7;
    
    /** Table of DC Luminance (Y1, Y2, Y3, Y4) variable length codes */
    protected final static 
                DCVariableLengthCode DC_Luminance_VariableLengthCodes[] = 
    {                    // code,  length,  differential lookup list
new DCVariableLengthCode("1111110" , 8,  DCDifferential(-255, -128,  128, 255)),
new DCVariableLengthCode("111110"  , 7,  DCDifferential(-127,  -64,   64, 127)),
new DCVariableLengthCode("11110"   , 6,  DCDifferential( -63,  -32,   32,  63)),
new DCVariableLengthCode("1110"    , 5,  DCDifferential( -31,  -16,   16,  31)),
new DCVariableLengthCode("110"     , 4,  DCDifferential( -15,   -8,    8,  15)),
new DCVariableLengthCode("101"     , 3,  DCDifferential(  -7,   -4,    4,   7)),
new DCVariableLengthCode("01"      , 2,  DCDifferential(  -3,   -2,    2,   3)),
new DCVariableLengthCode("00"      , 1,  DCDifferential(  -1,   -1,    1,   1)),
new DCVariableLengthCode("100"     , 0,  null)            
    };

    /** Construct a DC differential lookup list. Used only for the
     * DC_Chrominance_VariableLengthCodes and DC_Luminance_VariableLengthCodes 
     * lists. */
    protected static int[] DCDifferential(int iNegitiveStart, int iNegitiveEnd, 
                                        int iPositiveStart, int iPositiveEnd) 
    {
        int aiDifferentialArray[];
        
        int iArraySize = (iNegitiveEnd - iNegitiveStart + 1) 
                       + (iPositiveEnd - iPositiveStart + 1);
        
        aiDifferentialArray = new int[iArraySize];
        int iDifferentialArrayIndex = 0;
        
        for (int i = iNegitiveStart; i <= iNegitiveEnd; i++) {
            aiDifferentialArray[iDifferentialArrayIndex] = i;
            iDifferentialArrayIndex++;
        }
        
        for (int i = iPositiveStart; i <= iPositiveEnd; i++) {
            aiDifferentialArray[iDifferentialArrayIndex] = i;
            iDifferentialArrayIndex++;
        }
        
        return aiDifferentialArray;
    }
    
    /* ---------------------------------------------------------------------- */
    /* AC Variable length code stuff ---------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Represents a AC variable length code. */
    protected static class ACVariableLengthCode {
        public String VariableLengthCode;
        public int RunOfZeros;
        public int AbsoluteLevel;
        
        public ACVariableLengthCode(String vlc, int run, int level)
        {
            VariableLengthCode = vlc;
            RunOfZeros = run;
            AbsoluteLevel = level;
        }
    }
    
    // .........................................................................
    
    /** Sequence of bits indicating an escape code. */
    public final static String AC_ESCAPE_CODE = "000001";
    
    /** Sequence of bits indicating the end of a block.
     * Unlike the MPEG1 specification, these bits can, and often do appear as 
     * the first and only variable-length-code in a block. */
    public final static String VLC_END_OF_BLOCK = "10"; // bits 10
    
    /** The longest of all the AC variable-length-codes is 16 bits. */
    protected final static int AC_LONGEST_VARIABLE_LENGTH_CODE = 16;
    
    protected final static ACVariableLengthCode AC_VARIABLE_LENGTH_CODES_MPEG1[] = 
    {
    /*  new ACVariableLengthCode("1"                 , 0 , 1  ),
          The MPEG1 specification declares that if the first 
          variable-length-code in a block is "1" that it should be translated
          to the run-length-code (0, 1). The PSX variable-length-code
          decoding does not follow this rule. */
        
                             //  Code               "Run" "Level"
        new ACVariableLengthCode("11"                , 0 , 1  ),
        new ACVariableLengthCode("011"               , 1 , 1  ),
        new ACVariableLengthCode("0100"              , 0 , 2  ),
        new ACVariableLengthCode("0101"              , 2 , 1  ),
        new ACVariableLengthCode("00101"             , 0 , 3  ),
        new ACVariableLengthCode("00110"             , 4 , 1  ),
        new ACVariableLengthCode("00111"             , 3 , 1  ),
        new ACVariableLengthCode("000100"            , 7 , 1  ),
        new ACVariableLengthCode("000101"            , 6 , 1  ),
        new ACVariableLengthCode("000110"            , 1 , 2  ),
        new ACVariableLengthCode("000111"            , 5 , 1  ),
        new ACVariableLengthCode("0000100"           , 2 , 2  ),
        new ACVariableLengthCode("0000101"           , 9 , 1  ),
        new ACVariableLengthCode("0000110"           , 0 , 4  ),
        new ACVariableLengthCode("0000111"           , 8 , 1  ),
        new ACVariableLengthCode("00100000"          , 13, 1  ),
        new ACVariableLengthCode("00100001"          , 0 , 6  ),
        new ACVariableLengthCode("00100010"          , 12, 1  ),
        new ACVariableLengthCode("00100011"          , 11, 1  ),
        new ACVariableLengthCode("00100100"          , 3 , 2  ),
        new ACVariableLengthCode("00100101"          , 1 , 3  ),
        new ACVariableLengthCode("00100110"          , 0 , 5  ),
        new ACVariableLengthCode("00100111"          , 10, 1  ),
        new ACVariableLengthCode("0000001000"        , 16, 1  ),
        new ACVariableLengthCode("0000001001"        , 5 , 2  ),
        new ACVariableLengthCode("0000001010"        , 0 , 7  ),
        new ACVariableLengthCode("0000001011"        , 2 , 3  ),
        new ACVariableLengthCode("0000001100"        , 1 , 4  ),
        new ACVariableLengthCode("0000001101"        , 15, 1  ),
        new ACVariableLengthCode("0000001110"        , 14, 1  ),
        new ACVariableLengthCode("0000001111"        , 4 , 2  ),
        new ACVariableLengthCode("000000010000"      , 0 , 11 ),
        new ACVariableLengthCode("000000010001"      , 8 , 2  ),
        new ACVariableLengthCode("000000010010"      , 4 , 3  ),
        new ACVariableLengthCode("000000010011"      , 0 , 10 ),
        new ACVariableLengthCode("000000010100"      , 2 , 4  ),
        new ACVariableLengthCode("000000010101"      , 7 , 2  ),
        new ACVariableLengthCode("000000010110"      , 21, 1  ),
        new ACVariableLengthCode("000000010111"      , 20, 1  ),
        new ACVariableLengthCode("000000011000"      , 0 , 9  ),
        new ACVariableLengthCode("000000011001"      , 19, 1  ),
        new ACVariableLengthCode("000000011010"      , 18, 1  ),
        new ACVariableLengthCode("000000011011"      , 1 , 5  ),
        new ACVariableLengthCode("000000011100"      , 3 , 3  ),
        new ACVariableLengthCode("000000011101"      , 0 , 8  ),
        new ACVariableLengthCode("000000011110"      , 6 , 2  ),
        new ACVariableLengthCode("000000011111"      , 17, 1  ),
        new ACVariableLengthCode("0000000010000"     , 10, 2  ),
        new ACVariableLengthCode("0000000010001"     , 9 , 2  ),
        new ACVariableLengthCode("0000000010010"     , 5 , 3  ),
        new ACVariableLengthCode("0000000010011"     , 3 , 4  ),
        new ACVariableLengthCode("0000000010100"     , 2 , 5  ),
        new ACVariableLengthCode("0000000010101"     , 1 , 7  ),
        new ACVariableLengthCode("0000000010110"     , 1 , 6  ),
        new ACVariableLengthCode("0000000010111"     , 0 , 15 ),
        new ACVariableLengthCode("0000000011000"     , 0 , 14 ),
        new ACVariableLengthCode("0000000011001"     , 0 , 13 ),
        new ACVariableLengthCode("0000000011010"     , 0 , 12 ),
        new ACVariableLengthCode("0000000011011"     , 26, 1  ),
        new ACVariableLengthCode("0000000011100"     , 25, 1  ),
        new ACVariableLengthCode("0000000011101"     , 24, 1  ),
        new ACVariableLengthCode("0000000011110"     , 23, 1  ),
        new ACVariableLengthCode("0000000011111"     , 22, 1  ),
        new ACVariableLengthCode("00000000010000"    , 0 , 31 ),
        new ACVariableLengthCode("00000000010001"    , 0 , 30 ),
        new ACVariableLengthCode("00000000010010"    , 0 , 29 ),
        new ACVariableLengthCode("00000000010011"    , 0 , 28 ),
        new ACVariableLengthCode("00000000010100"    , 0 , 27 ),
        new ACVariableLengthCode("00000000010101"    , 0 , 26 ),
        new ACVariableLengthCode("00000000010110"    , 0 , 25 ),
        new ACVariableLengthCode("00000000010111"    , 0 , 24 ),
        new ACVariableLengthCode("00000000011000"    , 0 , 23 ),
        new ACVariableLengthCode("00000000011001"    , 0 , 22 ),
        new ACVariableLengthCode("00000000011010"    , 0 , 21 ),
        new ACVariableLengthCode("00000000011011"    , 0 , 20 ),
        new ACVariableLengthCode("00000000011100"    , 0 , 19 ),
        new ACVariableLengthCode("00000000011101"    , 0 , 18 ),
        new ACVariableLengthCode("00000000011110"    , 0 , 17 ),
        new ACVariableLengthCode("00000000011111"    , 0 , 16 ),
        new ACVariableLengthCode("000000000010000"   , 0 , 40 ),
        new ACVariableLengthCode("000000000010001"   , 0 , 39 ),
        new ACVariableLengthCode("000000000010010"   , 0 , 38 ),
        new ACVariableLengthCode("000000000010011"   , 0 , 37 ),
        new ACVariableLengthCode("000000000010100"   , 0 , 36 ),
        new ACVariableLengthCode("000000000010101"   , 0 , 35 ),
        new ACVariableLengthCode("000000000010110"   , 0 , 34 ),
        new ACVariableLengthCode("000000000010111"   , 0 , 33 ),
        new ACVariableLengthCode("000000000011000"   , 0 , 32 ),
        new ACVariableLengthCode("000000000011001"   , 1 , 14 ),
        new ACVariableLengthCode("000000000011010"   , 1 , 13 ),
        new ACVariableLengthCode("000000000011011"   , 1 , 12 ),
        new ACVariableLengthCode("000000000011100"   , 1 , 11 ),
        new ACVariableLengthCode("000000000011101"   , 1 , 10 ),
        new ACVariableLengthCode("000000000011110"   , 1 , 9  ),
        new ACVariableLengthCode("000000000011111"   , 1 , 8  ),
        new ACVariableLengthCode("0000000000010000"  , 1 , 18 ),
        new ACVariableLengthCode("0000000000010001"  , 1 , 17 ),
        new ACVariableLengthCode("0000000000010010"  , 1 , 16 ),
        new ACVariableLengthCode("0000000000010011"  , 1 , 15 ),
        new ACVariableLengthCode("0000000000010100"  , 6 , 3  ),
        new ACVariableLengthCode("0000000000010101"  , 16, 2  ),
        new ACVariableLengthCode("0000000000010110"  , 15, 2  ),
        new ACVariableLengthCode("0000000000010111"  , 14, 2  ),
        new ACVariableLengthCode("0000000000011000"  , 13, 2  ),
        new ACVariableLengthCode("0000000000011001"  , 12, 2  ),
        new ACVariableLengthCode("0000000000011010"  , 11, 2  ),
        new ACVariableLengthCode("0000000000011011"  , 31, 1  ),
        new ACVariableLengthCode("0000000000011100"  , 30, 1  ),
        new ACVariableLengthCode("0000000000011101"  , 29, 1  ),
        new ACVariableLengthCode("0000000000011110"  , 28, 1  ),
        new ACVariableLengthCode("0000000000011111"  , 27, 1  )
    };

    /** The custom Serial Experiments Lain Playstation game
     *  variable-length-code table. */
    protected final static ACVariableLengthCode AC_VARIABLE_LENGTH_CODES_LAIN[] = 
    {
                              // Code               "Run" "Level"
        new ACVariableLengthCode("11"                , 0  , 1  ),
        new ACVariableLengthCode("011"               , 0  , 2  ),
        new ACVariableLengthCode("0100"              , 1  , 1  ),
        new ACVariableLengthCode("0101"              , 0  , 3  ),
        new ACVariableLengthCode("00101"             , 0  , 4  ),
        new ACVariableLengthCode("00110"             , 2  , 1  ),
        new ACVariableLengthCode("00111"             , 0  , 5  ),
        new ACVariableLengthCode("000100"            , 0  , 6  ),
        new ACVariableLengthCode("000101"            , 3  , 1  ),
        new ACVariableLengthCode("000110"            , 1  , 2  ),
        new ACVariableLengthCode("000111"            , 0  , 7  ),
        new ACVariableLengthCode("0000100"           , 0  , 8  ),
        new ACVariableLengthCode("0000101"           , 4  , 1  ),
        new ACVariableLengthCode("0000110"           , 0  , 9  ),
        new ACVariableLengthCode("0000111"           , 5  , 1  ),
        new ACVariableLengthCode("00100000"          , 0  , 10 ),
        new ACVariableLengthCode("00100001"          , 0  , 11 ),
        new ACVariableLengthCode("00100010"          , 1  , 3  ),
        new ACVariableLengthCode("00100011"          , 6  , 1  ),
        new ACVariableLengthCode("00100100"          , 0  , 12 ),
        new ACVariableLengthCode("00100101"          , 0  , 13 ),
        new ACVariableLengthCode("00100110"          , 7  , 1  ),
        new ACVariableLengthCode("00100111"          , 0  , 14 ),
        new ACVariableLengthCode("0000001000"        , 0  , 15 ),
        new ACVariableLengthCode("0000001001"        , 2  , 2  ),
        new ACVariableLengthCode("0000001010"        , 8  , 1  ),
        new ACVariableLengthCode("0000001011"        , 1  , 4  ),
        new ACVariableLengthCode("0000001100"        , 0  , 16 ),
        new ACVariableLengthCode("0000001101"        , 0  , 17 ),
        new ACVariableLengthCode("0000001110"        , 9  , 1  ),
        new ACVariableLengthCode("0000001111"        , 0  , 18 ),
        new ACVariableLengthCode("000000010000"      , 0  , 19 ),
        new ACVariableLengthCode("000000010001"      , 1  , 5  ),
        new ACVariableLengthCode("000000010010"      , 0  , 20 ),
        new ACVariableLengthCode("000000010011"      , 10 , 1  ),
        new ACVariableLengthCode("000000010100"      , 0  , 21 ),
        new ACVariableLengthCode("000000010101"      , 3  , 2  ),
        new ACVariableLengthCode("000000010110"      , 12 , 1  ),
        new ACVariableLengthCode("000000010111"      , 0  , 23 ),
        new ACVariableLengthCode("000000011000"      , 0  , 22 ),
        new ACVariableLengthCode("000000011001"      , 11 , 1  ),
        new ACVariableLengthCode("000000011010"      , 0  , 24 ),
        new ACVariableLengthCode("000000011011"      , 0  , 28 ),
        new ACVariableLengthCode("000000011100"      , 0  , 25 ),
        new ACVariableLengthCode("000000011101"      , 1  , 6  ),
        new ACVariableLengthCode("000000011110"      , 2  , 3  ),
        new ACVariableLengthCode("000000011111"      , 0  , 27 ),
        new ACVariableLengthCode("0000000010000"     , 0  , 26 ),
        new ACVariableLengthCode("0000000010001"     , 13 , 1  ),
        new ACVariableLengthCode("0000000010010"     , 0  , 29 ),
        new ACVariableLengthCode("0000000010011"     , 1  , 7  ),
        new ACVariableLengthCode("0000000010100"     , 4  , 2  ),
        new ACVariableLengthCode("0000000010101"     , 0  , 31 ),
        new ACVariableLengthCode("0000000010110"     , 0  , 30 ),
        new ACVariableLengthCode("0000000010111"     , 14 , 1  ),
        new ACVariableLengthCode("0000000011000"     , 0  , 32 ),
        new ACVariableLengthCode("0000000011001"     , 0  , 33 ),
        new ACVariableLengthCode("0000000011010"     , 1  , 8  ),
        new ACVariableLengthCode("0000000011011"     , 0  , 35 ),
        new ACVariableLengthCode("0000000011100"     , 0  , 34 ),
        new ACVariableLengthCode("0000000011101"     , 5  , 2  ),
        new ACVariableLengthCode("0000000011110"     , 0  , 36 ),
        new ACVariableLengthCode("0000000011111"     , 0  , 37 ),
        new ACVariableLengthCode("00000000010000"    , 2  , 4  ),
        new ACVariableLengthCode("00000000010001"    , 1  , 9  ),
        new ACVariableLengthCode("00000000010010"    , 1  , 24 ),
        new ACVariableLengthCode("00000000010011"    , 0  , 38 ),
        new ACVariableLengthCode("00000000010100"    , 15 , 1  ),
        new ACVariableLengthCode("00000000010101"    , 0  , 39 ),
        new ACVariableLengthCode("00000000010110"    , 3  , 3  ),
        new ACVariableLengthCode("00000000010111"    , 7  , 3  ),
        new ACVariableLengthCode("00000000011000"    , 0  , 40 ),
        new ACVariableLengthCode("00000000011001"    , 0  , 41 ),
        new ACVariableLengthCode("00000000011010"    , 0  , 42 ),
        new ACVariableLengthCode("00000000011011"    , 0  , 43 ),
        new ACVariableLengthCode("00000000011100"    , 1  , 10 ),
        new ACVariableLengthCode("00000000011101"    , 0  , 44 ),
        new ACVariableLengthCode("00000000011110"    , 6  , 2  ),
        new ACVariableLengthCode("00000000011111"    , 0  , 45 ),
        new ACVariableLengthCode("000000000010000"   , 0  , 47 ),
        new ACVariableLengthCode("000000000010001"   , 0  , 46 ),
        new ACVariableLengthCode("000000000010010"   , 16 , 1  ),
        new ACVariableLengthCode("000000000010011"   , 2  , 5  ),
        new ACVariableLengthCode("000000000010100"   , 0  , 48 ),
        new ACVariableLengthCode("000000000010101"   , 1  , 11 ),
        new ACVariableLengthCode("000000000010110"   , 0  , 49 ),
        new ACVariableLengthCode("000000000010111"   , 0  , 51 ),
        new ACVariableLengthCode("000000000011000"   , 0  , 50 ),
        new ACVariableLengthCode("000000000011001"   , 7  , 2  ),
        new ACVariableLengthCode("000000000011010"   , 0  , 52 ),
        new ACVariableLengthCode("000000000011011"   , 4  , 3  ),
        new ACVariableLengthCode("000000000011100"   , 0  , 53 ),
        new ACVariableLengthCode("000000000011101"   , 17 , 1  ),
        new ACVariableLengthCode("000000000011110"   , 1  , 12 ),
        new ACVariableLengthCode("000000000011111"   , 0  , 55 ),
        new ACVariableLengthCode("0000000000010000"  , 0  , 54 ),
        new ACVariableLengthCode("0000000000010001"  , 0  , 56 ),
        new ACVariableLengthCode("0000000000010010"  , 0  , 57 ),
        new ACVariableLengthCode("0000000000010011"  , 21 , 1  ),
        new ACVariableLengthCode("0000000000010100"  , 0  , 58 ),
        new ACVariableLengthCode("0000000000010101"  , 3  , 4  ),
        new ACVariableLengthCode("0000000000010110"  , 1  , 13 ),
        new ACVariableLengthCode("0000000000010111"  , 23 , 1  ),
        new ACVariableLengthCode("0000000000011000"  , 8  , 2  ),
        new ACVariableLengthCode("0000000000011001"  , 0  , 59 ),
        new ACVariableLengthCode("0000000000011010"  , 2  , 6  ),
        new ACVariableLengthCode("0000000000011011"  , 19 , 1  ),
        new ACVariableLengthCode("0000000000011100"  , 0  , 60 ),
        new ACVariableLengthCode("0000000000011101"  , 9  , 2  ),
        new ACVariableLengthCode("0000000000011110"  , 24 , 1  ),
        new ACVariableLengthCode("0000000000011111"  , 18 , 1  )
    };   
     
    public static class MacroBlock implements Iterable<MDEC.Mdec16Bits> {
        public Block Cr;
        public Block Cb;
        public Block Y1;
        public Block Y2;
        public Block Y3;
        public Block Y4;
        
        public void setBlock(String sBlk, Block oBlk) {
            if (sBlk.equals("Cr"))
                Cr = oBlk;
            else if (sBlk.equals("Cb"))
                Cb = oBlk;
            else if (sBlk.equals("Y1"))
                Y1 = oBlk;
            else if (sBlk.equals("Y2"))
                Y2 = oBlk;
            else if (sBlk.equals("Y3"))
                Y3 = oBlk;
            else if (sBlk.equals("Y4"))
                Y4 = oBlk;
        }
        
        public Block getBlock(String sBlk) {
            if (sBlk.equals("Cr"))
                return Cr;
            else if (sBlk.equals("Cb"))
                return Cb;
            else if (sBlk.equals("Y1"))
                return Y1;
            else if (sBlk.equals("Y2"))
                return Y2;
            else if (sBlk.equals("Y3"))
                return Y3;
            else if (sBlk.equals("Y4"))
                return Y4;
            else
                throw new IllegalArgumentException("Invalid block name " + sBlk);
        }
        
        public Block getBlock(int iBlk) {
            switch (iBlk) {
                case 0: return Cr;
                case 1: return Cb;
                case 2: return Y1;
                case 3: return Y2;
                case 4: return Y3;
                case 5: return Y4;
                default: 
                    throw new IllegalArgumentException("Invalid block number " + iBlk);
            }
        }
        
        public long getBitLength() {
            return Cr.getBitLength() +
                   Cb.getBitLength() +
                   Y1.getBitLength() +
                   Y2.getBitLength() +
                   Y3.getBitLength() +
                   Y4.getBitLength();
        }
        
        public long getMdecCodeCount() {
            return Cr.getMdecCodeCount() +
                   Cb.getMdecCodeCount() +
                   Y1.getMdecCodeCount() +
                   Y2.getMdecCodeCount() +
                   Y3.getMdecCodeCount() +
                   Y4.getMdecCodeCount();
        }
        
        public Object clone() {
            MacroBlock oNew = new MacroBlock();
            oNew.Cr = (Block)Cr.clone();
            oNew.Cb = (Block)Cb.clone();
            oNew.Y1 = (Block)Y1.clone();
            oNew.Y1 = (Block)Y2.clone();
            oNew.Y1 = (Block)Y3.clone();
            oNew.Y1 = (Block)Y4.clone();
            return oNew;
        }

        public Iterator<Mdec16Bits> iterator() {
            return new Iterator<Mdec16Bits>() {

                private int m_iBlk = 0;
                private int m_iMdec = 0;
                
                public boolean hasNext() {
                    return m_iBlk < 6;
                }

                public Mdec16Bits next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    Mdec16Bits oMdec = getBlock(m_iBlk).getMdecCode(m_iMdec);
                    m_iMdec++;
                    if (m_iMdec == getBlock(m_iBlk).getMdecCodeCount()) {
                        m_iMdec = 0;
                        m_iBlk++;
                    }
                    return oMdec;
                }

                public void remove() {
                    throw new UnsupportedOperationException("Can't remove macroblocks from list.");
                }
            };
        }
    }
    
    public static class Block {
        public MDEC.Mdec16Bits DCCoefficient;
        public MDEC.Mdec16Bits[] ACCoefficients;
        public MDEC.Mdec16Bits EndOfBlock;
        
        public MDEC.Mdec16Bits getMdecCode(int i) {
            if (i == 0)
                return DCCoefficient;
            else if (i >= 1 && i <= ACCoefficients.length)
                return ACCoefficients[i-1];
            else if (i == ACCoefficients.length+1)
                return EndOfBlock;
            else
                throw new IllegalArgumentException("Invalid MDEC code index " + i);
        }
        
        public int getMdecCodeCount() {
            return ACCoefficients.length + 2;
        }
        
        public long getBitLength() {
            long lngLength = DCCoefficient.VariableLengthCodeBits.length();
            for (MDEC.Mdec16Bits oMdec : ACCoefficients)
                lngLength += oMdec.VariableLengthCodeBits.length();
            return lngLength + EndOfBlock.VariableLengthCodeBits.length();
        }
        
        public Object clone() {
            Block oNew = new Block();
            oNew.DCCoefficient = (MDEC.Mdec16Bits)DCCoefficient.clone();
            oNew.ACCoefficients = new MDEC.Mdec16Bits[ACCoefficients.length];
            for (int i = 0; i < ACCoefficients.length; i++)
                oNew.ACCoefficients[i] = (MDEC.Mdec16Bits)ACCoefficients[i].clone();
            oNew.EndOfBlock = (MDEC.Mdec16Bits)EndOfBlock.clone();
            return oNew;
        }
    }
    
    public class MdecReader extends InputStream implements IGetFilePointer {

        MacroBlock[] m_aoMacroBlocks;
        
        int m_iCurrentMacroBlock = 0;
        int m_iCurrentBlock = 0;
        int m_iCurrentMdecCode = 0;
        boolean m_blnReadLowByte = true;
        
        MDEC.Mdec16Bits m_oCurrentMdecCode;
        
        public MdecReader(MacroBlock[] aoMacroBlocks) {
            m_aoMacroBlocks = aoMacroBlocks;
            m_oCurrentMdecCode = m_aoMacroBlocks[0].Cr.DCCoefficient;
        }
        
        @Override
        public int read() throws IOException {
            if (m_iCurrentMacroBlock >= m_aoMacroBlocks.length) return -1;
            
            MacroBlock oCurMacBlk = m_aoMacroBlocks[m_iCurrentMacroBlock];
            
            if (oCurMacBlk == null) 
                throw m_oFailException;
            
            Block oCurBlk = oCurMacBlk.getBlock(m_iCurrentBlock);
            
            m_oCurrentMdecCode = oCurBlk.getMdecCode(m_iCurrentMdecCode);
            
            int iRet;
            
            // Alternate between reading the bottom byte and the top byte
            if (m_blnReadLowByte)
                iRet = (int)(m_oCurrentMdecCode.toMdecWord() & 0xFF);
            else {
                iRet = (int)((m_oCurrentMdecCode.toMdecWord() >>> 8) & 0xFF);
            }
            
            // increment the read pointers
            if (m_blnReadLowByte) 
                m_blnReadLowByte = false;
            else {
                m_blnReadLowByte = true;
                m_iCurrentMdecCode++;
                if (m_iCurrentMdecCode == oCurBlk.getMdecCodeCount()) {
                    m_iCurrentMdecCode = 0;
                    m_iCurrentBlock++;
                    if (m_iCurrentBlock == 6) {
                        m_iCurrentBlock = 0;
                        m_iCurrentMacroBlock++;
                    }
                }
            }
            
            return iRet;
        }

        public long getFilePointer() {
            return (long)m_oCurrentMdecCode.OriginalFilePos;
        }
        
    }
    
    
    /*########################################################################*/
    /*## Beginning of instance ###############################################*/
    /*########################################################################*/
    
    /** Binary bit reader encapsulates the 
     * InputStream to read as a bit stream. */
    protected BufferedBitReader m_oBitReader;
    
    /** A queue to store the uncompressed data as MDEC codes. */
    protected MacroBlock[] m_oMdecList;
    
    // Frame info
    protected long m_lngNumberOfRunLenthCodes;
    protected long m_lngHeader3800;
    /** The Chrominance quantization scale used throughout the frame. */
    protected long m_lngQuantizationScaleChrom;
    /** The Luminance quantization scale used throughout the frame. */
    protected long m_lngQuantizationScaleLumin;
    
    /** Most games use verion 2 or version 3. Currently handled exceptions
     *  are: FF7 uses version 1, Lain uses version 0 */
    protected long m_lngVersion;

    /** Width of the frame in pixels */
    protected long m_lngWidth;
    /** Height of the frame in pixels */
    protected long m_lngHeight;
    
    // For version 3 frames, all DC Coefficients are relative
    // TODO: Maybe try to encapsulate this somehow
    protected int m_iPreviousCr_DC = 0;
    protected int m_iPreviousCb_DC = 0;
    protected int m_iPreviousY_DC = 0;
    
    /** If there was an error decoding, don't pass the error up until reading
     * reaches the end of everything that could be decoded. */
    protected IOException m_oFailException = null;

    protected int m_iFrameType = -1;
    
    public final static int FRAME_VER2 = 2;
    public final static int FRAME_VER3 = 3;
    public final static int FRAME_LAIN = 0;
    public final static int FRAME_LAIN_FINAL_MOVIE = 10;
    public final static int FRAME_FF7 = 1;
    public final static int FRAME_FF7_WITHOUT_CAMERA = 11;
    public final static int FRAME_LOGO_IKI = 256;

    MdecReader m_oMdecReader;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public StrFrameUncompressorIS(InputStream oIS, long lngWidth, 
                                                   long lngHeight) 
        throws IOException 
    {
        
        // New bit reader. 
        // Read the data as Little-endian (not big-endian)
        // Be prepared to buffer 30 WORDs of data
        m_oBitReader = new BufferedBitReader(oIS, false, 30);
        
        /////////////////////////////////////////////////////////////
        // Determine the frame type from the first 50 bytes of data
        /////////////////////////////////////////////////////////////
        m_iFrameType = IdentifyFrame(m_oBitReader.PeekBytes(50), oIS);
        
        if (DebugVerbose >= 3) {
            System.err.print("Identified frame as ");
            switch (m_iFrameType) {
                case FRAME_VER2: System.err.println("Standard v2"); break;
                case FRAME_VER3: System.err.println("Standard v3"); break;
                case FRAME_LAIN: System.err.println("Lain"); break;
                case FRAME_LAIN_FINAL_MOVIE: 
                    System.err.println("Lain-Final Movie"); break;
                case FRAME_FF7: System.err.println("Final Fantasy 7"); break;
                case FRAME_FF7_WITHOUT_CAMERA: 
                    System.err.println("Final Fantasy 7-no camera"); break;
                case FRAME_LOGO_IKI:
                    System.err.println("logo.iki"); break;
                default:
                    System.err.println("Unknown??");
            }
        }

        /////////////////////////////////////////////////////////////
        // Now read the stream header information
        /////////////////////////////////////////////////////////////
        
        switch (m_iFrameType) {
            case FRAME_FF7:
                // FF7 videos have 40 bytes of camera data at the start of the frame
                m_oBitReader.SkipBits(40*8);
            case FRAME_FF7_WITHOUT_CAMERA:
            case FRAME_VER2: case FRAME_VER3:
                m_lngNumberOfRunLenthCodes = m_oBitReader.ReadUnsignedBits(16);
                m_lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                m_lngQuantizationScaleChrom = 
                m_lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(16);
                m_lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                break;
                
            case FRAME_LAIN_FINAL_MOVIE:
            case FRAME_LAIN:
                // because it's set to little-endian right now, 
                // these 16 bits are reversed
                m_lngQuantizationScaleChrom = (int)m_oBitReader.ReadUnsignedBits(8);
                m_lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(8);
                
                m_lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                m_lngNumberOfRunLenthCodes = (int)m_oBitReader.ReadUnsignedBits(16);
                m_lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                // Lain also uses an actual byte stream, so we want big-endian reads
                m_oBitReader.setBigEndian(true);
                break;
                
            case FRAME_LOGO_IKI:
                throw new CriticalUncompressException(
                        "This appears to be the infamous logo.iki. " +
                        "Sorry, but I have no idea how to decode this thing.");
            default:
                throw new CriticalUncompressException("Unknown frame type.");
        }
        
        // provide some debug about what was found
        if (DebugVerbose >= 3) {
            System.err.println("Frame Header:");
            System.err.println(String.format("  %04x -> Run Lenth Code Count (?) %d", 
                    m_lngNumberOfRunLenthCodes, 
                    m_lngNumberOfRunLenthCodes));
            System.err.println(String.format("  %04x", 
                    m_lngHeader3800));
            System.err.println(String.format("  %04x -> Quantization scale %d", 
                    m_lngQuantizationScaleChrom,
                    m_lngQuantizationScaleChrom));
            System.err.println(String.format("  %04x -> Version %d", 
                    m_lngVersion,
                    m_lngVersion));
        }
        
        // We only can handle version 0, 1, 2, or 3 so far
        if (m_lngVersion < 0 || m_lngVersion > 3)
            throw new CriticalUncompressException("We don't know how to handle version " 
                                   + m_lngVersion);

        // make sure we got a 0x3800 in the header
        if (m_lngHeader3800 != 0x3800 && m_iFrameType != FRAME_LAIN_FINAL_MOVIE)
            throw new CriticalUncompressException("0x3800 not found in start of frame");
        
        // make sure the quantization scale isn't 0
        if (m_lngQuantizationScaleChrom == 0 || m_lngQuantizationScaleLumin == 0)
            throw new CriticalUncompressException("Quantization scale of 0");
        
        /////////////////////////////////////////////////////////////
        
        // Save width and height
        m_lngWidth = lngWidth;
        m_lngHeight = lngHeight;
        
        // Calculate number of macro-blocks in the frame
        long lngMacroBlockCount = CalculateMacroBlocks(lngWidth, lngHeight);
        
        // Set the array to match
        m_oMdecList = new MacroBlock[(int)lngMacroBlockCount];
        
        if (DebugVerbose >= 3) System.err.println(
                    "Expecting " + lngMacroBlockCount + " macroblocks");
        
        /////////////////////////////////////////////////////////////
        // We have everything we need
        // now uncompress the entire frame, one macroblock at a time
        /////////////////////////////////////////////////////////////
        
        // keep track of the number of run length codes read for debugging
        long lngTotalCodesRead = 0;
        try {
            for (int i = 0; i < lngMacroBlockCount; i++) {
                if (DebugVerbose >= 3)
                    System.err.println("Decoding macroblock " + i);
                
                MacroBlock oThisMacBlk = new MacroBlock();
                lngTotalCodesRead += UncompressMacroBlock(oThisMacBlk);
                m_oMdecList[i] = oThisMacBlk;
            }
        } catch (IOException e) {
            // We failed! :(
            m_oFailException = e; // save the error for later
            if (DebugVerbose >= 3)
                e.printStackTrace();
        }
        
        if (DebugVerbose >= 4)
            System.err.println(lngTotalCodesRead + " codes read");
        
        // Setup for being read from the MDEC codes collected
        m_oMdecReader = new MdecReader(m_oMdecList);
        
    }
    
    protected static int IdentifyFrame(byte[] abHeaderBytes, InputStream oIS) throws CriticalUncompressException {
        
        // if 0x3800 found at the normal position
        if (abHeaderBytes[2] == 0x38 && abHeaderBytes[3] == 0x00)
        {
            // check the version at the normal position
            int iVersion = ((abHeaderBytes[6] & 0xFF) << 8) | (abHeaderBytes[7] & 0xFF);
            switch (iVersion) {
                case 0: return FRAME_LAIN;
                // if for some reason it's an ff7 frame without the camera data
                case 1: return FRAME_FF7_WITHOUT_CAMERA; 
                case 2: return FRAME_VER2;
                case 3: return FRAME_VER3;
                case 256:
                    if ((((abHeaderBytes[4] & 0xFF) << 8) | (abHeaderBytes[5] & 0xFF)) == 320)
                        return FRAME_LOGO_IKI;
                    else
                        throw new CriticalUncompressException("Unknown frame version " + iVersion);
                default:
                    throw new CriticalUncompressException("Unknown frame version " + iVersion);
            }
        } 
        // if 0x3800 is found 40 bytes from where it should be, and the
        // relative frame version is 1
        else if (abHeaderBytes[40+2] == 0x38 && abHeaderBytes[40+3] == 0x00 &&
                 abHeaderBytes[40+7] == 1) 
        {
            // it's an ff7 header
            return FRAME_FF7;
        } 
        else // what else could it be?
        {
            // if the supposed 'version' is 0
            if (abHeaderBytes[7] == 0) {
                // and the supposed 0x3800 bytes...
                int iFrameNum = (((abHeaderBytes[2] & 0xFF) << 8) | (abHeaderBytes[3] & 0xFF));
                // ...happend to equal the frame number (if available)
                if (oIS instanceof StrFramePullDemuxerIS) {
                    if (((StrFramePullDemuxerIS)oIS).getFrameNumber() == iFrameNum) {
                        // definitely lain final movie
                        return FRAME_LAIN_FINAL_MOVIE;
                    }
                // .. or are at least less-than the number
                //    of frames in the final Lain movie
                } else if (iFrameNum >= 1 && iFrameNum <= 4765){
                    // probably lain final movie
                    return FRAME_LAIN_FINAL_MOVIE;
                } else {
                    throw new CriticalUncompressException("0x3800 not found in start of frame");
                }
            } else {
                throw new CriticalUncompressException("0x3800 not found in start of frame");
            }
        }
        throw new CriticalUncompressException("Unknown frame type");
    }
    
    protected static long CalculateMacroBlocks(long lngWidth, long lngHeight) {
        // Actual width/height in macroblocks 
        // (since you can't have a partial macroblock)
        long lngActualWidth, lngActualHeight;
        
        if ((lngWidth % 16) > 0)
            lngActualWidth = (lngWidth / 16 + 1) * 16;
        else
            lngActualWidth = lngWidth;
        
        if ((lngHeight % 16) > 0)
            lngActualHeight = (lngHeight / 16 + 1) * 16;
        else
            lngActualHeight = lngHeight;
        
        // Calculate number of macro-blocks in the frame
        return (lngActualWidth / 16) * (lngActualHeight / 16);
    }

    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    public long getWidth() {
        return m_lngWidth;
    }

    public long getHeight() {
        return m_lngHeight;
    }
        
    public long getFrameVersion() {
        return m_lngVersion;
    }
    
    public int getFrameType() {
        return m_iFrameType;
    }

    public long getQuantizationScaleChrominance() {
        return m_lngQuantizationScaleChrom;
    }

    public long getQuantizationScaleLuminance() {
        return m_lngQuantizationScaleLumin;
    }
    
    public MacroBlock[] getDecodedMacroBlocks() {
        return m_oMdecList;
    }

    public long getNumberOfRunLenthCodes() {
        return m_lngNumberOfRunLenthCodes;
    }

    public long getHeader3800() {
        return m_lngHeader3800;
    }
    
    
    public MdecReader getStream() {
        return new MdecReader(m_oMdecList);
    }
    
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    protected long UncompressMacroBlock(MacroBlock oThisMacBlk) 
            throws IOException, UncompressionException 
    {
        
        long lngTotalCodesRead = 0;
        
        if (m_iFrameType == FRAME_VER2 || 
            m_iFrameType == FRAME_FF7  ||
            m_iFrameType == FRAME_LAIN || 
            m_iFrameType == FRAME_LAIN_FINAL_MOVIE) 
        {
            
            // For version 2, all Cr, Cb, Y1, Y2, Y3, Y4 
            // DC Coefficients are encoded the same
            for (String sBlock : new String[] {"Cr","Cb","Y1","Y2","Y3","Y4"}) 
            {
                Block oThisBlk = new Block();
                DecodeV2_DC_ChrominanceOrLuminance(sBlock, oThisBlk);
                lngTotalCodesRead++;
                lngTotalCodesRead += Decode_AC_Coefficients(oThisBlk);
                
                oThisMacBlk.setBlock(sBlock, oThisBlk);
            }
            
        } else if (m_iFrameType == FRAME_VER3) 
        {
            Block oThisBlk;
            
            // For version 3, DC coefficients are encoded differently for
            // DC Chrominance and DC Luminance. 
            // In addition, the value is relative to the previous value.
            // (this is the same way mpeg-1 does it)
            
            // Cr
            oThisBlk = new Block();
            m_iPreviousCr_DC = DecodeV3_DC_ChrominanceOrLuminance(
                                m_iPreviousCr_DC, 
                                DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE, 
                                DC_Chrominance_VariableLengthCodes, 
                                "Cr", oThisBlk);
            lngTotalCodesRead++;
            lngTotalCodesRead += Decode_AC_Coefficients(oThisBlk);
            oThisMacBlk.Cr = oThisBlk;

            // Cb
            oThisBlk = new Block();
            m_iPreviousCb_DC = DecodeV3_DC_ChrominanceOrLuminance(
                                m_iPreviousCb_DC, 
                                DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE, 
                                DC_Chrominance_VariableLengthCodes, 
                                "Cb", oThisBlk);
            lngTotalCodesRead++;
            lngTotalCodesRead += Decode_AC_Coefficients(oThisBlk);
            oThisMacBlk.Cb = oThisBlk;
            
            // Y1, Y2, Y3, Y4
            for (String sBlock : new String[] {"Y1", "Y2", "Y3", "Y4"}) {
                oThisBlk = new Block();
                m_iPreviousY_DC = DecodeV3_DC_ChrominanceOrLuminance(
                                    m_iPreviousY_DC, 
                                    DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE, 
                                    DC_Luminance_VariableLengthCodes, 
                                    sBlock, oThisBlk);
                lngTotalCodesRead++;
                lngTotalCodesRead += Decode_AC_Coefficients(oThisBlk);
                
                oThisMacBlk.setBlock(sBlock, oThisBlk);
            }
            
        } else {
            throw new UncompressionException("Error decoding macro block: Unhandled version " + m_lngVersion);
        }
        
        return lngTotalCodesRead;
    }
    
    /** Decodes the DC Coefficient at the start of every block (for v.2) and
     *  adds the resulting MDEC code to the queue.
     *  DC coefficients are stored the same way for both Chrominance and
     *  Luminance in version 2 frames (althouogh Lain has a minor tweak). */
    protected void DecodeV2_DC_ChrominanceOrLuminance(String sBlock, Block oThisBlk) 
        throws IOException 
    {
        MDEC.Mdec16Bits oDCChrominanceOrLuminance = 
                new MDEC.Mdec16Bits();
        
        // Save the current file position
        oDCChrominanceOrLuminance.OriginalFilePos = m_oBitReader.getPosition();
        
        // Save the bits that make up the DC coefficient
        oDCChrominanceOrLuminance.VariableLengthCodeBits = 
                                              m_oBitReader.PeekBitsToString(10);
        // Now read the DC coefficient value
        oDCChrominanceOrLuminance.Bottom10Bits = 
                                           (int)m_oBitReader.ReadSignedBits(10);
        
        if (m_iFrameType != FRAME_LAIN && m_iFrameType != FRAME_LAIN_FINAL_MOVIE)
            
            // The bottom 10 bits now hold the DC Coefficient for the block,
            // now squeeze the frame's quantization scale into the top 6 bits
            oDCChrominanceOrLuminance.Top6Bits = 
                                        (int)(m_lngQuantizationScaleChrom & 63);
        
        else {
            // Lain uses different values for the 
            // chorminance and luminance DC coefficients
            if (sBlock.startsWith("Y")) // if luminance
                oDCChrominanceOrLuminance.Top6Bits = 
                                        (int)(m_lngQuantizationScaleLumin & 63);
            else
                oDCChrominanceOrLuminance.Top6Bits = 
                                        (int)(m_lngQuantizationScaleChrom & 63);

        }
        
        if (DebugVerbose >= 7)
            System.err.println(String.format(
                    "%1.3f: %s -> %s DC coefficient %d",
                    m_oBitReader.getPosition(),
                    oDCChrominanceOrLuminance.VariableLengthCodeBits,
                    sBlock,
                    oDCChrominanceOrLuminance.Bottom10Bits));
        
        // Set this block's DC Coefficient to the MDEC code
        oThisBlk.DCCoefficient = oDCChrominanceOrLuminance;
    }
    
    
    /** Decodes the DC Coefficient at the start of every block (for v.3) and
     *  adds the resulting MDEC code to the queue.
     *  DC coefficients are stored differently for Chrominance and
     *  Luminance in version 3 frames. The arguments to this function
     *  take care of all the differences. */
    protected int DecodeV3_DC_ChrominanceOrLuminance(
                         int iPreviousDC, 
                         int iLongestVariableLengthCode, 
                         DCVariableLengthCode DCVariableLengthCodeTable[],
                         String sBlockName, Block oThisBlk) 
        throws IOException, UncompressionException 
    {
        MDEC.Mdec16Bits oDCChrominanceOrLuminance = 
                new MDEC.Mdec16Bits();
        
        // Save the current file position
        oDCChrominanceOrLuminance.OriginalFilePos = m_oBitReader.getPosition();
        
        // Peek enough bits
        String sBits = 
                m_oBitReader.PeekBitsToString(iLongestVariableLengthCode);
        boolean blnFoundCode = false;

        // Search though all the DC Coefficient codes for a match
        for (DCVariableLengthCode tDcVlc : DCVariableLengthCodeTable) {

            if (sBits.startsWith(tDcVlc.VariableLengthCode)) { // match?

                // Save the matching code, then skip past those bits
                oDCChrominanceOrLuminance.VariableLengthCodeBits = 
                        tDcVlc.VariableLengthCode;
                m_oBitReader.SkipBits(tDcVlc.VariableLengthCode.length());

                if (tDcVlc.DC_Length == 0) {

                    oDCChrominanceOrLuminance.Bottom10Bits = 0;

                } else {

                    // Save the additional bits
                    oDCChrominanceOrLuminance.VariableLengthCodeBits += 
                            m_oBitReader.PeekBitsToString(tDcVlc.DC_Length);
                    
                    // Read the DC differential
                    int iDC_Differential = 
                           (int)m_oBitReader.ReadUnsignedBits(tDcVlc.DC_Length);
                    // Lookup its value
                    oDCChrominanceOrLuminance.Bottom10Bits = 
                            tDcVlc.DC_DifferentialLookup[iDC_Differential];
                    
                    // !!! ???We must multiply it by 4 for no reason??? !!!
                    oDCChrominanceOrLuminance.Bottom10Bits *= 4; 
                }
                
                // Now adjust the DC Coefficent with the previous coefficient
                oDCChrominanceOrLuminance.Bottom10Bits += iPreviousDC;
                blnFoundCode = true; // we found the code
                break; // we're done
            }
        } 
        if (!blnFoundCode) 
            throw new UncompressionException("Error decoding macro block:" +
                                  " Unknown DC " + sBlockName + 
                                  " variable length code " + sBits);
        
        // The bottom 10 bits now hold the DC Coefficient for the block,
        // now squeeze the frame's quantization scale into the top 6 bits
        oDCChrominanceOrLuminance.Top6Bits = 
                (int)(m_lngQuantizationScaleChrom & 63);
        
        if (DebugVerbose >= 7)
            System.err.println(String.format(
                    "%d: %s -> %s DC coefficient %d",
                    m_oBitReader.getPosition(), 
                    oDCChrominanceOrLuminance.VariableLengthCodeBits,
                    sBlockName,
                    oDCChrominanceOrLuminance.Bottom10Bits));
        
        // Set this block's DC Coefficient
        oThisBlk.DCCoefficient = oDCChrominanceOrLuminance;
        
        // return the new DC Coefficient
        return oDCChrominanceOrLuminance.Bottom10Bits;
    }
    
    
    /** Decodes all the block's AC Coefficients in the stream, 
     * and adds the resulting MDEC codes to the queue.
     * AC Coefficients are decoded the same for version 2 and 3 frames */
    protected long Decode_AC_Coefficients(Block oThisBlk) 
            throws IOException, UncompressionException 
    {
        ArrayList<MDEC.Mdec16Bits> oACCoefficients =
                new ArrayList<MDEC.Mdec16Bits>();
        MDEC.Mdec16Bits oMdecCode;
        int iTotalRunLength = 0;
        long lngNumberOfRunLengthCodes = 0;
        double dblFilePos;
        
        while (true) {
            // First save the current file position
            dblFilePos = m_oBitReader.getPosition();
            
            // Decode the next bunch of bits into an MDEC run length code
            oMdecCode = Decode_AC_Code();
            // Add the saved file position
            oMdecCode.OriginalFilePos = dblFilePos;
            
            // Just one more variable-length-code read
            lngNumberOfRunLengthCodes++;
            
            // debug
            if (DebugVerbose >= 7)
                System.err.print(String.format(
                        "%1.3f: %s -> ",
                        m_oBitReader.getPosition(),
                        oMdecCode.VariableLengthCodeBits));
            
            // Did we hit the end of the block?
            if (oMdecCode.toMdecWord() == MDEC.MDEC_END_OF_BLOCK) {
                if (DebugVerbose >= 7)
                    System.err.println("EOB");
                
                oThisBlk.EndOfBlock = oMdecCode;
                
                // Then we're done here
                break;
            } else {
                if (DebugVerbose >= 7)
                    System.err.println(oMdecCode.toString());

                oACCoefficients.add(oMdecCode);
                
            }
            
            // Add this run length code to the total
            iTotalRunLength += oMdecCode.Top6Bits + 1;
            
            // Hopefully we haven't gone over
            if (iTotalRunLength > 63) {
                throw new UncompressionException(String.format(
                        "Error decoding macro block: " +
                        "Run length out of bounds: %d at %1.3f",
                        iTotalRunLength + 1,
                        m_oBitReader.getPosition()
                        ));
            }
        } 
        
        oThisBlk.ACCoefficients = oACCoefficients.toArray(new MDEC.Mdec16Bits[0]);
        
        return lngNumberOfRunLengthCodes;
    }
    
    
    /** Decodes the next AC Coefficient bits in the stream and returns the
     *  resulting MDEC code. */
    protected MDEC.Mdec16Bits Decode_AC_Code() 
            throws IOException, UncompressionException 
    {
        
        // Peek at the upcoming bits
        String sBits = 
                m_oBitReader.PeekBitsToString(AC_LONGEST_VARIABLE_LENGTH_CODE);
        
        if (sBits.startsWith(AC_ESCAPE_CODE)) { // Is it the escape code?
            
            return Decode_AC_EscapeCode();
            
        } else if (sBits.startsWith(VLC_END_OF_BLOCK)) { // end of block?
            
            m_oBitReader.SkipBits(VLC_END_OF_BLOCK.length());
            MDEC.Mdec16Bits tRlc = 
                    new MDEC.Mdec16Bits(MDEC.MDEC_END_OF_BLOCK);
            tRlc.VariableLengthCodeBits = VLC_END_OF_BLOCK;
            return tRlc;
            
        } else { // must be a normal code
            
            return Decode_AC_VariableLengthCode(sBits);
            
        }
            
    }

    /** Decodes the AC Escape Code bits from the stream and returns the
     *  resulting MDEC code. */
    protected MDEC.Mdec16Bits Decode_AC_EscapeCode() 
            throws IOException, UncompressionException 
    {
        MDEC.Mdec16Bits tRlc = new MDEC.Mdec16Bits();
        
        // Save the escape code bits, then skip them in the stream
        tRlc.VariableLengthCodeBits = AC_ESCAPE_CODE;
        m_oBitReader.SkipBits(AC_ESCAPE_CODE.length());
        
        // Read 6 bits for the run of zeros
        tRlc.VariableLengthCodeBits += 
                m_oBitReader.PeekBitsToString(6);
        tRlc.Top6Bits = (int)m_oBitReader.ReadUnsignedBits(6);

        if (m_iFrameType != FRAME_LAIN && m_iFrameType != FRAME_LAIN_FINAL_MOVIE)
        {
            // Normal playstation encoding stores the escape code in 16 bits:
            // 6 for run of zeros (already read), 10 for AC Coefficient
            
            // Read the 10 bits of AC Coefficient
            tRlc.VariableLengthCodeBits += 
                    m_oBitReader.PeekBitsToString(10);
            tRlc.Bottom10Bits = (int)m_oBitReader.ReadSignedBits(10);

            // Did we end up with an AC coefficient of zero?
            if (tRlc.Bottom10Bits == 0) {
                // Normally this is concidered an error
                // but FF7 has these pointless codes. So we'll only allow it
                // if this is FF7
                if (m_iFrameType != FRAME_FF7 && m_iFrameType != FRAME_FF7_WITHOUT_CAMERA) 
                    // If not FF7, throw an error
                    throw new UncompressionException(
                            "Error decoding macro block: " +
                            "AC Escape code: Run length is zero at " 
                            + m_oBitReader.getPosition());
            }
            
        } else { // Lain
            
            /* Lain playstation uses mpeg1 specification escape code
            Fixed Length Code       Level 
            forbidden               -256  
            1000 0000 0000 0001     -255  
            1000 0000 0000 0010     -254  
            ...                          
            1000 0000 0111 1111     -129  
            1000 0000 1000 0000     -128  
            1000 0001               -127  
            1000 0010               -126  
            ...                           
            1111 1110               -2    
            1111 1111               -1    
            forbidden                0    
            0000 0001                1    
            0000 0010                2    
            ...   
            0111 1110               126   
            0111 1111               127   
            0000 0000 1000 0000     128   
            0000 0000 1000 0001     129   
            ...   
            0000 0000 1111 1110     254   
            0000 0000 1111 1111     255   
             */
            // Peek at the first 8 bits
            String sBits = m_oBitReader.PeekBitsToString(8);
            tRlc.VariableLengthCodeBits += sBits;
            if (sBits.equals("00000000")) {
                // If it's the special 00000000
                // Positive
                m_oBitReader.SkipBits(8);
                tRlc.VariableLengthCodeBits += m_oBitReader.PeekBitsToString(8);
                tRlc.Bottom10Bits = (int)m_oBitReader.ReadUnsignedBits(8);
                
            } else if (sBits.equals("10000000")) {
                // If it's the special 10000000
                // Negitive
                m_oBitReader.SkipBits(8);
                tRlc.VariableLengthCodeBits += m_oBitReader.PeekBitsToString(8);
                tRlc.Bottom10Bits = -256 + (int)m_oBitReader.ReadUnsignedBits(8);
                
            } else {
                // Otherwise we already have the value
                tRlc.Bottom10Bits = (int)m_oBitReader.ReadSignedBits(8);
            }
            
        }
        
        return tRlc;
    }
    
    /** Decodes sBits into an AC Coefficient and skips the bits in the 
     *  stream.
     * @param sBits  A string of 16 bits if possible.
     */
    protected MDEC.Mdec16Bits Decode_AC_VariableLengthCode(String sBits) 
        throws IOException, UncompressionException 
    {
        MDEC.Mdec16Bits tRlc = new MDEC.Mdec16Bits();
        boolean blnFoundCode = false;
        ACVariableLengthCode tVarLenCodes[];
        
        // Use the correct AC variable length code list
        if (m_iFrameType != FRAME_LAIN && m_iFrameType != FRAME_LAIN_FINAL_MOVIE) 
        {
            tVarLenCodes = AC_VARIABLE_LENGTH_CODES_MPEG1;
        } else {
            tVarLenCodes = AC_VARIABLE_LENGTH_CODES_LAIN;
        }
        
        // Search through the list to find the matching AC variable length code
        for (ACVariableLengthCode vlc : tVarLenCodes) {
            if (sBits.startsWith(vlc.VariableLengthCode)) {
                
                // Yay we found it!
                // Skip that many bits
                m_oBitReader.SkipBits(vlc.VariableLengthCode.length());
                
                // Save the resulting code, and run of zeros
                tRlc.VariableLengthCodeBits = vlc.VariableLengthCode;
                tRlc.Top6Bits = vlc.RunOfZeros;
                // Take either the positive or negitive AC coefficient,
                // depending on the sign bit
                if (m_oBitReader.ReadUnsignedBits(1) == 1) {
                    // negitive
                    tRlc.Bottom10Bits = -vlc.AbsoluteLevel;
                    tRlc.VariableLengthCodeBits += "1";
                } else {
                    // positive
                    tRlc.Bottom10Bits = vlc.AbsoluteLevel;
                    tRlc.VariableLengthCodeBits += "0";
                }
                
                blnFoundCode = true;
                break;
            }
        }
        
        if (! blnFoundCode) {
            if (sBits.length() < 16)
                throw new UncompressionException(
                        "Error decoding macro block: " +
                        "End bit stream cut off AC variable length code: " +
                         sBits + " at " + String.format("%1.3f", m_oBitReader.getPosition()));
            else
                throw new UncompressionException(
                        "Error decoding macro block: " +
                        "Unmatched AC variable length code: " +
                         sBits + " at " + String.format("%1.3f", m_oBitReader.getPosition()));
        }
        
        return tRlc;
    }

}
