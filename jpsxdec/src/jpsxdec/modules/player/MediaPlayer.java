/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.cdreaders.CdReadException;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.modules.video.save.AutowireVDP;
import jpsxdec.modules.video.save.Frame2Bitstream;
import jpsxdec.modules.video.save.VDP;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.SimpleIDCT;
import jpsxdec.util.Fraction;
import jpsxdec.util.player.IFrameProcessor;
import jpsxdec.util.player.IMediaDataReader;
import jpsxdec.util.player.IPreprocessedFrameWriter;
import jpsxdec.util.player.PlayController;
import jpsxdec.util.player.StopPlayingException;

/** Holds all the class implementations that the {@link jpsxdec.util.player}
 * framework needs to playback PlayStation audio and/or video. */
public class MediaPlayer implements IMediaDataReader {

    private final int _iMovieStartSector;
    private final int _iMovieEndSector;
    @Nonnull
    private final ICdSectorReader _cdReader;

    @Nonnull
    private final PlayController _controller;

    private final AutowireVDP _demuxAutowire = new AutowireVDP();
    private final AutowireVDP _decodeAutowire = new AutowireVDP();

    //----------------------------------------------------------

    private final int _iSectorsPerSecond;

    public MediaPlayer(@Nonnull DiscItemVideoStream vid, @Nonnull ISectorClaimToDemuxedFrame demuxer, int iSectorPerSecond) {
        this(vid, demuxer, vid.getStartSector(), vid.getEndSector(), iSectorPerSecond);
    }


    public MediaPlayer(@Nonnull DiscItemVideoStream vid, @Nonnull ISectorClaimToDemuxedFrame demuxer,
                       int iSectorStart, int iSectorEnd, int iSectorPerSecond)
    {
        this(vid, demuxer, null, iSectorStart, iSectorEnd, iSectorPerSecond);
    }

    //-----------------------------------------------------------------------

    public MediaPlayer(@Nonnull DiscItemSectorBasedAudioStream aud, int iSectorPerSecond) {
        _cdReader = aud.getSourceCd();
        _iMovieStartSector = aud.getStartSector();
        _iMovieEndSector = aud.getEndSector();
        _iSectorsPerSecond = iSectorPerSecond;

        ISectorAudioDecoder audioDecoder = aud.makeDecoder(1.0);
        _demuxAutowire.setAudioDecoder(audioDecoder);

        _controller = new PlayController(audioDecoder.getOutputFormat());
        _controller.setReader(this);

        audioDecoder.getAbsolutePresentationStartSector(); // <-- TODO check if it would be better to use this to align on initial presentation sector

        AudioPlayerSectorTimedWriter audioWriter = new AudioPlayerSectorTimedWriter(_controller.getAudioOutputStream(), _iMovieStartSector, _iSectorsPerSecond, audioDecoder.getSampleFramesPerSecond());
        _demuxAutowire.setAudioPacketListener(audioWriter);
    }

    //----------------------------------------------------------

    public MediaPlayer(@Nonnull DiscItemVideoStream vid,
                       @Nonnull ISectorClaimToDemuxedFrame demuxer,
                       @Nonnull ISectorAudioDecoder audioDecoder, // tell everyone this can't be null, but secretly allow it
                       int iSectorStart, int iSectorEnd, int iSectorPerSecond)
    {
        // do the video init
        _cdReader = vid.getSourceCd();
        _iMovieStartSector = iSectorStart;
        _iMovieEndSector = iSectorEnd;
        _iSectorsPerSecond = iSectorPerSecond;

        _demuxAutowire.setSectorClaim2Frame(demuxer);

        if (audioDecoder == null) {
            _controller = new PlayController(vid.getWidth(), vid.getHeight());
        } else {
            _controller = new PlayController(vid.getWidth(), vid.getHeight(), audioDecoder.getOutputFormat());

            AudioPlayerSectorTimedWriter audioWriter = new AudioPlayerSectorTimedWriter(_controller.getAudioOutputStream(), _iMovieStartSector, _iSectorsPerSecond, audioDecoder.getSampleFramesPerSecond());
            _demuxAutowire.setAudioDecoder(audioDecoder);
            _demuxAutowire.setAudioPacketListener(audioWriter);
        }

        ProcessingThread pt = new ProcessingThread(vid.getWidth(), vid.getHeight());
        _controller.setVidProcressor(pt);
        _decodeAutowire.setFrame2Bitstream(pt);
        _decodeAutowire.setBitstream2Mdec(new VDP.Bitstream2Mdec());
        _decodeAutowire.setMdec2Decoded(new VDP.Mdec2Decoded(new MdecDecoder_int(new SimpleIDCT(), vid.getWidth(), vid.getHeight()), DebugLogger.Log));
        _decodeAutowire.setDecodedListener(pt);
        _decodeAutowire.autowire();

        _demuxAutowire.setFrameListener(new DemuxFrameToPlayerProcessor(_controller.getFrameWriter(), _iMovieStartSector, _iSectorsPerSecond));

        _controller.setReader(this);
    }

    @Override
    public void demuxThread(@Nonnull PlayController controller) throws StopPlayingException {
        try {

            final int iSectorLength = _iMovieEndSector - _iMovieStartSector + 1;

            SectorClaimSystem it = SectorClaimSystem.create(_cdReader, _iMovieStartSector, _iMovieEndSector);
            _demuxAutowire.attachToSectorClaimSystem(it);
            _demuxAutowire.autowire();

            IIdentifiedSector identifiedSector;
            for (int iSector = 0; it.hasNext() && !controller.isClosed(); iSector++) {
                identifiedSector = it.next(DebugLogger.Log);
            }
            it.flush(DebugLogger.Log);
        } catch (WrapException ex) {
            if (ex.getCause() instanceof StopPlayingException)
                throw (StopPlayingException)ex.getCause();
            else
                throw new StopPlayingException(ex.getCause());
        } catch (CdReadException ex) {
            throw new StopPlayingException(ex);
        } catch (LoggedFailure ex) {
            throw new StopPlayingException(ex);
        }
    }

    public @Nonnull PlayController getPlayController() {
        return _controller;
    }

    private static class DemuxFrameToPlayerProcessor implements IDemuxedFrame.Listener {
        @Nonnull
        private final IPreprocessedFrameWriter _processor;
        private final int _iAbsolutePresentationStartSector;
        private final int _iSectorsPerSecond;

        public DemuxFrameToPlayerProcessor(@Nonnull IPreprocessedFrameWriter processor,
                                           int iAbsolutePresentationStartSector,
                                           int iSectorsPerSecond)
        {
            _processor = processor;
            _iAbsolutePresentationStartSector = iAbsolutePresentationStartSector;
            _iSectorsPerSecond = iSectorsPerSecond;
        }

        @Override
        public void frameComplete(@Nonnull IDemuxedFrame frame) {

            long lngPresentationNanos = (long) ((frame.getPresentationSector().asDouble() - _iAbsolutePresentationStartSector) / _iSectorsPerSecond * 1000000000.);
            try {
                _processor.writeFrame(frame, lngPresentationNanos);
            } catch (StopPlayingException ex) {
                throw new WrapException(ex);
            }
        }
    }


    private static class ProcessingThread extends Frame2Bitstream implements IFrameProcessor<IDemuxedFrame>, VDP.IDecodedListener {

        private final int _iWidth, _iHeight;

        private int[] _aiDrawHere;

        public ProcessingThread(int iWidth, int iHeight) {
            super(FrameNumber.Type.Index, DebugLogger.Log);
            _iWidth = iWidth;
            _iHeight = iHeight;
        }

        @Override
        public void processFrame(@Nonnull IDemuxedFrame frame, int[] drawHere) {
            // ideally we would have a buffer in this class where the decoded frame is written, then copy that into drawHere
            // but we don't want an extra copy, so use this workaround
            _aiDrawHere = drawHere;
            try {
                frameComplete(frame);
            } catch (LoggedFailure ex) {
                System.err.println("Frame "+frame.getFrame()+" "+ex.getMessage());
            } finally {
                _aiDrawHere = null;
            }
        }

        @Override
        public void decoded(@Nonnull MdecDecoder decoder, FormattedFrameNumber _ignoredFN, Fraction _ignoredPS) {
            decoder.readDecodedRgb(_iWidth, _iHeight, _aiDrawHere);
        }

        @Override
        public void error(ILocalizedMessage errMsg, FormattedFrameNumber frameNumber, Fraction presentationSector) {
            System.err.println(errMsg.getEnglishMessage());
        }

        @Override
        public void assertAcceptsDecoded(MdecDecoder decoder) {}
    }

}
