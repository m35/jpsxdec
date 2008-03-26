package jpsxdec.media;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import jpsxdec.audiodecoding.ADPCMDecodingContext;
import jpsxdec.audiodecoding.SquareADPCMDecoder;
import jpsxdec.cdreaders.CDSectorReader;
import jpsxdec.cdreaders.CDXASector;
import jpsxdec.media.savers.StopPlayingException;
import jpsxdec.sectortypes.ISquareAudioSector;
import jpsxdec.sectortypes.IVideoChunkSector;
import jpsxdec.sectortypes.PSXSector;
import jpsxdec.audiodecoding.Short2dArrayInputStream;
import jpsxdec.util.NotThisTypeException;

public abstract class PSXMediaSquare extends PSXMediaStreaming {

    public PSXMediaSquare(CDSectorReader oCD, String sSerial, String sType) 
            throws NotThisTypeException 
    {
        super(oCD, sSerial, sType);
    }

    public PSXMediaSquare(CDSectorReader oCD) {
        super(oCD);
    }

    @Override
    public long getWidth() {
        return 320;
    }

    @Override
    public long getHeight() {
        return 224;
    }

    ISquareAudioSector m_oLeftAudioChunk;
    private ADPCMDecodingContext m_oLeftContext;
    private ADPCMDecodingContext m_oRightContext;

    @Override
    final protected void startPlay() {
        m_oLeftContext = new ADPCMDecodingContext(1.0);
        m_oRightContext = new ADPCMDecodingContext(1.0);
    }


    @Override
    final protected void playSector(PSXSector oPSXSect) throws StopPlayingException, IOException {

        if (oPSXSect instanceof IVideoChunkSector) {

            super.playVideoSector((IVideoChunkSector)oPSXSect);

        } else if (oPSXSect instanceof ISquareAudioSector) {
            playSquareAudio((ISquareAudioSector)oPSXSect, oPSXSect.getSector());
        }

    }

    final protected void playSquareAudio(ISquareAudioSector oAudChk, int iSectorNum) 
            throws IOException, StopPlayingException 
    {
        // only process the audio if there is a listener for it
        if (m_oAudioListener != null) {

            if (m_oLeftAudioChunk == null) {
                m_oLeftAudioChunk = oAudChk;
            } else {
                short[][] asi = new short[2][];
                asi[0] = SquareADPCMDecoder.DecodeMore(
                        m_oLeftAudioChunk.getUserDataStream(), m_oLeftContext, m_oLeftAudioChunk.getAudioDataSize());
                asi[1] = SquareADPCMDecoder.DecodeMore(
                        oAudChk.getUserDataStream(), m_oRightContext, oAudChk.getAudioDataSize());

                AudioInputStream ais = new AudioInputStream(
                        new Short2dArrayInputStream(asi), 
                        new AudioFormat(
                            oAudChk.getSamplesPerSecond(),
                            16,        
                            2, 
                            true,      
                            false
                        ),
                        AudioSystem.NOT_SPECIFIED);

                m_oAudioListener.event(ais, iSectorNum);
                m_oLeftAudioChunk = null;
            }
        }

    }

    @Override
    final protected void endPlay() throws IOException {
        m_oLeftAudioChunk = null;
        m_oLeftContext = null;
        m_oRightContext = null;
        super.endPlay();
    }

    @Override
    final public void seek(long lngFrame) throws IOException {
        // clamp the desired frame
        if (lngFrame < getStartFrame()) 
            lngFrame = (int)getStartFrame();
        else if (lngFrame > getEndFrame()) 
            lngFrame = (int)getEndFrame();
        // calculate an estimate where the frame will land
        double percent = (lngFrame - getStartFrame()) / (double)(getEndFrame() - getStartFrame());
        // backup 10% of the size of the media to 
        // hopefully land shortly before the frame
        int iSect = (int)
                ( (super.m_iEndSector - super.m_iStartSector) * (percent-0.1) ) 
                + super.m_iStartSector;
        if (iSect < super.m_iStartSector) iSect = super.m_iStartSector;

        super.m_oCDIter.gotoIndex(iSect);

        // now seek ahead until we reach the desired frame
        CDXASector oCDSect = super.m_oCDIter.peekNext();
        PSXSector oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        while (!(oPsxSect instanceof ISquareAudioSector) ||
               ((ISquareAudioSector)oPsxSect).getFrameNumber() < lngFrame) 
        {
            super.m_oCDIter.skipNext();
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }

        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(oPsxSect instanceof ISquareAudioSector) ||
               ((ISquareAudioSector)oPsxSect).getFrameNumber() > lngFrame ||
               ((ISquareAudioSector)oPsxSect).getAudioChunkNumber() > 0)
        {
            super.m_oCDIter.gotoIndex(m_oCDIter.getIndex() - 1);
            oCDSect = super.m_oCDIter.peekNext();
            oPsxSect = PSXSector.SectorIdentifyFactory(oCDSect);
        }

    }
}
