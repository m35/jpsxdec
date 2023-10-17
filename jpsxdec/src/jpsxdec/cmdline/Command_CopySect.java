/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2023  Michael Sabin
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

package jpsxdec.cmdline;

import argparser.BooleanHolder;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.CdRiffHeader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.util.ArgParser;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;


/** Command to copy sectors out of a disc image.
 * <pre>
 * -copysect {@code <start sector>}-{@code <end sector>}
 * </pre>
 */
class Command_CopySect extends Command {

    private static final Logger LOG = Logger.getLogger(Command_CopySect.class.getName());

    public Command_CopySect() {
        super("-copysect");
    }
    @Nonnull
    private int[] _aiStartEndSectors;

    @Override
    protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
        _aiStartEndSectors = parseNumberRange(s);
        if (_aiStartEndSectors == null) {
            return I.CMD_INVALID_VALUE_FOR_CMD(s, "-copysect");
        } else {
            return null;
        }
    }

    @Override
    public void execute(@Nonnull ArgParser ap) throws CommandLineException {
        ICdSectorReader cdReader = getCdReader();
        String sOutputFile = String.format("%s%d-%d.dat",
                Misc.removeExt(cdReader.getSourceFile().getName()),
                _aiStartEndSectors[0], _aiStartEndSectors[1]);

        _fbs.println(I.CMD_COPYING_SECTOR(_aiStartEndSectors[0], _aiStartEndSectors[1], sOutputFile));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(sOutputFile);
        } catch (FileNotFoundException ex) {
            throw new CommandLineException(I.IO_OPENING_FILE_NOT_FOUND_NAME(sOutputFile), ex);
        }

        OutputStream os = null;
        try {
            os = new BufferedOutputStream(fos);
            int iRawSectorSize = cdReader.getRawSectorSize();
            boolean blnAddCdxaHeader;
            if (iRawSectorSize == CdSector.SECTOR_SIZE_2048_ISO) {
                blnAddCdxaHeader = false;
            } else {
                if (!ap.hasRemaining()) {
                    blnAddCdxaHeader = true;
                } else {
                    BooleanHolder noCdxaHeader = ap.addBoolOption(false, "-nocdxa");
                    ap.match();
                    blnAddCdxaHeader = !noCdxaHeader.value;
                }
            }
            if (blnAddCdxaHeader) {
                long lngFileSize = (_aiStartEndSectors[1] - _aiStartEndSectors[0] + 1) * (long) iRawSectorSize;
                CdRiffHeader.write(os, lngFileSize);
            }
            for (int i = _aiStartEndSectors[0]; i <= _aiStartEndSectors[1]; i++) {
                CdSector sector = cdReader.getSector(i);
                os.write(sector.getRawSectorDataCopy());
            }
        } catch (CdException.Read ex) {
            throw new CommandLineException(I.IO_READING_FROM_FILE_ERROR_NAME(
                                           ex.getFile().toString()), ex);
        } catch (IOException ex) {
            throw new CommandLineException(I.IO_WRITING_TO_FILE_ERROR_NAME(sOutputFile), ex);
        } finally {
            IO.closeSilently(os, LOG);
        }
    }

    /** Parse a number range. e.g. 5-10
     * @return Array of 2 elements, or null on error. */
    private static @CheckForNull int[] parseNumberRange(@Nonnull String s) {
        int iStart, iEnd;
        String[] split = s.split("-");
        try {

            if (split.length == 2) {
                iStart = Integer.parseInt(split[0]);
                iEnd = Integer.parseInt(split[1]);
            } else {
                iStart = iEnd = Integer.parseInt(s);
            }

            return new int[] {iStart, iEnd};

        } catch (NumberFormatException ex) {
            return null;
        }
    }




}
