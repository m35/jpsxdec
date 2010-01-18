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

package jpsxdec.plugins;

import jpsxdec.cdreaders.CDSector;

/** Base class for all identified sector types. Encapsulates CD sectors with
 *  special meaning. */
public abstract class IdentifiedSector implements IIdentifiedSector {

    private CDSector m_oSourceCDSector;

    
    public IdentifiedSector(CDSector cdSector) {
        m_oSourceCDSector = cdSector;
        
    }

    /** Returns a string description of the sector type. */
    public String toString() {
        return m_oSourceCDSector.toString();
    }
    
    /** The 'file' value in the raw CD header, or -1 if there was no header. */
    public long getFile() {
        return m_oSourceCDSector.getFile();
    }
    
    /** @return The 'channel' value in the raw CDXA header, 
     *          or -1 if there was no header, or if it is a 'NULL' sector
     *          (overridden by PSXSectorNull).*/
    public int getChannel() {
        return m_oSourceCDSector.getChannel();
    }

    public boolean getEOFBit() {
        if (m_oSourceCDSector.hasSectorHeader())
            return m_oSourceCDSector.getSubMode().getEofMarker();
        else
            return false;
    }

    /** @return The sector number from the start of the source file. */
    public int getSectorNumber() {
        return m_oSourceCDSector.getSectorNumberFromStart();
    }
    
    public CDSector getCDSector() {
        return m_oSourceCDSector;
    }

}


