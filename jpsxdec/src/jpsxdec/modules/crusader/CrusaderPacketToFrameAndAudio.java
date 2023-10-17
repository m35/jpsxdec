/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.modules.crusader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.adpcm.SpuAdpcmDecoder;
import jpsxdec.adpcm.SpuAdpcmSoundUnit;
import jpsxdec.i18n.I;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.modules.audio.DecodedAudioPacket;
import jpsxdec.util.Fraction;

/** Listens for Crusader packets and sends frames to a frame listener
 * and holds an audio decoding context which decodes audio and sends it
 * to audio packet listeners. */
public class CrusaderPacketToFrameAndAudio implements CrusaderSectorToCrusaderPacket.PacketListener {

    public interface FrameListener {
        void frameComplete(@Nonnull DemuxedCrusaderFrame frame, @Nonnull ILocalizedLogger log)
                throws LoggedFailure;
        void videoEnd(@Nonnull ILocalizedLogger log);
    }

    @Nonnull
    private final SpuAdpcmDecoder.Stereo _audDecoder;
    @CheckForNull
    private DecodedAudioPacket.Listener _audioListener;
    @CheckForNull
    private FrameListener _frameListener;
    private final int _iAbsoluteInitialFramePresentationSector;


    public CrusaderPacketToFrameAndAudio(double dblVolume, int iAbsoluteInitialFramePresentationSector)
    {
        _audDecoder = new SpuAdpcmDecoder.Stereo(dblVolume);
        _iAbsoluteInitialFramePresentationSector = iAbsoluteInitialFramePresentationSector;
    }
    public CrusaderPacketToFrameAndAudio(double dblVolume,
                                         int iAbsoluteInitialFramePresentationSector,
                                         @Nonnull DecodedAudioPacket.Listener audioListener)
    {
        this(dblVolume, iAbsoluteInitialFramePresentationSector);
        _audioListener = audioListener;
    }
    public CrusaderPacketToFrameAndAudio(double dblVolume,
                                         int iAbsoluteInitialFramePresentationSector,
                                         @Nonnull FrameListener frameListener)
    {
        this(dblVolume, iAbsoluteInitialFramePresentationSector);
        _frameListener = frameListener;
    }
    public CrusaderPacketToFrameAndAudio(double dblVolume,
                                         int iInitialPresentationSector,
                                         @Nonnull DecodedAudioPacket.Listener audioListener,
                                         @Nonnull FrameListener frameListener)
    {
        this(dblVolume, iInitialPresentationSector);
        _audioListener = audioListener;
        _frameListener = frameListener;
    }
    public void setFrameListener(@CheckForNull FrameListener frameListener) {
        _frameListener = frameListener;
    }
    public void setAudioListener(@CheckForNull DecodedAudioPacket.Listener audioListener) {
        _audioListener = audioListener;
    }

    @Override
    public void packetComplete(CrusaderPacket packet, ILocalizedLogger log) throws LoggedFailure {
        if (packet instanceof CrusaderPacket.Video)
            frame((CrusaderPacket.Video) packet, log);
        else if (packet instanceof CrusaderPacket.Audio)
            audio((CrusaderPacket.Audio) packet, log);
    }

    @Override
    public void endOfVideo(ILocalizedLogger log) {
        if (_frameListener != null)
            _frameListener.videoEnd(log);
    }

    private void frame(@Nonnull CrusaderPacket.Video videoPacket,
                       @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        int iPresentationSector = videoPacket.getFrameNumber() * CrusaderPacket.SECTORS_PER_FRAME + _iAbsoluteInitialFramePresentationSector;

        if (_frameListener != null) {
            DemuxedCrusaderFrame frame = new DemuxedCrusaderFrame(videoPacket,
                                                                  iPresentationSector);
            _frameListener.frameComplete(frame, log);
        }
    }

    private void audio(@Nonnull CrusaderPacket.Audio audioPacket,
                       @Nonnull ILocalizedLogger log)
            throws LoggedFailure
    {
        // .. copy the audio data out of the sectors ...............
        byte[] abAudioDemuxBuffer = audioPacket.copyPayload();

        // .. decode the audio data .............................
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        int iChannelSize = abAudioDemuxBuffer.length / 2;
        int iSoundUnitsPerChannel = iChannelSize / SpuAdpcmSoundUnit.SIZEOF_SOUND_UNIT;
        try {
            _audDecoder.decode(new ByteArrayInputStream(abAudioDemuxBuffer, 0, iChannelSize),
                               new ByteArrayInputStream(abAudioDemuxBuffer, iChannelSize, iChannelSize),
                               iSoundUnitsPerChannel, audioBuffer);
        } catch (IOException ex) {
            throw new RuntimeException("ByteArrayInputStream shouldn't throw this", ex);
        }
        if (_audDecoder.hadCorruption())
            log.log(Level.WARNING, I.SPU_ADPCM_CORRUPTED(audioPacket.getStartSector(), _audDecoder.getSampleFramesWritten()));

        if (_audioListener != null) {
            Fraction presentationSector = new Fraction(audioPacket.getPresentationSampleFrame(), CrusaderPacket.SAMPLE_FRAMES_PER_SECTOR)
                                                  .add(_iAbsoluteInitialFramePresentationSector);
            DecodedAudioPacket packet = new DecodedAudioPacket(-1, CrusaderPacket.CRUSADER_AUDIO_FORMAT,
                                                               audioBuffer.toByteArray(),
                                                               presentationSector);
            _audioListener.audioPacketComplete(packet, log);
        }


    }

}
