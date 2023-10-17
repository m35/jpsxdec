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

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;
import jpsxdec.util.BinaryDataNotRecognized;

public class PathTableRecordBE extends ISO9660Struct {
    /** Valid characters include
     * uppercase A-Z,
     * digits 0-9,
     * hyphen -,
     * underscore _,
     * and period . */
    private static final String VALID_CHARACTERS = "[A-Z\\d-_\\.]+";

    final public int index;

    //final public int xa_len; = 0?
    final public long extent;
    final public int parent;
    final public String name;

    public PathTableRecordBE(@Nonnull InputStream is, int index) throws IOException, BinaryDataNotRecognized {
        this.index = index;

        int name_len = read1(is);
        if (name_len == 0) throw new BinaryDataNotRecognized();
        magic1(is, 0);
        extent = read4_BE(is);
        if (extent < 1) throw new BinaryDataNotRecognized();
        parent = read2_BE(is);
        if (parent < 1) throw new BinaryDataNotRecognized();
        name   = readS(is, name_len);
        magicXzero(is, name_len % 2);
        if (!name.matches(VALID_CHARACTERS))
            throw new BinaryDataNotRecognized();
    }

    @Override
    public String toString() {
        return name;
    }
}
