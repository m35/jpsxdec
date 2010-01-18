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

package jpsxdec.plugins.psx.str.fps;

import jpsxdec.util.Fraction;

/**
 * Generates a sequence of frame chunk numbers and audio chunks
 * corresponding to a specified sectors/frame and sectors/audio.
 */
class ConstantFrameSequence implements FrameSequence, Comparable<ConstantFrameSequence> {

    public static ConstantFrameSequence createNoAud(
            int iFirstFrame, int iFirstChunk, int iFirstChunkCount,
            int iSectors, int iPerFrame)
    {
        int iFrameStartSector = (int)Math.floor(iSectors / (double)iPerFrame * (iFirstFrame-1));
        int iNextFrameStartSector = (int)Math.floor(iSectors / (double)iPerFrame * (iFirstFrame));

        int iChunksInFrame = iNextFrameStartSector - iFrameStartSector;

        if (iFirstChunkCount == iChunksInFrame)
            return new ConstantFrameSequence(iFrameStartSector + iFirstChunk, iSectors, iPerFrame);
        else
            return null;
    }

    public static ConstantFrameSequence create(
            int iFirstFrame, int iFirstChunk, int iFirstChunkCount,
            int iSectors, int iPerFrame, int iAudStart, int iAudStride)
    {
        int iFrameSector = (int)Math.floor(iSectors / (double)iPerFrame * (iFirstFrame-1));
        int iNextFrameStartSector = (int)Math.floor(iSectors / (double)iPerFrame * (iFirstFrame));

        int iStartSector = -1;
        int iFrameChunkCount = 0;
        while (iFrameSector < iNextFrameStartSector) {
            if ((iFrameSector - iAudStart) % iAudStride != 0) {
                if (iFrameChunkCount == iFirstChunk)
                    iStartSector = iFrameSector;
                iFrameChunkCount++;
            }
            iFrameSector++;
        }

        if (iStartSector < 0)
            return null;

        if (iFrameChunkCount != iFirstChunkCount)
            return null;

        return new ConstantFrameSequence(iStartSector, iSectors, iPerFrame, iAudStride, iAudStart);
    }



    private final int _iSectors;
    private final int _iPerFrame;
    private final int _iAudioStride;
    private final int _iAudioStartOfs;
    private final int _iStartSector;

    private ConstantFrameSequence(int iStartSector, int iSectors, int iPerFrame) {
        super();
        _iStartSector = iStartSector;
        _iAudioStride = -1;
        _iAudioStartOfs = 0;
        _iSectors = iSectors;
        _iPerFrame = iPerFrame;
    }

    private ConstantFrameSequence(int iStartSector, int iSectors, int iPerFrame, int iAudioStride, int iAudioStartOfs) {
        super();
        _iStartSector = iStartSector;
        _iSectors = iSectors;
        _iPerFrame = iPerFrame;
        _iAudioStride = iAudioStride;
        _iAudioStartOfs = iAudioStartOfs;
    }

    private boolean isAudio(int iSector) {
        if (_iAudioStride < 1) {
            return false;
        }
        return (iSector - _iAudioStartOfs) % _iAudioStride == 0;
    }

    public boolean matchesNextVideo(final int iSector, final int iVidFrame, final int iVidChunk, final int iVidFrameChunkCount) {
        // increment the current sector no matter what
        // it's comparing a video sector when it should be an audio sector
        boolean blnMatches = !isAudio(iSector + _iStartSector);

        blnMatches = blnMatches && (iVidChunk == currentChunkNumberInFrame(iSector + _iStartSector, iVidFrame));
        
        blnMatches = blnMatches && (countChunksInFrame(iVidFrame) == iVidFrameChunkCount);

        return blnMatches;
    }

    private int currentChunkNumberInFrame(int iSector, int iFrame) {
        int iFrameSector = (int) Math.floor(_iSectors / (double) _iPerFrame * (iFrame - 1));
        final int iNextFrameStartSector = (int) Math.floor(_iSectors / (double) _iPerFrame * (iFrame));

        if (iSector < iFrameSector || iSector >= iNextFrameStartSector) {
            return -1;
        }

        int iCurChunk = 0;
        while (iFrameSector < iNextFrameStartSector) {
            if (iFrameSector == iSector)
                return iCurChunk;
            
            if (!isAudio(iFrameSector)) {
                iCurChunk++;
            }
            iFrameSector++;
        }
        return -1; // shouldn't ever happen
    }

    private int countChunksInFrame(int iFrame) {
        int iFrameSector = (int) Math.floor(_iSectors / (double) _iPerFrame * (iFrame - 1));
        final int iNextFrameStartSector = (int) Math.floor(_iSectors / (double) _iPerFrame * (iFrame));

        if (_iAudioStride < 1)
            return iNextFrameStartSector - iFrameSector;

        // this could be done with a bit more math, but I'm mentally tired
        int iFrameChunkCount = 0;
        while (iFrameSector < iNextFrameStartSector) {
            if (!isAudio(iFrameSector)) {
                iFrameChunkCount++;
            }
            iFrameSector++;
        }
        return iFrameChunkCount;
    }

    public Fraction getSectorsPerFrame() {
        return new Fraction(_iSectors, _iPerFrame);
    }

    public int compareTo(ConstantFrameSequence o) {
        int i = new Fraction(_iSectors, _iPerFrame).compareTo(new Fraction(o._iSectors, o._iPerFrame));
        if (i == 0) {
            if (_iAudioStartOfs < o._iAudioStartOfs) {
                return -1;
            } else if (_iAudioStartOfs > o._iAudioStartOfs) {
                return 1;
            } else if (_iAudioStride < o._iAudioStride) {
                return -1;
            } else if (_iAudioStride > o._iAudioStride) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return i;
        }
    }
}
