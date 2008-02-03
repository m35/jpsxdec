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
 * PSXSectorRangeIterator.java
 */

package jpsxdec.sectortypes;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.AdvancedIOIterator;

/** Class to walk a range of sectors of a CD and automatically convert them
 *  to PSXSectors before returning them. */
public class PSXSectorRangeIterator implements AdvancedIOIterator<PSXSector> {
    CDSectorReader m_oCD;
    int m_iSectorIndex;
    int m_iStartSector, m_iEndSector;
    
    /** Interface for callback classes. */
    public static interface ICurrentSector {
        void CurrentSector(int i);
    }
    ICurrentSector m_oCallBack;

    public PSXSectorRangeIterator(CDSectorReader oCD) {
        this(oCD, 0, oCD.size()-1);
    }
    
    public PSXSectorRangeIterator(CDSectorReader oCD, int iStartSector, int iEndSector) {
        m_oCD = oCD;
        m_iStartSector = iStartSector;
        m_iEndSector = iEndSector;
        m_iSectorIndex = iStartSector;
    }
    
    public CDSectorReader getSourceCD() {
        return m_oCD;
    }

    public PSXSector peekNext() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        return PSXSector.SectorIdentifyFactory(m_oCD.getSector(m_iSectorIndex));
    }
    
    public boolean hasNext() {
        return m_iSectorIndex <= m_iEndSector;
    }

    public PSXSector next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        CDXASector oCDSect = m_oCD.getSector(m_iSectorIndex);
        PSXSector oPSXSect = PSXSector.SectorIdentifyFactory(oCDSect);
        m_iSectorIndex++;
        if (m_oCallBack != null) m_oCallBack.CurrentSector(m_iSectorIndex);
        return oPSXSect;
    }
    
    public void skipNext() {
        if (!hasNext()) throw new NoSuchElementException();
        m_iSectorIndex++;
        if (m_oCallBack != null) m_oCallBack.CurrentSector(m_iSectorIndex);
    }

    
    public int getIndex() {
        return m_iSectorIndex;
    }
    
    public void gotoIndex(int i) {
        if (i < m_iStartSector || i > m_iEndSector+1) throw new NoSuchElementException();
        m_iSectorIndex = i;
    }

    public void remove() {throw new UnsupportedOperationException();}
    
    /** Set a callback for whenever the sector is incremented. */
    public void setCallback(ICurrentSector oCallbk) {
        m_oCallBack = oCallbk;
    }

}




