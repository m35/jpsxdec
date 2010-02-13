/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2010  Michael Sabin
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

package jpsxdec.plugins.psx.str;

import jpsxdec.plugins.DiscItemStreaming;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItemSaver;
import jpsxdec.plugins.DiscItemSerialization;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.DiscIndex;
import jpsxdec.plugins.JPSXPlugin;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.plugins.xa.DiscItemAudioStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;


public class DiscItemSTRVideo extends DiscItemStreaming {

    private static final Logger log = Logger.getLogger(DiscItemSTRVideo.class.getName());

    public static final String TYPE_ID = "Video";
    
    private static final String FRAMES_KEY = "Frames";
    private static final String DIMENSIONS_KEY = "Dimentions";
    private static final String DISC_SPEED_KEY = "Disc Speed";
    private static final String SECTORSPERFRAME_KEY = "Sectors/Frame";
    private static final String FRAME1_LAST_SECTOR_KEY = "Frame 1 last sector";

    
    /** Width of video in pixels. */
    private final int _iWidth;
    /** Height of video in pixels. */
    private final int _iHeight;
    
    /** First video frame number. */
    private final int _iStartFrame;
    /** Last video frame number. */
    private final int _iEndFrame;

    private final long _lngSectors;
    private final long _lngPerFrame;

    private final int _iFrame1LastSector;

    private int _iDiscSpeed;

    public DiscItemSTRVideo(int iStartSector, int iEndSector,
                            int lngStartFrame, int lngEndFrame,
                            int lngWidth, int lngHeight,
                            int iSectors, int iPerFrame,
                            int iFrame1LastSector)
    {
        super(iStartSector, iEndSector);

        _iStartFrame = lngStartFrame;
        _iEndFrame = lngEndFrame;
        _iWidth = lngWidth;
        _iHeight = lngHeight;
        _lngSectors = iSectors;
        _lngPerFrame = iPerFrame;
        _iFrame1LastSector = iFrame1LastSector;
        _iDiscSpeed = -1;
    }

    public DiscItemSTRVideo(DiscItemSerialization fields) throws NotThisTypeException {
        super(fields);
        
        int[] ai = fields.getDimensions(DIMENSIONS_KEY);
        _iWidth = ai[0];
        _iHeight = ai[1];
        
        ai = fields.getIntRange(FRAMES_KEY);
        _iStartFrame = ai[0];
        _iEndFrame = ai[1];

        long[] alng = fields.getFraction(SECTORSPERFRAME_KEY);
        _lngSectors = alng[0];
        _lngPerFrame = alng[1];

        _iFrame1LastSector = fields.getInt(FRAME1_LAST_SECTOR_KEY);

        _iDiscSpeed = fields.getInt(DISC_SPEED_KEY, -1);
    }

    public DiscItemSerialization serialize() {
        DiscItemSerialization oSerial = super.superSerial(TYPE_ID);
        oSerial.addRange(FRAMES_KEY, _iStartFrame, _iEndFrame);
        oSerial.addDimensions(DIMENSIONS_KEY, _iWidth, _iHeight);
        oSerial.addFraction(SECTORSPERFRAME_KEY, _lngSectors, _lngPerFrame);
        oSerial.addNumber(FRAME1_LAST_SECTOR_KEY, _iFrame1LastSector);
        if (_iDiscSpeed > 0)
            oSerial.addNumber(DISC_SPEED_KEY, _iDiscSpeed);
        return oSerial;
    }
    
    public int getStartFrame() {
        return _iStartFrame;
    }

    public int getEndFrame() {
        return _iEndFrame;
    }

    public int getWidth() {
        return _iWidth;
    }

    public boolean shouldBeCropped() {
        return (_iHeight % 16) != 0 ||
               (_iWidth  % 16) != 0;
    }

    public int getHeight() {
        return _iHeight;
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_VIDEO;
    }

    @Override
    public String getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int getDiscSpeed() {
        return _iDiscSpeed;
    }

    @Override
    public void setDiscSpeed(int iSpeed) {
        _iDiscSpeed = iSpeed;
    }

    public Fraction getSectorsPerFrame() {
        return new Fraction(_lngSectors, _lngPerFrame);
    }

    @Override
    public int getPresentationStartSector() {
        return getStartSector() + _iFrame1LastSector;
    }

    /** @return Sector number where the frame begins. */
    public IVideoSector seek(long lngFrame) throws IOException {
        // clamp the desired frame
        if (lngFrame < getStartFrame())
            lngFrame = (int)getStartFrame();
        else if (lngFrame > getEndFrame())
            lngFrame = (int)getEndFrame();
        // calculate an estimate where the frame will land
        double percent = (lngFrame - getStartFrame()) / (double)(getEndFrame() - getStartFrame());
        percent -= 0.05;
        // jump to the sector
        // hopefully land near the frame
        int iSect = (int)
                ( (getEndSector() - getStartSector()) * (percent) )
                + getStartSector();
        if (iSect < getStartSector()) iSect = getStartSector();

        // now seek ahead until we read the desired frame
        CDSector cdSect = getSourceCD().getSector(iSect);
        IdentifiedSector identifiedSect = JPSXPlugin.identifyPluginSector(cdSect);
        while ( (!(identifiedSect instanceof IVideoSector) ||
                ((IVideoSector)identifiedSect).getFrameNumber() < lngFrame)
                &&
                iSect < getSourceCD().size())
        {
            iSect++;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = JPSXPlugin.identifyPluginSector(cdSect);
        }

        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(identifiedSect instanceof IVideoSector) ||
               ((IVideoSector)identifiedSect).getFrameNumber() > lngFrame ||
               ((IVideoSector)identifiedSect).getChunkNumber() > 0)
        {
            iSect--;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = JPSXPlugin.identifyPluginSector(cdSect);
        }
        
        return (IVideoSector) identifiedSect;
    }

    @Override
    public DiscItemSaver getSaver() {
        return new STRVideoSaver(this);
    }




    private DiscItemAudioStream[] _aoAudioStreams;

    /** Called by plugin after index has been created. */
    /*package private*/void collectParallelAudio(DiscIndex index) {
        ArrayList<DiscItemAudioStream> parallelAudio = new ArrayList<DiscItemAudioStream>();
        for (DiscItem audioItem : index) {
            if (audioItem instanceof DiscItemAudioStream) {
                if (isAudioVideoAligned(audioItem)) {
                    parallelAudio.add((DiscItemAudioStream)audioItem);
                    if (log.isLoggable(Level.INFO))
                        log.info("Parallel audio: " + audioItem.toString());
                }
            }
        }
        if (parallelAudio.size() > 0) {
            if (log.isLoggable(Level.INFO))
                log.info("Added to this media item " + this.toString());
            _aoAudioStreams = parallelAudio.toArray(new DiscItemAudioStream[parallelAudio.size()]);

            // sort the parallel audio streams by size, in descending order
            Arrays.sort(_aoAudioStreams, new Comparator<DiscItemAudioStream>() {
                public int compare(DiscItemAudioStream o1, DiscItemAudioStream o2) {
                    int i1Overlap = overlap((DiscItem)o1);
                    int i2Overlap = overlap((DiscItem)o2);
                    if (i1Overlap > i2Overlap)
                        return -1;
                    else if (i1Overlap < i2Overlap)
                        return 1;
                    else return 0;
                }
            });

        }
    }

    public boolean isAudioVideoAligned(DiscItem audioItem) {
        int iSectorsInside = overlap(audioItem);
        if (iSectorsInside == 0)
            return false;
        
        int iSecotrsOutside = audioItem.getSectorLength() - iSectorsInside;

        return iSectorsInside > iSecotrsOutside;
    }

    public int overlap(DiscItem audioItem) {
        final int iVidStart = getStartSector();
        final int iVidEnd = getEndSector();
        final int iAudStart = audioItem.getStartSector();
        final int iAudEnd = audioItem.getEndSector();

        // basically if the majority of audio is inside the video,
        // then they're parallel. However, this misses the odd case where
        // one audio stream engulfs two video streams (though it is unlikely
        // because you can't start an audio stream in the middle).
        if (iAudStart > iVidEnd || iAudEnd < iVidStart)
            return 0;

        int iInsideStart;
        if (iAudStart < iVidStart)
            iInsideStart = iVidStart;
        else
            iInsideStart = iAudStart;

        int iInsideEnd;
        if (iAudEnd > iVidEnd)
            iInsideEnd = iVidEnd;
        else
            iInsideEnd = iAudEnd;

        return iInsideEnd - iInsideStart + 1;
    }

    public boolean hasAudio() {
        return _aoAudioStreams != null && _aoAudioStreams.length > 0;
    }

    public int getParallelAudioStreamCount() {
        return _aoAudioStreams == null ? 0 : _aoAudioStreams.length;
    }

    public DiscItemAudioStream getParallelAudioStream(int i) {
        if (i < 0 || i >= getParallelAudioStreamCount())
            throw new IllegalArgumentException("Video doens't have parllel audio stream " + i);

        return _aoAudioStreams[i];
    }

    public List<DiscItemAudioStream> getParallelAudio(boolean[] ablnFlags) {
        if (ablnFlags.length < getParallelAudioStreamCount())
            throw new IllegalArgumentException();
        
        ArrayList<DiscItemAudioStream> selected = new ArrayList<DiscItemAudioStream>(_aoAudioStreams.length);
        for (int i = 0; i < getParallelAudioStreamCount(); i++) {
            if (ablnFlags[i]) {
                selected.add(_aoAudioStreams[i]);
            }
        }
        selected.trimToSize();
        return selected;
    }

}
