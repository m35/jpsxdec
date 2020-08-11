/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
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
public class BitStreamUncompressor_STRv2 extends BitStreamUncompressor {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_STRv2.class.getName());

    public static class StrV2Header extends StrHeader {
        public StrV2Header(byte[] abFrameData, int iDataSize) {
            super(abFrameData, iDataSize, 2);
        }

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

    /** 11 bits found at the end of STR v2 movies.
     * <pre>011 111 111 10</pre> */
    private final static String END_OF_FRAME_EXTRA_BITS = "01111111110";

    /** 11 bits found at the end of STR v2 movies.
     * <pre>011 111 111 10</pre> */
    private static final int b01111111110 = 0x3FE;


    public static @Nonnull ArrayBitReader makeStrBitReader(@Nonnull byte[] abBitstream, int iDataSize) {
        return new ArrayBitReader(abBitstream, iDataSize, true, 8);
    }

    @Nonnull
    private final StrV2Header _header;

    private BitStreamUncompressor_STRv2(@Nonnull StrV2Header header,
                                        @Nonnull ArrayBitReader bitReader)
    {
        super(bitReader, ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1, 
              new QuantizationDc_STRv12(header.getQuantizationScale()), new AcEscapeCode_STR(),
              new FrameEndPaddingBits_STRv2());
        _header = header;
    }

    public static class QuantizationDc_STRv12 implements IQuantizationDc {

        private final int _iFrameQuantizationScale;

        public QuantizationDc_STRv12(int iFrameQuantizationScale) {
            _iFrameQuantizationScale = iFrameQuantizationScale;
        }

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
        public void skipPaddingBits(@Nonnull ArrayBitReader bitReader) throws MdecException.EndOfStream {
            int iPaddingBits = bitReader.readUnsignedBits(11);
            if (iPaddingBits != b01111111110)
                LOG.log(Level.WARNING, "Incorrect padding bits {0}", Misc.bitsToString(iPaddingBits, 11));
        }
    }
    public static final IFrameEndPaddingBits FRAME_END_PADDING_BITS_STRV2 = new FrameEndPaddingBits_STRv2();

    @Override
    public String toString() {
        return super.toString() + " Qscale=" + _header.getQuantizationScale();
    }

    @Override
    public @Nonnull BitStreamCompressor_STRv2 makeCompressor() {
        return new BitStreamCompressor_STRv2(_context.getTotalMacroBlocksRead());
    }

    /*########################################################################*/
    /*########################################################################*/
    /*########################################################################*/



    public static class BitStreamCompressor_STRv2 implements BitStreamCompressor {

        private final int _iMacroBlockCount;
        private int _iQscale;
        private int _iMdecCodeCount;

        public BitStreamCompressor_STRv2(int iMacroBlockCount) {
            _iMacroBlockCount = iMacroBlockCount;
        }

        public @CheckForNull byte[] compressFull(@Nonnull byte[] abOriginal,
                                                 @Nonnull String sFrameDescription,
                                                 @Nonnull MdecEncoder encoder, 
                                                 @Nonnull ILocalizedLogger log)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            for (int iQscale = 1; iQscale < 64; iQscale++) {
                log.log(Level.INFO, I.TRYING_QSCALE(iQscale));

                int[] aiNewQscale = { iQscale, iQscale, iQscale,
                                      iQscale, iQscale, iQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToFullEncode(aiNewQscale);
                }

                byte[] abNewDemux;
                try {
                    abNewDemux = compress(encoder.getStream());
                } catch (IncompatibleException ex) {
                    throw new RuntimeException("The encoder should be compatible here", ex);
                } catch (MdecException.TooMuchEnergy ex) {
                    throw new RuntimeException("This should not happen with STRv2", ex);
                }
                if (abNewDemux.length <= abOriginal.length) {
                    log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, abNewDemux.length, abOriginal.length));
                    return abNewDemux;
                } else {
                    log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, abNewDemux.length, abOriginal.length));
                }
            }
            return null;
        }

        public @CheckForNull byte[] compressPartial(@Nonnull byte[] abOriginal,
                                                    @Nonnull String sFrameDescription,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull ILocalizedLogger log)
                throws LocalizedIncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
        {
            final int iFrameQscale = getFrameQscale(abOriginal);
            int[] aiOriginalQscale = { iFrameQscale, iFrameQscale, iFrameQscale,
                                       iFrameQscale, iFrameQscale, iFrameQscale };
            
            for (int iNewQscale = iFrameQscale; iNewQscale < 64; iNewQscale++) {
                log.log(Level.INFO, I.TRYING_QSCALE(iNewQscale));

                int[] aiNewQscale = { iNewQscale, iNewQscale, iNewQscale,
                                      iNewQscale, iNewQscale, iNewQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToPartialEncode(aiOriginalQscale, aiNewQscale);
                }

                byte[] abNewDemux;
                try {
                    abNewDemux = compress(encoder.getStream());
                } catch (IncompatibleException ex) {
                    throw new RuntimeException("The encoder should be compatible here", ex);
                } catch (MdecException.TooMuchEnergy ex) {
                    throw new RuntimeException("This should not happen with STRv2", ex);
                }
                if (abNewDemux.length <= abOriginal.length) {
                    log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, abNewDemux.length, abOriginal.length));
                    return abNewDemux;
                } else {
                    log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, abNewDemux.length, abOriginal.length));
                }
            }
            return null;
        }

        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws IncompatibleException, MdecException.EndOfStream,
                       MdecException.ReadCorruption, MdecException.TooMuchEnergy
        {
            _iMdecCodeCount = -1;
            _iQscale = -1; // qscale will be set on first block read

            BitStreamWriter bitStream = new BitStreamWriter();
            bitStream.setLittleEndian(isBitstreamLittleEndian());

            final MdecCode code = new MdecCode();
            MdecContext context = new MdecContext();

            while (context.getTotalMacroBlocksRead() < _iMacroBlockCount) {
                String sBitsToWrite;
                boolean blnEod = inStream.readMdecCode(code);
                if (!code.isValid())
                    throw new MdecException.ReadCorruption("Invalid MDEC code " + code);
                if (blnEod) {
                    sBitsToWrite = ZeroRunLengthAcLookup_STR.END_OF_BLOCK.getBitString();
                    context.nextCodeEndBlock();
                } else {
                    if (context.atStartOfBlock()) {
                        setBlockQscale(context.getCurrentBlock(), code.getTop6Bits());
                        sBitsToWrite = encodeDC(code.getBottom10Bits(), context.getCurrentBlock());
                    } else {
                        sBitsToWrite = encodeAC(code);
                    }
                    context.nextCode();
                }
                if (BitStreamDebugging.DEBUG)
                    System.out.println("Converting " + code.toString() + " to " + sBitsToWrite + " at " + bitStream.getCurrentWordPosition());
                bitStream.write(sBitsToWrite);
            }

            if (!context.atStartOfBlock())
                throw new IllegalStateException("Ended compressing in the middle of a macroblock.");

            addTrailingBits(bitStream);
            byte[] abBitstream = bitStream.toByteArray();
            byte[] abHeader = createHeader(context.getTotalMdecCodesRead());
            byte[] abReturn = new byte[abHeader.length + abBitstream.length];
            System.arraycopy(abHeader, 0, abReturn, 0, abHeader.length);
            System.arraycopy(abBitstream, 0, abReturn, abHeader.length, abBitstream.length);

            _iMdecCodeCount = context.getTotalMdecCodesRead();
            return abReturn;
        }

        public int getMdecCodesFromLastCompress() {
            return _iMdecCodeCount;
        }

        protected boolean isBitstreamLittleEndian() {
            return true;
        }

        protected void addTrailingBits(@Nonnull BitStreamWriter bitStream) {
            bitStream.write(END_OF_FRAME_EXTRA_BITS);
        }

        /** Sets the quantization scale for the current block being encoded.
         * Performs any necessary preparations for encoding the block.
         * Ensures the quantization scale is compatible with the bitstream.
         * Caller will ensure parameters are valid. */
        protected void setBlockQscale(@Nonnull MdecBlock block, int iQscale) throws IncompatibleException {
            if (_iQscale < 0)
                _iQscale = iQscale;
            else if (_iQscale != iQscale)
                throw new IncompatibleException(String.format(
                        "Inconsistent qscale scale: current %d != new %d",
                        _iQscale, iQscale));
        }

        protected @Nonnull String encodeDC(int iDC, @Nonnull MdecBlock block) throws MdecException.TooMuchEnergy {
            if (iDC < -512 || iDC > 511)
                throw new IllegalArgumentException("Invalid DC code " + iDC);

            return Misc.bitsToString(iDC, 10);
        }

        private String encodeAC(@Nonnull MdecCode code) 
                throws MdecException.TooMuchEnergy
        {
            if (!code.isValid())
                throw new IllegalArgumentException("Invalid MDEC code " + code);

            for (ZeroRunLengthAc vlc : getAcVaribleLengthCodeList()) {
                if (vlc.equalsMdec(code))
                    return vlc.getBitString();
            }
            // not a pre-defined code
            return encodeAcEscape(code);
        }

        protected @Nonnull ZeroRunLengthAcLookup getAcVaribleLengthCodeList() {
            return ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1;
        }

        protected @Nonnull String encodeAcEscape(@Nonnull MdecCode code)
                throws MdecException.TooMuchEnergy
        {
            if (!code.isValid())
                throw new IllegalArgumentException("Invalid MDEC code " + code);

            return ZeroRunLengthAcLookup_STR.ESCAPE_CODE.getBitString() +
                    Misc.bitsToString(code.getTop6Bits(), 6) +
                    Misc.bitsToString(code.getBottom10Bits(), 10);
        }

        protected @Nonnull byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[8];

            IO.writeInt16LE(ab, 0, Calc.calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(ab, 2, (short)0x3800);
            IO.writeInt16LE(ab, 4, (short)_iQscale);
            IO.writeInt16LE(ab, 6, (short)getHeaderVersion());

            return ab;
        }

        protected int getHeaderVersion() { return 2; }
        
        protected int getFrameQscale(@Nonnull byte[] abFrameData) throws LocalizedIncompatibleException {
            StrV2Header header = new StrV2Header(abFrameData, abFrameData.length);
            if (!header.isValid())
                throw new LocalizedIncompatibleException(I.FRAME_IS_NOT_BITSTREAM_FORMAT("STRv2"));
            return header.getQuantizationScale();
        }
    }

}
