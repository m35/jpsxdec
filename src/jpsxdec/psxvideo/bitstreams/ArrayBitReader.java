/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

/** A (hopefully) very fast bit reader. */
public class ArrayBitReader {
    
    private final static byte LITTLE_ENDIAN = 2;
    private final static byte BIG_ENDIAN    = 0;
    
    private static final byte[] INC = { 1, 1,   
                                        3, -1};
    
    private byte _bEndian;
    private int _iByteOffset;
    private int _iBitsLeft;
    private byte[] _abData;

    
    private static int BIT_MASK[] = new int[] {
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
        0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF,
        0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    };

    /** Performs no initilization. {@link #reset(byte[], boolean, int)}
     * needs to be called before using this class. */
    public ArrayBitReader() {
        
    }

    /** Start reading from the start of the array as little-endian. */
    public ArrayBitReader(byte[] abData) {
        this(abData, true, 0);
    }

    /** Start reading from a requested point in the array with the requested
     *  endian-ness. 
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public ArrayBitReader(byte[] abDemux, boolean blnLittleEndian, int iReadStart) {
        reset(abDemux, blnLittleEndian, iReadStart);
    }
    
    /** Re-constructs this ArrayBitReader. Allows for re-using the object
     *  so there is no need to create a new one.
     *  @param iReadStart  Position in array to start reading. Must be an even number. */
    public void reset(byte[] abDemux, boolean blnLittleEndian, int iReadStart) {
        if ((iReadStart & 1) != 0)
            throw new IllegalArgumentException("Data start must be on word boundary.");
        _abData = abDemux;
        _iBitsLeft = 8;
        if (blnLittleEndian) {
            _bEndian = LITTLE_ENDIAN;
            _iByteOffset = iReadStart | 1;
        } else {
            _bEndian = BIG_ENDIAN;
            _iByteOffset = iReadStart;
        }
    }

    /** Set the endian-ness of the bit reading. */
    public void setLittleEndian(boolean bln) {
        if (bln && _bEndian == BIG_ENDIAN) {
            _bEndian = LITTLE_ENDIAN;
            if ((_iByteOffset & 1) == 0)
                _iByteOffset |= 1;  // set least-significant bit
            else
                _iByteOffset &= (~1); // clear least-significant bit
        } else if (!bln && _bEndian == LITTLE_ENDIAN) {
            _bEndian = BIG_ENDIAN;
            if ((_iByteOffset & 1) == 0)
                _iByteOffset |= 1;  // set least-significant bit
            else
                _iByteOffset &= (~1); // clear least-significant bit
        }
    }

    /** Returns the current byte that the bit reader is pointing to.
     *  This value may fluxuate for little-endian streams. */
    public int getPosition() {
        if (_bEndian == LITTLE_ENDIAN)
            return _iByteOffset & (~1);
        else
            return _iByteOffset;
    }
    
    /** Reads the requested number of bits. */
    public long readUnsignedBits(int iCount) throws EOFException {
        assert iCount >= 0 && iCount <= 31;
        
        long lngRet = 0;
        if (iCount <= _iBitsLeft) {
            try {
                lngRet = (_abData[_iByteOffset] & BIT_MASK[_iBitsLeft]) >>> (_iBitsLeft - iCount);
                if ((_iBitsLeft -= iCount) == 0) {
                    _iBitsLeft = 8;
                    _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }
        } else {
            try {
                iCount -= _iBitsLeft;
                lngRet = (_abData[_iByteOffset] & BIT_MASK[_iBitsLeft]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }
            
            _iBitsLeft = 8;
            _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];

            try {
                while (iCount >= 8) {
                    lngRet = (lngRet << 8) | (_abData[_iByteOffset] & 0xFF);
                    _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];
                    iCount -= 8;
                }

                if (iCount > 0) {
                    _iBitsLeft -= iCount;
                    lngRet = (lngRet << iCount) | ((_abData[_iByteOffset] & 0xFF) >>> _iBitsLeft);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return lngRet << iCount;
            }
            
        }

        return lngRet;
    }
    
    /** Reads the requested number of bits then sets the sign 
     *  according to the highest bit. */
    public long readSignedBits(int iCount) throws EOFException {
        return (readUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // change to signed
    }    
    
    public long peekUnsignedBits(int iCount) throws EOFException {
        assert iCount >= 0 && iCount <= 31;
        
        int iTmpOffs = _iByteOffset;
        int iTmpBitsLeft = _iBitsLeft;
        
        long lngRet = 0;
        
        if (iCount <= iTmpBitsLeft) {
            try {
                lngRet = (_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]) >>> (iTmpBitsLeft - iCount);
                if ((iTmpBitsLeft -= iCount) == 0) {
                    iTmpBitsLeft = 8;
                    iTmpOffs += INC[_bEndian + (iTmpOffs & 1)];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException("Unexpected end of bit-stream.");
            }
        } else {
            try {
                iCount -= iTmpBitsLeft;
                lngRet = (_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException("Unexpected end of bit-stream.");
            }

            iTmpBitsLeft = 8;
            iTmpOffs += INC[_bEndian + (iTmpOffs & 1)];

            try {
                while (iCount >= 8) {
                    lngRet = (lngRet << 8) | (_abData[iTmpOffs] & 0xFF);
                    iTmpOffs += INC[_bEndian + (iTmpOffs & 1)];
                    iCount -= 8;
                }

                if (iCount > 0) {
                    iTmpBitsLeft -= iCount;
                    lngRet = (lngRet << iCount) | ((_abData[iTmpOffs] & 0xFF) >>> iTmpBitsLeft);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return lngRet << iCount;
            }
        }

        return lngRet;
    }
    
    public long peekSignedBits(int iCount) throws EOFException {
        return (peekUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // change to signed
    }    
    
    public void skipBits(int iCount) {
        
        if (iCount <= _iBitsLeft) {
            if ((_iBitsLeft -= iCount) == 0) {
                _iBitsLeft = 8;
                _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];
            }
        } else {
            iCount -= _iBitsLeft;
            
            _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];
            
            while (iCount >= 8) {
                _iByteOffset += INC[_bEndian + (_iByteOffset & 1)];
                iCount -= 8;
            }
            
            _iBitsLeft = 8 - iCount;
            
        }
        
    }
    
    /** Test this class. */
    public static void main(String[] args) throws EOFException {
        ArrayBitReader abr = new ArrayBitReader(new byte[] {
            (byte)0xFF,
            (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF,
        });
        
        long l;
        l = abr.readUnsignedBits(31);
        l = abr.readUnsignedBits(3);
        l = abr.readUnsignedBits(3);
        l = abr.readUnsignedBits(3);
    }

}
