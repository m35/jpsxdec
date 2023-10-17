/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2023  Michael Sabin
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

package jpsxdec.modules.video.sectorbased;

import argparser.BooleanHolder;
import argparser.StringHolder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.TabularFeedback;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.audio.sectorbased.ISectorClaimToSectorBasedDecodedAudio;
import jpsxdec.modules.video.save.VideoSaver;
import jpsxdec.modules.video.save.VideoSaverBuilder;
import jpsxdec.util.ArgParser;
import jpsxdec.util.Fraction;
import jpsxdec.util.TaskCanceledException;


/** Extends {@link VideoSaverBuilder} with sector-based video specific settings. */
public class SectorBasedVideoSaverBuilder extends VideoSaverBuilder {

    private static final Logger LOG = Logger.getLogger(SectorBasedVideoSaverBuilder.class.getName());

    @Nonnull
    private final DiscItemSectorBasedVideoStream _sourceVidItem;

    public SectorBasedVideoSaverBuilder(@Nonnull DiscItemSectorBasedVideoStream vidItem) {
        super(vidItem);
        _sourceVidItem = vidItem;
        if (vidItem.hasAudio()) {
            _parallelAudio = vidItem.getParallelAudioStreams();
            _ablnParallelAudio = new boolean[vidItem.getChildCount()];
        } else {
            _parallelAudio = new ArrayList<DiscItemSectorBasedAudioStream>(0);
            _ablnParallelAudio = new boolean[0];
        }
        if (_sourceVidItem.hasAudio()) {
            List<DiscItemSectorBasedAudioStream> defaultAud = _sourceVidItem.getLongestNonIntersectingAudioStreams();
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                _ablnParallelAudio[i] = defaultAud.contains(_parallelAudio.get(i));
            }
        }
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder otherBuilder) {
        if (super.copySettingsTo(otherBuilder)) {
            if (otherBuilder instanceof SectorBasedVideoSaverBuilder) {
                SectorBasedVideoSaverBuilder other = (SectorBasedVideoSaverBuilder) otherBuilder;
                //other.setParallelAudio(getParallelAudio());
                if (getSingleSpeed_enabled())
                    other.setSingleSpeed(getSingleSpeed());
                if (getEmulatePsxAVSync_enabled())
                    other.setEmulatePsxAVSync(getEmulatePsxAvSync());
                if (hasAudio() && !getSavingAudio())
                    other.setParallelAudioNone();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new SectorBasedVideoSaverBuilderGui(this);
    }


    // .........................................................................

    private boolean _blnSingleSpeed = false;
    public boolean getSingleSpeed() {
        DiscSpeed discSpeed = findDiscSpeed();
        if (discSpeed == null)
            return _blnSingleSpeed;
        else
            return discSpeed == DiscSpeed.SINGLE;
    }
    private @CheckForNull DiscSpeed findDiscSpeed() {
        return _sourceVidItem.getDiscSpeed();
    }
    public void setSingleSpeed(boolean val) {
        _blnSingleSpeed = val;
        firePossibleChange();
    }
    public boolean getSingleSpeed_enabled() {
        return getVideoFormat().isVideo() &&
               (findDiscSpeed() == null);
    }
    public @Nonnull Fraction getFps() {
        return Fraction.divide(
                getSingleSpeed() ? DiscSpeed.SINGLE.getSectorsPerSecond() : DiscSpeed.DOUBLE.getSectorsPerSecond(),
                _sourceVidItem.getSectorsPerFrame());
    }

    // .........................................................................

    @Override
    public boolean getAudioVolume_enabled() {
        return getSavingAudio();
    }

    // .........................................................................

    @Nonnull
    private final List<DiscItemSectorBasedAudioStream> _parallelAudio;
    @Nonnull
    private final boolean[] _ablnParallelAudio;

    public int getParallelAudioCount() {
        return _sourceVidItem.getChildCount();
    }
    public @Nonnull DiscItemSectorBasedAudioStream getParallelAudio(int i) {
        return _parallelAudio.get(i);
    }

    public boolean getParallelAudio_selected(int i) {
        if (getParallelAudio_enabled())
            return _ablnParallelAudio[i];
        else
            return false;
    }

    public void setParallelAudio(@Nonnull DiscItemSectorBasedAudioStream parallelAudio, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        setParallelAudio(_parallelAudio.indexOf(parallelAudio), blnSelected);
    }

    public void setParallelAudio(int iIndex, boolean blnSelected) {
        if (!_sourceVidItem.hasAudio())
            return;

        if (iIndex < 0 || iIndex >= _sourceVidItem.getChildCount())
            return;

        DiscItemSectorBasedAudioStream aud = _parallelAudio.get(iIndex);
        for (int i = 0; i < _ablnParallelAudio.length; i++) {
            if (_ablnParallelAudio[i]) {
                DiscItemSectorBasedAudioStream other = _parallelAudio.get(i);
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
        return getVideoFormat().isVideo() && getSaveStartFrame() == null;
    }

    @Override
    public boolean hasAudio() {
        return _sourceVidItem.hasAudio();
    }

    @Override
    public boolean getSavingAudio() {
        if (!getParallelAudio_enabled())
            return false;

        for (boolean b : _ablnParallelAudio) {
            if (b)
                return true;
        }
        return false;
    }

    // .........................................................................

    private boolean _blnEmulatePsxAVSync = false;
    @Override
    public boolean getEmulatePsxAvSync() {
        if (getEmulatePsxAVSync_enabled())
            return _blnEmulatePsxAVSync;
        return false;
    }
    public void setEmulatePsxAVSync(boolean val) {
        _blnEmulatePsxAVSync = val;
        firePossibleChange();
    }

    public boolean getEmulatePsxAVSync_enabled() {
        return getSavingAudio();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs)
    {
        super.commandLineOptions(ap, fbs);
        if (!ap.hasRemaining())
            return;

        StringHolder discSpeed = ap.addStringOption("-ds");
        BooleanHolder noaud = ap.addBoolOption(false, "-noaud"); // Only with video & audio
        BooleanHolder emulateav = ap.addBoolOption(false, "-psxav"); // Only with video & audio
        ap.match();

        if (discSpeed.value != null) {
            if ("1".equals(discSpeed.value)) {
                setSingleSpeed(true);
            } else if ("2".equals(discSpeed.value)) {
                setSingleSpeed(false);
            } else {
                fbs.printWarn(I.CMD_IGNORING_INVALID_VALUE_FOR_CMD(discSpeed.value, "-ds"));
            }
        }

        setEmulatePsxAVSync(emulateav.value);

        if (noaud.value)
            setParallelAudioNone();
    }

    @Override
    protected void makeHelpTable(@Nonnull TabularFeedback tfb) {
        super.makeHelpTable(tfb);

        if (getSingleSpeed_enabled()) {
            tfb.newRow();
            tfb.addCell(I.CMD_VIDEO_DS()).addCell(I.CMD_VIDEO_DS_HELP());
        }

        //tfb.newRow();
        //tfb.print("-psxfps").tab().print("Emulate PSX FPS timing"); // I18N

        if (_sourceVidItem.hasAudio()) {
            tfb.newRow();
            tfb.addCell(I.CMD_VIDEO_NOAUD()).addCell(I.CMD_VIDEO_NOAUD_HELP());

            tfb.newRow();
            tfb.addCell(I.CMD_VIDEO_PSXAV()).addCell(I.CMD_VIDEO_PSXAV_HELP());
        }
    }

    @Override
    public void printSelectedOptions(@Nonnull ILocalizedLogger log) {
        super.printSelectedOptions(log);

        log.log(Level.INFO, I.CMD_DISC_SPEED(getSingleSpeed() ? 1 : 2, getFps().asDouble()));
    }

    @Override
    protected void printSelectedAudioOptions(@Nonnull ILocalizedLogger log) {
        log.log(Level.INFO, I.CMD_SAVING_WITH_AUDIO_ITEMS());

        ArrayList<DiscItemSectorBasedAudioStream> chosenAudio = collectSelectedAudio();
        LOG.log(Level.INFO, "Using {0} of {1} audio streams", new Object[]{chosenAudio.size(), _parallelAudio.size()});
        for (DiscItemSectorBasedAudioStream discItemAudioStream : chosenAudio) {
            log.log(Level.INFO, discItemAudioStream.getDetails());
        }

        log.log(Level.INFO, I.CMD_EMULATE_PSX_AV_SYNC_NY(getEmulatePsxAvSync() ? 1 : 0));
    }

    private @Nonnull ArrayList<DiscItemSectorBasedAudioStream> collectSelectedAudio() {
        ArrayList<DiscItemSectorBasedAudioStream> parallelAudio = new ArrayList<DiscItemSectorBasedAudioStream>();
        if (getSavingAudio()) {
            for (int i = 0; i < _ablnParallelAudio.length; i++) {
                if (_ablnParallelAudio[i])
                    parallelAudio.add(_parallelAudio.get(i));
            }
        }
        return parallelAudio;
    }

    @Override
    public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();
        printSelectedOptions(pl);

        final ISectorClaimToSectorBasedDecodedAudio audDecoder;
        ArrayList<DiscItemSectorBasedAudioStream> parallelAudio = collectSelectedAudio();
        if (parallelAudio.isEmpty())
            audDecoder = null;
        else if (parallelAudio.size() == 1)
            audDecoder = parallelAudio.get(0).makeDecoder(getAudioVolume());
        else
            audDecoder = new SectorBasedAudioStreamsCombiner(parallelAudio, getAudioVolume());

        SectorClaimToSectorBasedFrameAndAudio sc2fa = new SectorClaimToSectorBasedFrameAndAudio(_sourceVidItem.makeDemuxer(),
                                                                                                _sourceVidItem.getStartSector(),
                                                                                                _sourceVidItem.getEndSector(),
                                                                                                audDecoder);

        VideoSaver vs = new VideoSaver(_sourceVidItem, this, thisGeneratedFileListener, directory, pl, sc2fa,
                                       getSingleSpeed() ? DiscSpeed.SINGLE : DiscSpeed.DOUBLE,
                                       _sourceVidItem.getSectorsPerFrame(), _sourceVidItem.getAbsolutePresentationStartSector());
        vs.save(pl);
    }

}
