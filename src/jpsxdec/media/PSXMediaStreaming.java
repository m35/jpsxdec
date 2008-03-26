package jpsxdec.media;

import java.util.*;
import java.io.*;
import javax.sound.sampled.AudioInputStream;
import jpsxdec.*;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXAIterator;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.demuxers.StrFramePushDemuxerIS;
import jpsxdec.mdec.MDEC;
import jpsxdec.mdec.PsxYuv;
import jpsxdec.media.StrFpsCalc.FramesPerSecond;
import jpsxdec.media.savers.StopPlayingException;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.sectortypes.PSXSector.*;
import jpsxdec.uncompressors.StrFrameUncompressorIS;


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
            Throwable eex = ex.getCause();
            if (eex instanceof IOException)
                throw (IOException)eex;
        } finally {
            m_blnPlaying = false;
            endPlay();
        }
    }

    public void Stop() {
        m_blnPlaying = false;
    }


    protected void endPlay() throws IOException {
        if (m_oFrameDemuxer != null) {
            try {
                PlayDemuxFrame(m_oFrameDemuxer);
            } catch (StopPlayingException ex) {

            } finally {
                m_oFrameDemuxer = null;
            }
        }
    }        


    private boolean m_blnPlaying = false;
    private StrFramePushDemuxerIS m_oFrameDemuxer;
    protected void playVideoSector(IVideoChunkSector oFrmChk) throws StopPlayingException, IOException {
        // only process the frame if there is a listener for it
        if (m_oVidDemuxListener != null || m_oMdecListener != null || m_oFrameListener != null) {

            if (m_oFrameDemuxer == null) {
                m_oFrameDemuxer = new StrFramePushDemuxerIS();
                m_oFrameDemuxer.addChunk(oFrmChk);
            } else if (oFrmChk.getFrameNumber() != m_oFrameDemuxer.getFrameNumber()) {
                // if it's another frame (should be the next frame).

                // This is necessary if the movie is corrupted and somehow
                // is missing some chunks of the prior frame.

                // Save the demuxer and clear it from the class
                StrFramePushDemuxerIS oOldDemux = m_oFrameDemuxer;
                m_oFrameDemuxer = null;

                // create a new demuxer for the new frame sector
                StrFramePushDemuxerIS oNewDemux = new StrFramePushDemuxerIS();
                oNewDemux.addChunk(oFrmChk);

                // pass the completed frame along
                try {
                    PlayDemuxFrame(oOldDemux);

                } catch (StopPlayingException ex) {
                    if (m_oFrameDemuxer != null) {
                        try {
                            PlayDemuxFrame(m_oFrameDemuxer);
                        } finally {
                            m_oFrameDemuxer = null;
                        }
                    }
                    throw ex;
                }

                // only if there isn't an error, or a Stop,
                // do we want to save the new frame
                m_oFrameDemuxer = oNewDemux;
            } else {
                m_oFrameDemuxer.addChunk(oFrmChk);
            }


        }
    }


    /** After the last sector of a frame has been read, 
     *  this is called to then process it. */
    final protected void PlayDemuxFrame(StrFramePushDemuxerIS oDemux) 
            throws IOException, StopPlayingException
    {

        // send out the demux event if there is listener
        if (m_oVidDemuxListener != null) {
            m_oVidDemuxListener.event(oDemux.getStream(), oDemux.getFrameNumber());
        }

        // continue only if listener
        if (m_oMdecListener != null || m_oFrameListener != null) {
            StrFrameUncompressorIS oUncomprs;
            try {
                oUncomprs = new StrFrameUncompressorIS(oDemux.getStream(), oDemux.getWidth(), oDemux.getHeight());
            } catch (IOException ex) {
                if (m_oErrListener != null)
                    m_oErrListener.error(ex);
                return;
            }
            // send mdec event if listener
            if (m_oMdecListener != null) {
                m_oMdecListener.event(oUncomprs.getStream(), oDemux.getFrameNumber());
            }

            // finally decode frame and event if listener
            if (m_oFrameListener != null) { 
                
                PsxYuv yuv = MDEC.DecodeFrame(oUncomprs.getStream(), oUncomprs.getWidth(), oUncomprs.getHeight());

                if (yuv.getDecodingError() != null && m_oErrListener != null)
                    m_oErrListener.error(yuv.getDecodingError());

                m_oFrameListener.event(yuv, oDemux.getFrameNumber());
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
    public static interface IVidListener {
        void event(InputStream is, long frame) throws StopPlayingException, IOException;
    }
    public static interface IAudListener {
        void event(AudioInputStream is, int sector) throws StopPlayingException, IOException;
    }
    public static interface IFrameListener {
        void event(PsxYuv o, long frame) throws StopPlayingException, IOException ;
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

    protected IVidListener m_oVidDemuxListener;
    public void addVidDemuxListener(IVidListener oListen) {
        m_oVidDemuxListener = oListen;
    }

    protected IVidListener m_oMdecListener;
    public void addMdecListener(IVidListener o) {
        m_oMdecListener = o;
    }
    
    protected IFrameListener m_oFrameListener;
    public void addFrameListener(IFrameListener o) {
        m_oFrameListener = o;
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
        m_oFrameListener = null;
        m_oMdecListener = null;
    }
}
