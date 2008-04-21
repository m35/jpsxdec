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

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.audiodecoding.ADPCMDecodingContext;
import jpsxdec.audiodecoding.StrADPCMDecoder;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSectorNull;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.audiodecoding.Short2dArrayInputStream;
import jpsxdec.util.NotThisTypeException;

// TODO: Add doc

public class PSXMediaXA extends PSXMediaStreaming {
    
    public static ArrayList<PSXMediaXA> FindXAs(PSXSectorRangeIterator oSectIterator) throws IOException {
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorAudioChunk))
            return null;
        
        PSXMediaXA aoChannels[] = new PSXMediaXA[32];
        ArrayList<PSXMediaXA> oXAList = new ArrayList<PSXMediaXA>();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        PSXMediaXA oXAChan = new PSXMediaXA(oSectIterator.getSourceCD(), 
                                            (PSXSectorAudioChunk)oPsxSect);
        oXAList.add(oXAChan);
        aoChannels[(int)oPsxSect.getChannel()] = oXAChan;
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorNull) {
                
                for (int i = 0; i < aoChannels.length; i++) {
                    oXAChan = aoChannels[i];

                    if (oXAChan != null) {
                        if (oXAChan.addSector((PSXSectorNull)oPsxSect)) {
                            aoChannels[i] = null;
                        }
                    }
                }

            } else if (oPsxSect instanceof PSXSectorAudioChunk) {
                
                    PSXSectorAudioChunk oAudio = (PSXSectorAudioChunk)oPsxSect;

                    oXAChan = aoChannels[(int)oAudio.getChannel()];
                    if (oXAChan == null) {
                        oXAChan = new PSXMediaXA(oSectIterator.getSourceCD(), oAudio);
                        oXAList.add(oXAChan);
                        aoChannels[(int)oAudio.getChannel()] = oXAChan;
                    } else {
                        if (oXAChan.addSector(oAudio)) {
                            oXAChan = new PSXMediaXA(oSectIterator.getSourceCD(), oAudio);
                            oXAList.add(oXAChan);
                            aoChannels[(int)oAudio.getChannel()] = oXAChan;
                        }
                    }
                
                    
                }  else {
                    break; // some other sector type? we're done.
                }
            
            oSectIterator.skipNext();
        } // while

        return oXAList;
    }
    
    /** In the future this *may* instead hold how many samples,
     *  or perhaps sectors, found for each channel, but for now,
     *  it just holds 1 or 0 for if a channel has audio */
    private long m_lngSampleCount = 0;
    
    final private AudioChannelInfo m_oAuInfo;
    
    
    public PSXMediaXA(CDSectorReader oCD, PSXSectorAudioChunk oAudio)
    {
        super(oCD);
        
        if (DebugVerbose > 2)
            System.err.println(oAudio.toString());
        
        m_oAuInfo = new AudioChannelInfo();

        m_oAuInfo.LastAudioSect = oAudio.getSector();
        m_oAuInfo.BitsPerSample = oAudio.getBitsPerSample();
        m_oAuInfo.SamplesPerSecond = oAudio.getSamplesPerSecond();
        m_oAuInfo.MonoStereo = oAudio.getMonoStereo();
        m_oAuInfo.Channel = oAudio.getChannel();

        m_iStartSector = oAudio.getSector();
        m_iEndSector = m_iStartSector;
        
        m_lngSampleCount += oAudio.getSampleLength();
        
    }
    
    
    private boolean addSector(PSXSectorAudioChunk oAudio) {
        
                    
        if (m_oAuInfo.BitsPerSample != oAudio.getBitsPerSample() ||
            m_oAuInfo.SamplesPerSecond != oAudio.getSamplesPerSecond() ||
            m_oAuInfo.MonoStereo != oAudio.getMonoStereo() ||
            m_oAuInfo.Channel != oAudio.getChannel()) 
        {
            return true;
        }

        if (m_oAuInfo.AudioPeriod >= 1) {
            if ((oAudio.getSector() - m_oAuInfo.LastAudioSect) != m_oAuInfo.AudioPeriod)
                return true;
        } else {
            m_oAuInfo.AudioPeriod = oAudio.getSector() - m_oAuInfo.LastAudioSect;
        }

        m_oAuInfo.LastAudioSect = oAudio.getSector();

        m_iEndSector = oAudio.getSector();
        
        m_lngSampleCount += oAudio.getSampleLength();
        
        return false;
    }

    
    private boolean addSector(PSXSectorNull oNull) {
        if ((oNull.getSector() - m_oAuInfo.LastAudioSect) == m_oAuInfo.AudioPeriod)
            return true;
        else
            return false;
    }
    
    
    public PSXMediaXA(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "XA");
        IndexLineParser oParse = new IndexLineParser("$| Samples # ", sSerial);
        
        try {
            
            oParse.skip();
            m_lngSampleCount = oParse.get(m_lngSampleCount);

        } catch (NoSuchElementException ex) {
            throw new NotThisTypeException();
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        }
        
        m_oAuInfo = new AudioChannelInfo(oParse.getRemaining());
    }
    
    
    public String toString() {
        return super.toString("XA") + 
                String.format("| Samples %d ", m_lngSampleCount)
                + m_oAuInfo.toString();
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_XA;
    }

    public int getChannel() {
        return (int)m_oAuInfo.Channel;
    }

    //--------------------------------------------------------------------------
    //-- Playing ---------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    ADPCMDecodingContext m_oLeftContext;
    ADPCMDecodingContext m_oRightContext;
    
    @Override
    protected void startPlay() {
        m_oLeftContext = new ADPCMDecodingContext(1.0);
        m_oRightContext = new ADPCMDecodingContext(1.0);
    }

    @Override
    protected void playSector(PSXSector oPSXSect) throws StopPlayingException, IOException {
        if (oPSXSect instanceof PSXSectorAudioChunk) {
            // only process the audio if there is a listener for it
            if (super.m_oAudioListener != null) {
                PSXSectorAudioChunk oAudChk = (PSXSectorAudioChunk)oPSXSect;
                
                // make sure this is the right channel
                if (oAudChk.getChannel() == m_oAuInfo.Channel) {
                
                    short[][] asiDecoded = 
                            StrADPCMDecoder.DecodeMore(oAudChk.getUserDataStream(), 
                                                       oAudChk.getBitsPerSample(), 
                                                       oAudChk.getMonoStereo(), 
                                                       m_oLeftContext, 
                                                       m_oRightContext);

                    m_oAudioListener.event(
                        new AudioInputStream(
                            new Short2dArrayInputStream(asiDecoded), 
                            oAudChk.getAudioFormat(),
                            oAudChk.getSampleLength()
                        ),
                        oAudChk.getSector()
                    );
                }  
            }
        }
    }
    
    @Override
    protected void endPlay() throws IOException {
        m_oLeftContext = new ADPCMDecodingContext(1.0);
        m_oRightContext = new ADPCMDecodingContext(1.0);
        super.endPlay();
    }

    @Override
    public void seek(long lngFrame) throws IOException {
        throw new UnsupportedOperationException("Seeking in XA is not available.");
    }

    @Override
    public boolean hasVideo() {
        return false;
    }

    @Override
    public int getAudioChannels() {
        return (int)m_oAuInfo.MonoStereo;
    }
    
    @Override
    public int getSamplesPerSecond() {
        return (int)m_oAuInfo.SamplesPerSecond;
    }


    @Override
    public FramesPerSecond[] getPossibleFPS() {
        throw new UnsupportedOperationException("XA doesn't have frames.");    }

    @Override
    public long getStartFrame() {
        throw new UnsupportedOperationException("XA doesn't have frames.");    }

    @Override
    public long getEndFrame() {
        throw new UnsupportedOperationException("XA doesn't have frames.");    }

    @Override
    public long getWidth() {
        throw new UnsupportedOperationException("XA doesn't have frames.");    }

    @Override
    public long getHeight() {
        throw new UnsupportedOperationException("XA doesn't have frames.");    }

}