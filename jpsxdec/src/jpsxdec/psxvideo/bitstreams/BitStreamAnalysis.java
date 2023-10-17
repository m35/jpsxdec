/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

import java.io.PrintStream;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.ParsedMdecImage;
import jpsxdec.util.BinaryDataNotRecognized;

/** Analyzes a bitstream and calculates frequently used values. */
public class BitStreamAnalysis {

    @Nonnull
    private final ParsedMdecImage _parsed;

    @Nonnull
    private final IBitStreamUncompressor _uncompressor;
    private final boolean _blnCorrectPaddingBits;

    @Nonnull
    private final byte[] _abBitStream;

    private final int _iWidth, _iHeight;

    public BitStreamAnalysis(@Nonnull byte[] abBitstream, int iWidth, int iHeight)
            throws BinaryDataNotRecognized, MdecException.ReadCorruption, MdecException.EndOfStream
    {
        this(abBitstream, BitStreamUncompressor.identifyUncompressor(abBitstream), iWidth, iHeight);
    }

    public BitStreamAnalysis(@Nonnull byte[] abBitstream, @Nonnull IBitStreamUncompressor uncompressor, int iWidth, int iHeight)
            throws MdecException.ReadCorruption, MdecException.EndOfStream
    {
        _abBitStream = abBitstream;
        _uncompressor = uncompressor;

        _parsed = new ParsedMdecImage(_uncompressor, iWidth, iHeight);
        _blnCorrectPaddingBits = _uncompressor.skipPaddingBits();

        _iWidth = iWidth;
        _iHeight = iHeight;
    }

    public @Nonnull MdecInputStream makeNewStream() {
        return _parsed.getStream();
    }

    public int getBitStreamArrayLength() {
        return _abBitStream.length;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public boolean hasCorrectPaddingBits() {
        return _blnCorrectPaddingBits;
    }

    public @Nonnull IBitStreamUncompressor getCompletedBitStream() {
        return _uncompressor;
    }

    public @Nonnull BitStreamCompressor makeBitStreamCompressor() {
        return _uncompressor.makeCompressor();
    }

    public <T extends IBitStreamUncompressor> boolean isBitStreamClass(@Nonnull Class<T> bitStreamClass) {
        return bitStreamClass.isAssignableFrom(_uncompressor.getClass());
    }

    public @Nonnull Class<? extends IBitStreamUncompressor> getBitStreamClass() {
        return _uncompressor.getClass();
    }

    public int getMdecCodeCount() {
        return _parsed.getMdecCodeCount();
    }

    public int calculateUsedBytesRoundUp4() {
        // The number of bytes actually used in a bitstream is a little tricky
        // since bytes are read (at least) 16-bits at a type, usually little-endian.
        // Additionally, the used bytes always seem to be rounded up to a
        // multiple of 4.

        // Calculate the number of used bytes using the bits read (round up to the nearest multiple of 4)
        int iBitByteOffset = ((_uncompressor.getBitPosition() + 31) & ~31) / 8;
        // Calculate the number of used bytes using the byte offset of the reader (round up to the nearest multiple of 4)
        int iReadOffset = (_uncompressor.getByteOffset() + 3) & ~3;

        // In theory this should be identical
        if (iBitByteOffset != iReadOffset)
            throw new RuntimeException("My logic is bad: " + iBitByteOffset + " != " + iReadOffset);

        return iReadOffset;
    }

    /** "Min" means the number of used bytes is *at least* the value returned.
     * The number of actually used bytes could be a little more.
     * This value can be useful if you want to limit the number of bytes
     * to replace in the frame to only the number of bytes used. */
    public int calculateMinUsedBytes() {
        // Calculate the number of used bytes using the bits read (round down to the nearest multiple of 4)
        int iBitByteOffset = (_uncompressor.getBitPosition() / 8) & ~3;
        // Calculate the number of used bytes using the byte offset of the reader (round down to the nearest multiple of 4)
        int iReadOffset = _uncompressor.getByteOffset() & ~3;

        // In theory this should be identical
        if (iBitByteOffset != iReadOffset)
            throw new RuntimeException("My logic is bad: " + iBitByteOffset + " != " + iReadOffset);

        return iReadOffset;
    }

    /** Find how many bytes are used in the frame until all that's left is zeroes.
     * Or in other words, how many zeroes are at the end of the frame,
     * subtracted from the size of the frame. */
    public int calculateNonZeroBytes() {
        int i = _abBitStream.length - 1;
        for (; i >= 0; i--) {
            if (_abBitStream[i] != 0)
                break;
        }
        return i+1;
    }

    /** @see Calc#calculateHalfCeiling32(int)  */
    public short calculateMdecHalfCeiling32() {
        return Calc.calculateHalfCeiling32(getMdecCodeCount());
    }

    /**
     * @throws UnsupportedOperationException if the underlying bitstream type does
     *                                       not have a single quantizaiton scale
     *                                       for the whole frame
     * @see IBitStreamWith1QuantizationScale
     */
    public int getFrameQuantizationScale() {
        if (_uncompressor instanceof IBitStreamWith1QuantizationScale)
            return ((IBitStreamWith1QuantizationScale)_uncompressor).getQuantizationScale();
        throw new UnsupportedOperationException();
    }

    public void arrayCopy(int iSrcPos, @Nonnull byte[] abDest, int iDestPos, int iLength) {
        System.arraycopy(_abBitStream, iSrcPos, abDest, iDestPos, iLength);
    }

    public void printInfo(@Nonnull PrintStream ps) {
        if (!_blnCorrectPaddingBits)
            ps.println("Incorrect padding bits!");
        ps.println("Frame data info: " + _uncompressor);
    }

    public void drawMacroBlocks(@Nonnull PrintStream ps) {
        _parsed.drawMacroBlocks(ps);
    }
}
