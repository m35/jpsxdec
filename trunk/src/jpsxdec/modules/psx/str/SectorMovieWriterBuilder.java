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

package jpsxdec.modules.psx.str;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.modules.psx.str.SectorMovieWriters.*;
import jpsxdec.modules.psx.video.mdec.MdecDecoder;
import jpsxdec.modules.psx.video.mdec.MdecDecoder_double;
import jpsxdec.modules.psx.video.mdec.MdecDecoder_double_interpolate;
import jpsxdec.modules.psx.video.mdec.MdecDecoder_int;
import jpsxdec.modules.psx.video.mdec.idct.PsxMdecIDCT_int;
import jpsxdec.modules.psx.video.mdec.idct.PsxMdecIDCT_double;
import jpsxdec.modules.psx.video.mdec.idct.simple_idct;
import jpsxdec.modules.xa.AudioStreamsCombiner;
import jpsxdec.modules.xa.IAudioSectorDecoder;
import jpsxdec.modules.xa.DiscItemAudioStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;
import jpsxdec.util.TabularFeedback;


public class SectorMovieWriterBuilder  {

    private static final Logger log = Logger.getLogger(SectorMovieWriterBuilder.class.getName());

    private final DiscItemSTRVideo _sourceVidItem;

    public SectorMovieWriterBuilder(DiscItemSTRVideo vidItem) {
        _sourceVidItem = vidItem;
        resetToDefaults();
    }

    public void resetToDefaults() {
        setVideoFormat(VideoFormat.AVI_MJPG);
        _blnSaveAudio = _sourceVidItem.hasAudio();
        setSaveStartFrame(_sourceVidItem.getStartFrame());
        setSaveEndFrame(_sourceVidItem.getEndFrame());
        setParallelAudioBySizeOrder(0);
        // TODO: finish
    }

    // .........................................................................

    private WeakHashMap<ChangeListener, Boolean> _changeListeners;
    private ChangeEvent _event;

    public void addChangeListener(ChangeListener listener) {
        if (_changeListeners == null)
            _changeListeners = new WeakHashMap<ChangeListener, Boolean>();
        _changeListeners.put(listener, Boolean.TRUE);
    }

    public void removeChangeListener(ChangeListener listener) {
        if (_changeListeners == null)
            return;
        _changeListeners.remove(listener);
    }

    private void firePossibleChange() {
        if (_changeListeners == null || _changeListeners.size() == 0)
            return;
        if (_event == null)
            _event = new ChangeEvent(this);
        for (ChangeListener listener : _changeListeners.keySet()) {
            listener.stateChanged(_event);
        }
    }

    // .........................................................................

    private String _sOutputBaseName;
    public String getOutputBaseName() {
        if (_sOutputBaseName == null)
            return _sourceVidItem.getSuggestedBaseName();
        else
            return _sOutputBaseName;
    }

    public void setOutputBaseName(String sName) {
        _sOutputBaseName = sName;
        firePossibleChange();
    }

    public String getOutputPostfixName() {
        return String.format(getVideoFormat().ext(_sourceVidItem), getSaveStartFrame());
    }

    // .........................................................................

    private boolean _blnSingleSpeed;
    public boolean getSingleSpeed() {
        switch (_sourceVidItem.getDiscSpeed()) {
            case 1:
                return true;
            case 2:
                return false;
            default:
                // if disc item doesn't know speed, try parallel audio item
                if (getSaveAudio()) {
                    switch (getParallelAudio().getDiscSpeed()) {
                        case 1:
                            return true;
                        case 2:
                            return false;
                    }
                }
        }
        return _blnSingleSpeed;
    }
    public void setSingleSpeed(boolean val) {
        _blnSingleSpeed = val;
        firePossibleChange();
    }
    public boolean getSingleSpeed_enabled() {
        return getVideoFormat().getContainer() == Container.AVI &&
               (_sourceVidItem.getDiscSpeed() < 1 &&
                getSaveAudio() &&
                getParallelAudio().getDiscSpeed() < 2);
    }
    public Fraction getFps() {
        return Fraction.divide( 
                getSingleSpeed() ? 75 : 150,
                _sourceVidItem.getSectorsPerFrame());
    }

    // .........................................................................

    private boolean _blnSaveAudio;
    public boolean getSaveAudio() {
        if (getSaveAudio_enabled())
            return _blnSaveAudio;
        else
            return false;
    }
    public void setSaveAudio(boolean val) {
        _blnSaveAudio = val;
        firePossibleChange();
    }
    public boolean getSaveAudio_enabled() {
        if (!_sourceVidItem.hasAudio()) return false;
        if (getSaveStartFrame() != _sourceVidItem.getStartFrame()) return false;
        if (!getVideoFormat().canSaveAudio()) return false;
        return true;
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
        return getSaveAudio();
    }

    // .........................................................................

    public static enum Container {
        AVI,
        IMGSEQ
    }

    public static enum VideoFormat {
        AVI_MJPG        ("AVI: Compressed (MJPG)"  , "avi:mjpg", Container.AVI, JavaImageFormat.JPG) {
            public String ext(DiscItemSTRVideo vid) { return ".avi"; }
        },
        AVI_BMP         ("AVI: Uncompressed RGB"   , "avi:rgb", Container.AVI) {
            public String ext(DiscItemSTRVideo vid) { return ".avi"; }
        },
        AVI_YUV         ("AVI: YUV"                , "avi:yuv", Container.AVI) {
            public String ext(DiscItemSTRVideo vid) { return ".avi"; }
        },
        IMGSEQ_PNG      ("Image sequence: png"     , "png", Container.IMGSEQ, JavaImageFormat.PNG) {
            public String ext(DiscItemSTRVideo vid) { return digitBlk(vid) + ".png"; }
        },
        IMGSEQ_JPG      ("Image sequence: jpg"     , "jpg", Container.IMGSEQ, JavaImageFormat.JPG) {
            public String ext(DiscItemSTRVideo vid) { return digitBlk(vid) + ".jpg"; }
        },
        IMGSEQ_BMP      ("Image sequence: bmp"     , "bmp", Container.IMGSEQ, JavaImageFormat.BMP) {
            public String ext(DiscItemSTRVideo vid) { return digitBlk(vid) + ".bmp"; }
        },
        //IMGSEQ_RAW      ("Image sequence: raw"     , "raw", Container.IMGSEQ),
        //IMGSEQ_YUV      ("Image sequence: yuv"     , "yuv", Container.IMGSEQ),
        //IMGSEQ_PSXYUV   ("Image sequence: Raw PSX yuv", "psxyuv", Container.IMGSEQ),
        IMGSEQ_DEMUX    ("Image sequence: demux"   , "demux", Container.IMGSEQ) {
            public String ext(DiscItemSTRVideo vid) { return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid) + ".demux"; }
        },
        IMGSEQ_MDEC     ("Image sequence: mdec"    , "mdec", Container.IMGSEQ) {
            public String ext(DiscItemSTRVideo vid) { return "_" + vid.getWidth() + "x" + vid.getHeight() + digitBlk(vid) + ".mdec"; }
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

        abstract public String ext(DiscItemSTRVideo vid);

        /////////////////////////////////////////////////////////

        private static String digitBlk(DiscItemSTRVideo vid) {
            return "[%0" + String.valueOf(String.valueOf(vid.getEndFrame()).length()) + "d]";
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

    public int getParallelAudio_listSize() {
        return _sourceVidItem.getParallelAudioStreamCount();
    }
    public DiscItemAudioStream getParallelAudio_listItem(int i) {
        return _sourceVidItem.getParallelAudioStream(i);
    }

    private DiscItemAudioStream _parallelAudio = null;
    public DiscItemAudioStream getParallelAudio() {
        if (!_sourceVidItem.hasAudio()) return null;
        if (_parallelAudio == null) return _sourceVidItem.getParallelAudioStream(0);
        return _parallelAudio;
    }

    public void setParallelAudio(DiscItemAudioStream parallelAudio) {
        if (_sourceVidItem.isAudioVideoAligned(_sourceVidItem)) {
            _parallelAudio = parallelAudio;
            firePossibleChange();
        }
    }

    public void setParallelAudioBySizeOrder(int iSizeIndex) {
        if (_sourceVidItem.hasAudio()) {
            _parallelAudio = _sourceVidItem.getParallelAudioStream(iSizeIndex);
            firePossibleChange();
        }
    }

    public void setParallelAudioByIndexNumber(int iIndex) {
        for (int i = 0; i < _sourceVidItem.getParallelAudioStreamCount(); i++) {
            DiscItemAudioStream audStream = _sourceVidItem.getParallelAudioStream(i);
            if (audStream.getIndex() == iIndex) {
                _parallelAudio = audStream;
                firePossibleChange();
                break;
            }
        }
    }

    public boolean getParallelAudio_enabled() {
        return getSaveAudio();
    }

    // .........................................................................

    public static enum DecodeQualities {
        LOW("Fast (lower quality)", "low"),
        HIGH("High quality (slower)", "high"),
        HIGH_PLUS("High quality + interplation (slowest)", "high+"),
        PSX("(not really) Exact PSX quality", "psx");

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
        if (getVideoFormat() == VideoFormat.AVI_YUV)
            return 1;
        return DecodeQualities.values().length;
    }
    public DecodeQualities getDecodeQuality_listItem(int i) {
        if (getVideoFormat() == VideoFormat.AVI_YUV)
            return DecodeQualities.HIGH;
        return DecodeQualities.values()[i];
    }

    private DecodeQualities _decodeQuality = DecodeQualities.LOW;
    public DecodeQualities getDecodeQuality() {
        if (getVideoFormat() == VideoFormat.AVI_YUV)
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
        return getSaveAudio();
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
        parser.addOption("-x %i {[1, 2]}", discSpeed);

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
        
        setSaveAudio(!noaud.value);

        if (discSpeed.value == 1) {
            setSingleSpeed(true);
        } else if (discSpeed.value == 2) {
            setSingleSpeed(false);
        }

        printSelectedOptions(fbs);

        return asRemain;
    }

    public void printSelectedOptions(FeedbackStream fbs) {
        fbs.format("Disc speed: %s (%1.3f fps)", getSingleSpeed() ? "1x" : "2x", getFps().asDouble());
        fbs.println();
        fbs.println("Video format: " + getVideoFormat());
        fbs.println("Frames: " + getSaveStartFrame() + "-" + getSaveEndFrame());
        if (getCrop_enabled())
            fbs.println("Cropping: " + (getCrop() ? "Yes" : "No"));
        if (getPreciseFrameTiming_enabled())
            fbs.println("Precise FPS: " + (getPreciseFrameTiming() ? "Yes" : "No"));
        if (getDecodeQuality_enabled())
            fbs.println("Decode quality: " + getDecodeQuality());
        if (getJpgCompression_enabled())
            fbs.println("JPG compression: " + getJpgCompression());

        if (getSaveAudio_enabled())
            fbs.println("Saving audio: " + (getSaveAudio() ? "Yes" : "No"));

        if (getParallelAudio_enabled()) {
            fbs.println("Audio item:");
            fbs.println(getParallelAudio());
        }
        if (getPreciseAVSync_enabled())
            fbs.println("Precise audio/video sync: " + (getPreciseAVSync() ? "Yes" : "No"));

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
            tfb.print("-x <disc speed>").tab().print("Specify 1 or 2 if disc speed is undetermined.");
        }

        if (_sourceVidItem.shouldBeCropped()) {
            tfb.newRow();
            tfb.print("-nocrop").tab().print("Don't crop data around unused frame edges.");
        }

        tfb.write(fbs);
    }

    
    public SectorMovieWriter openMovieWriter() throws IOException {
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

        final String sBaseName = _sourceVidItem.getSuggestedBaseName();

        // TODO: add api for selecting parallel audio
        IAudioSectorDecoder audDecoder = null;
        if (getSaveAudio()) {
            boolean[] ablnSelectedAudio = new boolean[getParallelAudio_listSize()];
            Arrays.fill(ablnSelectedAudio, true);
            List<DiscItemAudioStream> parallelAud = _sourceVidItem.getParallelAudio(ablnSelectedAudio);
            audDecoder = new AudioStreamsCombiner(parallelAud, false, getAudioVolume());
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
                        getJpgCompression(),
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

        } // end case
        throw new UnsupportedOperationException(getVideoFormat() + " not implemented yet.");
    }

}
