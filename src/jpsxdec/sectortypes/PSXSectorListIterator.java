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
 */

package jpsxdec.sectortypes;

import java.io.IOException;
import java.util.NoSuchElementException;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.util.AdvancedIOIterator;

/** Class to walk a list of sectors of a CD and automatically convert them
 *  to PSXSectors. */
public class PSXSectorListIterator implements AdvancedIOIterator<PSXSector> {
    CDSectorReader m_oCD;
    int m_iListIndex = 0;
    int[] m_aiSectorList;

    public PSXSectorListIterator(CDSectorReader oCD, int[] aiSectorList) {
        m_oCD = oCD;
        // check sectors
        for (int i : aiSectorList) {
            if (i < 0 || i >= oCD.size()) throw new NoSuchElementException();
        }
        m_aiSectorList = aiSectorList;
    }
    
    public CDSectorReader getSourceCD() {
        return m_oCD;
    }

    public boolean hasNext() {
        return m_iListIndex <= m_aiSectorList.length-1;
    }

    public PSXSector next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        PSXSector oSect;
        oSect = PSXSector.SectorIdentifyFactory(m_oCD.getSector(m_aiSectorList[m_iListIndex]));
        m_iListIndex++;
        return oSect;
    }

    public PSXSector peekNext() throws IOException {
        if (!hasNext()) throw new NoSuchElementException();
        return PSXSector.SectorIdentifyFactory(m_oCD.getSector(m_aiSectorList[m_iListIndex]));
    }

    public void skipNext() {
        if (!hasNext()) throw new NoSuchElementException();
    }

    public int getIndex() {
        return m_iListIndex;
    }

    public void gotoIndex(int i) {
        if (i < 0 || i > m_aiSectorList.length) throw new NoSuchElementException();
        m_iListIndex = i;
    }
    
    public void remove() {throw new UnsupportedOperationException();}

}
