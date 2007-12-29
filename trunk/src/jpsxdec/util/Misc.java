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
 * Misc.java
 */

package jpsxdec.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import javax.imageio.ImageIO;

public final class Misc {
    
    /** Returns a sorted array of available ImageIO formats, plus our own 
     * special formats. */
    public static Vector<String> GetJavaImageFormats() {
        Vector<String> oValidFormats = new Vector<String>();
        /*oValidFormats.add("yuv");
        oValidFormats.add("y4m");
        oValidFormats.add("demux");
        oValidFormats.add("0rlc");*/
        String[] asReaderFormats = ImageIO.getReaderFormatNames();
        for (String s : asReaderFormats) {
            s = s.toLowerCase();
            if (oValidFormats.indexOf(s) < 0)
                oValidFormats.add(s);
        }
        
        Collections.sort(oValidFormats);
        
        return oValidFormats;
    }
    
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        
        byte[] arr = new byte[newLength];
        int ceil = original.length-from;
        int len = (ceil < newLength) ? ceil : newLength;
        System.arraycopy(original, from, arr, 0, len);
        
        return arr;
    }
}
