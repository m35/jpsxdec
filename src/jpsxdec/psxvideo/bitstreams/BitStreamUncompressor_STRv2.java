/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;


/** Bitstream parser/decoder for the very common "version 2" demuxed video
 * frames, used by most games.
 * <p>
 * WARNING: The public methods are NOT thread safe. Create a separate
 * instance of this class for each thread, or wrap the calls with synchronize. */
public class BitStreamUncompressor_STRv2 extends BitStreamUncompressor {

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

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /** Frame's quantization scale. */
    protected int _iQscale = -1;
    protected int _iHalfVlcCountCeil32;

    public BitStreamUncompressor_STRv2() {
        super(AC_VARIABLE_LENGTH_CODES_MPEG1);
    }

    protected void readHeader(byte[] abFrameData, ArrayBitReader bitReader) throws NotThisTypeException {
        _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800       = IO.readUInt16LE(abFrameData, 2);
        _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion         = IO.readSInt16LE(abFrameData, 6);

        if (iMagic3800 != 0x3800 || _iQscale < 1 ||
            iVersion != 2  || _iHalfVlcCountCeil32 < 0)
            throw new NotThisTypeException();

        bitReader.reset(abFrameData, true, 8);
    }

    public static boolean checkHeader(byte[] abFrameData) {
        int _iHalfVlcCountCeil32 = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800           = IO.readUInt16LE(abFrameData, 2);
        int _iQscale             = IO.readSInt16LE(abFrameData, 4);
        int iVersion             = IO.readSInt16LE(abFrameData, 6);

        return !(iMagic3800 != 0x3800 || _iQscale < 1 ||
                 iVersion != 2 || _iHalfVlcCountCeil32 < 0);
    }

    protected void readQscaleAndDC(MdecCode code) throws MdecException.Uncompress, EOFException {
        code.setTop6Bits(_iQscale);
        code.setBottom10Bits(_bitReader.readSignedBits(10));
        assert DEBUG ? _debug.append(Misc.bitsToString(code.getBottom10Bits(), 10)) : true;
        assert !code.isEOD(); // a Qscale of 63 and DC of -512 would look like EOD
    }

    protected void readEscapeAcCode(MdecCode code) throws EOFException {
        // Normal playstation encoding stores the escape code in 16 bits:
        // 6 for run of zeros, 10 for AC Coefficient
        int iRunAndAc = _bitReader.readUnsignedBits(6 + 10);
        code.set(iRunAndAc);
        assert DEBUG ? _debug.append(Misc.bitsToString(iRunAndAc, 16)) : true;

        // Ignore AC == 0 coefficients.
        // (I consider this an error, but FF7 has these codes,
        // so clearly the MDEC can handle it.)
        if (code.getBottom10Bits() == 0) {
            log.info("Escape code has 0 AC coefficient.");
        }
    }

    private static final int b01111111110 = 0x3FE;

    @Override
    public void skipPaddingBits() throws EOFException {
        int iPaddingBits = _bitReader.readUnsignedBits(11);
        if (iPaddingBits != b01111111110)
            log.warning("Incorrect padding bits " + Misc.bitsToString(iPaddingBits, 11));
    }

    @Override
    public BitstreamCompressor_STRv2 makeCompressor() {
        return new BitstreamCompressor_STRv2();
    }

    public int getQscale() {
        return _iQscale;
    }

    @Override
    public String getName() {
        return "STRv2";
    }


    /** A strange value needed for video bitstreams and video sector headers.
     *  It's the number of MDEC codes, divided by two, then rounded up to the
     *  next closest multiple of 32 (if not already a multiple of 32).  */
    public static short calculateHalfCeiling32(int iMdecCodeCount) {
        return (short) ((((iMdecCodeCount + 1) / 2) + 31) & ~31);
    }


    public Iterator<int[]> qscaleIterator(final boolean blnStartAt1) {
        return new QscaleIter(blnStartAt1 ? 1 : _iQscale, getCurrentMacroBlock() * 6);
    }

    private static class QscaleIter implements Iterator<int[]> {
        private int _iQscale;
        private final int _iSize;

        public QscaleIter(int iQscale, int iSize) {
            _iQscale = iQscale;
            _iSize = iSize;
        }

        public boolean hasNext() { return _iQscale < 64; }

        public int[] next() {
            int[] ab = new int[_iSize];
            Arrays.fill(ab, _iQscale);
            _iQscale++;
            return ab;
        }

        public void remove() { throw new UnsupportedOperationException(); }

        public String toString() {
            return "Qscale " + _iQscale;
        }
    }

    public String toString() {
        return String.format("%s Qscale=%d, Current Offset=%d, Current MB.Blk=%d.%d, MDEC count=%d",
                getName(), getQscale(),
                getStreamPosition(),
                getCurrentMacroBlock(), getCurrentMacroBlockSubBlock(),
                getMdecCodeCount());
    }

    /*########################################################################*/
    /*########################################################################*/
    /*########################################################################*/

    public static class BitstreamCompressor_STRv2 implements BitStreamCompressor {

        private int _iQscale;

        public byte[] compress(MdecInputStream inStream, int iMdecCodeCount)
                throws MdecException
        {
            if (iMdecCodeCount < 0)
                throw new IllegalArgumentException("Invalid MDEC code count " + iMdecCodeCount);

            _iQscale = -1;

            ByteArrayOutputStream bits = new ByteArrayOutputStream();

            BitStreamWriter bitStream = new BitStreamWriter(bits);
            bitStream.setLittleEndian(isBitstreamLittleEndian());

            MdecCode code = new MdecCode();

            try {
                boolean blnNewBlk = true;
                int iBlock = 0;
                for (int i = 0; i < iMdecCodeCount; i++) {
                    String sBitsToWrite;
                    if (inStream.readMdecCode(code)) {
                        sBitsToWrite = AcLookup.END_OF_BLOCK.BitString;
                        blnNewBlk = true;
                        iBlock = (iBlock + 1) % 6;
                    } else {
                        if (blnNewBlk) {
                            validateQscale(iBlock, code.getTop6Bits());
                            sBitsToWrite = encodeDC(code.getBottom10Bits(), iBlock);
                            blnNewBlk = false;
                        } else {
                            sBitsToWrite = encodeAC(code);
                        }
                    }
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

            return abReturn;
        }

        protected boolean isBitstreamLittleEndian() {
            return true;
        }

        protected void codeRead(MdecCode code) throws MdecException.Compress, MdecException.Read {
            if (code.isEOD()) {
                if (_iQscale < 0) {
                    _iQscale = code.getTop6Bits();
                    if (_iQscale < 1 || _iQscale > 63)
                        throw new MdecException.Read("Invalid quantization scale " + _iQscale);
                } else if (_iQscale != code.getTop6Bits())
                    throw new MdecException.Compress("Inconsistent qscale");
            }
        }

        protected void addTrailingBits(BitStreamWriter bitStream) throws IOException {
            bitStream.write(END_OF_FRAME_EXTRA_BITS);
        }

        protected void validateQscale(int iBlock, int iQscale) throws MdecException.Write {
            if (_iQscale < 0) {
                _iQscale = iQscale;
                if (_iQscale < 1 || _iQscale > 63)
                    throw new MdecException.Encode("Invalid quantization scale " + _iQscale);
            } else if (_iQscale != iQscale)
                throw new MdecException.Compress("Inconsistent qscale");
        }

        protected String encodeDC(int iDC, int iBlock) {
            if (iDC < -1024 || iDC > 1023)
                throw new IllegalArgumentException("Invalid DC code " + iDC);
            if (iBlock < 0 || iBlock > 5)
                throw new IllegalArgumentException("Invalid block " + iBlock);

            return Misc.bitsToString(iDC, 10);
        }

        private String encodeAC(MdecCode code)throws MdecException.Compress {
            if (code.getTop6Bits() < 0 || code.getTop6Bits() > 63)
                throw new IllegalArgumentException("Invalid AC zero run length " + code.getTop6Bits());
            if (code.getBottom10Bits() < -512 || code.getBottom10Bits() > 511)
                throw new IllegalArgumentException("Invalid AC code " + code.getBottom10Bits());

            for (AcBitCode vlc : getAcVaribleLengthCodeList().getCodeList()) {
                if (code.getTop6Bits() == vlc.ZeroRun && Math.abs(code.getBottom10Bits()) == vlc.AcCoefficient) {
                    return vlc.BitString.replace('s', (code.getBottom10Bits() < 0) ? '1' : '0');
                }
            }
            // not a pre-defined code
            return encodeAcEscape(code);
        }

        protected AcLookup getAcVaribleLengthCodeList() {
            return AC_VARIABLE_LENGTH_CODES_MPEG1;
        }

        protected String encodeAcEscape(MdecCode code) throws MdecException.Compress {
            if (code.getTop6Bits() < 0 || code.getTop6Bits() > 63)
                throw new IllegalArgumentException("Invalid AC zero run length " + code.getTop6Bits());
            if (code.getBottom10Bits() < -1024 || code.getBottom10Bits() > 1023)
                throw new IllegalArgumentException("Invalid AC code " + code.getBottom10Bits());

            return AcLookup.ESCAPE_CODE.BitString +
                    Misc.bitsToString(code.getTop6Bits(), 6) +
                    Misc.bitsToString(code.getBottom10Bits(), 10);
        }

        protected byte[] createHeader(int iMdecCodeCount) {
            byte[] ab = new byte[8];

            IO.writeInt16LE(ab, 0, calculateHalfCeiling32(iMdecCodeCount));
            IO.writeInt16LE(ab, 2, (short)0x3800);
            IO.writeInt16LE(ab, 4, (short)_iQscale);
            IO.writeInt16LE(ab, 6, (short)getHeaderVersion());

            return ab;
        }

        protected int getHeaderVersion() { return 2; }

    }


    /** Debug */
    public static void main(String[] args) {
        AC_VARIABLE_LENGTH_CODES_MPEG1.print(System.out);
    }
}
