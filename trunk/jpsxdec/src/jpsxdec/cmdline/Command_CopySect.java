/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2016  Michael Sabin
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

import argparser.ArgParser;
import argparser.BooleanHolder;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdxaRiffHeader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.util.Misc;


/** Command to copy sectors out of a disc image. */
class Command_CopySect extends Command {

    private static final Logger LOG = Logger.getLogger(Command_CopySect.class.getName());

    public Command_CopySect() {
        super("-copysect");
    }
    @Nonnull
    private int[] _aiStartEndSectors;

    protected @CheckForNull ILocalizedMessage validate(@Nonnull String s) {
        _aiStartEndSectors = parseNumberRange(s);
        if (_aiStartEndSectors == null) {
            return I.CMD_SECTOR_RANGE_INVALID(s);
        } else {
            return null;
        }
    }

    public void execute(@CheckForNull String[] asRemainingArgs) throws CommandLineException {
        CdFileSectorReader cdReader = getCdReader();
        String sOutputFile = String.format("%s%d-%d.dat",
                Misc.removeExt(cdReader.getSourceFile().getName()),
                _aiStartEndSectors[0], _aiStartEndSectors[1]);
        _fbs.println(I.CMD_COPYING_SECTOR(_aiStartEndSectors[0], _aiStartEndSectors[1], sOutputFile));
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(sOutputFile));
            if (cdReader.getSectorSize() == CdFileSectorReader.SECTOR_SIZE_2352_BIN && asRemainingArgs != null)
            {
                ArgParser parser = new ArgParser("", false);
                BooleanHolder noCdxaHeader = new BooleanHolder(false);
                parser.addOption("-nocdxa %v", noCdxaHeader);
                parser.matchAllArgs(asRemainingArgs, 0, 0);
                if (!noCdxaHeader.value) {
                    long lngFileSize = (_aiStartEndSectors[1] - _aiStartEndSectors[0] + 1) * (long) 2352;
                    CdxaRiffHeader.write(os, lngFileSize);
                }
            }
            for (int i = _aiStartEndSectors[0]; i <= _aiStartEndSectors[1]; i++) {
                CdSector sector = cdReader.getSector(i);
                os.write(sector.getRawSectorDataCopy());
            }
        } catch (IOException ex) {
            throw new CommandLineException(ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
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
