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
 * Lain_Pk.java
 */

package jpsxdec.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import jpsxdec.util.IO;


/** Recreation of the compression alogrithm used to store data on the 
 *  Serial Experiments Lain PSX Game discs. */
public class Lain_Pk {
    
    public static int DebugVerbose = 2;
    
    private static interface IZipField {
        int getBit();
        void write(OutputStream os) throws IOException;
    }
    
    /** Represents a run of bytes before the current position that are
     *  identical to the bytes at the current position. */
    private static class BackCopy implements IZipField {
        public int RelativePosition;
        public int Length;

        public BackCopy(int Position, int Length) {
            this.RelativePosition = Position;
            this.Length = Length;
        }

        public int getBit() {
            return 1;
        }

        public void write(OutputStream os) throws IOException {
            os.write(this.RelativePosition - 1);
            os.write(this.Length - 3);
        }
    }
    /** Represents a literal byte at the current position. */
    private static class DirectCopy implements IZipField {
        public int Value;

        public DirectCopy(int Value) {
            this.Value = Value;
        }

        public int getBit() {
            return 0;
        }

        public void write(OutputStream os) throws IOException {
            os.write(Value);
        }
    }
    /** Compress data using the Lain compression method */
    public static void Compress(byte[] abData, OutputStream oStream) throws IOException {

        ArrayList<IZipField> oZipFields = new ArrayList<IZipField>();
        
        // search through the data to find how to compress it
        int x = 0;
        while (x < abData.length) {
            BackCopy oBackCpy = SearchForBackCopy(abData, x);
            if (oBackCpy == null) {
                oZipFields.add(new DirectCopy(abData[x]));
                x++;
            } else {
                oZipFields.add(oBackCpy);
                x += oBackCpy.Length;
            }

        }

        // write the size of the uncompressed data
        IO.WriteInt32LE(oStream, abData.length);
        
        IZipField[] o8fields = new IZipField[8];
        Iterator<IZipField> it = oZipFields.iterator();
        while (it.hasNext()) {
            int iFieldsToWrite;
            int iFlags = 0;
            // grab 8 feilds to write, and construct the bit-flag in the process
            // (make sure to break if no more fields are left)
            for (iFieldsToWrite = 0; iFieldsToWrite < 8 && it.hasNext(); iFieldsToWrite++) 
            {
                o8fields[iFieldsToWrite] = it.next();
                iFlags |= o8fields[iFieldsToWrite].getBit() << (7 - iFieldsToWrite);
            }
            // write the flag
            oStream.write(iFlags);
            // write the 8 fields (or as many as we have)
            for (int i = 0; i < iFieldsToWrite; i++) {
                o8fields[i].write(oStream);
            }
        }
        
    }
    
    /** Find the longest run of bytes that match the current position. */
    private static BackCopy SearchForBackCopy(byte[] abData, final int iEndPos) {
        int iLongestRunPos = 0;
        int iLongestRunLen = 0;
        
        int iMatchPos = iEndPos - 256;
        if (iMatchPos < 0) iMatchPos = 0;
        for (; iMatchPos < iEndPos; iMatchPos++) {
            int iMatchLen = MatchLength(abData, iMatchPos, iEndPos);
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
    private static int MatchLength(byte[] abData, int iMatchPos, int iEndPos) {
        int i = 0;
        while ((iEndPos + i < abData.length)
                && i <= 257
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
    public static byte[] Decompress(RandomAccessFile oRAF) 
            throws IOException 
    {
        int iUncompressedPos = 0;
        
        long lngUncompressedSize = IO.ReadUInt32LE(oRAF);
        byte[] abUncompressed = new byte[(int)lngUncompressedSize];
        
        while (iUncompressedPos < lngUncompressedSize)
        {
            int iFlags = oRAF.readUnsignedByte();
            
            if (DebugVerbose > 2)
                System.err.println(String.format("Flags %02x", iFlags));
            
            for (int iFlagMask = 0x80; iFlagMask > 0; iFlagMask>>>=1) {
                if (iUncompressedPos >= lngUncompressedSize) break;
                
                if (DebugVerbose > 2)
                    System.err.print(String.format(
                            "[InPos: %d OutPos: %d] Flags %02x: bit %02x: ",
                            oRAF.getFilePointer(),
                            iUncompressedPos,
                            iFlags,
                            iFlagMask
                            ));
                    
                if ((iFlags & iFlagMask) > 0) {
                    int iCopyOffset = oRAF.readUnsignedByte();
                    int iCopySize = oRAF.readUnsignedByte();
                    iCopyOffset = (iCopyOffset + 1);
                    iCopySize = (iCopySize + 3);
                    
                    if (DebugVerbose > 2)
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
                    byte b = oRAF.readByte();
                    
                    if (DebugVerbose > 2)
                        System.err.println(String.format("{Byte %02x}", b));
                    
                    abUncompressed[iUncompressedPos] = b;
                    iUncompressedPos++;
                }
            }
        }
        if (DebugVerbose > 2)
            System.err.println("File pos: " + oRAF.getFilePointer());
        
        return abUncompressed;
    }

}
