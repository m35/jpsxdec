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

package jpsxdec.psxvideo.mdec;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/** Wraps an InputStream (or creates a FileInputStream) to read MDEC values from. */
public class MdecInputStreamReader implements MdecInputStream {

    private static final Logger LOG = Logger.getLogger(MdecInputStreamReader.class.getName());

    @Nonnull
    private final ByteArrayInputStream _in;

    public MdecInputStreamReader(@Nonnull File file) throws FileNotFoundException, IOException {
        this(IO.readFile(file));
    }

    public MdecInputStreamReader(@Nonnull InputStream is) throws IOException {
        this(IO.readEntireStream(is));
    }

    public MdecInputStreamReader(@Nonnull byte[] abMdecWords) {
        _in = new ByteArrayInputStream(abMdecWords);
    }

    @Override
    public boolean readMdecCode(@Nonnull MdecCode code) throws MdecException.EndOfStream {
        try {
            code.set(IO.readSInt16LE(_in));
            return code.isEOD();
        } catch (EOFException ex) {
            throw new MdecException.EndOfStream(ex);
        } catch (IOException ex) {
            throw new RuntimeException("BAOS should not throw a general IOException", ex);
        }
    }

    public void reset() {
        _in.reset();
    }

    // -------------------------------------------------------------------------

    /** Writes a dimensions worth of macro blocks from an
     * {@link MdecInputStream} to an {@link OutputStream}. */
    public static void writeMdecDims(@Nonnull MdecInputStream mdecIn,
                                     @Nonnull OutputStream streamOut,
                                     int iWidth, int iHeight)
            throws MdecException.EndOfStream, MdecException.ReadCorruption, IOException
    {
        int iBlockCount = Calc.blocks(iWidth, iHeight);
        writeMdecBlocks(mdecIn, streamOut, iBlockCount);
    }
    /** Writes a number of blocks from an
     * {@link MdecInputStream} to an {@link OutputStream}.
     * Errors are monitored, but does not throw exceptions on bad data
     * (although source and destination streams might). */
    public static void writeMdecBlocks(@Nonnull MdecInputStream sourceMdecIn,
                                       @Nonnull OutputStream streamOut,
                                       int iBlockCount)
            throws MdecException.EndOfStream, MdecException.ReadCorruption, IOException
    {
        Ac0Checker mdecIn = Ac0Checker.wrapWithChecker(sourceMdecIn, false);

        MdecCode code = new MdecCode();
        int iBlock = 0;
        while (iBlock < iBlockCount) {
            // read Qscale+DC
            boolean blnIsEod = mdecIn.readMdecCode(code);
            if (blnIsEod)
                LOG.warning("Qscale+DC code returns as EOD!");
            if (code.isEOD())
                LOG.warning("Qscale+DC = EOD!");
            IO.writeInt16LE(streamOut, code.toMdecShort());
            if (!blnIsEod) {
                int iCurrentBlockVectorPosition = 0;
                while (!mdecIn.readMdecCode(code)) {
                    if (code.isEOD())
                        LOG.warning("EOD code found in at non-EOD position!");
                    iCurrentBlockVectorPosition += code.getTop6Bits() + 1;
                    if (iCurrentBlockVectorPosition >= 64)
                        LOG.warning("Run length out of bounds!");
                    IO.writeInt16LE(streamOut, code.toMdecShort());
                }
            }
            iBlock++;
            if (!code.isEOD())
                LOG.warning("non-EOD code found in at EOD position!");
            IO.writeInt16LE(streamOut, code.toMdecShort());
        }

        mdecIn.logIfAny0AcCoefficient();
    }

}
