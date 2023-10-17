/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

import java.util.logging.Level;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.util.DemuxedData;

/** Replace frames that are based on sectors
 * (as opposed to frames not based on sectors). */
public class SectorBasedFrameReplace {

    public interface IReplaceableVideoSector extends DemuxedData.Piece {
        int getVideoSectorHeaderSize();

        /** After analyzing the new demux data, updates the abCurrentVidSectorHeader
         * to match the demux data information.
         * @param abNewDemuxData The entire demux bitstream of new frame
         * @param iNewUsedSize The number of bytes the new demux data uses
         * @param iNewMdecCodeCount The number of MDEC codes found in the new demux data
         * @param abCurrentVidSectorHeader The video sector header found in this very sector,
         *                                 already extracted for your convenience
         *                                 TODO: probably should let the sector itself extract the header and have it run it
         * @throws LocalizedIncompatibleException if the new demux data is incompatible with this sector type
         */
        void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                      @Nonnull BitStreamAnalysis newFrame,
                                      @Nonnull byte[] abCurrentVidSectorHeader)
                throws LocalizedIncompatibleException;

        /** Returns the source CD sector behind this piece. */
        @Nonnull CdSector getCdSector();
    }

    public static void writeToSectors(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                      @Nonnull BitStreamAnalysis newFrame,
                                      @Nonnull DiscPatcher patcher,
                                      @Nonnull ILocalizedLogger log,
                                      @Nonnull Iterable<? extends IReplaceableVideoSector> chunks)
            throws LoggedFailure
    {
        int iDemuxOfs = 0;
        for (IReplaceableVideoSector vidSector : chunks) {
            if (vidSector == null) {
                log.log(Level.WARNING, I.CMD_FRAME_TO_REPLACE_MISSING_CHUNKS());
                continue;
            }
            byte[] abSectUserData = vidSector.getCdSector().getCdUserDataCopy();
            try {
                vidSector.replaceVideoSectorHeader(existingFrame, newFrame, abSectUserData);
            } catch (LocalizedIncompatibleException ex) {
                throw new LoggedFailure(log, Level.SEVERE, ex.getSourceMessage(), ex);
            }
            int iBytesToCopy = vidSector.getDemuxPieceSize();
            if (iDemuxOfs + iBytesToCopy > newFrame.getBitStreamArrayLength())
                iBytesToCopy = newFrame.getBitStreamArrayLength() - iDemuxOfs;
            // bytes to copy might be 0, which is ok because we
            // still need to write the updated headers
            int iSectorHeaderSize = vidSector.getVideoSectorHeaderSize();
            newFrame.arrayCopy(iDemuxOfs, abSectUserData, iSectorHeaderSize, iBytesToCopy);
            try {
                patcher.addPatch(vidSector.getCdSector().getSectorIndexFromStart(), 0, abSectUserData);
            } catch (DiscPatcher.WritePatchException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(ex.getFile().toString()), ex);
            }
            iDemuxOfs += iBytesToCopy;
        }
    }

}
