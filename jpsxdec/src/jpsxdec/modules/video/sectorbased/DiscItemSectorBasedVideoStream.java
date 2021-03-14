/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2020  Michael Sabin
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.IndexId;
import jpsxdec.discitems.SerializedDiscItem;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.i18n.exception.LocalizedDeserializationFail;
import jpsxdec.i18n.exception.LoggedFailure;
import jpsxdec.i18n.log.DebugLogger;
import jpsxdec.i18n.log.ProgressLogger;
import jpsxdec.modules.IIdentifiedSector;
import jpsxdec.modules.SectorClaimSystem;
import jpsxdec.modules.player.MediaPlayer;
import jpsxdec.modules.sharedaudio.DiscItemAudioStream;
import jpsxdec.modules.sharedaudio.ISectorAudioDecoder;
import jpsxdec.modules.video.AudioStreamsCombiner;
import jpsxdec.modules.video.Dimensions;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.IDemuxedFrame;
import jpsxdec.modules.video.ISectorClaimToDemuxedFrame;
import jpsxdec.modules.video.ParallelAudio;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.replace.ReplaceFrames;
import jpsxdec.modules.xa.DiscItemXaAudioStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.Misc;
import jpsxdec.util.TaskCanceledException;
import jpsxdec.util.player.PlayController;

/** Represents generic sector-based PlayStation video streams
 * (as opposed to non-sector-based streams). */
public abstract class DiscItemSectorBasedVideoStream extends DiscItemVideoStream {

    @Nonnull
    private final SectorBasedVideoInfo _vidInfo;

    private static final String DISC_SPEED_KEY = "Disc Speed";
    private int _iDiscSpeed = -1;

    private final ParallelAudio _parallelAudio = new ParallelAudio();

    public DiscItemSectorBasedVideoStream(@Nonnull CdFileSectorReader cd,
                                          int iStartSector, int iEndSector,
                                          @Nonnull Dimensions dim,
                                          @Nonnull IndexSectorFrameNumber.Format indexSectorFrameNumberFormat,
                                          @Nonnull SectorBasedVideoInfo vidInfo)
    {
        super(cd, iStartSector, iEndSector, dim, indexSectorFrameNumberFormat);
        _vidInfo = vidInfo;
    }

    public DiscItemSectorBasedVideoStream(@Nonnull CdFileSectorReader cd,
                                          @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _vidInfo = new SectorBasedVideoInfo(fields);
        _iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _vidInfo.serialize(serial);

        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed > 0)
            serial.addNumber(DISC_SPEED_KEY, iDiscSpeed);

        return serial;
    }

    @Override
    public int getDiscSpeed() {
        if (_iDiscSpeed > 0)
            return _iDiscSpeed;
        int iAudioDiscSpeed = _parallelAudio.getAudioDiscSpeed();
        if (iAudioDiscSpeed > 0)
            return iAudioDiscSpeed;
        return -1;
    }

    @Override
    final public int getAbsolutePresentationStartSector() {
        return getStartSector() + _vidInfo.getFirstFrameLastSector();
    }

    @Override
    final public @Nonnull Fraction getSectorsPerFrame() {
        return _vidInfo.getSectorsPerFrame();
    }

    @Override
    final public boolean addChild(@Nonnull DiscItem other) {
        return _parallelAudio.addChild(other, getParentRating(other));
    }

    @Override
    final public boolean setIndexId(@Nonnull IndexId id) {
        super.setIndexId(id);
        _parallelAudio.setIndexId(id.createChild());
        return true;
    }

    @Override
    final public int getChildCount() {
        return _parallelAudio.getCount();
    }

    @Override
    final public @Nonnull Iterable<DiscItemAudioStream> getChildren() {
        return _parallelAudio.getChildren();
    }

    final public boolean hasAudio() {
        return _parallelAudio.hasAudio();
    }

    final public @CheckForNull List<DiscItemAudioStream> getParallelAudioStreams() {
        return _parallelAudio.getParallelAudioStreams();
    }

    @Override
    final public double getApproxDuration() {
        int iDiscSpeed = getDiscSpeed();
        if (iDiscSpeed < 1)
            iDiscSpeed = 2;
        return getSectorLength() / (double)(iDiscSpeed * 75);
    }

    @Override
    final public @Nonnull ILocalizedMessage getInterestingDescription() {
        int iDiscSpeed = getDiscSpeed();
        int iFrameCount = getFrameCount();
        if (iDiscSpeed > 0) {
            int iSectorsPerSecond = iDiscSpeed * 75;
            Date secs = Misc.dateFromSeconds(Math.max(getSectorLength() / iSectorsPerSecond, 1));
            return I.GUI_STR_VIDEO_DETAILS(
                          getWidth(), getHeight(),
                          iFrameCount,
                          Fraction.divide(iSectorsPerSecond, getSectorsPerFrame()).asDouble(),
                          secs);
        } else {
            Date secs150 = Misc.dateFromSeconds(Math.max(getSectorLength() / 150, 1));
            Date secs75 = Misc.dateFromSeconds(Math.max(getSectorLength() / 75, 1));
            return I.GUI_STR_VIDEO_DETAILS_UNKNOWN_FPS(
                          getWidth(), getHeight(),
                          iFrameCount,
                          Fraction.divide(150, getSectorsPerFrame()).asDouble(),
                          secs150,
                          Fraction.divide(75, getSectorsPerFrame()).asDouble(),
                          secs75);
        }
    }


    @Override
    public @Nonnull SectorBasedVideoSaverBuilder makeSaverBuilder() {
        return new SectorBasedVideoSaverBuilder(this);
    }

    public @CheckForNull List<DiscItemAudioStream> getLongestNonIntersectingAudioStreams() {
        return _parallelAudio.getLongestNonIntersectingAudioStreams();
    }

    /** Finds where to split an audio item in relation to this item.
     * @return -1 if audio should not be split. */
    abstract public int findAudioSplitPoint(@Nonnull DiscItemXaAudioStream audio);

    abstract public void fpsDump(@Nonnull PrintStream ps) throws IOException;

    public void fpsDump2(@Nonnull final PrintStream ps) throws CdFileSectorReader.CdReadException {
        ISectorClaimToDemuxedFrame demuxer = makeDemuxer();
        demuxer.setFrameListener(new IDemuxedFrame.Listener() {
            @Override
            public void frameComplete(IDemuxedFrame frame) {
                ps.println((frame.getStartSector()-getStartSector())+"-"+
                           (frame.getEndSector()-getStartSector()));
            }
        });
        SectorClaimSystem it = createClaimSystem();
        demuxer.attachToSectorClaimer(it);
        while (it.hasNext()) {
            IIdentifiedSector isect = it.next(DebugLogger.Log);
        }
        it.close(DebugLogger.Log);
    }


    @Override
    public @Nonnull PlayController makePlayController() {

        MediaPlayer mp;
        if (hasAudio()) {

            List<DiscItemAudioStream> audios = _parallelAudio.getLongestNonIntersectingAudioStreams();
            assert audios != null;

            ISectorAudioDecoder decoder;
            if (audios.size() == 1)
                decoder = audios.get(0).makeDecoder(1.0);
            else
                decoder = new AudioStreamsCombiner(audios, 1.0);

            int iStartSector = Math.min(decoder.getStartSector(), getStartSector());
            int iEndSector = Math.max(decoder.getEndSector(), getEndSector());

            mp = new MediaPlayer(this, makeDemuxer(), decoder, iStartSector, iEndSector);
        } else {
            mp = new MediaPlayer(this, makeDemuxer());
        }
        return mp.getPlayController();
    }

    public void replaceFrames(@Nonnull ProgressLogger pl, @Nonnull String sXmlFile)
            throws LoggedFailure, TaskCanceledException
    {
        ReplaceFrames replacers;
        try {
            replacers = new ReplaceFrames(sXmlFile);
        } catch (ReplaceFrames.XmlFileNotFoundException ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    I.IO_OPENING_FILE_NOT_FOUND_NAME(sXmlFile), ex);
        } catch (ReplaceFrames.XmlReadException ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    I.IO_READING_FILE_ERROR_NAME(sXmlFile), ex);
        } catch (LocalizedDeserializationFail ex) {
            throw new LoggedFailure(pl, Level.SEVERE,
                                    ex.getSourceMessage(), ex);
        }
        replacers.replaceFrames(this, getSourceCd(), pl);
    }

}
