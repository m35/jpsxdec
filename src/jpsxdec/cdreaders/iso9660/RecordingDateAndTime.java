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
 * RecordingDateAndTime.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.InputStream;
import java.io.IOException;

/** ECMA119: 9.1.5 */
public class RecordingDateAndTime extends ISOstruct {
    public final int YearsSince1900;
    public final int Month;
    public final int Day;
    public final int Hour;
    public final int Minute;
    public final int Second;
    public final int GreenwichOffset;
    
    public RecordingDateAndTime(InputStream is) throws IOException {
        YearsSince1900  = read1(is);
        Month           = read1(is);
        Day             = read1(is);
        Hour            = read1(is);
        Minute          = read1(is);
        Second          = read1(is);
        GreenwichOffset = read1(is) - 128;
    }
    
    public String toString() {
        return String.format(
            "%d/%d/%d %02d:%02d:%d %d",
            Month+1, Day+1, YearsSince1900+1900,
            Hour+1, Minute+1, Second+1,
            GreenwichOffset * 15 / 60
        );        
    }

}
