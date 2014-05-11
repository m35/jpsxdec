/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import jpsxdec.I18N;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.discitems.ISectorAudioDecoder;
import jpsxdec.discitems.ISectorFrameDemuxer;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecDecoder_double_interpolate.Upsampler;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;
import jpsxdec.util.TabularFeedback;

/** Manages all possible options for saving PSX video. */
public abstract class VideoSaverBuilder extends DiscItemSaverBuilder {

    private final DiscItemVideoStream _sourceVidItem;

    public VideoSaverBuilder(DiscItemVideoStream vidItem) {
        _sourceVidItem = vidItem;
        resetToDefaults();
    }

    public void resetToDefaults() {
        setVideoFormat(VideoFormat.AVI_MJPG);
        setCrop(true);
        setDecodeQuality(MdecDecodeQuality.LOW);
        setChromaInterpolation(Upsampler.Bicubic);
        setSaveStartFrame(_sourceVidItem.getStartFrame());
        setSaveEndFrame(_sourceVidItem.getEndFrame());
        setSingleSpeed(false);
        setAudioVolume(1.0);
    }

    public boolean copySettingsTo(DiscItemSaverBuilder otherBuilder) {
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
            if (getSingleSpeed_enabled())
                other.setSingleSpeed(getSingleSpeed());
            if (getAudioVolume_enabled())
                other.setAudioVolume(getAudioVolume());
            return true;
        }
        return false;
    }

    // .........................................................................

    /** Returns array length 1 or 2. */
    public File[] getOutputFileRange() {
        VideoFormat vf = getVideoFormat();
        if (vf.isAvi()) {
            return new File[] { FrameFormatter.makeFile(null, vf, _sourceVidItem) };
        } else {
            FrameFormatter ff = FrameFormatter.makeFormatter(vf, _sourceVidItem);
            if (getSaveStartFrame() == getSaveEndFrame()) {
                return new File[] {
                    ff.format(getSaveStartFrame()),
                };
            } else {
                return new File[] {
                    ff.format(getSaveStartFrame()),
                    ff.format(getSaveEndFrame()),
                };
            }
        }
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
        return _sourceVidItem.getDiscSpeed();
    }
    public void setSingleSpeed(boolean val) {
        _blnSingleSpeed = val;
        firePossibleChange();
    }
    public boolean getSingleSpeed_enabled() {
        return getVideoFormat().isAvi() &&
               (findDiscSpeed() < 1);
    }
    public Fraction getFps() {
        return Fraction.divide( 
                getSingleSpeed() ? 75 : 150,
                _sourceVidItem.getSectorsPerFrame());
    }

    // .........................................................................

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
        if (getCrop())
            return _sourceVidItem.getWidth();
        else
            return Calc.fullDimension(_sourceVidItem.getWidth());
    }

    public int getHeight() {
        if (getCrop())
            return _sourceVidItem.getHeight();
        else
            return Calc.fullDimension(_sourceVidItem.getHeight());
    }

    // .........................................................................

    public int getDecodeQuality_listSize() {
        return getVideoFormat().getDecodeQualityCount();
    }
    public MdecDecodeQuality getDecodeQuality_listItem(int i) {
        return getVideoFormat().getMdecDecodeQuality(i);
    }

    private MdecDecodeQuality _decodeQuality = MdecDecodeQuality.LOW;
    public MdecDecodeQuality getDecodeQuality() {
        if (getVideoFormat().getDecodeQualityCount() == 1)
            return getVideoFormat().getMdecDecodeQuality(0);
        return _decodeQuality;
    }
    public void setDecodeQuality(MdecDecodeQuality val) {
        _decodeQuality = val;
        firePossibleChange();
    }

    public boolean getDecodeQuality_enabled() {
        return getVideoFormat().getDecodeQualityCount() > 1;
    }

    // .........................................................................

    private Upsampler _mdecUpsampler = Upsampler.Bicubic;

    public boolean getChromaInterpolation_enabled() {
        return getDecodeQuality_enabled() && getDecodeQuality().canUpsample();
    }

    public Upsampler getChromaInterpolation_listItem(int i) {
        return Upsampler.values()[i];
    }

    public int getChromaInterpolation_listSize() {
        return Upsampler.values().length;
    }

    public Upsampler getChromaInterpolation() {
        if (getChromaInterpolation_enabled())
            return _mdecUpsampler;
        else
            return Upsampler.NearestNeighbor;
    }

    public void setChromaInterpolation(Upsampler val) {
        _mdecUpsampler = val;
        firePossibleChange();
    }

    // .........................................................................

    private int _iSaveStartFrame;
    public int getSaveStartFrame() {
        return _iSaveStartFrame;
    }
    public void setSaveStartFrame(int val) {
        _iSaveStartFrame = val;
        _iSaveEndFrame = Math.max(_iSaveEndFrame, _iSaveStartFrame);
        firePossibleChange();
    }

    // .........................................................................
    
    private int _iSaveEndFrame;
    public int getSaveEndFrame() {
        return _iSaveEndFrame;
    }
    public void setSaveEndFrame(int val) {
        _iSaveEndFrame = val;
        _iSaveStartFrame = Math.min(_iSaveEndFrame, _iSaveStartFrame);
        firePossibleChange();
    }

    ////////////////////////////////////////////////////////////////////////////

    public String[] commandLineOptions(String[] asArgs, FeedbackStream fbs) {
        if (asArgs == null) return null;
        
        ArgParser parser = new ArgParser("", false);

        //...........

        StringHolder vidfmt = new StringHolder();
        parser.addOption("-vidfmt,-vf %s", vidfmt);

        BooleanHolder nocrop = new BooleanHolder(false);
        parser.addOption("-nocrop %v", nocrop); // only non demux & mdec formats

        StringHolder quality = new StringHolder();
        parser.addOption("-quality,-q %s", quality);

        StringHolder up = new StringHolder();
        parser.addOption("-up %s", up);

        //...........

        IntHolder discSpeed = new IntHolder(-10);
        parser.addOption("-ds %i {[1, 2]}", discSpeed);

        StringHolder frames = new StringHolder();
        parser.addOption("-frame,-frames %s", frames);

        //...........
        
        //BooleanHolder emulatefps = new BooleanHolder(false);
        //parser.addOption("-psxfps %v", emulatefps); // Mutually excusive with fps...

        // -------------------------
        String[] asRemain = parser.matchAllArgs(asArgs, 0, 0);
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
                    fbs.printlnWarn(I18N.S("Invalid frame(s) {0}", frames.value)); // I18N
                }
            }
        }

        if (vidfmt.value != null) {
            VideoFormat vf = VideoFormat.fromCmdLine(vidfmt.value);
            if (vf != null) 
                setVideoFormat(vf);
             else 
                fbs.printlnWarn(I18N.S("Invalid video format {0}", vidfmt.value)); // I18N
        }

        if (quality.value != null) {
            MdecDecodeQuality dq = MdecDecodeQuality.fromCmdLine(quality.value);
            if (dq != null)
                setDecodeQuality(dq);
            else
                fbs.printlnWarn(I18N.S("Invalid decode quality {0}", quality.value)); // I18N
        }

        if (up.value != null) {
            Upsampler upsampler = Upsampler.fromCmdLine(up.value);
            if (up != null)
                setChromaInterpolation(upsampler);
            else
                fbs.printlnWarn(I18N.S("Invalid upsample quality {0}", up.value)); // I18N
        }

        setCrop(!nocrop.value);

        if (discSpeed.value == 1) {
            setSingleSpeed(true);
        } else if (discSpeed.value == 2) {
            setSingleSpeed(false);
        }

        return asRemain;
    }

    final public void printHelp(FeedbackStream fbs) {
        TabularFeedback tfb = new TabularFeedback();
        makeHelpTable(tfb);
        tfb.write(fbs);
    }

    /** Override to append additional help items. */
    protected void makeHelpTable(TabularFeedback tfb) {
        tfb.setRowSpacing(1);

        tfb.print("-vidfmt,-vf <format>").tab().println("Output video format (default avi:mjpg).") // I18N
                                               .print("Options:"); // I18N
        tfb.indent();
        for (VideoFormat fmt : VideoFormat.values()) {
            if (fmt.isAvailable()) {
                tfb.ln().print(fmt.getCmdLine());
            }
        }

        tfb.newRow();
        tfb.print("-quality,-q <quality>").tab().println("Decoding quality (default low). Options:"); // I18N
        tfb.indent().print(MdecDecodeQuality.getCmdLineList());
        
        tfb.newRow();
        tfb.print("-up <upsampling>").tab().println("Chroma upsampling method") // I18N
                                           .print("(default "+Upsampler.Bicubic+"). Options:").indent(); // I18N
        for (Upsampler up : Upsampler.values()) {
            tfb.ln().print(up.name());
        }

        if (getSingleSpeed_enabled()) {
            tfb.newRow();
            tfb.print("-ds <disc speed>").tab().print("Specify 1 or 2 if disc speed is undetermined."); // I18N
        }
        
        //tfb.newRow();
        //tfb.print("-psxfps").tab().print("Emulate PSX FPS timing");

        tfb.newRow();
        tfb.print("-frame,-frames # or #-#").tab().print("Process only frames in range."); // I18N

        if (_sourceVidItem.shouldBeCropped()) {
            tfb.newRow();
            tfb.print("-nocrop").tab().print("Don't crop data around unused frame edges."); // I18N
        }
    }

    /** Make the snapshot with the right demuxer and audio decoder. */
    abstract protected SectorFeeder makeFeeder();

    public static abstract class SectorFeeder {
        public ISectorFrameDemuxer videoDemuxer;
        public ISectorAudioDecoder audioDecoder;
        abstract public void feedSector(IdentifiedSector sector, Logger log) throws IOException;
        public void flush(Logger log) throws IOException {
            videoDemuxer.flush(log);
        }
    }

    final public IDiscItemSaver makeSaver(File directory) {
        SectorFeeder feeder = makeFeeder();

        VideoSaver vs;
        if (getVideoFormat().isAvi())
            vs = new VideoSaver.Avi(_sourceVidItem, directory, feeder, this);
        else
            vs = new VideoSaver.Sequence(_sourceVidItem, directory, feeder, this);

        return vs;
    }

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
    
    abstract boolean getEmulatePsxAvSync();
}
