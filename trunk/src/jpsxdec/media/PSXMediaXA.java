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

import java.io.DataInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.audiodecoding.ADPCMDecodingContext;
import jpsxdec.audiodecoding.StrADPCMDecoder;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.sectortypes.PSXSector.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSector.PSXSectorNull;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.IO.Short2DArrayInputStream;
import jpsxdec.util.NotThisTypeException;

// TODO:

public class PSXMediaXA extends PSXMedia.PSXMediaStreaming {
    
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
        return super.toString("XA") + oSB.toString();
    }

    public int getMediaType() {
        return PSXMedia.MEDIA_TYPE_XA;
    }

    public int[] getChannelList() {
        // count how many channels there are
        int i = 0;
        for (long l : m_alngChannelHasAudio) {
            if (l > 0) i++;
        }
        // create an int array of those channels
        int[] lst = new int[i];
        i = 0;
        for (int j = 0; j < m_alngChannelHasAudio.length; j++) {
            if (m_alngChannelHasAudio[j] > 0) {
                lst[i] = j;
                i++;
            }
        }
        return lst;
    }

    //--------------------------------------------------------------------------
    //-- Playing ---------------------------------------------------------------
    //--------------------------------------------------------------------------
    
    private static class ContextListener {
        IAudListener Listen;
        ADPCMDecodingContext LeftContext;
        ADPCMDecodingContext RightContext;

        public ContextListener(IAudListener Listen, ADPCMDecodingContext LeftContext, ADPCMDecodingContext RightContext) {
            this.Listen = Listen;
            this.LeftContext = LeftContext;
            this.RightContext = RightContext;
        }
    }
    
    ContextListener[] m_oAud = new ContextListener[32];
    public void addChannelListener(IAudListener oAud, int iChannel) {
        if (iChannel < 0 || iChannel > 31) 
            throw new IllegalArgumentException("Channel should be 0 to 31");
        // even though we're creating 2 contexts, the 2nd won't be used unless it's stereo
        m_oAud[iChannel] = new ContextListener(oAud, new ADPCMDecodingContext(1.0), new ADPCMDecodingContext(1.0));
    }
    
    
    @Override
    protected void startPlay() {
        
    }

    @Override
    protected boolean playSector(PSXSector oPSXSect) throws IOException {
        if (m_oAud != null) {
            if (oPSXSect instanceof PSXSectorAudioChunk) {
                PSXSectorAudioChunk oAudChk = (PSXSectorAudioChunk)oPSXSect;
                ContextListener cl = m_oAud[(int)oAudChk.getChannel()];
                if (cl.Listen != null) { 
                    short[][] asiDecoded = 
                            StrADPCMDecoder.DecodeMore(new DataInputStream(oAudChk), 
                                                       oAudChk.getBitsPerSample(), 
                                                       oAudChk.getMonoStereo(), 
                                                       cl.LeftContext, 
                                                       cl.RightContext);
                    boolean bln = cl.Listen.event(
                        new AudioInputStream(
                            new Short2DArrayInputStream(asiDecoded), 
                            oAudChk.getAudioFormat(),
                            AudioSystem.NOT_SPECIFIED
                        ),
                        oAudChk.getSector()
                    );

                    if (bln) return true;
                }
            }
        }
        return false;
    }
    
    @Override
    protected void endPlay() throws IOException {
    }

    @Override
    public void clearListeners() {
        m_oAud = new ContextListener[32];
        super.clearListeners();
    }

}