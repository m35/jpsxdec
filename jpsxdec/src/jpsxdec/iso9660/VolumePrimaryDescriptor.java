/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.iso9660;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;

/** ECMA119: 8.4
 *<p>
 * The Volume Primary Descriptor should appear in sector 16 of a CD.
 */
public class VolumePrimaryDescriptor extends ISO9660Struct {

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

    public VolumePrimaryDescriptor(@Nonnull InputStream is)
            throws EOFException, IOException, BinaryDataNotRecognized
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

    @Override
    public String toString() {
        return system_id.trim() + " " + volume_id.trim();
    }
}
