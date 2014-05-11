/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2014  Michael Sabin
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
import jpsxdec.I18N;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.VideoSaverBuilderCrusader;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

/** Crusader: No Remorse audio/video stream. */
public class DiscItemCrusader extends DiscItemVideoStream {

    public static final String TYPE_ID = "Crusader";
    private static final Fraction SECTORS_PER_FRAME = new Fraction(10);
    private static final int FPS = 15;
    
    private static final String FRAMES_KEY = "Frames";
    /** First video frame number. */
    private final int _iStartFrame;
    /** Last video frame number. */
    private final int _iEndFrame;
    
    public DiscItemCrusader(int iStartSector, int iEndSector, 
                            int iWidth, int iHeight,
                            int iFrameCount,
                            int iStartFrame, int iEndFrame)
    {
        super(iStartSector, iEndSector,
              iWidth, iHeight,
              iFrameCount);
        _iStartFrame = iStartFrame;
        _iEndFrame = iEndFrame;
    }
    
    public DiscItemCrusader(SerializedDiscItem fields) throws NotThisTypeException {
        super(fields);

        int[] ai = fields.getIntRange(FRAMES_KEY);
        _iStartFrame = ai[0];
        _iEndFrame = ai[1];
    }

    @Override
    public SerializedDiscItem serialize() {
        SerializedDiscItem serial = super.serialize();
        serial.addRange(FRAMES_KEY, _iStartFrame, _iEndFrame);
        return serial;
    }

    @Override
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    public int getStartFrame() {
        return _iStartFrame;
    }

    public int getEndFrame() {
        return _iEndFrame;
    }

    @Override
    public String getFrameNumberFormat() {
        int iDigitCount = String.valueOf(_iEndFrame).length();
        return "%0" + String.valueOf(iDigitCount) + 'd';
    }

    @Override
    public String getInterestingDescription() {
        int iFrames = getFrameCount();
        Date secs = new Date(0, 0, 0, 0, 0, Math.max(iFrames / FPS, 1));
        return I18N.S("{0,number,#}x{1,number,#}, {2,number,#} frames, {3,number,#} fps = {4,time,m:ss}", // I18N
                             getWidth() ,getHeight(),
                             iFrames, FPS, secs);
    }
    
    @Override
    public DiscItemSaverBuilder makeSaverBuilder() {
        return new VideoSaverBuilderCrusader(this);
    }
    
    @Override
    public int getDiscSpeed() {
        return 2; // pretty sure it plays back at 2x
    }

    @Override
    public Fraction getSectorsPerFrame() {
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
    public CrusaderDemuxer makeDemuxer() {
        // TODO: how to expose the audio volume
        return new CrusaderDemuxer(getWidth(), getHeight(), getStartSector(), getEndSector());
    }

    @Override
    public PlayController makePlayController() {
        CrusaderDemuxer demuxer = makeDemuxer();
        return new PlayController(new MediaPlayer(this, demuxer, demuxer, getStartSector(), getEndSector()));
    }

}
