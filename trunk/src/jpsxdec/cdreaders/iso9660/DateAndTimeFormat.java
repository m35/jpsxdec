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
 * DateAndTimeFormat.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.IOException;
import java.io.InputStream;

/** ECMA119: 8.4.26.1 */
public class DateAndTimeFormat extends ISOstruct {

    final public String Year;
    final public String Month;
    final public String Day;
    final public String Hour;
    final public String Minute;
    final public String Second;
    final public String HundredthsSecond;
    final public int GreenwichOffset;
    
    public DateAndTimeFormat(InputStream is) throws IOException {
        Year             = readS(is, 4);
        Month            = readS(is, 2);
        Day              = readS(is, 2);
        Hour             = readS(is, 2);
        Minute           = readS(is, 2);
        Second           = readS(is, 2);
        HundredthsSecond = readS(is, 2);
        GreenwichOffset  = read1(is) - 128;
    }
    
    public String toString() {
        return String.format(
            "%s/%s/%s %s:%s:%s.%s %d",
            Month, Day, Year, 
            Hour, Minute, Second, HundredthsSecond,
            GreenwichOffset * 15 / 60
        );
    }

}
