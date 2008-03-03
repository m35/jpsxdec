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
 * CDXAIterator.java
 */

package jpsxdec.cdreaders;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.util.AdvancedIOIterator;


/** An AdvancedIOIterator of CDXASector sectors. */
public class CDXAIterator implements AdvancedIOIterator<CDXASector> {

    private CDSectorReader m_oCD;
    int m_iSectorIndex;
    int m_iStartSector, m_iEndSector;
    
    public CDXAIterator(CDSectorReader oCD, int iStartSector, int iEndSector) {
        m_oCD = oCD;
        m_iStartSector = iStartSector;
        m_iEndSector = iEndSector;
        m_iSectorIndex = iStartSector;
    }
    
    public CDXASector peekNext() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        return m_oCD.getSector(m_iSectorIndex);
    }
    
    public boolean hasNext() {
        return m_iSectorIndex <= m_iEndSector;
    }

    public CDXASector next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        CDXASector oCDSect = m_oCD.getSector(m_iSectorIndex);
        m_iSectorIndex++;
        return oCDSect;
    }
    
    public void skipNext() {
        if (!hasNext()) throw new NoSuchElementException();
        m_iSectorIndex++;
    }
    
    public int getIndex() {
        return m_iSectorIndex;
    }
    
    public void gotoIndex(int i) {
        if (i < m_iStartSector || i > m_iEndSector+1) throw new NoSuchElementException();
        m_iSectorIndex = i;
    }

    public void remove() {throw new UnsupportedOperationException();}
    
}
