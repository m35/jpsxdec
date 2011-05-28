/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.psxvideo.mdec;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jpsxdec.util.IO;

/** Wraps an InputStream (or creates a FileInputStream) to read MDEC values from. */
public class MdecInputStreamReader extends MdecInputStream {

    private InputStream _inStream;

    public MdecInputStreamReader(String sFile) throws FileNotFoundException {
        _inStream = new FileInputStream(sFile);
    }

    public MdecInputStreamReader(File file) throws FileNotFoundException {
        _inStream = new FileInputStream(file);
    }

    public MdecInputStreamReader(InputStream is) {
        _inStream = is;
    }

    @Override
    public boolean readMdecCode(MdecCode code) throws DecodingException, EOFException {
        try {
            code.set(IO.readSInt16LE(_inStream));
            return code.isEOD();
        } catch (EOFException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new DecodingException(ex);
        }
    }

    public static void writeMdecDims(MdecInputStream mdecIn, OutputStream streamOut,
            int iWidth, int iHeight)
            throws DecodingException, IOException
    {
        int iBlockCount = ((iWidth + 15) / 16) * ((iHeight + 15) / 16) * 6;
        writeMdecBlocks(mdecIn, streamOut, iBlockCount);
    }
    public static void writeMdecBlocks(MdecInputStream mdecIn, OutputStream streamOut,
            int iBlockCount)
            throws DecodingException, IOException
    {
        MdecCode code = new MdecCode();
        int iBlock = 0;
        while (iBlock < iBlockCount) {
            if (mdecIn.readMdecCode(code)) {
                iBlock++;
            }
            IO.writeInt16LE(streamOut, code.toMdecWord());
        }
    }

}
