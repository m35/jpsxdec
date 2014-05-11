/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2012-2014  Michael Sabin
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

package jpsxdec.discitems;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.sectors.SectorCrusader;
import jpsxdec.util.FeedbackStream;

public class DemuxedCrusaderFrame implements IDemuxedFrame {

    private static final Logger LOG = Logger.getLogger(DemuxedCrusaderFrame.class.getName());
    
    private final int _iWidth, _iHeight;
    private final SectorCrusader[] _aoSectors;
    private final int _iStartSector, _iEndSector;
    private final int _iSize;
    private final int _iStartOffset;
    private final int _iFrame;
    /** The sector the frame should be presented.
     * This will be many sectors after the frame was read. */
    private final int _iPresentationSector;

    public DemuxedCrusaderFrame(int iWidth, int iHeight, 
                                SectorCrusader[] aoSectors, 
                                int iByteSize, int iFirstSectorStartOffset, 
                                int iFrameNumber,
                                int iPresentationSector)
    {
        _iWidth = iWidth;
        _iHeight = iHeight;
        _aoSectors = aoSectors;
        _iSize = iByteSize;
        _iStartOffset = iFirstSectorStartOffset;
        _iFrame = iFrameNumber;
        _iPresentationSector = iPresentationSector;

        int iMin = Integer.MAX_VALUE, iMax = Integer.MIN_VALUE;
        for (SectorCrusader cruSect : aoSectors) {
            if (cruSect == null)
                continue;
            if (cruSect.getSectorNumber() < iMin)
                iMin = cruSect.getSectorNumber();
            if (cruSect.getSectorNumber() > iMax)
                iMax = cruSect.getSectorNumber();
            if (iMin == Integer.MAX_VALUE || iMax == Integer.MIN_VALUE)
                throw new IllegalArgumentException("No Crusader sectors");
        }
        _iStartSector = iMin;
        _iEndSector = iMax;
    }
    
    public byte[] copyDemuxData(byte[] abBuffer) {
        if (abBuffer == null || abBuffer.length < getDemuxSize())
            abBuffer = new byte[getDemuxSize()];

        int iLen = _aoSectors[0].getIdentifiedUserDataSize() - _iStartOffset;
        if (iLen > _iSize)
            iLen = _iSize;
        _aoSectors[0].copyIdentifiedUserData(_iStartOffset, abBuffer, 0, iLen);
        int iPos = iLen;
        for (int iChunk = 1; iChunk < _aoSectors.length; iChunk++) {
            SectorCrusader chunk = _aoSectors[iChunk];
            if (chunk != null) {
                iLen = chunk.getIdentifiedUserDataSize();
                if (iPos + iLen > _iSize)
                    iLen = _iSize - iPos;
                chunk.copyIdentifiedUserData(0, abBuffer, iPos, iLen);
                iPos += iLen;
            } else {
                LOG.log(Level.WARNING, "Frame {0,number,#} chunk {1,number,#} missing.", new Object[]{_iFrame, iChunk}); // I18N
            }
        }
        return abBuffer;
    }

    public int getChunksInFrame() {
        return _aoSectors.length;
    }

    public int getDemuxSize() {
        return _iSize;
    }

    public int getStartSector() {
        return _iStartSector;
    }

    public int getEndSector() {
        return _iEndSector;
    }

    public int getFrame() {
        return _iFrame;
    }

    public int getWidth() {
        return _iWidth;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getPresentationSector() {
        return _iPresentationSector;
    }

    public void printSectors(FeedbackStream fbs) {
        int iDemuxOfs = getDemuxSize();
        for (int i=0; i < getChunksInFrame(); i++) {
            fbs.print(_aoSectors[i]);
            if (i == 0) {
                fbs.println(" (start offset " + _iStartOffset + ")");
                iDemuxOfs -= SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE - _iStartOffset;
            } else if (i == getChunksInFrame() - 1)
                fbs.println(" (end offset " + iDemuxOfs + ")");
            else {
                iDemuxOfs -= SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE;
                fbs.println();
            }
        }
    }

    public void writeToSectors(byte[] abNewDemux,
                               int iUsedSize_ignore, int iMdecCodeCount_ignore,
                               CdFileSectorReader cd, FeedbackStream fbs)
             throws IOException 
    {
        if (abNewDemux.length > _iSize)
            throw new IllegalArgumentException("Unable to fit new demux size " + abNewDemux.length + " into " + _iSize + " bytes");

        int iDemuxOfs = 0;
        for (int iChunk = 0; iChunk < _aoSectors.length; iChunk++) {
            SectorCrusader vidSector = _aoSectors[iChunk];
            if (vidSector == null) {
                fbs.printlnWarn("Trying to replace a frame with missing chunks??");
                continue;
            }
            byte[] abSectUserData = vidSector.getCdSector().getCdUserDataCopy();
            int iBytesToCopy = SectorCrusader.CRUSADER_IDENTIFIED_USER_DATA_SIZE;
            int iCopyTo = SectorCrusader.HEADER_SIZE;
            if (iChunk == 0) {
                iCopyTo += _iStartOffset;
                iBytesToCopy -= _iStartOffset;
            }
            if (iDemuxOfs + iBytesToCopy > abNewDemux.length)
                iBytesToCopy = abNewDemux.length - iDemuxOfs;

            if (iBytesToCopy == 0)
                break;
            
            System.arraycopy(abNewDemux, iDemuxOfs, abSectUserData, iCopyTo, iBytesToCopy);

            cd.writeSector(vidSector.getSectorNumber(), abSectUserData);

            iDemuxOfs += iBytesToCopy;
        }

    }
    
}
