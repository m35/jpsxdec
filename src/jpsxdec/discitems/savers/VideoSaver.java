/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2013  Michael Sabin
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
import java.util.Arrays;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemVideoStream;
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
public abstract class VideoSaver implements IDiscItemSaver, ISectorFrameDemuxer.ICompletedFrameListener {

    protected final int _iStartFrame, _iEndFrame;
    protected final DiscItemVideoStream _videoItem;
    protected final VideoSaverBuilder.SectorFeeder _sectorFeeder;
    protected final int _iCroppedWidth, _iCroppedHeight;
    protected final ArrayList<String> _selectedOptions = new ArrayList<String>();
    protected VDP.IBitstreamListener _bsListener;
    private byte[] _abBitstream;
    protected int _iCurrentFrame;

    public VideoSaver(DiscItemVideoStream videoItem, SectorFeeder fdr, VideoSaverBuilder vsb) {
        _videoItem = videoItem;
        _sectorFeeder = fdr;
        
        _iCurrentFrame = _iStartFrame = vsb.getSaveStartFrame();
        _iEndFrame = vsb.getSaveEndFrame();

        if (vsb.getCrop()) {
            _iCroppedWidth  = videoItem.getWidth();
            _iCroppedHeight = videoItem.getHeight();
        } else {
            _iCroppedWidth  = Calc.fullDimension(videoItem.getWidth());
            _iCroppedHeight = Calc.fullDimension(videoItem.getHeight());
        }
        if (vsb.getCrop_enabled())
            _selectedOptions.add("Cropping: " + (vsb.getCrop() ? "Yes" : "No"));

        VideoFormat videoFormat = vsb.getVideoFormat();

        _selectedOptions.add("Frames: " + vsb.getSaveStartFrame() + "-" + vsb.getSaveEndFrame());
        _selectedOptions.add("Video format: " + videoFormat);


        _selectedOptions.add(String.format("Disc speed: %s (%s fps)",
                vsb.getSingleSpeed() ? "1x" : "2x",
                DiscItemVideoStream.formatFps(vsb.getFps())));

        _sectorFeeder.videoDemuxer.setFrameListener(this);
    }

    public DiscItem getDiscItem() {
        return _videoItem;
    }

    public String getInput() {
        // TODO: handling when index id is null
        return _videoItem.getIndexId().serialize();
    }

    public void printSelectedOptions(PrintStream ps) {
        for (String sLine : _selectedOptions) {
            ps.println(sLine);
        }
    }

    final public void frameComplete(IDemuxedFrame frame) throws IOException {
        _iCurrentFrame = frame.getFrame();
        if (!savingAudio() && _iCurrentFrame < _iStartFrame)
            return;
        _abBitstream = frame.copyDemuxData(_abBitstream);
        _bsListener.bitstream(_abBitstream, frame.getDemuxSize(), frame.getFrame(), frame.getPresentationSector());
    }

    abstract protected boolean savingAudio();

    protected MdecDecoder makeVideoDecoder(VideoSaverBuilder vsb) {
        MdecDecodeQuality quality = vsb.getDecodeQuality();
        _selectedOptions.add("Decode quality: " + quality);
        MdecDecoder vidDecoder = quality.makeDecoder(_videoItem.getWidth(), _videoItem.getHeight());
        if (vidDecoder instanceof MdecDecoder_double_interpolate) {
            Upsampler chroma = vsb.getChromaInterpolation();
            _selectedOptions.add("Chroma upsampling: " + chroma);
            ((MdecDecoder_double_interpolate)vidDecoder).setResampler(chroma);
        }
        return vidDecoder;
    }

    public static class Sequence extends VideoSaver {
        private final String _sOutSummary;

        public Sequence(DiscItemVideoStream videoItem, File directory, SectorFeeder fdr, VideoSaverBuilder vsb) {
            super(videoItem, fdr, vsb);
            
            File[] aoOutRng = vsb.getOutputFileRange();
            if (aoOutRng.length == 1)
                _sOutSummary = aoOutRng[0].toString();
            else
                _sOutSummary = aoOutRng[0] + "-" + aoOutRng[1];

            VideoFormat vf = vsb.getVideoFormat();
            File outFileStrFormat = new File(directory, vf.makeFormat(videoItem));
            switch (vf) {
                case IMGSEQ_BITSTREAM:
                    _bsListener = new VDP.Bitstream2File(outFileStrFormat);
                    break;
                case IMGSEQ_MDEC:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2File mdec2file = new VDP.Mdec2File(outFileStrFormat,
                            videoItem.getWidth(), videoItem.getHeight());
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2file);
                } break;
                case IMGSEQ_JPG:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2Jpeg mdec2jpeg = new VDP.Mdec2Jpeg(outFileStrFormat,
                            videoItem.getWidth(), videoItem.getHeight());
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2jpeg);
                } break;
                case IMGSEQ_BMP:
                case IMGSEQ_PNG:
                {
                    VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
                    VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(makeVideoDecoder(vsb));
                    VDP.Decoded2JavaImage decode2img = new VDP.Decoded2JavaImage(
                            outFileStrFormat, vf.getImgFmt(), _iCroppedWidth, _iCroppedHeight);
                    _bsListener = bs2mdec;
                    bs2mdec.setMdec(mdec2decode);
                    mdec2decode.setDecoded(decode2img);
                } break;
                default:
                    throw new UnsupportedOperationException(vf + " not implemented yet.");
            }

            _selectedOptions.add("Saving as: " + _sOutSummary);
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

            for (int iSector = 0; iSector < iSectorLen; iSector++) {

                IdentifiedSector identifiedSector = _videoItem.getRelativeIdentifiedSector(iSector);
                if (identifiedSector != null) {
                    _sectorFeeder.feedSector(identifiedSector, pll);
                }

                if (pll.seekingEvent())
                    pll.event("Frame " + _iCurrentFrame);

                pll.progressUpdate(iSector / (double)iSectorLen);

                // if we've already handled the frames we want to save
                // break early
                if (_iCurrentFrame > _iEndFrame)
                    break;
            }
            _sectorFeeder.flush(pll);
            pll.progressEnd();
        }

    }

    public static class Avi extends VideoSaver {
        private final VDP.ToAvi _2avi;

        public Avi(DiscItemVideoStream videoItem, File directory, SectorFeeder fdr, VideoSaverBuilder vsb) {
            super(videoItem, fdr, vsb);

            VideoFormat vf = vsb.getVideoFormat();
            
            VDP.Bitstream2Mdec bs2mdec = new VDP.Bitstream2Mdec();
            int iSectorsPerSecond = vsb.getSingleSpeed() ? 75 : 150;
            File outFile = new File(directory, vf.makeFormat(videoItem));
            if (fdr.audioDecoder == null) {
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
                _selectedOptions.add("With audio item(s):");
                _selectedOptions.addAll(Arrays.asList(fdr.audioDecoder.getAudioDetails()));
                _selectedOptions.add("Emulate PSX audio/video sync: " + (vsb.getEmulatePsxAvSync() ? "Yes" : "No"));

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

            if (_2avi instanceof VDP.IMdecListener) {
                bs2mdec.setMdec((VDP.IMdecListener)_2avi);
            } else if (_2avi instanceof VDP.IDecodedListener) {
                VDP.Mdec2Decoded mdec2decode = new VDP.Mdec2Decoded(makeVideoDecoder(vsb));
                bs2mdec.setMdec(mdec2decode);
                mdec2decode.setDecoded((VDP.IDecodedListener)_2avi);
            }
            _bsListener = bs2mdec;
            _selectedOptions.add("Saving as: " + _2avi.getOutputFile());
        }

        public String getOutputSummary() {
            return _2avi.getOutputFile().toString();
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

                    if (pll.seekingEvent())
                        pll.event("Frame " + _iCurrentFrame);

                    pll.progressUpdate((iSector - iStartSector) / SECTOR_LENGTH);

                    // if we've already handled the frames we want to save
                    // break early
                    if (_sectorFeeder.audioDecoder == null && _iCurrentFrame > _iEndFrame)
                        break;
                }

                _sectorFeeder.flush(pll);
                pll.progressEnd();
            } finally {
                try {
                    _2avi.close();
                } catch (Throwable ex) {
                    pll.log(Level.SEVERE, "Error closing AVI", ex);
                }
                _bsListener.setLog(null);
            }

        }

    }

}
