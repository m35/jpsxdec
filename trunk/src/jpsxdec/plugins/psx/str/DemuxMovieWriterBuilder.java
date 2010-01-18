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

import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.xa.PCM16bitAudioWriter;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.swing.JFrame;
import jpsxdec.MainCommandLineParser;
import jpsxdec.formats.JavaImageFormat;
import jpsxdec.formats.JavaImageFormat.JpgQualities;
import jpsxdec.formats.RgbIntImage;
import jpsxdec.formats.Yuv4mpeg2;
import jpsxdec.formats.Yuv4mpeg2Writer;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.ProgressListener;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.aviwriter.AviWriter;
import jpsxdec.plugins.psx.video.decode.DemuxFrameUncompressor;
import jpsxdec.plugins.psx.video.decode.UncompressionException;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_double;
import jpsxdec.plugins.psx.video.mdec.MdecDecoder_int;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream;
import jpsxdec.plugins.psx.video.mdec.MdecInputStream.MdecCode;
import jpsxdec.plugins.psx.video.mdec.idct.PsxMdecIDCT;
import jpsxdec.plugins.psx.video.mdec.idct.StephensIDCT;
import jpsxdec.plugins.psx.video.mdec.idct.simple_idct;
import jpsxdec.plugins.xa.IDiscItemAudioSectorDecoder;
import jpsxdec.plugins.xa.IDiscItemAudioStream;
import jpsxdec.util.AudioOutputStream;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.IO;
import jpsxdec.util.Misc;
import jpsxdec.util.TabularFeedback;


public class DemuxMovieWriterBuilder  {

    private static final Logger log = Logger.getLogger(DemuxMovieWriterBuilder.class.getName());

    private final DiscItemSTRVideo _sourceVidItem;

    public DemuxMovieWriterBuilder(DiscItemSTRVideo vidItem) {
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
        IDiscItemAudioStream[] aoAudStream = _sourceVidItem.getParallelAudioStreams();
        if (aoAudStream == null)
            return 0;
        else
            return aoAudStream.length;
    }

    private IDiscItemAudioStream _parallelAudio = null;

    public IDiscItemAudioStream getParallelAudio() {
        return _parallelAudio;
    }

    public boolean setParallelAudio(IDiscItemAudioStream parallelAudio) {
        if (_sourceVidItem.isAudioVideoAligned(_sourceVidItem)) {
            _parallelAudio = parallelAudio;
            return true;
        } else {
            return false;
        }
    }

    /** Returns the new setting, if valid, otherwise null. */
    public IDiscItemAudioStream setParallelAudioBySizeOrder(int iSizeIndex) {
        IDiscItemAudioStream[] aoAudStream = _sourceVidItem.getParallelAudioStreams();
        if (aoAudStream == null)
            return null;
        if (iSizeIndex < 0 || iSizeIndex >= aoAudStream.length)
            return null;

        IDiscItemAudioStream[] aoSorted = new IDiscItemAudioStream[aoAudStream.length];
        System.arraycopy(aoAudStream, 0, aoSorted, 0, aoAudStream.length);
        Arrays.sort(aoSorted, new Comparator<IDiscItemAudioStream>() {
            public int compare(IDiscItemAudioStream o1, IDiscItemAudioStream o2) {
                int i1Overlap = _sourceVidItem.overlap((DiscItem)o1);
                int i2Overlap = _sourceVidItem.overlap((DiscItem)o2);
                if (i1Overlap > i2Overlap)
                    return -1;
                else if (i1Overlap < i2Overlap)
                    return 1;
                else return 0;
            }
        });

        return _parallelAudio = aoSorted[iSizeIndex];
    }

    /** Returns the new setting, if valid, otherwise null. */
    public IDiscItemAudioStream setParallelAudioByIndexNumber(int iIndex) {
        IDiscItemAudioStream[] aoAudStream = _sourceVidItem.getParallelAudioStreams();
        if (aoAudStream == null)
            return null;

        for (IDiscItemAudioStream audStream : aoAudStream) {
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
        if (JPG.isAvailable()) {
            jpg = new StringHolder();
            String sParam = "-"+JPG.getId()+" %s {" + JpgQualities.getCmdLineList() +"}";
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

        BooleanHolder preciseav = new BooleanHolder(getPreciseAVSync());
        parser.addOption("-preciseav %v", preciseav); // Only with AVI & audio

        BooleanHolder precisefps = new BooleanHolder(getPreciseFrameTiming());
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
                int[] aiRange = Misc.splitInt(frames.value, "/");
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

    //##########################################################################
    //## The Writers ###########################################################
    //##########################################################################

    private static class ImgSeqDemux implements DemuxMovieWriter {

        protected final String _sBaseName;
        protected final DiscItemSTRVideo _vidItem;
        private final int _iStartFrame;
        private final int _iEndFrame;
        private ProgressListener _progress;

        protected final int _iDigitCount;

        public ImgSeqDemux(DiscItemSTRVideo vidItem, String sBaseName,
                           int iStartFrame, int iEndFrame)
        {
            _sBaseName = sBaseName;
            _iStartFrame = iStartFrame;
            _iEndFrame = iEndFrame;
            _vidItem = vidItem;

            _iDigitCount = String.valueOf(_iEndFrame).length();
        }

        public void open() throws IOException { /* nothing to do */ }
        public void close() throws IOException { /* nothing to do */ }
        public int getStartFrame() { return _iStartFrame; }
        public int getEndFrame() { return _iEndFrame; }
        public void repeatPreviousFrame() { /* nothing to do */ }
        protected ProgressListener getListener() { return _progress; }
        public void setListener(ProgressListener pl) { _progress = pl; }

        public void writeFrame(DemuxImage demux, int iSectorsFromStart) throws IOException {
            File f = new File(makeFileName(demux.getFrameNumber()));

            FileOutputStream fos = new FileOutputStream(f);
            try {
                fos.write(demux.getData(), 0, demux.getBufferSize());
            } finally {
                fos.close();
            }
        }

        protected String makeFileName(int iFrame) {
            return String.format("%s_%dx%d[%0"+_iDigitCount+"d].demux",
                                  _sBaseName,
                                  _vidItem.getWidth(), _vidItem.getHeight(),
                                  iFrame);
        }

        public String getOutputFile() {
            return makeFileName(_iStartFrame) + " to " + makeFileName(_iEndFrame);
        }

        public IDiscItemAudioSectorDecoder getAudioSectorDecoder() {
            throw new UnsupportedOperationException("Cannot write audio.");
        }
    }

    //..........................................................................

    private static class ImgSeqMdec extends ImgSeqDemux {

        private DemuxFrameUncompressor _uncompressor;
        
        public ImgSeqMdec(DiscItemSTRVideo vidItem, String sBaseName,
                          int iStartFrame, int iEndFrame)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame);
        }

        @Override
        public void writeFrame(DemuxImage demux, int iSectorsFromStart)
                throws IOException
        {

            DemuxFrameUncompressor uncompressor = getUncompressor(demux);
            if (uncompressor == null)
                return;

            File f = new File(makeFileName(demux.getFrameNumber()));

            FileOutputStream fos = new FileOutputStream(f);
            try {
                writeMdec(uncompressor, fos);
            } catch (UncompressionException ex) {
                getListener().warning(ex);
                log.log(Level.WARNING, "Error uncompressing frame " + demux.getFrameNumber(), ex);
            } finally {
                fos.close();
            }
        }

        @Override
        protected String makeFileName(int iFrame) {
            return String.format("%s_%dx%d[%0"+_iDigitCount+"d].mdec",
                                 _sBaseName,
                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                 iFrame);
        }

        private static void writeMdec(MdecInputStream oMdecIn, OutputStream oStreamOut)
                throws UncompressionException, IOException
        {
            MdecCode oCode = new MdecCode();
            while (true) {
                oMdecIn.readMdecCode(oCode);
                IO.writeInt16LE(oStreamOut, oCode.toMdecWord());
            }
        }


        protected DemuxFrameUncompressor getUncompressor(DemuxImage demux) {
            if (_uncompressor == null) {
                _uncompressor = JPSXPlugin.identifyUncompressor(demux);
                if (_uncompressor == null) {
                    getListener().warning("Unable to determine frame type.");
                    return null;
                } else {
                    getListener().info("Using " + _uncompressor.toString() + " uncompressor");
                }
            }
            try {
                _uncompressor.reset(demux.getData());
            } catch (NotThisTypeException ex) {
                _uncompressor = JPSXPlugin.identifyUncompressor(demux);
                if (_uncompressor == null) {
                    getListener().warning("Unable to determine frame type.");
                    return null;
                } else {
                    getListener().info("Using " + _uncompressor.toString() + " uncompressor");
                }
            }
            return _uncompressor;
        }
    }

    //..........................................................................

    private abstract static class ImgSeqRgbInt extends ImgSeqMdec {

        private final MdecDecoder _decoder;
        private final RgbIntImage _imgBuff;

        public ImgSeqRgbInt(DiscItemSTRVideo vidItem, 
                String sBaseName,
                int iStartFrame, int iEndFrame,
                MdecDecoder decoder, boolean blnCrop)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame);
            _decoder = decoder;
            int iWidth = _vidItem.getWidth(), iHeight = _vidItem.getHeight();
            if (!blnCrop) {
                iWidth = (iWidth + 15) & ~15;
                iHeight = (iHeight + 15) & ~15;
            }

            _imgBuff = new RgbIntImage(iWidth, iHeight);
        }

        @Override
        abstract public void writeFrame(DemuxImage demux, int iSectorsFromStart) throws IOException;

        protected RgbIntImage getRgb(DemuxImage demux) 
                throws UncompressionException, NotThisTypeException
        {
            DemuxFrameUncompressor uncompressor = getUncompressor(demux);
            if (uncompressor == null)
                throw new NotThisTypeException("Unable to identify frame type.");

            try {
                _decoder.decode(uncompressor);
            } catch (UncompressionException ex) {
                getListener().warning("Error uncompressing frame " + demux.getFrameNumber() + ": " + ex.getMessage());
                log.log(Level.WARNING, "Error uncompressing frame " + demux.getFrameNumber(), ex);
            }

            _decoder.readDecodedRGB(_imgBuff);
            return _imgBuff;
        }

        protected int getWidth() { return _imgBuff.getWidth(); }
        protected int getHeight() { return _imgBuff.getHeight(); }

        protected BufferedImage makeErrorImage(Throwable ex) {
            // draw the error onto a blank image
            BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            g.drawString(ex.getMessage(), 5, 20);
            g.dispose();
            return bi;
        }


    }

    //..........................................................................

    private static class ImgSeqJavaImage extends ImgSeqRgbInt {

        private final JavaImageFormat _eFmt;

        public ImgSeqJavaImage(DiscItemSTRVideo vidItem, 
                String sBaseName,
                int iStartFrame, int iEndFrame,
                MdecDecoder decoder, boolean blnCrop,
                JavaImageFormat eFormat)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame, decoder, blnCrop);

            _eFmt = eFormat;
        }

        @Override
        public void writeFrame(DemuxImage demux, int iSectorsFromStart) throws IOException {
            BufferedImage bi;
            try {
                RgbIntImage imgBuff = getRgb(demux);
                bi = imgBuff.toBufferedImage();
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Error with frame " + demux.getFrameNumber(), ex);
                bi = makeErrorImage(ex);
            }

            File f = new File(makeFileName(demux.getFrameNumber()));
            ImageIO.write(bi, _eFmt.getId(), f);
        }

        @Override
        protected String makeFileName(int iFrame) {
            return String.format("%s[%0"+_iDigitCount+"d].%s",
                              _sBaseName, iFrame,
                              _eFmt.getExtension());
        }

    }
    
    //..........................................................................

    private static class AviDemuxWriter extends ImgSeqRgbInt {
        private static final Fraction _150 = new Fraction(150);
        private static final Fraction _75 = new Fraction(75);

        private final Fraction _frameRate;
        private final float _fltJpgQuality;
        private final int _iSectorsPerSecond;

        private final IDiscItemAudioStream _audioDiscItem;
        private final AudioFormat _audioFmt;
        private IDiscItemAudioSectorDecoder _audioSectorDecoder;

        private double _volume = 1.0;
        
        private AviWriter _aviWriter;

        public AviDemuxWriter(DiscItemSTRVideo vidItem,
                String sBaseName,
                int iStartFrame, int iEndFrame,
                boolean blnSingleSpeed,
                MdecDecoder decoder,
                boolean blnCrop, float fltJpgQuality,
                IDiscItemAudioStream audioDiscItem)
        {
            super(vidItem, sBaseName,
                  iStartFrame, iEndFrame, 
                  decoder, blnCrop);

            _fltJpgQuality = fltJpgQuality;
            if (blnSingleSpeed) {
                _iSectorsPerSecond = 75;
                _frameRate = _75.divide(_vidItem.getSectorsPerFrame());
            } else {
                _iSectorsPerSecond = 150;
                _frameRate = _150.divide(_vidItem.getSectorsPerFrame());
            }

            _audioDiscItem = audioDiscItem;

            if (_audioDiscItem != null)
                _audioFmt = audioDiscItem.getAudioFormat(false);
            else
                _audioFmt = null;
        }

        private class AviPsxAudioWriter implements AudioOutputStream {
            public void close() { /* do nothing */ }
            public AudioFormat getFormat() { return _audioFmt; }

            public void write(AudioFormat inFormat, byte[] abData, int iOffset, int iLength) throws IOException
            {
                _aviWriter.writeAudio(abData, iOffset, iLength);
            }

            public String getOutputFile() { return AviDemuxWriter.this.getOutputFile(); }
        }
        @Override
        public IDiscItemAudioSectorDecoder getAudioSectorDecoder() {
            if (_audioDiscItem == null)
                return null;

            if (_audioSectorDecoder == null) {
                _audioSectorDecoder = _audioDiscItem.makeDecoder(new AviPsxAudioWriter(), false, _volume);
            }
            return _audioSectorDecoder;
        }


        @Override
        public void open() throws IOException {
            int iAvDelay = _vidItem.calculateAVoffset();
            double dblAvDelay = 0;
            if (_vidItem.getDiscSpeed() == 1) {
                dblAvDelay = iAvDelay / 75.0;
            } else {
                dblAvDelay = iAvDelay / 150.0;
            }
            // TODO: check selected frame rate to calculate av offset if disc speed is unknown

            if (_audioFmt != null) {
                if (_fltJpgQuality < 0) {
                    _aviWriter = new AviWriter(new File(getOutputFile()),
                                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                                 _frameRate.getNumerator(),
                                                 _frameRate.getDenominator(),
                                                 _audioFmt);
                } else {
                    _aviWriter = new AviWriter(new File(getOutputFile()),
                                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                                 _frameRate.getNumerator(),
                                                 _frameRate.getDenominator(),
                                                 _fltJpgQuality,
                                                 _audioFmt);
                }
            } else {
                if (_fltJpgQuality < 0) {
                    _aviWriter = new AviWriter(new File(getOutputFile()),
                                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                                 _frameRate.getNumerator(),
                                                 _frameRate.getDenominator());
                } else {
                    _aviWriter = new AviWriter(new File(getOutputFile()),
                                                 _vidItem.getWidth(), _vidItem.getHeight(),
                                                 _frameRate.getNumerator(),
                                                 _frameRate.getDenominator(),
                                                 _fltJpgQuality);
                }
            }
        }

        @Override
        public String getOutputFile() {
            return _sBaseName + ".avi";
        }

        @Override
        public void close() throws IOException {
            if (_aviWriter != null) {
                _aviWriter.close();
                _aviWriter = null;
                _audioSectorDecoder = null;
            }
        }

        @Override
        public void writeFrame(DemuxImage demux, int iSectorsFromStart) throws IOException {

            Fraction secondsPerFrame = _frameRate.reciprocal();

            Fraction discTime = new Fraction(iSectorsFromStart, _iSectorsPerSecond);
            Fraction movieTime = new Fraction(_aviWriter.getVideoFramesWritten(),1).multiply(secondsPerFrame);
            Fraction closestTime = discTime.subtract(movieTime).abs();
            int iCount = 0;
            while (true) {
                movieTime = movieTime.add(secondsPerFrame);
                
                Fraction timeDiff = discTime.subtract(movieTime).abs();
                if (timeDiff.compareTo(closestTime) < 0) {
                    closestTime = timeDiff;
/*
                    if (timeDiff.compareTo(Fraction.ZERO) >= 0) {
                        break;
                    }
*/
                } else {
                    break;
                }
                iCount++;
            }

            boolean blnWriteBefore = _aviWriter.getVideoFramesWritten() < 1;

            if (blnWriteBefore) {
                BufferedImage bi = null;
                try {
                    bi = getRgb(demux).toBufferedImage();
                } catch (Throwable ex) {
                    log.log(Level.WARNING, "Error with frame " + demux.getFrameNumber(), ex);
                    getListener().warning("Error with frame " + demux.getFrameNumber() + ": " + ex.getMessage());
                    bi = makeErrorImage(ex);
                }
                _aviWriter.writeFrame(bi);
            }

            while (iCount > 1) {
                _aviWriter.repeatPreviousFrame();
                iCount--;
            }

            if (!blnWriteBefore) {
                BufferedImage bi = null;
                try {
                    bi = getRgb(demux).toBufferedImage();
                } catch (Throwable ex) {
                    log.log(Level.WARNING, "Error with frame " + demux.getFrameNumber(), ex);
                    getListener().warning("Error with frame " + demux.getFrameNumber() + ": " + ex.getMessage());
                    bi = makeErrorImage(ex);
                }
                _aviWriter.writeFrame(bi);
            }
        }

    }

    //..........................................................................
    
    private static class Yuv4mpeg2MovieWriter extends ImgSeqMdec {

        private final MdecDecoder_double _decoder;
        private final Yuv4mpeg2 _yuvImgBuff;
        private Yuv4mpeg2Writer _writer;
        private final Fraction _frameRate;

        public Yuv4mpeg2MovieWriter(
               DiscItemSTRVideo vidItem,
               String sBaseName, int iStartFrame, int iEndFrame,
               boolean blnSingleSpeed,
               boolean blnCrop)
        {
            super(vidItem, sBaseName, iStartFrame, iEndFrame);

            if (blnSingleSpeed) {
                _frameRate = new Fraction(75).divide(_vidItem.getSectorsPerFrame());
            } else {
                _frameRate = new Fraction(150).divide(_vidItem.getSectorsPerFrame());
            }

            int iWidth = _vidItem.getWidth(), iHeight = _vidItem.getHeight();
            if (!blnCrop) {
                iWidth = (iWidth + 15) & ~15;
                iHeight = (iHeight + 15) & ~15;
            }
            _decoder = new MdecDecoder_double(new StephensIDCT(), iWidth, iHeight);
            _yuvImgBuff = new Yuv4mpeg2(iWidth, iHeight);
        }

        @Override
        public void open() throws IOException {
            File f = new File(_sBaseName + ".y4m");

            _writer = new Yuv4mpeg2Writer(f, _vidItem.getWidth(), _vidItem.getHeight(),
                                          (int)_frameRate.getNumerator(), (int)_frameRate.getDenominator(),
                                          Yuv4mpeg2.SUB_SAMPLING);
        }

        @Override
        public void close() throws IOException {
            _writer.close();
        }

        @Override
        public void writeFrame(DemuxImage demux, int iSectorsFromStart) throws IOException {
            DemuxFrameUncompressor uncompressor = getUncompressor(demux);
            if (uncompressor == null)
                return;

            try {
                _decoder.decode(uncompressor);
            } catch (UncompressionException ex) {
                log.log(Level.WARNING, "Error uncompressing frame " + demux.getFrameNumber(), ex);
            }

            _decoder.readDecodedYuv4mpeg2(_yuvImgBuff);

            _writer.writeFrame(_yuvImgBuff);
        }

        @Override
        public String getOutputFile() {
            return _sBaseName + ".y4m";
        }
    }

    
    
    public DemuxMovieWriter createDemuxWriter() {
        final MdecDecoder decoder;
        switch (getDecodeQuality()) {
            case HIGH:
                decoder = new MdecDecoder_double(new StephensIDCT(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case LOW:
                decoder = new MdecDecoder_int(new simple_idct(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            case PSX:
                decoder = new MdecDecoder_int(new PsxMdecIDCT(), _sourceVidItem.getWidth(), _sourceVidItem.getHeight());
                break;
            default:
                throw new RuntimeException("Oops");
        }

        final String sBaseName = _sourceVidItem.getSuggestedBaseName();
        int iDiscSpeed = _sourceVidItem.getDiscSpeed();
        if (iDiscSpeed < 1)
            iDiscSpeed = 2; // TODO:

        switch (getVideoFormat()) {
            case AVI_MJPG:
            case AVI_BMP:
                float fltJpgQuality = -1;
                if (getVideoFormat() == VideoFormat.AVI_MJPG) {
                    fltJpgQuality = getJpgCompressionOption().getQuality();
                }
                return new AviDemuxWriter(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(),
                        decoder,
                        getCrop(), fltJpgQuality,
                        getParallelAudio());
            case IMGSEQ_DEMUX:
                return new ImgSeqDemux(_sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame());
            case IMGSEQ_MDEC:
                return new ImgSeqMdec(_sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame());
            case IMGSEQ_JPG:
            case IMGSEQ_BMP:
            case IMGSEQ_PNG:
                return new ImgSeqJavaImage(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        decoder, getCrop(),
                        getVideoFormat().getImgFmt());

            case YUV4MPEG2_YUV:
                return new Yuv4mpeg2MovieWriter(
                        _sourceVidItem, sBaseName,
                        getSaveStartFrame(), getSaveEndFrame(),
                        getSingleSpeed(), _blnCrop);

                
        } // end case
        throw new UnsupportedOperationException(getVideoFormat() + " not implemented yet.");
    }

}
