/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2020-2023  Michael Sabin
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.indexing.DiscIndexer;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.ac3.DiscIndexerAceCombat3Video;
import jpsxdec.modules.dredd.DiscIndexerDredd;
import jpsxdec.modules.ngauge.DiscIndexerNGauge;
import jpsxdec.modules.strvideo.DiscIndexerStrVideo;
import jpsxdec.modules.xa.DiscItemXaAudioStream;

/** Manages all sector-based video indexing since they all share common
 * post-processing. */
public class DiscIndexerSectorBasedVideo extends DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexerSectorBasedVideo.class.getName());

    public static abstract class SubIndexer {

        private DiscIndexerSectorBasedVideo _parentIndexer;

        final protected void addVideo(@Nonnull DiscItemSectorBasedVideoStream video) {
            _parentIndexer._completedVideos.add(video);
            _parentIndexer.addDiscItem(video);
        }

        final protected @Nonnull ICdSectorReader getCd() {
            return _parentIndexer.getCd();
        }

        abstract public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs);

        abstract public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail;
    }

    private final Collection<DiscItemSectorBasedVideoStream> _completedVideos = new ArrayList<DiscItemSectorBasedVideoStream>();
    @Nonnull
    private final SubIndexer[] _sectorBasedVideoIndexers;

    public DiscIndexerSectorBasedVideo(@Nonnull ILocalizedLogger log) {
        _sectorBasedVideoIndexers = new SubIndexer[] {
            new DiscIndexerStrVideo(),
            new DiscIndexerAceCombat3Video(log),
            new DiscIndexerDredd(),
            new DiscIndexerNGauge(),
        };

        for (SubIndexer vidIndexer : _sectorBasedVideoIndexers) {
            vidIndexer._parentIndexer = this;
        }
    }

    @Override
    public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs) {
        for (SubIndexer indexer : _sectorBasedVideoIndexers) {
            indexer.attachToSectorClaimer(scs);
        }
    }

    @Override
    public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields) throws LocalizedDeserializationFail {
        for (SubIndexer indexer : _sectorBasedVideoIndexers) {
            DiscItem item = indexer.deserializeLineRead(fields);
            if (item != null)
                return item;
        }
        return null;
    }

    @Override
    public void listPostProcessing(@Nonnull Collection<DiscItem> allItems) {
        if (!_completedVideos.isEmpty())
            audioSplit(_completedVideos, allItems);
    }

    @Override
    public boolean filterChild(@CheckForNull DiscItem parent, @Nonnull DiscItem child) {

        boolean blnIsSilentWithoutVideoParent = (child instanceof DiscItemXaAudioStream) &&
                                                (!(parent instanceof DiscItemSectorBasedVideoStream)) &&
                                                ((DiscItemXaAudioStream) child).isConfirmedToBeSilent();
        if (blnIsSilentWithoutVideoParent)
            LOG.log(Level.INFO, "Discarding silent XA {0}", child);
        return blnIsSilentWithoutVideoParent;
    }

    private static void audioSplit(@Nonnull Collection<? extends DiscItemSectorBasedVideoStream> videos,
                                   @Nonnull Collection<DiscItem> allItems)
    {
        List<DiscItemXaAudioStream> added = new ArrayList<DiscItemXaAudioStream>();

        for (Iterator<DiscItem> it = allItems.iterator(); it.hasNext();) {
            DiscItem item = it.next();
            if (item instanceof DiscItemXaAudioStream) {
                DiscItemXaAudioStream audio = (DiscItemXaAudioStream) item;
                for (DiscItemSectorBasedVideoStream video : videos) {
                    int iSector = video.findAudioSplitPoint(audio);
                    if (iSector >= 0) {
                        DiscItemXaAudioStream[] aoSplit = audio.split(iSector);
                        it.remove();
                        added.add(aoSplit[0]);
                        added.add(aoSplit[1]);
                        break; // will process new items later
                    }
                }
            }
        }

        // now process the new items
        for (ListIterator<DiscItemXaAudioStream> it = added.listIterator(); it.hasNext();) {
            DiscItemXaAudioStream audio = it.next();
            for (DiscItemSectorBasedVideoStream video : videos) {
                int iSector = video.findAudioSplitPoint(audio);
                if (iSector >= 0) {
                    // split the already split item
                    DiscItemXaAudioStream[] aoSplit = audio.split(iSector);
                    it.remove();
                    it.add(aoSplit[0]);
                    it.add(aoSplit[1]);
                    it.previous(); // backup to before the new split items
                    it.previous();
                    break; // continue processing at split items
                }
            }
        }
        allItems.addAll(added);
    }


    @Override
    public void indexGenerated(@Nonnull DiscIndex index) {
    }

}
