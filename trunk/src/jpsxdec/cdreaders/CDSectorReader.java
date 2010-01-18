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

package jpsxdec.cdreaders;

import java.io.IOException;

/** Encapsulates the reading of a CD. 
 *  The term "CD" could mean an actual CD, a CD image 
 *  (BIN/CUE, ISO), or a file containing some (possibly raw) sectors of a CD. 
 *  The resulting data is mostly the same. */
public abstract class CDSectorReader {
    
    /** Factory to return either a image file, or a CD drive reader
     *  (drive not currently implemented). */
    public static CDSectorReader open(String sFile) throws IOException {
        return new CDFileSectorReader(sFile);
    }
    
    /** Returns true if the 'CD' has raw sector headers. The raw headers
     *  are necessary for decoding Mode 2 Form 2 sectors, which is most
     *  often used for audio. */
    abstract public boolean hasSectorHeader();

    /** Close the CD. */
    abstract public void close() throws IOException;

    /** Returns the actual offset in bytes from the start of the file/CD
     *  to the start of iSector. */
    abstract public long getFilePointer(int iSector);

    /** Returns the requested sector. */
    abstract public CDSector getSector(int iSector) throws IOException, IndexOutOfBoundsException;

    /** Returns the name of the CD image, or title of the CD in the drive. */
    abstract public String getSourceFile();

    abstract public String getSourceFileBaseName();

    /** Returns the size of the file/disc in sectors. */
    abstract public int size();

    abstract public String serialize();

    abstract public String getTypeDescription();
}