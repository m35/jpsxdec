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

import javax.annotation.Nonnull;
import jpsxdec.modules.panekit.BitStreamUncompressor_Panekit;
import jpsxdec.modules.starwars.BitStreamUncompressor_StarWars;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecCode;
import jpsxdec.psxvideo.mdec.MdecContext;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.Misc;

/** Common bitstream uncompressor for independent bitstreams. */
public abstract class BitStreamUncompressor implements IBitStreamUncompressor {

    public static @Nonnull BitStreamUncompressor identifyUncompressor(
            @Nonnull byte[] abBitstream)
            throws BinaryDataNotRecognized
    {
        return identifyUncompressor(abBitstream, abBitstream.length);
    }
    public static @Nonnull BitStreamUncompressor identifyUncompressor(
            @Nonnull byte[] abBitstream, int iBitstreamSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor bsu;
        bsu = BitStreamUncompressor_STRv2.makeV2NoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_STRv3.makeV3NoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_STRv1.makeV1NoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_Iki.makeIkiNoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_Lain.makeLainNoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_Panekit.makePanekitNoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        bsu = BitStreamUncompressor_StarWars.makeStarWarsNoThrow(abBitstream, iBitstreamSize);
        if (bsu != null)
            return bsu;
        throw new BinaryDataNotRecognized();
    }

    // #########################################################################

    public interface IAcEscapeCode {
        /** Read an AC Coefficient escaped (zero-run, AC level) value. */
        void readAcEscapeCode(@Nonnull ArrayBitReader bitReader, @Nonnull MdecCode code)
                throws MdecException.EndOfStream;
    }

    public interface IQuantizationDcReader {
        /** Read the quantization scale and DC coefficient from the bitstream. */
        void readQuantizationScaleAndDc(@Nonnull ArrayBitReader bitReader,
                                        @Nonnull MdecContext context,
                                        @Nonnull MdecCode out)
                throws MdecException.ReadCorruption, MdecException.EndOfStream;
    }

    public interface IFrameEndPaddingBits {
        /** @see #skipPaddingBits() */
        boolean skipPaddingBits(@Nonnull ArrayBitReader bitReader) throws MdecException.EndOfStream;
    }

    private static class FrameEndPaddingBits_None implements IFrameEndPaddingBits {
        @Override
        public boolean skipPaddingBits(@Nonnull ArrayBitReader bitReader) {
            return true;
        }
    }

    public static final IFrameEndPaddingBits FRAME_END_PADDING_BITS_NONE = new FrameEndPaddingBits_None();

    // #########################################################################

    /** Binary input stream being read. */
    @Nonnull
    private final ArrayBitReader _bitReader;

    /** Table for looking up AC Coefficient bit codes. */
    @Nonnull
    private final ZeroRunLengthAcLookup _lookupTable;

    @Nonnull
    private final IQuantizationDcReader _qscaleDcReader;

    @Nonnull
    private final IAcEscapeCode _escapeCodeReader;

    @Nonnull
    private final IFrameEndPaddingBits _endPaddingBits;

    protected final MdecContext _context = new MdecContext();

    /** Track the current Block's vector position to detect errors.  */
    private int _iCurrentBlockVectorPos = 0;

    /** Number of MDEC codes that have been read thus far. */
    public int getReadMdecCodeCount() { return _context.getTotalMdecCodesRead(); }

    public BitStreamUncompressor(@Nonnull ArrayBitReader bitReader,
                                 @Nonnull ZeroRunLengthAcLookup lookupTable,
                                 @Nonnull IQuantizationDcReader qscaleDcReader,
                                 @Nonnull IAcEscapeCode escapeCodeReader,
                                 @Nonnull IFrameEndPaddingBits endPaddingBits)
    {
        _bitReader = bitReader;
        _lookupTable = lookupTable;
        _qscaleDcReader = qscaleDcReader;
        _escapeCodeReader = escapeCodeReader;
        _endPaddingBits = endPaddingBits;
    }

    @Override
    final public int getBitPosition() {
        return _bitReader.getBitsRead();
    }

    @Override
    final public int getByteOffset() {
        return _bitReader.getCurrentShortPosition();
    }

    @Override
    final public boolean readMdecCode(@Nonnull MdecCode code) throws MdecException.EndOfStream, MdecException.ReadCorruption {

        assert !BitStreamDebugging.DEBUG || BitStreamDebugging.setPosition(_bitReader.getCurrentShortPosition());

        if (_context.atStartOfBlock()) {
            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.printStartOfBlock(_context);

            _qscaleDcReader.readQuantizationScaleAndDc(_bitReader, _context, code);
            _context.nextCode();
        } else {
            int i17bits = _bitReader.peekUnsignedBits(BitStreamCode.LONGEST_BITSTREAM_CODE_17BITS);
            ZeroRunLengthAc bitCode = _lookupTable.lookup(i17bits);
            if (bitCode == null) {
                String s = "Unmatched AC variable length code: " +
                           Misc.bitsToString(i17bits, BitStreamCode.LONGEST_BITSTREAM_CODE_17BITS) +
                           " " + this;
                throw new MdecException.ReadCorruption(s);
            }
            _bitReader.skipBits(bitCode.getBitLength());

            assert !BitStreamDebugging.DEBUG || BitStreamDebugging.appendBits(bitCode.getBitString());

            if (bitCode.isIsEndOfBlock()) {
                // end of block
                code.setToEndOfData();
                _iCurrentBlockVectorPos = 0;
                _context.nextCodeEndBlock();
            } else {
                // block continues
                if (bitCode.isIsEscapeCode()) {
                    _escapeCodeReader.readAcEscapeCode(_bitReader, code);
                } else {
                    bitCode.getMdecCode(code);
                }

                _iCurrentBlockVectorPos += code.getTop6Bits() + 1;
                if (_iCurrentBlockVectorPos >= 64) {
                    throw new MdecException.ReadCorruption(MdecException.RLC_OOB_IN_MB_BLOCK(_iCurrentBlockVectorPos, _context.getTotalMacroBlocksRead(), _context.getCurrentBlock().ordinal()));
                }

                _context.nextCode();
            }
        }

        assert !BitStreamDebugging.DEBUG || BitStreamDebugging.printBitsResult(_iCurrentBlockVectorPos, code);

        return _context.atStartOfBlock();
    }

    /** Skips macroblocks that would fit within the given dimensions. */
    final public void skipMacroBlocks(int iPixelWidth, int iPixelHeight) throws MdecException.EndOfStream, MdecException.ReadCorruption {
        int iBlocksToSkip = Calc.blocks(iPixelWidth, iPixelHeight);

        MdecCode code = new MdecCode();
        for (int i = 0; i < iBlocksToSkip; i++) {
            while (!readMdecCode(code)) {
            }
        }
    }

    /** STRv2 and STRv3 bitstreams have 10 extra padding bits that are necessary
     * when played by the game. This skips those bits and checks if they are as expected
     * (and for other bitstreams does nothing).
     * @return if the skipped bits match the expected padding bits. */
    @Override
    final public boolean skipPaddingBits() throws MdecException.EndOfStream {
        return _endPaddingBits.skipPaddingBits(_bitReader);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + _context + " offset=" + _bitReader.getCurrentShortPosition();
    }
}
