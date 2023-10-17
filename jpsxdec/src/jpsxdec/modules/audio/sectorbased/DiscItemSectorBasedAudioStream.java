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

package jpsxdec.modules.audio.sectorbased;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.cdreaders.CdException;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.formats.Signed16bitLittleEndianLinearPcmAudioInputStream;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.ILocalizedLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.audio.ISectorClaimToDecodedAudio;
import jpsxdec.modules.player.MediaPlayer;
import jpsxdec.util.IO;
import jpsxdec.util.IncompatibleException;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.player.PlayController;

/** Interface for DiscItems that represent a sector based audio stream.
 * This is necessary for the video items to utilize any audio stream that
 * runs parallel to the video. */
public abstract class DiscItemSectorBasedAudioStream extends DiscItem {

    private static final Logger LOG = Logger.getLogger(DiscItemSectorBasedAudioStream.class.getName());

    private boolean _blnIsPartOfVideo = false;

    public DiscItemSectorBasedAudioStream(@Nonnull ICdSectorReader cd,
                                          int iStartSector, int iEndSector)
    {
        super(cd, iStartSector, iEndSector);
    }

    public DiscItemSectorBasedAudioStream(@Nonnull ICdSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
    }

    public boolean overlaps(@Nonnull DiscItemSectorBasedAudioStream other) {
        //  [ this ]  < [ other ]
        if (getEndSector() + getSectorsPastEnd() < other.getStartSector())
            return false;
        //  [ other ] < [ this ]
        if (other.getEndSector() + other.getSectorsPastEnd() < getStartSector())
            return false;
        return true;
    }

    /** Returns if this audio stream is associated with a video stream. */
    public boolean isPartOfVideo() {
        return _blnIsPartOfVideo;
    }
    /** Sets if this audio stream is associated with a video stream. */
    public void setPartOfVideo(boolean bln) {
        _blnIsPartOfVideo = bln;
    }

    @Override
    public @Nonnull GeneralType getType() {
        return GeneralType.Audio;
    }

    abstract public long getSampleFrameCount();

    abstract public int getSampleFramesPerSecond();

    abstract public boolean isStereo();

    abstract public @CheckForNull DiscSpeed getDiscSpeed();

    abstract public @Nonnull ISectorClaimToSectorBasedDecodedAudio makeDecoder(double dblVolume);

    public @Nonnull ISectorClaimToDecodedAudio makeSectorClaimToDecodedAudio(double dblVolume) {
        ISectorClaimToSectorBasedDecodedAudio sectorBased = makeDecoder(dblVolume);
        return new SectorClaimToSectorBasedDecodedAudioAdaptor(sectorBased);
    }

    abstract public int getSectorsPastEnd();

    abstract public int getPresentationStartSector();

    /** Returns the approximate duration of the audio in seconds.
     *  Intended for use with audio playback progress bar. */
    abstract public double getApproxDuration();

    public ILocalizedMessage getDetails() {
        return new UnlocalizedMessage(serialize().serialize());
    }

    public @Nonnull PlayController makePlayController() {
        return new MediaPlayer(this, DiscSpeed.default2x(getDiscSpeed())).getPlayController();
    }

    @Override
    public @Nonnull SectorBasedAudioSaverBuilder makeSaverBuilder() {
        return new SectorBasedAudioSaverBuilder(this);
    }

    public boolean hasSameFormat(@Nonnull DiscItemSectorBasedAudioStream other) {
        return getSampleFramesPerSecond() == other.getSampleFramesPerSecond() && isStereo() == other.isStereo();
    }

    abstract public void replace(@Nonnull DiscPatcher patcher, @Nonnull File audioFile, @Nonnull ProgressLogger pl)
            throws IOException,
                   UnsupportedAudioFileException,
                   LocalizedIncompatibleException,
                   CdException.Read,
                   DiscPatcher.WritePatchException,
                   TaskCanceledException,
                   LoggedFailure;

    protected static @Nonnull Signed16bitLittleEndianLinearPcmAudioInputStream openForReplace(@Nonnull File audioFile,
                                                                                              int iExpectedSampleFramesPerSecond,
                                                                                              boolean blnExpectedStereo,
                                                                                              @Nonnull ILocalizedLogger log)
            throws IOException,
                   UnsupportedAudioFileException,
                   LocalizedIncompatibleException
    {
        Signed16bitLittleEndianLinearPcmAudioInputStream ais;
        try {
            ais = Signed16bitLittleEndianLinearPcmAudioInputStream.getAudioInputStream(audioFile);
        } catch (IncompatibleException ex) {
            throw new LocalizedIncompatibleException(I.AUDIO_INVALID_FORMAT(), ex);
        }

        boolean blnFormatEquals = iExpectedSampleFramesPerSecond == ais.getSampleFramesPerSecond() &&
                                  blnExpectedStereo == ais.isStereo();

        if (!blnFormatEquals) {
            IO.closeSilently(ais, LOG);
            throw new LocalizedIncompatibleException(
            I.AUDIO_REPLACE_FORMAT_MISMATCH(
                ais.getSampleFramesPerSecond(),    ais.isStereo() ? 2 : 1,
                iExpectedSampleFramesPerSecond, blnExpectedStereo ? 2 : 1
            ));
        }

        return ais;
    }
}
