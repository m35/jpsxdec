/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2023  Michael Sabin
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

import argparser.BooleanHolder;
import argparser.StringHolder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.TabularFeedback;
import jpsxdec.i18n.TabularFeedback.Cell;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.framenumber.FormattedFrameNumber;
import jpsxdec.modules.video.framenumber.FrameLookup;
import jpsxdec.modules.video.framenumber.FrameNumber;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.ChromaUpsample;
import jpsxdec.util.ArgParser;
import jpsxdec.util.TaskCanceledException;

/** Manages the common options for saving PSX video. */
public abstract class VideoSaverBuilder extends DiscItemSaverBuilder {

    private static final Logger LOG = Logger.getLogger(VideoSaverBuilder.class.getName());

    @Nonnull
    private final DiscItemVideoStream _sourceVidItem;

    private final List<VideoFormat> _imgFmtList = VideoFormat.getAvailable();
    private final List<VideoFormat> _imgFmtListNoBs;


    protected VideoSaverBuilder(@Nonnull DiscItemVideoStream vidItem) {
        _sourceVidItem = vidItem;
        _types = vidItem.getFrameNumberTypes();
        _imgFmtListNoBs = new ArrayList<VideoFormat>(_imgFmtList);
        _imgFmtListNoBs.remove(VideoFormat.IMGSEQ_BITSTREAM);
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder otherBuilder) {
        if (otherBuilder instanceof VideoSaverBuilder) {
            VideoSaverBuilder other = (VideoSaverBuilder) otherBuilder;
            // only copy valid settings
            other.setVideoFormat(getVideoFormat());
            //other.setParallelAudio(getParallelAudio());
            if (getCrop_enabled())
                other.setCrop(getCrop());
            if (getDecodeQuality_enabled())
                other.setDecodeQuality(getDecodeQuality());
            if (getChromaInterpolation_enabled())
                other.setChromaInterpolation(getChromaInterpolation());
            if (getAudioVolume_enabled())
                other.setAudioVolume(getAudioVolume());
            return true;
        }
        return false;
    }

    @Override
    public @Nonnull DiscItemVideoStream getDiscItem() {
        return _sourceVidItem;
    }

    // .........................................................................

    /** Returns range of files that may be saved based on the format, before filtering.
     * @return array length 1 or 2. */
    public @Nonnull File[] getOutputFileRange() {
        VideoFormat vf = getVideoFormat();
        if (vf.isVideo()) {
            return new File[] { VideoFileNameFormatter.singleFile(null, _sourceVidItem, vf) };
        } else {
            VideoFileNameFormatter ff = new VideoFileNameFormatter(null, _sourceVidItem, vf, false);
            FrameNumber startFrame = _sourceVidItem.getStartFrame();
            FrameNumber endFrame = _sourceVidItem.getEndFrame();

            // the frame types defined in the video should match the available
            // frame types of the start and end frames
            // if not, something weird happened

            FormattedFrameNumber start = startFrame.getNumber(getFileNumberType());
            if (start == null)
                throw new IllegalStateException("Video should have had a start and end header frame number type");
            if (startFrame.equalValue(endFrame)) {
                return new File[] {
                    ff.format(start, null),
                };
            } else {
                FormattedFrameNumber end = endFrame.getNumber(getFileNumberType());
                if (end == null)
                    throw new IllegalStateException("Video should have had a start and end header frame number type");
                return new File[] {
                    ff.format(start, null),
                    ff.format(end, null),
                };
            }
        }
    }

    // .........................................................................

    public @Nonnull VideoFormat getVideoFormat_listItem(int i) {
        if (_sourceVidItem.hasIndependentBitstream())
            return _imgFmtList.get(i);
        else
            return _imgFmtListNoBs.get(i);
    }
    public int getVideoFormat_listSize() {
        if (_sourceVidItem.hasIndependentBitstream())
            return _imgFmtList.size();
        else
            return _imgFmtListNoBs.size();
    }

    @Nonnull
    private VideoFormat _videoFormat = VideoFormat.AVI_MJPG;
    public @Nonnull VideoFormat getVideoFormat() {
        return _videoFormat;
    }
    public void setVideoFormat(@Nonnull VideoFormat val) {
        if (_sourceVidItem.hasIndependentBitstream()) {
            if (!_imgFmtList.contains(val))
                throw new IllegalArgumentException();
        } else {
            if (!_imgFmtListNoBs.contains(val))
                throw new IllegalArgumentException();
        }
        _videoFormat = val;
        firePossibleChange();
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
        return _sourceVidItem.shouldBeCropped() && getVideoFormat().isCroppable();
    }

    public int getWidth() {
        if (getCrop()) {
            if (getVideoFormat().mustHaveEvenDims() && _sourceVidItem.getWidth() % 2 == 1) {
                return _sourceVidItem.getWidth() + 1;
            } else {
                return _sourceVidItem.getWidth();
            }
        } else {
            return Calc.fullDimension(_sourceVidItem.getWidth());
        }
    }

    public int getHeight() {
        if (getCrop()) {
            if (getVideoFormat().mustHaveEvenDims() && _sourceVidItem.getHeight() % 2 == 1) {
                return _sourceVidItem.getHeight() + 1;
            } else {
                return _sourceVidItem.getHeight();
            }
        } else {
            return Calc.fullDimension(_sourceVidItem.getHeight());
        }
    }

    public boolean makingDimensionsEven() {
        return getCrop() && getVideoFormat().mustHaveEvenDims() &&
               ((_sourceVidItem.getWidth() | _sourceVidItem.getHeight() & 1) != 0);
    }

    // .........................................................................

    public int getDecodeQuality_listSize() {
        return getVideoFormat().getDecodeQualityCount();
    }
    public @Nonnull MdecDecodeQuality getDecodeQuality_listItem(int i) {
        return getVideoFormat().getMdecDecodeQuality(i);
    }

    @CheckForNull
    private MdecDecodeQuality _decodeQuality = MdecDecodeQuality.HIGH;
    public @CheckForNull MdecDecodeQuality getDecodeQuality() {
        if (getVideoFormat().getDecodeQualityCount() == 1)
            return getVideoFormat().getMdecDecodeQuality(0);
        return _decodeQuality;
    }
    public void setDecodeQuality(@Nonnull MdecDecodeQuality val) {
        _decodeQuality = val;
        firePossibleChange();
    }

    public boolean getDecodeQuality_enabled() {
        return getVideoFormat().getDecodeQualityCount() > 1; // TODO if disabled, show nothing
    }

    // .........................................................................

    @Nonnull
    private ChromaUpsample _mdecUpsampler = ChromaUpsample.Bicubic;

    public boolean getChromaInterpolation_enabled() {
        MdecDecodeQuality q = getDecodeQuality();
        return getDecodeQuality_enabled() && q != null && q.canUpsample();
    }

    public @Nonnull ChromaUpsample getChromaInterpolation_listItem(int i) {
        return ChromaUpsample.values()[i];
    }

    public int getChromaInterpolation_listSize() {
        return ChromaUpsample.values().length;
    }

    public @Nonnull ChromaUpsample getChromaInterpolation() {
        if (getChromaInterpolation_enabled())
            return _mdecUpsampler;
        else
            return ChromaUpsample.NearestNeighbor;
    }

    public void setChromaInterpolation(@Nonnull ChromaUpsample val) {
        _mdecUpsampler = val;
        firePossibleChange();
    }

    // .........................................................................

    public boolean getFileNumberType_enabled() {
        return !getVideoFormat().isVideo();
    }

    private final List<FrameNumber.Type> _types;
    public @Nonnull FrameNumber.Type getFileNumberType_listItem(int i) {
        if (getFileNumberType_enabled())
            return _types.get(i);
        else
            throw new IndexOutOfBoundsException();
    }

    public int getFileNumberType_listSize() {
        if (getFileNumberType_enabled())
            return _types.size();
        else
            return 0;
    }

    @Nonnull
    private FrameNumber.Type _frameNumberType = FrameNumber.Type.Index;
    public @Nonnull FrameNumber.Type getFileNumberType() {
        return _frameNumberType;
    }
    public void setFileNumberType(@Nonnull FrameNumber.Type val) {
        _frameNumberType = val;
        firePossibleChange();
    }

    // .........................................................................

    @CheckForNull
    private FrameLookup _saveStartFrame = null;
    public @CheckForNull FrameLookup getSaveStartFrame() {
        return _saveStartFrame;
    }
    public void setSaveStartFrame(@CheckForNull FrameLookup val) {
        _saveStartFrame = val;
        firePossibleChange();
    }

    // .........................................................................

    @CheckForNull
    private FrameLookup _saveEndFrame = null;
    public @CheckForNull FrameLookup getSaveEndFrame() {
        return _saveEndFrame;
    }
    public void setSaveEndFrame(@CheckForNull FrameLookup val) {
        _saveEndFrame = val;
        firePossibleChange();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    final public void printHelp(@Nonnull FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();
        makeHelpTable(tfb);
        tfb.write(fbs.getUnderlyingStream());
    }

    /** Override to append additional help items. */
    protected void makeHelpTable(@Nonnull TabularFeedback tfb) {
        tfb.setRowSpacing(1);

        tfb.addCell(I.CMD_VIDEO_VF());
        Cell c = new Cell(I.CMD_VIDEO_VF_HELP(VideoFormat.AVI_MJPG.getCmdLine()));
        for (VideoFormat fmt : VideoFormat.getAvailable()) {
            c.addLine(new UnlocalizedMessage(fmt.getCmdLine()), 2);
        }
        tfb.addCell(c);

        tfb.newRow();

        tfb.addCell(I.CMD_VIDEO_QUALITY());
        c = new Cell(I.CMD_VIDEO_QUALITY_HELP(MdecDecodeQuality.HIGH.getCmdLine()));
        for (MdecDecodeQuality quality : MdecDecodeQuality.values()) {
            c.addLine(quality.getCmdLine(), 2);
        }
        tfb.addCell(c);

        tfb.newRow();

        tfb.addCell(I.CMD_VIDEO_UP());
        c = new Cell(I.CMD_VIDEO_UP_HELP(ChromaUpsample.Bicubic.getDescription()));
        for (ChromaUpsample up : ChromaUpsample.values()) {
            c.addLine(up.getCmdLineHelp(), 2);
        }
        tfb.addCell(c);

        tfb.newRow();
        tfb.addCell(I.CMD_VIDEO_FRAMES()).addCell(I.CMD_VIDEO_FRAMES_HELP());

        if (_sourceVidItem.shouldBeCropped()) {
            tfb.newRow();
            tfb.addCell(I.CMD_VIDEO_NOCROP()).addCell(I.CMD_VIDEO_NOCROP_HELP());
        }

        tfb.newRow();

        tfb.addCell(I.CMD_VIDEO_NUM());
        c = new Cell(I.CMD_VIDEO_NUM_HELP(FrameNumber.Type.Index.getLocalizedName()));
        for (FrameNumber.Type type : _types) {
            c.addLine(type.getLocalizedName(), 2);
        }
        tfb.addCell(c);
    }

    @Override
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs) {
        if (!ap.hasRemaining())
            return;

        StringHolder vidfmt = ap.addStringOption("-vidfmt","-vf");
        BooleanHolder nocrop = ap.addBoolOption(false, "-nocrop"); // only non demux & mdec formats
        StringHolder quality = ap.addStringOption("-quality","-q");
        StringHolder up = ap.addStringOption("-up");
        StringHolder startFrame = ap.addStringOption("-start");
        StringHolder endFrame = ap.addStringOption("-end");
        StringHolder num = ap.addStringOption("-num");

        //BooleanHolder emulatefps = ap.addBoolOption(false, "-psxfps"); // Mutually excusive with fps...

        // -------------------------
        ap.match();
        // -------------------------

        boolean blnHeaderFrameNumberIgnored = false;

        if (startFrame.value != null) {
            try {
                FrameLookup fl = new FrameLookup(startFrame.value);
                if (_types.contains(fl.getType())) {
                    setSaveStartFrame(fl);
                } else if (fl.getType() == FrameNumber.Type.Header) {
                    blnHeaderFrameNumberIgnored = true;
                } else {
                    throw new RuntimeException("Only header type should ever be unsupported here");
                }
            } catch (LocalizedDeserializationFail ex) {
                fbs.printlnWarn(ex.getSourceMessage());
            }
        }


        if (endFrame.value != null) {
            try {
                FrameLookup fl = new FrameLookup(endFrame.value);
                if (_types.contains(fl.getType())) {
                    setSaveEndFrame(fl);
                } else if (fl.getType() == FrameNumber.Type.Header) {
                    blnHeaderFrameNumberIgnored = true;
                } else {
                    throw new RuntimeException("Only header type should ever be unsupported here");
                }
            } catch (LocalizedDeserializationFail ex) {
                fbs.printlnWarn(ex.getSourceMessage());
            }
        }

        if (num.value != null) {
            FrameNumber.Type t = FrameNumber.Type.fromCmdLine(num.value);
            if (t != null) {
                if (_types.contains(t)) {
                    setFileNumberType(t);
                } else if (t == FrameNumber.Type.Header) {
                    blnHeaderFrameNumberIgnored = true;
                } else {
                    throw new RuntimeException("Only header type should ever be unsupported here");
                }
            } else {
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(num.value, "-num"));
            }
        }

        if (blnHeaderFrameNumberIgnored) {
            fbs.printWarn(I.CMD_VIDEO_HEADER_FRAME_NUMBER_UNSUPPORTED());
        }

        if (vidfmt.value != null) {
            VideoFormat vf = VideoFormat.fromCmdLine(vidfmt.value);
            if (vf != null)
                setVideoFormat(vf);
             else
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(vidfmt.value, "-vf,-vidfmt"));
        }

        if (quality.value != null) {
            MdecDecodeQuality dq = MdecDecodeQuality.fromCmdLine(quality.value);
            if (dq != null)
                setDecodeQuality(dq);
            else
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(quality.value, "-q,-quality"));
        }

        if (up.value != null) {
            ChromaUpsample upsampler = ChromaUpsample.fromCmdLine(up.value);
            if (upsampler != null)
                setChromaInterpolation(upsampler);
            else
                fbs.printlnWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(up.value, "-up"));
        }

        setCrop(!nocrop.value);
    }
    @Override
    public void printSelectedOptions(@Nonnull ILocalizedLogger log) {
        VideoFormat vidFmt = getVideoFormat();

        log.log(Level.INFO, I.CMD_VIDEO_FORMAT(getVideoFormat().toString()));

        if (vidFmt.getDecodeQualityCount() > 0) {
            MdecDecodeQuality quality = getDecodeQuality();
            log.log(Level.INFO, I.CMD_DECODE_QUALITY(quality.toString()));
            if (quality.canUpsample()) {
                ChromaUpsample chroma = getChromaInterpolation();
                log.log(Level.INFO, I.CMD_UPSAMPLE_QUALITY(chroma.getDescription().getLocalizedMessage()));
            }
        }

        if (getCrop_enabled())
            log.log(Level.INFO, I.CMD_CROPPING(getCrop() ? 1 : 0));

        if (makingDimensionsEven())
            log.log(Level.INFO, I.CMD_VIDEO_MUST_HAVE_EVEN_DIMS(getVideoFormat().toString()));

        log.log(Level.INFO, I.CMD_DIMENSIONS(getWidth(), getHeight()));

        if (vidFmt.isSequence()) {
            File[] _aoOutRng = getOutputFileRange();
            if (_aoOutRng.length == 1) {
                log.log(Level.INFO, I.CMD_OUTPUT_FILE(_aoOutRng[0]));
            } else {
                log.log(Level.INFO, I.CMD_OUTPUT_FILES(_aoOutRng[0], _aoOutRng[1]));
            }
        } else {

            if (getSavingAudio()) {
                printSelectedAudioOptions(log);
            } else {
                log.log(Level.INFO, I.CMD_NO_AUDIO());
            }

            File outFile = VideoFileNameFormatter.singleFile(null, _sourceVidItem, vidFmt);
            log.log(Level.INFO, I.CMD_SAVING_AS(outFile));
        }

        FrameLookup startFrame = getSaveStartFrame();
        FrameLookup endFrame = getSaveEndFrame();
        if (startFrame != null)
            log.log(Level.INFO, I.CMD_FRAME_RANGE_BEFORE(startFrame.toString()));
        if (endFrame != null)
            log.log(Level.INFO, I.CMD_FRAME_RANGE_AFTER(endFrame.toString()));
    }

    abstract protected void printSelectedAudioOptions(@Nonnull ILocalizedLogger log);


    @Override
    public @Nonnull ILocalizedMessage getOutputSummary() {
        if (getVideoFormat().isSequence()) {
            File[] _aoOutRng = getOutputFileRange();
            if (_aoOutRng.length == 1) {
                return new UnlocalizedMessage(_aoOutRng[0].toString());
            } else {
                return I.VID_RANGE_OF_FILES_TO_SAVE(_aoOutRng[0], _aoOutRng[1]);
            }
        } else {
            File outFile = VideoFileNameFormatter.singleFile(null, _sourceVidItem, getVideoFormat());
            return new UnlocalizedMessage(outFile.getPath());
        }
    }

    protected final VDP.GeneratedFileListener thisGeneratedFileListener =
                new VDP.GeneratedFileListener() {
                    @Override
                    public void fileGenerated(File f) {
                        addGeneratedFile(f);
                    }
                };

    @Override
    abstract public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException;

    // audio related subclass methods

    private double _dblAudioVolume = 1.0;
    public double getAudioVolume() {
        return _dblAudioVolume;
    }
    public void setAudioVolume(double val) {
        _dblAudioVolume = Math.min(Math.max(0.0, val), 1.0);
        firePossibleChange();
    }
    abstract public boolean getAudioVolume_enabled();

    abstract public boolean hasAudio();
    abstract public boolean getSavingAudio();

    abstract public boolean getEmulatePsxAvSync();
}
