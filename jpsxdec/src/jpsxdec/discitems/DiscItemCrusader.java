/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2017  Michael Sabin
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

package jpsxdec.discitems;

import java.util.Date;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.VideoSaverBuilderCrusader;
import jpsxdec.i18n.I;
import jpsxdec.i18n.ILocalizedMessage;
import jpsxdec.util.DeserializationFail;
import jpsxdec.util.Fraction;
import jpsxdec.util.player.PlayController;

/** Crusader: No Remorse audio/video stream. */
public class DiscItemCrusader extends DiscItemVideoStream {

    public static final String TYPE_ID = "Crusader";
    private static final Fraction SECTORS_PER_FRAME = new Fraction(10);
    private static final int FPS = 15;
    
    private static final String FRAMES_KEY = "Frames";
    /** First video frame number. */
    @Nonnull
    private final FrameNumber _startFrame;
    /** Last video frame number. */
    @Nonnull
    private final FrameNumber _endFrame;
    
    public DiscItemCrusader(@Nonnull CdFileSectorReader cd,
                            int iStartSector, int iEndSector,
                            int iWidth, int iHeight,
                            int iFrameCount,
                            @Nonnull FrameNumberFormat frameNumberFormat,
                            @Nonnull FrameNumber startFrame,
                            @Nonnull FrameNumber endFrame)
    {
        super(cd, iStartSector, iEndSector,
              iWidth, iHeight,
              iFrameCount,
              frameNumberFormat);
        _startFrame = startFrame;
        _endFrame = endFrame;
    }
    
    public DiscItemCrusader(@Nonnull CdFileSectorReader cd, @Nonnull SerializedDiscItem fields)
            throws DeserializationFail
    {
        super(cd, fields);

        FrameNumber[] ao = FrameNumber.parseRange(fields.getString(FRAMES_KEY));
        _startFrame = ao[0];
        _endFrame = ao[1];
    }

    @Override
    public @Nonnull SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addString(FRAMES_KEY, FrameNumber.toRange(_startFrame, _endFrame));
        return serial;
    }

    @Override
    public @Nonnull String getSerializationTypeId() {
        return TYPE_ID;
    }

    public @Nonnull FrameNumber getStartFrame() {
        return _startFrame;
    }

    public @Nonnull FrameNumber getEndFrame() {
        return _endFrame;
    }

    @Override
    public @Nonnull ILocalizedMessage getInterestingDescription() {
        int iFrames = getFrameCount();
        Date secs = new Date(0, 0, 0, 0, 0, Math.max(iFrames / FPS, 1));
        return I.GUI_CRUSADER_VID_DETAILS(getWidth() ,getHeight(), iFrames, FPS, secs);
    }
    
    @Override
    public @Nonnull DiscItemSaverBuilder makeSaverBuilder() {
        return new VideoSaverBuilderCrusader(this);
    }
    
    @Override
    public int getDiscSpeed() {
        return 2; // pretty sure it plays back at 2x
    }

    @Override
    public @Nonnull Fraction getSectorsPerFrame() {
        return SECTORS_PER_FRAME;
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector();
    }

    @Override
    public double getApproxDuration() {
        return getFrameCount() / (double)FPS;
    }

    @Override
    public @Nonnull CrusaderDemuxer makeDemuxer() {
        // TODO: how to expose the audio volume
        return new CrusaderDemuxer(getWidth(), getHeight(), getStartSector(), getEndSector());
    }

    @Override
    public @Nonnull PlayController makePlayController() {
        CrusaderDemuxer demuxer = makeDemuxer();
        return new PlayController(new MediaPlayer(this, demuxer, demuxer, getStartSector(), getEndSector()));
    }

}
