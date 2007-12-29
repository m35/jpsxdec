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
 * PSXMediaFF8.java
 */

package jpsxdec.media;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.audiodecoding.FF8AudioDemuxerDecoderIS;
import jpsxdec.demuxers.StrFrameDemuxerIS;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSector.PSXSectorFF8Abstract;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;


public class PSXMediaFF8 extends PSXMedia implements VideoFrameConverter.IVideoMedia {
    
    long m_lngStartFrame = -1;
    long m_lngEndFrame = -1;
    
    public PSXMediaFF8(PSXSectorRangeIterator oSectIterator) 
                throws NotThisTypeException, IOException
    {
        super(oSectIterator);
        
        long iAudioPeriod = -1;
        long iCurFrame = -1;
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFF8Abstract))
            throw new NotThisTypeException();
        
        PSXSectorFF8Abstract oFF8Sect;
        
        oFF8Sect = (PSXSectorFF8Abstract) oPsxSect;

        iCurFrame = oFF8Sect.getFrameNumber();

        m_iStartSector = oPsxSect.getSector();
        m_iEndSector = m_iStartSector;

        m_lngStartFrame = oFF8Sect.getFrameNumber();
        m_lngEndFrame = oFF8Sect.getFrameNumber();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorFF8Abstract) {
                
                oFF8Sect = (PSXSectorFF8Abstract) oPsxSect;
                if (oFF8Sect.getFrameNumber() == iCurFrame ||
                    oFF8Sect.getFrameNumber() == iCurFrame+1) 
                {
                    iCurFrame = oFF8Sect.getFrameNumber();
                    m_lngEndFrame = iCurFrame;
                } else {
                    break;
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
    
    public PSXMediaFF8(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "FF8");
        String asParts[] = sSerial.split(":");
        if (asParts.length != 4)
            throw new NotThisTypeException();
        
        String asStartEndFrame[] = asParts[3].split("-");
        try {
            m_lngStartFrame = Integer.parseInt(asStartEndFrame[0]);
            m_lngEndFrame = Integer.parseInt(asStartEndFrame[1]);
        }  catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    public PSXSectorRangeIterator GetSectorIterator() {
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
    }
    
    public String toString() {
        return "FF8:" + super.toString() + ":"
                + m_lngStartFrame + "-" + m_lngEndFrame;
    }
    
    public long GetStartFrame() {
        return m_lngStartFrame;
    }
    
    public long GetEndFrame() {
        return m_lngEndFrame;
    }

    public int getMediaType() {
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
        PSXSectorRangeIterator oIter = GetSectorIterator();
        int iSaveIndex1 = oIter.getIndex();
        int iSaveIndex2 = iSaveIndex1;
        
        long lngStart = 
                VideoFrameConverter.ClampStartFrame(oVidOpts.getStartFrame(), m_lngStartFrame, m_lngEndFrame);
        
        long lngEnd =
                VideoFrameConverter.ClampEndFrame(oVidOpts.getEndFrame(), m_lngStartFrame, m_lngEndFrame);
        
        if (lngStart > lngEnd) {
            long lng = lngStart;
            lngStart = lngEnd;
            lngEnd = lng;
        }
        
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
        
        PSXSectorRangeIterator oIter = GetSectorIterator();
        FF8AudioDemuxerDecoderIS dec = 
                new FF8AudioDemuxerDecoderIS(oIter, oAudOpts.getScale());
        AudioInputStream oAudStream = new AudioInputStream(dec, dec.getFormat(), dec.getLength());
        
        String sFileName = oAudOpts.getOutputFileName() + "." + oAudOpts.getOutputFormat();
        
        AudioFileFormat.Type oType = super.AudioFileFormatStringToType(oAudOpts.getOutputFormat());
        
        AudioSystem.write(oAudStream, oType, new File(sFileName));
    }
}