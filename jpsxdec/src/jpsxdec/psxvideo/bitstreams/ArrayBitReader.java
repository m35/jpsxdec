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
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.Misc;

/** A (hopefully) very fast bit reader.
 *  The order the bytes read are determined by a {@link IByteOrder}. */
public class ArrayBitReader {

    private static final Logger LOG = Logger.getLogger(ArrayBitReader.class.getName());

    /** Data to be read as a binary stream. */
    @Nonnull
    private final byte[] _abData;
    @Nonnull
    private final IByteOrder _byteOrder;

    private final int _iStartOffset;
    protected final int _iEndOffset;

    protected int _iCurrentOffset = 0;

    /** The current 16-bit short value from the source data. */
    protected short _siCurrentShort;
    /** Bits remaining to be read from the current short. */
    protected int _iBitsLeft = 0;

    /** Quick lookup table to mask remaining bits. */
    private static final int[] BIT_MASK = {
        0x00000000,
        0x00000001, 0x00000003, 0x00000007, 0x0000000F,
        0x0000001F, 0x0000003F, 0x0000007F, 0x000000FF,
        0x000001FF, 0x000003FF, 0x000007FF, 0x00000FFF,
        0x00001FFF, 0x00003FFF, 0x00007FFF, 0x0000FFFF,
        0x0001FFFF, 0x0003FFFF, 0x0007FFFF, 0x000FFFFF,
        0x001FFFFF, 0x003FFFFF, 0x007FFFFF, 0x00FFFFFF,
        0x01FFFFFF, 0x03FFFFFF, 0x07FFFFFF, 0x0FFFFFFF,
        0x1FFFFFFF, 0x3FFFFFFF, 0x7FFFFFFF, 0xFFFFFFFF,
    };

    /** Start reading from a requested point in the array.
     *  @param iStartOffset  Must be an even number. */
    public ArrayBitReader(@Nonnull byte[] abData, @Nonnull IByteOrder byteOrder, int iStartOffset, int iEndOffset)
    {
        if (iStartOffset < 0 || iStartOffset > abData.length)
            throw new IllegalArgumentException("Read start out of array bounds.");
        if ((iStartOffset & 1) != 0)
            throw new IllegalArgumentException("Start offset must be a multiple of 2.");
        if (iEndOffset < 0 || iEndOffset > abData.length)
            throw new IllegalArgumentException("Invalid data size " + iEndOffset);
        _iEndOffset = iEndOffset & ~1; // trim off an extra byte if the size is not an even value
        if (_iEndOffset != iEndOffset)
            LOG.log(Level.WARNING, "Bitstream end offset is an odd number {0}, rounding down to even number", iEndOffset);
        _iStartOffset = iStartOffset;
        _abData = abData;
        _byteOrder = byteOrder;
    }

    /** Reads 16-bits at the requested offset. */
    protected short readShort(int iByteIndex) throws MdecException.EndOfStream {
        int iOffset1 = _iStartOffset + _byteOrder.getByteOffset(iByteIndex);
        int iOffset2 = _iStartOffset + _byteOrder.getByteOffset(iByteIndex+1);

        if (iOffset1 >= _iEndOffset || iOffset2 >= _iEndOffset)
            throw new MdecException.EndOfStream(MdecException.END_OF_BITSTREAM(iByteIndex));

        int b1 = _abData[iOffset1] & 0xff;
        int b2 = _abData[iOffset2] & 0xff;

        return (short)((b1 << 8) | b2);
    }

    /** Returns the offset to the current short that the bit reader is reading. */
    public int getCurrentShortPosition() {
        return _iStartOffset + _iCurrentOffset;
    }

    public int getBitsRead() {
        return (_iStartOffset + _iCurrentOffset) * 8 - _iBitsLeft;
    }

    /** Returns the number of bits remaining in the source data. */
    public int getBitsRemaining() {
        return (_iEndOffset - _iStartOffset - _iCurrentOffset) * 8 + _iBitsLeft;
    }

    /** Reads the requested number of bits.
     * @param iCount  expected to be from 1 to 31  */
    public int readUnsignedBits(int iCount) throws MdecException.EndOfStream {
        if (iCount < 0 || iCount >= 32)
            throw new IllegalArgumentException("Bits to read are out of range " + iCount);
        if (iCount == 0)
            return 0;

        // want to read the next 16-bit short only when it is needed
        // so we don't try to buffer data beyond the array
        if (_iBitsLeft == 0) {
            _siCurrentShort = readShort(_iCurrentOffset);
            _iCurrentOffset += 2;
            _iBitsLeft = 16;
        }

        int iRet;
        if (iCount <= _iBitsLeft) { // iCount <= _iBitsLeft <= 16
            iRet = (_siCurrentShort >>> (_iBitsLeft - iCount)) & BIT_MASK[iCount];
            _iBitsLeft -= iCount;
        } else {
            iRet = _siCurrentShort & BIT_MASK[_iBitsLeft];
            iCount -= _iBitsLeft;
            _iBitsLeft = 0;

            try {
                while (iCount >= 16) {
                    iRet = (iRet << 16) | (readShort(_iCurrentOffset) & 0xFFFF);
                    _iCurrentOffset += 2;
                    iCount -= 16;
                }

                if (iCount > 0) { // iCount < 16
                    _siCurrentShort = readShort(_iCurrentOffset);
                    _iCurrentOffset += 2;
                    _iBitsLeft = 16 - iCount;
                    iRet = (iRet << iCount) | ((_siCurrentShort & 0xFFFF) >>> _iBitsLeft);
                }
            } catch (MdecException.EndOfStream ex) {
                LOG.log(Level.FINE, "Bitstream is about to end", ex);
                // _iBitsLeft will == 0
                return iRet << iCount;
            }
        }

        return iRet;
    }

    /** Reads the requested number of bits then sets the sign
     *  according to the highest bit.
     * @param iCount  expected to be from 0 to 31  */
    public int readSignedBits(int iCount) throws MdecException.EndOfStream {
        return (readUnsignedBits(iCount) << (32 - iCount)) >> (32 - iCount); // extend sign bit
    }

    /** @param iCount  expected to be from 1 to 31  */
    public int peekUnsignedBits(int iCount) throws MdecException.EndOfStream {
        int iSaveOffs = _iCurrentOffset;
        int iSaveBitsLeft = _iBitsLeft;
        short siSaveCurrentShort = _siCurrentShort;
        try {
            return readUnsignedBits(iCount);
        } finally {
            _iCurrentOffset = iSaveOffs;
            _iBitsLeft = iSaveBitsLeft;
            _siCurrentShort = siSaveCurrentShort;
        }
    }

    /** @param iCount  expected to be from 0 to 31  */
    public int peekSignedBits(int iCount) throws MdecException.EndOfStream {
        return (peekUnsignedBits(iCount) << (32 - iCount)) >> (32 - iCount); // extend sign bit
    }

    public void skipBits(int iCount) throws MdecException.EndOfStream {

        _iBitsLeft -= iCount;
        if (_iBitsLeft < 0) {
            // same as _iByteOffset += -(_iBitsLeft / 16)*2;
            _iCurrentOffset += ((-_iBitsLeft) >> 4) << 1;
            // same as _iBitsLeft = _iBitsLeft % 16;
            _iBitsLeft = -((-_iBitsLeft) & 0xf);
            if (_iCurrentOffset > _iEndOffset) { // clearly out of bounds
                _iBitsLeft = 0;
                _iCurrentOffset = _iEndOffset;
                throw new MdecException.EndOfStream(MdecException.END_OF_BITSTREAM(_iCurrentOffset));
            } else if (_iBitsLeft < 0) { // _iBitsLeft should be <= 0
                if (_iCurrentOffset == _iEndOffset) { // also out of bounds
                    _iBitsLeft = 0;
                    throw new MdecException.EndOfStream(MdecException.END_OF_BITSTREAM(_iCurrentOffset));
                }
                _iBitsLeft += 16;
                _siCurrentShort = readShort(_iCurrentOffset);
                _iCurrentOffset += 2;
            }
        }
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public @Nonnull String peekBitsToString(int iCount) throws MdecException.EndOfStream {
        int iBitsRemaining = getBitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(peekUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(peekUnsignedBits(iCount), iCount);
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public @Nonnull String readBitsToString(int iCount) throws MdecException.EndOfStream {
        int iBitsRemaining = getBitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(readUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(readUnsignedBits(iCount), iCount);
    }

}
