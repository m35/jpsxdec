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
 * UnknownDataPullDemuxerIS.java
 */

package jpsxdec.demuxers;

import java.io.*;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.sectortypes.PSXSectorUnknownData;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IGetFilePointer;

/** Demuxes a series of PSXSectorUnknownData into a solid stream.
 *  Sectors are pulled from an iterator as they are needed. */
public class UnknownDataPullDemuxerIS extends InputStream implements IGetFilePointer 
{
    
    PSXSectorRangeIterator m_oPsxSectIter;
    /** Holds the current sector's stream being read */
    ByteArrayFPIS m_oPsxSectStream;
    boolean m_blnAtEnd = false;
    
    // holds the marked position, both the sector and the stream
    int m_iMarkIndex = -1;
    ByteArrayFPIS m_oMarkSectStream;
    
    public UnknownDataPullDemuxerIS(PSXSectorRangeIterator oPsxSectIter) 
            throws IOException 
    {
        m_oPsxSectIter = oPsxSectIter;
        PSXSector oSect = m_oPsxSectIter.peekNext();
        if (!(oSect instanceof PSXSectorUnknownData)) {
            m_blnAtEnd = true;
            m_oPsxSectStream = null;
        } else {
            m_oPsxSectStream = oSect.getUserDataStream();
        }
    }

    /** [InputStream] */ @Override 
    public int read() throws IOException {
        if ((m_oPsxSectStream == null) || m_blnAtEnd)
            return -1;
        else {
            int i = m_oPsxSectStream.read();
            while (i < 0) {
                m_oPsxSectIter.skipNext();
                if (m_oPsxSectIter.hasNext()) {
                    PSXSector oSect = m_oPsxSectIter.peekNext();
                    if (oSect instanceof PSXSectorUnknownData) {
                        m_oPsxSectStream = oSect.getUserDataStream();
                        i = m_oPsxSectStream.read();
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
        if (m_oPsxSectStream == null)  // there were never any sectors
            return -1;
        else
            return m_oPsxSectStream.getFilePointer();
    }

    /** @param readlimit ignored. You can read forever. */
    @Override /* [InputStream] */
    public void mark(int readlimit) {
        if (m_oPsxSectStream != null) { // there were never any sectors to mark
            // save the sector index
            m_iMarkIndex = m_oPsxSectIter.getIndex();
            // and save the stream in case 
            // furthur reads move past the current stream
            m_oMarkSectStream = m_oPsxSectStream;
            m_oMarkSectStream.mark(readlimit);
        }
    }

    @Override /* [InputStream] */
    public void reset() throws IOException {
        if (m_iMarkIndex < 0)
            throw new RuntimeException("Trying to reset when mark has not been called.");
        
        // go back to the sector we were at (if we've moved since then)
        if (m_iMarkIndex != m_oPsxSectIter.getIndex()) {
            m_oPsxSectIter.gotoIndex(m_iMarkIndex);
        }
        // restore the original stream (in case we moved on since then)
        m_oPsxSectStream = m_oMarkSectStream;
        // and reset the original position
        m_oPsxSectStream.reset();
        
        m_iMarkIndex = -1;
        m_oMarkSectStream = null;
    }

    @Override /* [InputStream] */
    public boolean markSupported() {
        return true;
    }
    
}
