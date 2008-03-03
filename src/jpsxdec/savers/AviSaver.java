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
 * AviSaver.java
 */

package jpsxdec.savers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.PSXMediaVideo;
import jpsxdec.media.StrFpsCalc;
import jpsxdec.util.aviwriter.AVIWriter;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves audio/video to AVI. */
public class AviSaver extends AbstractSaver
        implements PSXMediaVideo.IFrameListener, PSXMediaVideo.IAudListener 
{

    private AVIWriter m_oAviWriter;
    private PSXMediaVideo m_oMedia;
    private IOException m_oFailure;
    private long m_lngEndFrame;

    public IOException getException() {
        return m_oFailure;
    }
    
    public AviSaver(PSXMediaVideo oMedia, 
                    String sOutputFile, 
                    boolean blnUseMpeg, 
                    long lngEndFrame,
                    boolean blnDecodeAudio) 
            throws IOException 
    {
        if (!oMedia.hasVideo())
            throw new IllegalArgumentException("The media really should have video when saving as AVI");
        
        int iChannels = oMedia.getAudioChannels();
        if (!blnDecodeAudio) iChannels = 0;
        
        m_oAviWriter = new AVIWriter(
                new File(sOutputFile), 
                blnUseMpeg, iChannels);
        
        StrFpsCalc.FramesPerSecond oFps[] = oMedia.getPossibleFPS();
        m_oAviWriter.setFramesPerSecond((int)oFps[0].numerator(), 
                                        (int)oFps[0].denominator());
        
        oMedia.addFrameListener(this);
        if (iChannels > 0 && blnDecodeAudio) oMedia.addAudioListener(this);
        
        m_oMedia = oMedia;
        m_lngEndFrame = lngEndFrame;
    }
    
    /** [implements IFrameListener] */
    public boolean event(PsxYuv o, long frame) {
        try {
            if (m_lngEndFrame != -1 && frame > m_lngEndFrame) return true;
            fireProgressUpdate("Reading frame " + frame, 
                        m_oMedia.getStartFrame(),
                        m_oMedia.getEndFrame(),
                        frame);
            m_oAviWriter.writeFrame(o.toBufferedImage(BufferedImage.TYPE_3BYTE_BGR));
            if (m_lngEndFrame != -1 && frame >= m_lngEndFrame) return true;
            return false;
        } catch (IOException ex) {
            m_oFailure = ex;
            fireProgressError(ex);
            return true;
        }
    }
    /** [implements IAudioListener] */
    public boolean event(AudioInputStream is, int sector) {
        boolean ret;
        try {
            fireProgressUpdate("Reading audio sector " + sector, 
                    m_oMedia.getStartSector(), 
                    m_oMedia.getEndSector(), 
                    sector);
            m_oAviWriter.writeAudio(is);
            ret = false;
        } catch (IOException ex) {
            m_oFailure = ex;
            fireProgressError(ex);
            ret = true;
        }
        
        return ret;
    }

    public void done() throws IOException {
        m_oMedia.clearListeners();
        m_oAviWriter.close();
    }

}
