/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2020-2023  Michael Sabin
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

package jpsxdec.modules.starwars;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.ArrayBitReader;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv3;
import jpsxdec.psxvideo.bitstreams.IBitStreamWith1QuantizationScale;
import jpsxdec.psxvideo.bitstreams.IByteOrder;
import jpsxdec.psxvideo.bitstreams.ZeroRunLengthAcLookup_STR;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;

/**
 * Star Wars - Rebel Assault II - The Hidden Empire bitstream.
 * Star Wars bitstreams follow STRv2 and STRv3 styles.
 * Bytes are read as 16-bit little-endian values like normal.
 * What's unique is the first 2 bytes of the bitstream are swapped
 * with the 3rd and 4th bytes.
 */
public class BitStreamUncompressor_StarWars extends BitStreamUncompressor implements IBitStreamWith1QuantizationScale {

    public static @CheckForNull StarWarsHeader makeStarWarsHeader(@Nonnull byte[] abFrameData, int iDataSize) {
        if (iDataSize < 12) {
            return null;
        } else {
            int iHalfMdecCodeCountCeil32 = IO.readSInt16LE(abFrameData, 0);
            int iMagic3800               = IO.readUInt16LE(abFrameData, 2);
            int iQscale                  = IO.readSInt16LE(abFrameData, 4);
            // This normally is the frame version (e.g. 2 for STRv2 or 3 for STRv3)
            // but for this game, there is just a random number here.
            int iRandomVersion           = IO.readUInt16LE(abFrameData, 6);

            if (iMagic3800 != 0x3800 ||
                iQscale < 1 || iQscale > 63 ||
                iHalfMdecCodeCountCeil32 < 0)
            {
                return null;
            }

            // Lookup if the frame follows STRv2 or STRv3 style
            int iFrameVersion = StarWarsFrameTypeLookup.getFrameType(abFrameData);

            if (iFrameVersion != 2 && iFrameVersion != 3) {
                return null;
            } else {
                return new StarWarsHeader(iHalfMdecCodeCountCeil32, iQscale, iRandomVersion, iFrameVersion == 2);
            }
        }
    }

    public static class StarWarsHeader {

        public static final int SIZEOF = 8;

        private final int _iHalfMdecCodeCountCeil32;
        private final int _iQuantizationScale;
        private final int _iRandomVersion;
        /** true if the frame type is STRv2, otherwise STRv3. */
        private final boolean _blnIsVer2Not3;

        StarWarsHeader(int iHalfMdecCodeCountCeil32, int iQuantizationScale, int iRandomVersion, boolean blnIsVer2Not3) {
            _iQuantizationScale = iQuantizationScale;
            _iHalfMdecCodeCountCeil32 = iHalfMdecCodeCountCeil32;
            _iRandomVersion = iRandomVersion;
            _blnIsVer2Not3 = blnIsVer2Not3;
        }

        public boolean isVersion2Not3() {
            return _blnIsVer2Not3;
        }

        public int getQuantizationScale() {
            return _iQuantizationScale;
        }

        @Override
        public String toString() {
            return String.format("MDEC/2:%d Qscale:%d RandVer:%04x type:%d",
                                 _iHalfMdecCodeCountCeil32, _iQuantizationScale,
                                 _iRandomVersion, _blnIsVer2Not3 ? 2 : 3);
        }
    }

    /** 16-bit little-endian, except the first 2 bytes and the second 2 bytes are swapped. */
    private static final IByteOrder LITTLE_ENDIAN_FIRST4_SWAP = new IByteOrder() {
        @Override
        public int getByteOffset(int iByteIndex) {
            if (iByteIndex < 2) {
                iByteIndex += 2;
            } else if (iByteIndex < 4) {
                iByteIndex -= 2;
            }
            return iByteIndex ^ 1;
        }
        @Override
        public int getPaddingByteAlign() {
            return 4;
        }
    };

    public static @Nonnull BitStreamUncompressor_StarWars makeStarWars(@Nonnull byte[] abBitstream)
            throws BinaryDataNotRecognized
    {
        return makeStarWars(abBitstream, abBitstream.length);
    }
    public static @Nonnull BitStreamUncompressor_StarWars makeStarWars(@Nonnull byte[] abBitstream, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_StarWars bsu = makeStarWarsNoThrow(abBitstream, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    public static @CheckForNull BitStreamUncompressor_StarWars makeStarWarsNoThrow(@Nonnull byte[] abBitstream, int iDataSize)
            throws BinaryDataNotRecognized
    {
        StarWarsHeader header = makeStarWarsHeader(abBitstream, iDataSize);
        if (header == null)
            return null;

        return makeStarWars(abBitstream, iDataSize, header);
    }


    static @Nonnull BitStreamUncompressor_StarWars makeStarWars(@Nonnull byte[] abBitstream, int iDataSize,
                                                                @Nonnull StarWarsHeader header)
    {
        IQuantizationDcReader qscaleDcReader;
        IFrameEndPaddingBits endingBits;

        if (header.isVersion2Not3()) {
            // STRv2
            qscaleDcReader = new BitStreamUncompressor_STRv2.QuantizationDcReader_STRv12(header.getQuantizationScale());
            endingBits = BitStreamUncompressor_STRv2.FRAME_END_PADDING_BITS_STRV2;
        } else {
            // STRv3
            qscaleDcReader = new BitStreamUncompressor_STRv3.QuantizationDcReader_STRv3(header.getQuantizationScale());
            endingBits = BitStreamUncompressor_STRv3.FRAME_END_PADDING_BITS_STRV3;
        }

        ArrayBitReader bitReader = new ArrayBitReader(abBitstream, LITTLE_ENDIAN_FIRST4_SWAP, StarWarsHeader.SIZEOF, iDataSize);
        return new BitStreamUncompressor_StarWars(header, bitReader, qscaleDcReader, endingBits);
    }

    @Nonnull
    private final StarWarsHeader _header;

    private BitStreamUncompressor_StarWars(@Nonnull StarWarsHeader header,
                                           @Nonnull ArrayBitReader bitReader,
                                           @Nonnull IQuantizationDcReader qDcReader,
                                           @Nonnull IFrameEndPaddingBits padding)
    {
        super(bitReader, ZeroRunLengthAcLookup_STR.AC_VARIABLE_LENGTH_CODES_MPEG1,
              qDcReader, BitStreamUncompressor_STRv2.AC_ESCAPE_CODE_STR,
              padding);
        _header = header;
    }

    @Override
    public int getQuantizationScale() {
        return _header.getQuantizationScale();
    }

    @Override
    public String toString() {
        return super.toString() + " " + _header;
    }

    @Override
    public @Nonnull BitStreamCompressor makeCompressor() throws UnsupportedOperationException {
        if (_header.isVersion2Not3())
            return new BitStreamCompressor_StarWarsV2(_context.getTotalBlocksRead(), _header.getQuantizationScale());
        else
            return new BitStreamCompressor_StarWarsV3(_context.getTotalBlocksRead(), _header.getQuantizationScale());
    }

    // =========================================================================

    public static class BitStreamCompressor_StarWarsV2 extends BitStreamUncompressor_STRv2.BitStreamCompressor_STRv2 {

        public BitStreamCompressor_StarWarsV2(int iMacroBlockCount, int iOriginalQscale) {
            super(iMacroBlockCount, iOriginalQscale, LITTLE_ENDIAN_FIRST4_SWAP);
        }
    }

    public static class BitStreamCompressor_StarWarsV3 extends BitStreamUncompressor_STRv3.BitStreamCompressor_STRv3 {

        public BitStreamCompressor_StarWarsV3(int iMacroBlockCount, int iOriginalQscale) {
            super(iMacroBlockCount, iOriginalQscale, LITTLE_ENDIAN_FIRST4_SWAP);
        }
    }
}
