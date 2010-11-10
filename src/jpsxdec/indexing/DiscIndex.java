/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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
import jpsxdec.util.FeedbackStream;
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
    private ArrayList<IndexId> _root;
    private final ArrayList<DiscItem> _iterate;

    private final LinkedHashMap<Object, DiscItem> _lookup = new LinkedHashMap<Object, DiscItem>();

    /** Finds all the media on the CD.  */
    public DiscIndex(CdFileSectorReader cdReader, ProgressListener pl) throws TaskCanceledException {
        _sourceCD = cdReader;
        
        DiscIndexer[] aoIndexers = DiscIndexer.createIndexers();

        _iterate = new ArrayList<DiscItem>();

        for (DiscIndexer indexer : aoIndexers) {
            indexer.putYourCompletedMediaItemsHere(_iterate);
        }

        pl.progressStart();

        for (int iSector = 0; iSector < _sourceCD.getLength(); iSector++) {
            try {

                CdSector cdSector = _sourceCD.getSector(iSector);
                IdentifiedSector identSect = IdentifiedSector.identifySector(cdSector);

                if (identSect != null) {
                    if (log.isLoggable(Level.FINE))
                        log.fine(identSect.toString());

                    for (DiscIndexer indexer : aoIndexers) {
                        indexer.indexingSectorRead(identSect);
                    }
                } else {
                    if (log.isLoggable(Level.FINER))
                        log.finer(cdSector.toString());
                }

                if (pl.seekingEvent())
                    pl.event(String.format("Sector %d / %d  %d items found", iSector, _sourceCD.getLength(), _iterate.size()));

            } catch (IOException ex) {
                if (pl != null)
                    pl.error(ex);
                log.log(Level.WARNING, "Error reading sector "+iSector+" while indexing disc", ex);
            }
            pl.progressUpdate(iSector / (double)_sourceCD.getLength());
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

        SortByOverlap overlapSort = new SortByOverlap();
        for (IndexId video : videos) {
            overlapSort.overlapper = video.getItem();
            Collections.sort(video, overlapSort);
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

    private static class SortByOverlap implements Comparator<IndexId> {
        public DiscItem overlapper;
        public int compare(IndexId o1, IndexId o2) {
            int o1overlap = o1.getItem().getOverlap(overlapper);
            int o2overlap = o2.getItem().getOverlap(overlapper);
            if (o1overlap < o2overlap)
                return -1;
            else if (o1overlap > o2overlap)
                return 1;
            else
                return 0;
        };
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
    public DiscIndex(String sSerialFile, FeedbackStream fbs)
            throws IOException, NotThisTypeException
    {
        this(sSerialFile, (CdFileSectorReader)null, fbs);
    }

    /** Deserializes the CD index file, and creates a list of media items on the CD */
    public DiscIndex(String sIndexFile, boolean blnAllowWrites, FeedbackStream fbs)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, null, blnAllowWrites, fbs);
    }

    /** Deserializes the CD index file, and creates a list of media items on the CD */
    public DiscIndex(String sIndexFile, CdFileSectorReader cdReader, FeedbackStream fbs)
            throws IOException, NotThisTypeException
    {
        this(sIndexFile, cdReader, false, fbs);
    }

    private DiscIndex(String sIndexFile, CdFileSectorReader cdReader, boolean blnAllowWrites, FeedbackStream fbs)
            throws IOException, NotThisTypeException
    {
        _sourceCD = cdReader;

        File indexFile = new File(sIndexFile);

        BufferedReader reader = new BufferedReader(new FileReader(indexFile));
        
        _iterate = new ArrayList<DiscItem>();

        DiscIndexer[] aoIndexers = DiscIndexer.createIndexers();
        for (DiscIndexer indexer : aoIndexers) {
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
                        fbs.printlnWarn("Warning: Disc format does not match what index says.");
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
                for (DiscIndexer indexer : aoIndexers) {
                    // first parse the indexid
                    DiscItem item = indexer.deserializeLineRead(deserializedLine);
                    if (item != null) {
                        blnLineHandled = true;
                        item.setIndexId(new IndexId(sIndexId, item));
                        _iterate.add(item);
                    }
                }
                if (!blnLineHandled)
                    fbs.printlnWarn("Failed to do anything with " + sLine);
            } catch (NotThisTypeException e) {
                if (fbs != null) fbs.printlnWarn("Failed to parse line: " + sLine);
            }
        }
        reader.close();

        _root = recreateTree(_iterate);

        // copy the items to this class
        for (DiscItem item : _iterate) {
            addLookupItem(item);
        }
        // notify the indexers that the list has been generated
        for (DiscIndexer indexer : aoIndexers) {
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

        //recursePrint(root, "");
        return root;
    }

    private static void recursePrint(List<IndexId> tree, String sIndent) {
        for (IndexId id : tree) {
            System.out.println(sIndent + id);
            recursePrint(id, sIndent + "  ");
        }
    }



    /** Adds a media item to the internal hash and array. */
    private void addLookupItem(DiscItem item) {
        IndexId id = (IndexId) item.getIndexId();
        item.setSourceCD(_sourceCD);
        _lookup.put(Integer.valueOf(id.getListIndex()), item);
        _lookup.put(id.serialize(), item);
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
