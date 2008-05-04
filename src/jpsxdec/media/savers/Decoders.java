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
 * Decoders.java
 */

package jpsxdec.media.savers;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.IProgressListener;
import jpsxdec.media.IProgressListener.IProgressErrorListener;
import jpsxdec.nativeclass.NativeDecoder;
import jpsxdec.videodecoding.CriticalUncompressException;
import jpsxdec.videodecoding.StrFrameDecoderFast;
import jpsxdec.videodecoding.StrFrameUncompressor;


public class Decoders {
    
    // 11 11
    public static final int DEBUG_UNCOMPRESSOR = 0<<0;
    public static final int DEBUG_MDEC         = 0<<2;
    public static final int DEBUG_DECODER = DEBUG_UNCOMPRESSOR | DEBUG_MDEC;
    
    public static final int JAVA_FAST_UNCOMPRESSOR = 1<<0;
    public static final int JAVA_FAST_MDEC         = 1<<2;
    public static final int JAVA_FAST_DECODER = JAVA_FAST_UNCOMPRESSOR | JAVA_FAST_MDEC;

    public static final int NATIVE_DECODER         = 2<<0 | 2<<2;
    
    public static interface DemuxToRgb {
        BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException;
    }
    
    public static interface DemuxToYuv {
        PsxYuv UncompressDecode(StrFramePushDemuxer oDemux, Exception[] exout);
    }
    
    public static interface DemuxToUncompress {
        InputStream Uncompress(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException;
    }
    
    public static interface UncompressToYuv {
        PsxYuv Decode(InputStream oUncompress, Exception[] exout);
    }
    
    public static interface UncompressToRgb {
        BufferedImage Decode(InputStream oUncompress, Exception[] exout);
    }
    
    //--------------------------------------------------------------------------
    
    public static DemuxToRgb MakeDemuxToRgb(int iDecoder) {
        if (iDecoder == NATIVE_DECODER && NativeDecoder.hasNativeDecoder())
            
        return new DemuxToRgb() {
            public BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException {
                try {
                    return NativeDecoder.DecodeCrazyFast(oDemux);
                } catch (CriticalUncompressException ex) {
                    if (oListener instanceof IProgressErrorListener)
                        ((IProgressErrorListener)oListener).ProgressUpdate(ex);
                    BufferedImage bi = new BufferedImage(
                                        (int)oDemux.getWidth(), 
                                        (int)oDemux.getHeight(), 
                                        BufferedImage.TYPE_INT_RGB);
                    Graphics g = bi.getGraphics();
                    g.drawString(ex.getMessage(), 5, 20);
                    g.dispose();
                    return bi;
                }
            }
        };
        
        else if (iDecoder == NATIVE_DECODER || iDecoder == JAVA_FAST_DECODER)
            
        return new DemuxToRgb() {
            StrFrameDecoderFast oDecoder = new StrFrameDecoderFast();
            public BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException {
                try {
                    return oDecoder.UncompressDecodeRgb(oDemux, oListener);
                } catch (CriticalUncompressException ex) {
                    if (oListener instanceof IProgressErrorListener)
                        ((IProgressErrorListener)oListener).ProgressUpdate(ex);
                    BufferedImage bi = new BufferedImage(
                                        (int)oDemux.getWidth(), 
                                        (int)oDemux.getHeight(), 
                                        BufferedImage.TYPE_INT_RGB);
                    Graphics g = bi.getGraphics();
                    g.drawString(ex.getMessage(), 5, 20);
                    g.dispose();
                    return bi;
                }
            }
        };
        
        else
            
        return new DemuxToRgb() {
            public BufferedImage UncompressDecodeRgb(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException {
                try {
                    StrFrameUncompressor oUncompress = new StrFrameUncompressor(oDemux.getStream(), oDemux.getWidth(), oDemux.getHeight());

                    PsxYuv yuv = MDEC.getQualityMdec().DecodeFrame(oUncompress.getStream(), oUncompress.getWidth(), oUncompress.getHeight());

                    if (yuv.getDecodingError() != null && oListener instanceof IProgressErrorListener)
                        ((IProgressErrorListener)oListener).ProgressUpdate(yuv.getDecodingError());

                    return yuv.toBufferedImage();
                } catch (CriticalUncompressException ex) {
                    if (oListener instanceof IProgressErrorListener)
                        ((IProgressErrorListener)oListener).ProgressUpdate(ex);
                    BufferedImage bi = new BufferedImage(
                                        (int)oDemux.getWidth(), 
                                        (int)oDemux.getHeight(), 
                                        BufferedImage.TYPE_INT_RGB);
                    Graphics g = bi.getGraphics();
                    g.drawString(ex.getMessage(), 5, 20);
                    g.dispose();
                    return bi;
                }
            }
        };
    }
    
    public static DemuxToUncompress MakeDemuxToUncompress(int iDecoder) {
        return new DemuxToUncompress() {
            public InputStream Uncompress(StrFramePushDemuxer oDemux, IProgressListener oListener) throws IOException {
                try {
                    StrFrameUncompressor oUncompress = new StrFrameUncompressor(oDemux.getStream(), oDemux.getWidth(), oDemux.getHeight());
                    return oUncompress.getStream();
                } catch (CriticalUncompressException ex) {
                    if (oListener instanceof IProgressErrorListener)
                        ((IProgressErrorListener)oListener).ProgressUpdate(ex);
                    return null;
                }
            }
        };
    }
}
