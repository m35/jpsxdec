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

import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.NotThisTypeException;

/** Base class for all identified sector types. Encapsulates CD sectors with
 *  special meaning. */
public abstract class IdentifiedSector implements IIdentifiedSector {

    public static IdentifiedSector identifySector(CdSector cdSector) {
        // sorted in order of likelyhood of encountering (my best guess)
        try { return new SectorSTR(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorXA(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorXANull(cdSector); }
        catch (NotThisTypeException e) {}
        //try { return new SectorISO9660PathTable(cdSector); }
        //catch (NotThisTypeException e) {}
        try { return new SectorISO9660DirectoryRecords(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorISO9660VolumePrimaryDescriptor(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorFF7Video(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorFF8.SectorFF8Video(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorFF8.SectorFF8Audio(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorFF9.SectorFF9Video(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorFF9.SectorFF9Audio(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorChronoXAudio(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorChronoXVideo(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorChronoXVideoNull(cdSector); }
        catch (NotThisTypeException e) {}
        try { return new SectorLainVideo(cdSector); }
        catch (NotThisTypeException e) {}
        try {
            SectorAliceFrameChunkNull nullAlice = new SectorAliceFrameChunkNull(cdSector);
            try {
                return new SectorAliceFrameChunk(cdSector);
            } catch (NotThisTypeException ex) {
                return nullAlice;
            }
        } catch (NotThisTypeException e) {}
        return null;
    }

    private CdSector _sourceCdSector;

    
    public IdentifiedSector(CdSector cdSector) {
        _sourceCdSector = cdSector;
        
    }

    /** Returns a string description of the sector type. */
    public String toString() {
        return _sourceCdSector.toString();
    }
    
    /** The 'file' value in the raw CD header, or -1 if there was no header. */
    public long getFile() {
        return _sourceCdSector.getFile();
    }
    
    /** @return The 'channel' value in the raw CDXA header, 
     *          or -1 if there was no header, or if it is a 'NULL' sector
     *          (overridden by SectorXANull).*/
    public int getChannel() {
        return _sourceCdSector.getChannel();
    }

    /** @return The sector number from the start of the source file. */
    public int getSectorNumber() {
        return _sourceCdSector.getSectorNumberFromStart();
    }
    
    public CdSector getCDSector() {
        return _sourceCdSector;
    }

    protected String cdToString() {
        return _sourceCdSector.toString();
    }

}


