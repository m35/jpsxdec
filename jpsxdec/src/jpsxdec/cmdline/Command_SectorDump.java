/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2014  Michael Sabin
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

import java.io.IOException;
import java.io.PrintStream;
import jpsxdec.I18N;
import jpsxdec.LocalizedMessage;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.sectors.IdentifiedSector;


class Command_SectorDump extends Command {

    private String _sOutfile;

    public Command_SectorDump() {
        super("-sectordump");
    }

    protected LocalizedMessage validate(String s) {
        _sOutfile = s;
        return null;
    }

    public void execute(String[] asRemainingArgs) throws CommandLineException {
        CdFileSectorReader cdReader = getCdReader();
        _fbs.println(I18N.S("Generating sector list")); // I18N
        PrintStream ps = null;
        try {
            if (_sOutfile.equals("-")) {
                ps = System.out;
            } else {
                ps = new PrintStream(_sOutfile);
            }
            for (int i = 0; i < cdReader.getLength(); i++) {
                CdSector cdSect = cdReader.getSector(i);
                IdentifiedSector idSect = IdentifiedSector.identifySector(cdSect);
                if (idSect != null) {
                    String s = idSect.toString();
                    ps.println(s);
                } else {
                    ps.println(cdSect.toString());
                }
            }
        } catch (IOException ex) {
            throw new CommandLineException(ex);
        } finally {
            if (ps != null) {
                ps.flush();
                if (!ps.equals(System.out)) {
                    ps.close();
                }
            }
        }
    }

}
