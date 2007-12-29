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
 * PSXMediaXA.java
 */

package jpsxdec.media;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.audiodecoding.StrAudioDemuxerDecoderIS;
import jpsxdec.sectortypes.PSXSector.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSector.PSXSectorNull;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;



/**************************************************************************/
/**************************************************************************/

public class PSXMediaXA extends PSXMedia {
    
    long m_iAudioPeriod = -1;
    
    /** In the future this *may* instead hold how many samples,
     *  or perhaps sectors, found for each channel, but for now,
     *  it just holds 1 or 0 for if a channel has audio */
    long[] m_alngChannelHasAudio = new long[]{
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
        };
    
    
    public PSXMediaXA(PSXSectorRangeIterator oSectIterator) throws NotThisTypeException, IOException
    {
        super(oSectIterator);
        AudioChannelInfo aoChannelInfos[] = new AudioChannelInfo[32];
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorAudioChunk))
            throw new NotThisTypeException();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        m_iStartSector = oPsxSect.getSector();
        m_iEndSector = m_iStartSector;
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorNull) {
                // skip
            } else if (oPsxSect instanceof PSXSectorAudioChunk) {
                    
                    PSXSectorAudioChunk oAudio = (PSXSectorAudioChunk)oPsxSect;
                    
                    if (aoChannelInfos[(int)oAudio.getChannel()] != null) {
                        
                        AudioChannelInfo m_oAuInfo = aoChannelInfos[(int)oAudio.getChannel()];
                        
                        if (m_oAuInfo.BitsPerSampele != oAudio.getBitsPerSample() ||
                                m_oAuInfo.SamplesPerSecond != oAudio.getSamplesPerSecond() ||
                                m_oAuInfo.MonoStereo != oAudio.getMonoStereo() ||
                                m_oAuInfo.Channel != oAudio.getChannel()) 
                        {
                            break;
                        }
                        
                        if (m_iAudioPeriod > 0) {
                            if ((oAudio.getSector() - m_oAuInfo.LastAudioSect) != m_iAudioPeriod)
                                break;
                        } else {
                            m_iAudioPeriod = oAudio.getSector() - m_oAuInfo.LastAudioSect;
                        }
                        
                        m_oAuInfo.LastAudioSect = oAudio.getSector();
                        
                    }  else {
                        AudioChannelInfo m_oAuInfo = new AudioChannelInfo();
                        
                        m_oAuInfo.LastAudioSect = oAudio.getSector();
                        m_oAuInfo.BitsPerSampele = oAudio.getBitsPerSample();
                        m_oAuInfo.SamplesPerSecond = oAudio.getSamplesPerSecond();
                        m_oAuInfo.MonoStereo = oAudio.getMonoStereo();
                        m_oAuInfo.Channel = oAudio.getChannel();
                        
                        aoChannelInfos[(int) oAudio.getChannel()] = m_oAuInfo;
                    }
                    
                    m_alngChannelHasAudio[(int) oAudio.getChannel()] += oAudio.getSampleLength();
                    
                    m_iEndSector = oPsxSect.getSector();
                }  else {
                    break; // some other sector type? we're done.
                }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            oSectIterator.skipNext();
        } // while
        
    }
    
    public PSXMediaXA(CDSectorReader oCD, String sSerial) throws NotThisTypeException, IOException
    {
        super(oCD, sSerial, "XA");
        String asParts[] = sSerial.split(":");
        
        String asChannels[] = asParts[3].split(",");
        try {
            for (int i = 0; i < asChannels.length; i++) {
                int iChan = Integer.parseInt(asChannels[i]);
                m_alngChannelHasAudio[iChan] = 1;
            }
        }  catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
    }
    
    /** Returns null if no sectors are available */
    public PSXSectorRangeIterator GetChannelSectorIterator(int iChan) {
        assert(iChan >= 0 && iChan < 31);
        
        if (m_alngChannelHasAudio[iChan] == 0) return null;
        return new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
        
    }
    
    public String toString() {
        StringBuilder oSB = new StringBuilder();
        for (int i = 0; i < m_alngChannelHasAudio.length; i++) {
            if (m_alngChannelHasAudio[i] != 0) {
                if (oSB.length() > 0)
                    oSB.append(",");
                oSB.append(i);
            }
        }
        return "XA:" + super.toString() + ":" + oSB.toString();
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_XA;
    }

    public void Decode(Options oOpt) throws IOException {
        if (!(oOpt instanceof Options.IXAOptions))
            throw new IllegalArgumentException("Wrong Options type");

        Options.IXAOptions oXAOpts = (Options.IXAOptions)oOpt;
        
        if (oXAOpts.getChannel() < -1 || oXAOpts.getChannel() > 31) 
            throw new IllegalArgumentException();
        
        int iChannelStart, iChannelEnd, iChannelIndex;
        
        if (oXAOpts.getChannel() == -1) { // decode all if the channel is -1
            iChannelStart = 0;
            iChannelEnd = 31;
        } else if (oXAOpts.getChannel() >= 0 && oXAOpts.getChannel() <= 31) {
            iChannelStart = iChannelEnd = oXAOpts.getChannel();
        } else {
            throw new IllegalArgumentException("Invalid channel");
        }
        
        AudioFileFormat.Type oType = 
                super.AudioFileFormatStringToType(oXAOpts.getOutputFormat());
        
        for (iChannelIndex = iChannelStart; iChannelIndex <= iChannelEnd; iChannelIndex++) {
            if (m_alngChannelHasAudio[iChannelIndex] == 0) continue;
            
            System.err.println("Decoding channel " + iChannelIndex);
            
            PSXSectorRangeIterator oIter =
                    new PSXSectorRangeIterator(m_oCD, m_iStartSector, m_iEndSector);
            StrAudioDemuxerDecoderIS dec = 
                    new StrAudioDemuxerDecoderIS(oIter, iChannelIndex);
            AudioInputStream str = 
                    new AudioInputStream(dec, dec.getFormat(), dec.getLength());
            
            String sFileName = String.format(
                    oXAOpts.getOutputFileName() + "." + oXAOpts.getOutputFormat(), 
                    iChannelIndex);
            
            AudioSystem.write(str, oType, new File(sFileName));
        }

    }

}