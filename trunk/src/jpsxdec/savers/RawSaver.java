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
 * RawSaver.java
 */

package jpsxdec.savers;

import java.io.FileOutputStream;
import java.io.IOException;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.media.PSXMedia;
import jpsxdec.media.PSXMedia.PSXMediaStreaming;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. Saves raw sectors of the media, 
 * to create XA or STR files (currently without RIFF header). */
public class RawSaver extends AbstractSaver
        implements PSXMedia.PSXMediaStreaming.IRawListener 
{

    private PSXMediaStreaming m_oMedia;
    private FileOutputStream m_oFileOut;
    private IOException m_oFailure;

    public IOException getException() {
        return m_oFailure;
    }
    
    public RawSaver(PSXMediaStreaming oMedia, String sOutputFile) 
            throws IOException 
    {
        m_oFileOut = new FileOutputStream(sOutputFile);
        
        oMedia.addRawListener(this);
        
        m_oMedia = oMedia;
    }
    
    public boolean event(CDXASector oSect) {
        try {
            fireProgressUpdate("Reading audio sector " + oSect.getSector(), 
                        m_oMedia.getStartSector(),
                        m_oMedia.getEndSector(),
                        oSect.getSector());
            m_oFileOut.write(oSect.getRawSectorData());
        } catch (IOException ex) {
            m_oFailure = ex;
            fireProgressError(ex);
            return true;
        }
        return false;
    }
    
    public void done() throws IOException {
        m_oMedia.clearListeners();
        m_oFileOut.close();
    }

}
