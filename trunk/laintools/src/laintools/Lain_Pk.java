/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package laintools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import jpsxdec.util.IO;


/** Recreation of the compression algorithm used to store data on the
 *  Serial Experiments Lain PSX Game discs. 
 *<p>
 *  This is apparently an LZSS type of compression, which, according to
 *  halkun of Qhimm forums, is often used in PlayStation games. He
 *  referenced some similar code:
 *  http://sprite.phys.ncku.edu.tw/NCKUtech/DCM/pub/DCM_CODE_ASM/lzss.c
 *<p>
 *  Note that this LZSS format is very similar to the one used in Final
 *  Fantasy 7, and .iki STR format, but is fundamentally not compatible.
 */
public class Lain_Pk {
    
    public static int DEBUG = 2;
    
    private static interface IZipField {
        int getBitFlag();
        void write(OutputStream os) throws IOException;
    }
    
    /** Represents a run of bytes prior the current position that are
     *  identical to the bytes at the current position. */
    private static class BackCopy implements IZipField {
        private final int _iRelativePosition;
        private final int _iLength;

        public BackCopy(int iPosition, int iLength) {
            if (iPosition > 255+1)
                throw new IllegalArgumentException();
            if (iLength > 255+3)
                throw new IllegalArgumentException();
            _iRelativePosition = iPosition;
            _iLength = iLength;
        }

        public int getBitFlag() {
            return 1;
        }

        public void write(OutputStream os) throws IOException {
            os.write(_iRelativePosition - 1);
            os.write(_iLength - 3);
        }
    }
    /** Represents a literal byte at the current position. */
    private static class DirectCopy implements IZipField {
        private final int _iValue;

        public DirectCopy(int iValue) {
            _iValue = iValue;
        }

        public int getBitFlag() {
            return 0;
        }

        public void write(OutputStream os) throws IOException {
            os.write(_iValue);
        }
    }
    /** Compress data using the Lain compression method */
    public static void compress(byte[] abData, OutputStream os) throws IOException {

        ArrayList<IZipField> zipFields = new ArrayList<IZipField>();
        
        // search through the data to find how to compress it
        int x = 0;
        while (x < abData.length) {
            BackCopy oBackCpy = searchForBackCopy(abData, x);
            if (oBackCpy == null) {
                zipFields.add(new DirectCopy(abData[x]));
                x++;
            } else {
                zipFields.add(oBackCpy);
                x += oBackCpy._iLength;
            }

        }

        // write the size of the uncompressed data
        IO.writeInt32LE(os, abData.length);
        
        IZipField[] ao8fields = new IZipField[8];
        Iterator<IZipField> it = zipFields.iterator();
        while (it.hasNext()) {
            int iFieldsToWrite;
            int iFlags = 0;
            // grab 8 fields to write, and construct the bit-flag in the process.
            // also exit this construction if we run out of data
            for (iFieldsToWrite = 0; iFieldsToWrite < 8 && it.hasNext(); iFieldsToWrite++) 
            {
                ao8fields[iFieldsToWrite] = it.next();
                iFlags |= ao8fields[iFieldsToWrite].getBitFlag() << (7 - iFieldsToWrite);
            }
            // write the flags byte
            os.write(iFlags);
            // write the 8 fields (or as many as we have)
            for (int i = 0; i < iFieldsToWrite; i++) {
                ao8fields[i].write(os);
            }
        }
        
    }
    
    /** Find the longest run of bytes that match the current position. */
    private static BackCopy searchForBackCopy(byte[] abData, final int iEndPos) {
        int iLongestRunPos = 0;
        int iLongestRunLen = 0;
        
        int iMatchPos = iEndPos - (255 + 1);
        if (iMatchPos < 0) iMatchPos = 0;
        for (; iMatchPos < iEndPos; iMatchPos++) {
            int iMatchLen = matchLength(abData, iMatchPos, iEndPos);
            if (iMatchLen > iLongestRunLen && iMatchLen >= 3) {
                iLongestRunLen = iMatchLen;
                iLongestRunPos = iMatchPos;
            }
        }
        if (iLongestRunLen > 0)
            return new BackCopy(iEndPos - iLongestRunPos, iLongestRunLen);
        else
            return null;
    }
    
    /** Count how many bytes match the current position. */
    private static int matchLength(byte[] abData, int iMatchPos, int iEndPos) {
        int i = 0;
        while ((iEndPos + i < abData.length)
                && i < (255 + 3)
                && abData[iMatchPos+i] == abData[iEndPos+i])
        {
            i++;
        }
        return i;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /** 
     * read 4 bytes: size of decompressed data
     * While not all uncompressed:
     *      read 1 byte: then for each bit (highest to lowest)
     *          - if 1
     *            - read 1 byte: start offset to copy, + 1
     *            - read 1 byte: number of bytes to copy, + 3
     *          - if 0
     *            - read 1 byte: literal byte value to use
     */
    public static byte[] decompress(RandomAccessFile raf)
            throws IOException 
    {
        int iUncompressedPos = 0;
        
        long lngUncompressedSize = IO.readUInt32LE(raf);
        byte[] abUncompressed = new byte[(int)lngUncompressedSize];
        
        while (iUncompressedPos < lngUncompressedSize)
        {
            int iFlags = raf.readUnsignedByte();
            
            if (DEBUG > 2)
                System.err.println(String.format("Flags %02x", iFlags));
            
            for (int iFlagMask = 0x80; iFlagMask > 0; iFlagMask>>>=1) {
                if (iUncompressedPos >= lngUncompressedSize) break;
                
                if (DEBUG > 2)
                    System.err.print(String.format(
                            "[InPos: %d OutPos: %d] Flags %02x: bit %02x: ",
                            raf.getFilePointer(),
                            iUncompressedPos,
                            iFlags,
                            iFlagMask
                            ));
                    
                if ((iFlags & iFlagMask) != 0) {
                    int iCopyOffset = raf.readUnsignedByte();
                    int iCopySize = raf.readUnsignedByte();
                    iCopyOffset = (iCopyOffset + 1);
                    iCopySize = (iCopySize + 3);
                    
                    if (DEBUG > 2)
                        System.err.println(
                                "Copy " + iCopySize + 
                                " bytes from -" + iCopyOffset + 
                                " (" + (iUncompressedPos - iCopyOffset) + ")");
                    
                    for (int i = 0; i < iCopySize; i++) {
                        if (iUncompressedPos >= lngUncompressedSize) {
                            throw new RuntimeException("This should never happen.");
                        }
                        abUncompressed[iUncompressedPos] = abUncompressed[iUncompressedPos - iCopyOffset];
                        iUncompressedPos++;
                    }
                } else {
                    byte b = raf.readByte();
                    
                    if (DEBUG > 2)
                        System.err.println(String.format("{Byte %02x}", b));
                    
                    abUncompressed[iUncompressedPos] = b;
                    iUncompressedPos++;
                }
            }
        }
        if (DEBUG > 2)
            System.err.println("File pos: " + raf.getFilePointer());
        
        return abUncompressed;
    }

}
