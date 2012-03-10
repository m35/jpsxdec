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

import java.io.IOException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.NotThisTypeException;
import java.io.EOFException;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Uncompressor for demuxed STR v3 video data. 
 * Makes use of most of STR v2 code. Adds v3 handling for DC values. */
public class BitStreamUncompressor_STRv3 extends BitStreamUncompressor_STRv2 {

    /* From the offical MPEG-1 ISO standard specification (ISO 11172).
     * Specifically table 2-D.12 in 11172-2.
     * These tables are only used for version 3 STR frames. */

    // TODO: Cleanup these DC code tables

    /** The longest of all the DC Chroma variable-length-codes is 8 bits */
    public final static int DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE = 8;

    /** Table of DC Chroma (Cr, Cb) variable length codes */
    private final static DCVariableLengthCode DC_Chroma_VarLenCodes[] = {
        //                       Variable   Bits
        //                        length   used to    Negative             Positive
        //                         code   store DC  differential         differential
        new DCVariableLengthCode("00"       , 0,          0         ), //     0
        new DCVariableLengthCode("01"       , 1,         -1         ), //     1
        new DCVariableLengthCode("10"       , 2,    -3 /* to -2   */), //   2 to 3
        new DCVariableLengthCode("110"      , 3,    -7 /* to -4   */), //   4 to 7
        new DCVariableLengthCode("1110"     , 4,   -15 /* to -8   */), //   8 to 15
        new DCVariableLengthCode("11110"    , 5,   -31 /* to -16  */), //  16 to 31
        new DCVariableLengthCode("111110"   , 6,   -63 /* to -32  */), //  32 to 63
        new DCVariableLengthCode("1111110"  , 7,  -127 /* to -64  */), //  64 to 127
        new DCVariableLengthCode("11111110" , 8,  -255 /* to -128 */), // 128 to 255
    };

    /** The longest of all the DC Luma variable-length-codes is 7 bits */
    public final static int DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE = 7;

    /** Table of DC Luma (Y1, Y2, Y3, Y4) variable length codes */
    private final static DCVariableLengthCode DC_Luma_VarLenCodes[] = {
        //                       Variable   Bits
        //                        length   used to    Negative           Positive
        //                         code   store DC  differential       differential
        new DCVariableLengthCode("00"      , 1,        -1          ),//     1
        new DCVariableLengthCode("01"      , 2,    -3 /* to -2   */),//   2 to 3
        new DCVariableLengthCode("100"     , 0,         0          ),//     0
        new DCVariableLengthCode("101"     , 3,    -7 /* to -4   */),//   4 to 7
        new DCVariableLengthCode("110"     , 4,   -15 /* to -8   */),//   8 to 15
        new DCVariableLengthCode("1110"    , 5,   -31 /* to -16  */),//  16 to 31
        new DCVariableLengthCode("11110"   , 6,   -63 /* to -32  */),//  32 to 63
        new DCVariableLengthCode("111110"  , 7,  -127 /* to -64  */),//  64 to 127
        new DCVariableLengthCode("1111110" , 8,  -255 /* to -128 */),// 128 to 255
    };

    /** Holds information for the version 3 DC variable length code lookup */
    private static class DCVariableLengthCode {
        public final String VariableLengthCode;
        public final int DC_Length;
        public final int DC_NegativeDifferential;
        public final int DC_TopBitMask;

        /** Constructor */
        public DCVariableLengthCode(String sVariableLengthCode,
                                    int iDcCoefficientBitSize,
                                    int iNegativeDifferential)
        {
            VariableLengthCode = sVariableLengthCode;
            DC_Length = iDcCoefficientBitSize;
            DC_NegativeDifferential = iNegativeDifferential;
            DC_TopBitMask = 1 << (DC_Length - 1);
        }
        public String encode(int iDCdiffFromPrevDiv4) {
            if (iDCdiffFromPrevDiv4 == 0 && DC_NegativeDifferential == 0)
                return VariableLengthCode;
            else if (iDCdiffFromPrevDiv4 == -1 && DC_NegativeDifferential == -1)
                return VariableLengthCode + "0";
            else if (iDCdiffFromPrevDiv4 == 1 && DC_NegativeDifferential == -1)
                return VariableLengthCode + "1";
            else if (iDCdiffFromPrevDiv4 < 0 && iDCdiffFromPrevDiv4 >= getNegativeMin() && iDCdiffFromPrevDiv4 <= getNegativeMax())
                return VariableLengthCode + "0" + Misc.bitsToString(iDCdiffFromPrevDiv4 - getNegativeMin(), DC_Length-1);
            else if (iDCdiffFromPrevDiv4 > 0 && iDCdiffFromPrevDiv4 >= getPositiveMin() && iDCdiffFromPrevDiv4 <= getPositiveMax())
                return VariableLengthCode + "1" + Misc.bitsToString(iDCdiffFromPrevDiv4 - getPositiveMin(), DC_Length-1);
            return null;
        }
        private int getPositiveMax() { return -DC_NegativeDifferential; }
        private int getPositiveMin() { return -getNegativeMax(); }
        private int getNegativeMin() { return DC_NegativeDifferential; }
        /** This doesn't return a correct value if DC_NegativeDifferential == 0 or -1. */
        private int getNegativeMax() {
            return DC_NegativeDifferential + ((1 << DC_Length) - 1);
        }
    }

    /** 11 bits found at the end of STR v3 movies.
     * <pre>11111111110</pre> */
    private final static String END_OF_FRAME_EXTRA_BITS = "11111111110";

    public BitStreamUncompressor_STRv3() {
        super();
    }

    /** Holds the previous DC values during a version 3 frame decoding. */
    private int _iPreviousCr_DC,
                _iPreviousCb_DC,
                _iPreviousY_DC;

    @Override
    protected void readHeader(byte[] abFrameData, ArrayBitReader bitReader) throws NotThisTypeException {
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800       = IO.readUInt16LE(abFrameData, 2);
        _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion         = IO.readSInt16LE(abFrameData, 6);

        if (iMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 3  || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, true, 8);
        _iPreviousCr_DC = _iPreviousCb_DC = _iPreviousY_DC = 0;
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800           = IO.readUInt16LE(abFrameData, 2);
        int _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion             = IO.readSInt16LE(abFrameData, 6);

        return !(iMagic3800 != 0x3800 || _iQscale < 1 ||
                 iVersion != 3  || _iHalfVlcCountCeil32 < 0);
    }

    @Override
    protected void readQscaleAndDC(MdecCode code) throws MdecException.Uncompress, EOFException {
        code.setTop6Bits(_iQscale);
        switch (getCurrentMacroBlockSubBlock()) {
            case 0:
                code.setBottom10Bits(_iPreviousCr_DC = readV3DcChroma(_iPreviousCr_DC));
                return;
            case 1:
                code.setBottom10Bits(_iPreviousCb_DC = readV3DcChroma(_iPreviousCb_DC));
                return;
            default:
                readV3DcLuma();
                code.setBottom10Bits(_iPreviousY_DC);
        }
    }


    
    private static final int b10000000 = 0x80;
    private static final int b01000000 = 0x40;
    private int readV3DcChroma(int iPreviousDC) throws MdecException.Uncompress, EOFException
    {
        // Peek enough bits
        int iBits = _bitReader.peekUnsignedBits(
                DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode dcVlc;
        
        if ((iBits & b10000000) == 0) { // "0x"
            dcVlc = DC_Chroma_VarLenCodes[(iBits >>> 6) & 1];
        } else {                        // "1x*"
            // count how many 1's there are
            int iMask = b01000000;
            int iTblIndex = 2;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new MdecException.Uncompress(
                        "Error uncompressing macro block:" +
                        " Unknown DC variable length code " + Misc.bitsToString(iBits, 8));
            }
            
            dcVlc = DC_Chroma_VarLenCodes[iTblIndex];
            
        }
        assert DEBUG ? _debug.append(dcVlc.VariableLengthCode) : true;

        _bitReader.skipBits(dcVlc.VariableLengthCode.length());

        if (dcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = _bitReader.readUnsignedBits(dcVlc.DC_Length);

            assert DEBUG ? _debug.append(Misc.bitsToString(iDC_Differential, dcVlc.DC_Length)) : true;

            if ((iDC_Differential & dcVlc.DC_TopBitMask) == 0)
                iDC_Differential += dcVlc.DC_NegativeDifferential;

            // because v3 encoding only uses 8 bits of precision for DC,
            // it needs to be shifted up by 2 bits to be ready for
            // the Qtable[0] multiplication of 2 which will fill up it to the full
            // 11 bits that DC is supposed to have
            iPreviousDC += iDC_Differential << 2;
        }

        // return the new DC Coefficient
        return iPreviousDC;
    }
    
    private static final int b1000000 = 0x40;
    private static final int b0100000 = 0x20;
    private static final int b0010000 = 0x10;
    private void readV3DcLuma() throws MdecException.Uncompress, EOFException {
        // Peek enough bits
        int iBits = _bitReader.peekUnsignedBits(DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode dcVlc;
        
        if (       (iBits & b1000000) == 0) { // "0x"
            dcVlc = DC_Luma_VarLenCodes[    ((iBits >>> 5) & 1)];
        } else if ((iBits & b0100000) == 0) { // "10"
            dcVlc = DC_Luma_VarLenCodes[2 + ((iBits >>> 4) & 1)];
        } else {                              // "11x"
            int iMask = b0010000;
            int iTblIndex = 4;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new MdecException.Uncompress("Error decoding macro block:" +
                                            " Unknown DC variable length code " + iBits);
            }
            
            dcVlc = DC_Luma_VarLenCodes[iTblIndex];
        }
        assert DEBUG ? _debug.append(dcVlc.VariableLengthCode) : true;

        // skip the variable length code bits
        _bitReader.skipBits(dcVlc.VariableLengthCode.length());

        if (dcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = _bitReader.readUnsignedBits(dcVlc.DC_Length);

            assert DEBUG ? _debug.append(Misc.bitsToString(iDC_Differential, dcVlc.DC_Length)) : true;

            if ((iDC_Differential & dcVlc.DC_TopBitMask) == 0)
                iDC_Differential += dcVlc.DC_NegativeDifferential;

            // because v3 encoding only uses 8 bits of precision for DC,
            // it needs to be shifted up by 2 bits to be ready for
            // the Qtable[0] multiplication of 2 which will fill up it to the full
            // 11 bits that DC is supposed to have
            _iPreviousY_DC += iDC_Differential * 4;

            if (!(_iPreviousY_DC >= -512 && _iPreviousY_DC <= 511))
                throw new MdecException.Uncompress("DC out of bounds " + _iPreviousY_DC);
        }
    }

    private static final int b11111111110 = 0x7FE;

    @Override
    public void skipPaddingBits() throws EOFException {
        int iPaddingBits = _bitReader.readUnsignedBits(11);
        if (iPaddingBits != b11111111110) {
            log.warning("Incorrect padding bits " + Misc.bitsToString(iPaddingBits, 11));
        }
    }

    @Override
    public String getName() {
        return "STRv3";
    }
    
    @Override
    public BitstreamCompressor_STRv3 makeCompressor() {
        return new BitstreamCompressor_STRv3();
    }

    public static class BitstreamCompressor_STRv3 extends BitstreamCompressor_STRv2 {

        @Override
        protected int getHeaderVersion() { return 3; }

        @Override
        public byte[] compress(MdecInputStream inStream, int iMdecCodeCount) throws MdecException {
            _iPreviousCr_DC4 = _iPreviousCb_DC4 = _iPreviousY_DC4 = 0;
            return super.compress(inStream, iMdecCodeCount);
        }

        private int _iPreviousCr_DC4, _iPreviousCb_DC4, _iPreviousY_DC4;

        @Override
        protected String encodeDC(int iDC, int iBlock) {
            int iDCdiff;
            int iDC4 = 0;
            switch (iDC % 4) {
                case 0: iDC4 = iDC    ; break;
                case 1: iDC4 = iDC - 1; break;
                case 2: iDC4 = iDC - 2; break;
                case 3: iDC4 = iDC + 1; break;
            }
            DCVariableLengthCode[] lookupTable;
            switch (iBlock) {
                case 0:
                    iDCdiff = iDC4 - _iPreviousCr_DC4;
                    _iPreviousCr_DC4 = iDC4;
                    lookupTable = DC_Chroma_VarLenCodes;
                    break;
                case 1:
                    iDCdiff = iDC4 - _iPreviousCb_DC4;
                    _iPreviousCb_DC4 = iDC4;
                    lookupTable = DC_Chroma_VarLenCodes;
                    break;
                default:
                    iDCdiff = iDC4 - _iPreviousY_DC4;
                    _iPreviousY_DC4 = iDC4;
                    lookupTable = DC_Luma_VarLenCodes;
                    break;
            }

            // TODO: Maybe try to expose this quality loss somehow
            iDCdiff = (int) Math.round(iDCdiff / 4.0);
            for (DCVariableLengthCode dcCodeBits : lookupTable) {
                String sEncodedBits = dcCodeBits.encode(iDCdiff);
                if (sEncodedBits != null)
                    return sEncodedBits;
            }
            throw new IllegalArgumentException("Unable to encode DC value " + iDC);
        }

        @Override
        protected void addTrailingBits(BitStreamWriter bitStream) throws IOException {
            bitStream.write(END_OF_FRAME_EXTRA_BITS);
        }

    }
    
}

