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
 * ImageSequenceSaver.java
 */

package jpsxdec.savers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.IVidListener;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.PSXMediaVideo;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.PSXMediaVideo.IFrameListener;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves a series of images, 
 *  demux, mdec files. */
public class ImageSequenceSaver extends AudioSaver implements IVidListener, IFrameListener {

    private String m_sOutputFile;
    private String m_sOutputFormat;
    private long m_lngEndFrame;
    
    public ImageSequenceSaver(
            PSXMediaVideo oMedia, 
            String sOutputFile, 
            String sOutputFormat, 
            long lngEndFrame,
            boolean blnDecodeAudio) 
    {
        super(oMedia, sOutputFile, blnDecodeAudio);

        m_sOutputFile = sOutputFile;
        m_sOutputFormat = sOutputFormat;
        m_lngEndFrame = lngEndFrame;
        if (sOutputFormat.equals("demux"))
            oMedia.addVidDemuxListener(this);
        else if (sOutputFormat.equals("mdec"))
            oMedia.addMdecListener(this);
        else
            oMedia.addFrameListener(this);
    }

    private String FileName(long frame) {
        return String.format(
                "%s_f%04d.%s",
                m_sOutputFile, frame, m_sOutputFormat);
    }
    
    public boolean event(InputStream is, long frame) {
        try {
            if (m_lngEndFrame != -1 && frame > m_lngEndFrame) return true;
            if ( fireProgressUpdate("Reading frame " + frame, 
                        m_oMedia.getStartFrame(),
                        m_oMedia.getEndFrame(),
                        frame) )
                return true;
            IO.writeIStoFile(is, FileName(frame));
            if (m_lngEndFrame != -1 && frame >= m_lngEndFrame) return true;
            return false;
        } catch (IOException ex) {
            super.m_oFailure = ex;
            fireProgressError(ex);
            return true;
        }
        
    }

    public boolean event(PsxYuv yuv, long frame) {
        try {
            if (m_lngEndFrame != -1 && frame > m_lngEndFrame)
                return true;
            if ( fireProgressUpdate("Reading frame " + frame, 
                        super.m_oMedia.getStartFrame(),
                        super.m_oMedia.getEndFrame(),
                        frame) )
                return true;
            if (m_sOutputFormat.equals("yuv") || m_sOutputFormat.equals("y4m")) {
                yuv.Write(FileName(frame));
            } else {
                ImageIO.write(yuv.toBufferedImage(), m_sOutputFormat, new File(FileName(frame)));
            }
            if (m_lngEndFrame != -1 && frame >= m_lngEndFrame)
                return true;
            return false;
        } catch (IOException ex) {
            super.m_oFailure = ex;
            fireProgressError(ex);
            return true;
        }
    }

}
