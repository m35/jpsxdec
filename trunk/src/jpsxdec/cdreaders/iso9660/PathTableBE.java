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
 * PathTableBE.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.*;
import java.util.ArrayList;
import jpsxdec.util.NotThisTypeException;

public class PathTableBE extends ArrayList<PathTableRecordBE> {

    public PathTableBE(InputStream is) throws IOException {

        try {
            for (int index = 1; true ; index++) {
                PathTableRecordBE ptr = new PathTableRecordBE(is, index);
                super.add(ptr);
            }
        } catch (NotThisTypeException ex) {}
        super.trimToSize();
        
    }
    
}
