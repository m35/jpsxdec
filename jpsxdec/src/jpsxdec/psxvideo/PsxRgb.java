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

package jpsxdec.psxvideo;

import java.util.Arrays;
import javax.annotation.Nonnull;

/**
 * Conversion to/from the different PlayStation RGB color formats.
 */
public final class PsxRgb {
    private PsxRgb() {}

    public static final int PSX_VRAM_WORD_WIDTH = 1024;
    public static final int PSX_VRAM_HEIGHT = 512;

    /** Works the same as
     * <pre>
     * int CONVERT_5_TO_8_BIT(int i) {
     *   return (int)Math.round((double)i / 31.0);
     * }
     * </pre> */
    private static final int[] CONVERT_5_TO_8_BIT = new int[/*32*/] {
          0,   8,  16,  25,  33,  41,  49,  58,
         66,  74,  82,  90,  99, 107, 115, 123,
        132, 140, 148, 156, 165, 173, 181, 189,
        197, 206, 214, 222, 230, 239, 247, 255
    };
    static { assert CONVERT_5_TO_8_BIT.length == 32; }

    public static int psxABGR1555toARGB8888(byte b8first, byte b8second, int iAlphaValue) {
        int iPsx16 = ((b8first & 0xff)) | ((b8second & 0xff) << 8);
        return psxABGR1555toARGB8888(iPsx16, iAlphaValue);
    }

    /** PlayStation 16-bit ABGR1555 to ARGB8888. */
    public static int psxABGR1555toARGB8888(int i16, int iAlphaValue) {
        int b = CONVERT_5_TO_8_BIT[(i16 >>> 10) & 0x1F];
        int g = CONVERT_5_TO_8_BIT[(i16 >>>  5) & 0x1F];
        int r = CONVERT_5_TO_8_BIT[(i16       ) & 0x1F];
        int a;

        if (r == 0 && g == 0 && b == 0) {
            if ((i16 & 0x8000) == 0)
                // black, and the alpha bit is NOT set
                a = (byte)0; // totally transparent
            else
                // black, and the alpha bit IS set
                a = (byte)255; // totally opaque
        } else {
            if ((i16 & 0x8000) == 0)
                // some color, and the alpha bit is NOT set
                a = (byte)255; // totally opaque
            else
                // some color, and the alpha bit IS set
                a = (byte)iAlphaValue; // some variance of transparency
        }

        return a << 24 | r << 16 | g << 8 | b;
    }


    /** Works the same as
     * <pre>
     * int CONVERT_8_TO_5_BIT(int i) {
     *   return (int)Math.round(i*255/31.0);
     * }
     * </pre> */
    private static final int[] CONVERT_8_TO_5_BIT = {
        0,  0,  0,  0,  0,
        1,  1,  1,  1,  1,  1,  1,  1,
        2,  2,  2,  2,  2,  2,  2,  2,
        3,  3,  3,  3,  3,  3,  3,  3,
        4,  4,  4,  4,  4,  4,  4,  4,  4,
        5,  5,  5,  5,  5,  5,  5,  5,
        6,  6,  6,  6,  6,  6,  6,  6,
        7,  7,  7,  7,  7,  7,  7,  7,
        8,  8,  8,  8,  8,  8,  8,  8,
        9,  9,  9,  9,  9,  9,  9,  9,  9,
        10, 10, 10, 10, 10, 10, 10, 10,
        11, 11, 11, 11, 11, 11, 11, 11,
        12, 12, 12, 12, 12, 12, 12, 12,
        13, 13, 13, 13, 13, 13, 13, 13, 13,
        14, 14, 14, 14, 14, 14, 14, 14,
        15, 15, 15, 15, 15, 15, 15, 15,
        16, 16, 16, 16, 16, 16, 16, 16,
        17, 17, 17, 17, 17, 17, 17, 17,
        18, 18, 18, 18, 18, 18, 18, 18, 18,
        19, 19, 19, 19, 19, 19, 19, 19,
        20, 20, 20, 20, 20, 20, 20, 20,
        21, 21, 21, 21, 21, 21, 21, 21,
        22, 22, 22, 22, 22, 22, 22, 22, 22,
        23, 23, 23, 23, 23, 23, 23, 23,
        24, 24, 24, 24, 24, 24, 24, 24,
        25, 25, 25, 25, 25, 25, 25, 25,
        26, 26, 26, 26, 26, 26, 26, 26,
        27, 27, 27, 27, 27, 27, 27, 27, 27,
        28, 28, 28, 28, 28, 28, 28, 28,
        29, 29, 29, 29, 29, 29, 29, 29,
        30, 30, 30, 30, 30, 30, 30, 30,
        31, 31, 31, 31, 31,
    }; static { assert CONVERT_8_TO_5_BIT.length == 256; }

    /** ARGB8888 to PlayStation 16-bit ABGR1555. */
    public static short ARGB8888toPsxABGR1555(int i) {
        int a = (i >>> 24) & 0xFF;
        int r = CONVERT_8_TO_5_BIT[(i >>> 16) & 0xFF];
        int g = CONVERT_8_TO_5_BIT[(i >>>  8) & 0xFF];
        int b = CONVERT_8_TO_5_BIT[(i       ) & 0xFF];
        int bgr = (b << 10) | (g << 5) | r;
        if (a == 0) {
            // if totally transparent
            bgr = 0;
            a   = 0;
        } else if (a == 255) {
            // if totally opaque
            if (bgr == 0) // if totally opaque & black
                a = 1;
            else // if totally opaque & not black
                a = 0;
        } else {
            // if partially transparent
            a = 1;
        }
        return (short)((a << 15) | bgr);
    }


    /** Works the same as
     * <pre>
     * byte CONVERT_4_TO_8_BIT(int i) {
     *   return (byte)Math.round(i*15/255.0);
     * }
     * </pre> */
    private static final byte[] CONVERT_4_TO_8_BIT =
    {
        (byte)  0, (byte) 17, (byte) 34, (byte) 51,
        (byte) 68, (byte) 85, (byte)102, (byte)119,
        (byte)136, (byte)153, (byte)170, (byte)187,
        (byte)204, (byte)221, (byte)238, (byte)255,
    };
    static { assert CONVERT_4_TO_8_BIT.length == 16; }

    private static final byte[] GRAY_16_PALETTE = build16GrayRgbaPalette();
    private static @Nonnull byte[] build16GrayRgbaPalette() {
        byte[] abPalette = new byte[4*16];
        for (int i = 0; i < 16; i++) {
            byte bClr = CONVERT_4_TO_8_BIT[i];
            abPalette[i*4+0] = bClr; // r
            abPalette[i*4+1] = bClr; // g
            abPalette[i*4+2] = bClr; // b
            abPalette[i*4+3] = (byte)255; // a
        }
        return abPalette;
    }
    public static @Nonnull byte[] get16GrayRgbaPalette() {
        return Arrays.copyOf(GRAY_16_PALETTE, GRAY_16_PALETTE.length);
    }
    /** Fills buffer with RGBA8888 grayscale values (no transparency).
     * Assumes buffer is 16*4 bytes long. */
    public static void fill16GrayRgbaPalette(@Nonnull byte[] abPalette) {
        System.arraycopy(GRAY_16_PALETTE, 0, abPalette, 0, GRAY_16_PALETTE.length);
    }

    private static final byte[] GRAY_256_PALETTE = build256GrayRgbaPalette();
    private static @Nonnull byte[] build256GrayRgbaPalette() {
        byte[] abPalette = new byte[4*256];
        for (int i = 0; i < 256; i++) {
            byte bClr = (byte)i;
            abPalette[i*4+0] = bClr; // r
            abPalette[i*4+1] = bClr; // g
            abPalette[i*4+2] = bClr; // b
            abPalette[i*4+3] = (byte)255; // a
        }
        return abPalette;
    }
    public static @Nonnull byte[] get256GrayRgbaPalette() {
        return Arrays.copyOf(GRAY_256_PALETTE, GRAY_256_PALETTE.length);
    }
    /** Fills buffer with RGBA8888 grayscale values.
     * Assumes buffer is 256*4 bytes long. */
    public static void fill256GrayRgbaPalette(@Nonnull byte[] abPalette) {
        System.arraycopy(GRAY_256_PALETTE, 0, abPalette, 0, GRAY_256_PALETTE.length);
    }

}
