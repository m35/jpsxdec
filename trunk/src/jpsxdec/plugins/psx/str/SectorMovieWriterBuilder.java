/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.str;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.JavaImageFormat.JpgQualities;
import jpsxdec.plugins.psx.str.SectorMovieWriters.*;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_double;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_int;
import jpsxdec.plugins.psx.video.mdec.idct.PsxMdecIDCT;
import jpsxdec.plugins.psx.video.mdec.idct.StephensIDCT;
import jpsxdec.plugins.psx.video.mdec.idct.simple_idct;
import jpsxdec.plugins.xa.AudioStreamsCombiner;
import jpsxdec.plugins.xa.IAudioSectorDecoder;
import jpsxdec.plugins.xa.DiscItemAudioStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Misc;
import jpsxdec.util.TabularFeedback;


public class SectorMovieWriterBuilder  {

    private static final Logger log = Logger.getLogger(SectorMovieWriterBuilder.class.getName());

    private final DiscItemSTRVideo _sourceVidItem;

    public SectorMovieWriterBuilder(DiscItemSTRVideo vidItem) {
        _sourceVidItem = vidItem;
        setVideoFormat(VideoFormat.AVI_MJPG);
        _blnSaveAudio = _sourceVidItem.hasAudio();
        setSaveStartFrame(_sourceVidItem.getStartFrame());
        setSaveEndFrame(_sourceVidItem.getEndFrame());
        setParallelAudioBySizeOrder(0);
    }


    public static final String PROP_1X_DISC_SPEED = "singleSpeed";
    private boolean _blnSingleSpeed;
    public boolean getSingleSpeed() {
        switch (_sourceVidItem.getDiscSpeed()) {
            case 1:
                return true;
            case 2:
                return false;
            default:
                return _blnSingleSpeed;
        }
    }
    public void setSingleSpeed(boolean val) {
        boolean old = getSingleSpeed();
        _blnSingleSpeed = val;
    }

    public static final String PROP_SAVE_AUDIO = "saveAudio";
    private boolean _blnSaveAudio;
    public boolean getSaveAudio() {
        // can only decode audio if we're saving avi and
        // we're starting from the first frame (otherwise the ADPCM contex is unreliable)
        if (getVideoFormat().getContainer() != Container.AVI ||
            getSaveStartFrame() != _sourceVidItem.getStartFrame() ||
            !_sourceVidItem.hasAudio())
            return false;
        else
            return _blnSaveAudio;
    }
    public void setSaveAudio(boolean val) {
        _blnSaveAudio = val;
    }

    public static enum Container {
        AVI,
        IMGSEQ,
        YUV4MPEG2
    }

    public static enum VideoFormat {
        AVI_MJPG        ("AVI: Compressed (MJPG)"  , "avi:mjpg", Container.AVI, JavaImageFormat.JPG),
        AVI_BMP         ("AVI: Uncompressed (BMP)" , "avi:bmp", Container.AVI),
        AVI_YUV         ("AVI: YUV"                , "avi:yuv", Container.AVI),
        IMGSEQ_PNG      ("Image sequence: png"     , "png", Container.IMGSEQ, JavaImageFormat.PNG),
        IMGSEQ_JPG      ("Image sequence: jpg"     , "jpg", Container.IMGSEQ, JavaImageFormat.JPG),
        IMGSEQ_BMP      ("Image sequence: bmp"     , "bmp", Container.IMGSEQ, JavaImageFormat.BMP),
        //IMGSEQ_RAW      ("Image sequence: raw"     , "raw", Container.IMGSEQ),
        //IMGSEQ_YUV      ("Image sequence: yuv"     , "yuv", Container.IMGSEQ),
        //IMGSEQ_PSXYUV   ("Image sequence: PSX yuv", "psxyuv", Container.IMGSEQ),
        IMGSEQ_DEMUX    ("Image sequence: demux"   , "demux", Container.IMGSEQ),
        IMGSEQ_MDEC     ("Image sequence: mdec"    , "mdec", Container.IMGSEQ),
        YUV4MPEG2_YUV   ("yuv4mpeg2"          , "y4m", Container.YUV4MPEG2),
        //YUV4MPEG2_PSXYUV("yuv4mpeg2 w/ PSX yuv"      , "y4m:psx", Container.YUV4MPEG2),
        ;

        private final String _sGui;
        private final String _sCmdLine;
        private final Container _eContainer;
        private final JavaImageFormat _eImgFmt;

        VideoFormat(String sGui, String sCmdLine, Container eContainer) {
           this(sGui, sCmdLine, eContainer, null);
        }

        VideoFormat(String sGui, String sCmdLine, Container eContainer, JavaImageFormat eDepends) {
            _sGui = sGui;
            _sCmdLine = sCmdLine;
            _eContainer = eContainer;
            _eImgFmt = eDepends;
        }

        public String toString() { return _sGui; }
        public String getCmdLine() { return _sCmdLine; }
        public Container getContainer() { return _eContainer; }
        public boolean isAvailable() {
            return _eImgFmt == null ? true : _eImgFmt.isAvailable();
        }

        public boolean canSaveAudio() { return _eContainer == Container.AVI; }

        public JpgQualities getDefaultCompression() {
            return _eImgFmt == null ? null : _eImgFmt.getDefaultCompression();
        }
        public List<JpgQualities> getCompressionOptions() {
            return _eImgFmt == null ? null : _eImgFmt.getCompressionQualityDescriptions();
        }
        public JavaImageFormat getImgFmt() { return _eImgFmt; }

        public boolean isCropable() {
            return this != IMGSEQ_DEMUX && this != IMGSEQ_MDEC;
        }
        public boolean hasDecodableQuality() { return isCropable(); }

        /////////////////////////////////////////////////////////

        public static VideoFormat fromCmdLine(String sCmdLine) {
            for (VideoFormat fmt : values()) {
                if (fmt.getCmdLine().equalsIgnoreCase(sCmdLine))
                    return fmt;
            }
            return null;
        }

        public static String getCmdLineList() {
            StringBuilder sb = new StringBuilder();
            for (VideoFormat fmt : values()) {
                if (fmt.isAvailable()) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append(fmt.getCmdLine());
                }
            }
            return sb.toString();
        }

        public static List<VideoFormat> getAvailable() {
            ArrayList<VideoFormat> avalable = new ArrayList<VideoFormat>();
            for (VideoFormat fmt : values()) {
                if (fmt.isAvailable())
                    avalable.add(fmt);
            }
            return avalable;
        }

    }

    public static final String PROP_VIDEO_FORMAT_LIST = "videoFormatList";
    private final List<VideoFormat> _imgFmtList = VideoFormat.getAvailable();
    public List<VideoFormat> getVideoFormatList() {
        return _imgFmtList;
    }

    public static final String PROP_VIDEO_FORMAT = "imageFormat";
    private VideoFormat _videoFormat;
    public VideoFormat getVideoFormat() {
        return _videoFormat;
    }
    public void setVideoFormat(VideoFormat val) {
        _videoFormat = val;
    }

    public static final String PROP_JPG_COMPRESSION_LIST = "jpgCompressionList";
    private List<JpgQualities> _jpgList = JpgQualities.getList();
    public List<JpgQualities> getJpgCompressionList() {
        return _jpgList;
    }

    public static final String PROP_JPG_COMPRESSION_OPTION = "jpgCompressionOption";
    private JpgQualities _jpgCompressionOption = JpgQualities.GOOD_QUALITY;
    public JpgQualities getJpgCompressionOption() {
        return _jpgCompressionOption;
    }
    public void setJpgCompressionOption(JpgQualities val) {
        if (_jpgList != null && _jpgList.contains(val)) {
            _jpgCompressionOption = val;
        }
    }

    public static final String PROP_CROP = "noCrop";
    private boolean _blnCrop = true;
    public boolean getCrop() {
        return _blnCrop;
    }
    public void setCrop(boolean val) {
        _blnCrop = val;
    }

    public int getParallelAudioCount() {
        return _sourceVidItem.getParallelAudioStreamCount();
    }

    private DiscItemAudioStream _parallelAudio = null;

    public DiscItemAudioStream getParallelAudio() {
        return _parallelAudio;
    }

    public boolean setParallelAudio(DiscItemAudioStream parallelAudio) {
        if (_sourceVidItem.isAudioVideoAligned(_sourceVidItem)) {
            _parallelAudio = parallelAudio;
            return true;
        } else {
            return false;
        }
    }

    /** Returns the new setting, if valid, otherwise null. */
    public DiscItemAudioStream setParallelAudioBySizeOrder(int iSizeIndex) {
        if (_sourceVidItem.hasAudio())
            return _parallelAudio = _sourceVidItem.getParallelAudioStream(iSizeIndex);
        else
            return null;
    }

    /** Returns the new setting, if valid, otherwise null. */
    public DiscItemAudioStream setParallelAudioByIndexNumber(int iIndex) {
        for (int i = 0; i < _sourceVidItem.getParallelAudioStreamCount(); i++) {
            DiscItemAudioStream audStream = _sourceVidItem.getParallelAudioStream(i);
            if (audStream.getIndex() == iIndex)
                return _parallelAudio = audStream;
        }
        return null;
    }

    public static enum DecodeQualities {
        LOW("Fast (lower quality)", "low"),
        HIGH("High quality (slower)", "high"),
        PSX("Exact PSX quality", "psx");

        public static String getCmdLineList() {
            StringBuilder sb = new StringBuilder();
            for (DecodeQualities dq : DecodeQualities.values()) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(dq.getCmdLine());
            }
            return sb.toString();
        }

        public static DecodeQualities fromCmdLine(String sCmdLine) {
            for (DecodeQualities dq : DecodeQualities.values()) {
                if (dq.getCmdLine().equals(sCmdLine))
                    return dq;
            }
            return null;
        }

        private final String _sGui;
        private final String _sCmdLine;

        private DecodeQualities(String sDescription, String sCmdLine) {
            _sGui = sDescription;
            _sCmdLine = sCmdLine;
        }

        public String getCmdLine() { return _sCmdLine; }
        public String toString() { return _sGui; }

        public static List<DecodeQualities> getList() {
            return Arrays.asList(DecodeQualities.values());
        }
    }
    public List<DecodeQualities> getDecodeQualities() {
        return DecodeQualities.getList();
    }

    public static final String PROP_DECODE_QUALITY = "decodeQuality";
    private DecodeQualities _decodeQuality = DecodeQualities.LOW;
    public DecodeQualities getDecodeQuality() {
        return _decodeQuality;
    }
    public void setDecodeQuality(DecodeQualities val) {
        _decodeQuality = val;
    }

    public static final String PROP_PRECISE_FRAME_TIMING = "preciseFrameTiming";
    private boolean _blnPreciseFrameTiming = false;
    public boolean getPreciseFrameTiming() {
        return _blnPreciseFrameTiming;
    }
    public void setPreciseFrameTiming(boolean val) {
        _blnPreciseFrameTiming = val;
    }

    public static final String PROP_PRECISE_AUDIOVIDEO_SYNC = "preciseAVSync";
    private boolean _blnPreciseAVSync = false;
    public boolean getPreciseAVSync() {
        return _blnPreciseAVSync;
    }
    public void setPreciseAVSync(boolean val) {
        _blnPreciseAVSync = val;
    }

    public static final String PROP_SAVE_START_FRAME = "saveStartFrame";
    private int _iSaveStartFrame;
    public int getSaveStartFrame() {
        return _iSaveStartFrame;
    }
    public void setSaveStartFrame(int val) {
        _iSaveStartFrame = val;
    }

    public static final String PROP_SAVE_END_FRAME = "saveEndFrame";
    private int _iSaveEndFrame;
    public int getSaveEndFrame() {
        return _iSaveEndFrame;
    }
    public void setSaveEndFrame(int val) {
        _iSaveEndFrame = val;
    }

    ////////////////////////////////////////////////////////////////////////////

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return null;
        
        ArgParser parser = new ArgParser("", false);

        IntHolder discSpeed = new IntHolder(-10);
        parser.addOption("-x %i {[1, 2]}", discSpeed);

        StringHolder vidfmt = new StringHolder();
        parser.addOption("-vidfmt,-vf %s", vidfmt);

        StringHolder jpg = null;
        JavaImageFormat JPG = JavaImageFormat.JPG;
        if (JavaImageFormat.JPG.isAvailable()) {
            jpg = new StringHolder();
            String sParam = "-jpg %s";
            parser.addOption(sParam, jpg);
        }

        StringHolder frames = new StringHolder();
        parser.addOption("-frame,-frames,-f %s", frames);

        BooleanHolder nocrop = new BooleanHolder(false);
        parser.addOption("-nocrop %v", nocrop); // only non demux & mdec formats

        StringHolder quality = new StringHolder();
        parser.addOption("-quality,-q %s", quality);

        BooleanHolder noaud = new BooleanHolder(false);
        parser.addOption("-noaud %v", noaud); // Only with AVI & audio

        BooleanHolder preciseav = new BooleanHolder(false);
        parser.addOption("-preciseav %v", preciseav); // Only with AVI & audio

        BooleanHolder precisefps = new BooleanHolder(false);
        parser.addOption("-precisefps %v", precisefps); // Mutually excusive with fps...

        // -------------------------
        String[] asRemain = null;
        asRemain = parser.matchAllArgs(asArgs, 0, 0);
        // -------------------------

        if (frames.value != null) {
            try {
                int iFrame = Integer.parseInt(frames.value);
                setSaveStartFrame(iFrame);
                setSaveEndFrame(iFrame);
                fbs.printlnNorm(String.format("Frames %d-%d",
                        getSaveStartFrame(), getSaveEndFrame()));
            } catch (NumberFormatException ex) {
                int[] aiRange = Misc.splitInt(frames.value, "-");
                if (aiRange != null && aiRange.length == 2) {
                    setSaveStartFrame(aiRange[0]);
                    setSaveEndFrame(aiRange[1]);
                    fbs.printlnNorm(String.format("Frames %d-%d",
                            getSaveStartFrame(), getSaveEndFrame()));
                } else {
                    fbs.printlnWarn("Invalid frame(s) " + frames.value);
                }
            }
        }

        if (vidfmt.value != null) {
            VideoFormat vf = VideoFormat.fromCmdLine(vidfmt.value);
            if (vf != null) {
                setVideoFormat(vf);
                fbs.printlnNorm("Format " + getVideoFormat());
            } else {
                fbs.printlnWarn("Invalid video format " + vidfmt.value);
            }
        }

        if (quality.value != null) {
            DecodeQualities dq = DecodeQualities.fromCmdLine(quality.value);
            if (dq != null) {
                setDecodeQuality(dq);
                fbs.printlnNorm("Using decode quality " + getDecodeQuality());
            } else {
                fbs.printlnWarn("Invalid decode quality " + quality.value);
            }
        }

        // make sure to process this after the video format is set
        if (jpg != null && jpg.value != null) {
            JpgQualities q = JpgQualities.fromCmdLine(jpg.value);
            if (q != null) {
                setJpgCompressionOption(q);
                fbs.printlnNorm("Jpg compression " + getJpgCompressionOption());
            } else {
                fbs.printlnWarn("Invalid jpg compression " + jpg.value);
            }
        }

        if (!nocrop.value != getCrop()) {
            fbs.printlnNorm("Not cropping");
        }
        setCrop(!nocrop.value);

        if (preciseav.value != getPreciseAVSync()) {
            fbs.printlnNorm("Precise Audio/Video sync");
        }
        setPreciseAVSync(preciseav.value);
        
        if (!noaud.value != getSaveAudio()) {
            fbs.printlnNorm("Not saving audio");
        }
        setSaveAudio(!noaud.value);
        
        if (discSpeed.value == 1) {
            setSingleSpeed(true);
            fbs.printlnNorm("Forcing single disc speed");
        } else if (discSpeed.value == 2) {
            setSingleSpeed(true);
            fbs.printlnNorm("Forcing double disc speed");
        }

        return asRemain;
    }

    public void printHelp(FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();

        tfb.setRowSpacing(1);

        tfb.print("-vidfmt,-vf <format>").tab().print("Output video format (default avi:mjpg). Options:");
        tfb.indent();
        for (VideoFormat fmt : VideoFormat.values()) {
            if (fmt.isAvailable()) {
                tfb.ln().print(fmt.getCmdLine());
            }
        }
        tfb.newRow();

        if (_sourceVidItem.hasAudio()) {
            tfb.print("-noaud").tab().print("Don't save audio.");
            tfb.newRow();
        }

        tfb.print("-quality,-q <quality>").tab().println("Decoding quality (default low). Options:")
                              .indent().print(DecodeQualities.getCmdLineList());
        tfb.newRow();

        JavaImageFormat JPG = JavaImageFormat.JPG;
        if (JPG.isAvailable()) {
            tfb.print("-"+JPG.getId()+" <quality>").tab()
                    .println("Quality when saving as jpg or avi:mjpg (default good). Options:")
                    .indent().print(JpgQualities.getCmdLineList());
            tfb.newRow();
        }

        tfb.print("-frame,-frames <frames>").tab().print("One frame, or range of frames to save.");
        if (_sourceVidItem.hasAudio()) {
            tfb.ln().indent().print("(audio isn't available when using this option)");
        }
        tfb.newRow();

        tfb.print("-x <disc speed>").tab().print("Force disc speed of 1 or 2.");

        if (_sourceVidItem.shouldBeCropped()) {
            tfb.newRow();
            tfb.print("-nocrop").tab().print("Don't crop data around unused frame edges.");
        }

        tfb.write(fbs);
    }

    
    public SectorMovieWriter openDemuxWriter() throws IOException {
        final MdecDecoder vidDecoder;
        switch (getDecodeQuality()) {
            case HIGH:
                vidDecoder = new MdecDecoder_double(new StephensIDCT(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case LOW:
                vidDecoder = new MdecDecoder_int(new simple_idct(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case PSX:
                vidDecoder = new MdecDecoder_int(new PsxMdecIDCT(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            default:
                throw new RuntimeException("Oops");
        }

        final String sBaseName = _sourceVidItem.getSuggestedBaseName();

        // TODO: add api for selecting parallel audio
        IAudioSectorDecoder audDecoder = null;
        if (getSaveAudio()) {
            boolean[] ablnSelectedAudio = new boolean[getParallelAudioCount()];
            Arrays.fill(ablnSelectedAudio, true);
            List<DiscItemAudioStream> parallelAud = _sourceVidItem.getParallelAudio(ablnSelectedAudio);
            audDecoder = new AudioStreamsCombiner(parallelAud, false, 1.0);
        }

        switch (getVideoFormat()) {
            case AVI_YUV:
                return new DecodedAviWriter_YV12(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(),
                        getCrop(),
                        getPreciseAVSync(),
                        audDecoder);
            case AVI_MJPG:
                return new DecodedAviWriter_MJPG(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(),
                        vidDecoder,
                        getCrop(),
                        getJpgCompressionOption().getQuality(),
                        getPreciseAVSync(),
                        audDecoder);
            case AVI_BMP:
                return new DecodedAviWriter_DIB(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(),
                        vidDecoder,
                        getCrop(),
                        getPreciseAVSync(),
                        audDecoder);
            case IMGSEQ_DEMUX:
                return new DemuxSequenceWriter(_sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame());
            case IMGSEQ_MDEC:
                return new MdecSequenceWriter(_sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame());
            case IMGSEQ_JPG:
            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
                return new DecodedJavaImageSequenceWriter(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        vidDecoder, getCrop(),
                        getVideoFormat().getImgFmt());

            case YUV4MPEG2_YUV:
                return new DecodedYuv4mpeg2Writer(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(), _blnCrop);

                
        } // end case
        throw new UnsupportedOperationException(getVideoFormat() + " not implemented yet.");
    }

}
