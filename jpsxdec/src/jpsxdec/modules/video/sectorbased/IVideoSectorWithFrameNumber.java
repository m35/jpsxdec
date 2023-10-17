/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2014-2023  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;

/** The vast majority of video sector formats have frame number. */
public interface IVideoSectorWithFrameNumber extends ISelfDemuxingVideoSector {

    /** Height of the frame in pixels. */
    int getHeight();

    /** Width of the frame in pixels. */
    int getWidth();

    /** Frame number that this sector is a part of, found in the sector header. */
    int getHeaderFrameNumber();

    /** Number of video sectors used to hold the frame that this sector
     *  is a part of. */
    int getChunksInFrame();

    /** Where this sector falls in the order of video sectors for this frame. */
    int getChunkNumber();

    int getVideoSectorHeaderSize();

    /** Returns the size of the sector's payload, excluding any of the PSX (or otherwise)
     *  sector's specific headers or footers. */
    int getDemuxPieceSize();

    byte getDemuxPieceByte(int i);

    /** Copies the identified user data portion of the sector data to the
     *  output buffer. */
    void copyDemuxPieceData(@Nonnull byte[] abOut, int iOutPos);

    void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                  @Nonnull BitStreamAnalysis newFrame,
                                  @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException;

    @Override
    @Nonnull VideoSectorWithFrameNumberDemuxer createDemuxer(@Nonnull ILocalizedLogger log);

}
