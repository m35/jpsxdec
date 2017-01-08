/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2017  Michael Sabin
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

import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.LocalizedIncompatibleException;


/** Shared super class of several video sector types. */
public abstract class SectorAbstractVideo extends IdentifiedSector 
                                          implements IVideoSector
{

    /** First 16 bytes found in most video sector headers. 
     * Has readers that will verify the value according to common
     * convention. When the sector type does not use the common convention
     * also has readers that will just read the value without verification. */
    protected static class CommonVideoSectorFirst16bytes {
        public long lngMagic;             //  0    [4 bytes]
        public int  iChunkNumber;         //  4    [2 bytes]
        public int  iChunksInThisFrame;   //  6    [2 bytes]
        public int  iFrameNumber;         //  8    [4 bytes]
        public int  iUsedDemuxedSize;     //  12   [4 bytes]

        public void readMagic(@Nonnull CdSector cdSector) {
            lngMagic = cdSector.readUInt32LE(0);
        }
        /** Reads the number of this chunk.
         * @return If the chunk number is invalid according to common convention. */
        public boolean readChunkNumberStandard(@Nonnull CdSector cdSector) {
            iChunkNumber = cdSector.readSInt16LE(4);
            return iChunkNumber < 0;
        }
        /** Reads the number of chunks in this frame. */
        public void readChunksInFrame(@Nonnull CdSector cdSector) {
            iChunksInThisFrame = cdSector.readSInt16LE(6);
        }
        /** Reads the number of chunks in this frame.
         * @return If the chunk count is invalid according to common convention. */
        public boolean readChunksInFrameStandard(@Nonnull CdSector cdSector) {
            iChunksInThisFrame = cdSector.readSInt16LE(6);
            return iChunksInThisFrame < 1;
        }
        /** Reads this sector's frame number. */
        public void readFrameNumber(@Nonnull CdSector cdSector) {
            iFrameNumber = cdSector.readSInt32LE(8);
        }
        /** Reads this sector's frame number.
         * @return If the frame number is invalid according to common convention. */
        public boolean readFrameNumberStandard(@Nonnull CdSector cdSector) {
            iFrameNumber = cdSector.readSInt32LE(8);
            return iFrameNumber < 0;
        }
        /** Reads the frame's used demux size. */
        public void readUsedDemuxSize(@Nonnull CdSector cdSector) {
            iUsedDemuxedSize = cdSector.readSInt32LE(12);
        }
        /** Reads the frame's used demux size.
         * @return If the used demux size is invalid according to common convention. */
        public boolean readUsedDemuxSizeStandard(@Nonnull CdSector cdSector) {
            iUsedDemuxedSize = cdSector.readSInt32LE(12);
            return iUsedDemuxedSize < 1;
        }
    }

    // .. Constructor .....................................................

    public SectorAbstractVideo(@Nonnull CdSector cdSector) {
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

    final public @Nonnull ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCdSector().getCdUserDataStream(),
                getSectorHeaderSize(), getIdentifiedUserDataSize());
    }

    final public void copyIdentifiedUserData(@Nonnull byte[] abOut, int iOutPos) {
        super.getCdSector().getCdUserDataCopy(getSectorHeaderSize(), abOut,
                iOutPos, getIdentifiedUserDataSize());
    }

    public int checkAndPrepBitstreamForReplace(@Nonnull byte[] abDemuxData, int iUsedSize,
                                               int iMdecCodeCount, @Nonnull byte[] abSectUserData)
            throws LocalizedIncompatibleException
    {
        int iQscale;
        try {
            iQscale = BitStreamUncompressor_STRv2.getQscale(abDemuxData);
        } catch (BinaryDataNotRecognized ex) {
            try {
                iQscale = BitStreamUncompressor_STRv3.getQscale(abDemuxData);
            } catch (BinaryDataNotRecognized ex1) {
                throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_V2_V3(), ex1);
            }
        }

        int iDemuxSizeForHeader = (iUsedSize + 3) & ~3;

        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);
        IO.writeInt16LE(abSectUserData, 20,
                BitStreamUncompressor_STRv2.calculateHalfCeiling32(iMdecCodeCount));
        IO.writeInt16LE(abSectUserData, 24, (short)(iQscale));

        return getSectorHeaderSize();
    }

}

