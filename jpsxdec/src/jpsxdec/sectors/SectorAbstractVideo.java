/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2014  Michael Sabin
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
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;


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

    public int checkAndPrepBitstreamForReplace(byte[] abDemuxData, int iUsedSize,
                                               int iMdecCodeCount, byte[] abSectUserData)
    {
        int iQscale;
        if ((iQscale = BitStreamUncompressor_STRv2.getQscale(abDemuxData)) < 1 &&
            (iQscale = BitStreamUncompressor_STRv3.getQscale(abDemuxData)) < 1)
        {
            throw new IllegalArgumentException("Frame type is not v2 or v3");
        }

        int iDemuxSizeForHeader = (iUsedSize + 3) & ~3;

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abSectUserData, 20,
                BitStreamUncompressor_STRv2.calculateHalfCeiling32(iMdecCodeCount));
        IO.writeInt16LE(abSectUserData, 24, (short)(iQscale));

        return getSectorHeaderSize();
    }

}

