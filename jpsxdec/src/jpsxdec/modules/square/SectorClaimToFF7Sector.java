/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.square;

import java.io.IOException;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.util.IOIterator;

/** FF7 sectors have so much random data in the header it's hard to distinguish
 * it from other sector types. One thing that is unique to FF7 frames is they
 * all start with 40 bytes of unknown data before the actual frame starts.
 * This claimer takes advantage of that to do some contextual identification
 * of FF7 sectors.
 * This means that this claimer needs to come before general video sector claimers.
 */
public class SectorClaimToFF7Sector implements SectorClaimSystem.SectorClaimer {

    @Override
    public void sectorRead(@Nonnull SectorClaimSystem.ClaimableSector cs,
                           @Nonnull IOIterator<SectorClaimSystem.ClaimableSector> peekIt,
                           @Nonnull ILocalizedLogger log)
            throws IOException
    {
        if (cs.isClaimed()) {
            // either something else claimed it
            // or this claimed it already
            // either way, skip
            return;
        }

        // Look for first chunk
        SectorFF7Video firstFF7 = new SectorFF7Video(cs.getSector());
        if (firstFF7.getProbability() == 0 || firstFF7.getChunkNumber() != 0)
            return;

        // found first chunk

        ArrayList<SectorClaimSystem.ClaimableSector> claimSects = new ArrayList<SectorClaimSystem.ClaimableSector>();
        ArrayList<SectorFF7Video> ff7Sects = new ArrayList<SectorFF7Video>();
        claimSects.add(cs);
        ff7Sects.add(firstFF7);

        while (true) {
            if (!peekIt.hasNext())
                return; // end of sectors without finishing the frame

            SectorClaimSystem.ClaimableSector nextCs = peekIt.next();
            if (nextCs.isClaimed())
                continue; // skip claimed sectors (probably XA sectors)

            SectorFF7Video nextFF7 = new SectorFF7Video(nextCs.getSector());
            if (nextFF7.getProbability() == 0)
                continue; // skip non FF7 possible sectors

            if (nextFF7.getSectorNumber() > firstFF7.getSectorNumber() + 20)
                return; // too many sectors have passed, fail

            if (nextFF7.getHeaderFrameNumber() != firstFF7.getHeaderFrameNumber() ||
                nextFF7.getWidth() != firstFF7.getWidth() ||
                nextFF7.getHeight() != firstFF7.getHeight() ||
                nextFF7.getChunksInFrame() != firstFF7.getChunksInFrame())
                return; // sector not part of the frame? how did that happen? fail

            if (nextFF7.getChunkNumber() != ff7Sects.size())
                return; // chunk number not in sequence? fail

            ff7Sects.add(nextFF7);
            claimSects.add(nextCs);

            if (nextFF7.getChunkNumber() + 1 == nextFF7.getChunksInFrame())
                break; // done!
        }

        // claim all the sectors
        for (int i = 0; i < claimSects.size(); i++) {
            SectorClaimSystem.ClaimableSector ff7ClaimableSector = claimSects.get(i);
            ff7ClaimableSector.claim(ff7Sects.get(i));
        }
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
    }

}
