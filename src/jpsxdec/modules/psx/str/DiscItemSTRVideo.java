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

package jpsxdec.modules.psx.str;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.modules.DiscItemSaver;
import jpsxdec.modules.DiscItemSerialization;
import jpsxdec.modules.DiscItem;
import jpsxdec.modules.DiscIndex;
import jpsxdec.modules.JPSXModule;
import jpsxdec.modules.IdentifiedSector;
import jpsxdec.modules.xa.DiscItemAudioStream;
import jpsxdec.util.Fraction;
import jpsxdec.util.NotThisTypeException;


public class DiscItemSTRVideo extends DiscItem {

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
    public String getTypeId() {
        return TYPE_ID;
    }

    public int getDiscSpeed() {
        return _iDiscSpeed;
    }

    public void setDiscSpeed(int iSpeed) {
        _iDiscSpeed = iSpeed;
    }

    public Fraction getSectorsPerFrame() {
        return new Fraction(_lngSectors, _lngPerFrame);
    }

    public int getPresentationStartSector() {
        return getStartSector() + _iFrame1LastSector;
    }

    /** @return Sector number where the frame begins. */
    public IVideoSector seek(int iFrame) throws IOException {
        // clamp the desired frame
        if (iFrame < getStartFrame())
            iFrame = getStartFrame();
        else if (iFrame > getEndFrame())
            iFrame = getEndFrame();
        // calculate an estimate where the frame will land
        double percent = (iFrame - getStartFrame()) / (double)(getEndFrame() - getStartFrame()+1);
        percent -= 0.05;
        // jump to the sector
        // hopefully land near the frame
        int iSect = (int)
                ( (getEndSector() - getStartSector()) * (percent) )
                + getStartSector();
        if (iSect < getStartSector()) iSect = getStartSector();

        // now seek ahead until we read the desired frame
        CdSector cdSect = getSourceCD().getSector(iSect);
        IdentifiedSector identifiedSect = JPSXModule.identifyModuleSector(cdSect);
        while ( (!(identifiedSect instanceof IVideoSector) ||
                ((IVideoSector)identifiedSect).getFrameNumber() < iFrame)
                &&
                iSect < getSourceCD().size())
        {
            iSect++;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = JPSXModule.identifyModuleSector(cdSect);
        }

        // in case we ended up past the desired frame, backup until we're
        // at the first sector of the desired frame
        while (!(identifiedSect instanceof IVideoSector) ||
               ((IVideoSector)identifiedSect).getFrameNumber() > iFrame ||
               ((IVideoSector)identifiedSect).getChunkNumber() > 0)
        {
            iSect--;
            cdSect = getSourceCD().getSector(iSect);
            identifiedSect = JPSXModule.identifyModuleSector(cdSect);
        }
        
        return (IVideoSector) identifiedSect;
    }

    @Override
    public DiscItemSaver getSaver() {
        return new STRVideoSaver(this);
    }




    private DiscItemAudioStream[] _aoAudioStreams;

    /** Called by module after index has been created. */
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
                    int i1Overlap = getOverlap((DiscItem)o1);
                    int i2Overlap = getOverlap((DiscItem)o2);
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
        int iSectorsInside = getOverlap(audioItem);
        if (iSectorsInside == 0)
            return false;
        
        // basically if the majority of audio is inside the video,
        // then they're parallel. However, this misses the odd case where
        // one audio stream engulfs two video streams (though it is unlikely
        // because you can't start an audio stream in the middle).
        int iSecotrsOutside = audioItem.getSectorLength() - iSectorsInside;

        return iSectorsInside > iSecotrsOutside;
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
