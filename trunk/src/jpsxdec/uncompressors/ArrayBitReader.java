/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
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

package jpsxdec.uncompressors;

import java.io.EOFException;


public class ArrayBitReader {
    private byte m_bLittleEndian = 2;
    private int m_iByteOffset = 1;
    private int m_iBitsLeft = 8;
    private byte[] m_abData;

    
    private static int BIT_MASK[] = new int[] {
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
        0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF,
        0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    };
    
    public ArrayBitReader(byte[] m_abData) {
        this.m_abData = m_abData;
    }
    
    public void setLittleEndian(boolean b) {
        if (b && m_bLittleEndian == 0) {
            m_bLittleEndian = 2;
            if ((m_iByteOffset & 1) == 0)
                m_iByteOffset |= 1;
            else
                m_iByteOffset &= (~1<<1);
        } else if (!b && m_bLittleEndian > 0) {
            m_bLittleEndian = 0;
            if ((m_iByteOffset & 1) == 0)
                m_iByteOffset |= 1;
            else
                m_iByteOffset &= (~1<<1);
        }
    }
    
    private static final byte[] INC = { 1, 1,   
                                        3, -1};
    
    // 8|7|6|5|4|3|2|1
    public long ReadUnsignedBits(int iCount) throws EOFException {
        //if (iCount < 0 || iCount > 31) throw new IllegalArgumentException();
        
        long lngRet = 0;
        if (iCount <= m_iBitsLeft) {
            try {
                lngRet = (m_abData[m_iByteOffset] & BIT_MASK[m_iBitsLeft]) >>> (m_iBitsLeft - iCount);
                if ((m_iBitsLeft -= iCount) == 0) {
                    m_iBitsLeft = 8;
                    m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }
        } else {
            try {
                iCount -= m_iBitsLeft;
                lngRet = (m_abData[m_iByteOffset] & BIT_MASK[m_iBitsLeft]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }
            
            m_iBitsLeft = 8;
            m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];

            try {
                while (iCount >= 8) {
                    lngRet = (lngRet << 8) | (m_abData[m_iByteOffset] & 0xFF);
                    m_iByteOffset += INC[m_bLittleEndian + (m_iByteOffset & 1)];
                    iCount -= 8;
                }

                if (iCount > 0) {
                    m_iBitsLeft -= iCount;
                    lngRet = (lngRet << iCount) | ((m_abData[m_iByteOffset] & 0xFF) >>> m_iBitsLeft);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return lngRet << iCount;
            }
            
        }

        return lngRet;
    }
    
    public long ReadSignedBits(int iCount) throws EOFException {
        return (ReadUnsignedBits(iCount) << (64 - iCount)) >> (64 - iCount); // change to signed
    }    
    
    public long PeekUnsignedBits(int iCount) throws EOFException {
        //if (iCount < 0 || iCount > 31) throw new IllegalArgumentException();
        
        int iTmpOffs = m_iByteOffset;
        int iTmpBitsLeft = m_iBitsLeft;
        
        long lngRet = 0;
        
        if (iCount <= iTmpBitsLeft) {
            try {
                lngRet = (m_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]) >>> (iTmpBitsLeft - iCount);
                if ((iTmpBitsLeft -= iCount) == 0) {
                    iTmpBitsLeft = 8;
                    iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }
        } else {
            try {
                iCount -= iTmpBitsLeft;
                lngRet = (m_abData[iTmpOffs] & BIT_MASK[iTmpBitsLeft]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new EOFException();
            }

            iTmpBitsLeft = 8;
            iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];

            try {
                while (iCount >= 8) {
                    lngRet = (lngRet << 8) | (m_abData[iTmpOffs] & 0xFF);
                    iTmpOffs += INC[m_bLittleEndian + (iTmpOffs & 1)];
                    iCount -= 8;
                }

                if (iCount > 0) {
                    iTmpBitsLeft -= iCount;
                    lngRet = (lngRet << iCount) | ((m_abData[iTmpOffs] & 0xFF) >>> iTmpBitsLeft);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                return lngRet << iCount;
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
