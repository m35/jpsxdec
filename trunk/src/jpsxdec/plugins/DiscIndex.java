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

package jpsxdec.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import jpsxdec.Main;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.util.NotThisTypeException;

/** Searches for, and manages the collection of DiscItem items in a file.
 *  Playstation files (discs, STR, XA, etc.) can contain many media items.
 *  This will search for them, and hold the resulting list. It will also
 *  serialize to and deserialize from a stream.
 * 
 *  Note that this doesn't try to match parallel media items. See the
 *  ParallelMediaList for that.
 */
public class DiscIndex extends AbstractListModel implements Iterable<DiscItem> {

    private static final Logger log = Logger.getLogger(DiscIndex.class.getName());

    private static final String INDEX_HEADER = "[" + Main.VerString + "]";
    
    private final Hashtable<Object, DiscItem> _mediaHash = new Hashtable<Object, DiscItem>();
    private final ArrayList<DiscItem> _mediaList = new ArrayList<DiscItem>();
    private final CDSectorReader _sourceCD;
    private String _sDiscName = null;

    /** Count how many disc items report single speed playback. */
    private int _iSingleSpeedItemCount = 0;
    /** Count how many disc items report double speed playback. */
    private int _iDoubleSpeedItemCount = 0;
    
    public DiscIndex(CDSectorReader cdReader) {
        _sourceCD = cdReader;
    }
    
    /*
     * This appears to focus on just reading static data, but behind the scenes
     * it is also reading streams.
     *
     * After all the stream vectors have been identified
     * Then we need to find
     * -audio stream vectors that don't overlap video stream vectors
     * -video stream vectors that don't overlap audio stream vectors
     * -and overlapping audio/video stream vectors
     * -compare the ISO9660 file list to the sector ranges of the media
     * 
     */
    
    /** Finds all the media on the CD.
     * @param oListener  Optional progress listener.  */
    public void indexDisc(ProgressListener pl) {
        
        // ready to start indexing, so clear the existing lists
        _mediaHash.clear();
        _mediaList.clear();

        JPSXPlugin[] aoPlugins = JPSXPlugin.getPlugins();

        ArrayList<DiscItem> oCompletedItems = new ArrayList<DiscItem>();

        for (JPSXPlugin oIndexer : aoPlugins) {
            oIndexer.putYourCompletedMediaItemsHere(oCompletedItems);
        }

        pl.progressStart();

        for (int iSector = 0; iSector < _sourceCD.size(); iSector++) {
            try {

                CDSector cdSector = _sourceCD.getSector(iSector);
                IdentifiedSector identSect = JPSXPlugin.identifyPluginSector(cdSector);

                if (identSect != null) {
                    if (log.isLoggable(Level.FINE))
                        log.fine(identSect.toString());

                    for (JPSXPlugin plugin : aoPlugins) {
                        plugin.indexing_sectorRead(identSect);
                    }
                } else {
                    if (log.isLoggable(Level.FINER))
                        log.finer(cdSector.toString());
                }

                pl.event("Sector " + iSector + " / " + _sourceCD.size());
                pl.progressUpdate(iSector / (double)_sourceCD.size());

            } catch (IOException ex) {
                if (pl != null)
                    pl.error(ex);
                log.log(Level.WARNING, "Error reading sector "+iSector+" while indexing disc", ex);
            }
        }

        pl.progressEnd();

        // notify indexers that the disc is finished
        for (JPSXPlugin plugin : aoPlugins) {
            plugin.indexing_endOfDisc();
        }

        // sort the list according to the start sector
        Collections.sort(oCompletedItems, new Comparator<DiscItem>() {
            public int compare(DiscItem o1, DiscItem o2) {
                if (o1.getStartSector() < o2.getStartSector())
                    return -1;
                else if (o1.getStartSector() > o2.getStartSector())
                    return 1;
                else if (o1.getEndSector() > o2.getEndSector())
                    return -1;
                else if (o1.getEndSector() < o2.getEndSector())
                    return 1;
                else
                    return 0;
            }
        });

        // copy the items to this class
        for (DiscItem item : oCompletedItems) {
            addMediaItemInc(item);
        }
        // notify the plugins that the list has been generated
        for (JPSXPlugin plugin : aoPlugins) {
            plugin.mediaListGenerated(this);
        }
        // debug print the list contents
        if (log.isLoggable(Level.INFO)) {
            for (DiscItem item : this) log.info(item.toString());
        }

    }
    
    /** Deserializes the CD index file, and creates a
     *  list of media items on the CD */
    public void deserializeIndex(String sSerialFile) 
            throws IOException, NotThisTypeException
    {
        //TODO: Want to read as much as we can from the file before an exception
        //      and return what we can get, but we also want to return that
        //      there was an error.

        // TODO: Check that the disc matches what the serial file says

        BufferedReader reader = new BufferedReader(new FileReader(sSerialFile));
        String sLine;
        
        // ready to deserialize, so clear the existing lists
        _mediaHash.clear();
        _mediaList.clear();

        ArrayList<DiscItem> itemList = new ArrayList<DiscItem>();

        JPSXPlugin[] aoPlugins = JPSXPlugin.getPlugins();
        for (JPSXPlugin plugin : aoPlugins) {
            plugin.putYourCompletedMediaItemsHere(itemList);
        }

        // make sure the first line matches the current version
        sLine = reader.readLine();
        if (!INDEX_HEADER.equals(sLine)) {
            reader.close();
            throw new NotThisTypeException("Missing proper index header.");
        }

        while ((sLine = reader.readLine()) != null) {
            
            // comments
            if (sLine.startsWith(";"))
              continue;

            // source file
            if (sLine.startsWith("SourceFile|")) {
                String[] asParts = sLine.split("|");
                if (asParts.length != 5) continue;
                //TODO: Check if cd length matches serialized length
                continue;
            }
            
            try {
                DiscItemSerialization oDeserializedLine = new DiscItemSerialization(sLine);

                for (JPSXPlugin plugin : aoPlugins) {
                    plugin.deserialize_lineRead(oDeserializedLine);
                }
            } catch (NotThisTypeException e) {
                System.err.println("Failed to parse line: " + sLine);
            }
        }
        reader.close();


        // copy the items to this class
        for (DiscItem item : itemList) {
            addMediaItem(item);
        }
        // notify the plugins that the list has been generated
        for (JPSXPlugin plugin : JPSXPlugin.getPlugins()) {
            plugin.mediaListGenerated(this);
        }
        // debug print the list contents
        if (log.isLoggable(Level.INFO)) {
            for (DiscItem oMedItm : this) log.info(oMedItm.toString());
        }
        
    }

    public void setDiscName(String sName) {
        _sDiscName = sName;
    }

    /** Based on how many disc items report single or double speed playback,
     *  returns the best guess of disc speed for unknown disc items. */
    public int getDefaultDiscSpeed() {
        if (_iSingleSpeedItemCount > _iDoubleSpeedItemCount)
            return 1;
        else
            return 2; // if they are equal, assume 2x
    }
    
    /** Adds a media item to the internal hash and array. */
    private void addMediaItemInc(DiscItem item) {
        int iIndex = _mediaList.size();
        item.setIndex(iIndex);
        item.setSourceCD(_sourceCD);
        _mediaHash.put(new Integer(iIndex), item);
        _mediaList.add(item);
    }
    
    /** Adds a media item to the internal hash and array. */
    private void addMediaItem(DiscItem item) {
        item.setSourceCD(_sourceCD);
        _mediaHash.put(new Integer(item.getIndex()), item);
        _mediaList.add(item);
    }

    /** Serializes the list of media items to a file. */
    public void serializeIndex(PrintStream ps)
            throws IOException 
    {
        ps.println(INDEX_HEADER);
        ps.println("; Lines that begin with ; are ignored");
        ps.println(_sourceCD.serialize());
        for (DiscItem oMedia : this) {
            ps.println(oMedia.serialize().serialize());
        }
        ps.close();
    }

    public DiscItem getByIndex(int iIndex) {
        return _mediaHash.get(new Integer(iIndex));
    }
    
    public DiscItem getByString(String sId) {
        return _mediaHash.get(sId);
    }

    public boolean hasIndex(int iIndex) {
        return _mediaHash.containsKey(new Integer(iIndex));
    }

    public CDSectorReader getSourceCD() {
        return _sourceCD;
    }
    
    public int size() {
        return _mediaList.size();
    }
    
    /* [implements Iterable] */
    public Iterator<DiscItem> iterator() {
        return _mediaList.iterator();
    }

    // AbstractListModel stuff /////////////////////////////////////////////////
    public int getSize() {
        return _mediaList.size();
    }

    public DiscItem getElementAt(int iIndex) {
        return _mediaList.get(iIndex);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %d items", _sourceCD.getSourceFile(), _sDiscName, _mediaList.size());
    }


}
