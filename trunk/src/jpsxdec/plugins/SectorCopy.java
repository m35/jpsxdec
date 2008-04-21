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
 * Lain_SITE.java
 */

package jpsxdec.plugins;

import java.io.IOException;
import jpsxdec.cdreaders.CDFileSectorReader;

public class SectorCopy {

    public static void main(String[] args) throws IOException {
        
        if (args.length != 3) throw new IllegalArgumentException("Need 3 args");
        
        CDFileSectorReader oSrc = new CDFileSectorReader(args[0]);
        CDFileSectorReader oDest = new CDFileSectorReader(args[1], true);
        int iStartSector = Integer.parseInt(args[2]);
        
        if (oSrc.size() > oDest.size()) throw new IllegalArgumentException("Source file is larger than dest file");
        
        if (iStartSector + oSrc.size() > oDest.size()) throw new IllegalArgumentException("Source file will run off the end of dest file");
        
        for (int iOfsSect = 0; iOfsSect < oSrc.size(); iOfsSect++) {
            byte[] abSrcUserData = oSrc.getSector(iOfsSect).getSectorData();
            System.out.println("Overriting sector " + (iStartSector + iOfsSect));
            oDest.writeSector(iStartSector + iOfsSect, abSrcUserData);
        }
        
        System.out.println(oSrc.size() + " sectors overwritten.");
        
        oSrc.close();
        oDest.close();
        
    }


 
}
