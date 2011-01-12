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

package jpsxdec.discitems.savers;

import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.IDiscItemSaver;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jpsxdec.discitems.AudioStreamsCombiner;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.discitems.DiscItemAudioStream;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.discitems.savers.VideoSavers.*;
import jpsxdec.psxvideo.mdec.MdecDecoder;
import jpsxdec.psxvideo.mdec.MdecDecoder_double;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate;
import jpsxdec.psxvideo.mdec.MdecDecoder_int;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.psxvideo.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.psxvideo.mdec.idct.simple_idct;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;
import jpsxdec.util.TabularFeedback;

/** Manages all possible options for saving PSX video. */
public class VideoSaverBuilder extends DiscItemSaverBuilder {

    private static final Logger log = Logger.getLogger(VideoSaverBuilder.class.getName());

    private final DiscItemVideoStream _sourceVidItem;

    public VideoSaverBuilder(DiscItemVideoStream vidItem) {
        _sourceVidItem = vidItem;
        if (_sourceVidItem.hasAudio()) {
            _parallelAudio = _sourceVidItem.getParallelAudioStreams();
            _ablnParallelAudio = new boolean[_sourceVidItem.getParallelAudioStreamCount()];
        } else {
            _parallelAudio = new ArrayList<DiscItemAudioStream>(0);
            _ablnParallelAudio = new boolean[0];
        }
        resetToDefaults();
    }

    public void resetToDefaults() {
        setVideoFormat(VideoFormat.AVI_MJPG);
        if (_sourceVidItem.hasAudio()) {
            List<DiscItemAudioStream> defaultAud = _sourceVidItem.getLongestNonIntersectingAudioStreams();
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                _ablnParallelAudio[i] = defaultAud.contains(_parallelAudio.get(i));
            }
        }
        setAudioVolume(1.0);
        setCrop(true);
        setDecodeQuality(DecodeQualities.LOW);
        setJpgCompression(0.75f);
        setPreciseAVSync(false);
        setPreciseFrameTiming(false);
        setSaveStartFrame(_sourceVidItem.getStartFrame());
        setSaveEndFrame(_sourceVidItem.getEndFrame());
        setSingleSpeed(false);
    }

    public boolean copySettings(DiscItemSaverBuilder otherBuilder) {
        if (otherBuilder instanceof VideoSaverBuilder) {
            VideoSaverBuilder other = (VideoSaverBuilder) otherBuilder;
            // only copy valid settings
            other.setVideoFormat(getVideoFormat());
            //other.setParallelAudio(getParallelAudio());
            if (getAudioVolume_enabled())
                other.setAudioVolume(getAudioVolume());
            if (getCrop_enabled())
                other.setCrop(getCrop());
            if (getDecodeQuality_enabled())
                other.setDecodeQuality(getDecodeQuality());
            if (getJpgCompression_enabled())
                other.setJpgCompression(getJpgCompression());
            if (getPreciseAVSync_enabled())
                other.setPreciseAVSync(getPreciseAVSync());
            if (getPreciseFrameTiming_enabled())
                other.setPreciseFrameTiming(getPreciseFrameTiming());
            if (hasAudio() && !getSavingAudio())
                other.setParallelAudioNone();
            if (getSingleSpeed_enabled())
                other.setSingleSpeed(getSingleSpeed());
            return true;
        }
        return false;
    }

    public DiscItemSaverBuilderGui getOptionPane() {
        return new VideoSaverBuilderGui(this);
    }

    // .........................................................................

    public String getOutputBaseName() {
        return _sourceVidItem.getSuggestedBaseName().getPath();
    }

    public String getOutputPostfixStart() {
        return getVideoFormat().formatPostfix(_sourceVidItem, getStartFrame());
    }

    public String getOutputPostfixEnd() {
        return getVideoFormat().formatPostfix(_sourceVidItem, getEndFrame());
    }

    // .........................................................................

    private boolean _blnSingleSpeed;
    public boolean getSingleSpeed() {
        switch (findDiscSpeed()) {
            case 1:
                return true;
            case 2:
                return false;
            default:
                return _blnSingleSpeed;
        }
    }
    private int findDiscSpeed() {
        int iDiscSpeed = _sourceVidItem.getDiscSpeed();
        if (iDiscSpeed < 1) {
            // if disc item doesn't know speed, try parallel audio item
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                if (_ablnParallelAudio[i]) {
                    iDiscSpeed = _parallelAudio.get(i).getDiscSpeed();
                    break;
                }
            }
        }
        return iDiscSpeed;
    }
    public void setSingleSpeed(boolean val) {
        _blnSingleSpeed = val;
        firePossibleChange();
    }
    public boolean getSingleSpeed_enabled() {
        return getVideoFormat().getContainer() == Container.AVI &&
               (findDiscSpeed() < 1);
    }
    public Fraction getFps() {
        return Fraction.divide( 
                getSingleSpeed() ? 75 : 150,
                _sourceVidItem.getSectorsPerFrame());
    }

    // .........................................................................

    private double _dblAudioVolume = 1.0;
    public double getAudioVolume() {
        return _dblAudioVolume;
    }
    public void setAudioVolume(double val) {
        _dblAudioVolume = Math.min(Math.max(0.0, val), 1.0);
        firePossibleChange();
    }
    public boolean getAudioVolume_enabled() {
        return getSavingAudio();
    }

    // .........................................................................

    public static enum Container {
        AVI,
        IMGSEQ
    }

    public static enum VideoFormat {
        AVI_MJPG        ("AVI: Compressed (MJPG)"  , "avi:mjpg", Container.AVI, JavaImageFormat.JPG) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return ".avi";
            }
        },
        AVI_RGB         ("AVI: Uncompressed RGB"   , "avi:rgb", Container.AVI) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return ".avi";
            }
        },
        AVI_YUV         ("AVI: YUV"                , "avi:yuv", Container.AVI) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return ".avi";
            }
        },
        AVI_JYUV         ("AVI: YUV with [0-255] range", "avi:jyuv", Container.AVI) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return ".avi";
            }
        },
        IMGSEQ_PNG      ("Image sequence: png"     , "png", Container.IMGSEQ, JavaImageFormat.PNG) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return digitBlk(vid, iFrame) + ".png";
            }
        },
        IMGSEQ_JPG      ("Image sequence: jpg"     , "jpg", Container.IMGSEQ, JavaImageFormat.JPG) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return digitBlk(vid, iFrame) + ".jpg";
            }
        },
        IMGSEQ_BMP      ("Image sequence: bmp"     , "bmp", Container.IMGSEQ, JavaImageFormat.BMP) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return digitBlk(vid, iFrame) + ".bmp";
            }
        },
        IMGSEQ_DEMUX    ("Image sequence: bitstream"   , "bs", Container.IMGSEQ) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid, iFrame) + ".bs";
            }
        },
        IMGSEQ_MDEC     ("Image sequence: mdec"    , "mdec", Container.IMGSEQ) {
            public String formatPostfix(DiscItemVideoStream vid, int iFrame) {
                return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid, iFrame) + ".mdec";
            }
        },
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

        public JavaImageFormat getImgFmt() { return _eImgFmt; }

        public boolean isCropable() {
            return this != IMGSEQ_DEMUX && this != IMGSEQ_MDEC;
        }
        public boolean hasCompression() {
            return _eImgFmt == null ? false : _eImgFmt.hasCompression();
        }

        abstract public String formatPostfix(DiscItemVideoStream vid, int iStartFrame);
        
        /////////////////////////////////////////////////////////

        private static String digitBlk(DiscItemVideoStream vid, int iFrame) {
            // http://stackoverflow.com/questions/554521/how-can-i-count-the-digits-in-an-integer-without-a-string-cast
            int iDigitCount = (vid.getEndFrame() == 0) ? 1 : (int)Math.log10(vid.getEndFrame()) + 1;
            return String.format("[%0" + String.valueOf(iDigitCount) + "d]", iFrame);
        }

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

    private final List<VideoFormat> _imgFmtList = VideoFormat.getAvailable();
    public VideoFormat getVideoFormat_listItem(int i) {
        return _imgFmtList.get(i);
    }
    public int getVideoFormat_listSize() {
        return _imgFmtList.size();
    }

    private VideoFormat _videoFormat;
    public VideoFormat getVideoFormat() {
        return _videoFormat;
    }
    public void setVideoFormat(VideoFormat val) {
        _videoFormat = val;
        firePossibleChange();
    }

    // .........................................................................

    private float _jpgCompressionOption = 0.75f;
    public float getJpgCompression() {
        return _jpgCompressionOption;
    }
    public void setJpgCompression(float val) {
        _jpgCompressionOption = Math.max(Math.min(val, 1.f), 0.f);
        firePossibleChange();
    }

    public boolean getJpgCompression_enabled() {
        return getVideoFormat().hasCompression();
    }

    // .........................................................................

    private boolean _blnCrop = true;
    public boolean getCrop() {
        if (getCrop_enabled())
            return _blnCrop;
        else
            return false;
    }
    public void setCrop(boolean val) {
        _blnCrop = val;
        firePossibleChange();
    }
    public boolean getCrop_enabled() {
        return _sourceVidItem.shouldBeCropped() && 
               getVideoFormat() != VideoFormat.IMGSEQ_DEMUX && 
               getVideoFormat() != VideoFormat.IMGSEQ_MDEC;
    }

    public int getWidth() {
        if (!getCrop_enabled() || getCrop())
            return _sourceVidItem.getWidth();
        else
            return (_sourceVidItem.getWidth() + 15) & ~15;
    }

    public int getHeight() {
        if (!getCrop_enabled() || getCrop())
            return _sourceVidItem.getHeight();
        else
            return (_sourceVidItem.getHeight() + 15) & ~15;
    }

    // .........................................................................

    private final List<DiscItemAudioStream> _parallelAudio;
    private final boolean[] _ablnParallelAudio;

    public int getParallelAudioCount() {
        return _sourceVidItem.getParallelAudioStreamCount();
    }
    public DiscItemAudioStream getParallelAudio(int i) {
        return _parallelAudio.get(i);
    }

    public boolean getParallelAudio_selected(int i) {
        return _ablnParallelAudio[i];
    }

    public void setParallelAudio(DiscItemAudioStream parallelAudio, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        setParallelAudio(_parallelAudio.indexOf(parallelAudio), blnSelected);
    }

    public void setParallelAudio(int iIndex, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        if (iIndex < 0 || iIndex >= _sourceVidItem.getParallelAudioStreamCount())
            return;

        DiscItemAudioStream aud = _parallelAudio.get(iIndex);
        for (int i = 0; i < _ablnParallelAudio.length; i++) {
            if (_ablnParallelAudio[i]) {
                DiscItemAudioStream other = _parallelAudio.get(i);
                // if it overlaps or has a different format
                if (aud.overlaps(other) || !aud.hasSameFormat(other)) {
                    // disable it
                    _ablnParallelAudio[i] = false;
                }
            }
        }

        _ablnParallelAudio[iIndex] = blnSelected;
        firePossibleChange();
    }

    public void setParallelAudioNone() {
        for (int i = 0; i < getParallelAudioCount(); i++) {
            setParallelAudio(i, false);
        }
    }

    public boolean getParallelAudio_enabled() {
        return getSavingAudio();
    }

    public boolean hasAudio() {
        return _sourceVidItem.hasAudio();
    }

    public boolean getSavingAudio() {
        for (boolean b : _ablnParallelAudio) {
            if (b)
                return true;
        }
        return false;
    }

    // .........................................................................

    public static enum DecodeQualities {
        LOW("Fast (lower quality)", "low"),
        HIGH("High quality (slower)", "high"),
        HIGH_PLUS("High quality + interpolation (slowest)", "high+"),
        PSX("Nearly exact PSX quality", "psx");

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
    }

    public int getDecodeQuality_listSize() {
        if (getVideoFormat() == VideoFormat.AVI_YUV || getVideoFormat() == VideoFormat.AVI_JYUV)
            return 1;
        return DecodeQualities.values().length;
    }
    public DecodeQualities getDecodeQuality_listItem(int i) {
        if (getVideoFormat() == VideoFormat.AVI_YUV || getVideoFormat() == VideoFormat.AVI_JYUV)
            return DecodeQualities.HIGH;
        return DecodeQualities.values()[i];
    }

    private DecodeQualities _decodeQuality = DecodeQualities.LOW;
    public DecodeQualities getDecodeQuality() {
        if (getVideoFormat() == VideoFormat.AVI_YUV || getVideoFormat() == VideoFormat.AVI_JYUV)
            return DecodeQualities.HIGH;
        return _decodeQuality;
    }
    public void setDecodeQuality(DecodeQualities val) {
        _decodeQuality = val;
        firePossibleChange();
    }

    public boolean getDecodeQuality_enabled() {
        return getVideoFormat() != VideoFormat.IMGSEQ_DEMUX &&
               getVideoFormat() != VideoFormat.IMGSEQ_MDEC;
    }

    // .........................................................................

    private boolean _blnPreciseFrameTiming = false;
    public boolean getPreciseFrameTiming() {
        return _blnPreciseFrameTiming;
    }
    public void setPreciseFrameTiming(boolean val) {
        _blnPreciseFrameTiming = val;
        firePossibleChange();
    }
    public boolean getPreciseFrameTiming_enabled() {
        // this may be variable in the future
        // but for now this feature isn't implemented
        return false;
    }

    // .........................................................................
    
    private boolean _blnPreciseAVSync = false;
    public boolean getPreciseAVSync() {
        if (getPreciseAVSync_enabled())
            return _blnPreciseAVSync;
        return false;
    }
    public void setPreciseAVSync(boolean val) {
        _blnPreciseAVSync = val;
        firePossibleChange();
    }

    public boolean getPreciseAVSync_enabled() {
        return getSavingAudio();
    }

    // .........................................................................
    
    private int _iSaveStartFrame;
    public int getSaveStartFrame() {
        return _iSaveStartFrame;
    }
    public void setSaveStartFrame(int val) {
        _iSaveStartFrame = Math.max(val, _sourceVidItem.getStartFrame());
        _iSaveEndFrame = Math.max(_iSaveEndFrame, _iSaveStartFrame);
        firePossibleChange();
    }

    public int getStartFrame() {
        return _sourceVidItem.getStartFrame();
    }

    // .........................................................................
    
    private int _iSaveEndFrame;
    public int getSaveEndFrame() {
        return _iSaveEndFrame;
    }
    public void setSaveEndFrame(int val) {
        _iSaveEndFrame = Math.min(val, _sourceVidItem.getEndFrame());
        _iSaveStartFrame = Math.min(_iSaveEndFrame, _iSaveStartFrame);
        firePossibleChange();
    }

    public int getEndFrame() {
        return _sourceVidItem.getEndFrame();
    }

    ////////////////////////////////////////////////////////////////////////////

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return null;
        
        ArgParser parser = new ArgParser("", false);

        IntHolder discSpeed = new IntHolder(-10);
        parser.addOption("-ds %i {[1, 2]}", discSpeed);

        StringHolder vidfmt = new StringHolder();
        parser.addOption("-vidfmt,-vf %s", vidfmt);

        IntHolder jpg = null;
        if (JavaImageFormat.JPG.isAvailable()) {
            jpg = new IntHolder(-999);
            parser.addOption("-jpg %i", jpg);
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
            } catch (NumberFormatException ex) {
                int[] aiRange = Misc.splitInt(frames.value, "-");
                if (aiRange != null && aiRange.length == 2) {
                    setSaveStartFrame(aiRange[0]);
                    setSaveEndFrame(aiRange[1]);
                } else {
                    fbs.printlnWarn("Invalid frame(s) " + frames.value);
                }
            }
        }

        if (vidfmt.value != null) {
            VideoFormat vf = VideoFormat.fromCmdLine(vidfmt.value);
            if (vf != null) {
                setVideoFormat(vf);
            } else {
                fbs.printlnWarn("Invalid video format " + vidfmt.value);
            }
        }

        if (quality.value != null) {
            DecodeQualities dq = DecodeQualities.fromCmdLine(quality.value);
            if (dq != null) {
                setDecodeQuality(dq);
            } else {
                fbs.printlnWarn("Invalid decode quality " + quality.value);
            }
        }

        // make sure to process this after the video format is set
        if (jpg != null && jpg.value != -999) {
            if (jpg.value >= 0 && jpg.value <= 100) {
                setJpgCompression(jpg.value / 100.f);
            } else {
                fbs.printlnWarn("Invalid jpg compression " + jpg.value);
            }
        }

        setCrop(!nocrop.value);

        setPreciseAVSync(preciseav.value);

        if (noaud.value)
            setParallelAudioNone();

        if (discSpeed.value == 1) {
            setSingleSpeed(true);
        } else if (discSpeed.value == 2) {
            setSingleSpeed(false);
        }

        return asRemain;
    }

    public void printSelectedOptions(PrintStream ps) {
        ps.format("Disc speed: %s (%s fps)", getSingleSpeed() ? "1x" : "2x", DiscItemVideoStream.formatFps(getFps()));
        ps.println();
        ps.println("Video format: " + getVideoFormat());
        if (getJpgCompression_enabled())
            ps.println("JPG quality: " + Math.round(getJpgCompression()*100) + "%");
        ps.println("Frames: " + getSaveStartFrame() + "-" + getSaveEndFrame());
        if (getCrop_enabled())
            ps.println("Cropping: " + (getCrop() ? "Yes" : "No"));
        if (getPreciseFrameTiming_enabled())
            ps.println("Precise FPS: " + (getPreciseFrameTiming() ? "Yes" : "No"));
        if (getDecodeQuality_enabled())
            ps.println("Decode quality: " + getDecodeQuality());
        /*
        if (getSaveAudio_enabled())
            ps.println("Saving audio: " + (getSaveAudio() ? "Yes" : "No"));
        */
        if (getParallelAudio_enabled()) {
            ps.println("With audio item(s):");
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                if (_ablnParallelAudio[i])
                    ps.println(_parallelAudio.get(i));
            }
        }
        if (getPreciseAVSync_enabled())
            ps.println("Precise audio/video sync: " + (getPreciseAVSync() ? "Yes" : "No"));

        String sStartFile = getVideoFormat().formatPostfix(_sourceVidItem, getStartFrame());
        String sEndFile = getVideoFormat().formatPostfix(_sourceVidItem, getEndFrame());
        if (sStartFile.equals(sEndFile)) {
            ps.println("Saving as: " + getOutputBaseName() + sStartFile);
        } else {
            ps.println("Saving as: " + getOutputBaseName() + sStartFile + " - " + getOutputBaseName() + sEndFile);
        }
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
            tfb.print("-jpg <between 1 and 100>").tab()
                    .println("Output quality when saving as jpg or avi:mjpg")
                    .print("(default is 75).");
            tfb.newRow();
        }

        tfb.print("-frame,-frames <frames>").tab().print("One frame, or range of frames to save.");
        if (_sourceVidItem.hasAudio()) {
            tfb.ln().indent().print("(audio is disabled when using this option)");
        }

        if (getSingleSpeed_enabled()) {
            tfb.newRow();
            tfb.print("-ds <disc speed>").tab().print("Specify 1 or 2 if disc speed is undetermined.");
        }

        if (_sourceVidItem.shouldBeCropped()) {
            tfb.newRow();
            tfb.print("-nocrop").tab().print("Don't crop data around unused frame edges.");
        }

        tfb.write(fbs);
    }

    
    public IDiscItemSaver makeSaver() {

        final MdecDecoder vidDecoder;
        switch (getDecodeQuality()) {
            case HIGH_PLUS:
                vidDecoder = new MdecDecoder_double_interpolate(new PsxMdecIDCT_double(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case HIGH:
                vidDecoder = new MdecDecoder_double(new PsxMdecIDCT_double(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case LOW:
                vidDecoder = new MdecDecoder_int(new simple_idct(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case PSX:
                vidDecoder = new MdecDecoder_int(new PsxMdecIDCT_int(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            default:
                throw new RuntimeException("Oops");
        }
        
        ISectorAudioDecoder audDecoder = null;
        if (getSavingAudio()) {
            ArrayList<DiscItemAudioStream> parallelAudio = new ArrayList<DiscItemAudioStream>();
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                if (_ablnParallelAudio[i])
                    parallelAudio.add(_parallelAudio.get(i));
            }
            if (parallelAudio.size() == 1)
                audDecoder = parallelAudio.get(0).makeDecoder(getAudioVolume());
            else
                audDecoder = new AudioStreamsCombiner(parallelAudio, getAudioVolume());
        }

        VideoSaverBuilderSnapshot snap = new VideoSaverBuilderSnapshot(_sourceVidItem,
                 _sourceVidItem.getSuggestedBaseName(),
                vidDecoder, audDecoder, this);
        
        VideoSaver writer;
        switch (snap.videoFormat) {
            case AVI_JYUV:
                writer = new DecodedAviWriter_JYV12(snap); break;
            case AVI_YUV:
                writer = new DecodedAviWriter_YV12(snap); break;
            case AVI_MJPG:
                writer = new DecodedAviWriter_MJPG(snap); break;
            case AVI_RGB:
                writer = new DecodedAviWriter_DIB(snap); break;
            case IMGSEQ_DEMUX:
                writer = new BitstreamSequenceWriter(snap); break;
            case IMGSEQ_MDEC:
                writer = new MdecSequenceWriter(snap); break;
            case IMGSEQ_JPG:
            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
                writer = new DecodedJavaImageSequenceWriter(snap); break;
            default:
                throw new UnsupportedOperationException(getVideoFormat() + " not implemented yet.");
        } // end case
        return writer;

    }

}
