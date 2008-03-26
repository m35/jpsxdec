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
 * StrFrameRecompressorIS.java
 */

package jpsxdec.uncompressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import jpsxdec.mdec.MDEC.Mdec16Bits;
import jpsxdec.uncompressors.BufferedBitReader;
import jpsxdec.util.IO;

/** An extention of the StrFrameUncompresserIS which can re-compress the 
 *  uncompressed data. */
public class StrFrameRecompressorIS extends StrFrameUncompressorIS {

    /** Generate a quick reverse lookup of variable-length-code table.
     *  Map of run-length-code "#,#" to the bit code "101010101" */
    private static Hashtable<String, String> CreateReverseVLCtable(ACVariableLengthCode[] itbl) {
        Hashtable<String, String> otbl = new Hashtable<String, String>();
        for (ACVariableLengthCode oCode : itbl) {
            otbl.put(oCode.RunOfZeros + "," + oCode.AbsoluteLevel, 
                    oCode.VariableLengthCode);
        }
        return otbl;
    }
    
    private static Hashtable<String, String> AC_VARIABLE_LENGTH_CODES_LAIN_REVERSE =
            CreateReverseVLCtable(AC_VARIABLE_LENGTH_CODES_LAIN);
    
    private static Hashtable<String, String> AC_VARIABLE_LENGTH_CODES_MPEG1_REVERSE =
            CreateReverseVLCtable(AC_VARIABLE_LENGTH_CODES_MPEG1);
    
    // #########################################################################

    /** Just a wrapper for StrFrameUncompressorIS. */
    public StrFrameRecompressorIS(InputStream oIS, long m_lngWidth, long m_lngHeight) 
            throws IOException 
    {
        super(oIS, m_lngWidth, m_lngHeight);
    }
    
    /** Changes the number of run-length codes specified in the header. */
    public void setNumberOfRunLenthCodes(long lng) {
        super.m_lngNumberOfRunLenthCodes = lng;
    }
    
    /** Recompresses the frame and writes it to the OutputStream. */
    public void write(OutputStream os) throws IOException {
        // write the frame header
        WriteFrameHeader(os);
        
        BitStringWriter bsw = new BitStringWriter(os);
        if (super.m_iFrameType == FRAME_LAIN || 
            super.m_iFrameType == FRAME_LAIN_FINAL_MOVIE)
            bsw.setBigEndian(true);
        else
            bsw.setBigEndian(false);
            
        for (int i=0; i < super.m_oMdecList.length; i++) {
            if (DebugVerbose > 6)
                System.err.println("Compressing macroblock " + i);

            MacroBlock oMacroBlock = super.m_oMdecList[i];
            // compress the mdec codes
            GenerateVlcCodes(oMacroBlock);
            // write out each block's bits
            writeBlock(bsw, oMacroBlock.Cr);
            writeBlock(bsw, oMacroBlock.Cb);
            writeBlock(bsw, oMacroBlock.Y1);
            writeBlock(bsw, oMacroBlock.Y2);
            writeBlock(bsw, oMacroBlock.Y3);
            writeBlock(bsw, oMacroBlock.Y4);
        }
        bsw.flush();
        
    }
    
    private void WriteFrameHeader(OutputStream os) throws IOException {
        
        switch (m_iFrameType) {
            case FRAME_FF7:
                // FF7 videos have 40 bytes of camera data 
                // at the start of the frame. just write zeros
                os.write(new byte[40]);
            case FRAME_FF7_WITHOUT_CAMERA:
            case FRAME_VER2: case FRAME_VER3:
                IO.WriteInt16LE(os, super.m_lngNumberOfRunLenthCodes);
                IO.WriteInt16LE(os, super.m_lngHeader3800);
                IO.WriteInt16LE(os, super.m_lngQuantizationScaleChrom);
                IO.WriteInt16LE(os, super.m_lngVersion);
                break;
                
            case FRAME_LAIN_FINAL_MOVIE:
            case FRAME_LAIN:
                // bytes are reversed for little-endian writing
                os.write((byte)super.m_lngQuantizationScaleLumin);
                os.write((byte)super.m_lngQuantizationScaleChrom);
                
                IO.WriteInt16LE(os, super.m_lngHeader3800);
                IO.WriteInt16LE(os, super.m_lngNumberOfRunLenthCodes);
                IO.WriteInt16LE(os, super.m_lngVersion);
                break;
                
            default:
                throw new IOException("Unhandled frame type.");
        }
    }
    
    private void writeBlock(BitStringWriter bsw,Block oBlk) throws IOException {
        bsw.write(oBlk.DCCoefficient.VariableLengthCodeBits);
        for (Mdec16Bits oMdec : oBlk.ACCoefficients)
            bsw.write(oMdec.VariableLengthCodeBits);
        bsw.write(oBlk.EndOfBlock.VariableLengthCodeBits);
        
    }
    
    // #########################################################################
    // #########################################################################
    
    /** Rebuilds the variable-length bit codes for a 
     *  macro block based on the frame type. */
    private void GenerateVlcCodes(MacroBlock oMacBlk) 
    {
        
        try {
        
            for (String sBlock : new String[] {"Cr","Cb","Y1","Y2","Y3","Y4"}) {

                Block oOriginalBlk = oMacBlk.getBlock(sBlock);
                
                if (DebugVerbose > 6)
                    System.err.println("Compressing " + sBlock);
        
                // First compress the DC coefficient
                switch (m_iFrameType) {
                    case FRAME_VER2:
                    case FRAME_FF7:
                    case FRAME_FF7_WITHOUT_CAMERA:
                    case FRAME_LAIN:
                    case FRAME_LAIN_FINAL_MOVIE:
                        if (sBlock.startsWith("Y"))
                            oOriginalBlk.DCCoefficient.VariableLengthCodeBits = 
                                 Compress_v2_DC_Coefficient(oOriginalBlk.DCCoefficient, m_lngQuantizationScaleLumin);
                        else
                            oOriginalBlk.DCCoefficient.VariableLengthCodeBits = 
                                Compress_v2_DC_Coefficient(oOriginalBlk.DCCoefficient, m_lngQuantizationScaleChrom);
                        break;
                    case FRAME_VER3:
                        oOriginalBlk.DCCoefficient.VariableLengthCodeBits = 
                            Compress_v3_DC_Coefficient(oOriginalBlk.DCCoefficient);
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled frame type " + m_iFrameType);
                }

                // then compress the AC coefficients (except the EOB)
                for (Mdec16Bits oACMdecCode : oOriginalBlk.ACCoefficients) {
                    oACMdecCode.VariableLengthCodeBits =
                        Compress_AC_Coefficient(oACMdecCode);
                }
                
                // compress the end of block code
                // oOriginalBlk.EndOfBlock
                oOriginalBlk.EndOfBlock.VariableLengthCodeBits = VLC_END_OF_BLOCK;
                
            }
        
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException("Not enough mdec codes to make a macro block.");
        }
    }
    
    private static String Compress_v2_DC_Coefficient(
            Mdec16Bits oOriginalMdecCode, long lngFrameScale) 
    {
        // the top 6 bits should = the frame's Chrominance/Luminance quantization scale
        if (oOriginalMdecCode.Top6Bits != lngFrameScale) 
            throw new IllegalArgumentException("Frame's quantization scale must match every macro block's quantization scale.");

        String s = Int2Bits(oOriginalMdecCode.Bottom10Bits, 10); 
        
        if (DebugVerbose > 6)
            System.err.println("Compressing " + oOriginalMdecCode.toString() 
                    + " to " + s + " (DC)");
        
        return s;
    }
    
    private static String Compress_v3_DC_Coefficient(Mdec16Bits oCode) {
        throw new UnsupportedOperationException("v3 frame compression not implemented.");
    }
    
    /** First tries to compress using the lookup table. 
     *  If fails, creates an escape code. */
    private String Compress_AC_Coefficient(Mdec16Bits oOriginalMdec) {
        String sBits;
        if (m_iFrameType == FRAME_LAIN || m_iFrameType == FRAME_LAIN_FINAL_MOVIE) {
            sBits = AC_VARIABLE_LENGTH_CODES_LAIN_REVERSE.get(
                    oOriginalMdec.Top6Bits + "," + Math.abs(oOriginalMdec.Bottom10Bits));
            if (sBits == null)
                return Compress_AC_EscapeCode_Lain(oOriginalMdec);
        } else {
            sBits = AC_VARIABLE_LENGTH_CODES_MPEG1_REVERSE.get(
                    oOriginalMdec.Top6Bits + "," + Math.abs(oOriginalMdec.Bottom10Bits));
            if (sBits == null)
                return Compress_AC_EscapeCode_Normal(oOriginalMdec);
        }
        
        if (oOriginalMdec.Bottom10Bits < 0)
            sBits += "1";
        else
            sBits += "0";
        
        if (DebugVerbose > 6)
            System.err.println("Compressing " + oOriginalMdec.toString() 
                    + " to " + sBits + " (standard code)");
        
        return sBits;
    }

    private static String Compress_AC_EscapeCode_Normal(Mdec16Bits oOriginalMdec) 
    {
        String s = AC_ESCAPE_CODE +
               Int2Bits(oOriginalMdec.Top6Bits, 6) +
               Int2Bits(oOriginalMdec.Bottom10Bits, 10); 
        
        if (DebugVerbose > 6)
            System.err.println("Compressing " + oOriginalMdec.toString() 
                    + " to " + s + " (normal escape)");
        
        return s;
    }
    
    private static String Compress_AC_EscapeCode_Lain(Mdec16Bits oOriginalMdec) 
    {
        String s = AC_ESCAPE_CODE + 
                   Int2Bits(oOriginalMdec.Top6Bits, 6);
        
        if (oOriginalMdec.Bottom10Bits >= -256 && oOriginalMdec.Bottom10Bits <= -128)
            s += "10000000" + Int2Bits(oOriginalMdec.Bottom10Bits + 256, 8);
        else if (oOriginalMdec.Bottom10Bits <= 255 && oOriginalMdec.Bottom10Bits >= 128)
            s += "00000000" + Int2Bits(oOriginalMdec.Bottom10Bits, 8);
        else if (oOriginalMdec.Bottom10Bits > -128 && oOriginalMdec.Bottom10Bits < 128
                 && oOriginalMdec.Bottom10Bits != 0)
            s += Int2Bits(oOriginalMdec.Bottom10Bits, 8);
        else
            throw new IllegalArgumentException(
                    "Unable to create Lain escape code for " 
                    + oOriginalMdec.toString());
        
        if (DebugVerbose > 6)
            System.err.println("Compressing " + oOriginalMdec.toString() 
                    + " to " + s + " (Lain escape)");
        
        return s;
    }
    
    private final static int[] BITMASK = new int[] {
        0,
        (1 << 1 ) - 1,
        (1 << 2 ) - 1,
        (1 << 3 ) - 1,
        (1 << 4 ) - 1,
        (1 << 5 ) - 1,
        (1 << 6 ) - 1,
        (1 << 7 ) - 1,
        (1 << 8 ) - 1,
        (1 << 9 ) - 1,
        (1 << 10) - 1,
        (1 << 11) - 1,
    };
    
    /** Converts a int/long to a string of bits. */
    private static String Int2Bits(long lng, int bits) {
        return BufferedBitReader.PadZeroLeft(
                Long.toBinaryString(lng & BITMASK[bits]), bits);
    }
    
    /** Writes bits to an OutputStream, 16 bits at a time, 
     *  in little-endian or big-endian order. */
    private static class BitStringWriter {

        private OutputStream m_os;
        private int m_iNextWrite = 0;
        private int m_iIndex;
        private boolean m_blnIsBigEndian = true;
        
        public BitStringWriter(OutputStream os) {
            m_os = os;
        }
        
        /** Write a string of bits. */
        public void write(String s) throws IOException {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '0') {
                    write(false);
                } else if (c == '1') {
                    write(true);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        
        /** Write one bit */
        public void write(boolean blnBit) throws IOException {
            m_iNextWrite <<= 1;
            if (blnBit) {
                m_iNextWrite |= 1;
            }
            m_iIndex++;
            if (m_iIndex == 16) {
                write();
            }      
        }
        
        /** Closes the underlying stream after flushing the remaining bits. */
        public void close() throws IOException {
            flush();
            m_os.close();
        }

        /** If there are bits remaining to write, writes them, filling
         *  the remaining bits of the word with zeros. */
        void flush() throws IOException {
            if (m_iIndex != 0) {
                m_iNextWrite <<= 16 - m_iIndex;
                write();
            }
        }
        
        /** Write the buffered 16 bits as big-endian or little-endian. */
        private void write() throws IOException {
            if (m_blnIsBigEndian) {
                m_os.write((m_iNextWrite >>> 8) & 0xFF);
                m_os.write( m_iNextWrite        & 0xFF);
            } else {
                m_os.write( m_iNextWrite        & 0xFF);
                m_os.write((m_iNextWrite >>> 8) & 0xFF);
            }
            m_iIndex = 0;
            m_iNextWrite = 0;
        }
        
        public void setBigEndian(boolean bln) {
            m_blnIsBigEndian = bln;
        }
        
        public void setLittleEndian(boolean bln) {
            m_blnIsBigEndian = !bln;
        }
        
    }

}
