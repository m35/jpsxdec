/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.video.encode;

import java.io.IOException;
import jpsxdec.cdreaders.CDFileSectorReader;

public class SectorCopy {

    public static void main(String[] args) throws IOException {
        
        if (args.length != 3) 
            throw new IllegalArgumentException("Need 3 args: <source file> <dest CD image> <dest sector start>");
        
        CDFileSectorReader oSrc = new CDFileSectorReader(args[0]);
        CDFileSectorReader oDest = new CDFileSectorReader(args[1], true);
        int iStartSector = Integer.parseInt(args[2]);
        
        if (oSrc.size() > oDest.size())
            throw new IllegalArgumentException("Source file is larger than dest file");
        
        if (iStartSector + oSrc.size() > oDest.size())
            throw new IllegalArgumentException("Source file will run off the end of dest file");
        
        for (int iOfsSect = 0; iOfsSect < oSrc.size(); iOfsSect++) {
            byte[] abSrcUserData = oSrc.getSector(iOfsSect).getCdUserDataCopy();
            System.out.println("Overriting sector " + (iStartSector + iOfsSect));
            oDest.writeSector(iStartSector + iOfsSect, abSrcUserData);
        }
        
        System.out.println(oSrc.size() + " sectors overwritten.");
        
        oSrc.close();
        oDest.close();
        
    }


 
}
