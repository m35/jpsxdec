/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Bitstream parser/decoder for the very common "version 2" demuxed video
 * frames, used by most games.
 * <p>
 * WARNING: The public methods are NOT thread safe. Create a separate
 * instance of this class for each thread, or wrap the calls with synchronize. */
public class BitStreamUncompressor_STRv2 extends BitStreamUncompressor {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_STRv2.class.getName());
    
    /** STR v2, v3, FF7, and .iki AC coefficient variable-length (Huffman) codes.
     * Conveniently identical to MPEG1. */
    private static final AcLookup AC_VARIABLE_LENGTH_CODES_MPEG1 = new AcLookup()
                 //  Code        "Run" "Level"
                 // Table 1
                ._11s              (0 ,  1)
                ._011s             (1 ,  1)
                ._0100s            (0 ,  2)
                ._0101s            (2 ,  1)
                ._00101s           (0 ,  3)
                ._00110s           (4 ,  1)
                ._00111s           (3 ,  1)
                ._000100s          (7 ,  1)
                ._000101s          (6 ,  1)
                ._000110s          (1 ,  2)
                ._000111s          (5 ,  1)
                ._0000100s         (2 ,  2)
                ._0000101s         (9 ,  1)
                ._0000110s         (0 ,  4)
                ._0000111s         (8 ,  1)
                ._00100000s        (13,  1)
                ._00100001s        (0 ,  6)
                ._00100010s        (12,  1)
                ._00100011s        (11,  1)
                ._00100100s        (3 ,  2)
                ._00100101s        (1 ,  3)
                ._00100110s        (0 ,  5)
                ._00100111s        (10,  1)
                // Table 2
                ._0000001000s      (16,  1)
                ._0000001001s      (5 ,  2)
                ._0000001010s      (0 ,  7)
                ._0000001011s      (2 ,  3)
                ._0000001100s      (1 ,  4)
                ._0000001101s      (15,  1)
                ._0000001110s      (14,  1)
                ._0000001111s      (4 ,  2)
                ._000000010000s    (0 , 11)
                ._000000010001s    (8 ,  2)
                ._000000010010s    (4 ,  3)
                ._000000010011s    (0 , 10)
                ._000000010100s    (2 ,  4)
                ._000000010101s    (7 ,  2)
                ._000000010110s    (21,  1)
                ._000000010111s    (20,  1)
                ._000000011000s    (0 ,  9)
                ._000000011001s    (19,  1)
                ._000000011010s    (18,  1)
                ._000000011011s    (1 ,  5)
                ._000000011100s    (3 ,  3)
                ._000000011101s    (0 ,  8)
                ._000000011110s    (6 ,  2)
                ._000000011111s    (17,  1)
                ._0000000010000s   (10,  2)
                ._0000000010001s   (9 ,  2)
                ._0000000010010s   (5 ,  3)
                ._0000000010011s   (3 ,  4)
                ._0000000010100s   (2 ,  5)
                ._0000000010101s   (1 ,  7)
                ._0000000010110s   (1 ,  6)
                ._0000000010111s   (0 , 15)
                ._0000000011000s   (0 , 14)
                ._0000000011001s   (0 , 13)
                ._0000000011010s   (0 , 12)
                ._0000000011011s   (26,  1)
                ._0000000011100s   (25,  1)
                ._0000000011101s   (24,  1)
                ._0000000011110s   (23,  1)
                ._0000000011111s   (22,  1)
                // Table 3
                ._00000000010000s  (0 , 31)
                ._00000000010001s  (0 , 30)
                ._00000000010010s  (0 , 29)
                ._00000000010011s  (0 , 28)
                ._00000000010100s  (0 , 27)
                ._00000000010101s  (0 , 26)
                ._00000000010110s  (0 , 25)
                ._00000000010111s  (0 , 24)
                ._00000000011000s  (0 , 23)
                ._00000000011001s  (0 , 22)
                ._00000000011010s  (0 , 21)
                ._00000000011011s  (0 , 20)
                ._00000000011100s  (0 , 19)
                ._00000000011101s  (0 , 18)
                ._00000000011110s  (0 , 17)
                ._00000000011111s  (0 , 16)
                ._000000000010000s (0 , 40)
                ._000000000010001s (0 , 39)
                ._000000000010010s (0 , 38)
                ._000000000010011s (0 , 37)
                ._000000000010100s (0 , 36)
                ._000000000010101s (0 , 35)
                ._000000000010110s (0 , 34)
                ._000000000010111s (0 , 33)
                ._000000000011000s (0 , 32)
                ._000000000011001s (1 , 14)
                ._000000000011010s (1 , 13)
                ._000000000011011s (1 , 12)
                ._000000000011100s (1 , 11)
                ._000000000011101s (1 , 10)
                ._000000000011110s (1 ,  9)
                ._000000000011111s (1 ,  8)
                ._0000000000010000s(1 , 18)
                ._0000000000010001s(1 , 17)
                ._0000000000010010s(1 , 16)
                ._0000000000010011s(1 , 15)
                ._0000000000010100s(6 ,  3)
                ._0000000000010101s(16,  2)
                ._0000000000010110s(15,  2)
                ._0000000000010111s(14,  2)
                ._0000000000011000s(13,  2)
                ._0000000000011001s(12,  2)
                ._0000000000011010s(11,  2)
                ._0000000000011011s(31,  1)
                ._0000000000011100s(30,  1)
                ._0000000000011101s(29,  1)
                ._0000000000011110s(28,  1)
                ._0000000000011111s(27,  1);

    /** 11 bits found at the end of STR v2 movies.
     * <pre>011 111 111 10</pre> */
    private final static String END_OF_FRAME_EXTRA_BITS = "01111111110";

    /** 11 bits found at the end of STR v2 movies.
     * <pre>011 111 111 10</pre> */
    private static final int b01111111110 = 0x3FE;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /** Frame's quantization scale. */
    protected int _iQscale = -1;
    protected int _iHalfVlcCountCeil32 = -1;

    public BitStreamUncompressor_STRv2() {
        super(AC_VARIABLE_LENGTH_CODES_MPEG1);
    }

    protected void readHeader(@Nonnull byte[] abFrameData, int iDataSize,
                              @Nonnull ArrayBitReader bitReader)
            throws NotThisTypeException
    {
        if (iDataSize < 8)
            throw new NotThisTypeException();
        
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800       = IO.readUInt16LE(abFrameData, 2);
        _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion         = IO.readSInt16LE(abFrameData, 6);

        if (iMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 2  || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, iDataSize, true, 8);
    }

    /** Returns if this is a v2 frame. */
    public static boolean checkHeader(@Nonnull byte[] abFrameData) {
        if (abFrameData.length < 8)
            return false;

        int _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800           = IO.readUInt16LE(abFrameData, 2);
        int _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion             = IO.readSInt16LE(abFrameData, 6);

        return !(iMagic3800 != 0x3800 || _iQscale < 1 ||
                 iVersion != 2 || _iHalfVlcCountCeil32 < 0);
    }

    public static int getQscale(@Nonnull byte[] abFrameData) throws MdecException.Uncompress {
        if (!checkHeader(abFrameData))
            throw new MdecException.Uncompress(I.FRAME_NOT_STRV2());

        return IO.readSInt16LE(abFrameData, 4);
    }

    protected void readQscaleAndDC(@Nonnull MdecCode code)
            throws MdecException.Uncompress, MdecException.EndOfStream
    {
        code.setTop6Bits(_iQscale);
        code.setBottom10Bits(_bitReader.readSignedBits(10));
        assert !DEBUG || _debug.append(Misc.bitsToString(code.getBottom10Bits(), 10));
        assert !code.isEOD(); // a Qscale of 63 and DC of -512 would look like EOD
    }

    protected void readEscapeAcCode(@Nonnull MdecCode code) throws MdecException.EndOfStream {
        // Normal playstation encoding stores the escape code in 16 bits:
        // 6 for run of zeros, 10 for AC Coefficient
        int iRunAndAc = _bitReader.readUnsignedBits(6 + 10);
        code.set(iRunAndAc);
        assert !DEBUG || _debug.append(Misc.bitsToString(iRunAndAc, 16));

        // Ignore AC == 0 coefficients.
        // (I consider this an error, but FF7 has these codes,
        // so clearly the MDEC can handle it.)
        if (code.getBottom10Bits() == 0) {
            LOG.info("Escape code has 0 AC coefficient.");
        }
    }

    @Override
    public void skipPaddingBits() throws MdecException.EndOfStream {
        int iPaddingBits = _bitReader.readUnsignedBits(11);
        if (iPaddingBits != b01111111110)
            LOG.log(Level.WARNING, "Incorrect padding bits {0}", Misc.bitsToString(iPaddingBits, 11));
    }

    @Override
    public @Nonnull BitStreamCompressor_STRv2 makeCompressor() {
        return new BitStreamCompressor_STRv2();
    }

    @Override
    public @Nonnull String getName() {
        return "STRv2";
    }


    /** A strange value needed for video bitstreams and video sector headers.
     *  It's the number of MDEC codes, divided by two, then rounded up to the
     *  next closest multiple of 32 (if not already a multiple of 32).
     *  In other words, its the number of 32-byte blocks it would take to hold
     *  the MDEC codes. */
    public static short calculateHalfCeiling32(int iMdecCodeCount) {
        return (short) ((((iMdecCodeCount + 1) / 2) + 31) & ~31);
    }
    

    public String toString() {
        if (_iQscale == -1)
            return getName();
        return String.format("%s Qscale=%d, Current Offset=%d, Current MB.Blk=%d.%d, MDEC count=%d",
                getName(), _iQscale,
                _bitReader.getWordPosition(),
                getCurrentMacroBlock(), getCurrentMacroBlockSubBlock(),
                getMdecCodeCount());
    }

    /*########################################################################*/
    /*########################################################################*/
    /*########################################################################*/



    public static class BitStreamCompressor_STRv2 implements BitStreamCompressor {
        
        private int _iQscale;
        private int _iMdecCodeCount;

        public @CheckForNull byte[] compressFull(@Nonnull byte[] abOriginal,
                                                 @Nonnull FrameNumber frame,
                                                 @Nonnull MdecEncoder encoder, 
                                                 @Nonnull FeedbackStream fbs)
                throws MdecException
        {
            for (int iQscale = 1; iQscale < 64; iQscale++) {
                fbs.println(I.TRYING_QSCALE(iQscale));

                int[] aiNewQscale = { iQscale, iQscale, iQscale,
                                      iQscale, iQscale, iQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToFullEncode(aiNewQscale);
                }

                byte[] abNewDemux = compress(encoder.getStream(), encoder.getPixelWidth(), encoder.getPixelHeight());
                if (abNewDemux.length <= abOriginal.length) {
                    fbs.indent1().println(I.NEW_FRAME_FITS(frame, abNewDemux.length, abOriginal.length));
                    return abNewDemux;
                } else {
                    fbs.indent1().println(I.NEW_FRAME_DOES_NOT_FIT(frame, abNewDemux.length, abOriginal.length));
                }
            }
            return null;
        }

        public @CheckForNull byte[] compressPartial(@Nonnull byte[] abOriginal,
                                                    @Nonnull FrameNumber frame,
                                                    @Nonnull MdecEncoder encoder,
                                                    @Nonnull FeedbackStream fbs)
                throws MdecException
        {
            final int iFrameQscale = getQscale(abOriginal);
            int[] aiOriginalQscale = { iFrameQscale, iFrameQscale, iFrameQscale,
                                       iFrameQscale, iFrameQscale, iFrameQscale };
            
            for (int iNewQscale = iFrameQscale; iNewQscale < 64; iNewQscale++) {
                fbs.println(I.TRYING_QSCALE(iNewQscale));

                int[] aiNewQscale = { iNewQscale, iNewQscale, iNewQscale,
                                      iNewQscale, iNewQscale, iNewQscale };

                for (MacroBlockEncoder macblk : encoder) {
                    macblk.setToPartialEncode(aiOriginalQscale, aiNewQscale);
                }

                byte[] abNewDemux = compress(encoder.getStream(), encoder.getPixelWidth(), encoder.getPixelHeight());
                if (abNewDemux.length <= abOriginal.length) {
                    fbs.indent1().println(I.NEW_FRAME_FITS(frame, abNewDemux.length, abOriginal.length));
                    return abNewDemux;
                } else {
                    fbs.indent1().println(I.NEW_FRAME_DOES_NOT_FIT(frame, abNewDemux.length, abOriginal.length));
                }
            }
            return null;
        }

        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream,
                                        int iWidth, int iHeight)
                throws MdecException
        {
            _iMdecCodeCount = -1;
            if (iWidth < 1 || iHeight < 1)
                throw new IllegalArgumentException("Invalid dimensions " + iWidth + "x" + iHeight);
            final int iMacroBlockCount = Calc.macroblocks(iWidth, iHeight);

            _iQscale = -1; // qscale will be set on first block read

            ByteArrayOutputStream bits = new ByteArrayOutputStream();

            BitStreamWriter bitStream = new BitStreamWriter(bits);
            bitStream.setLittleEndian(isBitstreamLittleEndian());

            final MdecCode code = new MdecCode();
            int iMdecCodeCount = 0;

            try {
                boolean blnNewBlk = true;
                int iBlock = 0;
                for (int iMacroBlock = 0; iMacroBlock < iMacroBlockCount;) {
                    String sBitsToWrite;
                    boolean blnEod = inStream.readMdecCode(code);
                    if (!code.isValid())
                        throw new MdecException.Read(I.COMPRESS_INVALID_MDEC_CODE(code));
                    if (blnEod) {
                        sBitsToWrite = AcLookup.END_OF_BLOCK.BitString;
                        blnNewBlk = true;
                        iBlock = (iBlock + 1) % 6;
                        if (iBlock == 0)
                            iMacroBlock++;
                    } else {
                        if (blnNewBlk) {
                            setBlockQscale(iBlock, code.getTop6Bits());
                            sBitsToWrite = encodeDC(code.getBottom10Bits(), iBlock);
                            blnNewBlk = false;
                        } else {
                            sBitsToWrite = encodeAC(code);
                        }
                    }
                    iMdecCodeCount++;
                    if (DEBUG)
                        System.out.println("Converting " + code.toString() + " to " + sBitsToWrite + " at " + bits.size());
                    bitStream.write(sBitsToWrite);
                }

                if (iBlock != 0)
                    throw new IllegalStateException("Ended compressing in the middle of a macroblock.");

                addTrailingBits(bitStream);
                bitStream.close();
            } catch (IOException ex) {
                throw new MdecException.Write(ex);
            }

            byte[] abHeader = createHeader(iMdecCodeCount);

            byte[] abReturn = new byte[abHeader.length + bits.size()];
            System.arraycopy(abHeader, 0, abReturn, 0, abHeader.length);
            System.arraycopy(bits.toByteArray(), 0, abReturn, abHeader.length, bits.size());

            _iMdecCodeCount = iMdecCodeCount;
            return abReturn;
        }

        public int getMdecCodesFromLastCompress() {
            return _iMdecCodeCount;
        }



        protected boolean isBitstreamLittleEndian() {
            return true;
        }

        protected void addTrailingBits(@Nonnull BitStreamWriter bitStream) throws IOException {
            bitStream.write(END_OF_FRAME_EXTRA_BITS);
        }

        /** Sets the quantization scale for the current block being encoded.
         * Performs any necessary preparations for encoding the block.
         * Ensures the quantization scale is compatible with the bitstream.
         * Caller will ensure parameters are valid. */
        protected void setBlockQscale(int iBlock, int iQscale) throws MdecException.Compress {
            if (_iQscale < 0)
                _iQscale = iQscale;
            else if (_iQscale != iQscale)
                throw new MdecException.Compress(I.INCONSISTENT_QSCALE(_iQscale, iQscale));
        }

        protected @Nonnull String encodeDC(int iDC, int iBlock) throws MdecException.Compress {
            if (iDC < -512 || iDC > 511)
                throw new IllegalArgumentException("Invalid DC code " + iDC);
            if (iBlock < 0 || iBlock > 5)
                throw new IllegalArgumentException("Invalid block " + iBlock);

            return Misc.bitsToString(iDC, 10);
        }

        private String encodeAC(@Nonnull MdecCode code) 
                throws MdecException.TooMuchEnergyToCompress
        {
            if (!code.isValid())
                throw new IllegalArgumentException("Invalid MDEC code " + code);

            for (AcBitCode vlc : getAcVaribleLengthCodeList().getCodeList()) {
                if (code.getTop6Bits() == vlc.ZeroRun && Math.abs(code.getBottom10Bits()) == vlc.AcCoefficient) {
                    return vlc.BitString.replace('s', (code.getBottom10Bits() < 0) ? '1' : '0');
                }
            }
            // not a pre-defined code
            return encodeAcEscape(code);
        }

        protected @Nonnull AcLookup getAcVaribleLengthCodeList() {
            return AC_VARIABLE_LENGTH_CODES_MPEG1;
        }

        protected @Nonnull String encodeAcEscape(@Nonnull MdecCode code)
                throws MdecException.TooMuchEnergyToCompress
        {
            if (!code.isValid())
                throw new IllegalArgumentException("Invalid MDEC code " + code);

            return AcLookup.ESCAPE_CODE.BitString +
                    Misc.bitsToString(code.getTop6Bits(), 6) +
                    Misc.bitsToString(code.getBottom10Bits(), 10);
        }

        protected @Nonnull byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[8];

            IO.writeInt16LE(ab, 0, calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(ab, 2, (short)0x3800);
            IO.writeInt16LE(ab, 4, (short)_iQscale);
            IO.writeInt16LE(ab, 6, (short)getHeaderVersion());

            return ab;
        }

        protected int getHeaderVersion() { return 2; }
        
        protected int getQscale(@Nonnull byte[] abFrameData) throws MdecException.Uncompress {
            return BitStreamUncompressor_STRv2.getQscale(abFrameData);
        }
    }


    /** Debug */
    public static void main(String[] args) {
        AC_VARIABLE_LENGTH_CODES_MPEG1.print(System.out);
    }
}
