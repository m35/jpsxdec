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
 * PSXSectorListIterator.java
 *
 */

package jpsxdec;

import java.util.ListIterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class PSXSectorListIterator implements ListIterator<PSXSector> {
    CDSectorReader m_oCD;
    PSXSector m_oCurrentSector = null;
    ListIterator<Integer> m_oReadIterator;
    ArrayList<Integer> oIList;

    public PSXSectorListIterator(CDSectorReader oCD, int[] aiSectorList) {
        m_oCD = oCD;
        oIList = new ArrayList<Integer>();
        for (int i : aiSectorList) {
            // ignore invalid sectors
            //TODO: should probably throw an error
            if (i >= 0 && i < oCD.size())
                oIList.add(new Integer(i));
        }
        m_oReadIterator = oIList.listIterator();
    }
    

    /** Returns the previous retrieved item without changing
     *  the pointer */
    public PSXSector get() {
        if (m_oCurrentSector != null)
            return m_oCurrentSector;
        else
            throw new NoSuchElementException();
    }
    CDSectorReader getSourceCD() {
        return m_oCD;
    }


    public boolean hasNext() {
        return m_oReadIterator.hasNext();
    }

    public PSXSector next() {
        m_oCurrentSector = PSXSector.SectorIdentifyFactory(m_oCD.get(m_oReadIterator.next()));
        return m_oCurrentSector;
    }

    public boolean hasPrevious() {
        return m_oReadIterator.hasPrevious();
    }

    public PSXSector previous() {
        m_oCurrentSector = PSXSector.SectorIdentifyFactory(m_oCD.get(m_oReadIterator.previous()));
        return m_oCurrentSector;
    }

    public int nextIndex() {
        return oIList.get(m_oReadIterator.nextIndex());
    }

    public int previousIndex() {
        return oIList.get(m_oReadIterator.previousIndex());
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
