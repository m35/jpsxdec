/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
import jpsxdec.cdreaders.SectorReadErrorException;
import jpsxdec.util.AdvancedIOIterator;

/** Class to walk a range of sectors of a CD and automatically convert them
 *  to PSXSectors before returning them. */
public class PSXSectorRangeIterator implements AdvancedIOIterator<PSXSector> {
    private CDSectorReader m_oCD;
    private int m_iSectorIndex;
    private int m_iStartSector, m_iEndSector;
    private PSXSector m_oCachedSector;
    private boolean m_blnCached = false;
    
    /** Interface for listener classes. */
    public static interface ISectorChangeListener {
        void CurrentSector(int i);
        void ReadError(SectorReadErrorException ex);
    }
    private ISectorChangeListener m_oListener;

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
        if (!hasNext()) 
            throw new NoSuchElementException();
        if (!m_blnCached) {
            try {
                m_oCachedSector = PSXSector.SectorIdentifyFactory(m_oCD.getSector(m_iSectorIndex));
                m_blnCached = true;
            } catch (SectorReadErrorException ex) {
                if (m_oListener != null) m_oListener.ReadError(ex);
                return null;
            }
        }
        return m_oCachedSector;
    }
    
    public boolean hasNext() {
        return m_iSectorIndex <= m_iEndSector;
    }

    public PSXSector next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        PSXSector oPSXSect = null;
        if (!m_blnCached) {
            try {
                oPSXSect = PSXSector.SectorIdentifyFactory(m_oCD.getSector(m_iSectorIndex));
            } catch (SectorReadErrorException ex) {
                if (m_oListener != null) m_oListener.ReadError(ex);
            }
        } else {
            oPSXSect = m_oCachedSector;
            m_oCachedSector = null;
            m_blnCached = false;
        }
        m_iSectorIndex++;
        if (m_oListener != null) m_oListener.CurrentSector(m_iSectorIndex);
        return oPSXSect;
    }
    
    public void skipNext() {
        if (!hasNext()) throw new NoSuchElementException();
        m_blnCached = false;
        m_oCachedSector = null;
        m_iSectorIndex++;
        if (m_oListener != null) m_oListener.CurrentSector(m_iSectorIndex);
    }

    
    public int getIndex() {
        return m_iSectorIndex;
    }
    
    public void gotoIndex(int i) {
        if (i < m_iStartSector || i > m_iEndSector+1) throw new NoSuchElementException();
        m_blnCached = false;
        m_oCachedSector = null;
        m_iSectorIndex = i;
    }

    /** Unable to remove sectors */ 
    public void remove() {throw new UnsupportedOperationException();}
    
    /** Set a listener for whenever the sector is incremented. */
    public void setSectorChangeListener(ISectorChangeListener oCallbk) {
        m_oListener = oCallbk;
    }

}




