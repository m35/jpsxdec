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
 * BufferedBitReader.java
 *
 */


package jpsxdec.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.LinkedList;

/** A limited bit reader. Wraps an InputStream. Should be pretty quick. */
public class BufferedBitReader /* might extend FilterInputStream */ {

    
    private static class SimpleIntQueue {
        int[] m_abBytes;
        int m_iHeadPos = 0;
        int m_iTailPos = 0;
        
        private SimpleIntQueue() {}
        
        public SimpleIntQueue(int iSize) {
            m_abBytes = new int[iSize];
        }
        
        public void Queue(int i) {
            m_abBytes[m_iTailPos] = i;
            m_iTailPos = (m_iTailPos + 1) % m_abBytes.length;
            if (m_iTailPos == m_iHeadPos)
                throw new BufferOverflowException();
        }
        
        public int Peek() {
            if (m_iTailPos == m_iHeadPos)
                throw new BufferUnderflowException();
            return m_abBytes[m_iHeadPos];
        }
        
        public int Dequeue() {
            if (m_iTailPos == m_iHeadPos)
                throw new BufferUnderflowException();
            int i = m_abBytes[m_iHeadPos];
            m_iHeadPos = (m_iHeadPos + 1) % m_abBytes.length;
            return i;
        }
        
        public SimpleIntQueue ShallowCopy() {
            SimpleIntQueue oNew = new SimpleIntQueue();
            oNew.m_abBytes = m_abBytes;
            oNew.m_iHeadPos = m_iHeadPos;
            oNew.m_iTailPos = m_iTailPos;
            return oNew;
        }
        
        public int size() {
            int i = m_iTailPos - m_iHeadPos;
            if (i < 0)
                return m_abBytes.length + i - 1;
            else
                return i;
        }
    }
    
    
    /** Mask remaining bits */
    private static byte BIT_MASK[] = new byte[] {
        (byte)0x00, (byte)0x01, (byte)0x03, (byte)0x07, (byte)0x0F,
        (byte)0x1F, (byte)0x3F, (byte)0x7F, (byte)0xFF
    };
    
    /** A queue to store the bytes read */
    SimpleIntQueue m_oBitBuffer = new SimpleIntQueue(10);
    int m_iBitsRemainging = 0;
    
    /** The InputStream */
    InputStream m_oStrStream;
    
    /** Bytes per read has special behavior:
     *  1 means 1 byte per read (simple).
     *  2 means 2 bytes per read, but in little endian order. */
    int m_iBytesPerBuffer;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Main constructor. Takes a regular InputStream. */
    public BufferedBitReader(InputStream oIS, int iBytesPerRead) {
        assert(iBytesPerRead == 1 || iBytesPerRead == 2);
        m_iBytesPerBuffer = iBytesPerRead;
        m_oStrStream = oIS;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Gets/sets wheither to read the data as a stream of bits (therefore
     * big-endian), or to read in 16 bits at a time in little-endian order. */
    public int getBytesPerBuffer() {
        return m_iBytesPerBuffer;
    }
    
    public void setBytesPerBuffer(int iBytesPerRead) {
        assert(iBytesPerRead == 1 || iBytesPerRead == 2);
        m_iBytesPerBuffer = iBytesPerRead;
    }
    
    //..........................................................................
    
    /** If the base InputStream implements the getFilePointer method, then we
     *  can lookup the position.
     *  Returns the fractional position within the original input stream.
     *  If InputStream does not implement getFilePointer, or if failure,
     *  returns -1.0f */
    public double getPosition() {
        if (m_oStrStream instanceof IGetFilePointer) {
            long lngFilePos = ((IGetFilePointer)m_oStrStream).getFilePointer();
            return (double)lngFilePos - ((double)m_oBitBuffer.size() - 1
                    + (double)m_iBitsRemainging / 8.0);
        } else
            return -1.0f;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Public Functions ----------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Reads in the bits and returns the unsigned value */
    public long ReadUnsignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 8 - ((iCount - m_iBitsRemainging) % 8);
        return lngRet;
    }
    
    /** First reads the bits in as unsigned, then converts to signed using
     *  bit shifting. */
    public long ReadSignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 8 - ((iCount - m_iBitsRemainging) % 8);
        return (lngRet << (64 - iCount)) >> (64 - iCount);
    }
    
    /** Reads the bits into a string of "1" and "0" */
    public String ReadBitsToString(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        iCount = BufferEnoughForBits(iCount);
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 8 - ((iCount - m_iBitsRemainging) % 8);
        return PadZeroLeft(Long.toBinaryString(lngRet), iCount);
    }
    
    // .........................................................................
    
    /** Same as ReadUnsignedBits, but doesn't
     *  move the stream position forward  */
    public long PeekUnsignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return lngRet;
    }
    
    /** Same as ReadSignedBits, but doesn't
     *  move the stream position forward  */
    public long PeekSignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return (lngRet << (64 - iCount)) >> (64 - iCount);
    }
    
    /** Same as ReadBitsToString, but doesn't
     *  move the stream position forward  */
    public String PeekBitsToString(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        iCount = BufferEnoughForBits(iCount);
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return PadZeroLeft(Long.toBinaryString(lngRet), iCount);
    }
    
    // .........................................................................
    
    /** Simple skipping bits */
    public void SkipBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        iCount = BufferEnoughForBits(iCount);
        if (iCount <= m_iBitsRemainging) {
            m_iBitsRemainging -= iCount;
        } else {
            
            iCount -= m_iBitsRemainging;
            m_oBitBuffer.Dequeue();
            
            while (iCount >= 8) {
                m_oBitBuffer.Dequeue();
                iCount -= 8;
            }
            m_iBitsRemainging = 8 - iCount;
        }
    }
    
    /* ---------------------------------------------------------------------- */
    /* Private Functions ---------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Does all the bit reading for the public functions, using either
     *  the actual byte buffer (for reading bits), or a clone of the byte buffer
     *  (for peeking bits) */
    private static long ReadUBits(int iBitsRemainging,
                                  int iCount,
                                  SimpleIntQueue oBitBuffer) 
    {
        
        long lngRet = 0;
        int ib;
        if (iCount <= iBitsRemainging) {
            ib = oBitBuffer.Peek();      // 01234567
            ib = ib & BIT_MASK[iBitsRemainging];
            lngRet = (ib >>> (iBitsRemainging - iCount));
        } else {
            ib = oBitBuffer.Dequeue();
            lngRet = ib & BIT_MASK[iBitsRemainging];
            iCount -= iBitsRemainging;
            
            while (iCount >= 8) {
                lngRet = (lngRet << 8) | oBitBuffer.Dequeue();
                iCount -= 8;
            }
            
            if (iCount > 0) {
                ib = oBitBuffer.Peek();
                lngRet = (lngRet << iCount) | (ib >>> (8 - iCount));
            }
        }
        
        return lngRet;
        
    }
    
    // .........................................................................
    
    /** Used by the read/peek string functions. Adds the left zeros to the
     *  string */
    private static String PadZeroLeft(String s, int len) {
        int slen = s.length();
        
        assert(slen <= len);
        
        if (slen == len)
            return s;
        
        char buf[] = new char[len - slen];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = '0';
        }
        return new String(buf) + s;
    }
    
    /** First fixes m_iBitsRemainging so it is not zero,
     *  then buffers enough bytes for the requested bits.
     *  Return the number of bits that were actually buffered. */
    private int BufferEnoughForBits(int iCount) throws IOException {
        if (m_iBitsRemainging == 0) { // no bits remain for the head of the buffer
            // need to get rid of the head of the buffer
            if (m_oBitBuffer.size() > 0) { // but but only if the buffer isn't empty
                m_oBitBuffer.Dequeue(); // get rid of the used up head
            }
            if (m_oBitBuffer.size() == 0) { // now if the buffer is empty
                // add a little more to the buffer
                if (BufferData() < 1) throw new EOFException("End of bit file");
            }
            m_iBitsRemainging = 8; // and now we have 8 bits for the head again
        }
        
        int iBitsBuffed;
        
        while ((iBitsBuffed = ((m_oBitBuffer.size()-1) * 8 + m_iBitsRemainging)) < iCount) {
            if (BufferData() < 1)
                return iBitsBuffed;
        }
        
        return iCount;
    }
    
    /** This is the only function that actually reads from the InputStream.
     *  If it's little-endian mode, reads 16 bits as little-endian. If not,
     *  simply reads 8 bits. Returns the number of bytes buffered. */
    private int BufferData() throws IOException {
        
        if (m_iBytesPerBuffer == 2) {
            int ib1, ib2;
            if ((ib1 = m_oStrStream.read()) < 0) {
                /* If we've failed to read in a byte, then we're at the
                 * end of the stream, so return 0 */
                return 0;
            }
            if ((ib2 = m_oStrStream.read()) < 0) {
                /* If we've failed to read in a byte, then we're at the
                 * end of the stream, so return 0 */
                return 0;
            }
            // add the read bytes to the byte buffer in little-endian order
            m_oBitBuffer.Queue(ib2);
            m_oBitBuffer.Queue(ib1);
            return 2;
        }  else if (m_iBytesPerBuffer == 1) {
            int ib;
            if ((ib = m_oStrStream.read()) < 0) {
                return 0;
            }
            m_oBitBuffer.Queue(ib);
            return 1;
        } else {
            throw new
                IOException("Invalid bytes per buffer: " + m_iBytesPerBuffer);
        }
        
    }
    
}
