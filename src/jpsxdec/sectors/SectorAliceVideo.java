/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.sectors;

import java.io.IOException;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemVideoStream;
import jpsxdec.util.IO;


/** Alice In Cyber Land frame chunk sector. */
public class SectorAliceVideo extends SectorAliceNullVideo
        implements IVideoSector 
{


    public SectorAliceVideo(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // ingnore null frames between movies
        if (_iFrameNumber == 0xFFFF) return;

        setProbability(100);
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

        SectorAliceVideo prevAlice = (SectorAliceVideo) prevSector;
        if (getHeight() != prevAlice.getHeight() ||
            getWidth() != prevAlice.getWidth())
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
        return new DiscItemVideoStream(iStartSector, getSectorNumber(),
                                    iStartFrame, getFrameNumber(),
                                    getWidth(), getHeight(),
                                    iSectors, iPerFrame,
                                    iFrame1LastSector);
    }

    public int replaceFrameData(CdFileSectorReader cd,
                                byte[] abDemuxData, int iDemuxOfs, 
                                int iLuminQscale, int iChromQscale,
                                int iMdecCodeCount)
            throws IOException
    {
        if (iLuminQscale != iChromQscale)
            throw new IllegalArgumentException();

        int iDemuxSizeForHeader = (abDemuxData.length + 3) & ~3;

        byte[] abSectUserData = getCDSector().getCdUserDataCopy();

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);

        int iBytesToCopy = getIdentifiedUserDataSize();
        if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
            iBytesToCopy = abDemuxData.length - iDemuxOfs;

        // bytes to copy might be 0, which is ok because we
        // still need to write the updated headers
        System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, 32, iBytesToCopy);

        cd.writeSector(getSectorNumber(), abSectUserData);

        return iBytesToCopy;
    }

    public boolean splitAudio() {
        return (getFrameNumber() == 1 && getChunkNumber() == 0);
    }


}

