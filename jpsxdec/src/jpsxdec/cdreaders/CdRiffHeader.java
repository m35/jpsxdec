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

package jpsxdec.cdreaders;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/** When a portion of raw BIN/CUE disc image data is stored as a file,
 *  it is often found wrapped in a RIFF container. Use this to write such a
 *  header prior to writing the raw BIN/CUE data. */
public class CdRiffHeader {

    public static final int SIZEOF = 44;

    private static final byte[] RIFF = {'R','I','F','F'};
    private static final byte[] CDXA = {'C','D','X','A'};
    private static final byte[] fmt_ = {'f','m','t', ' '};
    private static final byte[] data = {'d','a','t', 'a'};
    private static final byte[] EMPTY_fmt = new byte[16];

    /** @param lngFileSize Size of the raw image data, excluding the RIFF header
     *                     (must be a multiple of 2352). */
    public static void write(@Nonnull OutputStream os, long lngFileSize) throws IOException {
        if (lngFileSize % 2352 != 0)
            throw new IllegalArgumentException(lngFileSize + " is not a multiple of 2352");
        if (lngFileSize > 0xffffffffL)
            throw new IllegalArgumentException("File size " + lngFileSize + " too big to write to header.");
        os.write(RIFF);
        IO.writeInt32LE(os, lngFileSize + SIZEOF - 8);
        os.write(CDXA);
        os.write(fmt_);
        IO.writeInt32LE(os, 16);
        // it seems you're technically supposed to write the disc's iso9660 directory record here
        // but I don't think leaving it blank will cause many problems in the wild
        os.write(EMPTY_fmt);
        os.write(data);
        IO.writeInt32LE(os, lngFileSize);
    }

}
