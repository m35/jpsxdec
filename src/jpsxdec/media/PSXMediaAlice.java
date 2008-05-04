/*
 * jPSXdec: Playstation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2008  Michael Sabin
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
 * PSXMediaAlice.java
 */

package jpsxdec.media;

import java.io.IOException;
import java.util.NoSuchElementException;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.audiodecoding.ADPCMDecodingContext;
import jpsxdec.audiodecoding.Short2dArrayInputStream;
import jpsxdec.audiodecoding.StrADPCMDecoder;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.sectortypes.PSXSectorAliceFrameChunk;
import jpsxdec.sectortypes.PSXSectorAudio2048;
import jpsxdec.sectortypes.PSXSectorAudioChunk;
import jpsxdec.sectortypes.PSXSectorRangeIterator;
import jpsxdec.util.NotThisTypeException;

/** An embarrisingly ugly hack just to get Alice in Cyberland 
 * variable frame rates saved to AVI. Thanks to this, and 
 * Puzzle Fighter 2 Turbo, I probably should implement disc indexing using
 * an approach more like PsxMC. */
public class PSXMediaAlice extends PSXMediaStreaming {
    
    // video
    private final long m_lngWidth = 320;
    private final long m_lngHeight = 224;
    
    /** First video frame number. */
    private long m_lngStartFrame = -1;
    /** Last video frame number. */
    private long m_lngEndFrame = -1;
    
    // audio
    /** 0 if it doesn't have audio, 1 for mono, 2 for stereo */
    private final int m_iAudioChannels = 2;
    
    private final int m_iAudioPeriod = 16;
    private final int m_iAudioSampleRate = 18900;
    private final int m_iAudioBitsPerSample = 4;
    
    /** 0 if it doesn't have audio, >0 if it does */
    private long m_lngAudioTotalSamples = 0;
    
    private static final int[] FPS_LOOKUP = {
        /* 60   30   20   15 */       
        /* F    F    F    F  */   0, //n/a
        /* F    F    F    T  */   15,
        /* F    F    T    F  */   20,
        /* F    F    T    T  */   60,
        /* F    T    F    F  */   30,
        /* F    T    F    T  */   30,
        /* F    T    T    F  */   60,
        /* F    T    T    T  */   60,
        /* T    F    F    F  */   60,
        /* T    F    F    T  */   60,
        /* T    F    T    F  */   60,
        /* T    F    T    T  */   60,
        /* T    T    F    F  */   60,
        /* T    T    F    T  */   60,
        /* T    T    T    F  */   60,
        /* T    T    T    T  */   60,
    };
    
    // possible frame durations:
    // 14, 15 -> 10 fps
    // 9, 10  -> 15 fps
    // 4, 5   -> 30 fps
    // 18, 19 -> 7.5 fps
    
    private int m_iFpsX2 = -1;
        
    public PSXMediaAlice(PSXSectorRangeIterator oSectIterator) throws NotThisTypeException, IOException
    {
        super(oSectIterator.getSourceCD());
        
        AudioChannelInfo oAudInf = null;
        
        PSXSector oPsxSect = oSectIterator.peekNext();
        
        if (!(oPsxSect instanceof PSXSectorAliceFrameChunk))
            throw new NotThisTypeException();
        
        PSXSectorAliceFrameChunk oFrame;
        PSXSectorAudioChunk oAudio;
        
        oFrame = (PSXSectorAliceFrameChunk) oPsxSect;

        long lngCurFrame = oFrame.getFrameNumber();

        // FIXME: !! Because Alice movies begin with an audio sector, we have to adjust the
        // start sector. Unfortunately we have a residual XA media item we're stuck
        // with for now.
        super.m_iStartSector = oPsxSect.getSector() - 1;
        super.m_iEndSector = m_iStartSector;

        m_lngStartFrame = oFrame.getFrameNumber();
        m_lngEndFrame = oFrame.getFrameNumber();
        
        if (DebugVerbose > 2)
            System.err.println(oPsxSect.toString());
        
        
        boolean m_bln10fps = false;
        boolean m_bln15fps = false;
        boolean m_bln30fps = false;
        boolean m_bln7_5fps = false;
        
        oSectIterator.skipNext();
        while (oSectIterator.hasNext()) {
            oPsxSect = oSectIterator.peekNext();
            
            if (oPsxSect instanceof PSXSectorAudio2048) {
                // just skip it
            } 
            else if ((oPsxSect instanceof PSXSectorAliceFrameChunk)) 
            {
                        
                oFrame = (PSXSectorAliceFrameChunk) oPsxSect;
                if (oFrame.getWidth() == m_lngWidth &&
                        oFrame.getHeight() == m_lngHeight &&
                        (oFrame.getFrameNumber() == lngCurFrame ||
                        oFrame.getFrameNumber() == lngCurFrame+1)) {

                    lngCurFrame = oFrame.getFrameNumber();
                    
                    if (oFrame.getChunksInFrame() == 4 ||
                        oFrame.getChunksInFrame() == 5)
                        m_bln30fps = true;
                    else if (oFrame.getChunksInFrame() == 9 ||
                        oFrame.getChunksInFrame() == 10)
                        m_bln15fps = true;
                    else if (oFrame.getChunksInFrame() == 14 ||
                        oFrame.getChunksInFrame() == 15)
                        m_bln10fps = true;
                    else if (oFrame.getChunksInFrame() == 18 ||
                        oFrame.getChunksInFrame() == 19)
                        m_bln7_5fps = true;
                    
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
            
            
            oSectIterator.skipNext();
        } // while
        
        int iLookupIdx = 
            (m_bln30fps  ? 8 : 0) |    
            (m_bln15fps  ? 4 : 0) |    
            (m_bln10fps  ? 2 : 0) |    
            (m_bln7_5fps ? 1 : 0);
        
        m_iFpsX2 = FPS_LOOKUP[iLookupIdx];
        
        m_aoFps[0] = new FramesPerSecond(m_iFpsX2, 2, 150);
    }

    public PSXMediaAlice(CDSectorReader oCD, String sSerial) 
            throws NotThisTypeException 
    {
        super(oCD, sSerial, "ALICE");
        
        try {
            IndexLineParser parse = new IndexLineParser(
                    "$| Frames #-# Total Samples # FPSx2 #", sSerial);
            
            parse.skip();
            m_lngStartFrame  = parse.get(m_lngStartFrame);
            m_lngEndFrame    = parse.get(m_lngEndFrame);
            m_lngAudioTotalSamples = parse.get(m_lngAudioTotalSamples);
            m_iFpsX2         = parse.get(m_iFpsX2);
            
            m_aoFps[0] = new FramesPerSecond(m_iFpsX2, 2, 150);
            
        } catch (NumberFormatException ex) {
            throw new NotThisTypeException();
        } catch (IllegalArgumentException ex) {
            throw new NotThisTypeException();
        } catch (NoSuchElementException ex) {
            throw new NotThisTypeException();
        }
        
    }

    @Override
    public String toString() {
        String s = super.toString("ALICE") +
                String.format(
                "| Frames %d-%d Total Samples %d FPSx2 %d",
                m_lngStartFrame, m_lngEndFrame, 
                m_lngAudioTotalSamples, m_iFpsX2);
        
        return s;
    }
    
    
    @Override
    public boolean hasVideo() {
        return true;
    }

    @Override
    public int getAudioChannels() {
        return m_iAudioChannels;
    }

    private final FramesPerSecond[] m_aoFps = new FramesPerSecond[1];
    @Override
    public FramesPerSecond[] getPossibleFPS() {
        return m_aoFps;
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

    @Override
    public int getSamplesPerSecond() {
        return m_iAudioSampleRate;
    }

    ////////////////////////////////////////////////////////////////////////////
    
    private ADPCMDecodingContext m_oLeftContext;
    private ADPCMDecodingContext m_oRightContext;
    
    @Override
    protected void startPlay() {
        m_oLeftContext = new ADPCMDecodingContext(1.0);
        m_oRightContext = new ADPCMDecodingContext(1.0);
    }

    @Override
    protected void playSector(PSXSector oPSXSect) throws StopPlayingException, IOException {
        if (oPSXSect instanceof PSXSectorAliceFrameChunk) {
            
            playVideoSector((PSXSectorAliceFrameChunk)oPSXSect);
            
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
    protected void playVideoSector(IVideoChunkSector oFrmChk) throws StopPlayingException, IOException {
        // only process the frame if there is a listener for it
        if (m_oVidDemuxListener != null) {

            if (m_oFrameDemuxer == null) {
                m_oFrameDemuxer = new StrFramePushDemuxer();
                m_oFrameDemuxer.addChunk(oFrmChk);
            } else if (oFrmChk.getFrameNumber() != m_oFrameDemuxer.getFrameNumber()) {
                // if it's another frame (should be the next frame).

                // This is necessary if the movie is corrupted and somehow
                // is missing some chunks of the prior frame.

                // Save the demuxer and clear it from the class
                StrFramePushDemuxer oOldDemux = m_oFrameDemuxer;
                m_oFrameDemuxer = null;

                // create a new demuxer for the new frame sector
                StrFramePushDemuxer oNewDemux = new StrFramePushDemuxer();
                oNewDemux.addChunk(oFrmChk);

                // pass the completed frame along
                try {
                    PlayDups(oOldDemux);
                    
                } catch (StopPlayingException ex) {
                    if (m_oFrameDemuxer != null) {
                        try {
                            PlayDups(m_oFrameDemuxer);
                        } finally {
                            m_oFrameDemuxer = null;
                        }
                    }
                    throw ex; // continue up the stop-playing chain
                }

                // only if there isn't an error, or a Stop,
                // do we want to save the new frame
                m_oFrameDemuxer = oNewDemux;
            } else {
                m_oFrameDemuxer.addChunk(oFrmChk);
            }


        }
    }
    
    private void PlayDups(StrFramePushDemuxer oDemux) throws StopPlayingException, IOException {
        int iLocalFpsX2;
        if (oDemux.getChunksInFrame() == 4 ||
            oDemux.getChunksInFrame() == 5)
            iLocalFpsX2 = 30*2;
        else if (oDemux.getChunksInFrame() == 9 ||
            oDemux.getChunksInFrame() == 10)
            iLocalFpsX2 = 15*2;
        else if (oDemux.getChunksInFrame() == 14 ||
            oDemux.getChunksInFrame() == 15)
            iLocalFpsX2 = 10*2;
        else /*if (oDemux.getChunksInFrame() == 18 ||
            oDemux.getChunksInFrame() == 19)*/
            iLocalFpsX2 = 15/*7.5*2*/;
        
        for (int i = 0; i < m_iFpsX2; i += iLocalFpsX2)
            m_oVidDemuxListener.event(oDemux);
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
        // jump to the sector
        // hopefully land near the frame
        int iSect = (int)
                ( (super.m_iEndSector - super.m_iStartSector) * (percent) ) 
                + super.m_iStartSector;
        if (iSect < super.m_iStartSector) iSect = super.m_iStartSector;
        
        super.m_oCDIter.gotoIndex(iSect);
        
        // now seek ahead until we read the desired frame
        CDXASector oCDSect = super.m_oCDIter.peekNext();
        PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        while (!(oPsxSect instanceof PSXSectorAliceFrameChunk) ||
               ((PSXSectorAliceFrameChunk)oPsxSect).getFrameNumber() < lngFrame) 
        {
            super.m_oCDIter.skipNext();
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }
        
        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(oPsxSect instanceof PSXSectorAliceFrameChunk) ||
               ((PSXSectorAliceFrameChunk)oPsxSect).getFrameNumber() > lngFrame ||
               ((PSXSectorAliceFrameChunk)oPsxSect).getChunkNumber() > 0)
        {
            super.m_oCDIter.gotoIndex(m_oCDIter.getIndex() - 1);
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }
        
    }

    
}
