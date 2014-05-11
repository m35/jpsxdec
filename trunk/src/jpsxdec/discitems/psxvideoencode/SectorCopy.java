/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

package jpsxdec.discitems.psxvideoencode;

import java.io.File;
import java.io.IOException;
import jpsxdec.cdreaders.CdFileSectorReader;

public class SectorCopy {

    public static void main(String[] args) throws IOException {

        if (args.length != 3)
            throw new IllegalArgumentException("Need 3 args: <source file> <dest CD image> <dest sector start>");

        sectorCopy(args[0], args[1], Integer.parseInt(args[2]));
    }
    public static void sectorCopy(String sSource, String sDest, int iStartSector) throws IOException {
        
        CdFileSectorReader src = new CdFileSectorReader(new File(sSource));
        CdFileSectorReader dest = new CdFileSectorReader(new File(sDest), true);
        
        if (src.getLength() > dest.getLength())
            throw new IllegalArgumentException("Source file is larger than dest file");
        
        if (iStartSector + src.getLength() > dest.getLength())
            throw new IllegalArgumentException("Source file will run off the end of dest file");
        
        for (int iOfsSect = 0; iOfsSect < src.getLength(); iOfsSect++) {
            byte[] abSrcUserData = src.getSector(iOfsSect).getCdUserDataCopy();
            System.out.println("Overriting sector " + (iStartSector + iOfsSect));
            dest.writeSector(iStartSector + iOfsSect, abSrcUserData);
        }
        
        System.out.println(src.getLength() + " sectors overwritten.");
        
        src.close();
        dest.close();
        
    }


 
}
