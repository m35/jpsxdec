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
 * PSXMediaStreaming.java
 */

package jpsxdec.media;

import java.util.*;
import java.io.*;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXAIterator;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.demuxers.StrFramePushDemuxer;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;


public abstract class PSXMediaStreaming extends PSXMedia {
    CDXAIterator m_oCDIter;

    public PSXMediaStreaming(CDSectorReader oCD) {
        super(oCD);
        Reset();
    }

    public PSXMediaStreaming(CDSectorReader oCD, String sSerial, String sType) 
            throws NotThisTypeException
    {
        super(oCD, sSerial, sType);
        Reset();
    }


    public abstract void seek(long lngFrame) throws IOException;
    public abstract boolean hasVideo();
    public boolean hasAudio() {
        return getAudioChannels() > 0;
    }
    public abstract int getAudioChannels();

    public abstract FramesPerSecond[] getPossibleFPS();

    public abstract long getStartFrame();
    public abstract long getEndFrame();

    public abstract long getWidth();
    public abstract long getHeight();
    
    /** Rounds the width to the next multiple of 16. */
    public long getActualWidth() {
        return (getWidth() + 15) & 0xfffffffffffffff0L;
    }

    /** Rounds the height to the next multiple of 16. */
    public long getActualHeight() {
        return (getHeight() + 15) & 0xfffffffffffffff0L;
    }
    
    public abstract int getSamplesPerSecond();

    protected abstract void startPlay();
    
    protected abstract void playSector(PSXSector oPSXSect) throws StopPlayingException, IOException;

    
    public void Reset() {
        m_oCDIter = new CDXAIterator(m_oCD, super.m_iStartSector, super.m_iEndSector);
    }

    public void Play() throws IOException {
        m_blnPlaying = true;
        startPlay();

        try {
            while (m_oCDIter.hasNext() && m_blnPlaying) {
                CDXASector oSect = m_oCDIter.next();
                if (m_oRawRead != null) m_oRawRead.event(oSect);

                PSXSector oPSXSect = PSXSector.SectorIdentifyFactory(oSect);

                if (oPSXSect == null) continue;

                playSector(oPSXSect);
            }
        } catch (StopPlayingException ex) {
            
        } finally {
            m_blnPlaying = false;
            endPlay();
        }
    }

    public void Stop() {
        m_blnPlaying = false;
    }


    private boolean m_blnPlaying = false;
    private StrFramePushDemuxer m_oFrameDemuxer;
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
                    m_oVidDemuxListener.event(oOldDemux);
                } catch (StopPlayingException ex) {
                    if (m_oFrameDemuxer != null) {
                        try {
                            m_oVidDemuxListener.event(m_oFrameDemuxer);
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

    protected void endPlay() throws IOException {
        if (m_oFrameDemuxer != null && m_oVidDemuxListener != null) {
            try {
                // send out the demux event if there is listener
                m_oVidDemuxListener.event(m_oFrameDemuxer);
            } catch (StopPlayingException ex) {

            } finally {
                m_oFrameDemuxer = null;
            }
        }
    }        

    ////////////////////////////////////////////////////////////////////////
    public static interface IErrorListener {
        void error(Exception ex) throws StopPlayingException;
    }
    public static interface IRawListener {
        void event(CDXASector oSect) throws StopPlayingException, IOException;
    }
    public static interface IDemuxListener {
        void event(StrFramePushDemuxer oDemux) throws StopPlayingException, IOException;
    }
    public static interface IAudListener {
        void event(AudioInputStream is, int sector) throws StopPlayingException, IOException;
    }
    ////////////////////////////////////////////////////////////////////////
    
    private IRawListener m_oRawRead;
    public void addRawListener(IRawListener oListen) {
        m_oRawRead = oListen;
    }

    protected IErrorListener m_oErrListener;
    public void addErrorListener(IErrorListener oListen) {
        m_oErrListener = oListen;
    }

    protected IDemuxListener m_oVidDemuxListener;
    public void addVidDemuxListener(IDemuxListener oListen) {
        m_oVidDemuxListener = oListen;
    }

    protected IAudListener m_oAudioListener;
    public void addAudioListener(IAudListener o) {
        m_oAudioListener = o;
    }

    public void clearListeners() {
        m_oRawRead = null;
        m_oErrListener = null;
        m_oVidDemuxListener = null;
        m_oAudioListener = null;
    }
}
