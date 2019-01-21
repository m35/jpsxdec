/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2019  Michael Sabin
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

package jpsxdec.modules.crusader;

import argparser.BooleanHolder;
import java.io.File;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.i18n.FeedbackStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.TabularFeedback;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.video.save.VideoSaver;
import jpsxdec.modules.video.save.VideoSaverBuilder;
import jpsxdec.util.ArgParser;
import jpsxdec.util.TaskCanceledException;

/** Extends {@link VideoSaverBuilder} with Crusader specific settings. */
public class VideoSaverBuilderCrusader extends VideoSaverBuilder {

    @Nonnull
    private final DiscItemCrusader _sourceVidItem;

    public VideoSaverBuilderCrusader(@Nonnull DiscItemCrusader vidItem) {
        super(vidItem);
        _sourceVidItem = vidItem;
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder otherBuilder) {
        if (super.copySettingsTo(otherBuilder)) {
            if (otherBuilder instanceof VideoSaverBuilderCrusader){
                VideoSaverBuilderCrusader other = (VideoSaverBuilderCrusader) otherBuilder;
                other.setSavingAudio(getSavingAudio());
            }
            return true;
        } else {
            return false;
        }
    }

    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new VideoSaverBuilderCrusaderGui(this);
    }

    // .........................................................................


    public boolean getAudioVolume_enabled() {
        return getSavingAudio();
    }

    // .........................................................................

    public boolean getSavingAudio_enabled() {
        return getVideoFormat().isAvi() && getSaveStartFrame() == null;
    }

    public boolean hasAudio() {
        return true;
    }

    private boolean _blnSavingAudio = true;
    public boolean getSavingAudio() {
        if (!getSavingAudio_enabled())
            return false;
        return _blnSavingAudio;
    }
    public void setSavingAudio(boolean val) {
        _blnSavingAudio = val;
        firePossibleChange();
    }

    // .........................................................................

    public boolean getEmulatePsxAvSync() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void commandLineOptions(@Nonnull ArgParser ap, @Nonnull FeedbackStream fbs)
    {
        super.commandLineOptions(ap, fbs);
        if (!ap.hasRemaining())
            return;
        
        BooleanHolder noaud = ap.addBoolOption(false, "-noaud"); // Only with AVI & audio

        ap.match();

        setSavingAudio(!noaud.value);
    }

    @Override
    protected void makeHelpTable(@Nonnull TabularFeedback tfb) {
        super.makeHelpTable(tfb);

        tfb.newRow();
        tfb.addCell(I.CMD_VIDEO_NOAUD()).addCell(I.CMD_VIDEO_NOAUD_HELP());
    }

    @Override
    protected void printSelectedAudioOptions(@Nonnull FeedbackStream fbs) {
        fbs.println(I.EMBEDDED_CRUSADER_AUDIO_HZ(CrusaderPacketToFrameAndAudio.CRUSADER_SAMPLES_PER_SECOND));
    }

    public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();

        DiscItemCrusader.Demuxer av = _sourceVidItem.makeDemuxer(getAudioVolume());
        DiscItemCrusader.Demuxer ad;
        if (getSavingAudio()) {
            // TODO the saver will call attach twice, 
            // which works since the 2nd time will simply overwrite the first time
            // it's just kinda messy
            ad = av;
        } else {
            ad = null;
        }

        VideoSaver vs = new VideoSaver(_sourceVidItem, this, thisGeneratedFileListener, directory);
        vs.save(pl, av, ad);
    }

}
