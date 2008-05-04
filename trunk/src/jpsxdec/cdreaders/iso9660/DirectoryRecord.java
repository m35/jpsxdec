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
 * DirectoryRecord.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.InputStream;
import java.io.IOException;
import jpsxdec.util.NotThisTypeException;

/** ECMA119: 9.1 */
public class DirectoryRecord extends ISOstruct {
    
    /*                                length                 */
    final public long                 extent;
    /*                                ext_attr_length        */
    final public long                 size;
    final public RecordingDateAndTime date;
    final public int                  flags;
    /*                                file_unit_size         */
    /*                                interleave             */ 
    /*                                volume_sequence_number */
    /*                                name_len               */
    final public String               name;
    /*                                name_extra             */
    
    public DirectoryRecord(InputStream is) 
            throws IOException, NotThisTypeException 
    {
        int    length                  = read1(is);
        if (length < 1) throw new NotThisTypeException();
        /*     ext_attr_length        */ magic1(is, 0);
               extent                  = read8_bothendian(is);
               size                    = read8_bothendian(is);
               date                    = /*7*/new RecordingDateAndTime(is);
               flags                   = read1(is);
        /*     file_unit_size         */ magic1(is, 0);
        /*     interleave             */ magic1(is, 0);
        /*     volume_sequence_number */ magic4_bothendian(is, 1);
        int    name_len                = read1(is);
               name                    = readS(is, name_len);
        byte[] name_extra              = readX(is, length - 33 - name_len);
    }
    
    public String toString() {
        return '"' + this.name + '"';
    }
}
