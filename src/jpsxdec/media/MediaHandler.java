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

import java.io.*;
import java.util.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.GenericContainer;
import jpsxdec.util.IProgressCallback;
import jpsxdec.util.NotThisTypeException;

/** Manages a collection of PSXMedia items. */
public class MediaHandler implements Iterable<PSXMedia> {
    
    static int DebugVerbose = 2;
    
    private Hashtable<Object, PSXMedia> m_oMediaHash = new Hashtable<Object, PSXMedia>();
    private LinkedList<PSXMedia> m_oMediaList = new LinkedList<PSXMedia>();
    private CDSectorReader m_oSourceCD;
    
    public MediaHandler(CDSectorReader oCD)
            throws IOException 
    {
        this(oCD, (IProgressCallback)null);
    }
    
    /** Finds all the media on the CD */
    public MediaHandler(CDSectorReader oCD, final IProgressCallback oCallbaker)
            throws IOException 
    {
        m_oSourceCD = oCD;
        final double dblCDSize = oCD.size();
        final GenericContainer<Boolean> oblnQuit = 
                new GenericContainer<Boolean>(new Boolean(false));
        
        PSXSectorRangeIterator oSectIterator = new PSXSectorRangeIterator(oCD);
        // If it doesn't even have 1 sector
        if (!oSectIterator.hasNext() && !oblnQuit.get()) return;

        // set the callback
        oSectIterator.setCallback(new PSXSectorRangeIterator.ICurrentSector() {
            public void CurrentSector(int i) {
                if (oCallbaker != null) {
                    if (!oCallbaker.ProgressCallback("Sector " + i, i / dblCDSize))
                        oblnQuit.set(new Boolean(true));
                }
            }
        });
        
        int iIndex = 0;
        while (oSectIterator.hasNext() && !oblnQuit.get()) {
            
            if (DebugVerbose > 6) 
                System.err.println("Sector: " + oSectIterator.getIndex());
            
            try {
                iIndex = AddMediaItem(new PSXMediaSTR(oSectIterator), iIndex, oCallbaker);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                iIndex = AddMediaItem(new PSXMediaXA(oSectIterator), iIndex, oCallbaker);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                iIndex = AddMediaItem(new PSXMediaTIM(oSectIterator), iIndex, oCallbaker);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                iIndex = AddMediaItem(new PSXMediaFF8(oSectIterator), iIndex, oCallbaker);
                continue;
            } catch (NotThisTypeException e) {}
            
            try {
                iIndex = AddMediaItem(new PSXMediaFF9(oSectIterator), iIndex, oCallbaker);
                continue;
            } catch (NotThisTypeException e) {}
            
            oSectIterator.skipNext();
        }
        
    }
    
    private int AddMediaItem(PSXMedia oMedia, int iIndex, IProgressCallback oCB) {
        oMedia.setIndex(iIndex);
        m_oMediaHash.put(new Integer(iIndex), oMedia);
        m_oMediaHash.put(oMedia.toString(), oMedia);
        m_oMediaList.add(oMedia);
        if (DebugVerbose > 1) System.err.println(oMedia.toString());
        if (oCB instanceof IProgressCallback.IProgressCallbackEvent)
            ((IProgressCallback.IProgressCallbackEvent)oCB).ProgressCallback(oMedia.toString());
        return iIndex + 1;
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
            
            try {
                if (sLine.substring(3, 8).equals(":STR:")) {
                    oPsxMedia = new PSXMediaSTR(oCD, sLine);
                } else if (sLine.substring(3, 7).equals(":XA:")) {
                    oPsxMedia = new PSXMediaXA(oCD, sLine);
                } else if (sLine.substring(3, 8).equals(":TIM:")) {
                    oPsxMedia = new PSXMediaTIM(oCD, sLine);
                } else if (sLine.substring(3, 8).equals(":FF8:")) {
                    oPsxMedia = new PSXMediaFF8(oCD, sLine);
                } else if (sLine.substring(3, 8).equals(":FF9:")) {
                    oPsxMedia = new PSXMediaFF9(oCD, sLine);
                } else {
                    continue;
                }
                AddMediaItem(oPsxMedia, oPsxMedia.getIndex(), null);
            } catch (NotThisTypeException e) {}
        }
        oReader.close();
        
    }
    
    /** Serializes the list of media items to a file */
    public void SerializeMediaList(PrintStream oPrinter)
            throws IOException 
    {
        oPrinter.println("# Lines that begin with # are comments");
        oPrinter.println("# Format:");
        oPrinter.println("#   media_num:STR:start_sector-end_sector:frame_start-frame_end:audio_sample_count");
        oPrinter.println("#   media_num:XA:start_sector-end_sector:list,of,channels");
        oPrinter.println("#   media_num:TIM:start_sector-end_sector:start_sector_offset");
        oPrinter.println("#   media_num:FF8:start_sector-end_sector:frame_start-frame_end:has_video?");
        oPrinter.println("#   media_num:FF9:start_sector-end_sector:frame_start-frame_end");
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
        return m_oMediaHash.size();
    }

    /** [implements Iterable] */
    public Iterator<PSXMedia> iterator() {
        return m_oMediaList.iterator();
    }

}
