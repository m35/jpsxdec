/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012  Michael Sabin
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

import java.io.IOException;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import jpsxdec.discitems.savers.MediaPlayer;
import jpsxdec.discitems.savers.VideoSaverBuilderCrusader;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

/** Crusader: No Remorse audio/video stream. */
public class DiscItemCrusader extends DiscItemVideoStream {

    public static final String TYPE_ID = "Crusader";
    
    
    public DiscItemCrusader(int iStartSector, int iEndSector, 
                            int iWidth, int iHeight,
                            int iStartFrame, int iEndFrame) 
    {
        super(iStartSector, iEndSector,
              iWidth, iHeight,
              iStartFrame, iEndFrame);
    }
    
    public DiscItemCrusader(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
    }

    @Override
    public String getSerializationTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getInterestingDescription() {
        return getWidth() + "x" + getHeight() + ", " + (getEndFrame() - getStartFrame() + 1) + " frames";
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
        return new Fraction(10);
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector();
    }

    @Override
    public CrusaderDemuxer makeDemuxer() {
        // TODO: how to expose the audio volume
        return new CrusaderDemuxer(getWidth(), getHeight(), getStartSector(), getEndSector());
    }

    @Override
    public PlayController makePlayController() throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        CrusaderDemuxer demuxer = makeDemuxer();
        return new PlayController(new MediaPlayer(this, demuxer, demuxer, getStartSector(), getEndSector()));
    }

}
