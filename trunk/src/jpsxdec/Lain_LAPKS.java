/* 
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


/*
 * Lain_LAPKS.java
 *
 */

package jpsxdec;

import java.io.*;
import java.awt.image.BufferedImage;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.LittleEndianIO;

public class Lain_LAPKS extends InputStream implements IGetFilePointer {

    /** Simple YCbCr class.
     * I'm not sure if this is functionally equivalent to YUV */
    private static class YCbCr {
    
        private final int Y_COLOR_SPACE = 128;
        
        public double Y;  // Luminance: range -128 to 128: -128 black, 128 white 
        public double Cb; // Component Blue: range 16 to 239, with 128 being zero
        public double Cr; // Component Red: range 16 to 239, with 128 being zero
        // Hey don't ask me why, this is just how the 
        // data comes out of the inverse DCT
        
        /* -- Constructors -------------------------------------------------- */
        
        public YCbCr(double Y, double Cb, double Cr) {
            this.Y = Y;
            this.Cb = Cb;
            this.Cr = Cr;
        }
        
        public YCbCr(RGB rgb) {
            Y =  0.299 * rgb.Red + 0.587 * rgb.Green + 0.114 * rgb.Blue - Y_COLOR_SPACE;
            Cb = - 0.1687 * rgb.Red - 0.3313 * rgb.Green + 0.5 * rgb.Blue;
            Cr = 0.5 * rgb.Red - 0.4187 * rgb.Green - 0.0813 * rgb.Blue;
        }
        
        /* -- Functions ----------------------------------------------------- */
        
        public RGB ToRGB() {
            return new RGB(
              /*R*/ (Y + Y_COLOR_SPACE) + 1.402   * Cr,
              /*G*/ (Y + Y_COLOR_SPACE) - 0.34414 * Cb - 0.71414 * Cr,
              /*B*/ (Y + Y_COLOR_SPACE) + 1.772   * Cb);
        }
        
        /** Output is in the format "(Y, Cb, Cr)" */
        public String ToString() {
            return "(" + Y + ", " + Cb + ", " + Cr + ")";
        }
    }
   
    // :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /** Simple RGB class */
    public static class RGB {
        public int Red;
        public int Green;
        public int Blue;
        
        /* -- Constructors -------------------------------------------------- */
        
        /** 'Clamps' values between 0 and 255 */
        public RGB(double r, double g, double b) {
            Red   = (int)Math.max(Math.min(r, 255), 0);
            Green = (int)Math.max(Math.min(g, 255), 0);
            Blue  = (int)Math.max(Math.min(b, 255), 0);
        }
        
        /* -- Functions ----------------------------------------------------- */
        
        public YCbCr ToYCbCr() {
            return new YCbCr(this);
        }
        
        public int ToRGBInt() {
            return (int)Red << 16 | (int)Green << 8 | (int)Blue;
        }
        
        /** Output is in the format "(R, G, B)" */
        public String ToString() {
            return "(" + Red + ", " + Green + ", " + Blue + ")";
        }
    }
    
    //private final long START_POS = 120;  // -> 8759 -> 9835
    //private final long START_POS = 9836; // -> 18475
    //private final long START_POS = 10320396; // -> 10329705
    //private final long START_POS = 10330980; // -> 10339485. -> 10340649
    
    
    RandomAccessFile m_oFile;

    long m_lngPkStart;
    long m_lngPkSize;      // 4
    long m_lngPkCellCount; // 4
    
    long m_lngStart;
    
    long m_lngWidth;     // 2
    long m_lngHeight;    // 2
    long m_lngQuantChrom;       // 2
    long m_lngQuantLumin;       // 2
    long m_lngByteSizeOfCellData;   // 4
    long m_lngNumRLC;   // 4
    ByteArrayInputStream m_oHeaderStream;
    
    public Lain_LAPKS(String sFile) {
        try {
            m_oFile = new RandomAccessFile(sFile, "r");
            SeekForNextPk();
            ReadHeader();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    
    
    public void MoveToNext() {
        try {
            m_lngStart = m_oFile.getFilePointer();
            if (m_lngStart >= getCurrentPkStartOffset() + m_lngPkSize)
                SeekForNextPk();
            else {
                m_lngStart = ((m_lngStart + 3) / 4) * 4;
                m_oFile.seek(m_lngStart);
            }
            ReadHeader();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void SeekForNextPk() throws IOException {
        /*
         *  _Pk header_
         * 4 bytes: 'lapk'
         * 4 bytes: size of pk (including header)
         * 4 bytes: number of cells
         * 12 * (number of cells): Cell descriptors
         *
         *  _Cell descriptor_
         * 4 bytes: offset of cell (after header)
         * 2 bytes: x pos?
         * 2 bytes: y pos?
         * 4 bytes: sound effect?
         * 
         */
        while (m_oFile.read() != 0x6c) {}
        m_lngPkStart = m_oFile.getFilePointer() - 1;
        m_oFile.skipBytes(3);
        m_lngPkSize = LittleEndianIO.ReadUInt32LE(m_oFile);
        m_lngPkCellCount = LittleEndianIO.ReadUInt32LE(m_oFile);
        m_oFile.skipBytes((int)getCurrentPkCellCount() * 12);
    }
    
    private void ReadHeader() throws IOException {
        /*
         *  _Cell header_
         * 2 bytes: Width
         * 2 bytes: Height
         * 2 bytes: Quantization Chrominance
         * 2 bytes: Quantization Luminance
         * 4 bytes: Length of cell data in bytes (after this value)
         * 4 bytes: Number of run length codes?
         */
        
        m_lngStart = m_oFile.getFilePointer();
        // read the header bytes
        byte buff[] = new byte [16];
        m_oFile.read(buff);
        DataInputStream oStream = 
                new DataInputStream(new ByteArrayInputStream(buff));
        m_lngWidth = LittleEndianIO.ReadUInt16LE(oStream);
        m_lngHeight = LittleEndianIO.ReadUInt16LE(oStream);
        m_lngQuantChrom = LittleEndianIO.ReadUInt16LE(oStream);
        m_lngQuantLumin = LittleEndianIO.ReadUInt16LE(oStream);
        m_lngByteSizeOfCellData = LittleEndianIO.ReadUInt32LE(oStream);
        m_lngNumRLC = LittleEndianIO.ReadUInt32LE(oStream);
        CreateFrameHeader();
        
    }
    
    
    private void CreateFrameHeader() {
        ByteArrayOutputStream bbuff = new ByteArrayOutputStream(8);
        DataOutputStream buff = new DataOutputStream(bbuff);
        try {
            buff.writeByte((int)m_lngQuantChrom); // normally run len code
            buff.writeByte((int)m_lngQuantLumin); // '''''''''''''''''''''
            buff.writeShort(0x0038);
            buff.writeShort((int)m_lngNumRLC); // normally q scale
            buff.writeShort(0x0000); // version
            m_oHeaderStream = new ByteArrayInputStream(bbuff.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public long getFilePointer() {
        try {
            return m_oFile.getFilePointer();
        } catch (IOException ex) {
            return -1;
        }
    }

    public int read() throws IOException {
        // first feed the header first until it is all read
        int iByte = m_oHeaderStream.read();
        if (iByte < 0)
            return m_oFile.read(); // then feed the file
        else
            return iByte;
    }
    
    public BufferedImage ReadBitMask() {
        byte abBitMask[] = null;
        int bitmaskpos = 0;
        try {
            m_oFile.seek(m_lngStart + 12 + m_lngByteSizeOfCellData);
            
            long bitmasksize = LittleEndianIO.ReadUInt32LE(m_oFile);
            abBitMask = new byte[(int)bitmasksize];
            while (bitmaskpos < bitmasksize)
            {
                int flags = m_oFile.readUnsignedByte();
                System.out.print(String.format("Flags %02x", flags));
                for (int bitmask = 0x80; bitmask > 0; bitmask>>>=1) {
                    if (bitmaskpos >= bitmasksize) break;
                    System.out.print(String.format(
                            "[InPos: %d OutPos: %d] Flags %02x: bit %02x: ",
                            m_oFile.getFilePointer(),
                            bitmaskpos,
                            flags,
                            bitmask
                            ));
                    if ((flags & bitmask) > 0) {
                        int copy_offset = m_oFile.readUnsignedByte();
                        int copy_counter = m_oFile.readUnsignedByte();
                        copy_offset += 1;
                        copy_counter = (copy_counter + 3);// % 256;
                        System.out.println(
                                "Copy " + copy_counter + 
                                " bytes from -" + copy_offset + 
                                " (" + (bitmaskpos - copy_offset) + ")");
                        for (int i = 0; i < copy_counter; i++) {
                            if (bitmaskpos >= abBitMask.length) {
                                int asdf = 1;
                            }
                            abBitMask[bitmaskpos] = abBitMask[bitmaskpos - copy_offset];
                            bitmaskpos++;
                        }
                    } else {
                        byte b = m_oFile.readByte();
                        System.out.println(String.format("{Byte %02x}", b));
                        abBitMask[bitmaskpos] = b;
                        bitmaskpos++;
                    }
                }
            }
            System.out.println("File pos: " + m_oFile.getFilePointer());
        } catch (Exception ex) {
            ex.printStackTrace(); 
            
        }
        
        BufferedImage bi = new BufferedImage((int)getCurrentCellWidth(), (int)getCurrentCellHeight(),
                BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < getCurrentCellHeight(); y++) {
            for (int x = 0; x < getCurrentCellWidth() / 4; x++) {
                byte b = abBitMask[(int)(x + y * getCurrentCellWidth()/4)];
                //System.out.print(String.format("%02x ", b));
                int bit;
                bit = ((b >>> 6) & 3) * 64;
                bi.setRGB(x*4 + 0, y, 
                        new RGB(bit, bit, bit).ToRGBInt());
                bit = ((b >>> 4) & 3) * 64;
                bi.setRGB(x*4 + 1, y, 
                        new RGB(bit, bit, bit).ToRGBInt());
                bit = ((b >>> 2) & 3) * 64;
                bi.setRGB(x*4 + 2, y, 
                        new RGB(bit, bit, bit).ToRGBInt());
                bit = ((b >>> 0) & 3) * 64;
                bi.setRGB(x*4 + 3, y, 
                        new RGB(bit, bit, bit).ToRGBInt());
            }        
            //System.out.println();
        }
        
        return bi;
    }
    

    public long getCurrentPkStartOffset() {
        return m_lngPkStart;
    }

    public long getCurrentPkCellCount() {
        return m_lngPkCellCount;
    }

    public long getCurrentCellWidth() {
        return m_lngWidth;
    }

    public long getCurrentCellHeight() {
        return m_lngHeight;
    }

}
