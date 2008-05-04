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
 * VolumePrimaryDescriptor.java
 */

package jpsxdec.cdreaders.iso9660;

import java.io.InputStream;
import java.io.IOException;
import jpsxdec.util.NotThisTypeException;

/** ECMA119: 8.4 */
public class VolumePrimaryDescriptor extends ISOstruct {
    
    /*                             type                   */
    /*                             id                     */
    /*                             version                */
    /*                             unused1                */
    final public String            system_id;
    final public String            volume_id;
    /*                             unused2                */
    final public long              volume_space_size;
    /*                             unused3                */
    /*                             volume_set_size        */
    /*                             volume_sequence_number */
    /*                             logical_block_size     */
    final public long              path_table_size;
    final public long              type_l_path_table;
    final public long              opt_type_l_path_table;
    final public long              type_m_path_table;
    final public long              opt_type_m_path_table;
    final public DirectoryRecord   root_directory_record;
    final public String            volume_set_id;
    final public String            publisher_id;
    final public String            preparer_id;
    final public String            application_id;
    final public String            copyright_file_id;
    final public String            abstract_file_id;
    final public String            bibliographic_file_id;
    final public DateAndTimeFormat creation_date;
    final public DateAndTimeFormat modification_date;
    final public DateAndTimeFormat expiration_date;
    final public DateAndTimeFormat effective_date;
    /*                             file_structure_version */
    /*                             unused4                */
    final public byte[]            application_data;
    /*                             unused5                */

    public VolumePrimaryDescriptor(InputStream is) 
            throws IOException, NotThisTypeException 
    {
        /* type                   */ magic1(is, 1); 
        /* id                     */ magicS(is, "CD001"); 
        /* version                */ magic1(is, 1);        
        /* unused1                */ magic1(is, 0);       
           system_id               = readS(is, 32);
           volume_id               = readS(is, 32);
        /* unused2                */ magicXzero(is, 8);
           volume_space_size       = read8_bothendian(is);
        /* unused3                */ magicXzero(is, 32);
        /* volume_set_size        */ magic4_bothendian(is, 1);
        /* volume_sequence_number */ magic4_bothendian(is, 1);
        /* logical_block_size     */ magic4_bothendian(is, 2048);
           path_table_size         = read8_bothendian(is);
           type_l_path_table       = read4_LE(is);
           opt_type_l_path_table   = read4_LE(is);
           type_m_path_table       = read4_BE(is);
           opt_type_m_path_table   = read4_BE(is);
           root_directory_record   = new DirectoryRecord(is);
           volume_set_id           = readS(is, 128);
           publisher_id            = readS(is, 128);
           preparer_id             = readS(is, 128);
           application_id          = readS(is, 128);
           copyright_file_id       = readS(is, 37);
           abstract_file_id        = readS(is, 37);
           bibliographic_file_id   = readS(is, 37);
           creation_date           = new DateAndTimeFormat(is);
           modification_date       = new DateAndTimeFormat(is);
           expiration_date         = new DateAndTimeFormat(is);
           effective_date          = new DateAndTimeFormat(is);
        /* file_structure_version */ magic1(is, 1);
        /* unused4                */ magic1(is, 0);
           application_data        = readX(is, 512);
        /* unused5                   magicXzero(is, 653); */
    }
    
    
    
}
