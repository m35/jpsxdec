/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2017  Michael Sabin
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

package jpsxdec.discitems.savers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.AudioFormat;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.FrameNumberFormatter;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.discitems.savers.VideoSaverBuilder.SectorFeeder;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.LocalizedFileNotFoundException;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate.Upsampler;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.sectors.IdentifiedSectorIterator;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;
import jpsxdec.util.LoggedFailure;
import jpsxdec.util.ProgressLogger;
import jpsxdec.util.TaskCanceledException;

/** Classes to perform the actual saving of video disc items. */
public abstract class VideoSaver 
        implements IDiscItemSaver,
                   ISectorFrameDemuxer.ICompletedFrameListener,
                   VDP.GeneratedFileListener
{
    private static final Logger LOG = Logger.getLogger(VideoSaver.class.getName());

    @Nonnull
    protected final DiscItemVideoStream _videoItem;
    @Nonnull
    protected final VideoFormat _vidFmt;
    @CheckForNull
    protected final MdecDecoder _decoder;
    @CheckForNull
    protected final FrameLookup _startFrame, _endFrame;
    @Nonnull
    protected final VideoSaverBuilder.SectorFeeder _sectorFeeder;
    protected final int _iCroppedWidth, _iCroppedHeight;
    protected final ArrayList<ILocalizedMessage> _selectedOptions = new ArrayList<ILocalizedMessage>();
    @CheckForNull
    protected VDP.IBitstreamListener _bsListener;
    /** Reusable buffer to temporarily hold bitstream. */
    @CheckForNull
    private byte[] _abBitstreamBuf;
    @CheckForNull
    protected FrameNumber _currentFrame;
    /** Initially null. {@link #startSave(jpsxdec.util.ProgressLogger)}
     * implementations need to create it before starting saving. */
    @CheckForNull
    protected ArrayList<File> _generatedFiles;

    @Nonnull
    protected final FrameNumberFormatter _numberFormatter;

    public VideoSaver(@Nonnull DiscItemVideoStream videoItem, @Nonnull SectorFeeder fdr, 
                      @Nonnull VideoSaverBuilder vsb)
    {
        // _bsListener initialized in subclasses
        _videoItem = videoItem;
        _vidFmt = vsb.getVideoFormat();
        _sectorFeeder = fdr;
        
        switch (_vidFmt) {
            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
            case AVI_JYUV:
            case AVI_YUV:
            case AVI_RGB:
                _decoder = makeVideoDecoder(vsb);
                break;
            case IMGSEQ_BITSTREAM:
            case IMGSEQ_MDEC:
            case IMGSEQ_JPG:
            case AVI_MJPG:
                _decoder = null;
                break;
            default:
                throw new UnsupportedOperationException(_vidFmt + " not implemented yet.");
        }

        _startFrame = vsb.getSaveStartFrame();
        _endFrame = vsb.getSaveEndFrame();

        if (vsb.getCrop()) {
            _iCroppedWidth  = videoItem.getWidth();
            _iCroppedHeight = videoItem.getHeight();
        } else {
            _iCroppedWidth  = Calc.fullDimension(videoItem.getWidth());
            _iCroppedHeight = Calc.fullDimension(videoItem.getHeight());
        }
        if (vsb.getCrop_enabled())
            _selectedOptions.add(I.CMD_CROPPING(vsb.getCrop() ? 1 : 0));

        _selectedOptions.add(I.CMD_VIDEO_FORMAT(vsb.getVideoFormat()));

        _numberFormatter = videoItem.getFrameNumberFormat().makeFormatter(vsb.getFileNumberType());

        _sectorFeeder.videoDemuxer.setFrameListener(this);
    }

    /** Assumes {@link #_generatedFiles} was created before this is called. */
    final public void fileGenerated(@Nonnull File f) {
        if (_generatedFiles == null)
            throw new IllegalStateException("Attempting to add generated files before start of saving");
        _generatedFiles.add(f);
    }

    final public @CheckForNull File[] getGeneratedFiles() {
        if (_generatedFiles == null)
            return null;
        else
            return _generatedFiles.toArray(new File[_generatedFiles.size()]);
    }

    public @Nonnull DiscItem getDiscItem() {
        return _videoItem;
    }

    public String getInput() {
        return _videoItem.getIndexId().serialize();
    }

    public void printSelectedOptions(@Nonnull FeedbackStream fbs) {
        for (ILocalizedMessage line : _selectedOptions) {
            fbs.println(line);
        }
    }

    final public void frameComplete(@Nonnull IDemuxedFrame frame) throws LoggedFailure {
        _currentFrame = frame.getFrame();
        if (!savingAudio() && ((_startFrame != null && _startFrame.compareTo(_currentFrame) > 0) ||
                               (_endFrame   != null && _endFrame.compareTo(_currentFrame)   < 0)))
            return; // haven't received the starting frame yet, or have past the end frame
        _abBitstreamBuf = frame.copyDemuxData(_abBitstreamBuf);
        _bsListener.bitstream(_abBitstreamBuf, frame.getDemuxSize(), frame.getFrame(), frame.getPresentationSector());
    }

    abstract protected boolean savingAudio();

    /** Call only when the target video format has decoders. */
    final protected @Nonnull MdecDecoder makeVideoDecoder(@Nonnull VideoSaverBuilder vsb) {
        // quality should != null for the target format
        MdecDecodeQuality quality = vsb.getDecodeQuality();
        _selectedOptions.add(I.CMD_DECODE_QUALITY(quality));
        MdecDecoder vidDecoder = quality.makeDecoder(_videoItem.getWidth(), _videoItem.getHeight());
        if (vidDecoder instanceof MdecDecoder_double_interpolate) {
            Upsampler chroma = vsb.getChromaInterpolation();
            _selectedOptions.add(I.CMD_UPSAMPLE_QUALITY(chroma));
            ((MdecDecoder_double_interpolate)vidDecoder).setResampler(chroma);
        }
        return vidDecoder;
    }

    final protected void addSkipFrameSelectedOptions() {
        if (_startFrame != null)
            _selectedOptions.add(I.CMD_FRAME_RANGE_BEFORE(_startFrame));
        if (_endFrame != null)
            _selectedOptions.add(I.CMD_FRAME_RANGE_AFTER(_endFrame));
    }

    // =========================================================================

    public static class Sequence extends VideoSaver {
        @Nonnull
        private final ILocalizedMessage _outSummary;
        @Nonnull
        private final File[] _aoOutRng;
        @Nonnull
        private final FrameFileFormatter _outFileFormat;
        public Sequence(@Nonnull DiscItemVideoStream videoItem, @Nonnull File directory,
                        @Nonnull SectorFeeder fdr, @Nonnull VideoSaverBuilder vsb)
        {
            super(videoItem, fdr, vsb);
            _outFileFormat = FrameFileFormatter.makeFormatter(directory, _vidFmt, videoItem, vsb.getFileNumberType());
            
            _aoOutRng = vsb.getOutputFileRange();
            if (_aoOutRng.length == 1) {
                _outSummary = new UnlocalizedMessage(_aoOutRng[0].toString());
                _selectedOptions.add(I.CMD_OUTPUT_FILE(_aoOutRng[0]));
            } else {
                _outSummary = I.VID_RANGE_OF_FILES_TO_SAVE(_aoOutRng[0], _aoOutRng[1]);
                _selectedOptions.add(I.CMD_OUTPUT_FILES(_aoOutRng[0], _aoOutRng[1]));
            }
            addSkipFrameSelectedOptions();
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return _outSummary;
        }

        @Override
        protected boolean savingAudio() {
            return false;
        }

        public void startSave(@Nonnull ProgressLogger pll) throws LoggedFailure, TaskCanceledException {

            switch (_vidFmt) {
                case IMGSEQ_BITSTREAM:
                {
                    VDP.Bitstream2File b2f = new VDP.Bitstream2File(_outFileFormat, pll);
                    b2f.setGenFileListener(this);
                    _bsListener = b2f;
                } break;
                case IMGSEQ_MDEC:
                {
                    VDP.Mdec2File mdec2file = new VDP.Mdec2File(_outFileFormat,
                            _videoItem.getWidth(), _videoItem.getHeight(), pll);
                    mdec2file.setGenFileListener(this);
                    _bsListener = new VDP.Bitstream2Mdec(mdec2file);
                } break;
                case IMGSEQ_JPG:
                {
                    VDP.Mdec2Jpeg mdec2jpeg = new VDP.Mdec2Jpeg(_outFileFormat,
                            _videoItem.getWidth(), _videoItem.getHeight(), pll);
                    mdec2jpeg.setGenFileListener(this);
                    _bsListener = new VDP.Bitstream2Mdec(mdec2jpeg);
                } break;
                case IMGSEQ_BMP:
                case IMGSEQ_PNG:
                {
                    // vf.getImgFmt() should != null for these image formats
                    VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(_decoder, pll);
                    VDP.Decoded2JavaImage decode2img = new VDP.Decoded2JavaImage(
                            _outFileFormat, _vidFmt.getImgFmt(), _iCroppedWidth, _iCroppedHeight, pll);
                    decode2img.setGenFileListener(this);
                    mdec2decode.setDecoded(decode2img);
                    _bsListener = new VDP.Bitstream2Mdec(mdec2decode);
                } break;
                default:
                    throw new UnsupportedOperationException(_vidFmt + " not implemented yet.");
            }

            pll.progressStart(_videoItem.getSectorLength());
            IdentifiedSectorIterator it = _videoItem.identifiedSectorIterator();

            _generatedFiles = new ArrayList<File>();
            for (int iSector = 0; it.hasNext(); iSector++) {

                IdentifiedSector identifiedSector;
                try {
                    identifiedSector = it.next();
                } catch (IOException ex) {
                    throw new LoggedFailure(pll, Level.SEVERE,
                            I.IO_READING_FROM_FILE_ERROR_NAME(it.getSourceCdFile().toString()), ex);
                }
                
                if (identifiedSector != null) {
                    _sectorFeeder.feedSector(identifiedSector, pll);
                }

                if (pll.isSeekingEvent() && _currentFrame != null)
                    pll.event(_numberFormatter.getDescription(_currentFrame));

                pll.progressUpdate(iSector);

                // if we've already handled the frames we want to save
                // break early
                if (_endFrame != null && _currentFrame != null && _endFrame.compareTo(_currentFrame) < 0)
                    break;
            }
            _sectorFeeder.flush(pll);
            if (pll.isSeekingEvent() && _currentFrame != null)
                pll.event(_numberFormatter.getDescription(_currentFrame));
            pll.progressEnd();
        }

    }

    // =========================================================================
    
    public static class Avi extends VideoSaver {
        private final int _iSectorsPerSecond;
        private final boolean _blnEmulatePsxAvSync;
        @Nonnull
        private final File _outFile;

        public Avi(@Nonnull DiscItemVideoStream videoItem, @CheckForNull File directory,
                   @Nonnull SectorFeeder fdr, @Nonnull VideoSaverBuilder vsb)
        {
            super(videoItem, fdr, vsb);
            
            _blnEmulatePsxAvSync = vsb.getEmulatePsxAvSync();
            _selectedOptions.add(I.CMD_DISC_SPEED(vsb.getSingleSpeed() ? 1 : 2,
                                 vsb.getFps().asDouble()));

            _iSectorsPerSecond = vsb.getSingleSpeed() ? 75 : 150;
            _outFile = FrameFileFormatter.makeFile(directory, _vidFmt, videoItem);
            
            if (_sectorFeeder.audioDecoder == null) {
                _selectedOptions.add(I.CMD_NO_AUDIO());
            } else {
                _selectedOptions.add(I.CMD_SAVING_WITH_AUDIO_ITEMS());
                _selectedOptions.addAll(Arrays.asList(_sectorFeeder.audioDecoder.getAudioDetails()));
                _selectedOptions.add(I.CMD_EMULATE_PSX_AV_SYNC_NY(_blnEmulatePsxAvSync ? 1 : 0));
            }

            _selectedOptions.add(I.CMD_SAVING_AS(_outFile));
            addSkipFrameSelectedOptions();
        }

        public @Nonnull ILocalizedMessage getOutputSummary() {
            return new UnlocalizedMessage(_outFile.getName());
        }

        @Override
        protected boolean savingAudio() {
            return _sectorFeeder.audioDecoder != null;
        }

        public void startSave(@Nonnull ProgressLogger pll) throws LoggedFailure, TaskCanceledException {
            final VDP.ToAvi toAvi;
            if (_sectorFeeder.audioDecoder == null) {
                VideoSync vidSync = new VideoSync(_videoItem.getPresentationStartSector(),
                                                  _iSectorsPerSecond,
                                                  _videoItem.getSectorsPerFrame());

                switch (_vidFmt) {
                    case AVI_JYUV:
                        toAvi = new VDP.Decoded2JYuvAvi(_outFile, _iCroppedWidth, _iCroppedHeight, vidSync, pll);
                        break;
                    case AVI_YUV:
                        toAvi = new VDP.Decoded2YuvAvi(_outFile, _iCroppedWidth, _iCroppedHeight, vidSync, pll);
                        break;
                    case AVI_RGB:
                        toAvi = new VDP.Decoded2RgbAvi(_outFile, _iCroppedWidth, _iCroppedHeight, vidSync, pll);
                        break;
                    case AVI_MJPG:
                        toAvi = new VDP.Mdec2MjpegAvi(_outFile, _iCroppedWidth, _iCroppedHeight, vidSync, pll);
                        break;
                    default: throw new UnsupportedOperationException(_vidFmt + " not implemented yet.");
                }
            } else {
                AudioVideoSync avSync = new AudioVideoSync(
                        _videoItem.getPresentationStartSector(),
                        _iSectorsPerSecond,
                        _videoItem.getSectorsPerFrame(),
                        _sectorFeeder.audioDecoder.getPresentationStartSector(),
                        _sectorFeeder.audioDecoder.getSamplesPerSecond(),
                        _blnEmulatePsxAvSync);
                AudioFormat af = _sectorFeeder.audioDecoder.getOutputFormat();

                switch (_vidFmt) {
                    case AVI_JYUV:
                        toAvi = new VDP.Decoded2JYuvAvi(_outFile, _iCroppedWidth, _iCroppedHeight, avSync, af, pll);
                        break;
                    case AVI_YUV:
                        toAvi = new VDP.Decoded2YuvAvi(_outFile, _iCroppedWidth, _iCroppedHeight, avSync, af, pll);
                        break;
                    case AVI_RGB:
                        toAvi = new VDP.Decoded2RgbAvi(_outFile, _iCroppedWidth, _iCroppedHeight, avSync, af, pll);
                        break;
                    case AVI_MJPG:
                        toAvi = new VDP.Mdec2MjpegAvi(_outFile, _iCroppedWidth, _iCroppedHeight, avSync, af, pll);
                        break;
                    default: throw new UnsupportedOperationException(_vidFmt + " not implemented yet.");
                }

                _sectorFeeder.audioDecoder.setAudioListener(toAvi);
            }
            toAvi.setGenFileListener(this);

            if (toAvi instanceof VDP.IMdecListener) {
                _bsListener = new VDP.Bitstream2Mdec((VDP.IMdecListener)toAvi);
            } else if (toAvi instanceof VDP.IDecodedListener) {
                VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(_decoder, pll);
                _bsListener = new VDP.Bitstream2Mdec(mdec2decode);
                mdec2decode.setDecoded((VDP.IDecodedListener)toAvi);
            }
            addSkipFrameSelectedOptions();
            final int iStartSector, iEndSector;
            if (_sectorFeeder.audioDecoder == null) {
                iStartSector = _videoItem.getStartSector();
                iEndSector = _videoItem.getEndSector();
            } else {
                iStartSector = Math.min(_videoItem.getStartSector(),
                                        _sectorFeeder.audioDecoder.getStartSector());
                iEndSector   = Math.max(_videoItem.getEndSector(),
                                        _sectorFeeder.audioDecoder.getEndSector());
            }

            _generatedFiles = new ArrayList<File>(1);
            try {
                toAvi.open();
            } catch (LocalizedFileNotFoundException ex) {
                throw new LoggedFailure(pll, Level.SEVERE, ex.getSourceMessage(), ex);
            } catch (FileNotFoundException ex) {
                throw new LoggedFailure(pll, Level.SEVERE, I.IO_OPENING_FILE_ERROR_NAME(toAvi.getOutputFile().toString()), ex);
            } catch (IOException ex) {
                throw new LoggedFailure(pll, Level.SEVERE, I.IO_WRITING_TO_FILE_ERROR_NAME(toAvi.getOutputFile().toString()), ex);
            }
            
            try {

                pll.progressStart(iEndSector - iStartSector + 1);

                IdentifiedSectorIterator it = IdentifiedSectorIterator.create(_videoItem.getSourceCd(), iStartSector, iEndSector);
                for (int iSector = 0; it.hasNext(); iSector++) {
                    IdentifiedSector identifiedSector;
                    try {
                        identifiedSector = it.next();
                    } catch (IOException ex) {
                        throw new LoggedFailure(pll, Level.SEVERE,
                                I.IO_READING_FROM_FILE_ERROR_NAME(it.getSourceCdFile().toString()), ex);
                    }

                    if (identifiedSector != null)
                        _sectorFeeder.feedSector(identifiedSector, pll);
                    
                    if (pll.isSeekingEvent() && _currentFrame != null)
                        pll.event(_numberFormatter.getDescription(_currentFrame));
                    pll.progressUpdate(iSector);

                    // if we've already handled the frames we want to save
                    // break early
                    if (_sectorFeeder.audioDecoder == null &&
                        _endFrame != null && _currentFrame != null && _endFrame.compareTo(_currentFrame) < 0)
                        break;
                }

                _sectorFeeder.flush(pll);
                if (pll.isSeekingEvent() && _currentFrame != null)
                    pll.event(_numberFormatter.getDescription(_currentFrame));
                pll.progressEnd();
            } finally {
                IO.closeSilently(toAvi, LOG);
            }

        }

    }

}
