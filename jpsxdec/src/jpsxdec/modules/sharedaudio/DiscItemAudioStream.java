/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2019  Michael Sabin
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

package jpsxdec.modules.sharedaudio;

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.UnlocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.modules.player.MediaPlayer;
import jpsxdec.util.player.PlayController;

/** Interface for all DiscItems that represent an audio stream.
 * This is necessary for the video items to utilize any audio stream that
 * runs parallel to the video. */
public abstract class DiscItemAudioStream extends DiscItem {

    private boolean _blnIsPartOfVideo = false;

    public DiscItemAudioStream(@Nonnull CdFileSectorReader cd,
                               int iStartSector, int iEndSector)
    {
        super(cd, iStartSector, iEndSector);
    }

    public DiscItemAudioStream(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
    }

    public boolean overlaps(@Nonnull DiscItemAudioStream other) {
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

    /** 1 for 1x (75 sectors/second)
     *  2 for 2x (150 sectors/second)
     *  {@code <= 0} if unknown. */
    abstract public int getDiscSpeed();

    /** Creates a decoder capable of converting IdentifiedSectors into audio
     * data which will then be fed to a {@link ISectorAudioDecoder.ISectorTimedAudioWriter}.
     * @see ISectorAudioDecoder#setAudioListener(ISectorAudioDecoder.ISectorTimedAudioWriter)  */
    abstract public @Nonnull ISectorAudioDecoder makeDecoder(double dblVolume);

    abstract public int getSectorsPastEnd();

    abstract public int getPresentationStartSector();

    /** Returns the approximate duration of the audio in seconds.
     *  Intended for use with audio playback progress bar. */
    abstract public double getApproxDuration();

    public ILocalizedMessage getDetails() {
        return new UnlocalizedMessage(serialize().serialize());
    }

    public @Nonnull PlayController makePlayController() {
        return new PlayController(new MediaPlayer(this));
    }

    @Override
    public @Nonnull AudioSaverBuilder makeSaverBuilder() {
        return new AudioSaverBuilder(this);
    }

    public boolean hasSameFormat(@Nonnull DiscItemAudioStream other) {
        return getSampleFramesPerSecond() == other.getSampleFramesPerSecond() && isStereo() == other.isStereo();
    }

}
