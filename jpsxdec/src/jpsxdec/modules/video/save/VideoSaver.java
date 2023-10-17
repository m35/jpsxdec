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

package jpsxdec.modules.video.save;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedFileNotFoundException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameCompareIs;
import jpsxdec.modules.video.framenumber.FrameLookup;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.ChromaUpsample;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.TaskCanceledException;

/** Constructs a {@link VDP Video decoder pipeline} from a
 * {@link VideoSaverBuilder} and performs the actual saving of video. */
public class VideoSaver {

    private static final Logger LOG = Logger.getLogger(VideoSaver.class.getName());

    @Nonnull
    private final DiscItemVideoStream _vidItem;
    @Nonnull
    private final VideoSaverBuilder _vsb;
    @Nonnull
    private final VideoFormat _videoFormat;
    @CheckForNull
    private final File _directory;

    private final AutowireVDP _pipeline = new AutowireVDP();

    private final int _iStartSector;
    private final int _iEndSector;

    @Nonnull
    private final DiscSpeed _discSpeed;
    private final Fraction _sectorsPerFrame;
    private final int _iAbsolutePresentationStartSector;

    @Nonnull
    private final FrameToBitstreamOrMdecFilter _frame2bsOrMdecFilter;

    public VideoSaver(@Nonnull DiscItemVideoStream vidItem,
                      @Nonnull VideoSaverBuilder vsb,
                      @Nonnull VDP.GeneratedFileListener genFileListener,
                      @CheckForNull File directory,
                      @Nonnull ILocalizedLogger log,
                      @Nonnull ISectorClaimToFrameAndAudio sectorClaimToFrameAndAudio,
                      @Nonnull DiscSpeed discSpeed,
                      @Nonnull Fraction sectorsPerFrame,
                      int iAbsolutePresentationStartSector)
    {
        _vidItem = vidItem;
        _vsb = vsb;
        _videoFormat = vsb.getVideoFormat();
        _directory = directory;
        _discSpeed = discSpeed;
        _sectorsPerFrame = sectorsPerFrame;
        _iAbsolutePresentationStartSector = iAbsolutePresentationStartSector;
        boolean blnSaveAudio = vsb.getSavingAudio();

        _pipeline.setSectorClaim2FrameAndAudio(sectorClaimToFrameAndAudio);
        _pipeline.setFileListener(genFileListener);

        VDP.ToVideo toVideo = null;
        switch (_videoFormat) {

            case IMGSEQ_BITSTREAM: {
                VDP.Bitstream2File bs2f = new VDP.Bitstream2File(makeFormatter(), log);
                _pipeline.setBitstreamListener(bs2f);
                _pipeline.setFileGenerator(bs2f);
            }
            break;

            case IMGSEQ_MDEC: {
                addBitstream2Mdec();
                VDP.Mdec2File m2f = new VDP.Mdec2File(makeFormatter(), _vsb.getWidth(), _vsb.getHeight(), log);
                _pipeline.setMdec2File(m2f);
            } break;

            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
            case IMGSEQ_TIFF: {
                addBitstream2Mdec();
                addMdec2Decoded(log);
                JavaImageFormat javaImgFmt = _videoFormat.getImgFmt();
                VDP.Decoded2JavaImage d2j = new VDP.Decoded2JavaImage(
                        makeFormatter(), javaImgFmt, _vsb.getWidth(), _vsb.getHeight(), log);
                _pipeline.setDecoded2File(d2j);
            } break;

            case IMGSEQ_JPG: {
                addBitstream2Mdec();
                VDP.Mdec2Jpeg m2jpg = new VDP.Mdec2Jpeg(makeFormatter(), _vsb.getWidth(), _vsb.getHeight(), log);
                _pipeline.setMdec2File(m2jpg);
            } break;

            case AVI_MJPG: {
                addBitstream2Mdec();
                VDP.Mdec2MjpegAvi m2mjpg;
                if (blnSaveAudio)
                    m2mjpg = new VDP.Mdec2MjpegAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(sectorClaimToFrameAndAudio), sectorClaimToFrameAndAudio.getOutputFormat(), log);
                else
                    m2mjpg = new VDP.Mdec2MjpegAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                _pipeline.setMdec2File(m2mjpg);
                toVideo = m2mjpg;
            } break;

            case AVI_JYUV: {
                addBitstream2Mdec();
                addMdec2Decoded(log);
                VDP.Decoded2JYuvAvi d2jyuv;
                if (blnSaveAudio)
                    d2jyuv = new VDP.Decoded2JYuvAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(sectorClaimToFrameAndAudio), sectorClaimToFrameAndAudio.getOutputFormat(), log);
                else
                    d2jyuv = new VDP.Decoded2JYuvAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                _pipeline.setDecoded2File(d2jyuv);
                toVideo = d2jyuv;
            } break;

            case AVI_YUV: {
                addBitstream2Mdec();
                addMdec2Decoded(log);
                VDP.Decoded2YuvAvi d2yuv;
                if (blnSaveAudio)
                    d2yuv = new VDP.Decoded2YuvAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(sectorClaimToFrameAndAudio), sectorClaimToFrameAndAudio.getOutputFormat(), log);
                else
                    d2yuv = new VDP.Decoded2YuvAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                _pipeline.setDecoded2File(d2yuv);
                toVideo = d2yuv;
            } break;

            case AVI_RGB: {
                addBitstream2Mdec();
                addMdec2Decoded(log);
                VDP.Decoded2RgbAvi d2rgb;
                if (blnSaveAudio)
                    d2rgb = new VDP.Decoded2RgbAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeAvSync(sectorClaimToFrameAndAudio), sectorClaimToFrameAndAudio.getOutputFormat(), log);
                else
                    d2rgb = new VDP.Decoded2RgbAvi(getVideoFile(), _vsb.getWidth(), _vsb.getHeight(), makeVSync(), log);
                _pipeline.setDecoded2File(d2rgb);
                toVideo = d2rgb;
            } break;

            default:
                throw new RuntimeException();
        }
        if (toVideo != null)
            _pipeline.setVideo(toVideo);

        _iStartSector = sectorClaimToFrameAndAudio.getStartSector();
        _iEndSector = sectorClaimToFrameAndAudio.getEndSector();
        if (blnSaveAudio) {
            if (toVideo != null)
                _pipeline.setAudioPacketListener(toVideo);

            // when saving with audio, you can't choose start/end frames
            _frame2bsOrMdecFilter = new FrameToBitstreamOrMdecFilter(_vsb.getFileNumberType(), null, null, log);
        } else {
            _frame2bsOrMdecFilter = new FrameToBitstreamOrMdecFilter(_vsb.getFileNumberType(), _vsb.getSaveStartFrame(), _vsb.getSaveEndFrame(), log);
        }
        _pipeline.setFrame2BitstreamOrMdec(_frame2bsOrMdecFilter);
    }

    private void addBitstream2Mdec() {
        VDP.Bitstream2Mdec bs2m = new VDP.Bitstream2Mdec();
        _pipeline.setBitstream2Mdec(bs2m);
    }

    private void addMdec2Decoded(@Nonnull ILocalizedLogger log) {
        MdecDecodeQuality quality = _vsb.getDecodeQuality();
        MdecDecoder vidDecoder = quality.makeDecoder(_vidItem.getWidth(), _vidItem.getHeight());
        if (vidDecoder instanceof MdecDecoder_double) {
            ChromaUpsample chroma = _vsb.getChromaInterpolation();
            ((MdecDecoder_double)vidDecoder).setUpsampler(chroma);
        }

        VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(vidDecoder, log);
        _pipeline.setMdec2Decoded(mdec2decode);
    }

    private void startup(@Nonnull ILocalizedLogger log) throws LoggedFailure {
        VDP.ToVideo video = _pipeline.getVideo();
        if (video != null) {
            try {
                video.open();
            } catch (LocalizedFileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, ex.getSourceMessage(), ex);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(video.getOutputFile().toString()), ex);
            } catch (IOException ex) {
                throw new LoggedFailure(log, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(video.getOutputFile().toString()), ex);
            }
        }
    }

    private void shutdown() {
        VDP.ToVideo video = _pipeline.getVideo();
        if (video != null)
            IO.closeSilently(video, LOG);
    }


    public void save(@Nonnull ProgressLogger pl) throws LoggedFailure, TaskCanceledException {
        SectorClaimSystem it = SectorClaimSystem.create(_vidItem.getSourceCd(), _iStartSector, _iEndSector);
        _pipeline.setSectorClaimSystem(it);

        // finish setting up the pipeline
        _pipeline.autowire();

        pl.progressStart(_iEndSector - _iStartSector + 1);
        startup(pl);
        boolean blnException = true;
        try {


            for (int iSector = 0; it.hasNext(); iSector++) {
                IIdentifiedSector identifiedSector; // keep it out here so I can see what it was while debugging
                try {
                    identifiedSector = it.next(pl);
                } catch (CdException.Read ex) {
                    throw new LoggedFailure(pl, Level.SEVERE,
                            I.IO_READING_FROM_FILE_ERROR_NAME(ex.getFile().toString()), ex);
                }

                sendLogEvent(pl, _frame2bsOrMdecFilter);
                pl.progressUpdate(iSector);

                // if we've already handled the frames we want to save, break early
                if (_frame2bsOrMdecFilter.isDone())
                    break;
            }

            it.flush(pl);
            sendLogEvent(pl, _frame2bsOrMdecFilter);

            blnException = false;
        } finally {
            shutdown();
            if (!blnException)
                pl.progressEnd();
        }

    }

    private void sendLogEvent(@Nonnull ProgressLogger pl,
                              @Nonnull FrameToBitstreamOrMdecFilter f2bsOrMdec)
    {
        if (!pl.isSeekingEvent())
            return;
        FrameNumber curFrm = f2bsOrMdec.getCurrentFrame();
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


    private @Nonnull VideoSync makeVSync() {
        VideoSync vidSync = new VideoSync(_iAbsolutePresentationStartSector,
                                          _discSpeed,
                                          _sectorsPerFrame);
        return vidSync;
    }

    private @Nonnull AudioVideoSync makeAvSync(@Nonnull ISectorClaimToFrameAndAudio audio) {
        AudioVideoSync avSync = new AudioVideoSync(
                _iAbsolutePresentationStartSector,
                _discSpeed,
                _sectorsPerFrame,
                audio.getAbsolutePresentationStartSector(),
                audio.getSampleFramesPerSecond(),
                _vsb.getEmulatePsxAvSync());
        return avSync;
    }

    private @Nonnull File getVideoFile() {
        return VideoFileNameFormatter.singleFile(_directory, _vidItem, _videoFormat);
    }

    private @Nonnull VideoFileNameFormatter makeFormatter() {
        return new VideoFileNameFormatter(_directory, _vidItem, _videoFormat, false);
    }


    // ==============

    private static class FrameToBitstreamOrMdecFilter extends Frame2BitstreamOrMdec {

        @CheckForNull
        private final FrameLookup _startFrame;
        @CheckForNull
        private final FrameLookup _endFrame;

        @Nonnull
        private final ILocalizedLogger _log;

        @CheckForNull
        private FrameNumber _currentFrame = null;
        private boolean _blnIsDone = false;

        public FrameToBitstreamOrMdecFilter(@Nonnull FrameNumber.Type frameNumberType,
                                            @CheckForNull FrameLookup startFrame,
                                            @CheckForNull FrameLookup endFrame,
                                            @Nonnull ILocalizedLogger log)
        {
            super(frameNumberType, log);
            _startFrame = startFrame;
            _endFrame = endFrame;
            _log = log;
        }

        @Override
        public void frameComplete(@Nonnull IDemuxedFrame frame) throws LoggedFailure {
            _currentFrame = frame.getFrame();
            if ((_startFrame != null && _startFrame.compareTo(_currentFrame) == FrameCompareIs.GREATERTHAN))
                return; // haven't received a starting frame yet
            if ((_endFrame != null && _endFrame.compareTo(_currentFrame) == FrameCompareIs.LESSTHAN)) {
                _blnIsDone = true;
                return; // have past the end frame
            }

            FormattedFrameNumber ffn = frame.getFrame().getNumber(getFrameNumberType());
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

            super.frameComplete(frame);
        }
        public @CheckForNull FrameNumber getCurrentFrame() {
            return _currentFrame;
        }
        public boolean isDone() {
            return _blnIsDone;
        }
    }

}
