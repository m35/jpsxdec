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

    /** The simplest queue class possible. Initialized to a requested
     *  size. A head and a tail index circle around the array.
     *  There is always at least one unused array entry. To make use
     *  of that unused entry would require an extra flag variable
     *  and additional checks which would only slow down the queue,
     *  and still use as much memory. */
    private static class SimpleIntQueue {
        int[] m_abBytes;
        int m_iHeadPos = 0;
        int m_iTailPos = 0;
        
        /** Private constructor for ShallowCopy() function.*/
        private SimpleIntQueue() {}
        
        /** @param iSize - Maximum number of elements allowed in the array - 1*/
        public SimpleIntQueue(int iSize) {
            m_abBytes = new int[iSize];
        }
        
        /** @param i - Integer to add to the queue */
        public void Queue(int i) {
            m_abBytes[m_iTailPos] = i;
            m_iTailPos = (m_iTailPos + 1) % m_abBytes.length;
            if (m_iTailPos == m_iHeadPos)
                throw new BufferOverflowException();
        }
        
        /** @return head of the queue without removing it */
        public int Peek() {
            if (m_iTailPos == m_iHeadPos)
                throw new BufferUnderflowException();
            return m_abBytes[m_iHeadPos];
        }

        /** Removes the integer at the head of the queue 
         *  @return integer removed */
        public int Dequeue() {
            if (m_iTailPos == m_iHeadPos)
                throw new BufferUnderflowException();
            int i = m_abBytes[m_iHeadPos];
            m_iHeadPos = (m_iHeadPos + 1) % m_abBytes.length;
            return i;
        }
        
        /** Creates a new queue instance, but only copies the head
          * and tail index to the original array. Used by the BufferedBitReader
          * to peek bytes from the stream without removing them */
        public SimpleIntQueue ShallowCopy() {
            SimpleIntQueue oNew = new SimpleIntQueue();
            oNew.m_abBytes = m_abBytes;
            oNew.m_iHeadPos = m_iHeadPos;
            oNew.m_iTailPos = m_iTailPos;
            return oNew;
        }
        
        /** @return number of elements in the queue */
        public int size() {
            int i = m_iTailPos - m_iHeadPos;
            if (i < 0)
                return m_abBytes.length + i;
            else
                return i;
        }
    }
    
    
    /** Mask remaining bits.
     * 0 = 00000000 (not used)
     * 1 = 00000001
     * 2 = 00000011
     * 3 = 00000111
     * 4 = 00001111
     * 5 = 00011111
     * 6 = 00111111
     * 7 = 01111111
     * 8 = 11111111
     */
    private static byte BIT_MASK[] = new byte[] {
        (byte)0x00, (byte)0x01, (byte)0x03, (byte)0x07, (byte)0x0F,
        (byte)0x1F, (byte)0x3F, (byte)0x7F, (byte)0xFF
    };
    
    /** A queue to store the bytes read/peeked from the stream.
     *  10 is just an arbritrary number. It really only needs 
     *  4 bytes. */
    SimpleIntQueue m_oBitBuffer = new SimpleIntQueue(10);
    /** Bits remaining in the byte at the head of the queue. */
    int m_iBitsRemainging = 0;
    
    /** The source InputStream. */
    InputStream m_oStrStream;
    
    /** Bytes per read has special behavior:
     *  1 means 1 byte per read (simple).
     *  2 means 2 bytes per read, but in little endian order. */
    int m_iBytesPerBuffer;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** @param oIS - Source input stream
     *  @param iBytesPerRead - 1 for big-endian, 2 for little-endian */
    public BufferedBitReader(InputStream oIS, int iBytesPerRead) {
        assert(iBytesPerRead == 1 || iBytesPerRead == 2);
        m_iBytesPerBuffer = iBytesPerRead;
        m_oStrStream = oIS;
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Returns whether the stream is set to be read in big-endian
     * (2 byte per buffer) or 16-bit little-endian (2 bytes per buffer). 
     * @return 1 or 2 */
    public int getBytesPerBuffer() {
        return m_iBytesPerBuffer;
    }
    
    /** Sets wheither to read the data as a stream of bits (therefore
     *  big-endian), or to read in 16 bits at a time in little-endian order.
     *  Note: Changing this value while there is still data in the
     *  buffer could result in strange values.
     *  @param iBytesPerRead - 1 for big-endian, 2 for little-endian */
    public void setBytesPerBuffer(int iBytesPerRead) {
        assert(iBytesPerRead == 1 || iBytesPerRead == 2);
        m_iBytesPerBuffer = iBytesPerRead;
    }
    
    //..........................................................................
    
    /** If the base InputStream implements the getFilePointer method, then we
     *  can lookup the position.
     *  Returns the fractional position within the original input stream.
     *  If InputStream does not implement getFilePointer, or if failure,
     *  returns -1.0f.
     *  @return fractional position in the stream, or -1.0 */
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
    
    /** Reads in the bits and returns the unsigned value. */
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
    
    /** Reads the bits into a string of "1" and "0". */
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
     *  move the stream position forward.  */
    public long PeekUnsignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return lngRet;
    }
    
    /** Same as ReadSignedBits, but doesn't
     *  move the stream position forward.  */
    public long PeekSignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("End of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return (lngRet << (64 - iCount)) >> (64 - iCount);
    }
    
    /** Same as ReadBitsToString, but doesn't
     *  move the stream position forward.  */
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
     *  (for peeking bits). Enough bytes should be in the buffer prior
     *  to calling this function. */
    private static long ReadUBits(int iBitsRemainging,
                                  int iCount,
                                  SimpleIntQueue oBitBuffer) 
    {
        long lngRet = 0;
        int ib;
        if (iCount <= iBitsRemainging) {
            ib = oBitBuffer.Peek();
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
    
    /** Used by the read/peek string functions. Pads the string with
     *  zeros on the left until the string is the desired length. */
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
    
    /** First fixes m_iBitsRemainging so it is not zero, then buffers enough 
     *  bytes for the requested bits. If the requrested number of bits or more 
     *  were buffered, then returns the requested number of bits. If fewer than
     *  the requested number of bits were buffered, then returns the
     *  number of bits that could be buffered.
     *  @param iCount - desired number of bits to buffer
     *  @return iCount, or less if fewer bits could be buffered */
    private int BufferEnoughForBits(int iCount) throws IOException {
        if (m_iBitsRemainging == 0) { // no bits remain for the buffer head
            // need to get rid of the head of the buffer
            if (m_oBitBuffer.size() > 0) { // but but only if buffer isn't empty
                m_oBitBuffer.Dequeue(); // get rid of the used up head
            }
            if (m_oBitBuffer.size() == 0) { // now if the buffer is empty
                // add a little more to the buffer
                if (BufferData() < 1) throw new EOFException("End of bit file");
            }
            m_iBitsRemainging = 8; // and now we have 8 bits for the head again
        }
        
        int iBitsBuffed;
        
        // keep buffering data until the requested number of bits were
        // buffered, or we hit the end of the stream.
        while ((iBitsBuffed = ((m_oBitBuffer.size()-1) * 8 + m_iBitsRemainging)) < iCount) {
            if (BufferData() < 1)
                return iBitsBuffed;
        }
        
        return iCount;
    }
    
    /** This is the only function that actually reads from the InputStream.
     *  If it's little-endian mode, reads 16 bits as little-endian. If not,
     *  simply reads 8 bits.
     *  @return the number of bytes buffered */
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
