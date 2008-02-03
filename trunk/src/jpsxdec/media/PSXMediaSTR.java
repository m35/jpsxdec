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
 * PSXMediaSTR.java
 */

package jpsxdec.media;

import java.io.*;
import javax.sound.sampled.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.audiodecoding.StrAudioDemuxerDecoderIS;
import jpsxdec.demuxers.StrFrameDemuxerIS;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;


/** Handles standard STR movies, and STR movies that deviate slightly from
 *  standard STR movies: Lain and FF7 */
public class PSXMediaSTR extends PSXMedia 
        implements VideoFrameConverter.IVideoMedia
{
    
    /** 0 if it doesn't have audio, nonzero if it does */
    long m_lngAudioSampleLength = 0;
    
    long m_iCurFrame = -1;
    long m_iWidth = -1;
    long m_iHeight = -1;
    
    long m_lngStartFrame = -1;
    long m_lngEndFrame = -1;
    
    public PSXMediaSTR(PSXSectorRangeIterator oSectIterator) throws NotThisTypeException, IOException
    {
        super(oSectIterator);
        
        long iAudioPeriod = -1;
        AudioChannelInfo oAudInf = null;
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFrameChunk))
            throw new NotThisTypeException();
        
        PSXSectorFrameChunk oFrame;
        PSXSectorAudioChunk oAudio;
        
        oFrame = (PSXSectorFrameChunk) oPsxSect;

        m_iWidth = oFrame.getWidth();
        m_iHeight = oFrame.getHeight();
        m_iCurFrame = oFrame.getFrameNumber();

        m_iStartSector = oPsxSect.getSector();
        m_iEndSector = m_iStartSector;

        m_lngStartFrame = oFrame.getFrameNumber();
        m_lngEndFrame = oFrame.getFrameNumber();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorNull) {
                // just skip it
            } else if (oPsxSect instanceof PSXSectorAudio2048) {
                // just skip it
            } else if (oPsxSect instanceof PSXSectorFrameChunk) {
                        
                oFrame = (PSXSectorFrameChunk) oPsxSect;
                if (oFrame.getWidth() == m_iWidth &&
                        oFrame.getHeight() == m_iHeight &&
                        (oFrame.getFrameNumber() == m_iCurFrame ||
                        oFrame.getFrameNumber() == m_iCurFrame+1)) {

                    m_iCurFrame = oFrame.getFrameNumber();
                    m_lngEndFrame = m_iCurFrame;
                } else {
                    break;
                }

                m_iEndSector = oPsxSect.getSector();
            }  else if (oPsxSect instanceof PSXSectorAudioChunk) {
                    oAudio = (PSXSectorAudioChunk) oPsxSect;

                    if (oAudInf != null) {

                        if (oAudio.getMonoStereo() != oAudInf.MonoStereo ||
                            oAudio.getSamplesPerSecond() != oAudInf.SamplesPerSecond ||
                            oAudio.getBitsPerSample() != oAudInf.BitsPerSampele ||
                            oAudio.getChannel() != oAudInf.Channel) 
                        {
                            break;
                        }

                        if ((oPsxSect.getSector() - oAudInf.LastAudioSect) != iAudioPeriod) {
                            //break;
                        }

                        oAudInf.LastAudioSect = oPsxSect.getSector();

                        m_lngAudioSampleLength += oAudio.getSampleLength();

                    } else {
                        iAudioPeriod = oPsxSect.getSector() - m_iStartSector + 1;

                        oAudInf = new AudioChannelInfo();
                        oAudInf.LastAudioSect = oPsxSect.getSector();
                        oAudInf.MonoStereo = oAudio.getMonoStereo();
                        oAudInf.SamplesPerSecond = oAudio.getSamplesPerSecond();
                        oAudInf.BitsPerSampele = oAudio.getBitsPerSample();
                        oAudInf.Channel = oAudio.getChannel();
                    }

                    m_iEndSector = oPsxSect.getSector();
                }  else {
                    break; // some other sector type? we're done.
                }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            oSectIterator.skipNext();
        } // while
        
    }
    
    public PSXMediaSTR(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "STR");
        String asParts[] = sSerial.split(":");
        if (asParts.length != 5)
            throw new NotThisTypeException();
        
        String asStartEndFrame[] = asParts[3].split("-");
        try {
            m_lngStartFrame = Integer.parseInt(asStartEndFrame[0]);
            m_lngEndFrame = Integer.parseInt(asStartEndFrame[1]);
            m_lngAudioSampleLength = Integer.parseInt(asParts[4]);
        }  catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    public PSXSectorRangeIterator getSectorIterator() {
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
    }
    
    public long getAudioSampleLength() {
        return m_lngAudioSampleLength;
    }
    
    public String toString() {
        return super.toString("STR") + ":"
                + m_lngStartFrame + "-" + m_lngEndFrame + ":" 
                + m_lngAudioSampleLength;
    }
    
    public long getStartFrame() {
        return m_lngStartFrame;
    }
    
    public long getEndFrame() {
        return m_lngEndFrame;
    }

    
    public int getMediaType() {
        if (m_lngAudioSampleLength == 0)
            return PSXMedia.MEDIA_TYPE_VIDEO;
        else
            return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO;
    }

    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public boolean hasAudio() {
        if (m_lngAudioSampleLength == 0)
            return false;
        else
            return true;
    }

    @Override
    public void DecodeVideo(String sFileBaseName, String sImgFormat, Integer oiStartFrame, Integer oiEndFrame)
    {
        
        long lngStart;
        if (oiStartFrame == null)
            lngStart = m_lngStartFrame;
        else
            lngStart = super.Clamp(oiStartFrame, m_lngStartFrame, m_lngEndFrame);
            
        long lngEnd;
        if (oiEndFrame == null)
            lngEnd = m_lngEndFrame;
        else
            lngEnd = super.Clamp(oiEndFrame, m_lngStartFrame, m_lngEndFrame);
            
        if (lngStart > lngEnd) {
            long lng = lngStart;
            lngStart = lngEnd;
            lngEnd = lng;
        }
        
        PSXSectorRangeIterator oIter = getSectorIterator();
        
        for (long iFrameIndex = lngStart; iFrameIndex <= lngEnd; iFrameIndex++) 
        {
            String sFrameFile = 
                    sFileBaseName + 
                    String.format("_f%04d", iFrameIndex)
                    + "." + sImgFormat;
            try {
                
                StrFrameDemuxerIS str = 
                        new StrFrameDemuxerIS(oIter, iFrameIndex);
                
                if (!super.Progress("Reading frame " + iFrameIndex, 
                        (iFrameIndex - lngStart) / (double)(lngEnd - lngStart)))
                    return;

                VideoFrameConverter.DecodeAndSaveFrame(
                        "demux",
                        sImgFormat,
                        str,
                        sFrameFile,
                        -1,
                        -1);
                
            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace(System.err);
                super.Error(ex);
            }

        } // for
        
    }
    
    @Override
    public void DecodeAudio(String sFileBaseName, String sAudFormat, Double odblScale)
    {
        try {
            if (!super.Progress("Decoding movie audio", 0)) {
                return;
            }

            PSXSectorRangeIterator oIter = getSectorIterator();
            StrAudioDemuxerDecoderIS dec =
                    odblScale == null ? new StrAudioDemuxerDecoderIS(oIter)
                    : new StrAudioDemuxerDecoderIS(oIter, odblScale);

            AudioInputStream oAudStream = new AudioInputStream(dec, dec.getFormat(), dec.getLength());

            String sFileName = sFileBaseName + "." + sAudFormat;

            AudioFileFormat.Type oType = super.AudioFileFormatStringToType(sAudFormat);

            AudioSystem.write(oAudStream, oType, new File(sFileName));

        } catch (IOException ex) {
            super.Error(ex);
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    public String CalculateFrameRateBase() {
        try {
            PSXSectorRangeIterator oIter = getSectorIterator();
            int iStartSector = oIter.getIndex();

            // first get the audio period (should be consistent)
            int iAudioPeriod = -1;
            int iStartFrmNum = -1;
            int iSampPerSec = -1;
            int iMonoStereo = -1;
            
            // should also check if there are 2 or more complete frames
            int iFrameChunks = -1;
            while (oIter.hasNext() && iAudioPeriod < 0) {
                int iCurSect = oIter.getIndex();
                PSXSector oSect = oIter.next();
                if (oSect instanceof PSXSectorAudioChunk) {
                    PSXSectorAudioChunk oAudChk = (PSXSectorAudioChunk) oSect;                   
                    iAudioPeriod = iCurSect - iStartSector + 1;
                    iSampPerSec = oAudChk.getSamplesPerSecond();
                    iMonoStereo = oAudChk.getMonoStereo();
                    // samples/second
                    break;
                } else if (oSect instanceof PSXSectorFrameChunk) {
                    PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oSect;
                    if (iStartFrmNum < 0)
                        iStartFrmNum = (int)oFrmChk.getFrameNumber();
                    
                    if (iFrameChunks == -1)
                        iFrameChunks = (int)oFrmChk.getChunksInFrame();
                    else if (iFrameChunks >= 0)
                        if (iFrameChunks != oFrmChk.getChunksInFrame())
                            iFrameChunks = -2;
                }
            }

            if (iAudioPeriod < 0) {
                if (iStartFrmNum < 0)
                    throw new IOException("no audio or video found in this clip.");
                else {
                    if (iFrameChunks >= 0)
                        return "no audio, consistent period " + iFrameChunks;
                    else
                        return "no audio, inconsistent period";
                }
            }
                

            // now try to find the frame period (via trial & error)
            periodloop:
            for (int iFramePeriod : new int[] {15, 10, 5}) 
            {

                oIter.gotoIndex(iStartSector);

                int iLastAudioSect = iStartSector - 1;
                int iThisFrameChunk = 0;
                int iThisFrame = iStartFrmNum;
                int iThisPeriodSize;
                iThisPeriodSize = iFramePeriod;

                while (oIter.hasNext()) {

                    for (int i = 0; (i < iThisPeriodSize) && oIter.hasNext(); i++) {
                        int iCurSect = oIter.getIndex();
                        PSXSector oSect = oIter.next();

                        if (oSect instanceof PSXSectorAudioChunk) {
                            if (iCurSect - iLastAudioSect != iAudioPeriod)
                                throw new IOException("audio doesn't match period");
                            iLastAudioSect = iCurSect;

                        } else if (oSect instanceof PSXSectorFrameChunk) {
                            PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oSect;

                            if (iThisFrameChunk != oFrmChk.getChunkNumber())
                                continue periodloop;

                            if (iThisFrame != oFrmChk.getFrameNumber())
                                continue periodloop;

                            iThisFrameChunk++;
                        }
                    }
                    iThisFrameChunk = 0;
                    iThisFrame++;
                    iThisPeriodSize = iFramePeriod;
                    
                } //while

                return "Confirmed period " + iFramePeriod;

            }

            return "Failed to identify";
            
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
    
    public String CalculateFrameRateWacked() {
        try {
            PSXSectorRangeIterator oIter = getSectorIterator();
            int iStartSector = oIter.getIndex();

            // first get the audio period (should be consistent)
            int iAudioPeriod = -1;
            int iStartFrmNum = -1;
            int iSampPerSec = -1;
            int iMonoStereo = -1;
            
            // should also check if there are 2 or more complete frames
            int iFrameChunks = -1;
            while (oIter.hasNext() && iAudioPeriod < 0) {
                int iCurSect = oIter.getIndex();
                PSXSector oSect = oIter.next();
                if (oSect instanceof PSXSectorAudioChunk) {
                    PSXSectorAudioChunk oAudChk = (PSXSectorAudioChunk) oSect;                   
                    iAudioPeriod = iCurSect - iStartSector + 1;
                    iSampPerSec = oAudChk.getSamplesPerSecond();
                    iMonoStereo = oAudChk.getMonoStereo();
                    // samples/second
                    break;
                } else if (oSect instanceof PSXSectorFrameChunk) {
                    PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oSect;
                    if (iStartFrmNum < 0)
                        iStartFrmNum = (int)oFrmChk.getFrameNumber();
                    
                    if (iFrameChunks == -1)
                        iFrameChunks = (int)oFrmChk.getChunksInFrame();
                    else if (iFrameChunks >= 0)
                        if (iFrameChunks != oFrmChk.getChunksInFrame())
                            iFrameChunks = -2;
                }
            }

            if (iAudioPeriod < 0) {
                if (iStartFrmNum < 0)
                    throw new IOException("no audio or video found in this clip.");
                else {
                    if (iFrameChunks >= 0)
                        return "no audio, consistent period " + iFrameChunks;
                    else
                        return "no audio, inconsistent period";
                }
            }
                
            oIter.gotoIndex(iStartSector);
            int iLastSectorLastFrame = -1;
            boolean blnCouldBeOneMore = false;
            while (oIter.hasNext()) {
                int iCurSect = oIter.getIndex();
                PSXSector oSect = oIter.next();

                if (oSect instanceof PSXSectorFrameChunk) {
                    PSXSectorFrameChunk oFrmChk = (PSXSectorFrameChunk)oSect;
                    if (oFrmChk.getFrameNumber() == m_lngEndFrame && 
                       (oFrmChk.getChunkNumber() + 1) == oFrmChk.getChunksInFrame()) 
                    {
                        iLastSectorLastFrame = iCurSect; 
                        if (oIter.hasNext() && (oIter.next() instanceof PSXSectorAudioChunk))
                            blnCouldBeOneMore = true;
                        
                        break;
                    }
                }
            }
            if (iLastSectorLastFrame < 0)
                return "where's the last sector?";
            
            return "well... " + 
                    (iLastSectorLastFrame - iStartSector + 1) / (double)(m_lngEndFrame - m_lngStartFrame + 1)
                    + (blnCouldBeOneMore ? (" or even " + (iLastSectorLastFrame - iStartSector + 2) / (double)(m_lngEndFrame - m_lngStartFrame + 1)) : "");

            
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
}