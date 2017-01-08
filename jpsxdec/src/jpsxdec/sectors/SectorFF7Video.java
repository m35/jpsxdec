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
import jpsxdec.i18n.I;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv1;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.util.IO;
import jpsxdec.util.LocalizedIncompatibleException;


/** Represents an FF7 video sector. */
public class SectorFF7Video extends SectorAbstractVideo implements IVideoSectorWithFrameNumber {
    
    public static final int FRAME_SECTOR_HEADER_SIZE = 32;

    // Magic is normal STR = 0x80010160
    private final CommonVideoSectorFirst16bytes _header = new CommonVideoSectorFirst16bytes();
    private int  _iWidth;               //  16   [2 bytes]
    private int  _iHeight;              //  18   [2 bytes]
    private long _lngUnknown8bytes;     //  20   [8 bytes]
    // FourZeros                        //  28   [4 bytes]
    //   32 TOTAL
    // First chunk may have 40 bytes of camera data

    public int getSectorHeaderSize() { return _iUserDataStart; }

    private int _iUserDataStart;
    
    // .. Constructor .....................................................

    // SubMode flags "--FT-A--" should never be set
    private static final int SUB_MODE_MASK = 
            (SubMode.MASK_FORM | SubMode.MASK_TRIGGER | SubMode.MASK_AUDIO);

    public SectorFF7Video(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // at least 1 movie doesn't have audio, video, or data flags set
        if (cdSector.hasSubHeader() && cdSector.subModeMask(SUB_MODE_MASK) != 0) {
            return;
        }

        _header.readMagic(cdSector);
        if (_header.lngMagic != SectorStrVideo.VIDEO_SECTOR_MAGIC) return;
        if (_header.readChunkNumberStandard(cdSector)) return;
        _header.readChunksInFrame(cdSector);
        if (_header.iChunksInThisFrame < 6 || _header.iChunksInThisFrame > 10) return;
        if (_header.readFrameNumberStandard(cdSector)) return;
        _header.readUsedDemuxSize(cdSector);
        // this block is unfortunately necessary to prevent false-positives with Lain sectors
        if (_header.iUsedDemuxedSize < 2500 || _header.iUsedDemuxedSize > 21000) return;
        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth != 320 && _iWidth != 640) return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight != 224 && _iHeight != 192 && _iHeight != 240) return;
        _lngUnknown8bytes = cdSector.readSInt64BE(20);
        
        if (_iHeight == 240) { // FF7 sampler has videos with 240 height
            // this block is unfortunately necessary to prevent false-positives with Lain sectors
            
            // if movie height is 240, then the unknown data must all be 0
            if (_lngUnknown8bytes != 0) return;

            // and no 10 chunk frames before frame 100
            if (_header.iChunksInThisFrame == 10 && _header.iFrameNumber < 100) return;
        }
        
        long iFourZeros = cdSector.readSInt32LE(28);
        if (iFourZeros != 0) return;

        // check for camera data which should always exist in chunk 0
        if (_header.iChunkNumber == 0) {
            if (cdSector.readUInt16LE(32+2) != 0x3800) {
                if (cdSector.readUInt16LE(32+40+2) != 0x3800)
                    return; // failure
                _iUserDataStart = FRAME_SECTOR_HEADER_SIZE + 40;
            } else {
                _iUserDataStart = FRAME_SECTOR_HEADER_SIZE;
            }
        } else {
            _iUserDataStart = FRAME_SECTOR_HEADER_SIZE;
        }

        // that 8 bytes of unknown data makes the confidence suspect
        setProbability(90);
    }

    // .. Public functions .................................................

    public String toString() {

        String sRet = String.format("%s %s frame:%d chunk:%d/%d %dx%d {used demux=%d unknown=%016x}",
            getTypeName(),
            super.cdToString(),
            _header.iFrameNumber,
            _header.iChunkNumber,
            _header.iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _header.iUsedDemuxedSize,
            _lngUnknown8bytes
            );

        if (_iUserDataStart == FRAME_SECTOR_HEADER_SIZE) {
            return sRet;
        } else {
            return sRet + " + Camera data";
        }
    }

    public @Nonnull String getTypeName() {
        return "FF7 Video";
    }

    @Override
    public int checkAndPrepBitstreamForReplace(@Nonnull byte[] abDemuxData, int iUsedSize, 
                                               int iMdecCodeCount, @Nonnull byte[] abSectUserData)
            throws LocalizedIncompatibleException
    {
        // In general FF7 only uses STRv1 bitstreams, however
        // the Spanish version (SCES-00900) is confirmed to have
        // a mix of STRv1 and STRv2 in ENDING2S.MOV
        if (!BitStreamUncompressor_STRv1.checkHeader(abDemuxData) && 
            !BitStreamUncompressor_STRv2.checkHeader(abDemuxData))
            throw new LocalizedIncompatibleException(I.REPLACE_FRAME_TYPE_NOT_V1_V2());

        // all frames need the additional camera data in the demux size
        int iDemuxSizeForHeader = ((iUsedSize + 3) & ~3) + 40;
        IO.writeInt32LE(abSectUserData, 12, iDemuxSizeForHeader);

        return getSectorHeaderSize();
    }
    
    public int getChunkNumber() {
        return _header.iChunkNumber;
    }

    public int getChunksInFrame() {
        return _header.iChunksInThisFrame;
    }

    public int getFrameNumber() {
        return _header.iFrameNumber;
    }

    public int getHeight() {
        return _iHeight;
    }

    public int getWidth() {
        return _iWidth;
    }

}


