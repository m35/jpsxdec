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
 * XASaver.java
 */

package jpsxdec.savers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.media.PSXMedia;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.IAudListener;
import jpsxdec.media.PSXMediaXA;
import jpsxdec.util.AudioWriter;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves audio channel(s) 
 *  from XA media to wav files. */
public class XASaver extends AbstractSaver {

    private PSXMediaXA m_oMedia;
    private ChannelSaver[] m_oSavers = new ChannelSaver[32];
    private IOException m_oFailure;
    private String m_sOutputFile;

    public IOException getException() {
        return m_oFailure;
    }
    
    public XASaver(PSXMediaXA oMedia, String sOutputFile) {
        m_oMedia = oMedia;
        m_sOutputFile = sOutputFile;
        for (int i = 0; i < m_oSavers.length; i++) {
            m_oSavers[i] = new ChannelSaver(oMedia, i);
        }
    }
    
    public XASaver(PSXMediaXA oMedia, String sOutputFile, int iChannel) {
        m_oMedia = oMedia;
        m_sOutputFile = sOutputFile;
        m_oSavers[0] = new ChannelSaver(oMedia, iChannel);
    }
    
    public void done() throws IOException {
        m_oMedia.clearListeners();
        for (ChannelSaver oSaver : m_oSavers) {
            if (oSaver != null) oSaver.done();
        }
    }
    
    /** Very much like the AudioSaver class, but attaches to an
     *  XA audio channel. */
    private class ChannelSaver implements IAudListener {

        private AudioWriter m_oAudWriter;
        private String m_sOutputFormat = "wav";
        private int m_iChannel;

        public ChannelSaver(PSXMediaXA oMedia, int iChannel) {
            m_iChannel = iChannel;
            oMedia.addChannelListener(this, iChannel);
        }

        public boolean event(AudioInputStream is, int sector) {
            try {
                if (m_oAudWriter == null) {
                    String s = String.format("%s_ch%02d.%s", 
                            m_sOutputFile, m_iChannel, m_sOutputFormat);
                    m_oAudWriter = new AudioWriter(
                        new File(s), 
                        is.getFormat(), 
                        PSXMedia.AudioFileFormatStringToType(m_sOutputFormat));
                }
                fireProgressUpdate("Reading audio sector " + sector + " channel " + m_iChannel, 
                        m_oMedia.getStartSector(), 
                        m_oMedia.getEndSector(), 
                        sector);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IO.writeIStoOS(is, bos);
                m_oAudWriter.write(bos.toByteArray());
                return false;
            } catch (IOException ex) {
                m_oFailure = ex;
                fireProgressError(ex);
                return true;
            }
        }

        public void done() throws IOException {
            if (m_oAudWriter != null)
                m_oAudWriter.close();
        }

    }
    
}
