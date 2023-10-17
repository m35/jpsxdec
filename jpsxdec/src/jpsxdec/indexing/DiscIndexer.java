/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.indexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.crusader.DiscIndexerCrusader;
import jpsxdec.modules.eavideo.DiscIndexerEAVideo;
import jpsxdec.modules.iso9660.DiscIndexerISO9660;
import jpsxdec.modules.policenauts.DiscIndexerPolicenauts;
import jpsxdec.modules.spu.DiscIndexerSpu;
import jpsxdec.modules.square.DiscIndexerSquareAudio;
import jpsxdec.modules.tim.DiscIndexerTim;
import jpsxdec.modules.video.sectorbased.DiscIndexerSectorBasedVideo;
import jpsxdec.modules.xa.DiscIndexerXaAudio;

/** Superclass of all disc indexers.
 * Be sure to also implement {@link Identified} and/or {@link Static}
 * to receive the data of interest. */
public abstract class DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexer.class.getName());

    public static @Nonnull List<DiscIndexer> createIndexers(@Nonnull ILocalizedLogger log) {
        DiscIndexer[] coreIndexers = new DiscIndexer[] {
            new DiscIndexerXaAudio(log),
            new DiscIndexerISO9660(log),
            new DiscIndexerSquareAudio(log),
            new DiscIndexerTim(),
            new DiscIndexerSectorBasedVideo(log),
            new DiscIndexerPolicenauts(),
            new DiscIndexerCrusader(),
            new DiscIndexerEAVideo(),
        };
        ArrayList<DiscIndexer> indexers = new ArrayList<DiscIndexer>(Arrays.asList(coreIndexers));

        if (DiscIndexerSpu.ENABLE_SPU_SUPPORT) {
            indexers.add(new DiscIndexerSpu());
        }
        return indexers;
    }

    @CheckForNull
    private Collection<DiscItem> _mediaList;
    @CheckForNull
    private ICdSectorReader _sourceCd;

    /** Called by {@link DiscIndex} right away. */
    final void indexInit(@Nonnull Collection<DiscItem> items,
                         @Nonnull ICdSectorReader cd)
    {
        _mediaList = items;
        _sourceCd = cd;
    }

    /** Subclasses should call this method when an item is ready to be added. */
    final protected void addDiscItem(@Nonnull DiscItem discItem) {
        LOG.log(Level.INFO, "Adding disc item {0}", discItem);
        _mediaList.add(discItem);
    }

    final protected @Nonnull ICdSectorReader getCd() {
        if (_sourceCd == null)
            throw new IllegalStateException("CD should have been set before use");
        return _sourceCd;
    }

    abstract public void attachToSectorClaimer(@Nonnull SectorClaimSystem scs);

    /** Lines from the index file as passed to be handled by the indexers.
     * @return  if the line successfully created a disc item. */
    abstract public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail;

    /** Called after the entire disc has been scanned.
     * Indexers can add, remove, or change disc items. */
    abstract public void listPostProcessing(@Nonnull Collection<DiscItem> allItems);

    /** Called for each disc item before it is added to a parent item.
     * Parent will be null if the child will be added to the root of the tree.
     * In most cases you should return {@code false}.
     * This was initially added to filter out silent XA streams not associated with a video. */
    abstract public boolean filterChild(@CheckForNull DiscItem parent, @Nonnull DiscItem child);

    /** Called after the entire indexing process is complete. The DiscIndex
     * will not be changing any further, but indexers can tweak individual items
     * as necessary.
     * This was initially added so the ISO9660 indexer can set the name of the index
     * to the name of the disc found in the filesystem. */
    abstract public void indexGenerated(@Nonnull DiscIndex index); // TODO consider not having a dependency on DiscIndex
}
