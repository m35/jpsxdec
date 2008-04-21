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
 * NativeDecoder.java
 */

package jpsxdec.nativeclass;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.uncompressors.ArrayBitReader;
import jpsxdec.uncompressors.CriticalUncompressException;
import jpsxdec.uncompressors.StrFrameUncompressor;
import jpsxdec.uncompressors.UncompressionException;
import jpsxdec.util.IO;


public class NativeDecoder {
    
    private NativeDecoder() {}
    
    private static boolean LIBRARY_FOUND;
    
    static {
        
        try {
            // load the lib
            System.loadLibrary("nativedecode");
            // try to call the function with invalid values
            int iErr = NativeDecode(null, -1, false, -1, -1, -1, 0, 0, null);
            LIBRARY_FOUND = true;
        } catch (Exception ex) {
            LIBRARY_FOUND = false;
        }
    }
    
    public final static boolean hasNativeDecoder() {
        return LIBRARY_FOUND;
    }
    
    private final static int OK                           = 0;
    private final static int OK_NOT_END_OF_BLOCK          = 1;
    private final static int OK_END_OF_BLOCK              = 2;
    private final static int ERR_RUN_LENGTH_OUT_OF_BOUNDS = -10;
    private final static int ERR_UNKNOWN_AC_VLC           = -20;
    private final static int ERR_AC_ESCAPE_IS_ZERO        = -30;
    private final static int ERR_UNKNOWN_V3_DC_CHROM_VLC  = -40;
    private final static int ERR_UNKNOWN_V3_DC_LUMIN_VLC  = -50;   
    
    private static native int NativeDecode(byte[] abDemux, 
            int iInitialOffset, boolean blnLittleEndian,
            int iActualWidth, int iActualHeight, int iFrameType,
            int iQuantizationScaleChrom, int iQuantizationScaleLumin,
            int[] aiRGBout);
    
    public static BufferedImage Massive(StrFramePushDemuxer oDemux) throws IOException {
        // 1. Read the entire demuxed frame into an array
        byte[] abDemux = IO.readByteArray(oDemux.getStream(), (int)oDemux.getDemuxFrameSize());
        
        // 2. Identify the type
        int iFrameType = IdentifyFrameBE(abDemux, oDemux.getFrameNumber());
        
        // 3. Wrap it with a bit reader
        ArrayBitReader m_oBitReader = new ArrayBitReader(abDemux);
        
        // 4. Read frame header
        
        // Frame info
        long lngNumberOfRunLenthCodes;
        long lngHeader3800;
        long lngQuantizationScaleChrom;
        long lngQuantizationScaleLumin;
        long lngVersion;
        int iInitialOffset = 0;
        boolean blnLittleEndian = true;

        switch (iFrameType) {
            case StrFrameUncompressor.FRAME_FF7:
                // some FF7 videos have 40 bytes of camera data at the start of the frame
                m_oBitReader.SkipBits(40*8);
                iInitialOffset = 40;
            case StrFrameUncompressor.FRAME_FF7_WITHOUT_CAMERA:
            case StrFrameUncompressor.FRAME_VER2: 
            case StrFrameUncompressor.FRAME_VER3:
                lngNumberOfRunLenthCodes = m_oBitReader.ReadUnsignedBits(16);
                lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                lngQuantizationScaleChrom = 
                lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(16);
                lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                iInitialOffset += 4;
                break;
                
            case StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE:
            case StrFrameUncompressor.FRAME_LAIN:
                // because it's set to little-endian right now, 
                // these 16 bits are reversed
                lngQuantizationScaleChrom = (int)m_oBitReader.ReadUnsignedBits(8);
                lngQuantizationScaleLumin = (int)m_oBitReader.ReadUnsignedBits(8);
                
                lngHeader3800 = m_oBitReader.ReadUnsignedBits(16);
                lngNumberOfRunLenthCodes = (int)m_oBitReader.ReadUnsignedBits(16);
                lngVersion = (int)m_oBitReader.ReadUnsignedBits(16);
                // Lain also uses an actual bit stream, so we want big-endian reads
                blnLittleEndian = false;
                iInitialOffset = 4;
                break;
                
            case StrFrameUncompressor.FRAME_LOGO_IKI:
                throw new CriticalUncompressException(
                        "This appears to be the infamous logo.iki. " +
                        "Sorry, but I have no idea how to decode this thing.");
            default:
                throw new CriticalUncompressException("Unknown frame type.");
        }
        
        // We only can handle version 0, 1, 2, or 3 so far
        if (lngVersion < 0 || lngVersion > 3)
            throw new CriticalUncompressException("We don't know how to handle version " 
                                   + lngVersion);

        // make sure we got a 0x3800 in the header
        if (lngHeader3800 != 0x3800 && iFrameType != StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE)
            throw new CriticalUncompressException("0x3800 not found in start of frame");
        
        // make sure the quantization scale isn't 0
        if (lngQuantizationScaleChrom == 0 || lngQuantizationScaleLumin == 0)
            throw new CriticalUncompressException("Quantization scale of 0");
        
        int iActualWidth =  (int)((oDemux.getWidth()  + 15) & ~15);
        int iActualHeight = (int)((oDemux.getHeight() + 15) & ~15);

        int[] aiRGB = new int[iActualWidth * iActualHeight];
        
        int iErr = NativeDecode(abDemux, iInitialOffset, blnLittleEndian,
                iActualWidth, iActualHeight, iFrameType, 
                (int)lngQuantizationScaleChrom, (int)lngQuantizationScaleLumin,
                aiRGB);
        
        Exception ex;
        switch (iErr) {
            case OK:      
                break;
            case ERR_RUN_LENGTH_OUT_OF_BOUNDS: 
                throw new UncompressionException("Run length code out of bounds.");
            case ERR_UNKNOWN_AC_VLC:           
                throw new UncompressionException("Unknown variable length code.");
            case ERR_AC_ESCAPE_IS_ZERO:        
                throw new UncompressionException("AC escape code coefficient is zero.");
            case ERR_UNKNOWN_V3_DC_CHROM_VLC:  
                throw new UncompressionException("Unknown v3 Chrominance DC.");
            case ERR_UNKNOWN_V3_DC_LUMIN_VLC:
                throw new UncompressionException("Unknown v3 Luminance DC.");
            default:
                throw new UncompressionException("Unknown native decoding error.");
        }
        
        // 7. And return it
        BufferedImage bi = new BufferedImage(iActualWidth, iActualHeight, BufferedImage.TYPE_INT_RGB);
        WritableRaster wr = bi.getRaster();
        wr.setDataElements(0, 0, iActualWidth, iActualHeight, aiRGB);
        
        return bi;
    }
    
    
    private static int IdentifyFrameBE(byte[] abHeaderBytes, long lngFrameNum) throws CriticalUncompressException {
        //TODO: make sure the array is long enough, or just catch indexoutofbounds
        // if 0x3800 found at the normal position
        if (abHeaderBytes[3] == 0x38 && abHeaderBytes[2] == 0x00)
        {
            // check the version at the normal position
            int iVersion = ((abHeaderBytes[7] & 0xFF) << 8) | (abHeaderBytes[6] & 0xFF);
            switch (iVersion) {
                case 0: return StrFrameUncompressor.FRAME_LAIN;
                // if for some reason it's an ff7 frame without the camera data
                case 1: return StrFrameUncompressor.FRAME_FF7_WITHOUT_CAMERA; 
                case 2: return StrFrameUncompressor.FRAME_VER2;
                case 3: return StrFrameUncompressor.FRAME_VER3;
                case 256:
                    if ((((abHeaderBytes[4] & 0xFF) << 8) | (abHeaderBytes[5] & 0xFF)) == 320)
                        return StrFrameUncompressor.FRAME_LOGO_IKI;
                    else
                        throw new CriticalUncompressException("Unknown frame version " + iVersion);
                default:
                    throw new CriticalUncompressException("Unknown frame version " + iVersion);
            }
        } 
        // if 0x3800 is found 40 bytes from where it should be, and the
        // relative frame version is 1
        else if (abHeaderBytes[40+3] == 0x38 && abHeaderBytes[40+2] == 0x00 &&
                 abHeaderBytes[40+6] == 1) 
        {
            // it's an ff7 header
            return StrFrameUncompressor.FRAME_FF7;
        } 
        else // what else could it be?
        {
            // if the supposed 'version' is 0
            if (abHeaderBytes[7] == 0) {
                // and the supposed 0x3800 bytes...
                int iFrameNum = (((abHeaderBytes[3] & 0xFF) << 8) | (abHeaderBytes[2] & 0xFF));
                // ...happend to equal the frame number (if available)
                if (lngFrameNum >= 0) {
                    if (lngFrameNum == iFrameNum) {
                        // definitely lain final movie
                        return StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE;
                    }
                // .. or are at least less-than the number
                //    of frames in the final Lain movie
                } else if (iFrameNum >= 1 && iFrameNum <= 4765){
                    // probably lain final movie
                    return StrFrameUncompressor.FRAME_LAIN_FINAL_MOVIE;
                } else {
                    throw new CriticalUncompressException("0x3800 not found in start of frame");
                }
            } else {
                throw new CriticalUncompressException("0x3800 not found in start of frame");
            }
        }
        throw new CriticalUncompressException("Unknown frame type");
    }
    
}
