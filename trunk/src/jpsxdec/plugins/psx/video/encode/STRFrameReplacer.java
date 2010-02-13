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

package jpsxdec.plugins.psx.video.encode;

import java.io.File;
import jpsxdec.cdreaders.CDFileSectorReader;
import jpsxdec.plugins.ConsoleProgressListener;
import jpsxdec.plugins.DiscIndex;
import jpsxdec.plugins.DiscItem;
import jpsxdec.plugins.psx.video.DemuxImage;
import jpsxdec.plugins.psx.str.DiscItemSTRVideo;
import jpsxdec.plugins.psx.str.SectorSTR;
import jpsxdec.plugins.IdentifiedSector;
import jpsxdec.util.FeedbackStream;
import jpsxdec.util.IO;


public class STRFrameReplacer {

    private CDFileSectorReader CD;

    private static final String DEMUX_FILE_PREFIX = "merged";

    public STRFrameReplacer(String sDemuxFilesFolder, String sOutputCD) throws Throwable {
        CD = new CDFileSectorReader(sOutputCD, true);
        DiscIndex index = new DiscIndex(CD, new ConsoleProgressListener(new FeedbackStream(System.out, FeedbackStream.NORM)));

        DiscItem item = index.getByIndex(0);
        DiscItemSTRVideo oVidItem = (DiscItemSTRVideo) item;

        File[] oNewFrames = IO.getSortedFileList(sDemuxFilesFolder, ".demux");

        int iStartFrameOfs = oVidItem.getStartFrame() - 0;

        for (File f : oNewFrames) {
            System.out.println(f);
            int iFrame = parseFrameNum(f);
            DemuxImage oDemux = new DemuxImage(oVidItem.getWidth(), oVidItem.getHeight(), f);
            replaceFrame(iFrame + iStartFrameOfs, oVidItem, oDemux);
        }
    }

    private static int parseFrameNum(File f) {
        String sFrame = f.getName().substring(DEMUX_FILE_PREFIX.length(), DEMUX_FILE_PREFIX.length() + 4);
        return Integer.parseInt(sFrame, 10);
    }

    private final int VIDEO_SECTOR_HEADER_DEMUX_SIZE_POS = 12;
    private final int VIDEO_SECTOR_HEADER_DEMUX_DATA_START = 32;
    private void replaceFrame(int iFrame, DiscItemSTRVideo oVidItem, DemuxImage oDemux) throws Throwable {
        int iSector = oVidItem.seek(iFrame).getSectorNumber();

        byte[] abDemuxData = oDemux.getData();
        int iHeaderDemuxSize = (abDemuxData.length + 3) & ~3;

        IdentifiedSector oSect = oVidItem.getIdentifiedSector(iSector);
        while (!(oSect instanceof SectorSTR)) {
            iSector++;
            oSect = oVidItem.getIdentifiedSector(iSector);
        }
        
        SectorSTR oVidSect = (SectorSTR) oSect;

        int iDemuxOfs = 0;
        while (oVidSect.getFrameNumber() == iFrame && iDemuxOfs < abDemuxData.length)
        {
            System.out.println(oSect);

            byte[] abSectUserData = oSect.getCDSector().getCdUserDataCopy();

            IO.writeInt32LE(abSectUserData, VIDEO_SECTOR_HEADER_DEMUX_SIZE_POS, iHeaderDemuxSize);

            int iBytesToCopy = oVidSect.getPSXUserDataSize();
            if (iDemuxOfs + iBytesToCopy > abDemuxData.length)
                iBytesToCopy = abDemuxData.length - iDemuxOfs;

            System.arraycopy(abDemuxData, iDemuxOfs, abSectUserData, VIDEO_SECTOR_HEADER_DEMUX_DATA_START, iBytesToCopy);
            iDemuxOfs += iBytesToCopy;

            CD.writeSector(iSector, abSectUserData);

            // next sector
            oSect = null;
            while (!(oSect instanceof SectorSTR) && iSector+1 < CD.size()) {
                iSector++;
                oSect = oVidItem.getIdentifiedSector(iSector);
            }
            oVidSect = (SectorSTR) oSect;
        }

        if (iDemuxOfs != abDemuxData.length)
            throw new RuntimeException(String.format(
                    "Demux data does fit in frame %d!! Demux data size %d, amount wrote %d",
                    iFrame, abDemuxData.length, iDemuxOfs));
    }
}
