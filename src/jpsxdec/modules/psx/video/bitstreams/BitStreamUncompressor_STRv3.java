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
import java.io.IOException;
import jpsxdec.modules.psx.video.mdec.MdecInputStream;
import jpsxdec.util.NotThisTypeException;
import java.io.EOFException;
import java.util.logging.Logger;
import jpsxdec.modules.psx.video.encode.BitStreamWriter;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;

/** Uncompressor for demuxed STR v3 video data. 
 * Makes use of most of STR v2 code. Adds v3 handling for DC values.
 */
public class BitStreamUncompressor_STRv3 extends BitStreamUncompressor_STRv2 {

    private static final Logger log = Logger.getLogger(BitStreamUncompressor_STRv3.class.getName());
    protected Logger getLog() { return log; }

    /* From the offical MPEG-1 ISO standard specification (ISO 11172).
     * Specifically table 2-D.12 in 11172-2.
     * These tables are only used for version 3 STR frames. */

    /** The longest of all the DC Chrominance variable-length-codes is 8 bits */
    public final static int DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE = 8;

    /** Table of DC Chrominance (Cr, Cb) variable length codes */
    private final static DCVariableLengthCode DC_Chrominance_VarLenCodes[] = {
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

    /** The longest of all the DC Luminance variable-length-codes is 7 bits */
    public final static int DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE = 7;

    /** Table of DC Luminance (Y1, Y2, Y3, Y4) variable length codes */
    private final static DCVariableLengthCode DC_Luminance_VarLenCodes[] = {
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

    private final static String END_OF_FRAME_EXTRA_BITS = "11111111110";

    public BitStreamUncompressor_STRv3() {
        super();
    }
    
    // Holds the previous DC values for ver 3 frames.
    private int _iPreviousCr_DC;
    private int _iPreviousCb_DC;
    private int _iPreviousY_DC;

    @Override
    public void reset(byte[] abDemuxData, int iStart) throws NotThisTypeException {
        _iPreviousCr_DC = _iPreviousCb_DC = _iPreviousY_DC = 0;
        super.reset(abDemuxData, iStart);
    }

    @Override
    protected void readHeader(byte[] abFrameData, int iStart, ArrayBitReader bitReader) throws NotThisTypeException {
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, iStart+0);
        _iMagic3800          = IO.readUInt16LE(abFrameData, iStart+2);
        _iQscale             = IO.readSInt16LE(abFrameData, iStart+4);
        int iVersion         = IO.readSInt16LE(abFrameData, iStart+6);

        if (_iMagic3800 != 0x3800 || _iQscale < 1 || iVersion != 3)
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
               iVersion == 3 &&
               iHalfVlcCountCeil32 >= 0;
    }

    @Override
    protected void readQscaleDC(MdecCode code) throws EOFException, DecodingException {
        code.setTop6Bits(_iQscale);
        switch (_iCurrentBlock) {
            case 0:
                code.setBottom10Bits(_iPreviousCr_DC = decodeV3_DC_Chrominance(_iPreviousCr_DC));
                return;
            case 1:
                code.setBottom10Bits(_iPreviousCb_DC = decodeV3_DC_Chrominance(_iPreviousCb_DC));
                return;
            default:
                decodeV3_DC_Luminance();
                code.setBottom10Bits(_iPreviousY_DC);
        }
    }



    private static final int b10000000 = 0x80;
    private static final int b01000000 = 0x40;
    private int decodeV3_DC_Chrominance(int iPreviousDC)
        throws DecodingException, EOFException
    {
        // Peek enough bits
        int iBits = (int)_bitReader.peekUnsignedBits(
                DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode dcVlc;
        
        if ((iBits & b10000000) == 0) { // "0x"
            dcVlc = DC_Chrominance_VarLenCodes[(iBits >>> 6) & 1];
        } else {                        // "1x*"
            // count how many 1's there are
            int iMask = b01000000;
            int iTblIndex = 2;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new DecodingException(
                        "Error decoding macro block:" +
                        " Unknown DC variable length code " + iBits);
            }
            
            dcVlc = DC_Chrominance_VarLenCodes[iTblIndex];
            
        }
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(dcVlc.VariableLengthCode);

        _bitReader.skipBits(dcVlc.VariableLengthCode.length());

        if (dcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential =
                   (int)_bitReader.readUnsignedBits(dcVlc.DC_Length);

            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(iDC_Differential, dcVlc.DC_Length));

            if ((iDC_Differential & dcVlc.DC_TopBitMask) == 0)
                iDC_Differential += dcVlc.DC_NegativeDifferential;

            iPreviousDC += iDC_Differential
                    // !!! ???We must multiply it by 4 for no reason??? !!!
                            * 4;
        }

        // return the new DC Coefficient
        return iPreviousDC;
    }
    
    private static final int b1000000 = 0x40;
    private static final int b0100000 = 0x20;
    private static final int b0010000 = 0x10;
    private final void decodeV3_DC_Luminance()
        throws DecodingException, EOFException
    {
        // Peek enough bits
        int iBits = (int)_bitReader.peekUnsignedBits(
                DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode dcVlc;
        
        if (       (iBits & b1000000) == 0) { // "0x"
            dcVlc = DC_Luminance_VarLenCodes[    ((iBits >>> 5) & 1)];
        } else if ((iBits & b0100000) == 0) { // "10"
            dcVlc = DC_Luminance_VarLenCodes[2 + ((iBits >>> 4) & 1)];
        } else {                              // "11x"
            int iMask = b0010000;
            int iTblIndex = 4;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new DecodingException("Error decoding macro block:" +
                                                 " Unknown DC variable length code " + iBits);
            }
            
            dcVlc = DC_Luminance_VarLenCodes[iTblIndex];
        }
        if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(dcVlc.VariableLengthCode);

        // skip the variable length code bits
        _bitReader.skipBits(dcVlc.VariableLengthCode.length());

        if (dcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = 
                   (int)_bitReader.readUnsignedBits(dcVlc.DC_Length);

            if (DEBUG_UNCOMPRESSOR) _debug.Bits.append(Misc.bitsToString(iDC_Differential, dcVlc.DC_Length));

            if ((iDC_Differential & dcVlc.DC_TopBitMask) == 0)
                iDC_Differential += dcVlc.DC_NegativeDifferential;

            _iPreviousY_DC += iDC_Differential
                    // !!! ???We must multiply it by 4 for no reason??? !!!
                              * 4;
        }
    }

    @Override
    public String toString() {
        return "STRv3";
    }
    
    @Override
    public BitStreamCompressor makeCompressor() {
        return new BitstreamCompressor_STRv3();
    }

    public static class BitstreamCompressor_STRv3 extends BitstreamCompressor_STRv2 {

        @Override
        protected int getHeaderVersion() { return 3; }

        @Override
        public byte[] compress(MdecInputStream inStream, int iLuminQscale, int iChromQscale, int iMdecCodeCount) throws DecodingException, IOException {
            _iPreviousCr_DC = _iPreviousCb_DC = _iPreviousY_DC = 0;
            return super.compress(inStream, iLuminQscale, iChromQscale, iMdecCodeCount);
        }

        private int _iPreviousCr_DC;
        private int _iPreviousCb_DC;
        private int _iPreviousY_DC;

        @Override
        protected String encodeDC(int iDC, int iBlock) {
            int iDCdiff;
            DCVariableLengthCode[] lookupTable;
            switch (iBlock) {
                case 0:
                    iDCdiff = iDC - _iPreviousCr_DC;
                    _iPreviousCr_DC = iDC;
                    lookupTable = DC_Chrominance_VarLenCodes;
                    break;
                case 1:
                    iDCdiff = iDC - _iPreviousCb_DC;
                    _iPreviousCb_DC = iDC;
                    lookupTable = DC_Chrominance_VarLenCodes;
                    break;
                default:
                    iDCdiff = iDC - _iPreviousY_DC;
                    _iPreviousY_DC = iDC;
                    lookupTable = DC_Luminance_VarLenCodes;
                    break;
            }

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

