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
 * PSXSectorIterator.java
 *
 */

package jpsxdec;

import java.io.IOException;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import jpsxdec.CDSectorReader.CDXASector;

public class PSXSectorRangeIterator implements ListIterator<PSXSector> {
    CDSectorReader m_oCD;
    PSXSector m_oCurrentSector = null;
    ListIterator<CDXASector> m_oReadIterator;
    int m_iStartSector, m_iEndSector;

    public PSXSectorRangeIterator(CDSectorReader oCD) {
        this(oCD, 0, oCD.size()-1);
    }
    
    public PSXSectorRangeIterator(CDSectorReader oCD, int iStatSector, int iEndSector) {
        m_oCD = oCD;
        m_oReadIterator = oCD.listIterator(iStatSector);
        m_iStartSector = iStatSector;
        m_iEndSector = iEndSector;
    }

    /** Returns the previous retrieved item without changing
     *  the pointer */
    public PSXSector get() {
        //if (m_oCurrentSector != null)
            return m_oCurrentSector;
        //else
        //    throw new NoSuchElementException();
    }
    CDSectorReader getSourceCD() {
        return m_oCD;
    }


    public boolean hasNext() {
        return m_oReadIterator.hasNext() && m_oReadIterator.nextIndex() <= m_iEndSector;
    }

    public PSXSector next() {
        //TODO: Check if beyond m_iEndSector
        m_oCurrentSector = PSXSector.SectorIdentifyFactory(m_oReadIterator.next());
        return m_oCurrentSector;
    }

    public boolean hasPrevious() {
        return m_oReadIterator.hasPrevious();
    }

    public PSXSector previous() {
        m_oCurrentSector = PSXSector.SectorIdentifyFactory(m_oReadIterator.previous());
        return m_oCurrentSector;
    }

    public int nextIndex() {
        //TODO: Check if beyond m_iEndSector
        return m_oReadIterator.nextIndex();
    }

    public int previousIndex() {
        return m_oReadIterator.previousIndex();
    }

    /** Read only list. */
    public void remove() 
    { throw new UnsupportedOperationException(); }
    /** Read only list. */
    public void set(PSXSector e) 
    { throw new UnsupportedOperationException(); }
    /** Read only list. */
    public void add(PSXSector e) 
    { throw new UnsupportedOperationException(); }

}




