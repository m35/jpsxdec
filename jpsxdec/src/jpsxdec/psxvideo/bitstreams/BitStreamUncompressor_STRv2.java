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
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;


/** Bitstream parser/decoder for the very common "version 2" demuxed video
 * frames, used by most games. */
public class BitStreamUncompressor_STRv2 extends BitStreamUncompressor implements IBitStreamWith1QuantizationScale {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_STRv2.class.getName());

    public static class StrV2Header extends StrHeader {
        public StrV2Header(@Nonnull byte[] abFrameData, int iDataSize) {
            super(abFrameData, iDataSize, 2);
        }

        @Override
        public @Nonnull BitStreamUncompressor_STRv2 makeNew(@Nonnull byte[] abBitstream, int iBitstreamSize)
                throws BinaryDataNotRecognized
        {
            return BitStreamUncompressor_STRv2.makeV2(abBitstream, iBitstreamSize);
        }
    }

    public static @Nonnull BitStreamUncompressor_STRv2 makeV2(@Nonnull byte[] abBitstream)
            throws BinaryDataNotRecognized
    {
        return makeV2(abBitstream, abBitstream.length);
    }
    public static @Nonnull BitStreamUncompressor_STRv2 makeV2(@Nonnull byte[] abBitstream, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_STRv2 bsu = makeV2NoThrow(abBitstream, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    static @CheckForNull BitStreamUncompressor_STRv2 makeV2NoThrow(@Nonnull byte[] abBitstream, int iDataSize)
    {
        StrV2Header header = new StrV2Header(abBitstream, iDataSize);
        if (!header.isValid())
            return null;
        ArrayBitReader bitReader = makeStrBitReader(abBitstream, iDataSize);

        return new BitStreamUncompressor_STRv2(header, bitReader);
    }

    /** 10 bits found at the end of STR v2 movies.
     * <pre>0111111111</pre> */
    private final static String END_OF_FRAME_EXTRA_BITS = "0111111111";
    private static final int PADDING_BITS_LENGTH = END_OF_FRAME_EXTRA_BITS.length();
    /** 10 bits found at the end of STR v2 movies.
     * <pre>0111111111</pre> */
    private static final int b0111111111 = 0x1ff;

    public static final IByteOrder LITTLE_ENDIAN_SHORT_ORDER = new IByteOrder() {
        @Override
        public int getByteOffset(int iByteIndex) {
            // flip the last bit so bytes are read in 16-bit little-endian
            return iByteIndex ^ 1;
        }
        @Override
        public int getPaddingByteAlign() {
            return 4;
        }
    };

    public static @Nonnull ArrayBitReader makeStrBitReader(@Nonnull byte[] abBitstream, int iDataSize) {
        return new ArrayBitReader(abBitstream, LITTLE_ENDIAN_SHORT_ORDER, StrHeader.SIZEOF, iDataSize);
    }

    @Nonnull
    private final StrV2Header _header;

    private BitStreamUncompressor_STRv2(@Nonnull StrV2Header header,
                                        @Nonnull ArrayBitReader bitReader)
    {
        super(bitReader, ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1,
              new QuantizationDcReader_STRv12(header.getQuantizationScale()),
              AC_ESCAPE_CODE_STR, FRAME_END_PADDING_BITS_STRV2);
        _header = header;
    }

    @Override
    public int getQuantizationScale() {
        return _header.getQuantizationScale();
    }

    public static class QuantizationDcReader_STRv12 implements IQuantizationDcReader {

        private final int _iFrameQuantizationScale;

        public QuantizationDcReader_STRv12(int iFrameQuantizationScale) {
            _iFrameQuantizationScale = iFrameQuantizationScale;
        }

        @Override
        public void readQuantizationScaleAndDc(@Nonnull ArrayBitReader bitReader, @Nonnull MdecContext context, @Nonnull MdecCode code)
                throws MdecException.ReadCorruption, MdecException.EndOfStream
        {
            code.setTop6Bits(_iFrameQuantizationScale);
            code.setBottom10Bits(bitReader.readSignedBits(10));
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(code.getBottom10Bits(), 10));
            assert !code.isEOD(); // a Qscale of 63 and DC of -512 would look like EOD
        }
    }

    private static class AcEscapeCode_STR implements IAcEscapeCode {
        @Override
        public void readAcEscapeCode(@Nonnull ArrayBitReader bitReader, @Nonnull MdecCode code)
                throws MdecException.EndOfStream
        {
            // Normal playstation encoding stores the escape code in 16 bits:
            // 6 for run of zeros, 10 for AC Coefficient
            int iRunAndAc = bitReader.readUnsignedBits(6 + 10);
            code.set(iRunAndAc);
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(iRunAndAc, 16));
        }
    }
    public static final IAcEscapeCode AC_ESCAPE_CODE_STR = new AcEscapeCode_STR();

    private static class FrameEndPaddingBits_STRv2 implements IFrameEndPaddingBits {
        @Override
        public boolean skipPaddingBits(@Nonnull ArrayBitReader bitReader) throws MdecException.EndOfStream {
            int iPaddingBits = bitReader.readUnsignedBits(PADDING_BITS_LENGTH);
            if (iPaddingBits != b0111111111) {
                LOG.log(Level.WARNING, "Incorrect padding bits {0} should be {1}",
                        new Object[]{Misc.bitsToString(iPaddingBits, PADDING_BITS_LENGTH),
                                     Misc.bitsToString(b0111111111, PADDING_BITS_LENGTH)});
                return false;
            }
            return true;
        }
    }
    public static final IFrameEndPaddingBits FRAME_END_PADDING_BITS_STRV2 = new FrameEndPaddingBits_STRv2();

    @Override
    public String toString() {
        return super.toString() + " Qscale=" + _header.getQuantizationScale();
    }

    @Override
    public @Nonnull BitStreamCompressor_STRv2 makeCompressor() {
        return new BitStreamCompressor_STRv2(_context.getTotalMacroBlocksRead(), getQuantizationScale());
    }

    /*########################################################################*/
    /*########################################################################*/
    /*########################################################################*/



    public static class BitStreamCompressor_STRv2 implements BitStreamCompressor, CommonBitStreamCompressing.BitStringEncoder {

        private final int _iMacroBlockCount;
        private final int _iOriginalQscale;
        private final int _iHeaderVersion;
        @Nonnull
        private final String _sTrailingBits;
        @Nonnull
        private final IByteOrder _byteOrder;

        private int _iQscale = -1;

        public BitStreamCompressor_STRv2(int iMacroBlockCount, int iOriginalQscale) {
            this(iMacroBlockCount, iOriginalQscale, 2, END_OF_FRAME_EXTRA_BITS, LITTLE_ENDIAN_SHORT_ORDER);
        }

        protected BitStreamCompressor_STRv2(int iMacroBlockCount, int iOriginalQscale, int iHeaderVersion) {
            this(iMacroBlockCount, iOriginalQscale, iHeaderVersion, END_OF_FRAME_EXTRA_BITS, LITTLE_ENDIAN_SHORT_ORDER);
        }

        protected BitStreamCompressor_STRv2(int iMacroBlockCount, int iOriginalQscale, @Nonnull IByteOrder byteOrder) {
            this(iMacroBlockCount, iOriginalQscale, 2, END_OF_FRAME_EXTRA_BITS, byteOrder);
        }

        protected BitStreamCompressor_STRv2(int iMacroBlockCount, int iOriginalQscale, int iHeaderVersion,
                                            @Nonnull String sTrailingBits, @Nonnull IByteOrder byteOrder)
        {
            _iMacroBlockCount = iMacroBlockCount;
            _iOriginalQscale = iOriginalQscale;
            _iHeaderVersion = iHeaderVersion;
            _sTrailingBits = sTrailingBits;
            _byteOrder = byteOrder;
        }

        @Override
        public @CheckForNull byte[] compressFull(int iMaxSize,
                                                 @Nonnull String sFrameDescription,
                                                 @Nonnull MdecEncoder encoder,
                                                 @Nonnull ILocalizedLogger log)
                throws IncompatibleException,
                       MdecException.EndOfStream, MdecException.ReadCorruption
        {
            return CommonBitStreamCompressing.singleQscaleCompressFull(iMaxSize, sFrameDescription, encoder, this, log);
        }

        @Override
        public @CheckForNull byte[] compressPartial(int iMaxSize,
                                                    @Nonnull String sFrameDescription,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull ILocalizedLogger log)
                throws IncompatibleException,
                       MdecException.EndOfStream, MdecException.ReadCorruption
        {
            return CommonBitStreamCompressing.singleQscaleCompressPartial(iMaxSize, sFrameDescription, encoder, this, _iOriginalQscale, log);
        }

        @Override
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws IncompatibleException,
                       MdecException.EndOfStream, MdecException.ReadCorruption, MdecException.TooMuchEnergy
        {
            _iQscale = -1; // qscale will be set on first block read

            BitStreamWriter bitStream = new BitStreamWriter();

            int iMdecCodeCount = CommonBitStreamCompressing.compress(bitStream, inStream, this, _iMacroBlockCount);

            bitStream.write(_sTrailingBits);

            byte[] abBitstream = bitStream.toByteArray(_byteOrder);
            byte[] abHeader = createHeader(iMdecCodeCount);

            byte[] abReturn = CommonBitStreamCompressing.joinByteArrays(abHeader, abBitstream);

            return abReturn;
        }

        @Override
        final public @Nonnull String encodeQscaleDc(@Nonnull MdecCode code, @Nonnull MdecBlock block)
                throws MdecException.TooMuchEnergy, IncompatibleException
        {
            int iQscale = code.getTop6Bits();
            if (_iQscale < 0)
                _iQscale = iQscale;
            else if (_iQscale != iQscale)
                throw new IncompatibleException(String.format("Inconsistent qscale value: current %d != new %d", _iQscale, iQscale));

            return encodeDC(code.getBottom10Bits(), block);
        }

        protected @Nonnull String encodeDC(int iDC, @Nonnull MdecBlock block) throws MdecException.TooMuchEnergy {
            if (iDC < -512 || iDC > 511)
                throw new IllegalArgumentException("Invalid DC code " + iDC);

            return Misc.bitsToString(iDC, 10);
        }

        @Override
        final public @Nonnull String encode0RlcAc(@Nonnull MdecCode code) {
            ZeroRunLengthAc match = ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1.lookup(code);

            if (match != null)
                return match.getBitString();

            // not a pre-defined code
            return ZeroRunLengthAcLookup_STR.ESCAPE_CODE.getBitString() +
                    Misc.bitsToString(code.getTop6Bits(), 6) +
                    Misc.bitsToString(code.getBottom10Bits(), 10);
        }

        private @Nonnull byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[StrHeader.SIZEOF];

            IO.writeInt16LE(ab, 0, Calc.calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(ab, 2, (short)0x3800);
            IO.writeInt16LE(ab, 4, (short)_iQscale);
            IO.writeInt16LE(ab, 6, (short)_iHeaderVersion);

            return ab;
        }

    }

}
