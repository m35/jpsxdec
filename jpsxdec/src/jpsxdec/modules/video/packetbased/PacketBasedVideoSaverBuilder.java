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

package jpsxdec.modules.video.packetbased;

import argparser.BooleanHolder;
import java.io.File;
import java.util.logging.Level;
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
import jpsxdec.modules.video.ISectorClaimToFrameAndAudio;
import jpsxdec.modules.video.save.VideoSaver;
import jpsxdec.modules.video.save.VideoSaverBuilder;
import jpsxdec.util.ArgParser;
import jpsxdec.util.Fraction;
import jpsxdec.util.TaskCanceledException;

public class PacketBasedVideoSaverBuilder extends VideoSaverBuilder {

    @Nonnull
    private final DiscItemPacketBasedVideoStream _sourceVidItem;

    public PacketBasedVideoSaverBuilder(@Nonnull DiscItemPacketBasedVideoStream vidItem) {
        super(vidItem);
        _sourceVidItem = vidItem;
    }

    @Override
    public boolean copySettingsTo(@Nonnull DiscItemSaverBuilder otherBuilder) {
        if (super.copySettingsTo(otherBuilder)) {
            if (otherBuilder instanceof PacketBasedVideoSaverBuilder){
                PacketBasedVideoSaverBuilder other = (PacketBasedVideoSaverBuilder) otherBuilder;
                other.setSavingAudio(getSavingAudio());
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public @Nonnull DiscItemSaverBuilderGui getOptionPane() {
        return new PacketBasedVideoSaverBuilderGui(this);
    }

    // .........................................................................


    @Override
    public boolean getAudioVolume_enabled() {
        return getSavingAudio();
    }

    // .........................................................................

    public boolean getSavingAudio_enabled() {
        return hasAudio() && getVideoFormat().isVideo() && getSaveStartFrame() == null;
    }

    @Override
    public boolean hasAudio() {
        return _sourceVidItem.hasAudio();
    }

    private boolean _blnSavingAudio = true;
    @Override
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

    @Override
    public boolean getEmulatePsxAvSync() { // TODO refactor so this function isn't in packet-based
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
    protected void printSelectedAudioOptions(@Nonnull ILocalizedLogger log) {
        log.log(Level.INFO, I.CMD_EMBEDDED_PACKET_BASED_AUDIO_HZ(_sourceVidItem.getAudioSampleFramesPerSecond()));
    }

    @Override
    public void startSave(@Nonnull ProgressLogger pl, @CheckForNull File directory)
            throws LoggedFailure, TaskCanceledException
    {
        clearGeneratedFiles();
        printSelectedOptions(pl);

        ISectorClaimToFrameAndAudio sc2fa = _sourceVidItem.makeVideoAudioStream(getAudioVolume());

        Fraction sectorsPerFrame = Fraction.divide(DiscSpeed.DOUBLE.getSectorsPerSecond(),
                                                   _sourceVidItem.getFramesPerSecond());

        VideoSaver vs = new VideoSaver(_sourceVidItem, this, thisGeneratedFileListener, directory, pl, sc2fa,
                                       DiscSpeed.DOUBLE, sectorsPerFrame, _sourceVidItem.getStartSector());
        vs.save(pl);
    }

}
