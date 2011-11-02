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

import jpsxdec.discitems.IndexId;
import jpsxdec.util.ProgressListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.Main;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSerialization;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemISO9660File;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.TaskCanceledException;

/** Searches for, and manages the collection of DiscItems in a file.
 *  PlayStation files (discs, STR, XA, etc.) can contain multiple media items.
 *  This will search for them, and hold the resulting list. It will also
 *  serialize/deserialize to/from a file.  */
public class DiscIndex implements Iterable<DiscItem> {

    private static final Logger log = Logger.getLogger(DiscIndex.class.getName());

    private static final String INDEX_HEADER = "[" + Main.VerString + "]";

    private static final String COMMENT_LINE_START = ";";
    
    private CdFileSectorReader _sourceCD;
    private String _sDiscName = null;
    private final ArrayList<IndexId> _root;
    private final ArrayList<DiscItem> _iterate;

    private final LinkedHashMap<Object, DiscItem> _lookup = new LinkedHashMap<Object, DiscItem>();

    /** Finds all the media on the CD.  */
    public DiscIndex(CdFileSectorReader cdReader, final ProgressListener pl) throws TaskCanceledException {
        _sourceCD = cdReader;
        
        final DiscIndexer[] aoIndexers = DiscIndexer.createIndexers(pl.getLog());

        DiscIndexer[] aoStaticIndexers = {
            new DiscIndexerTim(),
            //new DiscIndexerMdec(),
            //new DiscIndexerLzs()
        };

        _iterate = new ArrayList<DiscItem>();

        for (DiscIndexer indexer : aoIndexers) {
            indexer.putYourCompletedMediaItemsHere(_iterate);
        }
        for (DiscIndexer indexer : aoStaticIndexers) {
            indexer.putYourCompletedMediaItemsHere(_iterate);
        }


        DiscriminatingSectorIterator sectIter =
                new DiscriminatingSectorIterator(cdReader, 0);

        final TaskCanceledException[] aoTaskCanceled = { null };

        sectIter.setListener(new DiscriminatingSectorIterator.SectorReadListener() {
            int iCurrentSectorNumber = -1;
            public void sectorRead(CdSector sect) {
                sect.printErrors(pl.getLog());
                if (sect.hasRawSectorHeader()) {
                    if (iCurrentSectorNumber < 0) {
                        iCurrentSectorNumber = sect.getHeaderSectorNumber();
                    }  else {
                        if (iCurrentSectorNumber + 1 != sect.getHeaderSectorNumber())
                            pl.getLog().warning("Non-continuous sector header index: " +
                                    iCurrentSectorNumber + " -> " + sect.getHeaderSectorNumber());
                        iCurrentSectorNumber = sect.getHeaderSectorNumber();
                    }
                }

                int iSector = sect.getSectorNumberFromStart();
                try {
                    pl.progressUpdate(iSector / (double)_sourceCD.getLength());
                } catch (TaskCanceledException ex) {
                    aoTaskCanceled[0] = ex;
                }

                if (pl.seekingEvent())
                    pl.event(String.format("Sector %d / %d  %d items found", iSector, _sourceCD.getLength(), _iterate.size()));
            }
        });

        pl.progressStart();
        
        try {

            while (!sectIter.isEndOfDisc()) {

                while (sectIter.hasNextIdentified()) {

                    IdentifiedSector idSect = sectIter.nextIdentified();
                    
                    for (DiscIndexer indexer : aoIndexers) {

                        indexer.indexingSectorRead(idSect);

                        if (aoTaskCanceled[0] != null)
                            throw aoTaskCanceled[0];
                    }
                }

                if (sectIter.hasNextUnidentified()) {
                    DemuxedUnidentifiedDataStream staticStream = new DemuxedUnidentifiedDataStream(sectIter);

                    for (; staticStream.headHasMore(); staticStream.incrementStartAndReset(4)) {

                        aoStaticIndexers[0].staticRead(staticStream);

                        if (aoTaskCanceled[0] != null)
                            throw aoTaskCanceled[0];

                        for (int i = 1; i < aoStaticIndexers.length; i++) {
                            // reset the stream
                            staticStream.reset();

                            // pass it the stream to try
                            aoStaticIndexers[i].staticRead(staticStream);

                            if (aoTaskCanceled[0] != null)
                                throw aoTaskCanceled[0];
                        }

                    }
                    
                }


            }

        } catch (IOException ex) {
            pl.getLog().log(Level.SEVERE, "Error while indexing disc", ex);
        }

        // notify indexers that the disc is finished
        for (DiscIndexer indexer : aoIndexers) {
            indexer.indexingEndOfDisc();
        }

        // sort the list according to the start sector & hierarchy level
        Collections.sort(_iterate, SORT_BY_SECTORHIERARHCY);

        _root = buildTree(_iterate);
        
        // copy the items to the hash
        for (DiscItem item : _iterate) {
            addLookupItem(item);
        }
        
        // notify the indexerss that the list has been generated
        for (DiscIndexer indexer : aoIndexers) {
            indexer.mediaListGenerated(this);
        }

        if (pl.seekingEvent())
            pl.event(String.format("Sector %d / %d  %d items found", _sourceCD.getLength(), _sourceCD.getLength(), _iterate.size()));

        pl.progressEnd();

    }

    private ArrayList<IndexId> buildTree(ArrayList<DiscItem> LIST) {

        ArrayList<IndexId> root = new ArrayList<IndexId>();

        ArrayList<IndexId> files = new ArrayList<IndexId>();
        ArrayList<IndexId> videos = new ArrayList<IndexId>();
        ArrayList<IndexId> nonFiles = new ArrayList<IndexId>();

        // create ids for all the disc items, and take special interest in files and videos
        int iIndex = 0;
        for (DiscItem item : LIST) {
            if (item instanceof DiscItemISO9660File) {
                IndexId itemId = new IndexId(item, iIndex, ((DiscItemISO9660File)item).getPath());
                // files never have parents, so add them to root now
                root.add(itemId);
                // also keep a special list of them for quicker reference
                files.add(itemId);
            } else {
                IndexId itemId = new IndexId(item, iIndex);
                // otherwise keep special list of videos for quick reference
                if (item instanceof DiscItemVideoStream)
                    videos.add(itemId);
                // add them to the default pool
                nonFiles.add(itemId);
            }
            iIndex++;
        }

        EachNonFile:
        for (IndexId nonFile : nonFiles) {

            // if it's audio, first check if it can be a child of a video
            if (nonFile.getItem() instanceof DiscItemAudioStream) {
                for (IndexId video : videos) {
                    if (((DiscItemVideoStream)video.getItem()).isAudioVideoAligned(nonFile.getItem())) {
                        video.add(nonFile);
                        continue EachNonFile;
                    }
                }
            }

            // if not audio, or if audio doesn't have a video parent,
            // check if it can be a child of a file

            IndexId mostOverlap = null;
            int iMostOverlap = 0;
            for (IndexId file : files) {
                int iOverlap = file.getItem().getOverlap(nonFile.getItem());
                if (iOverlap > iMostOverlap) {
                    mostOverlap = file;
                    iMostOverlap = iOverlap;
                }
            }

            if (mostOverlap != null) {
                // it is part of a file
                mostOverlap.add(nonFile);
            } else {
                // if not, add it to root
                root.add(nonFile);
            }
        }

        Comparator<IndexId> sortBySector = new Comparator<IndexId>() {
            public int compare(IndexId o1, IndexId o2) {
                if (o1.getItem().getStartSector() < o2.getItem().getStartSector())
                    return -1;
                else if (o1.getItem().getStartSector() > o2.getItem().getStartSector())
                    return 1;
                else if (o1.getItem().getEndSector() > o2.getItem().getEndSector())
                    return -1;
                else if (o1.getItem().getEndSector() < o2.getItem().getEndSector())
                    return 1;
                else
                    return 0;
            };
        };

        Collections.sort(root, sortBySector);

        for (IndexId file : files) {
            Collections.sort(file, sortBySector);
        }

        for (IndexId video : videos) {
            Collections.sort(video, sortBySector);
        }

        // finally walk through the tree and generate ids for the disc items
        int iChildIndex = 0;
        for (IndexId child : root) {
            iChildIndex = child.recursiveSetTreeIndex(null, null, iChildIndex);
        }

        return root;
    }

    public List<IndexId> getRoot() {
        return _root;
    }


    private static int typeHierarchyLevel(DiscItem item) {
        if (item instanceof DiscItemISO9660File)
            return 1;
        else if (item instanceof DiscItemVideoStream)
            return 2;
        else if (item instanceof DiscItemAudioStream)
            return 3;
        else
            return 4;
    }

    private static Comparator<DiscItem> SORT_BY_SECTORHIERARHCY = new Comparator<DiscItem>() {
        public int compare(DiscItem o1, DiscItem o2) {
            if (o1.getStartSector() < o2.getStartSector())
                return -1;
            else if (o1.getStartSector() > o2.getStartSector())
                return 1;
            else if (typeHierarchyLevel(o1) < typeHierarchyLevel(o2))
                return -1;
            else if (typeHierarchyLevel(o1) > typeHierarchyLevel(o2))
                return 1;
            else if (o1.getEndSector() > o2.getEndSector())
                return -1;
            else if (o1.getEndSector() < o2.getEndSector())
                return 1;
            else
                return 0;
        }
    };

    /** Deserializes the CD index file, and tries to open the CD listed in the index. */
    public DiscIndex(String sIndexFile, Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, (CdFileSectorReader)null, errLog);
    }

    /** Deserializes the CD index file, and creates a list of media items on the CD */
    public DiscIndex(String sIndexFile, boolean blnAllowWrites, Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, null, blnAllowWrites, errLog);
    }

    /** Deserializes the CD index file, and creates a list of media items on the CD */
    public DiscIndex(String sIndexFile, CdFileSectorReader cdReader, Logger errLog)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, cdReader, false, errLog);
    }

    private DiscIndex(String sIndexFile, CdFileSectorReader cdReader, boolean blnAllowWrites, Logger errLog)
            throws IOException, NotThisTypeException
    {
        _sourceCD = cdReader;

        File indexFile = new File(sIndexFile);

        BufferedReader reader = new BufferedReader(new FileReader(indexFile));
        
        _iterate = new ArrayList<DiscItem>();

        ArrayList<DiscIndexer> indexers = new ArrayList<DiscIndexer>();
        indexers.addAll(Arrays.asList(DiscIndexer.createIndexers(errLog)));
        indexers.add(new DiscIndexerTim());

        for (DiscIndexer indexer : indexers) {
            indexer.putYourCompletedMediaItemsHere(_iterate);
        }

        // make sure the first line matches the current version
        String sLine = reader.readLine();
        if (!INDEX_HEADER.equals(sLine)) {
            reader.close();
            throw new NotThisTypeException("Missing proper index header.");
        }

        while ((sLine = reader.readLine()) != null) {
            
            // comments
            if (sLine.startsWith(COMMENT_LINE_START))
              continue;

            // source file
            if (sLine.startsWith(CdFileSectorReader.SERIALIZATION_START)) {
                if (cdReader != null) {
                    if (!sLine.equals(cdReader.serialize())) {
                        errLog.warning("Warning: Disc format does not match what index says.");
                    }
                } else {
                    _sourceCD = new CdFileSectorReader(sLine, blnAllowWrites);
                }
                continue;
            }

            String[] asParts = sLine.split("\\|", 2);
            String sIndexId = asParts[0];
            String sItem = asParts[1];
            
            try {
                DiscItemSerialization deserializedLine = new DiscItemSerialization(sItem);

                boolean blnLineHandled = false;
                for (DiscIndexer indexer : indexers) {
                    // first parse the indexid
                    DiscItem item = indexer.deserializeLineRead(deserializedLine);
                    if (item != null) {
                        blnLineHandled = true;
                        item.setIndexId(new IndexId(sIndexId, item));
                        _iterate.add(item);
                    }
                }
                if (!blnLineHandled)
                    errLog.warning("Failed to do anything with " + sLine);
            } catch (NotThisTypeException e) {
                errLog.warning("Failed to parse line: " + sLine);
            }
        }
        reader.close();

        _root = recreateTree(_iterate);

        // copy the items to this class
        for (DiscItem item : _iterate) {
            addLookupItem(item);
        }
        // notify the indexers that the list has been generated
        for (DiscIndexer indexer : indexers) {
            indexer.mediaListGenerated(this);
        }


        // debug print the list contents
        if (log.isLoggable(Level.INFO)) {
            for (DiscItem item : this) log.info(item.toString());
        }

    }

    private ArrayList<IndexId> recreateTree(ArrayList<DiscItem> LIST) {
        // TODO: optimize this
        for (DiscItem item : LIST) {
            IndexId id = item.getIndexId();
            id.findAndAddChildren(LIST);
        }

        ArrayList<IndexId> root = new ArrayList<IndexId>();
        for (DiscItem item : LIST) {
            IndexId id = item.getIndexId();
            if (id.isRoot()) {
                root.add(id);
            }
        }

        return root;
    }


    /** Adds a media item to the internal hash and array. */
    private void addLookupItem(DiscItem item) {
        IndexId id = (IndexId) item.getIndexId();
        item.setSourceCD(_sourceCD);
        _lookup.put(Integer.valueOf(id.getListIndex()), item);
        _lookup.put(id.getId(), item);
    }


    /** Serializes the list of media items to a file. */
    public void serializeIndex(PrintStream ps)
            throws IOException
    {
        ps.println(INDEX_HEADER);
        ps.println(COMMENT_LINE_START + " Lines that begin with "+COMMENT_LINE_START+" are ignored");
        // TODO: Serialize the CD file location relative to where this index file is being saved
        ps.println(_sourceCD.serialize());
        for (DiscItem item : this) {
            ps.println(item.getIndexId().serialize() + "|" + item.serialize().serialize());
        }
        ps.close();
    }

    public void setDiscName(String sName) {
        _sDiscName = sName;
    }

    public DiscItem getByIndex(int iIndex) {
        return _lookup.get(Integer.valueOf(iIndex));
    }

    public DiscItem getById(String sId) {
        return _lookup.get(sId);
    }
    
    public boolean hasIndex(int iIndex) {
        return _lookup.containsKey(Integer.valueOf(iIndex));
    }

    public CdFileSectorReader getSourceCD() {
        return _sourceCD;
    }
    
    public int size() {
        return _iterate.size();
    }
    
    /* [implements Iterable] */
    public Iterator<DiscItem> iterator() {
        return _iterate.iterator();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %d items", _sourceCD.getSourceFile(), _sDiscName, _iterate.size());
    }
}
