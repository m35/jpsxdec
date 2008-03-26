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
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;


/** Abstract PSX media type. Constructors of sub-classes will attempt to
 *  deserialize a Playatation media item from CD sectors. */
public abstract class PSXMedia implements Comparable {

    public static int DebugVerbose = 2;
    
    public static final int MEDIA_TYPE_VIDEO = 1;
    public static final int MEDIA_TYPE_AUDIO = 2;
    public static final int MEDIA_TYPE_VIDEO_AUDIO = 3;
    public static final int MEDIA_TYPE_XA = 4;
    public static final int MEDIA_TYPE_IMAGE = 8;
    
    /**************************************************************************/
    /**************************************************************************/
    
    protected int m_iStartSector;
    protected int m_iEndSector;
    protected CDSectorReader m_oCD;
    /** Index number of the media item in the file. */
    private int m_iMediaIndex;
    
    public PSXMedia(CDSectorReader oCD) {
        m_oCD = oCD;
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
            m_iMediaIndex = Integer.parseInt(asParts[0]);
            m_iStartSector = Integer.parseInt(asSectors[0]);
            m_iEndSector = Integer.parseInt(asSectors[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    /** For sorting purposes. */
    public int compareTo(Object o) {
        if (o instanceof PSXMedia) {
            PSXMedia om = (PSXMedia)o;
            if (m_iMediaIndex < om.m_iMediaIndex)
                return -1;
            else if (m_iMediaIndex > om.m_iMediaIndex)
                return 1;
            else
                return 0;
        } else {
            throw new IllegalArgumentException("PSXMedia.compareTo non-PSXMedia!");
        }
    }
    
    // ## Properties ###########################################################
    
    public long getStartSector() {
        return m_iStartSector;
    }
    public long getEndSector() {
        return m_iEndSector;
    }
    
    public int getIndex() {
        return m_iMediaIndex;
    }

    public void setIndex(int iMediaIndex) {
        m_iMediaIndex = iMediaIndex;
    }
    
    /** Extend this function for serializing */
    protected String toString(String sType) {
        return String.format("%03d:%s:%d-%d:",
                m_iMediaIndex, sType, m_iStartSector, m_iEndSector);
    }
    
    public PSXSectorRangeIterator getSectorIterator() {
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
    }
    
    public String getSuggestedName() {
        String s = new File(m_oCD.getSourceFile()).getName();
        int i = s.indexOf('.');
        if (i >= 0)
            s = s.substring(0, i);
        return String.format("%s%03d", s, m_iMediaIndex);
    }
    
    // -------------------------------------------------------------------------
    // Stuff shared by more than one sub-class ---------------------------------
    // -------------------------------------------------------------------------
    
    /** Handy class to help with indexing audio. */
    protected static class AudioChannelInfo {
        public long Channel          = -1;
        public long BitsPerSample    = -1;
        public long SamplesPerSecond = -1;
        public long MonoStereo       = -1;
        public long LastAudioSect    = -1;
        public long AudioPeriod      = 0;

        public AudioChannelInfo() {
        }

        public AudioChannelInfo(String sSerial) throws NotThisTypeException {
            try {
                IndexLineParser oParse = new IndexLineParser(
                        "Channel # $ Bits/Sample # Samples/sec # Perod #"
                        , sSerial);

                Channel = oParse.get(Channel);
                String s = oParse.get((String)null);
                if (s.equals("Mono")) 
                    MonoStereo = 1;
                else if (s.equals("Stereo"))
                    MonoStereo = 2;
                else
                    throw new NotThisTypeException();
                BitsPerSample = oParse.get(BitsPerSample);
                SamplesPerSecond = oParse.get(SamplesPerSecond);
                
                AudioPeriod = oParse.get(AudioPeriod);
                
            } catch (NoSuchElementException ex) {
                throw new NotThisTypeException();
            } catch (NumberFormatException ex) {
                throw new NotThisTypeException();
            } catch (IllegalArgumentException ex) {
                throw new NotThisTypeException();
            }
            
        }
        
        
        
        public String toString() {
            return String.format(
                    "Channel %d %s Bits/Sample %d Samples/sec %d Perod %d", 
                    Channel,
                    (MonoStereo == 1) ? "Mono" : "Stereo",
                    BitsPerSample,
                    SamplesPerSecond,
                    AudioPeriod
                    
                    );
        }
        
    }
    
}



