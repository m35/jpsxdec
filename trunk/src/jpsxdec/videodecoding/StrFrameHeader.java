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
 * StrFrameHeader.java
 */

package jpsxdec.videodecoding;

import java.io.PrintStream;
import jpsxdec.util.IO;

/** Parses the header of a frame. Identifies the frame type and other important
 *  frame values. */
public class StrFrameHeader {
    
    public final static int FRAME_VER2 = 2;
    public final static int FRAME_VER3 = 3;
    public final static int FRAME_LAIN = 0;
    public final static int FRAME_LAIN_FINAL_MOVIE = 10;
    public final static int FRAME_FF7 = 1;
    public final static int FRAME_FF7_WITHOUT_CAMERA = 11;
    public final static int FRAME_LOGO_IKI = 256;
    
    public static String type2str(int iFrameType) {
        switch (iFrameType) {
            case FRAME_VER2               : return "FRAME_VER2";                            
            case FRAME_VER3               : return "FRAME_VER3";                            
            case FRAME_LAIN               : return "FRAME_LAIN";                            
            case FRAME_LAIN_FINAL_MOVIE   : return "FRAME_LAIN_FINAL_MOVIE";    
            case FRAME_FF7                : return "FRAME_FF7";                              
            case FRAME_FF7_WITHOUT_CAMERA : return "FRAME_FF7_WITHOUT_CAMERA";
            case FRAME_LOGO_IKI           : return "FRAME_LOGO_IKI";
            default : return "unknown frame";
        }
    }
    
    /** Often holes the count of run-length codes that are found in this
     *  frame. Important for the PSX so it can allocate enough memory. */
    public long NumberOfRunLenthCodes;
    public final long Header3800;
    /** The Chrominance quantization scale used throughout the frame. */
    public final long QuantizationScaleChrom;
    /** The Luminance quantization scale used throughout the frame. */
    public final long QuantizationScaleLumin;
    /** Most games use verion 2 or version 3. Currently handled exceptions
     *  are: FF7 and FF Tactics use version 1, Lain uses version 0 */
    public final long Version;

    
    
    public final int FrameType;
    
    public final int DataStart;
    
    public final boolean LittleEndian;
    
    /** In worst case, will need about 50 bytes of frame data 
     *  to identify the frame type.
     * @param abHeaderBytes The frame byte data in Big-Endian order.
     * @param lngFrameNum   The frame number, or &lt; 0 if unknown.
     */
    public StrFrameHeader(byte[] abHeaderBytes, long lngFrameNum) throws CriticalUncompressException {
        
        try {
        
            FrameType = IdentifyFrame(abHeaderBytes, lngFrameNum);

            int iOfs = 0;

            switch (FrameType) {
                case FRAME_FF7:
                    // some FF7 videos have 40 bytes of camera data at the start of the frame
                    iOfs = 40;
                case FRAME_FF7_WITHOUT_CAMERA:
                case FRAME_VER2: 
                case FRAME_VER3:
                    NumberOfRunLenthCodes = IO.ReadUInt16LE(abHeaderBytes, iOfs); 
                    iOfs+=2;
                    Header3800 = IO.ReadUInt16LE(abHeaderBytes, iOfs); 
                    iOfs+=2;
                    QuantizationScaleChrom = 
                    QuantizationScaleLumin = (int)IO.ReadUInt16LE(abHeaderBytes, iOfs); 
                    iOfs+=2;
                    Version = (int)IO.ReadUInt16LE(abHeaderBytes, iOfs); 
                    iOfs+=2;

                    // Normal bit streams use 16-bit little-endian values
                    LittleEndian = true;
                    break;

                case FRAME_LAIN_FINAL_MOVIE:
                case FRAME_LAIN:
                    QuantizationScaleLumin = (int)IO.ReadUInt8(abHeaderBytes, iOfs); 
                    iOfs+=1;
                    QuantizationScaleChrom = (int)IO.ReadUInt8(abHeaderBytes, iOfs); 
                    iOfs+=1;

                    Header3800 = IO.ReadUInt16LE(abHeaderBytes, iOfs);
                    iOfs+=2;
                    NumberOfRunLenthCodes = (int)IO.ReadUInt16LE(abHeaderBytes, iOfs);
                    iOfs+=2;
                    Version = (int)IO.ReadUInt16LE(abHeaderBytes, iOfs);
                    iOfs+=2;

                    // Lain also uses an actual bit stream, so we want big-endian reads
                    LittleEndian = false;
                    break;

                case FRAME_LOGO_IKI:
                    throw new CriticalUncompressException(
                            "This appears to be the infamous logo.iki. " +
                            "Sorry, but I have no idea how to decode this thing.");
                default:
                    throw new CriticalUncompressException("Unknown frame type.");
            }

            DataStart = iOfs;

            // We only can handle version 0, 1, 2, or 3 so far
            if (Version < 0 || Version > 3)
                throw new CriticalUncompressException("We don't know how to handle version " 
                                       + Version);

            // make sure we got a 0x3800 in the header
            if (Header3800 != 0x3800 && FrameType != FRAME_LAIN_FINAL_MOVIE)
                throw new CriticalUncompressException("0x3800 not found in start of frame");

            // make sure the quantization scale isn't 0
            if (QuantizationScaleChrom < 1 || QuantizationScaleLumin < 1)
                throw new CriticalUncompressException("Quantization scale of 0");
        
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new CriticalUncompressException("Not enough frame data to identify the frame type.");
        }
    }

    
    private static int IdentifyFrame(byte[] abHeaderBytes, long lngFrameNum) throws CriticalUncompressException {
        //TODO: make sure the array is long enough, or just catch indexoutofbounds
        // if 0x3800 found at the normal position
        if (IO.ReadUInt16LE(abHeaderBytes, 2) == 0x3800)
        {
            // check the version at the normal position
            int iVersion = (int)IO.ReadUInt16LE(abHeaderBytes, 6);
            switch (iVersion) {
                case 0: return FRAME_LAIN;
                case 1: return FRAME_FF7_WITHOUT_CAMERA; 
                case 2: return FRAME_VER2;
                case 3: return FRAME_VER3;
                case 256:
                    if (IO.ReadUInt16LE(abHeaderBytes, 4) == 320)
                        return FRAME_LOGO_IKI;
                    else
                        throw new CriticalUncompressException("Unknown frame version " + iVersion);
                default:
                    throw new CriticalUncompressException("Unknown frame version " + iVersion);
            }
        } 
        // if 0x3800 is found 40 bytes from where it should be, and the
        // relative frame version is 1
        else if (IO.ReadUInt16LE(abHeaderBytes, 40+2) == 0x3800 &&
                 IO.ReadUInt16LE(abHeaderBytes, 40+6) == 1) 
        {
            // it's an ff7 header
            return FRAME_FF7;
        } 
        else // what else could it be?
        {
            // if the supposed 'version' is 0
            if (IO.ReadUInt16LE(abHeaderBytes, 6) == 0) {
                // and the supposed 0x3800 bytes...
                int iLainFrameNum = (int)IO.ReadUInt16LE(abHeaderBytes, 2);
                // ...happend to equal the frame number (if available)
                if (lngFrameNum >= 0) {
                    if (lngFrameNum == iLainFrameNum) {
                        // definitely lain final movie
                        return FRAME_LAIN_FINAL_MOVIE;
                    }
                // .. or are at least less-than the number
                //    of frames in the final Lain movie
                } else if (iLainFrameNum >= 1 && iLainFrameNum <= 4765){
                    // probably lain final movie
                    return FRAME_LAIN_FINAL_MOVIE;
                } else {
                    throw new CriticalUncompressException("0x3800 not found in start of frame");
                }
            } else {
                throw new CriticalUncompressException("0x3800 not found in start of frame");
            }
        }
        throw new CriticalUncompressException("Unknown frame type");
    }
    
    public void printInfo(PrintStream ps) {
        // provide some debug about what was found
        ps.println("Frame Header: type " + type2str(FrameType));
        ps.println(String.format("  %04x -> Run Lenth Code Count (?) %d", 
                NumberOfRunLenthCodes, 
                NumberOfRunLenthCodes));
        ps.println(String.format("  %04x", 
                Header3800));
        if (FrameType == FRAME_LAIN || FrameType == FRAME_LAIN_FINAL_MOVIE)
            ps.println(String.format("  %04x -> Quantization scale C %d Y %d", 
                    (QuantizationScaleChrom << 8) | QuantizationScaleLumin, 
                    QuantizationScaleChrom,
                    QuantizationScaleLumin));
        else
            ps.println(String.format("  %04x -> Quantization scale %d", 
                    QuantizationScaleChrom,
                    QuantizationScaleChrom));
        ps.println(String.format("  %04x -> Version %d", 
                Version,
                Version));
    }
    
}
