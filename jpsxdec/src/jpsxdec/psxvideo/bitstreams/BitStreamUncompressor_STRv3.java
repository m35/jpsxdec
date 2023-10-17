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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;

/** Uncompressor for demuxed STR v3 video bitstream data.
 * Makes use of most of STR v2 code. Adds v3 handling for DC values. */
public class BitStreamUncompressor_STRv3 extends BitStreamUncompressor implements IBitStreamWith1QuantizationScale {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_STRv3.class.getName());

    public static class StrV3Header extends StrHeader {
        public StrV3Header(@Nonnull byte[] abFrameData, int iDataSize) {
            super(abFrameData, iDataSize, 3);
        }

        @Override
        public @Nonnull BitStreamUncompressor_STRv3 makeNew(@Nonnull byte[] abBitstream, int iBitstreamSize)
                throws BinaryDataNotRecognized
        {
            return BitStreamUncompressor_STRv3.makeV3(abBitstream, iBitstreamSize);
        }
    }

    public static @Nonnull BitStreamUncompressor_STRv3 makeV3(@Nonnull byte[] abFrameData)
            throws BinaryDataNotRecognized
    {
        return makeV3(abFrameData, abFrameData.length);
    }
    public static @Nonnull BitStreamUncompressor_STRv3 makeV3(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_STRv3 bsu = makeV3NoThrow(abFrameData, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    static @CheckForNull BitStreamUncompressor_STRv3 makeV3NoThrow(@Nonnull byte[] abFrameData, int iDataSize)
    {
        StrV3Header header = new StrV3Header(abFrameData, iDataSize);
        if (!header.isValid())
            return null;
        ArrayBitReader bitReader = BitStreamUncompressor_STRv2.makeStrBitReader(abFrameData, iDataSize);

        return new BitStreamUncompressor_STRv3(header, bitReader);
    }

    // ########################################################################
    // ## Static stuff ########################################################
    // ########################################################################

    /** v3 DC variable length code. Allows for uncompressing and compressing
     * to/from a bit stream. */
    private static abstract class DcVariableLengthCode {
        /** The DC variable length code. */
        @Nonnull
        public final String VariableLengthCode;
        /** Number of bits to read for the differential. */
        protected final int _iDifferentialBitLen;

        public DcVariableLengthCode(@Nonnull String sCode, int iDifferentialBitLen) {
            VariableLengthCode = sCode;
            _iDifferentialBitLen = iDifferentialBitLen;
        }

        /** Populates a lookup array indexes that match this variable length code. */
        public void fillLookupArray(@Nonnull DcVariableLengthCode[] aoLookup, int iLongestCode) {
            int iUnimportantBits = iLongestCode - VariableLengthCode.length();
            int iStart = Integer.parseInt(VariableLengthCode+Misc.dup('0', iUnimportantBits), 2);
            int iEnd = Integer.parseInt(VariableLengthCode+Misc.dup('1', iUnimportantBits), 2);
            for (int i = iStart; i <= iEnd; i++) {
                if (aoLookup[i] != null)
                    throw new IllegalStateException("Lookup table "+i+" is not null.");
                aoLookup[i] = this;
            }
        }

        abstract public int readDc(@Nonnull ArrayBitReader bitReader)
                throws MdecException.EndOfStream;
        /** Attempts to encode a DC value that has already been diff'ed from
         *  the previous value and divided by 4.
         * @return bit string or null if could not encode with this VLC. */
        abstract public @CheckForNull String encode(int iDCdiffFromPrevDiv4);
    }

    /** v3 DC variable length code for 0 value. */
    private static class DcVlc0 extends DcVariableLengthCode {
        private final int _iDifferential;

        public DcVlc0(@Nonnull String sCode, int iDifferentialBitLen, int iDifferential) {
            super(sCode, iDifferentialBitLen);
            if (iDifferentialBitLen != 0)
                throw new IllegalArgumentException();
            this._iDifferential = iDifferential;
        }

        @Override
        public int readDc(@Nonnull ArrayBitReader bitReader) {
            return _iDifferential;
        }

        @Override
        public @CheckForNull String encode(int i) {
            if (i == _iDifferential)
                return VariableLengthCode;
            return null;
        }
    }

    /** v3 DC variable length code for non-0 values. */
    private static class DcVlc_ extends DcVariableLengthCode {
        private final int _iNegativeDifferentialMin;
        private final int _iNegativeDifferentialMax;
        private final int _iPositiveDifferentialMin;
        private final int _iPositiveDifferentialMax;
        private final int _iTopBitMask;

        public DcVlc_(@Nonnull String sCode, int iDifferentialBitLen, int iPositiveDifferential) {
            this(sCode, iDifferentialBitLen, iPositiveDifferential, iPositiveDifferential);
        }

        public DcVlc_(@Nonnull String sCode, int iDifferentialBitLen,
                      int iPositiveDifferentialMin, int iPositiveDifferentialMax)
        {
            super(sCode, iDifferentialBitLen);
            _iPositiveDifferentialMin = iPositiveDifferentialMin;
            _iPositiveDifferentialMax = iPositiveDifferentialMax;
            _iNegativeDifferentialMin = -iPositiveDifferentialMax;
            _iNegativeDifferentialMax = -iPositiveDifferentialMin;
            _iTopBitMask = 1 << (iDifferentialBitLen - 1);
        }

        @Override
        public int readDc(@Nonnull ArrayBitReader bitReader)
                throws MdecException.EndOfStream
        {
            int iDC_Differential = bitReader.readUnsignedBits(_iDifferentialBitLen);
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(iDC_Differential, _iDifferentialBitLen));
            // top bit == 0 means it's negative
            if ((iDC_Differential & _iTopBitMask) == 0) {
                iDC_Differential += _iNegativeDifferentialMin;
            }
            // value will be between -255 <= iDC_Differential <= 255

            // because v3 encoding only uses 8 bits of precision for DC,
            // it needs to be shifted up by 2 bits to be ready for
            // the Qtable[0] multiplication of 2 which will fill up it to the full
            // 11 bits that DC is supposed to have
            return iDC_Differential * 4;
        }

        @Override
        public @CheckForNull String encode(int iDCdiffFromPrevDiv4) {
            if (iDCdiffFromPrevDiv4 >= _iNegativeDifferentialMin &&
                iDCdiffFromPrevDiv4 <= _iNegativeDifferentialMax)
            {
                return VariableLengthCode +
                       Misc.bitsToString(iDCdiffFromPrevDiv4 - _iNegativeDifferentialMin,
                                         _iDifferentialBitLen);
            } else if (iDCdiffFromPrevDiv4 >= _iPositiveDifferentialMin &&
                      iDCdiffFromPrevDiv4 <= _iPositiveDifferentialMax)
            {
                return VariableLengthCode +
                       Misc.bitsToString(iDCdiffFromPrevDiv4, _iDifferentialBitLen);
            } else
                return null;
        }
    }

    /* From the offical MPEG-1 ISO standard specification (ISO 11172).
     * Specifically table 2-D.12 in 11172-2.
     * These tables are only used for version 3 STR frames. */

    /** The longest of all the DC Chroma variable-length-codes is 8 bits */
    public static final int DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE = 8;

    /** Table of DC Chroma (Cr, Cb) variable length codes */
    private static final DcVariableLengthCode[] DC_Chroma_VarLenCodes = {
        //       Variable     Bits
        //        length     used to     Negative          Positive
        //         code     store DC   differential      differential
        new DcVlc0("00"       , 0,  /*       0      */         0        ),
        new DcVlc_("01"       , 1,  /*      -1      */         1        ),
        new DcVlc_("10"       , 2,  /*   -3 to -2   */     2,/*to*/3    ),
        new DcVlc_("110"      , 3,  /*   -7 to -4   */     4,/*to*/7    ),
        new DcVlc_("1110"     , 4,  /*  -15 to -8   */     8,/*to*/15   ),
        new DcVlc_("11110"    , 5,  /*  -31 to -16  */    16,/*to*/31   ),
        new DcVlc_("111110"   , 6,  /*  -63 to -32  */    32,/*to*/63   ),
        new DcVlc_("1111110"  , 7,  /* -127 to -64  */    64,/*to*/127  ),
        new DcVlc_("11111110" , 8,  /* -255 to -128 */   128,/*to*/255  ),
    };

    /** The longest of all the DC Luma variable-length-codes is 7 bits */
    public static final int DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE = 7;

    /** Table of DC Luma (Y1, Y2, Y3, Y4) variable length codes */
    private static final DcVariableLengthCode[] DC_Luma_VarLenCodes = {
        //       Variable    Bits
        //        length    used to     Negative           Positive
        //         code    store DC   differential       differential
        new DcVlc_("00"      , 1,  /*      -1      */         1        ),
        new DcVlc_("01"      , 2,  /*   -3 to -2   */     2,/*to*/3    ),
        new DcVlc0("100"     , 0,  /*       0      */         0        ),
        new DcVlc_("101"     , 3,  /*   -7 to -4   */     4,/*to*/7    ),
        new DcVlc_("110"     , 4,  /*  -15 to -8   */     8,/*to*/15   ),
        new DcVlc_("1110"    , 5,  /*  -31 to -16  */    16,/*to*/31   ),
        new DcVlc_("11110"   , 6,  /*  -63 to -32  */    32,/*to*/63   ),
        new DcVlc_("111110"  , 7,  /* -127 to -64  */    64,/*to*/127  ),
        new DcVlc_("1111110" , 8,  /* -255 to -128 */   128,/*to*/255  ),
    };

    /** Crazy fast way to find the matching chroma variable-length code from 8 bits.
     * Invalid entries will be null. */
    private static final DcVariableLengthCode[] CHROMA_LOOKUP =
            new DcVariableLengthCode[1 << DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE];
    /** Crazy fast way to find the matching luma variable-length code from 7 bits.
     * Invalid entries will be null. */
    private static final DcVariableLengthCode[] LUMA_LOOKUP =
            new DcVariableLengthCode[1 << DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE];

    static {
        for (DcVariableLengthCode dcVlc : DC_Chroma_VarLenCodes) {
            dcVlc.fillLookupArray(CHROMA_LOOKUP, DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE);
        }
        for (DcVariableLengthCode dcVlc : DC_Luma_VarLenCodes) {
            dcVlc.fillLookupArray(LUMA_LOOKUP, DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE);
        }
    }


    /** 10 bits found at the end of STR v3 frames.
     * <pre>1111111111</pre> */
    private static final String END_OF_FRAME_EXTRA_BITS = "1111111111";
    private static final int PADDING_BITS_LENGTH = END_OF_FRAME_EXTRA_BITS.length();
    /** 10 bits found at the end of STR v3 frames.
     * <pre>11111111110</pre> */
    private static final int b1111111111 = 0x3FF;

    // ########################################################################
    // ## Instance stuff ######################################################
    // ########################################################################

    @Nonnull
    private final StrV3Header _header;

    public BitStreamUncompressor_STRv3(@Nonnull StrV3Header header,
                                       @Nonnull ArrayBitReader bitReader)
    {
        super(bitReader, ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1,
              new QuantizationDcReader_STRv3(header.getQuantizationScale()),
              BitStreamUncompressor_STRv2.AC_ESCAPE_CODE_STR,
              FRAME_END_PADDING_BITS_STRV3);
        _header = header;
    }

    @Override
    public int getQuantizationScale() {
        return _header.getQuantizationScale();
    }

    public static class QuantizationDcReader_STRv3 implements IQuantizationDcReader {

        private final int _iFrameQuantizationScale;

        /** Holds the previous DC values during a version 3 frame decoding. */
        private int _iPreviousCr_DC = 0,
                    _iPreviousCb_DC = 0,
                    _iPreviousY_DC = 0;

        public QuantizationDcReader_STRv3(int iFrameQuantizationScale) {
            _iFrameQuantizationScale = iFrameQuantizationScale;
        }

        @Override
        public void readQuantizationScaleAndDc(@Nonnull ArrayBitReader bitReader,
                                               @Nonnull MdecContext context,
                                               @Nonnull MdecCode code)
                throws MdecException.ReadCorruption, MdecException.EndOfStream
        {
            code.setTop6Bits(_iFrameQuantizationScale);
            switch (context.getCurrentBlock()) {
                case Cr:
                    code.setBottom10Bits(_iPreviousCr_DC = readV3DcChroma(_iPreviousCr_DC, bitReader, context));
                    return;
                case Cb:
                    code.setBottom10Bits(_iPreviousCb_DC = readV3DcChroma(_iPreviousCb_DC, bitReader, context));
                    return;
                default:
                    readV3DcLuma(bitReader, context);
                    code.setBottom10Bits(_iPreviousY_DC);
            }
        }


        private int readV3DcChroma(int iPreviousDC, @Nonnull ArrayBitReader bitReader, @Nonnull MdecContext context)
                throws MdecException.ReadCorruption, MdecException.EndOfStream
        {
            // Peek enough bits
            int iBits = bitReader.peekUnsignedBits(DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE);

            DcVariableLengthCode dcVlc = CHROMA_LOOKUP[iBits];

            if (dcVlc == null) {
                String s = "Unknown chroma DC variable length code: " +
                           Misc.bitsToString(iBits, DC_CHROMA_LONGEST_VARIABLE_LENGTH_CODE) +
                           " at " + context;
                throw new MdecException.ReadCorruption(s);
            }

            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(dcVlc.VariableLengthCode);

            // skip the variable length code bits
            bitReader.skipBits(dcVlc.VariableLengthCode.length());

            iPreviousDC += dcVlc.readDc(bitReader);

            if (iPreviousDC < -512 || iPreviousDC > 511) {
                throw new MdecException.ReadCorruption(MdecException.STRV3_BLOCK_UNCOMPRESS_ERR_CHROMA_DC_OOB(
                        context.getTotalMacroBlocksRead(), context.getCurrentBlock().ordinal(),
                        iPreviousDC));
            }

            return iPreviousDC;
        }

        private void readV3DcLuma(@Nonnull ArrayBitReader bitReader, @Nonnull MdecContext context) throws MdecException.ReadCorruption, MdecException.EndOfStream {
            // Peek enough bits
            int iBits = bitReader.peekUnsignedBits(DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE);

            DcVariableLengthCode dcVlc = LUMA_LOOKUP[iBits];

            if (dcVlc == null) {
                throw new MdecException.ReadCorruption(MdecException.STRV3_BLOCK_UNCOMPRESS_ERR_UNKNOWN_LUMA_DC_VLC(
                        context.getTotalMacroBlocksRead(), context.getCurrentBlock().ordinal(),
                        Misc.bitsToString(iBits, DC_LUMA_LONGEST_VARIABLE_LENGTH_CODE)));
            }

            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(dcVlc.VariableLengthCode);

            // skip the variable length code bits
            bitReader.skipBits(dcVlc.VariableLengthCode.length());

            _iPreviousY_DC += dcVlc.readDc(bitReader);

            if (_iPreviousY_DC < -512 || _iPreviousY_DC > 511) {
                throw new MdecException.ReadCorruption(MdecException.STRV3_BLOCK_UNCOMPRESS_ERR_LUMA_DC_OOB(
                        context.getTotalMacroBlocksRead(), context.getCurrentBlock().ordinal(),
                        _iPreviousY_DC));
            }
        }
    }


    private static class FrameEndPaddingBits_STRv3 implements IFrameEndPaddingBits {
        @Override
        public boolean skipPaddingBits(@Nonnull ArrayBitReader bitReader) throws MdecException.EndOfStream {
            int iPaddingBits = bitReader.readUnsignedBits(PADDING_BITS_LENGTH);
            if (iPaddingBits != b1111111111) {
                LOG.log(Level.WARNING, "Incorrect padding bits {0} should be {1}",
                        new Object[]{Misc.bitsToString(iPaddingBits, PADDING_BITS_LENGTH),
                                     Misc.bitsToString(b1111111111, PADDING_BITS_LENGTH)});
                return false;
            }
            return true;
        }
    }
    public static final IFrameEndPaddingBits FRAME_END_PADDING_BITS_STRV3 = new FrameEndPaddingBits_STRv3();

    @Override
    public @Nonnull BitStreamCompressor_STRv3 makeCompressor() {
        return new BitStreamCompressor_STRv3(_context.getTotalMacroBlocksRead(), getQuantizationScale());
    }

    // =========================================================================

    /** Note unlike all other compressors, STRv3 is LOSSY when encoding
     * DC coefficients. */
    public static class BitStreamCompressor_STRv3 extends BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2 {

        public BitStreamCompressor_STRv3(int iMacroBlockCount, int iOriginalQscale) {
            this(iMacroBlockCount, iOriginalQscale, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER);
        }

        protected BitStreamCompressor_STRv3(int iMacroBlockCount, int iOriginalQscale, @Nonnull IByteOrder byteOrder) {
            super(iMacroBlockCount, iOriginalQscale, 3, END_OF_FRAME_EXTRA_BITS, byteOrder);
        }

        @Override
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws IncompatibleException, MdecException.EndOfStream,
                       MdecException.ReadCorruption, MdecException.TooMuchEnergy
        {
            _iPreviousCr_DcRound4 = _iPreviousCb_DcRound4 = _iPreviousY_DcRound4 = 0;
            return super.compress(inStream);
        }

        /** Store the prior DC coefficients written. The value will always be
         *  a multiple of 4 since the value is divided by 4 before writing. */
        private int _iPreviousCr_DcRound4,
                    _iPreviousCb_DcRound4,
                    _iPreviousY_DcRound4;

        @Override
        protected @Nonnull String encodeDC(int iDC, @Nonnull MdecBlock block) throws MdecException.TooMuchEnergy {
            // round to the nearest multiple of 4
            // TODO: Maybe try to expose this quality loss somehow
            int iDcRound4 = (int)Math.round(iDC / 4.0) * 4;

            // diff it against the previous DC that was rounded
            // to the nearest multiple of 4, and divide by 4
            // and save the rounded DC for the next iteration
            DcVariableLengthCode[] lookupTable;
            int iDcDiffRound4Div4;
            switch (block) {
                case Cr:
                    iDcDiffRound4Div4 = (iDcRound4 - _iPreviousCr_DcRound4) / 4;
                    _iPreviousCr_DcRound4 = iDcRound4;
                    lookupTable = DC_Chroma_VarLenCodes;
                    break;
                case Cb:
                    iDcDiffRound4Div4 = (iDcRound4 - _iPreviousCb_DcRound4) / 4;
                    _iPreviousCb_DcRound4 = iDcRound4;
                    lookupTable = DC_Chroma_VarLenCodes;
                    break;
                default:
                    iDcDiffRound4Div4 = (iDcRound4 - _iPreviousY_DcRound4) / 4;
                    _iPreviousY_DcRound4 = iDcRound4;
                    lookupTable = DC_Luma_VarLenCodes;
                    break;
            }

            for (DcVariableLengthCode dcCodeBits : lookupTable) {
                String sEncodedBits = dcCodeBits.encode(iDcDiffRound4Div4);
                if (sEncodedBits != null)
                    return sEncodedBits;
            }
            // I forgot why this can happen
            throw new MdecException.TooMuchEnergy(String.format(
                    "Unable to compress DC value %d as diffed/4=%d",
                    iDC, iDcDiffRound4Div4));
        }

    }

}

