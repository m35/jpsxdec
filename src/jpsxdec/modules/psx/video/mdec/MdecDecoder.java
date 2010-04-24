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

package jpsxdec.modules.psx.video.mdec;

import jpsxdec.formats.RgbIntImage;

/** Super class of the two different MDEC decoders: int and double. */
public abstract class MdecDecoder {

    /** Stores the 4 luminance values for 1 sub-sampled chrominance value. */
    protected static class LuminSubSampleIndexes {
        /** Top-left value. */
        public final int TL;
        /** Top-right value. */
        public final int TR;
        /** Bottom-left value. */
        public final int BL;
        /** Bottom-right value. */
        public final int BR;
        public LuminSubSampleIndexes(int iTopLeft, int iTopRight, 
                                     int iBottomLeft, int iBottomRight)
        {
            TL = iTopLeft;
            TR = iTopRight;
            BL = iBottomLeft;
            BR = iBottomRight;
        }
    }

    /** Function with a shorter name to construct LuminSubSampleIndexes objects. */
    private static LuminSubSampleIndexes L(int iTopLeft, int iTopRight,
                                           int iBottomLeft, int iBottomRight)
    {
        return new LuminSubSampleIndexes(iTopLeft, iTopRight,
                                         iBottomLeft, iBottomRight);
    }

    /** Lookup table used to find the four lumin values associated with one
     * subsampled chrom value. The indexes are depended on how the lumin values
     * are stored in the internal buffer (which is one 8x8 block after another).
     * <p>
     * This is how the indexes are arranged visually for a 16x16 macro block.
     * <pre>
     *   0   1   2   3   4   5   6   7   64  65  66  67  68  69  70  71
     *   8   9  10  11  12  13  14  15   72  73  74  75  76  77  78  79
     *  16  17  18  19  20  21  22  23   80  81  82  83  84  85  86  87
     *  24  25  26  27  28  29  30  31   88  89  90  91  92  93  94  95
     *  32  33  34  35  36  37  38  39   96  97  98  99 100 101 102 103
     *  40  41  42  43  44  45  46  47  104 105 106 107 108 109 110 111
     *  48  49  50  51  52  53  54  55  112 113 114 115 116 117 118 119
     *  56  57  58  59  60  61  62  63  120 121 122 123 124 125 126 127
     *
     * 128 129 130 131 132 133 134 135  192 193 194 195 196 197 198 199
     * 136 137 138 139 140 141 142 143  200 201 202 203 204 205 206 207
     * 144 145 146 147 148 149 150 151  208 209 210 211 212 213 214 215
     * 152 153 154 155 156 157 168 159  216 217 218 219 220 221 222 223
     * 160 161 162 163 164 165 166 167  224 225 226 227 228 229 230 231
     * 168 169 170 171 172 173 174 175  232 233 234 235 236 237 238 239
     * 176 177 178 179 180 181 182 183  240 241 242 243 244 245 246 247
     * 184 185 186 187 188 189 190 191  248 249 250 251 252 253 254 255
     * </pre>
     */
    protected static final LuminSubSampleIndexes[] LUMIN_SUBSAMPLING_SEQUENCE = {
L(  0,   1,   8,   9), L(  2,   3,  10,  11), L(  4,   5,  12,  13), L(  6,   7,  14,  15),
L( 64,  65,  72,  73), L( 66,  67,  74,  75), L( 68,  69,  76,  77), L( 70,  71,  78,  79),
L( 16,  17,  24,  25), L( 18,  19,  26,  27), L( 20,  21,  28,  29), L( 22,  23,  30,  31),
L( 80,  81,  88,  89), L( 82,  83,  90,  91), L( 84,  85,  92,  93), L( 86,  87,  94,  95),
L( 32,  33,  40,  41), L( 34,  35,  42,  43), L( 36,  37,  44,  45), L( 38,  39,  46,  47),
L( 96,  97, 104, 105), L( 98,  99, 106, 107), L(100, 101, 108, 109), L(102, 103, 110, 111),
L( 48,  49,  56,  57), L( 50,  51,  58,  59), L( 52,  53,  60,  61), L( 54,  55,  62,  63),
L(112, 113, 120, 121), L(114, 115, 122, 123), L(116, 117, 124, 125), L(118, 119, 126, 127),
L(128, 129, 136, 137), L(130, 131, 138, 139), L(132, 133, 140, 141), L(134, 135, 142, 143),
L(192, 193, 200, 201), L(194, 195, 202, 203), L(196, 197, 204, 205), L(198, 199, 206, 207),
L(144, 145, 152, 153), L(146, 147, 154, 155), L(148, 149, 156, 157), L(150, 151, 158, 159),
L(208, 209, 216, 217), L(210, 211, 218, 219), L(212, 213, 220, 221), L(214, 215, 222, 223),
L(160, 161, 168, 169), L(162, 163, 170, 171), L(164, 165, 172, 173), L(166, 167, 174, 175),
L(224, 225, 232, 233), L(226, 227, 234, 235), L(228, 229, 236, 237), L(230, 231, 238, 239),
L(176, 177, 184, 185), L(178, 179, 186, 187), L(180, 181, 188, 189), L(182, 183, 190, 191),
L(240, 241, 248, 249), L(242, 243, 250, 251), L(244, 245, 252, 253), L(246, 247, 254, 255),
    };

    /** Reads an image from the MdecInputStream and decodes it to an internal
     *  PSX YCbCr buffer. */
    abstract public void decode(MdecInputStream oUncompressor)
            throws DecodingException;
    
    /** Retrieve the contents of the internal PSX YCbCr buffer converted to RGB. */
    abstract public void readDecodedRGB(RgbIntImage oRgb);
}
