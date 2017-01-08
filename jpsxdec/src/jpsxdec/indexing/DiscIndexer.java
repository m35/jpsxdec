/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.ILocalizedLogger;

/** Superclass of all disc indexers. 
 * Be sure to also implement {@link Identified} and/or {@link Static}
 * to receive the data of interest. */
public abstract class DiscIndexer {

    private static final Logger LOG = Logger.getLogger(DiscIndexer.class.getName());

    /** Indexer is interested in {@link IdentifiedSector}s found in the disc.
     * Most common indexer. */
    public interface Identified {
        /** Sectors are passed to this function in sequential order. */
        public void indexingSectorRead(@Nonnull CdSector cdSector,
                                       @CheckForNull IdentifiedSector idSector);
    }

    /** Indexer is interested in unidentified sectors demuxed into a stream. */
    public interface Static {
        /** Process a stream of data. */
        public void staticRead(@Nonnull DemuxedUnidentifiedDataStream is) throws IOException;
    }

    public static @Nonnull DiscIndexer[] createIndexers(@Nonnull ILocalizedLogger log) {
        return new DiscIndexer[] {
            new DiscIndexerISO9660(log),
            new DiscIndexerSquare(log),
            new DiscIndexerTim(),
            new DiscIndexerStrVideoWithFrame(log),
            new DiscIndexerAceCombat3Video(log),
            new DiscIndexerXaAudio(log),
            new DiscIndexerCrusader(log),
            new DiscIndexerDredd(log),
            //new DiscIndexerSpu(),
        };
    }

    @CheckForNull
    private Collection<DiscItem> _mediaList;
    @CheckForNull
    private CdFileSectorReader _sourceCd;

    /** Called by {@link DiscIndex} right away. */
    final void indexInit(@Nonnull Collection<DiscItem> items,
                         @Nonnull CdFileSectorReader cd)
    {
        _mediaList = items;
        _sourceCd = cd;
    }


    /** Subclasses should call this method when an item is ready to be added. */
    final protected void addDiscItem(@CheckForNull DiscItem discItem) {
        if (discItem == null) {
            LOG.log(Level.WARNING, "Something tried to add a null disc item.", new Exception());
            return;
        }
        LOG.log(Level.INFO, "Adding disc item {0}", discItem);
        _mediaList.add(discItem);
    }

    final protected @Nonnull CdFileSectorReader getCd() {
        if (_sourceCd == null)
            throw new IllegalStateException("CD should have been set before use");
        return _sourceCd;
    }

    /** Signals to the indexers that no more sectors will be passed, and that
     * indexers should close off and submit any lingering disc items. */
    abstract public void indexingEndOfDisc();

    /** Lines from the index file as passed to be handled by the indexers.
     * @return  if the line successfully created a disc item. */
    abstract public @CheckForNull DiscItem deserializeLineRead(@Nonnull SerializedDiscItem fields)
            throws DeserializationFail;

    abstract public void listPostProcessing(@Nonnull Collection<DiscItem> allItems);

    /** Called after the entire indexing process is complete. The DiscIndex
     * will not be changing any further, but indexers can tweak individual items
     * as necessary. */
    abstract public void indexGenerated(@Nonnull DiscIndex index);
}
