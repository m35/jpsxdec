/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.policenauts;

import java.util.Collection;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;

/** @see SPacket */
public class DiscIndexerPolicenauts extends DiscIndexer implements PolicenautsSectorToPacket.Listener {

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        PolicenautsSectorToPacket.attachToSectorClaimer(scs, this);
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        if (DiscItemPolicenauts.TYPE_ID.equals(fields.getType()))
            return new DiscItemPolicenauts(getCd(), fields);
        return null;
    }

    private class VidBuilder {
        @Nonnull
        private final Dimensions __dims;
        private final int __iStartKlbsStartSector;
        private int __iLastKlbsEndSector;
        @CheckForNull
        private IndexSectorFrameNumber.Format.Builder __indexSectorFrameNumberBuilder;
        @CheckForNull
        private HeaderFrameNumber.Format.Builder __headerFrameNumberBuilder;
        private int __iSoundUnitCount = 0;

        public VidBuilder(@Nonnull SPacketData firstPacket, @Nonnull Dimensions dims) {
            __dims = dims;
            __iStartKlbsStartSector = firstPacket.getKlbsStartSectorNum();
            __iLastKlbsEndSector = firstPacket.getKlbsEndSectorNum();
            addPacket(firstPacket);
        }

        final public void addPacket(@Nonnull SPacketData packet) {
            __iLastKlbsEndSector = packet.getKlbsEndSectorNum();

            if (packet.isAudio()) {
                __iSoundUnitCount += packet.getSoundUnitCount();
            } else if (packet.isVideo()) {
                if (__headerFrameNumberBuilder == null)
                    __headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(packet.getTimestamp());
                else
                    __headerFrameNumberBuilder.addHeaderFrameNumber(packet.getTimestamp());

                if (__indexSectorFrameNumberBuilder == null)
                    __indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(packet.getStartSector());
                else
                    __indexSectorFrameNumberBuilder.addFrameStartSector(packet.getStartSector());
            }
        }

        public void finishVid(ILocalizedLogger log) {
            if (__indexSectorFrameNumberBuilder == null) {
                log.log(Level.WARNING, I.POLICENAUTS_DATA_CORRUPTION());
                return;
            }
            DiscItemPolicenauts di = new DiscItemPolicenauts(getCd(),
                    __iStartKlbsStartSector, __iLastKlbsEndSector, __dims,
                    __indexSectorFrameNumberBuilder.makeFormat(),
                    __headerFrameNumberBuilder.makeFormat(),
                    __iSoundUnitCount);
            addDiscItem(di);
        }
    }

    @CheckForNull
    private VidBuilder _currentVid;
    @CheckForNull
    private Dimensions _dims;

    @Override
    public void videoStart(int iWidth, int iHeight, @Nonnull ILocalizedLogger log) {
        if (_currentVid != null) {
            _currentVid.finishVid(log);
            _currentVid = null;
        }
        _dims = new Dimensions(iWidth, iHeight);
    }

    @Override
    public void feedPacket(@Nonnull SPacketData packet, @Nonnull ILocalizedLogger log) {
        if (_dims == null) {
            log.log(Level.WARNING, I.POLICENAUTS_DATA_CORRUPTION());
            return;
        }
        if (_currentVid == null)
            _currentVid = new VidBuilder(packet, _dims);
        else
            _currentVid.addPacket(packet);
    }

    @Override
    public void endOfSectors(@Nonnull ILocalizedLogger log) {
        if (_currentVid != null) {
            _currentVid.finishVid(log);
            _currentVid = null;
            _dims = null;
        }
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
    }
    @Override
    public boolean filterChild(DiscItem parent, DiscItem child) {
        return false;
    }
    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
