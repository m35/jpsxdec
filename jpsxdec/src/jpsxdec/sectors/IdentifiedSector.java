/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2015  Michael Sabin
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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;

/** Abstract base class for all identified sector types. Encapsulates CD sectors
 *  with special meaning. */
public abstract class IdentifiedSector implements IIdentifiedSector {

    /** Attempts to identify the sector.
     * @return null if sector could not be identified. */
    public static @CheckForNull IdentifiedSector identifySector(@Nonnull CdSector cdSector) {
        IdentifiedSector s;
        // sorted in order of likelyhood of encountering (my best guess)
        if ((s = new SectorXaAudio(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorXaNull(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorStrVideo(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorISO9660DirectoryRecords(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorISO9660VolumePrimaryDescriptor(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorCdAudio(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorFF8.SectorFF8Video(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorFF8.SectorFF8Audio(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorFF9.SectorFF9Video(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorFF9.SectorFF9Audio(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorIkiVideo(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorChronoXAudio(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorChronoXVideo(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorChronoXVideoNull(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorAceCombat3Video(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorLainVideo(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorDreddVideo(cdSector)).getProbability() > 0) return s;
        if ((s = new SectorCrusader(cdSector)).getProbability() > 0) return s;

        // FF7 has such a vague header, it can easily be falsely identified
        // when it should be one of the headers above
        if ((s = new SectorFF7Video(cdSector)).getProbability() > 0) return s;
        
        // special handling for Alice
        SectorAliceNullVideo nullAlice = new SectorAliceNullVideo(cdSector);
        if (nullAlice.getProbability() > 0) {
            s = new SectorAliceVideo(cdSector);
            if (s.getProbability() > 0)
                return s;
            else
                return nullAlice;
        }
        
        return null;
    }

    @Nonnull
    private final CdSector _sourceCdSector;
    private int _iProbability = 1;
    
    public IdentifiedSector(@Nonnull CdSector cdSector) {
        _sourceCdSector = cdSector;
    }

    /** Returns true if the super-class is definitely not a match (probability=0).
     * If it returns false, then it also resets probability to 0 for the child-class
     * to determine its own probability. */
    protected boolean isSuperInvalidElseReset() {
        if (_iProbability == 0)
            return true;
        _iProbability = 0;
        return false;
    }
    /** Between 0 and 100. */
    protected void setProbability(int iProbability) {
        _iProbability = iProbability;
    }

    public int getProbability() {
        return _iProbability;
    }

    /** Returns a string description of the sector type. */
    public String toString() {
        return _sourceCdSector.toString();
    }
    
    /** @return The sector number from the start of the source file. */
    public int getSectorNumber() {
        return _sourceCdSector.getSectorNumberFromStart();
    }
    
    public @Nonnull CdSector getCdSector() {
        return _sourceCdSector;
    }

    protected String cdToString() {
        return _sourceCdSector.toString();
    }

    public int getErrorCount() {
        return 0;
    }

    public void printErrors(PrintStream ps) {
    }
}


