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

package jpsxdec.sectors;

import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.psxvideo.encode.ParsedMdecImage;
import jpsxdec.util.ByteArrayFPIS;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;


/** Shared super class of several video sector types. */
public abstract class SectorAbstractVideo extends IdentifiedSector implements IVideoSector {
    
    private static final Logger log = Logger.getLogger(SectorAbstractVideo.class.getName());
    protected Logger log() { return log; }

    // .. Instance fields ..................................................

    protected int  _iChunkNumber;
    protected int  _iChunksInThisFrame;
    protected int  _iFrameNumber;
    protected long _lngUsedDemuxedSize;
    protected int  _iWidth;
    protected int  _iHeight;
    protected int  _iRunLengthCodeCount;
    
    // .. Constructor .....................................................

    public SectorAbstractVideo(CdSector cdSector) throws NotThisTypeException {
        super(cdSector);
    }

    // .. Abstract methods .................................................

    abstract public String toString();

    abstract public String getTypeName();

    abstract public int getSectorHeaderSize();

    // .. Public methods ...................................................

    final public int getChunkNumber() {
        return _iChunkNumber;
    }

    final public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    final public int getFrameNumber() {
        return _iFrameNumber;
    }

    final public int getWidth() {
        return _iWidth;
    }

    final public int getHeight() {
        return _iHeight;
    }

    final public int getIdentifiedUserDataSize() {
            return super.getCDSector().getCdUserDataSize() -
                getSectorHeaderSize();
    }

    final public ByteArrayFPIS getIdentifiedUserDataStream() {
        return new ByteArrayFPIS(super.getCDSector().getCdUserDataStream(),
                getSectorHeaderSize(), getIdentifiedUserDataSize());
    }

    final public void copyIdentifiedUserData(byte[] abOut, int iOutPos) {
        super.getCDSector().getCdUserDataCopy(getSectorHeaderSize(), abOut,
                iOutPos, getIdentifiedUserDataSize());
    }

    final public int getSectorType() {
        return SECTOR_VIDEO;
    }

    /** Checks if this sector is part of the same stream as the previous video sector. */
    final public boolean matchesPrevious(IVideoSector prevSector) {
        if (!(prevSector.getClass().equals(prevSector.getClass())))
            return false;

        if (prevSector.getFrameNumber() == getFrameNumber() &&
            prevSector.getChunksInFrame() != getChunksInFrame())
            return false;

        if (getWidth()  != prevSector.getWidth() ||
            getHeight() != prevSector.getHeight())
               return false;

        /*  This logic is accurate, but not forgiving at all
        long iNextChunk = prevSector.getChunkNumber() + 1;
        long iNextFrame = prevSector.getFrameNumber();
        if (iNextChunk >= prevSector.getChunksInFrame()) {
            iNextChunk = 0;
            iNextFrame++;
        }

        if (iNextChunk != getChunkNumber() || iNextFrame != getFrameNumber())
            return false;
        */

        // softer logic
        if (prevSector.getFrameNumber() == getFrameNumber())
            return true;
        else if (prevSector.getFrameNumber() == getFrameNumber() - 1)
            return true;
        else
            return false;
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
        IO.writeInt16LE(abSectUserData, 20, ParsedMdecImage.calculateHalfCeiling32(iMdecCodeCount));
        IO.writeInt16LE(abSectUserData, 24, (short)iLuminQscale);

        int iBytesToCopy = getIdentifiedUserDataSize();
        if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
            iBytesToCopy = abDemuxData.length - iDemuxOfs;

        // bytes to copy might be 0, which is ok because we
        // still need to write the updated headers
        System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, 32, iBytesToCopy);

        cd.writeSector(getSectorNumber(), abSectUserData);

        return iBytesToCopy;
    }


    final public boolean splitAudio() {
        return (getFrameNumber() == 1 && getChunkNumber() == 0);
    }
}

