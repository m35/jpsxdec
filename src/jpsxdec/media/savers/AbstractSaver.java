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
 * AbstractSaver.java
 */

package jpsxdec.media.savers;

import java.io.IOException;
import jpsxdec.media.PSXMediaStreaming;
import jpsxdec.util.IProgressListener;

/** Attaches to PSXMedia classes to handle the physical saving 
 *  of media as the media item is played. */
public abstract class AbstractSaver implements PSXMediaStreaming.IErrorListener {
    
    private IProgressListener m_oListener;
    
    public AbstractSaver(PSXMediaStreaming oMedia) {
        oMedia.addErrorListener(this);
    }
    
    public void addProgressListener(IProgressListener oListener) {
        m_oListener = oListener;
    }
    
    protected void fireProgressUpdate(String msg, long lngStartPos, long lngEndPos, long lngCurPos) throws StopPlayingException {
        if (m_oListener != null) {
            boolean bln = m_oListener.ProgressUpdate(
                    msg, 
                    (lngCurPos - lngStartPos)
                    / (double)(lngEndPos - lngStartPos));
            if (bln) throw new StopPlayingException();
        }
    }
    
    public void error(Exception ex) throws StopPlayingException {
        if (m_oListener != null && 
            m_oListener instanceof IProgressListener.IProgressErrorListener) 
        {
            ((IProgressListener.IProgressErrorListener)m_oListener).ProgressUpdate(ex);
        }
    }

    /** Performs cleanup after the media is done playing (closes files,
     *  detaches listeners, etc.). */
    public abstract void done() throws IOException;
    
}
