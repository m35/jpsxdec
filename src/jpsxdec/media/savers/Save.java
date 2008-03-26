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
 * Save.java
 */

package jpsxdec.media.savers;

import java.io.IOException;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.util.IProgressListener;


public class Save {

    public static void DecodeStreaming(SavingOptions oOptions,
                                       IProgressListener oListener) 
            throws IOException 
    {
        
        PSXMediaStreaming oMedia = oOptions.getMedia();
        
        oMedia.Reset();
        
        AviSaver oAviSaver = null;
        ImageSequenceSaver oImageSaver = null;
        
        if (oOptions.decodeVideo() && oMedia.hasVideo()) {
        
            if (oOptions.getStartFrame() != oMedia.getStartFrame())
                oMedia.seek(oOptions.getStartFrame());
                
            if (oOptions.getVideoFormat().equals(SavingOptions.AVI_FORMAT.getId()))
            {
                oAviSaver = new AviSaver(oOptions);
                oAviSaver.addProgressListener(oListener);
            } 
            else
            { // image sequence

                oImageSaver = new ImageSequenceSaver(oOptions);
                oImageSaver.addProgressListener(oListener);
            } 
        }
        
        AudioSaver oAudioSaver = null;
        if (oOptions.decodeAudio() && !oOptions.getAudioFormat().equals(SavingOptions.AVI_AUDIO.getId())) 
        {
            oAudioSaver = new AudioSaver(oOptions);
            oAudioSaver.addProgressListener(oListener);
        }
        
        try {
            oMedia.Play();
        } finally {
            try {
                if (oAviSaver   != null) oAviSaver.done();
            } catch (Exception ex) {}
            try {
                if (oImageSaver != null) oImageSaver.done();
            } catch (Exception ex) {}
            try {
                if (oAudioSaver != null) oAudioSaver.done();
            } catch (Exception ex) {}
        }

    }
    
}
