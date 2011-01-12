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

package jpsxdec.sectors;

import java.io.PrintStream;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.ByteArrayFPIS;

public interface IIdentifiedSector {
    
    public static final int SECTOR_UNKNOWN = 0;
    public static final int SECTOR_VIDEO = 1;
    public static final int SECTOR_AUDIO = 2;
    public static final int SECTOR_ISO9660_VPD = 8;
    public static final int SECTOR_ISO9660_DR = 16;
    public static final int SECTOR_ISO9660_PT = 32;
    
    /** @return The sector type of the sector (SECOTR_*). */
    public int getSectorType();
    
    /** @return Human readable sector type (used for reference and debugging).*/
    public String getTypeName();
    
    /** @return The size of the sector's payload, excluding any of the PSX (or otherwise)
     *  sector's specific headers or footers. */
    public int getIdentifiedUserDataSize();

    /** @return A stream of the sector's user data payload. */
    public ByteArrayFPIS getIdentifiedUserDataStream();
    
    /** @return The 'channel' value in the raw CDXA header, 
     *          or -1 if there is no header. */
    public int getChannel();

    /** @return The sector offset from the start of the file. */
    public int getSectorNumber();

    public CdSector getCDSector();

    public int getErrorCount();

    public void printErrors(PrintStream ps);
}
