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
 * Decoders.java
 */

package jpsxdec.media.savers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.uncompressors.StrFrameRecompressor;
import jpsxdec.uncompressors.StrFrameUncompressor;


public class Decoders {

    public static interface DemuxToRgb {
        BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, Exception[] exout) throws IOException;
    }
    
    public static interface DemuxToYuv {
        PsxYuv UncompressDecode(StrFramePushDemuxer oDemux, Exception[] exout);
    }
    
    public static interface DemuxToUncompress {
        InputStream Uncompress(StrFramePushDemuxer oDemux, Exception[] exout) throws IOException;
    }
    
    public static interface UncompressToYuv {
        PsxYuv Decode(InputStream oUncompress, Exception[] exout);
    }
    
    public static interface UncompressToRgb {
        BufferedImage Decode(InputStream oUncompress, Exception[] exout);
    }
    
    //--------------------------------------------------------------------------
    
    public static DemuxToRgb MakeDemuxToRgb(Class c1) {
        return new DemuxToRgb() {
            public BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, Exception[] exout) throws IOException {
                StrFrameUncompressor oUncompress = new StrFrameUncompressor(
                        oDemux.getStream(), oDemux.getWidth(), oDemux.getHeight());
                
                PsxYuv yuv = MDEC.getQualityMdec().DecodeFrame(
                        oUncompress.getStream(), 
                        oUncompress.getWidth(), 
                        oUncompress.getHeight());
                
                exout[0] = yuv.getDecodingError();
                
                return yuv.toBufferedImage();
            }
        };
            
    }
    
    public static DemuxToUncompress MakeDemuxToUncompress() {
        return new DemuxToUncompress() {
            public InputStream Uncompress(StrFramePushDemuxer oDemux, Exception[] exout) throws IOException {
                StrFrameUncompressor oUncompress = new StrFrameRecompressor(
                        oDemux.getStream(), oDemux.getWidth(), oDemux.getHeight());
                return oUncompress.getStream();
            }
        };
    }
}
