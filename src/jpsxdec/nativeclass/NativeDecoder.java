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
 * NativeDecoder.java
 */

package jpsxdec.nativeclass;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.videodecoding.CriticalUncompressException;
import jpsxdec.videodecoding.StrFrameHeader;
import jpsxdec.videodecoding.UncompressionException;
import jpsxdec.util.IO;


public class NativeDecoder {
    
    public static int DebugVerbose = 2; // TODO: Change this before release
    
    private NativeDecoder() {}
    
    private static boolean LIBRARY_FOUND;
    
    static {
        // TODO: Clean this mess up
        // http://scv.bu.edu/Doc/Java/tutorial/java/system/properties.html
        try {
            // ensure the '.' path is in the list of paths to search
            String sSeparator = System.getProperty("path.separator");
            String sPaths = System.getProperty("java.library.path");
            
            System.setProperty("java.library.path", "." + sSeparator + sPaths);
            
            if (DebugVerbose > 2) {
                System.err.println(System.getProperty("java.library.path"));
            }
            
            // load the lib
            System.loadLibrary("nativedecode");
            
            // try to call the function with invalid values just to see if it works
            int iErr = NativeDecode(null, -1, false, -1, -1, -1, 0, 0, null);
            LIBRARY_FOUND = true;
        } catch (Error err1) {
            
            try {
                String sPath = System.getProperty("user.dir");
                sPath = new File(sPath, "libnativedecode.so").getAbsolutePath();

                System.err.println("Trying absolute path: " + sPath);
            
                System.load(sPath);
                // try to call the function with invalid values just to see if it works
                int iErr = NativeDecode(null, -1, false, -1, -1, -1, 0, 0, null);
                LIBRARY_FOUND = true;
            } catch (Error err2) {
                LIBRARY_FOUND = false;
            }
        }
        
        if (DebugVerbose > 2) {
            System.err.println("Library found: " + LIBRARY_FOUND);
        }
        
    }
    
    public final static boolean hasNativeDecoder() {
        return LIBRARY_FOUND;
    }
    
    private final static int ERR_PRIMITIVEARRAYCRITICAL_FAILED = -1;
    private final static int ERR_INVALID_ARGUMENT = -2;
    
    private final static int OK                           = 0;
    private final static int OK_NOT_END_OF_BLOCK          = 1;
    private final static int OK_END_OF_BLOCK              = 2;
    private final static int ERR_RUN_LENGTH_OUT_OF_BOUNDS = -10;
    private final static int ERR_UNKNOWN_AC_VLC           = -20;
    private final static int ERR_AC_ESCAPE_IS_ZERO        = -30;
    private final static int ERR_UNKNOWN_V3_DC_CHROM_VLC  = -40;
    private final static int ERR_UNKNOWN_V3_DC_LUMIN_VLC  = -50;   
    private final static int ERR_EOF                      = -105;
    
    private static native int NativeDecode(byte[] abDemux, 
            int iInitialOffset, boolean blnLittleEndian,
            int iActualWidth, int iActualHeight, int iFrameType,
            int iQuantizationScaleChrom, int iQuantizationScaleLumin,
            int[] aiRGBout);
    
    public static BufferedImage DecodeCrazyFast(StrFramePushDemuxer oDemux) throws IOException, CriticalUncompressException {
        
        if (!LIBRARY_FOUND) throw new IllegalStateException("Native decoder library is not available/working.");
        
        // 1. Read the entire demuxed frame into an array
        byte[] abDemux = IO.readByteArray(oDemux.getStream(), (int)oDemux.getDemuxFrameSize());
        
        // 2. Identify the type
        // 3. Read frame header
        StrFrameHeader oFrameHeader = new StrFrameHeader(abDemux, oDemux.getFrameNumber());
        
        if (DebugVerbose >= 3)
            oFrameHeader.printInfo(System.err);
        
        int iActualWidth =  (int)((oDemux.getWidth()  + 15) & ~15);
        int iActualHeight = (int)((oDemux.getHeight() + 15) & ~15);

        int[] aiRGB = new int[iActualWidth * iActualHeight];
        
        int iErr = NativeDecode(abDemux, 
                oFrameHeader.DataStart, oFrameHeader.LittleEndian,
                iActualWidth, iActualHeight, oFrameHeader.FrameType, 
                (int)oFrameHeader.QuantizationScaleChrom, 
                (int)oFrameHeader.QuantizationScaleLumin,
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
            case ERR_EOF:
                throw new EOFException("Unexpected end of bit stream.");
            case ERR_PRIMITIVEARRAYCRITICAL_FAILED:
                throw new RuntimeException("Call to native function GetPrimitiveArrayCritical() failed.");
            case ERR_INVALID_ARGUMENT:
                throw new IllegalArgumentException("Illegal argument passed to native decoder.");
            default:
                throw new UncompressionException("Unknown native decoding error " + iErr + ".");
        }
        
        // 7. And return it
        BufferedImage bi = new BufferedImage(iActualWidth, iActualHeight, BufferedImage.TYPE_INT_RGB);
        WritableRaster wr = bi.getRaster();
        wr.setDataElements(0, 0, iActualWidth, iActualHeight, aiRGB);
        
        return bi;
    }
    
}
