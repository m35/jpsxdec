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

package jpsxdec.modules.video.sectorbased;

import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.DiscPatcher;
import jpsxdec.cdreaders.DiscSpeed;
import jpsxdec.cdreaders.ICdSectorReader;
import jpsxdec.discitems.Dimensions;
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
import jpsxdec.modules.audio.sectorbased.DiscItemSectorBasedAudioStream;
import jpsxdec.modules.audio.sectorbased.ISectorClaimToSectorBasedDecodedAudio;
import jpsxdec.modules.player.MediaPlayer;
import jpsxdec.modules.video.DiscItemVideoStream;
import jpsxdec.modules.video.framenumber.IndexSectorFrameNumber;
import jpsxdec.modules.video.replace.ReplaceFrames;
import jpsxdec.modules.xa.DiscItemXaAudioStream;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.psxvideo.mdec.ParsedMdecImage;
import jpsxdec.util.BinaryDataNotRecognized;
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
    @CheckForNull
    private DiscSpeed _discSpeed;

    private final ParallelAudio _parallelAudio = new ParallelAudio();

    public DiscItemSectorBasedVideoStream(@Nonnull ICdSectorReader cd,
                                          int iStartSector, int iEndSector,
                                          @Nonnull Dimensions dim,
                                          @Nonnull IndexSectorFrameNumber.Format indexSectorFrameNumberFormat,
                                          @Nonnull SectorBasedVideoInfo vidInfo)
    {
        super(cd, iStartSector, iEndSector, dim, indexSectorFrameNumberFormat);
        _vidInfo = vidInfo;
    }

    public DiscItemSectorBasedVideoStream(@Nonnull ICdSectorReader cd,
                                          @Nonnull SerializedDiscItem fields)
            throws LocalizedDeserializationFail
    {
        super(cd, fields);
        _vidInfo = new SectorBasedVideoInfo(fields);

        int iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
        switch (iDiscSpeed) {
            case 1: _discSpeed = DiscSpeed.SINGLE; break;
            case 2: _discSpeed = DiscSpeed.DOUBLE; break;
            default: _discSpeed = null;
        }
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        _vidInfo.serialize(serial);

        DiscSpeed discSpeed = getDiscSpeed();
        if (discSpeed != null)
            serial.addNumber(DISC_SPEED_KEY, discSpeed.getSpeed());

        return serial;
    }

    public @CheckForNull DiscSpeed getDiscSpeed() {
        if (_discSpeed != null)
            return _discSpeed;
        return  _parallelAudio.getAudioDiscSpeed();
    }

    final public int getAbsolutePresentationStartSector() {
        return getStartSector() + _vidInfo.getFirstFrameLastSector();
    }

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
    final public @Nonnull Iterable<DiscItemSectorBasedAudioStream> getChildren() {
        return _parallelAudio.getChildren();
    }

    final public boolean hasAudio() {
        return _parallelAudio.hasAudio();
    }

    final public @CheckForNull List<DiscItemSectorBasedAudioStream> getParallelAudioStreams() {
        return _parallelAudio.getParallelAudioStreams();
    }

    @Override
    final public @Nonnull ILocalizedMessage getInterestingDescription() {
        DiscSpeed discSpeed = getDiscSpeed();
        int iFrameCount = getFrameCount();
        if (discSpeed != null) {
            int iSectorsPerSecond = discSpeed.getSectorsPerSecond();
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

    public @CheckForNull List<DiscItemSectorBasedAudioStream> getLongestNonIntersectingAudioStreams() {
        return _parallelAudio.getLongestNonIntersectingAudioStreams();
    }

    /** Finds where to split an audio item in relation to this item.
     * @return -1 if audio should not be split. */
    abstract public int findAudioSplitPoint(@Nonnull DiscItemXaAudioStream audio);

    abstract public @Nonnull SectorClaimToSectorBasedDemuxedFrame makeDemuxer();

    @Override
    public @Nonnull PlayController makePlayController() {

        DiscSpeed discSpeed = DiscSpeed.default2x(getDiscSpeed());

        final ISectorClaimToSectorBasedDecodedAudio audio;
        if (hasAudio()) {
            List<DiscItemSectorBasedAudioStream> audios = _parallelAudio.getLongestNonIntersectingAudioStreams();
            assert audios != null;

            if (audios.size() == 1)
                audio = audios.get(0).makeDecoder(1.0);
            else
                audio = new SectorBasedAudioStreamsCombiner(audios, 1.0);
        } else {
            audio = null;
        }

        SectorClaimToSectorBasedFrameAndAudio sc2fa = new SectorClaimToSectorBasedFrameAndAudio(makeDemuxer(), getStartSector(), getEndSector(), audio);

        MediaPlayer mp = new MediaPlayer(this, sc2fa, discSpeed);
        return mp.getPlayController();
    }

    public void replaceFrames(@Nonnull DiscPatcher patcher, @Nonnull String sXmlFile, @Nonnull ProgressLogger pl)
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
        replacers.replaceFrames(this, patcher, pl);
    }

    final public void frameInfoDump(@Nonnull final PrintStream ps, final boolean blnMore) {
        SectorClaimToSectorBasedDemuxedFrame demuxer = makeDemuxer();

        demuxer.setFrameListener(new ISectorBasedDemuxedFrame.Listener() {
            @Override
            public void frameComplete(ISectorBasedDemuxedFrame frame) {
                MdecInputStream mis = frame.getCustomFrameMdecStream();
                try {

                    if (mis != null) {
                        ps.println(frame);

                        ps.println("Frame data info: " + mis);
                        if (blnMore) {
                            ParsedMdecImage parsed = new ParsedMdecImage(mis, getWidth(), getHeight());
                            parsed.drawMacroBlocks(ps);
                        }
                    } else {
                        try {
                            SectorBasedFrameAnalysis frameAnalysis = SectorBasedFrameAnalysis.create(frame);
                            frameAnalysis.printInfo(ps);
                            if (blnMore)
                                frameAnalysis.drawMacroBlocks(ps);
                        } catch (BinaryDataNotRecognized ex) {
                            ps.println("Frame not recognized");
                            ex.printStackTrace(ps);
                        }
                    }
                } catch (MdecException.EndOfStream | MdecException.ReadCorruption ex) {
                    ex.printStackTrace(ps);
                }

                System.out.println("_____________________________________________________________________");
            }
        });

        System.out.println(this);
        SectorClaimSystem it = createClaimSystem();
        demuxer.attachToSectorClaimer(it);
        try {
            while (it.hasNext()) {
                IIdentifiedSector sector = it.next(DebugLogger.Log);
            }
            it.flush(DebugLogger.Log);
        } catch (Exception ex) {
            throw new RuntimeException("Error with dev tool", ex);
        }
    }

}
