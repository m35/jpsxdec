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
public class PSXMediaSTR extends PSXMedia implements VideoFrameConverter.IVideoMedia  {
    
    //** 0 if it doesn't have audio, nonzero if it does */
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
                    }  else                        if (oPsxSect instanceof PSXSectorAudioChunk) {
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
    
    public boolean HasAudio() {
        return m_lngAudioSampleLength != 0;
    }
    
    public PSXSectorRangeIterator GetSectorIterator() {
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
    }
    
    public long GetAudioSampleLength() {
        return m_lngAudioSampleLength;
    }
    
    public String toString() {
        return "STR:" + super.toString() + ":"
                + m_lngStartFrame + "-" + m_lngEndFrame + ":" 
                + m_lngAudioSampleLength;
    }
    
    public long GetStartFrame() {
        return m_lngStartFrame;
    }
    
    public long GetEndFrame() {
        return m_lngEndFrame;
    }

    
    public int getMediaType() {
        if (m_lngAudioSampleLength == 0)
            return PSXMedia.MEDIA_TYPE_VIDEO;
        else
            return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO;
    }

    public void Decode(Options oOpt) throws IOException {
        if (oOpt instanceof Options.IVideoOptions)
            DecodeVideo((Options.IVideoOptions)oOpt);
        else if (oOpt instanceof Options.IAudioOptions)
            DecodeAudio((Options.IAudioOptions)oOpt);
        else
            throw new IllegalArgumentException("Wrong Options type");
    }
    
    // -------------------------------------------------------------------------
    
    private void DecodeVideo(Options.IVideoOptions oVidOpts) throws IOException {
        
        long lngStart = 
                VideoFrameConverter.ClampStartFrame(oVidOpts.getStartFrame(), m_lngStartFrame, m_lngEndFrame);
        
        long lngEnd =
                VideoFrameConverter.ClampEndFrame(oVidOpts.getEndFrame(), m_lngStartFrame, m_lngEndFrame);
        
        if (lngStart > lngEnd) {
            long lng = lngStart;
            lngStart = lngEnd;
            lngEnd = lng;
        }
        
        PSXSectorRangeIterator oIter = GetSectorIterator();
        int iSaveIndex1 = oIter.getIndex();
        int iSaveIndex2 = iSaveIndex1;
        
        for (long iFrameIndex = lngStart; iFrameIndex <= lngEnd; iFrameIndex++) 
        {
            String sFrameFile = 
                    String.format(oVidOpts.getOutputFileName(), iFrameIndex)
                    + "." + oVidOpts.getOutputImageFormat();
            try {
                
                oIter.gotoIndex(iSaveIndex1);
                StrFrameDemuxerIS str = 
                        new StrFrameDemuxerIS(oIter, iFrameIndex);
                
                if (DebugVerbose > 0)
                    System.err.println("Reading frame " + iFrameIndex);

                VideoFrameConverter.DecodeAndSaveFrame(
                        "demux",
                        oVidOpts.getOutputImageFormat(),
                        str,
                        sFrameFile,
                        -1,
                        -1);
                
                // if the iterator was searched to the end, we don't
                // want to save the end sector (or we'll miss sectors
                // before it). This can happen when saving as demux because 
                // it has to search to the end of the stream
                if (oIter.hasNext()) {
                    iSaveIndex1 = iSaveIndex2;
                    iSaveIndex2 = oIter.getIndex();
                }

            } catch (IOException ex) {
                if (DebugVerbose > 2)
                    ex.printStackTrace();
                else if (DebugVerbose > 0)
                    System.err.println(ex.getMessage());
            }

        } // for
        
    }
    
    private void DecodeAudio(Options.IAudioOptions oAudOpts) throws IOException {
        if (m_lngAudioSampleLength == 0) return;
        
        PSXSectorRangeIterator oIter = GetSectorIterator();
        StrAudioDemuxerDecoderIS dec = 
                new StrAudioDemuxerDecoderIS(oIter, oAudOpts.getScale());
        AudioInputStream oAudStream = new AudioInputStream(dec, dec.getFormat(), dec.getLength());
        
        String sFileName = oAudOpts.getOutputFileName() + "." + oAudOpts.getOutputFormat();
        
        AudioFileFormat.Type oType = super.AudioFileFormatStringToType(oAudOpts.getOutputFormat());
        
        AudioSystem.write(oAudStream, oType, new File(sFileName));
    }

}