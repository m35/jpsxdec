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
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.util.IProgressCallback;



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
            m_iMediaIndex = Integer.parseInt(asParts[0]);
            m_iStartSector = Integer.parseInt(asSectors[0]);
            m_iEndSector = Integer.parseInt(asSectors[1]);
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
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

    public void setIndex(int m_iMediaIndex) {
        this.m_iMediaIndex = m_iMediaIndex;
    }
    
    /** Extend this function for serializing */
    protected String toString(String sType) {
        return String.format("%03d:%s:%d-%d",
                m_iMediaIndex, sType, m_iStartSector, m_iEndSector);
    }
    
    // -------------------------------------------------------------------------
    
    private IProgressCallback m_oCallbackObj;
    
    public void setCallback(IProgressCallback oCallBk) {
        m_oCallbackObj = oCallBk;
    }
    
    protected boolean Progress(String sWhatDoing, double dblProgress) {
        if (m_oCallbackObj instanceof IProgressCallback) {
            return ((IProgressCallback)m_oCallbackObj).ProgressCallback(sWhatDoing, dblProgress);
        }
        return true;
    }
    
    protected boolean Event(String sWhatHappen) {
        if (m_oCallbackObj instanceof IProgressCallback.IProgressCallbackEvent) {
            return ((IProgressCallback.IProgressCallbackEvent)m_oCallbackObj).ProgressCallback(sWhatHappen);
        }
        return true;
    }
    
    protected void Error(Exception e) {
        if (m_oCallbackObj instanceof IProgressCallback.IProgressCallbackError) {
            ((IProgressCallback.IProgressCallbackError)m_oCallbackObj).ProgressCallback(e);
        }
    }
    
    
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    abstract public int getMediaType();

    public void DecodeVideo(String sFileBaseName, String sImgFormat, Integer oiStartFrame, Integer oiEndFrame) {
        throw new UnsupportedOperationException("This media type does not have video.");
    }
    public boolean hasVideo() {
        return false;
    }
    
    public void DecodeAudio(String sFileBaseName, String sAudFormat, Double odblScale) {
        throw new UnsupportedOperationException("This media type does not have movie audio.");
    }
    public boolean hasAudio() {
        return false;
    }
    
    public void DecodeXA(String sFileBaseName, String sAudFormat, Double odblScale, Integer oiChannel) {
        throw new UnsupportedOperationException("This media type does not have XA audio.");
    }
    public boolean hasXAChannels() {
        return false;
    }
    
    public void DecodeImage(String sFileBaseName, String sImgFormat) {
        throw new UnsupportedOperationException("This media type is not an image.");
    }
    public boolean hasImage() {
        return false;
    }
    
    
    
    
    
    
    // -------------------------------------------------------------------------
    // Stuff shared by more than one sub-class ---------------------------------
    // -------------------------------------------------------------------------
    
    protected static long Clamp(long iVal, long iMin, long iMax) {
        if (iVal < iMin)
            return iMin;
        else if (iVal > iMax)
            return iMax;
        return iVal;
    }
    
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
