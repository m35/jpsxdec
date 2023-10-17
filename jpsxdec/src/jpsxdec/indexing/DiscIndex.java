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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.CdOpener;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorHeader;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.cdreaders.SerializedDisc;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.IndexId;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.iso9660.DiscItemISO9660File;
import jpsxdec.modules.strvideo.DiscItemStrVideoStream;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Searches for, and manages the collection of DiscItems in a file.
 *  PlayStation files (discs, STR, XA, etc.) can contain multiple items of interest.
 *  This will search for them, and hold the resulting list. It will also
 *  serialize/deserialize to/from a file.  */
public class DiscIndex implements Iterable<DiscItem> {

    public static boolean LOG_CORRUPTED_SECTORS = true;

    private static final Logger LOG = Logger.getLogger(DiscIndex.class.getName());

    public static class IndexNotFoundException extends FileNotFoundException {

        @Nonnull
        private final File _file;

        public IndexNotFoundException(@Nonnull File file, FileNotFoundException ex) {
            super(file.getPath());
            initCause(ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    public static class IndexReadException extends IOException {

        @Nonnull
        private final File _file;

        public IndexReadException(@Nonnull File file, IOException ex) {
            super(ex);
            _file = file;
        }

        public @Nonnull File getFile() {
            return _file;
        }
    }

    private static final String COMMENT_LINE_START = ";";

    @Nonnull
    private final ICdSectorReader _sourceCD;
    @CheckForNull
    private String _sDiscName = null;
    @Nonnull
    private final ArrayList<DiscItem> _root;
    private final List<DiscItem> _iterate = new LinkedList<DiscItem>();

    private final LinkedHashMap<Object, DiscItem> _lookup = new LinkedHashMap<Object, DiscItem>();

    /** Finds all the interesting items on the CD. */
    public DiscIndex(@Nonnull ICdSectorReader cdReader, @Nonnull final ProgressLogger pl)
            throws TaskCanceledException, CdException.Read, LoggedFailure
    {
        _sourceCD = cdReader;

        if (!_sourceCD.hasSectorHeader())
            pl.log(Level.WARNING, I.GUI_DISC_NO_RAW_HEADERS_WARNING());

        final List<DiscIndexer> indexers = DiscIndexer.createIndexers(pl);

        for (DiscIndexer indexer : indexers) {
            indexer.indexInit(_iterate, _sourceCD);
        }

        SectorHeaderChecker headerChecker = new SectorHeaderChecker(pl);

        int iEndSector = cdReader.getSectorCount() - 1;
        pl.progressStart(iEndSector);

        @Nonnull
        SectorClaimSystem sectorIter = SectorClaimSystem.create(cdReader);
        for (DiscIndexer indexer : indexers) {
            indexer.attachToSectorClaimer(sectorIter);
        }

        long lngStart, lngEnd;
        lngStart = System.currentTimeMillis();

        SectorTypeCounter sectorCounter = new SectorTypeCounter();

        while (sectorIter.hasNext()) {
            IIdentifiedSector idSector = sectorIter.next(pl);
            headerChecker.indexingSectorRead(idSector.getCdSector());
            sectorCounter.increment(idSector);
            int iSector = idSector.getSectorNumber();
            pl.progressUpdate(iSector);

            if (pl.isSeekingEvent())
                pl.event(I.INDEX_SECTOR_ITEM_PROGRESS(iSector, iEndSector, _iterate.size()));
        }

        sectorIter.flush(pl);


        for (DiscIndexer indexer : indexers) {
            indexer.listPostProcessing(_iterate);
        }

        // sort the numbered index list according to the start sector & hierarchy level
        Collections.sort(_iterate, SORT_BY_SECTOR_HIERARHCY);

        _root = buildTree(_iterate, indexers);

        // copy the items to the hash
        int iIndex = 0;
        for (DiscItem item : _iterate) {
            // take this opportunity to check for anything that isn't completely in the disc
            // (probably just iso 9660 files existing off the end of the CD)
            item.setIndex(iIndex);
            iIndex++;
            addLookupItem(item);
        }

        // notify the indexers that the list has been generated
        for (DiscIndexer indexer : indexers) {
            indexer.indexGenerated(this);
        }

        if (pl.isSeekingEvent())
            pl.event(I.INDEX_SECTOR_ITEM_PROGRESS(iEndSector, iEndSector, _iterate.size()));

        lngEnd = System.currentTimeMillis();
        pl.log(Level.INFO, I.PROCESS_TIME((lngEnd - lngStart) / 1000.0));
        pl.progressEnd();

        sectorCounter.logCount();
    }


    private static @Nonnull ArrayList<DiscItem> buildTree(@Nonnull Collection<DiscItem> allItems,
                                                          @Nonnull Collection<DiscIndexer> indexers)
    {

        ArrayList<DiscItem> rootItems = new ArrayList<DiscItem>();

        ItemLoop:
        for (Iterator<DiscItem> iterator = allItems.iterator(); iterator.hasNext();) {
            DiscItem child = iterator.next();

            DiscItem bestParent = null;
            int iBestParentRating = 0;
            for (DiscItem parent : allItems) {
                int iRating = parent.getParentRating(child);
                if (iRating > iBestParentRating) {
                    bestParent = parent;
                    iBestParentRating = iRating;
                }
            }

            for (DiscIndexer indexer : indexers) {
                if (indexer.filterChild(bestParent, child)) {
                    LOG.log(Level.INFO, "Filtered child item {0}", child);
                    iterator.remove();
                    continue ItemLoop;
                }
            }

            if (bestParent == null) {
                rootItems.add(child);
            } else {
                if (!bestParent.addChild(child))
                    throw new RuntimeException(bestParent + " should have accepted " + child);
            }
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
        else if (item instanceof DiscItemSectorBasedAudioStream)
            return 3;
        else
            return 4;
    }

    private final Comparator<DiscItem> SORT_BY_SECTOR_HIERARHCY = new Comparator<DiscItem>() {
        @Override
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
                return Integer.compare(o2.getEndSector(), o1.getEndSector());
        }
    };

    /** Deserializes the CD index file, and tries to open the CD listed in the index. */
    public DiscIndex(@Nonnull String sIndexFile, @Nonnull ILocalizedLogger errLog)
            throws IndexNotFoundException,
                   IndexReadException,
                   LocalizedDeserializationFail,
                   CdException.FileNotFound,
                   CdException.Read
    {
        this(sIndexFile, null, errLog);
    }

    /** Deserializes the CD index file, and creates a list of items on the CD */
    public DiscIndex(@Nonnull String sIndexFile, @CheckForNull ICdSectorReader cdReader, @Nonnull ILocalizedLogger errLog)
            throws IndexNotFoundException,
                   IndexReadException,
                   LocalizedDeserializationFail,
                   CdException.FileNotFound,
                   CdException.Read
    {
        File indexFile = new File(sIndexFile);

        FileInputStream fis;
        try {
            fis = new FileInputStream(indexFile);
        } catch (FileNotFoundException ex) {
            throw new IndexNotFoundException(indexFile, ex);
        }

        String sSourceCdLine = null;
        ArrayList<String> serializedLines = new ArrayList<String>();

        // ..........................................................
        // read all lines first, ignoring comments and empty lines
        // Only check that:
        // * header is correct
        // * there is 1 and only 1 serialized CD line
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
        try {
            // make sure the first line matches the current version
            String sLine;
            try {
                sLine = reader.readLine();
            } catch (IOException ex) {
                throw new IndexReadException(indexFile, ex);
            }
            if (!Version.IndexHeader.equals(sLine)) {
                throw new LocalizedDeserializationFail(I.INDEX_HEADER_MISSING());
            }

            // read all the lines, searching for the source CD
            while (true) {
                try {
                    sLine = reader.readLine();
                } catch (IOException ex) {
                    throw new IndexReadException(indexFile, ex);
                }

                if (sLine == null)
                    break;

                // comments
                if (sLine.startsWith(COMMENT_LINE_START))
                    continue;

                // blank lines
                if (sLine.matches("^\\s*$"))
                    continue;

                // source file
                if (sLine.startsWith(SerializedDisc.SERIALIZATION_START)) {
                    if (sSourceCdLine != null) {
                        throw new LocalizedDeserializationFail(
                                I.INDEX_MULTIPLE_CD(SerializedDisc.SERIALIZATION_START));
                    }
                    sSourceCdLine = sLine;
                } else {
                    // save the line for deserializing later
                    serializedLines.add(sLine);
                }
            }
        } finally {
            IO.closeSilently(reader, LOG);
        }

        // ..........................................................
        // open or compare the serialized CD
        if (sSourceCdLine == null) {
            throw new LocalizedDeserializationFail(I.INDEX_NO_CD(SerializedDisc.SERIALIZATION_START));
        } else {
            if (cdReader != null) {
                // verify that the source file matches
                if (!cdReader.matchesSerialization(sSourceCdLine)) {
                    errLog.log(Level.WARNING, I.CD_FORMAT_MISMATCH(cdReader.serialize(), sSourceCdLine));
                }
                _sourceCD = cdReader;
            } else {
                _sourceCD = CdOpener.deserialize(sSourceCdLine);
            }
        }

        boolean blnExceptionThrown = true;
        try {
            // setup indexers
            List<DiscIndexer> indexers = DiscIndexer.createIndexers(errLog);
            for (DiscIndexer indexer : indexers) {
                indexer.indexInit(_iterate, _sourceCD);
            }

            // ..........................................................
            // now create the disc items
            for (String sItemLine : serializedLines) {

                SerializedDiscItem deserializedLine;
                // malformed line?
                try {
                    deserializedLine = new SerializedDiscItem(sItemLine);
                } catch (LocalizedDeserializationFail ex) {
                    errLog.log(Level.WARNING, I.INDEX_PARSE_LINE_FAIL(sItemLine, ex.getSourceMessage()), ex);
                    continue;
                }

                // try to find an indexer that recognises the line
                boolean blnLineHandled = false;
                for (DiscIndexer indexer : indexers) {
                    try {
                        DiscItem item = indexer.deserializeLineRead(deserializedLine);
                        if (item != null) {
                            blnLineHandled = true;

                            if (item.notEntirelyInCd()) {
                                errLog.log(Level.WARNING, I.NOT_CONTAINED_IN_DISC(item.getIndexId().toString()));
                            }

                            _iterate.add(item);
                        }
                    } catch (LocalizedDeserializationFail ex) {
                        errLog.log(Level.WARNING, I.INDEX_PARSE_LINE_FAIL(sItemLine, ex.getSourceMessage()), ex);
                        blnLineHandled = true;
                    }
                }
                if (!blnLineHandled)
                    errLog.log(Level.WARNING, I.INDEX_UNHANDLED_LINE(sItemLine));
            }

            _root = recreateTree(_iterate, errLog);

            // ..........................................................
            // copy the items to this class
            for (DiscItem item : _iterate) {
                addLookupItem(item);
            }

            // ..........................................................
            // notify the indexers that the list has been generated
            for (DiscIndexer indexer : indexers) {
                indexer.indexGenerated(this);
            }

            // ..........................................................
            // important to log index for when issues are reported
            if (LOG.isLoggable(Level.INFO)) {
                for (DiscItem item : this)
                    LOG.info(item.toString());
            }

            // no exception thrown, don't close the CD in finally block
            blnExceptionThrown = false;
        } finally {
            if (blnExceptionThrown) {
                // something bad happened? close CD reader only if we opened it
                if (cdReader == null)
                    IO.closeSilently(_sourceCD, LOG);
            }
        }
    }

    private static @Nonnull ArrayList<DiscItem> recreateTree(@Nonnull Collection<DiscItem> allItems, @Nonnull ILocalizedLogger log) {
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
                        log.log(Level.WARNING, I.INDEX_INCONSTSTENCIES());
                        LOG.log(Level.WARNING, "{0} rejected {1}", new Object[]{possibleParent, child});
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
            throws FileNotFoundException
    {
        PrintStream ps;
        try {
            ps = new PrintStream(file, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Every implementation of the Java platform is required to support UTF-8", ex);
        }
        try {
            serializeIndex(ps);
        } finally {
            ps.close();
        }
    }

    /** Serializes the list of disc items to a stream. */
    private void serializeIndex(@Nonnull PrintStream ps) {
        ps.println(Version.IndexHeader);
        ps.println(I.INDEX_COMMENT(COMMENT_LINE_START));
        // TODO: Serialize the CD file location relative to where this index file is being saved
        ps.println(_sourceCD.serialize());
        for (DiscItem item : this) {
            ps.println(item.serialize().serialize());
        }
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

    public @Nonnull ICdSectorReader getSourceCd() {
        return _sourceCD;
    }

    public int size() {
        return _iterate.size();
    }

    @Override
    public @Nonnull Iterator<DiscItem> iterator() {
        return _iterate.iterator();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %d items", _sourceCD.getSourceFile(), _sDiscName, _iterate.size());
    }

    /** Partial indexer to monitor sector headers and warn if anything fishy is detected. */
    private static class SectorHeaderChecker {

        @Nonnull
        private final ILocalizedLogger _log;
        private int _iCurrentHeaderSectorNumber = -1;
        private int _iMode1Count = 0;
        private int _iMode2Count = 0;

        public SectorHeaderChecker(@Nonnull ProgressLogger pl) {
            _log = pl;
        }

        public void indexingSectorRead(@Nonnull CdSector cdSector) {
            if (LOG_CORRUPTED_SECTORS && cdSector.hasHeaderErrors())
                _log.log(Level.WARNING, I.INDEX_SECTOR_CORRUPTED(cdSector.getSectorIndexFromStart()));

            CdSectorHeader h = cdSector.getHeader();
            if (h != null) {
                int iNewSectNumber = h.calculateSectorNumber();
                if (iNewSectNumber != -1) {
                    if (_iCurrentHeaderSectorNumber >= 0) {
                        if (_iCurrentHeaderSectorNumber + 1 != iNewSectNumber) {
                            _log.log(Level.WARNING, I.INDEX_SECTOR_CORRUPTED_AT(cdSector.getSectorIndexFromStart()));
                            LOG.log(Level.WARNING, "Non-continuous sector header number: {0} -> {1}", new Object[]{_iCurrentHeaderSectorNumber, iNewSectNumber});
                        }
                    }
                    _iCurrentHeaderSectorNumber = iNewSectNumber;
                } else {
                    _iCurrentHeaderSectorNumber = -1;
                }
            } else {
                _iCurrentHeaderSectorNumber = -1;
            }

            switch (cdSector.getType()) {
                case MODE1:
                    if (_iMode1Count < _iMode2Count) {
                        _log.log(Level.WARNING, I.INDEX_SECTOR_CORRUPTED_AT(cdSector.getSectorIndexFromStart()));
                        LOG.log(Level.WARNING, "Sector {0} is Mode 1 found among Mode 2 sectors", new Object[]{cdSector.getSectorIndexFromStart()});
                    }
                    _iMode1Count++;
                    break;
                case UNKNOWN2048:
                case MODE2FORM1:
                case MODE2FORM2:
                    _iMode2Count++;
                    break;
                case CD_AUDIO:
                    // CD audio sectors aren't a big deal here
            }

        }

    }
}
