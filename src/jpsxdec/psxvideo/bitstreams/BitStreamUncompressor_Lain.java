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

package jpsxdec.psxvideo.bitstreams;

import jpsxdec.psxvideo.mdec.DecodingException;
import java.io.IOException;
import jpsxdec.psxvideo.encode.BitStreamWriter;
import jpsxdec.util.NotThisTypeException;
import java.io.EOFException;
import java.util.logging.Logger;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;

/** A fairly optimized, full Java implementation of the PSX video decoding
 *  process. 
 * <p>
 * WARNING: The public methods are NOT thread safe. Create a separate
 * instance of this class for each thread, or wrap the calls with syncronize. */
public class BitStreamUncompressor_Lain extends BitStreamUncompressor_STRv2 {

    private static final Logger log = Logger.getLogger(BitStreamUncompressor_Lain.class.getName());
    protected Logger getLog() { return log; }

    /** The custom Serial Experiments Lain Playstation game
     *  variable-length-code table. */
    private final static ACVariableLengthCode AC_VARIABLE_LENGTH_CODES_LAIN[] =
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

    // frame header info
    private int _iQscaleChrom;
    private int _iQscaleLumin;
    private int _iVlcCount;

    public BitStreamUncompressor_Lain() {
        super();
        _aoVarLenCodes = AC_VARIABLE_LENGTH_CODES_LAIN;
    }

    @Override
    protected void readHeader(byte[] abFrameData, int iStart, ArrayBitReader bitReader) throws NotThisTypeException {
        _iQscaleLumin            = abFrameData[iStart+0];
        _iQscaleChrom            = abFrameData[iStart+1];
        _iMagic3800              = IO.readUInt16LE(abFrameData, iStart+2);
        _iVlcCount               = IO.readSInt16LE(abFrameData, iStart+4);
        int iVersion             = IO.readSInt16LE(abFrameData, iStart+6);

        if (_iQscaleChrom < 1 || _iQscaleLumin < 1 ||
                 iVersion != 0 || _iVlcCount < 1)
            throw new NotThisTypeException();

        // final Lain movie doesn't have 0x3800
        if (_iMagic3800 != 0x3800 && (_iMagic3800 < 0 || _iMagic3800 > 4765))
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, false, iStart+8);
    }
    
    public static boolean checkHeader(byte[] abFrameData) {
        int iQscaleLumin            = abFrameData[0];
        int iQscaleChrom            = abFrameData[1];
        int iMagic3800              = IO.readUInt16LE(abFrameData, 2);
        int iVlcCount               = IO.readSInt16LE(abFrameData, 4);
        int iVersion                = IO.readSInt16LE(abFrameData, 6);

        if (iQscaleChrom < 1 || iQscaleLumin < 1 ||
               iVersion != 0 || iVlcCount < 1)
            return false;

        // final Lain movie doesn't have 0x3800
        if (iMagic3800 != 0x3800 && (iMagic3800 < 0 || iMagic3800 > 4765))
            return false;

        return true;
    }

    @Override
    public int getStreamPosition() {
        return _bitReader.getPosition();
    }

    @Override
    public int getLuminQscale() {
        return _iQscaleLumin;
    }

    @Override
    public int getChromQscale() {
        return _iQscaleChrom;
    }

    @Override
    protected void readQscaleDC(MdecCode oCode) throws EOFException, DecodingException {
        oCode.setBottom10Bits( (int)_bitReader.readSignedBits(10) );
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(oCode.getBottom10Bits(), 10));
        if (_iCurrentBlock < 2)
            oCode.setTop6Bits(_iQscaleChrom);
        else
            oCode.setTop6Bits(_iQscaleLumin);
    }


    
    @Override
    protected void decode_AC_EscapeCode(long lngBits, MdecCode code)
            throws DecodingException, EOFException
    {
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(AC_ESCAPE_CODE);
        
        // Get the (6 bit) run of zeros from the bits already read
        // 17 bits: eeeeeezzzzzz_____ : e = escape code, z = run of zeros
        code.setTop6Bits( (int)( (lngBits >>> (17 - 12)) & 63 ) );
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(code.getTop6Bits(), 6));

        // Skip the escape code (6 bits) and the run of zeros (6 bits)
        _bitReader.skipBits( 12 );
        
        // Lain
            
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
        // Read the first 8 bits
        lngBits = _bitReader.readSignedBits(8);
        int iACCoefficient;
        if (lngBits == 0x00) {
            // If it's the special 00000000
            // Positive
            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append("00000000");

            iACCoefficient = (int)_bitReader.readUnsignedBits(8);

            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(iACCoefficient, 8));

            code.setBottom10Bits(iACCoefficient);
        } else if (lngBits  == -0x80) {
            // If it's the special 10000000
            // Negitive
            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append("10000000");

            iACCoefficient = -256 + (int)_bitReader.readUnsignedBits(8);

            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(iACCoefficient, 8));

            code.setBottom10Bits(iACCoefficient);
        } else {
            // Otherwise we already have the value
            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(lngBits, 8));

            iACCoefficient = (int)lngBits;
            
            code.setBottom10Bits(iACCoefficient);
        }

    }
    
    @Override
    public String toString() {
        return "Lain";
    }

    @Override
    public BitStreamCompressor makeCompressor() {
        return new BitstreamCompressor_Lain();
    }

    public static class BitstreamCompressor_Lain extends BitstreamCompressor_STRv2 {

        @Override
        protected void writeHeader(BitStreamWriter bitStream, int iLuminQscale, int iChromScale, int iMdecCodeCount) throws IOException {
            bitStream.setLittleEndian(false);
            bitStream.write(iLuminQscale, 8);
            bitStream.write(iChromScale, 8);
            bitStream.setLittleEndian(true);
            // If this is the final movie, this should actually be the frame number.
            // This value will be corrected if the sector data is replaced using
            // SectorLainVideo class.
            bitStream.write(0x3800, 16); 
            bitStream.write(iMdecCodeCount, 16);
            bitStream.write(getHeaderVersion(), 16);
            bitStream.setLittleEndian(false);
        }

        @Override
        protected int getHeaderVersion() {
            return 0;
        }

        @Override
        public boolean separateQscales() {
            return true;
        }

        @Override
        protected ACVariableLengthCode[] getAcVaribleLengthCodeList() {
            return AC_VARIABLE_LENGTH_CODES_LAIN;
        }

        @Override
        protected String encodeACescape(MdecCode code) {
            String sTopBits = Misc.bitsToString(code.getTop6Bits(), 6);
            if (code.getBottom10Bits() == 0)
                throw new IllegalArgumentException("Invalid MDEC code to escape " + code.toString());
            if (code.getBottom10Bits() < -256 || code.getBottom10Bits() > 255)
                throw new IllegalArgumentException("Unable to escape, AC code too large for Lain " + code.toString());
            if (code.getBottom10Bits() >= -127 && code.getBottom10Bits() <= 127) {
                return AC_ESCAPE_CODE + sTopBits + Misc.bitsToString(code.getBottom10Bits(), 8);
            } else {
                if (code.getBottom10Bits() > 0) {
                    return AC_ESCAPE_CODE + sTopBits + "00000000" + Misc.bitsToString(code.getBottom10Bits(), 8);
                } else {
                    return AC_ESCAPE_CODE + sTopBits + "10000000" + Misc.bitsToString(code.getBottom10Bits()+256, 8);
                }
            }
        }

        @Override
        protected void addTrailingBits(BitStreamWriter bitStream) throws IOException {
            // do nothing
        }

    }


    public static void main(String[] args) throws IOException, NotThisTypeException, DecodingException {
        BitStreamUncompressor_Lain uncompressor = new BitStreamUncompressor_Lain();
        byte[] abBitstream = IO.readFile("F001[0]_320x240[3].demux");
        uncompressor.reset(abBitstream);
        ParsedMdecImage parsed = new ParsedMdecImage(320, 240);
        parsed.readFrom(uncompressor);
        byte[] abCompressed = uncompressor.makeCompressor().compress(parsed.getStream(), parsed.getLuminQscale(), parsed.getChromQscale(), parsed.getMdecCodeCount());
        IO.writeFile("F001[0]_320x240[3]-dup.demux", abCompressed);
        for (int i = 0; i < abCompressed.length; i++) {
            if (abBitstream[i] != abCompressed[i]) {
                System.out.format("@%d  %02x != %02x", i, abBitstream[i], abCompressed[i]);
                System.out.println();
            }
        }
    }
}
