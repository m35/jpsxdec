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

package jpsxdec.plugins.psx.alice;

import jpsxdec.plugins.psx.str.IVideoSector;
import jpsxdec.cdreaders.CDSector;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.util.NotThisTypeException;


/** Alice In Cyber Land frame chunk sector. */
public class SectorAliceFrameChunk extends SectorAliceFrameChunkNull
        implements IVideoSector 
{

    // .. Constructor .....................................................

    public SectorAliceFrameChunk(CDSector cdSector) throws NotThisTypeException
    {
        super(cdSector);
        // ingnore null frames between movies
        if (_iFrameNumber == 0xFFFF)
            throw new NotThisTypeException();
    }

    // .. Public functions .................................................

    public int getWidth() {
        return 320;
    }

    public int getHeight() {
        return 224;
    }

    @Override
    public int getSectorType() {
        return SECTOR_VIDEO;
    }

    @Override
    public String getTypeName() {
        return "Alice";
    }

    public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(prevSector.getClass().equals(prevSector.getClass())))
            return false;

        SectorAliceFrameChunk oAliceFrame = (SectorAliceFrameChunk) prevSector;
        if (getHeight() != oAliceFrame.getHeight() ||
            getWidth() != oAliceFrame.getWidth())
            return false;

        long lngNextChunkNum = prevSector.getChunkNumber()+1;
        long lngNextFrameNum = prevSector.getFrameNumber();
        if (lngNextChunkNum >= prevSector.getChunksInFrame()) {
            lngNextChunkNum = 0;
            lngNextFrameNum++;
        }
        return (lngNextChunkNum == getChunkNumber() &&
                lngNextFrameNum == getFrameNumber());
    }

    public DiscItem createMedia(int iStartSector, int iStartFrame, int iFrame1LastSector)
    {
        int iSectors = getSectorNumber() - iStartSector + 1;
        int iFrames = getFrameNumber() - iStartFrame + 1;
        return createMedia(iStartSector, iStartFrame, iFrame1LastSector, iSectors, iFrames);
    }
    public DiscItem createMedia(int iStartSector, int iStartFrame,
                                int iFrame1LastSector,
                                int iSectors, int iPerFrame)
    {
        return new DiscItemSTRVideo(iStartSector, getSectorNumber(),
                                    iStartFrame, getFrameNumber(),
                                    getWidth(), getHeight(),
                                    iSectors, iPerFrame,
                                    iFrame1LastSector);
    }

}

