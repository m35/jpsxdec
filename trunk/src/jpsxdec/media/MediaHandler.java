/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007  Michael Sabin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor,   
 * Boston, MA  02110-1301, USA.
 *
 */

/*
 * MediaHandler.java
 */

package jpsxdec.media;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.AbstractListModel;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.media.IProgressListener.*;
import jpsxdec.util.NotThisTypeException;

/** Manages a collection of PSXMedia items. */
public class MediaHandler extends AbstractListModel implements Iterable<PSXMedia> {
    
    public static int DebugVerbose = 2;
    
    private Hashtable<Object, PSXMedia> m_oMediaHash = new Hashtable<Object, PSXMedia>();
    private ArrayList<PSXMedia> m_oMediaList = new ArrayList<PSXMedia>();
    private CDSectorReader m_oSourceCD;
    
    public MediaHandler(CDSectorReader oCD)
            throws IOException 
    {
        this(oCD, (IProgressListener)null);
    }
    
    /** Finds all the media on the CD.
     * @param oListener  Optional progress listener.  */
    public MediaHandler(CDSectorReader oCD, final IProgressListener oListener)
            throws IOException 
    {
        m_oSourceCD = oCD;
        final double dblCDSize = oCD.size();
        // need to make it a final array so the anonymous class can use it
        final boolean[] oblnQuitIndexing = new boolean[] { false };

        PSXSectorRangeIterator oSectIterator = new PSXSectorRangeIterator(oCD);
        // If it doesn't even have 1 sector
        if (!oSectIterator.hasNext() || oblnQuitIndexing[0]) return;

        // add a progress listener
        oSectIterator.setSectorChangeListener(new PSXSectorRangeIterator.ISectorChangeListener() {
            public void CurrentSector(int i) {
                if (oListener != null) {
                    if (!oListener.ProgressUpdate("Sector " + i, i / dblCDSize))
                        oblnQuitIndexing[0] = true;
                }
            }
        });
        
        while (oSectIterator.hasNext() && !oblnQuitIndexing[0]) {
            
            if (DebugVerbose > 6) 
                System.err.println("Sector: " + oSectIterator.getIndex());
            
            try {
                AddMediaItem(new PSXMediaSTR(oSectIterator), oListener);
                continue;
            } catch (NotThisTypeException e) {}

            {
                ArrayList<PSXMediaXA> oXAList = PSXMediaXA.FindXAs(oSectIterator);
                if (oXAList != null) {
                    for (PSXMediaXA oXA : oXAList) {
                        AddMediaItem(oXA, oListener);
                    }
                    continue;
                }
            }
            
            try {
                AddMediaItem(new PSXMediaFF8(oSectIterator), oListener);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                AddMediaItem(new PSXMediaFF9(oSectIterator), oListener);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                AddMediaItem(new PSXMediaChronoX(oSectIterator), oListener);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                AddMediaItem(new PSXMediaTIM(oSectIterator), oListener);
                continue;
            } catch (NotThisTypeException e) {
                // Because TIM searching actually does increment the iterator
                // we need to make sure we aren't at the end of the iterator.
                if (!oSectIterator.hasNext()) break;
            }
            
            oSectIterator.skipNext();
        }
        
    }
    
    /** Adds a media item to the internal hash and array, and returns the
     *  incremented index. */
    private void AddMediaItem(PSXMedia oMedia, IProgressListener oCB) {
        int iIndex = m_oMediaList.size();
        oMedia.setIndex(iIndex);
        m_oMediaHash.put(new Integer(iIndex), oMedia);
        m_oMediaHash.put(oMedia.toString(), oMedia);
        m_oMediaList.add(oMedia);
        if (DebugVerbose > 3) System.err.println(oMedia.toString());
        if (oCB instanceof IProgressEventListener)
            ((IProgressEventListener)oCB).ProgressUpdate(oMedia.toString());
    }
    
    private void AddMediaItem(PSXMedia oMedia, int iIndex, IProgressListener oCB) {
        m_oMediaHash.put(new Integer(iIndex), oMedia);
        m_oMediaHash.put(oMedia.toString(), oMedia);
        m_oMediaList.add(oMedia);
        if (DebugVerbose > 3) System.err.println(oMedia.toString());
        if (oCB instanceof IProgressEventListener)
            ((IProgressEventListener)oCB).ProgressUpdate(oMedia.toString());
    }
    
    /** Deserializes the CD index file, and creates a
     *  list of media items on the CD */
    public MediaHandler(CDSectorReader oCD, String sSerialFile)
            throws IOException 
    {
        //TODO: Want to read as much as we can from the file before an exception
        //      and return what we can get, but we also want to return that
        //      there was an error.
        m_oSourceCD = oCD;
        PSXMedia oPsxMedia = null;
        BufferedReader oReader =
                new BufferedReader(new FileReader(sSerialFile));
        String sLine;
        while ((sLine = oReader.readLine()) != null) {
            
            // comments
            if (sLine.startsWith("#"))
                continue;
            
            // source file
            if (sLine.startsWith("SourceFile|")) {
                String[] asParts = sLine.split("|");
                if (asParts.length != 5) continue;
                //TODO: Check if m_oSourceCD length matches serialized length
                continue;
            }
            
            String[] asParts = sLine.split(":");
            if (asParts.length < 2) continue;
            
            try {
                if (asParts[1].equals("STR")) {
                    oPsxMedia = new PSXMediaSTR(oCD, sLine);
                } else if (asParts[1].equals("XA")) {
                    oPsxMedia = new PSXMediaXA(oCD, sLine);
                } else if (asParts[1].equals("TIM")) {
                    oPsxMedia = new PSXMediaTIM(oCD, sLine);
                } else if (asParts[1].equals("FF8")) {
                    oPsxMedia = new PSXMediaFF8(oCD, sLine);
                } else if (asParts[1].equals("FF9")) {
                    oPsxMedia = new PSXMediaFF9(oCD, sLine);
                } else if (asParts[1].equals("ChronoX")) {
                    oPsxMedia = new PSXMediaChronoX(oCD, sLine);
                } else {
                    continue;
                }
                AddMediaItem(oPsxMedia, oPsxMedia.getIndex(), null);
            } catch (NotThisTypeException e) {
                if (DebugVerbose > 3)
                    System.err.println("Failed to parse possible item: " + sLine);
            }
        }
        oReader.close();
        
    }
    
    /** Serializes the list of media items to a file */
    public void SerializeMediaList(PrintStream oPrinter)
            throws IOException 
    {
        oPrinter.println("# Lines that begin with # are comments");
        oPrinter.println(m_oSourceCD.toString());
        for (PSXMedia oMedia : this) {
            oPrinter.println(oMedia.toString());
        }
        oPrinter.close();
    }

    public PSXMedia getByIndex(int index) {
        return m_oMediaHash.get(new Integer(index));
    }
    
    public PSXMedia getBySting(String index) {
        return m_oMediaHash.get(index);
    }

    public boolean hasIndex(int i) {
        return m_oMediaHash.containsKey(new Integer(i));
    }

    public int size() {
        return m_oMediaList.size();
    }
    
    /* [implements Iterable] */
    public Iterator<PSXMedia> iterator() {
        return m_oMediaList.iterator();
    }

    // AbstractListModel stuff /////////////////////////////////////////////////
    public int getSize() {
        return m_oMediaList.size();
    }

    public Object getElementAt(int index) {
        return m_oMediaList.get(index);
    }

}
