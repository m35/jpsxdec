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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Lain;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;
import jpsxdec.util.Imaging;


/** Functions to decode the Lain 'poses' from the LAPKS.BIN file. */
public class Lain_LAPKS {

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Expecting 2 parameters: LAPKS.BIN <out-base-name>");
            return;
        }
        decodeLAPKS(args[0], args[1]);
    }

    /** Decodes the numerous Lain poses from the LAPKS.BIN file. The LAPKS.BIN
     *  file is the same on both disc 1 and disc 2. This function needs a
     *  standard 2048-per-sector (i.e. ISO) copy of the file. This can easily
     *  be accomplished by simply copying the file off the disc using normal
     *  operating system commands, or even providing the path directly to the
     *  file on the disc.
     *<p>
     *  This function will dump the over 1000 animation cells, including the
     *  bit-mask used to provide transparency to the images. The cells are
     *  centered in a larger image according to the cell's x,y position found
     *  with the cell data.
     *<p>
     *  Output file names will look like this
     *<pre>
     *  [sOutFileBase][animation#]_f[frame#].png
     *  [sOutFileBase][animation#]_f[frame#]_mask.png
     *</pre>
     *  Note that I don't quite understand why the bit-mask has 4 values,
     *  since I can only see a need for 3 (transparent, slightly transparent,
     *  and totally opaque). I got confused while stepping through the
     *  game's assembly code. Perhaps another go at it would be good.
     *
     * @param sInLAPKS_BIN  the path to the LAPKS.BIN file
     * @param sOutFileBase  output base name of the files
     */
    public static int decodeLAPKS(String sInLAPKS_BIN, String sOutFileBase) {

        try {
            Lain_LAPKS lnpk = new Lain_LAPKS(sInLAPKS_BIN);
            LaPkCellIS cell;
            while ((cell = lnpk.nextCell()) != null ){
                MdecDecoder_double oDecoder = new MdecDecoder_double(
                        new PsxMdecIDCT_double(), cell.Width, cell.Height);
                BitStreamUncompressor_Lain uncompresor = BitStreamUncompressor_Lain.makeLain(cell.Data);
                oDecoder.decode(uncompresor);
                RgbIntImage rgb = new RgbIntImage(cell.Width, cell.Height);
                oDecoder.readDecodedRgb(rgb.getWidth(), rgb.getHeight(), rgb.getData());

                String s = String.format("%s%02d_f%02d",
                        sOutFileBase,
                        cell.PkIndex,
                        cell.CellIndex);

                BufferedImage bi = rgb.toBufferedImage();
                int x, y, w, h;
                if (cell.Width == 320 && cell.Height == 240) {
                    // don't do anything
                    x = 0;
                    y = 0;
                    w = 320;
                    h = 240;
                } else {
                    // paste the image into a larger image
                    // at the correct relative position
                    x = 320/2+4 - cell.Xpos;
                    w = 320+32;
                    y = 352-1 - cell.Ypos;
                    h = 368-1;
                    bi = pasteImage(bi, x, y, w, h);
                }

                ImageIO.write(bi, "png", new File(s + ".png"));

                //biscreen = addImage(oCell.BitMask, (int)x, (int)y, (int)w, (int)h);
                ImageIO.write(cell.BitMask, "png", new File(s + "_mask.png"));
            }
        } catch (MdecException.EndOfStream ex) {
            ex.printStackTrace();
            return -1;
        } catch (MdecException.ReadCorruption ex) {
            ex.printStackTrace();
            return -1;
        } catch (BinaryDataNotRecognized ex) {
            ex.printStackTrace();
            return -1;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }

        return 0;
    }

    private static BufferedImage pasteImage(BufferedImage bi, int iX, int iY,
                                            int iWidth, int iHeight)
    {
        BufferedImage screen = new BufferedImage(iWidth, iHeight, bi.getType());
        Graphics2D oScreenGraphic = screen.createGraphics();
        oScreenGraphic.drawImage(bi, iX, iY, null);
        oScreenGraphic.dispose();
        return screen;
    }

    //-------------------------------------------------------------------------
    //-- Helper structors -----------------------------------------------------
    //-------------------------------------------------------------------------

    private static class NotLapkException extends Exception {
        public NotLapkException(String message) {
            super(message);
        }
    }

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

        public LaPk(RandomAccessFile raf, long lngIdx) throws IOException, NotLapkException {
            Index = lngIdx;
            StartOffset = raf.getFilePointer();

            lapk[0] = (char)raf.read();
            lapk[1] = (char)raf.read();
            lapk[2] = (char)raf.read();
            lapk[3] = (char)raf.read();

            if (!(new String(lapk).equals("lapk")))
                throw new NotLapkException("Not a lapk at " + StartOffset);

            Size = IO.readUInt32LE(raf);
            CellCount = IO.readUInt32LE(raf);
            CellDescriptors = new PkCellDescriptor[(int)CellCount];

            // Read the descriptors
            for (int i = 0; i < CellDescriptors.length; i++) {
                CellDescriptors[i] = new PkCellDescriptor(this, i, raf);
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
        public int Xpos;
        public int Ypos;
        public long Unknown;

        public LaPk ParentLaPk;
        public int Index;

        public PkCellDescriptor(LaPk lapk, int iIdx, RandomAccessFile raf) throws IOException {
            ParentLaPk = lapk;
            Index = iIdx;
            byte[] buff = new byte[12];
            if (raf.read(buff) != 12) throw new IOException();
            ByteArrayInputStream oBAIS = new ByteArrayInputStream(buff);
            CellOffset = IO.readUInt32LE(oBAIS);
            Xpos = IO.readUInt16LE(oBAIS);
            Ypos = IO.readUInt16LE(oBAIS);
            Unknown = IO.readUInt32LE(oBAIS);
        }
    }

    public static class LaPkCellIS {
        public int PkIndex;
        public int CellIndex;
        long _lngCellStart;
        public int Xpos; // copied from cell descriptor
        public int Ypos; // copied from cell descriptor

        /*  _Cell header_
         * 2 bytes: Image Width
         * 2 bytes: Image Height
         * 2 bytes: Chroma Quantization Scale
         * 2 bytes: Luma Quantization Scale
         * 4 bytes: Length of cell data in bytes (after this value)
         * 4 bytes: Number of run length codes?
         * (data length-4) bytes: width/16*height/16 compressed macro blocks
         *
         * _Bit Mask_ <- Starts at 12+Cell_Data_Size
         * 4 bytes: Bit mask size
         * (size) bytes: Bit Mask data
         */
        public int Width;     // 2
        public int Height;    // 2
        long QuantChrom;       // 2
        long QuantLumin;       // 2
        long Size;             // 4
        long NumRunLenCodes;   // 4

        public BufferedImage BitMask;

        public byte[] Data;

        private LaPkCellIS(PkCellDescriptor cell, RandomAccessFile raf)
                throws IOException
        {
            // Seek to the start of the cell
            raf.seek(cell.ParentLaPk.StartOffset + cell.ParentLaPk.HeaderSize + cell.CellOffset);

            _lngCellStart = raf.getFilePointer();
            CellIndex = cell.Index;
            PkIndex = (int)cell.ParentLaPk.Index;
            Xpos = cell.Xpos;
            Ypos = cell.Ypos;

            // read the header bytes
            byte abBuff[] = new byte [16];
            if (raf.read(abBuff) != 16) throw new IOException();
            ByteArrayInputStream bais = new ByteArrayInputStream(abBuff);
            Width = IO.readSInt16LE(bais);
            Height = IO.readSInt16LE(bais);
            QuantChrom = IO.readUInt16LE(bais);
            QuantLumin = IO.readUInt16LE(bais);
            Size = IO.readUInt32LE(bais);
            NumRunLenCodes = IO.readUInt32LE(bais);
            ByteArrayOutputStream cellWriter = new ByteArrayOutputStream((int)Size + 8);

            // Create an artifical header to feed to the StrFrameUncompresser
            writeLainFrameHeader(cellWriter, QuantChrom, QuantLumin, NumRunLenCodes);

            // Read the cell data
            for (int i = 0; i < Size; i++) {
                int b = raf.read();
                cellWriter.write(b);
            }
            cellWriter.flush();
            cellWriter.close();

            // Now read the compressed bit mask
            // results in a 2 bits-per-pixel image
            raf.seek(_lngCellStart + 12 + Size);
            byte[] abBitMask = Lain_Pk.decompress(raf);

            BitMask = convertBitMaskToImage(abBitMask, Width, Height);

            Data = cellWriter.toByteArray();
        }

        public static void writeLainFrameHeader(
                ByteArrayOutputStream os,
                long QuantChrom,
                long QuantLumin,
                long NumRunLenCodes)
        {
            try {
                os.write((int)QuantChrom); // normally run len code
                os.write((int)QuantLumin); // '''''''''''''''''''''
                IO.writeInt16LE(os, 0x3800);
                IO.writeInt16LE(os, (int)NumRunLenCodes); // normally q scale
                IO.writeInt16LE(os, 0x0000); // version 0 (Lain)
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    //-------------------------------------------------------------------------
    //-- Lain_LAPKS instance --------------------------------------------------
    //-------------------------------------------------------------------------

    private RandomAccessFile _raFile;
    private long _lngPkIndex = 0;
    private long _lngCellIndex = 0;
    private LaPk _currentPk;

    /** Setup for reading from the file. */
    public Lain_LAPKS(String sFile) throws IOException {
        _raFile = new RandomAccessFile(sFile, "r");
    }

    /** Retrieves the next animation cell in the LAPKS.BIN file.
     *  Returns null if there are no more cells available. */
    public LaPkCellIS nextCell() throws IOException {
        // Just getting started?
        if (_currentPk == null) {
            try {
                // Then read the first lapk header
                _currentPk = new LaPk(_raFile, _lngPkIndex);
            } catch (NotLapkException ex) {
                // If this is the first cell and there is no 'lapk' header
                // then this is not the LAPKS.BIN file.
                ex.printStackTrace();
                return null;
            }
        }
        else {
            _lngCellIndex++; // next cell

            // Done with the lapk?
            if (_lngCellIndex >= _currentPk.CellCount) {
                if (!moveToNextPk()) // move to next lapk
                    return null;
            }
        }
        // Create a cell input stream and return it
        return new LaPkCellIS(_currentPk.CellDescriptors[(int)_lngCellIndex], _raFile);
    }


    private boolean moveToNextPk() throws IOException {

        // next lapk, and reset the cell index
        _lngPkIndex++;
        _lngCellIndex = 0;

        // calculate the next pk start, at the start of the next 2048
        // boundary after the end of the current lapk
        long lngNextOffset = _currentPk.StartOffset + _currentPk.Size + 8;
        if ((lngNextOffset % 2048) != 0) {
            lngNextOffset += 2048 - (lngNextOffset % 2048);
        }


        do {
            // we're past the end of the file
            if (lngNextOffset >= _raFile.length()) return false;

            _raFile.seek(lngNextOffset);

            try {
                _currentPk = new LaPk(_raFile, _lngPkIndex);
            } catch (NotLapkException ex) {
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
                _currentPk = null;
                lngNextOffset += 2048;
            }
        } while (_currentPk == null);
        return true;
    }




    /** Converts 2 bits-per-pixel to 256 gray (8 bits-per-pixel) */
    private static byte[] Mask4BitsTo8Gray = new byte[] {
        (byte)0x00,
        (byte)0x55,
        (byte)0xAA,
        (byte)0xFF
    };

    /** Converts a 2 bits-per-pixel array to a grayscale BufferedImage */
    private static BufferedImage convertBitMaskToImage(byte[] abBitMask,
                                                       int iCellWidth,
                                                       int iCellHeight)
    {
        byte[] abPixels = new byte[iCellWidth * iCellHeight];

        for (int y = 0; y < iCellHeight; y++) {
            for (int x = 0; x < iCellWidth / 4; x++) {
                byte b = abBitMask[(int)(x + y * iCellWidth/4)];

                if (DEBUG) System.err.print(String.format("%02x ", b));

                int iBits;
                iBits = ((b >>> 6) & 3);
                abPixels[x*4 + 0  + y * iCellWidth] = Mask4BitsTo8Gray[iBits];

                iBits = ((b >>> 4) & 3);
                abPixels[x*4 + 1  + y * iCellWidth] = Mask4BitsTo8Gray[iBits];

                iBits = ((b >>> 2) & 3);
                abPixels[x*4 + 2  + y * iCellWidth] = Mask4BitsTo8Gray[iBits];

                iBits = ((b >>> 0) & 3);
                abPixels[x*4 + 3  + y * iCellWidth] = Mask4BitsTo8Gray[iBits];
            }
            if (DEBUG) System.err.println();
        }

        return Imaging.createLinearGrayIndexed256(abPixels, iCellWidth, iCellHeight);
    }


}
