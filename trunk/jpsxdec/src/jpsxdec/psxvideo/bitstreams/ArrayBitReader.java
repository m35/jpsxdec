/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.Misc;

/** A (hopefully) very fast bit reader. It can be initialized to read the bits
 * in big-endian order, or in 16-bit little-endian order. */
public class ArrayBitReader {

    private static final Logger LOG = Logger.getLogger(ArrayBitReader.class.getName());

    /** Data to be read as a binary stream. */
    @Nonnull
    private byte[] _abData;
    /** Size of the data (ignores data array size). */
    protected int _iDataSize;
    /** If 16-bit words should be read in big or little endian order. */
    private boolean _blnLittleEndian;
    /** Offset of first byte in the current word being read from the source buffer. */
    protected int _iByteOffset;
    /** The current 16-bit word value from the source data. */
    protected short _siCurrentWord;
    /** Bits remaining to be read from the current word. */
    protected int _iBitsLeft;

    /** Quick lookup table to mask remaining bits. */
    private static final int BIT_MASK[] = {
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

    /** Performs no initialization. {@link #reset(byte[], int, boolean, int)}
     * needs to be called before using this class. */
    public ArrayBitReader() {
    }

    /** Start reading from the start of the array with the requested
     * endian-ness. */
    public ArrayBitReader(@Nonnull byte[] abData, int iDataSize, boolean blnLittleEndian)
    {
        this(abData, iDataSize, blnLittleEndian, 0);
    }

    /** Start reading from a requested point in the array with the requested
     *  endian-ness. 
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public ArrayBitReader(@Nonnull byte[] abData, int iDataSize, boolean blnLittleEndian, int iReadStart)
    {
        reset(abData, iDataSize, blnLittleEndian, iReadStart);
    }
    
    /** Re-constructs this ArrayBitReader. Allows for re-using the object
     *  so there is no need to create a new one.
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public final void reset(@Nonnull byte[] abData, int iDataSize, boolean blnLittleEndian, int iReadStart) {
        if (iReadStart < 0 || iReadStart > abData.length)
            throw new IllegalArgumentException("Read start out of array bounds.");
        if ((iReadStart & 1) != 0)
            throw new IllegalArgumentException("Data start must be on word boundary.");
        if (iDataSize < 0 || iDataSize > abData.length)
            throw new IllegalArgumentException("Invalid data size.");
        if ((iDataSize & 1) != 0)
            throw new IllegalArgumentException("Data length must be even.");
        _iByteOffset = iReadStart;
        _iDataSize = iDataSize;
        _abData = abData;
        _iBitsLeft = 0;
        _blnLittleEndian = blnLittleEndian;
    }

    /** Reads 16-bits at the requested offset in the proper endian order. */
    protected short readWord(int i) throws EOFException {
        if (i + 1 >= _iDataSize)
            throw new EOFException("Unexpected end of bitstream at " + i);
        int b1, b2;
        if (_blnLittleEndian) {
            b1 = _abData[i  ] & 0xFF;
            b2 = _abData[i+1] & 0xFF;
        } else {
            b1 = _abData[i+1] & 0xFF;
            b2 = _abData[i  ] & 0xFF;
        }
        return (short)((b2 << 8) + b1);
    }

    /** Returns the offset to the current word that the bit reader is reading. */
    public int getWordPosition() {
        return _iByteOffset;
    }
    
    public int getBitsRead() {
        return _iByteOffset * 8 - _iBitsLeft;
    }

    /** Returns the number of bits remaining in the source data. */
    public int getBitsRemaining() {
        return (_iDataSize - _iByteOffset) * 8 + _iBitsLeft;
    }

    /** Reads the requested number of bits.
     * @param iCount  expected to be from 1 to 31  */
    public int readUnsignedBits(int iCount) throws EOFException {
        if (iCount < 0 || iCount >= 32)
            throw new IllegalArgumentException("Bits to read are out of range " + iCount);
        if (iCount == 0)
            return 0;
        
        // want to read the next 16-bit word only when it is needed
        // so we don't try to buffer data beyond the array
        if (_iBitsLeft == 0) {
            _siCurrentWord = readWord(_iByteOffset);
            _iByteOffset += 2;
            _iBitsLeft = 16;
        }

        int iRet;
        if (iCount <= _iBitsLeft) { // iCount <= _iBitsLeft <= 16
            iRet = (_siCurrentWord >>> (_iBitsLeft - iCount)) & BIT_MASK[iCount];
            _iBitsLeft -= iCount;
        } else {
            iRet = _siCurrentWord & BIT_MASK[_iBitsLeft];
            iCount -= _iBitsLeft;
            _iBitsLeft = 0;

            try {
                while (iCount >= 16) {
                    iRet = (iRet << 16) | (readWord(_iByteOffset) & 0xFFFF);
                    _iByteOffset += 2;
                    iCount -= 16;
                }

                if (iCount > 0) { // iCount < 16
                    _siCurrentWord = readWord(_iByteOffset);
                    _iByteOffset += 2;
                    _iBitsLeft = 16 - iCount;
                    iRet = (iRet << iCount) | ((_siCurrentWord & 0xFFFF) >>> _iBitsLeft);
                }
            } catch (EOFException ex) {
                LOG.log(Level.INFO, "Bitstream is about to end", ex);
                // _iBitsLeft will == 0
                return iRet << iCount;
            }
        }

        return iRet;
    }
    
    /** Reads the requested number of bits then sets the sign 
     *  according to the highest bit.
     * @param iCount  expected to be from 0 to 31  */
    public int readSignedBits(int iCount) throws EOFException {
        return (readUnsignedBits(iCount) << (32 - iCount)) >> (32 - iCount); // extend sign bit
    }    
    
    /** @param iCount  expected to be from 1 to 31  */
    public int peekUnsignedBits(int iCount) throws EOFException {
        int iSaveOffs = _iByteOffset;
        int iSaveBitsLeft = _iBitsLeft;
        short siSaveCurrentWord = _siCurrentWord;
        try {
            return readUnsignedBits(iCount);
        } finally {
            _iByteOffset = iSaveOffs;
            _iBitsLeft = iSaveBitsLeft;
            _siCurrentWord = siSaveCurrentWord;
        }
    }
    
    /** @param iCount  expected to be from 0 to 31  */
    public int peekSignedBits(int iCount) throws EOFException {
        return (peekUnsignedBits(iCount) << (32 - iCount)) >> (32 - iCount); // extend sign bit
    }    
    
    public void skipBits(int iCount) throws EOFException {

        _iBitsLeft -= iCount;
        if (_iBitsLeft < 0) {
            // same as _iByteOffset += -(_iBitsLeft / 16)*2;
            _iByteOffset += ((-_iBitsLeft) >> 4) << 1;
            // same as _iBitsLeft = _iBitsLeft % 16;
            _iBitsLeft = -((-_iBitsLeft) & 0xf);
            if (_iByteOffset > _iDataSize) { // clearly out of bounds
                _iBitsLeft = 0;
                _iByteOffset = _iDataSize;
                throw new EOFException("Unexpected end of bitstream at " + _iByteOffset);
            } else if (_iBitsLeft < 0) { // _iBitsLeft should be <= 0
                if (_iByteOffset == _iDataSize) { // also out of bounds
                    _iBitsLeft = 0;
                    throw new EOFException("Unexpected end of bitstream at " + _iByteOffset);
                }
                _iBitsLeft += 16;
                _siCurrentWord = readWord(_iByteOffset);
                _iByteOffset += 2;
            }
        }
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public @Nonnull String peekBitsToString(int iCount) throws EOFException {
        int iBitsRemaining = getBitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(peekUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(peekUnsignedBits(iCount), iCount);
    }

    /** Returns a String of 1 and 0 unless at the end of the stream, then
     * returns only the remaining bits. */
    public @Nonnull String readBitsToString(int iCount) throws EOFException {
        int iBitsRemaining = getBitsRemaining();
        if (iBitsRemaining < iCount)
            return Misc.bitsToString(readUnsignedBits(iBitsRemaining), iBitsRemaining);
        else
            return Misc.bitsToString(readUnsignedBits(iCount), iCount);
    }

}
