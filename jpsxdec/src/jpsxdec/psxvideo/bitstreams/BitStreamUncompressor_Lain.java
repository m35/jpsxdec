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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.i18n.I;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.MdecBlock;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.Misc;


/** Bitstream parser/decoder for the Serial Experiments Lain PlayStation game
 * demuxed video frames. */
public class BitStreamUncompressor_Lain extends BitStreamUncompressor {

    private static final int LAIN_FINAL_VIDEO_FRAME_COUNT = 4765;

    public static class LainHeader {

        private static final int SIZEOF = 8;

        int _iQscaleLuma = -1;
        int _iQscaleChroma = -1;
        int _iMagic3800orFrame = -1;
        int _iVlcCount = -1;

        private final boolean _blnIsValid;
        public LainHeader(@Nonnull byte[] abFrameData, int iDataSize) {
            if (iDataSize < 8) {
                _blnIsValid = false;
                return;
            }

            int iQscaleLuma       = abFrameData[0];
            int iQscaleChroma     = abFrameData[1];
            int iMagic3800orFrame = IO.readUInt16LE(abFrameData, 2);
            int iVlcCount         = IO.readSInt16LE(abFrameData, 4);
            int iVersion          = IO.readSInt16LE(abFrameData, 6);

            if (iQscaleChroma < 1 || iQscaleChroma > 63 ||
                iQscaleLuma < 1 || iQscaleLuma > 63 ||
                iVersion != 0 || iVlcCount < 1)
            {
                _blnIsValid = false;
                return;
            }

            // final Lain movie uses frame number instead of 0x3800
            if (iMagic3800orFrame != 0x3800 && (iMagic3800orFrame < 0 ||
                                                iMagic3800orFrame > LAIN_FINAL_VIDEO_FRAME_COUNT))
            {
                _blnIsValid = false;
                return;
            }

            _iQscaleLuma = iQscaleLuma;
            _iQscaleChroma = iQscaleChroma;
            _iMagic3800orFrame = iMagic3800orFrame;
            _iVlcCount = iVlcCount;

            _blnIsValid = true;
        }

        public boolean isValid() {
            return _blnIsValid;
        }

        public int getLumaQscale() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iQscaleLuma;
        }

        public int getChromaQscale() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iQscaleChroma;
        }

        public int getMagic3800orFrame() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iMagic3800orFrame;
        }

        public int getVlcCount() {
            if (!_blnIsValid) throw new IllegalStateException();
            return _iVlcCount;
        }
    }

    public static @Nonnull BitStreamUncompressor_Lain makeLain(@Nonnull byte[] abFrameData)
            throws BinaryDataNotRecognized
    {
        return makeLain(abFrameData, abFrameData.length);
    }
    public static @Nonnull BitStreamUncompressor_Lain makeLain(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_Lain bsu = makeLainNoThrow(abFrameData, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    public static final IByteOrder BIG_ENDIAN_ORDER = new IByteOrder() {
        @Override
        public int getByteOffset(int iByteIndex) {
            return iByteIndex;
        }
        @Override
        public int getPaddingByteAlign() {
            return 1;
        }
    };

    public static @CheckForNull BitStreamUncompressor_Lain makeLainNoThrow(@Nonnull byte[] abFrameData, int iDataSize)
    {
        LainHeader header = new LainHeader(abFrameData, iDataSize);
        if (!header.isValid())
            return null;

        ArrayBitReader bitReader = new ArrayBitReader(abFrameData, BIG_ENDIAN_ORDER, LainHeader.SIZEOF, iDataSize);

        return new BitStreamUncompressor_Lain(header, bitReader);
    }

    public static final ZeroRunLengthAc ESCAPE_CODE = new ZeroRunLengthAc(BitStreamCode._000001___________, true, false);
    public static final ZeroRunLengthAc END_OF_BLOCK = new ZeroRunLengthAc(BitStreamCode._10_______________,
            MdecCode.MDEC_END_OF_DATA_TOP6, MdecCode.MDEC_END_OF_DATA_BOTTOM10, false, true);

    /** The custom Serial Experiments Lain PlayStation game
     *  AC coefficient variable-length (Huffman) code table. */
    private final static ZeroRunLengthAcLookup AC_VARIABLE_LENGTH_CODES_LAIN = new ZeroRunLengthAcLookup.Builder()
      // Code               "Run" "Level"
        ._11s                ( 0  , 1  )
        ._011s               ( 0  , 2  )
        ._0100s              ( 1  , 1  )
        ._0101s              ( 0  , 3  )
        ._00101s             ( 0  , 4  )
        ._00110s             ( 2  , 1  )
        ._00111s             ( 0  , 5  )
        ._000100s            ( 0  , 6  )
        ._000101s            ( 3  , 1  )
        ._000110s            ( 1  , 2  )
        ._000111s            ( 0  , 7  )
        ._0000100s           ( 0  , 8  )
        ._0000101s           ( 4  , 1  )
        ._0000110s           ( 0  , 9  )
        ._0000111s           ( 5  , 1  )
        ._00100000s          ( 0  , 10 )
        ._00100001s          ( 0  , 11 )
        ._00100010s          ( 1  , 3  )
        ._00100011s          ( 6  , 1  )
        ._00100100s          ( 0  , 12 )
        ._00100101s          ( 0  , 13 )
        ._00100110s          ( 7  , 1  )
        ._00100111s          ( 0  , 14 )
        ._0000001000s        ( 0  , 15 )
        ._0000001001s        ( 2  , 2  )
        ._0000001010s        ( 8  , 1  )
        ._0000001011s        ( 1  , 4  )
        ._0000001100s        ( 0  , 16 )
        ._0000001101s        ( 0  , 17 )
        ._0000001110s        ( 9  , 1  )
        ._0000001111s        ( 0  , 18 )
        ._000000010000s      ( 0  , 19 )
        ._000000010001s      ( 1  , 5  )
        ._000000010010s      ( 0  , 20 )
        ._000000010011s      ( 10 , 1  )
        ._000000010100s      ( 0  , 21 )
        ._000000010101s      ( 3  , 2  )
        ._000000010110s      ( 12 , 1  )
        ._000000010111s      ( 0  , 23 )
        ._000000011000s      ( 0  , 22 )
        ._000000011001s      ( 11 , 1  )
        ._000000011010s      ( 0  , 24 )
        ._000000011011s      ( 0  , 28 )
        ._000000011100s      ( 0  , 25 )
        ._000000011101s      ( 1  , 6  )
        ._000000011110s      ( 2  , 3  )
        ._000000011111s      ( 0  , 27 )
        ._0000000010000s     ( 0  , 26 )
        ._0000000010001s     ( 13 , 1  )
        ._0000000010010s     ( 0  , 29 )
        ._0000000010011s     ( 1  , 7  )
        ._0000000010100s     ( 4  , 2  )
        ._0000000010101s     ( 0  , 31 )
        ._0000000010110s     ( 0  , 30 )
        ._0000000010111s     ( 14 , 1  )
        ._0000000011000s     ( 0  , 32 )
        ._0000000011001s     ( 0  , 33 )
        ._0000000011010s     ( 1  , 8  )
        ._0000000011011s     ( 0  , 35 )
        ._0000000011100s     ( 0  , 34 )
        ._0000000011101s     ( 5  , 2  )
        ._0000000011110s     ( 0  , 36 )
        ._0000000011111s     ( 0  , 37 )
        ._00000000010000s    ( 2  , 4  )
        ._00000000010001s    ( 1  , 9  )
        ._00000000010010s    ( 1  , 24 )
        ._00000000010011s    ( 0  , 38 )
        ._00000000010100s    ( 15 , 1  )
        ._00000000010101s    ( 0  , 39 )
        ._00000000010110s    ( 3  , 3  )
        ._00000000010111s    ( 7  , 3  )
        ._00000000011000s    ( 0  , 40 )
        ._00000000011001s    ( 0  , 41 )
        ._00000000011010s    ( 0  , 42 )
        ._00000000011011s    ( 0  , 43 )
        ._00000000011100s    ( 1  , 10 )
        ._00000000011101s    ( 0  , 44 )
        ._00000000011110s    ( 6  , 2  )
        ._00000000011111s    ( 0  , 45 )
        ._000000000010000s   ( 0  , 47 )
        ._000000000010001s   ( 0  , 46 )
        ._000000000010010s   ( 16 , 1  )
        ._000000000010011s   ( 2  , 5  )
        ._000000000010100s   ( 0  , 48 )
        ._000000000010101s   ( 1  , 11 )
        ._000000000010110s   ( 0  , 49 )
        ._000000000010111s   ( 0  , 51 )
        ._000000000011000s   ( 0  , 50 )
        ._000000000011001s   ( 7  , 2  )
        ._000000000011010s   ( 0  , 52 )
        ._000000000011011s   ( 4  , 3  )
        ._000000000011100s   ( 0  , 53 )
        ._000000000011101s   ( 17 , 1  )
        ._000000000011110s   ( 1  , 12 )
        ._000000000011111s   ( 0  , 55 )
        ._0000000000010000s  ( 0  , 54 )
        ._0000000000010001s  ( 0  , 56 )
        ._0000000000010010s  ( 0  , 57 )
        ._0000000000010011s  ( 21 , 1  )
        ._0000000000010100s  ( 0  , 58 )
        ._0000000000010101s  ( 3  , 4  )
        ._0000000000010110s  ( 1  , 13 )
        ._0000000000010111s  ( 23 , 1  )
        ._0000000000011000s  ( 8  , 2  )
        ._0000000000011001s  ( 0  , 59 )
        ._0000000000011010s  ( 2  , 6  )
        ._0000000000011011s  ( 19 , 1  )
        ._0000000000011100s  ( 0  , 60 )
        ._0000000000011101s  ( 9  , 2  )
        ._0000000000011110s  ( 24 , 1  )
        ._0000000000011111s  ( 18 , 1  )
        .add(ESCAPE_CODE)
        .add(END_OF_BLOCK)
        .build();

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    @Nonnull
    private final LainHeader _header;

    public BitStreamUncompressor_Lain(@Nonnull LainHeader header, @Nonnull ArrayBitReader bitReader) {
        super(bitReader, AC_VARIABLE_LENGTH_CODES_LAIN,
              new QuantizationDcReader_Lain(header.getLumaQscale(), header.getChromaQscale()),
              AC_ESCAPE_CODE_LAIN, FRAME_END_PADDING_BITS_NONE);
        _header = header;
    }

    public int getLumaQscale() {
        return _header.getLumaQscale();
    }

    public int getChromaQscale() {
        return _header.getChromaQscale();
    }

    private static class QuantizationDcReader_Lain implements IQuantizationDcReader {

        private final int _iFrameLumaQuantizationScale;
        private final int _iFrameChromaQuantizationScale;

        public QuantizationDcReader_Lain(int iFrameLumaQuantizationScale,
                                         int iFrameChromaQuantizationScale)
        {
            _iFrameLumaQuantizationScale = iFrameLumaQuantizationScale;
            _iFrameChromaQuantizationScale = iFrameChromaQuantizationScale;
        }

        @Override
        public void readQuantizationScaleAndDc(@Nonnull ArrayBitReader bitReader,
                                               @Nonnull MdecContext context,
                                               @Nonnull MdecCode code)
                throws MdecException.EndOfStream
        {
            code.setBottom10Bits(bitReader.readSignedBits(10) );
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(code.getBottom10Bits(), 10));
            if (context.getCurrentBlock().isChroma())
                code.setTop6Bits(_iFrameChromaQuantizationScale);
            else
                code.setTop6Bits(_iFrameLumaQuantizationScale);
        }
    }

    private static class AcEscapeCode_Lain implements IAcEscapeCode {

        @Override
        public void readAcEscapeCode(@Nonnull ArrayBitReader bitReader, @Nonnull MdecCode code) throws MdecException.EndOfStream
        {

            int iBits = bitReader.readUnsignedBits(6+8);

            // Get the (6 bit) run of zeros from the bits already read
            // 17 bits: eeeeeezzzzzz_____ : e = escape code, z = run of zeros
            code.setTop6Bits( (iBits >>> 8) & 63 );
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(code.getTop6Bits(), 6));

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
            // Chop down to the first 8 bits
            iBits = iBits & 0xff;
            int iACCoefficient;
            if (iBits == 0x00) {
                // If it's the special 00000000
                // Positive
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits("00000000");

                iACCoefficient = bitReader.readUnsignedBits(8);

                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(iACCoefficient, 8));

                code.setBottom10Bits(iACCoefficient);
            } else if (iBits  == 0x80) {
                // If it's the special 10000000
                // Negative
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits("10000000");

                iACCoefficient = -256 + bitReader.readUnsignedBits(8);

                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(iACCoefficient, 8));

                code.setBottom10Bits(iACCoefficient);
            } else {
                // Otherwise we already have the value
                assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(Misc.bitsToString(iBits, 8));

                // changed to signed
                iACCoefficient = (byte)iBits;

                code.setBottom10Bits(iACCoefficient);
            }

        }
    }
    private static final AcEscapeCode_Lain AC_ESCAPE_CODE_LAIN = new AcEscapeCode_Lain();

    @Override
    public String toString() {
        return super.toString() + " Qscale L=" + _header.getLumaQscale()
                                        +" C=" + _header.getChromaQscale();
    }


    @Override
    public @Nonnull BitStreamCompressor_Lain makeCompressor() {
        return new BitStreamCompressor_Lain(_context.getTotalMacroBlocksRead(),
                                            _header.getLumaQscale(),
                                            _header.getChromaQscale(),
                                            _header.getMagic3800orFrame());
    }

    // =========================================================================

    /** Lain videos luma:chroma ratio varies between 0.06 and 5.0.
     * Most are around the 1.0-2.0 range. */
    private static final double LUMA_TO_CHROMA_RATIO = 2.0;

    public static class BitStreamCompressor_Lain implements BitStreamCompressor, CommonBitStreamCompressing.BitStringEncoder {

        private final int _iMacroBlockCount;
        private final int _iMagic3800orFrame;
        private final int _iOriginalLumaQscale;
        private final int _iOriginalChromaQscale;

        public BitStreamCompressor_Lain(int iMacroBlockCount,
                                        int iOriginalLumaQscale, int iOriginalChromaQscale,
                                        int iMagic3800orFrame)
        {
            _iMacroBlockCount = iMacroBlockCount;
            _iMagic3800orFrame = iMagic3800orFrame;
            _iOriginalLumaQscale = iOriginalLumaQscale;
            _iOriginalChromaQscale = iOriginalChromaQscale;
        }

        @Override
        public @CheckForNull byte[] compressFull(int iMaxSize,
                                                 @Nonnull String sFrameDescription,
                                                 @Nonnull MdecEncoder encoder,
                                                 @Nonnull ILocalizedLogger log)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            int iLQscale = 1, iCQscale = 1;
            while (iLQscale < 64 && iCQscale < 64) {
                log.log(Level.INFO, I.TRYING_LUMA_CHROMA(iLQscale, iCQscale));

                int[] aiNewQscale = { iCQscale, iCQscale,
                                      iLQscale, iLQscale, iLQscale, iLQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToFullEncode(aiNewQscale);
                }

                try {
                    byte[] abNewDemux;
                    try {
                        abNewDemux = compress(encoder.getStream());
                    } catch (IncompatibleException ex) {
                        throw new RuntimeException("The encoder should be compatible here", ex);
                    }

                    int iNewDemuxSize = abNewDemux.length;
                    if (iNewDemuxSize <= iMaxSize) {
                        log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, iNewDemuxSize, iMaxSize));
                        return abNewDemux;
                    } else {
                        log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, iNewDemuxSize, iMaxSize));
                    }
                } catch (MdecException.TooMuchEnergy ex) {
                    log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(sFrameDescription), ex);
                }

                if ((iLQscale / (double)iCQscale) < LUMA_TO_CHROMA_RATIO)
                    iLQscale++;
                else
                    iCQscale++;
            }

            return null;
        }

        @Override
        public @CheckForNull byte[] compressPartial(int iMaxSize,
                                                    @Nonnull String sFrameDescription,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull ILocalizedLogger log)
                throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
        {
            final int iFrameLQscale = _iOriginalLumaQscale;
            final int iFrameCQscale = _iOriginalChromaQscale;
            final int[] aiOriginalQscale = { iFrameCQscale, iFrameCQscale, iFrameLQscale,
                                             iFrameLQscale, iFrameLQscale, iFrameLQscale };
            int iLQscale = iFrameLQscale, iCQscale = iFrameCQscale;
            while (iLQscale < 64 && iCQscale < 64) {
                log.log(Level.INFO, I.TRYING_LUMA_CHROMA(iLQscale, iCQscale));

                int[] aiNewQscale = { iCQscale, iCQscale,
                                      iLQscale, iLQscale, iLQscale, iLQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToPartialEncode(aiOriginalQscale, aiNewQscale);
                }

                try {
                    byte[] abNewDemux = compress(encoder.getStream());

                    if (abNewDemux.length <= iMaxSize) {
                        log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, abNewDemux.length, iMaxSize));
                        return abNewDemux;
                    } else {
                        log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, abNewDemux.length, iMaxSize));
                    }
                } catch (MdecException.TooMuchEnergy ex) {
                    log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(sFrameDescription), ex);
                }

                if ((iLQscale / (double)iCQscale) < LUMA_TO_CHROMA_RATIO)
                    iLQscale++;
                else
                    iCQscale++;
            }
            return null;
        }


        private int _iLumaQscale, _iChromaQscale;

        @Override
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws IncompatibleException, MdecException.EndOfStream,
                       MdecException.ReadCorruption, MdecException.TooMuchEnergy
        {
            _iLumaQscale = -1;
            _iChromaQscale = -1;
            BitStreamWriter bitStream = new BitStreamWriter();

            int iMdecCodeCount = CommonBitStreamCompressing.compress(bitStream, inStream, this, _iMacroBlockCount);

            byte[] abBitstream = bitStream.toByteArray(BitStreamUncompressor_Lain.BIG_ENDIAN_ORDER);
            byte[] abHeader = createHeader(iMdecCodeCount);

            byte[] abReturn = CommonBitStreamCompressing.joinByteArrays(abHeader, abBitstream);
            return abReturn;
        }

        @Override
        public @Nonnull String encodeQscaleDc(@Nonnull MdecCode code, @Nonnull MdecBlock mdecBlock)
                throws IncompatibleException
        {
            setBlockQscale(mdecBlock, code.getTop6Bits());

            int iDC = code.getBottom10Bits();
            if (iDC < -512 || iDC > 511)
                throw new IllegalArgumentException("Invalid DC code " + iDC);

            return Misc.bitsToString(iDC, 10);
        }

        protected void setBlockQscale(@Nonnull MdecBlock block, int iQscale) throws IncompatibleException {
            if (block.isChroma()) {
                if (_iChromaQscale < 0)
                    _iChromaQscale = iQscale;
                else if (_iChromaQscale != iQscale) {
                    throw new IncompatibleException(String.format(
                            "Inconsistent chroma qscale: current %d != new %d",
                            _iChromaQscale, iQscale));
                }
            } else {
                if (_iLumaQscale < 0)
                    _iLumaQscale = iQscale;
                else if (_iLumaQscale != iQscale) {
                    throw new IncompatibleException(String.format(
                            "Inconsistent luma qscale: current %d != new %d",
                            _iLumaQscale, iQscale));
                }
            }
        }

        @Override
        public @Nonnull String encode0RlcAc(@Nonnull MdecCode code) throws MdecException.TooMuchEnergy {
            ZeroRunLengthAc match = AC_VARIABLE_LENGTH_CODES_LAIN.lookup(code);

            if (match != null)
                return match.getBitString();

            // not a pre-defined code
            return encodeAcEscape(code);
        }

        private @Nonnull byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[LainHeader.SIZEOF];

            ab[0] = (byte)_iLumaQscale;
            ab[1] = (byte)_iChromaQscale;
            // Copy the 0x3800 or frame number from original frame
            IO.writeInt16LE(ab, 2, (short)_iMagic3800orFrame);
            IO.writeInt16LE(ab, 4, (short)iMdecCodeCount);
            IO.writeInt16LE(ab, 6, (short)0);

            return ab;
        }

        private @Nonnull String encodeAcEscape(@Nonnull MdecCode code)
                throws MdecException.TooMuchEnergy
        {
            String sTopBits = Misc.bitsToString(code.getTop6Bits(), 6);
            if (code.getBottom10Bits() == 0)
                throw new IllegalArgumentException("Invalid MDEC code to escape " + code);

            // Unlike any other bitstream, Lain's bitstream has a cap on
            // how much energy can fit in a frame.
            // The caller should increase the quantization scale and try again.
            if (code.getBottom10Bits() < -256 || code.getBottom10Bits() > 255)
                throw new MdecException.TooMuchEnergy(String.format(
                        "Unable to escape %s, AC code too large for Lain", code));

            if (code.getBottom10Bits() >= -127 && code.getBottom10Bits() <= 127) {
                return ESCAPE_CODE.getBitString() + sTopBits + Misc.bitsToString(code.getBottom10Bits(), 8);
            } else {
                if (code.getBottom10Bits() > 0) {
                    return ESCAPE_CODE.getBitString() + sTopBits + "00000000" + Misc.bitsToString(code.getBottom10Bits(), 8);
                } else {
                    return ESCAPE_CODE.getBitString() + sTopBits + "10000000" + Misc.bitsToString(code.getBottom10Bits()+256, 8);
                }
            }
        }

    }

}
