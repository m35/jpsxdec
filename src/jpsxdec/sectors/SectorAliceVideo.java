/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2012  Michael Sabin
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

import jpsxdec.cdreaders.CdSector;
import jpsxdec.util.IO;

// sector 233609 is flagged as an audio sector in the header
// but its content is the same as the previous sector, which is a video sector
// so if it has raw headers, it will be detected as audio, but
// if it doesn't, it will be detected as video

/** Alice In Cyber Land frame chunk sector. */
public class SectorAliceVideo extends SectorAliceNullVideo
        implements IVideoSector 
{


    public SectorAliceVideo(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // ignore null frames between movies
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
    public String getTypeName() {
        return "Alice";
    }

    public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(getClass().equals(prevSector.getClass())))
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

    public int checkAndPrepBitstreamForReplace(byte[] abDemuxData, int iUsedSize,
                                int iMdecCodeCount, byte[] abSectUserData)
    {
        int iDemuxSizeForHeader = (iUsedSize + 3) & ~3;

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);

        return ALICE_VIDEO_SECTOR_HEADER_SIZE;
    }

    public int splitXaAudio() {
        return (getFrameNumber() == 1 && getChunkNumber() == 0) ?
            SPLIT_XA_AUDIO_CURRENT : SPLIT_XA_AUDIO_NONE;
        // it would be nice to split the audio at previous sector
        // but there are EOF audio indicators that are messing things up
    }


}

