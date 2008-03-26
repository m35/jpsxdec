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
 * PSXSectorNull.java
 */

package jpsxdec.sectortypes;

import jpsxdec.cdreaders.CDXASector;
import jpsxdec.util.NotThisTypeException;


    
/** XA files have lots of audio channels. When a channel is no longer used
 * because the audio is finished, it is filled with what I call
 * 'null' sectors. These sectors have absolutely no SubMode flags set. */
public class PSXSectorNull extends PSXSector {

    public PSXSectorNull(CDXASector oCDSect)
            throws NotThisTypeException 
    {
        super(oCDSect);
        if (!oCDSect.hasSectorHeader() || oCDSect.getSubMode().toByte() != 0) 
        {
            throw new NotThisTypeException();
        }
        super.m_iChannel = -1;
    }

    public String toString() {
        return String.format("Null [Sector:%d]", getSector());
    }

    protected int getDemuxedDataStart(int iDataSize) {
        return 0;
    }

    protected int getDemuxedDataLength(int iDataSize) {
        return 1; // just making the sector 1 byte long for no reason
    }

}



