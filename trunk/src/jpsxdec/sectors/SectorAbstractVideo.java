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
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Shared super class of several video sector types. */
public abstract class SectorAbstractVideo extends IdentifiedSector 
                                          implements IVideoSector
{

    // .. Constructor .....................................................

    public SectorAbstractVideo(CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        if (cdSector.isCdAudioSector()) return;

        setProbability(1);
    }

    // .. Abstract methods .................................................

    // force subclasses to implement this for themselves
    abstract public String toString();

    /** Returns the size of any headers in the identified data that should not
     * be copied when demuxing the video data. */
    abstract protected int getSectorHeaderSize();

    // .. Public methods ...................................................


    final public int getIdentifiedUserDataSize() {
            return super.getCdSector().getCdUserDataSize() -
                getSectorHeaderSize();
    }

    final public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                getSectorHeaderSize(), getIdentifiedUserDataSize());
    }

    final public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCdSector().getCdUserDataCopy(getSectorHeaderSize(), abOut,
                iOutPos, getIdentifiedUserDataSize());
    }

    final public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(getClass().equals(prevSector.getClass())))
            return false;

        if (getWidth()  != prevSector.getWidth() ||
            getHeight() != prevSector.getHeight())
               return false;

        /*  This logic is accurate, but not forgiving at all

        if (prevSector.getFrameNumber() == getFrameNumber() &&
            prevSector.getChunksInFrame() != getChunksInFrame())
            return false;

        long iNextChunk = prevSector.getChunkNumber() + 1;
        long iNextFrame = prevSector.getFrameNumber();
        if (iNextChunk >= prevSector.getChunksInFrame()) {
            iNextChunk = 0;
            iNextFrame++;
        }

        if (iNextChunk != getChunkNumber() || iNextFrame != getFrameNumber())
            return false;
        */

        // much softer logic
        if (prevSector.getFrameNumber() <= getFrameNumber() &&
            getSectorNumber() < prevSector.getSectorNumber() + 100)
        {
            return true;
        } else {
            return false;
        }
    }

    public int checkAndPrepBitstreamForReplace(byte[] abDemuxData, int iUsedSize,
                                int iMdecCodeCount, byte[] abSectUserData)
    {
        // create a bitstream uncompressor just to get the qscale
        BitStreamUncompressor bsu = BitStreamUncompressor.identifyUncompressor(abDemuxData);
        if (!(bsu instanceof BitStreamUncompressor_STRv2))
            throw new IllegalArgumentException("Incompatable frame type " + bsu);
        BitStreamUncompressor_STRv2 bsu2 = (BitStreamUncompressor_STRv2) bsu;
        try {
            bsu2.reset(abDemuxData);
        } catch (NotThisTypeException ex) {
            throw new RuntimeException("Shouldn't happen");
        }

        int iQscale = bsu2.getQscale();

        int iDemuxSizeForHeader = (iUsedSize + 3) & ~3;

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abSectUserData, 20,
                BitStreamUncompressor_STRv2.calculateHalfCeiling32(iMdecCodeCount));
        IO.writeInt16LE(abSectUserData, 24, (short)(iQscale));

        return getSectorHeaderSize();
    }

}

