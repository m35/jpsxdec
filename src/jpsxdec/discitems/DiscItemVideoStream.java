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
import jpsxdec.discitems.psxvideoencode.ReplaceFrames;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.player.PlayController;

public abstract class DiscItemVideoStream extends DiscItem {
    
    private static final String DIMENSIONS_KEY = "Dimensions";
    private static final String FRAMES_KEY = "Frames";
    
    /** Width of video in pixels. */
    private final int _iWidth;
    /** Height of video in pixels. */
    private final int _iHeight;
    /** First video frame number. */
    private final int _iStartFrame;
    /** Last video frame number. */
    private final int _iEndFrame;
    //@TODO: missing frames handling

    public DiscItemVideoStream(int iStartSector, int iEndSector, 
                            int iWidth, int iHeight,
                            int iStartFrame, int iEndFrame) 
    {
        super(iStartSector, iEndSector);
        _iWidth = iWidth;
        _iHeight = iHeight;
        _iStartFrame = iStartFrame;
        _iEndFrame = iEndFrame;
    }
    
    public DiscItemVideoStream(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        
        int[] ai = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = ai[0];
        _iHeight = ai[1];
        
        ai = fields.getIntRange(FRAMES_KEY);
        _iStartFrame = ai[0];
        _iEndFrame = ai[1];
    }
    
    @Override
    public DiscItemSerialization serialize() {
        DiscItemSerialization serial = super.superSerial(getSerializationTypeId());
        serial.addRange(FRAMES_KEY, _iStartFrame, _iEndFrame);
        serial.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        return serial;
    }
    
    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }
    
    public int getStartFrame() {
        return _iStartFrame;
    }

    public int getEndFrame() {
        return _iEndFrame;
    }
    public boolean shouldBeCropped() {
        return (_iHeight % 16) != 0 ||
               (_iWidth  % 16) != 0;
    }

    @Override
    public GeneralType getType() {
        return GeneralType.Video;
    }

    /** 1 for 1x (75 sectors/second), 2 for 2x (150 sectors/second), or -1 if unknown. */
    abstract public int getDiscSpeed();
    
    abstract public PlayController makePlayController() throws LineUnavailableException, UnsupportedAudioFileException, IOException;
    
    abstract public ISectorFrameDemuxer makeDemuxer();
    
    abstract public Fraction getSectorsPerFrame();
    
    abstract public int getPresentationStartSector();
    
    public static String formatFps(Fraction fps) {
        if (fps.getNumerator() % fps.getDenominator() == 0)
            return String.valueOf(fps.getNumerator() / fps.getDenominator());
        else {
            return String.format("%1.3f", fps.asDouble())
                    .replaceFirst("0+$", ""); // trim trailing zeros
        }
    }

    public static String formatTime(long lngSeconds) {
        long lngMin = lngSeconds / 60;
        StringBuilder sb = new StringBuilder();
        if (lngMin > 0) {
            sb.append(lngMin);
            sb.append(" min");
        }
        lngSeconds = lngSeconds % 60;
        if (lngSeconds > 0) {
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(lngSeconds);
            sb.append(" sec");
        }
        return sb.toString();
    }
    
    
    public void frameInfoDump(final FeedbackStream Feedback) throws IOException {
        DiscItemVideoStream vidItem = this;

        ISectorFrameDemuxer demuxer = makeDemuxer();
        demuxer.setFrameListener(new ISectorFrameDemuxer.ICompletedFrameListener() {
            public void frameComplete(IDemuxedFrame frame) throws IOException {
                frame.printStats(Feedback);
            }
        });

        for (int iSector = 0;
             iSector < vidItem.getSectorLength();
             iSector++)
        {
            try {
                IdentifiedSector sector = vidItem.getRelativeIdentifiedSector(iSector);
                if (sector != null) {
                    demuxer.feedSector(sector);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        demuxer.flush();

    }

    public void replaceFrames(FeedbackStream Feedback, String sXmlFile) 
            throws IOException, NotThisTypeException, MdecException
    {
        ReplaceFrames replacers = new ReplaceFrames(sXmlFile);
        replacers.replaceFrames(this, getSourceCd(), Feedback);
    }
}
