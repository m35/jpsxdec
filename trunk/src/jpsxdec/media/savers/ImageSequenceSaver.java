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

package jpsxdec.media.savers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.PSXMediaStreaming.IFrameListener;
import jpsxdec.media.PSXMediaStreaming.IVidListener;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves a series of images, 
 *  demux, mdec files. */
public class ImageSequenceSaver extends AbstractSaver 
        implements IVidListener, IFrameListener 
{

    private final PSXMediaStreaming m_oMedia;
    private final File m_oFolder;
    private final String m_sFileVidBase;
    private final String m_sOutputFormat;
    private final Long m_olngEndFrame;
    private Fraction m_oFps;
    private boolean m_blnUseDefaultJpeg;
    private float m_fltJpegQuality;
    
    private String m_sFileVidExt;
    
    public ImageSequenceSaver(SavingOptions oOptions) {
        super(oOptions.getMedia());
        
        m_oMedia = oOptions.getMedia();
        m_oFolder = oOptions.getFolder();
        m_sFileVidBase = oOptions.getVideoFilenameBase();
        m_sOutputFormat = oOptions.getVideoFormat();
        m_sFileVidExt = oOptions.getVideoFilenameExt();
        
        m_olngEndFrame = oOptions.getEndFrame();
            
        if (m_sOutputFormat.equals("demux"))
            m_oMedia.addVidDemuxListener(this);
        else if (m_sOutputFormat.equals("mdec"))
            m_oMedia.addMdecListener(this);
        else {
            if (m_sOutputFormat.equals("yuv")) {
                m_oFps = oOptions.getFramesPerSecond();
            }
            if (m_sOutputFormat.equals("jpeg")) {
                m_blnUseDefaultJpeg = oOptions.useDefaultJpegQuality();
                if (!m_blnUseDefaultJpeg)
                    m_fltJpegQuality = oOptions.getJpegQuality();
            }
            m_oMedia.addFrameListener(this);
        }
    }

    @Override
    public void done() throws IOException {     
        m_oMedia.clearListeners();  //FIXME: This will clear other listeners
    }

    private File FileName(long frame) {
        return new File(m_oFolder, String.format( "%s%04d.%s",
                m_sFileVidBase, frame, m_sFileVidExt));
    }
    
    public void event(InputStream is, long frame) throws StopPlayingException, IOException {
            if (m_olngEndFrame != null && frame > m_olngEndFrame) new StopPlayingException();
            fireProgressUpdate("Reading frame " + frame, 
                        m_oMedia.getStartFrame(),
                        m_oMedia.getEndFrame(),
                        frame);
            
        IO.writeIStoFile(is, FileName(frame));
        if (m_olngEndFrame != null && frame >= m_olngEndFrame) new StopPlayingException();
    }

    public void event(PsxYuv yuv, long frame) throws StopPlayingException, IOException {
        if (m_olngEndFrame != null && frame > m_olngEndFrame) new StopPlayingException();
            
        fireProgressUpdate("Reading frame " + frame, 
                    m_oMedia.getStartFrame(),
                    m_oMedia.getEndFrame(),
                    frame);

        File oFile = FileName(frame);
            
        if (m_sOutputFormat.equals("yuv")) {
            yuv.Write(oFile, m_oFps);
        } else if (m_sOutputFormat.equals("jpeg") && !m_blnUseDefaultJpeg) {
            // TODO: write jpeg with quality
            ImageIO.write(yuv.toBufferedImage(), m_sOutputFormat, oFile);
        } else {
            // write default jpeg quality, or other format
            boolean ok = ImageIO.write(yuv.toBufferedImage(), m_sOutputFormat, oFile);
            if (!ok) throw new IOException("Unable to write frames as " + m_sOutputFormat + " file format.");
        }
            
        if (m_olngEndFrame != null && frame >= m_olngEndFrame) new StopPlayingException();
            
    }

}
