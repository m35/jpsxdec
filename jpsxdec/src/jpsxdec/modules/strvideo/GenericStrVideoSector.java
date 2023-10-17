/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2021-2023  Michael Sabin
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

package jpsxdec.modules.strvideo;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.cdreaders.CdSector;
import jpsxdec.cdreaders.CdSectorXaSubHeader;
import jpsxdec.cdreaders.CdSectorXaSubHeader.SubMode;
import jpsxdec.i18n.exception.LocalizedIncompatibleException;
import jpsxdec.modules.video.sectorbased.SectorAbstractVideo;
import jpsxdec.modules.video.sectorbased.SectorBasedFrameAnalysis;
import jpsxdec.psxvideo.bitstreams.BitStreamAnalysis;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Lain;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.IBitStreamWith1QuantizationScale;
import jpsxdec.util.IO;

/**
 * Matches common STR video sector types, along with many video sector types
 * with small variations from the common values.
 *
 * Up to now I've had to manually update jPSXdec to support every video sector
 * variant found in the wild. Some were handled the easiest by making the
 * standard STR video sector a little more lenient. Others needed a new class
 * of their own.
 *
 * Recently Star Wars - Rebel Assault II was found to be too different to
 * fit within a more lenient normal STR sector, but not different enough to
 * warrant a new video sector type.
 *
 * This sector class takes a new minimalist approach. Turns out you really only
 * need these fields to know how to interpret a video sector:
 * - chunk number
 * - chunks in frame (although even this isn't really necessary)
 * - frame number (although you could probably get away with not relying on this either)
 * - dimensions
 * Sectors that have these valid values, along with a little additional
 * validation, will be considered a video sector.
 *
 * This opens the identification to be able to match not only known video sector
 * variants, but also unknown types I've never seen before, without the need
 * to be manually added to jPSXdec.
 *
 * For completeness, this will still try to detect known video sector header
 * variants, but won't prevent matching any yet unknown types.
 *
 * Taking this new flexible approach does create a challenge of replacing video
 * frames. If you don't know the exact sector type, how do you update the header
 * when replacing a frame? I found the following solution to work for all known
 * video sector variants:
 * - If the frame bitstream type is Lain, consider this a Lain video sector
 *   and update the header accordingly
 * - Otherwise, compare the values in the existing sector header with the
 *   corresponding values in the existing bitstream. If they match, update
 *   the sector header with the corresponding values in the new bitstream.
 *   This takes care of sector variants that have unknown values in the header
 *   that aren't associated with the bitstream.
 * However, it's still possible that there are video sector variants in the wild
 * that put values of the bitstream in non-standard locations in the header.
 * In that case, jPSXdec would fail to update those sectors properly which could
 * break the video.
 * Still, this won't actually become a problem until someone tries to replace
 * frames of a game that does this. If that happens, only then would
 * special handling need to be added to jPSXdec. But this isn't any different
 * than the approach until now of manually adding video sector types as they
 * are found in the wild.
 */
public class GenericStrVideoSector extends SectorAbstractVideo {

    private static final Logger LOG = Logger.getLogger(GenericStrVideoSector.class.getName());


    private abstract static class VideoSectorHeader {
        protected boolean __blnIsMatch = false;
        private final @Nonnull String __sType;
        public VideoSectorHeader(@Nonnull String type) {
            __sType = type;
        }
        final public boolean isMatch() {
            return __blnIsMatch;
        }
        final public @Nonnull String getType() {
            return __sType;
        }
        abstract public @Nonnull String getFieldsDescription();
        @Override
        final public @Nonnull String toString() {
            return __sType + "{" + getFieldsDescription() + "}";
        }
    }

    private class IkiHeader extends VideoSectorHeader {
        public IkiHeader() {
            super("Iki");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iUsually3800 == 0x3800 &&
                _iUsuallyQuantizationScale == _iWidth &&
                _iUsuallyBitstreamVersionNumber == _iHeight &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x dims again:%dx%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale, _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Chrono Cross.
     * @TODO: This bleeds the 'square' package into this package
     * creating a bit of a circular dependency. Fix this somehow. */
    public class ChronoXHeader extends VideoSectorHeader {
        public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 = 0x81010160L;
        public static final long CHRONO_CROSS_VIDEO_CHUNK_MAGIC2 = 0x01030160L;

        public ChronoXHeader() {
            super("Chrono Cross");
            __blnIsMatch =
                (_lngMagic == CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 || _lngMagic == CHRONO_CROSS_VIDEO_CHUNK_MAGIC2) &&
                _iFrameNumber >= 1 &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                (_iUsually3800 == 0x3800 || _iUsually3800 == 0) &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                _iUsuallyBitstreamVersionNumber == 2 &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x qscale:%d ver:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Serial Experiments Lain */
    private class LainHeader extends VideoSectorHeader {
        public LainHeader() {
            super("Lain");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iWidth == 320 && _iHeight == 240 &&
                _iFrameNumber >= 1 &&
                getLumaQscale() >= 0 && getLumaQscale() < 64 &&
                getChromaQscale() >= 0 && getChromaQscale() < 64 &&
                (_iUsually3800 == 0x3800 || _iUsually3800 == 0 || _iUsually3800 == _iFrameNumber) &&
                _iUsuallyQuantizationScale >= 0 &&
                _iUsuallyBitstreamVersionNumber == 0 &&
                _lngUsually0 == 0;
        }
        private int getLumaQscale() {
            return _iUsuallyHalfMdecCodeCountCeil32 & 0xff;
        }

        private int getChromaQscale() {
            return (_iUsuallyHalfMdecCodeCountCeil32 >> 8) &0xff;
        }
        @Override
        public String getFieldsDescription() {
            String s3800;
            if (_iUsually3800 == _iFrameNumber)
                s3800 = String.valueOf(_iUsually3800);
            else if (_iUsually3800 == 0)
                s3800 = "0";
            else
                s3800 = String.format("%04x", _iUsually3800);
            return String.format("qscaleL:%d qscaleC:%d 3800|0|frame:%s mdec count:%d 0:%d 0:%d",
                    getLumaQscale(),
                    getChromaQscale(),
                    s3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0
                    );
        }
    }

    /** Standard STR */
    private class StrHeader extends VideoSectorHeader {
        public StrHeader() {
            super("Str");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iFrameNumber >= 1 &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                _iUsually3800 == 0x3800 &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                (_iUsuallyBitstreamVersionNumber == 2 || _iUsuallyBitstreamVersionNumber == 3) &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x qscale:%d ver:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Standard STR but with bitstream version 1 */
    private class StrV1Header extends VideoSectorHeader {
        public StrV1Header() {
            super("StrV1");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iFrameNumber >= 1 &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                _iUsually3800 == 0x3800 &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                _iUsuallyBitstreamVersionNumber == 1 &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x qscale:%d ver:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Jackie Chan Stuntmaster */
    private class JackieChanHeader extends VideoSectorHeader {
        public JackieChanHeader() {
            super("Jackie Chan");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iWidth == 320 &&
                _iHeight == 240 &&
                _iFrameNumber >= 1 &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                _iUsually3800 == 0 &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                _iUsuallyBitstreamVersionNumber == 2 &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 0:%d qscale:%d ver:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Found in Super Puzzle Fighter and Resident Evil.
     * Assuming a general thing that could be found in other Capcom games. */
    private class CapcomHeader extends VideoSectorHeader {

        public CapcomHeader() {
            super("Capcom");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iFrameNumber >= 1 &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                _iUsually3800 == 0x3800 &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                (_iUsuallyBitstreamVersionNumber == 2 || _iUsuallyBitstreamVersionNumber == 3);
            if (__blnIsMatch) {
                // sector that the frame started at, relative to the start of the video
                int iMinFrameChunkSize = _iChunksInThisFrame < 4 ? 1 : _iChunksInThisFrame - 4; // XA sectors replacing frame sectors
                int iMinimumFrameStartSector = (_iFrameNumber - 1) * iMinFrameChunkSize;
                __blnIsMatch = _lngUsually0 > iMinimumFrameStartSector; // 0 would be valid, but that just makes it a standard STR
            }
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x qscale:%d ver:%d frame start sector:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Star Wars - Rebel Assault II */
    private class StarWars extends VideoSectorHeader {
        public StarWars() {
            super("Star Wars");
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iFrameNumber >= 1 && (
                    (_iWidth == 496 && _iHeight == 256) ||
                    (_iWidth == 432 && _iHeight == 260) ||
                    (_iWidth == 416 && _iHeight == 256) ||
                    (_iWidth == 320 && _iHeight == 240)
                ) &&
                _iUsuallyHalfMdecCodeCountCeil32 > 0 &&
                _iUsually3800 == 0x3800 &&
                _iUsuallyQuantizationScale > 0 && _iUsuallyQuantizationScale < 64 &&
                _iUsuallyBitstreamVersionNumber > 3 && // random value, but 0,1,2,3 can be considered other types
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("MDEC/2:%d 3800:%04x qscale:%d ?:%04x 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Reboot */
    private class RebootHeader extends VideoSectorHeader {
        public RebootHeader() {
            super("Reboot");
            // note there are some non-video sectors that look a little like video sectors but with 1x1 dimensions
            // but that won't be a problem with the logic used in this file
            __blnIsMatch =
                _lngMagic == STANDARD_STR_VIDEO_SECTOR_MAGIC &&
                _iFrameNumber >= 0 &&
                _iUsuallyHalfMdecCodeCountCeil32 == 0 &&
                _iUsually3800 >= 56 && _iUsually3800 <= 2000 && // value a little larger than frame count
                _iUsuallyQuantizationScale == 0 &&
                _iUsuallyBitstreamVersionNumber == 0 &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("0:%d >frame count:%d 0:%d 0:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** Ridge Racer Type 4 PAL [SCES-01706] */
    private class RidgeRacer4PalHeader extends VideoSectorHeader {
        public static final long RIDGE_RACER_TYPE_4_PAL_MAGIC = 0x00010160;

        public RidgeRacer4PalHeader() {
            super("RR4 PAL");
            __blnIsMatch =
                _lngMagic == RIDGE_RACER_TYPE_4_PAL_MAGIC &&
                _iFrameNumber >= 1 &&
                _iWidth == 320 && _iHeight == 176 &&
                _iUsuallyHalfMdecCodeCountCeil32 == 0 &&
                _iUsually3800 == 0 &&
                _iUsuallyQuantizationScale == 0 &&
                _iUsuallyBitstreamVersionNumber == 0 &&
                _lngUsually0 == 0;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("0:%d 0:%d 0:%d 0:%d 0:%d",
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    private class UnknownHeader extends VideoSectorHeader {
        public UnknownHeader() {
            super("?");
            __blnIsMatch = true;
        }
        @Override
        public String getFieldsDescription() {
            return String.format("magic:%08x MDEC/2?:%d 3800?:%04x qscale?:%d ver?:%d 0?:%d",
                    _lngMagic,
                    _iUsuallyHalfMdecCodeCountCeil32,
                    _iUsually3800,
                    _iUsuallyQuantizationScale,
                    _iUsuallyBitstreamVersionNumber,
                    _lngUsually0);
        }
    }

    /** The most common video sector magic number.
     * Shared by most types of video sectors. */
    public static final long STANDARD_STR_VIDEO_SECTOR_MAGIC = 0x80010160L;

    private long _lngMagic;                  //  @0   [4 bytes]
    private int  _iChunkNumber;              //  @4   [2 bytes]
    private int  _iChunksInThisFrame;        //  @6   [2 bytes]
    private int  _iFrameNumber;              //  @8   [4 bytes]
    private int  _iUsedDemuxedSizeRoundUp4;  //  @12  [4 bytes]
    private int  _iWidth;                    //  @16  [2 bytes]
    private int  _iHeight;                   //  @18  [2 bytes]

    private int  _iUsuallyHalfMdecCodeCountCeil32;  //  @20  [2 bytes]
    private int  _iUsually3800;                     //  @22  [2 bytes]
    private int  _iUsuallyQuantizationScale;        //  @24  [2 bytes]
    private int  _iUsuallyBitstreamVersionNumber;   //  @26  [2 bytes]
    private long _lngUsually0;                      //  @28  [4 bytes]
    //   32 TOTAL

    @CheckForNull
    private VideoSectorHeader[] _aoPossibleSectorTypes;
    @CheckForNull
    private String _sMatchingHeadersToString;
    @CheckForNull
    private String _sSectorType;

    @Override
    public int getVideoSectorHeaderSize() { return 32; }

    public GenericStrVideoSector(@Nonnull CdSector cdSector) {
        super(cdSector);
        if (isSuperInvalidElseReset()) return;

        // only if it has a sector header should we check if it reports DATA or VIDEO
        CdSectorXaSubHeader sh = cdSector.getSubHeader();
        if (sh != null) {
            if (sh.getSubMode().mask(SubMode.MASK_DATA | SubMode.MASK_VIDEO) == 0)
                return;
            if (sh.getSubMode().mask(SubMode.MASK_FORM) != 0)
                return;
        }

        _lngMagic = cdSector.readUInt32LE(0);
        if (_lngMagic != STANDARD_STR_VIDEO_SECTOR_MAGIC &&
            _lngMagic != ChronoXHeader.CHRONO_CROSS_VIDEO_CHUNK_MAGIC1 &&
            _lngMagic != ChronoXHeader.CHRONO_CROSS_VIDEO_CHUNK_MAGIC2 &&
            _lngMagic != RidgeRacer4PalHeader.RIDGE_RACER_TYPE_4_PAL_MAGIC)
            return;

        _iChunkNumber = cdSector.readSInt16LE(4);
        if (_iChunkNumber < 0 || _iChunkNumber > 50) // Limit frame size to 50 sectors long
            return;

        _iChunksInThisFrame = cdSector.readSInt16LE(6);
        if (_iChunksInThisFrame < 1 || _iChunksInThisFrame <= _iChunkNumber || _iChunksInThisFrame > 50) // Limit frame size to 50 sectors long
            return;

        // In standard STR videos the frame number starts at 1, but some variants start at 0
        _iFrameNumber = cdSector.readSInt32LE(8);
        if (_iFrameNumber < 0 || _iFrameNumber > 700000) // Limit to 1 sector/frame over an entire disc
            return;

        _iUsedDemuxedSizeRoundUp4 = cdSector.readSInt32LE(12);
        if (_iUsedDemuxedSizeRoundUp4 < 0 || _iUsedDemuxedSizeRoundUp4 > 50 * 2048) // Limit frame size to 50 sectors long
            return;

        _iWidth = cdSector.readSInt16LE(16);
        if (_iWidth < 8 || _iWidth >= 8192)  // Limit to an arbitrarily large value, but at least 1 8x8 block
            return;
        _iHeight = cdSector.readSInt16LE(18);
        if (_iHeight < 8 || _iHeight >= 8192)  // Limit to an arbitrarily large value, but at least 1 8x8 block
            return;

        _iUsuallyHalfMdecCodeCountCeil32 = cdSector.readUInt16LE(20);
        _iUsually3800 = cdSector.readUInt16LE(22);
        _iUsuallyQuantizationScale = cdSector.readSInt16LE(24);
        _iUsuallyBitstreamVersionNumber = cdSector.readUInt16LE(26);
        _lngUsually0 = cdSector.readUInt32LE(28);

        setProbability(100);
    }

    @Override
    public int getChunkNumber() {
        return _iChunkNumber;
    }

    @Override
    public int getChunksInFrame() {
        return _iChunksInThisFrame;
    }

    @Override
    public int getHeaderFrameNumber() {
        return _iFrameNumber;
    }

    @Override
    public int getHeight() {
        return _iHeight;
    }

    @Override
    public int getWidth() {
        return _iWidth;
    }

    @Override
    public @Nonnull String getTypeName() {
        if (_sSectorType == null) {
            VideoSectorHeader[] aoPossibleHeaders = checkTypes();
            StringBuilder sb = new StringBuilder();
            for (VideoSectorHeader possibleHeader : aoPossibleHeaders) {
                if (possibleHeader == null)
                    break;
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(possibleHeader.getType());
            }
            if (sb.length() == 0)
                _sSectorType = "?";
            else
                _sSectorType = sb.toString();
        }
        return "STR " + _sSectorType;
    }

    private @Nonnull VideoSectorHeader[] checkTypes() {
        if (_aoPossibleSectorTypes == null) {
            _aoPossibleSectorTypes = new VideoSectorHeader[10];
            int i = 0;
            if ((_aoPossibleSectorTypes[i] = new ChronoXHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new IkiHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new LainHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new CapcomHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new JackieChanHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new RebootHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new RidgeRacer4PalHeader()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new StarWars()).isMatch()) i++;
            if ((_aoPossibleSectorTypes[i] = new StrHeader()).isMatch()) i++;
            if (!(_aoPossibleSectorTypes[i] = new StrV1Header()).isMatch()) {
                if (i == 0)
                    _aoPossibleSectorTypes[0] = new UnknownHeader();
                else
                    _aoPossibleSectorTypes[i] = null;
            }
        }
        return _aoPossibleSectorTypes;
    }

    @Override
    public String toString() {
        if (_sMatchingHeadersToString == null) {
            VideoSectorHeader[] aoPossibleHeaders = checkTypes();
            StringBuilder sb = new StringBuilder();
            for (VideoSectorHeader possibleHeader : aoPossibleHeaders) {
                if (possibleHeader == null)
                    break;
                sb.append(' ').append(possibleHeader);
            }
            _sMatchingHeadersToString = sb.toString();
        }

        return String.format("%s %s frame:%d chunk:%d/%d %dx%d demux size:%d%s",
            getTypeName(),
            super.cdToString(),
            _iFrameNumber,
            _iChunkNumber,
            _iChunksInThisFrame,
            _iWidth,
            _iHeight,
            _iUsedDemuxedSizeRoundUp4,
            _sMatchingHeadersToString);
    }

    @Override
    public void replaceVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                         @Nonnull BitStreamAnalysis newFrame,
                                         @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {

        // Lain sectors are unique because they have different Luma and Chroma quantization scales
        if (existingFrame.isBitStreamClass(BitStreamUncompressor_Lain.class))
            replaceLainVideoSectorHeader(existingFrame, newFrame, abCurrentVidSectorHeader);
        else
            replaceStrVideoSectorHeader(existingFrame, newFrame, abCurrentVidSectorHeader);
    }

    private void replaceStrVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                             @Nonnull BitStreamAnalysis newFrame,
                                             @Nonnull byte[] abCurrentVidSectorHeader)
    {
        // All known sector variants include the demux size at offset 12
        int iExistingDemuxedSizeRoundUp4 = IO.readSInt32LE(abCurrentVidSectorHeader, 12);
        if (iExistingDemuxedSizeRoundUp4 != existingFrame.calculateUsedBytesRoundUp4())
            LOG.log(Level.WARNING, "Existing sector demux size 4 {0,number,#} != frame demux size 4 {1,number,#}",
                    new Object[] {iExistingDemuxedSizeRoundUp4, existingFrame.calculateUsedBytesRoundUp4()});
        IO.writeInt32LE(abCurrentVidSectorHeader, 12, newFrame.calculateUsedBytesRoundUp4());

        // The ceil(mdec/2,32) value appears at offset 20 in normal STR sectors, but some variants may not have this
        int iExistingSectorHeaderMdec = IO.readSInt16LE(abCurrentVidSectorHeader, 20);
        // Check if the existing value exists at this position
        if (iExistingSectorHeaderMdec == existingFrame.calculateMdecHalfCeiling32()) {
            // If so, replace it
            IO.writeInt16LE(abCurrentVidSectorHeader, 20, newFrame.calculateMdecHalfCeiling32());
        }

        // The quantization scale of the frame exists at offset 24 for most video sectors, but not all
        // If existing frame has a single qscale
        IBitStreamUncompressor existingBs = existingFrame.getCompletedBitStream();
        if (existingBs instanceof IBitStreamWith1QuantizationScale) {
            int iExistingBsQscale = ((IBitStreamWith1QuantizationScale)existingBs).getQuantizationScale();
            // Check if the existing value exists at the normal place where the qscale should go
            int iExistingSectorHeaderQscale = IO.readSInt16LE(abCurrentVidSectorHeader, 24);
            if (iExistingSectorHeaderQscale == iExistingBsQscale) {
                // If so, replace it with the new one
                IBitStreamUncompressor newBs = newFrame.getCompletedBitStream();
                int iNewBsQscale = ((IBitStreamWith1QuantizationScale)newBs).getQuantizationScale();
                IO.writeInt16LE(abCurrentVidSectorHeader, 24, (short)(iNewBsQscale));
            }
        }
    }

    private void replaceLainVideoSectorHeader(@Nonnull SectorBasedFrameAnalysis existingFrame,
                                              @Nonnull BitStreamAnalysis newFrame,
                                              @Nonnull byte[] abCurrentVidSectorHeader)
            throws LocalizedIncompatibleException
    {
        // no need to replace used demux size because Lain sectors are always set to max

        BitStreamUncompressor_Lain existingLainBs = (BitStreamUncompressor_Lain) existingFrame.getCompletedBitStream();
        BitStreamUncompressor_Lain newLainBs = (BitStreamUncompressor_Lain) newFrame.getCompletedBitStream();

        int iExistingSectorHeaderLumaQscale = abCurrentVidSectorHeader[20];
        int iExistingSectorHeaderChromaQscale = abCurrentVidSectorHeader[21];
        // 1 vid has 0 qscale in the header, so no need to replace
        if (iExistingSectorHeaderLumaQscale != 0 || iExistingSectorHeaderChromaQscale != 0) {
            int iExistingBsLumaQscale = existingLainBs.getLumaQscale();
            int iExistingBsChromaQscale = existingLainBs.getChromaQscale();
            if (iExistingSectorHeaderLumaQscale != iExistingBsLumaQscale ||
                iExistingSectorHeaderChromaQscale != iExistingBsChromaQscale)
            {
                // this technically shouldn't happen because the bitstream should have already been verified to be the same as the one found in the sectors
                // for this to happen, somehow lain bitstream currently exists in non lain sectors, which doesn't make any sense
                throw new RuntimeException("Existing sector somehow does not appear to be Lain");
            }

            abCurrentVidSectorHeader[20] = (byte)newLainBs.getLumaQscale();
            abCurrentVidSectorHeader[21] = (byte)newLainBs.getChromaQscale();
        }

        int iExistingSectorHeaderMdecCodeCount = getCdSector().readUInt16LE(24);
        // Some videos have 0 in the mdec code count
        if (iExistingSectorHeaderMdecCodeCount != 0) {
            IO.writeInt16LE(abCurrentVidSectorHeader, 24, (short)newFrame.getMdecCodeCount());
        }
    }

}

