/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2018-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.aconcagua.SectorAconcaguaVideo;
import jpsxdec.modules.granturismo.SectorGTVideo;
import jpsxdec.modules.square.SectorChronoXVideoNull;
import jpsxdec.modules.square.SectorFF8;
import jpsxdec.modules.square.SectorFF9;
import jpsxdec.modules.strvideo.GenericStrVideoSector;
import jpsxdec.modules.strvideo.SectorAliceNullVideo;
import jpsxdec.modules.strvideo.SectorAliceVideo;
import jpsxdec.modules.strvideo.SectorStarbladeAlphaGalaxian3;

/** Shared place for all modules to register order sensitive
 *  video sector identification, and related sector types. */
public class VideoSectorIdentifier {

    /** Tries to identify if the claimable sector is a video sector, or another
     * video related sector. If it is identified, the sector is claimed. If it
     * is a video sector, it will be returned. If the sector was not identified,
     * or it was identified but was not a video sector, null will be returned. */
    public static @CheckForNull ISelfDemuxingVideoSector idAndClaim(
            @Nonnull SectorClaimSystem.ClaimableSector cs)
    {
        CdSector cdSector = cs.getSector();
        ISelfDemuxingVideoSector vid;

        if ((vid = isVideo(new SectorFF8.SectorFF8Video(cdSector), cs)) != null) return vid;
        if ((vid = isVideo(new SectorFF9.SectorFF9Video(cdSector), cs)) != null) return vid;
        if ((vid = isVideo(new SectorGTVideo(cdSector), cs)) != null) return vid;
        if ((vid = isVideo(new SectorAconcaguaVideo(cdSector), cs)) != null) return vid;
        if ((vid = isVideo(new SectorStarbladeAlphaGalaxian3(cdSector), cs)) != null) return vid;
        if (isMatch(new SectorChronoXVideoNull(cdSector), cs)) return null;

        if ((vid = isVideo(new GenericStrVideoSector(cdSector), cs)) != null) return vid;

        // special handling for Alice
        SectorAliceNullVideo an;
        if ((an = new SectorAliceNullVideo(cdSector)).getProbability() > 0) {
            if ((vid = isVideo(new SectorAliceVideo(cdSector), cs)) != null) return vid;
            cs.claim(an);
            return null;
        }

        return null;
    }

    private static @CheckForNull ISelfDemuxingVideoSector isVideo(
            @Nonnull ISelfDemuxingVideoSector videoSector,
            @Nonnull SectorClaimSystem.ClaimableSector cs)
    {
        if (videoSector.getProbability() > 0) {
            cs.claim(videoSector);
            return videoSector;
        } else {
            return null;
        }
    }

    private static boolean isMatch(@Nonnull IIdentifiedSector otherSector,
                                   @Nonnull SectorClaimSystem.ClaimableSector cs)
    {
        if (otherSector.getProbability() > 0) {
            cs.claim(otherSector);
            return true;
        } else {
            return false;
        }
    }

}
