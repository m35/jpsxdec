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
 * UnknownDataDemuxerIS.java
 */

package jpsxdec.demuxers;

import java.io.*;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.IGetFilePointer;

public class UnknownDataDemuxerIS extends InputStream implements IGetFilePointer 
{
    
    PSXSectorRangeIterator m_oPsxSectIter;
    /** Holds the current sector being read */
    PSXSector m_oPsxSect; 
    boolean m_blnAtEnd = false;
    
    int m_iMarkIndex = -1;
    int m_iMarkPos = -1;
    
    public UnknownDataDemuxerIS(PSXSectorRangeIterator oPsxSectIter) 
            throws IOException 
    {
        m_oPsxSectIter = oPsxSectIter;
        m_oPsxSect = oPsxSectIter.peekNext();
        if (!(m_oPsxSect instanceof PSXSector.PSXSectorUnknownData)) {
            m_blnAtEnd = true;
            m_oPsxSect = null;
        }
    }

    @Override /* [InputStream] */
    public int read() throws IOException {
        if ((m_oPsxSect == null) || m_blnAtEnd)
            return -1;
        else {
            int i = m_oPsxSect.read();
            while (i < 0) {
                m_oPsxSectIter.skipNext();
                if (m_oPsxSectIter.hasNext()) {
                    m_oPsxSect = m_oPsxSectIter.peekNext();
                    if (m_oPsxSect instanceof PSXSector.PSXSectorUnknownData) {
                        i = m_oPsxSect.read();
                    } else {
                        m_blnAtEnd = true;
                        return -1;
                    }
                } else {
                    m_blnAtEnd = true;
                    return -1;
                }
            }
            return i;
        }
    }

    /* [implements IGetFilePointer] */
    public long getFilePointer() {
        if (m_oPsxSect == null)  // there were never any sectors
            return -1;
        else
            return m_oPsxSect.getFilePointer();
    }

    /** @param readlimit ignored. You can read forever. */
    @Override /* [InputStream] */
    public void mark(int readlimit) {
        // TODO: what would happen if we marked at the end of the stream?
        if (m_oPsxSect != null) { // there were never any sectors to mark
            m_iMarkIndex = m_oPsxSectIter.getIndex();
            m_iMarkPos = m_oPsxSect.getSectorPos();
        }
    }

    @Override /* [InputStream] */
    public void reset() throws IOException {
        if (m_iMarkIndex < 0)
            throw new RuntimeException("Trying to reset when mark has not been called.");
        
        // TODO: what would happen if we reset when at the end of the stream?
        if (m_iMarkIndex != m_oPsxSectIter.getIndex()) {
            m_oPsxSectIter.gotoIndex(m_iMarkIndex);
            m_oPsxSect = m_oPsxSectIter.peekNext();
        }
        m_oPsxSect.setSectorPos(m_iMarkPos);
        
        m_iMarkIndex = -1;
        m_iMarkPos = -1;
    }

    @Override /* [InputStream] */
    public boolean markSupported() {
        return true;
    }
    
}
