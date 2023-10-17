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

package jpsxdec.modules.panekit;

import javax.annotation.Nonnull;
import jpsxdec.util.IO;

/**
 * Panekit - Infinitive Crafting Toy Case [SCPS-10096] IKI bitstream
 * obfuscation. The first 32 bytes of an IKI frame are rearranged
 * and modified for no reason.
 *
 * This is the first time in 10+ years I've come across
 * an obvious case of obfuscation in a PlayStation game.
 */
public class PanekitIkiObfuscation {

    /*
    Normal IKI header:
    @0  2 bytes : mdec code count
    @2  2 bytes : magic 0x3800
    @4  2 bytes : width
    @6  2 bytes : height
    @8  2 bytes : compressed data size
    @10+        : compressed lzss data

    IKI map     <- Panekit
    @0  2 bytes <- @0  : ~(mdec code count) [binary not]
    @2  2 bytes <- @2  : ~(3800) [binary not]
    @8  2 bytes <- @4  : compressed data size
    @10 2 bytes <- @6  : lzss bytes (10 and 11)
    @4  2 bytes <- @8  : width
    @6  2 bytes <- @10 : height
    @14 2 bytes <- @12 : lzss bytes (14 and 15)
    @12 2 bytes <- @14 : lzss bytes (12 and 13)
    @16 4 bytes <- @16 : lzss bytes (16, 17, 18, and 19) - 36997
    @20 2 bytes <- @20 : lzss bytes (20 and 21)
    @22 2 bytes <- @22 : lzss bytes (22 and 23) + 26900
    @24 4 bytes <- @24 : lzss bytes (24, 25, 26 and 27) - 34975
    @28 2 bytes <- @28 : lzss bytes (28 and 29)
    @30 2 bytes <- @30 : lzss bytes (30 and 31) + 27396
    */

    /**
     * Modifies the Panekit frame to deobfuscate the first 32 bytes,
     * making it a standard IKI frame.
     */
    public static void deobfuscate(@Nonnull byte[] abPanekitFrame) {

        int iMdecCodeCount      = IO.readSInt16LE(abPanekitFrame, 0);
        int iMagic3800          = IO.readSInt16LE(abPanekitFrame, 2);
        int iCompressedDataSize = IO.readSInt16LE(abPanekitFrame, 4);
        int iLzss10_11          = IO.readSInt16LE(abPanekitFrame, 6);
        int iWidth              = IO.readSInt16LE(abPanekitFrame, 8);
        int iHeight             = IO.readSInt16LE(abPanekitFrame, 10);
        int iLzss14_15          = IO.readSInt16LE(abPanekitFrame, 12);
        int iLzss12_13          = IO.readSInt16LE(abPanekitFrame, 14);
        int iLzss16_17_18_19    = IO.readSInt32LE(abPanekitFrame, 16);
        int iLzss20_21          = IO.readSInt16LE(abPanekitFrame, 20);
        int iLzss22_23          = IO.readSInt16LE(abPanekitFrame, 22);
        int iLzss24_25_26_27    = IO.readSInt32LE(abPanekitFrame, 24);
        int iLzss28_29          = IO.readSInt16LE(abPanekitFrame, 28);
        int iLzss30_31          = IO.readSInt16LE(abPanekitFrame, 30);

        iMdecCodeCount   = ~iMdecCodeCount;
        iMagic3800       = ~iMagic3800;
        iLzss16_17_18_19 = iLzss16_17_18_19 - 36997;
        iLzss22_23       = iLzss22_23       + 26900;
        iLzss24_25_26_27 = iLzss24_25_26_27 - 34975;
        iLzss30_31       = iLzss30_31       + 27396;

        IO.writeInt16LE(abPanekitFrame, 0, (short) iMdecCodeCount);
        IO.writeInt16LE(abPanekitFrame, 2, (short) iMagic3800);
        IO.writeInt16LE(abPanekitFrame, 4, (short) iWidth);
        IO.writeInt16LE(abPanekitFrame, 6, (short) iHeight);
        IO.writeInt16LE(abPanekitFrame, 8, (short) iCompressedDataSize);
        IO.writeInt16LE(abPanekitFrame, 10, (short) iLzss10_11);
        IO.writeInt16LE(abPanekitFrame, 12, (short) iLzss12_13);
        IO.writeInt16LE(abPanekitFrame, 14, (short) iLzss14_15);
        IO.writeInt32LE(abPanekitFrame, 16, iLzss16_17_18_19);
        IO.writeInt16LE(abPanekitFrame, 20, (short) iLzss20_21);
        IO.writeInt16LE(abPanekitFrame, 22, (short) iLzss22_23);
        IO.writeInt32LE(abPanekitFrame, 24, iLzss24_25_26_27);
        IO.writeInt16LE(abPanekitFrame, 28, (short) iLzss28_29);
        IO.writeInt16LE(abPanekitFrame, 30, (short) iLzss30_31);
    }

    /**
     * Modifies the IKI frame to obfuscate the first 32 bytes.
     */
    public static void obfuscate(@Nonnull byte[] abIkiFrame) {
        int iMdecCodeCount      = IO.readSInt16LE(abIkiFrame, 0);
        int iMagic3800          = IO.readSInt16LE(abIkiFrame, 2);
        int iWidth              = IO.readSInt16LE(abIkiFrame, 4);
        int iHeight             = IO.readSInt16LE(abIkiFrame, 6);
        int iCompressedDataSize = IO.readSInt16LE(abIkiFrame, 8);
        int iLzss10_11          = IO.readSInt16LE(abIkiFrame, 10);
        int iLzss12_13          = IO.readSInt16LE(abIkiFrame, 12);
        int iLzss14_15          = IO.readSInt16LE(abIkiFrame, 14);
        int iLzss16_17_18_19    = IO.readSInt32LE(abIkiFrame, 16);
        int iLzss20_21          = IO.readSInt16LE(abIkiFrame, 20);
        int iLzss22_23          = IO.readSInt16LE(abIkiFrame, 22);
        int iLzss24_25_26_27    = IO.readSInt32LE(abIkiFrame, 24);
        int iLzss28_29          = IO.readSInt16LE(abIkiFrame, 28);
        int iLzss30_31          = IO.readSInt16LE(abIkiFrame, 30);

        iMdecCodeCount   = ~iMdecCodeCount;
        iMagic3800       = ~iMagic3800;
        iLzss16_17_18_19 = iLzss16_17_18_19 + 36997;
        iLzss22_23       = iLzss22_23       - 26900;
        iLzss24_25_26_27 = iLzss24_25_26_27 + 34975;
        iLzss30_31       = iLzss30_31       - 27396;

        IO.writeInt16LE(abIkiFrame,  0, (short) iMdecCodeCount);
        IO.writeInt16LE(abIkiFrame,  2, (short) iMagic3800);
        IO.writeInt16LE(abIkiFrame,  4, (short) iCompressedDataSize);
        IO.writeInt16LE(abIkiFrame,  6, (short) iLzss10_11);
        IO.writeInt16LE(abIkiFrame,  8, (short) iWidth);
        IO.writeInt16LE(abIkiFrame, 10, (short) iHeight);
        IO.writeInt16LE(abIkiFrame, 12, (short) iLzss14_15);
        IO.writeInt16LE(abIkiFrame, 14, (short) iLzss12_13);
        IO.writeInt32LE(abIkiFrame, 16, iLzss16_17_18_19);
        IO.writeInt16LE(abIkiFrame, 20, (short) iLzss20_21);
        IO.writeInt16LE(abIkiFrame, 22, (short) iLzss22_23);
        IO.writeInt32LE(abIkiFrame, 24, iLzss24_25_26_27);
        IO.writeInt16LE(abIkiFrame, 28, (short) iLzss28_29);
        IO.writeInt16LE(abIkiFrame, 30, (short) iLzss30_31);
    }

}
