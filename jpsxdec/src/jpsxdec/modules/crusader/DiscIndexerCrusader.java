/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2019  Michael Sabin
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.framenumber.HeaderFrameNumber;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.Misc;


/** Identify Crusader: No Remorse audio/video streams. */
public class DiscIndexerCrusader extends DiscIndexer implements SectorClaimToSectorCrusader.Listener {

    private static final Logger LOG = Logger.getLogger(DiscIndexerCrusader.class.getName());

    private static final Dimensions MOST_COMMON = new Dimensions(240, 176);
    private static final Dimensions LESS_COMMON = new Dimensions(320, 240);
    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 240;
    private static final double IDEAL_ASPECT_RATIO = 240.0 / 176.0;


    static class DimensionCmp implements Comparator<Dimensions> {

        /** Really just for tree map.
         * No clear way to say which dimension is greater than another. */
        public int compare(Dimensions o1, Dimensions o2) {
            int i = Misc.intCompare(o1.getWidth(), o2.getWidth());
            if (i != 0)
                return i;
            else
                return Misc.intCompare(o1.getHeight(), o2.getHeight());
        }

    }

    static class DimCounter implements Comparable<DimCounter> {
        @Nonnull
        public final Dimensions dims;
        public int iCount = 0;
        public DimCounter(@Nonnull Dimensions dim, int iInitialCount) {
            this.dims = dim;
            this.iCount = iInitialCount;
        }
        /** Sort such that the most ideal dimension is the maximum. */
        public int compareTo(DimCounter o) {
            int iCompare = Misc.intCompare(iCount, o.iCount);
            if (iCompare != 0)
                return iCompare;
            iCompare = Misc.intCompare(expectedDimRating(), o.expectedDimRating());
            if (iCompare != 0)
                return iCompare;
            iCompare = Misc.intCompare(tooLargeDimRating(), o.tooLargeDimRating());
            if (iCompare != 0)
                return iCompare;

            return compareAspectRatio(dims, o.dims);
            // there should be no equals
        }

        private int expectedDimRating() {
            if (dims.equals(MOST_COMMON))
                return 2;
            else if (dims.equals(LESS_COMMON))
                return 1;
            else
                return 0;
        }

        private int tooLargeDimRating() {
            if (dims.getWidth() < MAX_WIDTH && dims.getHeight() < MAX_HEIGHT)
                return 1;
            else
                return 0;
        }

    }

    // wow, how random is this :O
    // https://stackoverflow.com/questions/10416366/how-to-determine-which-aspect-ratios-are-closest
    /** @return 1 for A, -1 for B */
    static int compareAspectRatio(Dimensions candidateA, Dimensions candidateB) {
        double aspectRatioCandidateA = candidateA.getWidth()/(double)candidateA.getHeight();
        double aspectRatioCandidateB = candidateB.getWidth()/(double)candidateB.getHeight();
        double closenessScoreA = 1 - (IDEAL_ASPECT_RATIO/aspectRatioCandidateA);
        double closenessScoreB = 1 - (IDEAL_ASPECT_RATIO/aspectRatioCandidateB);

        if (Math.abs(closenessScoreA) <= Math.abs(closenessScoreB))
        {
            return 1; // A is better
        }
        else
        {
            return -1; // B is better
        }
    }

    /** Tracks a single video stream. Object dies when stream ends. */
    private static class VidBuilder implements CrusaderSectorToCrusaderPacket.PacketListener {
        @Nonnull
        private final ILocalizedLogger _errLog;

        // these will either both be null, or both be !null
        @CheckForNull
        private IndexSectorFrameNumber.Format.Builder _indexSectorFrameNumberBuilder;
        @CheckForNull
        private HeaderFrameNumber.Format.Builder _headerFrameNumberBuilder;
        private int _iSoundUnitCount = 0;

        private final CrusaderSectorToCrusaderPacket _cs2cp = new CrusaderSectorToCrusaderPacket(this);

        private final TreeMap<Dimensions, DimCounter> _dimCounter = new TreeMap<Dimensions, DimCounter>(new DimensionCmp());

        private final int _iStartSector;
        private int _iEndSector;
        private int _iInitialFramePresentationSector = -1;

        public VidBuilder(@Nonnull ILocalizedLogger errLog,
                          @Nonnull SectorCrusader vidSect)
                throws LoggedFailure
        {
            _errLog = errLog;
            _iStartSector = _iEndSector = vidSect.getSectorNumber();
            if (!_cs2cp.sectorRead(vidSect, errLog))
                throw new RuntimeException("Sector should have been accepted " + vidSect);
        }

        /** Returns if the supplied sector is part of this movie. If not,
          * end this movie and start a new one. */
        public boolean feedSector(@Nonnull SectorCrusader sector) throws LoggedFailure {
            if (!_cs2cp.sectorRead(sector, _errLog)) {
                return false;
            }
            _iEndSector = sector.getSectorNumber();
            return true;
        }

        public void frame(@Nonnull CrusaderPacketHeaderReader.VideoHeader frame,
                          @Nonnull DemuxedData<CrusaderDemuxPiece> demux,
                          @Nonnull ILocalizedLogger log)
        {
            if (_iInitialFramePresentationSector < 0) {
                _iInitialFramePresentationSector = frame.getFrameNumber() * 10;
                if (_iInitialFramePresentationSector != 0)
                    LOG.log(Level.WARNING, "[Video] Setting initial presentation sector {0,number,#}", _iInitialFramePresentationSector);
            }


            Dimensions dims = new Dimensions(frame.getWidth(), frame.getHeight());
            if (_indexSectorFrameNumberBuilder == null) {
                _indexSectorFrameNumberBuilder = new IndexSectorFrameNumber.Format.Builder(demux.getStartSector());
                _headerFrameNumberBuilder = new HeaderFrameNumber.Format.Builder(frame.getFrameNumber());
                _dimCounter.put(dims, new DimCounter(dims, 1));
            } else {
                DimCounter dimCounts = _dimCounter.get(dims);
                if (dimCounts == null) {
                    LOG.log(Level.SEVERE, "Crusader inconsistent dimensions: {0}", dims);
                    dimCounts = new DimCounter(dims, 0);
                    _dimCounter.put(dims, dimCounts);
                }
                dimCounts.iCount++;
                _indexSectorFrameNumberBuilder.addFrameStartSector(demux.getStartSector());
                _headerFrameNumberBuilder.addHeaderFrameNumber(frame.getFrameNumber());
            }
        }

        public void audio(@Nonnull CrusaderPacketHeaderReader.AudioHeader audio,
                          @Nonnull DemuxedData<CrusaderDemuxPiece> demux,
                          @Nonnull ILocalizedLogger log)
        {
            // if the initial portion of a movie is missing, and audio is the first
            // payload found, we need to adjust the initial presentation offset
            // so we don't write a ton silence initially in an effort to catch up.
            // it seems audio payload presentation sectors run about 40 sectors
            // ahead of the video presentation sectors.
            // so pick an initial presentation sector a little before when the next
            // frame should be presented (-60)
            if (_iInitialFramePresentationSector < 0) {
                _iInitialFramePresentationSector = (audio.getPresentationSampleFrame() / CrusaderPacketToFrameAndAudio.SAMPLE_FRAMES_PER_SECTOR) - 60;
                if (_iInitialFramePresentationSector < 0) // don't start before the start of the movie
                    _iInitialFramePresentationSector = 0;
                else if (_iInitialFramePresentationSector > 0)
                    LOG.log(Level.WARNING, "[Audio] Setting initial presentation sector {0,number,#}", _iInitialFramePresentationSector);
            }
            _iSoundUnitCount += audio.getByteSize() / 2 / 16;
        }

        public @CheckForNull DiscItemCrusader endOfMovie(@Nonnull CdFileSectorReader cd) 
                throws LoggedFailure
        {
            _cs2cp.endVideo(_errLog);

            if (_indexSectorFrameNumberBuilder == null) // never received a frame
                return null;

            // TODO create audio-only crusader if no video is foound

            Set<Map.Entry<Dimensions, DimCounter>> y = _dimCounter.entrySet();
            ArrayList<Map.Entry<Dimensions, DimCounter>> x = new ArrayList<Map.Entry<Dimensions, DimCounter>>(y);
            Collections.sort(x, new Comparator<Map.Entry<Dimensions, DimCounter>>() {
                public int compare(Map.Entry<Dimensions, DimCounter> o1,
                                   Map.Entry<Dimensions, DimCounter> o2)
                {
                    return Misc.intCompare(o2.getValue().iCount, o1.getValue().iCount);
                }
            });

            DimCounter max = Collections.max(_dimCounter.values());
            Dimensions dims = max.dims;
            if (_dimCounter.size() > 1)
                LOG.log(Level.INFO, "Choosing crusader dimensions {0}", dims);

            return new DiscItemCrusader(cd, _iStartSector, _iEndSector, dims,
                                        _indexSectorFrameNumberBuilder.makeFormat(),
                                        _headerFrameNumberBuilder.makeFormat(),
                                        _iInitialFramePresentationSector,
                                        _iSoundUnitCount);
        }

    }
    

    @Nonnull
    private final ILocalizedLogger _errLog;
    @CheckForNull
    private VidBuilder _currentStream;

    public DiscIndexerCrusader(@Nonnull ILocalizedLogger errLog) {
        _errLog = errLog;
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        SectorClaimToSectorCrusader s2cs = scs.getClaimer(SectorClaimToSectorCrusader.class);
        s2cs.setListener(this);
    }
    
    public void sectorRead(@Nonnull SectorCrusader vidSect, @Nonnull ILocalizedLogger log) 
            throws LoggedFailure
    {
        if (_currentStream != null) {
            boolean blnAccepted = _currentStream.feedSector(vidSect);
            if (!blnAccepted) {
                DiscItemCrusader vid = _currentStream.endOfMovie(getCd());
                if (vid != null)
                    addDiscItem(vid);
                _currentStream = null;
            }
        }
        if (_currentStream == null) {
            _currentStream = new VidBuilder(_errLog, vidSect);
        }
    }

    public void endOfSectors(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_currentStream != null) {
            DiscItemCrusader vid = _currentStream.endOfMovie(getCd());
            if (vid != null)
                addDiscItem(vid);
            _currentStream = null;
        }
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) 
            throws LocalizedDeserializationFail
    {
        if (DiscItemCrusader.TYPE_ID.equals(fields.getType()))
            return new DiscItemCrusader(getCd(), fields);
        return null;
    }

    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
