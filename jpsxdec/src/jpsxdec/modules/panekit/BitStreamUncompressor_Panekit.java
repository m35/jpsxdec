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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jpsxdec.psxvideo.bitstreams.ArrayBitReader;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_Iki;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor_STRv2;
import jpsxdec.psxvideo.mdec.Calc;
import jpsxdec.psxvideo.mdec.MdecException;
import jpsxdec.psxvideo.mdec.MdecInputStream;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IO;

/**
 * Panekit - Infinitive Crafting Toy Case [SCPS-10096] frame is just an
 * obfuscated IKI frame. Extend IKI with obfuscation logic.
 */
public class BitStreamUncompressor_Panekit extends BitStreamUncompressor_Iki {

    private static final Logger LOG = Logger.getLogger(BitStreamUncompressor_Panekit.class.getName());


    public static @CheckForNull PanekitHeader makePanekitHeader(@Nonnull byte[] abFrameData, int iDataSize) {
        if (iDataSize < 32)
            return null;

        int iMdecCodeCount      = IO.readSInt16LE(abFrameData, 0);
        int iMagic3800          = IO.readSInt16LE(abFrameData, 2);
        int iCompressedDataSize = IO.readSInt16LE(abFrameData, 4);
        int iWidth              = IO.readSInt16LE(abFrameData, 8);
        int iHeight             = IO.readSInt16LE(abFrameData, 10);

        iMdecCodeCount = ~iMdecCodeCount;
        iMagic3800 = ~iMagic3800;

        if (iMdecCodeCount < 0 || iMagic3800 != 0x3800 || iWidth < 1 || iHeight < 1 || iCompressedDataSize < 1) {
            return null;
        }

        if (iDataSize < IkiHeader.SIZEOF + iCompressedDataSize) {
            LOG.log(Level.WARNING, "Incomplete Panekit frame header");
            return null;
        }

        int iBlockCount = Calc.blocks(iWidth, iHeight);
        int iQscaleDcLookupTableSize = iBlockCount * 2; // 2 bytes per block

        byte[] abDeobfuscatedHeader = Arrays.copyOf(abFrameData, IkiHeader.SIZEOF + iCompressedDataSize);
        PanekitIkiObfuscation.deobfuscate(abDeobfuscatedHeader);

        byte[] abQscaleDcLookupTable;
        try {
            abQscaleDcLookupTable = ikiLzssUncompress(abDeobfuscatedHeader, IkiHeader.SIZEOF, iQscaleDcLookupTableSize);
        } catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }

        return new PanekitHeader(iMdecCodeCount, iWidth, iHeight, iCompressedDataSize, iBlockCount, abQscaleDcLookupTable);
    }

    public static class PanekitHeader extends IkiHeader {

        public PanekitHeader(int iMdecCodeCount, int iWidth, int iHeight,
                             int iCompressedDataSize, int iBlockCount,
                             byte[] abQscaleDcLookupTable)
        {
            super(iMdecCodeCount, iWidth, iHeight, iCompressedDataSize, iBlockCount, abQscaleDcLookupTable);
        }
    }

    public static @Nonnull BitStreamUncompressor_Panekit makePanekit(@Nonnull byte[] abFrameData)
            throws BinaryDataNotRecognized
    {
        return makePanekit(abFrameData, abFrameData.length);
    }
    public static @Nonnull BitStreamUncompressor_Panekit makePanekit(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        BitStreamUncompressor_Panekit bsu = makePanekitNoThrow(abFrameData, iDataSize);
        if (bsu == null)
            throw new BinaryDataNotRecognized();
        return bsu;
    }

    public static @CheckForNull BitStreamUncompressor_Panekit makePanekitNoThrow(@Nonnull byte[] abFrameData, int iDataSize)
            throws BinaryDataNotRecognized
    {
        PanekitHeader header = makePanekitHeader(abFrameData, iDataSize);
        if (header == null)
            return null;

        ArrayBitReader bitReader = new ArrayBitReader(abFrameData, BitStreamUncompressor_STRv2.LITTLE_ENDIAN_SHORT_ORDER,
                                                      IkiHeader.SIZEOF + header.getCompressedDataSize(), iDataSize);

        return new BitStreamUncompressor_Panekit(header, bitReader);
    }

    private BitStreamUncompressor_Panekit(@Nonnull PanekitHeader header,
                                          @Nonnull ArrayBitReader bitReader)
    {
        super(header, bitReader);
    }

    @Override
    public @Nonnull BitStreamCompressor_Panekit makeCompressor() {
        return new BitStreamCompressor_Panekit(getWidth(), getHeight());
    }

    // =========================================================================

    public static class BitStreamCompressor_Panekit extends BitStreamCompressor_Iki {

        private BitStreamCompressor_Panekit(int iWidth, int iHeight) {
            super(iWidth, iHeight);
        }

        @Override
        public @Nonnull byte[] compress(@Nonnull MdecInputStream inStream)
                throws MdecException.EndOfStream, MdecException.ReadCorruption
        {
            byte[] abNewDemux = super.compress(inStream);
            PanekitIkiObfuscation.obfuscate(abNewDemux);
            return abNewDemux;
        }
    }
}
