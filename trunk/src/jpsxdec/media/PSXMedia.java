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
import javax.sound.sampled.AudioInputStream;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXAIterator;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.demuxers.StrFramePushDemuxerIS;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.uncompressors.StrFrameUncompressorIS;



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
        public long AudioPeriod      = -1;
    }
    
    /** Gets the AudioFileFormat.Type from its string representation */
    public static Type AudioFileFormatStringToType(String sFormat) {
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
    public static String AudioFileFormatTypeToString(Type oFormat) {
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

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    public static abstract class PSXMediaStreaming extends PSXMedia {
        CDXAIterator m_oCDIter;
        
        public PSXMediaStreaming(PSXSectorRangeIterator oSectIterator) {
            super(oSectIterator);
            Reset();
        }

        public PSXMediaStreaming(CDSectorReader oCD, String sSerial, String sType) 
                throws NotThisTypeException
        {
            super(oCD, sSerial, sType);
            Reset();
        }
        
        public void Reset() {
            m_oCDIter = new CDXAIterator(m_oCD, super.m_iStartSector, super.m_iEndSector);
        }

        private boolean m_blnPlaying = false;
        public void Play() throws IOException {
            m_blnPlaying = true;
            startPlay();

            while (m_oCDIter.hasNext() && m_blnPlaying) {
                CDXASector oSect = m_oCDIter.next();
                if (m_oRawRead != null) if (m_oRawRead.event(oSect)) return;

                PSXSector oPSXSect = PSXSector.SectorIdentifyFactory(oSect);

                if (oPSXSect == null) continue;

                if (playSector(oPSXSect)) return;
            }

            endPlay();
            m_blnPlaying = false;
        }
        
        public void Stop() {
            m_blnPlaying = false;
        }
        
        protected abstract void startPlay();
        protected abstract void endPlay() throws IOException;
        protected abstract boolean playSector(PSXSector oPSXSect) throws IOException;

        public void clearListeners() {
            m_oRawRead = null;
        }
        
        
        private IRawListener m_oRawRead;
        public void addRawListener(IRawListener oListen) {
            m_oRawRead = oListen;
        }

        ////////////////////////////////////////////////////////////////////////
        
        public static interface IRawListener {
            /** @return true to stop, false to continue. */
            boolean event(CDXASector oSect);
        }
        public static interface IVidListener {
            /** @return true to stop, false to continue. */
            boolean event(InputStream is, long frame);
        }
        public static interface IAudListener {
            /** @return true to stop, false to continue. */
            boolean event(AudioInputStream is, int sector);
        }
        
        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        
        public static abstract class PSXMediaVideo extends PSXMediaStreaming {
            public PSXMediaVideo(PSXSectorRangeIterator oSectIterator) {
                super(oSectIterator);
            }

            public PSXMediaVideo(CDSectorReader oCD, String sSerial, String sType) 
                    throws NotThisTypeException
            {
                super(oCD, sSerial, sType);
            }
            
            abstract public void seek(int iFrame) throws IOException;
            
            protected IVidListener m_oVidDemux;
            public void addVidDemuxListener(IVidListener oListen) {
                m_oVidDemux = oListen;
            }
        
            protected IVidListener m_oMdec;
            public void addMdecListener(IVidListener o) {
                m_oMdec = o;
            }
            protected IFrameListener m_oFrame;
            public void addFrameListener(IFrameListener o) {
                m_oFrame = o;
            }
            protected IAudListener m_oAudio;
            public void addAudioListener(IAudListener o) {
                m_oAudio = o;
            }
            
            @Override
            public void clearListeners() {
                m_oVidDemux = null;
                m_oAudio = null;
                m_oFrame = null;
                m_oMdec = null;
                super.clearListeners();
            }

            public static interface IFrameListener {
                /** @return true to stop, false to continue. */
                boolean event(PsxYuv o, long frame);
            }
            
            public abstract int getAudioChannels();

            public abstract FramesPerSecond[] getPossibleFPS();
                
            public abstract boolean hasVideo();
            
            public abstract long getStartFrame();
            public abstract long getEndFrame();

            /** After the last sector of a frame has been read, 
             *  this is called to then process it. */
            final protected boolean handleEndOfFrame(StrFramePushDemuxerIS oDemux) throws IOException {

                // send out the demux event if there is listener
                if (m_oVidDemux != null) {
                    if (m_oVidDemux.event(oDemux, oDemux.getFrameNumber())) return true;
                    return false;
                }

                // continue only if listener
                if (m_oMdec != null || m_oFrame != null) {
                    StrFrameUncompressorIS oUncomprs = new StrFrameUncompressorIS(oDemux, oDemux.getWidth(), oDemux.getHeight());
                    // send mdec event if listener
                    if (m_oMdec != null) {
                        if (m_oMdec.event(oUncomprs, oDemux.getFrameNumber())) return true;
                        return false;
                    }

                    // finally decode frame and event if listener
                    if (m_oFrame != null) { 
                        try {
                            //TODO need to pass the error but still decode
                            PsxYuv yuv = MDEC.DecodeFrame(oUncomprs, oUncomprs.getWidth(), oUncomprs.getHeight());
                            if (m_oFrame.event(yuv, oDemux.getFrameNumber())) return true;
                        } catch (MDEC.DecodingException ex) {
                            if (m_oFrame.event(ex.getYuv(), oDemux.getFrameNumber())) return true;
                        }
                        return false;
                    }

                }

                return false;
            }
        }

    }
    
    
    
}



