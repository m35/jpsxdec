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
 * VideoFrameDecoder.java
 */

package jpsxdec.media;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.NoSuchElementException;
import javax.imageio.ImageIO;
import jpsxdec.*;
import jpsxdec.mdec.MDEC;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.mdec.Yuv4mpeg2;
import jpsxdec.demuxers.StrFrameDemuxerIS;
import jpsxdec.uncompressors.StrFrameUncompressorIS;

/** Tries to make frame-by-frame decoding a little easier. Currently not used
 *  except for the static functions. */
public class VideoFrameConverter {
    
    public static void DecodeAndSaveFrame(String sInputFormat, 
                                          String sOutputFormat,
                                          InputStream str, 
                                          String sFrameFile,
                                          long lngWidth,
                                          long lngHeight) 
            throws IOException 
    {
        
        if (str instanceof StrFrameDemuxerIS) {
            lngWidth = ((StrFrameDemuxerIS)str).getWidth();
            lngHeight = ((StrFrameDemuxerIS)str).getHeight();
        } else if (str instanceof StrFrameUncompressorIS) {
            lngWidth = ((StrFrameUncompressorIS)str).getWidth();
            lngHeight = ((StrFrameUncompressorIS)str).getHeight();
        }
            
        
        if (sInputFormat.equals("demux")) {

            if (sOutputFormat.equals("demux")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

            str = new StrFrameUncompressorIS(str, lngWidth, lngHeight);
            if (sOutputFormat.equals("0rlc")) {
                FileOutputStream fos = new FileOutputStream(sFrameFile);
                int ib;
                while ((ib = str.read()) >= 0)
                    fos.write(ib);
                fos.close();
                return;
            }

        }
        
        Yuv4mpeg2 oYuv = MDEC.DecodeFrame(str, lngWidth, lngHeight);
        if (sOutputFormat.equals("yuv") || sOutputFormat.equals("y4m")) {
            FileOutputStream fos = new FileOutputStream(sFrameFile);
            oYuv.Write(fos);
            fos.close();
            return;
        }

        BufferedImage bi = oYuv.toBufferedImage();
        ImageIO.write(bi, sOutputFormat, new File(sFrameFile));
    }
    
    
    public static long ClampStartFrame(long lngRequestedStart, long lngActualStart, long lngActualEnd) {
        
        if (lngRequestedStart == -1) return lngActualStart;
        if (lngRequestedStart <= lngActualStart) return lngActualStart;
        if (lngRequestedStart >= lngActualEnd) return lngActualEnd;
        return lngRequestedStart;
    }
        
    public static long ClampEndFrame(long lngRequestedEnd, long lngActualStart, long lngActualEnd) {
        
        if (lngRequestedEnd == -1) return lngActualEnd;
        if (lngRequestedEnd <= lngActualStart) return lngActualStart;
        if (lngRequestedEnd >= lngActualEnd) return lngActualEnd;
        return lngRequestedEnd;
    }
    
    // #########################################################################
    
    public static interface IVideoMedia {
        long GetStartFrame();
        long GetEndFrame();
        jpsxdec.sectortypes.PSXSectorRangeIterator GetSectorIterator();
    }
    
    // #########################################################################
    
    private String m_sImportFormat = "demux";
    private String m_sOutputFormat = "png";
    
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    private long m_lngFirstFrame;
    private long m_lngLastFrame;
    
    private long m_lngStartFrame;
    private long m_lngEndFrame;
    private long m_lngCurFrame;
    
    private PSXSectorRangeIterator m_oIter;
    private String m_lngBaseFileName;
    
    private int m_iSaveIndex1;
    private int m_iSaveIndex2;
        
    public VideoFrameConverter(IVideoMedia oMedia) {
        m_oIter = oMedia.GetSectorIterator();
        m_lngFirstFrame = oMedia.GetStartFrame();
        m_lngLastFrame = oMedia.GetEndFrame();
        m_lngCurFrame = m_lngFirstFrame;
        
        m_iSaveIndex1 = m_oIter.getIndex();
        m_iSaveIndex2 = m_iSaveIndex1;
    }
    
    
    public void DecodeNext() throws IOException {
        if (m_lngCurFrame >= m_lngLastFrame) throw new NoSuchElementException();
        
        String sFrameFile = String.format(
                    "%s%03d_f%04d.%s",
                    m_lngBaseFileName);

        m_oIter.gotoIndex(m_iSaveIndex1);
        StrFrameDemuxerIS str = new StrFrameDemuxerIS(m_oIter, m_lngCurFrame);

        DecodeAndSaveFrame("demux", 
                           m_sOutputFormat,
                           str, sFrameFile, m_lngWidth, m_lngHeight);

        // if the iterator was searched to the end, we don't
        // want to save the end sector (or we'll miss sectors
        // before it). This can happen when saving as demux because 
        // it has to search to the end of the stream
        if (m_oIter.hasNext()) {
            m_iSaveIndex1 = m_iSaveIndex2;
            m_iSaveIndex2 = m_oIter.getIndex();
        }

    }
    
    public boolean HasNext() {
        return m_lngCurFrame < m_lngLastFrame;
    }
    
    public long NextFrame() {
        return m_lngCurFrame + 1;
    }
    
    public void GotoFrame(long lngFrame) {
        if (lngFrame < m_lngFirstFrame)
            m_lngCurFrame = m_lngFirstFrame;
        else if (lngFrame > m_lngLastFrame)
            m_lngCurFrame = m_lngLastFrame;
        else
            m_lngCurFrame = lngFrame;
    }
    
    /**************************************************************************/
    /**************************************************************************/

    public String getImportFormat() {
        return m_sImportFormat;
    }

    public void setImportFormat(String sImportFormat) {
        m_sImportFormat = sImportFormat;
    }

    public String getOutputFormat() {
        return m_sOutputFormat;
    }

    public void setOutputFormat(String sOutputFormat) {
        m_sOutputFormat = sOutputFormat;
    }

    public long getWidth() {
        return m_lngWidth;
    }

    public void setWidth(long lngWidth) {
        m_lngWidth = lngWidth;
    }

    public long getHeight() {
        return m_lngHeight;
    }

    public void setHeight(long lngHeight) {
        m_lngHeight = lngHeight;
    }

    public long getFrame() {
        return m_lngCurFrame;
    }

    public String getBaseFileName() {
        return m_lngBaseFileName;
    }

    public void setBaseFileName(String sBaseFileName) {
        m_lngBaseFileName = sBaseFileName;
    }
}
