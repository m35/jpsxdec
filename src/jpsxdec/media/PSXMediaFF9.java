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
 * PSXMediaFF9.java
 */

package jpsxdec.media;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.audiodecoding.FF8and9AudioDemuxerDecoderIS;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.demuxers.FF9FrameDemuxerIS;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.sectortypes.PSXSector.PSXSectorFF9Abstract;
import jpsxdec.util.NotThisTypeException;


public class PSXMediaFF9 extends PSXMedia {
    
    long m_lngStartFrame = -1;
    long m_lngEndFrame = -1;
    
    public PSXMediaFF9(PSXSectorRangeIterator oSectIterator) 
                throws NotThisTypeException, IOException 
    {
        super(oSectIterator);
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFF9Abstract))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        PSXSectorFF9Abstract oFF9Sect;
        
        oFF9Sect = (PSXSectorFF9Abstract)oPsxSect;

        super.m_iStartSector = oPsxSect.getSector();
        super.m_iEndSector = m_iStartSector;

        long iCurFrame = oFF9Sect.getFrameNumber();
        m_lngStartFrame = oFF9Sect.getFrameNumber();
        m_lngEndFrame = oFF9Sect.getFrameNumber();
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorFF9Abstract) {
                
                oFF9Sect = (PSXSectorFF9Abstract)oPsxSect;
                if (oFF9Sect.getFrameNumber() == iCurFrame ||
                    oFF9Sect.getFrameNumber() == iCurFrame+1) 
                {
                    iCurFrame = oFF9Sect.getFrameNumber();
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

    public PSXMediaFF9(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "FF9");
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
        return super.toString("FF9") + ":"
                + m_lngStartFrame + "-" + m_lngEndFrame;
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO;
    }
    
    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public boolean hasAudio() {
        return true;
    }

    @Override
    public void DecodeVideo(String sFileBaseName, String sImgFormat, 
                            Integer oiStartFrame, Integer oiEndFrame) 
    {
        PSXSectorRangeIterator oIter = GetSectorIterator();
        
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
        
        for (long iFrameIndex = lngStart; iFrameIndex <= lngEnd; iFrameIndex++) 
        {
            String sFrameFile = 
                    sFileBaseName + 
                    String.format("_f%04d", iFrameIndex)
                    + "." + sImgFormat;
            try {
                
                FF9FrameDemuxerIS str = 
                        new FF9FrameDemuxerIS(oIter, iFrameIndex);
                
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
    public void DecodeAudio(String sFileBaseName, String sAudFormat, Double odblScale) {
        
        try {
            if (!super.Progress("Decoding movie audio", 0)) {
                return;
            }

            PSXSectorRangeIterator oIter = GetSectorIterator();

            FF8and9AudioDemuxerDecoderIS dec =
                    odblScale == null ? new FF8and9AudioDemuxerDecoderIS(oIter)
                    : new FF8and9AudioDemuxerDecoderIS(oIter, odblScale);

            AudioInputStream oAudStream = new AudioInputStream(dec, dec.getFormat(), dec.getLength());

            String sFileName = sFileBaseName + "." + sAudFormat;

            AudioFileFormat.Type oType = super.AudioFileFormatStringToType(sAudFormat);

            AudioSystem.write(oAudStream, oType, new File(sFileName));

        } catch (IOException ex) {
            super.Error(ex);
        }
    }
    
}
