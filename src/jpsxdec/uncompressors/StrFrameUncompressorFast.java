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
 * StrFrameUncompressorFast.java
 */

package jpsxdec.uncompressors;

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.MDEC.Mdec16Bits;

public class StrFrameUncompressorFast extends StrFrameUncompressor {

    private static final int AC_ESCAPE_CODE_MASK = 0xFC00;   // 1111110000000000b
    private static final int AC_ESCAPE_CODE_BITS = 0x0400;   // 0000010000000000b
    private static final int VLC_END_OF_BLOCK_MASK = 0xC000; // 1100000000000000b
    private static final int VLC_END_OF_BLOCK_BITS = 0x8000; // 1000000000000000b
    private static final int VLC_END_OF_BLOCK_BITS_LEN = 2; 
    
    public StrFrameUncompressorFast(InputStream oIS, long lngWidth, long lngHeight) throws IOException {
        super(oIS, lngWidth, lngHeight);
    }

    @Override
    protected Mdec16Bits Decode_AC_Code() throws IOException, UncompressionException {
        
        // Peek at the upcoming bits
        long lngBits = m_oBitReader.PeekUnsignedBits(AC_LONGEST_VARIABLE_LENGTH_CODE);
        
        if ((lngBits & AC_ESCAPE_CODE_MASK) == AC_ESCAPE_CODE_BITS) { // Is it the escape code?
            
            return Decode_AC_EscapeCode();
            
        } else if ((lngBits & VLC_END_OF_BLOCK_MASK) == VLC_END_OF_BLOCK_BITS) { // end of block?
            
            m_oBitReader.SkipBits(VLC_END_OF_BLOCK_BITS_LEN);
            MDEC.Mdec16Bits tRlc = 
                    new MDEC.Mdec16Bits(MDEC.MDEC_END_OF_BLOCK);
            tRlc.VariableLengthCodeBits = VLC_END_OF_BLOCK;
            return tRlc;
            
        } else { // must be a normal code
            
            return Decode_AC_VariableLengthCode(lngBits);
            
        }
    }
    
    private static final int b1000000000000000 = 0x8000;
    private static final int b0100000000000000 = 0x4000;
    private static final int b0010000000000000 = 0x2000;
    private static final int b0001100000000000 = 0x1800;
    private static final int b0001000000000000 = 0x1000;
    private static final int b0000100000000000 = 0x0800;
    private static final int b0000001000000000 = 0x0200;
    private static final int b0000000100000000 = 0x0100;
    private static final int b0000000010000000 = 0x0080;
    private static final int b0000000001000000 = 0x0040;
    private static final int b0000000000100000 = 0x0020;
    private static final int b0000000000010000 = 0x0010;
    protected Mdec16Bits Decode_AC_VariableLengthCode(long lngBits) throws IOException, UncompressionException {
        MDEC.Mdec16Bits oRlc = new MDEC.Mdec16Bits();
        ACVariableLengthCode aoVarLenCodes[];
        ACVariableLengthCode vlc = null;
        
        // Use the correct AC variable length code list
        if (m_iFrameType != FRAME_LAIN && m_iFrameType != FRAME_LAIN_FINAL_MOVIE) 
        {
            aoVarLenCodes = AC_VARIABLE_LENGTH_CODES_MPEG1;
        } else {
            aoVarLenCodes = AC_VARIABLE_LENGTH_CODES_LAIN;
        }
        
        // Walk through the bit tree
        if (    (lngBits & b1000000000000000) > 0) {         // "1"
            if ((lngBits & b0100000000000000) > 0) {         // "11"
                vlc = aoVarLenCodes[0];
            } else {                                         // "10"
                throw new RuntimeException("this should already be handled!"); // this should already be handled!
            }
        } else if ((lngBits & b0100000000000000) > 0) {      // "01"
            if    ((lngBits & b0010000000000000) > 0) {      // "011"
                vlc = aoVarLenCodes[1];
            } else {                                         // "010x"
                vlc = aoVarLenCodes[2 + (int)((lngBits >>> 12) & 1)];
            }
        } else if ((lngBits & b0010000000000000) > 0) {      // "001"
            if    ((lngBits & b0001100000000000) > 0)  {     // "001xx"
                vlc = aoVarLenCodes[3 + (int)((lngBits >>> 11) & 3)];
            } else {                                         // "00100xxx"
                vlc = aoVarLenCodes[15 + (int)((lngBits >>> 8) & 7)];
            }
        } else if ((lngBits & b0001000000000000) > 0) {      // "0001xx"
            vlc = aoVarLenCodes[7 + (int)((lngBits >>> 10) & 3)];
        } else if ((lngBits & b0000100000000000) > 0) {      // "00001xx"
            vlc = aoVarLenCodes[11 + (int)((lngBits >>> 9) & 3)];
        } else if ((lngBits & b0000001000000000) > 0) {      // "0000001xxx"
            vlc = aoVarLenCodes[23 + (int)((lngBits >>> 6) & 7)];
        } else if ((lngBits & b0000000100000000) > 0) {      // "00000001xxxx"
            vlc = aoVarLenCodes[31 + (int)((lngBits >>> 4) & 15)];
        } else if ((lngBits & b0000000010000000) > 0) {      // "000000001xxxx"
            vlc = aoVarLenCodes[47 + (int)((lngBits >>> 3) & 15)];
        } else if ((lngBits & b0000000001000000) > 0) {      // "0000000001xxxx"
            vlc = aoVarLenCodes[63 + (int)((lngBits >>> 2) & 15)];
        } else if ((lngBits & b0000000000100000) > 0) {      // "00000000001xxxx"
            vlc = aoVarLenCodes[79 + (int)((lngBits >>> 1) & 15)];
        } else if ((lngBits & b0000000000010000) > 0) {      // "000000000001xxxx"
            vlc = aoVarLenCodes[95 + (int)((lngBits      ) & 15)];
        }
        
        // Search through the list to find the matching AC variable length code
        if (vlc != null) {
            // Yay we found it!
            /*    
            String s = BufferedBitReader.PadZeroLeft(Long.toBinaryString(lngBits), 16);
            if (!s.startsWith(vlc.VariableLengthCode)) {
                System.out.println(oRlc.VariableLengthCodeBits);
                System.out.println(vlc.VariableLengthCode);
            }
            */
            // Skip that many bits
            m_oBitReader.SkipBits(vlc.VariableLengthCode.length());

            // Save the resulting code, and run of zeros
            oRlc.VariableLengthCodeBits = vlc.VariableLengthCode;
            oRlc.Top6Bits = vlc.RunOfZeros;
            // Take either the positive or negitive AC coefficient,
            // depending on the sign bit
            if (m_oBitReader.ReadUnsignedBits(1) == 1) {
                // negative
                oRlc.Bottom10Bits = -vlc.AbsoluteLevel;
                oRlc.VariableLengthCodeBits += "1";
            } else {
                // positive
                oRlc.Bottom10Bits = vlc.AbsoluteLevel;
                oRlc.VariableLengthCodeBits += "0";
            }
            
            return oRlc;

        } else {
        
            throw new UncompressionException(
                    "Error decoding macro block: " +
                    "Unmatched AC variable length code: " +
                     BufferedBitReader.PadZeroLeft(Long.toBinaryString(lngBits), 16) +
                     " at " + String.format("%1.3f", m_oBitReader.getPosition()));
        }
        
    }
    
    

}
