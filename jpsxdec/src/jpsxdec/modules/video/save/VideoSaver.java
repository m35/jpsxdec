/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2019  Michael Sabin
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

package jpsxdec.modules.video.save;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.sharedaudio.DecodedAudioPacket;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameCompareIs;
import jpsxdec.modules.video.framenumber.FrameLookup;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Constructs a {@link VDP Video decoder pipeline} from a
 * {@link VideoSaverBuilder} and performs the actual saving of video. */
public class VideoSaver implements DecodedAudioPacket.Listener {

    private static final Logger LOG = Logger.getLogger(VideoSaver.class.getName());

    @Nonnull
    private final DiscItemVideoStream _vidItem;
    @Nonnull
    private final VideoSaverBuilder _vsb;
    @Nonnull
    private final VideoFormat _videoFormat;
    @Nonnull
    private final VDP.GeneratedFileListener _genFileListener;
    @CheckForNull
    private final File _directory;


    @CheckForNull
    private VDP.ToAvi _toAvi;

    public VideoSaver(@Nonnull DiscItemVideoStream vidItem,
                      @Nonnull VideoSaverBuilder vsb,
                      @Nonnull VDP.GeneratedFileListener genFileListener,
                      @CheckForNull File directory)
    {
        _vidItem = vidItem;
        _vsb = vsb;
        _videoFormat = vsb.getVideoFormat();
        _genFileListener = genFileListener;
        _directory = directory;
    }

    private void startup(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        if (_toAvi != null) {
            try {
                _toAvi.open();
            } catch (LocalizedFileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, ex.getSourceMessage(), ex);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(_toAvi.getOutputFile().toString()), ex);
            } catch (IOException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(_toAvi.getOutputFile().toString()), ex);
            }
        }
    }

    private void shutdown() {
        if (_toAvi != null)
            IO.closeSilently(_toAvi, LOG);
    }


    public void save(@Nonnull ProgressLogger pl,
                     @Nonnull ISectorClaimToDemuxedFrame demuxer,
                     @CheckForNull ISectorAudioDecoder audioDecoder)
            throws LoggedFailure, TaskCanceledException
    {
        VDP.IBitstreamListener bsListener = setupDecode(pl, audioDecoder);

        final int iStartSector, iEndSector;
        FrameToBitstream f2bs;
        if (audioDecoder == null) {
            iStartSector = _vidItem.getStartSector();
            iEndSector = _vidItem.getEndSector();
            f2bs = new FrameToBitstream(bsListener, _vsb.getFileNumberType(), _vsb.getSaveStartFrame(), _vsb.getSaveEndFrame(), pl);
        } else {
            iStartSector = Math.min(_vidItem.getStartSector(),
                                    audioDecoder.getStartSector());
            iEndSector   = Math.max(_vidItem.getEndSector(),
                                    audioDecoder.getEndSector());
            // when saving with audio, you can't choose start/end frames
            f2bs = new FrameToBitstream(bsListener, _vsb.getFileNumberType(), null, null, pl);
        }

        demuxer.setFrameListener(f2bs);

        startup(pl);

        try {

            pl.progressStart(iEndSector - iStartSector + 1);

            SectorClaimSystem it = SectorClaimSystem.create(_vidItem.getSourceCd(), iStartSector, iEndSector);
            demuxer.attachToSectorClaimer(it);
            if (audioDecoder != null)
                audioDecoder.attachToSectorClaimer(it);
            for (int iSector = 0; it.hasNext(); iSector++) {
                IIdentifiedSector identifiedSector; // keep it out here so I can see what it was while debugging
                try {
                    identifiedSector = it.next(pl).getClaimer();
                } catch (CdFileSectorReader.CdReadException ex) {
                    throw new LoggedFailure(pl, Level.SEVERE,
                            I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
                }

                sendEvent(pl, f2bs);
                pl.progressUpdate(iSector);

                // if we've already handled the frames we want to save, break early
                if (f2bs.isDone())
                    break;
            }

            it.close(pl);
            sendEvent(pl, f2bs);
            pl.progressEnd();
        } finally {
            shutdown();
        }

    }

    private @Nonnull void sendEvent(@Nonnull ProgressLogger pl,
                                    @Nonnull FrameToBitstream f2bs)
    {
        if (!pl.isSeekingEvent())
            return;
        FrameNumber curFrm = f2bs.getCurrentFrame();
        if (curFrm == null)
            return;
        
        ILocalizedMessage msg = curFrm.getDescription(_vsb.getFileNumberType());
        if (msg == null) {
            // if this video saver was created correctly, the frame number
            // type (i.e. header) should be available in every frame number
            // if not, something weird happened
            LOG.log(Level.SEVERE, "The frame builder says to use {0} but the frame {1} does not have it",
                                  new Object[]{_vsb.getFileNumberType(), curFrm});
            // but since it's just for information purposes,
            // we'll just switch to using the index number
            msg = curFrm.getIndexDescription();
        }
        pl.event(msg);
    }


    private @Nonnull VDP.IBitstreamListener setupDecode(@Nonnull ILocalizedLogger log, @CheckForNull ISectorAudioDecoder audio) {
        final VDP.IBitstreamListener bsListener;
        if (_videoFormat == VideoFormat.IMGSEQ_BITSTREAM) {
            VDP.Bitstream2File bs2f = new VDP.Bitstream2File(makeFormatter(), log);
            bs2f.setGenFileListener(_genFileListener);
            bsListener = bs2f;
        } else {
            bsListener = mdec(log, audio);
        }
        
        return bsListener;
    }

    private @Nonnull VDP.IBitstreamListener mdec(@Nonnull ILocalizedLogger log, @CheckForNull ISectorAudioDecoder audio) {
        final VDP.IMdecListener mdecListener;
        switch (_videoFormat) {
            case IMGSEQ_MDEC: {
                VideoFileNameFormatter outFileFormat = makeFormatter();
                VDP.Mdec2File m2f = new VDP.Mdec2File(outFileFormat, _vsb.getWidth(), _vsb.getHeight(), log);
                m2f.setGenFileListener(_genFileListener);
                mdecListener = m2f;
            } break;
            case IMGSEQ_JPG: {
                VDP.Mdec2Jpeg m2jpg = new VDP.Mdec2Jpeg(makeFormatter(), _vsb.getWidth(), _vsb.getHeight(), log);
                m2jpg.setGenFileListener(_genFileListener);
                mdecListener = m2jpg;
            } break;
            case AVI_MJPG: {
                if (audio == null) {
                    VDP.Mdec2MjpegAvi x = new VDP.Mdec2MjpegAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                    _toAvi = x;
                    mdecListener = x;
                } else {
                    VDP.Mdec2MjpegAvi x = new VDP.Mdec2MjpegAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(audio), audio.getOutputFormat(), log);
                    _toAvi = x;
                    mdecListener = x;
                    audio.setAudioListener(this);
                }
                _toAvi.setGenFileListener(_genFileListener);
            } break;
            default:
                mdecListener = toDecoded(log, audio);
        }
        return new VDP.Bitstream2Mdec(mdecListener);
    }



    private @Nonnull VDP.IMdecListener toDecoded(@Nonnull ILocalizedLogger log, @CheckForNull ISectorAudioDecoder audio) {
        final VDP.IDecodedListener decodedListener;
        if (_videoFormat == VideoFormat.IMGSEQ_BMP || _videoFormat == VideoFormat.IMGSEQ_PNG) {
            JavaImageFormat javaImgFmt = _videoFormat.getImgFmt();
            VDP.Decoded2JavaImage d2j = new VDP.Decoded2JavaImage(
                    makeFormatter(), javaImgFmt, _vsb.getWidth(), _vsb.getHeight(), log);
            d2j.setGenFileListener(_genFileListener);
            decodedListener = d2j;
        } else {
            decodedListener = toAvi(log, audio);
        }
        
        MdecDecodeQuality quality = _vsb.getDecodeQuality();
        MdecDecoder vidDecoder = quality.makeDecoder(_vidItem.getWidth(), _vidItem.getHeight());
        if (vidDecoder instanceof MdecDecoder_double_interpolate) {
            MdecDecoder_double_interpolate.Upsampler chroma = _vsb.getChromaInterpolation();
            ((MdecDecoder_double_interpolate)vidDecoder).setResampler(chroma);
        }
        
        VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(vidDecoder, log);
        mdec2decode.setDecoded(decodedListener);
        return mdec2decode;
    }

    private VDP.IDecodedListener toAvi(@Nonnull ILocalizedLogger log, @CheckForNull ISectorAudioDecoder audio) {
        final VDP.IDecodedListener toDec;
        if (audio == null) {
            switch (_videoFormat) {
                case AVI_JYUV: {
                    VDP.Decoded2JYuvAvi d2 = new VDP.Decoded2JYuvAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                case AVI_YUV: {
                    VDP.Decoded2YuvAvi d2 = new VDP.Decoded2YuvAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                case AVI_RGB: {
                    VDP.Decoded2RgbAvi d2 = new VDP.Decoded2RgbAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                default:
                    throw new RuntimeException();
            }
        } else {
            switch (_videoFormat) {
                case AVI_JYUV: {
                    VDP.Decoded2JYuvAvi d2 = new VDP.Decoded2JYuvAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(audio), audio.getOutputFormat(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                case AVI_YUV: {
                    VDP.Decoded2YuvAvi d2 = new VDP.Decoded2YuvAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(audio), audio.getOutputFormat(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                case AVI_RGB: {
                    VDP.Decoded2RgbAvi d2 = new VDP.Decoded2RgbAvi(getAviFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(audio), audio.getOutputFormat(), log);
                    _toAvi = d2;
                    toDec = d2;
                } break;
                default: 
                    throw new RuntimeException();
            }
            audio.setAudioListener(this);
        }
        _toAvi.setGenFileListener(_genFileListener);
        return toDec;
    }

    private @Nonnull VideoSync makeVSync() {
        VideoSync vidSync = new VideoSync(_vidItem.getAbsolutePresentationStartSector(),
                                          getSectorsPerSecond(),
                                          _vidItem.getSectorsPerFrame());
        return vidSync;
    }

    private int getSectorsPerSecond() {
        return _vsb.getSingleSpeed() ? 75 : 150;
    }

    private @Nonnull AudioVideoSync makeAvSync(@Nonnull ISectorAudioDecoder audio) {
        AudioVideoSync avSync = new AudioVideoSync(
                _vidItem.getAbsolutePresentationStartSector(),
                getSectorsPerSecond(),
                _vidItem.getSectorsPerFrame(),
                audio.getAbsolutePresentationStartSector(),
                audio.getSampleFramesPerSecond(),
                _vsb.getEmulatePsxAvSync());
        return avSync;
    }

    private @Nonnull File getAviFile() {
        return VideoFileNameFormatter.singleFile(_directory, _vidItem, _videoFormat);
    }

    private @Nonnull VideoFileNameFormatter makeFormatter() {
        return new VideoFileNameFormatter(_directory, _vidItem, _videoFormat, false);
    }


    // ==============

    /** Captures any output {@link LoggedFailure} and unwinds the stack so the
     * {@link LoggedFailure} can be thrown outside the pipeline. */
    private static class UnwindException extends RuntimeException {
        @Nonnull
        private final LoggedFailure _fail;
        public UnwindException(@Nonnull LoggedFailure fail) {
            _fail = fail;
        }
        public LoggedFailure getFailure() {
            return _fail;
        }
    }

    private static class FrameToBitstream implements IDemuxedFrame.Listener {
        @Nonnull
        private final VDP.IBitstreamListener _bsListener;

        @Nonnull
        private final FrameNumber.Type _frameNumberType;

        @CheckForNull
        private final FrameLookup _startFrame;
        @CheckForNull
        private final FrameLookup _endFrame;

        @Nonnull
        private final ILocalizedLogger _log;

        @CheckForNull
        private FrameNumber _currentFrame = null;
        private boolean _blnIsDone = false;

        public FrameToBitstream(@Nonnull VDP.IBitstreamListener bsListener,
                                @Nonnull FrameNumber.Type frameNumberType,
                                @CheckForNull FrameLookup startFrame,
                                @CheckForNull FrameLookup endFrame,
                                @Nonnull ILocalizedLogger log)
        {
            _bsListener = bsListener;
            _frameNumberType = frameNumberType;
            _startFrame = startFrame;
            _endFrame = endFrame;
            _log = log;
        }
        public void frameComplete(@Nonnull IDemuxedFrame frame) {
            _currentFrame = frame.getFrame();
            if ((_startFrame != null && _startFrame.compareTo(_currentFrame) == FrameCompareIs.GREATERTHAN))
                return; // haven't received a starting frame yet
            if ((_endFrame != null && _endFrame.compareTo(_currentFrame) == FrameCompareIs.LESSTHAN)) {
                _blnIsDone = true;
                return; // have past the end frame
            }
            byte[] abBitstream = frame.copyDemuxData();
            try {
                FormattedFrameNumber ffn = frame.getFrame().getNumber(_frameNumberType);
                if (ffn == null) {
                    // if this video saver was created correctly, the frame number
                    // type (i.e. header) should be available in every frame number
                    // if not, something weird happened

                    // this could get ugly down the pipeline if
                    // individual frame files are expected.
                    // without a frame number, it will keep
                    // generating the same file name over and over
                    _log.log(Level.SEVERE, I.FRAME_MISSING_FRAME_NUMBER_HEADER(frame.getFrame().getIndexNumber().getFrameValue()));
                    // maybe it would be better to just throw here?
                }
                _bsListener.bitstream(abBitstream, frame.getDemuxSize(), ffn, frame.getPresentationSector());
            } catch (LoggedFailure ex) {
                // fire the exception outside of the pipeline
                throw new UnwindException(ex);
            }
        }
        public @CheckForNull FrameNumber getCurrentFrame() {
            return _currentFrame;
        }
        public boolean isDone() {
            return _blnIsDone;
        }
    }

    public void audioPacketComplete(DecodedAudioPacket packet,
                                    ILocalizedLogger log)
    {
        try {
            _toAvi.writeAudio(packet);
        } catch (LoggedFailure ex) {
            // intercept the exception, then unwind outside of the pipeline
            throw new UnwindException(ex);
        }
    }
}
