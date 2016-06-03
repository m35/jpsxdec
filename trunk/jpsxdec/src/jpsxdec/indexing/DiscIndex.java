/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2016  Michael Sabin
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.Version;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.discitems.DiscItemStrVideoStream;
import jpsxdec.discitems.IndexId;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.Misc;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;

/** Searches for, and manages the collection of DiscItems in a file.
 *  PlayStation files (discs, STR, XA, etc.) can contain multiple items of interest.
 *  This will search for them, and hold the resulting list. It will also
 *  serialize/deserialize to/from a file.  */
public class DiscIndex implements Iterable<DiscItem> {

    private static final Logger LOG = Logger.getLogger(DiscIndex.class.getName());

    private static final String COMMENT_LINE_START = ";";
    
    private static final String INDEX_CODEPAGE = "UTF-8";

    @Nonnull
    private final CdFileSectorReader _sourceCD;
    @CheckForNull
    private String _sDiscName = null;
    @Nonnull
    private final ArrayList<DiscItem> _root;
    private final List<DiscItem> _iterate = new LinkedList<DiscItem>();

    private final LinkedHashMap<Object, DiscItem> _lookup = new LinkedHashMap<Object, DiscItem>();

    /** Finds all the interesting items on the CD. */
    public DiscIndex(@Nonnull CdFileSectorReader cdReader, @Nonnull final ProgressListenerLogger pl) 
            throws TaskCanceledException
    {
        _sourceCD = cdReader;
        
        final DiscIndexer[] aoIndexers = DiscIndexer.createIndexers(pl);

        final List<DiscIndexer.Identified> identifiedIndexers = new ArrayList<DiscIndexer.Identified>();
        final List<DiscIndexer.Static> staticIndexers = new ArrayList<DiscIndexer.Static>();

        for (DiscIndexer indexer : aoIndexers) {
            indexer.indexInit(_iterate, _sourceCD);
            
            if (indexer instanceof DiscIndexer.Identified)
                identifiedIndexers.add((DiscIndexer.Identified) indexer);
            if (indexer instanceof DiscIndexer.Static)
                staticIndexers.add((DiscIndexer.Static) indexer);
        }

        UnidentifiedSectorIteratorListener iterListener =
                new UnidentifiedSectorIteratorListener(cdReader, pl, identifiedIndexers);

        pl.progressStart();
        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();

        try {
            while (iterListener.seekToNextUnidentified()) {
                DemuxedUnidentifiedDataStream staticStream = new DemuxedUnidentifiedDataStream(iterListener);

                do {
                    // do the first static indexer first
                    staticIndexers.get(0).staticRead(staticStream);
                    iterListener.checkTaskCanceled();

                    // only if there is more than one should we resetMark()
                    for (int i = 1; i < staticIndexers.size(); i++) {
                        // reset+mark the stream
                        staticStream.resetMark();
                        iterListener.checkTaskCanceled();
                        // pass it to the next static indexer
                        staticIndexers.get(i).staticRead(staticStream);
                        iterListener.checkTaskCanceled();
                    }

                } while (staticStream.resetSkipMark(4));
                iterListener.checkTaskCanceled();
            }

        } catch (IOException ex) {
            I.INDEXING_ERROR().log(pl, Level.SEVERE, ex);
        }

        // notify indexers that the disc is finished
        for (DiscIndexer indexer : aoIndexers) {
            indexer.indexingEndOfDisc();
        }

        for (DiscIndexer indexer : aoIndexers) {
            indexer.listPostProcessing(_iterate);
        }

        // sort the numbered index list according to the start sector & hierarchy level
        Collections.sort(_iterate, SORT_BY_SECTOR_HIERARHCY);

        _root = buildTree(_iterate);

        // copy the items to the hash
        int iIndex = 0;
        for (DiscItem item : _iterate) {
            item.setIndex(iIndex);
            iIndex++;
            addLookupItem(item);
        }
        
        // notify the indexers that the list has been generated
        for (DiscIndexer indexer : aoIndexers) {
            indexer.indexGenerated(this);
        }

        if (pl.seekingEvent())
            pl.event(I.INDEX_SECTOR_ITEM_PROGRESS(_sourceCD.getLength(), _sourceCD.getLength(), _iterate.size()));

        lngEnd = System.currentTimeMillis();
        I.PROCESS_TIME((lngEnd - lngStart) / 1000.0).log(pl, Level.INFO);
        pl.progressEnd();

    }


    private @Nonnull ArrayList<DiscItem> buildTree(@Nonnull Collection<DiscItem> allItems) {

        ArrayList<DiscItem> rootItems = new ArrayList<DiscItem>();

        for (DiscItem child : allItems) {
            DiscItem bestParent = null;
            int iBestParentRating = 0;
            for (DiscItem parent : allItems) {
                int iRating = parent.getParentRating(child);
                if (iRating > iBestParentRating) {
                    bestParent = parent;
                    iBestParentRating = iRating;
                }
            }
            if (bestParent == null)
                rootItems.add(child);
            else
                if (!bestParent.addChild(child))
                    throw new RuntimeException(bestParent + " should have accepted " + child);
        }

        IndexId id = new IndexId(0);
        for (DiscItem item : rootItems) {
            if (item.setIndexId(id))
                id = id.createNext();
        }

        return rootItems;
    }


    /** The level in the hierarchy is determined by the type of item it is.
     * At the top is files, next is videos, under that audio, then everything else.
     * I don't want to include this prioritization in the disc items because
     * it requires too much awareness of other types, and is really outside the
     * scope of disc items which aren't really aware of an index. Also it's nice
     * to have it here in a central place. */
    private static int typeHierarchyLevel(@Nonnull DiscItem item) {
        if (item instanceof DiscItemISO9660File)
            return 1;
        else if (item instanceof DiscItemStrVideoStream)
            return 2;
        else if (item instanceof DiscItemAudioStream)
            return 3;
        else
            return 4;
    }

    private final Comparator<DiscItem> SORT_BY_SECTOR_HIERARHCY = new Comparator<DiscItem>() {
        public int compare(DiscItem o1, DiscItem o2) {
            if (o1.getClass() == o2.getClass())
                return o1.compareTo(o2);
            if (o1.getStartSector() < o2.getStartSector())
                return -1;
            else if (o1.getStartSector() > o2.getStartSector())
                return 1;
            else if (typeHierarchyLevel(o1) < typeHierarchyLevel(o2))
                return -1;
            else if (typeHierarchyLevel(o1) > typeHierarchyLevel(o2))
                return 1;
            else
                // have more encompassing disc items come first (result is much cleaner)
                return Misc.intCompare(o2.getEndSector(), o1.getEndSector());
        }
    };

    /** Deserializes the CD index file, and tries to open the CD listed in the index. */
    public DiscIndex(@Nonnull String sIndexFile, @Nonnull Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, (CdFileSectorReader)null, errLog);
    }

    /** Deserializes the CD index file, and creates a list of items on the CD */
    public DiscIndex(@Nonnull String sIndexFile, boolean blnAllowWrites, @Nonnull Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, null, blnAllowWrites, errLog);
    }

    /** Deserializes the CD index file, and creates a list of items on the CD */
    public DiscIndex(@Nonnull String sIndexFile, @CheckForNull CdFileSectorReader cdReader, @Nonnull Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, cdReader, false, errLog);
    }

    private DiscIndex(@Nonnull String sIndexFile, @CheckForNull CdFileSectorReader cdReader,
                      boolean blnAllowWrites, @Nonnull Logger errLog)
            throws IOException, NotThisTypeException
    {
        File indexFile = new File(sIndexFile);

        CdFileSectorReader sourceCd = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile), INDEX_CODEPAGE));
        boolean blnExceptionThrown = true; // hacky way to catch all exceptions in finally block
        try {
            // make sure the first line matches the current version
            String sLine = reader.readLine();
            if (!Version.IndexHeader.equals(sLine)) {
                reader.close();
                throw new NotThisTypeException(I.INDEX_HEADER_MISSING());
            }

            ArrayList<SerializedDiscItem> readLines = new ArrayList<SerializedDiscItem>();

            // read all the lines, checking for errors and searching for the source CD
            while ((sLine = reader.readLine()) != null) {

                // comments
                if (sLine.startsWith(COMMENT_LINE_START))
                    continue;

                // blank lines
                if (sLine.matches("^\\s*$"))
                    continue;
                
                // source file
                if (sLine.startsWith(CdFileSectorReader.SERIALIZATION_START)) {
                    if (cdReader != null) {
                        // verify that the source file matches
                        if (!cdReader.matchesSerialization(sLine)) {
                            I.DISC_FORMAT_MISMATCH(cdReader, sLine).log(errLog, Level.WARNING);
                        }
                    } else {
                        sourceCd = new CdFileSectorReader(sLine, blnAllowWrites);
                    }
                    continue;
                }

                try {
                    // save the line for deserializing later
                    readLines.add(new SerializedDiscItem(sLine));
                } catch (NotThisTypeException ex) {
                    I.INDEX_PARSE_LINE_FAIL(sLine).log(errLog, Level.WARNING, ex);
                }
            }
            
            if (sourceCd == null) {
                if (cdReader == null)
                    throw new NotThisTypeException(I.INDEX_NO_CD()); // TODO: find better exception
                _sourceCD = cdReader;
            } else {
                _sourceCD = sourceCd;
            }

            // setup indexers
            DiscIndexer[] aoIndexers = DiscIndexer.createIndexers(errLog);

            for (DiscIndexer indexer : aoIndexers) {
                indexer.indexInit(_iterate, _sourceCD);
            }

            // now create the disc items
            for (SerializedDiscItem deserializedLine : readLines) {

                boolean blnLineHandled = false;
                for (DiscIndexer indexer : aoIndexers) {
                    // first parse the indexid
                    DiscItem item = indexer.deserializeLineRead(deserializedLine);
                    if (item != null) {
                        blnLineHandled = true;
                        _iterate.add(item);
                    }
                }
                if (!blnLineHandled)
                    I.INDEX_UNHANDLED_LINE(deserializedLine).log(errLog, Level.WARNING);
            }

            _root = recreateTree(_iterate, errLog);

            // copy the items to this class
            for (DiscItem item : _iterate) {
                addLookupItem(item);
            }
            // notify the indexers that the list has been generated
            for (DiscIndexer indexer : aoIndexers) {
                indexer.indexGenerated(this);
            }


            // debug print the list contents
            if (LOG.isLoggable(Level.FINE)) {
                for (DiscItem item : this) LOG.fine(item.toString());
            }

            // no exception thrown, don't close the CD in finally block
            blnExceptionThrown = false;

        } finally {
            // something bad happened? close CD reader only if we opened it
            if (blnExceptionThrown && cdReader == null && sourceCd != null) {
                try {
                    sourceCd.close();
                } catch (IOException ex) {
                    errLog.log(Level.SEVERE, null, ex);
                }
            }
            
            try {
                reader.close();
            } catch (IOException ex) {
                errLog.log(Level.SEVERE, null, ex);
            }
        }

    }

    private @Nonnull ArrayList<DiscItem> recreateTree(@Nonnull Collection<DiscItem> allItems, @Nonnull Logger log) {
        ArrayList<DiscItem> rootItems = new ArrayList<DiscItem>();

        Outside:
        for (DiscItem child : allItems) {
            IndexId itemId = child.getIndexId();
            if (itemId.isRoot()) {
                rootItems.add(child);
                continue;
            }
            
            for (DiscItem possibleParent : allItems) {
                if (possibleParent == child)
                    continue;
                if (itemId.isParent(possibleParent.getIndexId())) {
                    if (!possibleParent.addChild(child)) {
                        I.INDEX_REBUILD_PARENT_REJECTED_CHILD(possibleParent, child).log(log, Level.WARNING);
                    }
                    continue Outside;
                }
            }
            rootItems.add(child);
        }

        return rootItems;
    }


    /** Adds item to the internal hash. */
    private void addLookupItem(@Nonnull DiscItem item) {
        _lookup.put(Integer.valueOf(item.getIndex()), item);
        _lookup.put(item.getIndexId().serialize(), item);
    }


    /** Serializes the list of disc items to a file. */
    public void serializeIndex(@Nonnull File file)
            throws IOException
    {
        PrintStream ps = new PrintStream(file, INDEX_CODEPAGE);
        try {
            serializeIndex(ps);
        } finally {
            ps.close();
        }
    }
    
    /** Serializes the list of disc items to a stream. */
    private void serializeIndex(@Nonnull PrintStream ps)
            throws IOException
    {
        ps.println(Version.IndexHeader);
        ps.println(I.INDEX_COMMENT(COMMENT_LINE_START));
        // TODO: Serialize the CD file location relative to where this index file is being saved
        ps.println(_sourceCD.serialize());
        for (DiscItem item : this) {
            ps.println(item.serialize().serialize());
        }
        ps.close();
    }

    public void setDiscName(@Nonnull String sName) {
        _sDiscName = sName;
    }

    public @Nonnull List<DiscItem> getRoot() {
        return _root;
    }

    public @CheckForNull DiscItem getByIndex(int iIndex) {
        return _lookup.get(Integer.valueOf(iIndex));
    }

    public @CheckForNull DiscItem getById(@Nonnull String sId) {
        return _lookup.get(sId);
    }
    
    public boolean hasIndex(int iIndex) {
        return _lookup.containsKey(Integer.valueOf(iIndex));
    }

    public @Nonnull CdFileSectorReader getSourceCd() {
        return _sourceCD;
    }
    
    public int size() {
        return _iterate.size();
    }
    
    //[implements Iterable]
    public @Nonnull Iterator<DiscItem> iterator() {
        return _iterate.iterator();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %d items", _sourceCD.getSourceFile(), _sDiscName, _iterate.size());
    }

    private class UnidentifiedSectorIteratorListener extends UnidentifiedSectorIterator {

        @Nonnull
        private final ProgressListenerLogger _pl;
        @Nonnull
        private final List<DiscIndexer.Identified> _identifiedIndexers;
        private int iCurrentHeaderSectorNumber = -1;
        private int iMode1Count = 0;
        private int iMode2Count = 0;

        public UnidentifiedSectorIteratorListener(@Nonnull CdFileSectorReader cd,
                                                  @Nonnull ProgressListenerLogger pl,
                                                  @Nonnull List<DiscIndexer.Identified> identifiedIndexers)
        {
            super(cd);
            _pl = pl;
            _identifiedIndexers = identifiedIndexers;
        }

        public void sectorRead(@Nonnull CdSector cdSector, @CheckForNull IdentifiedSector idSector) 
                throws TaskCanceledException
        {
            if (cdSector.hasHeaderErrors())
                I.INDEX_SECTOR_CORRUPTED(cdSector.getSectorNumberFromStart())
                        .log(_pl, Level.WARNING);

            if (cdSector.hasHeaderSectorNumber()) {
                int iNewSectNumber = cdSector.getHeaderSectorNumber();
                if (iNewSectNumber != -1) {
                    if (iCurrentHeaderSectorNumber >= 0) {
                        if (iCurrentHeaderSectorNumber + 1 != iNewSectNumber)
                            I.INDEX_SECTOR_HEADER_NUM_BREAK(iCurrentHeaderSectorNumber, iNewSectNumber).log(_pl, Level.WARNING);
                    }
                    iCurrentHeaderSectorNumber = iNewSectNumber;
                } else {
                    iCurrentHeaderSectorNumber = -1;
                }
            } else {
                iCurrentHeaderSectorNumber = -1;
            }

            if (cdSector.isMode1()) {
                if (iMode1Count < iMode2Count)
                    I.INDEX_MODE1_AMONG_MODE2(cdSector.getSectorNumberFromStart()).log(_pl, Level.WARNING);
                iMode1Count++;
            } else if (!cdSector.isCdAudioSector())
                iMode2Count++;

            for (DiscIndexer.Identified indexer : _identifiedIndexers) {
                indexer.indexingSectorRead(cdSector, idSector);
            }

            int iSector = cdSector.getSectorNumberFromStart();
            _pl.progressUpdate(iSector / (double)_sourceCD.getLength());

            if (_pl.seekingEvent())
                _pl.event(I.INDEX_SECTOR_ITEM_PROGRESS(iSector, _sourceCD.getLength(), _iterate.size()));
        }

    }
}
