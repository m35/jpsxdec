/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

package jpsxdec.modules.player;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.audio.ISectorClaimToDecodedAudio;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.save.AutowireVDP;
import jpsxdec.modules.video.save.Frame2BitstreamOrMdec;
import jpsxdec.modules.video.save.VDP;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.SimpleIDCT;
import jpsxdec.util.Fraction;
import jpsxdec.util.player.IMediaDataReadProcessor;
import jpsxdec.util.player.MediaDataWriter;
import jpsxdec.util.player.PlayController;
import jpsxdec.util.player.StopPlayingException;

/** Holds all the class implementations that the {@link jpsxdec.util.player}
 * framework needs to playback PlayStation audio and/or video. */
public class MediaPlayer {

    @Nonnull
    private final PlayController _playController;

    // audio only
    public MediaPlayer(@Nonnull DiscItemSectorBasedAudioStream audio, @Nonnull DiscSpeed discSpeed) {

        MediaProcessor mediaProcessor = new MediaProcessor(null, null, audio, discSpeed);

        audio.makeSectorClaimToDecodedAudio(1.0).getOutputFormat();

        _playController = new PlayController(mediaProcessor, mediaProcessor._audioDecoder.getOutputFormat());

    }

    //----------------------------------------------------------

    public MediaPlayer(@Nonnull DiscItemVideoStream video,
                       @Nonnull ISectorClaimToFrameAndAudio sectorClaimToFrameAndAudio,
                       @Nonnull DiscSpeed discSpeed)
    {
        MediaProcessor mediaProcessor = new MediaProcessor(video, sectorClaimToFrameAndAudio, null, discSpeed);

        if (sectorClaimToFrameAndAudio.hasAudio()) {
            _playController = new PlayController(mediaProcessor, video.getWidth(), video.getHeight(), sectorClaimToFrameAndAudio.getOutputFormat());
        } else {
            _playController = new PlayController(mediaProcessor, video.getWidth(), video.getHeight());
        }
    }

    public @Nonnull PlayController getPlayController() {
        return _playController;
    }

    private static class MediaProcessor extends Frame2BitstreamOrMdec implements IMediaDataReadProcessor<IDemuxedFrame>, VDP.IDecodedListener {

        private final AutowireVDP _autowireVDP = new AutowireVDP();
        private final int _iMovieStartSector;
        @Nonnull
        private final DiscSpeed _discSpeed;
        @CheckForNull
        private final ISectorClaimToDecodedAudio _audioDecoder;

        private MediaDataWriter<IDemuxedFrame> _dataWriter;
        private int[] _aiDrawRgb24Here;
        private int _iWidth, _iHeight;

        public MediaProcessor(@CheckForNull DiscItemVideoStream video,
                              @CheckForNull ISectorClaimToFrameAndAudio sectorClaimToFrameAndAudio,
                              @CheckForNull DiscItemSectorBasedAudioStream audio,
                              @Nonnull DiscSpeed discSpeed)
        {
            super(FrameNumber.Type.Index, DebugLogger.Log);
            _discSpeed = discSpeed;
            int iMovieEndSector;
            ICdSectorReader cdReader;
            if (video != null) {
                if (sectorClaimToFrameAndAudio == null)
                    throw new IllegalArgumentException();

                _iWidth = video.getWidth();
                _iHeight = video.getHeight();

                cdReader = video.getSourceCd();
                _iMovieStartSector = sectorClaimToFrameAndAudio.getStartSector();
                iMovieEndSector = sectorClaimToFrameAndAudio.getEndSector();
                _autowireVDP.setSectorClaim2FrameAndAudio(sectorClaimToFrameAndAudio);
                _autowireVDP.setFrame2BitstreamOrMdec(this); // this will intercept the pipeline and queue the frame into the processing thread
                _autowireVDP.setBitstream2Mdec(new VDP.Bitstream2Mdec());
                _autowireVDP.setMdec2Decoded(new VDP.Mdec2Decoded(new MdecDecoder_int(new SimpleIDCT(), video.getWidth(), video.getHeight()), DebugLogger.Log));
                _autowireVDP.setDecodedListener(this);

                if (sectorClaimToFrameAndAudio.hasAudio()) {
                    _audioDecoder = sectorClaimToFrameAndAudio;
                } else {
                    _audioDecoder = null;
                }
            } else if (audio != null) {
                cdReader = audio.getSourceCd();
                _iMovieStartSector = //audio.getStartSector();
                audio.getPresentationStartSector(); // use this instead? then audio won't start out of sync
                iMovieEndSector = audio.getEndSector();
                _audioDecoder = audio.makeSectorClaimToDecodedAudio(1.0);
                _autowireVDP.setSectorClaim2DecodedAudio(_audioDecoder);

            } else {
                throw new IllegalArgumentException();
            }

            SectorClaimSystem it = SectorClaimSystem.create(cdReader, _iMovieStartSector, iMovieEndSector);
            _autowireVDP.setSectorClaimSystem(it);
        }

        // -------------------------------------------------------------------------------

        @Override
        public void readerThread(@Nonnull MediaDataWriter<IDemuxedFrame> dataWriter) throws StopPlayingException {
            _dataWriter = dataWriter;
            try {
                if (_audioDecoder != null) {
                    AudioPlayerSectorTimedWriter audioWriter = new AudioPlayerSectorTimedWriter(dataWriter.getAudioPlayerOutputStream(),
                                                                                                _iMovieStartSector,
                                                                                                _discSpeed,
                                                                                                _audioDecoder.getSampleFramesPerSecond());
                    _autowireVDP.setAudioPacketListener(audioWriter);
                }
                _autowireVDP.autowire();

                SectorClaimSystem scs = _autowireVDP.getSectorClaimSystem();

                IIdentifiedSector identifiedSector;
                for (int iSector = 0; scs.hasNext() && !dataWriter.isClosed(); iSector++) {
                    identifiedSector = scs.next(DebugLogger.Log);
                }
                scs.flush(DebugLogger.Log);
            } catch (WrapException ex) {
                if (ex.getCause() instanceof StopPlayingException)
                    throw (StopPlayingException)ex.getCause();
                else
                    throw new StopPlayingException(ex.getCause());
            } catch (CdException.Read | LoggedFailure ex) {
                throw new StopPlayingException(ex);
            }
        }

        @Override
        public void frameComplete(@Nonnull IDemuxedFrame frame) {

            long lngPresentationNanos = (long) ((frame.getPresentationSector().asDouble() - _iMovieStartSector) /
                                                 _discSpeed.getSectorsPerSecond() * 1000000000.);
            try {
                // instead of immediately sending the frame through the pipeline, queue it up in the processing thread
                _dataWriter.writeFrame(frame, lngPresentationNanos);
            } catch (StopPlayingException ex) {
                throw new WrapException(ex);
            }
        }

        // -------------------------------------------------------------------------------

        @Override
        public void processFrameThread(@Nonnull IDemuxedFrame frame, @Nonnull int[] aiDrawRgb24Here) {
            // ideally we would have a buffer in this class where the decoded frame is written, then copy that into aiDrawRgb24Here
            // but we don't want an extra copy, so use this workaround
            _aiDrawRgb24Here = aiDrawRgb24Here;
            try {
                // now in the processing thread, continue the frame through the pipeline
                super.frameComplete(frame); // this will eventually call decoded() which will write to the screen
            } catch (LoggedFailure ex) {
                System.err.println("Frame "+frame.getFrame()+" "+ex.getMessage());
            } finally {
                _aiDrawRgb24Here = null;
            }
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, FormattedFrameNumber _ignoredFN, Fraction _ignoredPS) {
            decoder.readDecodedRgb(_iWidth, _iHeight, _aiDrawRgb24Here);
        }

        @Override
        public void error(@Nonnull ILocalizedMessage errMsg, FormattedFrameNumber _ignoredFN, Fraction _ignoredPS) {
            System.err.println(errMsg.getEnglishMessage());
        }

        @Override
        public void assertAcceptsDecoded(MdecDecoder decoder) {}
    }

}
