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
 * AudioSaver.java
 */

package jpsxdec.savers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.media.PSXMedia;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.IAudListener;
import jpsxdec.media.PSXMedia.PSXMediaStreaming.PSXMediaVideo;
import jpsxdec.util.AudioWriter;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves audio to wav file. */
public class AudioSaver extends AbstractSaver implements IAudListener {
    
    protected PSXMediaVideo m_oMedia;
    private AudioWriter m_oAudWriter;
    private String m_sOutputFile;
    private String m_sOutputFormat = "wav";
    protected IOException m_oFailure;

    public IOException getException() {
        return m_oFailure;
    }
    
    public AudioSaver(PSXMediaVideo oMedia, String sOutputFile) {
        this(oMedia, sOutputFile, true);
    }
    
    protected AudioSaver(PSXMediaVideo oMedia, String sOutputFile, boolean blnDecodeAudio) {
        m_oMedia = oMedia;
        if (blnDecodeAudio) {
            m_sOutputFile = sOutputFile;
            m_oMedia.addAudioListener(this);
        }
    }
    
    /** [implements IAudListener] */
    public boolean event(AudioInputStream is, int sector) {
        try {
            if (m_oAudWriter == null) {
                m_oAudWriter = new AudioWriter(
                    new File(m_sOutputFile + "." + m_sOutputFormat), 
                    is.getFormat(), 
                    PSXMedia.AudioFileFormatStringToType(m_sOutputFormat));
            }
            fireProgressUpdate("Reading audio sector " + sector, 
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
        if (m_oMedia != null) m_oMedia.clearListeners();
        if (m_oAudWriter != null) m_oAudWriter.close();
    }
}
