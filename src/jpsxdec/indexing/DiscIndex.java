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
import java.util.LinkedList;
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
import jpsxdec.discitems.FileBasedId;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.NotThisTypeException;

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

    private final LinkedHashMap<Integer, DiscItem> _mediaHash = new LinkedHashMap<Integer, DiscItem>();

    private final RootNode _rootTreeNode;

    /** Finds all the media on the CD.  */
    public DiscIndex(CdFileSectorReader cdReader, ProgressListener pl) {
        _sourceCD = cdReader;
        
        DiscIndexer[] aoIndexers = DiscIndexer.createIndexers();

        ArrayList<DiscItem> completedItems = new ArrayList<DiscItem>();

        for (DiscIndexer indexer : aoIndexers) {
            indexer.putYourCompletedMediaItemsHere(completedItems);
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

                pl.event(String.format("Sector %d / %d  %d items found", iSector, _sourceCD.getLength(), completedItems.size()));
                pl.progressUpdate(iSector / (double)_sourceCD.getLength());

            } catch (IOException ex) {
                if (pl != null)
                    pl.error(ex);
                log.log(Level.WARNING, "Error reading sector "+iSector+" while indexing disc", ex);
            }
        }

        pl.progressEnd();

        // notify indexers that the disc is finished
        for (DiscIndexer indexer : aoIndexers) {
            indexer.indexingEndOfDisc();
        }

        // sort the list according to the start sector & hierarchy level
        Collections.sort(completedItems, new DiscItemCompare());

        // copy the items to this class
        for (DiscItem item : completedItems) {
            addMediaItemInc(item);
        }
        // notify the indexerss that the list has been generated
        for (DiscIndexer indexer : aoIndexers) {
            indexer.mediaListGenerated(this);
        }

        _rootTreeNode = buildTree();
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

    private static class DiscItemCompare implements Comparator<DiscItem> {
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
    }

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
        
        ArrayList<DiscItem> itemList = new ArrayList<DiscItem>();

        DiscIndexer[] aoIndexers = DiscIndexer.createIndexers();
        for (DiscIndexer indexer : aoIndexers) {
            indexer.putYourCompletedMediaItemsHere(itemList);
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
            
            try {
                DiscItemSerialization deserializedLine = new DiscItemSerialization(sLine);

                boolean blnLineHandled = false;
                for (DiscIndexer indexer : aoIndexers) {
                    indexer.deserializeLineRead(deserializedLine);
                    blnLineHandled = true;
                }
                if (!blnLineHandled)
                    fbs.printlnWarn("Failed to do anything with " + sLine);
            } catch (NotThisTypeException e) {
                if (fbs != null) fbs.printlnWarn("Failed to parse line: " + sLine);
            }
        }
        reader.close();


        // copy the items to this class
        for (DiscItem item : itemList) {
            addMediaItem(item);
        }
        // notify the indexers that the list has been generated
        for (DiscIndexer indexer : aoIndexers) {
            indexer.mediaListGenerated(this);
        }

        _rootTreeNode = buildTree();

        // debug print the list contents
        if (log.isLoggable(Level.INFO)) {
            for (DiscItem item : this) log.info(item.toString());
        }

    }

    private abstract static class Node implements Comparable<Node> {
        private ArrayList<Node> _children;

        public int getChildCount() {
            return _children == null ? 0 : _children.size();
        }

        public int indexOf(Node child) {
            return _children == null ? -1 : _children.indexOf(child);
        }

        public Node getChild(int i) {
            return _children.get(i);
        }

        public void addChild(Node child) {
            if (_children == null)
                _children = new ArrayList<Node>();
            _children.add(child);
            Collections.sort(_children);
        }

        abstract public Object value();

        public DirectoryNode getOrCreateDir(String sName) {
            for (Node node : _children) {
                if (node instanceof DirectoryNode) {
                    DirectoryNode dirNode = (DirectoryNode) node;
                    if ( dirNode.getName().equals(sName) ) {
                        return dirNode;
                    }
                }
            }
            DirectoryNode dirNode = new DirectoryNode(sName);
            _children.add(dirNode);
            return dirNode;
        }
    }

    private static class RootNode extends Node {

        public int compareTo(Node o) {
            return 1;
        }

        public String toString() {
            return "Root (" + getChildCount() + ")";
        }

        @Override
        public Object value() {
            return "Root";
        }
    }

    private static class PhysicalNode extends Node {

        private DiscItem _discItem;

        public PhysicalNode(DiscItem item) {
            _discItem = item;
        }

        public DiscItem getDiscItem() {
            return _discItem;
        }

        public int compareTo(Node o) {
            if (o instanceof PhysicalNode)
                return new DiscItemCompare().compare(_discItem, ((PhysicalNode)o)._discItem);
            else
                return -1;
        }

        public String toString() {
            return _discItem.getUniqueId().serialize();
        }

        public Object value() {
            return _discItem;
        }
    }

    private static class DirectoryNode extends Node {
        private String _sName;

        public DirectoryNode(String sName) {
            _sName = sName;
        }

        public String getName() {
            return _sName;
        }

        public int compareTo(Node o) {
            if (o instanceof DirectoryNode) {
                return _sName.compareTo(((DirectoryNode)o)._sName);
            } else {
                return 1;
            }
        }

        public String toString() {
            return "/" + _sName + "/  (" + getChildCount() + ")";
        }

        @Override
        public Object value() {
            return "(dir)";
        }
    }


    private String[] splitFileDirs(File file) {
        ArrayList<String> dirs = new ArrayList<String>();
        File parent;
        while ((parent = file.getParentFile()) != null) {
            dirs.add(parent.getName());
            file = parent;
        }
        
        return dirs.toArray(new String[dirs.size()]);
    }

    private RootNode buildTree() {

        RootNode rootNode = new RootNode();

        // create nodes for all the disc items
        LinkedList<PhysicalNode> nodeItems = new LinkedList<PhysicalNode>();
        for (DiscItem item : this) {
            nodeItems.add(new PhysicalNode(item));
        }

        // identify just the file items
        // begin to build the tree with them at the root
        // remove those file nodes from the pool
        ArrayList<PhysicalNode> fileNodes = new ArrayList<PhysicalNode>();
        for (Iterator<PhysicalNode> it = nodeItems.iterator(); it.hasNext();) {
            PhysicalNode node = it.next();
            if (node.getDiscItem() instanceof DiscItemISO9660File) {
                DiscItemISO9660File fileItem = (DiscItemISO9660File) node.getDiscItem();
                String[] asDirs = splitFileDirs(fileItem.getPath());

                Node tree = rootNode;
                for (String sDir : asDirs) {
                    tree = tree.getOrCreateDir(sDir);
                }
                tree.addChild(node);
                fileNodes.add(node);
                
                it.remove();
            }
        }

        // move all audio nodes to be children of video nodes where possible
        for (Iterator<PhysicalNode> audIt = nodeItems.iterator(); audIt.hasNext();) {
            PhysicalNode audioNode = audIt.next();
            if (audioNode.getDiscItem() instanceof DiscItemAudioStream) {
                for (Iterator<PhysicalNode> vidIt = nodeItems.iterator(); vidIt.hasNext();) {
                    PhysicalNode videoNode = vidIt.next();
                    if (videoNode.getDiscItem() instanceof DiscItemVideoStream) {
                        DiscItemAudioStream audioItem = (DiscItemAudioStream) audioNode.getDiscItem();
                        DiscItemVideoStream videoItem = (DiscItemVideoStream) videoNode.getDiscItem();
                        if (videoItem.isAudioVideoAligned(audioItem)) {
                            videoNode.addChild(audioNode);
                            audIt.remove();
                            break;
                        }
                    }
                }
            }
        }

        // add nodes as children of files if possible,
        // otherwise just add to the root
        for (PhysicalNode nonFileNode : nodeItems) {
            PhysicalNode mostOverlapFileNode = null;
            int iMostOverlap = 0;
            for (PhysicalNode fileNode : fileNodes) {
                int iOverlap = fileNode.getDiscItem().getOverlap(nonFileNode.getDiscItem());
                if (iOverlap > iMostOverlap) {
                    mostOverlapFileNode = fileNode;
                    iMostOverlap = iOverlap;
                }
            }
            if (mostOverlapFileNode == null) {
                rootNode.addChild(nonFileNode);
            } else {
                mostOverlapFileNode.addChild(nonFileNode);
            }
        }

        // finally walk through the tree and generate ids for the disc items
        FileBasedId id = null;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            id = updateIds(rootNode.getChild(i), null, id);
        }

        return rootNode;
    }

    private FileBasedId updateIds(Node node, FileBasedId parentId, FileBasedId siblingId) {

        if (node instanceof PhysicalNode) {
            DiscItem item = ((PhysicalNode)node).getDiscItem();
            if (item instanceof DiscItemISO9660File) {
                siblingId = new FileBasedId( ((DiscItemISO9660File)((PhysicalNode)node).getDiscItem()).getPath() );
                item.setUniqueId(siblingId);
            } else {
                if (siblingId == null) {
                    if (parentId == null)
                        siblingId = new FileBasedId();
                    else
                        siblingId = parentId.newChild();
                } else
                    siblingId = siblingId.newIncrement();

                item.setUniqueId(siblingId);

            }
        }

        FileBasedId childId = null;
        for (int i = 0; i < node.getChildCount(); i++) {
            childId = updateIds(node.getChild(i), siblingId, childId);
        }
        return siblingId;
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
            ps.println(item.serialize().serialize());
        }
        ps.close();
    }

    public void setDiscName(String sName) {
        _sDiscName = sName;
    }

    /** Adds a media item to the internal hash and array. */
    private void addMediaItemInc(DiscItem item) {
        int iIndex = _mediaHash.size();
        item.setIndex(iIndex);
        item.setSourceCD(_sourceCD);
        _mediaHash.put(Integer.valueOf(iIndex), item);
    }
    
    /** Adds a media item to the internal hash and array. */
    private void addMediaItem(DiscItem item) {
        item.setSourceCD(_sourceCD);
        _mediaHash.put(Integer.valueOf(item.getIndex()), item);
    }

    public DiscItem getByIndex(int iIndex) {
        return _mediaHash.get(Integer.valueOf(iIndex));
    }
    
    public boolean hasIndex(int iIndex) {
        return _mediaHash.containsKey(Integer.valueOf(iIndex));
    }

    public CdFileSectorReader getSourceCD() {
        return _sourceCD;
    }
    
    public int size() {
        return _mediaHash.size();
    }
    
    /* [implements Iterable] */
    public Iterator<DiscItem> iterator() {
        return _mediaHash.values().iterator();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %d items", _sourceCD.getSourceFile(), _sDiscName, _mediaHash.size());
    }

    public int getNodeChildCount(Object node) {
        return ((Node)node).getChildCount();
    }

    public Object getNodeChild(Object node, int i) {
        return ((Node)node).getChild(i);
    }

    public Object getRoot() {
        return _rootTreeNode;
    }

    public int getIndexOfChild(Object parent, Object child) {
        return ((Node)parent).indexOf((Node)child);
    }

    public Object getNodeValue(Object node) {
        return ((Node)node).value();
    }

}
