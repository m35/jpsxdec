/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * ArrayBitReader.java
 */

package jpsxdec.videodecoding;

import java.io.EOFException;

/** If you thought the BufferedBitReader was fast, this blows it away. */
public class ArrayBitReader {
    
    private final static byte LITTLE_ENDIAN = 2;
    private final static byte BIG_ENDIAN    = 0;
    
    private static final byte[] INC = { 1, 1,   
                                        3, -1};
    
    private byte m_bLittleEndian;
    private int m_iByteOffset;
    private int m_iBitsLeft;
    private int m_iEndOfData;
    private byte[] m_abData;

    
    private static int BIT_MASK[] = new int[] {
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
        0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF,
        0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    };
    
    /** Start reading from the start of the array as little-endian. */
    public ArrayBitReader(byte[] abData) {
        this(abData, true, 0, abData.length);
    }

    /** Start reading from a requested point in the array with the requested
     *  endian-ness. 
     * @param iReadStart  Start of data reading. */
    public ArrayBitReader(byte[] abDemux, boolean blnLittleEndian, int iReadStart, int iDataLen) {
        Reset(abDemux, blnLittleEndian, iReadStart, iDataLen);
    }
    
    /**  */
    public void Reset(byte[] abDemux, boolean blnLittleEndian, int iReadStart, int iDataLen) {
        if ((iReadStart & 1) != 0)
            throw new IllegalArgumentException("Data start must be on word boundary.");
        m_abData = abDemux;
        m_iBitsLeft = 8;
        m_iEndOfData = iDataLen;
        if (blnLittleEndian) {
            m_bLittleEndian = LITTLE_ENDIAN;
            m_iByteOffset = iReadStart | 1;
        } else {
            m_bLittleEndian = BIG_ENDIAN;
            m_iByteOffset = iReadStart;
        }
    }

    /** Set the endian-ness of the bit reading. */
    public void setLittleEndian(boolean b) {
        if (b && m_bLittleEndian == BIG_ENDIAN) {
            m_bLittleEndian = LITTLE_ENDIAN;
            if ((m_iByteOffset & 1) == 0)
                m_iByteOffset |= 1;  // set least-significant bit
            else
                m_iByteOffset &= (~1); // clear least-significant bit
        } else if (!b && m_bLittleEndian == LITTLE_ENDIAN) {
            m_bLittleEndian = BIG_ENDIAN;
            if ((m_iByteOffset & 1) == 0)
                m_iByteOffset |= 1;  // set least-significant bit
            else
                m_iByteOffset &= (~1); // clear least-significant bit
        }
    }
    
    /** Reads the requested number of bits. */
    public long ReadUnsignedBits(int iCount) throws EOFException {
        //if (iCount < 0 || iCount > 31) throw new IllegalArgumentException();
        if (m_iByteOffset >= m_iEndOfData) throw new EOFException("ArrayBitReader unexpected end of stream.");
        
        long lngRet = 0;
        if (iCount <= m_iBitsLeft) {
            lngRet = (m_abData[m_iByteOffset] & BIT_MASK[m_iBitsLeft]) >>> (m_iBitsLeft - iCount);
            if ((m_iBitsLeft -= iCount) == 0) {
                m_iBitsLeft = 8;
                m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
            }
        } else {
            iCount -= m_iBitsLeft;
            lngRet = (m_abData[m_iByteOffset] & BIT_MASK[m_iBitsLeft]);
            
            m_iBitsLeft = 8;
            m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];

            while (iCount >= 8) {
                if (m_iByteOffset >= m_iEndOfData) return lngRet << iCount;
                lngRet = (lngRet << 8) | (m_abData[m_iByteOffset] & 0xFF);
                m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
                iCount -= 8;
            }

            if (iCount > 0) {
                m_iBitsLeft -= iCount;
                if (m_iByteOffset >= m_iEndOfData) return lngRet << iCount;
                lngRet = (lngRet << iCount) | ((m_abData[m_iByteOffset] & 0xFF) >>> m_iBitsLeft);
            }
            
        }

        return lngRet;
    }
    
    /** Reads the requested number of bits then sets the sign 
     *  according to the highest bit. */
    public long ReadSignedBits(int iCount) throws EOFException {
        return (ReadUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // change to signed
    }    
    
    public long PeekUnsignedBits(int iCount) throws EOFException {
        //if (iCount < 0 || iCount > 31) throw new IllegalArgumentException();
        
        if (m_iByteOffset >= m_iEndOfData) throw new EOFException("ArrayBitReader unexpected end of stream.");
        
        int iTmpOffs = m_iByteOffset;
        int iTmpBitsLeft = m_iBitsLeft;
        
        long lngRet = 0;
        
        if (iCount <= iTmpBitsLeft) {
            lngRet = (m_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]) >>> (iTmpBitsLeft - iCount);
            if ((iTmpBitsLeft -= iCount) == 0) {
                iTmpBitsLeft = 8;
                iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];
            }
        } else {
            iCount -= iTmpBitsLeft;
            lngRet = (m_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]);

            iTmpBitsLeft = 8;
            iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];

            while (iCount >= 8) {
                if (m_iByteOffset >= m_iEndOfData) return lngRet << iCount;
                lngRet = (lngRet << 8) | (m_abData[iTmpOffs] & 0xFF);
                iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];
                iCount -= 8;
            }

            if (iCount > 0) {
                iTmpBitsLeft -= iCount;
                if (m_iByteOffset >= m_iEndOfData) return lngRet << iCount;
                lngRet = (lngRet << iCount) | ((m_abData[iTmpOffs] & 0xFF) >>> iTmpBitsLeft);
            }
        }

        return lngRet;
    }
    
    public long PeekSignedBits(int iCount) throws EOFException {
        return (PeekUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // change to signed
    }    
    
    public void SkipBits(int iCount) {
        
        if (iCount <= m_iBitsLeft) {
            if ((m_iBitsLeft -= iCount) == 0) {
                m_iBitsLeft = 8;
                m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
            }
        } else {
            iCount -= m_iBitsLeft;
            
            m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
            
            while (iCount >= 8) {
                m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
                iCount -= 8;
            }
            
            m_iBitsLeft = 8 - iCount;
            
        }
        
    }
    
    /** Test this class. */
    public static void main(String[] args) throws EOFException {
        ArrayBitReader abr = new ArrayBitReader(new byte[] {(byte)0xFF, 
        (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF,
        });
        
        long l;
        l = abr.ReadUnsignedBits(31);
        l = abr.ReadUnsignedBits(3);
        l = abr.ReadUnsignedBits(3);
        l = abr.ReadUnsignedBits(3);
    }
    
}
