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

package jpsxdec.plugins.psx.video.decode;

import jpsxdec.util.NotThisTypeException;
import java.io.EOFException;
import java.util.logging.Logger;
import jpsxdec.util.IO;

/** Uncompressor for demuxed STR v3 video data. 
 * Makes use of most of STR v2 code. Adds v3 handling for DC values.
 */
public class DemuxFrameUncompressor_STRv3 extends DemuxFrameUncompressor_STRv2 {

    private static final Logger log = Logger.getLogger(DemuxFrameUncompressor_STRv3.class.getName());
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
    }

    public DemuxFrameUncompressor_STRv3() {
        super();
    }
    
    public DemuxFrameUncompressor_STRv3(byte[] abDemuxData) throws NotThisTypeException {
        super(abDemuxData);
    }

    // Holds the previous DC values for ver 3 frames.
    private int _iPreviousCr_DC;
    private int _iPreviousCb_DC;
    private int _iPreviousY_DC;

    @Override
    public void reset(byte[] abDemuxData) throws NotThisTypeException {
        _iPreviousCr_DC = _iPreviousCb_DC = _iPreviousY_DC = 0;
        super.reset(abDemuxData);
    }

    @Override
    protected ArrayBitReader readHeader(byte[] abFrameData) throws NotThisTypeException {
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        _lngMagic3800        = IO.readUInt16LE(abFrameData, 2);
        _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion          = IO.readSInt16LE(abFrameData, 6);

        if (_lngMagic3800 != 0x3800 || _iQscale < 1 || iVersion != 3)
            throw new NotThisTypeException();

        return new ArrayBitReader(abFrameData, true, 8);
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        long lngMagic3800       = IO.readUInt16LE(abFrameData, 2);
        int iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion            = IO.readSInt16LE(abFrameData, 6);

        return lngMagic3800 == 0x3800 &&
               iQscale >= 1 &&
               iVersion == 3 &&
               iHalfVlcCountCeil32 >= 0;
    }

    @Override
    protected void readQscaleDC(MdecCode code) throws EOFException, UncompressionException {
        code.Top6Bits = _iQscale;
        switch (_iCurrentBlock) {
            case 0:
                code.Bottom10Bits = _iPreviousCr_DC = decodeV3_DC_Chrominance(_iPreviousCr_DC);
                return;
            case 1:
                code.Bottom10Bits = _iPreviousCb_DC = decodeV3_DC_Chrominance(_iPreviousCb_DC);
                return;
            default:
                decodeV3_DC_Luminance();
                code.Bottom10Bits = _iPreviousY_DC;
        }
    }



    private static final int b10000000 = 0x80;
    private static final int b01000000 = 0x40;
    private int decodeV3_DC_Chrominance(int iPreviousDC)
        throws UncompressionException, EOFException 
    {
        // Peek enough bits
        int iBits = (int)_bitReader.peekUnsignedBits(
                DC_CHROMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode tDcVlc;
        
        if ((iBits & b10000000) == 0) { // "0x"
            tDcVlc = DC_Chrominance_VarLenCodes[(iBits >>> 6) & 1];
        } else {                        // "1x*"
            // count how many 1's there are
            int iMask = b01000000;
            int iTblIndex = 2;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new UncompressionException(
                        "Error decoding macro block:" +
                        " Unknown DC variable length code " + iBits);
            }
            
            tDcVlc = DC_Chrominance_VarLenCodes[iTblIndex];
            
        }

        _bitReader.skipBits(tDcVlc.VariableLengthCode.length());

        if (tDcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = 
                   (int)_bitReader.readUnsignedBits(tDcVlc.DC_Length);

            if ((iDC_Differential & tDcVlc.DC_TopBitMask) == 0)
                iDC_Differential += tDcVlc.DC_NegativeDifferential;

            iPreviousDC += iDC_Differential
                    // !!! ???We must multiply it by 4 for no reason??? !!!
                            * 4;
        }

        if (_debug != null) {
            _debug.codeRead(_bitReader.getPosition(),
                              tDcVlc.VariableLengthCode,
                              _iQscale, iPreviousDC);
        }

        // return the new DC Coefficient
        return iPreviousDC;
    }
    
    private static final int b1000000 = 0x40;
    private static final int b0100000 = 0x20;
    private static final int b0010000 = 0x10;
    private final void decodeV3_DC_Luminance()
        throws UncompressionException, EOFException 
    {
        // Peek enough bits
        int iBits = (int)_bitReader.peekUnsignedBits(
                DC_LUMINANCE_LONGEST_VARIABLE_LENGTH_CODE);
        
        DCVariableLengthCode tDcVlc;
        
        if (       (iBits & b1000000) == 0) { // "0x"
            tDcVlc = DC_Luminance_VarLenCodes[    ((iBits >>> 5) & 1)];
        } else if ((iBits & b0100000) == 0) { // "10"
            tDcVlc = DC_Luminance_VarLenCodes[2 + ((iBits >>> 4) & 1)];
        } else {                              // "11x"
            int iMask = b0010000;
            int iTblIndex = 4;
            while ((iBits & iMask) > 0) {
                iMask >>>= 1;
                iTblIndex++;
            }
            
            if (iMask == 0) {
                throw new UncompressionException("Error decoding macro block:" +
                                                 " Unknown DC variable length code " + iBits);
            }
            
            tDcVlc = DC_Luminance_VarLenCodes[iTblIndex];
            
        }

        // skip the variable length code bits
        _bitReader.skipBits(tDcVlc.VariableLengthCode.length());

        if (tDcVlc.DC_Length != 0) {

            // Read the DC differential
            int iDC_Differential = 
                   (int)_bitReader.readUnsignedBits(tDcVlc.DC_Length);

            if ((iDC_Differential & tDcVlc.DC_TopBitMask) == 0)
                iDC_Differential += tDcVlc.DC_NegativeDifferential;

            _iPreviousY_DC += iDC_Differential
                    // !!! ???We must multiply it by 4 for no reason??? !!!
                              * 4;
        }

        if (_debug != null) {
            _debug.codeRead(_bitReader.getPosition(),
                              tDcVlc.VariableLengthCode,
                              _iQscale, _iPreviousY_DC);
        }
    }

    @Override
    public String toString() {
        return "STRv3";
    }
    
}
