/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2017-2023  Michael Sabin
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

package jpsxdec.modules.dredd;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3;
import jpsxdec.psxvideo.bitstreams.StrHeader;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.DemuxedData;
import jpsxdec.util.ExposedBAOS;


/** Collects Judge Dredd sectors and generates one frame.
 * Create a new instance for each frame. */
public class DreddDemuxer {

    private static final Logger LOG = Logger.getLogger(DreddDemuxer.class.getName());

    public static final int MIN_CHUNKS_PER_FRAME = 9;
    public static final int MAX_CHUNKS_PER_FRAME = 10;

    public static @CheckForNull DreddDemuxer first(@Nonnull CdSector cdSector) {
        // subheader is necessary to know when videos end
        if (!commonSectorCheck(cdSector))
            return null;

        int iChunk = cdSector.readSInt32LE(0);
        if (iChunk != 0)
            return null;

        // only first chunk can we check for bitstream header
        StrHeader strHeader = hasBitstreamHeader(cdSector, 4);
        int iFirstHeaderSize;
        if (strHeader != null) {
            iFirstHeaderSize = 4;
        } else {
            strHeader = hasBitstreamHeader(cdSector, 44);
            if (strHeader != null)
                iFirstHeaderSize = 44;
            else
                return null;
        }

        return new DreddDemuxer(strHeader, new SectorDreddVideo(cdSector, iChunk, iFirstHeaderSize));
    }

    @Nonnull
    private final StrHeader _strHeader;
    @Nonnull
    private final ArrayList<SectorDreddVideo> _sectors = new ArrayList<SectorDreddVideo>(MAX_CHUNKS_PER_FRAME);

    public DreddDemuxer(@Nonnull StrHeader strHeader,
                        @Nonnull SectorDreddVideo firstDreddFrameSector)
    {
        _strHeader = strHeader;
        _sectors.add(firstDreddFrameSector);
    }

    /** Returns if the given CD sector is identical to the first sector in this demux Dredd frame. */
    public boolean matchesFirstSector(@Nonnull CdSector sector) {
        return _sectors.get(0).getCdSector() == sector;
    }

    public enum AddResult {
        FRAME_COMPLETE,
        FRAME_DEAD
    }

    /** Returns true if the caller should try to complete the frame. */
    public boolean addSector(@Nonnull CdSector cdSector) {
        if (!commonSectorCheck(cdSector)) // ignore sectors that are definitely not Dredd
            return false;

        int iChunk = cdSector.readSInt32LE(0);
        if (iChunk != _sectors.size())
            return true;

        // the chunk sequence continues
        // all remaining sectors have a 4 byte header
        _sectors.add(new SectorDreddVideo(cdSector, iChunk, 4));
        if (_sectors.size() == MAX_CHUNKS_PER_FRAME)
            return true;
        return false;
    }

    public @CheckForNull List<SectorDreddVideo> tryToFinishFrame() {
        if (_sectors.size() < MIN_CHUNKS_PER_FRAME-1) {
            // not enough sectors
            return null; // sequence fail
        }


        ExposedBAOS baos = new ExposedBAOS();
        for (SectorDreddVideo sectorDreddVideo : _sectors) {
            int iSize = sectorDreddVideo.getDemuxPieceSize();
            byte[] abBuff = new byte[iSize];
            sectorDreddVideo.copyDemuxPieceData(abBuff, 0);
            baos.write(abBuff);
        }
        byte[] abDemuxBuffer = baos.toByteArray();

        if (!checkHeight(abDemuxBuffer, _strHeader)) {
            LOG.log(Level.WARNING, "Possible Dredd frame failed bitstream check starting with sector {0}",
                                   _sectors.get(0));
            return null;
        }

        //    A. 320x352 dimensions, held in 9 chunks
        //    B. 320x240 dimensions, held in 10 chunks
        // of course this could have some problems if the last of 10 chunks
        // is corrupted and unrecognized, but it's more reliable than
        // other methods to determine the height
        int iHeight;
        if (_sectors.size() == MIN_CHUNKS_PER_FRAME)
            iHeight = FRAME_HEIGHT_A;
        else {
            if (_sectors.size() != MAX_CHUNKS_PER_FRAME)
                throw new RuntimeException("Dredd frame with " + _sectors.size() + " sectors");
            iHeight = FRAME_HEIGHT_B;
        }

        DemuxedDreddFrame frame = new DemuxedDreddFrame(new DemuxedData<SectorDreddVideo>(_sectors), iHeight);
        _sectors.get(_sectors.size() - 1)._dreddFrame = frame;

        // sector sequence success
        return _sectors;
    }


    // --- static checking functions ------------------------------------------

    private static boolean commonSectorCheck(@Nonnull CdSector cdSector) {
        // subheader is necessary to know when videos end
        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh == null) return false;
        if (sh.getFileNumber() != 1 || sh.getChannel() != 2)
            return false;
        if (sh.getSubMode().mask(~CdSectorXaSubHeader.SubMode.MASK_END_OF_FILE) != CdSectorXaSubHeader.SubMode.MASK_DATA)
            return false;
        return true;
    }

    /** Returns if this is a v2 or v3 frame. */
    private static @CheckForNull StrHeader hasBitstreamHeader(@Nonnull CdSector cdSector, int iOfs) {
        if (cdSector.getCdUserDataSize() + iOfs < 8)
            return null;

        byte[] abHeader = new byte[8];
        cdSector.getCdUserDataCopy(iOfs, abHeader, 0, abHeader.length);

        StrHeader v2 = new BitStreamUncompressor_STRv2.StrV2Header(abHeader, abHeader.length);
        if (v2.isValid())
            return v2;
        StrHeader v3 = new BitStreamUncompressor_STRv3.StrV3Header(abHeader, abHeader.length);
        if (v3.isValid())
            return v3;

        return null;
    }

    /** All Dredd frames are 320 pixels wide. */
    public static final int FRAME_WIDTH = 320;
    public static final int FRAME_HEIGHT_A = 352;
    public static final int FRAME_HEIGHT_B = 240;

    /** Uncompresses the bitstream by a minium amount to ensure it is valid. */
    private static boolean checkHeight(@Nonnull byte[] abFullFrame,
                                       @Nonnull StrHeader strHeader)
    {
        try {
            BitStreamUncompressor bsu = strHeader.makeNew(abFullFrame);
            bsu.skipMacroBlocks(FRAME_WIDTH, FRAME_HEIGHT_B);
            return true;
        } catch (MdecException.EndOfStream | MdecException.ReadCorruption ex) {
            return false;
        } catch (BinaryDataNotRecognized ex) {
            return false;
        }
    }
}
