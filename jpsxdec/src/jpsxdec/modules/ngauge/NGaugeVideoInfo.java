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

package jpsxdec.modules.ngauge;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;

/**
 * Video information for the game "N-Gauge Unten Kibun Game - Gatan Goton"
 * found in the sector preceeding the video frame sectors.
 *
 * Translation:
 *
 * "N gauge refers to the track dimensions"
 * (https://en.wikipedia.org/wiki/N_scale)
 *
 * Unten Kibun = "driving mood" or "operation feeling"
 * Gatan = *clangity clang*
 * Goton = *clickety-clack*
 *
 * The game simulates driving a train.
 *
 * The massive video containing all the train tracks begins with a sector
 * containing information about the video. This class captures that info. This
 * header sector is very reliably identified so can/should be identified before
 * most other sector types.
 *
 * The video is not played back in the standard "real-time" mode (as defined in
 * the CD standards). Instead the frame rate of the video is changed dynamically
 * by the game to simulate the train speeding up and slowing down. In this sense
 * the the video is technically "packet-based", but since the packets align with
 * sector boundaries it still fits in the "sector-based" style, and also allows
 * for frame replacing.
 *
 * The game dynamically starts and ends the video at different frames for the
 * different tracks, but there's no information in the sectors to indicate where
 * these start/end points are.
 */
public class NGaugeVideoInfo {

    public static final int SECTORS_PER_FRAME = 3;
    public static final int FRAME_COUNT_PLUS_1 = 51423;
    public static final int WIDTH = 160;
    public static final int HEIGHT = 112;

    /** Checks if the sector starts with a valid N Gauge header.
     * @return null if not */
    public static @CheckForNull NGaugeVideoInfo checkForInfoHeader(@Nonnull CdSector cdSector) {
        if (cdSector.isCdAudioSector())
            return null;
        if (cdSector.getCdUserDataSize() != CdSector.SECTOR_USER_DATA_SIZE_MODE1_MODE2FORM1)
            return null;

        NGaugeVideoInfo vi = new NGaugeVideoInfo(cdSector);
        if (!vi.isValid())
            return null;
        return vi;
    }

    public final int iSectorsPerFrame;
    public final int iFrameCountPlus1;
    public final int iWidth;
    public final int iHeight;

    public final int iDiscSpeed = 1;

    public final int iVideoStartSector;
    public final int iFirstFrameStartSector;
    public final int iEndSectorInclusive;

    private NGaugeVideoInfo(@Nonnull CdSector cdSector) {
        iSectorsPerFrame = cdSector.readSInt32LE(0);
        iFrameCountPlus1 = cdSector.readSInt32LE(4);
        iWidth = cdSector.readSInt32LE(8);
        iHeight = cdSector.readSInt32LE(12);

        iVideoStartSector = cdSector.getSectorIndexFromStart();
        iFirstFrameStartSector = iVideoStartSector + 1;
        iEndSectorInclusive = iFirstFrameStartSector + (FRAME_COUNT_PLUS_1 - 1) * SECTORS_PER_FRAME - 1;
    }

    private boolean isValid() {
        return iSectorsPerFrame == SECTORS_PER_FRAME &&
               iFrameCountPlus1 == FRAME_COUNT_PLUS_1 &&
               iWidth == WIDTH && iHeight == HEIGHT;
    }

    public int calculateFrameNumber(int iSectorNumber) {
        if (iSectorNumber < iFirstFrameStartSector || iSectorNumber > iEndSectorInclusive)
            throw new IllegalArgumentException();
        return (iSectorNumber - iFirstFrameStartSector) / iSectorsPerFrame;
    }

    public int calculateChunkNumber(int iSectorNumber) {
        if (iSectorNumber < iFirstFrameStartSector || iSectorNumber > iEndSectorInclusive)
            throw new IllegalArgumentException();
        return (iSectorNumber - iFirstFrameStartSector) % iSectorsPerFrame;
    }

    @Override
    public String toString() {
        return String.format("sectors/frame %d count %d %dx%d %d-%d",
                             iSectorsPerFrame, iFrameCountPlus1, iWidth, iHeight, iFirstFrameStartSector, iEndSectorInclusive);
    }

}
