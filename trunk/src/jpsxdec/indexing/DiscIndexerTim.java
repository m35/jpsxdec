/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
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

package jpsxdec.indexing;

import jpsxdec.discitems.DiscItemTIM;
import java.io.IOException;
import java.util.logging.Logger;
import jpsxdec.discitems.DiscItem;
import jpsxdec.discitems.DiscItemSerialization;
import jpsxdec.sectors.IdentifiedSector;
import jpsxdec.tim.Tim;
import jpsxdec.util.IO;
import jpsxdec.util.NotThisTypeException;

/**
 * Searches for TIM images
 */
public class DiscIndexerTim extends DiscIndexer {

    private static final Logger log = Logger.getLogger(DiscIndexerTim.class.getName());

    @Override
    public DiscItem deserializeLineRead(DiscItemSerialization serial) {
        try {
            if (DiscItemTIM.TYPE_ID.equals(serial.getType())) {
                return new DiscItemTIM(serial);
            }
        } catch (NotThisTypeException ex) {}
        return null;
    }

    @Override
    public void indexingSectorRead(IdentifiedSector oSect) {
    }

    @Override
    public void indexingEndOfDisc() {
    }

    @Override
    public void staticRead(DemuxedUnidentifiedDataStream inStream) throws IOException {

        final int iStartSector = inStream.getCurrentSector();
        final int iStartOffset = inStream.getCurrentSectorOffset();

        // tag
        if (IO.readUInt8(inStream) != 0x10)
            return;

        // version
        if (IO.readUInt8(inStream) != 0)
            return;

        // unkn 1
        if (IO.readUInt16LE(inStream) != 0)
            return;

        int iBpp_HasColorLookupTbl = IO.readUInt16LE(inStream);
        if ((iBpp_HasColorLookupTbl & 0xFFF4) != 0)
            return;

        // unkn 2
        if (IO.readUInt16LE(inStream) != 0)
            return;

        //-------------------------------------------------

        int iBitsPerPixel = Tim.BITS_PER_PIX[iBpp_HasColorLookupTbl & 3];

        final int iPaletteCount;
        // has CLUT
        if ((iBpp_HasColorLookupTbl & 8) != 0) {

            long lngLength = IO.readUInt32LE(inStream);
            if (lngLength <= 0)
                return;

            // clut x,y
            IO.skip(inStream, 4);

            int iClutWidth = IO.readUInt16LE(inStream);
            if (iClutWidth == 0)
                return;

            int iClutHeight = IO.readUInt16LE(inStream);
            if (iClutHeight == 0)
                return;

            if (lngLength != (iClutWidth * iClutHeight * 2 + 12))
                return;

            iPaletteCount = (iClutWidth * iClutHeight) / (1 << iBitsPerPixel);

            IO.skip(inStream, iClutWidth * iClutHeight * 2);
        } else {
            iPaletteCount = 1;
        }

        long lngImageLength = IO.readUInt32LE(inStream);
        if (lngImageLength <= 0)
            return;

        // image x,y
        IO.skip(inStream, 4);

        int iImageByteWidth = IO.readUInt16LE(inStream);
        if (iImageByteWidth == 0)
            return;

        int iImageHeight = IO.readUInt16LE(inStream);
        if (iImageHeight == 0)
            return;

        if (lngImageLength != iImageByteWidth * iImageHeight * 2 + 12)
            return;

        IO.skip(inStream, (iImageByteWidth * iImageHeight) * 2);

        int iPixelWidth;
        switch (iBitsPerPixel) {
            case 4:  iPixelWidth = (int)(iImageByteWidth * 2 * 2); break;
            case 8:  iPixelWidth = (int)(iImageByteWidth * 2    ); break;
            case 16: iPixelWidth = (int)(iImageByteWidth        ); break;
            case 24: iPixelWidth = (int)(iImageByteWidth * 2 / 3); break;
            default: throw new RuntimeException("Impossible Tim BPP " + iBitsPerPixel);
        }

        super.addDiscItem(new DiscItemTIM(
                iStartSector, inStream.getCurrentSector(),
                iStartOffset, iPaletteCount, iBitsPerPixel,
                iPixelWidth, iImageHeight));
    }

    @Override
    public void mediaListGenerated(DiscIndex index) {
    }

}
