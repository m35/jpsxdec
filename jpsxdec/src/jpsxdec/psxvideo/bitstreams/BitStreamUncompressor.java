/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import java.io.EOFException;
import java.io.PrintStream;
import java.util.Arrays;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;

/** Converts a (demuxed) video frame bitstream into an {@link MdecInputStream},
 * that can then be fed into an MDEC decoder to produce an image. */
public abstract class BitStreamUncompressor extends MdecInputStream {
    
    /** Enable to print the detailed decoding process. */
    public static boolean DEBUG = false;

    public static BitStreamUncompressor identifyUncompressor(byte[] abHeaderBytes) {
        if (BitStreamUncompressor_STRv2.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv2();
        else if(BitStreamUncompressor_STRv3.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv3();
        else if(BitStreamUncompressor_STRv1.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv1();
        else if(BitStreamUncompressor_Iki.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_Iki();
        else if(BitStreamUncompressor_Lain.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_Lain();
        else
            return null;
    }

    /** Longest AC variable-length (Huffman) bit code, in bits. */
    public final static int AC_LONGEST_VARIABLE_LENGTH_CODE = 17;

    public static BitStreamCompressor identifyCompressor(byte[] abHeaderBytes) {
        if (BitStreamUncompressor_STRv2.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv2.BitstreamCompressor_STRv2();
        else if(BitStreamUncompressor_STRv3.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv3.BitstreamCompressor_STRv3();
        else if(BitStreamUncompressor_STRv1.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_STRv1.BitStreamCompressor_STRv1();
        else if(BitStreamUncompressor_Iki.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_Iki.BitStreamCompressor_Iki();
        else if(BitStreamUncompressor_Lain.checkHeader(abHeaderBytes))
            return new BitStreamUncompressor_Lain.BitstreamCompressor_Lain();
        else
            return null;
    }

    /** Holds the mapping of bit string to Zero-Run Length + AC Coefficient. */
    protected static class AcBitCode {
        /** Readable string of bits of this code. */
        final public String BitString;
        /** Number of AC coefficient zero values, 
         * or <code>Integer.MIN_VALUE</code> if N/A.  */
        final public int ZeroRun;
        /** Non-zero AC coefficient value (may be an absolute value), 
         *  or <code>Integer.MIN_VALUE</code> if N/A. */
        final public int AcCoefficient;
        /** Bit length of this code (could use <code>Bits.length()</code>, but
         * this appears to be faster). */
        final public int BitLength;

        /** Create a bit code with valid {@link #Run} and {@link Ac} values. */
        private AcBitCode(String sBitString, int iZeroRun, int iAcCoefficient) {
            BitString = sBitString;
            ZeroRun = iZeroRun;
            AcCoefficient = iAcCoefficient;
            BitLength = sBitString.length();
        }

        @Override
        public String toString() {
            return BitString + "=(" + ZeroRun + ", " + AcCoefficient + ")";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final AcBitCode other = (AcBitCode) obj;
            return this.ZeroRun == other.ZeroRun && this.AcCoefficient == other.AcCoefficient;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.ZeroRun;
            hash = 79 * hash + this.AcCoefficient;
            return hash;
        }
    }

    private static class SpecialAcBitCode extends AcBitCode {

        private final String _sId;

        /** Create a bit code that doesn't have meaningful
         * {@link #ZeroRun} and {@link #AcCoefficient} values. */
        private SpecialAcBitCode(String sBitString, String sId) {
            super(sBitString, Integer.MIN_VALUE, Integer.MIN_VALUE);
            _sId = sId;
        }

        @Override
        public String toString() {
            return BitString + "=" + _sId;
        }
    }
    
    /** Blazing fast bitstream parser.
     * Specifically designed to decode the 111 PSX (and MPEG1) AC coefficient 
     * variable-length (Huffman) bit codes, including the {@link #END_OF_BLOCK}
     * code and {@link #ESCAPE_CODE}. Every method beginning with
     * underscore ('_') should be called before the class is used for parsing.
     */
    protected static final class AcLookup {

        /** Sequence of bits indicating the end of a block. */
        public final static AcBitCode END_OF_BLOCK = new SpecialAcBitCode("10", "EOB");
        /** Sequence of bits indicating an escape code. */
        public final static AcBitCode ESCAPE_CODE = new SpecialAcBitCode("000001", "ESCAPE_CODE");

        /** Bit codes for '11s'. Can be used as an alternative to the
         * {@link #Table_1xx} table. */
        private AcBitCode _110, _111;

        /** Table to look up END_OF_BLOCK ('10') and 11s codes using all but the first (1) bit. */
        private final AcBitCode[] Table_1xx = new AcBitCode[4];
        /** Table to look up codes '011s' to '00100111s' using all but the first (0) bit.
         * Given a bit code in that range, strip the leading zero bit, then pad
         * any extra trailing bits to make an 8 bit value. Use that value as the
         * index in this table to get the corresponding code. */
        private final AcBitCode[] Table_0xxxxxxx = new AcBitCode[256];
        /** Table to look up codes '0000001000s' to '0000000011111s' using all but
         *  the first 6 zero bits.
         * Given a bit code in that range, strip the leading 6 zero bits, then pad
         * any extra trailing bits to make an 8 bit value. Use that value as the
         * index in this table to get the corresponding code. */
        private final AcBitCode[] Table_000000xxxxxxxx = new AcBitCode[256];
        /** Table to look up codes '00000000010000s' to '0000000000011111s' using 
         *  all but the first 9 zero bits.
         * Given a bit code in that range, strip the leading 9 zero bits, then pad
         * any extra trailing bits to make an 8 bit value. Use that value as the
         * index in this table to get the corresponding code. */
        private final AcBitCode[] Table_000000000xxxxxxxx = new AcBitCode[256];

        /** Holds all the codes for references and compression. */
        private final AcBitCode[] _aoAcBitCodes = new AcBitCode[111];

        public AcLookup() {
            // initialize the two codes we know about
            setBits(END_OF_BLOCK);
            setBits(ESCAPE_CODE);
        }
        
        // <editor-fold defaultstate="collapsed" desc="===========Bit setters============">
        public AcLookup _11s(int r, int aac) {
            set(0, "11s", r,  aac);
            _110 = new AcBitCode("110", r,  aac);
            _111 = new AcBitCode("111", r, -aac);
            return this;
        }
        //    <editor-fold defaultstate="collapsed" desc="First 8 bits">
        public AcLookup _011s     (int r, int aac) { return set(  1, "011s", r, aac); }
        public AcLookup _0100s    (int r, int aac) { return set(  2, "0100s", r, aac); }
        public AcLookup _0101s    (int r, int aac) { return set(  3, "0101s", r, aac); }
        public AcLookup _00101s   (int r, int aac) { return set(  4, "00101s", r, aac); }
        public AcLookup _00110s   (int r, int aac) { return set(  5, "00110s", r, aac); }
        public AcLookup _00111s   (int r, int aac) { return set(  6, "00111s", r, aac); }
        public AcLookup _000100s  (int r, int aac) { return set(  7, "000100s", r, aac); }
        public AcLookup _000101s  (int r, int aac) { return set(  8, "000101s", r, aac); }
        public AcLookup _000110s  (int r, int aac) { return set(  9, "000110s", r, aac); }
        public AcLookup _000111s  (int r, int aac) { return set( 10, "000111s", r, aac); }
        public AcLookup _0000100s (int r, int aac) { return set( 11, "0000100s", r, aac); }
        public AcLookup _0000101s (int r, int aac) { return set( 12, "0000101s", r, aac); }
        public AcLookup _0000110s (int r, int aac) { return set( 13, "0000110s", r, aac); }
        public AcLookup _0000111s (int r, int aac) { return set( 14, "0000111s", r, aac); }
        public AcLookup _00100000s(int r, int aac) { return set( 15, "00100000s", r, aac); }
        public AcLookup _00100001s(int r, int aac) { return set( 16, "00100001s", r, aac); }
        public AcLookup _00100010s(int r, int aac) { return set( 17, "00100010s", r, aac); }
        public AcLookup _00100011s(int r, int aac) { return set( 18, "00100011s", r, aac); }
        public AcLookup _00100100s(int r, int aac) { return set( 19, "00100100s", r, aac); }
        public AcLookup _00100101s(int r, int aac) { return set( 20, "00100101s", r, aac); }
        public AcLookup _00100110s(int r, int aac) { return set( 21, "00100110s", r, aac); }
        public AcLookup _00100111s(int r, int aac) { return set( 22, "00100111s", r, aac); }
        //    </editor-fold>
        // ------------------------------------------------------------------------------
        //    <editor-fold defaultstate="collapsed" desc="Second 8 bits">
        public AcLookup _0000001000s(int r, int aac) { return set( 23, "0000001000s", r, aac); }
        public AcLookup _0000001001s(int r, int aac) { return set( 24, "0000001001s", r, aac); }
        public AcLookup _0000001010s(int r, int aac) { return set( 25, "0000001010s", r, aac); }
        public AcLookup _0000001011s(int r, int aac) { return set( 26, "0000001011s", r, aac); }
        public AcLookup _0000001100s(int r, int aac) { return set( 27, "0000001100s", r, aac); }
        public AcLookup _0000001101s(int r, int aac) { return set( 28, "0000001101s", r, aac); }
        public AcLookup _0000001110s(int r, int aac) { return set( 29, "0000001110s", r, aac); }
        public AcLookup _0000001111s(int r, int aac) { return set( 30, "0000001111s", r, aac); }
        public AcLookup _000000010000s(int r, int aac) { return set( 31, "000000010000s", r, aac); }
        public AcLookup _000000010001s(int r, int aac) { return set( 32, "000000010001s", r, aac); }
        public AcLookup _000000010010s(int r, int aac) { return set( 33, "000000010010s", r, aac); }
        public AcLookup _000000010011s(int r, int aac) { return set( 34, "000000010011s", r, aac); }
        public AcLookup _000000010100s(int r, int aac) { return set( 35, "000000010100s", r, aac); }
        public AcLookup _000000010101s(int r, int aac) { return set( 36, "000000010101s", r, aac); }
        public AcLookup _000000010110s(int r, int aac) { return set( 37, "000000010110s", r, aac); }
        public AcLookup _000000010111s(int r, int aac) { return set( 38, "000000010111s", r, aac); }
        public AcLookup _000000011000s(int r, int aac) { return set( 39, "000000011000s", r, aac); }
        public AcLookup _000000011001s(int r, int aac) { return set( 40, "000000011001s", r, aac); }
        public AcLookup _000000011010s(int r, int aac) { return set( 41, "000000011010s", r, aac); }
        public AcLookup _000000011011s(int r, int aac) { return set( 42, "000000011011s", r, aac); }
        public AcLookup _000000011100s(int r, int aac) { return set( 43, "000000011100s", r, aac); }
        public AcLookup _000000011101s(int r, int aac) { return set( 44, "000000011101s", r, aac); }
        public AcLookup _000000011110s(int r, int aac) { return set( 45, "000000011110s", r, aac); }
        public AcLookup _000000011111s(int r, int aac) { return set( 46, "000000011111s", r, aac); }
        public AcLookup _0000000010000s(int r, int aac) { return set( 47, "0000000010000s", r, aac); }
        public AcLookup _0000000010001s(int r, int aac) { return set( 48, "0000000010001s", r, aac); }
        public AcLookup _0000000010010s(int r, int aac) { return set( 49, "0000000010010s", r, aac); }
        public AcLookup _0000000010011s(int r, int aac) { return set( 50, "0000000010011s", r, aac); }
        public AcLookup _0000000010100s(int r, int aac) { return set( 51, "0000000010100s", r, aac); }
        public AcLookup _0000000010101s(int r, int aac) { return set( 52, "0000000010101s", r, aac); }
        public AcLookup _0000000010110s(int r, int aac) { return set( 53, "0000000010110s", r, aac); }
        public AcLookup _0000000010111s(int r, int aac) { return set( 54, "0000000010111s", r, aac); }
        public AcLookup _0000000011000s(int r, int aac) { return set( 55, "0000000011000s", r, aac); }
        public AcLookup _0000000011001s(int r, int aac) { return set( 56, "0000000011001s", r, aac); }
        public AcLookup _0000000011010s(int r, int aac) { return set( 57, "0000000011010s", r, aac); }
        public AcLookup _0000000011011s(int r, int aac) { return set( 58, "0000000011011s", r, aac); }
        public AcLookup _0000000011100s(int r, int aac) { return set( 59, "0000000011100s", r, aac); }
        public AcLookup _0000000011101s(int r, int aac) { return set( 60, "0000000011101s", r, aac); }
        public AcLookup _0000000011110s(int r, int aac) { return set( 61, "0000000011110s", r, aac); }
        public AcLookup _0000000011111s(int r, int aac) { return set( 62, "0000000011111s", r, aac); }
        //    </editor-fold>
        // ---------------------------------------------------------------------------------------
        //    <editor-fold defaultstate="collapsed" desc="Third 8 bits">
        public AcLookup _00000000010000s(int r, int aac) { return set( 63, "00000000010000s", r, aac); }
        public AcLookup _00000000010001s(int r, int aac) { return set( 64, "00000000010001s", r, aac); }
        public AcLookup _00000000010010s(int r, int aac) { return set( 65, "00000000010010s", r, aac); }
        public AcLookup _00000000010011s(int r, int aac) { return set( 66, "00000000010011s", r, aac); }
        public AcLookup _00000000010100s(int r, int aac) { return set( 67, "00000000010100s", r, aac); }
        public AcLookup _00000000010101s(int r, int aac) { return set( 68, "00000000010101s", r, aac); }
        public AcLookup _00000000010110s(int r, int aac) { return set( 69, "00000000010110s", r, aac); }
        public AcLookup _00000000010111s(int r, int aac) { return set( 70, "00000000010111s", r, aac); }
        public AcLookup _00000000011000s(int r, int aac) { return set( 71, "00000000011000s", r, aac); }
        public AcLookup _00000000011001s(int r, int aac) { return set( 72, "00000000011001s", r, aac); }
        public AcLookup _00000000011010s(int r, int aac) { return set( 73, "00000000011010s", r, aac); }
        public AcLookup _00000000011011s(int r, int aac) { return set( 74, "00000000011011s", r, aac); }
        public AcLookup _00000000011100s(int r, int aac) { return set( 75, "00000000011100s", r, aac); }
        public AcLookup _00000000011101s(int r, int aac) { return set( 76, "00000000011101s", r, aac); }
        public AcLookup _00000000011110s(int r, int aac) { return set( 77, "00000000011110s", r, aac); }
        public AcLookup _00000000011111s(int r, int aac) { return set( 78, "00000000011111s", r, aac); }
        public AcLookup _000000000010000s(int r, int aac) { return set( 79, "000000000010000s", r, aac); }
        public AcLookup _000000000010001s(int r, int aac) { return set( 80, "000000000010001s", r, aac); }
        public AcLookup _000000000010010s(int r, int aac) { return set( 81, "000000000010010s", r, aac); }
        public AcLookup _000000000010011s(int r, int aac) { return set( 82, "000000000010011s", r, aac); }
        public AcLookup _000000000010100s(int r, int aac) { return set( 83, "000000000010100s", r, aac); }
        public AcLookup _000000000010101s(int r, int aac) { return set( 84, "000000000010101s", r, aac); }
        public AcLookup _000000000010110s(int r, int aac) { return set( 85, "000000000010110s", r, aac); }
        public AcLookup _000000000010111s(int r, int aac) { return set( 86, "000000000010111s", r, aac); }
        public AcLookup _000000000011000s(int r, int aac) { return set( 87, "000000000011000s", r, aac); }
        public AcLookup _000000000011001s(int r, int aac) { return set( 88, "000000000011001s", r, aac); }
        public AcLookup _000000000011010s(int r, int aac) { return set( 89, "000000000011010s", r, aac); }
        public AcLookup _000000000011011s(int r, int aac) { return set( 90, "000000000011011s", r, aac); }
        public AcLookup _000000000011100s(int r, int aac) { return set( 91, "000000000011100s", r, aac); }
        public AcLookup _000000000011101s(int r, int aac) { return set( 92, "000000000011101s", r, aac); }
        public AcLookup _000000000011110s(int r, int aac) { return set( 93, "000000000011110s", r, aac); }
        public AcLookup _000000000011111s(int r, int aac) { return set( 94, "000000000011111s", r, aac); }
        public AcLookup _0000000000010000s(int r, int aac) { return set( 95, "0000000000010000s", r, aac); }
        public AcLookup _0000000000010001s(int r, int aac) { return set( 96, "0000000000010001s", r, aac); }
        public AcLookup _0000000000010010s(int r, int aac) { return set( 97, "0000000000010010s", r, aac); }
        public AcLookup _0000000000010011s(int r, int aac) { return set( 98, "0000000000010011s", r, aac); }
        public AcLookup _0000000000010100s(int r, int aac) { return set( 99, "0000000000010100s", r, aac); }
        public AcLookup _0000000000010101s(int r, int aac) { return set(100, "0000000000010101s", r, aac); }
        public AcLookup _0000000000010110s(int r, int aac) { return set(101, "0000000000010110s", r, aac); }
        public AcLookup _0000000000010111s(int r, int aac) { return set(102, "0000000000010111s", r, aac); }
        public AcLookup _0000000000011000s(int r, int aac) { return set(103, "0000000000011000s", r, aac); }
        public AcLookup _0000000000011001s(int r, int aac) { return set(104, "0000000000011001s", r, aac); }
        public AcLookup _0000000000011010s(int r, int aac) { return set(105, "0000000000011010s", r, aac); }
        public AcLookup _0000000000011011s(int r, int aac) { return set(106, "0000000000011011s", r, aac); }
        public AcLookup _0000000000011100s(int r, int aac) { return set(107, "0000000000011100s", r, aac); }
        public AcLookup _0000000000011101s(int r, int aac) { return set(108, "0000000000011101s", r, aac); }
        public AcLookup _0000000000011110s(int r, int aac) { return set(109, "0000000000011110s", r, aac); }
        public AcLookup _0000000000011111s(int r, int aac) { return set(110, "0000000000011111s", r, aac); }
        //    </editor-fold>        
        // </editor-fold>

        /** Given the AC coefficient absolute value, creates both positive and negative
         * versions and assigns them to appropriate lookup tables. Also adds
         * the absolute value version to the reference list of codes. */
        private AcLookup set(int iCodeNum, String sBits, int iRun, int iAbsoluteAc) {
            _aoAcBitCodes[iCodeNum] = new AcBitCode(sBits, iRun,  iAbsoluteAc);
            setBits(new AcBitCode(sBits.replace('s', '0'), iRun,  iAbsoluteAc));
            setBits(new AcBitCode(sBits.replace('s', '1'), iRun, -iAbsoluteAc));
            return this;
        }

        /** Identifies the lookup table in which to place the bit code.  */
        private void setBits(AcBitCode lu) {
            final int iBitsRemain;
            final AcBitCode[] aoTable;
            final int iTableStart;
            
            if        (lu.BitString.startsWith("000000000")) {
                aoTable =                 Table_000000000xxxxxxxx;
                iBitsRemain = 8 - (lu.BitString.length() - 9);
                iTableStart = Integer.parseInt(lu.BitString, 2) << iBitsRemain;
            } else if (lu.BitString.startsWith("000000"   )) {
                aoTable =                 Table_000000xxxxxxxx;
                iBitsRemain = 8 - (lu.BitString.length() - 6);
                iTableStart = Integer.parseInt(lu.BitString, 2) << iBitsRemain;
            } else if (lu.BitString.startsWith("0"        )) {
                aoTable =                 Table_0xxxxxxx;
                iBitsRemain = 8 - (lu.BitString.length() - 1);
                iTableStart = Integer.parseInt(lu.BitString, 2) << iBitsRemain;
            } else { // startsWith("1")
                aoTable = Table_1xx;
                iBitsRemain = 2 - (lu.BitString.length() - 1);
                iTableStart = Integer.parseInt(lu.BitString.substring(1), 2) << iBitsRemain;
            }
            
            final int iTableEntriesToAssociate = (1 << iBitsRemain);
            for (int i = 0; i < iTableEntriesToAssociate; i++) {
                if (aoTable[iTableStart + i] != null)
                    throw new RuntimeException("Resetting an existing bitstream lookup probably means some code is wrong.");
                aoTable[iTableStart + i] = lu;
            }
        }

        public Iterable<AcBitCode> getCodeList() {
            return Arrays.asList(_aoAcBitCodes);
        }

        /** Useful bitmasks. */
        private static final int
            b11000000000000000 = 0x18000,
            b10000000000000000 = 0x10000, 
            b01000000000000000 = 0x08000,
            b00100000000000000 = 0x04000,
            b01111100000000000 = 0x0F800,
            b00000011100000000 = 0x00700,
            b00000000011100000 = 0x000E0;
    
        /** Converts bits to the equivalent {@link BitstreamToMdecLookup}.
         * 17 bits need to be supplied to decode (the longest bit code).
         * Bits should start at the least-significant bit.
         * Bits beyond the 17 least-significant bits are ignored.
         * If a full 17 bits are unavailable, fill the remaining with zeros
         * to ensure failure if bit code is invalid.
         *
         * @param i17bits  Integer containing 17 bits to decode.
         */
        private AcBitCode lookup(final int i17bits) throws MdecException.Uncompress {
            if        ((i17bits & b10000000000000000) != 0) {
                assert !DEBUG || debugPrintln("Table 0 offset " + ((i17bits >> 14) & 3));
                return       Table_1xx[(i17bits >> 14) & 3];
            } else if ((i17bits & b01111100000000000) != 0) {
                assert !DEBUG || debugPrintln("Table 1 offset " + ((i17bits >> 8) & 0xff));
                return       Table_0xxxxxxx[(i17bits >> 8) & 0xff];
            } else if ((i17bits & b00000011100000000) != 0) {
                assert !DEBUG || debugPrintln("Table 2 offset " + ((i17bits >> 3) & 0xff));
                return       Table_000000xxxxxxxx[(i17bits >> 3) & 0xff];
            } else if ((i17bits & b00000000011100000) != 0) {
                assert !DEBUG || debugPrintln("Table 3 offset " + (i17bits & 0xff));
                return       Table_000000000xxxxxxxx[i17bits & 0xff];
            } else {
                throw new MdecException.Uncompress("Unmatched AC variable length code: {0}", // I18N
                        Misc.bitsToString(i17bits, AC_LONGEST_VARIABLE_LENGTH_CODE));
            }
        }

        // ..................................................
        
        // <editor-fold defaultstate="collapsed" desc="Slow lookups (for reference)">
        /** Alternative lookup method that is slower that {@link #lookup(int)}. */
        private AcBitCode lookup_slow1(final int i17bits) throws MdecException.Uncompress {
            if ((i17bits & b11000000000000000) == b10000000000000000) {
                return END_OF_BLOCK;
            } else if ((i17bits & b11000000000000000) == b11000000000000000) {
                // special handling for first AC code
                if ((i17bits & b00100000000000000) == 0) // check sign bit
                    return     _110;
                else
                    return     _111;
            } else {
                // escape code is already set in the lookup table
                final AcBitCode[] array;
                final int c;
                if        ((i17bits & b01111100000000000) != 0) {
                    array =      Table_0xxxxxxx;
                    c = (i17bits >> 8) & 0xff;
                } else if ((i17bits & b00000011100000000) != 0) {
                    array =      Table_000000xxxxxxxx;
                    c = (i17bits >> 3) & 0xff;
                } else if ((i17bits & b00000000011100000) != 0) {
                    array =      Table_000000000xxxxxxxx;
                    c = i17bits & 0xff;
                } else {
                    throw new MdecException.Uncompress("Unmatched AC variable length code: {0}", // I18N
                            Misc.bitsToString(i17bits, AC_LONGEST_VARIABLE_LENGTH_CODE));
                }
                return array[c];
            }
        }
        /** Alternative lookup method that is slower that {@link #lookup(int)}. */
        private AcBitCode lookup_slow2(final int i17bits) throws MdecException.Uncompress {
            if ((i17bits &        b11000000000000000) == b10000000000000000) {
                return END_OF_BLOCK;
            } else if ((i17bits & b11000000000000000) == b11000000000000000) {
                // special handling for first AC code
                if ((i17bits &    b00100000000000000) == 0) // check sign bit
                    return        _110;
                else
                    return        _111;
            } else {
                // escape code is already part of the lookup table
                if        ((i17bits & b01111100000000000) != 0) {
                    return       Table_0xxxxxxx[(i17bits >> 8) & 0xff];
                } else if ((i17bits & b00000011100000000) != 0) {
                    return       Table_000000xxxxxxxx[(i17bits >> 3) & 0xff];
                } else if ((i17bits & b00000000011100000) != 0) {
                    return       Table_000000000xxxxxxxx[i17bits & 0xff];
                } else {
                    throw new MdecException.Uncompress("Unmatched AC variable length code: {0}", // I18N
                            Misc.bitsToString(i17bits, AC_LONGEST_VARIABLE_LENGTH_CODE));
                }
            }
        }
        /** Alternative lookup method that is slower that {@link #lookup(int)}. */
        private AcBitCode lookup_slow3(final int i17bits) throws MdecException.Uncompress {
            if ((i17bits & b10000000000000000) != 0) {       // 1st bit is 1?
                if ((i17bits & b01000000000000000) == 0) {   // initial bits == '10'?
                    return END_OF_BLOCK;
                } else {                                     // initial bits == '11'
                    // special handling for first AC code
                    // check sign bit
                    if ((i17bits & b00100000000000000) == 0) // initial bits == '110'?
                        return     _110;
                    else                                     // initial bits == '111'
                        return     _111;
                }
            } else {                                         // 1st bit is 0
                // escape code is already set in the lookup table
                final AcBitCode[] array;
                final int c;
                if        ((i17bits & b01111100000000000) != 0) {
                    array =      Table_0xxxxxxx;
                    c = (i17bits >> 8) & 0xff;
                } else if ((i17bits & b00000011100000000) != 0) {
                    array =      Table_000000xxxxxxxx;
                    c = (i17bits >> 3) & 0xff;
                } else if ((i17bits & b00000000011100000) != 0) {
                    array =      Table_000000000xxxxxxxx;
                    c = i17bits & 0xff;
                } else {
                    throw new MdecException.Uncompress("Unmatched AC variable length code: {0}", // I18N
                            Misc.bitsToString(i17bits, AC_LONGEST_VARIABLE_LENGTH_CODE));
                }
                return array[c];
            }
        }

        /** Bit masks for
         * {@link #lookup_old(int, jpsxdec.psxvideo.mdec.MdecInputStream.MdecCode)} */
        private static final int
			b1000000000000000_ = 0x8000 << 1,
			b0100000000000000_ = 0x4000 << 1,
			b0010000000000000_ = 0x2000 << 1,
			b0001100000000000_ = 0x1800 << 1,
			b0001000000000000_ = 0x1000 << 1,
			b0000100000000000_ = 0x0800 << 1,
			b0000010000000000_ = 0x0400 << 1,
			b0000001000000000_ = 0x0200 << 1,
			b0000000100000000_ = 0x0100 << 1,
			b0000000010000000_ = 0x0080 << 1,
			b0000000001000000_ = 0x0040 << 1,
			b0000000000100000_ = 0x0020 << 1,
			b0000000000010000_ = 0x0010 << 1;
        /** This lookup approach used in 0.96.0 and earlier needs a slightly
         *  different API to work. The calling code will need some adjustment. */
        private AcBitCode lookup_old(final int i17bits, MdecCode code) throws MdecException.Uncompress {

            final AcBitCode vlc;

            // Walk through the bits, one-by-one
            // Fun fact: The Lain Playstation game uses this same decoding approach
            if (    (i17bits & b1000000000000000_) != 0) {        // "1"
                if ((i17bits & b0100000000000000_) != 0) {        // "11"
                    vlc = _aoAcBitCodes[0];
                } else {                                          // "10"
                    // End of block
                    code.setToEndOfData();
                    return END_OF_BLOCK;
                }
            } else if ((i17bits & b0100000000000000_) != 0) {     // "01"
                if    ((i17bits & b0010000000000000_) != 0) {     // "011"
                    vlc = _aoAcBitCodes[1];
                } else {                                          // "010x"
                    vlc = _aoAcBitCodes[2 + (int)((i17bits >>> 13) & 1)];
                }
            } else if ((i17bits & b0010000000000000_) != 0) {      // "001"
                if    ((i17bits & b0001100000000000_) != 0)  {     // "001xx"
                    vlc = _aoAcBitCodes[3 + (int)((i17bits >>> 12) & 3)];
                } else {                                           // "00100xxx"
                    vlc = _aoAcBitCodes[15 + (int)((i17bits >>> 9) & 7)];
                }
            } else if ((i17bits & b0001000000000000_) != 0) {      // "0001xx"
                vlc = _aoAcBitCodes[7 + (int)((i17bits >>> 11) & 3)];
            } else if ((i17bits & b0000100000000000_) != 0) {      // "00001xx"
                vlc = _aoAcBitCodes[11 + (int)((i17bits >>> 10) & 3)];
            } else if ((i17bits & b0000010000000000_) != 0) {      // "000001"
                // escape code
                return ESCAPE_CODE;
            } else if ((i17bits & b0000001000000000_) != 0) {      // "0000001xxx"
                vlc = _aoAcBitCodes[23 + (int)((i17bits >>> 7) & 7)];
            } else if ((i17bits & b0000000100000000_) != 0) {      // "00000001xxxx"
                vlc = _aoAcBitCodes[31 + (int)((i17bits >>> 5) & 15)];
            } else if ((i17bits & b0000000010000000_) != 0) {      // "000000001xxxx"
                vlc = _aoAcBitCodes[47 + (int)((i17bits >>> 4) & 15)];
            } else if ((i17bits & b0000000001000000_) != 0) {      // "0000000001xxxx"
                vlc = _aoAcBitCodes[63 + (int)((i17bits >>> 3) & 15)];
            } else if ((i17bits & b0000000000100000_) != 0) {      // "00000000001xxxx"
                vlc = _aoAcBitCodes[79 + (int)((i17bits >>> 2) & 15)];
            } else if ((i17bits & b0000000000010000_) != 0) {      // "000000000001xxxx"
                vlc = _aoAcBitCodes[95 + (int)((i17bits >>> 1) & 15)];
            } else {
                throw new MdecException.Uncompress("Unmatched AC variable length code: {0}", // I18N
                        Misc.bitsToString(i17bits, AC_LONGEST_VARIABLE_LENGTH_CODE));
            }

            code.setTop6Bits(vlc.ZeroRun);

            // Take either the positive or negitive AC coefficient,
            // depending on the sign bit
            if ((i17bits & (1 << (16 - vlc.BitLength))) == 0) {
                // positive
                code.setBottom10Bits(vlc.AcCoefficient);
            } else {
                // negative
                code.setBottom10Bits(-vlc.AcCoefficient);
            }

            return vlc;
        }
        // </editor-fold>


        public void print(PrintStream ps) {
            for (int i = 0; i < Table_1xx.length; i++) {
                ps.println("Table_1xx["+i+"] = "+Table_1xx[i]);
            }
            for (int i = 0; i < Table_0xxxxxxx.length; i++) {
                ps.println("Table_0xxxxxxx["+i+"] = "+Table_0xxxxxxx[i]);
            }
            for (int i = 0; i < Table_000000xxxxxxxx.length; i++) {
                ps.println("Table_000000xxxxxxxx["+i+"] = "+Table_000000xxxxxxxx[i]);
            }
            for (int i = 0; i < Table_000000000xxxxxxxx.length; i++) {
                ps.println("Table_000000000xxxxxxxx["+i+"] = "+Table_000000000xxxxxxxx[i]);
            }
        }
    }

    private static boolean debugPrintln(String s) {
        System.out.println(s);
        return true;
    }

    /** Class to gather debugging info for display on each read. */
    public static class MdecDebugger {
        public long Position;
        private StringBuilder Bits = new StringBuilder();
        public boolean append(String s) {
            Bits.append(s);
            return true;
        }
        public boolean print(MdecCode code) {
            System.out.format("@%d %s -> %s", Position, Bits, code);
            System.out.println();
            Bits.setLength(0);
            return true;
        }

        public boolean setPosition(int iPosition) {
            Position = iPosition;
            return true;
        }
    }

    // #########################################################################
    // #########################################################################

    /** Table for looking up AC Coefficient bit codes. */
    private final AcLookup _lookupTable;
    /** Binary input stream being read. */
    protected final ArrayBitReader _bitReader = new ArrayBitReader();

    /** HOlds the debugger when debugging is enabled. */
    protected final MdecDebugger _debug;

    /** Indicates if the next read will be the Quantization Scale and DC Coefficient. */
    private boolean _blnBlockStart;
    /** Indicates if the next read will be the Quantization Scale and DC Coefficient. */
    protected boolean atStartOfBlock() { return _blnBlockStart; }
    /** Number of the current Macro Block being read. */
    private int _iCurrentMacroBlock;
    /** Number of the current Macro Block being read. */
    protected int getCurrentMacroBlock() { return _iCurrentMacroBlock; }
    /** 0 to 5 to indicate the current Macro Block's sub-block being read. */
    private int _iCurrentMacroBlockSubBlock;
    /** Returns 0 to 5 to indicate the current Macro Block's sub-block being read. */
    protected int getCurrentMacroBlockSubBlock() { return _iCurrentMacroBlockSubBlock; }
    /** Track the current Block's vector position to detect errors.  */
    private int _iCurrentBlockVectorPos;

    private int _iMdecCodeCount;
    public int getMdecCodeCount() { return _iMdecCodeCount; }

    protected BitStreamUncompressor(AcLookup lookupTable) {
        _lookupTable = lookupTable;
        if (DEBUG)
            _debug = new MdecDebugger();
        else
            _debug = null;
    }

    /** Resets this instance as if a new object was created. */
    final public void reset(byte[] abBitstream, int iBitstreamSize, int iStart) throws NotThisTypeException {
        readHeader(abBitstream, iBitstreamSize, _bitReader);
        _blnBlockStart = true;
        _iMdecCodeCount = 0;
        _iCurrentBlockVectorPos = 0;
        _iCurrentMacroBlock = 0;
        _iCurrentMacroBlockSubBlock = 0;
    }


    /** Resets this instance as if a new object was created. */
    final public void reset(byte[] abBitstream, int iBitstreamSize) throws NotThisTypeException {
        reset(abBitstream, iBitstreamSize, 0);
    }

    /** @throws NullPointerException if {@link #reset(byte[])} has not been called. */
    final public boolean readMdecCode(MdecCode code) throws MdecException.Uncompress {

        assert !DEBUG || _debug.setPosition(_bitReader.getWordPosition());

        try {

            if (_blnBlockStart) {
                readQscaleAndDC(code);
                _blnBlockStart = false;
            } else {
                int i17bits = _bitReader.peekUnsignedBits(AC_LONGEST_VARIABLE_LENGTH_CODE);
                AcBitCode bitCode = _lookupTable.lookup(i17bits);
                _bitReader.skipBits(bitCode.BitLength);

                if (bitCode == AcLookup.END_OF_BLOCK) {
                    // end of block
                    code.setToEndOfData();
                    _blnBlockStart = true;
                    _iCurrentBlockVectorPos = 0;
                    _iCurrentMacroBlockSubBlock++;
                    if (_iCurrentMacroBlockSubBlock >= 6) {
                        _iCurrentMacroBlockSubBlock = 0;
                        _iCurrentMacroBlock++;
                    }
                    assert !DEBUG || _debug.append(AcLookup.END_OF_BLOCK.BitString);
                } else {
                    // block continues
                    assert !DEBUG || _debug.append(bitCode.BitString);
                    if (bitCode == AcLookup.ESCAPE_CODE) {
                        readEscapeAcCode(code);
                    } else {
                        code.setBits(bitCode.ZeroRun, bitCode.AcCoefficient);
                    }

                    _iCurrentBlockVectorPos += code.getTop6Bits() + 1;
                    if (_iCurrentBlockVectorPos >= 64) {
                        throw new MdecException.Uncompress("Run length out of bounds: {0,number,#}", // I18N
                                _iCurrentBlockVectorPos);
                    }
                }

            }
            
        } catch (EOFException ex) {
            throw new MdecException.Uncompress(ex);
        }

        assert !DEBUG || _debug.print(code);

        // _blnBlockStart will be set to true if an EOB code was read
        _iMdecCodeCount++;
        return _blnBlockStart;
    }

    final public int getBitPosition() {
        return _bitReader.getBitsRead();
    }
    
    public void readToEnd(int iWidth, int iHeight) throws MdecException.Read {
        int iBlockCount = Calc.blocks(iWidth, iHeight);

        MdecCode code = new MdecCode();
        for (int i = 0; i < iBlockCount; i++) {
            while (!readMdecCode(code)) {
            }
        }
    }

    abstract public void skipPaddingBits() throws EOFException;

    /** Validates the frame header and initializes for reading
     * (including resetting the bit reader to the proper start byte and endian). */
    abstract protected void readHeader(byte[] abFrameData, int iDataSize, ArrayBitReader bitReader) throws NotThisTypeException;

    /** Read the quantization scale and DC coefficient from the bitstream. */
    abstract protected void readQscaleAndDC(MdecCode code) throws MdecException.Uncompress, EOFException;

    /** Read an AC Coefficient escaped (zero-run, AC level) value. */
    abstract protected void readEscapeAcCode(MdecCode code) throws MdecException.Uncompress, EOFException;

    /** Create an equivalent bitstream compressor. */
    abstract public BitStreamCompressor makeCompressor();

    abstract public String getName();

    @Override
    abstract public String toString();

    // -------------------------------------------------------------------------

    public static class SaturnGen {

        public static void main(String[] args) throws Exception {
            BitStreamUncompressor v2 = new BitStreamUncompressor_STRv2();

            AcLookup lkup = v2._lookupTable;

            for (int i = 0; i < 256; i++) {

                AcBitCode c1 = lkup.Table_0xxxxxxx[i], c2 = lkup.Table_000000xxxxxxxx[i],  c3 = lkup.Table_000000000xxxxxxxx[i];

                int iBitLen = (c1 == AcLookup.ESCAPE_CODE ? 31 : c1 == null ? 0 : c1.BitLength) |
                              (c2 == null ? 0 : c2.BitLength << 5) |
                              (c3 == null ? 0 : c3.BitLength << 10)
                              ;

                System.err.format("{0x%04x, 0x%04x, 0x%04x, 0x%04x}, // %s, %s, %s",
                        (c1 == null || c1 == AcLookup.ESCAPE_CODE) ? 0 : new MdecCode(c1.ZeroRun, c1.AcCoefficient).toMdecWord(),
                        c2 == null ? 0 : new MdecCode(c2.ZeroRun, c2.AcCoefficient).toMdecWord(),
                        c3 == null ? 0 : new MdecCode(c3.ZeroRun, c3.AcCoefficient).toMdecWord(),
                        iBitLen,
                        c1 == null ? "<invalid>" : c1.BitString,
                        c2 == null ? "<invalid>" : c2.BitString,
                        c3 == null ? "<invalid>" : c3.BitString
                        );
                System.err.println();

            }

        }
    }
    
}
