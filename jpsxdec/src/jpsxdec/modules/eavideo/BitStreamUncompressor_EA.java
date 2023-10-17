/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2019-2023  Michael Sabin
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

package jpsxdec.modules.eavideo;

import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.ArrayBitReader;
import jpsxdec.psxvideo.bitstreams.BitStreamCode;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.bitstreams.ZeroRunLengthAcLookup;

public class BitStreamUncompressor_EA extends BitStreamUncompressor {

    private final int _iQuantizationScale;

    public BitStreamUncompressor_EA(@Nonnull byte[] abMdecPacketPayload,
                                         @Nonnull ZeroRunLengthAcLookup lookupTable,
                                         int iQuantizationScale)
    {
        super(
            new ArrayBitReader(abMdecPacketPayload, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER, 0, abMdecPacketPayload.length),
            lookupTable,
            new BitStreamUncompressor_STRv2.QuantizationDcReader_STRv12(iQuantizationScale),
            BitStreamUncompressor_STRv2.AC_ESCAPE_CODE_STR,
            BitStreamUncompressor.FRAME_END_PADDING_BITS_NONE
        );
        _iQuantizationScale = iQuantizationScale;
    }

    @Override
    public @Nonnull BitStreamCompressor makeCompressor() {
        // Writing a EA video encoder would be a significant amount of work.
        // And that might be ok, except to properly replace EA videos would
        // require re-building all of the packets from start to finish.
        // That is beyond the scope jPSXdec's functionality. Modders will have
        // to handle that part on their own.
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return super.toString() + " qscale:" + _iQuantizationScale;
    }

    /**
     * This MDEC code has special meaning, it is treated as the escape code.
    */
    static final int BITSTREAM_ESCAPE_CODE = 0x7c1f;

    /** The VLC0 packet contains a list of MDEC codes that are mapped to bit-codes in this order. */
    static final BitStreamCode[] EA_VIDEO_BIT_CODE_ORDER = {
        BitStreamCode._10_______________,
        BitStreamCode._110______________,
        BitStreamCode._111______________,
        BitStreamCode._0110_____________,
        BitStreamCode._0111_____________,
        BitStreamCode._01000____________,
        BitStreamCode._01001____________,
        BitStreamCode._01010____________,
        BitStreamCode._01011____________,
        BitStreamCode._001010___________,
        BitStreamCode._001011___________,
        BitStreamCode._001110___________,
        BitStreamCode._001111___________,
        BitStreamCode._001100___________,
        BitStreamCode._001101___________,
        BitStreamCode._0001100__________,
        BitStreamCode._0001101__________,
        BitStreamCode._0001110__________,
        BitStreamCode._0001111__________,
        BitStreamCode._0001010__________,
        BitStreamCode._0001011__________,
        BitStreamCode._0001000__________,
        BitStreamCode._0001001__________,
        BitStreamCode._000001___________,
        BitStreamCode._00001100_________,
        BitStreamCode._00001101_________,
        BitStreamCode._00001000_________,
        BitStreamCode._00001001_________,
        BitStreamCode._00001110_________,
        BitStreamCode._00001111_________,
        BitStreamCode._00001010_________,
        BitStreamCode._00001011_________,
        BitStreamCode._001001100________,
        BitStreamCode._001001101________,
        BitStreamCode._001000010________,
        BitStreamCode._001000011________,
        BitStreamCode._001001010________,
        BitStreamCode._001001011________,
        BitStreamCode._001001000________,
        BitStreamCode._001001001________,
        BitStreamCode._001001110________,
        BitStreamCode._001001111________,
        BitStreamCode._001000110________,
        BitStreamCode._001000111________,
        BitStreamCode._001000100________,
        BitStreamCode._001000101________,
        BitStreamCode._001000000________,
        BitStreamCode._001000001________,
        BitStreamCode._00000010100______,
        BitStreamCode._00000010101______,
        BitStreamCode._00000011000______,
        BitStreamCode._00000011001______,
        BitStreamCode._00000010110______,
        BitStreamCode._00000010111______,
        BitStreamCode._00000011110______,
        BitStreamCode._00000011111______,
        BitStreamCode._00000010010______,
        BitStreamCode._00000010011______,
        BitStreamCode._00000011100______,
        BitStreamCode._00000011101______,
        BitStreamCode._00000011010______,
        BitStreamCode._00000011011______,
        BitStreamCode._00000010000______,
        BitStreamCode._00000010001______,
        BitStreamCode._0000000111010____,
        BitStreamCode._0000000111011____,
        BitStreamCode._0000000110000____,
        BitStreamCode._0000000110001____,
        BitStreamCode._0000000100110____,
        BitStreamCode._0000000100111____,
        BitStreamCode._0000000100000____,
        BitStreamCode._0000000100001____,
        BitStreamCode._0000000110110____,
        BitStreamCode._0000000110111____,
        BitStreamCode._0000000101000____,
        BitStreamCode._0000000101001____,
        BitStreamCode._0000000111000____,
        BitStreamCode._0000000111001____,
        BitStreamCode._0000000100100____,
        BitStreamCode._0000000100101____,
        BitStreamCode._0000000111100____,
        BitStreamCode._0000000111101____,
        BitStreamCode._0000000101010____,
        BitStreamCode._0000000101011____,
        BitStreamCode._0000000100010____,
        BitStreamCode._0000000100011____,
        BitStreamCode._0000000111110____,
        BitStreamCode._0000000111111____,
        BitStreamCode._0000000110100____,
        BitStreamCode._0000000110101____,
        BitStreamCode._0000000110010____,
        BitStreamCode._0000000110011____,
        BitStreamCode._0000000101110____,
        BitStreamCode._0000000101111____,
        BitStreamCode._0000000101100____,
        BitStreamCode._0000000101101____,
        BitStreamCode._00000000110100___,
        BitStreamCode._00000000110101___,
        BitStreamCode._00000000110010___,
        BitStreamCode._00000000110011___,
        BitStreamCode._00000000110000___,
        BitStreamCode._00000000110001___,
        BitStreamCode._00000000101110___,
        BitStreamCode._00000000101111___,
        BitStreamCode._00000000101100___,
        BitStreamCode._00000000101101___,
        BitStreamCode._00000000101010___,
        BitStreamCode._00000000101011___,
        BitStreamCode._00000000101000___,
        BitStreamCode._00000000101001___,
        BitStreamCode._00000000100110___,
        BitStreamCode._00000000100111___,
        BitStreamCode._00000000100100___,
        BitStreamCode._00000000100101___,
        BitStreamCode._00000000100010___,
        BitStreamCode._00000000100011___,
        BitStreamCode._00000000100000___,
        BitStreamCode._00000000100001___,
        BitStreamCode._00000000111110___,
        BitStreamCode._00000000111111___,
        BitStreamCode._00000000111100___,
        BitStreamCode._00000000111101___,
        BitStreamCode._00000000111010___,
        BitStreamCode._00000000111011___,
        BitStreamCode._00000000111000___,
        BitStreamCode._00000000111001___,
        BitStreamCode._00000000110110___,
        BitStreamCode._00000000110111___,
        BitStreamCode._000000000111110__,
        BitStreamCode._000000000111111__,
        BitStreamCode._000000000111100__,
        BitStreamCode._000000000111101__,
        BitStreamCode._000000000111010__,
        BitStreamCode._000000000111011__,
        BitStreamCode._000000000111000__,
        BitStreamCode._000000000111001__,
        BitStreamCode._000000000110110__,
        BitStreamCode._000000000110111__,
        BitStreamCode._000000000110100__,
        BitStreamCode._000000000110101__,
        BitStreamCode._000000000110010__,
        BitStreamCode._000000000110011__,
        BitStreamCode._000000000110000__,
        BitStreamCode._000000000110001__,
        BitStreamCode._000000000101110__,
        BitStreamCode._000000000101111__,
        BitStreamCode._000000000101100__,
        BitStreamCode._000000000101101__,
        BitStreamCode._000000000101010__,
        BitStreamCode._000000000101011__,
        BitStreamCode._000000000101000__,
        BitStreamCode._000000000101001__,
        BitStreamCode._000000000100110__,
        BitStreamCode._000000000100111__,
        BitStreamCode._000000000100100__,
        BitStreamCode._000000000100101__,
        BitStreamCode._000000000100010__,
        BitStreamCode._000000000100011__,
        BitStreamCode._000000000100000__,
        BitStreamCode._000000000100001__,
        BitStreamCode._0000000000110000_,
        BitStreamCode._0000000000110001_,
        BitStreamCode._0000000000101110_,
        BitStreamCode._0000000000101111_,
        BitStreamCode._0000000000101100_,
        BitStreamCode._0000000000101101_,
        BitStreamCode._0000000000101010_,
        BitStreamCode._0000000000101011_,
        BitStreamCode._0000000000101000_,
        BitStreamCode._0000000000101001_,
        BitStreamCode._0000000000100110_,
        BitStreamCode._0000000000100111_,
        BitStreamCode._0000000000100100_,
        BitStreamCode._0000000000100101_,
        BitStreamCode._0000000000100010_,
        BitStreamCode._0000000000100011_,
        BitStreamCode._0000000000100000_,
        BitStreamCode._0000000000100001_,
        BitStreamCode._0000000000111110_,
        BitStreamCode._0000000000111111_,
        BitStreamCode._0000000000111100_,
        BitStreamCode._0000000000111101_,
        BitStreamCode._0000000000111010_,
        BitStreamCode._0000000000111011_,
        BitStreamCode._0000000000111000_,
        BitStreamCode._0000000000111001_,
        BitStreamCode._0000000000110110_,
        BitStreamCode._0000000000110111_,
        BitStreamCode._0000000000110100_,
        BitStreamCode._0000000000110101_,
        BitStreamCode._0000000000110010_,
        BitStreamCode._0000000000110011_,
        BitStreamCode._00000000000100110,
        BitStreamCode._00000000000100111,
        BitStreamCode._00000000000100100,
        BitStreamCode._00000000000100101,
        BitStreamCode._00000000000100010,
        BitStreamCode._00000000000100011,
        BitStreamCode._00000000000100000,
        BitStreamCode._00000000000100001,
        BitStreamCode._00000000000101000,
        BitStreamCode._00000000000101001,
        BitStreamCode._00000000000110100,
        BitStreamCode._00000000000110101,
        BitStreamCode._00000000000110010,
        BitStreamCode._00000000000110011,
        BitStreamCode._00000000000110000,
        BitStreamCode._00000000000110001,
        BitStreamCode._00000000000101110,
        BitStreamCode._00000000000101111,
        BitStreamCode._00000000000101100,
        BitStreamCode._00000000000101101,
        BitStreamCode._00000000000101010,
        BitStreamCode._00000000000101011,
        BitStreamCode._00000000000111110,
        BitStreamCode._00000000000111111,
        BitStreamCode._00000000000111100,
        BitStreamCode._00000000000111101,
        BitStreamCode._00000000000111010,
        BitStreamCode._00000000000111011,
        BitStreamCode._00000000000111000,
        BitStreamCode._00000000000111001,
        BitStreamCode._00000000000110110,
        BitStreamCode._00000000000110111,
    };

    static {
        if (EA_VIDEO_BIT_CODE_ORDER.length != BitStreamCode.getTotalCount())
            throw new AssertionError("EA VLC code count is wrong");
    }

}
