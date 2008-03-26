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
 */

package jpsxdec.uncompressors;

import jpsxdec.util.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/** A limited bit reader. Wraps an InputStream. Should be pretty quick. */
public class BufferedBitReader {
    
    /** Queue of 16-bit integers. Initialized to a requested
     *  size. A head index and a tail index circle around the array.
     *  There is always at least one unused array entry (to make use
     *  of that unused entry would require an extra flag variable
     *  and additional checks which would only slow down the queue,
     *  and still use as much memory). */
    private static class SimpleWordQueue {
        private long[] m_alngWords;
        private int m_iHeadPos;
        private int m_iTailPos;
        private boolean m_blnBigEndian;
        
        /** Private constructor for ShallowCopy() function.*/
        private SimpleWordQueue() {}
        
        /** @param  iSize Maximum number of 16-bits allowed in the array 
         *  @param  blnBigEndian true to read 16bits as big-endian, false for little-endian */
        public SimpleWordQueue(int iSize, boolean blnBigEndian) {
            m_blnBigEndian = blnBigEndian;
            m_iHeadPos = 0;
            m_iTailPos = 0;
            m_alngWords = new long[iSize + 1];
        }
        
        /** @param b1  Byte to add to the queue 
         *  @param b2  Byte to add to the queue */
        public void Queue(int b1, int b2) {
            m_alngWords[m_iTailPos] = (b1 << 8) | b2;
            m_iTailPos = (m_iTailPos + 1) % m_alngWords.length;
            if (m_iTailPos == m_iHeadPos) throw new BufferOverflowException();
        }
        
        /** Returns the 16-bit head of the queue without removing it. 
         *  The 16-bits will be returned as big-endian or little-endian
         *  depending on the setting.
         * @return  16-bit head of the queue without removing it */
        public long Peek() {
            if (m_iTailPos == m_iHeadPos) throw new BufferUnderflowException();
            long lng = m_alngWords[m_iHeadPos];
            if (m_blnBigEndian)
                return lng;
            else
                return ((lng & 0xFF) << 8) | ((lng >>> 8) & 0xFF);
        }
        
        /** Removes the 16-bit head of the queue and returns it. 
         *  The 16-bits will be returned as big-endian or little-endian
         *  depending on the setting.
         * @return  removed 16-bit head of the queue */
        public long Dequeue() {
            if (m_iTailPos == m_iHeadPos) throw new BufferUnderflowException();
            long lng = m_alngWords[m_iHeadPos];
            m_iHeadPos = (m_iHeadPos + 1) % m_alngWords.length;
            if (m_blnBigEndian)
                return lng;
            else
                return ((lng & 0xFF) << 8) | ((lng >>> 8) & 0xFF);
        }
        
        /** Creates a new queue instance, but only copies the head
         * and tail index to the original array, and the endianess.
         * Used by the BufferedBitReader to peek values from the stream 
         * without removing them */
        public SimpleWordQueue ShallowCopy() {
            SimpleWordQueue oNew = new SimpleWordQueue();
            oNew.m_alngWords = m_alngWords;
            oNew.m_iHeadPos = m_iHeadPos;
            oNew.m_iTailPos = m_iTailPos;
            oNew.m_blnBigEndian = m_blnBigEndian;
            return oNew;
        }
        
        /** @return number of bytes in the queue */
        public int size() {
            int i = m_iTailPos - m_iHeadPos;
            if (i < 0)
                return (m_alngWords.length + i) * 2;
            else
                return i * 2;
        }
        
        /** Number of bytes the queue can hold */
        public int getMaxCapacity() {
            return (m_alngWords.length - 1) * 2;
        }
        
        /** @return  If the queue 16-bits will be read as big-endian */
        public boolean isBigEndian() {
            return m_blnBigEndian;
        }
        
        /** @param blnBigEndian  The endianess of 16-bits returned by the queue */
        public void setBigEndian(boolean blnBigEndian) {
            m_blnBigEndian = blnBigEndian;
        }
    }
    
    
    /** Mask remaining bits. <pre>
     * 0  = 0000000000000000 (not used)
     * 1  = 0000000000000001
     * 2  = 0000000000000011
     * 3  = 0000000000000111
     * 4  = 0000000000001111
     * ...
     * 13 = 0001111111111111
     * 14 = 0011111111111111
     * 15 = 0111111111111111
     * 16 = 1111111111111111
     *</pre>*/
    private static int BIT_MASK[] = new int[] {
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
        0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF,
        0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    };
    
    /** A queue to store the bytes read from the stream. */
    private SimpleWordQueue m_oBitBuffer;
    /** Bits remaining in the 16-bits at the head of the queue. */
    int m_iBitsRemainging = 0;
    
    /** The source InputStream. */
    private InputStream m_oStrStream;
    
    /* ---------------------------------------------------------------------- */
    /* Constructors --------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** @param oIS           Source input stream
     *  @param blnBigEndian  If the stream should be read as big-endian
     *  @param iBufferSize   Size of the buffer in 16-bit words */
    public BufferedBitReader(InputStream oIS, boolean blnBigEndian, int iBufferSize) {
        m_oStrStream = oIS;
        m_oBitBuffer = new SimpleWordQueue(iBufferSize, blnBigEndian);
    }
    
    /* ---------------------------------------------------------------------- */
    /* Properties ----------------------------------------------------------- */
    /* ---------------------------------------------------------------------- */
    
    /** Returns whether the stream is set to be read 16-bits at a time in
     *  in big-endian or little-endian.
     * @return true if big-endian, false if little-endian */
    public boolean isBigEndian() {
        return m_oBitBuffer.isBigEndian();
    }
    
    /** Sets wheither to read 16-bits as big-endian, or little-endian.
     *  A RuntimeException() is thrown if the read point is not at a 
     *  word boundary.
     *  @param blnBigEndian  true for big-endian, false for little-endian */
    public void setBigEndian(boolean blnBigEndian) {
        if (m_iBitsRemainging != 0 && m_iBitsRemainging != 16)
            throw new RuntimeException("Changing endian while bits remain.");
        m_oBitBuffer.setBigEndian(blnBigEndian);
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
            int iBitsBuffered = m_oBitBuffer.size() * 8 - (16 - m_iBitsRemainging);
            if (iBitsBuffered < 0) iBitsBuffered = 0;
            return lngFilePos - (double)iBitsBuffered / 8.0;
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
            throw new EOFException("Unexpected end of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 16 - ((iCount - m_iBitsRemainging) % 16);
        return lngRet;
    }
    
    /** First reads the bits in as unsigned, then converts to signed using
     *  bit shifting. */
    public long ReadSignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("Unexpected end of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 16 - ((iCount - m_iBitsRemainging) % 16);
        return (lngRet << (64 - iCount)) >> (64 - iCount); // change to signed
    }
    
    /** Reads the bits into a string of "1" and "0". */
    public String ReadBitsToString(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        iCount = BufferEnoughForBits(iCount);
        long lngRet = ReadUBits(m_iBitsRemainging, iCount, m_oBitBuffer);
        if (iCount <= m_iBitsRemainging)
            m_iBitsRemainging -= iCount;
        else
            m_iBitsRemainging = 16 - ((iCount - m_iBitsRemainging) % 16);
        return PadZeroLeft(Long.toBinaryString(lngRet), iCount);
    }
    
    // .........................................................................
    
    /** Same as ReadUnsignedBits(), but doesn't
     *  move the stream position forward.  */
    public long PeekUnsignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("Unexpected end of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return lngRet;
    }
    
    /** Same as ReadSignedBits(), but doesn't
     *  move the stream position forward.  */
    public long PeekSignedBits(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        if (BufferEnoughForBits(iCount) != iCount)
            throw new EOFException("Unexpected end of bit file");
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return (lngRet << (64 - iCount)) >> (64 - iCount); // change to signed
    }
    
    /** Same as ReadBitsToString(), but doesn't
     *  move the stream position forward.  */
    public String PeekBitsToString(int iCount) throws IOException {
        assert(iCount < 32 && iCount > 0);
        iCount = BufferEnoughForBits(iCount);
        long lngRet = ReadUBits(m_iBitsRemainging, iCount,
                m_oBitBuffer.ShallowCopy());
        return PadZeroLeft(Long.toBinaryString(lngRet), iCount);
    }
    
    // .........................................................................
    
    /** Tries to skip the requsted number of bits. Stops at the end of the
     *  stream.
     * @param iCount  The number of bits to skip */
    public void SkipBits(int iCount) throws IOException {
        assert(iCount > 0 && iCount < m_oBitBuffer.getMaxCapacity() * 8 );
        iCount = BufferEnoughForBits(iCount);
        if (iCount <= m_iBitsRemainging) {
            m_iBitsRemainging -= iCount;
        } else {
            
            iCount -= m_iBitsRemainging;
            m_oBitBuffer.Dequeue();
            
            while (iCount >= 16) {
                m_oBitBuffer.Dequeue();
                iCount -= 16;
            }
            m_iBitsRemainging = 16 - iCount;
        }
    }
    
    // .........................................................................
    
    /** Peeks bytes into an array. If unable to read the number of requested
     *  bytes, returns as many as could be read.
     * @param iByteCount  Number of bytes to peek */
    public byte[] PeekBytes(int iByteCount) throws IOException {
        int iBitCount = BufferEnoughForBits(iByteCount * 8);
        byte[] ab = new byte[iBitCount / 8];
        SimpleWordQueue oCloneQueue = m_oBitBuffer.ShallowCopy();
        int iCloneBitRemain = m_iBitsRemainging;
        for (int i = 0; i < ab.length; i++) {
            ab[i] = (byte)ReadUBits(iCloneBitRemain, 8, oCloneQueue);
            if (8 <= iCloneBitRemain)
                iCloneBitRemain -= 8;
            else
                iCloneBitRemain = 16 - ((8 - iCloneBitRemain) % 16);
        }
        return ab;
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
            SimpleWordQueue oBitBuffer) {
        long lngRet = 0;
        long lngWrd;
        if (iCount <= iBitsRemainging) {
            lngWrd = oBitBuffer.Peek();
            lngWrd = lngWrd & BIT_MASK[iBitsRemainging];
            lngRet = (lngWrd >>> (iBitsRemainging - iCount));
        } else {
            lngWrd = oBitBuffer.Dequeue();
            lngRet = lngWrd & BIT_MASK[iBitsRemainging];
            iCount -= iBitsRemainging;
            
            while (iCount >= 16) {
                lngRet = (lngRet << 16) | oBitBuffer.Dequeue();
                iCount -= 16;
            }
            
            if (iCount > 0) {
                lngWrd = oBitBuffer.Peek();
                lngRet = (lngRet << iCount) | (lngWrd >>> (16 - iCount));
            }
        }
        
        return lngRet;
    }
    
    // .........................................................................
    
    private final static String[] ZERO_PAD = new String[] {
        "", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000",
        "000000000", "0000000000", "00000000000", "000000000000", 
        "0000000000000", "00000000000000", "000000000000000", 
        "0000000000000000", "00000000000000000", "000000000000000000", 
        "0000000000000000000", "00000000000000000000", "000000000000000000000", 
        "0000000000000000000000", "00000000000000000000000",
        "000000000000000000000000", "0000000000000000000000000",
        "00000000000000000000000000", "000000000000000000000000000",
        "0000000000000000000000000000", "00000000000000000000000000000",
        "000000000000000000000000000000", "0000000000000000000000000000000",
        "00000000000000000000000000000000"
    };
    
    /** Used by the read/peek string functions. Pads the string with
     *  zeros on the left until the string is the desired length. 
     *  Made public for use by other classes. */
    public static String PadZeroLeft(String s, int len) {
        int slen = s.length();
        
        assert(slen <= len);
        
        if (slen == len)
            return s;
        
        return ZERO_PAD[len - slen] + s;
    }
    
    /** First fixes m_iBitsRemainging and m_oBitBuffer so there is at least
     *  some data available in the queue, then buffers enough
     *  bytes for the requested bits. If the requrested number of bits or more
     *  were buffered, then returns the requested number of bits. If fewer than
     *  the requested number of bits were buffered, then returns the
     *  number of bits that could be buffered.
     *  @param iCount   desired number of bits to buffer.
     *  @return iCount, or less if fewer bits could be buffered. */
    private int BufferEnoughForBits(int iCount) throws IOException {
        if (m_iBitsRemainging == 0) { // no bits remain for the buffer head
            // need to get rid of the head of the buffer
            if (m_oBitBuffer.size() > 0) { // but but only if buffer isn't empty
                m_oBitBuffer.Dequeue(); // get rid of the used up head
            }
            if (m_oBitBuffer.size() == 0) { // now if the buffer is empty
                // add a little more to the buffer
                if (BufferData() < 2) throw new EOFException("Unexpected end of bit file");
            }
            m_iBitsRemainging = 16; // and now we have 8 bits for the head again
        }
        
        int iTotalBitsBuffed = m_oBitBuffer.size() * 8 - (16 - m_iBitsRemainging);
        
        // keep buffering data until the requested number of bits were
        // buffered, or we hit the end of the stream.
        while (iTotalBitsBuffed < iCount) {
            int iBytesBuffered = BufferData();
            if (iBytesBuffered < 2)
                return iTotalBitsBuffed;
            iTotalBitsBuffed += iBytesBuffered * 8;
        }
        
        return iCount;
    }
    
    /** This is the only function that actually reads from the InputStream.
     *  Read 16 bits from the stream and adds them to the queue. Returns
     *  either 0 or 2, for the number of bytes read. If only 1 byte
     *  could be read, throws EOFException().
     *  @return the number of bytes buffered */
    private int BufferData() throws IOException {
        
        int ib1, ib2;
        if ((ib1 = m_oStrStream.read()) < 0) {
            /* If we've failed to read in a byte, then we're at the
             * end of the stream, so return 0 */
            return 0;
        }
        if ((ib2 = m_oStrStream.read()) < 0) {
            /* If we've failed to read in a byte, then we're at the
             * end of the stream, so return 0 */
            throw new EOFException("Unexpected end of bit file");
        }
        // add the read bytes to the byte buffer in little-endian order
        m_oBitBuffer.Queue(ib1, ib2);
        return 2;
        
    }
    
}
