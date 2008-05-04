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
 * Lain_LAPKS.java
 */

package jpsxdec.plugins;

import java.awt.Graphics2D;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import jpsxdec.*;
import jpsxdec.mdec.MDEC;
import jpsxdec.videodecoding.CriticalUncompressException;
import jpsxdec.videodecoding.StrFrameUncompressor;
import jpsxdec.util.IGetFilePointer;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.mdec.PsxYuv;


/** Functions to decode the Lain poses from the LAPKS.BIN file. */
public class Lain_LAPKS {
    
    public static int DebugVerbose = 2;
    
    /** Decodes the numerous Lain poses from the LAPKS.BIN file. The LAPKS.BIN
     *  file is the same on both disc 1 and disc 2. This function needs a
     *  standard 2048-per-sector (i.e. ISO) copy of the file. This can easily
     *  be accomplished by simply copying the file off the disc using normal
     *  operating system commands, or even providing the path directly to the
     *  file on the disc.
     *  This function will dump the over 1000 animation cells, including the
     *  bit-mask used to provide transparency to the images. The cells are
     *  centered in a larger image according to the cell's x,y position found
     *  with the cell data. 
     *  Output file names will look like this
     *  <sOutFileBase><animation#>_f<frame#>.png
     *  <sOutFileBase><animation#>_f<frame#>_mask.png
     *  Note that I don't quite understand why the bit-mask has 4 values,
     *  since I can only see a need for 3 (transparent, slightly transparent,
     *  and totally opaque). I got confused while stepping through the
     *  game's assembly code. Perhaps another go at it would be good.
     * @param sInLAPKS_BIN - the path to the LAPKS.BIN file
     * @param sOutFileBase - output base name of the files
     */
    public static int DecodeLAPKS(String sInLAPKS_BIN, String sOutFileBase) {
            
        try {
            Lain_LAPKS lnpk = new Lain_LAPKS(sInLAPKS_BIN);
            Lain_LAPKS.LaPkCellIS oCell;
            while ((oCell = lnpk.NextCell()) != null ){
                StrFrameUncompressor dec = 
                        new StrFrameUncompressor(
                            oCell, 
                            oCell.Width, 
                            oCell.Height);
                PsxYuv yuv =
                        MDEC.getQualityMdec().DecodeFrame(
                            dec.getStream(), 
                            dec.getWidth(), 
                            dec.getHeight());
                
                String s = String.format("%s%02d_f%02d",
                        sOutFileBase,
                        oCell.PkIndex,
                        oCell.CellIndex);
                
                BufferedImage bi = yuv.toBufferedImage();
                long x, y, w, h; 
                if (oCell.Width == 320 && oCell.Height == 240) {
                    // don't do anything
                    x = 0;
                    y = 0;
                    w = 320;
                    h = 240;
                } else {
                    // paste the image into a larger image
                    // at the correct relative position
                    x = 320/2+4 - oCell.Xpos;
                    w = 320+32;
                    y = 352-1 - oCell.Ypos;
                    h = 368-1;
                    bi = PasteImage(bi, (int)x, (int)y, (int)w, (int)h);
                }
                
                ImageIO.write(bi, "png", new File(s + ".png"));
                
                //biscreen = addImage(oCell.BitMask, (int)x, (int)y, (int)w, (int)h);
                ImageIO.write(oCell.BitMask, "png", new File(s + "_mask.png"));
            } 
        } catch (CriticalUncompressException ex) {
            ex.printStackTrace();
            return -1;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }

        return 0;
    }
    
    private static BufferedImage PasteImage(BufferedImage oImage, int iX, int iY, int iWidth, int iHeight) {
        BufferedImage oScreen = new BufferedImage(iWidth, iHeight, oImage.getType());
        Graphics2D oScreenGraphic = oScreen.createGraphics();
        oScreenGraphic.drawImage(oImage, iX, iY, null);
        oScreenGraphic.dispose();
        return oScreen;
    }
    
    //-------------------------------------------------------------------------
    //-- Helper structors -----------------------------------------------------
    //-------------------------------------------------------------------------
    
    private static class LaPk {
        /*
         *  _Pk header_
         * 4 bytes: 'lapk'
         * 4 bytes: size of pk (starting after this value)
         * 4 bytes: number of cells
         * 12 * (number of cells): Cell descriptors
         */
        
        public char[] lapk = new char[4]; // 4
        public long Size;          // 4
        public long CellCount;     // 4
        public PkCellDescriptor[] CellDescriptors; // 12 * CellCount
        
        public long StartOffset;
        public long HeaderSize;
        public long Index;
        
        public LaPk(RandomAccessFile oRAF, long idx) throws IOException, NotThisTypeException {
            Index = idx;
            StartOffset = oRAF.getFilePointer();
            
            lapk[0] = (char)oRAF.read();
            lapk[1] = (char)oRAF.read();
            lapk[2] = (char)oRAF.read();
            lapk[3] = (char)oRAF.read();
            
            if (!(new String(lapk).equals("lapk"))) 
                throw new NotThisTypeException("Not a lapk at " + StartOffset);
            
            Size = IO.ReadUInt32LE(oRAF);
            CellCount = IO.ReadUInt32LE(oRAF);
            CellDescriptors = new PkCellDescriptor[(int)CellCount];
            
            // Read the descriptors
            for (int i = 0; i < CellDescriptors.length; i++) {
                CellDescriptors[i] = new PkCellDescriptor(this, i, oRAF);
            }
            
            HeaderSize = 4+4+4+ 12 * CellCount;
        }

        
    }
    
    private static class PkCellDescriptor {
        /*
         *  _Cell descriptor_
         * 4 bytes: offset of cell (after header)
         * 2 bytes: Negitive X pos
         * 2 bytes: Negitive Y pos
         * 4 bytes: sound effect?
         */
        public long CellOffset;
        public long Xpos;
        public long Ypos;
        public long Unknown;

        public LaPk ParentLaPk;
        public int Index;

        public PkCellDescriptor(LaPk lapk, int idx, RandomAccessFile oRAF) throws IOException {
            ParentLaPk = lapk;
            Index = idx;
            byte[] buff = new byte[12];
            if (oRAF.read(buff) != 12) throw new IOException();
            ByteArrayInputStream oBAIS = new ByteArrayInputStream(buff);
            CellOffset = IO.ReadUInt32LE(oBAIS);
            Xpos = IO.ReadUInt16LE(oBAIS);
            Ypos = IO.ReadUInt16LE(oBAIS);
            Unknown = IO.ReadUInt32LE(oBAIS);
        }
    }
    
    public static class LaPkCellIS extends ByteArrayInputStream implements IGetFilePointer {
        public int PkIndex;
        public int CellIndex;
        long m_lngCellStart;
        public long Xpos; // copied from cell descriptor
        public long Ypos; // copied from cell descriptor
        
        /*  _Cell header_
         * 2 bytes: Image Width
         * 2 bytes: Image Height
         * 2 bytes: Chrominance Quantization Scale 
         * 2 bytes: Luminance Quantization Scale
         * 4 bytes: Length of cell data in bytes (after this value)
         * 4 bytes: Number of run length codes?
         * (data length-4) bytes: width/16*height/16 compressed macro blocks
         * 
         * _Bit Mask_ <- Starts at 12+Cell_Data_Size
         * 4 bytes: Bit mask size
         * (size) bytes: Bit Mask data
         */
        public long Width;     // 2
        public long Height;    // 2
        long QuantChrom;       // 2
        long QuantLumin;       // 2
        long Size;             // 4
        long NumRunLenCodes;   // 4
        
        public BufferedImage BitMask;
        
        private LaPkCellIS(PkCellDescriptor oCell, RandomAccessFile oRAF) 
                throws IOException 
        {
            super(new byte[0]); // hafta call the ByteArrayInputStream constructor first
            this.buf = RealConstructor(oCell, oRAF);
            this.pos = 0;
            this.count = buf.length;
        }
        private byte[] RealConstructor(PkCellDescriptor oCell, RandomAccessFile oRAF) throws IOException {
            // Seek to the start of the cell
            oRAF.seek(oCell.ParentLaPk.StartOffset + oCell.ParentLaPk.HeaderSize + oCell.CellOffset);
            
            m_lngCellStart = oRAF.getFilePointer();
            CellIndex = oCell.Index;
            PkIndex = (int)oCell.ParentLaPk.Index;
            Xpos = oCell.Xpos;
            Ypos = oCell.Ypos;
            
            // read the header bytes
            byte buff[] = new byte [16];
            if (oRAF.read(buff) != 16) throw new IOException();
            ByteArrayInputStream oStream = 
                    new ByteArrayInputStream(buff);
            Width = IO.ReadUInt16LE(oStream);
            Height = IO.ReadUInt16LE(oStream);
            QuantChrom = IO.ReadUInt16LE(oStream);
            QuantLumin = IO.ReadUInt16LE(oStream);
            Size = IO.ReadUInt32LE(oStream);
            NumRunLenCodes = IO.ReadUInt32LE(oStream);
            ByteArrayOutputStream oCellWriter = new ByteArrayOutputStream((int)Size + 8);
            
            // Create an artifical header to feed to the StrFrameUncompresser
            WriteLainFrameHeader(oCellWriter, QuantChrom, QuantLumin, NumRunLenCodes);
            
            // Read the cell data
            for (int i = 0; i < Size; i++) {
                int b = oRAF.read();
                oCellWriter.write(b);
            }
            oCellWriter.flush();
            oCellWriter.close();
            
            // Now read the compressed bit mask
            // results in a 2 bits-per-pixel image
            oRAF.seek(m_lngCellStart + 12 + Size);
            byte[] abBitMask = Lain_Pk.Decompress(oRAF);
            
            BitMask = ConvertBitMaskToImage(abBitMask, Width, Height);
            
            return oCellWriter.toByteArray();
        }
        
        public static void WriteLainFrameHeader(
                ByteArrayOutputStream oByteStream,
                long QuantChrom,
                long QuantLumin,
                long NumRunLenCodes) 
        {
            try {
                oByteStream.write((int)QuantChrom); // normally run len code
                oByteStream.write((int)QuantLumin); // '''''''''''''''''''''
                IO.WriteInt16LE(oByteStream, 0x3800);
                IO.WriteInt16LE(oByteStream, NumRunLenCodes); // normally q scale
                IO.WriteInt16LE(oByteStream, 0x0000); // version 0 (Lain)
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public long getFilePointer() {
            return m_lngCellStart - 8 + this.pos;
        }
        
    }
    
    //-------------------------------------------------------------------------
    //-- Lain_LAPKS instance --------------------------------------------------
    //-------------------------------------------------------------------------
    
    RandomAccessFile m_oFile;
    long m_lngPkIndex = 0;
    long m_lngCellIndex = 0;
    LaPk m_oCurrentPk;
    
    /** Setup for reading from the file. */
    public Lain_LAPKS(String sFile) throws IOException {
        m_oFile = new RandomAccessFile(sFile, "r");
    }
    
    /** Retrieves the next animation cell in the LAPKS.BIN file.
     *  Returns null if there are no more cells available. */
    public LaPkCellIS NextCell() throws IOException {
        // Just getting started?
        if (m_oCurrentPk == null) {
            try {
                // Then read the first lapk header
                m_oCurrentPk = new LaPk(m_oFile, m_lngPkIndex);
            } catch (NotThisTypeException ex) {
                // If this is the first cell and there is no 'lapk' header
                // then this is not the LAPKS.BIN file.
                ex.printStackTrace();
                return null;
            }
        }
        else {
            m_lngCellIndex++; // next cell
        
            // Done with the lapk?
            if (m_lngCellIndex >= m_oCurrentPk.CellCount) {
                if (!MoveToNextPk()) // move to next lapk
                    return null;
            }
        }
        // Create a cell input stream and return it
        return new LaPkCellIS(m_oCurrentPk.CellDescriptors[(int)m_lngCellIndex], m_oFile);
    }
    
    
    private boolean MoveToNextPk() throws IOException {
        
        // next lapk, and reset the cell index
        m_lngPkIndex++;
        m_lngCellIndex = 0;
        
        // calculate the next pk start, at the start of the next 2048
        // boundary after the end of the current lapk
        long lngNextOffset = m_oCurrentPk.StartOffset + m_oCurrentPk.Size + 8;
        if ((lngNextOffset % 2048) != 0) {
            lngNextOffset += 2048 - (lngNextOffset % 2048);
        }
        
        
        do {
            // we're past the end of the file
            if (lngNextOffset >= m_oFile.length()) return false;
            
            m_oFile.seek(lngNextOffset);
            
            try {
                m_oCurrentPk = new LaPk(m_oFile, m_lngPkIndex);
            } catch (NotThisTypeException ex) {
                /* There seems to be an error in one of the lapk headers
                 * reporting that the lapk size is at least 4000 bytes smaller
                 * than it is. So we'll just keep trying every sector until
                 * we find the next lapk. */
                // These are the two offsets
                if (!(ex.getMessage().endsWith("13135872") || 
                      ex.getMessage().endsWith("13137920")))
                {
                    ex.printStackTrace();
                }
                m_oCurrentPk = null;
                lngNextOffset += 2048;
            }
        } while (m_oCurrentPk == null);
        return true;
    }
    

    
    
    /** Converts 2 bits-per-pixel to RGB gray (24 bits-per-pixel) */
    private static int[] Mask4BitsToRGBGray = new int[] {
        // TYPE_BYTE_GRAY is NOT linear...stupid Java crap
        0x000000, // -> 0x00
        0x9C9C9C, // -> 0x55
        0xD5D5D5, // -> 0xAA
        0xFFFFFF  // -> 0xFF
    };
    
    /** Converts a 2 bits-per-pixel array to a grayscale BufferedImage */
    private static BufferedImage ConvertBitMaskToImage(byte[] abBitMask, 
                                                       long lngCellWidth, 
                                                       long lngCellHeight) 
    {
        // TODO: it would be much better to use a Raster here
        BufferedImage bi = new BufferedImage(
                (int)lngCellWidth, 
                (int)lngCellHeight,
                BufferedImage.TYPE_BYTE_GRAY);
        
        for (int y = 0; y < lngCellHeight; y++) {
            for (int x = 0; x < lngCellWidth / 4; x++) {
                byte b = abBitMask[(int)(x + y * lngCellWidth/4)];
                
                if (DebugVerbose > 3)
                    System.err.print(String.format("%02x ", b));
                
                int bits;
                bits = ((b >>> 6) & 3);
                bi.setRGB(x*4 + 0, y, Mask4BitsToRGBGray[bits]);
                
                bits = ((b >>> 4) & 3);
                bi.setRGB(x*4 + 1, y, Mask4BitsToRGBGray[bits]);
                
                bits = ((b >>> 2) & 3);
                bi.setRGB(x*4 + 2, y, Mask4BitsToRGBGray[bits]);
                
                bits = ((b >>> 0) & 3);
                bi.setRGB(x*4 + 3, y, Mask4BitsToRGBGray[bits]);
            }        
            if (DebugVerbose > 3)
                System.err.println();
        }
        
        return bi;        
    }
    

}
