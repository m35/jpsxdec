/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ILocalizedLogger;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.LocalizedIncompatibleException;


/** Bitstream parser/decoder for the Serial Experiments Lain PlayStation game
 * demuxed video frames.
 * <p>
 * WARNING: The public methods are NOT thread safe. Create a separate
 * instance of this class for each thread, or wrap the calls with synchronize. */
public class BitStreamUncompressor_Lain extends BitStreamUncompressor {

    /** The custom Serial Experiments Lain PlayStation game
     *  AC coefficient variable-length (Huffman) code table. */
    private final static AcLookup AC_VARIABLE_LENGTH_CODES_LAIN = new AcLookup()
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
        ._0000000000011111s  ( 18 , 1  );

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    // frame header info
    private int _iQscaleChroma = -1;
    private int _iQscaleLuma = -1;
    private int _iVlcCount = -1;
    private int _iMagic3800orFrame = -1;

    public BitStreamUncompressor_Lain() {
        super(AC_VARIABLE_LENGTH_CODES_LAIN);
    }

    @Override
    protected boolean readHeader(@Nonnull byte[] abFrameData, int iDataSize,
                                 @Nonnull ArrayBitReader bitReader)
    {
        if (iDataSize < 8)
            return false;
        
        _iQscaleLuma             = abFrameData[0];
        _iQscaleChroma           = abFrameData[1];
        _iMagic3800orFrame       = IO.readUInt16LE(abFrameData, 2);
        _iVlcCount               = IO.readSInt16LE(abFrameData, 4);
        int iVersion             = IO.readSInt16LE(abFrameData, 6);

        if (_iQscaleChroma < 1 || _iQscaleLuma < 1 ||
                 iVersion != 0 || _iVlcCount < 1)
            return false;

        // final Lain movie uses frame number instead of 0x3800
        if (_iMagic3800orFrame != 0x3800 && (_iMagic3800orFrame < 0 || _iMagic3800orFrame > 4765))
            return false;

        bitReader.reset(abFrameData, iDataSize, false, 8);
        return true;
    }
    
    public static boolean checkHeader(@Nonnull byte[] abFrameData) {
        if (abFrameData.length < 8)
            return false;

        int _iQscaleLuma            = abFrameData[0];
        int _iQscaleChroma          = abFrameData[1];
        int _iMagic3800orFrame      = IO.readUInt16LE(abFrameData, 2);
        int _iVlcCount              = IO.readSInt16LE(abFrameData, 4);
        int iVersion                = IO.readSInt16LE(abFrameData, 6);

        if (_iQscaleChroma < 1 || _iQscaleLuma < 1 ||
                 iVersion != 0 || _iVlcCount < 1)
            return false;

        // final Lain movie uses frame number instead of 0x3800
        if (_iMagic3800orFrame != 0x3800 && (_iMagic3800orFrame < 0 || _iMagic3800orFrame > 4765))
            return false;

        return true;
    }

    /** @return int[2] array: {luma, chroma} */
    public static @Nonnull int[] getQscale(@Nonnull byte[] abFrameData)
            throws BinaryDataNotRecognized
    {
        if (!checkHeader(abFrameData))
            throw new BinaryDataNotRecognized();
        
        return new int[] { abFrameData[0], abFrameData[1] };
    }

    @Override
    protected void readQscaleAndDC(@Nonnull MdecCode code) throws MdecException.EndOfStream {
        code.setBottom10Bits( _bitReader.readSignedBits(10) );
        assert !DEBUG || _debug.append(Misc.bitsToString(code.getBottom10Bits(), 10));
        if (getCurrentMacroBlockSubBlock() < 2)
            code.setTop6Bits(_iQscaleChroma);
        else
            code.setTop6Bits(_iQscaleLuma);
    }


    
    protected void readEscapeAcCode(@Nonnull MdecCode code) throws MdecException.EndOfStream
    {

        int iBits = _bitReader.readUnsignedBits(6+8);

        // Get the (6 bit) run of zeros from the bits already read
        // 17 bits: eeeeeezzzzzz_____ : e = escape code, z = run of zeros
        code.setTop6Bits( (iBits >>> 8) & 63 );
        assert !DEBUG || _debug.append(Misc.bitsToString(code.getTop6Bits(), 6));

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
            assert !DEBUG || _debug.append("00000000");

            iACCoefficient = _bitReader.readUnsignedBits(8);

            assert !DEBUG || _debug.append(Misc.bitsToString(iACCoefficient, 8));

            code.setBottom10Bits(iACCoefficient);
        } else if (iBits  == 0x80) {
            // If it's the special 10000000
            // Negative
            assert !DEBUG || _debug.append("10000000");

            iACCoefficient = -256 + _bitReader.readUnsignedBits(8);

            assert !DEBUG || _debug.append(Misc.bitsToString(iACCoefficient, 8));

            code.setBottom10Bits(iACCoefficient);
        } else {
            // Otherwise we already have the value
            assert !DEBUG || _debug.append(Misc.bitsToString(iBits, 8));

            // changed to signed
            iACCoefficient = (byte)iBits;
            
            code.setBottom10Bits(iACCoefficient);
        }

    }

    public int getLumaQscale() {
        return _iQscaleLuma;
    }

    public int getChromaQscale() {
        return _iQscaleChroma;
    }

    public int getMagic3800orFrame() {
        return _iMagic3800orFrame;
    }

    @Override
    public void skipPaddingBits() {
        // Lain doesn't have ending padding bits
    }

    @Override
    public String getName() {
        return "Lain";
    }

    public String toString() {
        if (_iQscaleChroma == -1)
            return getName();
        return String.format("%s Qscale L=%d C=%d 3800=%x Offset=%d MB=%d.%d Mdec count=%d",
                getName(), getLumaQscale(), getChromaQscale(),
                getMagic3800orFrame(),
                _bitReader.getWordPosition(),
                getCurrentMacroBlock(), getCurrentMacroBlockSubBlock(),
                getMdecCodeCount());
    }


    @Override
    public @Nonnull BitStreamCompressor_Lain makeCompressor() {
        return new BitStreamCompressor_Lain();
    }

    // =========================================================================
    
    /** Lain videos luma:chroma ratio varies between 0.06 and 5.0.
     * Most are around the 1.0-2.0 range. */
    private static final double LUMA_TO_CHROMA_RATIO = 2.0;

    public static class BitStreamCompressor_Lain extends BitStreamCompressor_STRv2 {

        @Override
        public @CheckForNull byte[] compressFull(@Nonnull byte[] abOriginal,
                                                 @Nonnull String frameNum,
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
                        abNewDemux = compress(encoder.getStream(), encoder.getPixelWidth(), encoder.getPixelHeight());
                    } catch (IncompatibleException ex) {
                        throw new RuntimeException("The encoder should be compatible here", ex);
                    }

                    int iNewDemuxSize = abNewDemux.length;
                    if (iNewDemuxSize <= abOriginal.length) {
                        log.log(Level.INFO, I.NEW_FRAME_FITS(frameNum, iNewDemuxSize, abOriginal.length));
                        return abNewDemux;
                    } else {
                        log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(frameNum, iNewDemuxSize, abOriginal.length));
                    }
                } catch (MdecException.TooMuchEnergy ex) {
                    log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(frameNum), ex);
                }

                if ((iLQscale / (double)iCQscale) < LUMA_TO_CHROMA_RATIO)
                    iLQscale++;
                else
                    iCQscale++;
            }

            return null;
        }

        @Override
        public @CheckForNull byte[] compressPartial(@Nonnull byte[] abOriginal,
                                                    @Nonnull String frameNum,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull ILocalizedLogger log)
                throws LocalizedIncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
        {
            final int[] aiFrameQscale;
            try {
                aiFrameQscale = BitStreamUncompressor_Lain.getQscale(abOriginal);
            } catch (BinaryDataNotRecognized ex) {
                throw new LocalizedIncompatibleException(I.FRAME_NOT_LAIN(), ex);
            }
            final int iFrameLQscale = aiFrameQscale[0];
            final int iFrameCQscale = aiFrameQscale[1];
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
                    byte[] abNewDemux;
                    try {
                        abNewDemux = compress(encoder.getStream(), encoder.getPixelWidth(), encoder.getPixelHeight());
                    } catch (IncompatibleException ex) {
                        throw new RuntimeException("The encoder should be compatible here", ex);
                    }

                    if (abNewDemux.length <= abOriginal.length) {
                        log.log(Level.INFO, I.NEW_FRAME_FITS(frameNum, abNewDemux.length, abOriginal.length));
                        return abNewDemux;
                    } else {
                        log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(frameNum, abNewDemux.length, abOriginal.length));
                    }
                } catch (MdecException.TooMuchEnergy ex) {
                    log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(frameNum), ex);
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
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream,
                                        int iWidth, int iHeight)
                throws IncompatibleException, MdecException.EndOfStream,
                       MdecException.ReadCorruption, MdecException.TooMuchEnergy
        {
            _iLumaQscale = -1;
            _iChromaQscale = -1;
            return super.compress(inStream, iWidth, iHeight);
        }

        @Override
        protected void setBlockQscale(int iBlock, int iQscale) throws IncompatibleException {
            if (iBlock < 2) {
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
        protected @Nonnull byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[8];

            ab[0] = (byte)_iLumaQscale;
            ab[1] = (byte)_iChromaQscale;
            // If this is the final movie, this should actually be the frame number.
            // This value will be corrected if the sector data is replaced using
            // SectorLainVideo class.
            IO.writeInt16LE(ab, 2, (short)0x3800);
            IO.writeInt16LE(ab, 4, (short)iMdecCodeCount);
            IO.writeInt16LE(ab, 6, (short)getHeaderVersion());

            return ab;
        }

        @Override
        protected boolean isBitstreamLittleEndian() {
            return false;
        }

        @Override
        protected int getHeaderVersion() {
            return 0;
        }

        @Override
        protected @Nonnull AcLookup getAcVaribleLengthCodeList() {
            return AC_VARIABLE_LENGTH_CODES_LAIN;
        }

        @Override
        protected @Nonnull String encodeAcEscape(@Nonnull MdecCode code)
                throws MdecException.TooMuchEnergy
        {
            String sTopBits = Misc.bitsToString(code.getTop6Bits(), 6);
            if (code.getBottom10Bits() == 0)
                throw new IllegalArgumentException("Invalid MDEC code to escape " + code);

            // Unlike any other bitream, Lain's bitstream has a cap on
            // how much energy can fit in a frame.
            // The caller should increase the quanitation scale and try again.
            if (code.getBottom10Bits() < -256 || code.getBottom10Bits() > 255)
                throw new MdecException.TooMuchEnergy(String.format(
                        "Unable to escape %s, AC code too large for Lain", code));
            
            if (code.getBottom10Bits() >= -127 && code.getBottom10Bits() <= 127) {
                return AcLookup.ESCAPE_CODE.BitString + sTopBits + Misc.bitsToString(code.getBottom10Bits(), 8);
            } else {
                if (code.getBottom10Bits() > 0) {
                    return AcLookup.ESCAPE_CODE.BitString + sTopBits + "00000000" + Misc.bitsToString(code.getBottom10Bits(), 8);
                } else {
                    return AcLookup.ESCAPE_CODE.BitString + sTopBits + "10000000" + Misc.bitsToString(code.getBottom10Bits()+256, 8);
                }
            }
        }

        @Override
        protected void addTrailingBits(BitStreamWriter bitStream) {
            // do nothing
        }

    }
    
}
