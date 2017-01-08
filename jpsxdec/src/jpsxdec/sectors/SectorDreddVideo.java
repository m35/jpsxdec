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
import jpsxdec.cdreaders.CdxaSubHeader.SubMode;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.util.BinaryDataNotRecognized;

/** Judge Dredd video sector. 
 * <p>
 * Judge Dredd does not make video sector identification easy. Requires
 * contextual information about the surrounding sectors to
 * uniquely determine if a sector really is a Dredd video sector. */
public class SectorDreddVideo extends SectorAbstractVideo {

    static final int MIN_CHUNKS_PER_FRAME = 9;
    static final int MAX_CHUNKS_PER_FRAME = 10;

    /** All Dredd frames are 320 pixels wide. */
    private static final int FRAME_WIDTH = 320;

    /** Uncompresses the bitstream a line of macroblocks at a time until failure
     * to determine the frame height.
     * @return height of the frame in pixels. */
    static int getHeight(byte[] abBitstream) throws BinaryDataNotRecognized {
        BitStreamUncompressor bs = BitStreamUncompressor.identifyUncompressor(abBitstream);
        int iFrameHeightMB = 0;
        try {
            // keep reading lines until the stream ends
            while (true) {
                bs.skipMacroBlocks(FRAME_WIDTH, 16);
                iFrameHeightMB++;
            }
        } catch (MdecException.EndOfStream ex) {
            // expected
        } catch (MdecException.ReadCorruption ex) {
            // expected
        }
        if (iFrameHeightMB == 0)
            throw new BinaryDataNotRecognized();
        return iFrameHeightMB * 16;
    }

    /** Frame chunk number from first 4 bytes. */
    private int _iChunk;
    /** Dredd sector header size is either 4 or 44. */
    private int _iHeaderSize;

    /** Dredd frame height is either 352 or 240. Set by contextual identification. */
    private int _iHeight;
    /** Dredd chunk count is either 9 or 10. Set by contextual identification. */
    private int _iChunkCount;

    /** Performs initial, partial sector identification.
     * Additional verification is necessary which requires contextual information. */
    SectorDreddVideo(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // subheader is necessary to know when videos end
        if (!cdSector.hasSubHeader()) return;
        if (cdSector.getSubHeaderFile() != 1 || cdSector.getSubHeaderChannel() != 2)
            return;
        if (cdSector.subModeMask(~SubMode.MASK_EOF_MARKER) != SubMode.MASK_DATA)
            return;

        _iChunk = cdSector.readSInt32LE(0);
        if (_iChunk < 0 || _iChunk >= MAX_CHUNKS_PER_FRAME)
            return;

        if (_iChunk != 0 || isBitstream(cdSector, 4))
            _iHeaderSize = 4;
        else if (isBitstream(cdSector, 44))
            _iHeaderSize = 44;
        else
            return;

        setProbability(100);
    }

    /** Vital information set by contextual sector identification. */
    void setHeightChunks(int iHeight, int iChunkCount) {
        _iHeight = iHeight;
        _iChunkCount = iChunkCount;
    }

    /** Returns if this is a v2 or v3 frame. Be sure to keep in sync with
     * {@link jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2#checkHeader(byte[])}
     * and
     * {@link jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3#checkHeader(byte[])}.
     */
    private static boolean isBitstream(@Nonnull CdSector cdSector, int iOfs) {
        if (cdSector.getCdUserDataSize() + iOfs < 8)
            return false;

        int iHalfVlcCountCeil32 = cdSector.readSInt16LE(iOfs);
        int iMagic3800          = cdSector.readSInt16LE(iOfs+2);
        int iQscale             = cdSector.readSInt16LE(iOfs+4);
        int iVersion            = cdSector.readSInt16LE(iOfs+6);

        return iMagic3800 == 0x3800 && iQscale >= 1 &&
               (iVersion == 2 || iVersion == 3)  && iHalfVlcCountCeil32 >= 0;
    }

    public @Nonnull String getTypeName() {
        return "Dredd";
    }

    @Override
    protected int getSectorHeaderSize() {
        return _iHeaderSize;
    }

    public int getChunkNumber() {
        return _iChunk;
    }

    public int getChunksInFrame() {
        return _iChunkCount;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getWidth() {
        return FRAME_WIDTH;
    }

    @Override
    public String toString() {
        String s = String.format("%s %s 320x%d chunk:%d/%d",
                getTypeName(), cdToString(), 
                _iHeight, _iChunk, _iChunkCount);
        if (_iHeaderSize == 4)
            return s;
        else
            return s + " + Unknown data";
    }

}
