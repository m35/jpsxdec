/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.modules.psx.video.bitstreams;

import jpsxdec.modules.psx.video.mdec.DecodingException;
import java.io.ByteArrayOutputStream;
import jpsxdec.modules.psx.video.mdec.MdecInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.util.logging.Logger;
import jpsxdec.util.Misc;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.modules.psx.video.encode.BitStreamWriter;
import jpsxdec.modules.psx.video.encode.ParsedMdecImage;

/** Uncompressor for demuxed STR v2 video data. */
public class BitStreamUncompressor_STRv2 extends BitStreamUncompressor {

    private static final Logger log = Logger.getLogger(BitStreamUncompressor_STRv2.class.getName());
    protected Logger getLog() { return log; }

    /* ---------------------------------------------------------------------- */
    /* AC Variable length code stuff ---------------------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Sequence of bits indicating an escape code. */
    protected final static String AC_ESCAPE_CODE = "000001";

    /** Sequence of bits indicating the end of a block.
     * Unlike the MPEG1 specification, these bits can, and often do appear as
     * the first and only variable-length-code in a block. */
    private final static String VLC_END_OF_BLOCK = "10"; // bits 10

    /** The longest of all the AC variable-length-codes is 16 bits,
     *  not including the sign bit. */
    public final static int AC_LONGEST_VARIABLE_LENGTH_CODE = 16;

    /** 11 bits found at the end of STR v2 and v3 movies.
     * <pre>011 111 111 10</pre> */
    private final static String END_OF_FRAME_EXTRA_BITS = "01111111110";


    /** Represents an AC variable length code. */
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

    /** STR v2 varible-length codes. The order of these codes are important
     * because the uncompressor needs similar codes to be indexed. */
    private final static ACVariableLengthCode[] AC_VARIABLE_LENGTH_CODES_MPEG1 =
    {
    /*  new ACVariableLengthCode("1"                 , 0 , 1  ),
          The MPEG1 specification declares that if the first
          variable-length-code in a block is "1" that it should be translated
          to the run-length-code (0, 1). The PSX variable-length-code
          decoding does not follow this rule. */

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

    /* ---------------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    public static class MdecDebugger {
        public long Position;
        public StringBuilder Bits = new StringBuilder();
        public void print(MdecCode code) {
            System.out.format("@%d %s -> %s", Position, Bits, code);
            System.out.println();
            Bits.setLength(0);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */

    protected ACVariableLengthCode _aoVarLenCodes[];

    /* ---------------------------------------------------------------------- */

    // Frame header info
    protected int _iQscale;
    protected int _iMagic3800;
    protected int _iHalfVlcCountCeil32;

    protected ArrayBitReader _bitReader = new ArrayBitReader();

    protected MdecDebugger _debug;

    // current read state
    protected int _iCurrentMacroBlock;
    protected int _iCurrentBlock;
    protected boolean _blnStartOfBlock;
    protected int _iCurrentBlockVectorPos;

    public BitStreamUncompressor_STRv2() {
        _aoVarLenCodes = AC_VARIABLE_LENGTH_CODES_MPEG1;
        if (DEBUG_UNCOMPRESSOR) _debug = new MdecDebugger();
    }

    /* ---------------------------------------------------------------------- */

    public int getHalfVlcCountCeil32() {
        return _iHalfVlcCountCeil32;
    }

    public long getMagic3800() {
        return _iMagic3800;
    }

    public int getBlock() {
        return _iCurrentBlock;
    }

    public int getMacroBlock() {
        return _iCurrentMacroBlock;
    }

    @Override
    public int getStreamPosition() {
        return (_bitReader.getPosition() + 1) & ~1;
    }

    @Override
    public int getLuminQscale() {
        return _iQscale;
    }

    @Override
    public int getChromQscale() {
        return _iQscale;
    }

    /* ---------------------------------------------------------------------- */

    @Override
    public void reset(byte[] abDemuxData) throws NotThisTypeException {
        reset(abDemuxData, 0);
    }

    @Override
    public void reset(byte[] abDemuxData, int iStart) throws NotThisTypeException
    {
        readHeader(abDemuxData, iStart, _bitReader);
        _iCurrentMacroBlock = 0;
        _iCurrentBlock = 0;
        _blnStartOfBlock = true;
        _iCurrentBlockVectorPos = 0;
    }

    protected void readHeader(byte[] abFrameData, int iStart, ArrayBitReader bitReader) throws NotThisTypeException {
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, iStart+0);
        _iMagic3800          = IO.readUInt16LE(abFrameData, iStart+2);
        _iQscale             = IO.readSInt16LE(abFrameData, iStart+4);
        int iVersion         = IO.readSInt16LE(abFrameData, iStart+6);

        if (_iMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 2  || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, true, iStart+8);
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800          = IO.readUInt16LE(abFrameData, 2);
        int iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion            = IO.readSInt16LE(abFrameData, 6);

        return iMagic3800 == 0x3800 &&
               iQscale >= 1 &&
               iVersion == 2 &&
               iHalfVlcCountCeil32 >= 0;
    }

    @Override
    public boolean readMdecCode(MdecCode code) throws DecodingException, EOFException
    {
        if (DEBUG_UNCOMPRESSOR) _debug.Position = _bitReader.getPosition();
        
        if (_blnStartOfBlock) { // read Q-scale and DC if start of block
            readQscaleDC(code);
            _blnStartOfBlock = false;
        } else { // read AC otherwise
            if (decode_AC_VariableLengthCode(code)) {
                // end of block
                _iCurrentBlock++;
                if (_iCurrentBlock >= 6) {
                    _iCurrentMacroBlock++;
                    _iCurrentBlock = 0;
                }
                _blnStartOfBlock = true;
                _iCurrentBlockVectorPos = 1;
            } else {
                // block continues
                _iCurrentBlockVectorPos += code.getTop6Bits() + 1;
                if (_iCurrentBlockVectorPos > 64) {
                    throw new DecodingException(
                            "Run length out of bounds: " + _iCurrentBlockVectorPos);
                }
            }
        }
        
        if (DEBUG_UNCOMPRESSOR) _debug.print(code);

        return _blnStartOfBlock;
    }

    protected void readQscaleDC(MdecCode code) throws EOFException, DecodingException {
        code.setTop6Bits(_iQscale);
        code.setBottom10Bits((int)_bitReader.readSignedBits(10));
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(code.getBottom10Bits(), 10));
        assert !code.isEOD(); // a Qscale of 63 and DC of -512 would look like EOD
    }


    private static final int b1000000000000000_ = 0x8000 << 1;
    private static final int b0100000000000000_ = 0x4000 << 1;
    private static final int b0010000000000000_ = 0x2000 << 1;
    private static final int b0001100000000000_ = 0x1800 << 1;
    private static final int b0001000000000000_ = 0x1000 << 1;
    private static final int b0000100000000000_ = 0x0800 << 1;
    private static final int b0000010000000000_ = 0x0400 << 1;
    private static final int b0000001000000000_ = 0x0200 << 1;
    private static final int b0000000100000000_ = 0x0100 << 1;
    private static final int b0000000010000000_ = 0x0080 << 1;
    private static final int b0000000001000000_ = 0x0040 << 1;
    private static final int b0000000000100000_ = 0x0020 << 1;
    private static final int b0000000000010000_ = 0x0010 << 1;
    protected boolean decode_AC_VariableLengthCode(MdecCode code)
            throws DecodingException, EOFException
    {
        final ACVariableLengthCode vlc;
        
        // peek enough bits for the longest variable length code (16 bits)
        // plus 1 for the sign bit.
        long lngBits = _bitReader.peekUnsignedBits( AC_LONGEST_VARIABLE_LENGTH_CODE + 1 );
        
        // Walk through the bits, one-by-one
        // Fun fact: The Lain Playstation game uses this same decoding approach
        if (    (lngBits & b1000000000000000_) != 0) {        // "1"
            if ((lngBits & b0100000000000000_) != 0) {        // "11"
                vlc = _aoVarLenCodes[0];
            } else {                                          // "10"
                // End of block
                _bitReader.skipBits(2);
                code.setToEndOfData();
                if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(VLC_END_OF_BLOCK);
                return true;
            }
        } else if ((lngBits & b0100000000000000_) != 0) {     // "01"
            if    ((lngBits & b0010000000000000_) != 0) {     // "011"
                vlc = _aoVarLenCodes[1];
            } else {                                          // "010x"
                vlc = _aoVarLenCodes[2 + (int)((lngBits >>> 13) & 1)];
            }
        } else if ((lngBits & b0010000000000000_) != 0) {      // "001"
            if    ((lngBits & b0001100000000000_) != 0)  {     // "001xx"
                vlc = _aoVarLenCodes[3 + (int)((lngBits >>> 12) & 3)];
            } else {                                           // "00100xxx"
                vlc = _aoVarLenCodes[15 + (int)((lngBits >>> 9) & 7)];
            }
        } else if ((lngBits & b0001000000000000_) != 0) {      // "0001xx"
            vlc = _aoVarLenCodes[7 + (int)((lngBits >>> 11) & 3)];
        } else if ((lngBits & b0000100000000000_) != 0) {      // "00001xx"
            vlc = _aoVarLenCodes[11 + (int)((lngBits >>> 10) & 3)];
        } else if ((lngBits & b0000010000000000_) != 0) {      // "000001"
            // escape code
            decode_AC_EscapeCode(lngBits, code);
            return false;
        } else if ((lngBits & b0000001000000000_) != 0) {      // "0000001xxx"
            vlc = _aoVarLenCodes[23 + (int)((lngBits >>> 7) & 7)];
        } else if ((lngBits & b0000000100000000_) != 0) {      // "00000001xxxx"
            vlc = _aoVarLenCodes[31 + (int)((lngBits >>> 5) & 15)];
        } else if ((lngBits & b0000000010000000_) != 0) {      // "000000001xxxx"
            vlc = _aoVarLenCodes[47 + (int)((lngBits >>> 4) & 15)];
        } else if ((lngBits & b0000000001000000_) != 0) {      // "0000000001xxxx"
            vlc = _aoVarLenCodes[63 + (int)((lngBits >>> 3) & 15)];
        } else if ((lngBits & b0000000000100000_) != 0) {      // "00000000001xxxx"
            vlc = _aoVarLenCodes[79 + (int)((lngBits >>> 2) & 15)];
        } else if ((lngBits & b0000000000010000_) != 0) {      // "000000000001xxxx"
            vlc = _aoVarLenCodes[95 + (int)((lngBits >>> 1) & 15)];
        } else {
            throw new DecodingException(
                    "Unmatched AC variable length code: " +
                     Misc.bitsToString(lngBits, AC_LONGEST_VARIABLE_LENGTH_CODE + 1));
        }
        
        code.setTop6Bits(vlc.RunOfZeros);

        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(vlc.VariableLengthCode);

        // Take either the positive or negitive AC coefficient,
        // depending on the sign bit
        if ((lngBits & (1 << (16 - vlc.VariableLengthCode.length()))) == 0) {
            // positive
            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append("0");
            code.setBottom10Bits(vlc.AbsoluteLevel);
        } else {
            // negative
            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append("1");
            code.setBottom10Bits(-vlc.AbsoluteLevel);
        }

        // Skip that many bits
        _bitReader.skipBits(vlc.VariableLengthCode.length() + 1);
        
        return false;
    }
    
    protected void decode_AC_EscapeCode(long lngBits, MdecCode code)
            throws DecodingException, EOFException
    {
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(AC_ESCAPE_CODE);

        // Get the (6 bit) run of zeros from the bits already read
        // 17 bits: eeeeeezzzzzz_____ : e = escape code, z = run of zeros
        code.setTop6Bits((int)( (lngBits >>> (17 - 12)) & 63 ));
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(code.getTop6Bits(), 6));

        // Skip the escape code (6 bits) and the run of zeros (6 bits)
        _bitReader.skipBits( 12 );

        // Normal playstation encoding stores the escape code in 16 bits:
        // 6 for run of zeros (already read), 10 for AC Coefficient

        // Read the 10 bits of AC Coefficient
        code.setBottom10Bits((int)_bitReader.readSignedBits(10));
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(code.getBottom10Bits(), 10));

        // Ignore zero AC coefficients.
        // (I consider this an error, but FF7 has these codes,
        // so clearly the MDEC can handle it.)

        if (code.getBottom10Bits() == 0) {
            getLog().info("Escape code has 0 AC coefficient.");
        }

    }

    @Override
    public String toString() {
        return "STRv2";
    }

    @Override
    public BitStreamCompressor makeCompressor() {
        return new BitstreamCompressor_STRv2();
    }

    /*########################################################################*/
    /*########################################################################*/
    /*########################################################################*/

    public static class BitstreamCompressor_STRv2 implements BitStreamCompressor {

        public byte[] compress(MdecInputStream inStream, int iLuminQscale, int iChromQscale, int iMdecCodeCount)
                throws DecodingException, IOException
        {
            if (iLuminQscale < 1 || iLuminQscale > 63)
                throw new IllegalArgumentException("Invalid Luminance quantization scale " + iLuminQscale);
            if (iChromQscale < 1 || iChromQscale > 63)
                throw new IllegalArgumentException("Invalid Chrominance quantization scale " + iChromQscale);
            if (iMdecCodeCount < 0)
                throw new IllegalArgumentException("Invalid MDEC code count " + iMdecCodeCount);
            if (!separateQscales() && iLuminQscale != iChromQscale)
                throw new IllegalArgumentException("Quantization scales should be the same: " +
                                                   iLuminQscale + " != " + iChromQscale);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BitStreamWriter bitStream = new BitStreamWriter(baos);
            
            bitStream.setLittleEndian(true);
            writeHeader(bitStream, iLuminQscale, iChromQscale, iMdecCodeCount);

            MdecCode oCode = new MdecCode();

            boolean newBlk = true;
            int iBlock = 0;
            for (int i = 0; i < iMdecCodeCount; i++) {
                String sBitsToWrite;
                if (inStream.readMdecCode(oCode)) {
                    sBitsToWrite = VLC_END_OF_BLOCK;
                    newBlk = true;
                    iBlock = (iBlock + 1) % 6;
                } else {
                    if (newBlk) {
                        if (iBlock < 2) {
                            if (oCode.getTop6Bits() != iChromQscale) {
                                throw new IllegalArgumentException(String.format("Qscale given %d, should be %d in MDEC code %s #%d",
                                        oCode.getTop6Bits(), iChromQscale, oCode, i));
                            }
                        } else {
                            if (oCode.getTop6Bits() != iLuminQscale) {
                                throw new IllegalArgumentException(String.format("Qscale given %d, should be %d in MDEC code %s #%d",
                                        oCode.getTop6Bits(), iLuminQscale, oCode, i));
                            }
                        }
                        sBitsToWrite = encodeDC(oCode.getBottom10Bits(), iBlock);
                        newBlk = false;
                    } else {
                        sBitsToWrite = encodeAC(oCode);
                    }
                }
                if (DEBUG_COMPRESSOR)
                    System.out.println("Converting " + oCode.toString() + " to " + sBitsToWrite + " at " + baos.size());
                bitStream.write(sBitsToWrite);
            }

            if (iBlock != 0)
                throw new IllegalStateException("Ended compressing in the middle of a macroblock.");

            addTrailingBits(bitStream);
            bitStream.close();
            return baos.toByteArray();
        }

        protected void addTrailingBits(BitStreamWriter bitStream) throws IOException {
            bitStream.write(END_OF_FRAME_EXTRA_BITS);
        }

        protected String encodeDC(int iDC, int iBlock) {
            if (iDC < -1024 || iDC > 1023)
                throw new IllegalArgumentException("Invalid DC code " + iDC);
            if (iBlock < 0 || iBlock > 5)
                throw new IllegalArgumentException("Invalid block " + iBlock);

            return Misc.bitsToString(iDC, 10);
        }

        protected String encodeAC(MdecCode code) {
            if (code.getTop6Bits() < 0 || code.getTop6Bits() > 63)
                throw new IllegalArgumentException("Invalid AC zero run length " + code.getTop6Bits());
            if (code.getBottom10Bits() < -1024 || code.getBottom10Bits() > 1023)
                throw new IllegalArgumentException("Invalid AC code " + code.getBottom10Bits());

            for (ACVariableLengthCode oVlc : getAcVaribleLengthCodeList()) {
                if (code.getTop6Bits() == oVlc.RunOfZeros && Math.abs(code.getBottom10Bits()) == oVlc.AbsoluteLevel) {
                    return oVlc.VariableLengthCode + ((code.getBottom10Bits() < 0) ? "1" : "0");
                }
            }
            // not a pre-defined code
            return encodeACescape(code);
        }

        protected ACVariableLengthCode[] getAcVaribleLengthCodeList() {
            return AC_VARIABLE_LENGTH_CODES_MPEG1;
        }

        protected String encodeACescape(MdecCode code) {
            if (code.getTop6Bits() < 0 || code.getTop6Bits() > 63)
                throw new IllegalArgumentException("Invalid AC zero run length " + code.getTop6Bits());
            if (code.getBottom10Bits() < -1024 || code.getBottom10Bits() > 1023)
                throw new IllegalArgumentException("Invalid AC code " + code.getBottom10Bits());
            
            return AC_ESCAPE_CODE +
                    Misc.bitsToString(code.getTop6Bits(), 6) +
                    Misc.bitsToString(code.getBottom10Bits(), 10);
        }

        protected int getHeaderVersion() { return 2; }

        protected void writeHeader(BitStreamWriter bitStream, int iLuminQscale, int iChromScale, int iMdecCodeCount) throws IOException {
            if (iLuminQscale != iChromScale)
                throw new IllegalArgumentException("Quantization scales must be equal " +
                        iLuminQscale + " != " + iChromScale);
            if (iLuminQscale < 0)
                throw new IllegalArgumentException("Invalid Q scale " + iLuminQscale);

            bitStream.setLittleEndian(true);
            bitStream.write( ParsedMdecImage.calculateHalfCeiling32(iMdecCodeCount) , 16);
            bitStream.write( 0x3800, 16);
            bitStream.write( iLuminQscale, 16);
            bitStream.write( getHeaderVersion(), 16);
        }

        public boolean separateQscales() {
            return false;
        }
    }

}
