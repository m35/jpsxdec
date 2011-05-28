/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSerialization;
import jpsxdec.sectors.IdentifiedSector;

/** Superclass of all disc indexers. */
public abstract class DiscIndexer {

    private static final Logger log = Logger.getLogger(DiscIndexer.class.getName());

    public static DiscIndexer[] createIndexers(Logger log) {
        return new DiscIndexer[] {
            new DiscIndexerISO9660(log),
            new DiscIndexerSquare(log),
            //new DiscIndexerTim(),
            new DiscIndexerVideo(log),
            new DiscIndexerXaAudio(log)
        };
    }

    private Collection<DiscItem> _mediaList;

    /** The indexer needs a place to put the created disc items. */
    final public void putYourCompletedMediaItemsHere(Collection<DiscItem> items) {
        _mediaList = items;
    }


    /** Subclasses should call this method when an item is ready to be added. */
    protected void addDiscItem(DiscItem discItem) {
        if (discItem == null) {
            log.log(Level.WARNING, "Something tried to add a null disc item.", new Exception());
            return;
        }
        if (log.isLoggable(Level.INFO))
            log.info("Adding media item " + discItem.toString());
        _mediaList.add(discItem);
    }

    /** Sectors are passed to this function in sequential order. Indexers
     * may do with them what they will.  */
    abstract public void indexingSectorRead(IdentifiedSector identifiedSector);

    /** Signals to the indexers that no more sectors will be passed, and that
     * indexers should close off and submit any lingering disc items. */
    abstract public void indexingEndOfDisc();

    /** Lines from the index file as passed to be handled by the indexers.
     * @return  if the line successfully created a disc item. */
    abstract public DiscItem deserializeLineRead(DiscItemSerialization deserializedLine);

    abstract public void staticRead(DemuxedUnidentifiedDataStream is) throws IOException;

    /** Called after the entire indexing process is complete. The DiscIndex
     * will not be changing any further, but indexers can tweak individual items
     * as necessary. */
    abstract public void mediaListGenerated(DiscIndex index);
}
