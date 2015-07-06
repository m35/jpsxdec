/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013-2014  Michael Sabin
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import jpsxdec.I18N;
import jpsxdec.LocalizedMessage;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.FrameNumber;
import jpsxdec.discitems.FrameNumberFormatter;
import jpsxdec.discitems.IDemuxedFrame;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.discitems.savers.VideoSaverBuilder.SectorFeeder;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate.Upsampler;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.ProgressListenerLogger;
import jpsxdec.util.TaskCanceledException;

/** Classes to perform the actual saving of video disc items. */
public abstract class VideoSaver 
        implements IDiscItemSaver,
                   ISectorFrameDemuxer.ICompletedFrameListener,
                   VDP.GeneratedFileListener
{

    protected final FrameLookup _startFrame, _endFrame;
    protected final DiscItemVideoStream _videoItem;
    protected final VideoSaverBuilder.SectorFeeder _sectorFeeder;
    protected final int _iCroppedWidth, _iCroppedHeight;
    protected final ArrayList<LocalizedMessage> _selectedOptions = new ArrayList<LocalizedMessage>();
    protected VDP.IBitstreamListener _bsListener;
    /** Reusable buffer to temporarily hold bitstream. */
    private byte[] _abBitstreamBuf;
    protected FrameNumber _currentFrame;
    /** Initially null. {@link #startSave(jpsxdec.util.ProgressListenerLogger)}
     * implementations need to create it before starting saving. */
    protected ArrayList<File> _generatedFiles;

    protected final FrameNumberFormatter _numberFormatter;

    public VideoSaver(DiscItemVideoStream videoItem, SectorFeeder fdr, VideoSaverBuilder vsb) {
        _videoItem = videoItem;
        _sectorFeeder = fdr;
        
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
            _selectedOptions.add(
                    new LocalizedMessage("Cropping: {0,choice,0#No|1#Yes}", vsb.getCrop() ? 1 : 0)); // I18N

        _selectedOptions.add(new LocalizedMessage("Video format: {0}", vsb.getVideoFormat())); // I18N

        _numberFormatter = videoItem.getFrameNumberFormat().makeFormatter(vsb.getFileNumberType());

        _sectorFeeder.videoDemuxer.setFrameListener(this);
    }

    /** Assumes {@link #_generatedFiles} was created before this is called. */
    final public void fileGenerated(File f) {
        _generatedFiles.add(f);
    }

    final public File[] getGeneratedFiles() {
        if (_generatedFiles == null)
            return null;
        else
            return _generatedFiles.toArray(new File[_generatedFiles.size()]);
    }

    public DiscItem getDiscItem() {
        return _videoItem;
    }

    public String getInput() {
        // TODO: handling when index id is null
        return _videoItem.getIndexId().serialize();
    }

    public void printSelectedOptions(PrintStream ps) {
        for (LocalizedMessage line : _selectedOptions) {
            ps.println(line.getLocalizedMessage());
        }
    }

    final public void frameComplete(IDemuxedFrame frame) throws IOException {
        _currentFrame = frame.getFrame();
        if (!savingAudio() && ((_startFrame != null && _startFrame.compareTo(_currentFrame) > 0) ||
                               (_endFrame   != null && _endFrame.compareTo(_currentFrame)   < 0)))
            return; // haven't received the starting frame yet, or have past the end frame
        _abBitstreamBuf = frame.copyDemuxData(_abBitstreamBuf);
        _bsListener.bitstream(_abBitstreamBuf, frame.getDemuxSize(), frame.getFrame(), frame.getPresentationSector());
    }

    abstract protected boolean savingAudio();

    protected MdecDecoder makeVideoDecoder(VideoSaverBuilder vsb) {
        MdecDecodeQuality quality = vsb.getDecodeQuality();
        _selectedOptions.add(new LocalizedMessage("Decode quality: {0}", quality)); // I18N
        MdecDecoder vidDecoder = quality.makeDecoder(_videoItem.getWidth(), _videoItem.getHeight());
        if (vidDecoder instanceof MdecDecoder_double_interpolate) {
            Upsampler chroma = vsb.getChromaInterpolation();
            _selectedOptions.add(new LocalizedMessage("Chroma upsampling: {0}", chroma)); // I18N
            ((MdecDecoder_double_interpolate)vidDecoder).setResampler(chroma);
        }
        return vidDecoder;
    }

    protected void addSkipFrameSelectedOptions() {
        if (_startFrame != null)
            _selectedOptions.add(new LocalizedMessage("Skip frames before {0}", _startFrame)); // I18N
        if (_endFrame != null)
            _selectedOptions.add(new LocalizedMessage("Skip frames after {0}", _endFrame)); // I18N
    }

    // =========================================================================

    public static class Sequence extends VideoSaver {
        private final String _sOutSummary;

        public Sequence(DiscItemVideoStream videoItem, File directory, SectorFeeder fdr, VideoSaverBuilder vsb) {
            super(videoItem, fdr, vsb);
            
            VideoFormat vf = vsb.getVideoFormat();
            FrameFileFormatter outFileFormat = FrameFileFormatter.makeFormatter(directory, vf, videoItem, vsb.getFileNumberType());
            switch (vf) {
                case IMGSEQ_BITSTREAM:
                {
                    VDP.Bitstream2File b2f;
                    _bsListener = b2f = new VDP.Bitstream2File(outFileFormat);
                    b2f.setGenFileListener(this);
                } break;
                case IMGSEQ_MDEC:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2File mdec2file = new VDP.Mdec2File(outFileFormat,
                            videoItem.getWidth(), videoItem.getHeight());
                    mdec2file.setGenFileListener(this);
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2file);
                } break;
                case IMGSEQ_JPG:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2Jpeg mdec2jpeg = new VDP.Mdec2Jpeg(outFileFormat,
                            videoItem.getWidth(), videoItem.getHeight());
                    mdec2jpeg.setGenFileListener(this);
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2jpeg);
                } break;
                case IMGSEQ_BMP:
                case IMGSEQ_PNG:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(makeVideoDecoder(vsb));
                    VDP.Decoded2JavaImage decode2img = new VDP.Decoded2JavaImage(
                            outFileFormat, vf.getImgFmt(), _iCroppedWidth, _iCroppedHeight);
                    decode2img.setGenFileListener(this);
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2decode);
                    mdec2decode.setDecoded(decode2img);
                } break;
                default:
                    throw new UnsupportedOperationException(vf + " not implemented yet.");
            }

            File[] aoOutRng = vsb.getOutputFileRange();
            if (aoOutRng.length == 1) {
                _sOutSummary = aoOutRng[0].toString();
                _selectedOptions.add(new LocalizedMessage("Output file: {0}", aoOutRng[0])); // I18N
            } else {
                _sOutSummary = I18N.S("{0}-{1}", aoOutRng[0], aoOutRng[1]); // I18N
                _selectedOptions.add(new LocalizedMessage("Output files: {0}-{1}", aoOutRng[0], aoOutRng[1])); // I18N
            }
            addSkipFrameSelectedOptions();
        }

        public String getOutputSummary() {
            return _sOutSummary;
        }

        @Override
        protected boolean savingAudio() {
            return false;
        }

        public void startSave(ProgressListenerLogger pll) throws IOException, TaskCanceledException {

            pll.progressStart();
            final int iSectorLen = _videoItem.getSectorLength();

            _generatedFiles = new ArrayList<File>();
            for (int iSector = 0; iSector < iSectorLen; iSector++) {

                IdentifiedSector identifiedSector = _videoItem.getRelativeIdentifiedSector(iSector);
                if (identifiedSector != null) {
                    _sectorFeeder.feedSector(identifiedSector, pll);
                }

                if (pll.seekingEvent() && _currentFrame != null)
                    pll.event(_numberFormatter.getDescription(_currentFrame)); // I18N

                pll.progressUpdate(iSector / (double)iSectorLen);

                // if we've already handled the frames we want to save
                // break early
                if (_endFrame != null && _currentFrame != null && _endFrame.compareTo(_currentFrame) < 0)
                    break;
            }
            _sectorFeeder.flush(pll);
            if (pll.seekingEvent() && _currentFrame != null)
                pll.event(_numberFormatter.getDescription(_currentFrame)); // I18N
            pll.progressEnd();
        }

    }

    // =========================================================================
    
    public static class Avi extends VideoSaver {
        private final VDP.ToAvi _2avi;

        public Avi(DiscItemVideoStream videoItem, File directory, SectorFeeder fdr, VideoSaverBuilder vsb) {
            super(videoItem, fdr, vsb);

            VideoFormat vf = vsb.getVideoFormat();

            _selectedOptions.add(new LocalizedMessage("Disc speed: {0,choice,1#1x|2#2x} ({1} fps)", // I18N
                                 vsb.getSingleSpeed() ? 1 : 2,
                                 vsb.getFps().asDouble()));

            VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
            int iSectorsPerSecond = vsb.getSingleSpeed() ? 75 : 150;
            File outFile = FrameFileFormatter.makeFile(directory, vf, videoItem);
            if (fdr.audioDecoder == null) {
                _selectedOptions.add(new LocalizedMessage("No audio")); // I18N
                VideoSync vidSync = new VideoSync(_videoItem.getPresentationStartSector(),
                                                  iSectorsPerSecond,
                                                  _videoItem.getSectorsPerFrame());

                switch (vf) {
                    case AVI_JYUV:
                        _2avi = new VDP.Decoded2JYuvAvi(outFile, _iCroppedWidth, _iCroppedHeight, vidSync);
                        break;
                    case AVI_YUV:
                        _2avi = new VDP.Decoded2YuvAvi(outFile, _iCroppedWidth, _iCroppedHeight, vidSync);
                        break;
                    case AVI_RGB:
                        _2avi = new VDP.Decoded2RgbAvi(outFile, _iCroppedWidth, _iCroppedHeight, vidSync);
                        break;
                    case AVI_MJPG:
                        _2avi = new VDP.Mdec2MjpegAvi(outFile, _iCroppedWidth, _iCroppedHeight, vidSync);
                        break;
                    default: throw new UnsupportedOperationException(vf + " not implemented yet.");
                }
            } else {
                _selectedOptions.add(new LocalizedMessage("With audio item(s):")); // I18N
                for (String sDetails : fdr.audioDecoder.getAudioDetails()) {
                    _selectedOptions.add(new LocalizedMessage("{0}", sDetails));
                }
                _selectedOptions.add(new LocalizedMessage("Emulate PSX audio/video sync: {0,choice,0#No|1#Yes}", // I18N
                        vsb.getEmulatePsxAvSync() ? 1 : 0));

                AudioVideoSync avSync = new AudioVideoSync(
                        _videoItem.getPresentationStartSector(),
                        iSectorsPerSecond,
                        _videoItem.getSectorsPerFrame(),
                        fdr.audioDecoder.getPresentationStartSector(),
                        fdr.audioDecoder.getSamplesPerSecond(),
                        vsb.getEmulatePsxAvSync());
                AudioFormat af = fdr.audioDecoder.getOutputFormat();

                switch (vf) {
                    case AVI_JYUV:
                        _2avi = new VDP.Decoded2JYuvAvi(outFile, _iCroppedWidth, _iCroppedHeight, avSync, af);
                        break;
                    case AVI_YUV:
                        _2avi = new VDP.Decoded2YuvAvi(outFile, _iCroppedWidth, _iCroppedHeight, avSync, af);
                        break;
                    case AVI_RGB:
                        _2avi = new VDP.Decoded2RgbAvi(outFile, _iCroppedWidth, _iCroppedHeight, avSync, af);
                        break;
                    case AVI_MJPG:
                        _2avi = new VDP.Mdec2MjpegAvi(outFile, _iCroppedWidth, _iCroppedHeight, avSync, af);
                        break;
                    default: throw new UnsupportedOperationException(vf + " not implemented yet.");
                }
                
                fdr.audioDecoder.setAudioListener(_2avi);
            }
            _2avi.setGenFileListener(this);

            if (_2avi instanceof VDP.IMdecListener) {
                bs2mdec.setMdec((VDP.IMdecListener)_2avi);
            } else if (_2avi instanceof VDP.IDecodedListener) {
                VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(makeVideoDecoder(vsb));
                bs2mdec.setMdec(mdec2decode);
                mdec2decode.setDecoded((VDP.IDecodedListener)_2avi);
            }
            _bsListener = bs2mdec;
            _selectedOptions.add(new LocalizedMessage("Saving as: {0}", _2avi.getOutputFile())); // I18N
            addSkipFrameSelectedOptions();
        }

        public String getOutputSummary() {
            return _2avi.getOutputFile().getName();
        }

        @Override
        protected boolean savingAudio() {
            return _sectorFeeder.audioDecoder != null;
        }

        public void startSave(ProgressListenerLogger pll) throws IOException, TaskCanceledException {
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

            final double SECTOR_LENGTH = iEndSector - iStartSector + 1;

            _generatedFiles = new ArrayList<File>();
            _2avi.open();
            
            _bsListener.setLog(pll);
            try {
                pll.progressStart();

                for (int iSector = iStartSector; iSector <= iEndSector; iSector++) {

                    CdSector cdSector = _videoItem.getSourceCd().getSector(iSector);
                    IdentifiedSector identifiedSector = IdentifiedSector.identifySector(cdSector);
                    if (identifiedSector != null) {
                        _sectorFeeder.feedSector(identifiedSector, pll);
                    }

                    if (pll.seekingEvent() && _currentFrame != null)
                        pll.event(_numberFormatter.getDescription(_currentFrame)); // I18N

                    pll.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);

                    // if we've already handled the frames we want to save
                    // break early
                    if (_sectorFeeder.audioDecoder == null && _endFrame != null && _currentFrame != null && _endFrame.compareTo(_currentFrame) < 0)
                        break;
                }

                _sectorFeeder.flush(pll);
                if (pll.seekingEvent() && _currentFrame != null)
                    pll.event(_numberFormatter.getDescription(_currentFrame)); // I18N
                pll.progressEnd();
            } finally {
                try {
                    _2avi.close();
                } catch (Throwable ex) {
                    pll.log(Level.SEVERE, "Error closing AVI", ex); // I18N
                }
                _bsListener.setLog(null);
            }

        }

    }

}
