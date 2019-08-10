/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019  Michael Sabin
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

package jpsxdec.modules.roadrash;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.util.BinaryDataNotRecognized;

public class DiscIndexerRoadRash extends DiscIndexer implements SectorClaimToRoadRash.Listener {

    private static final Logger LOG = Logger.getLogger(DiscIndexerRoadRash.class.getName());

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        SectorClaimToRoadRash ui = scs.getClaimer(SectorClaimToRoadRash.class);
        ui.setListener(this);
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        if (DiscItemRoadRash.TYPE_ID.equals(fields.getType()))
            return new DiscItemRoadRash(getCd(), fields);
        return null;
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

    private static class VidBuilder {
        @Nonnull
        private final Dimensions _dims;
        @Nonnull
        private final IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @Nonnull
        private final HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;

        public VidBuilder(@Nonnull RoadRashPacket.MDEC firstMdec, int iStartSector) {
            _dims = new Dimensions(firstMdec.getWidth(), firstMdec.getHeight());
            _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(iStartSector);
            _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(firstMdec.getFrameNumber());
        }

        public void addFrame(@Nonnull RoadRashPacket.MDEC mdec, int iStartSector) throws BinaryDataNotRecognized {
            if (!_dims.equals(mdec.getWidth(), mdec.getHeight()) ||
                mdec.getFrameNumber() < _headerFrameNumberBuilder.getLastFrameNumber())
                throw new BinaryDataNotRecognized("Road Rash data corruption");

            _headerFrameNumberBuilder.addHeaderFrameNumber(mdec.getFrameNumber());
            _indexSectorFrameNumberBuilder.addFrameStartSector(iStartSector);
        }

        public HeaderFrameNumber.Format getFrameFormat() {
            // some movies have the same frame number for all the frames
            // if that is the case, then treat it like it has no header frame number
            if (_headerFrameNumberBuilder.getStartFrameNumber() != _headerFrameNumberBuilder.getLastFrameNumber())
                return _headerFrameNumberBuilder.makeFormat();
            else
                return null;

        }
    }

    private static class MovieBuilder {
        @CheckForNull
        private VidBuilder _vidBuilder;
        @Nonnull
        private final RoadRashPacket.VLC0 _vlcPacket;
        private int _iSpuSoundUnitPairCount = 0;
        private final int _iStartSector;
        private int _iEndSector;

        public MovieBuilder(RoadRashPacket.VLC0 vlcPacket, int iStartSector) {
            this._vlcPacket = vlcPacket;
            _iStartSector = iStartSector;
            _iEndSector = iStartSector;
        }

        public void addPacket(@Nonnull RoadRashPacketSectors packet, int iStartSector) throws BinaryDataNotRecognized {
            _iEndSector = packet.iEndSector;
            if (packet.packet instanceof RoadRashPacket.AU) {
                RoadRashPacket.AU au = (RoadRashPacket.AU)packet.packet;
                _iSpuSoundUnitPairCount += au.getSpuSoundUnitPairCount();
            } else if (packet.packet instanceof RoadRashPacket.MDEC) {
                if (_vidBuilder == null) {
                    _vidBuilder = new VidBuilder((RoadRashPacket.MDEC) packet.packet, packet.iStartSector);
                } else {
                    _vidBuilder.addFrame((RoadRashPacket.MDEC) packet.packet, packet.iStartSector);
                }
            } else if (packet.packet instanceof RoadRashPacket.VLC0) {
                throw new BinaryDataNotRecognized();
            } else {
                throw new RuntimeException("??");
            }
        }

        public @Nonnull DiscItemRoadRash makeItem(@Nonnull CdFileSectorReader cd) throws BinaryDataNotRecognized {
            if (_vidBuilder == null)
                throw new BinaryDataNotRecognized();

            DiscItemRoadRash item = new DiscItemRoadRash(cd, _iStartSector, _iEndSector,
                    _vidBuilder._dims,
                    _vidBuilder._indexSectorFrameNumberBuilder.makeFormat(),
                    _vidBuilder.getFrameFormat(),
                    _iSpuSoundUnitPairCount);
            return item;
        }
    }

    @CheckForNull
    private MovieBuilder _movieBuilder;

    public void feedPacket(@Nonnull RoadRashPacketSectors packet, @Nonnull ILocalizedLogger log) {

        try {
            if (_movieBuilder == null) {
                if (!(packet.packet instanceof RoadRashPacket.VLC0)) {
                    throw new BinaryDataNotRecognized();
                }
                _movieBuilder = new MovieBuilder((RoadRashPacket.VLC0) packet.packet, packet.iStartSector);
            } else {
                _movieBuilder.addPacket(packet, 0);
            }
        } catch (BinaryDataNotRecognized ex) {
            LOG.log(Level.SEVERE, "Road Rash data corruption", ex);
            endVideo(log);
        }
    }

    public void endVideo(@Nonnull ILocalizedLogger log) {
        if (_movieBuilder != null) {
            try {
                DiscItemRoadRash item = _movieBuilder.makeItem(getCd());
                addDiscItem(item);
            } catch (BinaryDataNotRecognized ex) {
                LOG.log(Level.SEVERE, "Road Rash data corruption", ex);
            }
            _movieBuilder = null;
        }
    }

}
