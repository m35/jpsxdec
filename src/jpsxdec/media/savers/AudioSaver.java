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

package jpsxdec.media.savers;

import jpsxdec.media.StopPlayingException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.media.PSXMediaStreaming.IAudListener;
import jpsxdec.media.savers.Formats.AudFormat;
import jpsxdec.util.AudioWriter;
import jpsxdec.util.IO;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves audio to wav file. */
public class AudioSaver extends AbstractSaver implements IAudListener {
    
    protected final PSXMediaStreaming m_oMedia;
    private AudioWriter m_oAudWriter;
    private final File m_oFile;
    private final Formats.AudFormat m_sAudioFormat;

    public AudioSaver(SavingOptions oOptions) {
        super(oOptions.getMedia());
        
        m_oMedia = oOptions.getMedia();
        
        if (m_oMedia.getAudioChannels() < 1 || m_oMedia.getAudioChannels() > 2)
            throw new IllegalArgumentException("Media must have 1 or 2 channels of audio.");
        
        m_oFile = new File(oOptions.getFolder(),
            oOptions.getAudioFilenameBase() + oOptions.getAudioFilenameExt());
        
        m_sAudioFormat = (AudFormat)oOptions.getAudioFormat();
        
        m_oMedia.addAudioListener(this);
    }
    
    /** [implements IAudListener] */
    public void event(AudioInputStream is, int sector) throws StopPlayingException, IOException {
        if (m_oAudWriter == null) {
            m_oAudWriter = new AudioWriter(
                m_oFile, 
                is.getFormat(), 
                m_sAudioFormat.getType());
        }
        
        fireProgressUpdate("Reading audio sector " + sector, 
                m_oMedia.getStartSector(), 
                m_oMedia.getEndSector(), 
                sector);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        IO.writeIStoOS(is, bos);
        m_oAudWriter.write(bos.toByteArray());
    }
    
    public void done() throws IOException {
        if (m_oMedia != null) m_oMedia.clearListeners();
        if (m_oAudWriter != null) m_oAudWriter.close();
    }
}
