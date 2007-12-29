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
 * PSXMedia.java
 */

package jpsxdec.media;

import java.util.*;
import java.io.*;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader.CDXASector;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;



/** Abstract PSX media type. Constructors of sub-classes will attempt to
 *  deserialize a Playatation media item from CD sectors. */
public abstract class PSXMedia {
    
    public static int DebugVerbose = 2;
    
    public static final int MEDIA_TYPE_VIDEO = 1;
    public static final int MEDIA_TYPE_VIDEO_AUDIO = 2;
    public static final int MEDIA_TYPE_XA = 4;
    public static final int MEDIA_TYPE_IMAGE = 8;
    
    
    /** Finds all the media on the CD */
    public static ArrayList<PSXMedia> IndexCD(CDSectorReader oCD)
            throws IOException 
    {
        
        PSXSectorRangeIterator oSectIterator = new PSXSectorRangeIterator(oCD);
        
        ArrayList<PSXMedia> oMediaList = new ArrayList<PSXMedia>();
        
        // If it doesn't even have 1 sector
        if (!oSectIterator.hasNext()) return oMediaList;
        
        PSXMedia oPsxMedia = null;
        while (oSectIterator.hasNext()) {
            
            try {
                oPsxMedia = new PSXMediaSTR(oSectIterator);
                if (DebugVerbose > 3)
                    System.err.println(oPsxMedia.toString());
                oMediaList.add(oPsxMedia);
                if (!oSectIterator.hasNext()) break;
            } catch (NotThisTypeException e1) {
                try {
                    oPsxMedia = new PSXMediaXA(oSectIterator);
                    if (DebugVerbose > 3)
                        System.err.println(oPsxMedia.toString());
                    oMediaList.add(oPsxMedia);
                    if (!oSectIterator.hasNext()) break;
                } catch (NotThisTypeException e2) {
                    try {
                        oPsxMedia = new PSXMediaTIM(oSectIterator);
                        if (DebugVerbose > 3)
                            System.err.println(oPsxMedia.toString());
                        oMediaList.add(oPsxMedia);
                        if (!oSectIterator.hasNext()) break;
                    } catch (NotThisTypeException e3) {
                        try {
                            oPsxMedia = new PSXMediaFF8(oSectIterator);
                            if (DebugVerbose > 3)
                                System.err.println(oPsxMedia.toString());
                            oMediaList.add(oPsxMedia);
                            if (!oSectIterator.hasNext()) break;
                        } catch (NotThisTypeException e4) {
                            if (!oSectIterator.hasNext()) break;
                            oSectIterator.skipNext();
                        }
                    }
                }
            }
            
        }
        
        return oMediaList;
    }
    
    /** Deserializes the CD index file, and creates a
     *  list of media items on the CD */
    public static ArrayList<PSXMedia> IndexCD(CDSectorReader oCD,
                                              String sSerialFile)
            throws IOException 
    {
        //TODO: Want to read as much as we can from the file before an exception
        //      and return what we can get, but we also want to return that
        //      there was an error.
        
        ArrayList<PSXMedia> oMediaList = new ArrayList<PSXMedia>();
        
        PSXMedia oPsxMedia = null;
        BufferedReader oReader =
                new BufferedReader(new FileReader(sSerialFile));
        String sLine;
        while ((sLine = oReader.readLine()) != null) {
            
            try {
                if (sLine.substring(3, 8).equals(":STR:")) {
                    oPsxMedia = new PSXMediaSTR(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                } else if (sLine.substring(3, 7).equals(":XA:")) {
                    oPsxMedia = new PSXMediaXA(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                } else if (sLine.substring(3, 8).equals(":TIM:")) {
                    oPsxMedia = new PSXMediaTIM(oCD, sLine);
                    oMediaList.add(oPsxMedia);
                }
            } catch (NotThisTypeException e1) {}
        }
        oReader.close();
        
        return oMediaList;
    }
    
    /** Serializes a list of media items to a file */
    public static void SerializeMediaList(AbstractList<PSXMedia> oMediaList,
                                          PrintStream oPrinter)
            throws IOException 
    {
        oPrinter.println("# Lines that begin with # are comments");
        oPrinter.println("# Format:");
        oPrinter.println("#   media_num:STR:start_sector-end_sector:frame_start-frame_end:audio_sample_count");
        oPrinter.println("#   media_num:XA:start_sector-end_sector:list,of,channels");
        oPrinter.println("#   media_num:TIM:start_sector-end_sector:start_sector_offset");
        oPrinter.println("#   media_num:FF8:start_sector-end_sector:frame_start-frame_end");
        int i = 0;
        for (PSXMedia oMedia : oMediaList) {
            oPrinter.println(String.format("%03d:%s", i, oMedia.toString()));
            i++;
        }
        oPrinter.close();
    }
    
    /**************************************************************************/
    /**************************************************************************/
    
    protected int m_iStartSector;
    protected int m_iEndSector;
    protected CDSectorReader m_oCD;
    
    public PSXMedia(PSXSectorRangeIterator oSectIterator) {
        m_oCD = oSectIterator.getSourceCD();
    }
    
    /** Deserializes the start and end sectors */
    public PSXMedia(CDSectorReader oCD, String sSerial, String sType)
            throws NotThisTypeException 
    {
        m_oCD = oCD;
        String asParts[] = sSerial.split(":");
        if (asParts.length < 2)
            throw new NotThisTypeException();
        if (!asParts[1].equals(sType))
            throw new NotThisTypeException();
        String asSectors[] = asParts[2].split("-");
        try {
            m_iStartSector = Integer.parseInt(asSectors[0]);
            m_iEndSector = Integer.parseInt(asSectors[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    public long getStartSector() {
        return m_iStartSector;
    }
    public long getEndSector() {
        return m_iEndSector;
    }
    
    /** Extend this function for serializing */
    public String toString() {
        return m_iStartSector + "-" + m_iEndSector;
    }
    
    abstract public int getMediaType();
    
    /** Decodes the media to the disk uses the supplied options */
    abstract public void Decode(Options oOpt) throws IOException;
    
    // -------------------------------------------------------------------------
    // Stuff shared by more than one sub-class ---------------------------------
    // -------------------------------------------------------------------------
    
    /** Handy class to help with indexing audio. */
    protected static class AudioChannelInfo {
        public long Channel          = -1;
        public long BitsPerSampele   = -1;
        public long SamplesPerSecond = -1;
        public long MonoStereo       = -1;
        public long LastAudioSect    = -1;
    }
    
    /** Gets the AudioFileFormat.Type from its string representation */
    protected static Type AudioFileFormatStringToType(String sFormat) {
        if (sFormat.equals("aifc"))
            return AudioFileFormat.Type.AIFC;
        else if (sFormat.equals("aiff"))
            return AudioFileFormat.Type.AIFF;
        else if (sFormat.equals("au"))
            return AudioFileFormat.Type.AU;
        else if (sFormat.equals("snd"))
            return AudioFileFormat.Type.SND;
        else if (sFormat.equals("wav"))
            return AudioFileFormat.Type.WAVE;
        else
            return null;
    }
    
    /** Gets the string representation of an AudioFileFormat.Type */
    protected static String AudioFileFormatTypeToString(Type oFormat) {
        if (oFormat.equals(AudioFileFormat.Type.AIFC))
            return "aifc";
        else if (oFormat.equals(AudioFileFormat.Type.AIFF))
            return "aiff";
        else if (oFormat.equals(AudioFileFormat.Type.AU))
            return "au";
        else if (oFormat.equals(AudioFileFormat.Type.SND))
            return "snd";
        else if (oFormat.equals(AudioFileFormat.Type.WAVE))
            return "wav";
        else
            return null;
    }
}
