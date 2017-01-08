/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.io.IOException;
import java.io.InputStream;
import jpsxdec.util.BinaryDataNotRecognized;

/** ECMA119: 9.1 */
public class DirectoryRecord extends ISO9660Struct {
    
    // TODO: Look up what this flag is really called
    static final public int FLAG_IS_DIRECTORY = 0x2;
    
    /*                                length                 */
    /** Sector number where the file starts. */
    final public long                 extent;
    /*                                ext_attr_length        */
    /** Size of the file in bytes. */
    final public long                 size; // # of sectors*2048 that list files in this dir
    final public RecordingDateAndTime date;
    final public int                  flags;
    /*                                file_unit_size         */
    /*                                interleave             */ 
    /*                                volume_sequence_number */
    /*                                name_len               */
    final public String               name;
    /*                                name_extra             */
    
    public DirectoryRecord(InputStream is) 
            throws IOException, BinaryDataNotRecognized 
    {
        int    length                  = read1(is);
        if (length < 1) throw new BinaryDataNotRecognized();
        /*     ext_attr_length        */ magic1(is, 0);
               extent                  = read8_bothendian(is);
               size                    = read8_bothendian(is);
        //if (size % 2048 != 0) throw new BinaryDataNotRecognized();
               date                    = /*7*/new RecordingDateAndTime(is);
               flags                   = read1(is);
        if (((flags & FLAG_IS_DIRECTORY) > 0) && ((size % 2048) != 0))
            throw new BinaryDataNotRecognized();
        /*     file_unit_size         */ magic1(is, 0);
        /*     interleave             */ magic1(is, 0);
        /*     volume_sequence_number */ magic4_bothendian(is, 1);
        int    name_len                = read1(is);
               name                    = sanitizeFileName(readS(is, name_len));
        byte[] name_extra              = readX(is, length - 33 - name_len);
    }
    
    public String toString() {
        return '"' + this.name + '"';
    }
    
    public static final String CURRENT_DIRECTORY = ".";
    public static final String PARENT_DIRECTORY = "..";
    
    private static String sanitizeFileName(String s) {
        if ("\0".equals(s)) return CURRENT_DIRECTORY;
        if ("\1".equals(s)) return PARENT_DIRECTORY;
        if (s.endsWith(";1")) s = s.substring(0, s.length() - 2);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }
    
}
