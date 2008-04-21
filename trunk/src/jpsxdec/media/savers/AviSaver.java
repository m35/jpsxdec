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

package jpsxdec.media.savers;

import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.media.StopPlayingException;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.util.Fraction;
import jpsxdec.util.aviwriter.AviWriter;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves audio/video to AVI. */
public class AviSaver extends AbstractSaver
        implements PSXMediaStreaming.IDemuxListener, PSXMediaStreaming.IAudListener 
{

    private AviWriter m_oAviWriter;
    private PSXMediaStreaming m_oMedia;
    private IOException m_oFailure;
    private long m_lngEndFrame;
    private boolean m_blnUseMjpg;
    private boolean m_blnDoNotCrop;
    
    private Decoders.DemuxToRgb oDecoder = Decoders.MakeDemuxToRgb(null);

    public IOException getException() {
        return m_oFailure;
    }
    
    public AviSaver(SavingOptions oOptions) 
            throws IOException 
    {
        super(oOptions.getMedia());
        
        m_oMedia = oOptions.getMedia();
        m_blnUseMjpg = oOptions.getVideoFormat() == Formats.MJPG_AVI;
        m_blnDoNotCrop = oOptions.getDoNotCrop();
        
        int iChannels = m_oMedia.getAudioChannels();
        if (!oOptions.getDecodeAudio() ||
            !(oOptions.getAudioFormat() instanceof Formats.AviAudFormat))
            iChannels = 0;
        
        File oFile = new File(oOptions.getFolder(),
                oOptions.getVidFilenameBase() + oOptions.getVidFilenameExt());
        
        if (!m_blnUseMjpg) {
            m_oAviWriter = new AviWriter(
                    oFile, 
                    iChannels);
        } else {
            if (oOptions.getUseDefaultJpegQuality())
                m_oAviWriter = new AviWriter(
                        oFile, 
                        iChannels,
                        true);
            else
                m_oAviWriter = new AviWriter(
                        oFile, 
                        iChannels,
                        oOptions.getJpegQuality());
        }
        
        Fraction fps = oOptions.getFps();
        
        m_oAviWriter.setFramesPerSecond((int)fps.numerator(), 
                                        (int)fps.denominator());
        
        if (m_blnDoNotCrop) {
            // TODO: Add do not crop option
            m_oAviWriter.setDimensions((int)m_oMedia.getActualWidth(), (int)m_oMedia.getActualHeight());
        } else {
            m_oAviWriter.setDimensions((int)m_oMedia.getActualWidth(), (int)m_oMedia.getActualHeight());
        }
        
        if (iChannels > 0)
            m_oAviWriter.setSamplesPerSecond(m_oMedia.getSamplesPerSecond());

        m_lngEndFrame = oOptions.getEndFrame();
        
        m_oMedia.addVidDemuxListener(this);
        if (iChannels > 0) m_oMedia.addAudioListener(this);
        
    }
    
    public void event(StrFramePushDemuxer oDemux) throws StopPlayingException, IOException {
        long frame = oDemux.getFrameNumber();
        
        if (m_lngEndFrame >= 0 && frame > m_lngEndFrame) throw new StopPlayingException();
        
        fireProgressUpdate("Reading frame " + frame, 
                    m_oMedia.getStartFrame(),
                    m_oMedia.getEndFrame(),
                    frame);

        Exception[] e = new Exception[1];
        if (m_blnDoNotCrop)
            // TODO: add crop option
            m_oAviWriter.writeFrame(oDecoder.UncompressDecodeRgb(oDemux, e));
        else
            m_oAviWriter.writeFrame(oDecoder.UncompressDecodeRgb(oDemux, e));
        
        if (e[0] != null)
            error(e[0]);
        
        if (m_lngEndFrame >= 0 && frame >= m_lngEndFrame) throw new StopPlayingException();
    }

    /** [implements IAudioListener] */
    public void event(AudioInputStream is, int sector) throws StopPlayingException, IOException {
        fireProgressUpdate("Reading audio sector " + sector, 
                m_oMedia.getStartSector(), 
                m_oMedia.getEndSector(), 
                sector);
        m_oAviWriter.writeAudio(is);
    }    
    
    public void done() throws IOException {
        m_oMedia.clearListeners();
        m_oAviWriter.close();
    }

}
