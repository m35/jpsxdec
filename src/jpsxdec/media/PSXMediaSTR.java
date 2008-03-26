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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.sound.sampled.*;
import jpsxdec.audiodecoding.ADPCMDecodingContext;
import jpsxdec.audiodecoding.StrADPCMDecoder;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.media.StrFpsCalc.*;
import jpsxdec.media.savers.StopPlayingException;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.sectortypes.PSXSectorAudio2048;
import jpsxdec.sectortypes.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSectorFrameChunk;
import jpsxdec.sectortypes.PSXSectorNull;
import jpsxdec.audiodecoding.Short2dArrayInputStream;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.Misc;


/** Represents standard STR movies, and STR movies that deviate slightly from
 *  standard STR movies: Lain and FF7. */
public class PSXMediaSTR extends PSXMediaStreaming 
{
    /** List of many theoretically possible 
     * sectors/frame and sectors/audio combinations.
     * @see StrFpsCalc */
    public static FrameSequence[] POSSIBLE_CHUNK_SEQUENCES = {
          // sectors/frame, sectors/audio
      new FrameSequence(15, 32),
      new FrameSequence(12, 32), // river1.str
      new FrameSequence(10, 32),
      new FrameSequence( 9, 32),
      new FrameSequence( 6, 32),
      new FrameSequence( 5, 32),
      new FrameSequence( 3, 32),
      new FrameSequence(15, 16),
      new FrameSequence(12, 16),
      new FrameSequence(10, 16),
      new FrameSequence( 9, 16),
      new FrameSequence( 6, 16),
      new FrameSequence( 5, 16),
      new FrameSequence( 3, 16),
      new FrameSequence(15,  8),
      new FrameSequence(12,  8),
      new FrameSequence(10,  8),
      new FrameSequence( 9,  8),
      new FrameSequence( 6,  8),
      new FrameSequence( 5,  8),
      new FrameSequence( 3,  8),
      new FrameSequence(15,  4),
      new FrameSequence(12,  4),
      new FrameSequence(10,  4),
      new FrameSequence( 9,  4),
      new FrameSequence( 6,  4),
      new FrameSequence( 5,  4),
      new FrameSequence( 3,  4),
      new FrameSequence(15),
      new FrameSequence(12),
      new FrameSequence(10),
      new FrameSequence( 9),
      new FrameSequence( 6), // Valkyrie profile
      new FrameSequence( 5),
      new FrameSequence( 3)
    };
    
    // video
    private long m_lngWidth = -1;
    private long m_lngHeight = -1;
    
    /** First video frame number. */
    private long m_lngStartFrame = -1;
    /** Last video frame number. */
    private long m_lngEndFrame = -1;
    
    // audio
    /** 0 if it doesn't have audio, 1 for mono, 2 for stereo */
    private int m_iAudioChannels = 0;
    
    private int m_iAudioPeriod = -1;
    private int m_iAudioSampleRate = -1;
    private int m_iAudioBitsPerSample = -1;
    
    /** 0 if it doesn't have audio, >0 if it does */
    private long m_lngAudioTotalSamples = 0;
    
    private final FramesPerSecond[] m_aoPossibleFps;
    
    public PSXMediaSTR(PSXSectorRangeIterator oSectIterator) throws NotThisTypeException, IOException
    {
        super(oSectIterator.getSourceCD());
        
        AudioChannelInfo oAudInf = null;
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorFrameChunk))
            throw new NotThisTypeException();
        
        PSXSectorFrameChunk oFrame;
        PSXSectorAudioChunk oAudio;
        
        oFrame = (PSXSectorFrameChunk) oPsxSect;

        m_lngWidth = oFrame.getWidth();
        m_lngHeight = oFrame.getHeight();
        long lngCurFrame = oFrame.getFrameNumber();

        super.m_iStartSector = oPsxSect.getSector();
        super.m_iEndSector = m_iStartSector;

        m_lngStartFrame = oFrame.getFrameNumber();
        m_lngEndFrame = oFrame.getFrameNumber();
        
        LinkedList<StrFpsCalc.FrameSequenceWalker> oSequences = 
            StrFpsCalc.GenerateSequenceWalkers(oFrame, POSSIBLE_CHUNK_SEQUENCES);
        
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
                if (oFrame.getWidth() == m_lngWidth &&
                        oFrame.getHeight() == m_lngHeight &&
                        (oFrame.getFrameNumber() == lngCurFrame ||
                        oFrame.getFrameNumber() == lngCurFrame+1)) {

                    lngCurFrame = oFrame.getFrameNumber();
                    m_lngEndFrame = lngCurFrame;
                } else {
                    break;
                }

                m_iEndSector = oPsxSect.getSector();
            }  else if (oPsxSect instanceof PSXSectorAudioChunk) {
                    oAudio = (PSXSectorAudioChunk) oPsxSect;

                    if (oAudInf == null) {
                        oAudInf = new AudioChannelInfo();
                        oAudInf.LastAudioSect = oPsxSect.getSector();
                        oAudInf.MonoStereo = oAudio.getMonoStereo();
                        oAudInf.SamplesPerSecond = oAudio.getSamplesPerSecond();
                        oAudInf.BitsPerSample = oAudio.getBitsPerSample();
                        oAudInf.Channel = oAudio.getChannel();
                    } else {
                        if (oAudio.getMonoStereo() != oAudInf.MonoStereo ||
                            oAudio.getSamplesPerSecond() != oAudInf.SamplesPerSecond ||
                            oAudio.getBitsPerSample() != oAudInf.BitsPerSample ||
                            oAudio.getChannel() != oAudInf.Channel) 
                        {
                            break;
                        }

                        oAudInf.AudioPeriod = oPsxSect.getSector() - oAudInf.LastAudioSect;
                        oAudInf.LastAudioSect = oPsxSect.getSector();

                        m_lngAudioTotalSamples += oAudio.getSampleLength();
                    }

                    m_iEndSector = oPsxSect.getSector();
                }  else {
                    break; // some other sector type? we're done.
                }
            
            if (oPsxSect != null && DebugVerbose > 2)
                System.err.println(oPsxSect.toString());
            
            for (Iterator<StrFpsCalc.FrameSequenceWalker> it = oSequences.iterator(); it.hasNext();) {
                StrFpsCalc.FrameSequenceWalker oWalker = it.next();
                if (!oWalker.Next(oPsxSect))
                    it.remove();
            }
            
            oSectIterator.skipNext();
        } // while
        
        
        long lngFramesPerMovie = (m_lngEndFrame - m_lngStartFrame + 1);
        long lngSectorsPerMovie = (super.m_iEndSector - super.m_iStartSector + 1);
        
        // now wrap up the frame rate calculation
        if (oAudInf != null && oAudInf.AudioPeriod >= 1) {
            m_iAudioPeriod = (int)oAudInf.AudioPeriod;
            m_iAudioChannels = (int)oAudInf.MonoStereo;
            m_iAudioBitsPerSample = (int)oAudInf.BitsPerSample;
            m_iAudioSampleRate = (int)oAudInf.SamplesPerSecond;
            
            long lngSectorsPerSecond = StrFpsCalc.GetSectorsPerSecond(
                            m_iAudioPeriod, 
                            m_iAudioSampleRate, 
                            m_iAudioChannels);
            
            m_aoPossibleFps = new FramesPerSecond[] {
                        StrFpsCalc.FigureOutFps(oSequences, 
                                     lngSectorsPerSecond, 
                                     lngFramesPerMovie, 
                                     lngSectorsPerMovie)
            };
        } else {
            // if there is no audio, then we don't know the disc speed,
            // so there are 2 possible frames/second
            m_iAudioChannels = 0;
            
            m_aoPossibleFps = new FramesPerSecond[] {
                StrFpsCalc.FigureOutFps(oSequences, 150, lngFramesPerMovie, lngSectorsPerMovie),
                StrFpsCalc.FigureOutFps(oSequences, 75, lngFramesPerMovie, lngSectorsPerMovie)
            };
        }
        
        //System.out.println("The next item is sect/frame: " + jpsxdec.util.Misc.join(m_aoPossibleFps, ", "));
        
    }
    
    public PSXMediaSTR(CDSectorReader oCD, String sSerial) throws NotThisTypeException
    {
        super(oCD, sSerial, "STR");
        
        boolean blnFailed = true;
        ArrayList<FramesPerSecond> oFps = null;
        
        try {
            IndexLineParser parse = new IndexLineParser(
                    "$| Frames #-# #x# | Audio channels # |", sSerial);
            
            parse.skip();
            m_lngStartFrame  = parse.get(m_lngStartFrame);
            m_lngEndFrame    = parse.get(m_lngEndFrame);
            m_lngWidth         = parse.get(m_lngWidth);
            m_lngHeight        = parse.get(m_lngHeight);
            m_iAudioChannels = parse.get(m_iAudioChannels);
            
            String sRemain = parse.getRemaining();
            
            if (m_iAudioChannels > 0) {
                parse = new IndexLineParser(
                        " Period # Rate # Bits # Total # |", sRemain);
                
                m_iAudioPeriod         = parse.get(m_iAudioPeriod);
                m_iAudioSampleRate     = parse.get(m_iAudioSampleRate);
                m_iAudioBitsPerSample  = parse.get(m_iAudioBitsPerSample);
                m_lngAudioTotalSamples = parse.get(m_lngAudioTotalSamples);
                
                sRemain = parse.getRemaining();
            }
            
            oFps = new ArrayList<FramesPerSecond>(10);
            do {
                parse = new IndexLineParser(
                        " #x #/#", sRemain);

                int iSpd = parse.get(0);
                long iNum   = parse.get(0L);
                long iDenom = parse.get(0L);
                oFps.add(new FramesPerSecond(iNum, iDenom, iSpd));

                blnFailed = false;
                sRemain = parse.getRemaining();
            } while (true);
            
        } catch (NumberFormatException ex) {
            if (blnFailed) throw new NotThisTypeException();
        } catch (IllegalArgumentException ex) {
            if (blnFailed) throw new NotThisTypeException();
        } catch (NoSuchElementException ex) {
            if (blnFailed) throw new NotThisTypeException();
        }
        
        m_aoPossibleFps = oFps.toArray(new FramesPerSecond[0]);
    }
    public String toString() {
        /* ###:STR:start-end:frmfrst-frmlast:WIDTHxHEGHT:auch[bitpsamp,samppsec,audioperiod,numausect]:fps1:fps2
         */
        String s = super.toString("STR") +
                String.format(
                "| Frames %d-%d %dx%d | Audio channels %d | ",
                m_lngStartFrame, m_lngEndFrame, 
                m_lngWidth, m_lngHeight,
                m_iAudioChannels);
        if (m_iAudioChannels > 0) {
            s += String.format(
                "Period %d Rate %d Bits %d Total %d | ",
                m_iAudioPeriod,
                m_iAudioSampleRate,
                m_iAudioBitsPerSample,
                m_lngAudioTotalSamples);
        }
        
        return s + Misc.join(m_aoPossibleFps, " ");
    }
    
    public long getAudioSampleLength() {
        return m_lngAudioTotalSamples;
    }
    
    @Override
    public long getStartFrame() {
        return m_lngStartFrame;
    }
    
    @Override
    public long getEndFrame() {
        return m_lngEndFrame;
    }

    @Override
    public long getWidth() {
        return m_lngWidth;
    }

    @Override
    public long getHeight() {
        return m_lngHeight;
    }
    
    public int getMediaType() {
        if (m_lngAudioTotalSamples == 0)
            return PSXMedia.MEDIA_TYPE_VIDEO;
        else
            return PSXMedia.MEDIA_TYPE_VIDEO_AUDIO;
    }

    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public int getAudioChannels() {
        return m_iAudioChannels;
    }

    @Override
    public FramesPerSecond[] getPossibleFPS() {
        return m_aoPossibleFps;
    }

    @Override
    public int getSamplesPerSecond() {
        return m_iAudioSampleRate;
    }
    
    
    
    //--------------------------------------------------------------------------
    //-- Playing ---------------------------------------------------------------
    //--------------------------------------------------------------------------

    private ADPCMDecodingContext m_oLeftContext;
    private ADPCMDecodingContext m_oRightContext;
    
    @Override
    protected void startPlay() {
        m_oLeftContext = new ADPCMDecodingContext(1.0);
        m_oRightContext = new ADPCMDecodingContext(1.0);
    }

    @Override
    protected void playSector(PSXSector oPSXSect) throws StopPlayingException, IOException {
        if (oPSXSect instanceof PSXSectorFrameChunk) {
            
            super.playVideoSector((PSXSectorFrameChunk)oPSXSect);
            
        } else if (oPSXSect instanceof PSXSectorAudioChunk) {
            // only process the audio if there is a listener for it
            if (super.m_oAudioListener != null) {
                PSXSectorAudioChunk oAudChk = (PSXSectorAudioChunk)oPSXSect;
                
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
    
    
    @Override
    protected void endPlay() throws IOException {
        m_oLeftContext = null;
        m_oRightContext = null;
        super.endPlay();
    }

    @Override
    public void seek(long lngFrame) throws IOException {
        // clamp the desired frame
        if (lngFrame < m_lngStartFrame) 
            lngFrame = (int)m_lngStartFrame; 
        else if (lngFrame > m_lngEndFrame) 
            lngFrame = (int)m_lngEndFrame;
        // calculate an estimate where the frame will land
        double percent = (lngFrame - m_lngStartFrame) / (double)(m_lngEndFrame - m_lngStartFrame);
        // backup 10% of the size of the media to 
        // hopefully land shortly before the frame
        int iSect = (int)
                ( (super.m_iEndSector - super.m_iStartSector) * (percent-0.1) ) 
                + super.m_iStartSector;
        if (iSect < super.m_iStartSector) iSect = super.m_iStartSector;
        
        super.m_oCDIter.gotoIndex(iSect);
        
        // now seek ahead until we read the desired frame
        CDXASector oCDSect = super.m_oCDIter.peekNext();
        PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        while (!(oPsxSect instanceof PSXSectorFrameChunk) ||
               ((PSXSectorFrameChunk)oPsxSect).getFrameNumber() < lngFrame) 
        {
            super.m_oCDIter.skipNext();
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }
        
        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(oPsxSect instanceof PSXSectorFrameChunk) ||
               ((PSXSectorFrameChunk)oPsxSect).getFrameNumber() > lngFrame ||
               ((PSXSectorFrameChunk)oPsxSect).getChunkNumber() > 0)
        {
            super.m_oCDIter.gotoIndex(m_oCDIter.getIndex() - 1);
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }
        
    }

}