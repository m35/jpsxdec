/*
 * LainTools: PSX Serial Experiments Lain Hacking and Translation Tools
 * Copyright (C) 2011  Michael Sabin
 *
 * Redistribution and use of the LainTools code or any derivative works are
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

package laintools;

import java.io.File;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdOpener;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.i18n.log.ConsoleProgressLogger;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.modules.iso9660.DiscItemISO9660File;

public class PatchLainImage {

    public static void main(String[] args) throws Exception {
        

        if (args.length < 3) {
            System.out.println("Requires at least 3 arguments");
            System.out.println("   <jpsxdec index of the disc image to modify>");
            System.out.println("   <SLPS file to insert>");
            System.out.println("   <SITE*.BIN file to insert>");
            System.out.println("   [BIN.BIN file to insert]");
        }
        String sIndex = args[0],
                sSlps = args[1],
                sSite = args[2];
        String sBin=null;
        if (args.length > 3) {
            sBin = args[3];
        }        
        
        DiscIndex index = new DiscIndex(sIndex, DebugLogger.Log);

        DiscPatcher patcher = new DiscPatcher();
        DiscItem item = index.getById("SLPS_016.03");
        if (item == null)
            item = index.getById("SLPS_016.04");
        replaceFile((DiscItemISO9660File)item, patcher, sSlps);
        
        item = index.getById("SITEA.BIN");
        if (item == null)
            item = index.getById("SITEB.BIN");
        replaceFile((DiscItemISO9660File)item, patcher, sSite);

        if (sBin != null)
            replaceFile((DiscItemISO9660File)index.getById("BIN.BIN"), patcher, sBin);

        patcher.applyPatches((CdFileSectorReader) index.getSourceCd(), new ConsoleProgressLogger(PatchLainImage.class.getSimpleName(),
                                                                            System.out));
    }

    private static void replaceFile(DiscItemISO9660File isoFile, DiscPatcher patcher, String sReplaceFile) throws Exception {
        System.out.println("Replacing " + sReplaceFile);

        File replaceFile = new File(sReplaceFile);
        ICdSectorReader replaceDisc = CdOpener.openWithSectorSize(replaceFile, 2048);
        if (replaceDisc.getSectorCount() > isoFile.getSectorLength())
            throw new RuntimeException(sReplaceFile + " too big to fit " + isoFile);

        for (int iOfsSect = 0; iOfsSect < replaceDisc.getSectorCount(); iOfsSect++) {
            byte[] abSrcUserData = replaceDisc.getSector(iOfsSect).getCdUserDataCopy();
            //System.out.println("Overwriting sector " + (isoFile.getStartSector() + iOfsSect));
            patcher.addPatch(isoFile.getStartSector() + iOfsSect, 0, abSrcUserData);
        }

        replaceDisc.close();
    }

}
