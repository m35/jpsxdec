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
 * ISOFile.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.File;

public class ISOFile extends File {

    private final long m_iStartSector;
    private final long m_iSize;
    
    public ISOFile(File oDir, String sFileName, long iStartSector, long iSize) {
        super(oDir, sFileName);
        m_iStartSector = iStartSector;
        m_iSize = iSize;
    }

    public long getLength() {
        return (m_iSize+2047) / 2048;
    }

    public long getEndSector() {
        return m_iStartSector + getLength() - 1;
    }
    
    public long getStartSector() {
        return m_iStartSector;
    }
    
}
