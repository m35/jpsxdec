/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.crusader;

import java.util.Collection;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.IdentifiedSectorListener;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.SectorRange;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.util.BinaryDataNotRecognized;


/** Identify Crusader: No Remorse audio/video streams. */
public class DiscIndexerCrusader extends DiscIndexer implements IdentifiedSectorListener<SectorCrusader> {

    /** Tracks a single video stream. Object dies when stream ends. */
    private static class VidBuilder {

        // these 3 will either all be null, or !null
        @CheckForNull
        private IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @CheckForNull
        private HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;
        @CheckForNull
        private Dimensions _dims;

        private int _iSoundPairUnitCount = 0;

        private final int _iStartSector;
        private int _iEndSector;
        private int _iPrevCrusaderSectorNum;

        public VidBuilder(@Nonnull SectorCrusader sector) throws BinaryDataNotRecognized {
            if (sector.getCrusaderSectorNumber() != 0)
                throw new BinaryDataNotRecognized(); // data corrupted
            _iPrevCrusaderSectorNum = sector.getCrusaderSectorNumber();
            _iStartSector = _iEndSector = sector.getSectorNumber();
        }

        /** Returns if the supplied sector is part of this movie. If not,
          * end this movie and start a new one. */
        public boolean addSector(@Nonnull SectorCrusader sector) {
            if (sector.getCrusaderSectorNumber() != _iPrevCrusaderSectorNum + 1)
                return false;
            _iEndSector = sector.getSectorNumber();
            _iPrevCrusaderSectorNum++;
            return true;
        }

        public void addPacket(@Nonnull CrusaderPacket packet) throws BinaryDataNotRecognized {
            if (packet instanceof CrusaderPacket.Video) {
                frame((CrusaderPacket.Video) packet);
            } else {
                audio((CrusaderPacket.Audio) packet);
            }
        }

        private void frame(@Nonnull CrusaderPacket.Video frame) throws BinaryDataNotRecognized {
            if (_indexSectorFrameNumberBuilder == null) {
                _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(frame.getStartSector());
                _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(frame.getFrameNumber());
                _dims = new Dimensions(frame.getWidth(), frame.getHeight());
            } else {
                if (_dims.getWidth() != frame.getWidth() || _dims.getHeight() != frame.getHeight())
                    throw new BinaryDataNotRecognized(); // data corrupted

                _indexSectorFrameNumberBuilder.addFrameStartSector(frame.getStartSector());
                _headerFrameNumberBuilder.addHeaderFrameNumber(frame.getFrameNumber());
            }
        }

        public void audio(@Nonnull CrusaderPacket.Audio audio) {
            _iSoundPairUnitCount += audio.getSoundUnitPairCount();
        }

        public @Nonnull DiscItemCrusader endOfMovie(@Nonnull ICdSectorReader cd) throws BinaryDataNotRecognized {

            if (_indexSectorFrameNumberBuilder == null) // never received a frame
                throw new BinaryDataNotRecognized(); // data corruption
            DiscItemCrusader di = new DiscItemCrusader(
                    cd, _iStartSector, _iEndSector, _dims,
                    _indexSectorFrameNumberBuilder.makeFormat(),
                    _headerFrameNumberBuilder.makeFormat(),
                    _iSoundPairUnitCount);
            return di;
        }


    }

    private static class WrappedBinaryDataNotRecognized extends RuntimeException {

        private WrappedBinaryDataNotRecognized(@Nonnull BinaryDataNotRecognized ex) {
            super(ex);
        }

        @Override
        public BinaryDataNotRecognized getCause() {
            return (BinaryDataNotRecognized) super.getCause();
        }
    }

    @CheckForNull
    private VidBuilder _currentStream;

    private final CrusaderSectorToCrusaderPacket _cs2cp;

    private final CrusaderSectorToCrusaderPacket.PacketListener _packetListener =
        new CrusaderSectorToCrusaderPacket.PacketListener() {
        @Override
        public void packetComplete(@Nonnull CrusaderPacket packet, @Nonnull ILocalizedLogger log) {
            try {
                _currentStream.addPacket(packet);
            } catch (BinaryDataNotRecognized ex) {
                throw new WrappedBinaryDataNotRecognized(ex);
            }
        }

        @Override
        public void endOfVideo(ILocalizedLogger log) {
            // will let the sectors be the end
        }
    };

    public DiscIndexerCrusader() {
        _cs2cp = new CrusaderSectorToCrusaderPacket(SectorRange.ALL, _packetListener);
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        scs.addIdListener(this);
    }

    @Override
    public @Nonnull Class<SectorCrusader> getListeningFor() {
        return SectorCrusader.class;
    }

    @Override
    public void feedSector(@Nonnull SectorCrusader idSector, @Nonnull ILocalizedLogger log) {
        try {
            if (_currentStream == null) {
                _currentStream = new VidBuilder(idSector);
            } else if (!_currentStream.addSector(idSector)) {
                endOfFeedSectors(log);
                _currentStream = new VidBuilder(idSector);
            }
            try {
                _cs2cp.feedSector(idSector, log);
            } catch (WrappedBinaryDataNotRecognized ex) {
                throw ex.getCause();
            }
        } catch (BinaryDataNotRecognized ex) {
            log.log(Level.SEVERE, I.CRUSADER_DATA_CORRUPTED(), ex);
            _currentStream = null;
        } catch (LoggedFailure ex) {
            throw new RuntimeException("Shouldn't happen", ex);
        }
    }

    @Override
    public void endOfFeedSectors(@Nonnull ILocalizedLogger log) {
        if (_currentStream != null) {
            try {
                addDiscItem(_currentStream.endOfMovie(getCd()));
            } catch (BinaryDataNotRecognized ex) {
                log.log(Level.SEVERE, I.CRUSADER_DATA_CORRUPTED(), ex);
            }
        }
        _currentStream = null;
    }

    @Override
    public @CheckForNull DiscItemCrusader deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        if (DiscItemCrusader.TYPE_ID.equals(fields.getType()))
            return new DiscItemCrusader(getCd(), fields);
        return null;
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
